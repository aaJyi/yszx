CREATE TABLE IF NOT EXISTS user_club_profile (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    archive_id      INT           NULL COMMENT '关联档案ID',
    analysis_id     BIGINT        NULL COMMENT '关联分析ID',
    profile_version VARCHAR(32)   NOT NULL DEFAULT 'v1' COMMENT '画像版本',
    feature_json    LONGTEXT      NOT NULL COMMENT '五层画像JSON',
    vector_json     VARCHAR(2048) NOT NULL COMMENT '向量JSON',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id),
    KEY idx_archive (archive_id),
    KEY idx_analysis (analysis_id),
    KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户社团画像';

