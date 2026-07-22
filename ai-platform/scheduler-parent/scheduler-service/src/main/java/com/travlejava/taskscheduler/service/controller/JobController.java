package com.travlejava.taskscheduler.service.controller;

import com.travlejava.taskscheduler.service.JobService;
import com.travlejava.taskscheduler.service.dto.JobCreateRequest;
import com.travlejava.taskscheduler.service.dto.JobResponse;
import com.travlejava.taskscheduler.service.dto.JobUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
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
}
