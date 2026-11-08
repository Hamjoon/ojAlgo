package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addPresolver_Test {

    /**
     * Ensure a clean presolver registry before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The class provides a public static method to clear the registry.
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore a clean state for any other tests that might run after this suite.
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Verify that {@code ExpressionsBasedModel.addPresolver(Presolver)} returns {@code true}
     * when a presolver is added for the first time and {@code false} when the same
     * presolver (or an equal one) is added again.
     */
    @Test
    void testAddPresolverReturnsTrueThenFalseOnDuplicate() throws Exception {
        // Create a simple concrete Presolver (the constructor is protected, but we are
        // in the same package so we can subclass it).
        Presolver testPresolver = new Presolver(1) {
        };
        // First addition – should succeed.
        boolean firstAddResult = ExpressionsBasedModel.addPresolver(testPresolver);
        assertTrue(firstAddResult, "First addition of a new presolver must return true");
        // Second addition of the same instance – should be rejected as duplicate.
        boolean secondAddResult = ExpressionsBasedModel.addPresolver(testPresolver);
        assertFalse(secondAddResult, "Adding the same presolver again must return false");
        // Use reflection to inspect the internal TreeSet size (optional sanity check).
        Field presolversField = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        presolversField.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeSet<Presolver> presolvers = (TreeSet<Presolver>) presolversField.get(null);
        assertEquals(1, presolvers.size(), "Exactly one presolver should be stored after duplicate add");
    }

    /**
     * Verify that different presolver instances (with different execution order)
     * are considered distinct and both can be added.
     */
    @Test
    void testAddMultipleDistinctPresolvers() throws Exception {
        Presolver presolverA = new Presolver(10) {
        };
        Presolver presolverB = new Presolver(20) {
        };
        assertTrue(ExpressionsBasedModel.addPresolver(presolverA), "First distinct presolver should be added");
        assertTrue(ExpressionsBasedModel.addPresolver(presolverB), "Second distinct presolver should be added");
        // Verify internal registry contains both.
        Field presolversField = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        presolversField.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeSet<Presolver> presolvers = (TreeSet<Presolver>) presolversField.get(null);
        assertEquals(2, presolvers.size(), "Two distinct presolvers should be stored");
    }

    /**
     * Adding {@code null} must result in a {@link NullPointerException}
     * because {@link TreeSet} does not accept null elements.
     */
    @Test
    void testAddPresolverNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.addPresolver(null), "addPresolver(null) must throw NullPointerException");
    }

    /**
     * After clearing the global presolver set, a previously added presolver can be added again.
     */
    @Test
    void testAddPresolverAfterClear() {
        Presolver presolver = new Presolver(10) {
        };
        // Add once – should succeed.
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "First addition should succeed");
        // Clear the global registry.
        ExpressionsBasedModel.clearPresolvers();
        // Add again – should succeed because the set is empty now.
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "Addition after clear should succeed");
    }

    /**
     * A presolver whose {@code compareTo} throws {@link ClassCastException}
     * should cause {@code addPresolver} to propagate that exception.
     */
    @Test
    void testAddPresolverNonComparableThrowsClassCastException() {
        Presolver badPresolver = new Presolver(0) {

            @Override
            public int compareTo(Presolver o) {
                throw new ClassCastException("Deliberate ClassCastException for testing");
            }
        };
        // Adding the first element works (TreeSet does not need to compare yet),
        // but adding a second element forces a comparison and triggers the exception.
        assertTrue(ExpressionsBasedModel.addPresolver(badPresolver), "First addition of a non‑comparable presolver should succeed");
        Presolver anotherPresolver = new Presolver(1) {
        };
        // The second addition must fail with ClassCastException.
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.addPresolver(anotherPresolver), "addPresolver should propagate ClassCastException from compareTo");
    }

    /**
     * A presolver whose {@code compareTo} throws {@link IllegalStateException}
     * should have that exception propagated by {@code addPresolver}.
     */
    @Test
    void testAddPresolverCompareToThrowsIllegalStateException() {
        Presolver faultyPresolver = new Presolver(5) {

            @Override
            public int compareTo(Presolver o) {
                throw new IllegalStateException("Deliberate IllegalStateException for testing");
            }
        };
        // Adding the first element succeeds (no comparison needed yet).
        assertTrue(ExpressionsBasedModel.addPresolver(faultyPresolver), "First addition of faulty presolver should succeed");
        Presolver anotherPresolver = new Presolver(6) {
        };
        // Adding a second element forces a comparison and should propagate IllegalStateException.
        assertThrows(IllegalStateException.class, () -> ExpressionsBasedModel.addPresolver(anotherPresolver), "addPresolver should propagate IllegalStateException from compareTo");
    }
}
