package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_7_Test {

    /**
     * Simple linear model where the optimal solution is known.
     * The test checks that {@code validate(NumberContext, BasicLogger)} returns {@code true}
     * for the solution produced by the model.
     */
    @Test
    void testValidateReturnsTrueForOptimalSolution() {
        // -----------------------------------------------------------------
        // GIVEN – a tiny optimisation problem: minimise  x  subject to 0 ≤ x ≤ 10
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a single variable bounded between 0 and 10, weight 1 (i.e. part of the objective)
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // No explicit constraints are needed – the bounds on the variable are enough.
        // -----------------------------------------------------------------
        // WHEN – the model is solved and the result is validated
        // -----------------------------------------------------------------
        Optimisation.Result result = model.minimise();
        // a NumberContext with a reasonable precision/scale for the test
        // 8‑digit precision, 8‑digit scale
        NumberContext context = NumberContext.of(8, 8);
        // BasicLogger is a lightweight logger; an empty instance is sufficient for the test
        BasicLogger logger = new BasicLogger();
        boolean isValid = model.validate(context, logger);
        // -----------------------------------------------------------------
        // THEN – the validation must succeed and the solution must be optimal
        // -----------------------------------------------------------------
        assertTrue(isValid, "The model should validate the optimal solution");
        assertTrue(result.getState().isOptimal(), "The optimisation result should be optimal");
        // The optimal solution for this model is x = 0 (minimum of the objective)
        BigDecimal xValue = result.get(model.indexOf(x));
        assertEquals(BigDecimal.ZERO, xValue, "Variable x should be 0 in the optimal solution");
    }

    /**
     * The same model as above, but this time we validate with a stricter
     * {@link NumberContext}. The method must still return {@code true}
     * because the solution satisfies the tighter tolerance.
     */
    @Test
    void testValidateWithStricterNumberContext() {
        // -----------------------------------------------------------------
        // GIVEN – a model identical to the previous test
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Optimisation.Result result = model.minimise();
        // A stricter context (higher precision, same scale)
        NumberContext strictContext = NumberContext.of(12, 12);
        BasicLogger logger = new BasicLogger();
        // -----------------------------------------------------------------
        // WHEN – validation is performed
        // -----------------------------------------------------------------
        boolean isValid = model.validate(strictContext, logger);
        // -----------------------------------------------------------------
        // THEN – validation must still succeed
        // -----------------------------------------------------------------
        assertTrue(isValid, "Validation should succeed even with a stricter NumberContext");
        assertTrue(result.getState().isOptimal(), "Result should still be optimal");
    }

    /* --------------------------------------------------------------------- *
     *  1. Normal validation – feasible solution
     * --------------------------------------------------------------------- */
    @Test
    void testValidateFeasibleSolution() {
        // GIVEN: a model with a single variable x, minimise x, constraint x >= 5
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// constraint
        BigDecimal.valueOf(5)).weight(// part of the objective (minimise)
        BigDecimal.ONE);
        // WHEN: validation is performed with a normal NumberContext
        NumberContext context = NumberContext.of(8, 8);
        BasicLogger logger = new BasicLogger();
        boolean valid = model.validate(context, logger);
        // THEN: validation must succeed
        assertTrue(valid, "Validation should return true for a feasible solution");
    }

    /* --------------------------------------------------------------------- *
     *  2. Null NumberContext – expect NPE
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullNumberContext() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").weight(BigDecimal.ONE);
        BasicLogger logger = new BasicLogger();
        assertThrows(NullPointerException.class, () -> model.validate(null, logger), "Calling validate with a null NumberContext should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null BasicLogger – expect NPE
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullBasicLogger() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").weight(BigDecimal.ONE);
        NumberContext context = NumberContext.of(8, 8);
        assertThrows(NullPointerException.class, () -> model.validate(context, null), "Calling validate with a null BasicLogger should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  4. Infeasible model – validation returns false
     * --------------------------------------------------------------------- */
    @Test
    void testValidateInfeasibleModel() {
        // GIVEN: two variables with contradictory bounds (x >= 2 and x <= 1)
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// lower bound 2
        BigDecimal.valueOf(2)).upper(// upper bound 1 → infeasible
        BigDecimal.valueOf(1)).weight(BigDecimal.ONE);
        Variable y = model.newVariable("y").weight(// does not affect feasibility
        BigDecimal.ONE);
        // WHEN: validation is performed
        NumberContext context = NumberContext.of(8, 8);
        BasicLogger logger = new BasicLogger();
        boolean valid = model.validate(context, logger);
        // THEN: validation must detect infeasibility and return false
        assertFalse(valid, "Validation should return false for an infeasible model");
    }

    /* --------------------------------------------------------------------- *
     *  5. Rounding precision – validation fails due to tight context
     * --------------------------------------------------------------------- */
    @Test
    void testValidateFailsDueToTightNumberContext() {
        // GIVEN: a variable with a very small lower bound
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// constraint that requires at least 0.005
        new BigDecimal("0.005")).weight(// minimise x
        BigDecimal.ONE);
        // WHEN: we validate with a NumberContext that rounds to two decimal places
        // 2‑digit precision & scale
        NumberContext tightContext = NumberContext.of(2, 2);
        BasicLogger logger = new BasicLogger();
        boolean valid = model.validate(tightContext, logger);
        // THEN: the rounded solution (0.01) violates the original constraint after rounding,
        //       therefore validation must return false
        assertFalse(valid, "Validation should fail when rounding destroys feasibility");
    }

    /* --------------------------------------------------------------------- *
     *  6. Integer variable violation – validation returns false
     * --------------------------------------------------------------------- */
    @Test
    void testValidateIntegerVariableViolation() {
        // GIVEN: an integer variable with a non‑integer lower bound
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable z = model.newVariable("z").integer(// mark as integer
        true).lower(// constraint that forces a non‑integer value
        new BigDecimal("1.5")).weight(// minimise z
        BigDecimal.ONE);
        // WHEN: validation is performed with a normal context
        NumberContext context = NumberContext.of(8, 8);
        BasicLogger logger = new BasicLogger();
        boolean valid = model.validate(context, logger);
        // THEN: because the variable is integer but the solution is non‑integer,
        //       validation must return false
        assertFalse(valid, "Validation should fail when an integer variable takes a non‑integer value");
    }

    /* --------------------------------------------------------------------- *
     *  7. Model with no constraints – validation succeeds
     * --------------------------------------------------------------------- */
    @Test
    void testValidateModelWithoutConstraints() {
        // GIVEN: two free variables, objective minimise a + b, no constraints added
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("a").weight(// part of objective
        BigDecimal.ONE);
        model.newVariable("b").weight(// part of objective
        BigDecimal.ONE);
        // WHEN: validation is performed
        NumberContext context = NumberContext.of(8, 8);
        BasicLogger logger = new BasicLogger();
        boolean valid = model.validate(context, logger);
        // THEN: with no constraints the solution is trivially feasible
        assertTrue(valid, "Validation should succeed for a model without constraints");
    }

    /* --------------------------------------------------------------------- *
     *  8. Empty model (no variables, no expressions) – validation succeeds
     * --------------------------------------------------------------------- */
    @Test
    void testValidateEmptyModel() {
        // GIVEN: a freshly created model with nothing added
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // WHEN: validation is performed
        NumberContext context = NumberContext.of(8, 8);
        BasicLogger logger = new BasicLogger();
        boolean valid = model.validate(context, logger);
        // THEN: an empty model is considered valid
        assertTrue(valid, "Validation should succeed for an empty model");
    }
}
