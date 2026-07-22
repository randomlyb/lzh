CREATE DATABASE IF NOT EXISTS task_scheduler DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE task_scheduler;

-- ==========================================
-- 1. 任务 (Job)
-- ==========================================
CREATE TABLE IF NOT EXISTS job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    job_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    trigger_type VARCHAR(50) NOT NULL COMMENT '触发类型 CRON/FIXED_RATE',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    fixed_rate BIGINT COMMENT '固定频率（毫秒）',
    executor_handler VARCHAR(255) NOT NULL COMMENT '执行器处理器',
    executor_params TEXT COMMENT '执行器参数',
    route_strategy VARCHAR(50) NOT NULL COMMENT '路由策略 RANDOM/ROUND_ROBIN',
    max_retry_count INT DEFAULT 3 COMMENT '最大重试次数',
    retry_delay BIGINT DEFAULT 5000 COMMENT '重试延迟（毫秒）',
    max_concurrent INT DEFAULT 1 COMMENT '最大并发数',
    status VARCHAR(50) DEFAULT 'RUNNING' COMMENT '任务状态 RUNNING/PAUSED',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_name (job_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ==========================================
-- 2. 任务日志 (JobLog)
-- ==========================================
CREATE TABLE IF NOT EXISTS job_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    job_id BIGINT NOT NULL COMMENT '任务ID',
    executor_address VARCHAR(255) COMMENT '执行器地址',
    trigger_time DATETIME COMMENT '触发时间',
    start_time DATETIME COMMENT '开始执行时间',
    end_time DATETIME COMMENT '结束执行时间',
    status VARCHAR(50) NOT NULL COMMENT '日志状态 SUCCESS/FAILED/RUNNING',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    INDEX idx_job_id (job_id),
    INDEX idx_trigger_time (trigger_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';

-- 添加外键约束 (可选，但推荐用于数据完整性)
ALTER TABLE job_log ADD CONSTRAINT fk_job_log_job_id FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE;
