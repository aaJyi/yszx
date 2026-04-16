-- 若曾增加 instruction、source_file（为 drug.csv 准备），可执行本脚本恢复为仅 drug.json 三列 + 时间
-- 表已是简化结构时可忽略

ALTER TABLE rag_kb_drug
    DROP COLUMN instruction,
    DROP COLUMN source_file;
