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
            
            int resumeChunkSize = 10;
            int jdChunkSize = 5;
            
            for (int i = 0; i < resumeIds.size(); i += resumeChunkSize) {
                int end = Math.min(i + resumeChunkSize, resumeIds.size());
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

                // Map to accumulate the matches for these 25 candidates across all JD chunks
                Map<String, Map<String, Object>> candidateAggregatedResults = new HashMap<>();
                
                // 2D Chunking over JDs to prevent Python API timeouts
                for (int j = 0; j < mlPayloadJds.size(); j += jdChunkSize) {
                    int jdEnd = Math.min(j + jdChunkSize, mlPayloadJds.size());
                    List<Map<String, Object>> jdChunk = mlPayloadJds.subList(j, jdEnd);
                    
                    String url = mlServiceUrl + "/api/ml/matrix-analyze";
                    Map<String, Object> request = new HashMap<>();
                    request.put("jobDescriptions", jdChunk);
                    request.put("applications", mlPayloadApps);

                    try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            List<Map<String, Object>> chunkResults = (List<Map<String, Object>>) response.getBody().get("results");
                            
                            for (Map<String, Object> res : chunkResults) {
                                String appId = (String) res.get("applicationId");
                                List<Map<String, Object>> matches = (List<Map<String, Object>>) res.get("matches");
                                
                                if (candidateAggregatedResults.containsKey(appId)) {
                                    // Append matches to existing candidate
                                    List<Map<String, Object>> existingMatches = (List<Map<String, Object>>) candidateAggregatedResults.get(appId).get("matches");
                                    existingMatches.addAll(matches);
                                } else {
                                    // New candidate in this chunk: Ensure the matches list is strictly mutable
                                    res.put("candidateName", candidateNames.get(appId));
                                    res.put("matches", new ArrayList<>(matches));
                                    candidateAggregatedResults.put(appId, res);
                                }
                            }
                        }
                    } catch (Exception mlEx) {
                        log.error("ML Service matrix-analyze failed for resume chunk {} to {}, JD chunk {} to {}: {}", i, end, j, jdEnd, mlEx.getMessage());
                    }
                    
                    try {
                        Thread.sleep(700); // 700ms strictly caps throughput to ~85 req/min, preventing proxy limits
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // Add the fully aggregated candidate matches to the final results list
                allResults.addAll(candidateAggregatedResults.values());

                job.setProcessedResumes(end);
                batchJobRepository.save(job);

                try {
                    Thread.sleep(4000); // 4000ms buffer between chunks dramatically lowers average requests per minute
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

    @Async
    public void processPlacementBatchFromDisk(String batchId, List<Map<String, String>> tempJdFiles, List<Map<String, String>> tempResumeFiles, com.smartresume.model.User systemUser) {
        log.info("Starting async Placement Matrix processing from disk for BatchJob: {}", batchId);
        
        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            log.error("BatchJob {} not found!", batchId);
            return;
        }

        try {
            // 1. Store JDs to GridFS and Extract Text
            List<Map<String, String>> jdsData = new ArrayList<>();
            for (Map<String, String> jdInfo : tempJdFiles) {
                java.io.File file = new java.io.File(jdInfo.get("path"));
                if (file.exists()) {
                    com.smartresume.model.ResumeMeta jdMeta = resumeService.store(file, jdInfo.get("originalFilename"), jdInfo.get("contentType"), systemUser, "JD_DOC");
                    String text = resumeService.extractTextFromResume(jdMeta.getId());
                    
                    Map<String, String> jdMap = new HashMap<>();
                    jdMap.put("id", jdMeta.getId());
                    jdMap.put("text", text);
                    jdMap.put("name", jdInfo.get("name"));
                    jdsData.add(jdMap);
                    
                    file.delete(); // cleanup temp file
                }
            }
            
            job.setJds(jdsData);
            batchJobRepository.save(job);

            // 2. Store Resumes to GridFS
            List<String> resumeIds = new ArrayList<>();
            Map<String, String> candidateNames = new HashMap<>();
            
            for (Map<String, String> resInfo : tempResumeFiles) {
                java.io.File file = new java.io.File(resInfo.get("path"));
                if (file.exists()) {
                    com.smartresume.model.ResumeMeta resMeta = resumeService.store(file, resInfo.get("originalFilename"), resInfo.get("contentType"), systemUser, "CANDIDATE_RESUME");
                    resumeIds.add(resMeta.getId());
                    candidateNames.put(resMeta.getId(), resInfo.get("name"));
                    
                    file.delete(); // cleanup temp file
                }
            }
            
            // 3. Delegate to the regular matrix processing method, removing the @Async wrapper to run in the same thread
            // Wait, processPlacementBatch is @Async! If we call it from another @Async method inside the same class, Spring won't proxy it.
            // It will run synchronously in this same background thread, which is exactly what we want!
            processPlacementBatch(batchId, jdsData, resumeIds, candidateNames);
            
        } catch (Exception e) {
            log.error("Fatal error processing Placement BatchJob from disk {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            batchJobRepository.save(job);
        }
    }

    @Async
    public void processPlacementBatchFromText(String batchId, List<Map<String, String>> jds, List<Map<String, String>> resumes, com.smartresume.model.User systemUser) {
        log.info("Starting async FAST Placement Matrix processing from text for BatchJob: {}", batchId);
        
        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            log.error("BatchJob {} not found!", batchId);
            return;
        }

        try {
            List<Map<String, Object>> allResults = new ArrayList<>();
            
            // Format JDs payload and save to job for frontend rendering
            List<Map<String, Object>> mlPayloadJds = new ArrayList<>();
            List<Map<String, String>> jdsMetadata = new ArrayList<>();
            for (Map<String, String> jd : jds) {
                Map<String, Object> jdObj = new HashMap<>();
                jdObj.put("jobId", jd.get("filename")); // Use filename as jobId for fast mode
                jdObj.put("jobDescriptionText", jd.get("text"));
                mlPayloadJds.add(jdObj);

                Map<String, String> meta = new HashMap<>();
                meta.put("id", jd.get("filename"));
                meta.put("name", jd.get("filename").replaceAll("(?i)\\.(pdf|txt|png|jpg|jpeg|doc|docx)$", ""));
                jdsMetadata.add(meta);
            }
            job.setJds(jdsMetadata);
            batchJobRepository.save(job);
            
            int resumeChunkSize = 10;
            int jdChunkSize = 5;
            
            for (int i = 0; i < resumes.size(); i += resumeChunkSize) {
                int end = Math.min(i + resumeChunkSize, resumes.size());
                List<Map<String, String>> chunk = resumes.subList(i, end);
                
                List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
                for (Map<String, String> res : chunk) {
                    Map<String, Object> appObj = new HashMap<>();
                    appObj.put("applicationId", res.get("filename")); // Use filename as app ID
                    appObj.put("resumeText", res.get("text"));
                    mlPayloadApps.add(appObj);
                }

                Map<String, Map<String, Object>> candidateAggregatedResults = new HashMap<>();
                
                for (int j = 0; j < mlPayloadJds.size(); j += jdChunkSize) {
                    int jdEnd = Math.min(j + jdChunkSize, mlPayloadJds.size());
                    List<Map<String, Object>> jdChunk = mlPayloadJds.subList(j, jdEnd);
                    
                    String url = mlServiceUrl + "/api/ml/matrix-analyze";
                    Map<String, Object> request = new HashMap<>();
                    request.put("jobDescriptions", jdChunk);
                    request.put("applications", mlPayloadApps);

                    try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            List<Map<String, Object>> chunkResults = (List<Map<String, Object>>) response.getBody().get("results");
                            
                            for (Map<String, Object> res : chunkResults) {
                                String appId = (String) res.get("applicationId");
                                List<Map<String, Object>> matches = (List<Map<String, Object>>) res.get("matches");
                                
                                if (candidateAggregatedResults.containsKey(appId)) {
                                    List<Map<String, Object>> existingMatches = (List<Map<String, Object>>) candidateAggregatedResults.get(appId).get("matches");
                                    existingMatches.addAll(matches);
                                } else {
                                    res.put("candidateName", appId); // Fallback name
                                    res.put("matches", new ArrayList<>(matches));
                                    candidateAggregatedResults.put(appId, res);
                                }
                            }
                        }
                    } catch (Exception mlEx) {
                        log.error("ML Service matrix-analyze failed: {}", mlEx.getMessage());
                    }
                }
                
                allResults.addAll(candidateAggregatedResults.values());
                job.setProcessedResumes(end);
                batchJobRepository.save(job);
            }

            job.setStatus("COMPLETED");
            job.setResults(allResults);
            batchJobRepository.save(job);
            log.info("FAST Placement BatchJob {} completed successfully.", batchId);

        } catch (Exception e) {
            log.error("Fatal error processing FAST Placement BatchJob {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            batchJobRepository.save(job);
        }
    }

    /**
     * Process a batch of pre-extracted text (no PDF uploads needed).
     * Browser parses PDFs locally and sends only text strings.
     * This method chunks them and forwards to ML service.
     */
    @Async
    public void processTextBatch(String batchId, String jdText, List<Map<String, String>> resumeTexts) {
        log.info("Starting TEXT-BASED async batch processing for BatchJob: {}. {} resumes.", batchId, resumeTexts.size());

        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            log.error("BatchJob {} not found!", batchId);
            return;
        }

        try {
            int chunkSize = 10;
            List<Map<String, Object>> allResults = new ArrayList<>();

            for (int i = 0; i < resumeTexts.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, resumeTexts.size());
                List<Map<String, String>> chunk = resumeTexts.subList(i, end);

                List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
                for (Map<String, String> resume : chunk) {
                    Map<String, Object> appObj = new HashMap<>();
                    appObj.put("applicationId", resume.get("candidateName"));
                    appObj.put("resumeText", resume.get("text"));
                    appObj.put("jobDescription", jdText);
                    mlPayloadApps.add(appObj);
                }

                String url = mlServiceUrl + "/api/ml/batch-analyze";
                Map<String, Object> request = new HashMap<>();
                request.put("jobId", batchId);
                request.put("applications", mlPayloadApps);

                log.info("Sending text chunk {}-{} of {} to ML Service...", i + 1, end, resumeTexts.size());

                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        List<Map<String, Object>> chunkResults = (List<Map<String, Object>>) response.getBody().get("results");
                        for (Map<String, Object> res : chunkResults) {
                            res.put("candidateName", res.get("applicationId"));
                            allResults.add(res);
                        }
                        log.info("Chunk {}-{} processed successfully by ML Service.", i + 1, end);
                    }
                } catch (Exception mlEx) {
                    log.error("ML Service failed for text chunk {} to {}: {}", i, end, mlEx.getMessage());
                    for (Map<String, String> resume : chunk) {
                        Map<String, Object> fallbackRes = new HashMap<>();
                        fallbackRes.put("applicationId", resume.get("candidateName"));
                        fallbackRes.put("candidateName", resume.get("candidateName"));
                        fallbackRes.put("matchScore", 0);
                        fallbackRes.put("missingSkills", new ArrayList<>());
                        allResults.add(fallbackRes);
                    }
                }

                job.setProcessedResumes(end);
                batchJobRepository.save(job);
                // No delay needed — HF Spaces has no Cloudflare rate limiting
            }

            job.setStatus("COMPLETED");
            allResults.sort((a, b) -> {
                Number scoreA = (Number) a.get("matchScore");
                Number scoreB = (Number) b.get("matchScore");
                if (scoreA == null) scoreA = 0;
                if (scoreB == null) scoreB = 0;
                return Double.compare(scoreB.doubleValue(), scoreA.doubleValue());
            });
            job.setResults(allResults);
            batchJobRepository.save(job);
            log.info("TEXT-BASED BatchJob {} completed successfully. {} resumes processed.", batchId, allResults.size());

        } catch (Exception e) {
            log.error("Fatal error processing TEXT-BASED BatchJob {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            batchJobRepository.save(job);
        }
    }

    /**
     * Process a many-to-many placement matrix from pre-extracted text.
     * Browser parses PDFs locally and sends text strings for both JDs and resumes.
     */
    @Async
    public void processTextPlacementBatch(String batchId, List<Map<String, String>> jdTexts, List<Map<String, String>> resumeTexts) {
        log.info("Starting TEXT-BASED Placement Matrix for BatchJob: {}. {} JDs x {} resumes.", batchId, jdTexts.size(), resumeTexts.size());

        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            log.error("BatchJob {} not found!", batchId);
            return;
        }

        try {
            // Format JDs for ML
            List<Map<String, Object>> mlPayloadJds = new ArrayList<>();
            List<Map<String, String>> jdsMetadata = new ArrayList<>();
            for (Map<String, String> jd : jdTexts) {
                Map<String, Object> jdObj = new HashMap<>();
                jdObj.put("jobId", jd.get("name"));
                jdObj.put("jobDescriptionText", jd.get("text"));
                mlPayloadJds.add(jdObj);

                Map<String, String> meta = new HashMap<>();
                meta.put("id", jd.get("name"));
                meta.put("name", jd.get("name"));
                jdsMetadata.add(meta);
            }

            int chunkSize = 10;
            List<Map<String, Object>> allResults = new ArrayList<>();

            for (int i = 0; i < resumeTexts.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, resumeTexts.size());
                List<Map<String, String>> chunk = resumeTexts.subList(i, end);

                List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
                for (Map<String, String> resume : chunk) {
                    Map<String, Object> appObj = new HashMap<>();
                    appObj.put("applicationId", resume.get("candidateName"));
                    appObj.put("resumeText", resume.get("text"));
                    mlPayloadApps.add(appObj);
                }

                String url = mlServiceUrl + "/api/ml/matrix-analyze";
                Map<String, Object> request = new HashMap<>();
                request.put("jobDescriptions", mlPayloadJds);
                request.put("applications", mlPayloadApps);

                log.info("Sending placement text chunk {}-{} of {} to ML Service...", i + 1, end, resumeTexts.size());

                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        List<Map<String, Object>> chunkResults = (List<Map<String, Object>>) response.getBody().get("results");
                        for (Map<String, Object> res : chunkResults) {
                            res.put("candidateName", res.get("applicationId"));
                            allResults.add(res);
                        }
                        log.info("Placement chunk {}-{} processed successfully.", i + 1, end);
                    }
                } catch (Exception mlEx) {
                    log.error("ML Service failed for placement chunk {} to {}: {}", i, end, mlEx.getMessage());
                    for (Map<String, String> resume : chunk) {
                        Map<String, Object> fallbackRes = new HashMap<>();
                        fallbackRes.put("applicationId", resume.get("candidateName"));
                        fallbackRes.put("candidateName", resume.get("candidateName"));
                        fallbackRes.put("matches", new ArrayList<>());
                        allResults.add(fallbackRes);
                    }
                }

                job.setProcessedResumes(end);
                batchJobRepository.save(job);
            }

            job.setStatus("COMPLETED");
            job.setJds(jdsMetadata);
            job.setResults(allResults);
            batchJobRepository.save(job);
            log.info("TEXT-BASED Placement BatchJob {} completed successfully.", batchId);

        } catch (Exception e) {
            log.error("Fatal error processing TEXT-BASED Placement BatchJob {}: {}", batchId, e.getMessage(), e);
            job.setStatus("FAILED");
            batchJobRepository.save(job);
        }
    }
}
