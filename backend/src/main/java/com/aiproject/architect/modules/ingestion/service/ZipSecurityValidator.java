package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.common.exception.SecurityValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Component
public class ZipSecurityValidator {

    private final long maxExtractedBytes;
    private final int maxFileCount;
    private final int maxCompressionRatio;

    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(
            "node_modules",
            "target",
            "build",
            "dist",
            ".git",
            ".idea",
            ".vscode",
            "vendor",
            "__pycache__",
            ".gradle"
    );

    public ZipSecurityValidator(
            @Value("${app.ingestion.max-extracted-bytes:524288000}")
            long maxExtractedBytes,

            @Value("${app.ingestion.max-file-count:50000}")
            int maxFileCount,

            @Value("${app.ingestion.max-compression-ratio:100}")
            int maxCompressionRatio) {

        if (maxExtractedBytes <= 0 ||
                maxFileCount <= 0 ||
                maxCompressionRatio <= 0) {
            throw new IllegalArgumentException(
                    "ZIP extraction limits must be positive."
            );
        }

        this.maxExtractedBytes = maxExtractedBytes;
        this.maxFileCount = maxFileCount;
        this.maxCompressionRatio = maxCompressionRatio;
    }

    public Path validateAndResolvePath(
            Path targetDir,
            String entryName) throws IOException {

        if (entryName == null || entryName.isBlank()) {
            throw new SecurityValidationException(
                    "Archive contains an empty entry name."
            );
        }

        String sanitizedName = entryName.replace('\\', '/');

        // Reject null bytes.
        if (sanitizedName.indexOf('\0') >= 0) {
            throw new SecurityValidationException(
                    "Invalid null byte in archive entry."
            );
        }

        // Reject Unix absolute paths.
        if (sanitizedName.startsWith("/")) {
            throw new SecurityValidationException(
                    "Absolute archive paths are not allowed."
            );
        }

        // Reject Windows drive paths.
        if (sanitizedName.matches("(?i)^[a-z]:.*")) {
            throw new SecurityValidationException(
                    "Windows absolute paths are not allowed."
            );
        }

        Path root = targetDir.toAbsolutePath().normalize();

        Path relativePath = Path.of(sanitizedName).normalize();

        // Reject parent traversal.
        if (relativePath.isAbsolute() ||
                relativePath.startsWith("..")) {

            throw new SecurityValidationException(
                    "Archive path traversal detected."
            );
        }

        Path resolvedPath = root.resolve(relativePath).normalize();

        // Final extraction boundary validation.
        if (!resolvedPath.startsWith(root)) {
            throw new SecurityValidationException(
                    "Archive entry escapes extraction directory."
            );
        }

        return resolvedPath;
    }

    public boolean shouldSkipPath(String relativePath) {

        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }

        String normalized = relativePath
                .replace('\\', '/')
                .toLowerCase();

        String[] segments = normalized.split("/");

        for (String segment : segments) {

            if (IGNORED_DIRECTORY_NAMES.contains(segment.trim())) {
                return true;
            }
        }

        return false;
    }

    public void checkArchiveBombLimits(
            long currentExtractedBytes,
            int currentFileCount) {

        if (currentExtractedBytes > maxExtractedBytes) {
            throw new SecurityValidationException(
                    "Archive extracted size exceeds the configured limit."
            );
        }

        if (currentFileCount > maxFileCount) {
            throw new SecurityValidationException(
                    "Archive contains too many files."
            );
        }
    }

    public void checkCompressionRatio(
            long uncompressedSize,
            long compressedSize) {

        if (uncompressedSize < 0 || compressedSize < 0) {
            throw new SecurityValidationException(
                    "Invalid archive entry size."
            );
        }

        if (compressedSize == 0 && uncompressedSize > 0) {
            throw new SecurityValidationException(
                    "Invalid compression size."
            );
        }

        if (compressedSize > 0) {

            double ratio =
                    (double) uncompressedSize / compressedSize;

            if (ratio > maxCompressionRatio) {
                throw new SecurityValidationException(
                        "Suspicious ZIP compression ratio detected."
                );
            }
        }
    }
}