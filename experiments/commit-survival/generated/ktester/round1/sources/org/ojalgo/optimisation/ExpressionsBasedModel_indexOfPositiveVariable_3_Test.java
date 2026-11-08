package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfPositiveVariable_3_Test {

    private ExpressionsBasedModel model;

    private Variable varNegative;

    private Variable varPositive;

    private Variable varZeroLevel;

    // lower bound == 0
    private Variable varZeroBound;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test case.
        model = new ExpressionsBasedModel();
        // 1️⃣ Negative variable – upper bound < 0 (lower bound defaults to null → not positive)
        varNegative = model.addVariable();
        varNegative.upper(BigDecimal.valueOf(-1));
        // 2️⃣ Positive variable – lower bound ≥ 0
        varPositive = model.addVariable();
        varPositive.lower(BigDecimal.ZERO);
        // 3️⃣ Zero‑level variable – fixed at 0 (treated as non‑negative)
        varZeroLevel = model.addVariable();
        varZeroLevel.level(BigDecimal.ZERO);
        // 4️⃣ Zero‑bound variable – lower bound exactly 0 (not considered positive)
        varZeroBound = model.addVariable();
        varZeroBound.lower(BigDecimal.ZERO);
    }

    /**
     * Supplies a stream of arguments for {@link #testOrderingAmongMultiplePositiveVariables}.
     * The model already contains {@code varPositive} (lower bound = 1). We add two more
     * positive variables and verify the order reflects insertion order.
     */
    private static Stream<Arguments> positiveVariableOrderingProvider() {
        // The model instance is created per test, therefore we need to recreate the
        // variables inside the provider using a fresh model.
        ExpressionsBasedModel tmpModel = new ExpressionsBasedModel();
        // First positive variable (index 0)
        Variable first = tmpModel.addVariable();
        first.lower(BigDecimal.ONE);
        // Second positive variable (index 1)
        Variable second = tmpModel.addVariable();
        second.lower(BigDecimal.valueOf(2));
        // Third positive variable (index 2)
        Variable third = tmpModel.addVariable();
        third.lower(BigDecimal.valueOf(3));
        // Return arguments paired with the expected positive‑variable indices.
        return Stream.of(Arguments.of(first, 0), Arguments.of(second, 1), Arguments.of(third, 2));
    }

    @Test
    void testNegativeVariableReturnsMinusOne() {
        int index = model.indexOfPositiveVariable(varNegative);
        assertEquals(-1, index, "A variable with a negative upper bound must not be considered positive");
    }

    @Test
    void testPositiveVariableReturnsCorrectIndex() {
        // The first positive variable in the model should have index 0.
        int posIdx = model.indexOfPositiveVariable(varPositive);
        assertEquals(0, posIdx, "The first positive variable should be index 0 among positive variables");
        // Verify the overload that accepts the global index yields the same result.
        int globalIdx = model.indexOf(varPositive);
        int posIdxViaGlobal = model.indexOfPositiveVariable(globalIdx);
        assertEquals(posIdx, posIdxViaGlobal, "indexOfPositiveVariable(int) must be consistent with indexOfPositiveVariable(Variable)");
    }

    @Test
    void testZeroLevelVariableIsTreatedAsPositive() {
        // A variable fixed at zero is non‑negative and therefore should be counted as positive.
        int zeroIdx = model.indexOfPositiveVariable(varZeroLevel);
        // It follows the previously added positive variable, so its index should be 1.
        assertEquals(1, zeroIdx, "A zero‑level (fixed) variable should be considered positive and follow the earlier positive variable");
    }

    @Test
    void testMixedOrderOfVariables() {
        // Ensure the ordering of positive indices reflects the order in which positive
        // variables are added to the model, ignoring negative ones.
        Variable anotherPositive = model.addVariable();
        anotherPositive.lower(BigDecimal.ONE);
        // expected 0
        int idxPos = model.indexOfPositiveVariable(varPositive);
        // expected 1
        int idxZero = model.indexOfPositiveVariable(varZeroLevel);
        // expected 2
        int idxAnother = model.indexOfPositiveVariable(anotherPositive);
        assertAll(
                () -> assertEquals(0, idxPos, "First positive variable index"),
                () -> assertEquals(1, idxZero, "Zero‑level variable index"),
                () -> assertEquals(2, idxAnother, "Second added positive variable index")
        );
    }

    /**
     * 1. Valid positive variable **
     */
    @Test
    void testValidPositiveVariableReturnsNonNegativeIndex() {
        int idx = model.indexOfPositiveVariable(varPositive);
        assertTrue(idx >= 0, "A variable with a strictly positive lower bound must be recognised as positive and receive a non‑negative index");
        // The first positive variable added should be at position 0.
        assertEquals(0, idx, "The first positive variable should have index 0 among positive variables");
    }

    /**
     * 2. Variable not added to the model **
     */
    @Test
    void testVariableNotInModelReturnsMinusOne() {
        // Create a Variable instance that is never added to the model.
        Variable orphan = new Variable("orphan");
        int idx = model.indexOfPositiveVariable(orphan);
        assertEquals(-1, idx, "A variable unknown to the model must yield -1");
    }

    /**
     * 3. Zero lower bound – not strictly positive **
     */
    @Test
    void testZeroLowerBoundVariableReturnsMinusOne() {
        int idx = model.indexOfPositiveVariable(varZeroBound);
        assertEquals(-1, idx, "A variable whose lower bound equals 0 is not considered positive and must return -1");
    }

    /**
     * 4. Negative lower bound **
     */
    @Test
    void testNegativeLowerBoundVariableReturnsMinusOne() {
        int idx = model.indexOfPositiveVariable(varNegative);
        assertEquals(-1, idx, "A variable with a negative lower bound must not be classified as positive");
    }

    /**
     * 5. Null argument handling **
     */
    @Test
    void testNullVariableThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(null), "Passing null should result in a NullPointerException");
    }

    /**
     * 6. Ordering among multiple positive variables **
     */
    @ParameterizedTest(name = "{index}: {0} should have positive index {1}")
    @MethodSource("positiveVariableOrderingProvider")
    void testOrderingAmongMultiplePositiveVariables(Variable variable, int expectedIndex) {
        int actual = model.indexOfPositiveVariable(variable);
        assertEquals(expectedIndex, actual, () -> "Variable '" + variable.getName() + "' expected at positive index " + expectedIndex);
    }

    /**
     * 7. Behaviour after model disposal **
     */
    @Test
    void testModelDisposedThrowsProgrammingError() {
        // Dispose the model first.
        model.dispose();
        // After disposal any call to indexOfPositiveVariable must fail with ProgrammingError.
        assertThrows(ProgrammingError.class, () -> model.indexOfPositiveVariable(varPositive), "Calling indexOfPositiveVariable on a disposed model should throw ProgrammingError");
    }
}