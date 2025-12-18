package com.smartresume.controller;

import com.smartresume.model.Application;
import com.smartresume.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<?> applyToJob(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String jobId = request.get("jobId");
            String candidateEmail = auth.getName();
            Application application = applicationService.applyToJob(jobId, candidateEmail);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<Application>> getMyApplications(Authentication auth) {
        String candidateEmail = auth.getName();
        return ResponseEntity.ok(applicationService.getCandidateApplications(candidateEmail));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getJobApplications(@PathVariable String jobId, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            List<Application> applications = applicationService.getJobApplications(jobId, recruiterEmail);
            return ResponseEntity.ok(applications);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            String status = request.get("status");
            String notes = request.get("notes");
            String recruiterEmail = auth.getName();
            Application application = applicationService.updateApplicationStatus(id, status, notes, recruiterEmail);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/resume")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getApplicationResume(@PathVariable String id, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            // Get resume for this application (validates recruiter owns the job)
            return applicationService.getApplicationResume(id, recruiterEmail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
