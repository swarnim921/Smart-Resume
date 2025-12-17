package com.smartresume.service;

import com.smartresume.model.Job;
import com.smartresume.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;

    public Job createJob(Job job, String recruiterEmail) {
        job.setPostedBy(recruiterEmail);
        job.setPostedAt(LocalDateTime.now());
        job.setStatus("ACTIVE");
        return jobRepository.save(job);
    }

    public List<Job> getAllActiveJobs() {
        return jobRepository.findByStatus("ACTIVE");
    }

    public Optional<Job> getJobById(String id) {
        return jobRepository.findById(id);
    }

    public Job updateJob(String id, Job updatedJob, String recruiterEmail) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized to update this job");
        }

        job.setTitle(updatedJob.getTitle());
        job.setCompany(updatedJob.getCompany());
        job.setLocation(updatedJob.getLocation());
        job.setType(updatedJob.getType());
        job.setSalary(updatedJob.getSalary());
        job.setDescription(updatedJob.getDescription());
        job.setRequirements(updatedJob.getRequirements());
        job.setStatus(updatedJob.getStatus());

        return jobRepository.save(job);
    }

    public void deleteJob(String id, String recruiterEmail) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPostedBy().equals(recruiterEmail)) {
            throw new RuntimeException("Not authorized to delete this job");
        }

        jobRepository.deleteById(id);
    }

    public List<Job> getJobsByRecruiter(String recruiterEmail) {
        return jobRepository.findByPostedBy(recruiterEmail);
    }
}
