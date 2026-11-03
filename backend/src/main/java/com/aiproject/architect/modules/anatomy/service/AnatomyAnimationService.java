package com.aiproject.architect.modules.anatomy.service;

import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.anatomy.adapter.ExecutionTraceAdapter;
import com.aiproject.architect.modules.anatomy.adapter.DatabaseIntelligenceAdapter;
import com.aiproject.architect.modules.anatomy.adapter.InternalExplodedAdapter;
import com.aiproject.architect.modules.anatomy.adapter.ProjectAssemblyAdapter;
import com.aiproject.architect.modules.anatomy.repository.RuntimeTraceEventRepository;
import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.AnalysisModule;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.ModuleDependency;
import com.aiproject.architect.modules.intelligence.entity.ProjectAnalysis;
import com.aiproject.architect.modules.intelligence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnatomyAnimationService {

    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectAnalysisRepository analysisRepository;
    private final AnalysisModuleRepository moduleRepository;
    private final AnalysisComponentRepository componentRepository;
    private final ModuleDependencyRepository dependencyRepository;
    private final BusinessRuleRepository ruleRepository;

    private final ProjectAssemblyAdapter projectAssemblyAdapter;
    private final InternalExplodedAdapter internalExplodedAdapter;
    private final ExecutionTraceAdapter executionTraceAdapter;
    private final DatabaseIntelligenceAdapter databaseIntelligenceAdapter;
    private final RuntimeTraceEventRepository runtimeTraceRepository;

    @Transactional(readOnly = true)
    public AnatomySceneResponse getModuleToModuleScene(String projectId) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        List<AnalysisModule> modules = moduleRepository.findAllByAnalysisId(analysis.getId());
        List<ModuleDependency> dependencies = dependencyRepository.findAllByAnalysisId(analysis.getId());

        return projectAssemblyAdapter.buildModuleScene(projectId, modules, dependencies);
    }

    @Transactional(readOnly = true)
    public AnatomySceneResponse getInternalExplodedScene(String projectId, String moduleId) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        List<AnalysisModule> modules = moduleRepository.findAllByAnalysisId(analysis.getId());

        AnalysisModule targetModule;
        if (moduleId != null && !moduleId.trim().isEmpty() && !moduleId.equalsIgnoreCase("default")) {
            targetModule = modules.stream().filter(m -> m.getId().equals(moduleId)).findFirst()
                    .orElse(modules.isEmpty() ? null : modules.get(0));
        } else {
            targetModule = modules.stream().filter(m -> "BACKEND_SERVICE".equals(m.getModuleType())).findFirst()
                    .orElse(modules.isEmpty() ? null : modules.get(0));
        }

        if (targetModule == null) {
            throw new ResourceNotFoundException("No modules found to explode for project " + projectId);
        }

        List<AnalysisComponent> components = componentRepository.findAllByModuleId(targetModule.getId());
        return internalExplodedAdapter.buildExplodedScene(projectId, targetModule, components);
    }

    @Transactional(readOnly = true)
    public AnatomySceneResponse getExecutionTraceScene(String projectId, String operationName) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        List<AnalysisModule> modules = moduleRepository.findAllByAnalysisId(analysis.getId());
        List<AnalysisComponent> components = modules.stream()
                .flatMap(module -> componentRepository.findAllByModuleId(module.getId()).stream())
                .toList();

        AnatomySceneResponse scene = executionTraceAdapter.buildExecutionTraceScene(projectId, operationName, components);
        String operation = operationName == null || operationName.isBlank() ? "selected operation" : operationName;
        int observations = runtimeTraceRepository.findTop100ByProjectIdAndOperationOrderByOccurredAtDesc(projectId, operation).size();
        scene.setReadout(scene.getReadout() + " | Runtime observations: " + observations);
        scene.setSubtitle(scene.getSubtitle() + (observations == 0 ? " No runtime data has been ingested." : " Runtime data has been ingested for this operation."));
        return scene;
    }

    @Transactional(readOnly = true)
    public AnatomySceneResponse getDatabaseIntelligenceScene(String projectId) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        List<AnalysisComponent> components = moduleRepository.findAllByAnalysisId(analysis.getId()).stream()
                .flatMap(module -> componentRepository.findAllByModuleId(module.getId()).stream()).toList();
        return databaseIntelligenceAdapter.build(projectId, components);
    }

    private ProjectAnalysis getLatestAnalysis(String projectId) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("SourceSnapshot", projectId));
        return analysisRepository.findTopBySnapshotIdOrderByCreatedAtDesc(snapshot.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ProjectAnalysis", projectId));
    }
}
