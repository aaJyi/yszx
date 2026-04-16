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
 * 过敏史表
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_allergy_history")
@ApiModel(value="HealthAllergyHistory对象", description="过敏史表")
public class HealthAllergyHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "过敏ID")
    @TableId(value = "allergy_id", type = IdType.AUTO)
    private Integer allergyId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "过敏类型")
    private String allergyType;

    @ApiModelProperty(value = "药物过敏详情")
    private String drugAllergyDetails;

    @ApiModelProperty(value = "食物过敏详情")
    private String foodAllergyDetails;

    @ApiModelProperty(value = "其他过敏详情")
    private String otherAllergyDetails;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
