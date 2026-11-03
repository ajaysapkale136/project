package com.aiproject.architect.modules.jobs.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.jobs.entity.GenerationJob;
import com.aiproject.architect.modules.jobs.service.JobExecutionService;
import com.aiproject.architect.modules.jobs.service.JobProgressBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobExecutionService jobExecutionService;
    private final JobProgressBroadcaster broadcaster;

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<GenerationJob>> getJobStatus(@PathVariable String jobId) {
        GenerationJob job = jobExecutionService.getJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @GetMapping(value = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobEvents(@PathVariable String jobId) {
        return broadcaster.register(jobId);
    }
}
