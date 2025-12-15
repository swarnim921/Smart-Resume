package com.smartresume.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRecommendation {

    private String skillName; // Skill to learn

    private String courseName; // Recommended course name

    private String courseUrl; // URL to the course

    private String provider; // Course provider (e.g., Coursera, Udemy, edX)

    private Integer priority; // 1 (high) to 5 (low)

    private String duration; // Estimated duration (e.g., "4 weeks", "20 hours")

    private String level; // "Beginner", "Intermediate", "Advanced"
}
