package com.travlejava.taskscheduler.common.model;

import lombok.Data;
import java.io.Serializable;

@Data
public class TaskExecuteParam implements Serializable {
    private Long jobId;
    private String executorHandler;
    private String executorParams;
    private Long logId;
}
