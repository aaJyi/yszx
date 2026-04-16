package com.android.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganDiseaseBundleVO {
    private String organKey;
    private String organZh;
    private List<OrganDiseaseItemVO> items;
}
