package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    private String name;
    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String email;
    private String password;
    private String role;
    private boolean isVerified;
    private String verificationCode;
    @org.springframework.data.mongodb.core.index.Indexed(expireAfterSeconds = 0)
    private LocalDateTime verificationCodeExpiresAt;
    private String authProvider; // LOCAL, GOOGLE, LINKEDIN

    // Candidate profile fields
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private List<String> skills;
    private Integer yearsOfExperience;
    private String location;
    private LocalDateTime profileCompletedAt;

    // Recruiter-Grade Expansion
    private List<Education> education;
    private List<Project> projects;
    private List<Experience> experienceList;
    private List<Certification> certifications;
    private List<String> achievements;
    private Boolean hasCertifications;  // null = not answered, true = yes, false = no
    private Boolean hasAchievements;    // null = not answered, true = yes, false = no
    private List<String> preferredRoles;
    private List<String> preferredLocations;
    private String availability;
    private List<String> languages;
}
