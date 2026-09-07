package com.travlejava.taskscheduler.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.taskscheduler.common.entity.JobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface JobLogMapper extends BaseMapper<JobLog> {

    @Select("<script>" +
            "SELECT " +
            "  COUNT(*) as total, " +
            "  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success, " +
            "  SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM job_log " +
            "<where>" +
            "  <if test='startTime != null'>AND trigger_time &gt;= #{startTime}</if>" +
            "  <if test='endTime != null'>AND trigger_time &lt;= #{endTime}</if>" +
            "</where>" +
            "</script>")
    Map<String, Object> selectStatistics(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Select("<script>" +
            "SELECT " +
            "  DATE_FORMAT(trigger_time, '%m-%d') as date, " +
            "  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success, " +
            "  SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM job_log " +
            "WHERE trigger_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY DATE_FORMAT(trigger_time, '%m-%d') " +
            "ORDER BY date ASC" +
            "</script>")
    List<Map<String, Object>> selectDailyStatistics(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Select("SELECT " +
            "  jl.job_id as jobId, " +
            "  j.job_name as jobName, " +
            "  COUNT(*) as failedCount, " +
            "  jl.error_message as lastError, " +
            "  AVG(TIMESTAMPDIFF(MILLISECOND, jl.start_time, jl.end_time)) as avgDuration " +
            "FROM job_log jl " +
            "LEFT JOIN job j ON jl.job_id = j.id " +
            "WHERE jl.status = 'FAILED' " +
            "GROUP BY jl.job_id, j.job_name, jl.error_message " +
            "ORDER BY failedCount DESC")
    List<Map<String, Object>> selectFailedJobsStatistics();

    @Select("SELECT " +
            "  jl.job_id as jobId, " +
            "  j.job_name as jobName, " +
            "  AVG(TIMESTAMPDIFF(MICROSECOND, jl.start_time, jl.end_time) / 1000) as avgDuration, " +
            "  COUNT(*) as executionCount " +
            "FROM job_log jl " +
            "LEFT JOIN job j ON jl.job_id = j.id " +
            "WHERE jl.start_time IS NOT NULL AND jl.end_time IS NOT NULL " +
            "GROUP BY jl.job_id, j.job_name " +
            "ORDER BY avgDuration DESC")
    List<Map<String, Object>> selectDurationStatistics();
}