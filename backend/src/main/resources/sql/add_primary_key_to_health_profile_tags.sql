-- 为 health_profile_tags 表添加主键
-- 执行前请确保表中没有重复的 archive_id 数据

-- 1. 添加主键字段 tag_id
ALTER TABLE health_profile_tags 
ADD COLUMN tag_id INT AUTO_INCREMENT PRIMARY KEY FIRST;

-- 注意：如果表中已有数据，上面的语句会自动为现有数据生成主键ID
-- 如果执行失败，可能是因为表中已有重复的 archive_id，需要先处理数据

-- 2. 如果需要保留 archive_id 的唯一性约束，可以添加唯一索引（可选）
-- ALTER TABLE health_profile_tags 
-- ADD UNIQUE INDEX uk_archive_id (archive_id);
