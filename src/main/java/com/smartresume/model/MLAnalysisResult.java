package com.smartresume.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "ml_analysis_results")
@NoArgsConstructor
@AllArgsConstructor
public class MLAnalysisResult {

    @Id
    private String id;

    private String applicationId; // Reference to JobApplication

    private Double matchPercentage; // 0-100 match score

    private String selectionStatus; // "SELECTED", "REJECTED", "PENDING"

    private List<String> skillGaps; // Skills missing from resume

    private List<CourseRecommendation> recommendedCourses; // Course suggestions

    private Date analyzedAt = new Date();

    private String mlModelVersion; // Track which ML model version was used

    private String notes; // Additional notes from ML analysis
}
