package com.aiproject.architect.modules.testing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunTestRequest {
    private String changeSetId;
    @Builder.Default
    private String testType = "ALL"; // UNIT, INTEGRATION, REGRESSION, SECURITY, ALL
    @Builder.Default
    private int timeoutSeconds = 60;
}
