package com.aiproject.architect.modules.assistant.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.assistant.dto.*;
import com.aiproject.architect.modules.assistant.entity.CodeProposal;
import com.aiproject.architect.modules.assistant.entity.GeneratedTest;
import com.aiproject.architect.modules.assistant.service.CodeAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final CodeAssistantService assistantService;

    @PostMapping("/proposals")
    public ResponseEntity<ApiResponse<ProposalDetailResponse>> generateProposal(
            @PathVariable String projectId,
            @RequestBody CreateProposalRequest request) {
        ProposalDetailResponse response = assistantService.generateModuleProposal(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Code proposal generated successfully", response));
    }

    @PostMapping("/refactor")
    public ResponseEntity<ApiResponse<ProposalDetailResponse>> refactorComponent(
            @PathVariable String projectId,
            @RequestBody RefactorComponentRequest request) {
        ProposalDetailResponse response = assistantService.refactorComponent(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Component refactoring proposed", response));
    }

    @GetMapping("/proposals")
    public ResponseEntity<ApiResponse<List<CodeProposal>>> listProposals(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Proposals retrieved successfully", assistantService.listProposals(projectId)));
    }

    @GetMapping("/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalDetailResponse>> getProposal(
            @PathVariable String projectId,
            @PathVariable String proposalId) {
        return ResponseEntity.ok(ApiResponse.ok("Proposal details retrieved", assistantService.getProposalDetails(proposalId)));
    }

    @PostMapping("/proposals/{proposalId}/approve")
    public ResponseEntity<ApiResponse<CodeProposal>> approveProposal(
            @PathVariable String projectId,
            @PathVariable String proposalId) {
        return ResponseEntity.ok(ApiResponse.ok("Proposal approved", assistantService.approveProposal(projectId, proposalId)));
    }

    @PostMapping("/proposals/{proposalId}/reject")
    public ResponseEntity<ApiResponse<CodeProposal>> rejectProposal(
            @PathVariable String projectId,
            @PathVariable String proposalId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Rejected by user") : "Rejected by user";
        return ResponseEntity.ok(ApiResponse.ok("Proposal rejected", assistantService.rejectProposal(projectId, proposalId, reason)));
    }

    @PostMapping("/proposals/{proposalId}/apply")
    public ResponseEntity<ApiResponse<CodeProposal>> applyProposal(
            @PathVariable String projectId,
            @PathVariable String proposalId,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.getOrDefault("user", "USER") : "USER";
        return ResponseEntity.ok(ApiResponse.ok("Proposal successfully applied to workspace", assistantService.applyProposal(projectId, proposalId, user)));
    }

    @PostMapping("/tests")
    public ResponseEntity<ApiResponse<GeneratedTest>> generateTest(
            @PathVariable String projectId,
            @RequestBody GenerateTestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Test generated successfully", assistantService.generateTestForFile(projectId, request)));
    }
}
