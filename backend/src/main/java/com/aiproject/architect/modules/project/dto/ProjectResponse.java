package com.aiproject.architect.modules.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private String id;
    private String name;
    private String description;
    private String repositoryUrl;
    private String defaultBranch;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Supplementary metrics for UI cards
    private Long fileCount;
    private Long totalBytes;
    private Integer totalEndpoints;
    private Integer totalEntities;
    private String primaryLanguage;
    private String frameworkFamily;
}
