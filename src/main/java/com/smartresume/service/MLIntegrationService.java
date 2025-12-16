package com.smartresume.service;

import com.smartresume.model.MLAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class MLIntegrationService {

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Analyze resume-job match using ML service
     * INTEGRATION READY: Uncomment when ML team delivers
     */
    public MLAnalysisResult analyzeMatch(String resumeText, String jobDescription, String jobTitle,
            String jobRequirements) {
        log.info("Analyzing resume-job match for job: {}", jobTitle);

        // TODO: Uncomment this when ML service is ready
        /*
         * try {
         * String url = mlServiceUrl + "/api/ml/analyze";
         * 
         * Map<String, String> request = new HashMap<>();
         * request.put("resumeText", resumeText);
         * request.put("jobDescription", jobDescription);
         * request.put("jobTitle", jobTitle);
         * request.put("jobRequirements", jobRequirements);
         * 
         * HttpHeaders headers = new HttpHeaders();
         * headers.setContentType(MediaType.APPLICATION_JSON);
         * HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
         * 
         * ResponseEntity<Map> response = restTemplate.postForEntity(url, entity,
         * Map.class);
         * 
         * if (response.getStatusCode() == HttpStatus.OK) {
         * Map<String, Object> mlResponse = response.getBody();
         * return convertToMLResult(mlResponse);
         * }
         * } catch (Exception e) {
         * log.error("Error calling ML service: {}", e.getMessage());
         * }
         */

        // MOCK DATA - Remove when ML service is ready
        return createMockResult(resumeText, jobTitle);
    }

    /**
     * Extract skills from resume using ML service
     * INTEGRATION READY: Uncomment when ML team delivers
     */
    public Map<String, Object> extractSkills(String resumeText) {
        log.info("Extracting skills from resume");

        // TODO: Uncomment this when ML service is ready
        /*
         * try {
         * String url = mlServiceUrl + "/api/ml/extract-skills";
         * 
         * Map<String, String> request = new HashMap<>();
         * request.put("resumeText", resumeText);
         * 
         * HttpHeaders headers = new HttpHeaders();
         * headers.setContentType(MediaType.APPLICATION_JSON);
         * HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
         * 
         * ResponseEntity<Map> response = restTemplate.postForEntity(url, entity,
         * Map.class);
         * 
         * if (response.getStatusCode() == HttpStatus.OK) {
         * return response.getBody();
         * }
         * } catch (Exception e) {
         * log.error("Error calling ML service: {}", e.getMessage());
         * }
         */

        // MOCK DATA
        Map<String, Object> skills = new HashMap<>();
        skills.put("technicalSkills", Arrays.asList("Python", "Java", "React", "MongoDB"));
        skills.put("softSkills", Arrays.asList("Leadership", "Communication"));
        skills.put("experience", "5 years");
        return skills;
    }

    /**
     * Get course recommendations using ML service
     * INTEGRATION READY: Uncomment when ML team delivers
     */
    public List<Map<String, String>> recommendCourses(List<String> currentSkills, List<String> targetSkills) {
        log.info("Getting course recommendations");

        // TODO: Uncomment this when ML service is ready
        /*
         * try {
         * String url = mlServiceUrl + "/api/ml/recommend-courses";
         * 
         * Map<String, Object> request = new HashMap<>();
         * request.put("currentSkills", currentSkills);
         * request.put("targetSkills", targetSkills);
         * 
         * HttpHeaders headers = new HttpHeaders();
         * headers.setContentType(MediaType.APPLICATION_JSON);
         * HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
         * 
         * ResponseEntity<Map> response = restTemplate.postForEntity(url, entity,
         * Map.class);
         * 
         * if (response.getStatusCode() == HttpStatus.OK) {
         * Map<String, Object> mlResponse = response.getBody();
         * return (List<Map<String, String>>) mlResponse.get("recommendations");
         * }
         * } catch (Exception e) {
         * log.error("Error calling ML service: {}", e.getMessage());
         * }
         */

        // MOCK DATA
        List<Map<String, String>> courses = new ArrayList<>();
        Map<String, String> course = new HashMap<>();
        course.put("courseName", "AWS Certified Developer");
        course.put("provider", "Udemy");
        course.put("priority", "High");
        courses.add(course);
        return courses;
    }

    /**
     * Batch analyze applications using ML service
     * INTEGRATION READY: Uncomment when ML team delivers
     */
    public List<Map<String, Object>> batchAnalyze(String jobId, List<Map<String, String>> applications) {
        log.info("Batch analyzing {} applications for job {}", applications.size(), jobId);

        // TODO: Uncomment this when ML service is ready
        /*
         * try {
         * String url = mlServiceUrl + "/api/ml/batch-analyze";
         * 
         * Map<String, Object> request = new HashMap<>();
         * request.put("jobId", jobId);
         * request.put("applications", applications);
         * 
         * HttpHeaders headers = new HttpHeaders();
         * headers.setContentType(MediaType.APPLICATION_JSON);
         * HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
         * 
         * ResponseEntity<Map> response = restTemplate.postForEntity(url, entity,
         * Map.class);
         * 
         * if (response.getStatusCode() == HttpStatus.OK) {
         * Map<String, Object> mlResponse = response.getBody();
         * return (List<Map<String, Object>>) mlResponse.get("results");
         * }
         * } catch (Exception e) {
         * log.error("Error calling ML service: {}", e.getMessage());
         * }
         */

        // MOCK DATA
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < applications.size(); i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("applicationId", applications.get(i).get("applicationId"));
            result.put("matchScore", 85.0 - (i * 5));
            result.put("rank", i + 1);
            results.add(result);
        }
        return results;
    }

    // Helper method to convert ML response to MLAnalysisResult
    private MLAnalysisResult convertToMLResult(Map<String, Object> mlResponse) {
        MLAnalysisResult result = new MLAnalysisResult();
        result.setMatchScore((Double) mlResponse.get("matchScore"));
        result.setSkillsMatched((List<String>) mlResponse.get("skillsMatched"));
        result.setSkillsGap((List<String>) mlResponse.get("skillsGap"));
        result.setConfidence((Double) mlResponse.get("confidence"));
        // Add more mappings as needed
        return result;
    }

    // Mock data generator - Remove when ML service is ready
    private MLAnalysisResult createMockResult(String resumeText, String jobTitle) {
        MLAnalysisResult result = new MLAnalysisResult();
        result.setMatchScore(85.5);
        result.setSkillsMatched(Arrays.asList("Python", "Java", "React"));
        result.setSkillsGap(Arrays.asList("AWS", "Docker"));
        result.setConfidence(0.87);
        result.setStatus("COMPLETED");
        return result;
    }
}
