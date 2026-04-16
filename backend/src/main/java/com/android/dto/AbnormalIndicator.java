package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 异常指标
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "异常指标")
public class AbnormalIndicator implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "指标代码")
    @JsonProperty("indicatorCode")
    private String indicatorCode;

    @Schema(description = "指标名称")
    @JsonProperty("indicatorName")
    private String indicatorName;

    @Schema(description = "指标值")
    @JsonProperty("value")
    private String value;

    @Schema(description = "参考范围")
    @JsonProperty("reference")
    private String reference;

    @Schema(description = "严重程度")
    @JsonProperty("severity")
    private String severity;

    @Schema(description = "关联维度")
    @JsonProperty("relatedDimension")
    private String relatedDimension;
}
