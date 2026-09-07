package com.travlejava.taskscheduler.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travlejava.taskscheduler.common.entity.Job;
import com.travlejava.taskscheduler.common.enums.TriggerType;
import com.travlejava.taskscheduler.service.job.BaseJob;
import com.travlejava.taskscheduler.service.mapper.JobMapper;
import com.travlejava.taskscheduler.service.dto.JobCreateRequest;
import com.travlejava.taskscheduler.service.dto.JobUpdateRequest;
import com.travlejava.taskscheduler.service.dto.JobResponse;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService extends ServiceImpl<JobMapper, Job> {

    private final Scheduler scheduler;

    public JobService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restoreJobsOnStartup() {
        try {
            List<Job> runningJobs = list().stream()
                    .filter(j -> "RUNNING".equals(j.getStatus()))
                    .collect(Collectors.toList());
            for (Job job : runningJobs) {
                try {
                    scheduleJob(job);
                } catch (SchedulerException e) {
                    logger.error("Failed to restore job {} on startup", job.getId(), e);
                }
            }
            if (!runningJobs.isEmpty()) {
                logger.info("Restored {} running jobs from database", runningJobs.size());
            }
        } catch (Exception e) {
            logger.error("Failed to restore jobs on startup", e);
        }
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JobService.class);

    public JobResponse createJob(JobCreateRequest request) {
        Job job = new Job();
        BeanUtils.copyProperties(request, job);
        job.setStatus("RUNNING"); // Default status
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        save(job); // Save to DB first to get ID

        try {
            scheduleJob(job);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule job in Quartz", e);
        }

        return convertToResponse(job);
    }

    public JobResponse updateJob(JobUpdateRequest request) {
        Job job = getById(request.getId());
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        BeanUtils.copyProperties(request, job);
        job.setUpdateTime(LocalDateTime.now());
        updateById(job);

        try {
            unscheduleJob(job.getId());
            scheduleJob(job);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to update job in Quartz", e);
        }
        return convertToResponse(job);
    }

    public JobResponse getJobById(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        return convertToResponse(job);
    }

    public List<JobResponse> getAllJobs() {
        return list().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public void deleteJob(Long id) {
        removeById(id);
        try {
            unscheduleJob(id);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to delete job from Quartz", e);
        }
    }

    public void pauseJob(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        job.setStatus("PAUSED");
        job.setUpdateTime(LocalDateTime.now());
        updateById(job);
        try {
            scheduler.pauseJob(JobKey.jobKey(String.valueOf(id)));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to pause job in Quartz", e);
        }
    }

    public void resumeJob(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
        job.setStatus("RUNNING");
        job.setUpdateTime(LocalDateTime.now());
        updateById(job);
        try {
            scheduler.resumeJob(JobKey.jobKey(String.valueOf(id)));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to resume job in Quartz", e);
        }
    }

    private JobResponse convertToResponse(Job job) {
        JobResponse response = new JobResponse();
        BeanUtils.copyProperties(job, response);
        return response;
    }

    private void scheduleJob(Job job) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(job);
        Trigger trigger = buildTrigger(job);
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void unscheduleJob(Long jobId) throws SchedulerException {
        scheduler.deleteJob(JobKey.jobKey(String.valueOf(jobId)));
    }

    private JobDetail buildJobDetail(Job job) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", job.getId());
        jobDataMap.put("executorHandler", job.getExecutorHandler());
        jobDataMap.put("executorParams", job.getExecutorParams());

        return JobBuilder.newJob(BaseJob.class)
                .withIdentity(String.valueOf(job.getId()))
                .withDescription(job.getJobName())
                .usingJobData(jobDataMap)
                .build();
    }

    private Trigger buildTrigger(Job job) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(String.valueOf(job.getId()))
                .withDescription(job.getJobName());

        if (job.getTriggerType() == TriggerType.CRON) {
            triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()));
        } else if (job.getTriggerType() == TriggerType.FIXED_RATE) {
            triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(job.getFixedRate())
                    .repeatForever());
        } else {
            throw new IllegalArgumentException("Unknown trigger type: " + job.getTriggerType());
        }

        return triggerBuilder.build();
    }
}