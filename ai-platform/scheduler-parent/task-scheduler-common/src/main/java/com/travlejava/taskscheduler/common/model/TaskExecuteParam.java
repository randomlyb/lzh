package com.travlejava.taskscheduler.common.model;

import lombok.Data;
import java.io.Serializable;

@Data
public class TaskExecuteParam implements Serializable {
    private Long jobId;
    private String executorHandler;
    private String executorParams;
    private Long logId;

    public String getJobParam() {
        return executorParams;
    }

    public void setJobParam(String jobParam) {
        this.executorParams = jobParam;
    }
}