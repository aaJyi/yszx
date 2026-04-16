-- =========================================================
-- drop_feature_tables.sql
-- 下线业务：删除以下 26 张表（关闭外键检查后顺序无关）
-- 执行前请先：USE your_database;
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_activities;
DROP TABLE IF EXISTS mdt_consultation_attachment;
DROP TABLE IF EXISTS mdt_consultation_expert;
DROP TABLE IF EXISTS mdt_consultation;
DROP TABLE IF EXISTS mall_order_item;
DROP TABLE IF EXISTS mall_order;
DROP TABLE IF EXISTS referral_order_attachment;
DROP TABLE IF EXISTS referral_order;
DROP TABLE IF EXISTS escort_order;
DROP TABLE IF EXISTS gene_test_order;
DROP TABLE IF EXISTS home_service_order;
DROP TABLE IF EXISTS travel_healing_order;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS nutrition_intake_record;
DROP TABLE IF EXISTS activities;
DROP TABLE IF EXISTS ai_doctor;
DROP TABLE IF EXISTS chronic_disease_task;
DROP TABLE IF EXISTS escort_staff;
DROP TABLE IF EXISTS gene_test_product;
DROP TABLE IF EXISTS home_service_type;
DROP TABLE IF EXISTS mall_product;
DROP TABLE IF EXISTS management_plan;
DROP TABLE IF EXISTS nutrition_recipe;
DROP TABLE IF EXISTS real_doctor;
DROP TABLE IF EXISTS sleep_record;
DROP TABLE IF EXISTS travel_healing_package;

SET FOREIGN_KEY_CHECKS = 1;
