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
 * 暴露史表
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_exposure_history")
@ApiModel(value="HealthExposureHistory对象", description="暴露史表")
public class HealthExposureHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "暴露ID")
    @TableId(value = "exposure_id", type = IdType.AUTO)
    private Integer exposureId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "暴露类型")
    private String exposureType;

    @ApiModelProperty(value = "暴露详情说明")
    private String exposureDetails;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
