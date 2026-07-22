package com.travlejava.taskscheduler.service.dto;

import com.travlejava.taskscheduler.common.enums.LogStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JobLogResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long jobId;
    private String executorAddress;
    private LocalDateTime triggerTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LogStatus status;
    private String errorMessage;
    private Integer retryCount;
}
