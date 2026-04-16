package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 证据项
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "证据项")
public class EvidenceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "指标名称")
    @JsonProperty("indicator")
    private String indicator;

    @Schema(description = "指标值")
    @JsonProperty("value")
    private String value;

    @Schema(description = "参考范围")
    @JsonProperty("reference")
    private String reference;
}
