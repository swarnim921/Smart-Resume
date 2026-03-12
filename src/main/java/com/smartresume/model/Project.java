package com.smartresume.model;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String title;
    private String description;
    private List<String> technologies;
    private String githubLink;
    private String liveLink;
}
