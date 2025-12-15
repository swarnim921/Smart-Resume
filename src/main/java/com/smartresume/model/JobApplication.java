package com.smartresume.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "job_applications")
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    private String id;

    private String resumeId; // Reference to ResumeMeta
    private String jobId; // Reference to Job
    private String userId; // Reference to User

    private Date appliedAt = new Date();

    private String status; // "PENDING", "SELECTED", "REJECTED", "UNDER_REVIEW"

    private String mlAnalysisId; // Reference to ML analysis result (optional, set after ML processing)
}
