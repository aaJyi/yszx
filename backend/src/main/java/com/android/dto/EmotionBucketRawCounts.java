package com.android.dto;

import lombok.Data;

/**
 * 情绪监测表按分值区间的条数（单行查询结果）
 */
@Data
public class EmotionBucketRawCounts {
    private Long veryHappy;
    private Long happy;
    private Long calm;
    private Long low;
    private Long verySad;
}
