package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.ojalgo.matrix.decomposition.MatrixDecomposition.Solver;
import org.junit.jupiter.api.BeforeAll;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addIntegration_Test {

    /**
     * Holds a mock {@link Optimisation.Integration} used in the tests.
     */
    @Mock
    private Optimisation.Integration<Object> mockIntegration;

    /**
     * Reflective access to the private static {@code INTEGRATIONS} list.
     */
    private static Field integrationsField;

    /**
     * Simple concrete class used only for readability in test code when a real
     * instance (instead of a mock) is desired. It implements
     * {@link ExpressionsBasedModel.Integration} without adding any behaviour.
     */
    private static final class TestIntegration implements ExpressionsBasedModel.Integration<Solver> {

        private final String name;

        TestIntegration(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "TestIntegration{" + "name='" + name + '\'' + '}';
        }
    }

    /**
     * Very small dummy implementation used as a stand‑in for a mock.
     */
    private static final class DummyIntegration implements ExpressionsBasedModel.Integration<Solver> {
        // no additional behaviour required
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static field once; it will be reused in every test.
        integrationsField = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        integrationsField.setAccessible(true);
        // Ensure a clean start for the whole test suite.
        ExpressionsBasedModel.clearIntegrations();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Guarantee that each test starts with an empty integration list.
        ExpressionsBasedModel.clearIntegrations();
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Clean the static list after each test to avoid side effects.
        ExpressionsBasedModel.clearIntegrations();
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Final clean‑up for good measure.
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Verify that {@code addIntegration} returns {@code true} and that the supplied integration
     * instance is present in the internal list.
     */
    @Test
    void testAddIntegrationReturnsTrueAndIsStored() throws Exception {
        DummyIntegration dummy = new DummyIntegration();
        // When
        boolean added = ExpressionsBasedModel.addIntegration(dummy);
        // Then
        assertTrue(added, "addIntegration should return true for a non‑null integration");
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertNotNull(list, "Internal integrations list must not be null");
        assertEquals(1, list.size(), "Exactly one integration should have been added");
        assertSame(dummy, list.get(0), "The stored integration should be the one that was added");
    }

    /**
     * Adding the same integration twice should result in two entries (the underlying list
     * permits duplicates). This test confirms that behaviour and also demonstrates that
     * {@code removeIntegration} can clean a specific entry.
     */
    @Test
    void testAddDuplicateIntegration() throws Exception {
        DummyIntegration dummy = new DummyIntegration();
        // Add the same dummy twice
        assertTrue(ExpressionsBasedModel.addIntegration(dummy));
        assertTrue(ExpressionsBasedModel.addIntegration(dummy));
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertEquals(2, list.size(), "Two entries should be present after adding the same integration twice");
        // Remove one occurrence and verify the list shrinks
        assertTrue(ExpressionsBasedModel.removeIntegration(dummy));
        list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertEquals(1, list.size(), "One entry should remain after removing the integration once");
    }

    /**
     * Verify that passing {@code null} does not throw and the method still returns {@code true},
     * while a {@code null} element is stored in the internal list.
     */
    @Test
    void testAddNullIntegration() throws Exception {
        boolean added = ExpressionsBasedModel.addIntegration(null);
        assertTrue(added, "addIntegration should return true even when the argument is null");
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertEquals(1, list.size(), "List should contain exactly one element after adding null");
        assertNull(list.get(0), "The stored element should be null");
    }

    /**
     * After clearing the static list, a new integration can be added and the list size
     * should be exactly one.
     */
    @Test
    void testAddIntegrationAfterClear() throws Exception {
        // Ensure the list is empty
        ExpressionsBasedModel.clearIntegrations();
        DummyIntegration fresh = new DummyIntegration();
        boolean added = ExpressionsBasedModel.addIntegration(fresh);
        assertTrue(added, "addIntegration should succeed after clearIntegrations");
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertEquals(1, list.size(), "List should contain exactly one element after clear + add");
        assertSame(fresh, list.get(0), "Stored integration must be the freshly added instance");
    }

    /**
     * Parameterised test that adds a varying number of integrations to verify that the
     * internal list grows without a hard limit.
     *
     * @param count number of integrations to add
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 10_000 })
    void testMultipleAdditions(int count) throws Exception {
        // Add 'count' distinct dummy integrations
        for (int i = 0; i < count; i++) {
            DummyIntegration integration = new DummyIntegration();
            boolean added = ExpressionsBasedModel.addIntegration(integration);
            assertTrue(added, "addIntegration should return true for integration #" + i);
        }
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertEquals(count, list.size(), "Internal list size should match the number of added integrations (" + count + ")");
    }

    /**
     * Demonstrates usage of a real {@link TestIntegration} instance (instead of a mock) for
     * the “normal addition” case described in the external JSON test specification.
     */
    @Test
    void testAddIntegrationUsingConcreteInstance() throws Exception {
        TestIntegration integration = new TestIntegration("first");
        boolean added = ExpressionsBasedModel.addIntegration(integration);
        assertTrue(added, "addIntegration should return true for a concrete Integration instance");
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) integrationsField.get(null);
        assertEquals(1, list.size(), "List should contain exactly one element");
        assertSame(integration, list.get(0), "Stored integration must be the concrete instance we added");
    }
}
