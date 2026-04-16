package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI分析结果数据结构
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "AI分析结果数据结构")
public class AnalysisResultData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分析元信息")
    @JsonProperty("analysisMeta")
    private Map<String, Object> analysisMeta;

    @Schema(description = "整体评估")
    @JsonProperty("overallAssessment")
    private OverallAssessment overallAssessment;

    @Schema(description = "维度分析列表")
    @JsonProperty("dimensionAnalysis")
    private List<DimensionAnalysis> dimensionAnalysis;

    @Schema(description = "异常指标列表")
    @JsonProperty("abnormalIndicators")
    private List<AbnormalIndicator> abnormalIndicators;

    @Schema(description = "模型置信度")
    @JsonProperty("modelConfidence")
    private ModelConfidence modelConfidence;

    @Schema(description = "扩展数据：包含生活方式状态、系统症状筛查、心理评估、社会背景、体格检查等表的数据（可选，智能体分析后以映射方式提供）")
    @JsonProperty("extendedData")
    private Map<String, Object> extendedData;
}
