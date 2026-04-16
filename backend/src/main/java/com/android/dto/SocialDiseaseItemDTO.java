package com.android.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SocialDiseaseItemDTO {
    private String diseaseName;
    private BigDecimal confidence;
}
