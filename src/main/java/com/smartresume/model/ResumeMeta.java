package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Document(collection = "resumes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ResumeMeta {
    @Id
    private String id;
    private String filename;
    private String contentType;
    private long size;
    private String gridFsId;
    private String ownerId;
    private String extractedText;
}
