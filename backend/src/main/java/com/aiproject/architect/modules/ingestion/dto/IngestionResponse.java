package com.aiproject.architect.modules.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {
    private String jobId;
    private String projectId;
    private String snapshotId;
    private String status;
    private Long fileCount;
    private Long totalBytes;
    private String message;
}
