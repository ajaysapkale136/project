package com.aiproject.architect.modules.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioOperationDto {
    private String id;
    private String name;
    private String httpMethod;
    private String path;
    private String moduleName;
    private String controllerClass;
    private String description;
}
