package com.aiproject.architect.modules.ingestion.controller;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.ingestion.dto.GitHubImportRequest;
import com.aiproject.architect.modules.ingestion.dto.IngestionResponse;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final SourceSnapshotRepository snapshotRepository;

    @PostMapping("/zip")
    public ResponseEntity<ApiResponse<IngestionResponse>> uploadZip(
            @PathVariable String projectId,
            @RequestParam("file") MultipartFile file) {
        IngestionResponse response = ingestionService.startZipIngestion(projectId, file);
        return ResponseEntity.ok(ApiResponse.ok("Archive upload started", response));
    }

    @PostMapping("/github")
    public ResponseEntity<ApiResponse<IngestionResponse>> importGitHub(
            @PathVariable String projectId,
            @Valid @RequestBody GitHubImportRequest request) {
        IngestionResponse response = ingestionService.startGitHubIngestion(projectId, request);
        return ResponseEntity.ok(ApiResponse.ok("GitHub import started", response));
    }

    @PostMapping("/workspace")
    public ResponseEntity<ApiResponse<IngestionResponse>> importWorkspace(
            @PathVariable String projectId,
            @RequestBody Map<String, String> payload) {
        String path = payload.get("path");
        IngestionResponse response = ingestionService.startWorkspaceIngestion(projectId, path);
        return ResponseEntity.ok(ApiResponse.ok("Local workspace import started", response));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<ApiResponse<List<SourceSnapshot>>> getSnapshots(@PathVariable String projectId) {
        List<SourceSnapshot> snapshots = snapshotRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
        return ResponseEntity.ok(ApiResponse.ok(snapshots));
    }

    @GetMapping("/snapshots/latest")
    public ResponseEntity<ApiResponse<SourceSnapshot>> getLatestSnapshot(@PathVariable String projectId) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(snapshot));
    }
}
