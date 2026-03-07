package com.smartresume.service;

import com.smartresume.model.*;
import com.smartresume.repository.ApplicationRepository;
import com.smartresume.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles multi-panel auto interview scheduling:
 * - Recruiter defines panels with slots on a Job
 * - One call auto-assigns ALL shortlisted/pending-interview candidates
 * round-robin
 * - Sends ICS calendar invites to candidates AND all panel interviewers
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a",
            Locale.ENGLISH);

    // ─── Panel Management ───────────────────────────────────────────────────

    /**
     * Save/replace the interview panels for a job.
     * Called from recruiter dashboard when panels are configured.
     */
    public Job savePanels(String jobId, List<InterviewPanel> panels, String recruiterEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized");
        }
        // Assign UUIDs to panels and slots
        for (InterviewPanel panel : panels) {
            if (panel.getPanelId() == null || panel.getPanelId().isEmpty()) {
                panel.setPanelId(UUID.randomUUID().toString());
            }
            if (panel.getSlots() != null) {
                for (InterviewSlot slot : panel.getSlots()) {
                    if (slot.getSlotId() == null || slot.getSlotId().isEmpty()) {
                        slot.setSlotId(UUID.randomUUID().toString());
                    }
                    slot.setBooked(false);
                }
            }
        }
        job.setInterviewPanels(panels);
        return jobRepository.save(job);
    }

    /**
     * Get current interview schedule for a job (all panels + their assignments).
     */
    public Job getSchedule(String jobId, String recruiterEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized");
        }
        return job;
    }

    // ─── Auto-Scheduling ────────────────────────────────────────────────────

    /**
     * Auto-schedule ALL shortlisted candidates for a job.
     * Assigns candidates to free slots round-robin across panels.
     * Sends ICS to candidate + all panel interviewers.
     * Returns summary of {scheduled, skipped, reason}.
     */
    public Map<String, Object> autoSchedule(String jobId, String recruiterEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized");
        }

        List<InterviewPanel> panels = job.getInterviewPanels();
        if (panels == null || panels.isEmpty()) {
            throw new RuntimeException("No interview panels configured for this job. Add panels first.");
        }

        // Get candidates eligible for scheduling (SHORTLISTED or already tagged
        // INTERVIEW but no slot)
        List<Application> candidates = applicationRepository.findByJobId(jobId).stream()
                .filter(a -> "SHORTLISTED".equals(a.getStatus()) || "UNDER_REVIEW".equals(a.getStatus()))
                .filter(a -> a.getInterviewDateTime() == null) // not already scheduled
                .toList();

        if (candidates.isEmpty()) {
            return Map.of("scheduled", 0, "skipped", 0,
                    "message", "No shortlisted candidates without a scheduled interview.");
        }

        // Build ordered list of all free slots across all panels (round-robin order)
        List<PanelSlotPair> freeSlots = buildRoundRobinSlots(panels);

        if (freeSlots.isEmpty()) {
            return Map.of("scheduled", 0, "skipped", candidates.size(),
                    "message", "No free slots available. Add more interview slots.");
        }

        int scheduled = 0, skipped = 0;
        List<String> scheduledNames = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            if (i >= freeSlots.size()) {
                skipped++;
                continue;
            }

            Application app = candidates.get(i);
            PanelSlotPair pair = freeSlots.get(i);
            InterviewPanel panel = pair.panel;
            InterviewSlot slot = pair.slot;

            // Assign candidate to slot
            slot.setBooked(true);
            slot.setCandidateId(app.getId());
            slot.setCandidateEmail(app.getCandidateEmail());
            slot.setCandidateName(app.getCandidateName());

            // Update application
            app.setStatus("INTERVIEW");
            app.setInterviewDateTime(slot.getDateTime());
            applicationRepository.save(app);

            // Send emails
            sendBulkInterviewEmails(app, job, panel, slot);

            scheduled++;
            scheduledNames.add(app.getCandidateName() + " → " + panel.getPanelName() +
                    " @ " + slot.getDateTime().format(DISPLAY_FMT));
        }

        // Persist updated slots
        jobRepository.save(job);

        return Map.of(
                "scheduled", scheduled,
                "skipped", skipped,
                "totalCandidates", candidates.size(),
                "assignments", scheduledNames,
                "message", "Auto-scheduling complete. " + scheduled + " candidates scheduled, " + skipped
                        + " skipped (not enough slots).");
    }

    /**
     * Build round-robin ordered list of free slots:
     * Panel A slot1, Panel B slot1, Panel C slot1, Panel A slot2, Panel B slot2 …
     */
    private List<PanelSlotPair> buildRoundRobinSlots(List<InterviewPanel> panels) {
        List<List<InterviewSlot>> freeSlotsByPanel = new ArrayList<>();
        for (InterviewPanel panel : panels) {
            List<InterviewSlot> free = new ArrayList<>();
            if (panel.getSlots() != null) {
                panel.getSlots().stream()
                        .filter(s -> !s.isBooked())
                        .sorted(Comparator.comparing(InterviewSlot::getDateTime))
                        .forEach(free::add);
            }
            freeSlotsByPanel.add(free);
        }

        List<PanelSlotPair> result = new ArrayList<>();
        boolean hasMore = true;
        int round = 0;
        while (hasMore) {
            hasMore = false;
            for (int p = 0; p < panels.size(); p++) {
                List<InterviewSlot> slots = freeSlotsByPanel.get(p);
                if (round < slots.size()) {
                    result.add(new PanelSlotPair(panels.get(p), slots.get(round)));
                    hasMore = true;
                }
            }
            round++;
        }
        return result;
    }

    private record PanelSlotPair(InterviewPanel panel, InterviewSlot slot) {
    }

    // ─── Email dispatch ──────────────────────────────────────────────────────

    private void sendBulkInterviewEmails(Application app, Job job,
            InterviewPanel panel, InterviewSlot slot) {
        String dateStr = slot.getDateTime().format(DISPLAY_FMT);
        String interviewersDisplay = panel.getInterviewerNames() != null && !panel.getInterviewerNames().isEmpty()
                ? String.join(", ", panel.getInterviewerNames())
                : panel.getPanelName();

        // 1. Email to candidate
        try {
            emailService.sendInterviewEmailWithICS(
                    app.getCandidateEmail(),
                    app.getCandidateName(),
                    job.getTitle(),
                    job.getCompany() != null ? job.getCompany() : "the company",
                    slot.getDateTime().toString(),
                    "Your interviewers will be: " + interviewersDisplay + ". Panel: " + panel.getPanelName());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send interview email to candidate " + app.getCandidateEmail() + ": "
                    + e.getMessage());
        }

        // 2. Email to all panel interviewers
        if (panel.getInterviewerEmails() != null) {
            for (int i = 0; i < panel.getInterviewerEmails().size(); i++) {
                String interviewerEmail = panel.getInterviewerEmails().get(i);
                String interviewerName = (panel.getInterviewerNames() != null && i < panel.getInterviewerNames().size())
                        ? panel.getInterviewerNames().get(i)
                        : "Interviewer";
                try {
                    emailService.sendInterviewerNotification(
                            interviewerEmail, interviewerName,
                            app.getCandidateName(), app.getCandidateEmail(),
                            job.getTitle(), job.getCompany() != null ? job.getCompany() : "your company",
                            slot.getDateTime().toString(),
                            panel.getPanelName(),
                            app.getMatchScore(), app.getSkillsGap());
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to send interviewer notification to " + interviewerEmail + ": "
                            + e.getMessage());
                }
            }
        }
    }
}
