package com.travlejava.taskscheduler.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travlejava.taskscheduler.common.entity.JobLog;
import com.travlejava.taskscheduler.common.enums.LogStatus;
import com.travlejava.taskscheduler.service.mapper.JobLogMapper;
import com.travlejava.taskscheduler.service.mq.RabbitMQProducerService;
import com.travlejava.taskscheduler.common.mq.JobStatusNotification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class JobLogService extends ServiceImpl<JobLogMapper, JobLog> {

    private final RabbitMQProducerService rabbitMQProducerService;

    public JobLogService(RabbitMQProducerService rabbitMQProducerService) {
        this.rabbitMQProducerService = rabbitMQProducerService;
    }

    public void recordJobLog(Long jobId, String executorAddress, LocalDateTime triggerTime, 
                             LocalDateTime startTime, LocalDateTime endTime, LogStatus status, String message) {
        JobLog jobLog = new JobLog();
        jobLog.setJobId(jobId);
        jobLog.setExecutorAddress(executorAddress);
        jobLog.setTriggerTime(triggerTime);
        jobLog.setStartTime(startTime);
        jobLog.setEndTime(endTime);
        jobLog.setStatus(status);
        jobLog.setErrorMessage(message);
        jobLog.setCreateTime(LocalDateTime.now());
        // You might want to track retry count in JobLog as well
        // jobLog.setRetryCount(retryCount);
        save(jobLog);

        // Send notification to ai-service
        JobStatusNotification notification = new JobStatusNotification(jobId, null, status.name(), message);
        rabbitMQProducerService.sendJobStatusNotification(notification);
    }
}
