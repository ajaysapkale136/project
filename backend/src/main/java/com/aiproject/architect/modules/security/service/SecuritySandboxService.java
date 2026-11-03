package com.aiproject.architect.modules.security.service;

import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.assistant.entity.CodeChange;
import com.aiproject.architect.modules.assistant.repository.CodeChangeRepository;
import com.aiproject.architect.modules.assistant.repository.CodeProposalRepository;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.security.dto.*;
import com.aiproject.architect.modules.security.entity.ContractVerification;
import com.aiproject.architect.modules.security.entity.SecurityScan;
import com.aiproject.architect.modules.security.entity.VulnerabilityFinding;
import com.aiproject.architect.modules.security.repository.ContractVerificationRepository;
import com.aiproject.architect.modules.security.repository.SecurityScanRepository;
import com.aiproject.architect.modules.security.repository.VulnerabilityFindingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import com.aiproject.architect.modules.security.hardening.AuditLogService;
import com.aiproject.architect.modules.security.hardening.DependencyVulnerabilityScanner;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecuritySandboxService {

    private static final Logger log = LoggerFactory.getLogger(SecuritySandboxService.class);

    private final SecurityScanRepository scanRepository;
    private final VulnerabilityFindingRepository findingRepository;
    private final ContractVerificationRepository contractRepository;
    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectFileRepository fileRepository;
    private final CodeProposalRepository proposalRepository;
    private final CodeChangeRepository changeRepository;
    private final StaticSecurityScanner staticScanner;
    private final DependencyVulnerabilityScanner dependencyScanner;
    private final AuditLogService auditLogService;
    private final ApiContractVerifier contractVerifier;
    private final SandboxedExecutionCoordinator sandboxCoordinator;

    @Transactional
    public SecurityScanResponse runSecurityScan(String projectId) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No snapshot found for project " + projectId));

        SecurityScan scan = SecurityScan.builder()
                .projectId(projectId)
                .snapshotId(snapshot.getId())
                .scannedFilesCount(0)
                .criticalCount(0)
                .highCount(0)
                .mediumCount(0)
                .lowCount(0)
                .passed(true)
                .build();
        scan = scanRepository.save(scan);

        List<VulnerabilityFinding> allFindings = new ArrayList<>();
        int scannedCount = 0;

        // 1. Scan cataloged text files from the source snapshot
        List<ProjectFile> textFiles = fileRepository.findAllBySnapshotIdAndIsBinaryFalse(snapshot.getId());
        Path rootPath = Paths.get(snapshot.getStoragePath());

        for (ProjectFile pf : textFiles) {
            Path diskFile = rootPath.resolve(pf.getFilePath()).normalize();
            if (Files.exists(diskFile)) {
                try {
                    String content = Files.readString(diskFile);
                    List<VulnerabilityFinding> fileFindings = staticScanner.scanFile(scan.getId(), pf.getFilePath(), content);
                    allFindings.addAll(fileFindings);

                    // If file is a build descriptor, scan dependencies
                    String fileName = pf.getFileName().toLowerCase();
                    if (fileName.equals("pom.xml") || fileName.equals("package.json") || fileName.equals("requirements.txt")) {
                        List<VulnerabilityFinding> depFindings = dependencyScanner.scanBuildFile(scan.getId(), pf.getFilePath(), content);
                        allFindings.addAll(depFindings);
                    }

                    scannedCount++;
                } catch (IOException e) {
                    log.warn("Could not read file {} for security scanning: {}", pf.getFilePath(), e.getMessage());
                }
            }
        }

        // 2. Also scan any AI Assistant code proposals to catch vulnerabilities before code is applied!
        var proposals = proposalRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        for (var p : proposals) {
            List<CodeChange> changes = changeRepository.findByProposalId(p.getId());
            for (CodeChange c : changes) {
                if (c.getProposedContent() != null) {
                    List<VulnerabilityFinding> changeFindings = staticScanner.scanFile(scan.getId(), "[PROPOSAL] " + c.getFilePath(), c.getProposedContent());
                    allFindings.addAll(changeFindings);
                    scannedCount++;
                }
            }
        }

        // Aggregate severities
        int critical = 0, high = 0, medium = 0, low = 0;
        for (VulnerabilityFinding vf : allFindings) {
            switch (vf.getSeverity().toUpperCase()) {
                case "CRITICAL" -> critical++;
                case "HIGH" -> high++;
                case "MEDIUM" -> medium++;
                case "LOW" -> low++;
            }
        }

        if (!allFindings.isEmpty()) {
            allFindings = findingRepository.saveAll(allFindings);
        }

        boolean passed = (critical == 0 && high == 0);
        String summary = String.format("Scanned %d files. Found %d issues (%d critical, %d high, %d medium, %d low). Security Gate: %s",
                scannedCount, allFindings.size(), critical, high, medium, low, passed ? "PASSED" : "FAILED (BLOCKING VULNERABILITIES DETECTED)");

        scan.setScannedFilesCount(scannedCount);
        scan.setCriticalCount(critical);
        scan.setHighCount(high);
        scan.setMediumCount(medium);
        scan.setLowCount(low);
        scan.setPassed(passed);
        scan.setScanSummary(summary);
        scan = scanRepository.save(scan);

        auditLogService.logEvent(projectId, "SECURITY_SCAN", "SECURITY_ENGINE",
                String.format("{\"scannedFiles\": %d, \"issues\": %d, \"passed\": %b}", scannedCount, allFindings.size(), passed));

        return SecurityScanResponse.builder()
                .scan(scan)
                .findings(allFindings)
                .build();
    }

    @Transactional(readOnly = true)
    public SecurityScanResponse getLatestScan(String projectId) {
        SecurityScan scan = scanRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElse(null);
        if (scan == null) {
            return SecurityScanResponse.builder()
                    .scan(null)
                    .findings(List.of())
                    .build();
        }
        List<VulnerabilityFinding> findings = findingRepository.findByScanId(scan.getId());
        return SecurityScanResponse.builder()
                .scan(scan)
                .findings(findings)
                .build();
    }

    @Transactional
    public ContractVerificationMatrixResponse verifyContracts(String projectId) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No snapshot found for project " + projectId));

        List<ContractVerification> verifications = contractVerifier.verifyContracts(projectId, snapshot.getId());

        int compliant = (int) verifications.stream().filter(v -> "COMPLIANT".equals(v.getComplianceStatus())).count();
        int breaking = (int) verifications.stream().filter(v -> "BREAKING_CHANGE".equals(v.getComplianceStatus())).count();

        return ContractVerificationMatrixResponse.builder()
                .totalEndpoints(verifications.size())
                .compliantCount(compliant)
                .breakingChangesCount(breaking)
                .verifications(verifications)
                .build();
    }

    @Transactional(readOnly = true)
    public ContractVerificationMatrixResponse getContracts(String projectId) {
        List<ContractVerification> verifications = contractRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        int compliant = (int) verifications.stream().filter(v -> "COMPLIANT".equals(v.getComplianceStatus())).count();
        int breaking = (int) verifications.stream().filter(v -> "BREAKING_CHANGE".equals(v.getComplianceStatus())).count();

        return ContractVerificationMatrixResponse.builder()
                .totalEndpoints(verifications.size())
                .compliantCount(compliant)
                .breakingChangesCount(breaking)
                .verifications(verifications)
                .build();
    }

    public SandboxRunResponse runSandboxedValidation(String projectId, SandboxRunRequest request) {
        return sandboxCoordinator.executeSandboxedValidation(projectId, request);
    }
}
