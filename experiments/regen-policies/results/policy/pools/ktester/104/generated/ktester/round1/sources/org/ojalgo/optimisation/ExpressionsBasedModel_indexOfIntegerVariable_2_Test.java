package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_indexOfIntegerVariable_2_Test {

    private ExpressionsBasedModel model;

    // declared as integer
    private Variable intVar;

    // regular (continuous) variable
    private Variable nonIntVar;

    //     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // Integer variable – the flag is set via the {@code integer(boolean)} API.
        intVar = model.newVariable("intVar").integer(true);
        // Continuous variable – default is non‑integer.
        nonIntVar = model.newVariable("nonIntVar");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /**
     * Verify that indexes which are outside the range of defined variables cause
     * {@link IndexOutOfBoundsException}. Both a large positive index and a negative index
     * are exercised via a parameterised test.
     */
    @ParameterizedTest
    @ValueSource(ints = { 100, -1 })
    void testIndexOfIntegerVariable_InvalidIndices(int outOfRangeGlobalIdx) {
        // The model currently contains only two variables, so any index >=2 or <0 is invalid.
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(outOfRangeGlobalIdx), "An out‑of‑range global index should trigger IndexOutOfBoundsException.");
    }

    /**
     * Verify that the index of an integer variable is reported correctly.
     */
    @Test
    void testIndexOfIntegerVariable_WithIntegerVariable() {
        // Global index of the integer variable within the whole model.
        int globalIdx = model.indexOf(intVar);
        // Convert the global index to an {@link IntIndex} instance.
        IntIndex intIndex = model.toIntIndex(globalIdx);
        // The method under test should return the position of the variable among all
        // integer variables – for the first integer variable this is 0.
        int integerPosition = model.indexOfIntegerVariable(intIndex);
        assertEquals(0, integerPosition, "The first integer variable should have position 0 among integer variables.");
    }

    /**
     * Verify that a non‑integer variable yields {@code -1}.
     */
    @Test
    void testIndexOfIntegerVariable_WithNonIntegerVariable() {
        int globalIdx = model.indexOf(nonIntVar);
        IntIndex intIndex = model.toIntIndex(globalIdx);
        int integerPosition = model.indexOfIntegerVariable(intIndex);
        assertEquals(-1, integerPosition, "A non‑integer variable should not have an integer variable position (expected -1).");
    }

    /**
     * Verify behavior when the supplied {@link IntIndex} does not correspond to any variable
     * (e.g. an out‑of‑range index). The contract of the method is to return {@code -1} in such
     * cases, mirroring the behaviour of {@link #indexOfIntegerVariable(int)}.
     */
    @Test
    void testIndexOfIntegerVariable_WithInvalidIndex() {
        // Choose an index that is larger than the number of variables in the model.
        int invalidGlobalIdx = model.countVariables() + 10;
        IntIndex invalidIndex = model.toIntIndex(invalidGlobalIdx);
        int result = model.indexOfIntegerVariable(invalidIndex);
        assertEquals(-1, result, "An out‑of‑range index should result in -1 (no integer variable at that position).");
    }

    /**
     * Passing a {@code null} {@link IntIndex} must result in a {@link NullPointerException}
     * because the implementation dereferences {@code variableIndex.index}.
     */
    @Test
    void testIndexOfIntegerVariable_NullIntIndex() {
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable((IntIndex) null), "Calling the method with a null IntIndex should throw NullPointerException.");
    }

    /**
     * Verify behavior when the supplied {@link IntIndex} refers to a variable that exists
     * but is not part of the integer variable list. The method should return {@code -1}.
     */
    @Test
    void testIndexOfIntegerVariable_WithValidNonIntegerIndex() {
        // The second variable (nonIntVar) is valid but not integer.
        int globalIdx = model.indexOf(nonIntVar);
        IntIndex intIndex = model.toIntIndex(globalIdx);
        int result = model.indexOfIntegerVariable(intIndex);
        assertEquals(-1, result, "A valid non‑integer variable should return -1 (not an integer variable).");
    }
}
