package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_clearIntegrations_Test {

    /**
     * Simple stub for {@link Optimisation.Integration}.
     */
    private static final class DummyIntegration implements Optimisation.Integration<ExpressionsBasedModel, Object> {

        @Override
        public void integrate(final ExpressionsBasedModel model) {
            // No‑op implementation – only needed so that the object can be stored.
        }
    }

    /**
     * Simple stub for {@link Presolver}.
     */
    private static final class DummyPresolver implements Presolver {

        @Override
        public void presolve(final ExpressionsBasedModel model) {
            // No‑op implementation – only needed so that the object can be stored.
        }
    }

    /**
     * Ensure a clean state before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The static list may contain integrations left over from other tests.
        // Clearing it guarantees deterministic behaviour.
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Clean up after each test – also clears the static list for safety.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Verify that an integration added before a clear is no longer present afterwards.
     */
    @Test
    void testClearIntegrationsRemovesPreviouslyAddedIntegration() {
        // GIVEN – a (stub) integration that can be added to the model.
        Optimisation.Integration<ExpressionsBasedModel, ?> integration = new DummyIntegration();
        // Adding the integration should succeed.
        assertTrue(ExpressionsBasedModel.addIntegration(integration), "The integration should be added successfully before clearing.");
        // WHEN – the static clear method is invoked.
        ExpressionsBasedModel.clearIntegrations();
        // THEN – the previously added integration can no longer be removed
        // (because it is no longer present) and can be added again.
        assertFalse(ExpressionsBasedModel.removeIntegration(integration), "After clearing, the previously added integration should not be removable.");
        // Adding the same integration again must succeed, proving that the list is empty.
        assertTrue(ExpressionsBasedModel.addIntegration(integration), "After clearing, the integration should be addable again.");
    }

    /**
     * Verify that calling clearIntegrations on an already empty list does not fail.
     */
    @Test
    void testClearIntegrationsOnEmptyListIsNoOp() {
        // The list is already cleared by @BeforeEach.
        // Calling clearIntegrations again must not throw any exception.
        assertDoesNotThrow(ExpressionsBasedModel::clearIntegrations, "Calling clearIntegrations on an empty list should not throw.");
    }

    /**
     * After a clear, a newly added integration should be present (size == 1).
     */
    @Test
    void testSubsequentAdditionAfterClear() {
        // Ensure the list is empty.
        ExpressionsBasedModel.clearIntegrations();
        Optimisation.Integration<ExpressionsBasedModel, ?> newIntegration = new DummyIntegration();
        // Add a new integration.
        assertTrue(ExpressionsBasedModel.addIntegration(newIntegration), "Integration should be added after clear.");
        // The integration must now be removable, indicating the list size is 1.
        assertTrue(ExpressionsBasedModel.removeIntegration(newIntegration), "Integration should be removable, confirming it was stored.");
    }

    /**
     * clearIntegrations must not affect the static PRESOLVERS collection.
     */
    @Test
    void testClearIntegrationsDoesNotAffectPresolvers() {
        // Add a dummy presolver to the static set.
        Presolver dummyPresolver = new DummyPresolver();
        assertTrue(ExpressionsBasedModel.addPresolver(dummyPresolver), "Presolver should be added successfully.");
        // Clear integrations – this must not touch PRESOLVERS.
        ExpressionsBasedModel.clearIntegrations();
        // The presolver should still be removable, proving it is still present.
        assertTrue(ExpressionsBasedModel.removePresolver(dummyPresolver), "Presolver must remain after clearIntegrations().");
    }

    /**
     * Calling clearIntegrations twice consecutively leaves the list empty (idempotent).
     */
    @Test
    void testIdempotentMultipleClearCalls() {
        // First clear – list is already empty due to @BeforeEach.
        ExpressionsBasedModel.clearIntegrations();
        // Second clear – should be a no‑op and not throw.
        assertDoesNotThrow(ExpressionsBasedModel::clearIntegrations, "Second clearIntegrations() call should not throw.");
        // Verify the list is still empty by attempting to remove a dummy integration.
        Optimisation.Integration<ExpressionsBasedModel, ?> dummyIntegration = new DummyIntegration();
        assertFalse(ExpressionsBasedModel.removeIntegration(dummyIntegration), "After two clears, no integration should be present.");
    }
}
