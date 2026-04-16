package com.android.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * AI健康建议表（用户行动层）
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_recommendation")
@ApiModel(value="HealthRecommendation对象", description="AI健康建议表（用户行动层）")
public class HealthRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "建议ID")
    @TableId(value = "recommendation_id", type = IdType.AUTO)
    private Long recommendationId;

    @ApiModelProperty(value = "分析ID")
    private Long analysisId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "建议类型：LIFESTYLE-生活方式, DIET-饮食, EXERCISE-运动, MEDICAL-医疗关注, FOLLOW_UP-随访, SERVICE-服务")
    private String recommendationType;

    @ApiModelProperty(value = "建议标题")
    private String title;

    @ApiModelProperty(value = "建议内容")
    private String content;

    @ApiModelProperty(value = "优先级：低、中、高")
    private String priority;

    @ApiModelProperty(value = "是否生效")
    private Boolean isActive;

    @ApiModelProperty(value = "生效日期")
    private LocalDate validFrom;

    @ApiModelProperty(value = "失效日期")
    private LocalDate validTo;

    @ApiModelProperty(value = "关联的维度代码（如METABOLIC）")
    private String dimensionCode;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
