package com.smartresume.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "resumes")
public class ResumeMeta {

    @Id
    private String id;

    private String userId;

    private String filename;

    private String contentType;

    private String gridFsId;
}
