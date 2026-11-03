package com.aiproject.architect.modules.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubImportRequest {

    @NotBlank(message = "GitHub repository URL is required")
    private String repositoryUrl;

    private String branch;

    /** Optional personal access token for authorized private repository ingestion */
    private String accessToken;
}
