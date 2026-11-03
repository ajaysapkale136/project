package com.aiproject.architect.modules.security.service;

import com.aiproject.architect.modules.security.entity.VulnerabilityFinding;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StaticSecurityScanner {

    private record RulePattern(
            String ruleId,
            String severity,
            String title,
            String description,
            Pattern pattern,
            String remediation
    ) {}

    private final List<RulePattern> rules = List.of(
            new RulePattern(
                    "SEC-001",
                    "CRITICAL",
                    "Hardcoded AWS Access Key",
                    "Found potential AWS Access Key ID exposed directly in source code.",
                    Pattern.compile("(?:AKIA|ABIA|ACCA|ASIA)[0-9A-Z]{16}"),
                    "Extract AWS credentials into environment variables or use IAM Roles/AWS Secrets Manager."
            ),
            new RulePattern(
                    "SEC-002",
                    "CRITICAL",
                    "Hardcoded Private Cryptographic Key",
                    "Found private cryptographic key header embedded in file.",
                    Pattern.compile("-----BEGIN (?:RSA|DSA|EC|OPENSSH)? ?PRIVATE KEY-----"),
                    "Store private keys in a secure key vault (HashiCorp Vault, AWS KMS, Azure Key Vault). Never commit keys."
            ),
            new RulePattern(
                    "SEC-003",
                    "CRITICAL",
                    "Hardcoded GitHub Personal Access Token",
                    "Found GitHub Personal Access Token pattern exposed in source.",
                    Pattern.compile("ghp_[0-9a-zA-Z]{36}|github_pat_[0-9a-zA-Z_]{82}"),
                    "Revoke token immediately and configure via GitHub Actions secrets or environment variables."
            ),
            new RulePattern(
                    "SEC-004",
                    "HIGH",
                    "Hardcoded Secret / Password",
                    "Potential hardcoded password or credential assigned to variable.",
                    Pattern.compile("(?i)(?:password|client_secret|db_pass|api_secret)\\s*[:=]\\s*[\"'][^\"'\\s]{6,}[\"']"),
                    "Use external configuration files (.env, application.yml) populated via secure environment variables."
            ),
            new RulePattern(
                    "SEC-005",
                    "HIGH",
                    "Potential SQL Injection via String Concatenation",
                    "Dynamic SQL query built via string concatenation without parameter binding.",
                    Pattern.compile("(?i)(?:SELECT|INSERT|UPDATE|DELETE)\\s+.*\\+\\s*[a-zA-Z0-9_]+"),
                    "Use parameterized queries, PreparedStatement, or Spring Data JPA parameter binding (:paramName)."
            ),
            new RulePattern(
                    "SEC-006",
                    "HIGH",
                    "Potential Path Traversal Vulnerability",
                    "File or Path constructed from unvalidated input without canonical path containment check.",
                    Pattern.compile("(?i)new\\s+File\\s*\\(.*(?:param|request|input|path).*\\)"),
                    "Validate and normalize target paths against an allowed base directory using Path.normalize() and startsWith()."
            ),
            new RulePattern(
                    "SEC-007",
                    "HIGH",
                    "Command Injection Risk",
                    "Runtime command execution with potential unvalidated parameter injection.",
                    Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec\\(|new\\s+ProcessBuilder\\("),
                    "Avoid invoking OS shell directly. If mandatory, validate inputs against strict alphanumeric whitelists."
            ),
            new RulePattern(
                    "SEC-008",
                    "MEDIUM",
                    "Overly Permissive CORS Configuration",
                    "CORS allows wildcard origin '*' while permitting credentials.",
                    Pattern.compile("(?i)addAllowedOrigin\\([\"']\\*[\"']\\)|allowCredentials\\(true\\)"),
                    "Explicitly define authorized domain origins rather than using wildcard '*'."
            ),
            new RulePattern(
                    "SEC-009",
                    "MEDIUM",
                    "Insecure Random Number Generator",
                    "Standard pseudo-random number generator used instead of cryptographically secure generator.",
                    Pattern.compile("new\\s+java\\.util\\.Random\\("),
                    "Use java.security.SecureRandom for cryptographic keys, tokens, salts, or passwords."
            )
    );

    public List<VulnerabilityFinding> scanFile(String scanId, String filePath, String content) {
        List<VulnerabilityFinding> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (RulePattern rule : rules) {
                    Matcher matcher = rule.pattern().matcher(line);
                    if (matcher.find()) {
                        String snippet = line.trim();
                        if (snippet.length() > 160) {
                            snippet = snippet.substring(0, 160) + "...";
                        }
                        findings.add(VulnerabilityFinding.builder()
                                .scanId(scanId)
                                .filePath(filePath)
                                .lineNumber(lineNumber)
                                .ruleId(rule.ruleId())
                                .severity(rule.severity())
                                .title(rule.title())
                                .description(rule.description())
                                .codeSnippet(snippet)
                                .remediationRecommendation(rule.remediation())
                                .build());
                    }
                }
            }
        } catch (IOException e) {
            // Should not occur on StringReader
        }

        return findings;
    }
}
