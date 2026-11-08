package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

/**
 * Unit tests for {@link ExpressionsBasedModel}.
 */
public class ExpressionsBasedModelTest {

    /**
     * Basic test of variable creation, indexing and free‑variable categorisation.
     */
    @Test
    public void testVariableCreationAndIndexing() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y").integer(); // make y integer

        assertEquals(2, model.countVariables());
        assertSame(x, model.getVariable(0));
        assertSame(y, model.getVariable(1));

        // free‑variable indices (both are free because no value is fixed yet)
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));

        // integer variable index
        assertEquals(1, model.indexOfIntegerVariable(y));
        assertEquals(-1, model.indexOfIntegerVariable(x));

        // free‑variable list size
        assertEquals(2, model.getFreeVariables().size());
        assertTrue(model.getFreeVariables().contains(x));
        assertTrue(model.getFreeVariables().contains(y));
    }

    /**
     * Test that the description correctly counts variable/constraint categories.
     */
    @Test
    public void testDescribeCounts() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // variable with lower bound only
        model.addVariable("a").lower(0);
        // variable with upper bound only
        model.addVariable("b").upper(10);
        // variable with both bounds (equality)
        model.addVariable("c").lower(5).upper(5);
        // integer variable
        model.addVariable("d").integer();

        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(4, desc.nbVariables);
        assertEquals(2, desc.nbLowerBounds);   // a and c
        assertEquals(2, desc.nbUpperBounds);   // b and c
        assertEquals(1, desc.nbEqualityBounds); // c
        assertEquals(1, desc.nbIntegerVariables); // d
    }

    /**
     * Verify that {@code isAnyVariableInteger()} reacts to integer variables.
     */
    @Test
    public void testAnyVariableInteger() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v = model.addVariable("v");
        assertFalse(model.isAnyVariableInteger());

        v.integer(); // mark as integer
        assertTrue(model.isAnyVariableInteger());
    }

    /**
     * Verify quadratic detection on expressions.
     */
    @Test
    public void testQuadraticExpressionDetection() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // Linear expression – should not be quadratic
        Expression lin = model.addExpression("lin");
        lin.add(x, BigMath.ONE);
        assertFalse(model.isAnyExpressionQuadratic());

        // Add a quadratic term x*y
        Expression quad = model.addExpression("quad");
        quad.add(x, y, BigMath.ONE);
        assertTrue(model.isAnyExpressionQuadratic());
    }

    /**
     * Test {@code limitObjective} creates a constraint expression and sets the supplied limits.
     */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // simple objective: variable with weight 1
        Variable v = model.addVariable("v").weight(BigMath.ONE);
        v.setValue(BigDecimal.valueOf(3));

        // limit the objective between 2 and 4
        Expression limit = model.limitObjective(BigDecimal.valueOf(2), BigDecimal.valueOf(4));

        assertNotNull(limit);
        assertEquals(BigDecimal.valueOf(2), limit.getLowerLimit());
        assertEquals(BigDecimal.valueOf(4), limit.getUpperLimit());
    }

    /**
     * Test {@code getVariableValues()} returns the current variable values (or defaults) and that the
     * objective value matches the expected contribution.
     */
    @Test
    public void testGetVariableValuesAndObjective() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").weight(BigMath.ONE);
        Variable y = model.addVariable("y").weight(BigMath.ONE).lower(0).upper(10);

        x.setValue(BigDecimal.valueOf(2));
        y.setValue(BigDecimal.valueOf(5));

        Optimisation.Result values = model.getVariableValues();

        assertEquals(2, values.size());
        assertEquals(2.0, values.get(0).doubleValue(), 1e-12);
        assertEquals(5.0, values.get(1).doubleValue(), 1e-12);

        // Objective = x + y = 7
        Optimisation.Result obj = model.objective().evaluate(values);
        assertEquals(BigDecimal.valueOf(7), obj);
    }

    /**
     * Validate that a feasible solution passes {@code validate()} and an infeasible one fails.
     */
    @Test
    public void testValidateFeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.addVariable("a").lower(0).upper(5);
        Variable b = model.addVariable("b").lower(1).upper(3);

        // feasible values
        a.setValue(BigDecimal.valueOf(3));
        b.setValue(BigDecimal.valueOf(2));
        assertTrue(model.validate());

        // make b infeasible
        b.setValue(BigDecimal.valueOf(10));
        assertFalse(model.validate());
    }

    /**
     * Test copy and shallow‑copy behaviour together with relaxation.
     */
    @Test
    public void testCopyAndRelaxation() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();

        Variable v = original.addVariable("v").integer().lower(0).upper(1);
        v.setValue(BigDecimal.ONE);

        // Deep copy – should retain integer flag
        ExpressionsBasedModel deepCopy = original.copy();
        assertTrue(deepCopy.isAnyVariableInteger());

        // Relaxed shallow copy – integer flag cleared
        ExpressionsBasedModel shallowRelaxed = original.copy(true, false);
        shallowRelaxed.relax();
        assertFalse(shallowRelaxed.isAnyVariableInteger());
    }

    /**
     * Verify the integration switch handling works (set and query).
     */
    @Test
    public void testIntegrationSwitches() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // initially false
        assertFalse(model.isIntegrationSwitch(ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE));

        // set switch and verify
        model.setIntegrationSwitch(ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE, true);
        assertTrue(model.isIntegrationSwitch(ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE));
    }

    /**
     * Test {@code isInfeasible()} detects an infeasible variable bound.
     */
    @Test
    public void testInfeasibilityDetection() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // contradictory bounds -> infeasible
        model.addVariable("z").lower(10).upper(5);
        assertTrue(model.isInfeasible());
    }

    /**
     * Ensure that {@code toString()} produces a non‑empty representation.
     */
    @Test
    public void testToStringNonEmpty() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        model.addVariable("x").lower(0).upper(10);
        model.addExpression("c1").add(model.getVariable(0), BigMath.ONE).upper(BigDecimal.valueOf(5));

        String txt = model.toString();
        assertNotNull(txt);
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("c1"));
    }
}
