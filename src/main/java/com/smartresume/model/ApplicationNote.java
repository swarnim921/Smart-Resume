package com.smartresume.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Embedded model for a single recruiter note on an application.
 * Stored as a list so the full notes timeline is preserved.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationNote {
    private String text;
    private String recruiterEmail;
    private LocalDateTime timestamp;
    /** The status the application moved to when this note was added */
    private String status;
}
