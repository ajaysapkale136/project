package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ZipExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ZipExtractionService.class);
    private final ZipSecurityValidator securityValidator;

    public ExtractionResult extractZipStream(InputStream inputStream, Path destinationDir) {
        long totalBytesExtracted = 0;
        int totalFilesCount = 0;

        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(new BufferedInputStream(inputStream))) {
            ZipArchiveEntry entry;

            byte[] buffer = new byte[8192];

            while ((entry = zipIn.getNextZipEntry()) != null) {
                String entryName = entry.getName();

                if (securityValidator.shouldSkipPath(entryName)) {
                    continue;
                }

                Path resolvedPath = securityValidator.validateAndResolvePath(destinationDir, entryName);

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                    continue;
                }

                // Ensure parent directories exist
                Path parent = resolvedPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                totalFilesCount++;
                securityValidator.checkArchiveBombLimits(totalBytesExtracted, totalFilesCount);

                long entryBytes = 0;
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(resolvedPath))) {
                    int read;
                    while ((read = zipIn.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        entryBytes += read;
                        totalBytesExtracted += read;
                        securityValidator.checkArchiveBombLimits(totalBytesExtracted, totalFilesCount);
                    }
                }

                if (entry.getCompressedSize() > 0) {
                    securityValidator.checkCompressionRatio(entryBytes, entry.getCompressedSize());
                }
            }

            log.info("Securely extracted {} files, total {} bytes into {}", totalFilesCount, totalBytesExtracted, destinationDir);
            return new ExtractionResult(totalFilesCount, totalBytesExtracted);

        } catch (IOException e) {
            log.error("Failed to extract ZIP archive: {}", e.getMessage(), e);
            throw new AppException("Failed to extract ZIP archive: " + e.getMessage(), e);
        }
    }

    public record ExtractionResult(int fileCount, long totalBytes) {}
}
