-- 社交/资讯：智能体推断疾病 + 今日头条相关文章推荐（需遵守平台规则与版权）

CREATE TABLE IF NOT EXISTS user_inferred_disease (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  disease_name VARCHAR(255) NOT NULL COMMENT '推断疾病名称（中文或英文均可，用作检索关键词）',
  confidence DECIMAL(6,4) DEFAULT NULL COMMENT '置信度 0~1',
  source_type VARCHAR(32) NOT NULL DEFAULT 'agent' COMMENT '来源：agent / manual',
  analysis_id BIGINT DEFAULT NULL COMMENT '关联 health_ai_analysis.analysis_id',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_uid (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推断可能疾病（供资讯检索）';

CREATE TABLE IF NOT EXISTS social_toutiao_article (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  keyword VARCHAR(255) NOT NULL COMMENT '检索关键词（多为疾病名）',
  title VARCHAR(512) NOT NULL COMMENT '标题',
  summary TEXT COMMENT '摘要',
  cover_url VARCHAR(1024) DEFAULT NULL COMMENT '封面图',
  article_url VARCHAR(2048) NOT NULL COMMENT '原文链接（多为头条域名）',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同批次内排序',
  fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
  PRIMARY KEY (id),
  KEY idx_user_fetched (user_id, fetched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐给用户的资讯条目（NewsAPI/RSS 等，表名历史保留）';
