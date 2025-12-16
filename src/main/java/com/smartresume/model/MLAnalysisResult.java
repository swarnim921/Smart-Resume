package com.smartresume.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "ml_analysis_results")
public class MLAnalysisResult {

    @Id
    private String id;

    private String applicationId;
    private String userId;
    private String jobId;

    private Double matchScore;
    private List<String> skillsMatched;
    private List<String> skillsGap;
    private String experienceMatch;
    private Double confidence;

    private List<CourseRecommendation> recommendations;

    private String status; // PENDING, COMPLETED, FAILED
    private Date analyzedAt;
    private Date createdAt;

    public MLAnalysisResult() {
        this.createdAt = new Date();
        this.status = "PENDING";
    }
}
