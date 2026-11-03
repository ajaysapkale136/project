package com.aiproject.architect.modules.testing.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.testing.dto.RunTestRequest;
import com.aiproject.architect.modules.testing.dto.TestExecutionReportResponse;
import com.aiproject.architect.modules.testing.entity.TestExecution;
import com.aiproject.architect.modules.testing.service.SandboxedTestingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/testing")
@RequiredArgsConstructor
public class TestingEngineController {

    private final SandboxedTestingEngine testingEngine;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<TestExecutionReportResponse>> runTests(
            @PathVariable String projectId,
            @RequestBody(required = false) RunTestRequest request) {
        if (request == null) {
            request = new RunTestRequest();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sandboxed test run completed", testingEngine.runTests(projectId, request)));
    }

    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<List<TestExecution>>> listExecutions(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Test executions retrieved", testingEngine.listExecutions(projectId)));
    }
}
