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
 * 家族史表
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_family_history")
@ApiModel(value="HealthFamilyHistory对象", description="家族史表")
public class HealthFamilyHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "家族史ID")
    @TableId(value = "family_id", type = IdType.AUTO)
    private Integer familyId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "亲属关系")
    private String relativeType;

    @ApiModelProperty(value = "疾病信息")
    private String diseases;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
