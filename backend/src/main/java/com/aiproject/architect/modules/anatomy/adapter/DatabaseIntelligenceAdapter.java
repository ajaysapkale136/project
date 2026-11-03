package com.aiproject.architect.modules.anatomy.adapter;

import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/** Converts parser-backed SQL table facts into a relationship scene. */
@Component
public class DatabaseIntelligenceAdapter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnatomySceneResponse build(String projectId, List<AnalysisComponent> components) {
        List<AnalysisComponent> tables = components.stream().filter(c -> "ENTITY".equals(c.getComponentType()))
                .filter(c -> details(c).containsKey("tableName")).toList();
        Map<String, AnatomySceneResponse.IsometricAssembly> byTable = new HashMap<>();
        List<AnatomySceneResponse.IsometricAssembly> assemblies = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            AnalysisComponent component = tables.get(i);
            Map<String, Object> details = details(component);
            String table = String.valueOf(details.get("tableName"));
            AnatomySceneResponse.IsometricAssembly assembly = AnatomySceneResponse.IsometricAssembly.builder()
                    .id("table-" + component.getId()).componentId(component.getId()).name(table).role("DATABASE")
                    .material("CHROME").x((i % 4) * 145).y((i / 4) * 125).z(0).width(108).depth(82).height(38)
                    .filePath(component.getFilePath()).verificationState(component.getConfidenceState().name()).metadata(details).build();
            assemblies.add(assembly);
            byTable.put(table.toLowerCase(Locale.ROOT), assembly);
        }
        List<AnatomySceneResponse.VisualConnection> connections = new ArrayList<>();
        for (AnalysisComponent component : tables) {
            AnatomySceneResponse.IsometricAssembly source = byTable.get(String.valueOf(details(component).get("tableName")).toLowerCase(Locale.ROOT));
            Object references = details(component).get("foreignKeyReferences");
            if (!(references instanceof Collection<?> refs) || source == null) continue;
            for (Object ref : refs) {
                AnatomySceneResponse.IsometricAssembly target = byTable.get(String.valueOf(ref).toLowerCase(Locale.ROOT));
                if (target != null) connections.add(AnatomySceneResponse.VisualConnection.builder().id("fk-" + source.getId() + "-" + target.getId())
                        .sourceAssemblyId(source.getId()).targetAssemblyId(target.getId()).label("FOREIGN KEY")
                        .connectionType("FOREIGN_KEY").style("WIRE").isAnimated(false).build());
            }
        }
        return AnatomySceneResponse.builder().sceneId("database-" + projectId).projectId(projectId).animationFormat("DATABASE_INTELLIGENCE")
                .targetIdentifier("database").title("Database Intelligence").subtitle("Tables and foreign-key links parsed from uploaded SQL source.")
                .readout("Verified tables: " + assemblies.size() + " | Verified foreign keys: " + connections.size())
                .hint(assemblies.isEmpty() ? "No SQL CREATE TABLE statements were parsed." : "Only parser-discovered foreign keys are displayed.")
                .assemblies(assemblies).connections(connections).steps(List.of()).build();
    }

    private Map<String, Object> details(AnalysisComponent component) {
        try { return objectMapper.readValue(component.getDetailsJson(), new TypeReference<>() {}); }
        catch (Exception ignored) { return Map.of(); }
    }
}
