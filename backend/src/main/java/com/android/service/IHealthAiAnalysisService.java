package com.android.service;

import com.android.dto.AiAnalysisDataResponse;
import com.android.dto.AiAnalysisRequest;
import com.android.entity.HealthAiAnalysis;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI健康分析结果表 服务类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
public interface IHealthAiAnalysisService extends IService<HealthAiAnalysis> {

    /**
     * 保存AI分析结果并生成建议
     * @param request AI分析请求
     * @return 分析结果
     */
    HealthAiAnalysis saveAnalysisAndGenerateRecommendations(AiAnalysisRequest request);

    /**
     * 根据档案ID查询分析结果
     * @param archiveId 档案ID
     * @return 分析结果列表
     */
    List<HealthAiAnalysis> getAnalysisByArchiveId(Integer archiveId);

    /**
     * 根据用户ID查询分析结果
     * @param userId 用户ID
     * @return 分析结果列表
     */
    List<HealthAiAnalysis> getAnalysisByUserId(Long userId);

    /**
     * 获取用户最新的健康分析（供 medical-ai 智能体获取，用于回答健康建议类问题）
     * @param userId 用户ID
     * @return 含 archiveId、sourceRawId、analysisResult 的 Map，无数据时返回 null
     */
    Map<String, Object> getLatestAnalysisForUser(Long userId);

    /**
     * 根据用户ID获取完整数据（供智能体分析使用）
     * 包括：该用户的所有原始数据 + 所有健康档案及其从表数据
     * @param userId 用户ID
     * @return 完整数据响应
     */
    AiAnalysisDataResponse getUserCompleteData(Long userId);
}
