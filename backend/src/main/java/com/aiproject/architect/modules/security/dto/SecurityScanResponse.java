package com.aiproject.architect.modules.security.dto;

import com.aiproject.architect.modules.security.entity.SecurityScan;
import com.aiproject.architect.modules.security.entity.VulnerabilityFinding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityScanResponse {
    private SecurityScan scan;
    private List<VulnerabilityFinding> findings;
}
