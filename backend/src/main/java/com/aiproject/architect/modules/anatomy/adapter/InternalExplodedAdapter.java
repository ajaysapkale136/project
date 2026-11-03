package com.aiproject.architect.modules.anatomy.adapter;

import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.AnalysisModule;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InternalExplodedAdapter {

    public AnatomySceneResponse buildExplodedScene(String projectId, AnalysisModule module, List<AnalysisComponent> components) {
        List<AnatomySceneResponse.IsometricAssembly> assemblies = new ArrayList<>();
        List<AnatomySceneResponse.AnimationStep> steps = new ArrayList<>();
        Map<String, List<AnalysisComponent>> byRole = new TreeMap<>();

        for (AnalysisComponent component : components) {
            byRole.computeIfAbsent(component.getComponentType(), ignored -> new ArrayList<>()).add(component);
        }

        int layerIndex = 0;
        for (Map.Entry<String, List<AnalysisComponent>> layer : byRole.entrySet()) {
            List<AnalysisComponent> layerComponents = layer.getValue();
            layerComponents.sort(Comparator.comparing(AnalysisComponent::getName));
            for (int index = 0; index < layerComponents.size(); index++) {
                AnalysisComponent component = layerComponents.get(index);
                String assemblyId = "component_" + component.getId();
                assemblies.add(AnatomySceneResponse.IsometricAssembly.builder()
                        .id(assemblyId)
                        .name(component.getName())
                        .role(component.getComponentType())
                        .material(materialFor(component.getComponentType()))
                        .x(80 + (index % 4) * 115)
                        .y(80 + (index / 4) * 85)
                        .z(25 + layerIndex * 60)
                        .width(92).depth(62).height(32)
                        .explodedOffsetZ(layerIndex * 55)
                        .componentId(component.getId())
                        .filePath(component.getFilePath())
                        .verificationState(component.getConfidenceState().name())
                        .metadata(Map.of(
                                "componentType", component.getComponentType(),
                                "filePath", component.getFilePath(),
                                "endpointCount", component.getEndpointCount()
                        ))
                        .build());
                steps.add(AnatomySceneResponse.AnimationStep.builder()
                        .stepIndex(steps.size() + 1)
                        .stepCode("COMPONENT-" + (steps.size() + 1))
                        .title(component.getName())
                        .description("Source-discovered " + component.getComponentType().toLowerCase(Locale.ROOT) + " in " + component.getFilePath())
                        .activeAssemblyId(assemblyId)
                        .durationMs(0)
                        .phase("INSPECTION")
                        .verificationState(component.getConfidenceState().name())
                        .build());
            }
            layerIndex++;
        }

        String readout = components.isEmpty()
                ? "No source components were discovered for this module."
                : "Source components: " + components.size() + " across " + byRole.size() + " discovered roles.";

        return AnatomySceneResponse.builder()
                .sceneId(UUID.randomUUID().toString())
                .projectId(projectId)
                .animationFormat("INTERNAL_EXPLODED")
                .targetIdentifier(module.getId())
                .title("Exploded internal assembly - " + module.getName())
                .subtitle("Each block is a source-discovered component; spatial tiers group components by detected role.")
                .readout(readout)
                .hint("Use the explode control to separate role tiers. Connections are omitted until call-graph evidence is available.")
                .assemblies(assemblies)
                .connections(List.of())
                .steps(steps)
                .build();
    }

    private String materialFor(String role) {
        return switch (role) {
            case "CONTROLLER", "UI_COMPONENT", "DATA_VIEW" -> "GLASS";
            case "SERVICE" -> "CHROME";
            case "REPOSITORY", "CONFIG" -> "STEEL";
            case "ENTITY", "DATABASE" -> "BRASS";
            default -> "MATTE_DARK";
        };
    }
}
