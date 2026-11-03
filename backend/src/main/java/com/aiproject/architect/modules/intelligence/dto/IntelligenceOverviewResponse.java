package com.aiproject.architect.modules.intelligence.dto;

import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceOverviewResponse {
    private String projectId;
    private String snapshotId;
    private String analysisId;
    private String status;
    private String primaryLanguage;
    private String frameworkFamily;
    private Integer totalFiles;
    private Long totalLinesOfCode;
    private Integer totalControllers;
    private Integer totalEndpoints;
    private Integer totalServices;
    private Integer totalRepositories;
    private Integer totalEntities;
    private String summary;
    private LocalDateTime completedAt;

    private List<DetectedTechnology> technologies;
    private List<ModuleSummary> modules;
    private List<BusinessRule> businessRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleSummary {
        private String id;
        private String name;
        private String moduleType;
        private String basePath;
        private Integer componentCount;
        private List<AnalysisComponent> components;
    }
}
