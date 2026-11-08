package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_isInteger_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
        // create a handful of variables (indices 0 … 7) and configure their integer flag
        // 0,2,3,4,5,7 -> integer, 1,6 -> non‑integer
        for (int i = 0; i < 8; i++) {
            Variable v = model.addVariable();
            boolean integer = (i == 0 || i == 2 || i == 3 || i == 4 || i == 5 || i == 7);
            v.integer(integer);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Supplies the normal (non‑exception) test cases.
     */
    private static Stream<Arguments> provideNormalCases() {
        // Helper to build a set from an array of int indices
        java.util.function.Function<int[], Set<IntIndex>> setFrom = indices -> {
            Set<IntIndex> set = new HashSet<>();
            for (int idx : indices) {
                set.add(IntIndex.of(idx));
            }
            return set;
        };
        return Stream.of(// [0] -> true (single integer variable)
        Arguments.of(setFrom.apply(new int[] { 0 }), true, "Single integer variable should return true"), // [0,1] -> false (second variable is non‑integer)
        Arguments.of(setFrom.apply(new int[] { 0, 1 }), false, "Mixed integer / non‑integer set should return false"), // [2,3,4] -> true (all three are integer)
        Arguments.of(setFrom.apply(new int[] { 2, 3, 4 }), true, "All‑integer larger set should return true"), // [5,6,7] -> false (index 6 is non‑integer)
        Arguments.of(setFrom.apply(new int[] { 5, 6, 7 }), false, "Larger set with a non‑integer entry should return false"));
    }

    /**
     * Empty set must return {@code false}.
     */
    @Test
    void testIsIntegerEmptySet() {
        Set<IntIndex> empty = Collections.emptySet();
        assertFalse(model.isInteger(empty), "Empty set should not be considered integer");
    }

    /**
     * A set that contains only integer variables must return {@code true}.
     */
    @Test
    void testIsIntegerAllIntegerVariables() {
        // create an integer variable
        Variable v = model.addVariable();
        // mark as integer
        v.integer(true);
        // build the set containing the variable's index
        Set<IntIndex> set = new HashSet<>();
        set.add(IntIndex.of(model.indexOf(v)));
        assertTrue(model.isInteger(set), "Set with only integer variables should be integer");
    }

    /**
     * A set that contains a non‑integer variable must return {@code false}.
     */
    @Test
    void testIsIntegerContainsNonInteger() {
        // integer variable
        Variable intVar = model.addVariable();
        intVar.integer(true);
        // non‑integer variable (default)
        Variable nonIntVar = model.addVariable();
        // set contains both indices
        Set<IntIndex> mixed = new HashSet<>();
        mixed.add(IntIndex.of(model.indexOf(intVar)));
        mixed.add(IntIndex.of(model.indexOf(nonIntVar)));
        assertFalse(model.isInteger(mixed), "Set containing a non‑integer variable must be false");
    }

    /**
     * A set that contains only non‑integer variables must return {@code false}.
     */
    @Test
    void testIsIntegerAllNonIntegerVariables() {
        // default non‑integer
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Set<IntIndex> set = new HashSet<>();
        set.add(IntIndex.of(model.indexOf(v1)));
        set.add(IntIndex.of(model.indexOf(v2)));
        assertFalse(model.isInteger(set), "Set with only non‑integer variables should be false");
    }

    /**
     * Parameterized test covering the normal (non‑exceptional) scenarios described in the
     * JSON test matrix.
     *
     * Each argument consists of:
     * <ul>
     *   <li>a {@code Set<IntIndex>} to pass to {@code isInteger}</li>
     *   <li>the expected boolean result</li>
     *   <li>a textual description (used only for readability)</li>
     * </ul>
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideNormalCases")
    void testIsIntegerNormalCases(Set<IntIndex> variables, boolean expected, String description) {
        assertEquals(expected, model.isInteger(variables), description);
    }

    /**
     * Passing a {@code null} reference must throw {@link NullPointerException}.
     */
    @Test
    void testIsIntegerNullSet() {
        assertThrows(NullPointerException.class, () -> model.isInteger(null), "Calling isInteger with a null set should throw NullPointerException");
    }

    /**
     * A set containing a {@code null} element must throw {@link NullPointerException}.
     */
    @Test
    void testIsIntegerSetWithNullElement() {
        Set<IntIndex> set = new HashSet<>();
        // deliberately insert a null element
        set.add(null);
        assertThrows(NullPointerException.class, () -> model.isInteger(set), "Set containing a null element should cause NullPointerException");
    }

    /**
     * An index that exceeds the number of defined variables must throw {@link IndexOutOfBoundsException}.
     */
    @Test
    void testIsIntegerIndexOutOfBoundsPositive() {
        // The model only has variables 0‑7; using 10 should be out of bounds.
        Set<IntIndex> set = new HashSet<>();
        set.add(IntIndex.of(10));
        assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(set), "Index greater than defined variables should throw IndexOutOfBoundsException");
    }

    /**
     * A negative index must throw {@link IndexOutOfBoundsException}.
     */
    @Test
    void testIsIntegerNegativeIndex() {
        Set<IntIndex> set = new HashSet<>();
        set.add(IntIndex.of(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(set), "Negative index should throw IndexOutOfBoundsException");
    }
}
