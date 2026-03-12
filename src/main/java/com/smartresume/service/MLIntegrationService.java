package com.smartresume.service;

import com.smartresume.model.MLAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.*;

@Service
@Slf4j
public class MLIntegrationService {

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate;

    public MLIntegrationService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(15000);    // 15 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Analyze resume-job match using ML service
     * INTEGRATION READY: Uncomment when ML team delivers
     */
    public MLAnalysisResult analyzeMatch(String resumeText, String jobDescription, String jobTitle,
            String jobRequirements) {
        log.info("Analyzing resume-job match for job: {}", jobTitle);

        try {
            String url = mlServiceUrl + "/api/ml/analyze";

            Map<String, String> request = new HashMap<>();
            request.put("resumeText", resumeText);
            request.put("jobDescription", jobDescription);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> mlResponse = response.getBody();
                return convertToMLResult(mlResponse);
            }
        } catch (Exception e) {
            log.error("ML service unavailable: {}", e.getMessage());
            // Return null — ApplicationService will save null score (shows as "-" in UI)
            return null;
        }

        // Fallback if response wasn't OK
        return null;
    }

    /**
     * Extract skills from resume using ML service
     */
    public Map<String, Object> extractSkills(String resumeText) {
        log.info("Extracting skills from resume");

        try {
            String url = mlServiceUrl + "/api/ml/extract-skills";

            Map<String, String> request = new HashMap<>();
            request.put("resumeText", resumeText);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error calling ML service: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Get course recommendations using ML service
     */
    public List<Map<String, String>> recommendCourses(List<String> currentSkills, List<String> targetSkills) {
        log.info("Getting course recommendations");

        try {
            String url = mlServiceUrl + "/api/ml/recommend-courses";

            Map<String, Object> request = new HashMap<>();
            request.put("currentSkills", currentSkills);
            request.put("targetSkills", targetSkills);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> mlResponse = response.getBody();
                return (List<Map<String, String>>) mlResponse.get("recommendations");
            }
        } catch (Exception e) {
            log.error("Error calling ML service: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Batch analyze applications using ML service
     */
    public List<Map<String, Object>> batchAnalyze(String jobId, List<Map<String, String>> applications) {
        log.info("Batch analyzing {} applications for job {}", applications.size(), jobId);

        try {
            String url = mlServiceUrl + "/api/ml/batch-analyze";

            Map<String, Object> request = new HashMap<>();
            request.put("jobId", jobId);
            request.put("applications", applications);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> mlResponse = response.getBody();
                return (List<Map<String, Object>>) mlResponse.get("results");
            }
        } catch (Exception e) {
            log.error("Error calling ML service: {}", e.getMessage());
        }

        return null;
    }

    // Helper method to convert ML response to MLAnalysisResult
    private MLAnalysisResult convertToMLResult(Map<String, Object> mlResponse) {
        MLAnalysisResult result = new MLAnalysisResult();
        // Flask sends match_percentage, semantic_score, etc.
        // Backend's Map expects matchScore, but the Flask response uses
        // match_percentage.
        // I'll handle both cases if needed, but per app.py:
        // response = { "matchScore": ..., "predictedRole": ... }

        result.setMatchScore(
                mlResponse.get("matchScore") != null ? ((Number) mlResponse.get("matchScore")).doubleValue() : 0.0);
        result.setSkillsMatched((List<String>) mlResponse.get("skillsMatched"));
        result.setSkillsGap((List<String>) mlResponse.get("skillsGap"));
        result.setConfidence(
                mlResponse.get("confidence") != null ? ((Number) mlResponse.get("confidence")).doubleValue() : 0.0);
        result.setPredictedRole((String) mlResponse.get("predictedRole"));

        return result;
    }

    // Mock data generator removed — null is returned when ML service is unavailable
}
