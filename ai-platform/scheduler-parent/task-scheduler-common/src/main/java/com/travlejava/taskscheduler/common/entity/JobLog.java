package com.travlejava.taskscheduler.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.travlejava.taskscheduler.common.enums.LogStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_log")
public class JobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private String executorAddress;
    private LocalDateTime triggerTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LogStatus status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createTime;
}