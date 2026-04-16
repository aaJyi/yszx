package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 个人健康标识表
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_profile_tags")
public class HealthProfileTags implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签ID（主键）
     */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Integer tagId;

    /**
     * 档案ID（外键关联到健康档案表）
     */
    @TableField("archive_id")
    private Integer archiveId;

    /**
     * 是否0-6岁儿童
     */
    @TableField("is_child_0_6")
    private Boolean isChild06;

    /**
     * 是否65岁以上
     */
    @TableField("is_elderly_65")
    private Boolean isElderly65;

    /**
     * 是否孕产妇
     */
    @TableField("is_pregnant")
    private Boolean isPregnant;

    /**
     * 风险等级：低风险、一般风险、较高风险、高风险
     */
    @TableField("pregnancy_risk")
    private String pregnancyRisk;

    /**
     * 慢性病/重点疾病（JSON格式）
     * 示例：["高血压", "2型糖尿病", "脑卒中"]
     */
    @TableField("chronic_disease")
    private String chronicDisease;

    /**
     * 法定传染病（JSON格式）
     * 示例：["肺结核", "肝炎"]
     */
    @TableField("statutory_info")
    private String statutoryInfo;

    /**
     * 体重状况：低、正常、超重、肥胖
     */
    @TableField("weight_status")
    private String weightStatus;

    /**
     * 血型：A、B、O、AB、不详
     */
    @TableField("blood_type")
    private String bloodType;
}
