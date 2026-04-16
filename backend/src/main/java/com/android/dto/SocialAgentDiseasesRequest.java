package com.android.dto;

import lombok.Data;

import java.util.List;

@Data
public class SocialAgentDiseasesRequest {
    private Long userId;
    private Long analysisId;
    private List<SocialDiseaseItemDTO> diseases;
}
