package com.smartresume.repository;

import com.smartresume.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByStatus(String status);

    List<Job> findByPostedBy(String postedBy);
}
