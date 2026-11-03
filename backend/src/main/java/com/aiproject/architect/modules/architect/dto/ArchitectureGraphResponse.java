package com.aiproject.architect.modules.architect.dto;

import com.aiproject.architect.modules.architect.entity.ArchitectureEdge;
import com.aiproject.architect.modules.architect.entity.ArchitectureNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectureGraphResponse {
    private String versionId;
    private String analysisId;
    private String versionType;
    private Integer revisionNumber;
    private String summary;
    private String antiPatternsJson;
    private String recommendationsJson;
    private List<ArchitectureNode> nodes;
    private List<ArchitectureEdge> edges;
}
