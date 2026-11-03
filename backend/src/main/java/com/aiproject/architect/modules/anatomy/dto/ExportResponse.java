package com.aiproject.architect.modules.anatomy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {
    private String exportFormat;
    private String fileName;
    private String mimeType;
    private long sizeBytes;
    private String content; // Base64 or plain string for SVG/HTML
    private String downloadUrl;
}
