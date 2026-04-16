-- =========================================================
-- verify_cascade_rules.sql
-- 用途：
-- 1) 在你“仅保留指定表”的前提下，检查这些表之间外键的 ON DELETE 规则。
-- 2) 输出所有：DELETE_RULE 不是 CASCADE / SET NULL / SET DEFAULT 的外键约束。
--
-- 说明：
-- - 这是“校验输出脚本”，不会修改任何约束。
-- - 你可以将输出结果用于后续按需执行 ALTER ... ON DELETE CASCADE。
-- =========================================================

SET @v_schema = DATABASE();

DROP TEMPORARY TABLE IF EXISTS keep_tables;
CREATE TEMPORARY TABLE keep_tables (
  table_name VARCHAR(255) PRIMARY KEY
) ENGINE=InnoDB;

-- 保留表清单（与 drop_unused_tables.sql 保持一致）
INSERT INTO keep_tables(table_name) VALUES
  ('t_user'),
  ('raw_health_data'),
  ('health_ai_analysis'),
  ('health_recommendation'),
  ('health_recommendation_rule'),

  ('emotion_monitoring'),

  ('family_member'),
  ('family_archive_mapping'),

  ('health_archive'),
  ('health_allergy_history'),
  ('health_disability_status'),
  ('health_disease_history'),
  ('health_exposure_history'),
  ('health_family_history'),
  ('health_genetic_history'),
  ('health_lifestyle_status'),
  ('health_physical_examination'),
  ('health_profile_tags'),
  ('health_psychological_assessment'),
  ('health_social_relationship_assessment'),
  ('health_system_disease_screening'),
  ('health_user_info'),
  ('health_user_certificates'),
  ('health_user_emergency_contacts'),
  ('health_vaccination_history');

-- 输出：保留表之间的外键中，DELETE_RULE 不符合预期的约束
SELECT
  rc.CONSTRAINT_NAME AS constraint_name,
  kcu.TABLE_NAME AS child_table,
  kcu.COLUMN_NAME AS child_column,
  kcu2.TABLE_NAME AS parent_table,
  kcu2.COLUMN_NAME AS parent_column,
  rc.UPDATE_RULE AS update_rule,
  rc.DELETE_RULE AS delete_rule
FROM information_schema.REFERENTIAL_CONSTRAINTS rc
JOIN information_schema.KEY_COLUMN_USAGE kcu
  ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
 AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
JOIN information_schema.KEY_COLUMN_USAGE kcu2
  ON rc.CONSTRAINT_SCHEMA = kcu2.CONSTRAINT_SCHEMA
 AND rc.UNIQUE_CONSTRAINT_NAME = kcu2.CONSTRAINT_NAME
WHERE rc.CONSTRAINT_SCHEMA = @v_schema
  AND kcu.TABLE_NAME IN (SELECT table_name FROM keep_tables)
  AND kcu2.TABLE_NAME IN (SELECT table_name FROM keep_tables)
  AND rc.DELETE_RULE NOT IN ('CASCADE', 'SET NULL', 'SET DEFAULT');

-- 可选：如果你想人工定位某个约束的建表语句/当前规则，可按 constraint_name 再查
-- SELECT * FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = '...';

