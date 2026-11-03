package com.aiproject.architect.modules.jobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgressEvent {
    private String jobId;
    private String status;
    private Integer progressPercent;
    private String currentStep;
    private String errorMessage;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
