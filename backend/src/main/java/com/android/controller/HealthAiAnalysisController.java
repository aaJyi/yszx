package com.android.controller;

import com.android.common.Result;
import com.android.dto.AiAnalysisDataResponse;
import com.android.dto.AiAnalysisRequest;
import com.android.dto.AiAnalysisSubmitRequest;
import com.android.entity.HealthAiAnalysis;
import com.android.entity.HealthRecommendation;
import com.android.service.IHealthAiAnalysisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.android.mapper.HealthRecommendationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * AI健康分析控制器
 * 供智能体开发程序员调用
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Slf4j
@RestController
@RequestMapping("/health-ai-analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "AI健康分析", description = "AI健康分析相关接口（供智能体调用）")
public class HealthAiAnalysisController {

    private final IHealthAiAnalysisService healthAiAnalysisService;
    private final HealthRecommendationMapper recommendationMapper;

    /**
     * 获取用户完整数据接口（智能体开发程序员调用）
     * 
     * 智能体调用此接口获取用户的完整数据：
     * 1. 该用户的所有原始健康数据（raw_health_data）
     * 2. 该用户的所有健康档案及其所有从表数据
     * 
     * 智能体可以根据这些数据进行分析
     *
     * @param userId 用户ID（路径参数）
     * @return 用户的完整数据
     */
    @Operation(summary = "获取用户完整数据", description = "智能体调用此接口获取用户的完整数据（原始数据+健康档案）")
    @GetMapping("/data/user/{userId}")
    public Result<AiAnalysisDataResponse> getUserCompleteData(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return Result.badRequest("用户ID不能为空");
            }

            log.info("智能体请求获取用户{}的完整数据", userId);
            AiAnalysisDataResponse response = healthAiAnalysisService.getUserCompleteData(userId);

            log.info("成功获取用户{}的完整数据，原始数据{}条，健康档案{}个", 
                    userId, 
                    response.getRawHealthDataList() != null ? response.getRawHealthDataList().size() : 0,
                    response.getHealthArchives() != null ? response.getHealthArchives().size() : 0);

            return Result.success("获取用户完整数据成功", response);

        } catch (RuntimeException e) {
            log.error("获取用户完整数据失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取用户完整数据时发生异常", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * AI分析结果提交接口（智能体开发程序员调用）
     * 
     * 智能体开发程序员在完成AI分析后，调用此接口提交分析结果
     * 系统处理逻辑：
     * 1. 从路径参数获取userId（无需在JSON中传递）
     * 2. 保存分析结果到 health_ai_analysis 表
     * 3. 优先保存智能体提交的健康建议（如果提供了recommendations字段）
     * 4. 如果智能体没有提交建议或保存失败，则使用规则引擎生成建议
     *
     * @param userId 用户ID（路径参数，从当前登录用户获取或传入）
     * @param request AI分析请求（包含分析结果和可选的建议列表，无需包含userId）
     * @return 保存结果
     */
    @Operation(summary = "提交AI分析结果", description = "智能体开发程序员调用此接口提交AI分析结果和建议，userId从路径参数获取，系统优先使用智能体建议，失败时使用规则引擎生成")
    @PostMapping("/submit/user/{userId}")
    public Result<Map<String, Object>> submitAnalysis(
            @PathVariable Long userId,
            @RequestBody AiAnalysisSubmitRequest request) {
        try {
            log.info("收到AI分析结果提交，用户ID：{}，档案ID：{}，模型：{}-{}", 
                    userId, request.getArchiveId(), 
                    request.getModelName(), request.getModelVersion());

            // 验证必填字段
            if (userId == null) {
                return Result.badRequest("用户ID不能为空");
            }
            if (request.getArchiveId() == null) {
                return Result.badRequest("档案ID不能为空");
            }
            if (request.getSourceRawId() == null) {
                return Result.badRequest("原始数据ID不能为空");
            }
            if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
                return Result.badRequest("模型名称不能为空");
            }
            if (request.getAnalysisResult() == null) {
                return Result.badRequest("分析结果不能为空");
            }

            // 构建完整的请求对象
            // userId 从路径参数获取，将保存到 health_ai_analysis 表和 health_recommendation 表中
            AiAnalysisRequest fullRequest = new AiAnalysisRequest();
            fullRequest.setUserId(userId); // 使用路径参数中的userId，将保存到数据库
            fullRequest.setArchiveId(request.getArchiveId());
            fullRequest.setSourceRawId(request.getSourceRawId());
            fullRequest.setModelName(request.getModelName());
            fullRequest.setModelVersion(request.getModelVersion());
            fullRequest.setAnalysisResult(request.getAnalysisResult());
            fullRequest.setRecommendations(request.getRecommendations());
            
            log.info("准备保存AI分析结果，用户ID：{}（来自路径参数），档案ID：{}，原始数据ID：{}", 
                    userId, request.getArchiveId(), request.getSourceRawId());

            // 保存分析结果并生成建议
            HealthAiAnalysis analysis = healthAiAnalysisService.saveAnalysisAndGenerateRecommendations(fullRequest);

            // 查询生成的建议
            LambdaQueryWrapper<HealthRecommendation> recWrapper = new LambdaQueryWrapper<>();
            recWrapper.eq(HealthRecommendation::getAnalysisId, analysis.getAnalysisId())
                    .eq(HealthRecommendation::getIsActive, true)
                    .orderByDesc(HealthRecommendation::getPriority);

            List<HealthRecommendation> recommendations = recommendationMapper.selectList(recWrapper);

            Map<String, Object> data = new HashMap<>();
            data.put("analysisId", analysis.getAnalysisId());
            data.put("userId", analysis.getUserId());
            data.put("archiveId", analysis.getArchiveId());
            data.put("overallHealthScore", analysis.getOverallHealthScore());
            data.put("riskLevel", analysis.getRiskLevel());
            data.put("recommendationCount", recommendations.size());
            data.put("recommendations", recommendations);

            String message = "AI分析结果提交成功";
            if (request.getRecommendations() != null && !request.getRecommendations().isEmpty()) {
                message += "，已保存智能体生成的健康建议";
            } else {
                message += "，已通过规则引擎生成健康建议";
            }

            log.info("AI分析结果提交成功，分析ID：{}，生成{}条建议", analysis.getAnalysisId(), recommendations.size());

            return Result.success(message, data);

        } catch (RuntimeException e) {
            log.error("提交AI分析结果失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("提交AI分析结果时发生异常", e);
            return Result.error("提交失败: " + e.getMessage());
        }
    }

    /**
     * 根据档案ID查询分析结果
     *
     * @param archiveId 档案ID
     * @return 分析结果列表
     */
    @Operation(summary = "查询档案的分析结果", description = "根据档案ID查询该档案的所有AI分析结果")
    @GetMapping("/archive/{archiveId}")
    public Result<List<HealthAiAnalysis>> getAnalysisByArchiveId(@PathVariable Integer archiveId) {
        try {
            List<HealthAiAnalysis> analysisList = healthAiAnalysisService.getAnalysisByArchiveId(archiveId);
            log.info("查询档案{}的分析结果成功，共{}条", archiveId, analysisList.size());
            return Result.success("查询成功", analysisList);
        } catch (Exception e) {
            log.error("查询分析结果失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户最新的健康分析结果（供 medical-ai 智能体聊天时获取，用于回答健康建议类问题）
     *
     * @param userId 用户ID
     * @return 最新分析结果（含 archiveId、sourceRawId、analysisResult）
     */
    @Operation(summary = "获取用户最新分析结果", description = "供智能体获取用户最新健康分析，用于回答健康建议类提问")
    @GetMapping("/user/{userId}/latest")
    public Result<Map<String, Object>> getLatestAnalysisByUserId(@PathVariable Long userId) {
        try {
            Map<String, Object> latest = healthAiAnalysisService.getLatestAnalysisForUser(userId);
            if (latest == null) {
                return Result.error(404, "用户暂无健康分析结果");
            }
            log.info("查询用户{}的最新分析结果成功", userId);
            return Result.success("查询成功", latest);
        } catch (Exception e) {
            log.error("查询最新分析结果失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询分析结果
     *
     * @param userId 用户ID
     * @return 分析结果列表
     */
    @Operation(summary = "查询用户的分析结果", description = "根据用户ID查询该用户的所有AI分析结果")
    @GetMapping("/user/{userId}")
    public Result<List<HealthAiAnalysis>> getAnalysisByUserId(@PathVariable Long userId) {
        try {
            List<HealthAiAnalysis> analysisList = healthAiAnalysisService.getAnalysisByUserId(userId);
            log.info("查询用户{}的分析结果成功，共{}条", userId, analysisList.size());
            return Result.success("查询成功", analysisList);
        } catch (Exception e) {
            log.error("查询分析结果失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户最新 N 条健康建议（供首页三条建议框展示）
     * 智能体在健康档案生成后自动分析并提交建议，本接口取最新建议按创建时间倒序取 limit 条。
     *
     * @param userId 用户ID
     * @param limit  条数，默认 3
     * @return 建议列表（仅含 title、content 等展示字段）
     */
    @Operation(summary = "获取用户最新健康建议", description = "供首页展示，取用户最新 N 条健康建议（智能体分析后生成）")
    @GetMapping("/user/{userId}/recommendations/latest")
    public Result<List<Map<String, Object>>> getLatestRecommendationsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") Integer limit) {
        try {
            if (userId == null) {
                return Result.badRequest("用户ID不能为空");
            }
            LambdaQueryWrapper<HealthRecommendation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthRecommendation::getUserId, userId)
                    .eq(HealthRecommendation::getIsActive, true)
                    .orderByDesc(HealthRecommendation::getCreateTime)
                    .last("LIMIT " + Math.min(limit != null && limit > 0 ? limit : 3, 20));

            List<HealthRecommendation> list = recommendationMapper.selectList(wrapper);
            List<Map<String, Object>> result = list.stream().map(rec -> {
                Map<String, Object> m = new HashMap<>();
                m.put("title", rec.getTitle());
                m.put("content", rec.getContent());
                m.put("recommendationType", rec.getRecommendationType());
                return m;
            }).collect(Collectors.toList());
            log.info("查询用户{}最新健康建议成功，共{}条", userId, result.size());
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询用户最新健康建议失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据分析ID查询建议列表
     *
     * @param analysisId 分析ID
     * @return 建议列表
     */
    @Operation(summary = "查询分析的建议列表", description = "根据分析ID查询该分析生成的所有健康建议")
    @GetMapping("/{analysisId}/recommendations")
    public Result<List<HealthRecommendation>> getRecommendationsByAnalysisId(@PathVariable Long analysisId) {
        try {
            LambdaQueryWrapper<HealthRecommendation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthRecommendation::getAnalysisId, analysisId)
                    .eq(HealthRecommendation::getIsActive, true)
                    .orderByDesc(HealthRecommendation::getPriority)
                    .orderByAsc(HealthRecommendation::getCreateTime);

            List<HealthRecommendation> recommendations = recommendationMapper.selectList(wrapper);
            log.info("查询分析{}的建议列表成功，共{}条", analysisId, recommendations.size());
            return Result.success("查询成功", recommendations);
        } catch (Exception e) {
            log.error("查询建议列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
 