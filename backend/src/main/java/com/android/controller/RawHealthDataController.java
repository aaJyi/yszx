package com.android.controller;

import com.android.entity.RawHealthData;
import com.android.service.IRawHealthDataService;
import com.android.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 原始健康数据前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-12
 */
@Slf4j
@RestController
@RequestMapping("/raw-health-data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RawHealthDataController {
    
    private final IRawHealthDataService rawHealthDataService;
    
    /**
     * 通用上传接口（支持指定userId，用于为家人上传）
     * 
     * @param file 文件
     * @param userId 用户ID（可选，如果未提供则从ThreadLocal获取）
     * @param dataType 数据类型（可选，默认REPORT）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "dataType", required = false, defaultValue = "REPORT") String dataType) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 如果未提供userId，从ThreadLocal获取
            Long finalUserId = userId;
            if (finalUserId == null) {
                finalUserId = UserContext.getUserId();
            }
            if (finalUserId == null) {
                result.put("code", 401);
                result.put("message", "用户ID不能为空");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 临时设置userId到ThreadLocal，以便Service层使用
            Long originalUserId = UserContext.getUserId();
            try {
                UserContext.setUserId(finalUserId);
                // 根据dataType调用不同的上传方法
                RawHealthData rawHealthData;
                switch (dataType.toUpperCase()) {
                    case "REPORT":
                        rawHealthData = rawHealthDataService.uploadReport(file);
                        break;
                    case "MEDICAL_RECORD":
                        rawHealthData = rawHealthDataService.uploadMedicalRecordImage(file);
                        break;
                    default:
                        rawHealthData = rawHealthDataService.uploadReport(file);
                }
                
                result.put("code", 200);
                result.put("message", "上传成功");
                result.put("data", Map.of(
                        "id", rawHealthData.getId(),
                        "userId", rawHealthData.getUserId(),
                        "dataType", rawHealthData.getDataType(),
                        "formatType", rawHealthData.getFormatType(),
                        "fileName", rawHealthData.getFileName(),
                        "fileSize", rawHealthData.getFileSize(),
                        "uploadTime", rawHealthData.getUploadTime()
                ));
                
                return ResponseEntity.ok(result);
            } finally {
                // 恢复原始userId
                if (originalUserId != null) {
                    UserContext.setUserId(originalUserId);
                } else {
                    UserContext.clear();
                }
            }
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 拍报告接口（上传报告图片）
     * 路径包含REPORT，用户ID通过ThreadLocal获取
     * data_type固定为REPORT，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/REPORT/upload")
    public ResponseEntity<Map<String, Object>> uploadReport(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器或过滤器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传报告
            RawHealthData rawHealthData = rawHealthDataService.uploadReport(file);
            
            result.put("code", 200);
            result.put("message", "报告上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("报告上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 就诊记录接口（拍照上传）
     * 路径包含MEDICAL_RECORD，用户ID通过ThreadLocal获取
     * data_type固定为MEDICAL_RECORD，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/MEDICAL_RECORD/upload-image")
    public ResponseEntity<Map<String, Object>> uploadMedicalRecordImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传就诊记录（图片）
            RawHealthData rawHealthData = rawHealthDataService.uploadMedicalRecordImage(file);
            
            result.put("code", 200);
            result.put("message", "就诊记录上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("就诊记录（图片）上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 就诊记录接口（文件上传）
     * 路径包含MEDICAL_RECORD，用户ID通过ThreadLocal获取
     * data_type固定为MEDICAL_RECORD，format_type为PDF
     * 
     * @param file PDF文件
     * @return 上传结果
     */
    @PostMapping("/MEDICAL_RECORD/upload-file")
    public ResponseEntity<Map<String, Object>> uploadMedicalRecordFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传就诊记录（PDF）
            RawHealthData rawHealthData = rawHealthDataService.uploadMedicalRecordFile(file);
            
            result.put("code", 200);
            result.put("message", "就诊记录上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("就诊记录（PDF）上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 拍皮肤接口（上传皮肤照片）
     * 路径包含SKIN，用户ID通过ThreadLocal获取
     * data_type固定为SKIN，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/SKIN/upload")
    public ResponseEntity<Map<String, Object>> uploadSkin(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传皮肤照片
            RawHealthData rawHealthData = rawHealthDataService.uploadSkin(file);
            
            result.put("code", 200);
            result.put("message", "皮肤照片上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("皮肤照片上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 检查检验接口（拍报告上传）
     * 路径包含LAB，用户ID通过ThreadLocal获取
     * data_type固定为LAB，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/LAB/upload")
    public ResponseEntity<Map<String, Object>> uploadLab(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传检查检验
            RawHealthData rawHealthData = rawHealthDataService.uploadLab(file);
            
            result.put("code", 200);
            result.put("message", "检查检验上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("检查检验上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 情绪检测接口（拍报告上传）
     * 路径包含EMOTION，用户ID通过ThreadLocal获取
     * data_type固定为EMOTION，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/EMOTION/upload")
    public ResponseEntity<Map<String, Object>> uploadEmotion(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传情绪检测
            RawHealthData rawHealthData = rawHealthDataService.uploadEmotion(file);
            
            result.put("code", 200);
            result.put("message", "情绪检测上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("情绪检测上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 基因检测接口（拍报告上传）
     * 路径包含GENETIC，用户ID通过ThreadLocal获取
     * data_type固定为GENETIC，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/GENETIC/upload")
    public ResponseEntity<Map<String, Object>> uploadGenetic(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传基因检测
            RawHealthData rawHealthData = rawHealthDataService.uploadGenetic(file);
            
            result.put("code", 200);
            result.put("message", "基因检测上传成功");
            result.put("data", Map.of(
                    "id", rawHealthData.getId(),
                    "userId", rawHealthData.getUserId(),
                    "dataType", rawHealthData.getDataType(),
                    "formatType", rawHealthData.getFormatType(),
                    "fileName", rawHealthData.getFileName(),
                    "fileSize", rawHealthData.getFileSize(),
                    "uploadTime", rawHealthData.getUploadTime()
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("基因检测上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 拍三餐接口（上传三餐照片）
     * 路径包含MEAL，用户ID通过ThreadLocal获取
     * data_type固定为MEAL，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/MEAL/upload")
    public ResponseEntity<Map<String, Object>> uploadMeal(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从ThreadLocal获取用户ID（在拦截器中设置）
            Long userId = UserContext.getUserId();
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "用户未登录，请先登录");
                result.put("data", null);
                return ResponseEntity.status(401).body(result);
            }
            
            // 调用Service层方法上传拍三餐
            RawHealthData rawHealthData = rawHealthDataService.uploadMeal(file);
            
            result.put("code", 200);
            result.put("message", "拍三餐上传成功");
            Map<String, Object> data = new HashMap<>();
            data.put("id", rawHealthData.getId());
            data.put("userId", rawHealthData.getUserId());
            data.put("dataType", rawHealthData.getDataType());
            data.put("formatType", rawHealthData.getFormatType());
            data.put("fileName", rawHealthData.getFileName());
            data.put("fileSize", rawHealthData.getFileSize());
            data.put("uploadTime", rawHealthData.getUploadTime());
            data.put("nutrition", rawHealthData.getMealNutrition());
            data.put("nutritionError", rawHealthData.getMealNutritionError());
            result.put("data", data);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("拍三餐上传失败", e);
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 根据用户ID批量查询该用户的所有最新健康数据
     * 用于传递给智能体模块，生成健康档案
     * 返回JSON格式包含：userId、dataType、formatType、rawData（Base64编码）
     * 
     * @param userId 用户ID（路径参数）
     * @return 用户的所有最新健康数据列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getAllLatestHealthDataByUserId(@PathVariable("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (userId == null) {
                result.put("code", 400);
                result.put("message", "用户ID不能为空");
                result.put("data", null);
                return ResponseEntity.status(400).body(result);
            }
            
            // 调用Service层方法查询用户的所有最新健康数据
            List<Map<String, Object>> healthDataList = rawHealthDataService.getAllLatestHealthDataByUserId(userId);
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", healthDataList);
            result.put("count", healthDataList.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("查询用户{}的健康数据失败", userId, e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 获取用户及其管理的家人的所有原始数据列表（包括所属人名字）
     * 
     * @param userId 用户ID（路径参数）
     * @return 原始数据列表
     */
    @GetMapping("/list/user/{userId}")
    public ResponseEntity<Map<String, Object>> getAllRawHealthDataWithOwnerName(@PathVariable("userId") Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (userId == null) {
                result.put("code", 400);
                result.put("message", "用户ID不能为空");
                result.put("data", null);
                return ResponseEntity.status(400).body(result);
            }
            
            // 调用Service层方法查询用户及其家人的原始数据
            List<Map<String, Object>> dataList = rawHealthDataService.getAllRawHealthDataWithOwnerName(userId);
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", dataList);
            result.put("count", dataList.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("查询用户{}及其家人的原始数据失败", userId, e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 根据数据ID下载健康数据的原始文件
     * 支持下载图片、PDF等所有格式的文件
     * 文件名格式：所属人名字+"健康数据"+文档格式
     * 
     * @param id 数据ID（路径参数）
     * @param ownerName 所属人名字（查询参数，可选）
     * @return 文件的二进制流
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadHealthData(
            @PathVariable("id") Long id,
            @RequestParam(value = "ownerName", required = false) String ownerName) {
        try {
            if (id == null) {
                return ResponseEntity.status(400).build();
            }
            
            // 调用Service层方法获取健康数据
            RawHealthData healthData = rawHealthDataService.getHealthDataById(id);
            
            if (healthData == null || healthData.getRawData() == null || healthData.getRawData().length == 0) {
                return ResponseEntity.status(404).build();
            }
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            
            // 根据formatType设置Content-Type
            String contentType = "application/octet-stream"; // 默认二进制流
            String fileExtension = "";
            
            if (healthData.getFormatType() != null) {
                switch (healthData.getFormatType()) {
                    case "IMAGE":
                        // 根据文件扩展名判断图片类型
                        String originalFileName = healthData.getFileName();
                        if (originalFileName != null) {
                            String lowerFileName = originalFileName.toLowerCase();
                            if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                                contentType = "image/jpeg";
                                fileExtension = ".jpg";
                            } else if (lowerFileName.endsWith(".png")) {
                                contentType = "image/png";
                                fileExtension = ".png";
                            } else if (lowerFileName.endsWith(".gif")) {
                                contentType = "image/gif";
                                fileExtension = ".gif";
                            } else {
                                contentType = "image/jpeg";
                                fileExtension = ".jpg";
                            }
                        } else {
                            contentType = "image/jpeg";
                            fileExtension = ".jpg";
                        }
                        break;
                    case "PDF":
                        contentType = "application/pdf";
                        fileExtension = ".pdf";
                        break;
                    case "JSON":
                        contentType = "application/json";
                        fileExtension = ".json";
                        break;
                    case "CSV":
                        contentType = "text/csv";
                        fileExtension = ".csv";
                        break;
                    case "TEXT":
                        contentType = "text/plain";
                        fileExtension = ".txt";
                        break;
                    default:
                        // 从原始文件名提取扩展名
                        if (healthData.getFileName() != null) {
                            int lastDot = healthData.getFileName().lastIndexOf('.');
                            if (lastDot > 0) {
                                fileExtension = healthData.getFileName().substring(lastDot);
                            }
                        }
                }
            }
            
            // 构建文件名：所属人名字+"健康数据"+文档格式
            String downloadFileName;
            if (ownerName != null && !ownerName.trim().isEmpty()) {
                downloadFileName = ownerName + "健康数据" + fileExtension;
            } else {
                // 如果没有提供所属人名字，使用原始文件名或默认名称
                downloadFileName = healthData.getFileName() != null 
                        ? healthData.getFileName() 
                        : "健康数据" + id + fileExtension;
            }
            
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", downloadFileName);
            headers.setContentLength(healthData.getRawData().length);
            
            log.info("下载健康数据，ID: {}, 用户ID: {}, 文件名: {}, 大小: {} bytes", 
                    id, healthData.getUserId(), downloadFileName, healthData.getRawData().length);
            
            return new ResponseEntity<>(healthData.getRawData(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("下载健康数据失败，ID: {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }
}
