package com.travlejava.taskscheduler.service.job;

import com.travlejava.taskscheduler.common.entity.ExecutorInfo;
import com.travlejava.taskscheduler.common.entity.Job;
import com.travlejava.taskscheduler.common.enums.LogStatus;
import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import com.travlejava.taskscheduler.service.JobService;
import com.travlejava.taskscheduler.service.JobLogService;
import com.travlejava.taskscheduler.service.router.ExecutorRouteService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class BaseJob extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(BaseJob.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ExecutorRouteService executorRouteService;
    @Autowired
    private JobService jobService;
    @Autowired
    private JobLogService jobLogService;

    public BaseJob() {
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Long jobId = jobDataMap.getLong("jobId");
        String executorHandler = jobDataMap.getString("executorHandler");
        String executorParams = jobDataMap.getString("executorParams");

        Job job = jobService.getById(jobId);
        if (job == null) {
            logger.error("Job not found for ID: {}", jobId);
            jobLogService.recordJobLog(jobId, "unknown", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LogStatus.FAILED, "Job not found");
            return;
        }

        Result<String> executeResult = null;
        ExecutorInfo selectedExecutor = null;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = null;
        LogStatus logStatus = LogStatus.FAILED;
        String logMessage = "";

        try {
            selectedExecutor = executorRouteService.routeExecutor(job);
            if (selectedExecutor == null) {
                throw new RuntimeException("No executor available for job: " + jobId);
            }
            logger.info("Job {} routed to executor: {}", jobId, selectedExecutor.getInstanceId());

            // For now, we assume the scheduler itself can execute the job.
            // In a real distributed system, this would involve sending a message to the remote executor.
            Executor executor = applicationContext.getBean(executorHandler, Executor.class);

            TaskExecuteParam param = new TaskExecuteParam();
            param.setJobId(jobId);
            param.setJobParam(executorParams);

            executeResult = executor.execute(param);
            endTime = LocalDateTime.now();

            if (executeResult.isSuccess()) {
                logStatus = LogStatus.SUCCESS;
                logMessage = executeResult.getMessage();
                logger.info("Job {} executed successfully on {}. Result: {}", jobId, selectedExecutor.getInstanceId(), executeResult.getMessage());
            } else {
                logStatus = LogStatus.FAILED;
                logMessage = executeResult.getMessage();
                logger.error("Job {} execution failed on {}. Error: {}", jobId, selectedExecutor.getInstanceId(), executeResult.getMessage());
            }
        } catch (Exception e) {
            endTime = LocalDateTime.now();
            logStatus = LogStatus.FAILED;
            logMessage = "Error executing job: " + e.getMessage();
            logger.error("Error executing job {}: {}", jobId, e.getMessage(), e);
            throw new JobExecutionException(e);
        } finally {
            jobLogService.recordJobLog(
                    jobId,
                    selectedExecutor != null ? selectedExecutor.getInstanceId() : "unknown",
                    LocalDateTime.ofInstant(context.getFireTime().toInstant(), ZoneOffset.UTC),
                    startTime,
                    endTime,
                    logStatus,
                    logMessage
            );
        }
    }
}