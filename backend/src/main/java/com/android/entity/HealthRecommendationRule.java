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
import java.time.LocalDateTime;

/**
 * <p>
 * 健康建议规则表（规则引擎配置）
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_recommendation_rule")
@ApiModel(value="HealthRecommendationRule对象", description="健康建议规则表（规则引擎配置）")
public class HealthRecommendationRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "规则ID")
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    @ApiModelProperty(value = "维度代码（如METABOLIC）")
    private String dimensionCode;

    @ApiModelProperty(value = "风险等级：低、中、高、极高")
    private String riskLevel;

    @ApiModelProperty(value = "指标代码（可选，精确匹配）")
    private String indicatorCode;

    @ApiModelProperty(value = "建议类型：LIFESTYLE-生活方式, DIET-饮食, EXERCISE-运动, MEDICAL-医疗关注, FOLLOW_UP-随访, SERVICE-服务")
    private String recommendationType;

    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    @ApiModelProperty(value = "内容模板（支持变量替换）")
    private String contentTemplate;

    @ApiModelProperty(value = "优先级：低、中、高")
    private String priority;

    @ApiModelProperty(value = "是否启用")
    private Boolean isActive;

    @ApiModelProperty(value = "排序号")
    private Integer orderNum;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
