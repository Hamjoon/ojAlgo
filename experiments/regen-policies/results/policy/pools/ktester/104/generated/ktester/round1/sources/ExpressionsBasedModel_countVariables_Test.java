package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_countVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test case
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up – the model will be garbage‑collected
        model = null;
    }

    /**
     * Method source for {@link #testAddingVariableCounts(int)}.
     */
    private static Stream<Integer> provideVariableCounts() {
        return Stream.of(0, 1, 2, 5, 10);
    }

    @Test
    void testCountVariablesOnEmptyModel() {
        assertEquals(0, model.countVariables(), "A newly created model must contain no variables.");
    }

    @Test
    void testCountVariablesAfterAddingVariables() {
        // add three variables using the public API
        // variable 0
        model.addVariable();
        // variable 1 (named)
        model.addVariable("x1");
        // variable 2 (configured)
        model.newVariable("x2").lower(0);
        assertEquals(3, model.countVariables(), "The model should report three variables after three additions.");
    }

    @Test
    void testCountVariablesOnCopiedModel() {
        // Build an original model with a known number of variables
        model.addVariable("a");
        model.addVariable("b");
        model.addVariable("c");
        int originalCount = model.countVariables();
        assertEquals(3, originalCount, "Original model should have three variables.");
        // Create a copy via the public copy() method (deep copy)
        ExpressionsBasedModel copy = model.copy();
        // The copy must contain the same number of variables
        assertEquals(originalCount, copy.countVariables(), "A copy of the model must preserve the variable count.");
    }

    @Test
    void testCountVariablesRemainsStableAfterOperationsNotAddingVariables() {
        // Add a variable and a constraint, but no further variables
        model.addVariable("x");
        Expression expr = model.addExpression("expr");
        // just configure the expression
        expr.lower(0).upper(10);
        assertEquals(1, model.countVariables(), "Variable count should stay at 1 after adding only expressions.");
    }

    /**
     * Initial state – a newly created model must contain no variables.
     */
    @Test
    void testInitialState() {
        Assertions.assertEquals(0, model.countVariables(), "A freshly created ExpressionsBasedModel must contain no variables.");
    }

    /**
     * Adding a single variable should increase the count to 1.
     */
    @Test
    void testAddingSingleVariable() {
        // unnamed variable
        model.addVariable();
        Assertions.assertEquals(1, model.countVariables(), "After adding one variable the count must be 1.");
    }

    /**
     * Adding three named variables – count must be 3.
     */
    @Test
    void testAddingMultipleNamedVariables() {
        model.addVariable("x");
        model.addVariable("y");
        model.addVariable("z");
        Assertions.assertEquals(3, model.countVariables(), "After adding three named variables the count must be 3.");
    }

    /**
     * Copying a model (deep copy) preserves the variable count.
     */
    @Test
    void testCopyPreservesVariableCount() {
        // create a model with 5 variables
        for (int i = 0; i < 5; i++) {
            model.addVariable("v" + i);
        }
        int originalCount = model.countVariables();
        Assertions.assertEquals(5, originalCount, "Original model should have 5 variables.");
        // deep copy
        ExpressionsBasedModel copy = model.copy();
        Assertions.assertEquals(originalCount, copy.countVariables(), "A deep copy must retain the same number of variables.");
    }

    /**
     * Copying a model with {@code copy(true)} (relaxed copy) also preserves the count.
     */
    @Test
    void testRelaxedCopyPreservesVariableCount() {
        for (int i = 0; i < 5; i++) {
            model.addVariable("v" + i);
        }
        int originalCount = model.countVariables();
        // relaxed copy – the boolean argument corresponds to the overloaded copy(boolean relax) method
        ExpressionsBasedModel relaxedCopy = model.copy(true);
        Assertions.assertEquals(originalCount, relaxedCopy.countVariables(), "A relaxed copy must retain the same number of variables.");
    }

    /**
     * Adding a large number of variables (e.g., 1 000) works correctly.
     */
    @Test
    void testLargeNumberOfVariables() {
        final int N = 1_000;
        for (int i = 0; i < N; i++) {
            // unnamed variables are fine for the test
            model.addVariable();
        }
        Assertions.assertEquals(N, model.countVariables(), "Model should report the correct count after adding 1,000 variables.");
    }

    /**
     * Calling {@code countVariables()} multiple times must be idempotent.
     */
    @Test
    void testIdempotentCountVariables() {
        model.addVariable();
        model.addVariable();
        int first = model.countVariables();
        int second = model.countVariables();
        Assertions.assertEquals(2, first, "First call should return 2.");
        Assertions.assertEquals(2, second, "Second call should also return 2 (idempotent).");
    }

    /**
     * Adding expressions does not change the variable count.
     */
    @Test
    void testVariableCountUnaffectedByExpressions() {
        // add four variables
        model.addVariable("a");
        model.addVariable("b");
        model.addVariable("c");
        model.addVariable("d");
        // add two expressions – they should not affect the variable count
        Expression expr1 = model.addExpression("expr1");
        expr1.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Expression expr2 = model.addExpression("expr2");
        expr2.lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        Assertions.assertEquals(4, model.countVariables(), "Variable count must stay at 4 after adding expressions.");
    }

    /**
     * Demonstrates a parameterised approach for the “adding variables” cases.
     * Each argument is an {@code int} representing how many variables to add.
     */
    @ParameterizedTest(name = "add {0} variables → expected count = {0}")
    @MethodSource("provideVariableCounts")
    void testAddingVariableCounts(int numberOfVariables) {
        for (int i = 0; i < numberOfVariables; i++) {
            model.addVariable();
        }
        Assertions.assertEquals(numberOfVariables, model.countVariables(), "Model should report the correct count after adding " + numberOfVariables + " variables.");
    }
}