package com.aiproject.architect;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
import com.aiproject.architect.modules.intelligence.service.TechnologyDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TechnologyDetectorTest {

    private final TechnologyDetector detector = new TechnologyDetector();

    @Test
    @DisplayName("Should detect Maven, Java, Spring Boot, and PostgreSQL from pom.xml")
    void testDetectFromMaven(@TempDir Path tempDir) throws IOException {
        String pomXml = """
                <project>
                    <properties>
                        <java.version>21</java.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                            <version>3.3.5</version>
                        </dependency>
                        <dependency>
                            <groupId>org.postgresql</groupId>
                            <artifactId>postgresql</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Path pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, pomXml);

        List<ProjectFile> files = List.of(ProjectFile.builder()
                .fileName("pom.xml")
                .filePath("pom.xml")
                .extension("xml")
                .isBinary(false)
                .build());

        List<DetectedTechnology> result = detector.detectTechnologies("analysis-1", tempDir, files);
        assertFalse(result.isEmpty());

        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Java") && "21".equals(t.getVersion())));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Spring Boot")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("PostgreSQL Driver")));
        assertTrue(result.stream().allMatch(t -> t.getConfidenceState() == ConfidenceState.VERIFIED));
    }

    @Test
    @DisplayName("Should detect React, TypeScript, and Tailwind from package.json")
    void testDetectFromPackageJson(@TempDir Path tempDir) throws IOException {
        String packageJson = """
                {
                  "name": "frontend-app",
                  "dependencies": {
                    "react": "^18.3.1",
                    "@tanstack/react-query": "^5.0.0"
                  },
                  "devDependencies": {
                    "typescript": "^5.5.0",
                    "tailwindcss": "^3.4.0"
                  }
                }
                """;
        Path pkgPath = tempDir.resolve("package.json");
        Files.writeString(pkgPath, packageJson);

        List<ProjectFile> files = List.of(ProjectFile.builder()
                .fileName("package.json")
                .filePath("package.json")
                .extension("json")
                .isBinary(false)
                .build());

        List<DetectedTechnology> result = detector.detectTechnologies("analysis-2", tempDir, files);
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("React")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("TypeScript")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Tailwind CSS")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("TanStack Query")));
    }
}
