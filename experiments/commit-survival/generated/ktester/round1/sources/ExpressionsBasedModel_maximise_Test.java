package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_maximise_Test {

    class TestPresolver implements Presolver, Comparable<Presolver> {

        final AtomicBoolean called = new AtomicBoolean(false);

        @Override
        public int compareTo(Presolver o) {
            // All test presolvers have the same priority.
            return 0;
        }

        @Override
        public boolean presolve(ExpressionsBasedModel model) {
            called.set(true);
            // Remove the redundant constraint if it exists.
            model.removeExpression("redundant");
            // indicate that the model was modified
            return true;
        }
    }

    class TestPresolver implements Presolver, Comparable<Presolver> {

        final AtomicBoolean called = new AtomicBoolean(false);

        @Override
        public int compareTo(Presolver o) {
            // All test presolvers have the same priority.
            return 0;
        }

        @Override
        public boolean presolve(ExpressionsBasedModel model) {
            called.set(true);
            // Remove the redundant constraint if it exists.
            model.removeExpression("redundant");
            // indicate that the model was modified
            return true;
        }
    }

    /**
     * Simple sanity check: maximise a single variable with a positive weight.
     * The optimal solution should push the variable to its upper bound.
     */
    @Test
    void testMaximiseSingleVariable() {
        // Arrange – create a model with one variable (bounds 0 … 10) and weight 1.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// lower bound = 0
        BigDecimal.ZERO).upper(// upper bound = 10
        BigDecimal.TEN).weight(// contribution to objective
        BigDecimal.ONE);
        // Act – maximise the model.
        Optimisation.Result result = model.maximise();
        // Assert – the optimisation should be successful and optimal.
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getState().isOptimal(), "Result state should be optimal");
        // The objective value for a single variable with weight 1 equals the variable value.
        BigDecimal expectedObjective = BigDecimal.TEN;
        assertEquals(0, expectedObjective.compareTo(result.getValue()), "Objective value should be 10");
        // The variable itself should be at its upper bound.
        int index = model.indexOf(x);
        BigDecimal xValue = result.get(index);
        assertEquals(0, BigDecimal.TEN.compareTo(xValue), "Variable x should be at its upper bound (10)");
    }

    /**
     * Verify that calling {@code maximise()} after a previous call to {@code minimise()}
     * correctly switches the optimisation sense to MAX and produces the expected result.
     */
    @Test
    void testSwitchSenseFromMinimiseToMaximise() {
        // Arrange – a model with two variables, each bounded [0,5] and weight 1.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        Variable b = model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // Add a simple constraint: a + b ≤ 5 (so the sum cannot exceed 5).
        model.newExpression("c1").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        // Act – first minimise (should push the sum to its lower bound 0),
        // then maximise (should push the sum to its upper bound 5).
        Optimisation.Result minResult = model.minimise();
        Optimisation.Result maxResult = model.maximise();
        // Assert – minimise result.
        assertTrue(minResult.getState().isOptimal(), "Minimise should be optimal");
        assertEquals(0, minResult.getValue().compareTo(BigDecimal.ZERO), "Minimise objective should be 0");
        assertEquals(0, minResult.get(a).compareTo(BigDecimal.ZERO), "Variable a should be 0 after minimise");
        assertEquals(0, minResult.get(b).compareTo(BigDecimal.ZERO), "Variable b should be 0 after minimise");
        // Assert – maximise result.
        assertTrue(maxResult.getState().isOptimal(), "Maximise should be optimal");
        assertEquals(0, maxResult.getValue().compareTo(BigDecimal.valueOf(5)), "Maximise objective should be 5");
        // The exact distribution between a and b is not important; they must satisfy the constraint.
        BigDecimal sum = maxResult.get(a).add(maxResult.get(b));
        assertEquals(0, sum.compareTo(BigDecimal.valueOf(5)), "Sum of a and b should be 5 after maximise");
    }

    @Test
    void testInfeasibleModel() {
        // Arrange – variable x with contradictory bounds (x ≥ 5 and x ≤ 3).
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ONE);
        // Act
        Optimisation.Result result = model.maximise();
        // Assert – the solver must report infeasibility and the sense must be MAX.
        assertTrue(result.getState().isInfeasible(), "Result should be INFEASIBLE");
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense must be MAX after maximise()");
    }

    @Test
    void testUnboundedModel() {
        // Arrange – variable x without an upper bound, weight 1.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// only a lower bound
        BigDecimal.ZERO).weight(BigDecimal.ONE);
        // Act
        Optimisation.Result result = model.maximise();
        // Assert – the solver must report UNBOUNDED and the sense must be MAX.
        assertTrue(result.getState().isUnbounded(), "Result should be UNBOUNDED");
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense must be MAX after maximise()");
    }

    @Test
    void testSenseIsSetWhenNoVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Pre‑condition: default sense is MIN (the implementation uses MIN by default,
        // but we do not rely on that – we explicitly set it to MIN first).
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Pre‑condition: sense should be MIN");
        // Act
        model.maximise();
        // Assert
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "maximise() must set the optimisation sense to MAX");
    }

    @Test
    void testOverridesExistingSense() {
        // Arrange – set sense to MIN explicitly.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Pre‑condition: sense should be MIN");
        // Add a simple variable.
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Act
        Optimisation.Result result = model.maximise();
        // Assert – sense must now be MAX and result optimal.
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Sense must be switched to MAX before solving");
        assertTrue(result.getState().isOptimal(), "Result should be OPTIMAL");
        assertEquals(0, result.getValue().compareTo(BigDecimal.TEN), "Objective value should be 10");
        assertEquals(0, result.get(x).compareTo(BigDecimal.TEN), "Variable x should be at its upper bound (10)");
    }

    @Test
    void testIntegerVariableModel() {
        // Arrange – integer variable x, bounds 0..5, weight 1.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE).integer(// mark as integer
        true);
        // Act
        Optimisation.Result result = model.maximise();
        // Assert – optimal integer solution x = 5.
        assertTrue(result.getState().isOptimal(), "Result should be OPTIMAL");
        assertEquals(0, result.getValue().compareTo(BigDecimal.valueOf(5)), "Objective value should be 5");
        assertEquals(0, result.get(x).compareTo(BigDecimal.valueOf(5)), "Integer variable x should be 5");
    }

    @Test
    void testObjectiveConstantOnly() {
        // Arrange – empty model, add a constant 42 to the objective.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addObjectiveConstant(BigDecimal.valueOf(42));
        // Act
        Optimisation.Result result = model.maximise();
        // Assert – optimal, objective = 42, no variable values.
        assertTrue(result.getState().isOptimal(), "Result should be OPTIMAL");
        assertEquals(0, result.getValue().compareTo(BigDecimal.valueOf(42)), "Objective value should be the constant 42");
        // No variables → the result should contain zero entries.
        assertEquals(0, result.size(), "Result should contain no variable assignments");
    }

    @Test
    void testPresolverInvokedAndModelReduced() {
        // A very small custom presolver that simply records that it has been called.
        class TestPresolver implements Presolver, Comparable<Presolver> {

            final AtomicBoolean called = new AtomicBoolean(false);

            @Override
            public int compareTo(Presolver o) {
                // All test presolvers have the same priority.
                return 0;
            }

            @Override
            public boolean presolve(ExpressionsBasedModel model) {
                called.set(true);
                // Remove the redundant constraint if it exists.
                model.removeExpression("redundant");
                // indicate that the model was modified
                return true;
            }
        }
        TestPresolver presolver = new TestPresolver();
        // Register the presolver (static registration).
        ExpressionsBasedModel.addPresolver(presolver);
        // Build a model with a redundant constraint: x ≤ 100 while x already has upper bound 10.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(// real upper bound = 10
        BigDecimal.TEN).weight(BigDecimal.ONE);
        // Redundant constraint
        model.newExpression("redundant").set(x, BigDecimal.ONE).upper(BigDecimal.valueOf(100));
        // Act
        Optimisation.Result result = model.maximise();
        // Assert – presolver must have been called.
        assertTrue(presolver.called.get(), "Custom presolver should have been invoked");
        // Result must still be optimal with x = 10.
        assertTrue(result.getState().isOptimal(), "Result should be OPTIMAL");
        assertEquals(0, result.getValue().compareTo(BigDecimal.TEN), "Objective value should be 10");
        assertEquals(0, result.get(x).compareTo(BigDecimal.TEN), "Variable x should be at its true upper bound (10)");
        // Clean‑up: deregister the test presolver to avoid side effects on other tests.
        ExpressionsBasedModel.removePresolver(presolver);
    }

    @Test
    void testAfterMinimiseCall() {
        // Arrange – feasible model with a single variable.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // First minimise (should set sense to MIN).
        Optimisation.Result minResult = model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "After minimise() the sense should be MIN");
        // Act – now maximise.
        Optimisation.Result maxResult = model.maximise();
        // Assert – sense must be MAX and the optimum is the same numeric value (10).
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "maximise() must flip the sense back to MAX");
        assertTrue(maxResult.getState().isOptimal(), "Result should be OPTIMAL");
        assertEquals(0, maxResult.getValue().compareTo(BigDecimal.TEN), "Objective value after maximise should be 10");
        assertEquals(0, maxResult.get(x).compareTo(BigDecimal.TEN), "Variable x should be at its upper bound (10)");
    }

    @Test
    void testKnownSolutionIsIgnored() {
        // Arrange – simple model.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Compute a solution via minimise (this will also set a known solution internally).
        Optimisation.Result known = model.minimise();
        // Explicitly store the known solution.
        model.setKnownSolution(known);
        // Act – call maximise().
        Optimisation.Result result = model.maximise();
        // Assert – maximise must recompute (objective 10, not the minimise value 0).
        assertTrue(result.getState().isOptimal(), "Result should be OPTIMAL");
        assertEquals(0, result.getValue().compareTo(BigDecimal.TEN), "maximise() must recompute the optimum, not reuse the known solution");
        assertEquals(0, result.get(x).compareTo(BigDecimal.TEN), "Variable x should be at its upper bound (10)");
    }

    @Test
    void testAfterDisposeThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.dispose();
        // The contract states that a disposed model should refuse further optimisation.
        assertThrows(ProgrammingError.class, model::maximise, "Calling maximise() on a disposed model should throw ProgrammingError");
    }

    @Test
    void testInvalidVariableBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // lower > upper – this is illegal.
        model.newVariable("x").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // Optimisation should fail with an IllegalArgumentException (validation occurs inside optimise()).
        assertThrows(IllegalArgumentException.class, model::maximise, "Variable with lower bound > upper bound must cause IllegalArgumentException");
    }

    @Test
    void testUnsupportedQuadraticObjective() {
        // Arrange – build a model with a quadratic term (x²) in the objective.
        // The public API for quadratic terms is not part of the simplified
        // documentation, but Expression provides a `quadratic` method in the
        // actual library.  We use reflection to add the term without depending
        // on the exact method signature.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Obtain the objective expression and add a quadratic coefficient via reflection.
        Expression obj = model.objective();
        try {
            java.lang.reflect.Method quadraticMethod = Expression.class.getMethod("quadratic", Variable.class, Number.class);
            // add 1 * x^2
            quadraticMethod.invoke(obj, x, BigDecimal.ONE);
        } catch (Exception e) {
            // If the method does not exist we simply skip the test – the library version
            // used in the execution environment does not support quadratic expressions.
            Assumptions.assumeTrue(false, "Quadratic API not available in this version");
        }
        // Act & Assert – expecting a ProgrammingError because no quadratic solver is configured.
        assertThrows(ProgrammingError.class, model::maximise, "Quadratic objective without a suitable solver must raise ProgrammingError");
    }

    @Test
    void testNullObjectiveConstant() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Adding a null constant should raise NPE when the model is later processed.
        assertThrows(NullPointerException.class, () -> model.addObjectiveConstant(null), "addObjectiveConstant(null) must throw NullPointerException");
    }
}
