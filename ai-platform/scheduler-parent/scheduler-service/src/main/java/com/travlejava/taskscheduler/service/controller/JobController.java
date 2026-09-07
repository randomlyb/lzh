package com.travlejava.taskscheduler.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travlejava.taskscheduler.common.entity.BlogArticle;
import com.travlejava.taskscheduler.common.entity.JobLog;
import com.travlejava.taskscheduler.common.enums.LogStatus;
import com.travlejava.taskscheduler.service.JobLogService;
import com.travlejava.taskscheduler.service.JobService;
import com.travlejava.taskscheduler.service.JobStatisticsService;
import com.travlejava.taskscheduler.service.dto.JobCreateRequest;
import com.travlejava.taskscheduler.service.dto.JobLogResponse;
import com.travlejava.taskscheduler.service.dto.JobResponse;
import com.travlejava.taskscheduler.service.dto.JobStatisticsDTO;
import com.travlejava.taskscheduler.service.dto.JobUpdateRequest;
import com.travlejava.taskscheduler.service.mapper.BlogArticleMapper;
import com.travlejava.taskscheduler.service.mapper.JobLogMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobLogService jobLogService;
    private final JobLogMapper jobLogMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final JobStatisticsService jobStatisticsService;

    public JobController(JobService jobService, JobLogService jobLogService, JobLogMapper jobLogMapper, BlogArticleMapper blogArticleMapper, JobStatisticsService jobStatisticsService) {
        this.jobService = jobService;
        this.jobLogService = jobLogService;
        this.jobLogMapper = jobLogMapper;
        this.blogArticleMapper = blogArticleMapper;
        this.jobStatisticsService = jobStatisticsService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@RequestBody JobCreateRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @PutMapping
    public ResponseEntity<JobResponse> updateJob(@RequestBody JobUpdateRequest request) {
        return ResponseEntity.ok(jobService.updateJob(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable Long id) {
        jobService.pauseJob(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resumeJob(@PathVariable Long id) {
        jobService.resumeJob(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<JobLogResponse>> getJobLogs(@PathVariable Long id) {
        List<JobLog> logs = jobLogMapper.selectList(
                new LambdaQueryWrapper<JobLog>()
                        .eq(JobLog::getJobId, id)
                        .orderByDesc(JobLog::getCreateTime)
        );
        List<JobLogResponse> responses = logs.stream().map(log -> {
            JobLogResponse response = new JobLogResponse();
            response.setId(log.getId());
            response.setJobId(log.getJobId());
            response.setExecutorAddress(log.getExecutorAddress());
            response.setTriggerTime(log.getTriggerTime() != null ? log.getTriggerTime() : LocalDateTime.now());
            response.setStartTime(log.getStartTime() != null ? log.getStartTime() : LocalDateTime.now());
            response.setEndTime(log.getEndTime() != null ? log.getEndTime() : LocalDateTime.now());
            response.setStatus(log.getStatus() != null ? log.getStatus() : LogStatus.RUNNING);
            response.setErrorMessage(log.getErrorMessage());
            response.setRetryCount(log.getRetryCount() != null ? log.getRetryCount() : 0);
            return response;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/executors")
    public ResponseEntity<List<Map<String, String>>> getExecutors() {
        List<Map<String, String>> executors = List.of(
                Map.of("value", "spiderCollectJobExecutor", "label", "博客采集", "desc", "从博客园采集技术文章"),
                Map.of("value", "blogSummaryJobExecutor", "label", "AI摘要生成", "desc", "为文章生成智能摘要和标签"),
                Map.of("value", "blogToKnowledgeSyncJobExecutor", "label", "博客同步到知识库", "desc", "将采集的博客导入AI知识库"),
                Map.of("value", "knowledgeSyncJobExecutor", "label", "GitHub仓库同步", "desc", "同步GitHub仓库文档到知识库"),
                Map.of("value", "pdfParseJobExecutor", "label", "PDF解析", "desc", "解析PDF文档内容"),
                Map.of("value", "chunkSplitJobExecutor", "label", "文本切片", "desc", "将文本切分为片段并索引到ES")
        );
        return ResponseEntity.ok(executors);
    }

    @GetMapping("/blogs")
    public ResponseEntity<List<BlogArticle>> getBlogs(@RequestParam(defaultValue = "20") int limit) {
        List<BlogArticle> blogs = blogArticleMapper.selectList(
                new LambdaQueryWrapper<BlogArticle>()
                        .orderByDesc(BlogArticle::getCollectedTime)
                        .last("LIMIT " + limit)
        );
        return ResponseEntity.ok(blogs);
    }

    // ==================== 任务监控统计接口 ====================

    @GetMapping("/statistics")
    public ResponseEntity<JobStatisticsDTO> getJobStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return ResponseEntity.ok(jobStatisticsService.getJobStatistics(startTime, endTime));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getJobExecutionTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(jobStatisticsService.getJobExecutionTrend(days));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<Map<String, Object>>> getFailedJobs() {
        return ResponseEntity.ok(jobStatisticsService.getFailedJobs());
    }

    @GetMapping("/duration")
    public ResponseEntity<List<Map<String, Object>>> getJobDurationStatistics() {
        return ResponseEntity.ok(jobStatisticsService.getJobDurationStatistics());
    }
}