package com.aiproject.architect.modules.intelligence.service;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TechnologyDetector {

    private static final Logger log = LoggerFactory.getLogger(TechnologyDetector.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DetectedTechnology> detectTechnologies(String analysisId, Path snapshotDir, List<ProjectFile> files) {
        List<DetectedTechnology> technologies = new ArrayList<>();

        for (ProjectFile file : files) {
            String fileName = file.getFileName().toLowerCase();
            Path fullPath = snapshotDir.resolve(file.getFilePath());

            if (!Files.exists(fullPath)) continue;

            try {
                if (fileName.equals("pom.xml")) {
                    detectFromMavenPom(analysisId, fullPath, file.getFilePath(), technologies);
                } else if (fileName.equals("build.gradle") || fileName.equals("build.gradle.kts")) {
                    detectFromGradle(analysisId, fullPath, file.getFilePath(), technologies);
                } else if (fileName.equals("package.json")) {
                    detectFromPackageJson(analysisId, fullPath, file.getFilePath(), technologies);
                } else if (fileName.equals("requirements.txt") || fileName.equals("pyproject.toml")) {
                    detectFromPythonManifest(analysisId, fullPath, file.getFilePath(), technologies);
                } else if (fileName.equals("dockerfile")) {
                    technologies.add(buildTech(analysisId, "Docker", "DEPLOYMENT", null, file.getFilePath()));
                } else if (fileName.equals("docker-compose.yml") || fileName.equals("docker-compose.yaml")) {
                    technologies.add(buildTech(analysisId, "Docker Compose", "DEPLOYMENT", null, file.getFilePath()));
                }
            } catch (Exception e) {
                log.warn("Error detecting technology from file {}: {}", file.getFilePath(), e.getMessage());
            }
        }

        // Detect SQL schemas
        boolean hasSql = files.stream().anyMatch(f -> "sql".equalsIgnoreCase(f.getExtension()));
        if (hasSql) {
            technologies.add(buildTech(analysisId, "SQL Database Schema", "DATABASE", null, "Schema DDL files"));
        }

        return technologies;
    }

    private void detectFromMavenPom(String analysisId, Path path, String relPath, List<DetectedTechnology> list) throws Exception {
        String content = Files.readString(path);
        list.add(buildTech(analysisId, "Java", "BACKEND", extractPattern(content, "<java\\.version>([^<]+)</java\\.version>"), relPath));
        list.add(buildTech(analysisId, "Apache Maven", "BUILD_TOOL", null, relPath));

        if (content.contains("spring-boot")) {
            String version = extractPattern(content, "<version>([23]\\.[0-9]+\\.[0-9]+)</version>");
            list.add(buildTech(analysisId, "Spring Boot", "BACKEND", version, relPath));
        }
        if (content.contains("spring-boot-starter-security")) {
            list.add(buildTech(analysisId, "Spring Security", "BACKEND", null, relPath));
        }
        if (content.contains("spring-boot-starter-data-jpa") || content.contains("hibernate")) {
            list.add(buildTech(analysisId, "Hibernate / JPA", "DATABASE", null, relPath));
        }
        if (content.contains("postgresql")) {
            list.add(buildTech(analysisId, "PostgreSQL Driver", "DATABASE", null, relPath));
        }
        if (content.contains("h2database")) {
            list.add(buildTech(analysisId, "H2 In-Memory DB", "DATABASE", null, relPath));
        }
        if (content.contains("flyway")) {
            list.add(buildTech(analysisId, "Flyway Migrations", "DATABASE", null, relPath));
        }
    }

    private void detectFromGradle(String analysisId, Path path, String relPath, List<DetectedTechnology> list) throws Exception {
        String content = Files.readString(path);
        list.add(buildTech(analysisId, "Gradle", "BUILD_TOOL", null, relPath));
        if (content.contains("spring-boot") || content.contains("org.springframework.boot")) {
            list.add(buildTech(analysisId, "Spring Boot", "BACKEND", null, relPath));
        }
    }

    private void detectFromPackageJson(String analysisId, Path path, String relPath, List<DetectedTechnology> list) throws Exception {
        JsonNode root = objectMapper.readTree(Files.readAllBytes(path));
        list.add(buildTech(analysisId, "Node.js", "BACKEND", null, relPath));

        JsonNode deps = root.path("dependencies");
        JsonNode devDeps = root.path("devDependencies");

        checkJsDep(analysisId, deps, devDeps, "react", "React", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "next", "Next.js", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "typescript", "TypeScript", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "express", "Express.js", "BACKEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "tailwindcss", "Tailwind CSS", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "@tanstack/react-query", "TanStack Query", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "react-router-dom", "React Router", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "reactflow", "React Flow", "FRONTEND", relPath, list);
        checkJsDep(analysisId, deps, devDeps, "prisma", "Prisma ORM", "DATABASE", relPath, list);
    }

    private void detectFromPythonManifest(String analysisId, Path path, String relPath, List<DetectedTechnology> list) throws Exception {
        String content = Files.readString(path).toLowerCase();
        list.add(buildTech(analysisId, "Python", "BACKEND", null, relPath));
        if (content.contains("fastapi")) {
            list.add(buildTech(analysisId, "FastAPI", "BACKEND", null, relPath));
        }
        if (content.contains("flask")) {
            list.add(buildTech(analysisId, "Flask", "BACKEND", null, relPath));
        }
        if (content.contains("django")) {
            list.add(buildTech(analysisId, "Django", "BACKEND", null, relPath));
        }
        if (content.contains("sqlalchemy")) {
            list.add(buildTech(analysisId, "SQLAlchemy", "DATABASE", null, relPath));
        }
    }

    private void checkJsDep(String analysisId, JsonNode deps, JsonNode devDeps, String pkg, String name, String category, String relPath, List<DetectedTechnology> list) {
        String version = null;
        if (deps.has(pkg)) {
            version = deps.get(pkg).asText();
        } else if (devDeps.has(pkg)) {
            version = devDeps.get(pkg).asText();
        }
        if (version != null) {
            list.add(buildTech(analysisId, name, category, version, relPath));
        }
    }

    private String extractPattern(String content, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private DetectedTechnology buildTech(String analysisId, String name, String category, String version, String source) {
        return DetectedTechnology.builder()
                .analysisId(analysisId)
                .name(name)
                .category(category)
                .version(version)
                .detectionSource(source)
                .confidenceState(ConfidenceState.VERIFIED)
                .build();
    }
}
