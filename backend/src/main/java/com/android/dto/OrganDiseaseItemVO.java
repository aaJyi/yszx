package com.android.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条疾病知识（前端展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganDiseaseItemVO {
    private String diseaseName;
    private String description;
    /** 多条治疗措施可用换行分隔，前端可拆成列表 */
    private String treatmentMeasures;
}
