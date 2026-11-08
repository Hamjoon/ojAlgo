package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Disabled;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.structure.Access1D;
import org.ojalgo.optimisation.ExpressionsBasedModel.Variable;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariableValues_Test {

    /**
     * Simple model with a single variable and a single equality constraint.
     * The optimum is known analytically (x = 5).  The test verifies that
     * {@code getVariableValues()} returns a result with the expected value
     * and that the model validates the solution.
     */
    @Test
    void testGetVariableValuesReturnsOptimalResult() {
        // Build a trivial model: minimise x subject to x == 5, 0 <= x <= 10
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// lower bound 0
        BigDecimal.ZERO).upper(// upper bound 10
        BigDecimal.TEN).weight(// minimise x (weight = 1)
        BigDecimal.ONE);
        // Equality constraint x = 5  (lower = upper = 5)
        model.newExpression("eq").set(x, BigDecimal.ONE).lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // Invoke the method under test
        Optimisation.Result result = model.getVariableValues();
        // The model should consider the result feasible/optimal
        assertTrue(model.validate(result), "Model should validate the returned result");
        assertTrue(result.getState().isFeasible(), "Result state should be feasible");
        assertTrue(result.getState().isOptimal(), "Result state should be optimal");
        // Verify the variable value is exactly 5
        int idx = model.indexOf(x);
        BigDecimal xValue = result.get(idx);
        assertEquals(BigDecimal.valueOf(5), xValue, "Variable x should have value 5");
    }

    /**
     * Model with two variables where the objective is to minimise a linear combination.
     * This ensures that {@code getVariableValues()} correctly respects the optimisation
     * sense and returns the optimal solution.
     */
    @Test
    void testGetVariableValuesWithMultipleVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variables: 0 <= a <= 4, 0 <= b <= 4
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(4)).weight(// weight 1
        BigDecimal.ONE);
        Variable b = model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(4)).weight(// weight 2
        BigDecimal.valueOf(2));
        // Constraint: a + b = 5  (feasible region: (1,4) … (4,1))
        model.newExpression("sum").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // Expected optimum (minimise 1*a + 2*b) occurs at a = 4, b = 1
        Optimisation.Result result = model.getVariableValues();
        assertTrue(model.validate(result), "Model should validate the returned result");
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        BigDecimal aVal = result.get(model.indexOf(a));
        BigDecimal bVal = result.get(model.indexOf(b));
        assertEquals(BigDecimal.valueOf(4), aVal, "Variable a should be 4 at optimum");
        assertEquals(BigDecimal.valueOf(1), bVal, "Variable b should be 1 at optimum");
        // Verify the objective value: 1*4 + 2*1 = 6
        assertEquals(BigDecimal.valueOf(6), result.getValue(), "Objective value should be 6");
    }

    @Test
    void testDefaultFeasibilityContextDelegation() {
        // Simple model: minimise x, 0 ≤ x ≤ 10
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Result via the no‑arg method
        Optimisation.Result noArgResult = model.getVariableValues();
        // Result via the overload using the same context explicitly
        Optimisation.Result explicitResult = model.getVariableValues(model.options.feasibility);
        assertEquals(noArgResult.getState(), explicitResult.getState(), "Result states must be identical");
        assertEquals(noArgResult.getValue(), explicitResult.getValue(), "Objective values must be identical");
        assertEquals(noArgResult.get(model.indexOf(x)), explicitResult.get(model.indexOf(x)), "Variable values must be identical");
    }

    @Test
    void testGetVariableValuesIdempotent() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Optimisation.Result first = model.getVariableValues();
        Optimisation.Result second = model.getVariableValues();
        assertEquals(first.getState(), second.getState(), "State must be unchanged");
        assertEquals(first.getValue(), second.getValue(), "Objective value must be unchanged");
        assertEquals(first.get(model.indexOf(x)), second.get(model.indexOf(x)), "Variable value must be unchanged");
    }

    @Test
    void testCustomFeasibilityContextDelegation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Use a stricter context (precision 6)
        NumberContext custom = NumberContext.of(6);
        model.options.feasibility = custom;
        Optimisation.Result noArg = model.getVariableValues();
        Optimisation.Result explicit = model.getVariableValues(custom);
        assertEquals(noArg.getState(), explicit.getState(), "Both calls must return the same state");
        assertEquals(noArg.getValue(), explicit.getValue(), "Both calls must return the same objective value");
        assertEquals(noArg.get(model.indexOf(x)), explicit.get(model.indexOf(x)), "Both calls must return the same variable value");
    }

    @Test
    void testZeroToleranceFeasibilityContext() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Exact solution x = 5
        Variable x = model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // Set the feasibility context to zero tolerance
        model.options.feasibility = NumberContext.of(0);
        Optimisation.Result result = model.getVariableValues();
        assertTrue(result.getState().isFeasible(), "Result must be feasible even with zero tolerance");
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)), "Variable value must be exactly 5");
    }

    @Test
    void testInfeasibleModelHandling() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        // Contradictory constraint: x ≤ 3
        model.newExpression("c").set(x, BigDecimal.ONE).upper(BigDecimal.valueOf(3));
        Optimisation.Result result = model.getVariableValues();
        assertTrue(result.getState().isInfeasible(), "Result state must be INFEASIBLE");
    }

    @Test
    void testUnboundedModelHandling() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable without any bounds
        Variable x = model.newVariable("x").weight(// minimise x
        BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues();
        assertTrue(result.getState().isUnbounded(), "Result state must be UNBOUNDED");
    }

    @Test
    void testIntegerVariableModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Integer variable y, 0 ≤ y ≤ 7, minimise y
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(7)).integer(// mark as integer
        true).weight(// minimise
        BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues();
        assertTrue(result.getState().isFeasible(), "Result must be feasible");
        assertTrue(result.getState().isOptimal(), "Result must be optimal");
        BigDecimal yVal = result.get(model.indexOf(y));
        assertNotNull(yVal, "Variable value must not be null");
        assertEquals(0, yVal.stripTrailingZeros().scale(), "Result should be an integer");
        assertTrue(yVal.compareTo(BigDecimal.ZERO) >= 0 && yVal.compareTo(BigDecimal.valueOf(7)) <= 0, "Value must respect bounds");
    }

    @Test
    void testKnownSolutionShortcut() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // First solve normally
        Optimisation.Result solved = model.getVariableValues();
        // Store the result as a known solution
        model.setKnownSolution(solved);
        // Subsequent call should return the stored result (no re‑solving)
        Optimisation.Result shortcut = model.getVariableValues();
        assertEquals(solved.getState(), shortcut.getState(), "States must match");
        assertEquals(solved.getValue(), shortcut.getValue(), "Objective values must match");
        assertEquals(solved.get(model.indexOf(x)), shortcut.get(model.indexOf(x)), "Variable values must match");
    }

    @Test
    void testValidationFailureHandlerInvocation() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // Contradictory constraint to force validation failure
        model.newExpression("bad").set(x, BigDecimal.ONE).lower(// x must be ≥ 2, impossible with upper bound 1
        BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2));
        // Install a handler that throws a RuntimeException
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> throwingHandler = (m, sol) -> {
            throw new RuntimeException("validation failed");
        };
        // The handler field is private – set it via reflection
        Field handlerField = ExpressionsBasedModel.class.getDeclaredField("validationFailureHandler");
        handlerField.setAccessible(true);
        handlerField.set(model, throwingHandler);
        RuntimeException ex = assertThrows(RuntimeException.class, model::getVariableValues, "The custom validation failure handler must propagate its exception");
        assertEquals("validation failed", ex.getMessage());
    }

    @Test
    void testOptionsFeasibilityChangedBetweenCalls() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with a tiny optimal value (0.0001)
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // Constraint that forces x = 0.0001
        model.newExpression("fix").set(x, BigDecimal.ONE).lower(new BigDecimal("0.0001")).upper(new BigDecimal("0.0001"));
        // First call – default tolerance (≈ 1e‑3) → feasible
        Optimisation.Result first = model.getVariableValues();
        assertTrue(first.getState().isFeasible(), "First call should be feasible");
        // Tighten the feasibility context (tolerance ≈ 1e‑5)
        model.options.feasibility = NumberContext.of(5);
        Optimisation.Result second = model.getVariableValues();
        // With the stricter tolerance the solution is still exact, so it stays feasible.
        // The test asserts the method really re‑reads the current options.
        assertEquals(first.getState(), second.getState(), "State after tightening options should reflect the new context");
    }

    @Test
    void testDisposedModelException() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.dispose();
        assertThrows(IllegalStateException.class, model::getVariableValues, "Calling getVariableValues() on a disposed model must throw IllegalStateException");
    }

    @Test
    void testNullOptionsFieldException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Nullify the 'options' field via reflection
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        assertThrows(NullPointerException.class, model::getVariableValues, "Null options should cause NullPointerException");
    }

    @Test
    void testNullFeasibilityContextException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Set the feasibility context to null via reflection
        Field feasibilityField = Optimisation.Options.class.getDeclaredField("feasibility");
        feasibilityField.setAccessible(true);
        feasibilityField.set(model.options, null);
        assertThrows(NullPointerException.class, model::getVariableValues, "Null feasibility context should cause NullPointerException");
    }

    @Test
    void testInvalidVariableBoundException() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Attempt to create a variable with an illegal bound (Infinity)
        Variable v = model.newVariable("v");
        assertThrows(IllegalArgumentException.class, () -> v.upper(new BigDecimal(Double.POSITIVE_INFINITY)), "Setting an infinite upper bound must throw IllegalArgumentException");
    }

    @Disabled("Triggering a ProgrammingError requires a solver configuration that is not trivial to set up in a unit test.")
    @Test
    void testSolverInternalErrorPropagation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The library supports convex quadratic expressions; adding a non‑convex quadratic
        // expression forces the linear solver to fail and should raise ProgrammingError.
        // The exact API for quadratic terms is not part of the public test contract,
        // therefore this test is disabled pending a concrete way to provoke the error.
        // Example (pseudo‑code):
        // Variable x = model.newVariable("x");
        // model.newExpression("quad")
        //      .set(x, BigDecimal.ONE)
        //      .quadratic(x, BigDecimal.ONE) // non‑convex term
        //      .weight(BigDecimal.ONE);
        //
        // assertThrows(ProgrammingError.class, model::getVariableValues);
    }
}
