package com.aiproject.architect.modules.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioQueryResponse {
    private String answer;
    private String recommendedMode; // MODULE_ASSEMBLY | INTERNAL_EXPLODED | EXECUTION_TRACE
    private String targetModuleId;
    private String targetModuleName;
    private String targetOperation;
    private String highlightAssemblyId;
    private Integer stepIndex;
    private List<StudioActionCard> actionCards;
    private List<StudioEvidenceDto> sourceEvidence;
    private StudioTraceDto orchestrationTrace;
}
