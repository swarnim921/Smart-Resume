package com.smartresume.service;

import com.smartresume.model.Application;
import com.smartresume.model.Job;
import com.smartresume.model.ResumeMeta;
import com.smartresume.model.User;
import com.smartresume.repository.ApplicationRepository;
import com.smartresume.repository.JobRepository;
import com.smartresume.repository.ResumeRepository;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    public Application applyToJob(String jobId, String candidateEmail) {
        // Check if job exists
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Check if already applied
        if (applicationRepository.existsByJobIdAndCandidateEmail(jobId, candidateEmail)) {
            throw new RuntimeException("Already applied to this job");
        }

        // Get user details
        User user = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has uploaded resume
        List<ResumeMeta> resumes = resumeRepository.findByOwnerId(user.getId());
        if (resumes.isEmpty()) {
            throw new RuntimeException("Please upload your resume first");
        }

        // Get the most recent resume
        ResumeMeta resume = resumes.get(resumes.size() - 1);

        // Create application
        Application application = new Application();
        application.setJobId(jobId);
        application.setCandidateEmail(candidateEmail);
        application.setCandidateName(user.getName());
        application.setResumeId(resume.getId());
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());

        return applicationRepository.save(application);
    }

    public List<Application> getCandidateApplications(String candidateEmail) {
        return applicationRepository.findByCandidateEmail(candidateEmail);
    }

    public List<Application> getJobApplications(String jobId, String recruiterEmail) {
        // Verify the job belongs to this recruiter
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized to view these applications");
        }

        return applicationRepository.findByJobId(jobId);
    }

    public Application updateApplicationStatus(String id, String status, String notes, String recruiterEmail) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Verify the job belongs to this recruiter
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized to update this application");
        }

        application.setStatus(status);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewNotes(notes);

        return applicationRepository.save(application);
    }
}
