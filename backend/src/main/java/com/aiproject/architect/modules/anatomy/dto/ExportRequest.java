package com.aiproject.architect.modules.anatomy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    private String exportFormat; // HTML, SVG, PDF, MP4
    private String animationFormat; // MODULE_TO_MODULE, INTERNAL_EXPLODED, EXECUTION_TRACE
    private String targetIdentifier;
    private boolean includeDocumentation;
}
