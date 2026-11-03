package com.aiproject.architect.modules.security.dto;

import com.aiproject.architect.modules.security.entity.ContractVerification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractVerificationMatrixResponse {
    private int totalEndpoints;
    private int compliantCount;
    private int breakingChangesCount;
    private List<ContractVerification> verifications;
}
