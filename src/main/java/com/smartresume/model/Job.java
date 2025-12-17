package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    private String id;
    private String title;
    private String company;
    private String location;
    private String type; // Full-time, Part-time, Contract, etc.
    private String salary;
    private String description;
    private String requirements;
    private String postedBy; // Recruiter email
    private LocalDateTime postedAt;
    private String status; // ACTIVE, CLOSED
}
