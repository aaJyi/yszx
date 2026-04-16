package com.android.dto;

import lombok.Data;

@Data
public class HealthCrawlKeywordSaveDTO {
    private String keyword;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
}
