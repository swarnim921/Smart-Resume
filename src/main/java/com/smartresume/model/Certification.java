package com.smartresume.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certification {
    private String name;
    private String fileId;     // ResumeMeta id (GridFS reference), null if no file uploaded
    private String fileName;   // Original filename for display, null if no file uploaded
}
