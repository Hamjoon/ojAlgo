package org.ojalgo.optimisation;

import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_indexOfNegativeVariable_2_Test {

    private ExpressionsBasedModel model;

    //     @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    //     @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Out‑of‑range indices (equal to or greater than {@code countVariables()})
     * must cause {@link IndexOutOfBoundsException}.
     *
     * <p>The test is parameterised to cover the boundary case (index == count)
     * and a far‑beyond case (index == count + 5).</p>
     */
    @ParameterizedTest(name = "index = countVariables() + {0}")
    @ValueSource(ints = { 0, 5 })
    void testIndexOfNegativeVariable_IndexOutOfRange(int offset) {
        // Ensure the model has at least one variable so countVariables() > 0
        model.newVariable("v");
        // valid range: 0 .. count‑1
        int count = model.countVariables();
        // >= count -> invalid
        int outOfRangeIdx = count + offset;
        IntIndex idx = model.toIntIndex(outOfRangeIdx);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(idx), "Index " + outOfRangeIdx + " should trigger IndexOutOfBoundsException");
    }

    /**
     * Verify that a variable with a strictly negative feasible range is reported
     * correctly by {@code indexOfNegativeVariable}. The first negative variable
     * should have index {@code 0}, the second {@code 1}, etc.
     */
    @Test
    void testIndexOfNegativeVariableFound() {
        // Positive variable – should not be considered negative
        Variable pos = model.newVariable("pos").lower(0).upper(10);
        // Negative variable – feasible region entirely below zero
        Variable neg = model.newVariable("neg").lower(-10).upper(-1);
        // Another negative variable to ensure ordering works
        Variable neg2 = model.newVariable("neg2").lower(-5).upper(-0.1);
        int posIdx = model.indexOf(pos);
        int negIdx = model.indexOf(neg);
        int neg2Idx = model.indexOf(neg2);
        // The first negative variable should be at position 0
        int idxNeg = model.indexOfNegativeVariable(new IntIndex(negIdx));
        assertEquals(0, idxNeg, "First negative variable should have index 0");
        // The second negative variable should be at position 1
        int idxNeg2 = model.indexOfNegativeVariable(new IntIndex(neg2Idx));
        assertEquals(1, idxNeg2, "Second negative variable should have index 1");
        // Positive variable must return -1 (not a negative variable)
        int idxPos = model.indexOfNegativeVariable(new IntIndex(posIdx));
        assertEquals(-1, idxPos, "Positive variable should not be found as negative");
    }

    /**
     * When the supplied {@link IntIndex} does not correspond to any variable
     * (e.g. out‑of‑range index), the method must return {@code -1}.
     */
    @Test
    void testIndexOfNegativeVariableInvalidIndex() {
        // The model is empty – any index is invalid
        int invalidIdx = 999;
        int result = model.indexOfNegativeVariable(new IntIndex(invalidIdx));
        assertEquals(-1, result, "Out‑of‑range index should yield -1");
    }

    /**
     * Calling {@code indexOfNegativeVariable} with a {@code null} {@link IntIndex}
     * should throw {@link NullPointerException} because the implementation dereferences
     * {@code variableIndex.index}.
     */
//     @Test
//     void testIndexOfNegativeVariable_NullIntIndex() {
//         assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(null), "Null IntIndex should result in NullPointerException");
//     }

    /**
     * An {@link IntIndex} wrapping a negative integer is illegal. The delegated
     * call {@code indexOfNegativeVariable(int)} is expected to reject it with
     * {@link IllegalArgumentException}.
     */
    @Test
    void testIndexOfNegativeVariable_NegativeIndex() {
        IntIndex negativeIdx = model.toIntIndex(-1);
        assertThrows(IllegalArgumentException.class, () -> model.indexOfNegativeVariable(negativeIdx), "Negative index should result in IllegalArgumentException");
    }
}