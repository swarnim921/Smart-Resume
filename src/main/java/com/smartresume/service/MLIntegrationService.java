package com.smartresume.service;

import com.smartresume.model.*;
import com.smartresume.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLIntegrationService {

    private final MLAnalysisRepository mlAnalysisRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final JobApplicationService applicationService;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    @Value("${ml.service.enabled:false}")
    private boolean mlServiceEnabled;

    /**
     * Trigger ML analysis for a job application
     * If ML service is not available, returns mock data
     */
    public MLAnalysisResult analyzeApplication(String applicationId) {
        // Get application details
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Get job details
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Get resume details
        ResumeMeta resume = resumeRepository.findById(application.getResumeId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        MLAnalysisResult result;

        if (mlServiceEnabled) {
            // Call actual ML service
            result = callMLService(application, job, resume);
        } else {
            // Return mock data for development
            log.info("ML service disabled, returning mock analysis data");
            result = generateMockAnalysis(application, job);
        }

        // Save analysis result
        result = mlAnalysisRepository.save(result);

        // Link analysis to application
        applicationService.linkMLAnalysis(applicationId, result.getId());

        // Update application status based on ML result
        applicationService.updateApplicationStatus(applicationId, result.getSelectionStatus());

        return result;
    }

    /**
     * Get ML analysis result for an application
     */
    public Optional<MLAnalysisResult> getAnalysisForApplication(String applicationId) {
        return mlAnalysisRepository.findByApplicationId(applicationId);
    }

    /**
     * Get all analyses with a specific status
     */
    public List<MLAnalysisResult> getAnalysesByStatus(String status) {
        return mlAnalysisRepository.findBySelectionStatus(status);
    }

    /**
     * Get course recommendations for a user based on their applications
     */
    public List<CourseRecommendation> getCourseRecommendations(String userId) {
        List<JobApplication> applications = applicationRepository.findByUserId(userId);
        List<CourseRecommendation> allRecommendations = new ArrayList<>();

        for (JobApplication app : applications) {
            mlAnalysisRepository.findByApplicationId(app.getId())
                    .ifPresent(analysis -> {
                        if (analysis.getRecommendedCourses() != null) {
                            allRecommendations.addAll(analysis.getRecommendedCourses());
                        }
                    });
        }

        // Remove duplicates and sort by priority
        return allRecommendations.stream()
                .distinct()
                .sorted(Comparator.comparing(CourseRecommendation::getPriority))
                .toList();
    }

    /**
     * Call actual ML service (to be implemented when ML team provides API)
     */
    private MLAnalysisResult callMLService(JobApplication application, Job job, ResumeMeta resume) {
        try {
            // TODO: Implement actual ML service call when ML team provides endpoint
            // Example structure:
            // RestTemplate restTemplate = new RestTemplate();
            // Map<String, Object> request = Map.of(
            // "resumeId", resume.getGridFsId(),
            // "jobDescription", job.getDescription(),
            // "requiredSkills", job.getSkills()
            // );
            // MLAnalysisResult result = restTemplate.postForObject(
            // mlServiceUrl + "/analyze",
            // request,
            // MLAnalysisResult.class
            // );

            log.warn("ML service call not yet implemented, returning mock data");
            return generateMockAnalysis(application, job);
        } catch (Exception e) {
            log.error("Failed to call ML service: {}", e.getMessage());
            throw new RuntimeException("ML service unavailable: " + e.getMessage());
        }
    }

    /**
     * Generate mock analysis data for development/testing
     */
    private MLAnalysisResult generateMockAnalysis(JobApplication application, Job job) {
        MLAnalysisResult result = new MLAnalysisResult();
        result.setApplicationId(application.getId());

        // Generate random match percentage between 40-95
        double matchPercentage = 40 + (Math.random() * 55);
        result.setMatchPercentage(Math.round(matchPercentage * 100.0) / 100.0);

        // Determine selection status based on match percentage
        if (matchPercentage >= 75) {
            result.setSelectionStatus("SELECTED");
        } else if (matchPercentage >= 60) {
            result.setSelectionStatus("UNDER_REVIEW");
        } else {
            result.setSelectionStatus("REJECTED");
        }

        // Mock skill gaps
        result.setSkillGaps(Arrays.asList(
                "Advanced Java",
                "Spring Cloud",
                "Kubernetes",
                "Microservices Architecture"));

        // Mock course recommendations
        List<CourseRecommendation> courses = new ArrayList<>();

        CourseRecommendation course1 = new CourseRecommendation();
        course1.setSkillName("Advanced Java");
        course1.setCourseName("Java Programming Masterclass");
        course1.setCourseUrl("https://www.udemy.com/course/java-the-complete-java-developer-course/");
        course1.setProvider("Udemy");
        course1.setPriority(1);
        course1.setDuration("80 hours");
        course1.setLevel("Intermediate");
        courses.add(course1);

        CourseRecommendation course2 = new CourseRecommendation();
        course2.setSkillName("Kubernetes");
        course2.setCourseName("Kubernetes for Developers");
        course2.setCourseUrl("https://www.coursera.org/learn/kubernetes");
        course2.setProvider("Coursera");
        course2.setPriority(2);
        course2.setDuration("4 weeks");
        course2.setLevel("Intermediate");
        courses.add(course2);

        result.setRecommendedCourses(courses);
        result.setMlModelVersion("mock-v1.0");
        result.setNotes("This is mock data generated for development. Replace with actual ML service when available.");

        return result;
    }
}
