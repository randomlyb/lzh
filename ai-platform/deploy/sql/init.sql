-- =============================================
-- Database: ai_knowledge_platform
-- Used by: ai-auth-service, ai-service
-- =============================================
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS ai_knowledge_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_knowledge_platform;

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(500) COMMENT '头像URL',
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time DATETIME NULL,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色描述',
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    description VARCHAR(200) COMMENT '权限描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 文档元数据表
DROP TABLE IF EXISTS ai_document_metadata;
CREATE TABLE ai_document_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) COMMENT '文档标题',
    description TEXT COMMENT '文档描述',
    file_path VARCHAR(1000) COMMENT '文件存储路径',
    file_name VARCHAR(500) COMMENT '原始文件名',
    file_type VARCHAR(200) COMMENT '文件类型',
    status VARCHAR(20) DEFAULT 'UPLOADED' COMMENT '状态: UPLOADED, PARSING, PARSED, FAILED',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    knowledge_base_id BIGINT COMMENT '所属知识库ID',
    INDEX idx_knowledge_base_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档元数据表';

-- 文档标签表
DROP TABLE IF EXISTS ai_document_tag;
CREATE TABLE ai_document_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '文档ID',
    tag VARCHAR(100) NOT NULL COMMENT '标签',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档标签表';

-- 文档分块表
DROP TABLE IF EXISTS ai_document_chunk;
CREATE TABLE ai_document_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '文档ID',
    chunk_no INT NOT NULL COMMENT '分块序号',
    content TEXT COMMENT '分块内容',
    token_count INT COMMENT 'Token数量',
    embedding_status INT DEFAULT 0 COMMENT '0-未向量化 1-已向量化',
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块表';

-- 知识库表
DROP TABLE IF EXISTS ai_knowledge_base;
CREATE TABLE ai_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '知识库名称',
    description VARCHAR(1000) COMMENT '知识库描述',
    owner_id BIGINT COMMENT '所属用户ID',
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 知识库文档表
DROP TABLE IF EXISTS ai_knowledge_document;
CREATE TABLE ai_knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库ID',
    title VARCHAR(500) COMMENT '文档标题',
    source_type VARCHAR(50) COMMENT '来源类型: FILE, URL, MANUAL',
    source_url VARCHAR(1000) COMMENT '来源URL',
    content_hash VARCHAR(100) COMMENT '内容哈希',
    status INT DEFAULT 0 COMMENT '0-待处理 1-处理中 2-已完成 3-失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_knowledge_base_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- 连接器表
DROP TABLE IF EXISTS ai_connector;
CREATE TABLE ai_connector (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL COMMENT '连接器类型',
    config_json TEXT COMMENT '配置JSON',
    last_sync_time DATETIME NULL,
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器表';

-- AI对话历史表
DROP TABLE IF EXISTS ai_chat_history;
CREATE TABLE ai_chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    question TEXT COMMENT '问题',
    answer TEXT COMMENT '回答',
    source_ids VARCHAR(1000) COMMENT '来源文档ID列表',
    token_usage INT COMMENT 'Token使用量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史表';


-- =============================================
-- Database: task_scheduler
-- Used by: scheduler-service
-- =============================================
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS task_scheduler DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE task_scheduler;

-- 任务表
DROP TABLE IF EXISTS job;
CREATE TABLE job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发类型: CRON, FIXED_RATE',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    fixed_rate BIGINT COMMENT '固定频率(毫秒)',
    executor_handler VARCHAR(100) COMMENT '执行器处理器名称',
    executor_params TEXT COMMENT '执行器参数(JSON)',
    route_strategy VARCHAR(20) COMMENT '路由策略',
    max_retry_count INT DEFAULT 3 COMMENT '最大重试次数',
    retry_delay BIGINT DEFAULT 5000 COMMENT '重试延迟(毫秒)',
    max_concurrent INT DEFAULT 1 COMMENT '最大并发数',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态: RUNNING, PAUSED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 任务日志表
DROP TABLE IF EXISTS job_log;
CREATE TABLE job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL COMMENT '任务ID',
    executor_address VARCHAR(200) COMMENT '执行器地址',
    trigger_time DATETIME COMMENT '触发时间',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status VARCHAR(20) COMMENT '状态: RUNNING, SUCCESS, FAILED, RETRYING',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';

-- 博客文章表
DROP TABLE IF EXISTS ai_blog_article;
CREATE TABLE ai_blog_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) COMMENT '文章标题',
    summary TEXT COMMENT '文章摘要',
    source_url VARCHAR(500) COMMENT '原文URL',
    tags VARCHAR(500) COMMENT '标签',
    collected_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_url (source_url(200))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章表';

SET FOREIGN_KEY_CHECKS = 1;