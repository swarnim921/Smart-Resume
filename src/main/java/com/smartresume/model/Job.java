package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    private String id;
    private String title;
    private String company;
    private String location;
    private String type;
    private String salary;
    private String description;
    private String requirements;
    private String postedBy;
    private LocalDateTime postedAt;
    private String status;

    /**
     * Custom hiring pipeline stages with per-stage email templates.
     * Null/empty = use default system pipeline (UNDER_REVIEW → SHORTLISTED →
     * INTERVIEW → OFFERED).
     */
    private List<HiringStage> hiringPipeline;
    /** Auto-close date — if set, job moves to CLOSED after this datetime */
    private LocalDateTime expiresAt;
}
