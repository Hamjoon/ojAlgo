package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_Test {

    /**
     * The model used in each test – created fresh for every test method.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a clean model instance.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly necessary but keeps the pattern tidy.
        model = null;
    }

    /**
     * A variable that is not fixed should obtain a non‑negative free‑variable index.
     * The first free variable added to a fresh model must be mapped to index {@code 0}.
     */
    @Test
    void testIndexOfFreeVariable_ForFirstFreeVariable() {
        // make it free (bounds do not fix the variable)
        ExpressionsBasedModel.Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        int globalIdx = model.indexOf(x);
        int freeIdx = model.indexOfFreeVariable(globalIdx);
        assertEquals(0, freeIdx, "The first free variable should have free‑variable index 0");
    }

    /**
     * Adding several free variables should give consecutive free‑variable indices
     * reflecting the order in which they were added.
     */
    @Test
    void testIndexOfFreeVariable_ForMultipleFreeVariables() {
        ExpressionsBasedModel.Variable a = model.newVariable("a").lower(0).upper(5).weight(1);
        ExpressionsBasedModel.Variable b = model.newVariable("b").lower(-2).upper(8).weight(1);
        ExpressionsBasedModel.Variable c = model.newVariable("c").lower(0).upper(10).weight(1);
        // Verify the mapping for each variable
        assertEquals(0, model.indexOfFreeVariable(model.indexOf(a)), "Variable a should be the first free variable");
        assertEquals(1, model.indexOfFreeVariable(model.indexOf(b)), "Variable b should be the second free variable");
        assertEquals(2, model.indexOfFreeVariable(model.indexOf(c)), "Variable c should be the third free variable");
    }

    /**
     * A variable whose lower and upper limits are identical (i.e. fixed) must not appear
     * in the free‑variable list and therefore its free index should be {@code -1}.
     */
    @Test
    void testIndexOfFreeVariable_ForFixedVariable() {
        // sets both lower and upper to 5 → fixed
        ExpressionsBasedModel.Variable fixed = model.newVariable("fixed").level(5).weight(1);
        int globalIdx = model.indexOf(fixed);
        int freeIdx = model.indexOfFreeVariable(globalIdx);
        assertEquals(-1, freeIdx, "A fixed variable must have a free‑variable index of -1");
    }

    /**
     * Mix free and fixed variables and ensure that only the free ones receive
     * non‑negative indices while the fixed one returns {@code -1}.
     */
    @Test
    void testIndexOfFreeVariable_MixedFreeAndFixed() {
        ExpressionsBasedModel.Variable free1 = model.newVariable("free1").lower(0).upper(3).weight(1);
        ExpressionsBasedModel.Variable fixed = model.newVariable("fixed").level(7).weight(1);
        ExpressionsBasedModel.Variable free2 = model.newVariable("free2").lower(-5).upper(5).weight(1);
        // Expected free indices: free1 → 0, free2 → 1, fixed → -1
        assertEquals(0, model.indexOfFreeVariable(model.indexOf(free1)), "First free variable should map to index 0");
        assertEquals(-1, model.indexOfFreeVariable(model.indexOf(fixed)), "Fixed variable should map to index -1");
        assertEquals(1, model.indexOfFreeVariable(model.indexOf(free2)), "Second free variable should map to index 1");
    }

    /* --------------------------------------------------------------
     *  Group: Empty model – expect exception
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_EmptyModel_ThrowsException() {
        // The model contains no variables; any request must fail.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(0), "Calling indexOfFreeVariable on an empty model should throw ArrayIndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------
     *  Group: Single free variable
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_SingleFreeVariable() {
        // make it free (bounds do not fix the variable)
        ExpressionsBasedModel.Variable v = model.newVariable("v").lower(0).upper(10).weight(1);
        int globalIdx = model.indexOf(v);
        int freeIdx = model.indexOfFreeVariable(globalIdx);
        assertEquals(0, freeIdx, "A model with one free variable should map global index 0 to free index 0");
    }

    /* --------------------------------------------------------------
     *  Group: Mixed fixed and free variables
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_MixedFixedAndFree() {
        // both lower and upper set to 5 → fixed
        ExpressionsBasedModel.Variable fixed = model.newVariable("fixed").level(5).weight(1);
        ExpressionsBasedModel.Variable free = model.newVariable("free").lower(0).upper(10).weight(1);
        // Fixed variable → -1
        assertEquals(-1, model.indexOfFreeVariable(model.indexOf(fixed)), "Fixed variable must have free‑variable index -1");
        // Free variable → 0 (first free variable)
        assertEquals(0, model.indexOfFreeVariable(model.indexOf(free)), "First free variable should have free‑variable index 0");
    }

    /* --------------------------------------------------------------
     *  Group: Multiple free variables with a trailing fixed variable
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_MultipleFreeWithTrailingFixed() {
        // Two free variables followed by a fixed one.
        ExpressionsBasedModel.Variable v1 = model.newVariable("v1").lower(0).upper(5).weight(1);
        ExpressionsBasedModel.Variable v2 = model.newVariable("v2").lower(-2).upper(8).weight(1);
        ExpressionsBasedModel.Variable fixed = model.newVariable("fixed").level(7).weight(1);
        assertEquals(0, model.indexOfFreeVariable(model.indexOf(v1)), "First free variable should map to free index 0");
        assertEquals(1, model.indexOfFreeVariable(model.indexOf(v2)), "Second free variable should map to free index 1");
        assertEquals(-1, model.indexOfFreeVariable(model.indexOf(fixed)), "Fixed variable should map to free index -1");
    }

    /* --------------------------------------------------------------
     *  Group: Free integer variable
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_FreeIntegerVariable() {
        // mark as integer
        ExpressionsBasedModel.Variable intVar = model.newVariable("intVar").integer(true).lower(0).upper(10).weight(1);
        int freeIdx = model.indexOfFreeVariable(model.indexOf(intVar));
        assertEquals(0, freeIdx, "An integer but free variable should still obtain free index 0");
    }

    /* --------------------------------------------------------------
     *  Group: Free variable with zero upper limit
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_FreeVariableZeroUpperLimit() {
        // Upper limit = 0 (non‑positive) does not make the variable non‑free.
        ExpressionsBasedModel.Variable v = model.newVariable("v").lower(-5).upper(0).weight(1);
        int freeIdx = model.indexOfFreeVariable(model.indexOf(v));
        assertEquals(0, freeIdx, "A free variable with upper limit 0 should still have free index 0");
    }

    /* --------------------------------------------------------------
     *  Group: Global index out of bounds
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_GlobalIndexOutOfBounds_ThrowsException() {
        // Add two variables; valid global indices are 0 and 1.
        model.newVariable("a").lower(0).upper(5).weight(1);
        model.newVariable("b").lower(0).upper(5).weight(1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(5), "Requesting a free‑variable index for a global index beyond the model size should throw ArrayIndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------
     *  Group: Negative global index
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfFreeVariable_NegativeGlobalIndex_ThrowsException() {
        // Add a single variable so the model is not empty.
        model.newVariable("x").lower(0).upper(5).weight(1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(-1), "Calling indexOfFreeVariable with a negative global index should throw ArrayIndexOutOfBoundsException");
    }
}
