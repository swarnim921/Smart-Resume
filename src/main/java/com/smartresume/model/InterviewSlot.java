package com.smartresume.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * A single interview slot within an InterviewPanel.
 * Once assigned, candidateId and candidateEmail are set.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSlot {
    private String slotId; // UUID
    private LocalDateTime dateTime; // When the interview happens
    private int durationMinutes; // Default 60
    private boolean booked; // true once a candidate is assigned
    private String candidateId; // Application ID
    private String candidateEmail;
    private String candidateName;
}
