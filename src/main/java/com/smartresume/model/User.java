package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Document(collection = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    private String id;
    private String name;
    private String email;
    private String password; // bcrypt-hashed
    private String role; // ROLE_USER or ROLE_ADMIN
}
