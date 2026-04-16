-- 英文知识库（ai-doctor-rag/data/index/docs.csv）对应表
-- docs.csv 列：input, output

CREATE TABLE IF NOT EXISTS rag_kb_en_doc (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    input         TEXT COMMENT '英文问句（docs.csv.input）',
    output        MEDIUMTEXT COMMENT '英文回答（docs.csv.output）',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_kb_en_doc_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 英文知识库 docs.csv';
