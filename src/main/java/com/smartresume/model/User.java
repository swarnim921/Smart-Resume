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
    private String email;
    private String password;
    private String role;
    private boolean isVerified;
    private String verificationCode;
    @org.springframework.data.mongodb.core.index.Indexed(expireAfterSeconds = 0)
    private LocalDateTime verificationCodeExpiresAt;

    // Candidate profile fields
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private List<String> skills;
    private Integer yearsOfExperience;
    private String location;
    private LocalDateTime profileCompletedAt;
}
