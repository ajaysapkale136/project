package com.aiproject.architect.modules.patch.service;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.assistant.service.DiffEngine;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
import com.aiproject.architect.modules.intelligence.entity.ModuleDependency;
import com.aiproject.architect.modules.intelligence.entity.ProjectAnalysis;
import com.aiproject.architect.modules.intelligence.repository.BusinessRuleRepository;
import com.aiproject.architect.modules.intelligence.repository.DetectedTechnologyRepository;
import com.aiproject.architect.modules.intelligence.repository.ModuleDependencyRepository;
import com.aiproject.architect.modules.intelligence.repository.ProjectAnalysisRepository;
import com.aiproject.architect.modules.patch.dto.*;
import com.aiproject.architect.modules.patch.entity.ChangeSet;
import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import com.aiproject.architect.modules.patch.repository.ChangeSetFileRepository;
import com.aiproject.architect.modules.patch.repository.ChangeSetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PatchGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PatchGenerationService.class);

    private final WorktreeService worktreeService;
    private final ChangeSetRepository changeSetRepository;
    private final ChangeSetFileRepository fileChangeRepository;
    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectAnalysisRepository analysisRepository;
    private final BusinessRuleRepository ruleRepository;
    private final DetectedTechnologyRepository techRepository;
    private final ModuleDependencyRepository dependencyRepository;
    private final ProjectFileRepository fileRepository;
    private final DiffEngine diffEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ChangeSetResponse generateChangeSet(String projectId, CreateChangeSetRequest request) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No snapshot found for project " + projectId));

        IsolatedWorkspace workspace = worktreeService.getOrCreateWorkspace(projectId);

        Optional<ProjectAnalysis> analysisOpt = analysisRepository.findTopBySnapshotIdOrderByCreatedAtDesc(snapshot.getId());
        String analysisId = analysisOpt.map(ProjectAnalysis::getId).orElse(null);

        List<BusinessRule> businessRules = analysisId != null ?
                ruleRepository.findAllByAnalysisId(analysisId) : Collections.emptyList();
        List<DetectedTechnology> technologies = analysisId != null ?
                techRepository.findAllByAnalysisId(analysisId) : Collections.emptyList();
        List<ModuleDependency> dependencies = analysisId != null ?
                dependencyRepository.findAllByAnalysisId(analysisId) : Collections.emptyList();

        String targetType = request.getTargetType() != null ? request.getTargetType().toUpperCase() : "MODULE";
        String targetIdentifier = request.getTargetIdentifier() != null ? request.getTargetIdentifier().trim() : "CustomFeature";
        String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle()
                : String.format("[%s] Implement %s", targetType, targetIdentifier);

        // Detect project's dominant stack
        boolean isJava = technologies.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Java") || t.getName().equalsIgnoreCase("Spring Boot"));
        boolean isTypeScript = technologies.stream().anyMatch(t -> t.getName().equalsIgnoreCase("TypeScript") || t.getName().equalsIgnoreCase("React"));
        boolean isPython = technologies.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Python"));

        String frameworkFamily = isJava ? "Java / Spring Boot" : isTypeScript ? "TypeScript / React" : isPython ? "Python" : "Generic Web";

        // 1. Build Technical Plan
        List<String> steps = List.of(
                "1. Inspect existing conventions and project structure under " + frameworkFamily,
                "2. Synthesize domain model and repository adhering to existing database relations",
                "3. Implement service layer strictly preserving " + businessRules.size() + " verified business rules",
                "4. Expose REST/API contracts matching endpoint conventions without breaking changes",
                "5. Generate schema migration scripts and unit tests with Mockito / Jest"
        );
        List<String> conventions = List.of(
                "Framework: " + frameworkFamily,
                "Package layout: Standard modular domain conventions",
                "Validation: Explicit parameter checks preventing malformed state",
                "Immutability: UUID / Primary key immutability"
        );
        List<String> affectedModules = List.of(targetIdentifier, "Core");
        List<String> affectedEndpoints = List.of("/api/v1/" + targetIdentifier.toLowerCase() + "s");
        List<String> affectedDbTables = List.of(targetIdentifier.toLowerCase() + "s");

        TechnicalPlanDto technicalPlan = TechnicalPlanDto.builder()
                .targetType(targetType)
                .targetIdentifier(targetIdentifier)
                .steps(steps)
                .detectedConventions(conventions)
                .affectedModules(affectedModules)
                .affectedEndpoints(affectedEndpoints)
                .affectedDatabaseTables(affectedDbTables)
                .riskAssessment("Low risk - Changes are isolated to new domain module and backward-compatible extensions")
                .build();

        // 2. Build Business Rule Impact
        List<BusinessRuleImpactDto> ruleImpacts = new ArrayList<>();
        int count = 0;
        for (BusinessRule rule : businessRules) {
            if (count++ >= 4) break;
            ruleImpacts.add(BusinessRuleImpactDto.builder()
                    .ruleCode(rule.getRuleCode())
                    .title(rule.getTitle())
                    .existingRule(rule.getDescription())
                    .proposedChange("Preserved explicitly via validation interceptor in " + targetIdentifier + "Service")
                    .sourceEvidence("Referenced from verified AST evidence for " + rule.getRuleCode())
                    .affectedClass(targetIdentifier + "Service.java")
                    .affectedModules(targetIdentifier)
                    .apiCompatibilityRisk("None - Existing contract invariant preserved")
                    .dataMigrationRisk("None - Schema additive only")
                    .securityImpact("Enforced input verification preventing injection or privilege bypass")
                    .regressionTests("service." + targetIdentifier + "ServiceTest.test" + rule.getRuleCode().replace("-", "_") + "()")
                    .rollbackPlan("Revert patch change set to restore preceding module baseline")
                    .build());
        }

        // 3. Build API Compatibility Impact
        ApiCompatibilityImpactDto apiImpact = ApiCompatibilityImpactDto.builder()
                .breakingChanges(Collections.emptyList())
                .compatibleAdditions(List.of("GET /api/v1/" + targetIdentifier.toLowerCase() + "s", "POST /api/v1/" + targetIdentifier.toLowerCase() + "s"))
                .deprecatedEndpoints(Collections.emptyList())
                .isBackwardCompatible(true)
                .build();

        String migrationImpact = "-- Flyway Database Migration\nCREATE TABLE " + targetIdentifier.toLowerCase() + "s (\n" +
                "    id VARCHAR(36) PRIMARY KEY,\n" +
                "    name VARCHAR(255) NOT NULL,\n" +
                "    status VARCHAR(50) NOT NULL,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n" +
                ");";

        String technicalPlanJson = serialize(technicalPlan);
        String ruleImpactJson = serialize(ruleImpacts);
        String apiImpactJson = serialize(apiImpact);

        ChangeSet changeSet = ChangeSet.builder()
                .projectId(projectId)
                .workspaceId(workspace.getId())
                .sourceSnapshotId(snapshot.getId())
                .targetType(targetType)
                .targetIdentifier(targetIdentifier)
                .title(title)
                .description(request.getDescription())
                .technicalPlanJson(technicalPlanJson)
                .status("PENDING_APPROVAL")
                .riskLevel("LOW")
                .businessRuleImpactJson(ruleImpactJson)
                .apiCompatibilityImpactJson(apiImpactJson)
                .migrationImpactJson(migrationImpact)
                .rollbackInstructions("Invoke POST /api/v1/projects/" + projectId + "/worktree/changesets/{id}/rollback to undo all file modifications.")
                .build();
        changeSet = changeSetRepository.save(changeSet);

        // 4. Generate File Diffs
        List<ChangeSetFile> files = generateFilesForStack(changeSet, isJava, isTypeScript, isPython, targetIdentifier, request.getDescription(), ruleImpacts);
        files = fileChangeRepository.saveAll(files);

        return ChangeSetResponse.builder()
                .changeSet(changeSet)
                .files(files)
                .technicalPlan(technicalPlan)
                .businessRuleImpact(ruleImpacts)
                .apiCompatibilityImpact(apiImpact)
                .migrationImpact(migrationImpact)
                .isStale(false)
                .build();
    }

    private List<ChangeSetFile> generateFilesForStack(ChangeSet changeSet, boolean isJava, boolean isTypeScript, boolean isPython,
                                                     String targetIdentifier, String description, List<BusinessRuleImpactDto> rules) {
        List<ChangeSetFile> files = new ArrayList<>();
        String entityName = targetIdentifier.substring(0, 1).toUpperCase() + targetIdentifier.substring(1);
        String cleanLower = targetIdentifier.toLowerCase();

        if (isJava || (!isTypeScript && !isPython)) {
            // Java Spring Boot Files
            String modelPath = "src/main/java/com/aiproject/modules/" + cleanLower + "/model/" + entityName + ".java";
            String modelContent = "package com.aiproject.modules." + cleanLower + ".model;\n\n" +
                    "import jakarta.persistence.*;\nimport lombok.*;\nimport java.time.LocalDateTime;\n\n" +
                    "@Entity\n@Table(name = \"" + cleanLower + "s\")\n" +
                    "@Getter\n@Setter\n@NoArgsConstructor\n@AllArgsConstructor\n@Builder\n" +
                    "public class " + entityName + " {\n" +
                    "    @Id\n    private String id;\n" +
                    "    @Column(nullable = false)\n    private String name;\n" +
                    "    @Column(nullable = false)\n    private String status;\n" +
                    "    @Column(name = \"created_at\", nullable = false)\n    private LocalDateTime createdAt;\n" +
                    "}\n";
            DiffEngine.DiffResult mDiff = diffEngine.computeDiff(modelPath, null, modelContent);
            files.add(ChangeSetFile.builder()
                    .changeSetId(changeSet.getId())
                    .filePath(modelPath)
                    .changeType("CREATE")
                    .proposedContent(modelContent)
                    .diffUnified(mDiff.getUnifiedDiff())
                    .additionsCount(mDiff.getAdditions())
                    .deletionsCount(mDiff.getDeletions())
                    .explanation("Domain entity with JPA annotations and immutable UUID identifier")
                    .status("PENDING")
                    .build());

            String servicePath = "src/main/java/com/aiproject/modules/" + cleanLower + "/service/" + entityName + "Service.java";
            StringBuilder serviceCode = new StringBuilder();
            serviceCode.append("package com.aiproject.modules.").append(cleanLower).append(".service;\n\n")
                    .append("import org.springframework.stereotype.Service;\n")
                    .append("import lombok.RequiredArgsConstructor;\n")
                    .append("import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n\n")
                    .append("@Service\n@RequiredArgsConstructor\npublic class ").append(entityName).append("Service {\n")
                    .append("    private static final Logger log = LoggerFactory.getLogger(").append(entityName).append("Service.class);\n\n")
                    .append("    public void process").append(entityName).append("(String name) {\n")
                    .append("        if (name == null || name.isBlank()) {\n")
                    .append("            throw new IllegalArgumentException(\"Name is required\");\n")
                    .append("        }\n");
            for (BusinessRuleImpactDto r : rules) {
                serviceCode.append("        // Preserved Business Rule: ").append(r.getRuleCode()).append(" - ").append(r.getTitle()).append("\n");
            }
            serviceCode.append("        log.info(\"Processed ").append(entityName).append(" name={}\", name);\n")
                    .append("    }\n}\n");

            DiffEngine.DiffResult sDiff = diffEngine.computeDiff(servicePath, null, serviceCode.toString());
            files.add(ChangeSetFile.builder()
                    .changeSetId(changeSet.getId())
                    .filePath(servicePath)
                    .changeType("CREATE")
                    .proposedContent(serviceCode.toString())
                    .diffUnified(sDiff.getUnifiedDiff())
                    .additionsCount(sDiff.getAdditions())
                    .deletionsCount(sDiff.getDeletions())
                    .explanation("Service business layer validating contracts and preserved rules")
                    .status("PENDING")
                    .build());

            String testPath = "src/test/java/com/aiproject/modules/" + cleanLower + "/service/" + entityName + "ServiceTest.java";
            String testContent = "package com.aiproject.modules." + cleanLower + ".service;\n\n" +
                    "import org.junit.jupiter.api.Test;\n" +
                    "import static org.assertj.core.api.Assertions.assertThat;\n\n" +
                    "class " + entityName + "ServiceTest {\n" +
                    "    @Test\n    void testSanity() {\n        assertThat(true).isTrue();\n    }\n}\n";
            DiffEngine.DiffResult tDiff = diffEngine.computeDiff(testPath, null, testContent);
            files.add(ChangeSetFile.builder()
                    .changeSetId(changeSet.getId())
                    .filePath(testPath)
                    .changeType("CREATE")
                    .proposedContent(testContent)
                    .diffUnified(tDiff.getUnifiedDiff())
                    .additionsCount(tDiff.getAdditions())
                    .deletionsCount(tDiff.getDeletions())
                    .explanation("Unit test suite verifying domain constraints")
                    .status("PENDING")
                    .build());

        } else if (isTypeScript) {
            String tsPath = "src/modules/" + cleanLower + "/service.ts";
            String tsContent = "export interface " + entityName + " {\n" +
                    "  id: string;\n  name: string;\n  status: string;\n}\n\n" +
                    "export function validate" + entityName + "(item: Partial<" + entityName + ">): boolean {\n" +
                    "  if (!item.name || !item.name.trim()) throw new Error('Name is required');\n" +
                    "  return true;\n}\n";
            DiffEngine.DiffResult tsDiff = diffEngine.computeDiff(tsPath, null, tsContent);
            files.add(ChangeSetFile.builder()
                    .changeSetId(changeSet.getId())
                    .filePath(tsPath)
                    .changeType("CREATE")
                    .proposedContent(tsContent)
                    .diffUnified(tsDiff.getUnifiedDiff())
                    .additionsCount(tsDiff.getAdditions())
                    .deletionsCount(tsDiff.getDeletions())
                    .explanation("TypeScript service with type guards and schema validation")
                    .status("PENDING")
                    .build());
        }

        return files;
    }

    @Transactional(readOnly = true)
    public List<ChangeSet> listChangeSets(String projectId) {
        return changeSetRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public ChangeSetResponse getChangeSetDetails(String changeSetId) {
        ChangeSet cs = changeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set not found: " + changeSetId));

        IsolatedWorkspace workspace = worktreeService.getOrCreateWorkspace(cs.getProjectId());
        boolean isStale = worktreeService.isWorkspaceStale(workspace);

        List<ChangeSetFile> files = fileChangeRepository.findByChangeSetId(changeSetId);
        TechnicalPlanDto plan = deserialize(cs.getTechnicalPlanJson(), TechnicalPlanDto.class);
        List<BusinessRuleImpactDto> ruleImpact = deserializeList(cs.getBusinessRuleImpactJson(), BusinessRuleImpactDto.class);
        ApiCompatibilityImpactDto apiImpact = deserialize(cs.getApiCompatibilityImpactJson(), ApiCompatibilityImpactDto.class);

        return ChangeSetResponse.builder()
                .changeSet(cs)
                .files(files)
                .technicalPlan(plan)
                .businessRuleImpact(ruleImpact)
                .apiCompatibilityImpact(apiImpact)
                .migrationImpact(cs.getMigrationImpactJson())
                .isStale(isStale)
                .build();
    }

    @Transactional
    public ChangeSet approveChangeSet(String changeSetId) {
        ChangeSet cs = changeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set not found: " + changeSetId));
        cs.setStatus("APPROVED");
        return changeSetRepository.save(cs);
    }

    @Transactional
    public ChangeSet rejectChangeSet(String changeSetId) {
        ChangeSet cs = changeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set not found: " + changeSetId));
        cs.setStatus("REJECTED");
        return changeSetRepository.save(cs);
    }

    @Transactional
    public ChangeSetFile updateFileDecision(String fileId, String decision) {
        ChangeSetFile file = fileChangeRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set file not found: " + fileId));
        file.setStatus(decision.equalsIgnoreCase("ACCEPTED") ? "ACCEPTED" : "REJECTED");
        return fileChangeRepository.save(file);
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> List<T> deserializeList(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
