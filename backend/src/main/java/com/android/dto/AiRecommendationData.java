package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * AI生成的健康建议数据（智能体提交）
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "AI生成的健康建议数据")
public class AiRecommendationData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "建议类型：LIFESTYLE-生活方式, DIET-饮食, EXERCISE-运动, MEDICAL-医疗关注, FOLLOW_UP-随访, SERVICE-服务")
    @JsonProperty("recommendationType")
    private String recommendationType;

    @Schema(description = "建议标题")
    @JsonProperty("title")
    private String title;

    @Schema(description = "建议内容（较长的详细建议）")
    @JsonProperty("content")
    private String content;

    @Schema(description = "优先级：低、中、高")
    @JsonProperty("priority")
    private String priority;

    @Schema(description = "关联的维度代码（如METABOLIC）")
    @JsonProperty("dimensionCode")
    private String dimensionCode;
}
