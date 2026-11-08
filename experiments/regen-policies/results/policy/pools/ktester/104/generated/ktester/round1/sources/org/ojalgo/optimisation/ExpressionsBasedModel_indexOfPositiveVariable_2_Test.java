package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

// @ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class ExpressionsBasedModel_indexOfPositiveVariable_2_Test {

    private ExpressionsBasedModel model;

    private final List<Variable> vars = new ArrayList<>();

    /**
     * Create a fresh model before each test and add three positive variables.
     */
    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
        // add three variables – by default they are positive
        // index 0
        vars.add(model.addVariable());
        // index 1
        vars.add(model.addVariable());
        // index 2
        vars.add(model.addVariable());
    }

    /**
     * Release references after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        model = null;
        vars.clear();
    }

    /**
     * Supplies pairs of (global index, expected positive index).
     */
    private static Stream<Arguments> positiveIndexProvider() {
        return Stream.of(Arguments.of(0, 0), Arguments.of(1, 1), Arguments.of(2, 2));
    }

    /**
     * All variables are positive, therefore the positive‑variable index must be identical
     * to the global index returned by {@link ExpressionsBasedModel#indexOf(Variable)}.
     */
    @Test
    void testIndexOfPositiveVariable_AllPositive() {
        for (Variable var : vars) {
            int globalIdx = model.indexOf(var);
            IntIndex intIdx = new IntIndex(globalIdx);
            int positiveIdx = model.indexOfPositiveVariable(intIdx);
            assertEquals(globalIdx, positiveIdx, () -> "Positive index should equal global index for variable " + var);
        }
    }

    /**
     * When a variable is marked as negative, it must not appear in the positive‑variable
     * list.  The method should return {@code -1} for such a variable (the contract of the
     * original implementation is to return {@code -1} when the variable is not positive).
     *
     * The {@code Variable} API provides {@code setNegative(boolean)} to change the sign.
     * We use that to create a negative variable and verify the behaviour.
     */
    @Test
    void testIndexOfPositiveVariable_WithNegativeVariable() {
        // create a negative variable
        Variable negative = model.addVariable();
        // make it a negative variable
        negative.setNegative(true);
        // sanity check – the variable is indeed reported as negative
        assertTrue(negative.isNegative(), "Variable should be marked as negative");
        // its global index
        int globalIdx = model.indexOf(negative);
        IntIndex intIdx = new IntIndex(globalIdx);
        // The positive‑variable index must be -1 because the variable is not positive
        int positiveIdx = model.indexOfPositiveVariable(intIdx);
        assertEquals(-1, positiveIdx, () -> "Negative variable should yield -1 from indexOfPositiveVariable");
    }

    /**
     * Verify that an {@link IntIndex} that does not correspond to any variable in the model
     * results in {@code -1}.  This guards against accidental ArrayIndexOutOfBoundsException.
     */
    @Test
    void testIndexOfPositiveVariable_InvalidIntIndex() {
        // Choose an index far beyond the current number of variables
        int invalidGlobal = vars.size() + 10;
        IntIndex invalidIdx = new IntIndex(invalidGlobal);
        int result = model.indexOfPositiveVariable(invalidIdx);
        assertEquals(-1, result, "Calling indexOfPositiveVariable with an out‑of‑range IntIndex should return -1");
    }

    /**
     * Null IntIndex should cause a NullPointerException.
     */
    @Test
    void testIndexOfPositiveVariable_NullIntIndex() {
        assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(null), "Calling with a null IntIndex must throw NullPointerException");
    }

    /**
     * Simple positive variable – expect index 0.
     */
    @Test
    void testIndexOfPositiveVariable_ValidPositiveVariable() {
        // the first variable added in @BeforeEach is positive
        IntIndex idx = model.toIntIndex(0);
        int result = model.indexOfPositiveVariable(idx);
        assertEquals(0, result, "The first positive variable should have index 0");
    }

    /**
     * Fixed (non‑positive) variable – expect -1.
     */
    @Test
    void testIndexOfPositiveVariable_NonPositiveVariable_Fixed() {
        // add a second variable and fix it (lower == upper)
        Variable fixed = model.addVariable();
        fixed.lower(BigDecimal.ZERO);
        // now the variable is fixed
        fixed.upper(BigDecimal.ZERO);
        assertTrue(fixed.isFixed(), "Variable should be recognised as fixed");
        IntIndex idx = model.toIntIndex(model.indexOf(fixed));
        int result = model.indexOfPositiveVariable(idx);
        assertEquals(-1, result, "A fixed variable must not appear in the positive‑variable list");
    }

    /**
     * Index equal to countVariables() – out of range – expect IndexOutOfBoundsException.
     */
    @Test
    void testIndexOfPositiveVariable_IndexOutOfBounds_TooHigh() {
        // valid indices are 0 .. count-1
        int outOfRange = model.countVariables();
        IntIndex idx = model.toIntIndex(outOfRange);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(idx), "Index equal to countVariables() must throw IndexOutOfBoundsException");
    }

    /**
     * Negative index – out of range – expect IndexOutOfBoundsException.
     */
    @Test
    void testIndexOfPositiveVariable_IndexOutOfBounds_Negative() {
        IntIndex idx = new IntIndex(-1);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(idx), "Negative IntIndex must throw IndexOutOfBoundsException");
    }

    /**
     * Positive variable after a shallow copy – expect the same index (0).
     */
    @Test
    void testIndexOfPositiveVariable_AfterShallowCopy() {
        // create a shallow copy (shallow = true, prune = false)
        ExpressionsBasedModel copyModel = model.copy(true, false);
        // the first variable in the original model should still be positive in the copy
        IntIndex idx = copyModel.toIntIndex(0);
        int result = copyModel.indexOfPositiveVariable(idx);
        assertEquals(0, result, "Positive variable index must be preserved after a shallow copy");
    }

    /**
     * Positive variable after model relaxation – expect the same index (0).
     */
    @Test
    void testIndexOfPositiveVariable_AfterRelaxation() {
        // relax the model in‑place (converts integer variables to continuous, etc.)
        model.relax();
        IntIndex idx = model.toIntIndex(0);
        int result = model.indexOfPositiveVariable(idx);
        assertEquals(0, result, "Positive variable index must remain valid after model relaxation");
    }

    /**
     * Calling the method on a disposed model must throw IllegalStateException.
     */
    @Test
    void testIndexOfPositiveVariable_DisposedModel() {
        // render the model unusable
        model.dispose();
        IntIndex idx = model.toIntIndex(0);
        assertThrows(IllegalStateException.class, () -> model.indexOfPositiveVariable(idx), "Disposed model must throw IllegalStateException");
    }

    /**
     * For a model that contains only positive variables, the positive‑variable index
     * should be identical to the global index.  This parameterised test checks the
     * first three variables added in {@code @BeforeEach}.
     */
    @ParameterizedTest(name = "globalIdx={0} → positiveIdx={1}")
    @MethodSource("positiveIndexProvider")
    void testIndexOfPositiveVariable_PositiveIndices(int globalIdx, int expectedPositiveIdx) {
        IntIndex idx = model.toIntIndex(globalIdx);
        int actual = model.indexOfPositiveVariable(idx);
        assertEquals(expectedPositiveIdx, actual, () -> "Positive index mismatch for global index " + globalIdx);
    }
}