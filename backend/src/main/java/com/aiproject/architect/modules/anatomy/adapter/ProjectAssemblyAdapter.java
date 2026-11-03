package com.aiproject.architect.modules.anatomy.adapter;

import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.intelligence.entity.AnalysisModule;
import com.aiproject.architect.modules.intelligence.entity.ModuleDependency;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProjectAssemblyAdapter {

    public AnatomySceneResponse buildModuleScene(String projectId, List<AnalysisModule> modules, List<ModuleDependency> dependencies) {
        List<AnatomySceneResponse.IsometricAssembly> assemblies = new ArrayList<>();
        List<AnatomySceneResponse.VisualConnection> connections = new ArrayList<>();
        List<AnatomySceneResponse.AnimationStep> steps = new ArrayList<>();

        // Baseplate Rig
        assemblies.add(AnatomySceneResponse.IsometricAssembly.builder()
                .id("rig_baseplate")
                .name("System Substrate Bed")
                .role("BASE")
                .material("MATTE_DARK")
                .x(0).y(0).z(0)
                .width(600).depth(400).height(20)
                .explodedOffsetZ(0)
                .verificationState("INFERRED")
                .metadata(Map.of("description", "Visual-only substrate for arranging source-discovered modules"))
                .build());

        // Place modules across the bed in an isometric grid
        double startX = 60;
        double startY = 80;
        double gapX = 160;

        for (int i = 0; i < modules.size(); i++) {
            AnalysisModule m = modules.get(i);
            String asmId = "asm_mod_" + m.getId();
            double x = startX + (i * gapX);
            double y = startY + ((i % 2) * 50);

            String material = switch (m.getModuleType()) {
                case "FRONTEND_SPA" -> "GLASS";
                case "BACKEND_SERVICE" -> "CHROME";
                case "DATABASE_LAYER" -> "BRASS";
                default -> "STEEL";
            };

            assemblies.add(AnatomySceneResponse.IsometricAssembly.builder()
                    .id(asmId)
                    .name(m.getName())
                    .role(m.getModuleType())
                    .material(material)
                    .x(x).y(y).z(20)
                    .width(120).depth(90).height(50)
                    .explodedOffsetZ(60 * (i + 1))
                    .componentId(m.getId())
                    .filePath(m.getBasePath())
                    .verificationState("VERIFIED")
                    .metadata(Map.of(
                            "componentCount", m.getComponentCount(),
                            "moduleType", m.getModuleType(),
                            "basePath", m.getBasePath()
                    ))
                    .build());
        }

        // Add verified connections
        for (int i = 0; i < dependencies.size(); i++) {
            ModuleDependency dep = dependencies.get(i);
            String connId = "conn_dep_" + dep.getId();

            connections.add(AnatomySceneResponse.VisualConnection.builder()
                    .id(connId)
                    .sourceAssemblyId("asm_mod_" + dep.getSourceModuleId())
                    .targetAssemblyId("asm_mod_" + dep.getTargetModuleId())
                    .label(dep.getDependencyType())
                    .connectionType(dep.getDependencyType())
                    .style("PIPE")
                    .isAnimated(true)
                    .build());

            steps.add(AnatomySceneResponse.AnimationStep.builder()
                    .stepIndex(i + 1)
                    .stepCode("DEP-" + (i + 1))
                    .title("Inter-Module Traffic: " + dep.getDependencyType())
                    .description("Parser-backed dependency routed between source-discovered modules.")
                    .activeAssemblyId("asm_mod_" + dep.getSourceModuleId())
                    .targetAssemblyId("asm_mod_" + dep.getTargetModuleId())
                    .activeConnectionId(connId)
                    .durationMs(0)
                    .phase("DISPATCH")
                    .verificationState(dep.getConfidenceState().name())
                    .build());
        }

        if (steps.isEmpty()) {
            steps.add(AnatomySceneResponse.AnimationStep.builder()
                    .stepIndex(1)
                    .stepCode("IDLE-1")
                    .title("No parser-backed module dependencies discovered")
                    .description("The assembly contains source-discovered modules. Dependency animation stays unavailable until import/call evidence is modeled.")
                    .activeAssemblyId(assemblies.get(0).getId())
                    .durationMs(0)
                    .phase("INSPECTION")
                    .verificationState("UNVERIFIED")
                    .build());
        }

        return AnatomySceneResponse.builder()
                .sceneId(UUID.randomUUID().toString())
                .projectId(projectId)
                .animationFormat("MODULE_TO_MODULE")
                .targetIdentifier("ALL_MODULES")
                .title("Format 1: System-Level Module Architecture")
                .subtitle("Isometric physical machine model representing detected modules, conduits, and dependency lines.")
                .readout(String.format("Verified Modules: %d • Active Conduits: %d", modules.size(), connections.size()))
                .hint("Click any module chassis to inspect internal components. Play controls animate inter-module data flow.")
                .assemblies(assemblies)
                .connections(connections)
                .steps(steps)
                .build();
    }
}
