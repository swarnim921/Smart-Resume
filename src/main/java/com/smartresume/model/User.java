package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

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
    private String password; // bcrypt-hashed
    private String role; // ROLE_USER or ROLE_RECRUITER
    private boolean isVerified;
    private String verificationCode;
    @org.springframework.data.mongodb.core.index.Indexed(expireAfterSeconds = 0)
    private java.time.LocalDateTime verificationCodeExpiresAt;
}
