package com.aiproject.architect.modules.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioEvidenceDto {
    private String id;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
    private String className;
    private String methodName;
    private String snippet;
    private String verificationState; // VERIFIED | STATIC_CODE_DERIVED | AI_INFERRED
    private String explanation;
}
