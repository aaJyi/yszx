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
 * 健康档案健康服务凭证表
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_user_certificates")
@ApiModel(value="HealthUserCertificates对象", description="健康档案健康服务凭证表")
public class HealthUserCertificates implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "凭证ID")
    @TableId(value = "certificate_id", type = IdType.AUTO)
    private Integer certificateId;

    @ApiModelProperty(value = "档案ID（关联health_user_info.archive_id）")
    private Integer archiveId;

    @ApiModelProperty(value = "凭证类型（如：社保卡、医保卡、电子健康卡等）")
    private String certificateType;

    @ApiModelProperty(value = "凭证签发机构")
    private String certificateIssuer;

    @ApiModelProperty(value = "凭证签发时间")
    private LocalDate issueDate;

    @ApiModelProperty(value = "凭证有效期（可为空，表示永久有效）")
    private LocalDate expiryDate;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
