package com.android.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * AI健康分析结果表（模型结论层）
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_ai_analysis")
@ApiModel(value="HealthAiAnalysis对象", description="AI健康分析结果表（模型结论层）")
public class HealthAiAnalysis implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "分析ID")
    @TableId(value = "analysis_id", type = IdType.AUTO)
    private Long analysisId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "触发分析的原始数据ID（如体检报告）")
    private Long sourceRawId;

    @ApiModelProperty(value = "模型名称")
    private String modelName;

    @ApiModelProperty(value = "模型版本")
    private String modelVersion;

    @ApiModelProperty(value = "健康评分 0-100")
    private Integer overallHealthScore;

    @ApiModelProperty(value = "风险等级")
    private String riskLevel;

    @ApiModelProperty(value = "分析摘要（系统/医生用）")
    private String analysisSummary;

    @ApiModelProperty(value = "结构化分析结果（模型结论）JSON格式")
    private String analysisDetail;

    @ApiModelProperty(value = "分析元信息（来源类型、分析时间等）JSON格式")
    private String analysisMeta;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
