package com.travlejava.taskscheduler.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExecuteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long jobId;
    private String handlerName;
    private String params;
}
