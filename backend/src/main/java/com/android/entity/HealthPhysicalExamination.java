package com.android.entity;

import java.math.BigDecimal;
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
 * 健康档案-体格检查表
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_physical_examination")
@ApiModel(value="HealthPhysicalExamination对象", description="健康档案-体格检查表")
public class HealthPhysicalExamination implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "体格检查ID")
    @TableId(value = "exam_id", type = IdType.AUTO)
    private Integer examId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "体温 ℃")
    private BigDecimal temperature;

    @ApiModelProperty(value = "脉搏 次/分")
    private Integer pulse;

    @ApiModelProperty(value = "呼吸 次/分")
    private Integer respiration;

    @ApiModelProperty(value = "收缩压 mmHg")
    private Integer systolicBp;

    @ApiModelProperty(value = "舒张压 mmHg")
    private Integer diastolicBp;

    @ApiModelProperty(value = "身高 cm")
    private BigDecimal heightCm;

    @ApiModelProperty(value = "体重 kg")
    private BigDecimal weightKg;

    @ApiModelProperty(value = "血糖 mmol/L")
    private BigDecimal gluValue;

    @ApiModelProperty(value = "血糖类型")
    private String gluType;

    @ApiModelProperty(value = "发育情况")
    private String development;

    @ApiModelProperty(value = "发育异常描述")
    private String developmentDesc;

    @ApiModelProperty(value = "营养状况")
    private String nutrition;

    @ApiModelProperty(value = "体型")
    private String bodyType;

    @ApiModelProperty(value = "面容")
    private String facialExpression;

    @ApiModelProperty(value = "病容类型")
    private String facialExpressionDesc;

    @ApiModelProperty(value = "体位")
    private String posture;

    @ApiModelProperty(value = "强迫体位类型")
    private String postureDesc;

    @ApiModelProperty(value = "步态")
    private String gait;

    @ApiModelProperty(value = "异常步态描述")
    private String gaitDesc;

    @ApiModelProperty(value = "意识状态")
    private String consciousness;

    @ApiModelProperty(value = "语言表达")
    private String speech;

    @ApiModelProperty(value = "皮肤颜色")
    private String skinColor;

    @ApiModelProperty(value = "皮肤湿度")
    private String skinMoisture;

    @ApiModelProperty(value = "皮肤温度")
    private String skinTemperature;

    @ApiModelProperty(value = "皮肤弹性")
    private String skinElasticity;

    @ApiModelProperty(value = "水肿")
    private String edema;

    @ApiModelProperty(value = "水肿部位及程度")
    private String edemaDesc;

    @ApiModelProperty(value = "皮肤完整性")
    private String skinIntegrity;

    @ApiModelProperty(value = "皮损描述")
    private String skinIntegrityDesc;

    @ApiModelProperty(value = "淋巴结")
    private String lymphNodes;

    @ApiModelProperty(value = "眼睑")
    private String eyelid;

    @ApiModelProperty(value = "结膜")
    private String conjunctiva;

    @ApiModelProperty(value = "巩膜")
    private String sclera;

    @ApiModelProperty(value = "瞳孔")
    private String pupil;

    @ApiModelProperty(value = "瞳孔异常描述")
    private String pupilDesc;

    @ApiModelProperty(value = "对光反射")
    private String lightReflex;

    @ApiModelProperty(value = "口唇")
    private String lips;

    @ApiModelProperty(value = "口腔黏膜")
    private String oralMucosa;

    @ApiModelProperty(value = "牙齿")
    private String teeth;

    @ApiModelProperty(value = "视力")
    private String vision;

    @ApiModelProperty(value = "视力异常描述")
    private String visionDesc;

    @ApiModelProperty(value = "听力")
    private String hearing;

    @ApiModelProperty(value = "听力异常描述")
    private String hearingDesc;

    @ApiModelProperty(value = "嗅觉")
    private String smell;

    @ApiModelProperty(value = "嗅觉异常描述")
    private String smellDesc;

    @ApiModelProperty(value = "颈项强直")
    private String neckStiffness;

    @ApiModelProperty(value = "颈静脉")
    private String jugularVein;

    @ApiModelProperty(value = "气管")
    private String trachea;

    @ApiModelProperty(value = "肝颈静脉回流征")
    private String hepatojugularReflex;

    @ApiModelProperty(value = "呼吸方式")
    private String breathingMode;

    @ApiModelProperty(value = "呼吸节律")
    private String breathingRhythm;

    @ApiModelProperty(value = "呼吸节律异常描述")
    private String breathingRhythmDesc;

    @ApiModelProperty(value = "呼吸困难")
    private String dyspnea;

    @ApiModelProperty(value = "呼吸音")
    private String breathSound;

    @ApiModelProperty(value = "异常呼吸音描述")
    private String breathSoundDesc;

    @ApiModelProperty(value = "啰音")
    private String rales;

    @ApiModelProperty(value = "啰音描述")
    private String ralesDesc;

    @ApiModelProperty(value = "心率 次/分")
    private Integer heartRate;

    @ApiModelProperty(value = "心律")
    private String heartRhythm;

    @ApiModelProperty(value = "杂音")
    private String murmur;

    @ApiModelProperty(value = "杂音描述")
    private String murmurDesc;

    @ApiModelProperty(value = "腹部外形")
    private String abdomenShape;

    @ApiModelProperty(value = "腹部包块")
    private String abdominalMass;

    @ApiModelProperty(value = "包块描述")
    private String abdominalMassDesc;

    @ApiModelProperty(value = "腹肌紧张")
    private String abdominalTension;

    @ApiModelProperty(value = "腹肌紧张描述")
    private String abdominalTensionDesc;

    @ApiModelProperty(value = "压痛")
    private String tenderness;

    @ApiModelProperty(value = "压痛描述")
    private String tendernessDesc;

    @ApiModelProperty(value = "反跳痛")
    private String reboundTenderness;

    @ApiModelProperty(value = "反跳痛描述")
    private String reboundTendernessDesc;

    @ApiModelProperty(value = "肝大")
    private String hepatomegaly;

    @ApiModelProperty(value = "肝大描述")
    private String hepatomegalyDesc;

    @ApiModelProperty(value = "脾大")
    private String splenomegaly;

    @ApiModelProperty(value = "脾大描述")
    private String splenomegalyDesc;

    @ApiModelProperty(value = "移动性浊音")
    private String shiftingDullness;

    @ApiModelProperty(value = "肠鸣音 次/分")
    private Integer bowelSounds;

    @ApiModelProperty(value = "肠鸣音状态")
    private String bowelSoundStatus;

    @ApiModelProperty(value = "直肠肛门")
    private String rectalExam;

    @ApiModelProperty(value = "直肠异常描述")
    private String rectalExamDesc;

    @ApiModelProperty(value = "外生殖器")
    private String genitalExam;

    @ApiModelProperty(value = "外生殖器异常描述")
    private String genitalExamDesc;

    @ApiModelProperty(value = "脊柱外形")
    private String spineShape;

    @ApiModelProperty(value = "脊柱畸形描述")
    private String spineDesc;

    @ApiModelProperty(value = "脊柱活动")
    private String spineActivity;

    @ApiModelProperty(value = "四肢外形")
    private String limbShape;

    @ApiModelProperty(value = "四肢畸形描述")
    private String limbDesc;

    @ApiModelProperty(value = "四肢活动")
    private String limbActivity;

    @ApiModelProperty(value = "疼痛")
    private String pain;

    @ApiModelProperty(value = "疼痛描述")
    private String painDesc;

    @ApiModelProperty(value = "疼痛评分")
    private String painScore;

    @ApiModelProperty(value = "肌力")
    private String muscleStrength;

    @ApiModelProperty(value = "肌力异常描述")
    private String muscleStrengthDesc;

    @ApiModelProperty(value = "肢体瘫痪")
    private String paralysis;

    @ApiModelProperty(value = "瘫痪描述")
    private String paralysisDesc;

    @ApiModelProperty(value = "肌力分级")
    private Integer muscleStrengthGrade;

    @ApiModelProperty(value = "病理反射")
    private String pathologicalReflex;

    @ApiModelProperty(value = "脑膜刺激征")
    private String meningealSign;

    @ApiModelProperty(value = "脑膜刺激征类型")
    private String meningealSignType;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
