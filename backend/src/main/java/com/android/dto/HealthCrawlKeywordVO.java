package com.android.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthCrawlKeywordVO {
    private Long id;
    private String keyword;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
