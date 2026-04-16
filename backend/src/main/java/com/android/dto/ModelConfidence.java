package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 模型置信度
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "模型置信度")
public class ModelConfidence implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "整体置信度 0-1")
    @JsonProperty("overallConfidence")
    private Double overallConfidence;

    @Schema(description = "局限性说明")
    @JsonProperty("limitations")
    private List<String> limitations;
}
