package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * AI分析结果提交请求DTO（简化版，智能体只需传递分析结果和建议）
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "AI分析结果提交请求DTO（简化版）")
public class AiAnalysisSubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "档案ID（必填）")
    @JsonProperty("archiveId")
    private Integer archiveId;

    @Schema(description = "原始数据ID（触发分析的体检报告ID，必填）")
    @JsonProperty("sourceRawId")
    private Long sourceRawId;

    @Schema(description = "模型名称（必填）")
    @JsonProperty("modelName")
    private String modelName;

    @Schema(description = "模型版本（必填）")
    @JsonProperty("modelVersion")
    private String modelVersion;

    @Schema(description = "AI分析结果数据（必填）")
    @JsonProperty("analysisResult")
    private AnalysisResultData analysisResult;

    @Schema(description = "AI生成的健康建议列表（可选，如果智能体生成建议失败，系统会使用规则引擎生成）")
    @JsonProperty("recommendations")
    private List<AiRecommendationData> recommendations;
}
