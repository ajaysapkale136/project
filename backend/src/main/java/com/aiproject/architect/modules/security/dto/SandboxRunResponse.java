package com.aiproject.architect.modules.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxRunResponse {
    private String executionId;
    private String status; // PASSED, FAILED, TIMED_OUT
    private long durationMs;
    private int testsRun;
    private int testsPassed;
    private int testsFailed;
    private String consoleOutput;
    private String sandboxIsolationPath;
}
