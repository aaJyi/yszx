-- 全局健康资讯知识库（RSS/NewsAPI 等拉取，按 URL 去重；供小程序按疾病关键词匹配、管理端展示）

CREATE TABLE IF NOT EXISTS health_knowledge_article (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  keyword VARCHAR(255) NOT NULL COMMENT '检索关键词（疾病/主题）',
  title VARCHAR(512) NOT NULL COMMENT '标题',
  summary TEXT COMMENT '摘要',
  cover_url VARCHAR(1024) DEFAULT NULL COMMENT '封面图',
  article_url VARCHAR(2048) NOT NULL COMMENT '原文链接',
  url_hash VARCHAR(64) NOT NULL COMMENT 'article_url 的 SHA-256 十六进制，用于去重',
  fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_url_hash (url_hash),
  KEY idx_keyword_fetched (keyword, fetched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康资讯知识库（全局）';
