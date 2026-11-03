package com.aiproject.architect.modules.patch.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.patch.dto.*;
import com.aiproject.architect.modules.patch.entity.ChangeSet;
import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import com.aiproject.architect.modules.patch.service.PatchExportService;
import com.aiproject.architect.modules.patch.service.PatchGenerationService;
import com.aiproject.architect.modules.patch.service.WorktreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/worktree")
@RequiredArgsConstructor
public class PatchWorktreeController {

    private final PatchGenerationService patchService;
    private final WorktreeService worktreeService;
    private final PatchExportService exportService;

    @PostMapping("/changesets")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> createChangeSet(
            @PathVariable String projectId,
            @RequestBody CreateChangeSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Patch change set synthesized", patchService.generateChangeSet(projectId, request)));
    }

    @GetMapping("/changesets")
    public ResponseEntity<ApiResponse<List<ChangeSet>>> listChangeSets(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Change sets retrieved", patchService.listChangeSets(projectId)));
    }

    @GetMapping("/changesets/{changeSetId}")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> getChangeSet(
            @PathVariable String projectId,
            @PathVariable String changeSetId) {
        return ResponseEntity.ok(ApiResponse.ok("Change set details retrieved", patchService.getChangeSetDetails(changeSetId)));
    }

    @PostMapping("/changesets/{changeSetId}/approve")
    public ResponseEntity<ApiResponse<ChangeSet>> approveChangeSet(
            @PathVariable String projectId,
            @PathVariable String changeSetId) {
        return ResponseEntity.ok(ApiResponse.ok("Change set approved", patchService.approveChangeSet(changeSetId)));
    }

    @PostMapping("/changesets/{changeSetId}/reject")
    public ResponseEntity<ApiResponse<ChangeSet>> rejectChangeSet(
            @PathVariable String projectId,
            @PathVariable String changeSetId) {
        return ResponseEntity.ok(ApiResponse.ok("Change set rejected", patchService.rejectChangeSet(changeSetId)));
    }

    @PostMapping("/changesets/{changeSetId}/apply")
    public ResponseEntity<ApiResponse<ChangeSet>> applyChangeSet(
            @PathVariable String projectId,
            @PathVariable String changeSetId) {
        return ResponseEntity.ok(ApiResponse.ok("Change set applied to isolated worktree", worktreeService.applyChangeSet(changeSetId)));
    }

    @PostMapping("/changesets/{changeSetId}/rollback")
    public ResponseEntity<ApiResponse<RollbackResponse>> rollbackChangeSet(
            @PathVariable String projectId,
            @PathVariable String changeSetId) {
        return ResponseEntity.ok(ApiResponse.ok("Change set rolled back", worktreeService.rollbackChangeSet(changeSetId)));
    }

    @PostMapping("/files/{fileId}/decision")
    public ResponseEntity<ApiResponse<ChangeSetFile>> updateFileDecision(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @RequestBody Map<String, String> body) {
        String decision = body.getOrDefault("decision", "ACCEPTED");
        return ResponseEntity.ok(ApiResponse.ok("File decision updated", patchService.updateFileDecision(fileId, decision)));
    }

    @GetMapping("/changesets/{changeSetId}/export/patch")
    public ResponseEntity<String> exportPatch(
            @PathVariable String projectId,
            @PathVariable String changeSetId) {
        String patch = exportService.exportUnifiedPatch(changeSetId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"changeset-" + changeSetId + ".patch\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(patch);
    }

    @GetMapping("/export/zip")
    public ResponseEntity<byte[]> exportWorkspaceZip(@PathVariable String projectId) throws IOException {
        IsolatedWorkspace ws = worktreeService.getOrCreateWorkspace(projectId);
        byte[] zipBytes = exportService.exportWorkspaceZip(ws.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"workspace-" + projectId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }
}
