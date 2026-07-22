package com.travlejava.taskscheduler.service.dto;

import com.travlejava.taskscheduler.common.enums.RouteStrategy;
import com.travlejava.taskscheduler.common.enums.TriggerType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String jobName;
    private TriggerType triggerType;
    private String cronExpression;
    private Long fixedRate;
    private String executorHandler;
    private String executorParams;
    private RouteStrategy routeStrategy;
    private Integer maxRetryCount;
    private Long retryDelay;
    private Integer maxConcurrent;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
