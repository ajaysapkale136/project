package com.aiproject.architect.modules.testing.dto;

import com.aiproject.architect.modules.testing.entity.TestExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionReportResponse {
    private TestExecution execution;
    private String environment;
    private String testSummary;
}
