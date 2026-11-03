package com.aiproject.architect.modules.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioActionCard {
    private String id;
    private String title;
    private String mode; // MODULE_ASSEMBLY | INTERNAL_EXPLODED | EXECUTION_TRACE | CODE_EVIDENCE
    private String targetId;
    private String description;
    private String icon;
}
