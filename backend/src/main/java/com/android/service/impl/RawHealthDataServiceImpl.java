package com.android.service.impl;

import com.android.entity.RawHealthData;
import com.android.entity.FamilyMember;
import com.android.entity.TUser;
import com.android.mapper.RawHealthDataMapper;
import com.android.service.IRawHealthDataService;
import com.android.service.IFamilyMemberService;
import com.android.service.ITUserService;
import com.android.util.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * <p>
 * 原始健康数据服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RawHealthDataServiceImpl extends ServiceImpl<RawHealthDataMapper, RawHealthData> implements IRawHealthDataService {
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    /** 注入自身代理，用于同类内调用 @Async 方法，确保异步生效 */
    @Lazy
    @Autowired
    private RawHealthDataServiceImpl self;
    
    private final IFamilyMemberService familyMemberService;
    private final ITUserService tUserService;
    
    /**
     * Medical AI 智能体服务基础URL
     * 配置示例：medical.ai.base.url=http://localhost:8000
     * 如果未配置，则不调用 webhook
     */
    @Value("${medical.ai.base.url:}")
    private String medicalAiBaseUrl;

    /**
     * food-train HTTP 预测地址，如 http://127.0.0.1:5001/predict
     */
    @Value("${food.train.predict-url:}")
    private String foodTrainPredictUrl;

    /**
     * 调用本地 food-train 服务估算营养成分（失败时不影响已落库的原始图）。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void attachMealNutritionPrediction(RawHealthData entity, byte[] imageBytes, String filename) {
        if (restTemplate == null) {
            entity.setMealNutritionError("RestTemplate 未注入，无法调用 food-train");
            return;
        }
        if (foodTrainPredictUrl == null || foodTrainPredictUrl.isBlank()) {
            entity.setMealNutritionError("未配置 food.train.predict-url");
            return;
        }
        String url = foodTrainPredictUrl.trim();
        if (!url.contains("/predict")) {
            url = url.replaceAll("/$", "") + "/predict";
        }
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename != null && !filename.isEmpty() ? filename : "meal.jpg";
                }
            };
            body.add("file", resource);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
            Map<?, ?> respBody = resp.getBody();
            if (respBody == null) {
                entity.setMealNutritionError("food-train 返回空");
                return;
            }
            Object code = respBody.get("code");
            int c = code instanceof Number ? ((Number) code).intValue() : -1;
            if (c == 200) {
                Object nutrition = respBody.get("nutrition");
                if (nutrition instanceof Map) {
                    entity.setMealNutrition((Map<String, Object>) nutrition);
                } else {
                    entity.setMealNutritionError("响应无 nutrition 字段");
                }
            } else {
                Object msg = respBody.get("message");
                entity.setMealNutritionError(msg != null ? String.valueOf(msg) : "预测失败");
            }
        } catch (Exception e) {
            log.warn("food-train 预测异常: {}", e.getMessage());
            entity.setMealNutritionError("预测服务不可用: " + e.getMessage());
        }
    }

    /**
     * 统一触发智能体重建流程（异步），确保上传后自动分析。
     */
    private void triggerMedicalAiRebuild(Long userId, String dataType) {
        if (self != null) {
            self.notifyMedicalAiDataChanged(userId);
            log.info("已触发智能体重建通知，用户ID: {}, 数据类型: {}", userId, dataType);
        } else {
            log.warn("RawHealthDataServiceImpl 代理未注入，无法触发智能体重建通知，用户ID: {}, 数据类型: {}", userId, dataType);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadReport(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "report_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建报告实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("REPORT"); // 固定为REPORT（拍报告）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("报告保存失败");
            }
            
            log.info("报告上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "REPORT");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("报告上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("报告上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadMedicalRecordImage(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "medical_record_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建就诊记录实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("MEDICAL_RECORD"); // 固定为MEDICAL_RECORD（就诊记录）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("就诊记录保存失败");
            }
            
            log.info("就诊记录（图片）上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "MEDICAL_RECORD");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("就诊记录（图片）上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("就诊记录上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadMedicalRecordFile(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "medical_record_" + System.currentTimeMillis() + ".pdf";
        }
        
        // 验证文件类型（只允许PDF）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new RuntimeException("只能上传PDF文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建就诊记录实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("MEDICAL_RECORD"); // 固定为MEDICAL_RECORD（就诊记录）
            rawHealthData.setFormatType("PDF"); // 格式类型为PDF
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("就诊记录保存失败");
            }
            
            log.info("就诊记录（PDF）上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "MEDICAL_RECORD");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("就诊记录（PDF）上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("就诊记录上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadSkin(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "skin_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建皮肤照片实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("SKIN"); // 固定为SKIN（拍皮肤）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("皮肤照片保存失败");
            }
            
            log.info("皮肤照片上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "SKIN");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("皮肤照片上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("皮肤照片上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadLab(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "lab_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建检查检验实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("LAB"); // 固定为LAB（检查检验）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("检查检验保存失败");
            }
            
            log.info("检查检验上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "LAB");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("检查检验上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("检查检验上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadEmotion(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "emotion_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建情绪检测实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("EMOTION"); // 固定为EMOTION（情绪检测）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("情绪检测保存失败");
            }
            
            log.info("情绪检测上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "EMOTION");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("情绪检测上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("情绪检测上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadGenetic(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "genetic_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建基因检测实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("GENETIC"); // 固定为GENETIC（基因检测）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("基因检测保存失败");
            }
            
            log.info("基因检测上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);

            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "GENETIC");
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("基因检测上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("基因检测上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RawHealthData uploadMeal(MultipartFile file) {
        // 从ThreadLocal获取用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "meal_" + System.currentTimeMillis() + ".jpg";
        }
        
        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }
        
        // 验证文件大小（限制10MB以内）
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
        
        try {
            // 读取文件二进制内容
            byte[] rawData = file.getBytes();
            
            // 创建拍三餐实体
            LocalDateTime now = LocalDateTime.now();
            RawHealthData rawHealthData = new RawHealthData();
            rawHealthData.setUserId(userId);
            rawHealthData.setDataType("MEAL"); // 固定为MEAL（拍三餐）
            rawHealthData.setFormatType("IMAGE"); // 格式类型为IMAGE（图片）
            rawHealthData.setFileName(originalFilename);
            rawHealthData.setFileSize(fileSize);
            rawHealthData.setRawData(rawData); // 设置二进制内容
            rawHealthData.setUploadTime(now);
            rawHealthData.setCreateTime(now);
            rawHealthData.setUpdateTime(now);
            
            // 保存到数据库
            boolean saved = this.save(rawHealthData);
            
            if (!saved) {
                throw new RuntimeException("拍三餐保存失败");
            }
            
            log.info("拍三餐上传成功，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    rawHealthData.getId(), userId, originalFilename, fileSize);
            
            // 异步通知智能体（通过代理调用，确保 @Async 生效，不阻塞返回）
            triggerMedicalAiRebuild(userId, "MEAL");

            attachMealNutritionPrediction(rawHealthData, rawData, originalFilename);
            
            // 返回时不包含二进制内容，减少内存占用
            rawHealthData.setRawData(null);
            return rawHealthData;
            
        } catch (IOException e) {
            log.error("读取文件内容失败，用户ID: {}", userId, e);
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("拍三餐上传失败，用户ID: {}", userId, e);
            throw new RuntimeException("拍三餐上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<Map<String, Object>> getAllLatestHealthDataByUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        try {
            // 查询该用户的所有健康数据，按上传时间降序排序（最新的在前）
            QueryWrapper<RawHealthData> queryWrapper = Wrappers.query();
            queryWrapper.eq("user_id", userId)
                       .orderByDesc("upload_time");
            
            List<RawHealthData> healthDataList = this.list(queryWrapper);
            
            // 转换为Map列表，包含Base64编码的raw-data
            List<Map<String, Object>> resultList = new ArrayList<>();
            
            for (RawHealthData data : healthDataList) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", data.getUserId());
                item.put("dataType", data.getDataType());
                item.put("formatType", data.getFormatType());
                
                // 将raw-data转换为Base64编码，方便JSON传输
                if (data.getRawData() != null && data.getRawData().length > 0) {
                    String base64Data = Base64.getEncoder().encodeToString(data.getRawData());
                    item.put("rawData", base64Data);
                } else {
                    item.put("rawData", null);
                }
                
                // 额外信息，方便调试和识别
                item.put("id", data.getId());
                item.put("fileName", data.getFileName());
                item.put("fileSize", data.getFileSize());
                item.put("uploadTime", data.getUploadTime());
                
                resultList.add(item);
            }
            
            log.info("查询用户{}的健康数据，共{}条", userId, resultList.size());
            
            return resultList;
            
        } catch (Exception e) {
            log.error("查询用户{}的健康数据失败", userId, e);
            throw new RuntimeException("查询健康数据失败: " + e.getMessage());
        }
    }
    
    @Override
    public RawHealthData getHealthDataById(Long id) {
        if (id == null) {
            throw new RuntimeException("数据ID不能为空");
        }
        
        try {
            RawHealthData healthData = this.getById(id);
            
            if (healthData == null) {
                throw new RuntimeException("未找到ID为" + id + "的健康数据");
            }
            
            log.info("查询健康数据，ID: {}, 用户ID: {}, 数据类型: {}", 
                    id, healthData.getUserId(), healthData.getDataType());
            
            return healthData;
            
        } catch (Exception e) {
            log.error("查询健康数据失败，ID: {}", id, e);
            throw new RuntimeException("查询健康数据失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<Map<String, Object>> getAllRawHealthDataWithOwnerName(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        try {
            List<Map<String, Object>> resultList = new ArrayList<>();
            
            // 1. 获取用户自己上传的原始数据
            QueryWrapper<RawHealthData> userWrapper = Wrappers.query();
            userWrapper.eq("user_id", userId.longValue())
                       .orderByDesc("upload_time");
            List<RawHealthData> userDataList = this.list(userWrapper);
            
            // 获取用户自己的名字
            TUser user = tUserService.getById(userId.longValue());
            String userName = user != null ? (user.getNickname() != null ? user.getNickname() : "用户" + userId) : "用户" + userId;
            
            for (RawHealthData data : userDataList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", data.getId());
                item.put("userId", data.getUserId());
                item.put("dataType", data.getDataType());
                item.put("formatType", data.getFormatType());
                item.put("fileName", data.getFileName());
                item.put("fileSize", data.getFileSize());
                item.put("uploadTime", data.getUploadTime());
                item.put("ownerName", userName); // 所属人名字
                resultList.add(item);
            }
            
            // 2. 获取用户管理的家人列表
            LambdaQueryWrapper<FamilyMember> familyWrapper = new LambdaQueryWrapper<>();
            familyWrapper.eq(FamilyMember::getOwnerUserId, userId);
            List<FamilyMember> familyMembers = familyMemberService.list(familyWrapper);
            
            // 3. 获取每个家人上传的原始数据
            for (FamilyMember member : familyMembers) {
                Integer memberUserId = member.getRegisteredUserId();
                if (memberUserId == null) {
                    // 如果家人未注册，跳过
                    continue;
                }
                
                QueryWrapper<RawHealthData> memberWrapper = Wrappers.query();
                memberWrapper.eq("user_id", memberUserId.longValue())
                           .orderByDesc("upload_time");
                List<RawHealthData> memberDataList = this.list(memberWrapper);
                
                // 获取家人的名字
                String memberName = member.getFullName() != null ? member.getFullName() : "家人" + memberUserId;
                
                for (RawHealthData data : memberDataList) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", data.getId());
                    item.put("userId", data.getUserId());
                    item.put("dataType", data.getDataType());
                    item.put("formatType", data.getFormatType());
                    item.put("fileName", data.getFileName());
                    item.put("fileSize", data.getFileSize());
                    item.put("uploadTime", data.getUploadTime());
                    item.put("ownerName", memberName); // 所属人名字
                    resultList.add(item);
                }
            }
            
            // 按上传时间降序排序
            resultList.sort((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("uploadTime");
                LocalDateTime timeB = (LocalDateTime) b.get("uploadTime");
                if (timeA == null || timeB == null) {
                    return 0;
                }
                return timeB.compareTo(timeA);
            });
            
            log.info("查询用户{}及其家人的原始数据，共{}条", userId, resultList.size());
            
            return resultList;
            
        } catch (Exception e) {
            log.error("查询用户{}及其家人的原始数据失败", userId, e);
            throw new RuntimeException("查询原始数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步通知外部系统触发健康档案重建
     * 在原始数据上传成功后调用此方法，触发外部系统的健康档案重建流程
     * TODO: 智能体开发程序员 - 后续更新 medical.ai.base.url、rebuild 接口地址、请求/响应格式
     * 
     * @param userId 用户ID
     */
    @Async
    public void notifyMedicalAiDataChanged(Long userId) {
        // 使用 data_created 事件类型，表示新数据已创建
        notifyMedicalAiHealthUpdate(userId, "data_created", null);
    }
    
    /**
     * 异步通知外部系统触发健康档案重建
     * 根据接口规范：POST /health-archive-process/rebuild
     * 
     * @param userId 用户ID
     * @param event 事件类型：data_updated, data_created, data_deleted
     * @param updateTime 更新时间（ISO 8601格式），如果为null则使用当前时间
     */
    @Async
    public void notifyMedicalAiHealthUpdate(Long userId, String event, String updateTime) {
        // 如果未配置 Medical AI 基础URL，则不调用
        if (medicalAiBaseUrl == null || medicalAiBaseUrl.trim().isEmpty()) {
            log.debug("未配置 Medical AI 基础URL，跳过重建接口调用，用户ID：{}", userId);
            return;
        }
        
        // 如果 RestTemplate 未注入，则不调用
        if (restTemplate == null) {
            log.warn("RestTemplate 未注入，无法调用重建接口，用户ID：{}", userId);
            return;
        }
        
        // 构建请求URL（重建接口：/health-archive-process/rebuild）
        String baseUrl = medicalAiBaseUrl.endsWith("/") 
                ? medicalAiBaseUrl.substring(0, medicalAiBaseUrl.length() - 1) 
                : medicalAiBaseUrl;
        String rebuildUrl = baseUrl + "/health-archive-process/rebuild";
        
        try {
            // 1. 验证事件类型
            if (event == null || event.trim().isEmpty()) {
                event = "data_updated"; // 默认值
            }
            
            // 验证事件类型是否有效
            if (!event.equals("data_updated") && !event.equals("data_created") && !event.equals("data_deleted")) {
                log.warn("无效的事件类型：{}，使用默认值 data_updated", event);
                event = "data_updated";
            }
            
            // 2. 获取更新时间
            String updateTimeStr = updateTime;
            if (updateTimeStr == null || updateTimeStr.trim().isEmpty()) {
                updateTimeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            // 3. 构建请求体（符合重建接口规范）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId.intValue());
            requestBody.put("event", event);
            requestBody.put("updateTime", updateTimeStr);
            
            // 5. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 6. 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("调用外部系统重建接口，用户ID：{}，事件类型：{}，更新时间：{}，URL：{}", 
                    userId, event, updateTimeStr, rebuildUrl);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    rebuildUrl,
                    HttpMethod.POST,
                    requestEntity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            // 7. 处理响应（符合重建接口响应格式）
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer code = (Integer) responseBody.get("code");
                String message = (String) responseBody.get("message");
                Object data = responseBody.get("data");
                
                if (code != null && code == 200) {
                    log.info("重建任务提交成功，用户ID：{}，响应消息：{}，任务数据：{}", userId, message, data);
                } else {
                    log.warn("重建任务提交失败，用户ID：{}，响应码：{}，响应消息：{}", userId, code, message);
                }
            } else {
                log.warn("重建接口调用返回非成功状态码，用户ID：{}，HTTP状态码：{}", 
                        userId, response.getStatusCode());
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            // 404错误：接口不存在，这是可接受的（智能体系统可能还未实现该接口）
            // 记录为WARN级别，不影响主流程
            log.warn("外部系统重建接口不存在（404），用户ID：{}，事件类型：{}，URL：{}。智能体系统可能还未实现该接口，这是正常的。", 
                    userId, event, rebuildUrl);
        } catch (HttpClientErrorException e) {
            // 其他4xx错误（客户端错误）
            log.warn("调用外部系统重建接口返回客户端错误，用户ID：{}，事件类型：{}，HTTP状态码：{}，错误信息：{}", 
                    userId, event, e.getStatusCode(), e.getMessage());
        } catch (HttpServerErrorException e) {
            // 5xx错误（服务器错误）
            log.warn("外部系统重建接口返回服务器错误，用户ID：{}，事件类型：{}，HTTP状态码：{}，错误信息：{}", 
                    userId, event, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            // 其他异常（网络错误等）
            // 异步调用失败不影响主流程，只记录日志
            log.warn("调用外部系统重建接口失败，用户ID：{}，事件类型：{}，错误：{}", 
                    userId, event, e.getMessage());
        }
    }
}
