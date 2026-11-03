package com.aiproject.architect.modules.ai.studio.dto;

import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
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
public class StudioContextResponse {
    private String projectId;
    private String projectName;
    private String repositoryUrl;
    private String branch;
    private String commitHash;
    private String primaryLanguage;
    private String frameworkFamily;
    private List<DetectedTechnology> technologies;
    private List<StudioModuleDto> modules;
    private List<StudioOperationDto> operations;
    private String defaultModuleId;
    private String defaultOperation;
    private Map<String, Object> statistics;
}
