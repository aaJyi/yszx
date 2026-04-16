package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 维度分析
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "维度分析")
public class DimensionAnalysis implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "维度代码（如METABOLIC）")
    @JsonProperty("dimensionCode")
    private String dimensionCode;

    @Schema(description = "维度名称")
    @JsonProperty("dimensionName")
    private String dimensionName;

    @Schema(description = "风险等级")
    @JsonProperty("riskLevel")
    private String riskLevel;

    @Schema(description = "证据列表")
    @JsonProperty("evidence")
    private List<EvidenceItem> evidence;

    @Schema(description = "解释说明")
    @JsonProperty("interpretation")
    private String interpretation;

    @Schema(description = "置信度 0-1")
    @JsonProperty("confidence")
    private Double confidence;
}
