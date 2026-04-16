-- 器官关联疾病知识（详述 + 治疗措施），供病机图谱等模块查询
-- 执行前请确认数据库名与字符集（建议 utf8mb4）

CREATE TABLE IF NOT EXISTS organ_disease_detail (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  organ_key VARCHAR(64) NOT NULL COMMENT '与前端 organLabelMap key 一致，如 brain、liver',
  organ_zh VARCHAR(64) NOT NULL COMMENT '器官中文名',
  disease_name VARCHAR(255) NOT NULL COMMENT '疾病名称',
  description TEXT COMMENT '疾病详述（教学示意，非诊断）',
  treatment_measures TEXT COMMENT '治疗措施，多条可用换行分隔',
  data_source VARCHAR(128) DEFAULT 'seed' COMMENT '数据来源：seed / wikipedia_api / manual',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同器官下排序，小在前',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_organ_key (organ_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='器官-疾病知识条目';
