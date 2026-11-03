package com.aiproject.architect.modules.ai.studio.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.ai.studio.dto.StudioContextResponse;
import com.aiproject.architect.modules.ai.studio.dto.StudioQueryRequest;
import com.aiproject.architect.modules.ai.studio.dto.StudioQueryResponse;
import com.aiproject.architect.modules.ai.studio.service.StudioExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/studio")
@RequiredArgsConstructor
public class StudioAiController {

    private final StudioExplanationService studioExplanationService;

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<StudioContextResponse>> getStudioContext(@PathVariable String projectId) {
        StudioContextResponse context = studioExplanationService.getStudioContext(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Studio context loaded", context));
    }

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<StudioQueryResponse>> processStudioQuery(
            @PathVariable String projectId,
            @RequestBody StudioQueryRequest request) {
        StudioQueryResponse response = studioExplanationService.processStudioQuery(projectId, request);
        return ResponseEntity.ok(ApiResponse.ok("Studio response generated", response));
    }
}
