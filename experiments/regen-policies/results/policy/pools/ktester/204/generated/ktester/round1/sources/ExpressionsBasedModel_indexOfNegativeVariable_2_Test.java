package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfNegativeVariable_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> delegationAndNormalCases() {
        return Stream.of(// index 0 → first negative variable → position 0
        Arguments.of(new IntIndex(0), 0), // index 2 → second negative variable → position 1
        Arguments.of(new IntIndex(2), 1), // index 5 → not a negative variable → -1
        Arguments.of(new IntIndex(5), -1));
    }

    private static Stream<Arguments> edgeCaseIndices() {
        return Stream.of(// negative index
        Arguments.of(new IntIndex(-1), -1), // far beyond size
        Arguments.of(new IntIndex(1000), -1));
    }

    /**
     * A single negative variable should be found at index {@code 0}
     * in the negative‑variable list.
     */
    @Test
    void testSingleNegativeVariable() {
        // create a variable that can only take non‑positive values
        Variable neg = model.addVariable().upper(// upper bound 0
        BigDecimal.ZERO).lower(// lower bound -10
        new BigDecimal("-10")).weight(// weight is irrelevant for the test
        BigDecimal.ONE);
        int globalIdx = model.indexOf(neg);
        IntIndex intIdx = new IntIndex(globalIdx);
        int negPos = model.indexOfNegativeVariable(intIdx);
        assertEquals(0, negPos, "The first (and only) negative variable should have index 0");
    }

    /**
     * When several negative variables are present the method must return the
     * correct position for each of them.
     */
    @Test
    void testMultipleNegativeVariablesOrder() {
        // first negative variable
        Variable neg1 = model.addVariable().upper(BigDecimal.ZERO).lower(new BigDecimal("-5"));
        // a positive variable (should be ignored by the method)
        Variable pos = model.addVariable().lower(BigDecimal.ZERO).upper(new BigDecimal("10"));
        // second negative variable
        Variable neg2 = model.addVariable().upper(BigDecimal.ZERO).lower(new BigDecimal("-20"));
        // indices in the global variable list
        int idxNeg1 = model.indexOf(neg1);
        int idxNeg2 = model.indexOf(neg2);
        int idxPos = model.indexOf(pos);
        // verify ordering inside the negative‑variable list
        assertEquals(0, model.indexOfNegativeVariable(new IntIndex(idxNeg1)), "First negative variable should be at position 0");
        assertEquals(1, model.indexOfNegativeVariable(new IntIndex(idxNeg2)), "Second negative variable should be at position 1");
        // a positive variable must yield -1
        assertEquals(-1, model.indexOfNegativeVariable(new IntIndex(idxPos)), "Positive variable must not be found in negative‑variable list");
    }

    /**
     * A variable that is not negative must yield {@code -1}.
     */
    @Test
    void testPositiveVariableReturnsMinusOne() {
        Variable pos = model.addVariable().lower(// non‑negative lower bound
        BigDecimal.ZERO).upper(// positive upper bound
        new BigDecimal("5"));
        int globalIdx = model.indexOf(pos);
        IntIndex intIdx = new IntIndex(globalIdx);
        int result = model.indexOfNegativeVariable(intIdx);
        assertEquals(-1, result, "Positive variable should not be present in negative‑variable list");
    }

    /**
     * Supplying an invalid index (e.g. {@code -1}) must also return {@code -1}
     * rather than throwing an exception.
     */
    @Test
    void testInvalidIndexReturnsMinusOne() {
        IntIndex invalid = new IntIndex(-1);
        int result = model.indexOfNegativeVariable(invalid);
        assertEquals(-1, result, "Invalid index should result in -1");
    }

    /**
     * Verify that the {@code IntIndex} overload correctly delegates to the
     * {@code int} overload and returns the expected position (or {@code -1})
     * for a variety of indices.
     */
    @ParameterizedTest(name = "[{index}] variableIndex={0} → expected={1}")
    @MethodSource("delegationAndNormalCases")
    void testDelegationAndNormalCases(IntIndex variableIndex, int expected) {
        /* Build a model with six variables:
         *
         * 0 : negative (upper 0, lower -5)
         * 1 : positive (lower 0, upper 5)
         * 2 : negative (upper 0, lower -10)
         * 3 : positive (lower 1, upper 10)
         * 4 : zero‑bound (lower 0, upper 0) – not negative
         * 5 : positive (lower 2, upper 20) – not negative
         */
        // idx 0 (negative)
        model.addVariable().upper(BigDecimal.ZERO).lower(new BigDecimal("-5"));
        // idx 1 (positive)
        model.addVariable().lower(BigDecimal.ZERO).upper(new BigDecimal("5"));
        // idx 2 (negative)
        model.addVariable().upper(BigDecimal.ZERO).lower(new BigDecimal("-10"));
        // idx 3 (positive)
        model.addVariable().lower(BigDecimal.ONE).upper(new BigDecimal("10"));
        // idx 4 (zero‑bound)
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        // idx 5 (positive)
        model.addVariable().lower(new BigDecimal("2")).upper(new BigDecimal("20"));
        // The method under test
        int actual = model.indexOfNegativeVariable(variableIndex);
        assertEquals(expected, actual, () -> "Unexpected result for variableIndex=" + variableIndex);
    }

    /**
     * Negative or out‑of‑range indices must result in {@code -1}.
     */
    @ParameterizedTest(name = "[{index}] variableIndex={0} → expected={1}")
    @MethodSource("edgeCaseIndices")
    void testEdgeCaseInvalidIndices(IntIndex variableIndex, int expected) {
        // Model with a couple of variables – the actual content does not matter
        // because the supplied index is out of bounds.
        // idx 0
        model.addVariable();
        // idx 1
        model.addVariable();
        int actual = model.indexOfNegativeVariable(variableIndex);
        assertEquals(expected, actual, () -> "Expected " + expected + " for out‑of‑range index " + variableIndex);
    }

    /**
     * Passing {@code null} must raise a {@link NullPointerException}.
     */
    @Test
    void testNullIntIndexThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(null), "Calling with null should throw NullPointerException");
    }

    /**
     * When the model contains no variables, any valid index is out of bounds
     * and the method must return {@code -1}.
     */
    @Test
    void testEmptyModelReturnsMinusOne() {
        // No variables added to the model.
        int result = model.indexOfNegativeVariable(new IntIndex(0));
        assertEquals(-1, result, "Empty model should return -1 for any index");
    }

    /**
     * Model with a mixture of positive and negative variables. The variable at
     * global index 3 is the second negative variable, therefore the expected
     * position is 1.
     */
    @Test
    void testMixedSignVariables() {
        // idx 0 – positive
        model.addVariable().lower(BigDecimal.ZERO).upper(new BigDecimal("10"));
        // idx 1 – negative
        model.addVariable().upper(BigDecimal.ZERO).lower(new BigDecimal("-3"));
        // idx 2 – positive
        model.addVariable().lower(BigDecimal.ONE).upper(new BigDecimal("5"));
        // idx 3 – negative (second negative)
        model.addVariable().upper(BigDecimal.ZERO).lower(new BigDecimal("-7"));
        int actual = model.indexOfNegativeVariable(new IntIndex(3));
        assertEquals(1, actual, "Variable at global index 3 should be the second negative variable (position 1)");
    }
}
