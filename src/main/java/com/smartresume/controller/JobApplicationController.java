package com.smartresume.controller;

import com.smartresume.model.JobApplication;
import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import com.smartresume.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService applicationService;
    private final UserRepository userRepository;

    /**
     * Apply to a job (Authenticated users)
     */
    @PostMapping
    public ResponseEntity<?> applyToJob(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jobId = request.get("jobId");
        String resumeId = request.get("resumeId");

        // Validation
        if (jobId == null || jobId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job ID is required"));
        }
        if (resumeId == null || resumeId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Resume ID is required"));
        }

        try {
            JobApplication application = applicationService.applyToJob(user.getId(), jobId, resumeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to apply to job: " + e.getMessage()));
        }
    }

    /**
     * Get current user's applications
     */
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            List<JobApplication> applications = applicationService.getUserApplications(user.getId());
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch applications: " + e.getMessage()));
        }
    }

    /**
     * Get all applications for a specific job (Admin only)
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getJobApplications(@PathVariable String jobId) {
        try {
            List<JobApplication> applications = applicationService.getJobApplications(jobId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch applications: " + e.getMessage()));
        }
    }

    /**
     * Get application by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApplicationById(@PathVariable String id, Authentication auth) {
        try {
            return applicationService.getApplicationById(id)
                    .map(application -> {
                        // Verify user owns this application or is admin
                        String email = auth.getName();
                        User user = userRepository.findByEmail(email).orElseThrow();

                        if (!application.getUserId().equals(user.getId()) && !user.getRole().equals("ROLE_ADMIN")) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Access denied"));
                        }

                        return ResponseEntity.ok((Object) application);
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Application not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch application: " + e.getMessage()));
        }
    }

    /**
     * Update application status (Admin only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        try {
            JobApplication updatedApplication = applicationService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(updatedApplication);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update status: " + e.getMessage()));
        }
    }

    /**
     * Delete application (User can delete their own, Admin can delete any)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApplication(@PathVariable String id, Authentication auth) {
        try {
            JobApplication application = applicationService.getApplicationById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElseThrow();

            // Check if user owns this application or is admin
            if (!application.getUserId().equals(user.getId()) && !user.getRole().equals("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            applicationService.deleteApplication(id);
            return ResponseEntity.ok(Map.of("message", "Application deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete application: " + e.getMessage()));
        }
    }
}
