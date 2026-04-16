-- 健康资讯定时爬取：种子关键词（管理端工作台「智能导航」维护；定时任务仅抓取此处已启用的关键词，无配置默认回退）

CREATE TABLE IF NOT EXISTS health_crawl_keyword (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  keyword VARCHAR(128) NOT NULL COMMENT '爬取关键词（唯一）',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序，越小越先抓取',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库爬取种子关键词';

-- 初始种子（可按需删改；未启用或清空后本轮定时抓取不再爬种子）
INSERT IGNORE INTO health_crawl_keyword (keyword, sort_order, enabled, remark) VALUES
('高血压', 10, 1, 'seed'),
('糖尿病', 20, 1, 'seed'),
('冠心病', 30, 1, 'seed'),
('脑卒中', 40, 1, 'seed'),
('慢性病', 50, 1, 'seed'),
('肿瘤', 60, 1, 'seed'),
('康复', 70, 1, 'seed'),
('健康指南', 80, 1, 'seed'),
('心血管疾病', 90, 1, 'seed');
