package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    private String id;
    private String jobId;
    private String jobTitle;
    private String jobCompany;
    private String candidateEmail;
    private String candidateName;
    private String resumeId;
    private String status;
    private Double matchScore;
    private List<String> skillsGap;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    /** Kept for backward compatibility with existing records */
    private String reviewNotes;
    /** Full timeline of recruiter notes — new records get this */
    private List<ApplicationNote> notesHistory = new ArrayList<>();
    /** When the ATS score was last re-analyzed */
    private LocalDateTime reAnalyzedAt;
    /** Interview date/time set by recruiter for ICS invite */
    private LocalDateTime interviewDateTime;
    /** Predicted job role based on ML skill clustering */
    private String predictedRole;
    
    // Cached candidate profile data for recruiter "Industry View"
    private String candidateBio;
    private String candidateLinkedin;
    private String candidateGithub;
    private List<String> candidateSkills;
    private Integer candidateExperience;
    
    // Expanded profile data
    private List<Education> candidateEducation;
    private List<Project> candidateProjects;
    private List<Experience> candidateExperienceList;
    private List<String> candidateCertifications;
    private List<String> candidateAchievements;
    private List<String> candidatePreferredRoles;
    private List<String> candidatePreferredLocations;
    private String candidateAvailability;
    private List<String> candidateLanguages;
}
