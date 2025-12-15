package com.smartresume.controller;

import com.smartresume.model.CourseRecommendation;
import com.smartresume.model.MLAnalysisResult;
import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import com.smartresume.service.MLIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MLController {

    private final MLIntegrationService mlService;
    private final UserRepository userRepository;

    /**
     * Trigger ML analysis for an application (Admin or application owner)
     */
    @PostMapping("/analyze/{applicationId}")
    public ResponseEntity<?> analyzeApplication(@PathVariable String applicationId, Authentication auth) {
        try {
            MLAnalysisResult result = mlService.analyzeApplication(applicationId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to analyze application: " + e.getMessage()));
        }
    }

    /**
     * Get ML analysis result for an application
     */
    @GetMapping("/results/{applicationId}")
    public ResponseEntity<?> getAnalysisResult(@PathVariable String applicationId) {
        try {
            return mlService.getAnalysisForApplication(applicationId)
                    .map(result -> ResponseEntity.ok((Object) result))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No analysis found for this application")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch analysis: " + e.getMessage()));
        }
    }

    /**
     * Get all analyses with a specific status (Admin only)
     */
    @GetMapping("/results/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAnalysesByStatus(@PathVariable String status) {
        try {
            List<MLAnalysisResult> results = mlService.getAnalysesByStatus(status);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch analyses: " + e.getMessage()));
        }
    }

    /**
     * Get course recommendations for current user
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getCourseRecommendations(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            List<CourseRecommendation> recommendations = mlService.getCourseRecommendations(user.getId());
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recommendations: " + e.getMessage()));
        }
    }

    /**
     * Get course recommendations for a specific user (Admin only)
     */
    @GetMapping("/recommendations/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCourseRecommendationsForUser(@PathVariable String userId) {
        try {
            List<CourseRecommendation> recommendations = mlService.getCourseRecommendations(userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recommendations: " + e.getMessage()));
        }
    }
}
