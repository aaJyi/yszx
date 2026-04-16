-- 批量生成社团划分测试数据（MySQL 8+）
-- 目标：让 NCSS 可形成多个清晰社团，并保证 user_id=1 有推荐对象

SET NAMES utf8mb4;

-- 可按需调整规模
SET @N := 200;

DROP PROCEDURE IF EXISTS seed_social_club_test_data;
DELIMITER $$
CREATE PROCEDURE seed_social_club_test_data(IN p_n INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE g INT;
    DECLARE ex INT;
    DECLARE ad DECIMAL(4,2);
    DECLARE tr VARCHAR(16);
    DECLARE goals_json TEXT;
    DECLARE risk_level VARCHAR(8);
    DECLARE care_stage VARCHAR(32);
    DECLARE active_window VARCHAR(32);
    DECLARE v1 DECIMAL(6,4);
    DECLARE v2 DECIMAL(6,4);
    DECLARE v3 DECIMAL(6,4);
    DECLARE v4 DECIMAL(6,4);
    DECLARE d1 VARCHAR(32);
    DECLARE d2 VARCHAR(32);
    DECLARE d3 VARCHAR(32);
    DECLARE d4 VARCHAR(32);
    DECLARE d5 VARCHAR(32);

    -- 清理历史压测数据（保留系统原有小于100000的真实数据）
    DELETE FROM user_inferred_disease WHERE user_id >= 100000 OR user_id BETWEEN 1 AND p_n;
    DELETE FROM user_club_profile WHERE user_id >= 100000 OR user_id BETWEEN 1 AND p_n;
    DELETE FROM user_social_preference WHERE user_id >= 100000 OR user_id BETWEEN 1 AND p_n;
    DELETE FROM t_user WHERE id >= 100000 OR id BETWEEN 1 AND p_n;

    WHILE i <= p_n DO
        SET g = MOD(i - 1, 6); -- 6个主社团模板
        SET ex = 2 + MOD(i, 4); -- 2-5
        SET ad = 0.45 + (MOD(i, 6) * 0.06); -- 0.45-0.75
        SET tr = CASE MOD(i, 3) WHEN 0 THEN 'up' WHEN 1 THEN 'stable' ELSE 'down' END;
        SET active_window = CASE MOD(i, 3) WHEN 0 THEN '19:00-22:00' WHEN 1 THEN '12:00-14:00' ELSE '21:00-23:30' END;
        SET care_stage = CASE MOD(i, 4) WHEN 0 THEN '稳定管理期' WHEN 1 THEN '干预强化期' WHEN 2 THEN '康复巩固期' ELSE '随访观察期' END;
        SET risk_level = CASE MOD(i, 3) WHEN 0 THEN '高' WHEN 1 THEN '中' ELSE '低' END;

        -- 每个社团定义一组疾病语义
        IF g = 0 THEN
            SET d1 = '高血压风险'; SET d2 = '血脂异常风险'; SET d3 = '脂肪肝风险'; SET d4 = '超重肥胖风险'; SET d5 = '高尿酸风险';
            SET goals_json = '["控压","减重","降脂"]';
        ELSEIF g = 1 THEN
            SET d1 = '糖尿病风险'; SET d2 = '高血压风险'; SET d3 = '脂肪肝风险'; SET d4 = '肾功能受损风险'; SET d5 = '周围神经病变风险';
            SET goals_json = '["控糖","控压","肾脏保护"]';
        ELSEIF g = 2 THEN
            SET d1 = '冠心病风险'; SET d2 = '高血压风险'; SET d3 = '心律失常风险'; SET d4 = '血脂异常风险'; SET d5 = '动脉硬化风险';
            SET goals_json = '["心血管保护","控压","降脂"]';
        ELSEIF g = 3 THEN
            SET d1 = '慢阻肺风险'; SET d2 = '哮喘风险'; SET d3 = '睡眠呼吸暂停风险'; SET d4 = '焦虑风险'; SET d5 = '抑郁风险';
            SET goals_json = '["呼吸康复","睡眠改善","情绪管理"]';
        ELSEIF g = 4 THEN
            SET d1 = '骨关节炎风险'; SET d2 = '骨质疏松风险'; SET d3 = '肌少症风险'; SET d4 = '跌倒风险'; SET d5 = '慢性疼痛风险';
            SET goals_json = '["运动康复","防跌倒","疼痛管理"]';
        ELSE
            SET d1 = '甲状腺功能异常风险'; SET d2 = '贫血风险'; SET d3 = '焦虑风险'; SET d4 = '睡眠障碍风险'; SET d5 = '胃肠功能紊乱风险';
            SET goals_json = '["内分泌管理","情绪稳定","睡眠改善"]';
        END IF;

        -- 生成向量：按社团中心+轻微扰动，便于形成簇
        SET v1 = CASE g WHEN 0 THEN 0.82 WHEN 1 THEN 0.78 WHEN 2 THEN 0.76 WHEN 3 THEN 0.55 WHEN 4 THEN 0.62 ELSE 0.58 END + (MOD(i, 5) * 0.01);
        SET v2 = CASE g WHEN 0 THEN 0.52 WHEN 1 THEN 0.46 WHEN 2 THEN 0.43 WHEN 3 THEN 0.66 WHEN 4 THEN 0.61 ELSE 0.69 END + (MOD(i, 4) * 0.008);
        SET v3 = CASE g WHEN 0 THEN 0.44 WHEN 1 THEN 0.58 WHEN 2 THEN 0.63 WHEN 3 THEN 0.48 WHEN 4 THEN 0.53 ELSE 0.50 END + (MOD(i, 3) * 0.01);
        SET v4 = CASE g WHEN 0 THEN 0.59 WHEN 1 THEN 0.62 WHEN 2 THEN 0.57 WHEN 3 THEN 0.54 WHEN 4 THEN 0.67 ELSE 0.60 END + (MOD(i, 2) * 0.01);

        INSERT INTO t_user(id, openid, nickname, avatar_url, phone, password, gender, status, create_time, update_time)
        VALUES (i, CONCAT('test_openid_', i), CONCAT('测试用户', i), '', CONCAT('13', LPAD(i, 9, '0')), '', MOD(i, 2), 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            openid = VALUES(openid),
            nickname = VALUES(nickname),
            avatar_url = VALUES(avatar_url),
            phone = VALUES(phone),
            gender = VALUES(gender),
            status = 1,
            update_time = NOW();

        INSERT INTO user_club_profile(user_id, archive_id, analysis_id, profile_version, feature_json, vector_json, updated_at)
        VALUES (
            i,
            100000 + i,
            200000 + i,
            'v1',
            JSON_OBJECT(
                'diseaseRisk', JSON_OBJECT(
                    'confirmedDiseases', JSON_ARRAY(),
                    'inferredDiseases', JSON_ARRAY(
                        JSON_OBJECT('name', d1, 'confidence', 0.70),
                        JSON_OBJECT('name', d2, 'confidence', 0.66),
                        JSON_OBJECT('name', d3, 'confidence', 0.62),
                        JSON_OBJECT('name', d4, 'confidence', 0.58),
                        JSON_OBJECT('name', d5, 'confidence', 0.54)
                    ),
                    'riskLevel', risk_level,
                    'comorbidityPattern', JSON_ARRAY(d1, d2, d3)
                ),
                'behavior', JSON_OBJECT(
                    'exerciseLevel', ex,
                    'sleepQuality', '一般',
                    'smokingDrinkingLevel', '1-低/1-低',
                    'dietPattern', '均衡',
                    'adherenceScore', ad,
                    'followupRegularity', ad
                ),
                'metricTrend', JSON_OBJECT(
                    'trendStatus', tr,
                    'keyMetricCount', 4 + MOD(i, 3)
                ),
                'stageNeed', JSON_OBJECT(
                    'currentGoals', CAST(goals_json AS JSON),
                    'careStage', care_stage
                ),
                'socialTalkability', JSON_OBJECT(
                    'activeTimeWindow', active_window,
                    'replyStyle', 'normal'
                )
            ),
            JSON_ARRAY(v1, v2, v3, v4),
            NOW()
        )
        ON DUPLICATE KEY UPDATE
            archive_id = VALUES(archive_id),
            analysis_id = VALUES(analysis_id),
            profile_version = 'v1',
            feature_json = VALUES(feature_json),
            vector_json = VALUES(vector_json),
            updated_at = NOW();

        INSERT INTO user_inferred_disease(user_id, disease_name, confidence, source_type, analysis_id, created_at)
        VALUES
            (i, d1, 0.70, 'agent', 200000 + i, NOW()),
            (i, d2, 0.66, 'agent', 200000 + i, NOW()),
            (i, d3, 0.62, 'agent', 200000 + i, NOW()),
            (i, d4, 0.58, 'agent', 200000 + i, NOW()),
            (i, d5, 0.54, 'agent', 200000 + i, NOW());

        INSERT INTO user_social_preference(user_id, allow_recommend, allow_disease_based_match, active_time_window, reply_style, blocked_user_ids, updated_at)
        VALUES (i, 1, 1, active_window, 'normal', '', NOW())
        ON DUPLICATE KEY UPDATE
            allow_recommend = 1,
            allow_disease_based_match = 1,
            active_time_window = VALUES(active_time_window),
            reply_style = 'normal',
            blocked_user_ids = '',
            updated_at = NOW();

        SET i = i + 1;
    END WHILE;
END $$
DELIMITER ;

CALL seed_social_club_test_data(@N);
DROP PROCEDURE IF EXISTS seed_social_club_test_data;

