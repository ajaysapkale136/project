package com.aiproject.architect.modules.jobs.service;

import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.jobs.dto.JobProgressEvent;
import com.aiproject.architect.modules.jobs.entity.GenerationJob;
import com.aiproject.architect.modules.jobs.repository.GenerationJobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    private final GenerationJobRepository jobRepository;
    private final JobProgressBroadcaster broadcaster;

    @Transactional
    public GenerationJob createJob(String projectId, String jobType, String initialStep) {
        GenerationJob job = GenerationJob.builder()
                .projectId(projectId)
                .jobType(jobType)
                .status("RUNNING")
                .progressPercent(5)
                .currentStep(initialStep)
                .startedAt(LocalDateTime.now())
                .build();
        GenerationJob saved = jobRepository.save(job);
        broadcastProgress(saved);
        return saved;
    }

    @Transactional
    public void updateProgress(String jobId, int progressPercent, String step) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setProgressPercent(progressPercent);
            job.setCurrentStep(step);
            GenerationJob updated = jobRepository.save(job);
            broadcastProgress(updated);
        });
    }

    @Transactional
    public void completeJob(String jobId, String finalStep) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("COMPLETED");
            job.setProgressPercent(100);
            job.setCurrentStep(finalStep);
            job.setCompletedAt(LocalDateTime.now());
            GenerationJob updated = jobRepository.save(job);
            broadcastProgress(updated);
            log.info("Job {} completed successfully: {}", jobId, finalStep);
        });
    }

    @Transactional
    public void failJob(String jobId, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("FAILED");
            job.setErrorMessage(errorMessage);
            job.setCurrentStep("Job failed: " + errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            GenerationJob updated = jobRepository.save(job);
            broadcastProgress(updated);
            log.error("Job {} failed: {}", jobId, errorMessage);
        });
    }

    public GenerationJob getJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("GenerationJob", jobId));
    }

    private void broadcastProgress(GenerationJob job) {
        JobProgressEvent event = JobProgressEvent.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .progressPercent(job.getProgressPercent())
                .currentStep(job.getCurrentStep())
                .errorMessage(job.getErrorMessage())
                .build();
        broadcaster.broadcast(job.getId(), event);
    }
}
