package com.travlejava.taskscheduler.service.dto;

import lombok.Data;

@Data
public class JobStatisticsDTO {
    private Long total;
    private Long success;
    private Long failed;
    private Double successRate;
}