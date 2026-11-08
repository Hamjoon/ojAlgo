package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addIntegration_Test {

    /**
     * Holds a reference to the private static {@code INTEGRATIONS} field.
     */
    private static List<?> integrationsList;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static field via reflection once for the whole test class.
        Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<?> list = (List<?>) field.get(null);
        integrationsList = list;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        ExpressionsBasedModel.clearIntegrations();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test as a safety net.
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Verify that adding an {@code ExpressionsBasedModel.Integration} instance
     * stores the exact instance in the internal list and returns {@code true}.
     */
    @Test
    void testAddExpressionsBasedModelIntegration() {
        // Create a concrete integration – the inner class is package‑private,
        // but we can instantiate it via an anonymous subclass.
        ExpressionsBasedModel.Integration<Object> integration = new ExpressionsBasedModel.Integration<Object>() {

            @Override
            public void initialise(ExpressionsBasedModel model) {
                // no‑op for test
            }

            @Override
            public Object getResult() {
                return null;
            }
        };
        boolean added = ExpressionsBasedModel.addIntegration(integration);
        assertTrue(added, "addIntegration should return true for a valid integration");
        // The internal list should now contain exactly our instance.
        assertEquals(1, integrationsList.size(), "INTEGRATIONS should contain one element after adding");
        assertSame(integration, integrationsList.get(0), "The stored integration should be the same instance that was added");
    }

    /**
     * Verify that adding a generic {@code Optimisation.Integration} that is NOT an
     * {@code ExpressionsBasedModel.Integration} results in a wrapped instance being stored.
     */
    @Test
    void testAddForeignIntegrationIsWrapped() {
        // Create a simple foreign integration (does not extend ExpressionsBasedModel.Integration)
        @SuppressWarnings("rawtypes")
        Optimisation.Integration foreign = new Optimisation.Integration<Object, Object>() {

            @Override
            public void initialise(Object model) {
                // no‑op
            }

            @Override
            public Object getResult() {
                return null;
            }
        };
        // The addIntegration method expects a specific generic type, so we cast to the raw type.
        boolean added = ExpressionsBasedModel.addIntegration((Optimisation.Integration) foreign);
        assertTrue(added, "addIntegration should return true for a foreign integration");
        // After wrapping, the list should contain exactly one element.
        assertEquals(1, integrationsList.size(), "INTEGRATIONS should contain one element after adding a foreign integration");
        // The stored element must be an instance of the private wrapper class.
        Object stored = integrationsList.get(0);
        assertTrue(stored.getClass().getSimpleName().contains("IntegrationWrapper"), "The stored integration should be an IntegrationWrapper");
    }

    /**
     * Adding the same integration twice should result in {@code false} on the second call
     * because the underlying {@code List} (an {@code ArrayList}) allows duplicates,
     * but the method returns the result of {@code List.add}, which is always {@code true}
     * for {@code ArrayList}. This test documents the current behaviour.
     */
    @Test
    void testAddSameIntegrationMultipleTimes() {
        ExpressionsBasedModel.Integration<Object> integration = new ExpressionsBasedModel.Integration<Object>() {

            @Override
            public void initialise(ExpressionsBasedModel model) {
                // no‑op
            }

            @Override
            public Object getResult() {
                return null;
            }
        };
        boolean first = ExpressionsBasedModel.addIntegration(integration);
        boolean second = ExpressionsBasedModel.addIntegration(integration);
        assertTrue(first, "First addition should succeed");
        assertTrue(second, "Second addition also returns true because ArrayList.add always returns true");
        assertEquals(2, integrationsList.size(), "Both additions result in two entries (ArrayList permits duplicates)");
    }

    /**
     * Passing {@code null} should trigger the else‑branch, which attempts to wrap the
     * argument in an {@code IntegrationWrapper}. The wrapper constructor is expected
     * to throw a {@link NullPointerException}, and the method should propagate it.
     */
    @Test
    void testAddNullIntegrationThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.addIntegration(null), "addIntegration(null) should throw NullPointerException");
        // The list must remain empty after the exception.
        assertEquals(0, integrationsList.size(), "INTEGRATIONS should stay empty when adding null fails");
    }
}
