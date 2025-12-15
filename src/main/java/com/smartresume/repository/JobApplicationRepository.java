package com.smartresume.repository;

import com.smartresume.model.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {

    // Find all applications by user
    List<JobApplication> findByUserId(String userId);

    // Find all applications for a specific job
    List<JobApplication> findByJobId(String jobId);

    // Find all applications for a specific resume
    List<JobApplication> findByResumeId(String resumeId);

    // Find application by user and job (to prevent duplicate applications)
    JobApplication findByUserIdAndJobId(String userId, String jobId);
}
