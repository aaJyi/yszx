package com.android.dto;

import lombok.Data;

import java.util.List;

@Data
public class SocialRecommendUserVO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    /** 与对方疾病 Jaccard 相似度（无数据时为 null） */
    private Double diseaseSimilarity;
    /** 是否已关注对方 */
    private Boolean followed;
    /** 对方是否关注了我 */
    private Boolean followsMe;
    /** 是否互关 */
    private Boolean mutualFollow;
    /** 推断疾病交集示意 */
    private List<String> sharedDiseaseHints;
    private Integer clubIndex;
    /** 社团画像向量相似度 */
    private Double profileSimilarity;
    /** 综合匹配分（用于排序） */
    private Double matchScore;
    /** 强匹配 / 经验互补 / 行为陪伴 */
    private String matchType;
}
