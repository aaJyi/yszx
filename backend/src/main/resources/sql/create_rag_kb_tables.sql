-- ai-doctor-rag 向量索引数据源（与 data/index_ch 下 qa / liver_cancer / llama_data / drug.json 对应）
-- 执行前请确认数据库字符集为 utf8mb4

CREATE TABLE IF NOT EXISTS rag_kb_qa (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    instruction   VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '问题/指令',
    output        MEDIUMTEXT COMMENT '回答',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_kb_qa_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 医疗问答 qa.json';

CREATE TABLE IF NOT EXISTS rag_kb_liver_cancer (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    instruction   VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '问题/指令',
    output        MEDIUMTEXT COMMENT '回答',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_kb_liver_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 肝癌问答 liver_cancer.json';

CREATE TABLE IF NOT EXISTS rag_kb_llama (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    instruction   VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '问题/指令',
    output        MEDIUMTEXT COMMENT '回答',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_kb_llama_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG llama_data.json';

CREATE TABLE IF NOT EXISTS rag_kb_drug (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    drug_name          VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '药品',
    indication_text    TEXT COMMENT '药品适应症',
    diseases_labeled   VARCHAR(2000) DEFAULT NULL COMMENT '医学专家标注的适应病症',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_kb_drug_name (drug_name(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 药品 drug.json';

-- 示例数据（可替换为从 ai-doctor-rag/data/index_ch 导入的真实数据）
INSERT INTO rag_kb_qa (instruction, output) VALUES
('高血压患者日常饮食应注意什么？', '建议低盐饮食，每日钠摄入控制在合理范围；多吃蔬菜水果，限制高脂食物；戒烟限酒，规律监测血压并遵医嘱用药。'),
('糖尿病足如何预防？', '控制血糖与血脂；每日检查双足有无破溃；穿合适鞋袜，避免烫伤与外伤；发现问题及时就医。');

INSERT INTO rag_kb_liver_cancer (instruction, output) VALUES
('肝癌高危人群有哪些？', '慢性乙型/丙型肝炎病毒感染、肝硬化、长期酗酒、非酒精性脂肪性肝病相关肝硬化、黄曲霉毒素暴露等人群需定期筛查。'),
('甲胎蛋白升高是否一定是肝癌？', '不一定。AFP 升高可见于活动性肝炎、妊娠等情况，需结合影像学（如增强 CT/MRI）综合判断。');

INSERT INTO rag_kb_llama (instruction, output) VALUES
('什么是分级诊疗？', '指按照疾病的轻、重、缓、急及治疗难易程度，由不同级别医疗机构承担相应诊疗任务，合理分流患者。'),
('互联网医院主要提供哪些服务？', '常见复诊、用药指导、检验检查预约、健康咨询等；急危重症仍需线下急诊。');

INSERT INTO rag_kb_drug (drug_name, indication_text, diseases_labeled) VALUES
('阿司匹林肠溶片', '用于心绞痛预防、心肌梗死后的二级预防等', '动脉粥样硬化性心血管疾病'),
('二甲双胍片', '用于单纯饮食运动控制不佳的 2 型糖尿病', '2 型糖尿病');

-- 英文知识库（ai-doctor-rag/data/index/docs.csv）可选表
CREATE TABLE IF NOT EXISTS rag_kb_en_doc (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    input         TEXT COMMENT '英文问句（docs.csv.input）',
    output        MEDIUMTEXT COMMENT '英文回答（docs.csv.output）',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_kb_en_doc_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 英文知识库 docs.csv';
