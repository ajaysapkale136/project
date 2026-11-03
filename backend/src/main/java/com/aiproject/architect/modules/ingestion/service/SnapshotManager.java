package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SnapshotManager {

    private static final Logger log = LoggerFactory.getLogger(SnapshotManager.class);

    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectFileRepository fileRepository;

    @Transactional
    public SourceSnapshot catalogSnapshot(String projectId, String snapshotType, String commitHash, String branch, Path snapshotDir) throws IOException {
        List<ProjectFile> files = new ArrayList<>();
        long totalBytes = 0;

        try (var stream = Files.walk(snapshotDir)) {
            List<Path> filePaths = stream.filter(Files::isRegularFile).toList();

            for (Path path : filePaths) {
                String relativePath = snapshotDir.relativize(path).toString().replace('\\', '/');

                // Skip ignored directories
                if (shouldIgnore(relativePath)) {
                    continue;
                }

                long size = Files.size(path);
                totalBytes += size;
                String fileName = path.getFileName().toString();
                String extension = getExtension(fileName);
                boolean binary = isBinaryFile(path);
                boolean test = isTestFile(relativePath);
                String sha256 = computeSha256(path);

                ProjectFile pf = ProjectFile.builder()
                        .snapshotId("") // Will assign once snapshot is created
                        .filePath(relativePath)
                        .fileName(fileName)
                        .extension(extension)
                        .sizeBytes(size)
                        .sha256Hash(sha256)
                        .isBinary(binary)
                        .isTest(test)
                        .build();

                files.add(pf);
            }
        }

        SourceSnapshot snapshot = SourceSnapshot.builder()
                .projectId(projectId)
                .snapshotType(snapshotType)
                .commitHash(commitHash != null ? commitHash : "N/A")
                .branch(branch != null ? branch : "main")
                .fileCount((long) files.size())
                .totalBytes(totalBytes)
                .storagePath(snapshotDir.toString())
                .build();

        SourceSnapshot savedSnapshot = snapshotRepository.save(snapshot);

        // Assign snapshot ID and persist files
        for (ProjectFile pf : files) {
            pf.setSnapshotId(savedSnapshot.getId());
        }

        // Batch save files in chunks of 500
        int batchSize = 500;
        for (int i = 0; i < files.size(); i += batchSize) {
            int end = Math.min(i + batchSize, files.size());
            fileRepository.saveAll(files.subList(i, end));
        }

        log.info("Cataloged snapshot {} with {} files ({} bytes)", savedSnapshot.getId(), files.size(), totalBytes);
        return savedSnapshot;
    }

    private boolean shouldIgnore(String path) {
        String lower = path.toLowerCase();
        return lower.contains("/.git/") || lower.startsWith(".git/") ||
                lower.contains("/node_modules/") || lower.startsWith("node_modules/") ||
                lower.contains("/target/") || lower.startsWith("target/") ||
                lower.contains("/build/") || lower.startsWith("build/") ||
                lower.contains("/dist/") || lower.startsWith("dist/");
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private boolean isTestFile(String path) {
        String lower = path.toLowerCase();
        return lower.contains("/test/") || lower.contains("/tests/") || lower.contains("/src/test/") ||
                lower.endsWith("test.java") || lower.endsWith("tests.java") ||
                lower.endsWith(".test.ts") || lower.endsWith(".spec.ts") ||
                lower.endsWith(".test.js") || lower.endsWith(".spec.js");
    }

    private boolean isBinaryFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] bytes = new byte[512];
            int read = in.read(bytes);
            if (read <= 0) return false;
            for (int i = 0; i < read; i++) {
                if (bytes[i] == 0) { // Null byte indicates binary
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String computeSha256(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "";
        }
    }
}
