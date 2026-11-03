package com.aiproject.architect.modules.patch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackResponse {
    private String changeSetId;
    private String status;
    private List<String> revertedFiles;
    private String message;
}
