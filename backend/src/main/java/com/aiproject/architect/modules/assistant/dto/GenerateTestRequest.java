package com.aiproject.architect.modules.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestRequest {
    private String proposalId;
    private String targetFilePath;
    private String framework; // JUNIT5_MOCKITO, JEST_RTL, PYTEST
}
