package com.android.dto;

import lombok.Data;

@Data
public class SocialPreferenceBody {
    private Long userId;
    private Boolean allowRecommend;
    private Boolean allowDiseaseBasedMatch;
    private String activeTimeWindow;
    private String replyStyle;
    private String blockedUserIds;
}

