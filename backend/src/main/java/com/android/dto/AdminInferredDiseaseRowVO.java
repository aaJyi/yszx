package com.android.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminInferredDiseaseRowVO {
    private Long userId;
    private String nickname;
    private String diseaseName;
    private BigDecimal confidence;
    private String sourceType;
    private LocalDateTime createdAt;
}
