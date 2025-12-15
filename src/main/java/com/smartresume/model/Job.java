package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.util.Date;

@Document(collection = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    private String id;
    private String title;
    private String description;
    private String company;
    private String location;
    private String skills;
    private String postedDate;
    private Date postedAt = new Date();
}
