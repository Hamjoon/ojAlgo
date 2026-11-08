package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_Test {

    private ExpressionsBasedModel model;

    // free variable, index 0
    private Variable varFree0;

    // fixed variable, index 1
    private Variable varFixed;

    // integer & free variable, index 2
    private Variable varIntFree;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // 0 – free variable (no bounds, not fixed)
        varFree0 = model.newVariable("free0").lower(// lower bound 0
        BigDecimal.ZERO).upper(// upper bound 10
        BigDecimal.valueOf(10)).weight(// contribution to objective (irrelevant here)
        BigDecimal.ONE);
        // 1 – fixed variable (lower == upper)
        varFixed = model.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // 2 – integer variable that is free (bounds allow movement)
        varIntFree = model.newVariable("intFree").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20)).integer(true).weight(BigDecimal.ONE);
    }

    /**
     * Creates a new {@link ExpressionsBasedModel} and adds variables according to
     * the supplied configurators. The order of configurators defines the global
     * index of each variable.
     */
    private static ExpressionsBasedModel buildModel(List<Consumer<Variable>> configurators) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        for (int i = 0; i < configurators.size(); i++) {
            Variable v = m.newVariable("v" + i);
            configurators.get(i).accept(v);
        }
        return m;
    }

    private static Stream<Arguments> mixedVariablesProvider() {
        return Stream.of(// first free variable
        Arguments.of(0, 0), // fixed variable
        Arguments.of(1, -1), // second free variable
        Arguments.of(2, 1));
    }

    /**
     * Verify that {@code indexOfFreeVariable(int)} returns the correct free‑variable
     * index for each global variable index and {@code -1} for non‑free variables.
     */
    @Test
    void testIndexOfFreeVariableMapping() {
        // Trigger categorisation once (optional, but mirrors typical usage)
        model.getFreeVariables();
        // Global index 0 → first free variable (should be 0)
        int idx0 = model.indexOfFreeVariable(0);
        assertEquals(0, idx0, "First free variable should map to free index 0");
        // Global index 1 → fixed variable (not free, should be -1)
        int idx1 = model.indexOfFreeVariable(1);
        assertEquals(-1, idx1, "Fixed variable should map to -1 (not free)");
        // Global index 2 → second free variable (integer but free, should be 1)
        int idx2 = model.indexOfFreeVariable(2);
        assertEquals(1, idx2, "Second free variable (integer) should map to free index 1");
    }

    /**
     * The free‑variable list returned by {@code getFreeVariables()} must be consistent
     * with the indices produced by {@code indexOfFreeVariable(int)}.
     */
    @Test
    void testFreeVariableListConsistency() {
        // Obtain the list of free variables directly from the model.
        java.util.List<Variable> freeVars = model.getFreeVariables();
        // The list should contain exactly the two free variables we added.
        assertEquals(2, freeVars.size(), "Model should have exactly two free variables");
        assertTrue(freeVars.contains(varFree0), "Free list must contain varFree0");
        assertTrue(freeVars.contains(varIntFree), "Free list must contain varIntFree");
        assertFalse(freeVars.contains(varFixed), "Fixed variable must not be in free list");
        // Verify that each free variable's global index maps to its position in the free list.
        for (int i = 0; i < model.countVariables(); i++) {
            int freeIdx = model.indexOfFreeVariable(i);
            if (freeIdx >= 0) {
                Variable globalVar = model.getVariable(i);
                assertSame(freeVars.get(freeIdx), globalVar, "Free index " + freeIdx + " should correspond to variable at global index " + i);
            }
        }
    }

    /**
     * Empty model – out of bounds access
     */
    @Test
    void testEmptyModelOutOfBounds() {
        // no variables
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> empty.indexOfFreeVariable(0), "Calling indexOfFreeVariable on an empty model must throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Single free variable – basic mapping
     */
    @Test
    void testSingleFreeVariableBasicMapping() {
        ExpressionsBasedModel m = buildModel(List.of(v -> {
            // no bounds, not fixed → free
        }));
        assertEquals(0, m.indexOfFreeVariable(0), "A single free variable should map to free index 0");
    }

    /**
     * Fixed variable – should return -1
     */
    @Test
    void testFixedVariableReturnsMinusOne() {
        ExpressionsBasedModel m = buildModel(List.of(v -> v.fix(BigDecimal.ONE)));
        assertEquals(-1, m.indexOfFreeVariable(0), "A fixed variable must return -1 as it is not free");
    }

    /**
     * Variable with negative upper limit – still free
     */
    @Test
    void testVariableWithNegativeUpperLimitStillFree() {
        ExpressionsBasedModel m = buildModel(List.of(v -> v.upper(BigDecimal.valueOf(-5))));
        assertEquals(0, m.indexOfFreeVariable(0), "Variable with negative upper limit is still free and should map to index 0");
    }

    /**
     * Two free variables – sequential mapping
     */
    @Test
    void testTwoFreeVariablesSequentialMapping() {
        ExpressionsBasedModel m = buildModel(List.of(v -> {
            /* free */
        }, v -> {
            /* free */
        }));
        assertAll(() -> assertEquals(0, m.indexOfFreeVariable(0), "First free variable → free index 0"), () -> assertEquals(1, m.indexOfFreeVariable(1), "Second free variable → free index 1"));
    }

    /**
     * Free variable after fixed – non‑contiguous mapping
     */
    @Test
    void testFreeVariableAfterFixedNonContiguousMapping() {
        ExpressionsBasedModel m = buildModel(List.of(v -> {
            /* free */
        }, // fixed
        v -> v.fix(BigDecimal.ONE), v -> {
            /* free */
        }));
        assertAll(() -> assertEquals(0, m.indexOfFreeVariable(0), "First free variable → free index 0"), () -> assertEquals(-1, m.indexOfFreeVariable(1), "Fixed variable → -1"), () -> assertEquals(1, m.indexOfFreeVariable(2), "Second free variable → free index 1"));
    }

    /**
     * Integer free variable – index present
     */
    @Test
    void testIntegerFreeVariableIndexPresent() {
        ExpressionsBasedModel m = buildModel(List.of(// integer but not fixed → free
        v -> v.integer(true)));
        assertEquals(0, m.indexOfFreeVariable(0), "Integer free variable should still be considered free and map to index 0");
    }

    /**
     * Fixed integer variable – should return -1
     */
    @Test
    void testFixedIntegerVariableReturnsMinusOne() {
        ExpressionsBasedModel m = buildModel(List.of(// integer and fixed
        v -> v.integer(true).fix(BigDecimal.ONE)));
        assertEquals(-1, m.indexOfFreeVariable(0), "Fixed integer variable must return -1");
    }

    /**
     * Out of bounds index beyond size – exception
     */
    @Test
    void testOutOfBoundsIndexBeyondSizeException() {
        ExpressionsBasedModel m = buildModel(List.of(v -> {
        }, v -> {
        }, // three variables
        v -> {
        }));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.indexOfFreeVariable(5), "Requesting an index beyond the number of variables must throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Negative globalIndex – exception
     */
    @Test
    void testNegativeGlobalIndexException() {
        ExpressionsBasedModel m = buildModel(List.of(v -> {
        }));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.indexOfFreeVariable(-1), "Negative global index must throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Stale cache after fixing variable – unchanged result
     */
    @Test
    void testStaleCacheAfterFixingVariable() {
        // Model with a single free variable.
        ExpressionsBasedModel m = buildModel(List.of(v -> {
        }));
        // First call populates the internal cache.
        int first = m.indexOfFreeVariable(0);
        assertEquals(0, first, "Initial free index must be 0");
        // Fix the variable without changing the model size.
        m.getVariable(0).fix(BigDecimal.ONE);
        // Second call should still return the cached value (0) because the cache
        // is refreshed only when the number of variables changes.
        int second = m.indexOfFreeVariable(0);
        assertEquals(0, second, "After fixing the variable the cached free index should remain 0 (cache not refreshed)");
    }

    /**
     * Parameterised test that reproduces the “Mixed variables – free, fixed, free”
     * group from the JSON specification.
     */
    @ParameterizedTest(name = "globalIndex={0} → expected={1}")
    @MethodSource("mixedVariablesProvider")
    void testMixedVariables(int globalIndex, int expected) {
        // Model: var0 free, var1 fixed, var2 free
        ExpressionsBasedModel m = buildModel(List.of(// free
        v -> {
        }, // fixed
        v -> v.fix(BigDecimal.ONE), // free
        v -> {
        }));
        assertEquals(expected, m.indexOfFreeVariable(globalIndex), "Mixed variables mapping failed for global index " + globalIndex);
    }
}