package com.smartresume.controller;

import com.smartresume.model.Application;
import com.smartresume.model.Job;
import com.smartresume.service.ApplicationService;
import com.smartresume.service.EmailService;
import com.smartresume.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    private final EmailService emailService;
    private final JobService jobService;

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
            String interviewDateTime = request.get("interviewDateTime");
            String recruiterEmail = auth.getName();

            Application application = applicationService.updateApplicationStatus(id, status, notes, recruiterEmail);

            // Set interview date/time if provided
            if (interviewDateTime != null && !interviewDateTime.isEmpty()) {
                try {
                    java.time.LocalDateTime dt = java.time.LocalDateTime.parse(interviewDateTime);
                    application.setInterviewDateTime(dt);
                    // Save again with interview time
                } catch (Exception ignored) {
                }
            }

            // Send status update email with optional ICS attachment
            try {
                String jobTitle = jobService.getJobById(application.getJobId())
                        .map(Job::getTitle).orElse("the position");
                String jobCompany = jobService.getJobById(application.getJobId())
                        .map(Job::getCompany).orElse("the company");
                if ("INTERVIEW".equals(status) && interviewDateTime != null && !interviewDateTime.isEmpty()) {
                    emailService.sendInterviewEmailWithICS(
                            application.getCandidateEmail(),
                            application.getCandidateName(),
                            jobTitle, jobCompany, interviewDateTime, notes);
                } else {
                    Job fullJob = jobService.getJobById(application.getJobId()).orElse(null);
                    emailService.sendStatusUpdateEmailWithJob(
                            application.getCandidateEmail(),
                            application.getCandidateName(),
                            fullJob, status, status, notes);
                }
            } catch (Exception emailEx) {
                System.err.println("Email failed after status update: " + emailEx.getMessage());
            }

            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk-status-update")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> bulkUpdateStatus(@RequestBody Map<String, Object> request, Authentication auth) {
        try {
            Object idsObj = request.get("ids");
            List<String> ids = new ArrayList<>();
            if (idsObj instanceof List<?>) {
                for (Object o : (List<?>) idsObj) {
                    if (o instanceof String)
                        ids.add((String) o);
                }
            }
            String status = (String) request.get("status");
            String stageName = (String) request.get("stageName");
            String notes = (String) request.get("notes");
            String recruiterEmail = auth.getName();

            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No application IDs provided"));
            }

            List<Application> updated = applicationService.updateBulkStatus(ids, status, stageName, notes,
                    recruiterEmail);
            return ResponseEntity.ok(Map.of("message", "Successfully updated " + updated.size() + " applications",
                    "updatedCount", updated.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reanalyze")
    public ResponseEntity<?> reAnalyze(@PathVariable String id, Authentication auth) {
        try {
            String requesterEmail = auth.getName();
            Application application = applicationService.reAnalyze(id, requesterEmail);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/job/{jobId}/export.csv")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> exportCsv(@PathVariable String jobId, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            return applicationService.exportCsv(jobId, recruiterEmail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/resume")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getApplicationResume(@PathVariable String id, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            return applicationService.getApplicationResume(id, recruiterEmail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> withdrawApplication(@PathVariable String id, Authentication auth) {
        try {
            String candidateEmail = auth.getName();
            applicationService.withdrawApplication(id, candidateEmail);
            return ResponseEntity.ok(Map.of("message", "Application withdrawn successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
