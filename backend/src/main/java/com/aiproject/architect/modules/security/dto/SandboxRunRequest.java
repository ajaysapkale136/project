package com.aiproject.architect.modules.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxRunRequest {
    @Builder.Default
    private String testSuite = "ALL";
    @Builder.Default
    private int timeoutSeconds = 30;
}
