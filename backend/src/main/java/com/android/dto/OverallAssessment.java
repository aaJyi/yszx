package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 整体评估
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "整体评估")
public class OverallAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "健康评分 0-100")
    @JsonProperty("overallHealthScore")
    private Integer overallHealthScore;

    @Schema(description = "风险等级：低、中、高、极高")
    @JsonProperty("riskLevel")
    private String riskLevel;

    @Schema(description = "关键风险列表")
    @JsonProperty("keyRisks")
    private List<String> keyRisks;

    @Schema(description = "摘要")
    @JsonProperty("summary")
    private String summary;
}
