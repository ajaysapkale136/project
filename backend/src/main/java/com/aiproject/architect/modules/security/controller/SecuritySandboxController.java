package com.aiproject.architect.modules.security.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.security.dto.*;
import com.aiproject.architect.modules.security.service.SecuritySandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/security")
@RequiredArgsConstructor
public class SecuritySandboxController {

    private final SecuritySandboxService securityService;

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<SecurityScanResponse>> runScan(@PathVariable String projectId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Security scan completed", securityService.runSecurityScan(projectId)));
    }

    @GetMapping("/scans/latest")
    public ResponseEntity<ApiResponse<SecurityScanResponse>> getLatestScan(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Latest scan retrieved", securityService.getLatestScan(projectId)));
    }

    @PostMapping("/contracts/verify")
    public ResponseEntity<ApiResponse<ContractVerificationMatrixResponse>> verifyContracts(@PathVariable String projectId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("API contract verification completed", securityService.verifyContracts(projectId)));
    }

    @GetMapping("/contracts")
    public ResponseEntity<ApiResponse<ContractVerificationMatrixResponse>> getContracts(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("API contracts retrieved", securityService.getContracts(projectId)));
    }

    @PostMapping("/sandbox/run")
    public ResponseEntity<ApiResponse<SandboxRunResponse>> runSandbox(
            @PathVariable String projectId,
            @RequestBody(required = false) SandboxRunRequest request) {
        if (request == null) {
            request = new SandboxRunRequest();
        }
        return ResponseEntity.ok(ApiResponse.ok("Sandboxed test execution completed", securityService.runSandboxedValidation(projectId, request)));
    }
}
