package com.aiproject.architect.modules.anatomy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnatomySceneResponse {
    private String sceneId;
    private String projectId;
    private String animationFormat; // MODULE_TO_MODULE, INTERNAL_EXPLODED, EXECUTION_TRACE
    private String targetIdentifier;
    private String title;
    private String subtitle;
    private String readout;
    private String hint;

    private List<IsometricAssembly> assemblies;
    private List<VisualConnection> connections;
    private List<AnimationStep> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IsometricAssembly {
        private String id;
        private String name;
        private String role; // BASE, FRAME, CARRIAGE, CONTROLLER, SERVICE, REPOSITORY, DATABASE
        private String material; // STEEL, BRASS, GLASS, CHROME, MATTE_DARK
        private double x;
        private double y;
        private double z;
        private double width;
        private double depth;
        private double height;
        private double explodedOffsetZ; // Separation distance when in Exploded View
        private String componentId;
        private String filePath;
        private String codeSnippet;
        private String verificationState; // VERIFIED, INFERRED
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualConnection {
        private String id;
        private String sourceAssemblyId;
        private String targetAssemblyId;
        private String label;
        private String connectionType; // REST_HTTP, METHOD_CALL, JPA_QUERY, ASYNC_EVENT
        private String style; // PIPE, WIRE, LIGHT_BEAM
        private boolean isAnimated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnimationStep {
        private int stepIndex;
        private String stepCode;
        private String title;
        private String description;
        private String activeAssemblyId;
        private String targetAssemblyId;
        private String activeConnectionId;
        private String dataPayload;
        private int durationMs;
        private String phase; // INGRESS, AUTH_CHECK, DISPATCH, BUSINESS_LOGIC, DATA_MUTATION, RESPONSE
        private String verificationState;
    }
}
