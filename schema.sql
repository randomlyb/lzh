CREATE DATABASE IF NOT EXISTS task_scheduler DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE task_scheduler;

CREATE TABLE IF NOT EXISTS job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    job_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发类型: CRON/FIXED_RATE',
    cron_expression VARCHAR(100) COMMENT 'cron表达式',
    fixed_rate BIGINT COMMENT '固定频率(毫秒)',
    executor_handler VARCHAR(255) NOT NULL COMMENT '执行器方法名',
    executor_params TEXT COMMENT '执行参数(JSON格式)',
    route_strategy VARCHAR(50) NOT NULL COMMENT '路由策略: ROUND_ROBIN/CONSISTENT_HASH/BROADCAST',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    retry_delay BIGINT NOT NULL DEFAULT 5000 COMMENT '重试延迟(毫秒)',
    max_concurrent INT NOT NULL DEFAULT 1 COMMENT '最大并发数',
    status VARCHAR(20) NOT NULL COMMENT '任务状态: RUNNING/PAUSED',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

CREATE TABLE IF NOT EXISTS job_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    job_id BIGINT NOT NULL COMMENT '关联任务ID',
    executor_address VARCHAR(255) COMMENT '执行任务的节点地址',
    trigger_time DATETIME COMMENT '触发时间',
    start_time DATETIME COMMENT '执行开始时间',
    end_time DATETIME COMMENT '执行结束时间',
    status VARCHAR(20) NOT NULL COMMENT '执行状态: RUNNING/SUCCESS/FAILED/RETRYING',
    error_message TEXT COMMENT '失败时的错误信息',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    INDEX idx_job_id (job_id),
    INDEX idx_trigger_time (trigger_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';
