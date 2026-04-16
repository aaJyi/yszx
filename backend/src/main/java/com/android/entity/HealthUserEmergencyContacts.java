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
 * 健康档案用户紧急联系人表
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_user_emergency_contacts")
@ApiModel(value="HealthUserEmergencyContacts对象", description="健康档案用户紧急联系人表")
public class HealthUserEmergencyContacts implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "联系人ID")
    @TableId(value = "contact_id", type = IdType.AUTO)
    private Integer contactId;

    @ApiModelProperty(value = "档案ID（关联health_user_info.archive_id）")
    private Integer archiveId;

    @ApiModelProperty(value = "紧急联系人姓名")
    private String contactName;

    @ApiModelProperty(value = "与本人关系（如：配偶、子女、父母、朋友等）")
    private String relationship;

    @ApiModelProperty(value = "电话")
    private String phoneNumber;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
