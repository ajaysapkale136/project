package com.aiproject.architect.modules.intelligence.service;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.SourceEvidence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BusinessRuleExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void extractIntegrityAndContractRules(String analysisId, List<AnalysisComponent> components,
                                                 List<BusinessRule> rules, List<SourceEvidence> evidences) {
        for (AnalysisComponent comp : components) {
            if ("ENTITY".equals(comp.getComponentType()) && comp.getDetailsJson() != null) {
                try {
                    JsonNode node = objectMapper.readTree(comp.getDetailsJson());
                    if (node.has("foreignKeyReferences")) {
                        for (JsonNode fk : node.get("foreignKeyReferences")) {
                            String targetTable = fk.asText();
                            String ruleCode = "REL-" + Math.abs((comp.getName() + targetTable).hashCode() % 10000);
                            BusinessRule rule = BusinessRule.builder()
                                    .analysisId(analysisId)
                                    .ruleCode(ruleCode)
                                    .title("Foreign Key Constraint: " + comp.getName() + " -> " + targetTable)
                                    .description("Table " + comp.getName() + " maintains referential integrity to " + targetTable)
                                    .category("DATA_INTEGRITY")
                                    .confidenceState(ConfidenceState.VERIFIED)
                                    .build();
                            rules.add(rule);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if ("CONTROLLER".equals(comp.getComponentType()) && comp.getDetailsJson() != null) {
                try {
                    JsonNode node = objectMapper.readTree(comp.getDetailsJson());
                    if (node.has("endpoints")) {
                        for (JsonNode ep : node.get("endpoints")) {
                            String method = ep.path("method").asText();
                            String path = ep.path("path").asText();
                            String ruleCode = "API-" + Math.abs((comp.getName() + method + path).hashCode() % 10000);
                            BusinessRule rule = BusinessRule.builder()
                                    .analysisId(analysisId)
                                    .ruleCode(ruleCode)
                                    .title("API Contract: " + method + " " + path)
                                    .description("Controller " + comp.getName() + " exposes public contract " + method + " " + path)
                                    .category("API_CONTRACT")
                                    .confidenceState(ConfidenceState.VERIFIED)
                                    .build();
                            rules.add(rule);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
