package com.smartresume.repository;

import com.smartresume.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByCandidateEmail(String candidateEmail);

    List<Application> findByJobId(String jobId);

    boolean existsByJobIdAndCandidateEmail(String jobId, String candidateEmail);
}
