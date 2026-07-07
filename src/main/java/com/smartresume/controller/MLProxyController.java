package com.smartresume.controller;

import com.smartresume.model.MLAnalysisResult;
import com.smartresume.service.MLIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Proxy controller that forwards ML requests from the frontend to the ML microservice.
 * This ensures the Java Backend acts as a relay, making traffic visible in Render logs.
 */
@RestController
@RequestMapping("/api/ml")
@Slf4j
@SuppressWarnings("unchecked")
public class MLProxyController {

    @Autowired
    private MLIntegrationService mlIntegrationService;

    /**
     * 1-to-1 Resume-Job Match Analysis
     * Frontend sends resumeText + jobDescription → Backend forwards to ML → Returns scores
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMatch(@RequestBody Map<String, String> request) {
        try {
            String resumeText = request.get("resumeText");
            String jobDescription = request.get("jobDescription");

            if (resumeText == null || jobDescription == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing resumeText or jobDescription"));
            }

            log.info("ML Proxy: 1-to-1 analyze request received. Forwarding to ML Service...");
            MLAnalysisResult result = mlIntegrationService.analyzeMatch(resumeText, jobDescription, "", "");

            if (result == null) {
                return ResponseEntity.status(502).body(Map.of("error", "ML Service returned no result"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("matchScore", result.getMatchScore());
            response.put("skillsMatched", result.getSkillsMatched());
            response.put("skillsGap", result.getSkillsGap());
            response.put("confidence", result.getConfidence());
            response.put("predictedRole", result.getPredictedRole());

            log.info("ML Proxy: 1-to-1 analyze complete. Score: {}%", result.getMatchScore());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("ML Proxy analyze failed: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "ML Service Error: " + e.getMessage()));
        }
    }

    /**
     * 1-to-Many / Many-to-Many Matrix Analysis
     * Frontend sends jobDescriptions[] + applications[] → Backend forwards to ML → Returns matrix scores
     */
    @PostMapping("/matrix-analyze")
    public ResponseEntity<?> matrixAnalyze(@RequestBody Map<String, Object> request) {
        try {
            List<Map<String, Object>> jobDescriptions = (List<Map<String, Object>>) request.get("jobDescriptions");
            List<Map<String, Object>> applications = (List<Map<String, Object>>) request.get("applications");

            if (jobDescriptions == null || applications == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing jobDescriptions or applications"));
            }

            log.info("ML Proxy: Matrix analyze request received. {} JDs x {} Applications. Forwarding to ML Service...",
                    jobDescriptions.size(), applications.size());
            
            List<Map<String, Object>> results = mlIntegrationService.matrixAnalyze(jobDescriptions, applications);

            if (results == null) {
                return ResponseEntity.status(502).body(Map.of("error", "ML Service returned no result"));
            }

            log.info("ML Proxy: Matrix analyze complete. {} results returned.", results.size());
            return ResponseEntity.ok(Map.of("results", results));

        } catch (Exception e) {
            log.error("ML Proxy matrix-analyze failed: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "ML Service Error: " + e.getMessage()));
        }
    }
}
