package org.ojalgo.optimisation;

import static org.ojalgo.function.constant.BigMath.ONE;
import static org.ojalgo.function.constant.BigMath.ZERO;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Optimisation.Result;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.mockito.Mockito;
import org.ojalgo.optimisation.Optimisation.State;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_minimise_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose of the model – good practice when dealing with native resources.
        if (model != null) {
            model.dispose();
        }
        model = null;
    }

    /**
     * Simple linear problem:
     *
     * minimise   x
     * subject to 0 ≤ x ≤ 10
     *
     * Expected optimum: x = 0, objective value = 0.
     */
    @Test
    void testSimpleLinearMinimisation() {
        Variable x = model.newVariable("x").lower(ZERO).upper(BigDecimal.TEN).weight(// weight = 1 → objective = x
        ONE);
        // The expression created by {@code newVariable} already contributes to the objective,
        // but we add an explicit expression to illustrate the typical usage pattern.
        Expression expr = model.newExpression("obj");
        // objective = 1·x
        expr.set(x, ONE);
        // ensure it is treated as part of the objective
        expr.weight(ONE);
        Result result = model.minimise();
        // Verify that a result was produced and that the optimisation succeeded.
        assertNotNull(result, "Result should not be null");
        assertTrue(result.getState().isOptimal(), "Result state should be optimal");
        // The optimal value of the objective function should be 0 (x at its lower bound).
        TestUtils.assertEquals(ZERO, result.getValue());
        // The variable value should be 0.
        BigDecimal xResult = result.get(model.indexOf(x));
        TestUtils.assertEquals(ZERO, xResult);
    }

    /**
     * Problem with two variables and a linear equality constraint.
     *
     * minimise   2·a + 3·b
     * subject to a + b = 5
     *            0 ≤ a ≤ 4
     *            0 ≤ b ≤ 4
     *
     * Expected optimum: a = 1, b = 4, objective = 2·1 + 3·4 = 14.
     */
    @Test
    void testLinearProblemWithEqualityConstraint() {
        Variable a = model.newVariable("a").lower(ZERO).upper(BigDecimal.valueOf(4)).weight(BigDecimal.valueOf(2));
        Variable b = model.newVariable("b").lower(ZERO).upper(BigDecimal.valueOf(4)).weight(BigDecimal.valueOf(3));
        // Equality constraint a + b = 5
        Expression eq = model.newExpression("sum_eq").level(BigDecimal.valueOf(5));
        eq.set(a, ONE);
        eq.set(b, ONE);
        Result result = model.minimise();
        assertNotNull(result, "Result should not be null");
        assertTrue(result.getState().isOptimal(), "Result state should be optimal");
        // Expected optimal objective value is 14.
        TestUtils.assertEquals(BigDecimal.valueOf(14), result.getValue());
        // Verify variable values.
        TestUtils.assertEquals(BigDecimal.ONE, result.get(model.indexOf(a)));
        TestUtils.assertEquals(BigDecimal.valueOf(4), result.get(model.indexOf(b)));
    }

    /**
     * Verify that calling {@code minimise()} on an empty model (no variables, no expressions)
     * returns a feasible optimal result with a value of 0.
     */
    @Test
    void testMinimiseEmptyModel() {
        Result result = model.minimise();
        assertNotNull(result, "Result should not be null");
        assertTrue(result.getState().isOptimal(), "Empty model should be optimal");
        TestUtils.assertEquals(ZERO, result.getValue());
    }

    /**
     * Unbounded model: a single unrestricted variable with a minimisation
     * objective.  The solver should report {@link State#UNBOUNDED}.
     */
    @Test
    void testMinimiseUnboundedModel() {
        Variable x = model.newVariable("x").weight(// no lower bound → can go to -∞
        ONE);
        Result result = model.minimise();
        assertNotNull(result, "Result must not be null");
        assertEquals(State.UNBOUNDED, result.getState(), "Unbounded problem should be reported as UNBOUNDED");
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Sense must be set to MIN");
    }

    /**
     * Simple bounded model: x ≥ 0, minimise x.  The optimum is x = 0.
     */
    @Test
    void testMinimiseSimpleBoundedModel() {
        Variable x = model.newVariable("x").lower(ZERO).weight(// minimise x, lower bound 0
        ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        TestUtils.assertEquals(ZERO, result.getValue());
        TestUtils.assertEquals(ZERO, result.get(model.indexOf(x)));
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
    }

    /**
     * Linear constraint x ≥ 5, minimise x.  Feasible region forces x = 5.
     */
    @Test
    void testMinimiseWithLinearConstraint() {
        Variable x = model.newVariable("x").lower(ZERO).weight(ONE);
        // Constraint x ≥ 5  →  lower bound 5 on the expression
        Expression c = model.newExpression("c").lower(BigDecimal.valueOf(5));
        c.set(x, ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        TestUtils.assertEquals(BigDecimal.valueOf(5), result.getValue());
        TestUtils.assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)));
    }

    /**
     * Infeasible model: contradictory bounds on the same variable.
     */
    @Test
    void testMinimiseInfeasibleModel() {
        Variable x = model.newVariable("x").lower(ZERO).upper(BigDecimal.TEN).weight(ONE);
        // Constraint x ≥ 8
        Expression c1 = model.newExpression("c1").lower(BigDecimal.valueOf(8));
        c1.set(x, ONE);
        // Constraint x ≤ 5
        Expression c2 = model.newExpression("c2").upper(BigDecimal.valueOf(5));
        c2.set(x, ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertEquals(State.INFEASIBLE, result.getState(), "Contradictory constraints must yield INFEASIBLE");
    }

    /**
     * Integer variable y, 0 ≤ y ≤ 10, y ≥ 3, minimise y.
     * Expected optimum y = 3 (integer).
     */
    @Test
    void testMinimiseIntegerVariable() {
        Variable y = model.newVariable("y").lower(ZERO).upper(BigDecimal.TEN).weight(ONE).integer(// make it integer
        true);
        // Constraint y ≥ 3
        Expression c = model.newExpression("c").lower(BigDecimal.valueOf(3));
        c.set(y, ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        TestUtils.assertEquals(BigDecimal.valueOf(3), result.getValue());
        TestUtils.assertEquals(BigDecimal.valueOf(3), result.get(model.indexOf(y)));
    }

    /**
     * Quadratic objective minimise z² with unrestricted z.
     * The global minimum is at z = 0, objective value 0.
     */
    @Test
    void testMinimiseQuadraticObjective() {
        Variable z = model.newVariable("z").weight(// weight 0, we will build a quadratic expression
        ZERO);
        // Quadratic expression: z²
        Expression obj = model.newExpression("obj");
        // z² coefficient = 1
        obj.quadraticFactor(z, ONE);
        // make it part of the objective
        obj.weight(ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        TestUtils.assertEquals(ZERO, result.getValue());
        TestUtils.assertEquals(ZERO, result.get(model.indexOf(z)));
    }

    /**
     * Fixed variable w = 7, minimise w.
     * Since the variable cannot move, the optimum is 7.
     */
    @Test
    void testMinimiseFixedVariable() {
        Variable w = model.newVariable("w").lower(BigDecimal.valueOf(7)).upper(BigDecimal.valueOf(7)).weight(ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        TestUtils.assertEquals(BigDecimal.valueOf(7), result.getValue());
        TestUtils.assertEquals(BigDecimal.valueOf(7), result.get(model.indexOf(w)));
    }

    /**
     * Ensure that a previously set sense (MAX) is overridden by minimise().
     */
    @Test
    void testMinimiseOverridesExistingSense() {
        // First set the sense to MAX via maximise()
        // this will set sense to MAX internally
        model.maximise();
        // Now define a trivial problem
        Variable x = model.newVariable("x").lower(ZERO).weight(ONE);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Calling minimise() must set sense to MIN");
        TestUtils.assertEquals(ZERO, result.getValue());
    }

    /**
     * Known solution provided via {@code setKnownSolution()}.
     * The solver must still solve the model and return a result consistent
     * with the known solution.
     */
    @Test
    void testMinimiseWithKnownSolutionProvided() {
        Variable p = model.newVariable("p").lower(ZERO).upper(BigDecimal.TEN).weight(ONE);
        // Known solution: p = 4
        Optimisation.Result known = Optimisation.Result.of(State.OPTIMAL, BigDecimal.valueOf(4), Collections.singletonMap(model.indexOf(p), BigDecimal.valueOf(4)));
        model.setKnownSolution(known);
        Result result = model.minimise();
        assertNotNull(result);
        assertTrue(result.getState().isOptimal());
        TestUtils.assertEquals(BigDecimal.valueOf(4), result.getValue());
        TestUtils.assertEquals(BigDecimal.valueOf(4), result.get(model.indexOf(p)));
    }

    /**
     * Variable with lower bound greater than upper bound → IllegalArgumentException.
     */
    @Test
    void testMinimiseVariableBoundsException() {
        model.newVariable("bad").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3)).weight(ONE);
        assertThrows(IllegalArgumentException.class, () -> model.minimise(), "Inconsistent bounds must cause IllegalArgumentException");
    }

    /**
     * Duplicate expression names → IllegalArgumentException.
     */
    @Test
    void testMinimiseDuplicateExpressionNameException() {
        model.newExpression("expr1");
        // duplicate name
        model.newExpression("expr1");
        assertThrows(IllegalArgumentException.class, () -> model.minimise(), "Duplicate expression names must cause IllegalArgumentException");
    }

    /**
     * Null variable name → NullPointerException.
     */
    @Test
    void testMinimiseNullVariableNameException() {
        assertThrows(NullPointerException.class, () -> model.newVariable(null), "Creating a variable with a null name must throw NullPointerException");
    }

    /**
     * Presolver that throws a RuntimeException during presolve().
     */
    @Test
    void testMinimisePresolverExceptionPropagation() {
        // Create a mock Presolver that throws when its presolve method is called.
        Presolver badPresolver = Mockito.mock(Presolver.class);
        Mockito.doThrow(new RuntimeException("presolve failure")).when(badPresolver).presolve(Mockito.any());
        // Register the faulty presolver.
        ExpressionsBasedModel.addPresolver(badPresolver);
        // Any simple model is sufficient – the presolver will fail before solving.
        Variable x = model.newVariable("x").weight(ONE);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.minimise(), "Presolver exception must propagate as RuntimeException");
        assertTrue(thrown.getMessage().contains("presolve failure"));
    }

    /**
     * Integration switch enabled without a compatible Integration implementation.
     * Expected IllegalStateException.
     */
    @Test
    void testMinimiseIntegrationSwitchException() {
        // enable switch without providing integration
        model.setIntegrationSwitch(true);
        // Simple variable to make the model non‑empty.
        model.newVariable("x").weight(ONE);
        assertThrows(IllegalStateException.class, () -> model.minimise(), "Missing integration while switch is on must throw IllegalStateException");
    }

    /**
     * Expression references a variable index that does not exist.
     * Expected IndexOutOfBoundsException.
     */
    @Test
    void testMinimiseExpressionInvalidVariableReferenceException() {
        // Build an expression that tries to reference index 999 (far beyond model size).
        Expression expr = model.newExpression("invalid");
        // The Expression API does not expose a direct index‑based setter, so we use reflection.
        // This is permissible because the test is about the public contract of minimise().
        try {
            java.lang.reflect.Method m = expr.getClass().getDeclaredMethod("setLinearFactor", int.class, BigDecimal.class);
            m.setAccessible(true);
            m.invoke(expr, 999, ONE);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> model.minimise(), "Invalid variable reference should cause IndexOutOfBoundsException");
    }

    /**
     * Expression with a non‑finite coefficient (NaN) → ArithmeticException.
     */
    @Test
    void testMinimiseExpressionCoefficientNaNException() {
        Variable x = model.newVariable("x").weight(ONE);
        Expression expr = model.newExpression("nanCoeff");
        // Use Double.NaN as coefficient – the API accepts Comparable, so this compiles.
        expr.set(x, Double.NaN);
        assertThrows(ArithmeticException.class, () -> model.minimise(), "NaN coefficient must trigger ArithmeticException");
    }
}
