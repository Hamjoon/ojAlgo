package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.netio.BasicLogger;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests only use public API that is present in the supplied source code and the
 * minimal set of dependent classes listed in the prompt. No external mocking frameworks
 * are used – real objects are created through the constructors / factory methods.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    /** Helper to create a variable with a given name and optional bounds. */
    private Variable createVariable(String name, Number lower, Number upper, boolean integer) {
        Variable var = model.addVariable(name);
        if (lower != null) {
            var.lower(lower);
        }
        if (upper != null) {
            var.upper(upper);
        }
        if (integer) {
            var.integer();
        }
        return var;
    }

    /** Helper to create a simple linear expression:   coeff * variable   */
    private Expression createLinearExpression(String name, Variable var, Number coeff) {
        Expression expr = model.addExpression(name);
        expr.set(var, coeff);
        return expr;
    }

    @Test
    public void testAddVariableAndExpressionBasicProperties() {
        Variable x = createVariable("X", null, null, false);
        Variable y = createVariable("Y", 0, 10, true); // integer variable with bounds

        // Verify the variables are stored and indexed correctly
        assertEquals(0, model.indexOf(x));
        assertEquals(1, model.indexOf(y));

        // Free‑variable indices (both are free because not fixed)
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));

        // Integer‑variable index (only y is integer)
        assertEquals(-1, model.indexOfIntegerVariable(x));
        assertEquals(0, model.indexOfIntegerVariable(y));

        // Positive / negative indices
        assertEquals(0, model.indexOfPositiveVariable(x));
        assertEquals(1, model.indexOfPositiveVariable(y));
        assertEquals(-1, model.indexOfNegativeVariable(x));
        assertEquals(-1, model.indexOfNegativeVariable(y));

        // Check that the model recognises an integer variable
        assertTrue(model.isAnyVariableInteger());

        // Check that the model recognises a variable that is not fixed
        assertFalse(model.isFixed());

        // Description counts
        ExpressionsBasedModel.Description desc = model.describe();
        assertEquals(2, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(1, desc.nbPositiveVariables);
        assertEquals(0, desc.nbNegativeVariables);
        assertEquals(0, desc.nbLowerBounds);
        assertEquals(1, desc.nbUpperBounds);
        assertEquals(0, desc.nbEqualityBounds);
    }

    @Test
    public void testObjectiveConstructionAndEvaluation() {
        Variable a = createVariable("A", null, null, false);
        Variable b = createVariable("B", null, null, false);

        // Set contribution weights (objective coefficients)
        a.weight(BigDecimal.valueOf(2));
        b.weight(BigDecimal.valueOf(3));

        // Build the aggregated objective expression
        Expression objective = model.objective();

        // The objective should contain the two linear terms with the correct coefficients
        assertEquals(BigDecimal.valueOf(2), objective.get(a));
        assertEquals(BigDecimal.valueOf(3), objective.get(b));

        // Set explicit variable values and verify the evaluated objective value
        a.value(BigDecimal.valueOf(1));
        b.value(BigDecimal.valueOf(4));

        Optimisation.Result varValues = model.getVariableValues();
        double objectiveValue = objective.evaluate(varValues).doubleValue();

        // Expected: 2*1 + 3*4 = 14
        assertEquals(14.0, objectiveValue, 1e-12);
    }

    @Test
    public void testVariableValueRetrievalAndFeasibility() {
        Variable v = createVariable("V", -5, 5, false);
        v.value(BigDecimal.valueOf(2));

        Optimisation.Result result = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // The variable value should be present in the solution vector
        assertEquals(2.0, result.get(0), 1e-12);
    }

    @Test
    public void testValidateMethodology() {
        Variable v = createVariable("V", 0, 10, false);
        v.value(BigDecimal.valueOf(7));

        // Simple validation should succeed
        assertTrue(model.validate());

        // Introduce an infeasible value (outside bounds)
        v.value(BigDecimal.valueOf(15));
        assertFalse(model.validate());
    }

    @Test
    public void testInfeasibilityDetectionViaContradictoryBounds() {
        Variable v = createVariable("V", 5, 3, false); // lower > upper -> infeasible
        assertTrue(model.isInfeasible());
    }

    @Test
    public void testUnboundedDetection() {
        // Variable without any bounds is considered unbounded
        Variable v = createVariable("U", null, null, false);
        assertTrue(model.isUnbounded());
    }

    @Test
    public void testLimitObjectiveCreatesConstraintExpression() {
        Variable x = createVariable("X", null, null, false);
        x.weight(BigDecimal.ONE);

        // Apply limits to the objective (turns it into a constraint expression)
        Expression constr = model.limitObjective(BigDecimal.valueOf(-10), BigDecimal.valueOf(10));

        // The constraint expression should now be present and have the same linear term as the objective
        assertNotNull(constr);
        assertEquals(BigDecimal.ONE, constr.get(x));

        // Verify that the limits are set correctly
        assertEquals(BigDecimal.valueOf(-10), constr.getLowerLimit());
        assertEquals(BigDecimal.valueOf(10), constr.getUpperLimit());
    }

    @Test
    public void testCopyAndSnapshotBehaviour() {
        Variable a = createVariable("A", null, null, false);
        a.weight(BigDecimal.valueOf(5));

        // Normal deep copy
        ExpressionsBasedModel copy = model.copy();
        assertNotSame(model, copy);
        assertEquals(1, copy.countVariables());
        assertEquals(BigDecimal.valueOf(5), copy.getVariable(0).getContributionWeight());

        // Snapshot creates a shallow copy flagged as relaxed
        ExpressionsBasedModel snapshot = model.snapshot();
        assertTrue(snapshot.isRelaxed());
        // The snapshot should contain the same variable (same count) but be a different instance
        assertNotSame(model, snapshot);
        assertEquals(1, snapshot.countVariables());
    }

    @Test
    public void testRelaxRemovesIntegerProperty() {
        Variable i = createVariable("I", 0, 10, true); // integer variable
        assertTrue(i.isInteger());

        // Relax the whole model (hard relaxation)
        model.relax(false);
        assertFalse(i.isInteger());

        // Soft relaxation keeps the integer flag but disables integer solving
        Variable j = createVariable("J", 0, 10, true);
        model.relax(true);
        assertTrue(j.isInteger());
        assertTrue(model.isRelaxed());
    }

    @Test
    public void testSetKnownSolutionAndValidation() {
        Variable x = createVariable("X", null, null, false);
        x.weight(BigDecimal.ONE);
        x.value(BigDecimal.valueOf(3));

        // Build a simple optimal solution manually
        Optimisation.Result known = Optimisation.Result.of(Optimisation.State.OPTIMAL, 3.0, 3.0);
        model.setKnownSolution(known);

        // The validator should accept the known solution
        assertTrue(model.validate(known));
    }

    @Test
    public void testExpressionAndVariableRetrieval() {
        Variable v = createVariable("V", null, null, false);
        Expression e = createLinearExpression("E1", v, 4);
        model.addExpression("E2"); // empty expression

        // Retrieval by name
        assertSame(e, model.getExpression("E1"));
        assertNotNull(model.getExpression("E2"));

        // Collection view
        assertEquals(2, model.getExpressions().size());

        // Variables list is unmodifiable
        try {
            model.getVariables().add(v);
            fail("Variables list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testPresolveAndSimplifyDoNotThrow() {
        // Create a simple model with a linear constraint that can be simplified
        Variable x = createVariable("X", 0, 10, false);
        Expression expr = model.addExpression("C1");
        expr.set(x, BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(5));

        // Call simplify – it should produce a new model without throwing exceptions
        ExpressionsBasedModel simplified = model.simplify();
        assertNotNull(simplified);
        // The simplified model should still contain the variable and constraint
        assertEquals(1, simplified.countVariables());
        assertEquals(1, simplified.countExpressions());
    }

    @Test
    public void testToStringContainsVariablesAndExpressions() {
        Variable a = createVariable("A", null, null, false);
        a.lower(0).upper(10);
        Expression e = model.addExpression("E");
        e.set(a, BigDecimal.valueOf(2));
        e.upper(BigDecimal.valueOf(15));

        String representation = model.toString();
        // Basic sanity checks – the string should contain the variable name and the expression name
        assertTrue(representation.contains("A"));
        assertTrue(representation.contains("E"));
    }

    @Test
    public void testIndexOfMethodsWithFixedVariable() {
        Variable x = createVariable("X", 5, 5, false); // fixed variable (lower == upper)
        assertTrue(x.isFixed());

        // Fixed variables are not considered free
        assertEquals(-1, model.indexOfFreeVariable(x));

        // They are still part of the global index
        assertEquals(0, model.indexOf(x));
    }

    @Test
    public void testIsAnyVariableFixedAndIsAnyVariableInteger() {
        Variable v1 = createVariable("V1", 2, 2, false); // fixed
        Variable v2 = createVariable("V2", null, null, true); // integer, not fixed

        assertTrue(model.isAnyVariableFixed());
        assertTrue(model.isAnyVariableInteger());
    }

    @Test
    public void testIsAnyExpressionQuadraticAndConstraintQuadratic() {
        Variable x = createVariable("X", null, null, false);
        Variable y = createVariable("Y", null, null, false);

        // Linear expression – should not be quadratic
        Expression lin = model.addExpression("Lin");
        lin.set(x, BigDecimal.ONE);
        lin.set(y, BigDecimal.ONE);
        lin.lower(BigDecimal.ZERO);

        // Quadratic expression
        Expression quad = model.addExpression("Quad");
        quad.add(x, y, BigDecimal.valueOf(2)); // xy term
        quad.upper(BigDecimal.valueOf(10));

        assertFalse(model.isAnyExpressionQuadratic()); // only linear expression is objective/constraint
        // Mark the quadratic expression as objective to make it count
        quad.weight(BigDecimal.ONE);
        assertTrue(model.isAnyExpressionQuadratic());

        // Constraint quadratic detection
        assertTrue(model.isAnyConstraintQuadratic());
    }

    @Test
    public void testIntegrationSwitchHandling() {
        // By default no switches are set
        assertFalse(model.isIntegrationSwitch(ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE));

        // Set a switch via the protected API (use the integration class)
        ExpressionsBasedModel.Integration<?> dummy = new ExpressionsBasedModel.Integration<Optimisation.Solver>() {
            @Override
            public Optimisation.Solver build(ExpressionsBasedModel model) { return null; }
            @Override
            public boolean isCapable(ExpressionsBasedModel model) { return false; }
        };
        dummy.setSwitch(model, ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE, true);
        assertTrue(model.isIntegrationSwitch(ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE));

        // Verify the getter works through the integration as well
        assertTrue(dummy.isSwitch(model, ExpressionsBasedModel.IntegrationProperty.JAVA_OR_NATIVE_CODE));
    }

    @Test
    public void testAddReferenceAndGetReferences() {
        Variable v = createVariable("V", null, null, false);
        model.addReference(v.getIndex());
        assertTrue(model.getReferences().contains(v.getIndex()));
    }
}
