package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.common.exception.SecurityValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path rootStoragePath;

    public StorageService(@Value("${app.storage.base-path:d:/project/storage}") String basePath) {
        this.rootStoragePath = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            if (!Files.exists(this.rootStoragePath)) {
                Files.createDirectories(this.rootStoragePath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize storage root directory at {}", this.rootStoragePath, e);
            throw new RuntimeException("Could not initialize storage service", e);
        }
    }

    public Path createSnapshotDirectory(String projectId) throws IOException {
        String snapshotId = UUID.randomUUID().toString();
        Path projectDir = rootStoragePath.resolve(projectId).normalize();
        Path snapshotDir = projectDir.resolve(snapshotId).normalize();

        // Enforce boundary check against directory traversal
        if (!snapshotDir.startsWith(rootStoragePath)) {
            throw new SecurityValidationException("Path traversal attempt detected during directory creation.");
        }

        Files.createDirectories(snapshotDir);
        return snapshotDir;
    }

    public Path getRootStoragePath() {
        return rootStoragePath;
    }
}
