package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addPresolver_Test {

    /**
     * Simple concrete presolver used only for testing.
     */
    private static final class DummyPresolver extends ExpressionsBasedModel.Presolver {

        DummyPresolver(final int executionOrder) {
            super(executionOrder);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean static state before each test.
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test as a safety net.
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Supplies different execution orders for the parameterized test.
     */
    private static Stream<Arguments> presolverProvider() {
        return Stream.of(Arguments.of(10), Arguments.of(20), Arguments.of(30));
    }

    @Test
    void testAddNewPresolverReturnsTrue() {
        DummyPresolver presolver = new DummyPresolver(1);
        boolean added = ExpressionsBasedModel.addPresolver(presolver);
        assertTrue(added, "First addition of a presolver should return true");
        // Verify that the presolver is actually stored in the static set.
        Set<Presolver> stored = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(1, stored.size(), "Exactly one presolver should be stored");
        assertTrue(stored.contains(presolver), "Stored set should contain the added presolver");
    }

    @Test
    void testAddDuplicatePresolverReturnsFalse() {
        DummyPresolver presolver = new DummyPresolver(2);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "First addition must succeed");
        // Adding the same instance again should be rejected (TreeSet uses compareTo, but the same
        // execution order makes it equal, therefore the set will not change and the method returns false).
        boolean secondAdd = ExpressionsBasedModel.addPresolver(presolver);
        assertFalse(secondAdd, "Adding the same presolver again should return false");
        // The set must still contain only one element.
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size(), "Duplicate addition must not increase the size of the presolver set");
    }

    @Test
    void testAddDifferentPresolversBothSucceed() {
        DummyPresolver p1 = new DummyPresolver(3);
        DummyPresolver p2 = new DummyPresolver(4);
        assertTrue(ExpressionsBasedModel.addPresolver(p1), "First presolver should be added");
        assertTrue(ExpressionsBasedModel.addPresolver(p2), "Second presolver with different order should be added");
        assertEquals(2, ExpressionsBasedModel.PRESOLVERS.size(), "Both distinct presolvers should be present in the set");
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(p1));
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(p2));
    }

    @Test
    void testClearPresolversEmptiesSet() {
        // Populate the set first.
        ExpressionsBasedModel.addPresolver(new DummyPresolver(5));
        ExpressionsBasedModel.addPresolver(new DummyPresolver(6));
        assertFalse(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Set should not be empty after additions");
        // Now clear it.
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Set must be empty after clearPresolvers()");
    }

    /**
     * JSON test case: "addPresolver - successful addition".
     *
     * <p>
     * Adding a new comparable {@code Presolver} instance should succeed and return {@code true}.
     * The static {@code PRESOLVERS} set is cleared before the test (handled by {@code @BeforeEach}).
     * </p>
     */
    @Test
    void testAddPresolverSuccessfulAddition() {
        DummyPresolver presolver = new DummyPresolver(1);
        boolean added = ExpressionsBasedModel.addPresolver(presolver);
        assertTrue(added, "Adding a new presolver should return true");
        Set<Presolver> stored = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(1, stored.size(), "Exactly one presolver should be stored");
        assertTrue(stored.contains(presolver), "Stored set should contain the added presolver");
    }

    /**
     * JSON test case: "addPresolver - null argument".
     *
     * <p>
     * Passing {@code null} to {@code addPresolver} must throw a {@link NullPointerException}
     * because {@code TreeSet} does not accept {@code null} elements.
     * </p>
     */
    @Test
    void testAddPresolverNullArgument() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.addPresolver(null), "Calling addPresolver(null) should throw NullPointerException");
    }

    /**
     * Parameterized version of the successful addition test to demonstrate handling of
     * multiple inputs (different execution orders). This complements the explicit test above.
     */
    @ParameterizedTest(name = "addPresolver with executionOrder={0} should return true")
    @MethodSource("presolverProvider")
    void testAddPresolverParameterized(int executionOrder) {
        DummyPresolver presolver = new DummyPresolver(executionOrder);
        boolean added = ExpressionsBasedModel.addPresolver(presolver);
        assertTrue(added, "Adding a new presolver should return true");
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(presolver), "Set must contain the newly added presolver");
    }
}