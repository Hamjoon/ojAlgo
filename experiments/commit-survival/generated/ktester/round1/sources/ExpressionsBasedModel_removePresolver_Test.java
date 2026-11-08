package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removePresolver_Test {

    /**
     * Simple concrete Presolver that can be instantiated from the test.
     */
    private static final class DummyPresolver extends ExpressionsBasedModel.Presolver {

        DummyPresolver(final int order) {
            super(order);
        }
    }

    /**
     * A Presolver that deliberately throws {@link ClassCastException}
     * when {@code compareTo} is invoked. Used to verify that such an
     * exception propagates from {@code TreeSet.remove}.
     */
    private static final class IncompatiblePresolver extends ExpressionsBasedModel.Presolver {

        IncompatiblePresolver(final int order) {
            super(order);
        }

        @Override
        public int compareTo(ExpressionsBasedModel.Presolver o) {
            throw new ClassCastException("Incompatible presolver");
        }
    }

    /**
     * A Presolver whose {@code compareTo} throws a generic
     * {@link RuntimeException}. This simulates a buggy implementation.
     */
    private static final class FaultyPresolver extends ExpressionsBasedModel.Presolver {

        FaultyPresolver(final int order) {
            super(order);
        }

        @Override
        public int compareTo(ExpressionsBasedModel.Presolver o) {
            throw new RuntimeException("Faulty compareTo");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side‑effects on other tests
        ExpressionsBasedModel.clearPresolvers();
    }

    @Test
    void testRemoveExistingPresolverReturnsTrue() {
        DummyPresolver presolver = new DummyPresolver(1);
        // Add the presolver first – the method returns true if the set changed
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "addPresolver should return true when a new presolver is added");
        // Verify that the internal set actually contains the presolver
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(presolver), "PRESOLVERS should contain the added presolver before removal");
        // Now remove it – expected to return true
        boolean removed = ExpressionsBasedModel.removePresolver(presolver);
        assertTrue(removed, "removePresolver should return true for an existing presolver");
        // And the set must no longer contain it
        assertFalse(ExpressionsBasedModel.PRESOLVERS.contains(presolver), "PRESOLVERS should not contain the presolver after removal");
    }

    @Test
    void testRemoveNonExistingPresolverReturnsFalse() {
        DummyPresolver presolver = new DummyPresolver(2);
        // Ensure the set is empty; clearPresolvers already did this in @BeforeEach
        assertFalse(ExpressionsBasedModel.PRESOLVERS.contains(presolver), "PRESOLVERS should be empty before the test");
        // Attempt to remove a presolver that was never added
        boolean removed = ExpressionsBasedModel.removePresolver(presolver);
        assertFalse(removed, "removePresolver should return false when the presolver is not present");
        // The set should still be empty
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS should remain empty after attempting to remove a non‑existent presolver");
    }

    @Test
    void testRemoveNullThrowsNullPointerException() {
        // TreeSet.remove(null) is expected to throw NPE because the comparator
        // does not accept null keys.
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.removePresolver(null), "removePresolver(null) should propagate NullPointerException");
    }

    @Test
    void testRemovePresolverEqualityHandling() {
        // Two DummyPresolver instances with the same order are considered equal.
        DummyPresolver first = new DummyPresolver(10);
        // same order → equal
        DummyPresolver second = new DummyPresolver(10);
        assertTrue(ExpressionsBasedModel.addPresolver(first), "First presolver should be added successfully");
        // Removing the second (equal) instance should succeed.
        assertTrue(ExpressionsBasedModel.removePresolver(second), "Removing an equal presolver instance should return true");
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Set should be empty after removal");
    }

    @Test
    void testRemoveMultipleEntries() {
        DummyPresolver a = new DummyPresolver(1);
        DummyPresolver b = new DummyPresolver(2);
        // Add both presolvers
        assertTrue(ExpressionsBasedModel.addPresolver(a));
        assertTrue(ExpressionsBasedModel.addPresolver(b));
        // Remove A – should succeed
        assertTrue(ExpressionsBasedModel.removePresolver(a), "First removal of presolver A should return true");
        // Remove A again – now absent, should return false
        assertFalse(ExpressionsBasedModel.removePresolver(a), "Second removal of presolver A should return false");
        // Remove B – should succeed
        assertTrue(ExpressionsBasedModel.removePresolver(b), "Removal of presolver B should return true");
        // Set must be empty now
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "All presolvers should have been removed");
    }

    @Test
    void testRemoveAfterClearAndAdd() {
        // clearPresolvers already executed in @BeforeEach, add a new one
        DummyPresolver e = new DummyPresolver(5);
        assertTrue(ExpressionsBasedModel.addPresolver(e), "Presolver E should be added after clear");
        // Removal must succeed
        assertTrue(ExpressionsBasedModel.removePresolver(e), "Removing presolver E after clear should return true");
    }

    @Test
    void testRemoveIncompatiblePresolverThrowsClassCastException() {
        // Add a normal DummyPresolver so the TreeSet has an element of a different class
        DummyPresolver dummy = new DummyPresolver(1);
        assertTrue(ExpressionsBasedModel.addPresolver(dummy));
        // Attempt to remove an IncompatiblePresolver – its compareTo throws CCE
        IncompatiblePresolver inc = new IncompatiblePresolver(2);
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.removePresolver(inc), "Removing an incompatible presolver should throw ClassCastException");
    }

    @Test
    void testRemoveIncompatiblePresolverWhenSetEmptyThrowsClassCastException() {
        // Ensure the set is empty (handled by @BeforeEach)
        IncompatiblePresolver inc = new IncompatiblePresolver(3);
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.removePresolver(inc), "Removing an incompatible presolver from an empty set should still throw ClassCastException");
    }

    @Test
    void testRemoveFaultyPresolverThrowsRuntimeException() {
        // Add a normal presolver so the TreeSet will attempt a comparison
        DummyPresolver dummy = new DummyPresolver(1);
        assertTrue(ExpressionsBasedModel.addPresolver(dummy));
        FaultyPresolver faulty = new FaultyPresolver(2);
        assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.removePresolver(faulty), "Removing a faulty presolver should propagate RuntimeException");
    }

    @Test
    void testRemoveFaultyPresolverWhenNotPresentThrowsRuntimeException() {
        // Set contains a DummyPresolver only
        DummyPresolver dummy = new DummyPresolver(1);
        assertTrue(ExpressionsBasedModel.addPresolver(dummy));
        // FaultyPresolver not present, but removal still triggers compareTo
        FaultyPresolver faulty = new FaultyPresolver(99);
        assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.removePresolver(faulty), "Removing a non‑present faulty presolver should still trigger RuntimeException");
    }
}
