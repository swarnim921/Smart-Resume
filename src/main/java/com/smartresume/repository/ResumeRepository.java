package com.smartresume.repository;

import com.smartresume.model.ResumeMeta;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeRepository extends MongoRepository<ResumeMeta, String> {
}
