package com.aiproject.architect;

import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import com.aiproject.architect.modules.patch.service.WorktreeService;
import com.aiproject.architect.modules.testing.dto.RunTestRequest;
import com.aiproject.architect.modules.testing.dto.TestExecutionReportResponse;
import com.aiproject.architect.modules.testing.entity.TestExecution;
import com.aiproject.architect.modules.testing.repository.TestExecutionRepository;
import com.aiproject.architect.modules.testing.service.SandboxedTestingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SandboxedTestingEngineTest {

    @Mock
    private WorktreeService worktreeService;

    @Mock
    private TestExecutionRepository executionRepository;

    private SandboxedTestingEngine testingEngine;

    @TempDir
    Path tempWorktree;

    @BeforeEach
    void setUp() {
        testingEngine = new SandboxedTestingEngine(worktreeService, executionRepository);
    }

    @Test
    void testDetectsMavenStackAndExecutesTests() throws IOException {
        String projectId = "proj-test-mvn";
        // Create pom.xml and sample test file
        Files.writeString(tempWorktree.resolve("pom.xml"), "<project></project>");
        Path testDir = tempWorktree.resolve("src/test/java");
        Files.createDirectories(testDir);
        Files.writeString(testDir.resolve("SampleServiceTest.java"),
                "class SampleServiceTest { @Test void testOne() {} }");

        IsolatedWorkspace workspace = IsolatedWorkspace.builder()
                .projectId(projectId)
                .workspacePath(tempWorktree.toString())
                .status("ACTIVE")
                .build();
        workspace.setId("ws-test-mvn");

        when(worktreeService.getOrCreateWorkspace(projectId)).thenReturn(workspace);
        when(executionRepository.save(any(TestExecution.class))).thenAnswer(i -> {
            TestExecution e = i.getArgument(0);
            e.setId("exec-1");
            return e;
        });

        RunTestRequest request = RunTestRequest.builder()
                .testType("ALL")
                .timeoutSeconds(30)
                .build();

        TestExecutionReportResponse response = testingEngine.runTests(projectId, request);

        assertThat(response).isNotNull();
        assertThat(response.getExecution().getStatus()).isEqualTo("PASSED");
        assertThat(response.getExecution().getCommandExecuted()).contains("mvn test");
        assertThat(response.getEnvironment()).contains("Java 21 / Maven 3.9");
        assertThat(response.getExecution().getPassedCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testDetectsNodeStackWhenPackageJsonPresent() throws IOException {
        String projectId = "proj-test-node";
        Files.writeString(tempWorktree.resolve("package.json"), "{}");
        Files.writeString(tempWorktree.resolve("app.test.ts"), "it('should test', () => {})");

        IsolatedWorkspace workspace = IsolatedWorkspace.builder()
                .projectId(projectId)
                .workspacePath(tempWorktree.toString())
                .status("ACTIVE")
                .build();
        workspace.setId("ws-test-node");

        when(worktreeService.getOrCreateWorkspace(projectId)).thenReturn(workspace);
        when(executionRepository.save(any(TestExecution.class))).thenAnswer(i -> i.getArgument(0));

        TestExecutionReportResponse response = testingEngine.runTests(projectId, new RunTestRequest());

        assertThat(response.getExecution().getCommandExecuted()).contains("npm test");
        assertThat(response.getEnvironment()).contains("Node.js 20 / Jest");
        assertThat(response.getExecution().getPassedCount()).isGreaterThanOrEqualTo(1);
    }
}
