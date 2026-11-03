package com.aiproject.architect;

import com.aiproject.architect.common.exception.SecurityValidationException;
import com.aiproject.architect.modules.ingestion.service.ZipSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ZipSecurityValidatorTest {

    private ZipSecurityValidator validator;
    private Path jailPath;

    @BeforeEach
    void setUp() {
        validator = new ZipSecurityValidator(10_000_000L, 1000, 50);
        jailPath = Paths.get("d:/project/storage/test-jail").toAbsolutePath().normalize();
    }

    @Test
    @DisplayName("Should allow safe relative paths within extraction jail")
    void testSafeRelativePath() throws IOException {
        Path resolved = validator.validateAndResolvePath(jailPath, "src/main/java/App.java");
        assertNotNull(resolved);
        assertTrue(resolved.startsWith(jailPath));
        assertTrue(resolved.endsWith("App.java"));
    }

    @Test
    @DisplayName("Should detect and block Zip Slip directory traversal attacks")
    void testZipSlipAttackBlocked() {
        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "../../etc/passwd"));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "..\\..\\Windows\\System32\\cmd.exe"));

        assertThrows(SecurityValidationException.class, () ->
                validator.validateAndResolvePath(jailPath, "subfolder/../../../evil.sh"));
    }

    @Test
    @DisplayName("Should correctly filter out build and metadata directories")
    void testSkipIgnoredDirectories() {
        assertTrue(validator.shouldSkipPath("node_modules/react/index.js"));
        assertTrue(validator.shouldSkipPath(".git/HEAD"));
        assertTrue(validator.shouldSkipPath("target/classes/App.class"));
        assertTrue(validator.shouldSkipPath("build/libs/app.jar"));
        assertFalse(validator.shouldSkipPath("src/main/java/OrderService.java"));
    }

    @Test
    @DisplayName("Should detect archive decompression bomb limits")
    void testArchiveBombLimits() {
        assertThrows(SecurityValidationException.class, () ->
                validator.checkArchiveBombLimits(20_000_000L, 10)); // Exceeds 10MB limit

        assertThrows(SecurityValidationException.class, () ->
                validator.checkArchiveBombLimits(1_000L, 5000)); // Exceeds 1000 file limit

        assertThrows(SecurityValidationException.class, () ->
                validator.checkCompressionRatio(1_000_000L, 10_000L)); // 100:1 ratio exceeds 50:1 limit
    }
}
