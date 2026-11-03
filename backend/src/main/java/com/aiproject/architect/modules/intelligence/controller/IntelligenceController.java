package com.aiproject.architect.modules.intelligence.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.intelligence.dto.IntelligenceOverviewResponse;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.SourceEvidence;
import com.aiproject.architect.modules.intelligence.repository.BusinessRuleRepository;
import com.aiproject.architect.modules.intelligence.repository.SourceEvidenceRepository;
import com.aiproject.architect.modules.intelligence.service.ProjectIntelligenceService;
import com.aiproject.architect.modules.jobs.entity.GenerationJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
public class IntelligenceController {

    private final ProjectIntelligenceService intelligenceService;
    private final BusinessRuleRepository ruleRepository;
    private final SourceEvidenceRepository evidenceRepository;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<GenerationJob>> triggerAnalysis(@PathVariable String projectId) {
        GenerationJob job = intelligenceService.startAnalysis(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Intelligence analysis scheduled", job));
    }

    @GetMapping("/intelligence")
    public ResponseEntity<ApiResponse<IntelligenceOverviewResponse>> getIntelligence(@PathVariable String projectId) {
        IntelligenceOverviewResponse overview = intelligenceService.getIntelligenceOverview(projectId);
        return ResponseEntity.ok(ApiResponse.ok(overview));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<BusinessRule>>> getRules(@PathVariable String projectId) {
        IntelligenceOverviewResponse overview = intelligenceService.getIntelligenceOverview(projectId);
        if (overview.getAnalysisId() == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<BusinessRule> rules = ruleRepository.findAllByAnalysisId(overview.getAnalysisId());
        return ResponseEntity.ok(ApiResponse.ok(rules));
    }

    @GetMapping("/evidence/rule/{ruleId}")
    public ResponseEntity<ApiResponse<List<SourceEvidence>>> getRuleEvidence(@PathVariable String ruleId) {
        List<SourceEvidence> evidence = evidenceRepository.findAllByBusinessRuleId(ruleId);
        return ResponseEntity.ok(ApiResponse.ok(evidence));
    }

    @GetMapping("/evidence/component/{componentId}")
    public ResponseEntity<ApiResponse<List<SourceEvidence>>> getComponentEvidence(
            @PathVariable String projectId, @PathVariable String componentId) {
        // Component IDs are generated during analysis and are project-snapshot scoped.
        return ResponseEntity.ok(ApiResponse.ok(evidenceRepository.findAllByComponentId(componentId)));
    }
}
