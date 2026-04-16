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
 * 健康档案用户基本信息表
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_user_info")
@ApiModel(value="HealthUserInfo对象", description="健康档案用户基本信息表")
public class HealthUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "信息ID")
    @TableId(value = "info_id", type = IdType.AUTO)
    private Integer infoId;

    @ApiModelProperty(value = "档案ID（关联健康档案主表）")
    private Integer archiveId;

    @ApiModelProperty(value = "姓名")
    private String fullName;

    @ApiModelProperty(value = "性别")
    private String gender;

    @ApiModelProperty(value = "出生日期")
    private LocalDate birthDate;

    @ApiModelProperty(value = "证件类型")
    private String idType;

    @ApiModelProperty(value = "证件号码")
    private String idNumber;

    @ApiModelProperty(value = "工作单位/学校")
    private String workSchool;

    @ApiModelProperty(value = "籍贯")
    private String nativePlace;

    @ApiModelProperty(value = "出生地")
    private String birthPlace;

    @ApiModelProperty(value = "民族（用户输入）")
    private String ethnicity;

    @ApiModelProperty(value = "家庭医生签约（是否签约）")
    private Boolean familyDoctorSigned;

    @ApiModelProperty(value = "家庭医生姓名")
    private String familyDoctorName;

    @ApiModelProperty(value = "家庭医生电话")
    private String familyDoctorPhone;

    @ApiModelProperty(value = "本人电话")
    private String personalPhone;

    @ApiModelProperty(value = "常住类型")
    private String residenceType;

    @ApiModelProperty(value = "户籍地址/常住地址")
    private String residenceAddress;

    @ApiModelProperty(value = "文化程度")
    private String educationLevel;

    @ApiModelProperty(value = "职业")
    private String occupation;

    @ApiModelProperty(value = "婚姻状况")
    private String maritalStatus;

    @ApiModelProperty(value = "医疗费用支付方式")
    private String paymentMethod;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
