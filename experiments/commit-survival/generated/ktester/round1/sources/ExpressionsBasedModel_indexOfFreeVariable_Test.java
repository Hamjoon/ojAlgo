package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Stream;

class ExpressionsBasedModel_indexOfFreeVariable_Test {

    private ExpressionsBasedModel model;

    private Variable freeVar0;

    private Variable freeVar1;

    private Variable fixedVar;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Variable 0 – free (both lower and upper limits, not fixed)
        freeVar0 = model.newVariable("free0").lower(// lower bound 0
        BigDecimal.ZERO).upper(// upper bound 10
        BigDecimal.TEN).weight(// weight (irrelevant for this test)
        BigDecimal.ONE);
        // Variable 1 – free (only a lower bound, still not fixed)
        freeVar1 = model.newVariable("free1").lower(BigDecimal.ZERO).weight(BigDecimal.ONE);
        // Variable 2 – fixed (lower == upper via level)
        fixedVar = model.newVariable("fixed").level(// makes the variable fixed at 5
        BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        model = null;
        freeVar0 = null;
        freeVar1 = null;
        fixedVar = null;
    }

    @Test
    void testIndexOfFreeVariable_ForFreeVariables() {
        // Global indices are the order of insertion: 0, 1, 2
        int idxFree0 = model.indexOfFreeVariable(0);
        int idxFree1 = model.indexOfFreeVariable(1);
        // The first free variable should map to position 0 in the free‑variable list,
        // the second free variable to position 1.
        assertEquals(0, idxFree0, "First free variable should have free index 0");
        assertEquals(1, idxFree1, "Second free variable should have free index 1");
    }

    @Test
    void testIndexOfFreeVariable_ForFixedVariable() {
        // Fixed variable is at global index 2; it should not appear in the free list.
        int idxFixed = model.indexOfFreeVariable(2);
        assertEquals(-1, idxFixed, "Fixed variable should have free index -1");
    }

    @Test
    void testIndexOfFreeVariable_UsingVariableOverload() {
        // The overload that accepts a Variable delegates to the int version.
        int idxFromVariable0 = model.indexOfFreeVariable(freeVar0);
        int idxFromVariable1 = model.indexOfFreeVariable(freeVar1);
        int idxFromFixed = model.indexOfFreeVariable(fixedVar);
        assertEquals(0, idxFromVariable0, "Free variable 0 overload should return 0");
        assertEquals(1, idxFromVariable1, "Free variable 1 overload should return 1");
        assertEquals(-1, idxFromFixed, "Fixed variable overload should return -1");
    }

    @Test
    void testIndexOfFreeVariable_SingleFreeVariable() {
        // Model with a single free variable (no bounds, not fixed, not integer)
        ExpressionsBasedModel singleFree = new ExpressionsBasedModel();
        // default is free
        singleFree.newVariable("v0");
        int idx = singleFree.indexOfFreeVariable(0);
        assertEquals(0, idx, "Single free variable should map to free index 0");
    }

    @Test
    void testIndexOfFreeVariable_ThreeFreeVariables() {
        // Model with three free variables, all unbounded
        ExpressionsBasedModel threeFree = new ExpressionsBasedModel();
        threeFree.newVariable("v0");
        threeFree.newVariable("v1");
        threeFree.newVariable("v2");
        int idx = threeFree.indexOfFreeVariable(2);
        assertEquals(2, idx, "Third free variable should have free index 2");
    }

    @Test
    void testIndexOfFreeVariable_FixedVariable() {
        // Model contains a single fixed variable (level set)
        ExpressionsBasedModel fixedOnly = new ExpressionsBasedModel();
        fixedOnly.newVariable("fixed").level(BigDecimal.ONE);
        int idx = fixedOnly.indexOfFreeVariable(0);
        assertEquals(-1, idx, "Fixed variable should have free index -1");
    }

    @Test
    void testIndexOfFreeVariable_MixedFixedAndFree() {
        // V0 fixed, V1 free, V2 fixed, V3 free, V4 free
        ExpressionsBasedModel mixed = new ExpressionsBasedModel();
        // fixed
        mixed.newVariable("v0").level(BigDecimal.ZERO);
        // free
        mixed.newVariable("v1");
        // fixed
        mixed.newVariable("v2").level(BigDecimal.ONE);
        // free
        mixed.newVariable("v3");
        // free
        mixed.newVariable("v4");
        // Expected mapping according to the JSON description
        assertEquals(-1, mixed.indexOfFreeVariable(0), "V0 is fixed → -1");
        assertEquals(0, mixed.indexOfFreeVariable(1), "V1 is first free → 0");
        assertEquals(-1, mixed.indexOfFreeVariable(2), "V2 is fixed → -1");
        assertEquals(1, mixed.indexOfFreeVariable(3), "V3 is second free → 1");
        assertEquals(2, mixed.indexOfFreeVariable(4), "V4 is third free → 2");
    }

    @Test
    void testIndexOfFreeVariable_IntegerVariableIsFree() {
        // Single integer variable, not fixed, no bounds
        ExpressionsBasedModel integerModel = new ExpressionsBasedModel();
        integerModel.newVariable("intVar").integer(true);
        int idx = integerModel.indexOfFreeVariable(0);
        assertEquals(0, idx, "Integer variable should still receive free index 0");
    }

    @Test
    void testIndexOfFreeVariable_OutOfRangeIndex() {
        // Model with three variables (indices 0‑2)
        ExpressionsBasedModel threeVars = new ExpressionsBasedModel();
        threeVars.newVariable("v0");
        threeVars.newVariable("v1");
        threeVars.newVariable("v2");
        assertThrows(IndexOutOfBoundsException.class, () -> threeVars.indexOfFreeVariable(5), "Accessing index 5 should throw IndexOutOfBoundsException");
    }

    @Test
    void testIndexOfFreeVariable_NegativeIndex() {
        // Model with the standard three variables from @BeforeEach
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(-1), "Negative index -1 should throw IndexOutOfBoundsException");
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(-5), "Negative index -5 should throw IndexOutOfBoundsException");
    }

    @Test
    void testIndexOfFreeVariable_EmptyModel() {
        // no variables added
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.indexOfFreeVariable(0), "Calling on an empty model should throw IndexOutOfBoundsException");
    }

    @Test
    void testIndexOfFreeVariable_DynamicUpdateAfterAddingVariable() {
        // Start with three free variables
        ExpressionsBasedModel model3 = new ExpressionsBasedModel();
        model3.newVariable("v0");
        model3.newVariable("v1");
        model3.newVariable("v2");
        // First call triggers the internal update
        assertEquals(2, model3.indexOfFreeVariable(2), "Initial third free variable should be index 2");
        // Add a fourth free variable
        model3.newVariable("v3");
        // The mapping should now contain the new variable at free index 3
        assertEquals(3, model3.indexOfFreeVariable(3), "After adding a new free variable, its free index should be 3");
    }
}
