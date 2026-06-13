package com.smartresume.controller;

import com.smartresume.model.Job;
import com.smartresume.service.JobService;
import com.smartresume.model.ResumeMeta;
import com.smartresume.model.User;
import com.smartresume.repository.ResumeRepository;
import com.smartresume.repository.UserRepository;
import com.smartresume.service.MLIntegrationService;
import com.smartresume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeService resumeService;
    private final MLIntegrationService mlIntegrationService;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllActiveJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable String id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> createJob(@RequestBody Job job, Authentication auth) {
        String recruiterEmail = auth.getName();
        Job createdJob = jobService.createJob(job, recruiterEmail);
        return ResponseEntity.ok(createdJob);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> updateJob(@PathVariable String id, @RequestBody Job job, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            Job updatedJob = jobService.updateJob(id, job, recruiterEmail);
            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteJob(@PathVariable String id, Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            jobService.deleteJob(id, recruiterEmail);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<Job>> getMyJobs(Authentication auth) {
        try {
            String recruiterEmail = auth.getName();
            List<Job> jobs = jobService.getJobsByRecruiter(recruiterEmail);
            return ResponseEntity.ok(jobs != null ? jobs : List.of());
        } catch (Exception e) {
            // Return empty list instead of error if something goes wrong
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/match-my-resume")
    @PreAuthorize("hasAnyRole('USER', 'CANDIDATE')")
    public ResponseEntity<?> matchMyResume(Authentication auth) {
        try {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || user.getResumeUploaded() == null || !user.getResumeUploaded()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "No resume uploaded. Please upload a resume first."));
            }

            List<ResumeMeta> resumes = resumeRepository.findByOwnerId(user.getId());
            if (resumes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Resume file not found."));
            }

            ResumeMeta latestResume = resumes.get(resumes.size() - 1);
            String resumeText = resumeService.extractTextFromResume(latestResume.getId());

            List<Job> activeJobs = jobService.getAllActiveJobs();
            if (activeJobs.isEmpty()) {
                return ResponseEntity.ok(List.of()); // No jobs to match
            }

            // Prepare ML payloads
            List<Map<String, Object>> mlPayloadJds = new ArrayList<>();
            for (Job job : activeJobs) {
                Map<String, Object> jdObj = new HashMap<>();
                jdObj.put("jobId", job.getId());
                // Combine Title and Description for ML
                jdObj.put("jobDescriptionText", job.getTitle() + "\n" + job.getDescription() + "\n" + job.getRequirements());
                mlPayloadJds.add(jdObj);
            }

            List<Map<String, Object>> mlPayloadApps = new ArrayList<>();
            Map<String, Object> appObj = new HashMap<>();
            appObj.put("applicationId", latestResume.getId());
            appObj.put("resumeText", resumeText);
            mlPayloadApps.add(appObj);

            // Call Matrix Analyze
            List<Map<String, Object>> matrixResults = mlIntegrationService.matrixAnalyze(mlPayloadJds, mlPayloadApps);
            if (matrixResults == null || matrixResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "ML Service failed to process match."));
            }

            // matrixResults is a List of candidate matches. Since we sent 1 application, it's index 0.
            Map<String, Object> myMatches = matrixResults.get(0);
            List<Map<String, Object>> jobMatches = (List<Map<String, Object>>) myMatches.get("matches");
            
            if (jobMatches == null) {
                jobMatches = new ArrayList<>();
            }

            // Map the Job details into the jobMatches so frontend can render them
            List<Map<String, Object>> finalRankedJobs = new ArrayList<>();
            for (Map<String, Object> match : jobMatches) {
                String jobId = (String) match.get("jobId");
                // Find corresponding job
                Job matchingJob = activeJobs.stream().filter(j -> j.getId().equals(jobId)).findFirst().orElse(null);
                if (matchingJob != null) {
                    Map<String, Object> rankedJob = new HashMap<>();
                    rankedJob.put("job", matchingJob);
                    rankedJob.put("matchScore", match.get("matchScore"));
                    rankedJob.put("skillsMatched", match.get("skillsMatched"));
                    rankedJob.put("skillsGap", match.get("skillsGap"));
                    finalRankedJobs.add(rankedJob);
                }
            }

            // Sort by matchScore descending
            finalRankedJobs.sort((a, b) -> {
                Number scoreA = (Number) a.get("matchScore");
                Number scoreB = (Number) b.get("matchScore");
                if (scoreA == null) scoreA = 0;
                if (scoreB == null) scoreB = 0;
                return Double.compare(scoreB.doubleValue(), scoreA.doubleValue());
            });

            return ResponseEntity.ok(finalRankedJobs);

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error: " + e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        }
    }
}
