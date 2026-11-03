package com.aiproject.architect.modules.assistant.dto;

import com.aiproject.architect.modules.assistant.entity.CodeChange;
import com.aiproject.architect.modules.assistant.entity.CodeProposal;
import com.aiproject.architect.modules.assistant.entity.GeneratedTest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalDetailResponse {
    private CodeProposal proposal;
    private List<CodeChange> changes;
    private List<GeneratedTest> tests;
}
