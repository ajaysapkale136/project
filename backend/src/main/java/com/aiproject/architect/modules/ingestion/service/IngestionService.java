package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.common.exception.SecurityValidationException;
import com.aiproject.architect.modules.ingestion.dto.GitHubImportRequest;
import com.aiproject.architect.modules.ingestion.dto.IngestionResponse;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.jobs.entity.GenerationJob;
import com.aiproject.architect.modules.jobs.service.JobExecutionService;
import com.aiproject.architect.modules.project.entity.Project;
import com.aiproject.architect.modules.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ProjectService projectService;
    private final StorageService storageService;
    private final ZipExtractionService zipExtractionService;
    private final GitImportService gitImportService;
    private final SnapshotManager snapshotManager;
    private final JobExecutionService jobExecutionService;

    public IngestionResponse startZipIngestion(String projectId, MultipartFile file) {
        Project project = projectService.getEntityById(projectId);

        if (file.isEmpty()) {
            throw new SecurityValidationException("Uploaded archive is empty.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".zip")) {
            throw new SecurityValidationException("Only standard .zip archives are supported.");
        }

        GenerationJob job = jobExecutionService.createJob(projectId, "INGESTION", "Validating uploaded archive...");

        // Process extraction and cataloging asynchronously
        processZipAsync(project, file, job.getId());

        return IngestionResponse.builder()
                .jobId(job.getId())
                .projectId(projectId)
                .status("RUNNING")
                .message("Archive uploaded successfully. Background ingestion in progress.")
                .build();
    }

    @Async("jobTaskExecutor")
    public void processZipAsync(Project project, MultipartFile file, String jobId) {
        try {
            jobExecutionService.updateProgress(jobId, 20, "Extracting files securely with anti-ZIP-slip protection...");
            Path snapshotDir = storageService.createSnapshotDirectory(project.getId());

            try (InputStream in = file.getInputStream()) {
                zipExtractionService.extractZipStream(in, snapshotDir);
            }

            jobExecutionService.updateProgress(jobId, 60, "Scanning directory hierarchy and computing cryptographic file hashes...");
            SourceSnapshot snapshot = snapshotManager.catalogSnapshot(
                    project.getId(), "ZIP_UPLOAD", "LOCAL_ARCHIVE", "local", snapshotDir
            );

            jobExecutionService.updateProgress(jobId, 90, "Finalizing project snapshot...");
            projectService.updateStatus(project.getId(), "INGESTED");

            jobExecutionService.completeJob(jobId, String.format("Ingestion complete. %d files cataloged.", snapshot.getFileCount()));

        } catch (Exception e) {
            log.error("ZIP ingestion failed for project {}", project.getId(), e);
            jobExecutionService.failJob(jobId, e.getMessage());
            projectService.updateStatus(project.getId(), "ERROR");
        }
    }

    public IngestionResponse startGitHubIngestion(String projectId, GitHubImportRequest request) {
        Project project = projectService.getEntityById(projectId);
        GenerationJob job = jobExecutionService.createJob(projectId, "INGESTION", "Connecting to GitHub repository...");

        processGitHubAsync(project, request, job.getId());

        return IngestionResponse.builder()
                .jobId(job.getId())
                .projectId(projectId)
                .status("RUNNING")
                .message("GitHub clone started. Ingestion in progress.")
                .build();
    }

    @Async("jobTaskExecutor")
    public void processGitHubAsync(Project project, GitHubImportRequest request, String jobId) {
        try {
            jobExecutionService.updateProgress(jobId, 25, "Cloning repository branch " + (request.getBranch() != null ? request.getBranch() : "default") + "...");
            Path snapshotDir = storageService.createSnapshotDirectory(project.getId());

            GitImportService.GitImportResult importResult = gitImportService.importRepository(
                    request.getRepositoryUrl(), request.getBranch(), request.getAccessToken(), snapshotDir
            );

            jobExecutionService.updateProgress(jobId, 65, "Indexing files and computing checksums...");
            SourceSnapshot snapshot = snapshotManager.catalogSnapshot(
                    project.getId(), "GITHUB_IMPORT", importResult.commitHash(), importResult.branch(), snapshotDir
            );

            jobExecutionService.updateProgress(jobId, 90, "Finalizing snapshot...");
            projectService.updateStatus(project.getId(), "INGESTED");

            jobExecutionService.completeJob(jobId, String.format("GitHub import complete. Commit %s (%d files).",
                    importResult.commitHash(), snapshot.getFileCount()));

        } catch (Exception e) {
            log.error("GitHub ingestion failed for project {}", project.getId(), e);
            jobExecutionService.failJob(jobId, e.getMessage());
            projectService.updateStatus(project.getId(), "ERROR");
        }
    }

    public IngestionResponse startWorkspaceIngestion(String projectId, String localPath) {
        Project project = projectService.getEntityById(projectId);
        Path sourcePath = Paths.get(localPath).toAbsolutePath().normalize();

        if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
            throw new SecurityValidationException("Specified local workspace path does not exist or is not a directory.");
        }

        GenerationJob job = jobExecutionService.createJob(projectId, "INGESTION", "Scanning local workspace...");

        processWorkspaceAsync(project, sourcePath, job.getId());

        return IngestionResponse.builder()
                .jobId(job.getId())
                .projectId(projectId)
                .status("RUNNING")
                .message("Workspace ingestion initiated.")
                .build();
    }

    @Async("jobTaskExecutor")
    public void processWorkspaceAsync(Project project, Path sourcePath, String jobId) {
        try {
            jobExecutionService.updateProgress(jobId, 30, "Copying workspace files to secure snapshot storage...");
            Path snapshotDir = storageService.createSnapshotDirectory(project.getId());

            // Copy non-ignored files
            try (var stream = Files.walk(sourcePath)) {
                stream.forEach(source -> {
                    try {
                        Path relative = sourcePath.relativize(source);
                        String relStr = relative.toString().replace('\\', '/').toLowerCase();
                        if (relStr.contains("node_modules") || relStr.contains(".git") || relStr.contains("target") || relStr.contains("build")) {
                            return;
                        }
                        Path destination = snapshotDir.resolve(relative);
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(destination);
                        } else {
                            Path parent = destination.getParent();
                            if (parent != null && !Files.exists(parent)) {
                                Files.createDirectories(parent);
                            }
                            Files.copy(source, destination);
                        }
                    } catch (Exception ignored) {
                    }
                });
            }

            jobExecutionService.updateProgress(jobId, 70, "Indexing local files and calculating hashes...");
            SourceSnapshot snapshot = snapshotManager.catalogSnapshot(
                    project.getId(), "WORKSPACE", "LOCAL_WORKSPACE", "local", snapshotDir
            );

            projectService.updateStatus(project.getId(), "INGESTED");
            jobExecutionService.completeJob(jobId, String.format("Workspace ingested. %d files cataloged.", snapshot.getFileCount()));

        } catch (Exception e) {
            log.error("Workspace ingestion failed for project {}", project.getId(), e);
            jobExecutionService.failJob(jobId, e.getMessage());
            projectService.updateStatus(project.getId(), "ERROR");
        }
    }
}
