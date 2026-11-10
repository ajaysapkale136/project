
package com.aiproject.architect;

import com.aiproject.architect.common.exception.SecurityValidationException;
import com.aiproject.architect.modules.ingestion.service.ZipSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipSecurityValidatorTest {

    private ZipSecurityValidator validator;

    @TempDir
    Path jailPath;

    @BeforeEach
    void setUp() {
        validator = new ZipSecurityValidator(10_000_000L, 1000, 50);
    }

    @Test
    @DisplayName("Allows safe relative paths")
    void allowsSafeRelativePath() throws IOException {
        Path resolved = validator.validateAndResolvePath(
                jailPath, "src/main/java/App.java"
        );

        assertNotNull(resolved);
        assertTrue(resolved.startsWith(jailPath));
        assertTrue(resolved.endsWith("App.java"));
    }

    @Test
    @DisplayName("Blocks directory traversal attacks")
    void blocksZipSlipAttacks() {
        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "../../etc/passwd"));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(
                        jailPath, "..\\..\\Windows\\System32\\cmd.exe"));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(
                        jailPath, "src/../../../outside.txt"));
    }

    @Test
    @DisplayName("Blocks absolute paths")
    void blocksAbsolutePaths() {
        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "/etc/passwd"));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "C:\\Windows\\System32\\cmd.exe"));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "\\\\server\\share\\file.txt"));
    }

    @Test
    @DisplayName("Rejects empty and null-byte entry names")
    void rejectsInvalidEntryNames() {
        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, null));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, ""));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "folder/file\u0000.txt"));
    }

    @Test
    @DisplayName("Skips ignored directories")
    void skipsIgnoredDirectories() {
        assertTrue(validator.shouldSkipPath("node_modules/react/index.js"));
        assertTrue(validator.shouldSkipPath(".git/HEAD"));
        assertTrue(validator.shouldSkipPath("target/classes/App.class"));
        assertTrue(validator.shouldSkipPath("build/libs/app.jar"));
        assertTrue(validator.shouldSkipPath("src\\node_modules\\package\\index.js"));

        assertFalse(validator.shouldSkipPath("src/main/java/OrderService.java"));
    }

    @Test
    @DisplayName("Rejects archives exceeding extracted size limit")
    void rejectsExtractedSizeLimit() {
        assertThrows(SecurityValidationException.class, () ->
                validator.checkArchiveBombLimits(10_000_001L, 10));
    }

    @Test
    @DisplayName("Rejects archives exceeding file count limit")
    void rejectsFileCountLimit() {
        assertThrows(SecurityValidationException.class, () ->
                validator.checkArchiveBombLimits(1000L, 1001));
    }

    @Test
    @DisplayName("Rejects excessive compression ratio")
    void rejectsExcessiveCompressionRatio() {
        assertThrows(SecurityValidationException.class, () ->
                validator.checkCompressionRatio(1_000_000L, 10_000L));
    }

    @Test
    @DisplayName("Allows compression ratio within configured limit")
    void allowsCompressionRatioWithinLimit() {
        assertDoesNotThrow(() ->
                validator.checkCompressionRatio(500_000L, 10_000L));
    }

    @Test
    @DisplayName("Rejects zero compressed size for non-empty entry")
    void handlesZeroCompressedSize() {
        assertThrows(SecurityValidationException.class, () ->
                validator.checkCompressionRatio(1_000_000L, 0));
    }
}
