package com.android.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
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
 * 家人信息表
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("family_member")
@ApiModel(value="FamilyMember对象", description="家人信息表")
public class FamilyMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "家人ID（主键）")
    @TableId(value = "member_id", type = IdType.AUTO)
    private Integer memberId;

    @ApiModelProperty(value = "创建者用户ID（谁添加的家人）")
    private Integer ownerUserId;

    @ApiModelProperty(value = "家人姓名")
    private String fullName;

    @ApiModelProperty(value = "手机号（唯一标识，用于匹配）")
    private String phone;

    @ApiModelProperty(value = "身份证号（可选）")
    private String idNumber;

    @ApiModelProperty(value = "出生日期")
    private LocalDate birthDate;

    @ApiModelProperty(value = "性别：0-未知，1-男，2-女")
    private Boolean gender;

    @ApiModelProperty(value = "关系：父亲、母亲、配偶、子女等")
    private String relation;

    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "是否已注册：0-未注册，1-已注册")
    private Boolean isRegistered;

    @ApiModelProperty(value = "如果已注册，对应的用户ID")
    private Integer registeredUserId;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
