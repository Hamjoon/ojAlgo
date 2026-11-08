package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removePresolver_Test {

    /**
     * Simple concrete Presolver implementation for normal test scenarios.
     */
    private static final class TestPresolver extends ExpressionsBasedModel.Presolver {

        TestPresolver(final int executionOrder) {
            super(executionOrder);
        }
    }

    /**
     * A Presolver that deliberately breaks the {@link Comparable} contract by
     * throwing {@link ClassCastException} from {@code compareTo}. Used to
     * verify that {@code removePresolver} propagates such exceptions.
     */
    private static final class NonComparablePresolver extends ExpressionsBasedModel.Presolver {

        NonComparablePresolver(final int executionOrder) {
            super(executionOrder);
        }

        @Override
        public int compareTo(ExpressionsBasedModel.Presolver o) {
            // Force a ClassCastException regardless of the argument
            throw new ClassCastException("Incompatible Presolver type");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side‑effects
        ExpressionsBasedModel.clearPresolvers();
    }

    @Test
    void testRemoveExistingPresolver() {
        // Given: a presolver added to the static set
        TestPresolver presolver = new TestPresolver(1);
        boolean added = ExpressionsBasedModel.addPresolver(presolver);
        assertTrue(added, "Presolver should be added successfully");
        // When: we attempt to remove the same presolver
        boolean removed = ExpressionsBasedModel.removePresolver(presolver);
        // Then: removal should succeed and the set should be empty
        assertTrue(removed, "removePresolver should return true for an existing presolver");
        assertFalse(ExpressionsBasedModel.PRESOLVERS.contains(presolver), "Presolver set should no longer contain the removed presolver");
    }

    @Test
    void testRemoveNonExistingPresolver() {
        // Given: a presolver that has never been added
        TestPresolver presolver = new TestPresolver(2);
        // When: we attempt to remove it
        boolean removed = ExpressionsBasedModel.removePresolver(presolver);
        // Then: removal should fail (return false) and the set remains unchanged (still empty)
        assertFalse(removed, "removePresolver should return false for a non‑existing presolver");
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Presolver set should remain empty when removing a non‑existing presolver");
    }

    @Test
    void testRemoveNullPresolver() {
        // Given: the presolver set is empty
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        // When: we attempt to remove a null reference
        boolean removed = ExpressionsBasedModel.removePresolver(null);
        // Then: removal should return false and the set stays empty
        assertFalse(removed, "removePresolver should return false when null is passed");
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Presolver set should remain empty after attempting to remove null");
    }

    @Test
    @DisplayName("removePresolver – duplicate add then remove")
    void testDuplicateAddThenRemove() {
        TestPresolver presolver = new TestPresolver(3);
        // First add
        assertTrue(ExpressionsBasedModel.addPresolver(presolver));
        // Duplicate add – TreeSet ignores it, returns false
        assertFalse(ExpressionsBasedModel.addPresolver(presolver), "Adding the same presolver a second time should be ignored by the set");
        // Remove once – should succeed
        assertTrue(ExpressionsBasedModel.removePresolver(presolver), "First removal should succeed");
        // Second removal – now the set is empty, should return false
        assertFalse(ExpressionsBasedModel.removePresolver(presolver), "Second removal should fail because the presolver is no longer present");
    }

    @Test
    @DisplayName("removePresolver – after clearPresolvers")
    void testRemoveAfterClearPresolvers() {
        TestPresolver presolver = new TestPresolver(10);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver));
        // Clear the static set
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS should be empty after clearPresolvers()");
        // Attempt removal – should return false
        assertFalse(ExpressionsBasedModel.removePresolver(presolver), "Removal after clearPresolvers should return false");
    }

    @Test
    @DisplayName("removePresolver – equal but different instance")
    void testEqualButDifferentInstanceRemoval() {
        // Two distinct instances with the same ordering key (executionOrder = 100)
        TestPresolver first = new TestPresolver(100);
        // equal by compareTo
        TestPresolver second = new TestPresolver(100);
        assertTrue(ExpressionsBasedModel.addPresolver(first), "First presolver should be added");
        // Adding the second instance is ignored because compareTo deems them equal
        assertFalse(ExpressionsBasedModel.addPresolver(second), "Second presolver with same order should not be added (duplicate)");
        // Removing using the second instance should still succeed
        assertTrue(ExpressionsBasedModel.removePresolver(second), "Removal using an equal‑by‑order but different instance should succeed");
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Set should be empty after successful removal");
    }

    @Test
    @DisplayName("removePresolver – multiple presolvers, remove one")
    void testMultiplePresolversRemoveOne() {
        TestPresolver presolverA = new TestPresolver(12);
        TestPresolver presolverB = new TestPresolver(13);
        assertTrue(ExpressionsBasedModel.addPresolver(presolverA));
        assertTrue(ExpressionsBasedModel.addPresolver(presolverB));
        // Remove presolverA
        assertTrue(ExpressionsBasedModel.removePresolver(presolverA), "Removal of presolverA should succeed");
        // Verify presolverB is still present
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(presolverB), "PresolverB should remain in the set after removing presolverA");
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size(), "Only one presolver should remain in the set");
    }

    @Test
    @DisplayName("removePresolver – after resetPresolvers")
    void testRemoveAfterResetPresolvers() {
        TestPresolver presolver = new TestPresolver(14);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver));
        // Reset the static set (clears it)
        ExpressionsBasedModel.resetPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS should be empty after resetPresolvers()");
        // Attempt removal – should return false
        assertFalse(ExpressionsBasedModel.removePresolver(presolver), "Removal after resetPresolvers should return false");
    }

    @Test
    @DisplayName("removePresolver – remove when set already empty")
    void testRemoveWhenSetEmpty() {
        // Ensure the set is empty
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        TestPresolver presolver = new TestPresolver(15);
        // Direct removal without any prior add
        assertFalse(ExpressionsBasedModel.removePresolver(presolver), "Removing from an empty set should return false");
    }

    @Test
    @DisplayName("removePresolver – incompatible type causing ClassCastException")
    void testIncompatibleTypeClassCastException() {
        // Add a normal presolver so that TreeSet has an element to compare against
        TestPresolver normal = new TestPresolver(20);
        assertTrue(ExpressionsBasedModel.addPresolver(normal));
        // Create a presolver that throws ClassCastException from compareTo
        NonComparablePresolver bad = new NonComparablePresolver(21);
        // Attempt removal – expect ClassCastException to propagate
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.removePresolver(bad), "Removing a Presolver with an incompatible compareTo implementation should throw ClassCastException");
    }
}