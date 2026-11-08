package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class ExpressionsBasedModel_indexOfNegativeVariable_Test {

    private ExpressionsBasedModel model;

    // negative because lower bound is negative
    private Variable vNegWithBounds;

    // not negative (lower bound = 0)
    private Variable vPosOnly;

    // negative because lower bound is not set
    private Variable vNegNoLower;

    /**
     * Helper holder describing how a single variable should be configured.
     */
    private static class VariableSetup {

        // null → not set
        final Number lower;

        // true → call fix()
        final boolean fixed;

        // true → mark as integer
        final boolean integer;

        // true → only upper bound is set
        final boolean noLowerSet;

        VariableSetup(Number lower, boolean fixed, boolean integer, boolean noLowerSet) {
            this.lower = lower;
            this.fixed = fixed;
            this.integer = integer;
            this.noLowerSet = noLowerSet;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // 1st variable: lower = -5, upper = 5  → negative (lower < 0) and positive (upper > 0)
        vNegWithBounds = model.newVariable("vNegWithBounds").lower(-5).upper(5).weight(1);
        // 2nd variable: lower = 0, upper = 10 → not negative (lower ≥ 0)
        vPosOnly = model.newVariable("vPosOnly").lower(0).upper(10).weight(1);
        // 3rd variable: no lower bound, upper = 5 → negative because lower bound is not set
        vNegNoLower = model.newVariable("vNegNoLower").upper(5).weight(1);
    }

    /**
     * Provides a stream of arguments for the parameterised test that checks
     * the behaviour of {@code indexOfNegativeVariable(int)} for a variety of
     * single‑variable models.
     *
     * Each entry corresponds to one of the JSON cases where the global index
     * is {@code 0}.  The {@code description} field is used as the test name.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> singleVariableCases() {
        return Stream.of(// 1) empty model – already covered by a dedicated test, but kept for completeness
        Arguments.of("empty", new VariableSetup(null, false, false, false), "IndexOutOfBoundsException"), // 2) fixed variable
        Arguments.of("fixed", new VariableSetup(null, true, false, false), "-1"), // 3) no lower bound (negative by definition)
        Arguments.of("noLower", new VariableSetup(null, false, false, true), "0"), // 4) negative lower bound
        Arguments.of("negLower", new VariableSetup(-5, false, false, false), "0"), // 5) positive lower bound
        Arguments.of("posLower", new VariableSetup(5, false, false, false), "-1"), // 6) zero lower bound
        Arguments.of("zeroLower", new VariableSetup(0, false, false, false), "-1"));
    }

    @Test
    void testIndexOfNegativeVariable_byGlobalIndex() {
        // Global indices are assigned in the order of creation: 0, 1, 2
        assertEquals(0, model.indexOfNegativeVariable(0), "First variable should be the first negative variable (index 0)");
        assertEquals(-1, model.indexOfNegativeVariable(1), "Second variable is not negative, should return -1");
        assertEquals(1, model.indexOfNegativeVariable(2), "Third variable should be the second negative variable (index 1)");
    }

    @Test
    void testIndexOfNegativeVariable_byVariable() {
        assertEquals(0, model.indexOfNegativeVariable(vNegWithBounds), "Variable vNegWithBounds should map to negative index 0");
        assertEquals(-1, model.indexOfNegativeVariable(vPosOnly), "Variable vPosOnly is not negative, should map to -1");
        assertEquals(1, model.indexOfNegativeVariable(vNegNoLower), "Variable vNegNoLower should map to negative index 1");
    }

    @Test
    void testIndexOfNegativeVariable_byIntIndex() {
        assertEquals(0, model.indexOfNegativeVariable(IntIndex.of(0)), "IntIndex 0 should map to negative index 0");
        assertEquals(-1, model.indexOfNegativeVariable(IntIndex.of(1)), "IntIndex 1 should map to -1 (not negative)");
        assertEquals(1, model.indexOfNegativeVariable(IntIndex.of(2)), "IntIndex 2 should map to negative index 1");
    }

    @Test
    void testEmptyModelThrowsIndexOutOfBoundsException() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.indexOfNegativeVariable(0), "Calling indexOfNegativeVariable on an empty model should throw IndexOutOfBoundsException");
    }

    @Test
    void testFixedVariableIsNotNegative() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // A fixed variable (lower == upper) – set both bounds to the same value.
        Variable fixed = m.newVariable("fixed").lower(3).upper(3);
        // No need for an explicit fix() call; the bounds already make it fixed.
        assertEquals(-1, m.indexOfNegativeVariable(0), "A fixed variable must never appear in the negative list");
    }

    @Test
    void testVariableWithoutLowerBoundIsNegative() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.newVariable("noLower").upper(5);
        assertEquals(0, m.indexOfNegativeVariable(0), "Variable with no lower bound is treated as negative and should obtain index 0");
    }

    @Test
    void testVariableWithNegativeLowerBoundIsNegative() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.newVariable("negLower").lower(-4);
        assertEquals(0, m.indexOfNegativeVariable(0), "Variable whose lower bound is negative must be in the negative list");
    }

    @Test
    void testVariableWithPositiveLowerBoundIsNotNegative() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.newVariable("posLower").lower(2);
        assertEquals(-1, m.indexOfNegativeVariable(0), "Variable with a positive lower bound must not be considered negative");
    }

    @Test
    void testVariableWithZeroLowerBoundIsNotNegative() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.newVariable("zeroLower").lower(0);
        assertEquals(-1, m.indexOfNegativeVariable(0), "Variable whose lower bound is exactly zero is not negative");
    }

    @Test
    void testOutOfRangeGlobalIndexThrows() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // create three variables just to have a non‑empty model
        m.newVariable("v0").lower(-1);
        m.newVariable("v1").lower(-1);
        m.newVariable("v2").lower(-1);
        assertThrows(IndexOutOfBoundsException.class, () -> m.indexOfNegativeVariable(5), "Requesting a negative index beyond the number of variables must throw IndexOutOfBoundsException");
    }

    @Test
    void testNegativeGlobalIndexThrows() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.newVariable("v0").lower(-1);
        assertThrows(IndexOutOfBoundsException.class, () -> m.indexOfNegativeVariable(-1), "Negative global indices must result in IndexOutOfBoundsException");
    }

    @Test
    void testIntegerVariableWithNegativeLowerBoundIsNegative() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.newVariable("intNeg").integer(true).lower(-3);
        assertEquals(0, m.indexOfNegativeVariable(0), "Integer variable with a negative lower bound must appear in the negative list");
    }

    @Test
    void testDynamicPropertyChangeAfterFirstCallIsCached() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // no lower bound → negative
        Variable v = m.newVariable("dynamic").upper(5);
        // first call – cache is built
        assertEquals(0, m.indexOfNegativeVariable(0), "First call should treat the variable as negative and return index 0");
        // change the lower bound to a positive value *after* the cache has been built
        v.lower(10);
        // second call – should return the previously cached value because the categorisation
        // is not refreshed automatically
        assertEquals(0, m.indexOfNegativeVariable(0), "After changing the bound, the method must still return the cached index 0");
    }

    @Test
    void testLargeModelWithAlternatingBounds() {
        final int SIZE = 100;
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // even indices → negative lower bound, odd indices → positive lower bound
        for (int i = 0; i < SIZE; i++) {
            Variable v = m.newVariable("v" + i);
            if (i % 2 == 0) {
                // negative
                v.lower(-1);
            } else {
                // positive
                v.lower(1);
            }
        }
        // index 0 (even) → first negative variable
        assertEquals(0, m.indexOfNegativeVariable(0), "Variable 0 (even) should be negative and have negative index 0");
        // index 1 (odd) → not negative
        assertEquals(-1, m.indexOfNegativeVariable(1), "Variable 1 (odd) is positive and must return -1");
        // index 98 (even) → 49th negative variable (0‑based)
        assertEquals(49, m.indexOfNegativeVariable(98), "Variable 98 (even) should be the 50th negative variable → index 49");
        // index 99 (odd) → not negative
        assertEquals(-1, m.indexOfNegativeVariable(99), "Variable 99 (odd) is positive and must return -1");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleVariableCases")
    void testSingleVariableScenarios(String description, VariableSetup setup, String expected) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        if (!"empty".equals(description)) {
            Variable v = m.newVariable("v");
            if (setup.noLowerSet) {
                v.upper(5);
            } else {
                if (setup.lower != null) {
                    // lower expects a primitive, use double conversion
                    v.lower(setup.lower.doubleValue());
                }
                v.upper(5);
            }
            if (setup.integer) {
                v.integer(true);
            }
            if (setup.fixed) {
                // fixing the variable: set lower and upper to the same value (zero is fine)
                v.level(0);
            }
        }
        if ("IndexOutOfBoundsException".equals(expected)) {
            assertThrows(IndexOutOfBoundsException.class, () -> m.indexOfNegativeVariable(0), description);
        } else {
            int expectedInt = Integer.parseInt(expected);
            assertEquals(expectedInt, m.indexOfNegativeVariable(0), description);
        }
    }
}
