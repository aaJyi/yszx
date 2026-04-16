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
 * 家人档案映射表
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("family_archive_mapping")
@ApiModel(value="FamilyArchiveMapping对象", description="家人档案映射表")
public class FamilyArchiveMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "映射ID（主键）")
    @TableId(value = "mapping_id", type = IdType.AUTO)
    private Integer mappingId;

    @ApiModelProperty(value = "档案ID（关联health_archive.archive_id）")
    private Integer archiveId;

    @ApiModelProperty(value = "创建者用户ID（谁帮助创建的档案）")
    private Integer creatorUserId;

    @ApiModelProperty(value = "档案拥有者手机号（用于匹配）")
    private String ownerPhone;

    @ApiModelProperty(value = "档案拥有者用户ID（如果已注册）")
    private Integer ownerUserId;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
