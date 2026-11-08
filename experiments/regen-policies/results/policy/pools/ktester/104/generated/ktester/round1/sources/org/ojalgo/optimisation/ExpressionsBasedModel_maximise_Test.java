package org.ojalgo.optimisation;

import static org.ojalgo.function.constant.BigMath.ONE;
import static org.ojalgo.function.constant.BigMath.TWO;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_maximise_Test {

    private ExpressionsBasedModel model;

    /**
     * Create a fresh model before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    /**
     * Clean up after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose of the model to free any native resources the
        // optimisation engine might hold.
        if (model != null) {
            model.dispose();
            model = null;
        }
    }

    /**
     * Simple linear maximisation:
     *
     * <pre>
     * maximise   f = 1·x + 2·y
     * subject to  x + y ≤ 4
     *             x ≥ 0 , y ≥ 0
     * </pre>
     *
     * The analytical optimum is x = 0, y = 4, with f = 8.
     */
    @Test
    void testSimpleLinearMaximise() {
        // ----- variables -------------------------------------------------------
        Variable x = model.newVariable("x").lower(// x ≥ 0
        BigDecimal.ZERO).weight(// contribution 1·x to the objective
        ONE);
        Variable y = model.newVariable("y").lower(// y ≥ 0
        BigDecimal.ZERO).weight(// contribution 2·y to the objective
        TWO);
        // ----- constraint -------------------------------------------------------
        Expression constraint = model.addExpression("c1").upper(// x + y ≤ 4
        BigDecimal.valueOf(4));
        constraint.set(x, ONE);
        constraint.set(y, ONE);
        // ----- optimisation -----------------------------------------------------
        Optimisation.Result result = model.maximise();
        // ----- assertions -------------------------------------------------------
        assertTrue(result.getState().isOptimal(), () -> "Expected optimisation state to be optimal, but was " + result.getState());
        // Objective value should be exactly 8 (or very close due to rounding)
        BigDecimal expectedValue = BigDecimal.valueOf(8);
        assertEquals(0, expectedValue.compareTo(result.getValue()), () -> "Expected objective value 8, but got " + result.getValue());
        // Variable values
        assertEquals(BigDecimal.ZERO, result.get(model.indexOf(x)), () -> "Expected x = 0, but got " + result.get(model.indexOf(x)));
        assertEquals(BigDecimal.valueOf(4), result.get(model.indexOf(y)), () -> "Expected y = 4, but got " + result.get(model.indexOf(y)));
    }

    /**
     * Verify that calling {@code maximise()} on a model without any
     * variables or constraints yields a feasible (zero) solution.
     */
    @Test
    void testMaximiseOnEmptyModel() {
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isFeasible(), () -> "Empty model should be feasible, but state was " + result.getState());
        // With no variables, the objective value defaults to zero.
        assertEquals(BigDecimal.ZERO, result.getValue(), () -> "Expected objective value 0 for empty model, but got " + result.getValue());
    }

    /**
     * Verify that {@code maximise()} forces the optimisation sense to
     * {@link Optimisation.Sense#MAX} even if the model started with the
     * default sense {@code MIN}.
     */
    @Test
    void testMaximiseSetsOptimisationSenseToMAX() {
        // Default sense is MIN – we do not change it explicitly.
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "After maximise() the optimisation sense must be MAX");
    }

    /**
     * Simple linear model with a single bounded variable.
     *
     * maximise   f = x
     * subject to 0 ≤ x ≤ 10
     *
     * Expected optimum: x = 10, value = 10, state = OPTIMAL.
     */
    @Test
    void testSimpleLinearModelOptimalResult() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(// upper bound 10
        BigDecimal.TEN).weight(// maximise x
        ONE);
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isOptimal(), () -> "Result should be optimal, but was " + result.getState());
        assertEquals(BigDecimal.TEN, result.getValue(), () -> "Optimal objective value should be 10, but was " + result.getValue());
        assertEquals(BigDecimal.TEN, result.get(model.indexOf(x)), () -> "Variable x should be 10, but was " + result.get(model.indexOf(x)));
    }

    /**
     * Conflicting constraints produce an infeasible model.
     *
     * maximise   f = x + y
     * subject to 0 ≤ x ≤ 5 , 0 ≤ y ≤ 5
     *            x + y ≤ 3
     *            x + y ≥ 8   (conflict)
     */
    @Test
    void testInfeasibleModelReturnsInfeasibleState() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        // x + y ≤ 3
        model.addExpression("c1").upper(BigDecimal.valueOf(3)).set(x, ONE).set(y, ONE);
        // x + y ≥ 8  →  -x - y ≤ -8
        model.addExpression("c2").upper(BigDecimal.valueOf(-8)).set(x, ONE.negate()).set(y, ONE.negate());
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isInfeasible(), () -> "Result should be infeasible, but was " + result.getState());
    }

    /**
     * Model with a single variable that has no upper bound – the solver
     * must report an {@code UNBOUNDED} state.
     */
    @Test
    void testUnboundedModelReturnsUnboundedState() {
        model.newVariable("x").lower(// lower bound only
        BigDecimal.ZERO).weight(// maximise x
        ONE);
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isUnbounded(), () -> "Result should be UNBOUNDED, but was " + result.getState());
    }

    /**
     * Variable bounds are respected – the optimiser must not exceed the
     * declared upper bound.
     *
     * maximise   f = x
     * subject to 2 ≤ x ≤ 3
     *
     * Expected optimum: x = 3, value = 3.
     */
    @Test
    void testVariableBoundsRespected() {
        Variable x = model.newVariable("x").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(3)).weight(ONE);
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isOptimal(), () -> "Result should be optimal, but was " + result.getState());
        assertEquals(BigDecimal.valueOf(3), result.getValue(), () -> "Objective value should be 3, but was " + result.getValue());
        assertEquals(BigDecimal.valueOf(3), result.get(model.indexOf(x)), () -> "Variable x should be 3, but was " + result.get(model.indexOf(x)));
    }

    /**
     * Explicitly set the sense to {@code MIN} before calling {@code maximise()};
     * the method must flip it to {@code MAX}.
     */
    @Test
    void testFlipsSenseFromMinToMaxEvenIfAlreadyMin() {
        model.setOptimisationSense(Optimisation.Sense.MIN);
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "maximise() must set the sense to MAX even if it was MIN before");
    }

    /**
     * If the sense is already {@code MAX} before calling {@code maximise()},
     * it must stay {@code MAX}.
     */
    @Test
    void testKeepsSenseWhenAlreadyMax() {
        model.setOptimisationSense(Optimisation.Sense.MAX);
        // add a trivial feasible objective so that optimisation can finish
        model.newVariable("x").weight(ONE);
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense should remain MAX after maximise()");
    }

    /**
     * Integer variable handling – the optimiser must respect integrality.
     *
     * maximise   f = x
     * subject to 0 ≤ x ≤ 10 , x ≤ 3   (integer variable)
     *
     * Expected optimum: x = 3 (integer), value = 3.
     */
    @Test
    void testIntegerVariableReturnsIntegerOptimalSolution() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(// mark as integer
        true).weight(// maximise x
        ONE);
        // Constraint x ≤ 3
        model.addExpression("c1").upper(BigDecimal.valueOf(3)).set(x, ONE);
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isOptimal(), () -> "Result should be optimal, but was " + result.getState());
        assertEquals(BigDecimal.valueOf(3), result.getValue(), () -> "Objective value should be 3, but was " + result.getValue());
        assertEquals(BigDecimal.valueOf(3), result.get(model.indexOf(x)), () -> "Integer variable x should be 3, but was " + result.get(model.indexOf(x)));
    }

    /**
     * Calling {@code maximise()} after the model has been disposed must
     * throw {@link IllegalStateException}.
     */
    @Test
    void testMaximiseAfterDisposeThrowsIllegalStateException() {
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.maximise(), "Calling maximise() on a disposed model should throw IllegalStateException");
    }

    /**
     * Forcing a {@code null} optimisation sense via reflection and then
     * invoking {@code maximise()} must result in a {@link NullPointerException}.
     */
    @Test
    void testNullOptimisationSenseThrowsNullPointerException() throws Exception {
        // Use reflection to set the private field that stores the sense to null.
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        Optimisation.Options opts = (Optimisation.Options) optionsField.get(model);
        Field senseField = Optimisation.Options.class.getDeclaredField("optimisationSense");
        senseField.setAccessible(true);
        senseField.set(opts, null);
        assertThrows(NullPointerException.class, () -> model.maximise(), "maximise() should throw NullPointerException when optimisation sense is null");
    }

    /**
     * Adding a quadratic term to the objective (which the default linear
     * solver does not support) must raise {@link UnsupportedOperationException}.
     */
    @Test
    void testQuadraticObjectiveThrowsUnsupportedOperationException() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).weight(// linear part (will be overwritten by quadratic)
        ONE);
        // Create an expression that will become the objective.
        Expression obj = model.objective();
        // Add a quadratic term x*x with coefficient 1.
        obj.set(x, x, ONE);
        assertThrows(UnsupportedOperationException.class, () -> model.maximise(), "A quadratic objective should cause UnsupportedOperationException");
    }

    /**
     * Setting an illegal bound (NaN) on a variable must be detected during
     * validation and result in {@link IllegalArgumentException}.
     */
    @Test
    void testVariableWithNaNBoundsThrowsIllegalArgumentException() {
        Variable x = model.newVariable("x");
        // BigDecimal cannot represent NaN directly; using Double.NaN forces the
        // validation code to reject the bound.
        assertThrows(IllegalArgumentException.class, () -> x.lower(BigDecimal.valueOf(Double.NaN)), "Setting a NaN lower bound should throw IllegalArgumentException");
    }

    /**
     * Adding an expression term with a {@code null} coefficient (i.e. a null
     * variable reference) must lead to {@link NullPointerException} during
     * model preparation.
     */
    @Test
    void testExpressionWithNullCoefficientThrowsNullPointerException() {
        Expression expr = model.addExpression("badExpr");
        // The first argument is the variable; passing null should trigger NPE.
        assertThrows(NullPointerException.class, () -> expr.set(null, ONE), "Expression.set(null, ...) should throw NullPointerException");
    }
}