package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure1D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Variable 0 – free (default bounds)
        model.addVariable("free0");
        // Variable 1 – fixed (lower == upper)
        model.addVariable("fixed1").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        // Variable 2 – free
        model.addVariable("free2").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Variable 3 – integer but still free (integer flag does not affect free‑variable list)
        model.addVariable("int3").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).integer(true);
        // No further re‑initialisation – keep the variables added above
    }

    @AfterEach
    void teardownAfterEach() {
        if (model != null) {
            model.dispose();
            model = null;
        }
    }

    /**
     * Verify that the index returned for a free variable matches its position
     * in {@code getFreeVariables()}.
     */
    @Test
    void testIndexOfFreeVariableMatchesFreeVariableList() {
        List<Variable> freeVars = model.getFreeVariables();
        // According to ojAlgo semantics, integer variables are still considered free,
        // therefore the free list should contain free0, free2, int3.
        assertEquals(3, freeVars.size(), "Unexpected number of free variables");
        for (int i = 0; i < freeVars.size(); ++i) {
            Variable var = freeVars.get(i);
            int globalIdx = model.indexOf(var);
            Structure1D.IntIndex intIdx = model.toIntIndex(globalIdx);
            int freeIdx = model.indexOfFreeVariable(intIdx);
            assertEquals(i, freeIdx, () -> "Free variable '" + var.getName() + "' expected free index " + i + " but got " + freeIdx);
        }
    }

    /**
     * Ensure that a fixed variable is **not** considered a free variable.
     * The method should return {@code -1} for an index that does not belong to the free set.
     */
    @Test
    void testFixedVariableReturnsMinusOne() {
        // Fixed variable is the second one added (global index 1)
        Variable fixed = model.getVariable(1);
        assertTrue(fixed.isFixed(), "Variable should be fixed");
        IntIndex fixedIdx = model.toIntIndex(model.indexOf(fixed));
        int freeIdx = model.indexOfFreeVariable(fixedIdx);
        // The contract of the method is to return -1 when the variable is not free
        assertEquals(-1, freeIdx, "Fixed variable should yield -1 for free index");
    }

    /**
     * Check that the method works correctly when the model contains no variables.
     */
    @Test
    void testEmptyModelReturnsMinusOne() {
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        try {
            IntIndex anyIdx = new IntIndex(0);
            int result = emptyModel.indexOfFreeVariable(anyIdx);
            assertEquals(-1, result, "Empty model should always return -1");
        } finally {
            emptyModel.dispose();
        }
    }

    /**
     * Verify that the method correctly maps an {@link IntIndex} created via
     * {@link Structure1D.IntIndex#of(int)} as well as one created directly.
     */
    @Test
    void testIntIndexVariations() {
        // Choose a known free variable (global index 0 -> first free variable)
        int globalIdx = 0;
        Variable var = model.getVariable(globalIdx);
        assertNotNull(var);
        // Using the factory method
        IntIndex idxFactory = Structure1D.IntIndex.of(globalIdx);
        int freeIdxFactory = model.indexOfFreeVariable(idxFactory);
        assertEquals(0, freeIdxFactory);
        // Using the constructor directly
        IntIndex idxCtor = new IntIndex(globalIdx);
        int freeIdxCtor = model.indexOfFreeVariable(idxCtor);
        assertEquals(0, freeIdxCtor);
    }

    /* --------------------------------------------------------------------- *
     *  1. Null handling
     * --------------------------------------------------------------------- */
    @Test
    void testNullIntIndexThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> model.indexOfFreeVariable((Structure1D.IntIndex) null), "Calling with a null IntIndex must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  2. Simple free variable
     * --------------------------------------------------------------------- */
    @Test
    void testValidFreeVariableIndex() {
        // index 0, free
        model.addVariable("free0");
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(0);
        int freeIdx = model.indexOfFreeVariable(idx);
        assertEquals(0, freeIdx, "The first free variable should map to free index 0");
    }

    /* --------------------------------------------------------------------- *
     *  3. Fixed variable (non‑free)
     * --------------------------------------------------------------------- */
    @Test
    void testValidNonFreeVariableIndex() {
        // a fixed variable (lower == upper)
        model.addVariable("fixed0").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(0);
        int freeIdx = model.indexOfFreeVariable(idx);
        assertEquals(-1, freeIdx, "A fixed variable must return -1");
    }

    /* --------------------------------------------------------------------- *
     *  4. Negative index handling
     * --------------------------------------------------------------------- */
    @Test
    void testNegativeIndexThrowsIndexOutOfBoundsException() {
        model.addVariable("v0");
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(-1);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(idx), "Negative global index should cause IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  5. Index beyond variable count
     * --------------------------------------------------------------------- */
    @Test
    void testIndexBeyondVariableCountThrowsIndexOutOfBoundsException() {
        model.addVariable("v0");
        model.addVariable("v1");
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(1000);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(idx), "Index far beyond variable count must throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  6. Mixed free / integer variables
     * --------------------------------------------------------------------- */
    @Test
    void testFreeVariableMappingWithMixedVariableTypes() {
        // 0 – free, 1 – integer, 2 – free
        model.addVariable("free0");
        model.addVariable("int1").integer(true);
        model.addVariable("free2");
        // global index 2 should be the second free variable (free index = 1)
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(2);
        int freeIdx = model.indexOfFreeVariable(idx);
        assertEquals(1, freeIdx, "Global index 2 should map to free index 1");
    }

    /* --------------------------------------------------------------------- *
     *  8. Integer variable returns -1
     * --------------------------------------------------------------------- */
    @Test
    void testIntegerVariableReturnsMinusOne() {
        // single integer variable at index 0
        model.addVariable("int0").integer(true);
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(0);
        int freeIdx = model.indexOfFreeVariable(idx);
        assertEquals(-1, freeIdx, "Integer variable must yield -1");
    }

    /* --------------------------------------------------------------------- *
     *  9. Model with no variables
     * --------------------------------------------------------------------- */
    @Test
    void testModelWithNoVariablesThrowsIndexOutOfBoundsException() {
        // model is empty
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(0);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(idx), "Empty model should throw IndexOutOfBoundsException for any index");
    }

    /* --------------------------------------------------------------------- *
     * 10. Last free variable in a larger mixed model
     * --------------------------------------------------------------------- */
    @Test
    void testIntIndexReferencingLastFreeVariable() {
        // 0 – free, 1 – integer, 2 – free, 3 – fixed, 4 – free
        model.addVariable("free0");
        model.addVariable("int1").integer(true);
        model.addVariable("free2");
        model.addVariable("fixed3").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        model.addVariable("free4");
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(4);
        int freeIdx = model.indexOfFreeVariable(idx);
        assertEquals(2, freeIdx, "Global index 4 should map to free index 2");
    }

    /* --------------------------------------------------------------------- *
     * 11. Repeated calls are idempotent
     * --------------------------------------------------------------------- */
    @Test
    void testRepeatedCallsAreIdempotent() {
        model.addVariable("free0");
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(0);
        int first = model.indexOfFreeVariable(idx);
        int second = model.indexOfFreeVariable(idx);
        assertEquals(first, second, "Repeated calls must return the same free index");
        assertEquals(0, first, "The free index for the first variable should be 0");
    }

    /* --------------------------------------------------------------------- *
     * 12. Boundary: index equals free‑variable count
     * --------------------------------------------------------------------- */
    @Test
    void testVariableIndexEqualsFreeVariableCountBoundary() {
        // three free variables: indices 0,1,2
        model.addVariable("free0");
        model.addVariable("free1");
        model.addVariable("free2");
        // global index 3 is the first non‑free (does not exist) – expect -1
        Structure1D.IntIndex idx = Structure1D.IntIndex.of(3);
        int freeIdx = model.indexOfFreeVariable(idx);
        assertEquals(-1, freeIdx, "Index equal to free‑variable count must return -1");
    }
}
