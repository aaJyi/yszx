package com.android.controller;

import com.android.common.Result;
import com.android.dto.HealthArchiveRequest;
import com.android.entity.HealthArchive;
import com.android.service.IHealthArchiveProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 健康档案处理控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Slf4j
@RestController
@RequestMapping("/health-archive-process")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "健康档案处理", description = "健康档案处理相关接口")
public class HealthArchiveProcessController {

    /**
     * 注入健康档案处理服务
     */
    private final IHealthArchiveProcessService healthArchiveProcessService;

    /**
     * 首次注册时保存基本信息（创建健康档案）
     * 简化版本，只保存基本信息、用户信息和健康标识
     *
     * @param request 健康档案请求对象
     * @param userId 用户ID（从header获取）
     * @return 保存结果
     */
    @Operation(summary = "首次注册保存基本信息", description = "用户首次注册时填写基本信息，创建健康档案")
    @PostMapping("/basic-info")
    public Result<Map<String, Object>> saveBasicInfo(
            @RequestBody HealthArchiveRequest request,
            @RequestHeader(value = "userId", required = false) Integer userId) {
        
        try {
            // 从header获取userId，如果没有则从ThreadLocal获取
            Integer finalUserId = userId;
            if (finalUserId == null) {
                Long userIdFromContext = com.android.util.UserContext.getUserId();
                if (userIdFromContext != null) {
                    finalUserId = userIdFromContext.intValue();
                }
            }
            
            if (finalUserId == null) {
                return Result.badRequest("用户ID不能为空");
            }
            
            // 调用处理服务保存健康档案
            HealthArchive healthArchive = healthArchiveProcessService.processAndSaveHealthArchive(
                    request, finalUserId, null);
            
            log.info("首次注册基本信息保存成功，档案ID：{}，用户ID：{}", 
                    healthArchive.getArchiveId(), healthArchive.getUserId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("archiveId", healthArchive.getArchiveId());
            data.put("userId", healthArchive.getUserId());
            data.put("userName", healthArchive.getUserName());
            
            return Result.success("基本信息保存成功", data);
            
        } catch (Exception e) {
            log.error("保存基本信息失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 更新健康档案
     * 更新健康档案主表及所有关联表的数据
     *
     * @param archiveId 档案ID
     * @param request 健康档案请求对象
     * @param userId 用户ID（从header获取）
     * @return 更新结果
     */
    @Operation(summary = "更新健康档案", description = "更新健康档案的所有信息")
    @PostMapping("/update/{archiveId}")
    public Result<Map<String, Object>> updateHealthArchive(
            @PathVariable Integer archiveId,
            @RequestBody HealthArchiveRequest request,
            @RequestHeader(value = "userId", required = false) Integer userId) {
        
        try {
            // 从header获取userId，如果没有则从ThreadLocal获取
            Integer finalUserId = userId;
            if (finalUserId == null) {
                Long userIdFromContext = com.android.util.UserContext.getUserId();
                if (userIdFromContext != null) {
                    finalUserId = userIdFromContext.intValue();
                }
            }
            
            if (finalUserId == null) {
                return Result.badRequest("用户ID不能为空");
            }
            
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }
            
            // 调用处理服务更新健康档案
            HealthArchive healthArchive = healthArchiveProcessService.updateHealthArchive(
                    archiveId, request, finalUserId);
            
            log.info("健康档案更新成功，档案ID：{}，用户ID：{}", 
                    healthArchive.getArchiveId(), healthArchive.getUserId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("archiveId", healthArchive.getArchiveId());
            data.put("userId", healthArchive.getUserId());
            data.put("userName", healthArchive.getUserName());
            
            return Result.success("健康档案更新成功", data);
            
        } catch (RuntimeException e) {
            log.error("更新健康档案失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("更新健康档案时发生异常", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 处理并保存健康档案
     * 在同一事务中保存健康档案主表和个人健康标识表
     *
     * @param request 健康档案请求对象
     * @param userId 用户ID（可选，如果未提供则从ThreadLocal获取）
     * @param familyMemberId 家人ID（可选，如果提供则为家人创建档案，使用家人的userId）
     * @return 保存结果
     */
    @Operation(summary = "处理并保存健康档案", description = "处理健康档案数据并在同一事务中保存到健康档案主表和个人健康标识表。如果提供familyMemberId，则为家人创建档案，使用家人的userId")
    @PostMapping("/process")
    public Result<Map<String, Object>> processHealthArchive(
            @RequestBody HealthArchiveRequest request,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer familyMemberId) {

        try {
            // 调用处理服务
            HealthArchive healthArchive = healthArchiveProcessService.processAndSaveHealthArchive(request, userId, familyMemberId);

            log.info("健康档案处理成功，档案ID：{}，用户ID：{}", healthArchive.getArchiveId(), healthArchive.getUserId());

            Map<String, Object> data = new HashMap<>();
            data.put("archiveId", healthArchive.getArchiveId());
            data.put("userId", healthArchive.getUserId());
            data.put("userName", healthArchive.getUserName());
            data.put("archiveNo", healthArchive.getArchiveNo() != null ? healthArchive.getArchiveNo() : "");
            data.put("archiveName", healthArchive.getArchiveName() != null ? healthArchive.getArchiveName() : "");
            data.put("archiveDate", healthArchive.getArchiveDate() != null ? healthArchive.getArchiveDate().toString() : "");
            data.put("archiveYear", healthArchive.getArchiveYear() != null ? healthArchive.getArchiveYear() : "");

            return Result.success("健康档案处理并保存成功", data);

        } catch (RuntimeException e) {
            log.error("处理健康档案失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("处理健康档案时发生异常", e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询该用户的所有健康档案列表
     * 包括自己创建的以及别人为自己创建的所有健康档案
     *
     * @param userId 用户ID（档案拥有者ID）
     * @param asCreator 是否作为创建者查询（可选，true=查询创建的所有档案，false/null=查询拥有的所有档案）
     * @return 健康档案列表
     */
    @Operation(summary = "查询用户健康档案列表", description = "根据用户ID查询健康档案列表。默认查询拥有的所有档案（包括自己创建的和他人为自己创建的），如果asCreator=true则查询创建的所有档案（包括自己和家人的）")
    @GetMapping("/list")
    public Result<List<HealthArchive>> getHealthArchiveList(
            @RequestParam Integer userId,
            @RequestParam(required = false) Boolean asCreator) {
        try {
            List<HealthArchive> archiveList;
            if (Boolean.TRUE.equals(asCreator)) {
                // 作为创建者查询
                archiveList = healthArchiveProcessService.getHealthArchiveListByCreatorId(userId);
                log.info("查询用户{}创建的健康档案列表成功，共{}条", userId, archiveList.size());
            } else {
                // 作为档案拥有者查询
                archiveList = healthArchiveProcessService.getHealthArchiveListByUserId(userId);
                log.info("查询用户{}拥有的健康档案列表成功，共{}条", userId, archiveList.size());
            }
            return Result.success("查询成功", archiveList);
        } catch (RuntimeException e) {
            log.error("查询健康档案列表失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询健康档案列表时发生异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据档案ID查询完整的健康档案信息（包括所有关联表）
     *
     * @param archiveId 档案ID
     * @return 完整的健康档案信息
     */
    @Operation(summary = "查询健康档案详情", description = "根据档案ID查询完整的健康档案信息，包括主表、个人健康标识、用户基本信息、紧急联系人、健康服务凭证")
    @GetMapping("/detail/{archiveId}")
    public Result<Map<String, Object>> getHealthArchiveDetail(@PathVariable Integer archiveId) {
        try {
            Map<String, Object> detail = healthArchiveProcessService.getHealthArchiveDetailById(archiveId);
            log.info("查询健康档案详情成功，档案ID：{}", archiveId);
            return Result.success("查询成功", detail);
        } catch (RuntimeException e) {
            log.error("查询健康档案详情失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询健康档案详情时发生异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 导出健康档案为PDF
     *
     * @param archiveId 档案ID
     * @return PDF文件流
     */
    @Operation(summary = "导出健康档案PDF", description = "根据档案ID导出健康档案为PDF文件")
    @GetMapping("/export/pdf/{archiveId}")
    public ResponseEntity<ByteArrayResource> exportHealthArchivePdf(@PathVariable Integer archiveId) {
        try {
            if (archiveId == null) {
                throw new RuntimeException("档案ID不能为空");
            }

            log.info("开始导出健康档案PDF，档案ID：{}", archiveId);

            // 获取健康档案详情
            Map<String, Object> archiveDetail = healthArchiveProcessService.getHealthArchiveDetailById(archiveId);

            // 生成PDF
            byte[] pdfBytes = healthArchiveProcessService.generateHealthArchivePdf(archiveDetail);

            // 获取档案名称作为文件名
            String archiveName = "健康档案";
            Object healthArchiveObj = archiveDetail.get("healthArchive");
            if (healthArchiveObj != null) {
                // 支持实体对象和Map对象
                Map<String, Object> healthArchive;
                if (healthArchiveObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tempMap = (Map<String, Object>) healthArchiveObj;
                    healthArchive = tempMap;
                } else {
                    // 如果是实体对象，使用ObjectMapper转换
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tempMap = mapper.convertValue(healthArchiveObj, Map.class);
                        healthArchive = tempMap;
                    } catch (Exception e) {
                        log.warn("转换健康档案对象失败", e);
                        healthArchive = null;
                    }
                }
                if (healthArchive != null) {
                    if (healthArchive.get("archiveName") != null) {
                        archiveName = healthArchive.get("archiveName").toString();
                    } else if (healthArchive.get("userName") != null) {
                        archiveName = healthArchive.get("userName").toString() + "的健康档案";
                    }
                }
            }

            String fileName = archiveName + ".pdf";
            // 处理文件名编码，确保中文文件名正确显示
            fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            log.info("健康档案PDF导出成功，档案ID：{}，文件大小：{} bytes", archiveId, pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (RuntimeException e) {
            log.error("导出健康档案PDF失败", e);
            throw e;
        } catch (Exception e) {
            log.error("导出健康档案PDF时发生异常", e);
            throw new RuntimeException("导出PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询登录用户最新健康档案中的慢病列表
     *
     * @param userId 用户ID（可选，如果不提供则从UserContext获取）
     * @return 慢病列表
     */
    @Operation(summary = "查询用户慢病列表", description = "查询登录用户最新健康档案中的慢病列表")
    @GetMapping("/chronic-disease/list")
    public Result<List<String>> getChronicDiseaseList(
            @RequestParam(required = false) Integer userId) {
        try {
            List<String> chronicDiseaseList = healthArchiveProcessService.getChronicDiseaseList(userId);
            log.info("查询用户慢病列表成功，共{}条", chronicDiseaseList.size());
            return Result.success("查询成功", chronicDiseaseList);
        } catch (RuntimeException e) {
            log.error("查询慢病列表失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询慢病列表时发生异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据健康档案ID更新健康档案主表信息
     *
     * @param archiveId 档案ID
     * @param healthArchive 健康档案对象
     * @return 更新结果
     */
    @Operation(summary = "根据档案ID更新健康档案主表", description = "根据健康档案ID更新健康档案主表信息")
    @PutMapping("/archive/{archiveId}")
    public Result<HealthArchive> updateArchiveById(
            @PathVariable Integer archiveId,
            @RequestBody HealthArchive healthArchive) {
        try {
            if (archiveId == null) {
                return Result.badRequest("档案ID不能为空");
            }
            
            healthArchive.setArchiveId(archiveId);
            boolean result = healthArchiveProcessService.updateArchiveById(healthArchive);
            
            if (result) {
                log.info("更新健康档案主表成功，档案ID：{}", archiveId);
                return Result.success("更新成功", healthArchive);
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新健康档案主表失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取健康档案统计数据
     * 包括：总记录数（我的+我管理的家人的）、体检报告数、监测数据数
     *
     * @param userId 用户ID（从header获取）
     * @return 统计数据
     */
    @Operation(summary = "获取健康档案统计数据", description = "获取健康档案统计数据：总记录数、体检报告数、监测数据数")
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getHealthArchiveStats(
            @RequestHeader(value = "userId", required = false) Integer userId) {
        try {
            // 从header获取userId，如果没有则从ThreadLocal获取
            Integer finalUserId = userId;
            if (finalUserId == null) {
                Long userIdFromContext = com.android.util.UserContext.getUserId();
                if (userIdFromContext != null) {
                    finalUserId = userIdFromContext.intValue();
                }
            }

            if (finalUserId == null) {
                return Result.badRequest("用户ID不能为空");
            }

            Map<String, Integer> stats = healthArchiveProcessService.getHealthArchiveStats(finalUserId);
            log.info("获取健康档案统计数据成功，用户ID：{}，统计结果：{}", finalUserId, stats);
            return Result.success("查询成功", stats);
        } catch (RuntimeException e) {
            log.error("获取健康档案统计数据失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取健康档案统计数据时发生异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 生成最新健康档案
     * 1. 查询用户最新的健康档案
     * 2. 复制最新档案的基本信息（用户基本信息）
     * 3. 创建新的健康档案（使用当前年份）
     * 4. 返回新创建的档案（前端可调用智能体接口生成其他信息）
     *
     * @param userId 用户ID（从header获取）
     * @return 新创建的健康档案
     */
    @Operation(summary = "生成最新健康档案", description = "生成用户的最新健康档案，复制最新档案的基本信息，创建新档案")
    @PostMapping("/generate-latest")
    public Result<Map<String, Object>> generateLatestHealthArchive(
            @RequestHeader(value = "userId", required = false) Integer userId) {
        try {
            // 从header获取userId，如果没有则从ThreadLocal获取
            Integer finalUserId = userId;
            if (finalUserId == null) {
                Long userIdFromContext = com.android.util.UserContext.getUserId();
                if (userIdFromContext != null) {
                    finalUserId = userIdFromContext.intValue();
                }
            }

            if (finalUserId == null) {
                return Result.badRequest("用户ID不能为空");
            }

            HealthArchive newArchive = healthArchiveProcessService.generateLatestHealthArchive(finalUserId);

            Map<String, Object> data = new HashMap<>();
            data.put("archiveId", newArchive.getArchiveId());
            data.put("userId", newArchive.getUserId());
            data.put("userName", newArchive.getUserName());
            data.put("archiveNo", newArchive.getArchiveNo());
            data.put("archiveName", newArchive.getArchiveName());
            data.put("archiveDate", newArchive.getArchiveDate() != null ? newArchive.getArchiveDate().toString() : "");
            data.put("archiveYear", newArchive.getArchiveYear());

            log.info("生成最新健康档案成功，档案ID：{}，用户ID：{}，档案名称：{}", 
                    newArchive.getArchiveId(), finalUserId, newArchive.getArchiveName());

            return Result.success("健康档案生成成功", data);
        } catch (RuntimeException e) {
            log.error("生成最新健康档案失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("生成最新健康档案时发生异常", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }
}
