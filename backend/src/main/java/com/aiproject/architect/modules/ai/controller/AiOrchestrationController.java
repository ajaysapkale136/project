package com.aiproject.architect.modules.ai.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.ai.dto.*;
import com.aiproject.architect.modules.ai.entity.AiInteractionTrace;
import com.aiproject.architect.modules.ai.repository.AiInteractionTraceRepository;
import com.aiproject.architect.modules.ai.service.MultiAiOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiOrchestrationController {

    private final MultiAiOrchestrationService orchestrationService;
    private final AiInteractionTraceRepository traceRepository;

    @GetMapping("/ai/providers")
    public ResponseEntity<ApiResponse<List<ProviderHealth>>> getProviders() {
        List<ProviderHealth> health = orchestrationService.getProviderStatus();
        return ResponseEntity.ok(ApiResponse.ok("Configured AI providers retrieved", health));
    }

    @PostMapping("/projects/{projectId}/ai/generate")
    public ResponseEntity<ApiResponse<AIResponse>> generate(
            @PathVariable String projectId,
            @RequestParam(required = false) String provider,
            @RequestBody AIRequest request) {
        request.setProjectId(projectId);
        AIResponse response = (provider != null && !provider.trim().isEmpty())
                ? orchestrationService.executeSingleProvider(provider, request)
                : orchestrationService.executeAutoRouted(request);
        return ResponseEntity.ok(ApiResponse.ok("AI response generated", response));
    }

    @PostMapping("/projects/{projectId}/ai/review")
    public ResponseEntity<ApiResponse<MultiModelReviewResponse>> review(
            @PathVariable String projectId,
            @RequestBody(required = false) MultiModelReviewRequest request) {
        if (request == null) {
            request = MultiModelReviewRequest.builder().build();
        }
        MultiModelReviewResponse response = orchestrationService.executeMultiModelReview(projectId, request);
        return ResponseEntity.ok(ApiResponse.ok("Multi-model review completed", response));
    }

    @GetMapping("/projects/{projectId}/ai/traces")
    public ResponseEntity<ApiResponse<List<AiInteractionTrace>>> getTraces(@PathVariable String projectId) {
        List<AiInteractionTrace> traces = traceRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
        return ResponseEntity.ok(ApiResponse.ok(traces));
    }
}
