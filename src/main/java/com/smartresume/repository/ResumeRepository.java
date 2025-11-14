package com.smartresume.repository;

import com.smartresume.model.ResumeMeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ResumeRepository extends MongoRepository<ResumeMeta, String> {
    List<ResumeMeta> findByOwnerId(String ownerId);
}
