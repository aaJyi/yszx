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
 * 健康档案-日常生活方式与行为状况表
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_lifestyle_status")
@ApiModel(value="HealthLifestyleStatus对象", description="健康档案-日常生活方式与行为状况表")
public class HealthLifestyleStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "生活方式记录ID")
    @TableId(value = "lifestyle_id", type = IdType.AUTO)
    private Integer lifestyleId;

    @ApiModelProperty(value = "档案ID")
    private Integer archiveId;

    @ApiModelProperty(value = "主要膳食种类（可多选）")
    private String dietType;

    @ApiModelProperty(value = "三餐是否规律")
    private Boolean mealsRegular;

    @ApiModelProperty(value = "三餐不规律描述")
    private String mealsIrregularDesc;

    @ApiModelProperty(value = "外出用餐频率（次/周）")
    private Integer eatOutFrequency;

    @ApiModelProperty(value = "特殊饮食习惯")
    private String specialDietHabit;

    @ApiModelProperty(value = "特殊饮食习惯描述")
    private String specialDietDesc;

    @ApiModelProperty(value = "食欲")
    private String appetite;

    @ApiModelProperty(value = "排尿情况")
    private String urination;

    @ApiModelProperty(value = "排便情况")
    private String defecationStatus;

    @ApiModelProperty(value = "便秘持续天数")
    private Integer constipationDays;

    @ApiModelProperty(value = "是否辅助排便")
    private Boolean constipationNeedAssist;

    @ApiModelProperty(value = "腹泻次数（次/日）")
    private Integer diarrheaTimesPerDay;

    @ApiModelProperty(value = "活动能力")
    private String activityAbility;

    @ApiModelProperty(value = "自理能力")
    private String selfCareAbility;

    @ApiModelProperty(value = "体格锻炼方式")
    private String exerciseType;

    @ApiModelProperty(value = "体格锻炼频率（次/周）")
    private Integer exerciseFrequency;

    @ApiModelProperty(value = "外出/上班方式")
    private String commuteMode;

    @ApiModelProperty(value = "作息是否规律")
    private Boolean routineRegular;

    @ApiModelProperty(value = "作息不规律描述")
    private String routineIrregularDesc;

    @ApiModelProperty(value = "睡眠情况")
    private String sleepStatus;

    @ApiModelProperty(value = "睡眠异常描述")
    private String sleepDesc;

    @ApiModelProperty(value = "定期保养/理疗")
    private String regularTherapy;

    @ApiModelProperty(value = "理疗项目")
    private String therapyProject;

    @ApiModelProperty(value = "理疗频率（次/年）")
    private Integer therapyFrequencyPerYear;

    @ApiModelProperty(value = "是否定期体检")
    private String regularCheckup;

    @ApiModelProperty(value = "体检频率（次/年）")
    private Integer checkupFrequencyPerYear;

    @ApiModelProperty(value = "减肥/增重行为")
    private String weightControl;

    @ApiModelProperty(value = "体重与去年对比")
    private String weightChange;

    @ApiModelProperty(value = "吸烟情况")
    private String smokingStatus;

    @ApiModelProperty(value = "吸烟量（支/日）")
    private Integer cigarettesPerDay;

    @ApiModelProperty(value = "已吸烟年数")
    private Integer smokingYears;

    @ApiModelProperty(value = "已戒烟年数")
    private Integer quitSmokingYears;

    @ApiModelProperty(value = "饮酒情况")
    private String drinkingStatus;

    @ApiModelProperty(value = "饮酒次数（次/日）")
    private Integer drinkingTimesPerDay;

    @ApiModelProperty(value = "已饮酒年数")
    private Integer drinkingYears;

    @ApiModelProperty(value = "已戒酒年数")
    private Integer quitDrinkingYears;

    @ApiModelProperty(value = "药物依赖")
    private String drugDependence;

    @ApiModelProperty(value = "药物名称及剂量描述")
    private String drugDependenceDesc;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
