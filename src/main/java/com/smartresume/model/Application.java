package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    private String id;
    private String jobId;
    private String candidateEmail;
    private String candidateName;
    private String resumeId;
    private String status; // PENDING, REVIEWED, ACCEPTED, REJECTED
    private Double matchScore; // ML-calculated match score (0-100)
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
}
