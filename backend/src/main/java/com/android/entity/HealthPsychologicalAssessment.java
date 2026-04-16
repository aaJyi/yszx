package com.android.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 健康档案-心理与社会心理评估表
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_psychological_assessment")
@ApiModel(value="HealthPsychologicalAssessment对象", description="健康档案-心理与社会心理评估表")
public class HealthPsychologicalAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "心理评估ID")
    @TableId(value = "assessment_id", type = IdType.AUTO)
    private Integer assessmentId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "疲劳、压抑")
    private String fatigueDepression;

    @ApiModelProperty(value = "记忆力减退")
    private String memoryDecline;

    @ApiModelProperty(value = "适应能力减退")
    private String adaptabilityDecline;

    @ApiModelProperty(value = "活力、反应能力减退")
    private String vitalityResponseDecline;

    @ApiModelProperty(value = "情绪状态")
    private String emotionStatus;

    @ApiModelProperty(value = "是否存在压力")
    private String stressStatus;

    @ApiModelProperty(value = "压力来源（可多选）")
    private String stressSource;

    @ApiModelProperty(value = "缓压方法")
    private String stressReliefMethods;

    @ApiModelProperty(value = "对自我的看法")
    private String selfPerception;

    @ApiModelProperty(value = "对疾病的认识程度")
    private String diseaseCognition;

    @ApiModelProperty(value = "过去1年内是否有重要生活事件")
    private String majorLifeEvent;

    @ApiModelProperty(value = "重要生活事件描述")
    private String majorLifeEventDesc;

    @ApiModelProperty(value = "遇到困难最愿倾诉对象")
    private String preferredConfidant;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
