package com.travlejava.taskscheduler.service;

import com.travlejava.taskscheduler.common.entity.Job;
import com.travlejava.taskscheduler.service.dto.JobStatisticsDTO;
import com.travlejava.taskscheduler.service.mapper.JobLogMapper;
import com.travlejava.taskscheduler.service.mapper.JobMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class JobStatisticsService {

    private final JobLogMapper jobLogMapper;
    private final JobMapper jobMapper;

    public JobStatisticsService(JobLogMapper jobLogMapper, JobMapper jobMapper) {
        this.jobLogMapper = jobLogMapper;
        this.jobMapper = jobMapper;
    }

    /**
     * 获取任务执行统计（使用数据库聚合查询）
     */
    public JobStatisticsDTO getJobStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = jobLogMapper.selectStatistics(startTime, endTime);

        long total = result.get("total") != null ? ((Number) result.get("total")).longValue() : 0;
        long success = result.get("success") != null ? ((Number) result.get("success")).longValue() : 0;
        long failed = result.get("failed") != null ? ((Number) result.get("failed")).longValue() : 0;
        double successRate = total > 0 ? (double) success / total * 100 : 0;

        JobStatisticsDTO dto = new JobStatisticsDTO();
        dto.setTotal(total);
        dto.setSuccess(success);
        dto.setFailed(failed);
        dto.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
        return dto;
    }

    /**
     * 获取任务执行趋势（最近N天，使用数据库聚合查询）
     */
    public List<Map<String, Object>> getJobExecutionTrend(int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        // 使用数据库聚合查询
        List<Map<String, Object>> dbResult = jobLogMapper.selectDailyStatistics(startTime, endTime);

        // 补充缺失的日期（数据库可能没有某些日期的数据）
        Map<String, Map<String, Object>> dateMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date = startTime.toLocalDate(); !date.isAfter(endTime.toLocalDate()); date = date.plusDays(1)) {
            String dateStr = date.format(formatter);
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", dateStr);
            dayStat.put("success", 0L);
            dayStat.put("failed", 0L);
            dateMap.put(dateStr, dayStat);
        }

        // 填充数据库返回的数据
        for (Map<String, Object> item : dbResult) {
            String date = (String) item.get("date");
            if (dateMap.containsKey(date)) {
                dateMap.get(date).put("success", ((Number) item.get("success")).longValue());
                dateMap.get(date).put("failed", ((Number) item.get("failed")).longValue());
            }
        }

        return new ArrayList<>(dateMap.values());
    }

    /**
     * 获取失败任务详情（使用数据库聚合查询）
     */
    public List<Map<String, Object>> getFailedJobs() {
        // 使用数据库聚合查询，直接返回统计结果
        List<Map<String, Object>> dbResult = jobLogMapper.selectFailedJobsStatistics();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : dbResult) {
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("jobId", item.get("jobId"));
            formatted.put("jobName", item.get("jobName") != null ? item.get("jobName") : "Unknown");
            formatted.put("failedCount", ((Number) item.get("failedCount")).longValue());
            formatted.put("lastError", item.get("lastError"));
            formatted.put("avgDuration", item.get("avgDuration") != null ? 
                    Math.round(((Number) item.get("avgDuration")).doubleValue()) : 0);
            result.add(formatted);
        }

        return result;
    }

    /**
     * 获取任务耗时统计（使用数据库聚合查询）
     */
    public List<Map<String, Object>> getJobDurationStatistics() {
        // 使用数据库聚合查询，直接返回统计结果
        List<Map<String, Object>> dbResult = jobLogMapper.selectDurationStatistics();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : dbResult) {
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("jobId", item.get("jobId"));
            formatted.put("jobName", item.get("jobName") != null ? item.get("jobName") : "Unknown");
            formatted.put("avgDuration", item.get("avgDuration") != null ? 
                    Math.round(((Number) item.get("avgDuration")).doubleValue()) : 0);
            formatted.put("executionCount", ((Number) item.get("executionCount")).longValue());
            result.add(formatted);
        }

        return result;
    }
}