package com.android.dto;

import com.android.entity.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 健康档案请求对象
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Data
@Schema(description = "健康档案请求对象")
public class HealthArchiveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 健康档案信息
     */
    @Schema(description = "健康档案信息")
    @JsonProperty("healthArchive")
    private HealthArchive healthArchive;

    /**
     * 个人健康标识信息
     */
    @Schema(description = "个人健康标识信息")
    @JsonProperty("healthProfileTags")
    private HealthProfileTagsRequest healthProfileTags;

    /**
     * 健康档案用户基本信息
     */
    @Schema(description = "健康档案用户基本信息")
    @JsonProperty("userInfo")
    private HealthUserInfo userInfo;

    /**
     * 健康档案用户紧急联系人信息
     */
    @Schema(description = "健康档案用户紧急联系人信息")
    @JsonProperty("userEmergencyContacts")
    private HealthUserEmergencyContacts userEmergencyContacts;

    /**
     * 健康档案用户健康服务凭证信息
     */
    @Schema(description = "健康档案用户健康服务凭证信息")
    @JsonProperty("userCertificates")
    private HealthUserCertificates userCertificates;

    /**
     * 过敏史
     */
    @Schema(description = "过敏史")
    @JsonProperty("allergyHistory")
    private HealthAllergyHistory allergyHistory;

    /**
     * 暴露史
     */
    @Schema(description = "暴露史")
    @JsonProperty("exposureHistory")
    private HealthExposureHistory exposureHistory;

    /**
     * 既往史（列表，一个档案可以有多条既往史）
     */
    @Schema(description = "既往史列表")
    @JsonProperty("diseaseHistory")
    private List<HealthDiseaseHistory> diseaseHistory;

    /**
     * 预防接种史（列表，一个档案可以有多条接种史）
     */
    @Schema(description = "预防接种史列表")
    @JsonProperty("vaccinationHistory")
    private List<HealthVaccinationHistory> vaccinationHistory;

    /**
     * 家族史（列表，一个档案可以有多条家族史）
     */
    @Schema(description = "家族史列表")
    @JsonProperty("familyHistory")
    private List<HealthFamilyHistory> familyHistory;

    /**
     * 遗传史
     */
    @Schema(description = "遗传史")
    @JsonProperty("geneticHistory")
    private HealthGeneticHistory geneticHistory;

    /**
     * 残疾情况
     */
    @Schema(description = "残疾情况")
    @JsonProperty("disabilityHistory")
    private HealthDisabilityStatus disabilityHistory;

}
