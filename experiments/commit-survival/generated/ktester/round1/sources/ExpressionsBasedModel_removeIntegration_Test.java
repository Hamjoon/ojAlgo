package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.ojalgo.matrix.decomposition.MatrixDecomposition;
import org.ojalgo.optimisation.Optimisation.Solver;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removeIntegration_Test {

    /**
     * Simple integration used for normal add/remove scenarios.
     */
    private static final class SimpleIntegration implements Integration<Solver> {

        private final String id;

        SimpleIntegration(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof SimpleIntegration))
                return false;
            SimpleIntegration that = (SimpleIntegration) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "SimpleIntegration{" + id + '}';
        }
    }

    /**
     * Integration whose equals() throws a RuntimeException.
     */
    private static final class IntegrationThrowsRuntimeException implements Integration<Solver> {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("forced RuntimeException from equals()");
        }
    }

    /**
     * Integration whose equals() throws a NullPointerException.
     */
    private static final class IntegrationThrowsNullPointerException implements Integration<Solver> {

        @Override
        public boolean equals(Object o) {
            throw new NullPointerException("forced NullPointerException from equals()");
        }
    }

    /**
     * Integration whose equals() throws a ClassCastException.
     */
    private static final class IntegrationThrowsClassCastException implements Integration<Solver> {

        @Override
        public boolean equals(Object o) {
            // deliberately cast to an unrelated type
            String s = (String) o;
            return false;
        }
    }

    /**
     * Ensure a clean state before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The static list of integrations is shared across all tests, so we clear it.
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Test data for the “removeIntegration” group (basic scenarios).
     */
    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// null argument – should return false, no exception
        Arguments.of(null, false, "null argument should safely return false"), // never‑added integration – should return false
        Arguments.of(new SimpleIntegration("neverAdded"), false, "removing an integration that was never added must return false"));
    }

    /**
     * Test data for the “removeIntegration_additional” group (more complex scenarios).
     */
    private static Stream<Arguments> additionalScenarios() {
        return Stream.of(// after clearIntegrations the list is empty – removal must be false
        Arguments.of(new SimpleIntegration("integrationX"), false, "list cleared, removal must be false"), // add then remove – should succeed
        Arguments.of(new SimpleIntegration("integrationY"), true, "add then remove should succeed"), // equality based removal – different instance but equal according to equals()
        Arguments.of(new SimpleIntegration("equalB"), true, "different but equal instance should be removable"), // multiple integrations – remove one of them
        Arguments.of(new SimpleIntegration("integrationC"), true, "removing one from several integrations should succeed"), // duplicate entries – first removal
        Arguments.of(new SimpleIntegration("duplicate"), true, "first removal of duplicate entry should succeed"), // duplicate entries – second removal (second copy still present)
        Arguments.of(new SimpleIntegration("duplicate"), true, "second removal of duplicate entry should succeed"));
    }

    /**
     * Verify that an integration that has been added can be removed.
     */
    @Test
    void testRemoveExistingIntegration() {
        // GIVEN – an integration that has been added to the model
        Integration<Solver> integration = new SimpleIntegration("mock");
        boolean added = ExpressionsBasedModel.addIntegration(integration);
        assertTrue(added, "The integration should have been added successfully");
        // WHEN – we attempt to remove the same integration
        boolean removed = ExpressionsBasedModel.removeIntegration(integration);
        // THEN – the removal should succeed and the integration list should be empty again
        assertTrue(removed, "removeIntegration should return true for an existing integration");
        assertFalse(ExpressionsBasedModel.removeIntegration(integration), "Subsequent removal of the same integration must return false");
    }

    /**
     * Verify that attempting to remove an integration that was never added returns false.
     */
    @Test
    void testRemoveNonExistingIntegration() {
        // GIVEN – an integration that has NOT been added
        Integration<Solver> integration = new SimpleIntegration("notAdded");
        // WHEN – we try to remove it
        boolean removed = ExpressionsBasedModel.removeIntegration(integration);
        // THEN – the operation must indicate failure
        assertFalse(removed, "removeIntegration should return false when the integration is not present");
    }

    /**
     * Verify that the static clearIntegrations method truly empties the collection.
     */
    @Test
    void testClearIntegrationsEffect() {
        // GIVEN – two different integrations added to the model
        Integration<Solver> int1 = new SimpleIntegration("int1");
        Integration<Solver> int2 = new SimpleIntegration("int2");
        ExpressionsBasedModel.addIntegration(int1);
        ExpressionsBasedModel.addIntegration(int2);
        // WHEN – we clear all integrations
        ExpressionsBasedModel.clearIntegrations();
        // THEN – removing either integration must now fail
        assertFalse(ExpressionsBasedModel.removeIntegration(int1), "After clearIntegrations, int1 should no longer be removable");
        assertFalse(ExpressionsBasedModel.removeIntegration(int2), "After clearIntegrations, int2 should no longer be removable");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicScenarios")
    void testBasicScenarios(Integration<Object> integration, boolean expectedResult, String description) {
        // GIVEN – the static list is already cleared by @BeforeEach
        // WHEN – attempt to remove the supplied integration
        boolean result = ExpressionsBasedModel.removeIntegration(integration);
        // THEN – verify the expected boolean outcome
        assertEquals(expectedResult, result, description);
    }

    @Test
    void testDuplicateIntegrationRemovalSequence() {
        // GIVEN – same integration added twice
        Integration<Solver> duplicate = new SimpleIntegration("duplicate");
        ExpressionsBasedModel.addIntegration(duplicate);
        // second copy
        ExpressionsBasedModel.addIntegration(duplicate);
        // WHEN – first removal
        boolean first = ExpressionsBasedModel.removeIntegration(duplicate);
        // THEN – first removal succeeds
        assertTrue(first, "first removal of duplicate should return true");
        // WHEN – second removal
        boolean second = ExpressionsBasedModel.removeIntegration(duplicate);
        // THEN – second removal also succeeds (second copy removed)
        assertTrue(second, "second removal of duplicate should return true");
        // WHEN – third removal (no copies left)
        boolean third = ExpressionsBasedModel.removeIntegration(duplicate);
        // THEN – now returns false
        assertFalse(third, "third removal should return false as list is empty for that instance");
    }

    @Test
    void testEqualityBasedRemoval() {
        // GIVEN – an integration added with id "equal"
        Integration<Solver> original = new SimpleIntegration("equal");
        ExpressionsBasedModel.addIntegration(original);
        // AND – a different instance but equal according to equals()
        Integration<Solver> equalCopy = new SimpleIntegration("equal");
        // WHEN – remove using the equal copy
        boolean removed = ExpressionsBasedModel.removeIntegration(equalCopy);
        // THEN – removal succeeds
        assertTrue(removed, "Removal should succeed when an equal instance is supplied");
    }

    @Test
    void testRemovalWhenMultipleIntegrationsPresent() {
        // GIVEN – three distinct integrations
        Integration<Solver> a = new SimpleIntegration("A");
        Integration<Solver> b = new SimpleIntegration("B");
        Integration<Solver> c = new SimpleIntegration("C");
        ExpressionsBasedModel.addIntegration(a);
        ExpressionsBasedModel.addIntegration(b);
        ExpressionsBasedModel.addIntegration(c);
        // WHEN – remove integration C
        boolean removedC = ExpressionsBasedModel.removeIntegration(c);
        assertTrue(removedC, "Removing integration C should succeed");
        // THEN – A and B are still present
        assertTrue(ExpressionsBasedModel.removeIntegration(a), "Integration A should still be removable");
        assertTrue(ExpressionsBasedModel.removeIntegration(b), "Integration B should still be removable");
    }

    @Test
    void testRemoveIntegrationPropagatesRuntimeException() {
        Integration<Solver> bad = new IntegrationThrowsRuntimeException();
        // add a dummy integration to ensure the list is not empty (not required but realistic)
        ExpressionsBasedModel.addIntegration(new SimpleIntegration("dummy"));
        // WHEN / THEN – the RuntimeException from equals() must propagate
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.removeIntegration(bad), "RuntimeException from equals() should be propagated");
        assertEquals("forced RuntimeException from equals()", ex.getMessage());
    }

    @Test
    void testRemoveIntegrationPropagatesNullPointerException() {
        Integration<Solver> bad = new IntegrationThrowsNullPointerException();
        // WHEN / THEN – the NullPointerException must propagate
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.removeIntegration(bad), "NullPointerException from equals() should be propagated");
        assertEquals("forced NullPointerException from equals()", ex.getMessage());
    }

    @Test
    void testRemoveIntegrationPropagatesClassCastException() {
        Integration<Solver> bad = new IntegrationThrowsClassCastException();
        // WHEN / THEN – the ClassCastException must propagate
        ClassCastException ex = assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.removeIntegration(bad), "ClassCastException from equals() should be propagated");
        // message may be null depending on JVM; we only assert the type
        assertNotNull(ex);
    }
}
