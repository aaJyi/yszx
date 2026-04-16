package com.android.dto;

import lombok.Data;

@Data
public class SocialArticleItemVO {
    private Long id;
    private String keyword;
    private String title;
    private String summary;
    private String coverUrl;
    private String articleUrl;
}
