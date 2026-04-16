-- =========================================================
-- drop_unused_tables.sql
-- 仅用于“清理多余表”，尽量保留现有表结构与数据（drop-only preserve）。
--
-- 使用方式：
-- 1) 先切换到目标数据库：USE your_db;
-- 2) 直接执行本脚本
--
-- 保留表集合：仅保留 keep_tables 中列出的业务表（已移除活动/商城/预约等下线表）
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

DELIMITER $$
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_table VARCHAR(255);
  DECLARE v_schema VARCHAR(255) DEFAULT DATABASE();

  -- 保留表清单
  DROP TEMPORARY TABLE IF EXISTS keep_tables;
  CREATE TEMPORARY TABLE keep_tables (
    table_name VARCHAR(255) PRIMARY KEY
  ) ENGINE=InnoDB;

  INSERT INTO keep_tables(table_name) VALUES
    -- 基础用户/原始数据
    ('t_user'),
    ('raw_health_data'),

    -- AI 医生与对话/健康分析
    ('health_ai_analysis'),
    ('health_recommendation'),
    ('health_recommendation_rule'),

    -- 运动识别（识别只用到接口，本清单保留 ai/记录相关的表；本项目目前依赖 raw_health_data 表承载原始数据）
    ('emotion_monitoring'),

    -- 情绪/监测
    ('emotion_monitoring'),

    -- 家人
    ('family_member'),
    ('family_archive_mapping'),

    -- 健康档案与各子表
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

  DECLARE cur CURSOR FOR
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = v_schema
      AND table_type = 'BASE TABLE';

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_table;
    IF done = 1 THEN
      LEAVE read_loop;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM keep_tables WHERE table_name = v_table) THEN
      SET @stmt = CONCAT('DROP TABLE IF EXISTS `', v_table, '`;');
      PREPARE stmt FROM @stmt;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END IF;
  END LOOP;

  CLOSE cur;
END$$
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;

