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
 * 健康档案-系统疾病与症状筛查表
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_system_disease_screening")
@ApiModel(value="HealthSystemDiseaseScreening对象", description="健康档案-系统疾病与症状筛查表")
public class HealthSystemDiseaseScreening implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "系统疾病筛查ID")
    @TableId(value = "screening_id", type = IdType.AUTO)
    private Integer screeningId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "头颅五官系统症状")
    private String headFacialSymptoms;

    @ApiModelProperty(value = "呼吸系统症状")
    private String respiratorySymptoms;

    @ApiModelProperty(value = "循环系统症状")
    private String circulatorySymptoms;

    @ApiModelProperty(value = "消化系统症状")
    private String digestiveSymptoms;

    @ApiModelProperty(value = "泌尿生殖系统症状")
    private String urogenitalSymptoms;

    @ApiModelProperty(value = "内分泌与代谢症状")
    private String endocrineMetabolicSymptoms;

    @ApiModelProperty(value = "造血系统症状")
    private String hematopoieticSymptoms;

    @ApiModelProperty(value = "肌肉骨骼系统症状")
    private String musculoskeletalSymptoms;

    @ApiModelProperty(value = "神经系统症状")
    private String neurologicalSymptoms;

    @ApiModelProperty(value = "精神状态症状")
    private String mentalStateSymptoms;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
