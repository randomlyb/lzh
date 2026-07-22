package com.travlejava.taskscheduler.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String host;
    private int port;
    private String instanceId;
}
