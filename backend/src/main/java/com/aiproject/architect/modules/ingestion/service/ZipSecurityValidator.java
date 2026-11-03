package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.common.exception.SecurityValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Component
public class ZipSecurityValidator {

    private final long maxExtractedBytes;
    private final int maxFileCount;
    private final int maxCompressionRatio;

    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(
            "node_modules", "target", "build", "dist", ".git", ".idea", ".vscode", "vendor", "__pycache__", ".gradle"
    );

    public ZipSecurityValidator(
            @Value("${app.ingestion.max-extracted-bytes:524288000}") long maxExtractedBytes,
            @Value("${app.ingestion.max-file-count:50000}") int maxFileCount,
            @Value("${app.ingestion.max-compression-ratio:100}") int maxCompressionRatio) {
        this.maxExtractedBytes = maxExtractedBytes;
        this.maxFileCount = maxFileCount;
        this.maxCompressionRatio = maxCompressionRatio;
    }

    public Path validateAndResolvePath(Path targetDir, String entryName) throws IOException {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new SecurityValidationException("Archive contains an empty entry name.");
        }

        // Clean redundant slashes and null bytes
        String sanitizedName = entryName.replace('\\', '/');
        if (sanitizedName.contains("\0")) {
            throw new SecurityValidationException("Poison NULL byte detected in archive entry name.");
        }

        Path resolvedPath = targetDir.resolve(sanitizedName).normalize();

        // Enforce anti-ZIP-slip jail check
        if (!resolvedPath.startsWith(targetDir.normalize())) {
            throw new SecurityValidationException("Zip Slip attack detected! Malicious entry: " + entryName);
        }

        return resolvedPath;
    }

    public boolean shouldSkipPath(String relativePath) {
        String normalized = relativePath.replace('\\', '/').toLowerCase();
        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if (IGNORED_DIRECTORY_NAMES.contains(segment.trim())) {
                return true;
            }
        }
        return false;
    }

    public void checkArchiveBombLimits(long currentExtractedBytes, int currentFileCount) {
        if (currentExtractedBytes > maxExtractedBytes) {
            throw new SecurityValidationException(String.format(
                    "Archive bomb protection triggered: extracted size exceeds maximum allowed of %d bytes.", maxExtractedBytes));
        }
        if (currentFileCount > maxFileCount) {
            throw new SecurityValidationException(String.format(
                    "Archive bomb protection triggered: file count exceeds maximum allowed of %d files.", maxFileCount));
        }
    }

    public void checkCompressionRatio(long uncompressedSize, long compressedSize) {
        if (compressedSize > 0 && (uncompressedSize / compressedSize) > maxCompressionRatio) {
            throw new SecurityValidationException(String.format(
                    "Suspicious compression ratio detected (> %d:1). Extraction halted.", maxCompressionRatio));
        }
    }
}
