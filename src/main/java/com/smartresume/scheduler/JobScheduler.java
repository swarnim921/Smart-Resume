package com.smartresume.scheduler;

import com.smartresume.model.Job;
import com.smartresume.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that auto-closes job listings past their expiry date.
 * Runs hourly. If a job has expiresAt set and it's in the past, status →
 * CLOSED.
 */
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;

    @Scheduled(fixedRate = 3_600_000) // every hour
    public void autoCloseExpiredJobs() {
        List<Job> activeJobs = jobRepository.findByStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        int closed = 0;
        for (Job job : activeJobs) {
            if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(now)) {
                job.setStatus("CLOSED");
                jobRepository.save(job);
                closed++;
                System.out.println("🔒 Auto-closed expired job: " + job.getTitle() + " (id=" + job.getId() + ")");
            }
        }
        if (closed > 0) {
            System.out.println("✅ Auto-closed " + closed + " expired job(s)");
        }
    }
}
