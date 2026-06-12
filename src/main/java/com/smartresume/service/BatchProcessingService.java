package com.smartresume.service;

import com.smartresume.model.BatchJob;
import com.smartresume.repository.BatchJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BatchProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessingService.class);

    @Autowired
    private BatchJobRepository batchJobRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ResumeService resumeService;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Async
    public void processBatch(String batchId, String jdText, List<String> resumeIds, Map<String, String> candidateNames) {
        log.info("Starting async processing for BatchJob: {}", batchId);
        
        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            log.error("BatchJob {} not found!", batchId);
            return;
        }

        try {
            // Because ML Service might timeout if we send 250 at once, we chunk them.
            // Using 25 resumes per chunk with a 5s delay safely bypasses Render/Cloudflare 429 limits
            int chunkSize = 25;
            List<Map<String, Object>> allResults = new ArrayList<>();
            
            for (int i = 0; i < resumeIds.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, resumeIds.size());
                List<String> chunk = resumeIds.subList(i, end);
                
                List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
                for (String resId : chunk) {
                    String resText = resumeService.extractTextFromResume(resId);
                    Map<String, Object> appObj = new HashMap<>();
                    appObj.put("applicationId", resId);
                    appObj.put("resumeText", resText);
                    appObj.put("jobDescription", jdText);
                    mlPayloadApps.add(appObj);
                }

                // Send chunk to Python ML Service
                String url = mlServiceUrl + "/api/ml/batch-analyze";
                Map<String, Object> request = new HashMap<>();
                request.put("jobId", batchId);
                request.put("applications", mlPayloadApps);

                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        List<Map<String, Object>> chunkResults = (List<Map<String, Object>>) response.getBody().get("results");
                        for (Map<String, Object> res : chunkResults) {
                            String appId = (String) res.get("applicationId");
                            res.put("candidateName", candidateNames.get(appId));
                            allResults.add(res);
                        }
                    }
                } catch (Exception mlEx) {
                    log.error("ML Service failed for chunk {} to {}: {}", i, end, mlEx.getMessage());
                    // Create fallback empty results for this chunk to prevent hanging
                    for (String resId : chunk) {
                        Map<String, Object> fallbackRes = new HashMap<>();
                        fallbackRes.put("applicationId", resId);
                        fallbackRes.put("candidateName", candidateNames.get(resId));
                        fallbackRes.put("matchScore", 0);
                        fallbackRes.put("missingSkills", new ArrayList<>());
                        allResults.add(fallbackRes);
                    }
                }

                // Update progress after chunk finishes
                job.setProcessedResumes(end);
                batchJobRepository.save(job);

                // Add a delay between chunks to avoid hitting the ML Service rate limits (Cloudflare 429)
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            // All chunks processed
            job.setStatus("COMPLETED");
            
            // Sort by match score highest first
            allResults.sort((a, b) -> {
                Number scoreA = (Number) a.get("matchScore");
                Number scoreB = (Number) b.get("matchScore");
                if (scoreA == null) scoreA = 0;
                if (scoreB == null) scoreB = 0;
                return Double.compare(scoreB.doubleValue(), scoreA.doubleValue());
            });
            
            job.setResults(allResults);
            batchJobRepository.save(job);
            
            log.info("BatchJob {} completed successfully.", batchId);

        } catch (Exception e) {
            log.error("Fatal error processing BatchJob {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            batchJobRepository.save(job);
        }
    }

    @Async
    public void processPlacementBatch(String batchId, List<Map<String, String>> jds, List<String> resumeIds, Map<String, String> candidateNames) {
        log.info("Starting async Placement Matrix processing for BatchJob: {}", batchId);
        
        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            log.error("BatchJob {} not found!", batchId);
            return;
        }

        try {
            int chunkSize = 25;
            List<Map<String, Object>> allResults = new ArrayList<>();
            
            // Format JDs
            List<Map<String, Object>> mlPayloadJds = new ArrayList<>();
            for (Map<String, String> jd : jds) {
                Map<String, Object> jdObj = new HashMap<>();
                jdObj.put("jobId", jd.get("id"));
                jdObj.put("jobDescriptionText", jd.get("text"));
                mlPayloadJds.add(jdObj);
            }
            
            for (int i = 0; i < resumeIds.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, resumeIds.size());
                List<String> chunk = resumeIds.subList(i, end);
                
                List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
                for (String resId : chunk) {
                    try {
                        String resText = resumeService.extractTextFromResume(resId);
                        Map<String, Object> appObj = new HashMap<>();
                        appObj.put("applicationId", resId);
                        appObj.put("resumeText", resText);
                        mlPayloadApps.add(appObj);
                    } catch (Exception ex) {
                        log.warn("Failed to extract text for {}: {}", resId, ex.getMessage());
                    }
                }

                String url = mlServiceUrl + "/api/ml/matrix-analyze";
                Map<String, Object> request = new HashMap<>();
                request.put("jobDescriptions", mlPayloadJds);
                request.put("applications", mlPayloadApps);

                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        List<Map<String, Object>> chunkResults = (List<Map<String, Object>>) response.getBody().get("results");
                        for (Map<String, Object> res : chunkResults) {
                            String appId = (String) res.get("applicationId");
                            res.put("candidateName", candidateNames.get(appId));
                            allResults.add(res);
                        }
                    }
                } catch (Exception mlEx) {
                    log.error("ML Service matrix-analyze failed for chunk {} to {}: {}", i, end, mlEx.getMessage());
                }

                job.setProcessedResumes(end);
                batchJobRepository.save(job);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            job.setStatus("COMPLETED");
            job.setResults(allResults);
            batchJobRepository.save(job);
            log.info("Placement BatchJob {} completed successfully.", batchId);

        } catch (Exception e) {
            log.error("Fatal error processing Placement BatchJob {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            batchJobRepository.save(job);
        }
    }
}
