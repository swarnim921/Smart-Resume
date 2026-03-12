package com.smartresume.service;

import com.smartresume.model.*;
import com.smartresume.repository.ApplicationRepository;
import com.smartresume.repository.JobRepository;
import com.smartresume.repository.ResumeRepository;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final MLIntegrationService mlIntegrationService;
    private final ResumeService resumeService;
    private final EmailService emailService;

    private static final double ATS_THRESHOLD = 50.0;
    private static final int DAILY_APPLICATION_LIMIT = 10;

    public Application applyToJob(String jobId, String candidateEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if ("CLOSED".equals(job.getStatus())) {
            throw new RuntimeException("This job listing is no longer accepting applications");
        }

        if (applicationRepository.existsByJobIdAndCandidateEmail(jobId, candidateEmail)) {
            throw new RuntimeException("You have already applied to this job");
        }

        // Daily limit check
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        long todayCount = applicationRepository.countByCandidateEmailAndAppliedAtBetween(
                candidateEmail, startOfDay, endOfDay);
        if (todayCount >= DAILY_APPLICATION_LIMIT) {
            throw new RuntimeException("Daily application limit reached (" + DAILY_APPLICATION_LIMIT +
                    " applications per day). Please try again tomorrow.");
        }

        User user = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ResumeMeta> resumes = resumeRepository.findByOwnerId(user.getId());
        if (resumes.isEmpty()) {
            throw new RuntimeException("Please upload your resume before applying");
        }

        ResumeMeta resume = resumes.get(resumes.size() - 1);

        // Profile Guard: Check for minimum profile completion
        boolean isProfileComplete = (user.getSkills() != null && !user.getSkills().isEmpty()) &&
                                  (user.getBio() != null && user.getBio().length() >= 20);
        
        if (!isProfileComplete) {
            throw new RuntimeException("Profile incomplete! Please add your skills and a short bio in 'My Profile' before applying.");
        }

        Application application = new Application();
        application.setJobId(jobId);
        application.setJobTitle(job.getTitle());
        application.setJobCompany(job.getCompany());
        application.setCandidateEmail(candidateEmail);
        application.setCandidateName(user.getName());
        application.setResumeId(resume.getId());
        application.setAppliedAt(LocalDateTime.now());
        application.setNotesHistory(new ArrayList<>());
        
        // Populate candidate metadata for Recruiter Industry View
        application.setCandidateBio(user.getBio());
        application.setCandidateLinkedin(user.getLinkedinUrl());
        application.setCandidateGithub(user.getGithubUrl());
        application.setCandidateSkills(user.getSkills());
        application.setCandidateExperience(user.getYearsOfExperience());

        // Run ATS/ML scoring
        try {
            String resumeText = resumeService.extractTextFromResume(resume.getId());
            String jobDescription = job.getDescription() + " " + job.getRequirements();
            var mlResult = mlIntegrationService.analyzeMatch(
                    resumeText, jobDescription, job.getTitle(), job.getRequirements());

                if (mlResult != null) {
                application.setMatchScore(mlResult.getMatchScore());
                application.setSkillsGap(mlResult.getSkillsGap());
                application.setPredictedRole(mlResult.getPredictedRole());

                if (mlResult.getMatchScore() < ATS_THRESHOLD) {
                    application.setStatus("ATS_REJECTED");
                    System.out.println("❌ ATS rejected: score=" + mlResult.getMatchScore() + ", role="
                            + mlResult.getPredictedRole());
                    Application saved = applicationRepository.save(application);
                    emailService.sendStatusUpdateEmailWithJob(candidateEmail, user.getName(), job, "ATS_REJECTED",
                            "ATS screening", null);
                    return saved;
                } else {
                    application.setStatus("UNDER_REVIEW");
                    System.out.println("✅ ATS passed: score=" + mlResult.getMatchScore() + ", role="
                            + mlResult.getPredictedRole());
                    Application saved = applicationRepository.save(application);
                    emailService.sendApplicationConfirmationEmail(candidateEmail, user.getName(), job.getTitle(), job.getCompany());
                    return saved;
                }
            } else {
                application.setStatus("PENDING");
            }
        } catch (Exception e) {
            System.err.println("❌ ML/ATS failed: " + e.getMessage());
            application.setStatus("PENDING");
        }

        Application saved = applicationRepository.save(application);
        emailService.sendApplicationConfirmationEmail(candidateEmail, user.getName(), job.getTitle(), job.getCompany());
        return saved;
    }

    public List<Application> getCandidateApplications(String candidateEmail) {
        return applicationRepository.findByCandidateEmail(candidateEmail);
    }

    public List<Application> getJobApplications(String jobId, String recruiterEmail) {
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

        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized to update this application");
        }

        application.setStatus(status);
        application.setReviewedAt(LocalDateTime.now());
        // Keep legacy single-note field for compatibility
        if (notes != null && !notes.isEmpty()) {
            application.setReviewNotes(notes);
        }
        // Append to notes history timeline
        if (notes != null && !notes.isEmpty()) {
            List<ApplicationNote> history = application.getNotesHistory();
            if (history == null)
                history = new ArrayList<>();
            history.add(ApplicationNote.builder()
                    .text(notes)
                    .recruiterEmail(recruiterEmail)
                    .timestamp(LocalDateTime.now())
                    .status(status)
                    .build());
            application.setNotesHistory(history);
        }

        return applicationRepository.save(application);
    }

    public List<Application> updateBulkStatus(List<String> ids, String status, String stageName, String notes,
            String recruiterEmail) {
        List<Application> updatedApps = new ArrayList<>();
        for (String id : ids) {
            try {
                Application app = updateApplicationStatus(id, status, notes, recruiterEmail);
                updatedApps.add(app);

                // Send email notification for each updated application
                try {
                    Job job = jobRepository.findById(app.getJobId()).orElse(null);
                    if (job != null) {
                        // If stageName is null/empty, use status as fallback
                        String finalStageName = (stageName != null && !stageName.isEmpty()) ? stageName : status;

                        emailService.sendStatusUpdateEmailWithJob(
                                app.getCandidateEmail(),
                                app.getCandidateName(),
                                job, status, finalStageName, notes);
                    }
                } catch (Exception e) {
                    System.err.println("Bulk email failed for " + app.getCandidateEmail() + ": " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Failed to update app " + id + " in bulk: " + e.getMessage());
            }
        }
        return updatedApps;
    }

    /**
     * Re-run ATS ML analysis on an existing application (candidate updated resume,
     * or recruiter requested).
     */
    public Application reAnalyze(String applicationId, String requesterEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Verify authorization: either the candidate themselves or the job's recruiter
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));
        boolean isCandidate = application.getCandidateEmail().equals(requesterEmail);
        boolean isRecruiter = job.getPostedBy().equals(requesterEmail);
        if (!isCandidate && !isRecruiter) {
            throw new RuntimeException("Not authorized to re-analyze this application");
        }

        try {
            User user = userRepository.findByEmail(application.getCandidateEmail())
                    .orElseThrow(() -> new RuntimeException("Candidate not found"));
            List<ResumeMeta> resumes = resumeRepository.findByOwnerId(user.getId());
            if (resumes.isEmpty())
                throw new RuntimeException("No resume found");

            ResumeMeta resume = resumes.get(resumes.size() - 1);
            String resumeText = resumeService.extractTextFromResume(resume.getId());
            String jobDescription = job.getDescription() + " " + job.getRequirements();

            var mlResult = mlIntegrationService.analyzeMatch(
                    resumeText, jobDescription, job.getTitle(), job.getRequirements());

            if (mlResult != null) {
                application.setMatchScore(mlResult.getMatchScore());
                application.setSkillsGap(mlResult.getSkillsGap());
                application.setPredictedRole(mlResult.getPredictedRole());
                application.setReAnalyzedAt(LocalDateTime.now());

                // Update ATS status if it was previously pending
                if ("PENDING".equals(application.getStatus())) {
                    if (mlResult.getMatchScore() < ATS_THRESHOLD) {
                        application.setStatus("ATS_REJECTED");
                    } else {
                        application.setStatus("UNDER_REVIEW");
                    }
                }
                System.out.println(
                        "✅ Re-analyzed: score=" + mlResult.getMatchScore() + ", role=" + mlResult.getPredictedRole());
            }
        } catch (Exception e) {
            System.err.println("❌ Re-analysis failed: " + e.getMessage());
            throw new RuntimeException("Re-analysis failed: " + e.getMessage());
        }

        return applicationRepository.save(application);
    }

    /**
     * Export all applications for a job as a CSV byte array.
     */
    public ResponseEntity<byte[]> exportCsv(String jobId, String recruiterEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized");
        }

        List<Application> apps = applicationRepository.findByJobId(jobId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Email,Status,ATS Score,Predicted Role,Applied At,Last Reviewed,Skills Gap\n");
        for (Application a : apps) {
            csv.append(escape(a.getCandidateName())).append(",");
            csv.append(escape(a.getCandidateEmail())).append(",");
            csv.append(escape(a.getStatus())).append(",");
            csv.append(a.getMatchScore() != null ? String.format("%.1f%%", a.getMatchScore()) : "-").append(",");
            csv.append(escape(a.getPredictedRole() != null ? a.getPredictedRole() : "-")).append(",");
            csv.append(a.getAppliedAt() != null ? a.getAppliedAt().format(fmt) : "-").append(",");
            csv.append(a.getReviewedAt() != null ? a.getReviewedAt().format(fmt) : "-").append(",");
            String gap = a.getSkillsGap() != null ? String.join("; ", a.getSkillsGap()) : "";
            csv.append(escape(gap)).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "applications-" + job.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private String escape(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public ResponseEntity<?> getApplicationResume(String applicationId, String recruiterEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized to view this resume");
        }
        String resumeId = application.getResumeId();
        if (resumeId == null)
            throw new RuntimeException("No resume found for this application");
        String resumeUrl = "/api/resumes/" + resumeId + "/download";
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .header("Location", resumeUrl).build();
    }

    public void withdrawApplication(String applicationId, String candidateEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!application.getCandidateEmail().equals(candidateEmail)) {
            throw new RuntimeException("Not authorized to withdraw this application");
        }
        applicationRepository.deleteById(applicationId);
    }
}
