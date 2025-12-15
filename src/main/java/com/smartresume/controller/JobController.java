package com.smartresume.controller;

import com.smartresume.model.Job;
import com.smartresume.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * Create a new job (Admin or Recruiter)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<?> createJob(@RequestBody Job job) {
        // Validation
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job title is required"));
        }
        if (job.getDescription() == null || job.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job description is required"));
        }
        if (job.getCompany() == null || job.getCompany().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Company name is required"));
        }

        try {
            Job createdJob = jobService.createJob(job);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdJob);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create job: " + e.getMessage()));
        }
    }

    /**
     * Get all jobs (Public access)
     */
    @GetMapping
    public ResponseEntity<?> getAllJobs() {
        try {
            List<Job> jobs = jobService.getAllJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch jobs: " + e.getMessage()));
        }
    }

    /**
     * Get job by ID (Public access)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable String id) {
        try {
            return jobService.getJobById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch job: " + e.getMessage()));
        }
    }

    /**
     * Update job (Admin or Recruiter)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<?> updateJob(@PathVariable String id, @RequestBody Job job) {
        try {
            Job updatedJob = jobService.updateJob(id, job);
            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update job: " + e.getMessage()));
        }
    }

    /**
     * Delete job (Admin or Recruiter)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<?> deleteJob(@PathVariable String id) {
        try {
            jobService.deleteJob(id);
            return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete job: " + e.getMessage()));
        }
    }

    /**
     * Search jobs by keyword (Public access)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search keyword is required"));
        }

        try {
            List<Job> jobs = jobService.searchJobs(keyword);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search jobs: " + e.getMessage()));
        }
    }
}
