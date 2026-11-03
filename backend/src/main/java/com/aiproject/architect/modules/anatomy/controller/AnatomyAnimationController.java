package com.aiproject.architect.modules.anatomy.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.anatomy.dto.ExportRequest;
import com.aiproject.architect.modules.anatomy.dto.ExportResponse;
import com.aiproject.architect.modules.anatomy.service.AnatomyAnimationService;
import com.aiproject.architect.modules.anatomy.service.ArchitectureExportService;
import com.aiproject.architect.modules.anatomy.dto.RuntimeTraceIngestRequest;
import com.aiproject.architect.modules.anatomy.entity.RuntimeTraceEvent;
import com.aiproject.architect.modules.anatomy.repository.RuntimeTraceEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/anatomy")
@RequiredArgsConstructor
public class AnatomyAnimationController {

    private final AnatomyAnimationService animationService;
    private final ArchitectureExportService exportService;
    private final RuntimeTraceEventRepository runtimeTraceRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/scenes/modules")
    public ResponseEntity<ApiResponse<AnatomySceneResponse>> getModuleScene(@PathVariable String projectId) {
        AnatomySceneResponse scene = animationService.getModuleToModuleScene(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Module architecture scene generated", scene));
    }

    @GetMapping("/scenes/exploded")
    public ResponseEntity<ApiResponse<AnatomySceneResponse>> getExplodedScene(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "default") String moduleId) {
        AnatomySceneResponse scene = animationService.getInternalExplodedScene(projectId, moduleId);
        return ResponseEntity.ok(ApiResponse.ok("Internal exploded module scene generated", scene));
    }

    @GetMapping("/scenes/execution")
    public ResponseEntity<ApiResponse<AnatomySceneResponse>> getExecutionScene(
            @PathVariable String projectId,
            @RequestParam(required = false) String operation) {
        AnatomySceneResponse scene = animationService.getExecutionTraceScene(projectId, operation);
        return ResponseEntity.ok(ApiResponse.ok("End-to-end execution trace scene generated", scene));
    }

    @GetMapping("/scenes/database")
    public ResponseEntity<ApiResponse<AnatomySceneResponse>> getDatabaseScene(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Database intelligence scene generated", animationService.getDatabaseIntelligenceScene(projectId)));
    }

    @PostMapping("/runtime-traces")
    public ResponseEntity<ApiResponse<Void>> ingestRuntimeTrace(@PathVariable String projectId,
            @Valid @RequestBody RuntimeTraceIngestRequest request) throws Exception {
        runtimeTraceRepository.save(RuntimeTraceEvent.builder().projectId(projectId).operation(request.operation())
                .componentName(request.componentName()).occurredAt(request.occurredAt()).durationMs(request.durationMs())
                .outcome(request.outcome()).attributesJson(objectMapper.writeValueAsString(request.attributes())).build());
        return ResponseEntity.ok(ApiResponse.ok("Runtime observation recorded", null));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<ExportResponse>> exportScene(
            @PathVariable String projectId,
            @RequestBody ExportRequest request) {
        ExportResponse response = exportService.exportScene(projectId, request);
        return ResponseEntity.ok(ApiResponse.ok("Export successfully generated", response));
    }
}
