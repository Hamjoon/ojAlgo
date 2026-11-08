package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class ExpressionsBasedModel_indexOfNegativeVariable_Test {

    private ExpressionsBasedModel model;

    // lower < 0
    private Variable varNeg1;

    // lower >= 0
    private Variable varPos;

    // another negative variable
    private Variable varNeg2;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test – no variables are added here
        model = new ExpressionsBasedModel();
    }

    /**
     * Provides a stream of arguments for the parameterised test.
     * Each element contains:
     *   - a description (used only for readability)
     *   - a {@code Runnable} that prepares the model for the case
     *   - the global index to query
     *   - the expected result (either an {@code int} or an {@code Class<? extends Throwable>})
     */
    private static Stream<Arguments> negativeVariableScenarios() {
        return Stream.of(// 1. First variable negative lower limit → 0
        Arguments.of("neg lower -5", (Runnable) () -> {
        }, 0, 0), // 2. First variable without lower limit → 0
        Arguments.of("no lower limit", (Runnable) () -> {
        }, 0, 0), // 3. First variable lower == 0 → -1
        Arguments.of("zero lower limit", (Runnable) () -> {
        }, 0, -1), // 4. First variable positive lower limit → -1
        Arguments.of("positive lower limit", (Runnable) () -> {
        }, 0, -1), // 5. Fixed variable with negative lower limit → -1
        Arguments.of("fixed negative", (Runnable) () -> {
        }, 0, -1), // 6‑8. Three‑variable model (neg, pos, neg)
        Arguments.of("three vars – index 0", (Runnable) () -> {
        }, 0, 0), Arguments.of("three vars – index 1", (Runnable) () -> {
        }, 1, -1), Arguments.of("three vars – index 2", (Runnable) () -> {
        }, 2, 1), // 9. Out‑of‑bounds positive → exception
        Arguments.of("out of bounds (5)", (Runnable) () -> {
        }, 5, ArrayIndexOutOfBoundsException.class), // 10. Out‑of‑bounds negative → exception
        Arguments.of("out of bounds (-1)", (Runnable) () -> {
        }, -1, ArrayIndexOutOfBoundsException.class), // 11. Fixed fourth variable → -1
        Arguments.of("fixed fourth", (Runnable) () -> {
        }, 3, -1));
    }

    @Test
    void testIndexOfNegativeVariable_basic() {
        Variable varNeg1 = model.newVariable("neg1").lower(-5).upper(10).weight(1);
        Variable varPos = model.newVariable("pos").lower(0).upper(10).weight(1);
        int idxNeg1 = model.indexOfNegativeVariable(0);
        int idxPos = model.indexOfNegativeVariable(1);
        assertEquals(0, idxNeg1, "First negative variable should have index 0 in the negative list");
        assertEquals(-1, idxPos, "Non‑negative variable should yield -1");
    }

    @Test
    void testIndexOfNegativeVariable_withAdditionalNegative() {
        Variable varNeg1 = model.newVariable("neg1").lower(-5).upper(10).weight(1);
        Variable varPos = model.newVariable("pos").lower(0).upper(10).weight(1);
        Variable varNeg2 = model.newVariable("neg2").lower(-2).upper(5).weight(1);
        int idxNeg1 = model.indexOfNegativeVariable(0);
        int idxPos = model.indexOfNegativeVariable(1);
        int idxNeg2 = model.indexOfNegativeVariable(2);
        assertEquals(0, idxNeg1, "First negative variable should be at position 0");
        assertEquals(-1, idxPos, "Positive variable should still be -1");
        assertEquals(1, idxNeg2, "Second negative variable should be at position 1");
    }

    @Test
    void testIndexOfNegativeVariable_overloads() {
        Variable varNeg1 = model.newVariable("neg1").lower(-5).upper(10).weight(1);
        // overload that accepts a Variable
        int idxFromVar = model.indexOfNegativeVariable(varNeg1);
        assertEquals(0, idxFromVar, "Overload with Variable should return same index as int version");
        // overload that accepts an IntIndex
        IntIndex idx = IntIndex.of(0);
        int idxFromIntIndex = model.indexOfNegativeVariable(idx);
        assertEquals(0, idxFromIntIndex, "Overload with IntIndex should return same index as int version");
    }

    @Test
    void testIndexOfNegativeVariable_fixedVariableIsIgnored() {
        // create the two normal variables first
        model.newVariable("neg1").lower(-5).upper(10).weight(1);
        model.newVariable("pos").lower(0).upper(10).weight(1);
        // now add a fixed variable (lower == upper)
        Variable fixed = model.newVariable("fixed").lower(0).upper(0).weight(1);
        int idxFixed = model.indexOfNegativeVariable(2);
        assertEquals(-1, idxFixed, "Fixed variable must not be considered negative");
    }

    @Test
    void testNegativeLowerLimit_isRecognised() {
        Variable v = model.newVariable("neg").lower(-5).upper(10).weight(1);
        int idx = model.indexOfNegativeVariable(0);
        assertEquals(0, idx, "Variable with negative lower limit must be at position 0");
    }

    @Test
    void testNoLowerLimit_isTreatedAsNegative() {
        Variable v = model.newVariable("noLower").upper(10).weight(1);
        int idx = model.indexOfNegativeVariable(0);
        assertEquals(0, idx, "Variable without a lower limit must be considered negative");
    }

    @Test
    void testZeroLowerLimit_isNotNegative() {
        Variable v = model.newVariable("zero").lower(0).upper(10).weight(1);
        int idx = model.indexOfNegativeVariable(0);
        assertEquals(-1, idx, "Variable with zero lower limit must not be classified as negative");
    }

    @Test
    void testPositiveLowerLimit_isNotNegative() {
        Variable v = model.newVariable("pos").lower(3).upper(10).weight(1);
        int idx = model.indexOfNegativeVariable(0);
        assertEquals(-1, idx, "Variable with positive lower limit must not be classified as negative");
    }

    @Test
    void testFixedVariableEvenIfNegative_isIgnored() {
        Variable v = model.newVariable("fixedNeg").lower(-4).upper(-4).weight(1);
        int idx = model.indexOfNegativeVariable(0);
        assertEquals(-1, idx, "Fixed variable must never appear in the negative list");
    }

    @Test
    void testThreeVariablesMixedCategories() {
        Variable v0 = model.newVariable("neg0").lower(-5).upper(10).weight(1);
        Variable v1 = model.newVariable("pos1").lower(2).upper(10).weight(1);
        Variable v2 = model.newVariable("neg2").lower(-2).upper(5).weight(1);
        assertAll(() -> assertEquals(0, model.indexOfNegativeVariable(0), "First negative variable should be at position 0"), () -> assertEquals(-1, model.indexOfNegativeVariable(1), "Positive variable must yield -1"), () -> assertEquals(1, model.indexOfNegativeVariable(2), "Second negative variable should be at position 1"));
    }

    @Test
    void testFourVariablesFourthIsFixed() {
        model.newVariable("neg0").lower(-5).upper(10).weight(1);
        model.newVariable("pos1").lower(2).upper(10).weight(1);
        model.newVariable("neg2").lower(-2).upper(5).weight(1);
        model.newVariable("fixed").lower(0).upper(0).weight(1);
        assertEquals(-1, model.indexOfNegativeVariable(3), "Fixed variable must not be considered negative even when placed after other variables");
    }

    @Test
    void testOverloadWithVariable() {
        Variable v = model.newVariable("negVar").lower(-3).upper(5).weight(1);
        int idx = model.indexOfNegativeVariable(v);
        assertEquals(0, idx, "Overload accepting Variable must return the same index as the int version");
    }

    @Test
    void testOverloadWithIntIndex() {
        Variable v = model.newVariable("negIdx").lower(-1).upper(4).weight(1);
        IntIndex intIdx = IntIndex.of(0);
        int idx = model.indexOfNegativeVariable(intIdx);
        assertEquals(0, idx, "Overload accepting IntIndex must return the same index as the int version");
    }

    @Test
    void testIndexOutOfBoundsPositive() {
        model.newVariable("only").lower(-1).upper(1).weight(1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(5), "Accessing an index larger than the variable count must throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void testIndexOutOfBoundsNegative() {
        model.newVariable("only").lower(-1).upper(1).weight(1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(-1), "Negative global indices must throw ArrayIndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeVariableScenarios")
    void testParameterizedScenarios(String description, Runnable modelSetup, int globalIndex, Object expected) {
        // -----------------------------------------------------------------
        // Build the model according to the description.
        // -----------------------------------------------------------------
        switch(description) {
            case "neg lower -5":
                model.newVariable("v0").lower(-5).upper(10).weight(1);
                break;
            case "no lower limit":
                model.newVariable("v0").upper(10).weight(1);
                break;
            case "zero lower limit":
                model.newVariable("v0").lower(0).upper(10).weight(1);
                break;
            case "positive lower limit":
                model.newVariable("v0").lower(3).upper(10).weight(1);
                break;
            case "fixed negative":
                model.newVariable("v0").lower(-4).upper(-4).weight(1);
                break;
            case "three vars – index 0":
            case "three vars – index 1":
            case "three vars – index 2":
                model.newVariable("v0").lower(-5).upper(10).weight(1);
                model.newVariable("v1").lower(2).upper(10).weight(1);
                model.newVariable("v2").lower(-2).upper(5).weight(1);
                break;
            case "out of bounds (5)":
            case "out of bounds (-1)":
                model.newVariable("v0").lower(-1).upper(1).weight(1);
                model.newVariable("v1").lower(-1).upper(1).weight(1);
                break;
            case "fixed fourth":
                model.newVariable("v0").lower(-5).upper(10).weight(1);
                model.newVariable("v1").lower(2).upper(10).weight(1);
                model.newVariable("v2").lower(-2).upper(5).weight(1);
                model.newVariable("v3").lower(0).upper(0).weight(1);
                break;
            default:
                // No special preparation required.
                break;
        }
        // -----------------------------------------------------------------
        // Execute the method under test and verify the outcome.
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> model.indexOfNegativeVariable(globalIndex), "Expected exception for index " + globalIndex);
        } else {
            int expectedIdx = (int) expected;
            int actualIdx = model.indexOfNegativeVariable(globalIndex);
            assertEquals(expectedIdx, actualIdx, "Unexpected negative‑variable index for global index " + globalIndex);
        }
    }
}
