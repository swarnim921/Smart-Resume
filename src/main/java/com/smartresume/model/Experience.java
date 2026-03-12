package com.smartresume.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experience {
    private String role;
    private String company;
    private String startDate;
    private String endDate;
    private String description;
}
