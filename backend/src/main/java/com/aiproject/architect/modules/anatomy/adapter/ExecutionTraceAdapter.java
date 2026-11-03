package com.aiproject.architect.modules.anatomy.adapter;

import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExecutionTraceAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnatomySceneResponse buildExecutionTraceScene(String projectId, String requestedOperation,
                                                          List<AnalysisComponent> components) {
        String operation = requestedOperation == null || requestedOperation.isBlank()
                ? "selected operation" : requestedOperation;
        Map<String, AnalysisComponent> componentsByName = new HashMap<>();
        components.forEach(component -> componentsByName.put(component.getName(), component));
        AnalysisComponent handler = selectHandler(operation, components);

        if (handler == null) return unavailableScene(projectId, operation);

        List<AnatomySceneResponse.IsometricAssembly> assemblies = new ArrayList<>();
        List<AnatomySceneResponse.VisualConnection> connections = new ArrayList<>();
        List<AnatomySceneResponse.AnimationStep> steps = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        AnalysisComponent current = handler;
        String previousAssemblyId = null;
        String previousCall = null;
        for (int depth = 0; depth < 5 && current != null && visited.add(current.getId()); depth++) {
            String assemblyId = "trace_" + current.getId();
            assemblies.add(assembly(current, assemblyId, depth));

            String connectionId = null;
            if (previousAssemblyId != null) {
                connectionId = "call_" + steps.size();
                connections.add(AnatomySceneResponse.VisualConnection.builder()
                        .id(connectionId)
                        .sourceAssemblyId(previousAssemblyId)
                        .targetAssemblyId(assemblyId)
                        .label(previousCall)
                        .connectionType("METHOD_CALL")
                        .style("PIPE")
                        .isAnimated(true)
                        .build());
            }

            steps.add(AnatomySceneResponse.AnimationStep.builder()
                    .stepIndex(steps.size() + 1)
                    .stepCode("STATIC-CALL-" + (steps.size() + 1))
                    .title(depth == 0 ? "Request handler" : "Source-resolved method dependency")
                    .description(depth == 0
                            ? "Static analysis selected " + current.getName() + " for " + operation + "."
                            : "Static AST evidence resolved the injected call target " + current.getName() + ".")
                    .activeAssemblyId(assemblyId)
                    .activeConnectionId(connectionId)
                    .durationMs(900)
                    .phase(depth == 0 ? "DISPATCH" : "METHOD_CALL")
                    .verificationState("VERIFIED")
                    .build());

            ResolvedCall next = resolveNextCall(current, componentsByName);
            previousAssemblyId = assemblyId;
            previousCall = next == null ? null : next.label();
            current = next == null ? null : next.target();
        }

        return AnatomySceneResponse.builder()
                .sceneId(UUID.randomUUID().toString())
                .projectId(projectId)
                .animationFormat("EXECUTION_TRACE")
                .targetIdentifier(operation)
                .title("Static execution flow - " + operation)
                .subtitle("Animated edges represent AST-discovered calls through injected source dependencies. Runtime telemetry is not shown.")
                .readout("Source-derived steps: " + steps.size() + " | Runtime observations: 0")
                .hint("Select a step to inspect its source component. Missing calls are intentionally omitted.")
                .assemblies(assemblies)
                .connections(connections)
                .steps(steps)
                .build();
    }

    private AnatomySceneResponse unavailableScene(String projectId, String operation) {
        return AnatomySceneResponse.builder()
                .sceneId(UUID.randomUUID().toString())
                .projectId(projectId)
                .animationFormat("EXECUTION_TRACE")
                .targetIdentifier(operation)
                .title("Execution flow unavailable")
                .subtitle("No source-discovered controller supports the requested operation.")
                .readout("Static execution evidence: unavailable")
                .hint("Select a discovered API operation after project analysis completes.")
                .assemblies(List.of()).connections(List.of()).steps(List.of()).build();
    }

    private AnatomySceneResponse.IsometricAssembly assembly(AnalysisComponent component, String id, int depth) {
        return AnatomySceneResponse.IsometricAssembly.builder()
                .id(id).name(component.getName()).role(component.getComponentType())
                .material(materialFor(component.getComponentType()))
                .x(80 + depth * 135).y(155).z(35).width(105).depth(72).height(42)
                .explodedOffsetZ(0).componentId(component.getId()).filePath(component.getFilePath())
                .verificationState(component.getConfidenceState().name())
                .metadata(Map.of("filePath", component.getFilePath(), "componentType", component.getComponentType()))
                .build();
    }

    private ResolvedCall resolveNextCall(AnalysisComponent source, Map<String, AnalysisComponent> componentsByName) {
        Map<String, Object> details = detailsOf(source);
        List<Map<String, String>> fields = listOfMaps(details.get("fields"));
        List<Map<String, String>> calls = listOfMaps(details.get("methodCalls"));
        for (Map<String, String> call : calls) {
            for (Map<String, String> field : fields) {
                if (!Objects.equals(call.get("scope"), field.get("name"))) continue;
                AnalysisComponent target = componentsByName.get(field.get("type"));
                if (target != null) return new ResolvedCall(target, call.get("scope") + "." + call.get("method") + "()");
            }
        }
        return null;
    }

    private AnalysisComponent selectHandler(String operation, List<AnalysisComponent> components) {
        String query = operation.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        List<AnalysisComponent> controllers = components.stream()
                .filter(component -> "CONTROLLER".equals(component.getComponentType()))
                .sorted(Comparator.comparing(AnalysisComponent::getName)).toList();
        return controllers.stream()
                .filter(controller -> query.contains(controller.getName().toLowerCase(Locale.ROOT).replace("controller", "")))
                .findFirst().orElse(controllers.size() == 1 ? controllers.get(0) : null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> listOfMaps(Object value) {
        if (value == null) return List.of();
        return objectMapper.convertValue(value, new TypeReference<List<Map<String, String>>>() {});
    }

    private Map<String, Object> detailsOf(AnalysisComponent component) {
        try {
            return objectMapper.readValue(component.getDetailsJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String materialFor(String role) {
        return switch (role) {
            case "CONTROLLER" -> "GLASS";
            case "SERVICE" -> "CHROME";
            case "REPOSITORY" -> "STEEL";
            case "ENTITY" -> "BRASS";
            default -> "MATTE_DARK";
        };
    }

    private record ResolvedCall(AnalysisComponent target, String label) {}
}
