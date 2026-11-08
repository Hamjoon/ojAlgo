package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

/**
 * Unit tests for {@link ExpressionsBasedModel}. The tests focus on the public API that can be exercised
 * without requiring any of the heavy solver integrations. They verify that model construction,
 * description, index mapping, feasibility checks and simple validation work as expected.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // Use default options – they are sufficient for the lightweight tests we perform.
        model = new ExpressionsBasedModel(new Optimisation.Options());
    }

    @Test
    public void testAddVariableAndExpressionRetrieval() {
        Variable x = model.addVariable("x");
        assertNotNull("Variable should be created", x);

        Expression expr = model.addExpression("expr");
        expr.add(x, BigDecimal.ONE);
        assertSame("Expression should be retrievable by name", expr, model.getExpression("expr"));
    }

    @Test
    public void testDescribeCounts() {
        // Variable 0 – lower and upper bounds
        Variable v0 = model.addVariable("v0");
        v0.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);

        // Variable 1 – integer binary variable (no explicit bounds)
        Variable v1 = model.addVariable("v1").integer().binary();

        // Constraint expression using v0
        Expression c0 = model.addExpression("c0");
        c0.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        c0.add(v0, BigDecimal.ONE);

        ExpressionsBasedModel.Description desc = model.describe();

        // Variables
        assertEquals("Total variables", 2, desc.nbVariables);
        assertEquals("Lower bounds on variables", 1, desc.nbLowerBounds);
        assertEquals("Upper bounds on variables", 1, desc.nbUpperBounds);
        assertEquals("Equality bounds on variables", 0, desc.nbEqualityBounds);
        assertEquals("Integer variables", 1, desc.nbIntegerVariables);
        assertEquals("Positive variables", 2, desc.nbPositiveVariables);
        assertEquals("Negative variables", 0, desc.nbNegativeVariables);

        // Expressions (constraints)
        assertEquals("Lower constraints", 1, desc.nbLowerConstraints);
        assertEquals("Upper constraints", 1, desc.nbUpperConstraints);
        assertEquals("Equality constraints", 0, desc.nbEqualityConstraints);
    }

    @Test
    public void testIndexMappingAndFreeVariableDetection() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        Variable c = model.addVariable("c");

        // Fix variable a by setting equal lower & upper limits
        a.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);

        // b is positive (upper limit > 0)
        b.upper(BigDecimal.ONE);
        // c is negative (lower limit < 0)
        c.lower(BigDecimal.valueOf(-5));

        // Global indices are the insertion order
        int idxA = model.indexOf(a);
        int idxB = model.indexOf(b);
        int idxC = model.indexOf(c);

        // a is fixed → not a free variable
        assertEquals("Fixed variable should have free index -1", -1, model.indexOfFreeVariable(idxA));
        // b and c are free
        assertTrue("b should be a free variable", model.indexOfFreeVariable(idxB) >= 0);
        assertTrue("c should be a free variable", model.indexOfFreeVariable(idxC) >= 0);

        // Integer variable detection (none are integer yet)
        assertEquals("No integer variables", -1, model.indexOfIntegerVariable(idxB));
        // Positive/negative classification
        assertTrue("b should be positive", model.indexOfPositiveVariable(idxB) >= 0);
        assertEquals("b should not be negative", -1, model.indexOfNegativeVariable(idxB));
        assertTrue("c should be negative", model.indexOfNegativeVariable(idxC) >= 0);
    }

    @Test
    public void testAnyVariableIntegerAndFixedDetection() {
        Variable intVar = model.addVariable("intVar").integer();
        Variable fixVar = model.addVariable("fixVar");
        fixVar.lower(BigDecimal.ONE).upper(BigDecimal.ONE); // fixed at 1

        assertTrue("Model should report an integer variable", model.isAnyVariableInteger());
        assertTrue("Model should report a fixed variable", model.isAnyVariableFixed());
    }

    @Test
    public void testUnboundedDetection() {
        // Variable without any bounds is unbounded
        model.addVariable("unboundedVar");
        assertTrue("Model should be unbounded", model.isUnbounded());
    }

    @Test
    public void testInfeasibilityDetection() {
        Variable x = model.addVariable("x");
        // Contradictory bounds: lower > upper
        x.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3));
        assertTrue("Model should be infeasible due to contradictory bounds", model.isInfeasible());
    }

    @Test
    public void testLimitObjectiveCreatesConstraintExpression() {
        // Create a simple variable and set a weight (objective contribution) via its value.
        Variable v = model.addVariable("v");
        v.value(BigDecimal.ZERO); // value set, not a weight – the test only needs the method to run

        // Call limitObjective – it should create (or reuse) an internal constraint expression
        Expression limitExpr = model.limitObjective(BigDecimal.ONE, BigDecimal.TEN);
        assertNotNull("limitObjective should return a non‑null expression", limitExpr);
        assertEquals("Lower limit should be set", 0, limitExpr.getLowerLimit().compareTo(BigDecimal.ONE));
        assertEquals("Upper limit should be set", 0, limitExpr.getUpperLimit().compareTo(BigDecimal.TEN));
    }

    @Test
    public void testGetVariableValuesAndValidate() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        x.value(BigDecimal.valueOf(5));

        Optimisation.Result result = model.getVariableValues();
        assertEquals("Result state should be FEASIBLE", Optimisation.State.FEASIBLE, result.getState());
        assertEquals("Variable value should be 5", 0, result.get(0).compareTo(BigDecimal.valueOf(5)));

        // Validation should succeed with the default feasibility context
        assertTrue("Model validation should succeed", model.validate());
    }

    @Test
    public void testSimplifyDoesNotThrowAndPreservesExpressions() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Expression expr = model.addExpression("expr");
        expr.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        expr.add(x, BigDecimal.ONE);

        // simplify should run without throwing and produce a copy with the same expression count
        ExpressionsBasedModel simplified = model.simplify();
        assertNotNull("Simplified model should not be null", simplified);
        assertEquals("Expression count should be preserved", model.countExpressions(), simplified.countExpressions());
    }

    @Test
    public void testToStringContainsModelElements() {
        Variable v = model.addVariable("v");
        v.lower(BigDecimal.ZERO);
        Expression e = model.addExpression("e");
        e.upper(BigDecimal.TEN);
        e.add(v, BigDecimal.ONE);

        String representation = model.toString();
        assertTrue("String representation should contain variable name", representation.contains("v"));
        assertTrue("String representation should contain expression name", representation.contains("e"));
    }

    @Test
    public void testConstraintAndBoundStreams() {
        Variable a = model.addVariable("a").lower(BigDecimal.ZERO);
        Variable b = model.addVariable("b").upper(BigDecimal.ONE);
        Variable c = model.addVariable("c"); // no bounds

        // Expression used as constraint
        Expression constr = model.addExpression("constr");
        constr.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(2));
        constr.add(a, BigDecimal.ONE);
        constr.add(b, BigDecimal.ONE);

        // Verify bounds stream returns only variables that are constraints (i.e., have a bound) and are not fixed
        Set<Variable> boundVars = model.bounds().collect(java.util.stream.Collectors.toSet());
        assertTrue("Bounds should contain a", boundVars.contains(a));
        assertTrue("Bounds should contain b", boundVars.contains(b));
        assertFalse("Bounds should not contain c (no bound)", boundVars.contains(c));

        // Verify constraints stream returns only constraint expressions that are not redundant
        Set<Expression> constraints = model.constraints().collect(java.util.stream.Collectors.toSet());
        assertTrue("Constraints should contain the created constraint", constraints.contains(constr));
    }

    @Test
    public void testIntegrationSwitchDefaults() {
        // By default no integration switches are set
        for (ExpressionsBasedModel.IntegrationProperty prop : ExpressionsBasedModel.IntegrationProperty.values()) {
            assertFalse("Integration switch should be false by default for " + prop, model.isIntegrationSwitch(prop));
        }
    }

    @Test
    public void testRelaxedFlagBehaviour() {
        // Initially not relaxed
        assertFalse("Model should not be relaxed initially", model.isRelaxed());

        // Relax the model (soft = false) – all integer flags are cleared
        model.relax(false);
        assertFalse("After hard relax, model should still report not relaxed", model.isRelaxed());

        // Soft relax – only the flag is set
        model.relax(true);
        assertTrue("After soft relax, model should be marked as relaxed", model.isRelaxed());
    }

    @Test
    public void testReferenceTracking() {
        Variable v = model.addVariable("v");
        // No references yet
        assertFalse("Variable should not be referenced initially", model.isReferenced(v));

        // Create an expression that uses the variable – this adds a reference internally
        Expression expr = model.addExpression("expr");
        expr.add(v, BigDecimal.ONE);
        // After adding, the variable should be referenced
        assertTrue("Variable should be referenced after being used in an expression", model.isReferenced(v));
    }

    @Test
    public void testFixedAllVariablesDetection() {
        Variable v1 = model.addVariable("v1").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        Variable v2 = model.addVariable("v2").lower(BigDecimal.TEN).upper(BigDecimal.TEN);
        assertTrue("All variables are fixed", model.isFixed());
    }

    @Test
    public void testGetKnownSolutionAndValidationHandlerDefaults() {
        // No known solution set – getters should return null
        assertNull("Known solution should be null initially", model.getKnownSolution());
        assertNotNull("Validation failure handler should never be null", model.getValidationFailureHandler());
    }
}
