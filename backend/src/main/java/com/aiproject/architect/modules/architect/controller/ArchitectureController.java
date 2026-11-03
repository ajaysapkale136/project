package com.aiproject.architect.modules.architect.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.architect.dto.ArchitectureDiffResponse;
import com.aiproject.architect.modules.architect.dto.ArchitectureGraphResponse;
import com.aiproject.architect.modules.architect.dto.ModuleBlueprintResponse;
import com.aiproject.architect.modules.architect.service.ArchitectureGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/architecture")
@RequiredArgsConstructor
public class ArchitectureController {

    private final ArchitectureGenerationService architectureService;

    @GetMapping("/existing")
    public ResponseEntity<ApiResponse<ArchitectureGraphResponse>> getExistingArchitecture(@PathVariable String projectId) {
        ArchitectureGraphResponse graph = architectureService.generateOrGetExistingArchitecture(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Existing architecture retrieved", graph));
    }

    @GetMapping("/proposed")
    public ResponseEntity<ApiResponse<ArchitectureGraphResponse>> getProposedArchitecture(@PathVariable String projectId) {
        ArchitectureGraphResponse graph = architectureService.generateOrGetProposedArchitecture(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Proposed architecture retrieved", graph));
    }

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<ArchitectureDiffResponse>> compareArchitectures(@PathVariable String projectId) {
        ArchitectureDiffResponse diff = architectureService.compareArchitectures(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Architecture comparison generated", diff));
    }

    @GetMapping("/blueprint")
    public ResponseEntity<ApiResponse<ModuleBlueprintResponse>> getModuleBlueprint(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "OrderProcessingService") String moduleName) {
        ModuleBlueprintResponse blueprint = architectureService.generateModuleBlueprint(projectId, moduleName);
        return ResponseEntity.ok(ApiResponse.ok("Module blueprint generated", blueprint));
    }
}
