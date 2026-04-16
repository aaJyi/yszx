package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * AI分析请求DTO（智能体开发程序员调用时传入）
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "AI分析请求DTO")
public class AiAnalysisRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", required = true)
    @JsonProperty("userId")
    private Long userId;

    @Schema(description = "档案ID", required = true)
    @JsonProperty("archiveId")
    private Integer archiveId;

    @Schema(description = "原始数据ID（触发分析的体检报告ID）", required = true)
    @JsonProperty("sourceRawId")
    private Long sourceRawId;

    @Schema(description = "模型名称", required = true)
    @JsonProperty("modelName")
    private String modelName;

    @Schema(description = "模型版本", required = true)
    @JsonProperty("modelVersion")
    private String modelVersion;

    @Schema(description = "AI分析结果数据")
    @JsonProperty("analysisResult")
    private AnalysisResultData analysisResult;

    @Schema(description = "AI生成的健康建议列表（可选，如果智能体生成建议失败，系统会使用规则引擎生成）")
    @JsonProperty("recommendations")
    private java.util.List<AiRecommendationData> recommendations;
}
