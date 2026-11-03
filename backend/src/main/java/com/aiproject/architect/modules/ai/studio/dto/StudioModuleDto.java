package com.aiproject.architect.modules.ai.studio.dto;

import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioModuleDto {
    private String id;
    private String name;
    private String moduleType;
    private String basePath;
    private String description;
    private int componentCount;
    private List<AnalysisComponent> components;
}
