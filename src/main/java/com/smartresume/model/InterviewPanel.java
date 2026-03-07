package com.smartresume.model;

import lombok.*;
import java.util.List;
import java.util.ArrayList;

/**
 * An interview panel — a named group of interviewers with a set of time slots.
 * Multiple panels can be attached to a single Job.
 * e.g. "Panel A" → interviewers: rohit@a.com, priya@a.com → slots: Mon 10am,
 * Mon 11am
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPanel {
    private String panelId; // UUID
    private String panelName; // e.g. "Panel A", "Technical Panel 1"
    /** Comma-separated or list of interviewer email addresses */
    private List<String> interviewerEmails;
    /** Display names to show candidates (optional) */
    private List<String> interviewerNames;
    /** Available slots — booked=false means still open */
    @Builder.Default
    private List<InterviewSlot> slots = new ArrayList<>();
}
