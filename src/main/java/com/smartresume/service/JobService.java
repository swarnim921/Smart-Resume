package com.smartresume.service;

import com.smartresume.model.Job;
import com.smartresume.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    /**
     * Create a new job posting
     */
    public Job createJob(Job job) {
        if (job.getPostedAt() == null) {
            job.setPostedAt(new Date());
        }
        return jobRepository.save(job);
    }

    /**
     * Get all jobs
     */
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * Get job by ID
     */
    public Optional<Job> getJobById(String id) {
        return jobRepository.findById(id);
    }

    /**
     * Update existing job
     */
    public Job updateJob(String id, Job updatedJob) {
        return jobRepository.findById(id)
                .map(existingJob -> {
                    existingJob.setTitle(updatedJob.getTitle());
                    existingJob.setDescription(updatedJob.getDescription());
                    existingJob.setCompany(updatedJob.getCompany());
                    existingJob.setLocation(updatedJob.getLocation());
                    existingJob.setSkills(updatedJob.getSkills());
                    existingJob.setPostedDate(updatedJob.getPostedDate());
                    return jobRepository.save(existingJob);
                })
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    /**
     * Delete job by ID
     */
    public void deleteJob(String id) {
        if (!jobRepository.existsById(id)) {
            throw new RuntimeException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
    }

    /**
     * Search jobs by title or skills (simple implementation)
     */
    public List<Job> searchJobs(String keyword) {
        // For now, return all jobs. Can be enhanced with custom queries
        return jobRepository.findAll().stream()
                .filter(job -> (job.getTitle() != null && job.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                        ||
                        (job.getSkills() != null && job.getSkills().toLowerCase().contains(keyword.toLowerCase())) ||
                        (job.getCompany() != null && job.getCompany().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
    }
}
