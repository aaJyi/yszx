package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐给用户的资讯条目（来源为 NewsAPI / RSS 等外部资讯，表名历史保留）
 */
@Data
@TableName("social_toutiao_article")
public class SocialToutiaoArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String keyword;

    private String title;

    private String summary;

    private String coverUrl;

    private String articleUrl;

    private Integer sortOrder;

    private LocalDateTime fetchedAt;
}
