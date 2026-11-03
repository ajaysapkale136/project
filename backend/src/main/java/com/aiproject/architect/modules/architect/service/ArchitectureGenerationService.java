package com.aiproject.architect.modules.architect.service;

import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.architect.dto.ArchitectureDiffResponse;
import com.aiproject.architect.modules.architect.dto.ArchitectureGraphResponse;
import com.aiproject.architect.modules.architect.dto.ModuleBlueprintResponse;
import com.aiproject.architect.modules.architect.entity.ArchitectureEdge;
import com.aiproject.architect.modules.architect.entity.ArchitectureNode;
import com.aiproject.architect.modules.architect.entity.ArchitectureVersion;
import com.aiproject.architect.modules.architect.repository.ArchitectureEdgeRepository;
import com.aiproject.architect.modules.architect.repository.ArchitectureNodeRepository;
import com.aiproject.architect.modules.architect.repository.ArchitectureVersionRepository;
import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.AnalysisModule;
import com.aiproject.architect.modules.intelligence.entity.ProjectAnalysis;
import com.aiproject.architect.modules.intelligence.repository.AnalysisComponentRepository;
import com.aiproject.architect.modules.intelligence.repository.AnalysisModuleRepository;
import com.aiproject.architect.modules.intelligence.repository.ProjectAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ArchitectureGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureGenerationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SourceSnapshotRepository snapshotRepository;
    private final ProjectAnalysisRepository analysisRepository;
    private final AnalysisModuleRepository moduleRepository;
    private final AnalysisComponentRepository componentRepository;
    private final ArchitectureVersionRepository versionRepository;
    private final ArchitectureNodeRepository nodeRepository;
    private final ArchitectureEdgeRepository edgeRepository;

    @Transactional
    public ArchitectureGraphResponse generateOrGetExistingArchitecture(String projectId) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        Optional<ArchitectureVersion> existing = versionRepository
                .findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(analysis.getId(), "EXISTING");

        if (existing.isPresent()) {
            return buildGraphResponse(existing.get());
        }

        return generateExistingArchitecture(analysis);
    }

    @Transactional
    public ArchitectureGraphResponse generateOrGetProposedArchitecture(String projectId) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        Optional<ArchitectureVersion> proposed = versionRepository
                .findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(analysis.getId(), "PROPOSED");

        if (proposed.isPresent()) {
            return buildGraphResponse(proposed.get());
        }

        // Generate existing first if not present
        generateOrGetExistingArchitecture(projectId);
        return generateProposedArchitecture(analysis);
    }

    @Transactional
    public ArchitectureGraphResponse generateExistingArchitecture(ProjectAnalysis analysis) {
        List<AnalysisModule> modules = moduleRepository.findAllByAnalysisId(analysis.getId());
        List<AnalysisComponent> controllers = componentRepository.findAllByComponentType("CONTROLLER");
        List<AnalysisComponent> services = componentRepository.findAllByComponentType("SERVICE");
        List<AnalysisComponent> repositories = componentRepository.findAllByComponentType("REPOSITORY");
        List<AnalysisComponent> entities = componentRepository.findAllByComponentType("ENTITY");

        // Identify Architectural Anti-Patterns & Deficiencies
        List<Map<String, String>> antiPatterns = new ArrayList<>();
        List<Map<String, String>> recommendations = new ArrayList<>();

        if (controllers.size() > services.size()) {
            antiPatterns.add(Map.of(
                    "type", "ANEMIC_SERVICE_LAYER",
                    "severity", "HIGH",
                    "description", "Controllers detected without corresponding business service delegation, risking business logic leakage into HTTP handlers."
            ));
            recommendations.add(Map.of(
                    "action", "INTRODUCE_DOMAIN_SERVICES",
                    "target", "Business Logic Layer",
                    "benefit", "Isolates transaction boundaries and business validation from HTTP transport."
            ));
        }

        if (repositories.isEmpty() && !entities.isEmpty()) {
            antiPatterns.add(Map.of(
                    "type", "MISSING_REPOSITORY_ABSTRACTION",
                    "severity", "MEDIUM",
                    "description", "Database entities lack typed Repository/DAO interfaces, leading to ad-hoc SQL coupling."
            ));
        }

        antiPatterns.add(Map.of(
                "type", "MONOLITHIC_TIGHT_COUPLING",
                "severity", "MEDIUM",
                "description", "Direct synchronous method invocation between cross-domain boundaries without decoupled event contracts."
        ));

        recommendations.add(Map.of(
                "action", "MODULAR_DOMAIN_BOUNDARIES",
                "target", "Modular Monolith Structure",
                "benefit", "Preserves existing functionality while introducing explicit module interfaces and DTO boundaries."
        ));

        String summary = String.format("Current Architecture Analysis: Monolithic topology with %d controllers, %d services, %d repositories, and %d persistence entities.",
                controllers.size(), services.size(), repositories.size(), entities.size());

        String antiPatternsJson = "";
        String recsJson = "";
        try {
            antiPatternsJson = objectMapper.writeValueAsString(antiPatterns);
            recsJson = objectMapper.writeValueAsString(recommendations);
        } catch (Exception ignored) {}

        ArchitectureVersion version = ArchitectureVersion.builder()
                .analysisId(analysis.getId())
                .versionType("EXISTING")
                .revisionNumber(1)
                .summary(summary)
                .antiPatternsJson(antiPatternsJson)
                .recommendationsJson(recsJson)
                .build();
        version = versionRepository.save(version);

        List<ArchitectureNode> nodes = new ArrayList<>();
        List<ArchitectureEdge> edges = new ArrayList<>();

        // 1. Client / Consumer Node
        nodes.add(createNode(version.getId(), "node_client", "Web Client (SPA / Browser)", "CLIENT", "FRONTEND", 250, 50,
                Map.of("role", "UI Presentation Layer", "protocol", "HTTPS / JSON")));

        // 2. Security Gateway Node
        nodes.add(createNode(version.getId(), "node_gateway", "Security & API Gateway", "GATEWAY", "INFRASTRUCTURE", 250, 160,
                Map.of("role", "Authentication & CORS", "filter", "Stateless Token Filter")));
        edges.add(createEdge(version.getId(), "node_client", "node_gateway", "HTTPS REST", "REST_HTTP"));

        // 3. Controllers Node
        String ctrlLabel = "REST Controllers (" + controllers.size() + " endpoints)";
        nodes.add(createNode(version.getId(), "node_controllers", ctrlLabel, "CONTROLLER", "BACKEND", 250, 270,
                Map.of("count", controllers.size())));
        edges.add(createEdge(version.getId(), "node_gateway", "node_controllers", "Dispatch Request", "DISPATCH"));

        // 4. Services Node
        String srvLabel = "Domain Services (" + services.size() + " services)";
        nodes.add(createNode(version.getId(), "node_services", srvLabel, "SERVICE", "BACKEND", 250, 380,
                Map.of("count", services.size(), "transactional", true)));
        edges.add(createEdge(version.getId(), "node_controllers", "node_services", "Business Invocation", "METHOD_CALL"));

        // 5. Repositories Node
        String repoLabel = "Data Repositories (" + repositories.size() + " DAOs)";
        nodes.add(createNode(version.getId(), "node_repositories", repoLabel, "REPOSITORY", "PERSISTENCE", 250, 490,
                Map.of("count", repositories.size())));
        edges.add(createEdge(version.getId(), "node_services", "node_repositories", "Data Access", "METHOD_CALL"));

        // 6. Database Node
        String dbLabel = "Relational Database (" + entities.size() + " tables)";
        nodes.add(createNode(version.getId(), "node_database", dbLabel, "DATABASE", "PERSISTENCE", 250, 600,
                Map.of("entities", entities.size(), "engine", "PostgreSQL / H2")));
        edges.add(createEdge(version.getId(), "node_repositories", "node_database", "SQL / JPA Queries", "JPA_QUERY"));

        nodeRepository.saveAll(nodes);
        edgeRepository.saveAll(edges);

        return buildGraphResponse(version);
    }

    @Transactional
    public ArchitectureGraphResponse generateProposedArchitecture(ProjectAnalysis analysis) {
        String summary = "Proposed Modular Architecture: Clean Architecture with Domain-Driven Modular Monolith structure. Introduces dedicated Domain Events, Application Facades, Isolated Data Access, and Caching without rewriting original business logic.";

        List<Map<String, String>> enhancements = List.of(
                Map.of("enhancement", "Domain Event Bus", "reason", "Decouples write workflows from secondary notifications and audit logging."),
                Map.of("enhancement", "Multi-Layer Caching (Redis/In-Memory)", "reason", "Accelerates hot queries and reduces database I/O."),
                Map.of("enhancement", "Modular Security Interceptor", "reason", "Centralizes RBAC authorization and audit logging.")
        );

        String enhancementsJson = "";
        try {
            enhancementsJson = objectMapper.writeValueAsString(enhancements);
        } catch (Exception ignored) {}

        ArchitectureVersion version = ArchitectureVersion.builder()
                .analysisId(analysis.getId())
                .versionType("PROPOSED")
                .revisionNumber(2)
                .summary(summary)
                .recommendationsJson(enhancementsJson)
                .antiPatternsJson("[]")
                .build();
        version = versionRepository.save(version);

        List<ArchitectureNode> nodes = new ArrayList<>();
        List<ArchitectureEdge> edges = new ArrayList<>();

        // Layer 1: Client & Presentation
        nodes.add(createNode(version.getId(), "prop_client", "Modern Frontend (React + TanStack)", "CLIENT", "FRONTEND", 200, 50,
                Map.of("tech", "React 18 / TypeScript", "state", "TanStack Query")));

        // Layer 2: API Gateway & Security Interceptor
        nodes.add(createNode(version.getId(), "prop_gateway", "API Gateway & Security Guard", "GATEWAY", "INFRASTRUCTURE", 200, 160,
                Map.of("features", "Stateless JWT, CORS, Rate Limiting, Audit Interceptor")));
        edges.add(createEdge(version.getId(), "prop_client", "prop_gateway", "HTTPS / WSS", "REST_HTTP"));

        // Layer 3: Application Service Modules (Parallel Domain Subsystems)
        nodes.add(createNode(version.getId(), "prop_core_domain", "Core Business Module", "SERVICE", "BACKEND", 100, 280,
                Map.of("responsibility", "Preserved Original Core Domain Logic")));
        nodes.add(createNode(version.getId(), "prop_auth_module", "Identity & Access Module", "SERVICE", "BACKEND", 350, 280,
                Map.of("responsibility", "User Authentication, RBAC, Token Governance")));

        edges.add(createEdge(version.getId(), "prop_gateway", "prop_core_domain", "Domain Requests", "DISPATCH"));
        edges.add(createEdge(version.getId(), "prop_gateway", "prop_auth_module", "Auth & Token Checks", "DISPATCH"));

        // Layer 4: Event Bus & Cache Layer
        nodes.add(createNode(version.getId(), "prop_event_bus", "Domain Event Pipeline", "QUEUE", "INFRASTRUCTURE", 100, 400,
                Map.of("pattern", "Transactional Outbox / Spring Events")));
        nodes.add(createNode(version.getId(), "prop_cache", "Distributed Cache (Redis)", "CACHE", "INFRASTRUCTURE", 350, 400,
                Map.of("pattern", "Cache-Aside, Hot Query Cache")));

        edges.add(createEdge(version.getId(), "prop_core_domain", "prop_event_bus", "Publish Events", "ASYNC_EVENT"));
        edges.add(createEdge(version.getId(), "prop_core_domain", "prop_cache", "Cache Check", "CACHE_QUERY"));

        // Layer 5: Data Access & Repositories
        nodes.add(createNode(version.getId(), "prop_repository", "Hardened Data Layer (JPA + Flyway)", "REPOSITORY", "PERSISTENCE", 220, 520,
                Map.of("features", "Optimistic Locking, Flyway Migrations, Audit Fields")));
        edges.add(createEdge(version.getId(), "prop_core_domain", "prop_repository", "Validated CRUD", "METHOD_CALL"));

        // Layer 6: PostgreSQL Database
        nodes.add(createNode(version.getId(), "prop_database", "PostgreSQL Primary Store", "DATABASE", "PERSISTENCE", 220, 640,
                Map.of("engine", "PostgreSQL 16+", "schema", "Normalized with Foreign Keys")));
        edges.add(createEdge(version.getId(), "prop_repository", "prop_database", "ACID Transactions", "JPA_QUERY"));

        nodeRepository.saveAll(nodes);
        edgeRepository.saveAll(edges);

        return buildGraphResponse(version);
    }

    public ArchitectureDiffResponse compareArchitectures(String projectId) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);
        ArchitectureVersion existing = versionRepository
                .findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(analysis.getId(), "EXISTING")
                .orElseGet(() -> {
                    generateExistingArchitecture(analysis);
                    return versionRepository.findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(analysis.getId(), "EXISTING").orElseThrow();
                });

        ArchitectureVersion proposed = versionRepository
                .findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(analysis.getId(), "PROPOSED")
                .orElseGet(() -> {
                    generateProposedArchitecture(analysis);
                    return versionRepository.findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(analysis.getId(), "PROPOSED").orElseThrow();
                });

        List<ArchitectureNode> existingNodes = nodeRepository.findAllByVersionId(existing.getId());
        List<ArchitectureNode> proposedNodes = nodeRepository.findAllByVersionId(proposed.getId());

        List<String> addedCapabilities = List.of(
                "Asynchronous Domain Event Bus for decoupled notifications and audit tracking",
                "Distributed Query Caching layer with Cache-Aside semantics",
                "Explicit Boundary Segregation between Presentation DTOs and Persistence Entities",
                "Anti-ZIP-Slip & Memory-safe Ingestion Sandbox guards"
        );

        List<String> resolvedAntiPatterns = List.of(
                "Eliminates tight coupling by establishing modular service contracts",
                "Fixes controller-to-database leak by strictly enforcing the Service-Repository boundary",
                "Guarantees preservation of all existing REST endpoint paths and schemas"
        );

        return ArchitectureDiffResponse.builder()
                .projectId(projectId)
                .existingVersionId(existing.getId())
                .proposedVersionId(proposed.getId())
                .existingNodeCount(existingNodes.size())
                .proposedNodeCount(proposedNodes.size())
                .addedCapabilities(addedCapabilities)
                .resolvedAntiPatterns(resolvedAntiPatterns)
                .riskLevel("LOW - Zero destructive schema modifications. Additive modular architecture only.")
                .rollbackStrategy("Revert to baseline Git commit; Flyway V1 schema remains fully forward and backward compatible.")
                .build();
    }

    public ModuleBlueprintResponse generateModuleBlueprint(String projectId, String moduleName) {
        ProjectAnalysis analysis = getLatestAnalysis(projectId);

        String targetName = (moduleName != null && !moduleName.trim().isEmpty()) ? moduleName.trim() : "Analytics and Audit Module";

        String antigravityPrompt = String.format("""
                # ANTIGRAVITY IMPLEMENTATION PROMPT
                # MODULE: %s
                # TARGET WORKSPACE: d:\\project
                
                You are implementing module '%s' for AI Project Architect.
                
                REQUIREMENTS:
                1. Follow Clean Architecture and Spring Boot 3.3 standards.
                2. Preserve all existing business rules and public REST API contracts.
                3. Implement DTO validation, service business rules, repository interfaces, and unit tests.
                4. Maintain strict four-state confidence provenance (VERIFIED).
                
                STEPS:
                1. Create package `com.aiproject.architect.modules.%s`.
                2. Implement Entity, Repository, Service, and Controller.
                3. Add integration test verifying end-to-end functionality.
                4. Never modify existing database migrations destructively.
                """, targetName, targetName, targetName.toLowerCase().replace(" ", ""));

        return ModuleBlueprintResponse.builder()
                .moduleName(targetName)
                .purpose("Provides isolated domain capability with zero breaking changes to existing APIs.")
                .businessRequirements(List.of(
                        "Must maintain 100% backward compatibility with existing client applications",
                        "Must audit all state mutations with user identity and timestamp",
                        "Must enforce role-based access control on write operations"
                ))
                .functionalRequirements(List.of(
                        "REST API endpoints with Jakarta validation",
                        "Transactional service layer with rollback on unchecked exceptions",
                        "Spring Data JPA repository with indexed lookup fields"
                ))
                .nonFunctionalRequirements(List.of(
                        "API response latency < 100ms under standard load",
                        "Zero memory leaks via stateless design",
                        "Thread-safe concurrent execution"
                ))
                .folderStructure(List.of(
                        "src/main/java/com/aiproject/architect/modules/" + targetName.toLowerCase().replace(" ", "") + "/controller",
                        "src/main/java/com/aiproject/architect/modules/" + targetName.toLowerCase().replace(" ", "") + "/service",
                        "src/main/java/com/aiproject/architect/modules/" + targetName.toLowerCase().replace(" ", "") + "/repository",
                        "src/main/java/com/aiproject/architect/modules/" + targetName.toLowerCase().replace(" ", "") + "/entity",
                        "src/main/java/com/aiproject/architect/modules/" + targetName.toLowerCase().replace(" ", "") + "/dto"
                ))
                .testCases(List.of(
                        "Unit test for service validation rules",
                        "MockMvc controller test for HTTP status codes and error responses",
                        "DataJpaTest for repository queries"
                ))
                .acceptanceCriteria(List.of(
                        "All unit and integration tests compile and pass",
                        "No regression in existing endpoints",
                        "Swagger / OpenAPI documentation reflects new module endpoints"
                ))
                .implementationSequence(List.of(
                        "Step 1: Entity & Database Schema migration",
                        "Step 2: Repository and queries",
                        "Step 3: Service business logic & validation",
                        "Step 4: REST Controller & DTOs",
                        "Step 5: Automated unit & integration tests"
                ))
                .antigravityPrompt(antigravityPrompt)
                .build();
    }

    private ProjectAnalysis getLatestAnalysis(String projectId) {
        SourceSnapshot snapshot = snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("SourceSnapshot", projectId));
        return analysisRepository.findTopBySnapshotIdOrderByCreatedAtDesc(snapshot.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ProjectAnalysis for project", projectId));
    }

    private ArchitectureGraphResponse buildGraphResponse(ArchitectureVersion version) {
        List<ArchitectureNode> nodes = nodeRepository.findAllByVersionId(version.getId());
        List<ArchitectureEdge> edges = edgeRepository.findAllByVersionId(version.getId());

        return ArchitectureGraphResponse.builder()
                .versionId(version.getId())
                .analysisId(version.getAnalysisId())
                .versionType(version.getVersionType())
                .revisionNumber(version.getRevisionNumber())
                .summary(version.getSummary())
                .antiPatternsJson(version.getAntiPatternsJson())
                .recommendationsJson(version.getRecommendationsJson())
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    private ArchitectureNode createNode(String versionId, String key, String label, String type, String category, int x, int y, Map<String, Object> meta) {
        String metaJson = "";
        try {
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (Exception ignored) {}

        return ArchitectureNode.builder()
                .versionId(versionId)
                .nodeKey(key)
                .label(label)
                .nodeType(type)
                .category(category)
                .posX(x)
                .posY(y)
                .metadataJson(metaJson)
                .build();
    }

    private ArchitectureEdge createEdge(String versionId, String source, String target, String label, String type) {
        return ArchitectureEdge.builder()
                .versionId(versionId)
                .sourceNodeKey(source)
                .targetNodeKey(target)
                .label(label)
                .edgeType(type)
                .build();
    }
}
