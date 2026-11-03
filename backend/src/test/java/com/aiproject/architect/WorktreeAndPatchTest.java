package com.aiproject.architect;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.modules.assistant.service.DiffEngine;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
import com.aiproject.architect.modules.intelligence.entity.ProjectAnalysis;
import com.aiproject.architect.modules.intelligence.repository.BusinessRuleRepository;
import com.aiproject.architect.modules.intelligence.repository.DetectedTechnologyRepository;
import com.aiproject.architect.modules.intelligence.repository.ModuleDependencyRepository;
import com.aiproject.architect.modules.intelligence.repository.ProjectAnalysisRepository;
import com.aiproject.architect.modules.patch.dto.ChangeSetResponse;
import com.aiproject.architect.modules.patch.dto.CreateChangeSetRequest;
import com.aiproject.architect.modules.patch.dto.RollbackResponse;
import com.aiproject.architect.modules.patch.entity.ChangeSet;
import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import com.aiproject.architect.modules.patch.repository.ChangeSetFileRepository;
import com.aiproject.architect.modules.patch.repository.ChangeSetRepository;
import com.aiproject.architect.modules.patch.repository.IsolatedWorkspaceRepository;
import com.aiproject.architect.modules.patch.service.PatchExportService;
import com.aiproject.architect.modules.patch.service.PatchGenerationService;
import com.aiproject.architect.modules.patch.service.WorktreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorktreeAndPatchTest {

    @Mock
    private IsolatedWorkspaceRepository workspaceRepository;
    @Mock
    private SourceSnapshotRepository snapshotRepository;
    @Mock
    private ProjectFileRepository fileRepository;
    @Mock
    private ChangeSetRepository changeSetRepository;
    @Mock
    private ChangeSetFileRepository fileChangeRepository;
    @Mock
    private ProjectAnalysisRepository analysisRepository;
    @Mock
    private BusinessRuleRepository ruleRepository;
    @Mock
    private DetectedTechnologyRepository techRepository;
    @Mock
    private ModuleDependencyRepository dependencyRepository;

    private WorktreeService worktreeService;
    private PatchGenerationService patchService;
    private PatchExportService exportService;
    private DiffEngine diffEngine;

    @TempDir
    Path tempStorage;

    @BeforeEach
    void setUp() {
        diffEngine = new DiffEngine();
        worktreeService = new WorktreeService(
                workspaceRepository,
                snapshotRepository,
                fileRepository,
                changeSetRepository,
                fileChangeRepository
        );
        patchService = new PatchGenerationService(
                worktreeService,
                changeSetRepository,
                fileChangeRepository,
                snapshotRepository,
                analysisRepository,
                ruleRepository,
                techRepository,
                dependencyRepository,
                fileRepository,
                diffEngine
        );
        exportService = new PatchExportService(
                changeSetRepository,
                fileChangeRepository,
                workspaceRepository
        );
    }

    @Test
    void testGetOrCreateWorkspaceInitializesDirectory() throws IOException {
        String projectId = "proj-worktree-1";
        Path snapshotDir = tempStorage.resolve("snapshot-1");
        Files.createDirectories(snapshotDir);
        Files.writeString(snapshotDir.resolve("README.md"), "# Project Readme");

        SourceSnapshot snapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .storagePath(snapshotDir.toString())
                .build();
        snapshot.setId("snap-1");

        ProjectFile pf = ProjectFile.builder()
                .snapshotId(snapshot.getId())
                .filePath("README.md")
                .fileName("README.md")
                .build();

        when(snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(snapshot));
        when(workspaceRepository.findTopByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "ACTIVE")).thenReturn(Optional.empty());
        when(fileRepository.findAllBySnapshotId(snapshot.getId())).thenReturn(List.of(pf));
        when(workspaceRepository.save(any(IsolatedWorkspace.class))).thenAnswer(i -> i.getArgument(0));

        IsolatedWorkspace ws = worktreeService.getOrCreateWorkspace(projectId);

        assertThat(ws).isNotNull();
        assertThat(ws.getProjectId()).isEqualTo(projectId);
        assertThat(ws.getStatus()).isEqualTo("ACTIVE");
        assertThat(Files.exists(Paths.get(ws.getWorkspacePath()).resolve("README.md"))).isTrue();
    }

    @Test
    void testStalenessConflictBlocksChangeSetApplication() {
        String changeSetId = "cs-stale-1";
        String projectId = "proj-stale";

        IsolatedWorkspace workspace = IsolatedWorkspace.builder()
                .projectId(projectId)
                .snapshotId("snap-old")
                .workspacePath(tempStorage.toString())
                .build();
        workspace.setId("ws-1");

        ChangeSet cs = ChangeSet.builder()
                .projectId(projectId)
                .workspaceId(workspace.getId())
                .sourceSnapshotId("snap-old")
                .status("APPROVED")
                .build();
        cs.setId(changeSetId);

        // Newer snapshot exists
        SourceSnapshot newerSnapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .storagePath(tempStorage.toString())
                .build();
        newerSnapshot.setId("snap-NEWER");

        when(changeSetRepository.findById(changeSetId)).thenReturn(Optional.of(cs));
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(newerSnapshot));

        assertThatThrownBy(() -> worktreeService.applyChangeSet(changeSetId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Conflict detected: Project source has changed");

        assertThat(cs.getStatus()).isEqualTo("STALE_CONFLICT");
    }

    @Test
    void testApplyAndRollbackChangeSet() throws IOException {
        String changeSetId = "cs-apply-1";
        String projectId = "proj-apply";
        Path worktreeDir = tempStorage.resolve("ws-apply");
        Files.createDirectories(worktreeDir);
        Files.writeString(worktreeDir.resolve("config.properties"), "env=test\n");

        IsolatedWorkspace workspace = IsolatedWorkspace.builder()
                .projectId(projectId)
                .snapshotId("snap-1")
                .workspacePath(worktreeDir.toString())
                .build();
        workspace.setId("ws-apply");

        ChangeSet cs = ChangeSet.builder()
                .projectId(projectId)
                .workspaceId(workspace.getId())
                .sourceSnapshotId("snap-1")
                .status("APPROVED")
                .build();
        cs.setId(changeSetId);

        ChangeSetFile file1 = ChangeSetFile.builder()
                .changeSetId(changeSetId)
                .filePath("config.properties")
                .changeType("UPDATE")
                .originalContent("env=test\n")
                .proposedContent("env=production\n")
                .status("PENDING")
                .build();

        ChangeSetFile file2 = ChangeSetFile.builder()
                .changeSetId(changeSetId)
                .filePath("new_feature.txt")
                .changeType("CREATE")
                .proposedContent("feature content")
                .status("PENDING")
                .build();

        SourceSnapshot matchingSnapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .storagePath(worktreeDir.toString())
                .build();
        matchingSnapshot.setId("snap-1");

        when(changeSetRepository.findById(changeSetId)).thenReturn(Optional.of(cs));
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(matchingSnapshot));
        when(fileChangeRepository.findByChangeSetId(changeSetId)).thenReturn(List.of(file1, file2));
        when(changeSetRepository.save(any(ChangeSet.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Apply Change Set
        ChangeSet applied = worktreeService.applyChangeSet(changeSetId);
        assertThat(applied.getStatus()).isEqualTo("APPLIED");
        assertThat(Files.readString(worktreeDir.resolve("config.properties"))).isEqualTo("env=production\n");
        assertThat(Files.exists(worktreeDir.resolve("new_feature.txt"))).isTrue();

        // 2. Rollback Change Set
        RollbackResponse rollback = worktreeService.rollbackChangeSet(changeSetId);
        assertThat(rollback.getStatus()).isEqualTo("ROLLED_BACK");
        assertThat(Files.readString(worktreeDir.resolve("config.properties"))).isEqualTo("env=test\n");
        assertThat(Files.exists(worktreeDir.resolve("new_feature.txt"))).isFalse();
    }

    @Test
    void testPatchGenerationIncludesTechnicalPlanAndPreservedRules() {
        String projectId = "proj-patch-gen";
        Path snapPath = tempStorage.resolve("snap-patch");
        snapPath.toFile().mkdirs();

        SourceSnapshot snapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .storagePath(snapPath.toString())
                .build();
        snapshot.setId("snap-1");

        IsolatedWorkspace workspace = IsolatedWorkspace.builder()
                .projectId(projectId)
                .snapshotId(snapshot.getId())
                .workspacePath(snapPath.toString())
                .build();
        workspace.setId("ws-patch");

        ProjectAnalysis analysis = ProjectAnalysis.builder().snapshotId(snapshot.getId()).build();
        analysis.setId("ana-1");

        BusinessRule rule = BusinessRule.builder()
                .analysisId(analysis.getId())
                .ruleCode("RULE-TAX-01")
                .title("State Tax Validation")
                .description("Applicable tax rate must be positive decimal")
                .category("DATA_INTEGRITY")
                .build();

        DetectedTechnology tech = DetectedTechnology.builder()
                .analysisId(analysis.getId())
                .name("Spring Boot")
                .category("BACKEND")
                .build();

        when(snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(snapshot));
        when(workspaceRepository.findTopByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "ACTIVE")).thenReturn(Optional.of(workspace));
        when(analysisRepository.findTopBySnapshotIdOrderByCreatedAtDesc(snapshot.getId())).thenReturn(Optional.of(analysis));
        when(ruleRepository.findAllByAnalysisId(analysis.getId())).thenReturn(List.of(rule));
        when(techRepository.findAllByAnalysisId(analysis.getId())).thenReturn(List.of(tech));
        when(changeSetRepository.save(any(ChangeSet.class))).thenAnswer(i -> {
            ChangeSet c = i.getArgument(0);
            c.setId("cs-gen-1");
            return c;
        });
        when(fileChangeRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        CreateChangeSetRequest request = CreateChangeSetRequest.builder()
                .targetType("MODULE")
                .targetIdentifier("Accounting")
                .title("Implement Accounting Module")
                .description("Tax calculation and fiscal ledger")
                .build();

        ChangeSetResponse response = patchService.generateChangeSet(projectId, request);

        assertThat(response).isNotNull();
        assertThat(response.getChangeSet().getTargetIdentifier()).isEqualTo("Accounting");
        assertThat(response.getTechnicalPlan().getSteps()).isNotEmpty();
        assertThat(response.getBusinessRuleImpact()).isNotEmpty();
        assertThat(response.getBusinessRuleImpact().get(0).getRuleCode()).isEqualTo("RULE-TAX-01");
        assertThat(response.getApiCompatibilityImpact().isBackwardCompatible()).isTrue();
        assertThat(response.getFiles()).isNotEmpty();
    }
}
