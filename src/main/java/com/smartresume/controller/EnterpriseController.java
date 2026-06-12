package com.smartresume.controller;

import com.smartresume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.smartresume.model.User;
import com.smartresume.model.ResumeMeta;

import java.util.*;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@Slf4j
public class EnterpriseController {

    private final ResumeService resumeService;
    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    /**
     * B2B Feature: Parse a JD file and return auto-fill data.
     */
    @PostMapping("/parse-jd")
    public ResponseEntity<?> parseJobDescription(@RequestParam("file") MultipartFile file) {
        try {
            // Temporarily store the file in GridFS under a dummy/system user, or just parse directly?
            // To reuse extractTextFromResume, we need to save it. But it's better to just parse it in memory if possible.
            // Since we updated ResumeService to require a saved ResumeMeta, we'll save it temporarily.
            // A cleaner B2B way:
            User systemUser = new User();
            systemUser.setId("system_enterprise");
            
            ResumeMeta jdMeta = resumeService.store(file, systemUser, "JD_DOC");
            String extractedText = resumeService.extractTextFromResume(jdMeta.getId());

            // Ask ML service to extract structured JD fields
            String url = mlServiceUrl + "/api/ml/extract-skills";
            Map<String, String> request = new HashMap<>();
            request.put("resumeText", extractedText);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            List<String> techSkills = (List<String>) responseBody.getOrDefault("technical_skills", Collections.emptyList());
            List<String> softSkills = (List<String>) responseBody.getOrDefault("soft_skills", Collections.emptyList());
            
            String allSkills = String.join(", ", techSkills);
            if (!softSkills.isEmpty()) {
                allSkills += ", " + String.join(", ", softSkills);
            }

            // Heuristically extract a title from the first line
            String[] lines = extractedText.split("\n");
            String title = lines.length > 0 ? lines[0].trim() : "Parsed Job Title";
            if (title.length() > 100) title = title.substring(0, 100);

            Map<String, Object> result = new HashMap<>();
            result.put("jobTitle", title);
            result.put("description", extractedText);
            result.put("requirements", allSkills);

            return ResponseEntity.ok(result);

        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            log.error("ML Service Rate Limited: {}", e.getMessage());
            return ResponseEntity.status(429).body(Map.of("error", "The AI Analysis Service is currently experiencing high traffic. Please wait a few seconds and try again."));
        } catch (Exception e) {
            log.error("Failed to parse JD file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * B2B Feature: Many-to-1 Batch Resume Screening
     */
    @PostMapping("/batch-screen")
    public ResponseEntity<?> batchScreen(
            @RequestParam("jdFile") MultipartFile jdFile,
            @RequestParam("resumes") MultipartFile[] resumes) {
        
        try {
            User systemUser = new User();
            systemUser.setId("system_enterprise");

            // 1. Parse JD
            ResumeMeta jdMeta = resumeService.store(jdFile, systemUser, "JD_DOC");
            String jdText = resumeService.extractTextFromResume(jdMeta.getId());

            // 2. Parse all Resumes
            List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
            Map<String, String> candidateNames = new HashMap<>();

            for (MultipartFile resumeFile : resumes) {
                ResumeMeta resMeta = resumeService.store(resumeFile, systemUser, "CANDIDATE_RESUME");
                String resText = resumeService.extractTextFromResume(resMeta.getId());
                
                String filename = resumeFile.getOriginalFilename();
                String candidateName = filename != null ? filename.replaceAll("(?i)\\.(pdf|txt|png|jpg|jpeg|doc|docx)$", "") : "Unknown Candidate";
                candidateNames.put(resMeta.getId(), candidateName);

                Map<String, Object> appObj = new HashMap<>();
                appObj.put("applicationId", resMeta.getId());
                appObj.put("resumeText", resText);
                appObj.put("jobDescription", jdText);
                mlPayloadApps.add(appObj);
            }

            // 3. Send to Python ML Service for Batch Analysis
            String url = mlServiceUrl + "/api/ml/batch-analyze";
            Map<String, Object> request = new HashMap<>();
            request.put("jobId", "enterprise-batch-1");
            request.put("applications", mlPayloadApps);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                
                // Add candidate names back into the results
                for (Map<String, Object> res : results) {
                    String appId = (String) res.get("applicationId");
                    res.put("candidateName", candidateNames.get(appId));
                }
                
                return ResponseEntity.ok(responseBody);
            } else {
                throw new RuntimeException("ML Service failed to batch analyze");
            }

        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            log.error("ML Service Rate Limited: {}", e.getMessage());
            return ResponseEntity.status(429).body(Map.of("error", "The AI Analysis Service is currently experiencing high traffic. Please wait 10 seconds and try again."));
        } catch (Exception e) {
            log.error("Batch screening failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
