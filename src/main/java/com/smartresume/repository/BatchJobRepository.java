package com.smartresume.repository;

import com.smartresume.model.BatchJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchJobRepository extends MongoRepository<BatchJob, String> {
}
