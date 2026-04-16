package com.android.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 个人健康标识请求对象
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Data
@Schema(description = "个人健康标识请求对象")
public class HealthProfileTagsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否0-6岁儿童
     */
    @Schema(description = "是否0-6岁儿童")
    private Boolean isChild06;

    /**
     * 是否65岁以上
     */
    @Schema(description = "是否65岁以上")
    private Boolean isElderly65;

    /**
     * 是否孕产妇
     */
    @Schema(description = "是否孕产妇")
    private Boolean isPregnant;

    /**
     * 风险等级：低风险、一般风险、较高风险、高风险
     */
    @Schema(description = "风险等级")
    private String pregnancyRisk;

    /**
     * 慢性病/重点疾病列表
     * 可选值：高血压、2型糖尿病、脑卒中、冠心病、脑血管病后遗症、慢性阻塞性肺疾病、哮喘、尿毒症、恶性肿瘤、严重精神障碍、地方病、职业病、失能、失智、先天畸形
     */
    @Schema(description = "慢性病/重点疾病列表")
    private List<String> chronicDisease;

    /**
     * 法定传染病列表
     * 可选值：肺结核、肝炎、其他法定传染病
     */
    @Schema(description = "法定传染病列表")
    private List<String> statutoryInfo;

    /**
     * 体重状况：低、正常、超重、肥胖
     */
    @Schema(description = "体重状况")
    private String weightStatus;

    /**
     * 血型：A、B、O、AB、不详
     */
    @Schema(description = "血型")
    private String bloodType;
}
