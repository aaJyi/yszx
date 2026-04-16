package com.android.dto;

import lombok.Data;

@Data
public class ClubDivisionRecomputeBody {
    /** 与 NCSS 建边一致的综合相似度阈值，默认见 club.division.similarity.threshold.default（当前 0.45） */
    private Double similarityThreshold;
}
