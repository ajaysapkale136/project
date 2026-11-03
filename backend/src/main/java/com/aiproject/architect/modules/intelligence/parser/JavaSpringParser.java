package com.aiproject.architect.modules.intelligence.parser;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import com.aiproject.architect.modules.intelligence.entity.SourceEvidence;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.UUID;

@Component
public class JavaSpringParser {

    private static final Logger log = LoggerFactory.getLogger(JavaSpringParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedJavaFile parse(Path filePath, String relativePath, String moduleId, String analysisId) {
        try {
            String content = Files.readString(filePath);
            CompilationUnit cu = StaticJavaParser.parse(content);

            List<AnalysisComponent> components = new ArrayList<>();
            List<BusinessRule> businessRules = new ArrayList<>();
            List<SourceEvidence> evidenceList = new ArrayList<>();

            String packageName = cu.getPackageDeclaration().map(p -> p.getName().asString()).orElse("");

            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                String className = type.getNameAsString();
                String componentType = determineComponentType(type);

                if (componentType == null) {
                    continue; // Skip utility / internal helper classes without architectural roles
                }

                Map<String, Object> details = new HashMap<>();
                details.put("className", className);
                details.put("package", packageName);
                details.put("isInterface", type.isInterface());

                int endpointCount = 0;
                List<Map<String, String>> endpoints = new ArrayList<>();
                List<Map<String, String>> fields = new ArrayList<>();
                List<String> annotations = new ArrayList<>();
                List<Map<String, String>> methodCalls = new ArrayList<>();

                for (AnnotationExpr ann : type.getAnnotations()) {
                    annotations.add(ann.getNameAsString());
                }
                details.put("annotations", annotations);

                // Analyze fields & relationships
                for (FieldDeclaration field : type.getFields()) {
                    for (VariableDeclarator var : field.getVariables()) {
                        Map<String, String> fInfo = new HashMap<>();
                        fInfo.put("name", var.getNameAsString());
                        fInfo.put("type", var.getTypeAsString());

                        for (AnnotationExpr ann : field.getAnnotations()) {
                            String annName = ann.getNameAsString();
                            if (annName.equals("Id") || annName.equals("OneToMany") ||
                                    annName.equals("ManyToOne") || annName.equals("ManyToMany") ||
                                    annName.equals("JoinColumn")) {
                                fInfo.put("relationship", annName);
                            }
                        }
                        fields.add(fInfo);
                    }
                }
                details.put("fields", fields);

                // Analyze methods, endpoints, security rules
                for (MethodDeclaration method : type.getMethods()) {
                    Map<String, String> ep = extractEndpoint(method);
                    if (ep != null) {
                        endpointCount++;
                        endpoints.add(ep);
                    }

                    // Extract business logic & security rules
                    extractBusinessRulesFromMethod(method, analysisId, relativePath, businessRules, evidenceList);

                    for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                        if (call.getScope().isEmpty()) continue;
                        Map<String, String> callInfo = new HashMap<>();
                        callInfo.put("ownerMethod", method.getNameAsString());
                        callInfo.put("scope", call.getScope().get().toString());
                        callInfo.put("method", call.getNameAsString());
                        callInfo.put("line", String.valueOf(call.getBegin().map(p -> p.line).orElse(1)));
                        methodCalls.add(callInfo);
                    }
                }
                details.put("endpoints", endpoints);
                details.put("methodCalls", methodCalls);

                int startLine = type.getBegin().map(p -> p.line).orElse(1);
                int endLine = type.getEnd().map(p -> p.line).orElse(1);

                AnalysisComponent component = AnalysisComponent.builder()
                        .moduleId(moduleId)
                        .name(className)
                        .componentType(componentType)
                        .filePath(relativePath)
                        .packageOrNamespace(packageName)
                        .endpointCount(endpointCount)
                        .confidenceState(ConfidenceState.VERIFIED)
                        .detailsJson(objectMapper.writeValueAsString(details))
                        .build();
                component.setId(UUID.randomUUID().toString());

                components.add(component);

                // Add component evidence
                String snippet = extractSnippet(content, startLine, Math.min(startLine + 15, endLine));
                SourceEvidence compEvidence = SourceEvidence.builder()
                        .componentId(component.getId())
                        .filePath(relativePath)
                        .startLine(startLine)
                        .endLine(endLine)
                        .codeSnippet(snippet)
                        .verificationState(ConfidenceState.VERIFIED)
                        .build();
                evidenceList.add(compEvidence);
            }

            return new ParsedJavaFile(components, businessRules, evidenceList);

        } catch (Exception e) {
            log.warn("Could not AST-parse Java file {}: {}", relativePath, e.getMessage());
            return new ParsedJavaFile(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private String determineComponentType(ClassOrInterfaceDeclaration type) {
        Set<String> annNames = new HashSet<>();
        type.getAnnotations().forEach(a -> annNames.add(a.getNameAsString()));

        if (annNames.contains("RestController") || annNames.contains("Controller")) {
            return "CONTROLLER";
        }
        if (annNames.contains("Service")) {
            return "SERVICE";
        }
        if (annNames.contains("Repository") || type.getExtendedTypes().stream().anyMatch(t -> t.getNameAsString().contains("Repository"))) {
            return "REPOSITORY";
        }
        if (annNames.contains("Entity") || annNames.contains("Table") || annNames.contains("Document")) {
            return "ENTITY";
        }
        if (annNames.contains("Configuration") || annNames.contains("EnableWebSecurity")) {
            return "CONFIG";
        }
        if (type.getNameAsString().endsWith("DTO") || type.getNameAsString().endsWith("Request") || type.getNameAsString().endsWith("Response")) {
            return "DTO";
        }
        return null;
    }

    private Map<String, String> extractEndpoint(MethodDeclaration method) {
        for (AnnotationExpr ann : method.getAnnotations()) {
            String name = ann.getNameAsString();
            String httpMethod = switch (name) {
                case "GetMapping" -> "GET";
                case "PostMapping" -> "POST";
                case "PutMapping" -> "PUT";
                case "DeleteMapping" -> "DELETE";
                case "PatchMapping" -> "PATCH";
                case "RequestMapping" -> "REQUEST";
                default -> null;
            };

            if (httpMethod != null) {
                Map<String, String> ep = new HashMap<>();
                ep.put("method", httpMethod);
                ep.put("handler", method.getNameAsString());
                ep.put("path", ann.toString());
                ep.put("returnType", method.getTypeAsString());
                return ep;
            }
        }
        return null;
    }

    private void extractBusinessRulesFromMethod(MethodDeclaration method, String analysisId, String relativePath,
                                                List<BusinessRule> rules, List<SourceEvidence> evidences) {
        int startLine = method.getBegin().map(p -> p.line).orElse(1);
        int endLine = method.getEnd().map(p -> p.line).orElse(startLine);

        for (AnnotationExpr ann : method.getAnnotations()) {
            String annName = ann.getNameAsString();

            if (annName.equals("PreAuthorize") || annName.equals("Secured") || annName.equals("RolesAllowed")) {
                String ruleCode = "AUTH-" + Math.abs((relativePath + method.getNameAsString()).hashCode() % 10000);
                BusinessRule rule = BusinessRule.builder()
                        .analysisId(analysisId)
                        .ruleCode(ruleCode)
                        .title("Authorization Requirement: " + method.getNameAsString())
                        .description("Endpoint method " + method.getNameAsString() + " requires security constraint: " + ann)
                        .category("AUTH_SECURITY")
                        .confidenceState(ConfidenceState.VERIFIED)
                        .build();
                rules.add(rule);

                SourceEvidence evidence = SourceEvidence.builder()
                        .filePath(relativePath)
                        .startLine(startLine)
                        .endLine(endLine)
                        .codeSnippet(ann + "\n" + method.getDeclarationAsString())
                        .verificationState(ConfidenceState.VERIFIED)
                        .build();
                evidences.add(evidence);
            }

            if (annName.equals("Transactional")) {
                String ruleCode = "TXN-" + Math.abs((relativePath + method.getNameAsString()).hashCode() % 10000);
                BusinessRule rule = BusinessRule.builder()
                        .analysisId(analysisId)
                        .ruleCode(ruleCode)
                        .title("Atomic Transaction Boundary: " + method.getNameAsString())
                        .description("Method " + method.getNameAsString() + " executes within an ACID transactional boundary.")
                        .category("TRANSACTIONAL")
                        .confidenceState(ConfidenceState.VERIFIED)
                        .build();
                rules.add(rule);
            }
        }
    }

    private String extractSnippet(String content, int startLine, int endLine) {
        String[] lines = content.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, startLine - 1);
        int end = Math.min(lines.length, endLine);
        for (int i = start; i < end; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().trim();
    }

    public record ParsedJavaFile(List<AnalysisComponent> components, List<BusinessRule> businessRules, List<SourceEvidence> evidenceList) {}
}
