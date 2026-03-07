package com.smartresume.controller;

import com.smartresume.model.InterviewPanel;
import com.smartresume.model.Job;
import com.smartresume.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for multi-panel interview scheduling.
 *
 * POST /api/interviews/panels/{jobId} — save panel config for a job
 * GET /api/interviews/schedule/{jobId} — view current schedule
 * POST /api/interviews/auto-schedule/{jobId} — auto-assign all shortlisted
 * candidates
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class InterviewController {

    private final InterviewService interviewService;

    /** Save / replace interview panels for a job */
    @PostMapping("/panels/{jobId}")
    public ResponseEntity<?> savePanels(
            @PathVariable String jobId,
            @RequestBody List<InterviewPanel> panels,
            Authentication auth) {
        try {
            Job updatedJob = interviewService.savePanels(jobId, panels, auth.getName());
            return ResponseEntity.ok(Map.of(
                    "message", "Panels saved. " + panels.size() + " panel(s) configured.",
                    "job", updatedJob));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** View current interview schedule — all panels and their bookings */
    @GetMapping("/schedule/{jobId}")
    public ResponseEntity<?> getSchedule(
            @PathVariable String jobId,
            Authentication auth) {
        try {
            Job job = interviewService.getSchedule(jobId, auth.getName());
            List<InterviewPanel> panels = job.getInterviewPanels();
            if (panels == null || panels.isEmpty()) {
                return ResponseEntity.ok(Map.of("panels", List.of(), "message", "No panels configured"));
            }
            return ResponseEntity.ok(Map.of("panels", panels, "jobTitle", job.getTitle()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Auto-schedule ALL shortlisted candidates for a job.
     * Round-robins across panels, books slots, sends emails.
     */
    @PostMapping("/auto-schedule/{jobId}")
    public ResponseEntity<?> autoSchedule(
            @PathVariable String jobId,
            Authentication auth) {
        try {
            Map<String, Object> result = interviewService.autoSchedule(jobId, auth.getName());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
