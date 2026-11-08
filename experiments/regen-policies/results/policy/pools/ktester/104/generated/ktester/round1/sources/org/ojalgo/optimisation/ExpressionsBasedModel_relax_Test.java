package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_relax_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // help the garbage collector – not strictly necessary but keeps the pattern consistent
        model = null;
    }

    /**
     * Verify that the {@code relax()} method toggles the public {@code isRelaxed()} flag.
     */
    @Test
    void testRelaxSetsRelaxedFlag() {
        Assertions.assertFalse(model.isRelaxed(), "Model should start in a non‑relaxed state");
        // invokes relax(false) internally
        model.relax();
        Assertions.assertTrue(model.isRelaxed(), "Model should be marked as relaxed after calling relax()");
    }

    /**
     * When a model contains only continuous variables, relaxing it must not alter the optimisation
     * result. The test builds a tiny linear programme, solves it, relaxes the model and solves it again,
     * asserting that both results are identical.
     */
    @Test
    void testRelaxDoesNotChangeResultForContinuousModel() {
        // Build a simple model: minimise x subject to 0 ≤ x ≤ 10 and x = 7
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// weight = 1 → minimise x
        BigDecimal.ONE);
        model.newExpression("eq").set(x, BigDecimal.ONE).level(// equality constraint x = 7
        BigDecimal.valueOf(7));
        // Solve before relaxation
        Optimisation.Result before = model.minimise();
        Assertions.assertTrue(model.validate(before), "Result before relaxation must be valid");
        Assertions.assertEquals(Optimisation.State.OPTIMAL, before.getState(), "Model should be optimal before relaxation");
        BigDecimal valueBefore = before.getValue();
        // Relax the model – it is already continuous, so nothing should change
        model.relax();
        // Solve after relaxation
        Optimisation.Result after = model.minimise();
        Assertions.assertTrue(model.validate(after), "Result after relaxation must be valid");
        Assertions.assertEquals(Optimisation.State.OPTIMAL, after.getState(), "Model should be optimal after relaxation");
        BigDecimal valueAfter = after.getValue();
        // The optimisation values must be identical
        Assertions.assertEquals(0, valueBefore.compareTo(valueAfter), () -> "Optimisation value should not change after relaxation (before=" + valueBefore + ", after=" + valueAfter + ")");
    }

    /**
     * relax() on a model that already contains integer variables – they must become continuous.
     */
    @Test
    void testRelaxConvertsIntegerVariablesToContinuous() {
        // Create an integer variable
        Variable intVar = model.newVariable("i").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(true).weight(BigDecimal.ONE);
        // Simple constraint to make the model feasible
        model.newExpression("c").set(intVar, BigDecimal.ONE).level(BigDecimal.valueOf(3));
        Assertions.assertTrue(model.isAnyVariableInteger(), "Pre‑condition: model must contain at least one integer variable");
        // should convert integer variables to continuous
        model.relax();
        Assertions.assertTrue(model.isRelaxed(), "Model must be marked as relaxed after calling relax()");
        Assertions.assertFalse(model.isAnyVariableInteger(), "All integer variables must be converted to continuous after relaxation");
    }

    /**
     * relax() on a model without integer variables – flag is set but no conversion occurs.
     */
    @Test
    void testRelaxOnModelWithoutIntegerVariables() {
        // Continuous variable only
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        Assertions.assertFalse(model.isAnyVariableInteger(), "Pre‑condition: model must not contain integer variables");
        model.relax();
        Assertions.assertTrue(model.isRelaxed(), "Model must be marked as relaxed even if there are no integer variables");
        Assertions.assertFalse(model.isAnyVariableInteger(), "Model should still report no integer variables after relaxation");
    }

    /**
     * Calling relax() after the model has been disposed must throw IllegalStateException.
     */
    @Test
    void testRelaxOnDisposedModelThrows() {
        model.dispose();
        Assertions.assertThrows(IllegalStateException.class, model::relax, "Calling relax() on a disposed model should throw IllegalStateException");
    }

    /**
     * relax() on a model that contains a quadratic expression must throw IllegalArgumentException.
     */
    @Test
    void testRelaxOnQuadraticModelThrows() {
        // Create two variables
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Build a quadratic expression: x*y = 5
        Expression quad = model.newExpression("quad").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE);
        // The API for adding a quadratic term is `quadratic(Variable, Variable, Number)`.
        quad.quadratic(x, y, BigDecimal.ONE);
        quad.level(BigDecimal.valueOf(5));
        Assertions.assertTrue(model.isAnyExpressionQuadratic(), "Pre‑condition: model must contain a quadratic expression");
        Assertions.assertThrows(IllegalArgumentException.class, model::relax, "Relaxing a model with quadratic expressions must throw IllegalArgumentException");
    }

    /**
     * relax() when the integration switch is enabled must raise a ProgrammingError.
     */
    @Test
    void testRelaxWithIntegrationSwitchEnabledThrows() {
        model.setIntegrationSwitch(true);
        Assertions.assertTrue(model.isIntegrationSwitch(), "Pre‑condition: integration switch must be on");
        Assertions.assertThrows(IllegalStateException.class, model::relax, "Relaxing a model with integration switch enabled should raise IllegalStateException");
    }

    /**
     * relax() on a model that has been marked infeasible must throw IllegalStateException.
     */
    @Test
    void testRelaxOnInfeasibleModelThrows() {
        model.setInfeasible();
        Assertions.assertTrue(model.isInfeasible(), "Pre‑condition: model must be infeasible");
        Assertions.assertThrows(IllegalStateException.class, model::relax, "Relaxing an infeasible model should throw IllegalStateException");
    }

    /**
     * relax(true) – soft relaxation – is not supported for integer conversion and must throw.
     */
    @Test
    void testSoftRelaxThrowsUnsupportedOperationException() {
        // Add an integer variable to make the scenario realistic
        model.newVariable("i").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(true);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> model.relax(true), "Calling relax(true) should throw UnsupportedOperationException");
    }
}