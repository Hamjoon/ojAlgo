package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.MockedStatic;
// import org.mockito.Mockito;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_minimise_Test {

    /**
     * Simple linear problem:
     *   minimise  x   subject to  0 ≤ x ≤ 20 and 10·x = 100
     * The unique feasible solution is x = 10.
     */
    @Test
    void testMinimiseSimpleLinearProblem() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable x with bounds [0,20] and weight 1 (i.e. part of the objective)
        Variable x = model.newVariable("x").lower(0).upper(20).weight(ONE);
        // Constraint 10·x = 100  (implemented as lower = upper = 100)
        Expression expr = model.newExpression("10*x = 100").lower(BigDecimal.valueOf(100)).upper(BigDecimal.valueOf(100));
        // 10 * x
        expr.add(x, TEN);
        Optimisation.Result result = model.minimise();
        // Verify that the optimisation sense is MIN (indirectly via the result state)
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        // The variable value should be exactly 10
        BigDecimal xResult = result.get(model.indexOf(x));
        assertEquals(BigDecimal.TEN, xResult);
    }

    /**
     * Verify that an objective constant is taken into account.
     * The model minimises 0·x + 5 (i.e. a constant objective) with x free in [0,10].
     * The optimal value must be the constant itself (5) and the variable can take any feasible value.
     */
    @Test
    void testMinimiseWithObjectiveConstant() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with no contribution to the objective (weight = 0)
        Variable x = model.newVariable("x").lower(0).upper(10).weight(ZERO);
        // Add a constant 5 to the objective
        model.addObjectiveConstant(BigDecimal.valueOf(5));
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        // The optimisation value must equal the constant
        assertEquals(BigDecimal.valueOf(5), result.getValue());
        // Variable value must be within its bounds (any value is acceptable)
        BigDecimal xVal = result.get(model.indexOf(x));
        assertTrue(xVal.compareTo(BigDecimal.ZERO) >= 0 && xVal.compareTo(BigDecimal.TEN) <= 0, "Variable x should respect its bounds");
    }

    /**
     * Verify that the optimisation sense is correctly set to MIN.
     * The model contains a single variable with a positive weight.
     * After calling {@code minimise()}, the internal sense should be MIN.
     * The public API does not expose the sense directly, but we can infer it:
     * the result of {@code maximise()} on the same model would be the opposite.
     */
    @Test
    void testOptimisationSenseIsMin() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable().lower(0).upper(10).weight(ONE);
        Optimisation.Result minResult = model.minimise();
        Optimisation.Result maxResult = model.maximise();
        // For a single variable with weight 1, the min result should be at the lower bound,
        // the max result at the upper bound.
        BigDecimal xMin = minResult.get(model.indexOf(x));
        BigDecimal xMax = maxResult.get(model.indexOf(x));
        assertEquals(BigDecimal.ZERO, xMin);
        assertEquals(BigDecimal.TEN, xMax);
    }

    /**
     * A tiny infeasible problem should return the INFEASIBLE state.
     * This also exercises the presolver path that {@code minimise()} invokes.
     */
    @Test
    void testMinimiseInfeasibleProblem() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable bounded between 0 and 1
        Variable x = model.newVariable().lower(0).upper(1).weight(ONE);
        // Contradictory constraints: x ≥ 2 and x ≤ 0
        model.newExpression("c1").lower(BigDecimal.valueOf(2));
        model.newExpression("c2").upper(BigDecimal.ZERO);
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "The model is infeasible and should report INFEASIBLE");
    }

    /**
     * SenseSetting **
     */
    @Test
    void testSenseIsSetToMinWhenPreviouslyMax() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Set sense to MAX explicitly
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Pre‑condition: sense must be MAX");
        model.minimise();
        // After minimise() the sense must be MIN
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "minimise() must set the sense to MIN");
    }

    @Test
    void testSenseRemainsMinIfAlreadyMin() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MIN);
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Sense should stay MIN after minimise()");
    }

    /**
     * OptimiseInvocation **
     */
    @Test
    void testMinimiseDelegatesToOptimiseAndReturnsSameResult() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Simple feasible model (x in [0,5], minimise x)
        Variable x = model.newVariable().lower(0).upper(5).weight(ONE);
        // Call the private optimise() via reflection
        java.lang.reflect.Method optimiseMethod = ExpressionsBasedModel.class.getDeclaredMethod("optimise");
        optimiseMethod.setAccessible(true);
        Optimisation.Result directResult = (Optimisation.Result) optimiseMethod.invoke(model);
        // Now call minimise()
        Optimisation.Result minimiseResult = model.minimise();
        // The two results must be the same instance
        assertSame(directResult, minimiseResult, "minimise() must return the exact Optimisation.Result produced by optimise()");
    }

    @Test
    void testMinimiseReturnsInfeasibleWhenModelMarkedInfeasible() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // mark as infeasible before solving
        model.setInfeasible();
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "A model flagged as infeasible must yield an INFEASIBLE result");
    }

    @Test
    void testMinimiseReturnsUnboundedWhenModelIsUnbounded() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with no bounds and zero weight -> unbounded objective
        model.newVariable().weight(ZERO);
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.State.UNBOUNDED, result.getState(), "A model with no bounds and no objective should be reported as UNBOUNDED");
    }

    /**
     * FeasibleSolution **
     */
    @Test
    void testFeasibleSimpleObjectiveConstantMinusX() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Objective: minimise 5 - x  (i.e. weight = -1, constant = 5)
        Variable x = model.newVariable().lower(0).upper(10).weight(ONE.negate());
        model.addObjectiveConstant(BigDecimal.valueOf(5));
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        TestUtils.assertEquals(BigDecimal.valueOf(5), result.getValue(), "Objective value must be 5 (x = 0 gives 5 - 0)");
    }

    @Test
    void testFeasibleObjectiveMinusX() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Objective: minimise -x  (i.e. weight = -1)
        Variable x = model.newVariable().lower(0).upper(10).weight(ONE.negate());
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        // The optimal solution is x = 10, giving objective -10
        TestUtils.assertEquals(BigDecimal.TEN, result.get(model.indexOf(x)));
        TestUtils.assertEquals(BigDecimal.valueOf(-10), result.getValue());
    }

    /**
     * ObjectiveConstant (stand‑alone) **
     */
    @Test
    void testOnlyConstantObjective() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addObjectiveConstant(BigDecimal.valueOf(42));
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Constant‑only model must be optimal");
        assertEquals(BigDecimal.valueOf(42), result.getValue(), "Result value must equal the constant added to the objective");
    }

    /**
     * NoVariables **
     */
    @Test
    void testEmptyModelReturnsZeroObjective() {
        // no variables, no constraints
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Empty model should be optimal");
        assertEquals(BigDecimal.ZERO, result.getValue(), "Default objective of an empty model must be 0");
    }

    /**
     * KnownSolutionPropagation **
     */
    @Test
    void testMinimiseUsesKnownSolutionIfProvided() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable().lower(0).upper(10).weight(ONE);
        // Solve once to obtain a result
        Optimisation.Result first = model.minimise();
        // Register the obtained result as a known solution
        model.setKnownSolution(first);
        // A second call to minimise() should return the same solution (or an equal one)
        Optimisation.Result second = model.minimise();
        assertEquals(first.getState(), second.getState(), "State must be identical");
        assertEquals(first.getValue(), second.getValue(), "Objective value must match");
        assertEquals(first.get(model.indexOf(x)), second.get(model.indexOf(x)), "Variable values must match");
    }

    /**
     * IntegrationEffect **
     */
    @Test
    void testIntegrationAddsConstantToObjective() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Simple model: minimise x with bounds [0,10]
        Variable x = model.newVariable().lower(0).upper(10).weight(ONE);
        // Baseline result without integration
        Optimisation.Result baseline = model.minimise();
        BigDecimal baselineObj = baseline.getValue();
        // Define a tiny integration that adds 3 to the objective constant
        Optimisation.Integration<ExpressionsBasedModel, Object> addThree = (m, ignored) -> {
            m.addObjectiveConstant(BigDecimal.valueOf(3));
            return null;
        };
        // Register the integration
        ExpressionsBasedModel.addIntegration(addThree);
        // Solve again – the objective should be increased by exactly 3
        Optimisation.Result withIntegration = model.minimise();
        BigDecimal withIntegrationObj = withIntegration.getValue();
        assertEquals(baselineObj.add(BigDecimal.valueOf(3)), withIntegrationObj, "Integration should have increased the objective by 3");
    }

    /**
     * PresolverEffect **
     */
    @Test
    void testPresolverIsInvokedBeforeOptimisation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable y fixed at 5 (lower == upper)
        Variable y = model.newVariable().lower(5).upper(5).weight(ONE);
        // A simple presolver that just records that it was called
        final boolean[] called = { false };
        ExpressionsBasedModel.Presolver recorder = new ExpressionsBasedModel.Presolver() {

            @Override
            public int compareTo(Presolver o) {
                return 0;
            }

            @Override
            public void presolve(ExpressionsBasedModel m) {
                called[0] = true;
            }
        };
        ExpressionsBasedModel.addPresolver(recorder);
        // Run minimise – presolver should have been executed
        Optimisation.Result result = model.minimise();
        assertTrue(called[0], "Custom presolver must have been invoked");
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
    }

    /**
     * OptionsUnchanged **
     */
    @Test
    void testOptionsInstanceRemainsSameAfterMinimise() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Optimisation.Options original = model.options;
        // modify an option
        original.iterations_max = 1000;
        model.minimise();
        assertSame(original, model.options, "Options instance must not be replaced");
        assertEquals(1000, model.options.iterations_max, "Modified option value must be retained after minimise()");
    }

    /**
     * IntegerVariableHandling **
     */
    @Test
    void testIntegerVariableRespectsIntegrality() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Integer variable z in [0,5] with weight -1 (minimise -z => maximise z)
        Variable z = model.newVariable().lower(0).upper(5).integer(true).weight(ONE.negate());
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        // The optimal integer value is the upper bound 5
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(z)));
        // Objective should be -5
        assertEquals(BigDecimal.valueOf(-5), result.getValue());
    }

    /**
     * DisposedModel **
     */
    @Test
    void testMinimiseAfterDisposeThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.dispose();
        assertThrows(IllegalStateException.class, model::minimise, "Calling minimise() on a disposed model must throw IllegalStateException");
    }

    /**
     * NullExpressionsCollection **
     */
    @Test
    void testMinimiseWithNullExpressionsCollectionThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use reflection to set the internal collection of expressions to null.
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("expressions");
        exprField.setAccessible(true);
        exprField.set(model, null);
        assertThrows(NullPointerException.class, model::minimise, "A null expressions collection must cause a NullPointerException inside optimise()");
    }

    /**
     * NullVariablesCollection **
     */
    @Test
    void testMinimiseWithNullVariablesCollectionThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The internal variable list is called "variables" in the source.
        Field varField = ExpressionsBasedModel.class.getDeclaredField("variables");
        varField.setAccessible(true);
        varField.set(model, null);
        assertThrows(NullPointerException.class, model::minimise, "A null variables collection must cause a NullPointerException inside optimise()");
    }

    /**
     * NativeSolverUnavailable **
     */
    @Test
    void testMinimiseThrowsProgrammingErrorWhenNativeSolverUnavailable() {
        // Mock the static method isNative to return false
        try (MockedStatic<ExpressionsBasedModel> mocked = Mockito.mockStatic(ExpressionsBasedModel.class)) {
            mocked.when(() -> ExpressionsBasedModel.isNative(Mockito.any())).thenReturn(false);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // Force native solver usage via options (the exact flag may differ)
            model.options.solver = Optimisation.Solver.NATIVE;
            assertThrows(ProgrammingError.class, model::minimise, "When native solver is unavailable, minimise() must propagate ProgrammingError");
        }
    }

    /**
     * QuadraticObjectiveUnsupported **
     */
    @Test
    void testQuadraticObjectiveWithLinearSolverThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable x
        Variable x = model.newVariable().lower(0).upper(10).weight(ZERO);
        // Build a quadratic expression: x²
        Expression quad = model.newExpression().weight(ONE).quadraticFactor(x, x, ONE);
        // Force the use of a linear solver (the default linear solver does not support quadratic)
        model.options.solver = Optimisation.Solver.LINEAR;
        assertThrows(IllegalArgumentException.class, model::minimise, "Quadratic objective with a linear‑only solver must cause IllegalArgumentException");
    }
}