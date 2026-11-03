package com.aiproject.architect.modules.patch.service;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.patch.dto.RollbackResponse;
import com.aiproject.architect.modules.patch.entity.ChangeSet;
import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import com.aiproject.architect.modules.patch.repository.ChangeSetFileRepository;
import com.aiproject.architect.modules.patch.repository.ChangeSetRepository;
import com.aiproject.architect.modules.patch.repository.IsolatedWorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorktreeService {

    private static final Logger log = LoggerFactory.getLogger(WorktreeService.class);

    private final IsolatedWorkspaceRepository workspaceRepository;
    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectFileRepository fileRepository;
    private final ChangeSetRepository changeSetRepository;
    private final ChangeSetFileRepository fileChangeRepository;

    @Transactional
    public IsolatedWorkspace getOrCreateWorkspace(String projectId) {
        SourceSnapshot latestSnapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No snapshot found for project " + projectId));

        Optional<IsolatedWorkspace> existing = workspaceRepository.findTopByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "ACTIVE");
        if (existing.isPresent()) {
            IsolatedWorkspace ws = existing.get();
            if (ws.getSnapshotId().equals(latestSnapshot.getId()) && Files.exists(Paths.get(ws.getWorkspacePath()))) {
                return ws;
            }
            // Mark existing as STALE if snapshot has evolved
            ws.setStatus("STALE");
            workspaceRepository.save(ws);
        }

        // Create new isolated worktree
        String workspaceId = UUID.randomUUID().toString();
        Path worktreePath = Paths.get("storage", "worktrees", workspaceId).toAbsolutePath().normalize();

        try {
            Files.createDirectories(worktreePath);
            copySnapshotToWorktree(latestSnapshot, worktreePath);
        } catch (IOException e) {
            throw new AppException("Failed to initialize isolated worktree at " + worktreePath + ": " + e.getMessage());
        }

        IsolatedWorkspace workspace = IsolatedWorkspace.builder()
                .projectId(projectId)
                .snapshotId(latestSnapshot.getId())
                .workspacePath(worktreePath.toString())
                .status("ACTIVE")
                .build();
        workspace.setId(workspaceId);
        return workspaceRepository.save(workspace);
    }

    private void copySnapshotToWorktree(SourceSnapshot snapshot, Path worktreePath) throws IOException {
        Path sourcePath = Paths.get(snapshot.getStoragePath());
        if (!Files.exists(sourcePath)) {
            return;
        }

        List<ProjectFile> files = fileRepository.findAllBySnapshotId(snapshot.getId());
        for (ProjectFile pf : files) {
            Path srcFile = sourcePath.resolve(pf.getFilePath()).normalize();
            Path destFile = worktreePath.resolve(pf.getFilePath()).normalize();

            // Anti-jail check
            if (!destFile.startsWith(worktreePath)) {
                log.warn("Skipping file copying outside worktree boundary: {}", pf.getFilePath());
                continue;
            }

            if (Files.exists(srcFile)) {
                if (destFile.getParent() != null) {
                    Files.createDirectories(destFile.getParent());
                }
                Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public boolean isWorkspaceStale(IsolatedWorkspace workspace) {
        SourceSnapshot latest = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(workspace.getProjectId())
                .orElse(null);
        if (latest == null || latest.getId() == null) return false;
        return !latest.getId().equals(workspace.getSnapshotId());
    }

    @Transactional
    public ChangeSet applyChangeSet(String changeSetId) {
        ChangeSet cs = changeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set not found: " + changeSetId));

        IsolatedWorkspace workspace = workspaceRepository.findById(cs.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + cs.getWorkspaceId()));

        if (isWorkspaceStale(workspace)) {
            cs.setStatus("STALE_CONFLICT");
            changeSetRepository.save(cs);
            throw new AppException("Conflict detected: Project source has changed since this change set was generated. " +
                    "Change set marked as STALE_CONFLICT to protect current codebase.");
        }

        if (!"APPROVED".equals(cs.getStatus())) {
            throw new AppException("Change set must be explicitly APPROVED before application. Current status: " + cs.getStatus());
        }

        Path worktreeRoot = Paths.get(workspace.getWorkspacePath());
        List<ChangeSetFile> files = fileChangeRepository.findByChangeSetId(changeSetId);

        for (ChangeSetFile file : files) {
            if ("REJECTED".equals(file.getStatus())) {
                continue; // Skip individual files rejected by developer
            }

            Path target = worktreeRoot.resolve(file.getFilePath()).normalize();
            if (!target.startsWith(worktreeRoot)) {
                throw new AppException("Path traversal blocked during patch application: " + file.getFilePath());
            }

            try {
                if ("DELETE".equalsIgnoreCase(file.getChangeType())) {
                    Files.deleteIfExists(target);
                } else {
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.writeString(target, file.getProposedContent());
                }
                file.setStatus("ACCEPTED");
            } catch (IOException e) {
                throw new AppException("Failed to apply patch to file " + file.getFilePath() + ": " + e.getMessage());
            }
        }

        fileChangeRepository.saveAll(files);
        cs.setStatus("APPLIED");
        cs.setAppliedAt(LocalDateTime.now());
        return changeSetRepository.save(cs);
    }

    @Transactional
    public RollbackResponse rollbackChangeSet(String changeSetId) {
        ChangeSet cs = changeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set not found: " + changeSetId));

        if (!"APPLIED".equals(cs.getStatus())) {
            throw new AppException("Cannot rollback change set with status: " + cs.getStatus() + ". Only APPLIED changes can be rolled back.");
        }

        IsolatedWorkspace workspace = workspaceRepository.findById(cs.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + cs.getWorkspaceId()));

        Path worktreeRoot = Paths.get(workspace.getWorkspacePath());
        List<ChangeSetFile> files = fileChangeRepository.findByChangeSetId(changeSetId);
        List<String> revertedFiles = new ArrayList<>();

        for (ChangeSetFile file : files) {
            Path target = worktreeRoot.resolve(file.getFilePath()).normalize();
            if (!target.startsWith(worktreeRoot)) continue;

            try {
                if ("CREATE".equalsIgnoreCase(file.getChangeType())) {
                    Files.deleteIfExists(target);
                    revertedFiles.add(file.getFilePath() + " (deleted newly created file)");
                } else if ("UPDATE".equalsIgnoreCase(file.getChangeType())) {
                    if (file.getOriginalContent() != null) {
                        Files.writeString(target, file.getOriginalContent());
                        revertedFiles.add(file.getFilePath() + " (restored original content)");
                    }
                } else if ("DELETE".equalsIgnoreCase(file.getChangeType())) {
                    if (file.getOriginalContent() != null) {
                        if (target.getParent() != null) Files.createDirectories(target.getParent());
                        Files.writeString(target, file.getOriginalContent());
                        revertedFiles.add(file.getFilePath() + " (restored deleted file)");
                    }
                }
            } catch (IOException e) {
                log.error("Failed to revert file {}: {}", file.getFilePath(), e.getMessage());
            }
        }

        cs.setStatus("ROLLED_BACK");
        cs.setRolledBackAt(LocalDateTime.now());
        changeSetRepository.save(cs);

        return RollbackResponse.builder()
                .changeSetId(changeSetId)
                .status("ROLLED_BACK")
                .revertedFiles(revertedFiles)
                .message("Change set successfully rolled back. Reverted " + revertedFiles.size() + " files.")
                .build();
    }
}
