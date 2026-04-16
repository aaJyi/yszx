package com.android.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SocialInferredDiseaseVO {
    private Long id;
    private String diseaseName;
    private BigDecimal confidence;
}
