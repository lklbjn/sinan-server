CREATE TABLE feedback (
  id varchar(36) NOT NULL COMMENT '反馈ID',
  user_id varchar(36) DEFAULT NULL COMMENT '用户ID',
  contact varchar(100) NOT NULL COMMENT '联系方式',
  content text NOT NULL COMMENT '反馈内容',
  status int NOT NULL DEFAULT 0 COMMENT '处理状态',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time),
  KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈表';