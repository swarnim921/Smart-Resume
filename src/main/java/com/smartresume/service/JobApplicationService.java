package com.smartresume.service;

import com.smartresume.model.Job;
import com.smartresume.model.JobApplication;
import com.smartresume.model.ResumeMeta;
import com.smartresume.repository.JobApplicationRepository;
import com.smartresume.repository.JobRepository;
import com.smartresume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    /**
     * Apply to a job with a resume
     */
    public JobApplication applyToJob(String userId, String jobId, String resumeId) {
        // Validate job exists
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        // Validate resume exists
        ResumeMeta resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + resumeId));

        // Verify resume belongs to user
        if (!resume.getUserId().equals(userId)) {
            throw new RuntimeException("Resume does not belong to the user");
        }

        // Check if user already applied to this job
        JobApplication existingApplication = applicationRepository.findByUserIdAndJobId(userId, jobId);
        if (existingApplication != null) {
            throw new RuntimeException("You have already applied to this job");
        }

        // Create new application
        JobApplication application = new JobApplication();
        application.setUserId(userId);
        application.setJobId(jobId);
        application.setResumeId(resumeId);
        application.setAppliedAt(new Date());
        application.setStatus("PENDING");

        return applicationRepository.save(application);
    }

    /**
     * Get all applications by user
     */
    public List<JobApplication> getUserApplications(String userId) {
        return applicationRepository.findByUserId(userId);
    }

    /**
     * Get all applications for a job (Admin)
     */
    public List<JobApplication> getJobApplications(String jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    /**
     * Get application by ID
     */
    public Optional<JobApplication> getApplicationById(String id) {
        return applicationRepository.findById(id);
    }

    /**
     * Update application status (Admin or ML service)
     */
    public JobApplication updateApplicationStatus(String applicationId, String status) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));

        // Validate status
        if (!isValidStatus(status)) {
            throw new RuntimeException("Invalid status. Must be one of: PENDING, SELECTED, REJECTED, UNDER_REVIEW");
        }

        application.setStatus(status);
        return applicationRepository.save(application);
    }

    /**
     * Link ML analysis to application
     */
    public JobApplication linkMLAnalysis(String applicationId, String mlAnalysisId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));

        application.setMlAnalysisId(mlAnalysisId);
        return applicationRepository.save(application);
    }

    /**
     * Delete application
     */
    public void deleteApplication(String id) {
        if (!applicationRepository.existsById(id)) {
            throw new RuntimeException("Application not found with id: " + id);
        }
        applicationRepository.deleteById(id);
    }

    /**
     * Validate status value
     */
    private boolean isValidStatus(String status) {
        return status.equals("PENDING") ||
                status.equals("SELECTED") ||
                status.equals("REJECTED") ||
                status.equals("UNDER_REVIEW");
    }
}
