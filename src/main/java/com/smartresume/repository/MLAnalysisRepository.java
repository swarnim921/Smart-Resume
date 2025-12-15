package com.smartresume.repository;

import com.smartresume.model.MLAnalysisResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MLAnalysisRepository extends MongoRepository<MLAnalysisResult, String> {

    // Find analysis by application ID
    Optional<MLAnalysisResult> findByApplicationId(String applicationId);

    // Find all analyses with specific selection status
    List<MLAnalysisResult> findBySelectionStatus(String selectionStatus);

    // Find analyses with match percentage above threshold
    List<MLAnalysisResult> findByMatchPercentageGreaterThanEqual(Double threshold);
}
