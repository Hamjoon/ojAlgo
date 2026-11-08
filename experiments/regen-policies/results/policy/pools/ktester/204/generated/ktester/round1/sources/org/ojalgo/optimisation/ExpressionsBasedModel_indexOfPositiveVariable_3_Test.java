package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfPositiveVariable_3_Test {

    private ExpressionsBasedModel model;

    /**
     * Functional interface used to configure a variable (and possibly other model state)
     * for a parameterised test.
     */
    @FunctionalInterface
    interface VariableSetup {

        Variable configure(ExpressionsBasedModel model);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test method
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // clear the reference – not strictly required but keeps the test tidy
        model = null;
    }

    /**
     * Provides arguments for {@link #testAdditionalScenarios}.
     */
    static Stream<Arguments> additionalScenarioProvider() {
        return Stream.of(// 1. negative variable first, then a positive variable
        Arguments.of("Positive variable after a negative one should be index 0", (VariableSetup) m -> {
            m.newVariable("neg").lower(BigDecimal.valueOf(-10)).upper(BigDecimal.valueOf(-1)).weight(BigDecimal.ONE);
            return m.newVariable("pos").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        }, 0), // 2. integer positive variable after a non‑integer positive variable
        Arguments.of("Integer positive variable should be second (index 1) irrespective of integer flag", (VariableSetup) m -> {
            m.newVariable("regularPos").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
            return m.newVariable("intPos").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8)).integer(true).weight(BigDecimal.ONE);
        }, 1), // 3. fixed variable (lb == ub) is not part of positive list
        Arguments.of("Fixed variable (lb == ub) must return -1", (VariableSetup) m -> m.newVariable("fixed").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ONE), -1), // 4. positive variable after model.relax()
        Arguments.of("Positive variable remains positive after model.relax()", (VariableSetup) m -> {
            Variable v = m.newVariable("pos").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
            m.relax();
            return v;
        }, 0), // 5. shallow copy – variable belongs to original model only
        Arguments.of("Variable from original model should be unknown in shallow copy (‑1)", (VariableSetup) m -> {
            Variable original = m.newVariable("orig").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(4)).weight(BigDecimal.ONE);
            return original;
        }, -1));
    }

    /**
     * A variable whose lower bound is non‑negative is considered “positive”.
     * The method {@code indexOfPositiveVariable(Variable)} should therefore return the
     * same index as {@code indexOf(Variable)}.
     */
    @Test
    void testPositiveVariableReturnsItsIndex() {
        Variable posVar = model.newVariable("pos").lower(// non‑negative lower bound → positive
        BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        int expected = model.indexOf(posVar);
        int actual = model.indexOfPositiveVariable(posVar);
        assertEquals(expected, actual, "A positive variable should return its global index from indexOfPositiveVariable");
    }

    /**
     * A variable whose lower bound is negative is not “positive”.  In this case
     * {@code indexOfPositiveVariable(Variable)} is expected to return {@code -1}
     * (the behaviour of the underlying {@code indexOfPositiveVariable(int)} method
     * for non‑positive variables).
     */
    @Test
    void testNegativeVariableReturnsMinusOne() {
        Variable negVar = model.newVariable("neg").lower(// negative lower bound → not positive
        BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        int actual = model.indexOfPositiveVariable(negVar);
        assertEquals(-1, actual, "A non‑positive variable should yield -1 from indexOfPositiveVariable");
    }

    /**
     * When the supplied variable does not belong to the model, the method should
     * also return {@code -1}.  This guards against accidental misuse.
     */
    @Test
    void testVariableNotInModelReturnsMinusOne() {
        // Create a variable that is *not* added to the model
        Variable external = new Variable("external", 999);
        int actual = model.indexOfPositiveVariable(external);
        assertEquals(-1, actual, "A variable not part of the model must return -1 from indexOfPositiveVariable");
    }

    /**
     * Variable with upper bound 0 (still non‑positive) must also yield -1.
     */
    @Test
    void testUpperBoundZeroIsNotPositive() {
        Variable ubZero = model.newVariable("ubZero").lower(BigDecimal.valueOf(-10)).upper(// upper bound 0 → not positive
        BigDecimal.ZERO).weight(BigDecimal.ONE);
        int actual = model.indexOfPositiveVariable(ubZero);
        assertEquals(-1, actual, "Variable whose upper bound is 0 must not be considered positive");
    }

    /**
     * A variable whose bounds cross zero (free variable) is not positive.
     */
    @Test
    void testFreeVariableReturnsMinusOne() {
        Variable free = model.newVariable("free").lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        int actual = model.indexOfPositiveVariable(free);
        assertEquals(-1, actual, "A free variable (lb < 0 < ub) must not be classified as positive");
    }

    /**
     * Passing {@code null} must result in a {@link NullPointerException}
     * because the implementation dereferences the argument.
     */
    @Test
    void testNullVariableThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(null), "Calling indexOfPositiveVariable with null should throw NPE");
    }

    /**
     * Adding a second positive variable should yield index 1 (second position).
     */
    @Test
    void testSecondPositiveVariableIndexIsOne() {
        Variable first = model.newVariable("first").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable second = model.newVariable("second").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(20)).weight(BigDecimal.ONE);
        int idxSecond = model.indexOfPositiveVariable(second);
        assertEquals(1, idxSecond, "The second positive variable should have index 1 among positive variables");
    }

    /**
     * Parameterised tests covering the “additional scenarios” group.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalScenarioProvider")
    void testAdditionalScenarios(String description, VariableSetup setup, int expected) {
        // Apply the variable configuration supplied by the lambda
        Variable var = setup.configure(model);
        int actual = model.indexOfPositiveVariable(var);
        assertEquals(expected, actual, description);
    }

    /**
     * Helper that runs the shallow‑copy scenario because the variable must be looked up
     * in a *different* model instance.
     */
    @Test
    void testShallowCopyVariableNotPresent() {
        Variable original = model.newVariable("orig").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(4)).weight(BigDecimal.ONE);
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        int actual = shallowCopy.indexOfPositiveVariable(original);
        assertEquals(-1, actual, "Variable from original model must not be found in shallow copy");
    }

    /**
     * Supplying a {@link Variable} that belongs to a *different* model must cause an
     * {@link IllegalArgumentException} (or a subclass such as {@link ProgrammingError}).
     */
    @Test
    void testVariableFromDifferentModelThrowsException() {
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        Variable otherVar = otherModel.newVariable("other").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> model.indexOfPositiveVariable(otherVar), "Variable from a different model should trigger IllegalArgumentException");
    }

    /**
     * After disposing a model, any call to {@code indexOfPositiveVariable} must raise
     * an {@link IllegalStateException} (or the library‑specific {@link ProgrammingError}).
     */
    @Test
    void testDisposedModelThrowsException() {
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // mark the model as disposed
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.indexOfPositiveVariable(v), "Calling indexOfPositiveVariable on a disposed model must throw IllegalStateException");
    }
}