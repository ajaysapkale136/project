package com.aiproject.architect;

import com.aiproject.architect.modules.anatomy.adapter.ExecutionTraceAdapter;
import com.aiproject.architect.modules.anatomy.adapter.InternalExplodedAdapter;
import com.aiproject.architect.modules.anatomy.adapter.ProjectAssemblyAdapter;
import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.aiproject.architect.modules.intelligence.entity.AnalysisModule;
import com.aiproject.architect.modules.intelligence.entity.ModuleDependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnatomyAnimationEngineTest {

    private final ProjectAssemblyAdapter projectAssemblyAdapter = new ProjectAssemblyAdapter();
    private final InternalExplodedAdapter internalExplodedAdapter = new InternalExplodedAdapter();
    private final ExecutionTraceAdapter executionTraceAdapter = new ExecutionTraceAdapter();

    @Test
    @DisplayName("Format 1: Should build complete Module-to-Module scene with assemblies and connections")
    void testFormat1ModuleToModuleScene() {
        AnalysisModule mod1 = AnalysisModule.builder().name("Frontend SPA").moduleType("FRONTEND_SPA").basePath("frontend/src").build();
        mod1.setId("m1");
        AnalysisModule mod2 = AnalysisModule.builder().name("Core Backend").moduleType("BACKEND_SERVICE").basePath("src/main/java").build();
        mod2.setId("m2");

        ModuleDependency dep = ModuleDependency.builder().sourceModuleId("m1").targetModuleId("m2").dependencyType("SYNCHRONOUS_REST").build();
        dep.setId("d1");

        AnatomySceneResponse scene = projectAssemblyAdapter.buildModuleScene("p1", List.of(mod1, mod2), List.of(dep));

        assertNotNull(scene);
        assertEquals("MODULE_TO_MODULE", scene.getAnimationFormat());
        assertTrue(scene.getAssemblies().size() >= 3); // Baseplate + 2 modules
        assertEquals(1, scene.getConnections().size());
        assertFalse(scene.getSteps().isEmpty());
    }

    @Test
    @DisplayName("Format 2: Should build Internal Exploded View with layered assemblies and Z offsets")
    void testFormat2InternalExplodedScene() {
        AnalysisModule mod = AnalysisModule.builder().name("Core Backend").moduleType("BACKEND_SERVICE").basePath("src/main/java").build();
        mod.setId("m2");

        AnalysisComponent c1 = AnalysisComponent.builder().name("OrderController").componentType("CONTROLLER").filePath("OrderController.java").build();
        c1.setId("c1");
        AnalysisComponent c2 = AnalysisComponent.builder().name("OrderService").componentType("SERVICE").filePath("OrderService.java").build();
        c2.setId("c2");
        AnalysisComponent c3 = AnalysisComponent.builder().name("OrderRepository").componentType("REPOSITORY").filePath("OrderRepository.java").build();
        c3.setId("c3");
        AnalysisComponent c4 = AnalysisComponent.builder().name("Order").componentType("ENTITY").filePath("Order.java").build();
        c4.setId("c4");

        List<AnalysisComponent> components = List.of(c1, c2, c3, c4);

        AnatomySceneResponse scene = internalExplodedAdapter.buildExplodedScene("p1", mod, components);

        assertNotNull(scene);
        assertEquals("INTERNAL_EXPLODED", scene.getAnimationFormat());
        assertEquals(4, scene.getAssemblies().size());
        assertTrue(scene.getAssemblies().stream().anyMatch(a -> a.getExplodedOffsetZ() > 0), "Expected vertical exploded Z offsets");
        assertEquals(4, scene.getSteps().size());
    }

    @Test
    @DisplayName("Format 3: Should build a source-derived execution trace")
    void testFormat3ExecutionTraceScene() {
        AnalysisComponent controller = component("OrderController", "CONTROLLER", "c1",
                "{\"fields\":[{\"name\":\"orderService\",\"type\":\"OrderService\"}],\"methodCalls\":[{\"scope\":\"orderService\",\"method\":\"placeOrder\"}]}" );
        AnalysisComponent service = component("OrderService", "SERVICE", "c2",
                "{\"fields\":[{\"name\":\"orderRepository\",\"type\":\"OrderRepository\"}],\"methodCalls\":[{\"scope\":\"orderRepository\",\"method\":\"save\"}]}" );
        AnalysisComponent repository = component("OrderRepository", "REPOSITORY", "c3", "{}");

        AnatomySceneResponse scene = executionTraceAdapter.buildExecutionTraceScene("p1", "POST /api/orders", List.of(controller, service, repository));

        assertNotNull(scene);
        assertEquals("EXECUTION_TRACE", scene.getAnimationFormat());
        assertEquals(3, scene.getAssemblies().size());
        assertEquals(3, scene.getSteps().size());
        assertEquals(2, scene.getConnections().size());
        assertEquals("DISPATCH", scene.getSteps().get(0).getPhase());
    }

    @Test
    @DisplayName("Format 3: Should report unavailable when no controller supports the operation")
    void testFormat3AuthenticationTraceScene() {
        AnatomySceneResponse scene = executionTraceAdapter.buildExecutionTraceScene("p1", "POST /api/auth/login", List.of());

        assertNotNull(scene);
        assertEquals("EXECUTION_TRACE", scene.getAnimationFormat());
        assertTrue(scene.getAssemblies().isEmpty());
        assertTrue(scene.getSteps().isEmpty());
    }

    private AnalysisComponent component(String name, String type, String id, String detailsJson) {
        AnalysisComponent component = AnalysisComponent.builder()
                .name(name).componentType(type).filePath(name + ".java").detailsJson(detailsJson).build();
        component.setId(id);
        return component;
    }
}
