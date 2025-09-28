-- 创建忽略组表
CREATE TABLE sn_ignored_groups (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    UNIQUE KEY unique_user_group (user_id, group_name),
    INDEX idx_user_id (user_id),
    INDEX idx_group_name (group_name),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 添加表注释
ALTER TABLE sn_ignored_groups COMMENT = '忽略组管理表';