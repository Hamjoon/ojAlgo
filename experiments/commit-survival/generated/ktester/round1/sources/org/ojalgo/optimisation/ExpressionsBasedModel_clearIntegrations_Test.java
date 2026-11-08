package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_clearIntegrations_Test {

    /**
     * A mock integration used for the test.
     */
//     @Mock
//     private Integration<?> mockIntegration;

    /**
     * A dummy integration used for the test.
     */
//     private static Integration<?, ?> dummyIntegration = createDummyIntegration();

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure a clean state before any test runs.
        ExpressionsBasedModel.clearIntegrations();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test starts with a clean integration list.
        ExpressionsBasedModel.clearIntegrations();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side‑effects.
        ExpressionsBasedModel.clearIntegrations();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Final cleanup.
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Returns the current size of the internal INTEGRATIONS list.
     */
    private static int integrationCount() {
        List<ExpressionsBasedModel.Integration<?>> list = getIntegrationsList();
        return list == null ? 0 : list.size();
    }

    @Test
    void testClearIntegrationsResetsIntegrationSwitch() {
        // 1. No integrations have been added yet → switch should be false.
        ExpressionsBasedModel modelBefore = new ExpressionsBasedModel();
        assertFalse(modelBefore.isIntegrationSwitch(), "Integration switch should be false before any integration is added.");
        // 2. Add a dummy integration using the public static method.
        boolean added = ExpressionsBasedModel.addIntegration(dummyIntegration);
        assertTrue(added, "addIntegration should return true for the dummy integration.");
        // 3. After adding, any new model should report the integration switch as true.
        ExpressionsBasedModel modelAfterAdd = new ExpressionsBasedModel();
        assertTrue(modelAfterAdd.isIntegrationSwitch(), "Integration switch should be true after an integration has been added.");
        // 4. Clear all integrations.
        ExpressionsBasedModel.clearIntegrations();
        // 5. A fresh model should now see the switch as false again.
        ExpressionsBasedModel modelAfterClear = new ExpressionsBasedModel();
        assertFalse(modelAfterClear.isIntegrationSwitch(), "Integration switch should be false after clearIntegrations() is called.");
    }

    @Test
    void testClearIntegrationsIsIdempotent() {
        // Call clearIntegrations multiple times – it should not throw and the state stays cleared.
        ExpressionsBasedModel.clearIntegrations();
        // second call
        ExpressionsBasedModel.clearIntegrations();
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isIntegrationSwitch(), "Integration switch should remain false after multiple clearIntegrations() calls.");
    }

    /**
     * Helper to obtain the private static {@code INTEGRATIONS} list via reflection.
     */
//     @SuppressWarnings("unchecked")
//     private static List<Integration<?>> getIntegrationsList() {
//         try {
//             Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
//             field.setAccessible(true);
//             // The field is static, so the instance argument is null.
//             return (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
//         } catch (NoSuchFieldException | IllegalAccessException e) {
//             throw new AssertionError("Unable to access INTEGRATIONS field via reflection", e);
//         }
//     }

    @Test
    void testClearIntegrationsWhenAlreadyEmpty() {
        // The list should be empty initially.
        assertEquals(0, integrationCount(), "INTEGRATIONS list should be empty before clearIntegrations is called.");
        // Call clearIntegrations on an already empty list.
        ExpressionsBasedModel.clearIntegrations();
        // Verify that the list remains empty and no exception is thrown.
        assertEquals(0, integrationCount(), "INTEGRATIONS list should remain empty after clearIntegrations on an empty list.");
    }

    @Test
    void testClearIntegrationsAfterAddingTwoIntegrations() {
        // Add two distinct dummy integrations.
        ExpressionsBasedModel.Integration<?> i1 = createDummyIntegration();
        ExpressionsBasedModel.Integration<?> i2 = createDummyIntegration();
        assertTrue(ExpressionsBasedModel.addIntegration(i1), "First integration should be added.");
        assertTrue(ExpressionsBasedModel.addIntegration(i2), "Second integration should be added.");
        assertEquals(2, integrationCount(), "INTEGRATIONS list should contain two elements after two additions.");
        // Clear and verify size becomes zero.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "INTEGRATIONS list should be empty after clearIntegrations.");
    }

    @Test
    void testClearIntegrationsIdempotentWhenCalledTwice() {
        // Ensure list is empty, then call clear twice.
        ExpressionsBasedModel.clearIntegrations();
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "INTEGRATIONS list should stay empty after two consecutive clearIntegrations calls.");
    }

    @Test
    void testClearIntegrationsAfterRemovingOneIntegration() {
        ExpressionsBasedModel.Integration<?> i1 = createDummyIntegration();
        ExpressionsBasedModel.Integration<?> i2 = createDummyIntegration();
        ExpressionsBasedModel.addIntegration(i1);
        ExpressionsBasedModel.addIntegration(i2);
        assertEquals(2, integrationCount(), "Pre‑condition: two integrations added.");
        // Remove one integration.
        assertTrue(ExpressionsBasedModel.removeIntegration(i1), "Removal of first integration should succeed.");
        assertEquals(1, integrationCount(), "One integration should remain after removal.");
        // Clear and verify empty.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "INTEGRATIONS list should be empty after clearIntegrations following a removal.");
    }

    @Test
    void testClearIntegrationsThenAddAgain() {
        ExpressionsBasedModel.Integration<?> i1 = createDummyIntegration();
        ExpressionsBasedModel.addIntegration(i1);
        assertEquals(1, integrationCount(), "One integration should be present after first add.");
        // Clear the list.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "List should be empty after clearIntegrations.");
        // Add a new integration.
        ExpressionsBasedModel.Integration<?> i2 = createDummyIntegration();
        ExpressionsBasedModel.addIntegration(i2);
        assertEquals(1, integrationCount(), "After clear and a new add, list should contain exactly one integration.");
    }

    @Test
    void testClearIntegrationsWithDuplicateAddition() {
        ExpressionsBasedModel.Integration<?> dup = createDummyIntegration();
        // Add the same instance twice.
        ExpressionsBasedModel.addIntegration(dup);
        ExpressionsBasedModel.addIntegration(dup);
        // The internal list may contain one or two entries; we only care that clear works.
        int sizeBeforeClear = integrationCount();
        assertTrue(sizeBeforeClear == 1 || sizeBeforeClear == 2, "List size after duplicate adds should be 1 or 2, actual: " + sizeBeforeClear);
        // Clear and verify emptiness.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "INTEGRATIONS list should be empty after clearIntegrations even if duplicates were added.");
    }

    @Test
    void testClearIntegrationsAfterFailedNullAdd() {
        // Attempt to add a null integration – should return false and not modify the list.
        assertFalse(ExpressionsBasedModel.addIntegration(null), "addIntegration(null) should return false.");
        assertEquals(0, integrationCount(), "INTEGRATIONS list should remain empty after failed null addition.");
        // Clear (no‑op) and verify still empty.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "INTEGRATIONS list should stay empty after clearIntegrations following a failed add.");
    }

    @Test
    void testClearIntegrationsAlternatingAddsAndClears() {
        // First batch: add two integrations.
        ExpressionsBasedModel.Integration<?> a1 = createDummyIntegration();
        ExpressionsBasedModel.Integration<?> a2 = createDummyIntegration();
        ExpressionsBasedModel.addIntegration(a1);
        ExpressionsBasedModel.addIntegration(a2);
        assertEquals(2, integrationCount(), "After first two adds, size should be 2.");
        // Clear.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "After first clear, size should be 0.");
        // Second batch: add three integrations.
        ExpressionsBasedModel.Integration<?> b1 = createDummyIntegration();
        ExpressionsBasedModel.Integration<?> b2 = createDummyIntegration();
        ExpressionsBasedModel.Integration<?> b3 = createDummyIntegration();
        ExpressionsBasedModel.addIntegration(b1);
        ExpressionsBasedModel.addIntegration(b2);
        ExpressionsBasedModel.addIntegration(b3);
        assertEquals(3, integrationCount(), "After three adds, size should be 3.");
        // Second clear.
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationCount(), "After second clearIntegrations, list size should return to 0.");
    }

    /**
     * Creates a dummy {@link Integration} implementation using a dynamic proxy.
     * All method invocations simply return {@code null} (or {@code false} for primitive boolean returns).
     */
//     @SuppressWarnings("unchecked")
//     private static Integration<?, ?> createDummyIntegration() {
//         return (ExpressionsBasedModel.Integration<?, ?>) Proxy.newProxyInstance(ExpressionsBasedModel.Integration.class.getClassLoader(), new Class[] { ExpressionsBasedModel.Integration.class }, (proxy, method, args) -> {
//             Class<?> returnType = method.getReturnType();
//             if (returnType.isPrimitive()) {
//                 if (returnType == boolean.class) {
//                     return false;
//                 } else if (returnType == char.class) {
//                     return '\0';
//                 } else if (returnType == byte.class || returnType == short.class || returnType == int.class || returnType == long.class) {
//                     return 0;
//                 } else if (returnType == float.class || returnType == double.class) {
//                     return 0.0;
//                 }
//             }
//             return null;
//         });
//     }
}