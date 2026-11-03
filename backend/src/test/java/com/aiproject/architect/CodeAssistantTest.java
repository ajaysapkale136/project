package com.aiproject.architect;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.modules.assistant.dto.CreateProposalRequest;
import com.aiproject.architect.modules.assistant.dto.ProposalDetailResponse;
import com.aiproject.architect.modules.assistant.entity.CodeChange;
import com.aiproject.architect.modules.assistant.entity.CodeProposal;
import com.aiproject.architect.modules.assistant.repository.CodeChangeRepository;
import com.aiproject.architect.modules.assistant.repository.CodeProposalRepository;
import com.aiproject.architect.modules.assistant.repository.GeneratedTestRepository;
import com.aiproject.architect.modules.assistant.service.CodeAssistantService;
import com.aiproject.architect.modules.assistant.service.DiffEngine;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.ProjectAnalysis;
import com.aiproject.architect.modules.intelligence.repository.AnalysisComponentRepository;
import com.aiproject.architect.modules.intelligence.repository.BusinessRuleRepository;
import com.aiproject.architect.modules.intelligence.repository.ProjectAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeAssistantTest {

    private DiffEngine diffEngine;

    @Mock
    private CodeProposalRepository proposalRepository;

    @Mock
    private CodeChangeRepository changeRepository;

    @Mock
    private GeneratedTestRepository testRepository;

    @Mock
    private SourceSnapshotRepository snapshotRepository;

    @Mock
    private ProjectAnalysisRepository analysisRepository;

    @Mock
    private BusinessRuleRepository ruleRepository;

    @Mock
    private AnalysisComponentRepository componentRepository;

    @Mock
    private ProjectFileRepository fileRepository;

    private CodeAssistantService assistantService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        diffEngine = new DiffEngine();
        assistantService = new CodeAssistantService(
                proposalRepository,
                changeRepository,
                testRepository,
                snapshotRepository,
                analysisRepository,
                ruleRepository,
                componentRepository,
                fileRepository,
                diffEngine,
                null // aiOrchestrator
        );
    }

    @Test
    void testDiffEngineUnifiedDiffCalculation() {
        String original = "line1\nline2\nline3\n";
        String proposed = "line1\nline2_modified\nline3\nline4\n";

        DiffEngine.DiffResult result = diffEngine.computeDiff("sample.txt", original, proposed);

        assertThat(result.getUnifiedDiff()).contains("--- a/sample.txt");
        assertThat(result.getUnifiedDiff()).contains("+++ b/sample.txt");
        assertThat(result.getAdditions()).isGreaterThan(0);
        assertThat(result.getDeletions()).isGreaterThan(0);
    }

    @Test
    void testDiffEngineNewFileAllAdditions() {
        String proposed = "public class NewEntity {}\n";
        DiffEngine.DiffResult result = diffEngine.computeDiff("NewEntity.java", null, proposed);

        assertThat(result.getDeletions()).isEqualTo(0);
        assertThat(result.getAdditions()).isEqualTo(1);
        assertThat(result.getUnifiedDiff()).contains("+public class NewEntity {}");
    }

    @Test
    void testGenerateModuleProposalPreservesRules() {
        String projectId = "proj-123";
        SourceSnapshot snapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .storagePath(tempDir.toString())
                .build();
        snapshot.setId("snap-123");

        ProjectAnalysis analysis = ProjectAnalysis.builder()
                .snapshotId(snapshot.getId())
                .build();
        analysis.setId("ana-123");

        BusinessRule rule = BusinessRule.builder()
                .analysisId(analysis.getId())
                .ruleCode("RULE-VAL-01")
                .title("Unique Customer Email")
                .description("Customer email must be globally unique across active accounts")
                .category("DATA_INTEGRITY")
                .build();

        when(snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(snapshot));
        when(analysisRepository.findTopBySnapshotIdOrderByCreatedAtDesc(snapshot.getId())).thenReturn(Optional.of(analysis));
        when(ruleRepository.findAllByAnalysisId(analysis.getId())).thenReturn(List.of(rule));

        when(proposalRepository.save(any(CodeProposal.class))).thenAnswer(invocation -> {
            CodeProposal p = invocation.getArgument(0);
            p.setId("prop-123");
            return p;
        });

        when(changeRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(testRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateProposalRequest request = CreateProposalRequest.builder()
                .moduleName("Billing")
                .title("Customer Billing Domain")
                .description("Automated recurring billing module")
                .targetLanguage("JAVA")
                .build();

        ProposalDetailResponse response = assistantService.generateModuleProposal(projectId, request);

        assertThat(response).isNotNull();
        assertThat(response.getProposal().getId()).isEqualTo("prop-123");
        assertThat(response.getChanges()).hasSize(4); // Entity, Repository, Service, Controller
        assertThat(response.getTests()).hasSize(1); // Service unit test

        // Verify that business rule is preserved in service
        CodeChange serviceChange = response.getChanges().stream()
                .filter(c -> c.getFilePath().contains("Service.java"))
                .findFirst().orElseThrow();
        assertThat(serviceChange.getProposedContent()).contains("RULE-VAL-01");
    }

    @Test
    void testExplicitUserApprovalGuardrailBlocksUnapprovedApplication() {
        String projectId = "proj-123";
        String proposalId = "prop-unapproved";

        CodeProposal pendingProposal = CodeProposal.builder()
                .projectId(projectId)
                .status("PENDING_REVIEW")
                .build();
        pendingProposal.setId(proposalId);

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(pendingProposal));

        assertThatThrownBy(() -> assistantService.applyProposal(projectId, proposalId, "developer"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Explicit user approval is required");
    }

    @Test
    void testApproveAndApplyProposalWritesToDisk() throws IOException {
        String projectId = "proj-123";
        String proposalId = "prop-appr";

        SourceSnapshot snapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .storagePath(tempDir.toString())
                .build();
        snapshot.setId("snap-123");

        CodeProposal proposal = CodeProposal.builder()
                .projectId(projectId)
                .status("PENDING_REVIEW")
                .build();
        proposal.setId(proposalId);

        CodeChange change = CodeChange.builder()
                .proposalId(proposalId)
                .filePath("src/main/resources/application.properties")
                .changeType("CREATE")
                .proposedContent("app.feature.enabled=true\n")
                .status("PENDING")
                .build();
        change.setId("chg-1");

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(changeRepository.findByProposalId(proposalId)).thenReturn(List.of(change));
        when(proposalRepository.save(any(CodeProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Approve proposal
        CodeProposal approved = assistantService.approveProposal(projectId, proposalId);
        assertThat(approved.getStatus()).isEqualTo("APPROVED");

        // 2. Apply proposal to disk
        when(snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(snapshot));
        CodeProposal applied = assistantService.applyProposal(projectId, proposalId, "admin");

        assertThat(applied.getStatus()).isEqualTo("APPLIED");
        File writtenFile = tempDir.resolve("src/main/resources/application.properties").toFile();
        assertThat(writtenFile).exists();
        assertThat(java.nio.file.Files.readString(writtenFile.toPath())).contains("app.feature.enabled=true");
    }
}
