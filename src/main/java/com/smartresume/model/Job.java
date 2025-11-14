package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Document(collection = "jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Job {
    @Id
    private String id;
    private String title;
    private String description;
}
