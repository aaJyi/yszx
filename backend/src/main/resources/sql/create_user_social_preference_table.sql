CREATE TABLE IF NOT EXISTS user_social_preference (
    id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id                   BIGINT       NOT NULL COMMENT '用户ID',
    allow_recommend           TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '允许被推荐',
    allow_disease_based_match TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '允许按疾病画像匹配',
    active_time_window        VARCHAR(32)  NULL COMMENT '活跃时间窗',
    reply_style               VARCHAR(32)  NULL COMMENT '回复风格',
    blocked_user_ids          VARCHAR(1024) NULL COMMENT '屏蔽用户ID列表（逗号分隔）',
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户社交偏好';

