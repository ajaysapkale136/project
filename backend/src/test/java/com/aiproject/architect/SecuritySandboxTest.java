package com.aiproject.architect;

import com.aiproject.architect.modules.assistant.repository.GeneratedTestRepository;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.repository.AnalysisComponentRepository;
import com.aiproject.architect.modules.security.dto.SandboxRunRequest;
import com.aiproject.architect.modules.security.dto.SandboxRunResponse;
import com.aiproject.architect.modules.security.entity.ContractVerification;
import com.aiproject.architect.modules.security.entity.VulnerabilityFinding;
import com.aiproject.architect.modules.security.repository.ContractVerificationRepository;
import com.aiproject.architect.modules.security.service.ApiContractVerifier;
import com.aiproject.architect.modules.security.service.SandboxedExecutionCoordinator;
import com.aiproject.architect.modules.security.service.StaticSecurityScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecuritySandboxTest {

    private StaticSecurityScanner scanner;

    @Mock
    private AnalysisComponentRepository componentRepository;

    @Mock
    private ContractVerificationRepository contractRepository;

    @Mock
    private GeneratedTestRepository testRepository;

    private ApiContractVerifier contractVerifier;
    private SandboxedExecutionCoordinator sandboxCoordinator;

    @BeforeEach
    void setUp() {
        scanner = new StaticSecurityScanner();
        contractVerifier = new ApiContractVerifier(componentRepository, contractRepository);
        sandboxCoordinator = new SandboxedExecutionCoordinator(testRepository);
    }

    @Test
    void testScannerDetectsHardcodedAwsCredentials() {
        String codeWithAwsKey = "public class Config {\n" +
                "    private String awsKey = \"AKIAIOSFODNN7EXAMPLE\";\n" +
                "}\n";

        List<VulnerabilityFinding> findings = scanner.scanFile("scan-1", "Config.java", codeWithAwsKey);

        assertThat(findings).isNotEmpty();
        VulnerabilityFinding awsFinding = findings.stream()
                .filter(f -> f.getRuleId().equals("SEC-001"))
                .findFirst().orElseThrow();
        assertThat(awsFinding.getSeverity()).isEqualTo("CRITICAL");
        assertThat(awsFinding.getTitle()).contains("AWS Access Key");
        assertThat(awsFinding.getLineNumber()).isEqualTo(2);
    }

    @Test
    void testScannerDetectsSqlInjectionRisk() {
        String unsafeSql = "public List<User> findUsers(String userInput) {\n" +
                "    String sql = \"SELECT * FROM users WHERE name = '\" + userInput + \"'\";\n" +
                "    return jdbcTemplate.query(sql, mapper);\n" +
                "}\n";

        List<VulnerabilityFinding> findings = scanner.scanFile("scan-1", "UserDao.java", unsafeSql);

        assertThat(findings).isNotEmpty();
        VulnerabilityFinding sqlFinding = findings.stream()
                .filter(f -> f.getRuleId().equals("SEC-005"))
                .findFirst().orElseThrow();
        assertThat(sqlFinding.getSeverity()).isEqualTo("HIGH");
        assertThat(sqlFinding.getTitle()).contains("SQL Injection");
    }

    @Test
    void testScannerDetectsPathTraversalRisk() {
        String unsafePath = "public File getFile(String userPath) {\n" +
                "    return new File(\"/var/data/\" + userPath);\n" +
                "}\n";

        List<VulnerabilityFinding> findings = scanner.scanFile("scan-1", "FileController.java", unsafePath);

        assertThat(findings).isNotEmpty();
        VulnerabilityFinding pathFinding = findings.stream()
                .filter(f -> f.getRuleId().equals("SEC-006"))
                .findFirst().orElseThrow();
        assertThat(pathFinding.getSeverity()).isEqualTo("HIGH");
        assertThat(pathFinding.getTitle()).contains("Path Traversal");
    }

    @Test
    void testScannerCleanFileReportsNoVulnerabilities() {
        String cleanCode = "package com.example.service;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "@Service\n" +
                "public class CleanService {\n" +
                "    public String greet(String name) {\n" +
                "        return \"Hello, \" + name;\n" +
                "    }\n" +
                "}\n";

        List<VulnerabilityFinding> findings = scanner.scanFile("scan-1", "CleanService.java", cleanCode);
        assertThat(findings).isEmpty();
    }

    @Test
    void testApiContractVerification() {
        AnalysisComponent controllerComp = AnalysisComponent.builder()
                .moduleId("mod-1")
                .name("OrderController")
                .componentType("CONTROLLER")
                .filePath("src/main/java/OrderController.java")
                .build();
        controllerComp.setId("comp-order");

        when(componentRepository.findAllByComponentType("CONTROLLER")).thenReturn(List.of(controllerComp));
        when(contractRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ContractVerification> verifications = contractVerifier.verifyContracts("proj-1", "snap-1");

        assertThat(verifications).hasSize(2); // GET and POST
        assertThat(verifications.get(0).getEndpointPath()).isEqualTo("/api/v1/orders");
        assertThat(verifications.get(0).getComplianceStatus()).isEqualTo("COMPLIANT");
    }

    @Test
    void testSandboxedExecutionCoordinator() {
        when(testRepository.findByProjectIdOrderByCreatedAtDesc("proj-1")).thenReturn(List.of());

        SandboxRunRequest request = SandboxRunRequest.builder()
                .testSuite("UNIT")
                .timeoutSeconds(15)
                .build();

        SandboxRunResponse response = sandboxCoordinator.executeSandboxedValidation("proj-1", request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PASSED");
        assertThat(response.getTestsRun()).isGreaterThanOrEqualTo(1);
        assertThat(response.getConsoleOutput()).contains("AI Project Architect Sandboxed Test Coordinator");
    }
}
