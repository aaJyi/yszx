package com.android.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminKnowledgeArticleRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String keyword;
    private String title;
    private String summary;
    private String coverUrl;
    private String articleUrl;
    private LocalDateTime fetchedAt;
}
