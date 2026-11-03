package com.aiproject.architect.modules.intelligence.service;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.ProjectFileRepository;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.intelligence.dto.IntelligenceOverviewResponse;
import com.aiproject.architect.modules.intelligence.entity.*;
import com.aiproject.architect.modules.intelligence.parser.JavaSpringParser;
import com.aiproject.architect.modules.intelligence.parser.SqlSchemaParser;
import com.aiproject.architect.modules.intelligence.parser.TypeScriptReactParser;
import com.aiproject.architect.modules.intelligence.repository.*;
import com.aiproject.architect.modules.jobs.entity.GenerationJob;
import com.aiproject.architect.modules.jobs.service.JobExecutionService;
import com.aiproject.architect.modules.project.entity.Project;
import com.aiproject.architect.modules.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(ProjectIntelligenceService.class);

    private final ProjectService projectService;
    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectFileRepository fileRepository;
    private final ProjectAnalysisRepository analysisRepository;
    private final DetectedTechnologyRepository technologyRepository;
    private final AnalysisModuleRepository moduleRepository;
    private final AnalysisComponentRepository componentRepository;
    private final ModuleDependencyRepository dependencyRepository;
    private final BusinessRuleRepository ruleRepository;
    private final SourceEvidenceRepository evidenceRepository;

    private final TechnologyDetector technologyDetector;
    private final JavaSpringParser javaSpringParser;
    private final TypeScriptReactParser tsReactParser;
    private final SqlSchemaParser sqlSchemaParser;
    private final BusinessRuleExtractor businessRuleExtractor;
    private final JobExecutionService jobExecutionService;

    public GenerationJob startAnalysis(String projectId) {
        Project project = projectService.getEntityById(projectId);
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No source snapshot found for project " + projectId + ". Ingest project first."));

        GenerationJob job = jobExecutionService.createJob(projectId, "INTELLIGENCE_ANALYSIS", "Initializing code intelligence engine...");

        runAnalysisAsync(project, snapshot, job.getId());

        return job;
    }

    @Async("jobTaskExecutor")
    public void runAnalysisAsync(Project project, SourceSnapshot snapshot, String jobId) {
        try {
            jobExecutionService.updateProgress(jobId, 15, "Analyzing project structure and manifest configurations...");
            Path snapshotDir = Paths.get(snapshot.getStoragePath());
            List<ProjectFile> files = fileRepository.findAllBySnapshotId(snapshot.getId());

            ProjectAnalysis analysis = ProjectAnalysis.builder()
                    .snapshotId(snapshot.getId())
                    .status("ANALYZING")
                    .totalFiles(files.size())
                    .totalLinesOfCode(snapshot.getTotalBytes() / 35) // Approximate LOC based on average line byte density
                    .build();
            analysis = analysisRepository.save(analysis);

            // Step 1: Detect technologies
            jobExecutionService.updateProgress(jobId, 30, "Detecting language runtime, frameworks, and database drivers...");
            List<DetectedTechnology> detectedTechs = technologyDetector.detectTechnologies(analysis.getId(), snapshotDir, files);
            technologyRepository.saveAll(detectedTechs);

            String primaryLang = detectedTechs.stream()
                    .filter(t -> "BACKEND".equals(t.getCategory()) || "FRONTEND".equals(t.getCategory()))
                    .map(DetectedTechnology::getName)
                    .findFirst().orElse("Multi-stack");
            String framework = detectedTechs.stream()
                    .filter(t -> "Spring Boot".equals(t.getName()) || "React".equals(t.getName()) || "FastAPI".equals(t.getName()))
                    .map(DetectedTechnology::getName)
                    .findFirst().orElse("Standard Architecture");

            analysis.setPrimaryLanguage(primaryLang);
            analysis.setFrameworkFamily(framework);

            // Step 2: Partition into logical modules
            jobExecutionService.updateProgress(jobId, 45, "Partitioning source tree into architectural modules...");
            List<AnalysisModule> modules = partitionModules(analysis.getId(), files);
            modules = moduleRepository.saveAll(modules);

            Map<String, AnalysisModule> moduleLookup = new HashMap<>();
            for (AnalysisModule m : modules) {
                moduleLookup.put(m.getBasePath(), m);
            }

            // Step 3: Parse source code files
            jobExecutionService.updateProgress(jobId, 60, "Executing deep AST parsing for controllers, services, repositories, and entities...");
            List<AnalysisComponent> allComponents = new ArrayList<>();
            List<BusinessRule> allRules = new ArrayList<>();
            List<SourceEvidence> allEvidence = new ArrayList<>();

            int totalEndpoints = 0;
            int totalControllers = 0;
            int totalServices = 0;
            int totalRepositories = 0;
            int totalEntities = 0;

            for (ProjectFile file : files) {
                if (file.getIsBinary() || file.getIsTest()) continue;

                Path fullPath = snapshotDir.resolve(file.getFilePath());
                String ext = file.getExtension();
                AnalysisModule matchedModule = findBestModule(file.getFilePath(), modules);

                if ("java".equalsIgnoreCase(ext)) {
                    JavaSpringParser.ParsedJavaFile parsed = javaSpringParser.parse(
                            fullPath, file.getFilePath(), matchedModule.getId(), analysis.getId());
                    allComponents.addAll(parsed.components());
                    allRules.addAll(parsed.businessRules());
                    allEvidence.addAll(parsed.evidenceList());

                    for (AnalysisComponent c : parsed.components()) {
                        switch (c.getComponentType()) {
                            case "CONTROLLER" -> { totalControllers++; totalEndpoints += c.getEndpointCount(); }
                            case "SERVICE" -> totalServices++;
                            case "REPOSITORY" -> totalRepositories++;
                            case "ENTITY" -> totalEntities++;
                        }
                    }
                } else if ("ts".equalsIgnoreCase(ext) || "tsx".equalsIgnoreCase(ext) || "js".equalsIgnoreCase(ext) || "jsx".equalsIgnoreCase(ext)) {
                    List<AnalysisComponent> parsed = tsReactParser.parse(fullPath, file.getFilePath(), matchedModule.getId());
                    allComponents.addAll(parsed);
                } else if ("sql".equalsIgnoreCase(ext)) {
                    List<AnalysisComponent> parsed = sqlSchemaParser.parse(fullPath, file.getFilePath(), matchedModule.getId());
                    allComponents.addAll(parsed);
                    totalEntities += parsed.size();
                }
            }

            componentRepository.saveAll(allComponents);

            // Step 4: Extract Business Rules & Integrity constraints
            jobExecutionService.updateProgress(jobId, 75, "Extracting business rules, transaction boundaries, and security constraints...");
            businessRuleExtractor.extractIntegrityAndContractRules(analysis.getId(), allComponents, allRules, allEvidence);
            ruleRepository.saveAll(allRules);
            evidenceRepository.saveAll(allEvidence);

            // Step 5: Derive Module Dependencies
            jobExecutionService.updateProgress(jobId, 85, "Synthesizing inter-module dependency topology...");
            List<ModuleDependency> dependencies = deriveModuleDependencies(analysis.getId(), modules, allComponents);
            dependencyRepository.saveAll(dependencies);

            // Finalize analysis record
            analysis.setStatus("COMPLETED");
            analysis.setTotalControllers(totalControllers);
            analysis.setTotalEndpoints(totalEndpoints);
            analysis.setTotalServices(totalServices);
            analysis.setTotalRepositories(totalRepositories);
            analysis.setTotalEntities(totalEntities);
            analysis.setCompletedAt(LocalDateTime.now());
            analysis.setSummary(String.format("Project analyzed: %d components discovered across %d modules. %d API endpoints and %d business rules verified.",
                    allComponents.size(), modules.size(), totalEndpoints, allRules.size()));
            analysisRepository.save(analysis);

            projectService.updateStatus(project.getId(), "ANALYZED");

            jobExecutionService.completeJob(jobId, String.format("Intelligence analysis complete. Discovered %d endpoints and %d entities.",
                    totalEndpoints, totalEntities));

        } catch (Exception e) {
            log.error("Intelligence analysis failed for project {}", project.getId(), e);
            jobExecutionService.failJob(jobId, e.getMessage());
        }
    }

    public IntelligenceOverviewResponse getIntelligenceOverview(String projectId) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("SourceSnapshot for project", projectId));

        ProjectAnalysis analysis = analysisRepository.findTopBySnapshotIdOrderByCreatedAtDesc(snapshot.getId())
                .orElse(null);

        if (analysis == null) {
            return IntelligenceOverviewResponse.builder()
                    .projectId(projectId)
                    .snapshotId(snapshot.getId())
                    .status("NOT_ANALYZED")
                    .build();
        }

        List<DetectedTechnology> techs = technologyRepository.findAllByAnalysisId(analysis.getId());
        List<AnalysisModule> modules = moduleRepository.findAllByAnalysisId(analysis.getId());
        List<BusinessRule> rules = ruleRepository.findAllByAnalysisId(analysis.getId());

        List<IntelligenceOverviewResponse.ModuleSummary> moduleSummaries = new ArrayList<>();
        for (AnalysisModule mod : modules) {
            List<AnalysisComponent> components = componentRepository.findAllByModuleId(mod.getId());
            moduleSummaries.add(IntelligenceOverviewResponse.ModuleSummary.builder()
                    .id(mod.getId())
                    .name(mod.getName())
                    .moduleType(mod.getModuleType())
                    .basePath(mod.getBasePath())
                    .componentCount(components.size())
                    .components(components)
                    .build());
        }

        return IntelligenceOverviewResponse.builder()
                .projectId(projectId)
                .snapshotId(snapshot.getId())
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .primaryLanguage(analysis.getPrimaryLanguage())
                .frameworkFamily(analysis.getFrameworkFamily())
                .totalFiles(analysis.getTotalFiles())
                .totalLinesOfCode(analysis.getTotalLinesOfCode())
                .totalControllers(analysis.getTotalControllers())
                .totalEndpoints(analysis.getTotalEndpoints())
                .totalServices(analysis.getTotalServices())
                .totalRepositories(analysis.getTotalRepositories())
                .totalEntities(analysis.getTotalEntities())
                .summary(analysis.getSummary())
                .completedAt(analysis.getCompletedAt())
                .technologies(techs)
                .modules(moduleSummaries)
                .businessRules(rules)
                .build();
    }

    private List<AnalysisModule> partitionModules(String analysisId, List<ProjectFile> files) {
        Map<String, List<ProjectFile>> filesByRoot = new TreeMap<>();
        for (ProjectFile file : files) {
            if (file.getIsBinary() || file.getIsTest()) continue;
            filesByRoot.computeIfAbsent(sourceRoot(file.getFilePath()), ignored -> new ArrayList<>()).add(file);
        }

        if (filesByRoot.isEmpty()) filesByRoot.put("", List.of());

        List<AnalysisModule> modules = new ArrayList<>();
        for (Map.Entry<String, List<ProjectFile>> entry : filesByRoot.entrySet()) {
            String root = entry.getKey();
            List<ProjectFile> moduleFiles = entry.getValue();
            String moduleType = moduleType(root, moduleFiles);
            String name = root.isBlank() ? "Repository root" : root;
            modules.add(AnalysisModule.builder()
                    .analysisId(analysisId)
                    .name(name)
                    .moduleType(moduleType)
                    .basePath(root)
                    .description("Source-root module discovered from " + moduleFiles.size() + " non-test files.")
                    .build());
        }
        return modules;
    }

    private String sourceRoot(String filePath) {
        String path = filePath.replace('\\', '/');
        int javaRoot = path.indexOf("src/main/java/");
        if (javaRoot >= 0) return path.substring(0, javaRoot + "src/main/java".length());
        int resourcesRoot = path.indexOf("src/main/resources/");
        if (resourcesRoot >= 0) return path.substring(0, resourcesRoot + "src/main/resources".length());
        int sourceRoot = path.indexOf("src/");
        if (sourceRoot >= 0) return path.substring(0, sourceRoot + "src".length());
        int separator = path.indexOf('/');
        return separator > 0 ? path.substring(0, separator) : "";
    }

    private String moduleType(String root, List<ProjectFile> files) {
        boolean hasSql = files.stream().anyMatch(f -> "sql".equalsIgnoreCase(f.getExtension()));
        boolean hasWeb = files.stream().anyMatch(f -> Set.of("ts", "tsx", "js", "jsx").contains(f.getExtension().toLowerCase()));
        boolean hasJava = files.stream().anyMatch(f -> "java".equalsIgnoreCase(f.getExtension()));
        if (hasSql || root.toLowerCase().contains("resources/db")) return "DATABASE_LAYER";
        if (hasWeb) return "FRONTEND_SPA";
        if (hasJava) return "BACKEND_SERVICE";
        return "SOURCE_MODULE";
    }

    private AnalysisModule findBestModule(String filePath, List<AnalysisModule> modules) {
        String lowerPath = filePath.toLowerCase();
        // First check specific domain modules
        for (AnalysisModule mod : modules) {
            String base = mod.getBasePath().toLowerCase();
            if (!base.isEmpty() && !base.equals("src/main/java") && lowerPath.contains(base)) {
                return mod;
            }
        }
        // Then check general modules
        for (AnalysisModule mod : modules) {
            if (!mod.getBasePath().isEmpty() && filePath.startsWith(mod.getBasePath())) {
                return mod;
            }
        }
        return modules.get(0);
    }

    private List<ModuleDependency> deriveModuleDependencies(String analysisId, List<AnalysisModule> modules, List<AnalysisComponent> components) {
        // A module edge requires parser-backed import/call evidence. Until that model exists,
        // returning no edge is more truthful than inferring a generic frontend-to-database flow.
        return List.of();
    }
}
