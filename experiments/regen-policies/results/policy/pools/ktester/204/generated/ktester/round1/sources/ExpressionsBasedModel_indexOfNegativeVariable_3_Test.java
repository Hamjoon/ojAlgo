package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfNegativeVariable_3_Test {

    /**
     * The model used by the tests.
     */
    private ExpressionsBasedModel model;

    /* -----------------------------------------------------------
     *  Helper class to create variables on a fresh model instance
     * ----------------------------------------------------------- */
    private static class VariableFactory {

        /**
         * Creates a new variable with the given name and weight on a fresh model.
         * The returned variable is already part of that model.
         */
        Variable create(String name, BigDecimal weight) {
            ExpressionsBasedModel tmpModel = new ExpressionsBasedModel();
            return tmpModel.newVariable(name).weight(weight);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test guarantees independence.
        model = new ExpressionsBasedModel();
    }

    static Stream<Arguments> multipleNegativeVariablesProvider() {
        // Build a fresh model for each argument set inside the test method.
        return Stream.of(Arguments.of("Two negative variables, first weight -5, second weight -2", new VariableFactory().create("neg1", BigDecimal.valueOf(-5)), new VariableFactory().create("neg2", BigDecimal.valueOf(-2)), 0, 1), Arguments.of("Two negative variables, first weight -2, second weight -5 (order unchanged)", new VariableFactory().create("negA", BigDecimal.valueOf(-2)), new VariableFactory().create("negB", BigDecimal.valueOf(-5)), 0, 1));
    }

    /**
     * Verify that a variable that is not classified as negative returns {@code -1}.
     */
    @Test
    void testPositiveVariableReturnsMinusOne() {
        // a positive variable: lower bound >= 0
        Variable pos = model.newVariable("pos").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        int idx = model.indexOfNegativeVariable(pos);
        assertEquals(-1, idx, "A positive variable should not have a negative index");
    }

    /**
     * Verify that the first negative variable gets index {@code 0}.
     */
    @Test
    void testFirstNegativeVariableReturnsZero() {
        // a negative variable: upper bound ≤ 0
        Variable neg = model.newVariable("neg").lower(BigDecimal.valueOf(-5)).upper(BigDecimal.ZERO).weight(BigDecimal.ONE);
        int idx = model.indexOfNegativeVariable(neg);
        assertEquals(0, idx, "The first negative variable should have index 0");
    }

    /**
     * Verify that a second negative variable receives index {@code 1}.
     */
    @Test
    void testSecondNegativeVariableReturnsOne() {
        // first negative variable
        Variable neg1 = model.newVariable("neg1").lower(BigDecimal.valueOf(-10)).upper(BigDecimal.ZERO).weight(BigDecimal.ONE);
        // second negative variable
        Variable neg2 = model.newVariable("neg2").lower(BigDecimal.valueOf(-3)).upper(BigDecimal.ZERO).weight(BigDecimal.ONE);
        int idx1 = model.indexOfNegativeVariable(neg1);
        int idx2 = model.indexOfNegativeVariable(neg2);
        assertEquals(0, idx1, "First negative variable should be index 0");
        assertEquals(1, idx2, "Second negative variable should be index 1");
    }

    /**
     * Verify that passing {@code null} results in a {@link NullPointerException}.
     * The implementation calls {@link ExpressionsBasedModel#indexOf(Variable)} which
     * validates the argument and throws NPE for a null reference.
     */
    @Test
    void testNullVariableThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(null), "Passing null must result in NullPointerException");
    }

    /**
     * Verify that a variable created outside the model (i.e. not added to the model)
     * yields {@code -1}. This is exercised by constructing a variable via the
     * protected constructor using reflection – the variable is not registered in the
     * model's internal list, so the lookup fails.
     */
    @Test
    void testVariableNotInModelReturnsMinusOne() throws Exception {
        // Use reflection to create a Variable that is not added to the model.
        java.lang.reflect.Constructor<Variable> ctor = Variable.class.getDeclaredConstructor(String.class, int.class);
        ctor.setAccessible(true);
        Variable external = ctor.newInstance("external", 999);
        int idx = model.indexOfNegativeVariable(external);
        assertEquals(-1, idx, "A variable not part of the model should return -1");
    }

    @Test
    void testZeroWeightVariableReturnsMinusOne() {
        Variable zero = model.newVariable("zero").weight(BigDecimal.ZERO);
        assertEquals(-1, model.indexOfNegativeVariable(zero), "A zero‑weight variable must not be reported as negative");
    }

    @Test
    void testNegativeVariableReturnsZero() {
        Variable neg = model.newVariable("neg").weight(BigDecimal.valueOf(-5));
        assertEquals(0, model.indexOfNegativeVariable(neg), "The first negative variable should have index 0");
    }

    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("multipleNegativeVariablesProvider")
    void testMultipleNegativeVariables(String description, Variable first, Variable second, int expectedFirstIdx, int expectedSecondIdx) {
        // The variables are added to the model when created via newVariable(...)
        assertEquals(expectedFirstIdx, model.indexOfNegativeVariable(first), "First negative variable index mismatch");
        assertEquals(expectedSecondIdx, model.indexOfNegativeVariable(second), "Second negative variable index mismatch");
    }

    @Test
    void testNegativeVariableBecomesPositiveAfterWeightChange() {
        Variable v = model.newVariable("v").weight(BigDecimal.valueOf(-3));
        assertEquals(0, model.indexOfNegativeVariable(v), "Initially the variable should be recognised as negative");
        // Change weight to positive – the variable should disappear from the negative list.
        v.weight(BigDecimal.valueOf(4));
        assertEquals(-1, model.indexOfNegativeVariable(v), "After changing to positive weight the variable must no longer be negative");
    }

    @Test
    void testNegativeVariableBecomesZeroAfterWeightChange() {
        Variable v = model.newVariable("v").weight(BigDecimal.valueOf(-3));
        assertEquals(0, model.indexOfNegativeVariable(v), "Initially the variable should be recognised as negative");
        // Change weight to zero – the variable should disappear from the negative list.
        v.weight(BigDecimal.ZERO);
        assertEquals(-1, model.indexOfNegativeVariable(v), "After changing to zero weight the variable must no longer be negative");
    }

    @Test
    void testNegativeIntegerVariable() {
        Variable iv = model.newVariable("intNeg").weight(BigDecimal.valueOf(-7)).integer(true);
        assertEquals(0, model.indexOfNegativeVariable(iv), "Negative integer variable should be indexed like any other negative variable");
    }

    @Test
    void testNegativeVariableInShallowCopy() {
        Variable v = model.newVariable("v").weight(BigDecimal.valueOf(-4));
        // shallow copy, same Variable instances
        ExpressionsBasedModel copy = model.copy();
        assertEquals(0, copy.indexOfNegativeVariable(v), "Shallow copy must preserve the negative‑variable index");
    }

    @Test
    void testNegativeVariableInShallowCopyWithPrune() {
        Variable v = model.newVariable("v").weight(BigDecimal.valueOf(-4));
        // shallow = true, prune = false
        ExpressionsBasedModel copy = model.copy(true, false);
        assertEquals(0, copy.indexOfNegativeVariable(v), "Shallow copy with pruning must still preserve the negative‑variable index");
    }

    @Test
    void testNegativeVariableIndexShiftsAfterPrecedingBecomesNonNegative() {
        Variable first = model.newVariable("first").weight(BigDecimal.valueOf(-6));
        Variable second = model.newVariable("second").weight(BigDecimal.valueOf(-2));
        // Verify initial ordering
        assertEquals(0, model.indexOfNegativeVariable(first));
        assertEquals(1, model.indexOfNegativeVariable(second));
        // Make the first variable non‑negative
        first.weight(BigDecimal.ONE);
        // Now the second variable should shift to index 0
        assertEquals(-1, model.indexOfNegativeVariable(first), "First variable is no longer negative");
        assertEquals(0, model.indexOfNegativeVariable(second), "Second variable should shift to index 0 after first loses negative status");
    }
}
