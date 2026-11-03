package com.aiproject.architect;

import com.aiproject.architect.modules.architect.dto.ArchitectureDiffResponse;
import com.aiproject.architect.modules.architect.dto.ArchitectureGraphResponse;
import com.aiproject.architect.modules.architect.dto.ModuleBlueprintResponse;
import com.aiproject.architect.modules.ingestion.dto.IngestionResponse;
import com.aiproject.architect.modules.intelligence.dto.IntelligenceOverviewResponse;
import com.aiproject.architect.modules.project.dto.ProjectCreateRequest;
import com.aiproject.architect.modules.project.dto.ProjectResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class AiProjectArchitectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Pipeline: Ingest ZIP -> Analyze Intelligence -> Synthesize Architecture -> Generate Blueprint")
    void testEndToEndPipeline() throws Exception {
        // 1. Create Project
        ProjectCreateRequest createReq = ProjectCreateRequest.builder()
                .name("E-Commerce Microservice")
                .description("Sample store backend with orders, catalog, and checkout")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertTrue(createNode.get("success").asBoolean());
        String projectId = createNode.get("data").get("id").asText();
        assertNotNull(projectId);

        // 2. Prepare Sample In-Memory ZIP with real Java Spring Boot & SQL files
        byte[] zipBytes = createSampleProjectZip();
        MockMultipartFile file = new MockMultipartFile(
                "file", "ecommerce-service.zip", "application/zip", zipBytes
        );

        // 3. Upload ZIP Archive
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/projects/" + projectId + "/ingest/zip")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadNode = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        assertTrue(uploadNode.get("success").asBoolean());
        String jobId = uploadNode.get("data").get("jobId").asText();
        assertNotNull(jobId);

        // Wait up to 5 seconds for async ingestion to complete
        Thread.sleep(1500);

        // 4. Trigger Code Intelligence Analysis
        MvcResult analyzeResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/analyze"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode analyzeNode = objectMapper.readTree(analyzeResult.getResponse().getContentAsString());
        assertTrue(analyzeNode.get("success").asBoolean());

        // Wait up to 5 seconds for async analysis to complete
        Thread.sleep(2000);

        // 5. Query Project Intelligence
        MvcResult intelResult = mockMvc.perform(get("/api/v1/projects/" + projectId + "/intelligence"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode intelNode = objectMapper.readTree(intelResult.getResponse().getContentAsString());
        assertTrue(intelNode.get("success").asBoolean());
        JsonNode intelData = intelNode.get("data");

        assertEquals("COMPLETED", intelData.get("status").asText());
        assertTrue(intelData.get("totalEndpoints").asInt() >= 2, "Expected at least 2 discovered endpoints");
        assertTrue(intelData.get("totalControllers").asInt() >= 1, "Expected at least 1 discovered controller");
        assertTrue(intelData.get("totalServices").asInt() >= 1, "Expected at least 1 discovered service");
        assertTrue(intelData.get("totalRepositories").asInt() >= 1, "Expected at least 1 discovered repository");

        // 6. Query Existing Architecture
        MvcResult existingResult = mockMvc.perform(get("/api/v1/projects/" + projectId + "/architecture/existing"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode existNode = objectMapper.readTree(existingResult.getResponse().getContentAsString());
        assertTrue(existNode.get("success").asBoolean());
        JsonNode existData = existNode.get("data");
        assertEquals("EXISTING", existData.get("versionType").asText());
        assertTrue(existData.get("nodes").size() >= 5, "Expected layered architecture nodes");
        assertTrue(existData.get("edges").size() >= 4, "Expected flow edges connecting layers");

        // 7. Query Proposed Modular Architecture
        MvcResult proposedResult = mockMvc.perform(get("/api/v1/projects/" + projectId + "/architecture/proposed"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode propNode = objectMapper.readTree(proposedResult.getResponse().getContentAsString());
        assertTrue(propNode.get("success").asBoolean());
        JsonNode propData = propNode.get("data");
        assertEquals("PROPOSED", propData.get("versionType").asText());

        // 8. Query Architecture Comparison / Diff
        MvcResult diffResult = mockMvc.perform(get("/api/v1/projects/" + projectId + "/architecture/compare"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode diffNode = objectMapper.readTree(diffResult.getResponse().getContentAsString());
        assertTrue(diffNode.get("success").asBoolean());
        JsonNode diffData = diffNode.get("data");
        assertTrue(diffData.get("addedCapabilities").size() > 0);
        assertNotNull(diffData.get("riskLevel").asText());

        // 9. Generate Antigravity Module Blueprint
        MvcResult bpResult = mockMvc.perform(get("/api/v1/projects/" + projectId + "/architecture/blueprint?moduleName=PaymentProcessingModule"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode bpNode = objectMapper.readTree(bpResult.getResponse().getContentAsString());
        assertTrue(bpNode.get("success").asBoolean());
        JsonNode bpData = bpNode.get("data");
        assertEquals("PaymentProcessingModule", bpData.get("moduleName").asText());
        assertTrue(bpData.get("antigravityPrompt").asText().contains("PaymentProcessingModule"));
    }

    private byte[] createSampleProjectZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. pom.xml
            zos.putNextEntry(new ZipEntry("pom.xml"));
            zos.write("""
                    <project>
                        <properties><java.version>21</java.version></properties>
                        <dependencies>
                            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
                            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
                        </dependencies>
                    </project>
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. OrderController.java
            zos.putNextEntry(new ZipEntry("src/main/java/com/store/order/OrderController.java"));
            zos.write("""
                    package com.store.order;
                    import org.springframework.web.bind.annotation.*;
                    import java.util.List;
                    
                    @RestController
                    @RequestMapping("/api/orders")
                    public class OrderController {
                        @GetMapping
                        public List<String> listOrders() { return List.of("ORD-1", "ORD-2"); }
                        
                        @PostMapping
                        public String placeOrder(@RequestBody String body) { return "CREATED"; }
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. OrderService.java
            zos.putNextEntry(new ZipEntry("src/main/java/com/store/order/OrderService.java"));
            zos.write("""
                    package com.store.order;
                    import org.springframework.stereotype.Service;
                    import org.springframework.transaction.annotation.Transactional;
                    
                    @Service
                    public class OrderService {
                        @Transactional
                        public void createOrder(String payload) {
                            // Atomic transaction
                        }
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. OrderRepository.java
            zos.putNextEntry(new ZipEntry("src/main/java/com/store/order/OrderRepository.java"));
            zos.write("""
                    package com.store.order;
                    import org.springframework.stereotype.Repository;
                    
                    @Repository
                    public interface OrderRepository {
                        void findById(String id);
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 5. Order.java
            zos.putNextEntry(new ZipEntry("src/main/java/com/store/order/Order.java"));
            zos.write("""
                    package com.store.order;
                    import jakarta.persistence.Entity;
                    import jakarta.persistence.Table;
                    import jakarta.persistence.Id;
                    
                    @Entity
                    @Table(name = "orders")
                    public class Order {
                        @Id
                        private String id;
                        private Double total;
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 6. Database schema.sql
            zos.putNextEntry(new ZipEntry("src/main/resources/db/schema.sql"));
            zos.write("""
                    CREATE TABLE orders (
                        id VARCHAR(36) PRIMARY KEY,
                        total DECIMAL(10,2) NOT NULL
                    );
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
