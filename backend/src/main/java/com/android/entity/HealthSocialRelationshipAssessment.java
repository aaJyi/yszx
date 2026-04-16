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
 * 健康档案-社会关系与社会支持评估表
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_social_relationship_assessment")
@ApiModel(value="HealthSocialRelationshipAssessment对象", description="健康档案-社会关系与社会支持评估表")
public class HealthSocialRelationshipAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "社会关系评估ID")
    @TableId(value = "assessment_id", type = IdType.AUTO)
    private Integer assessmentId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "家庭关系")
    private String familyRelationship;

    @ApiModelProperty(value = "婚姻状况")
    private String maritalStatus;

    @ApiModelProperty(value = "居住情况")
    private String livingCondition;

    @ApiModelProperty(value = "职业性质")
    private String occupationType;

    @ApiModelProperty(value = "文化程度")
    private String educationLevel;

    @ApiModelProperty(value = "社会交往情况")
    private String socialInteraction;

    @ApiModelProperty(value = "医疗费用支付形式")
    private String medicalPaymentMethod;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
