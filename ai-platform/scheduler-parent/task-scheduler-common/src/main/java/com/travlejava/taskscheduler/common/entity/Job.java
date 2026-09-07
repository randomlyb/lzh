package com.travlejava.taskscheduler.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.travlejava.taskscheduler.common.enums.JobStatus;
import com.travlejava.taskscheduler.common.enums.RouteStrategy;
import com.travlejava.taskscheduler.common.enums.TriggerType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private TriggerType triggerType;
    private String cronExpression;
    private Long fixedRate;
    private String executorHandler;
    private String executorParams;
    private RouteStrategy routeStrategy;
    private Integer maxRetryCount = 3;
    private Long retryDelay = 5000L;
    private Integer maxConcurrent = 1;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}