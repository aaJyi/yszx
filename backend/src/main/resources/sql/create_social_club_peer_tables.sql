-- 病友社交：用户关注、病友私信、NCSS 社团划分快照（执行前请确认库名与字符集）

-- 用户关注（单向）；互相关注后方可发起病友聊天
CREATE TABLE IF NOT EXISTS social_user_follow (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  follower_user_id BIGINT NOT NULL COMMENT '关注方用户ID',
  followee_user_id BIGINT NOT NULL COMMENT '被关注方用户ID',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow (follower_user_id, followee_user_id),
  KEY idx_followee (followee_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交用户关注';

-- 病友私信（简化存储，轮询拉取；不做已读回执亦可后续扩展）
CREATE TABLE IF NOT EXISTS social_peer_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  from_user_id BIGINT NOT NULL COMMENT '发送方',
  to_user_id BIGINT NOT NULL COMMENT '接收方',
  content VARCHAR(2000) NOT NULL COMMENT '文本内容',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_pair_time (from_user_id, to_user_id, created_at),
  KEY idx_to (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病友交流私信';

-- NCSS 一次完整计算结果（含图数据 JSON，供管理端可视化）
CREATE TABLE IF NOT EXISTS club_division_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  similarity_threshold DECIMAL(5,4) NOT NULL DEFAULT 0.3000 COMMENT 'Jaccard 建边阈值',
  user_count INT NOT NULL DEFAULT 0 COMMENT '参与计算的用户数',
  club_count INT NOT NULL DEFAULT 0 COMMENT '社团数',
  result_json LONGTEXT NOT NULL COMMENT 'run_ncss_json.py 完整输出 JSON',
  error_message VARCHAR(1024) DEFAULT NULL COMMENT '失败原因（成功为空）',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NCSS 社团划分快照';

-- 快照内用户归属（便于 SQL 查询推荐与管理端列表）
CREATE TABLE IF NOT EXISTS club_division_member (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  snapshot_id BIGINT NOT NULL COMMENT '快照ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  club_index INT NOT NULL COMMENT '社团序号（从0开始）',
  centrality DECIMAL(16,10) DEFAULT NULL COMMENT 'PRcen 中心度',
  PRIMARY KEY (id),
  UNIQUE KEY uk_snap_user (snapshot_id, user_id),
  KEY idx_snap_club (snapshot_id, club_index),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社团成员（按快照）';
