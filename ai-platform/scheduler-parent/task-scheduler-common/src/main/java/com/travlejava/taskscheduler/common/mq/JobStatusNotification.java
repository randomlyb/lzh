package com.travlejava.taskscheduler.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusNotification implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long jobId;
    private Long documentId; // If applicable
    private String status; // SUCCESS, FAILED
    private String message;
}
