package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局健康资讯知识库（按原文 URL 去重）
 */
@Data
@TableName("health_knowledge_article")
public class HealthKnowledgeArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String keyword;

    private String title;

    private String summary;

    private String coverUrl;

    private String articleUrl;

    private String urlHash;

    private LocalDateTime fetchedAt;
}
