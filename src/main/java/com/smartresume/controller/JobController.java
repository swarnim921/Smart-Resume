package com.smartresume.controller;

import com.smartresume.model.Job;
import com.smartresume.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllActiveJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable String id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> createJob(@RequestBody Job job, Authentication auth) {
        String recruiterEmail = auth.getName();
        Job createdJob = jobService.createJob(job, recruiterEmail);
        return ResponseEntity.ok(createdJob);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> updateJob(@PathVariable String id, @RequestBody Job job, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            Job updatedJob = jobService.updateJob(id, job, recruiterEmail);
            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteJob(@PathVariable String id, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            jobService.deleteJob(id, recruiterEmail);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<Job>> getMyJobs(Authentication auth) {
        String recruiterEmail = auth.getName();
        return ResponseEntity.ok(jobService.getJobsByRecruiter(recruiterEmail));
    }
}
