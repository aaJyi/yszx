package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户社团分析画像（由智能体分析结果抽取）
 */
@Data
@TableName("user_club_profile")
public class UserClubProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer archiveId;

    private Long analysisId;

    private String profileVersion;

    /** 五层结构化画像 JSON */
    private String featureJson;

    /** 用于相似度计算的数值向量 JSON */
    private String vectorJson;

    private LocalDateTime updatedAt;
}

