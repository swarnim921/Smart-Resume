package com.smartresume.model;

import lombok.Data;

@Data
public class CourseRecommendation {

    private String courseName;
    private String provider;
    private String duration;
    private String priority; // High, Medium, Low
    private String reason;
    private String url;

    public CourseRecommendation() {
    }

    public CourseRecommendation(String courseName, String provider, String duration, String priority, String reason) {
        this.courseName = courseName;
        this.provider = provider;
        this.duration = duration;
        this.priority = priority;
        this.reason = reason;
    }
}
