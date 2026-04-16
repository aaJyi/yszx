-- =====================================================
-- AI健康分析相关表结构
-- 基于现有表结构扩展，不推翻原有设计
-- =====================================================

-- 1. AI分析结果表（模型结论层）
CREATE TABLE IF NOT EXISTS health_ai_analysis (
    analysis_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分析ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    archive_id INT NOT NULL COMMENT '档案ID',
    
    -- 触发来源（非常重要）
    source_raw_id BIGINT COMMENT '触发分析的原始数据ID（如体检报告）',
    
    -- 模型信息
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_version VARCHAR(50) NOT NULL COMMENT '模型版本',
    
    -- 分析结果
    overall_health_score INT COMMENT '健康评分 0-100',
    risk_level ENUM('低','中','高','极高') NOT NULL COMMENT '风险等级',
    
    -- 分析内容
    analysis_summary TEXT COMMENT '分析摘要（系统/医生用）',
    analysis_detail JSON COMMENT '结构化分析结果（模型结论）',
    
    -- 分析元信息
    analysis_meta JSON COMMENT '分析元信息（来源类型、分析时间等）',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (archive_id) REFERENCES health_archive(archive_id) ON DELETE CASCADE,
    FOREIGN KEY (source_raw_id) REFERENCES raw_health_data(id) ON DELETE SET NULL,
    
    -- 索引
    INDEX idx_user_archive (user_id, archive_id),
    INDEX idx_model (model_name, model_version),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_source_raw (source_raw_id),
    INDEX idx_risk_level (risk_level)
    
) COMMENT 'AI健康分析结果表（模型结论层）' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 用户健康建议表（用户可见层）
CREATE TABLE IF NOT EXISTS health_recommendation (
    recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '建议ID',
    analysis_id BIGINT NOT NULL COMMENT '分析ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 建议类型
    recommendation_type ENUM(
        'LIFESTYLE',
        'DIET',
        'EXERCISE',
        'MEDICAL',
        'FOLLOW_UP',
        'SERVICE'
    ) NOT NULL COMMENT '建议类型',
    
    -- 建议内容
    title VARCHAR(200) NOT NULL COMMENT '建议标题',
    content TEXT NOT NULL COMMENT '建议内容',
    
    -- 优先级和状态
    priority ENUM('低','中','高') DEFAULT '中' COMMENT '优先级',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否生效',
    
    -- 有效期
    valid_from DATE COMMENT '生效日期',
    valid_to DATE COMMENT '失效日期',
    
    -- 关联维度（可选）
    dimension_code VARCHAR(50) COMMENT '关联的维度代码（如METABOLIC）',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (analysis_id) REFERENCES health_ai_analysis(analysis_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_user_active (user_id, is_active),
    INDEX idx_analysis (analysis_id),
    INDEX idx_type (recommendation_type),
    INDEX idx_priority (priority),
    INDEX idx_dimension (dimension_code)
    
) COMMENT 'AI健康建议表（用户行动层）' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 建议规则表（规则引擎配置）
CREATE TABLE IF NOT EXISTS health_recommendation_rule (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规则ID',
    
    -- 规则触发条件
    dimension_code VARCHAR(50) COMMENT '维度代码（如METABOLIC）',
    risk_level ENUM('低','中','高','极高') COMMENT '风险等级',
    indicator_code VARCHAR(50) COMMENT '指标代码（可选，精确匹配）',
    
    -- 建议类型和内容
    recommendation_type ENUM(
        'LIFESTYLE',
        'DIET',
        'EXERCISE',
        'MEDICAL',
        'FOLLOW_UP',
        'SERVICE'
    ) NOT NULL COMMENT '建议类型',
    
    title_template VARCHAR(200) NOT NULL COMMENT '标题模板',
    content_template TEXT NOT NULL COMMENT '内容模板（支持变量替换）',
    
    -- 优先级和状态
    priority ENUM('低','中','高') DEFAULT '中' COMMENT '优先级',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    
    -- 规则顺序（同条件多条规则时，按order_num排序）
    order_num INT DEFAULT 0 COMMENT '排序号',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_dimension_risk (dimension_code, risk_level),
    INDEX idx_indicator (indicator_code),
    INDEX idx_type (recommendation_type),
    INDEX idx_active (is_active, order_num)
    
) COMMENT '健康建议规则表（规则引擎配置）' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 初始化规则数据（示例）
-- =====================================================

-- 插入默认规则数据
INSERT INTO health_recommendation_rule (dimension_code, risk_level, recommendation_type, title_template, content_template, priority, order_num) VALUES
-- 代谢健康 - 高风险 - 生活方式建议
('METABOLIC', '高', 'LIFESTYLE', '关注代谢健康，建议进行生活方式管理', '根据体检结果显示，部分代谢指标与参考范围存在差异，提示可能存在代谢异常风险。建议在日常生活中关注饮食结构，减少高脂高糖食物摄入，规律作息，并根据自身情况适当增加身体活动。如有不适或疑问，建议咨询专业医疗人员。', '高', 1),

-- 代谢健康 - 高风险 - 医疗关注建议
('METABOLIC', '高', 'MEDICAL', '建议进一步关注相关指标', '体检结果中部分代谢指标需要进一步关注。本分析仅基于现有体检数据，不能替代医生诊断。建议在条件允许的情况下，前往正规医疗机构进行进一步检查或咨询专业医生意见。', '高', 2),

-- 代谢健康 - 中风险 - 生活方式建议
('METABOLIC', '中', 'LIFESTYLE', '关注代谢指标变化', '体检结果显示部分代谢指标略有偏离参考范围，建议持续关注并适当调整生活方式，保持规律作息和适度运动。', '中', 1),

-- 血糖控制 - 高风险 - 医疗关注建议
('GLYCEMIC', '高', 'MEDICAL', '建议关注血糖指标', '体检结果显示血糖相关指标偏离参考范围，提示可能存在血糖控制问题。本分析仅基于现有体检数据，不能替代医生诊断。建议前往正规医疗机构进行进一步检查或咨询专业医生意见。', '高', 1),

-- 血糖控制 - 中风险 - 饮食建议
('GLYCEMIC', '中', 'DIET', '建议调整饮食结构', '体检结果显示血糖指标略有偏离参考范围，建议关注饮食结构，减少高糖食物摄入，选择低升糖指数的食物，保持规律饮食。', '中', 1),

-- 泌尿系统异常 - 高风险 - 医疗关注建议
('URINARY', '高', 'MEDICAL', '建议关注泌尿系统指标', '体检结果中部分泌尿系统指标存在异常，需要进一步关注。本分析仅基于现有体检数据，不能替代医生诊断。建议前往正规医疗机构进行进一步检查。', '高', 1),

-- 运动建议 - 通用
(NULL, NULL, 'EXERCISE', '建议适度增加身体活动', '根据健康评估结果，建议根据自身情况适度增加身体活动，每周进行至少150分钟的中等强度有氧运动，如快走、慢跑、游泳等。运动前请充分热身，如有不适请立即停止。', '中', 1);
