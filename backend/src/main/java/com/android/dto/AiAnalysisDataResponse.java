package com.android.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 智能体获取用户完整数据的响应DTO
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@Schema(description = "智能体获取用户完整数据的响应DTO")
public class AiAnalysisDataResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @JsonProperty("userId")
    private Long userId;

    @Schema(description = "用户的原始健康数据列表（包括体检报告等）")
    @JsonProperty("rawHealthDataList")
    private List<Map<String, Object>> rawHealthDataList;

    @Schema(description = "用户的健康档案列表（每个档案包含所有从表数据）")
    @JsonProperty("healthArchives")
    private List<Map<String, Object>> healthArchives;
}
