package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import org.junit.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel.Description;
import org.ojalgo.optimisation.ExpressionsBasedModel.FileFormat;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;

/**
 * Unit tests for {@link ExpressionsBasedModel}.
 *
 * The tests focus on the model construction, description, indexing,
 * objective aggregation and validation logic that can be exercised without
 * invoking any external solvers.
 */
public class ExpressionsBasedModelTest {

    /**
     * Helper to create a model with a few variables and a simple expression.
     */
    private ExpressionsBasedModel createSimpleModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // three variables: x (free), y (fixed), z (integer)
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        Variable z = model.newVariable("z").integer();

        // fix y at 5
        y.lower(5).upper(5);

        // simple expression: 2*x + 3*z <= 10
        Expression expr = model.newExpression("c1");
        expr.set(x, BigDecimal.valueOf(2));
        expr.set(z, BigDecimal.valueOf(3));
        expr.upper(BigDecimal.valueOf(10));

        // objective: minimize 4*x + 5*z
        x.weight(BigDecimal.valueOf(4));
        z.weight(BigDecimal.valueOf(5));

        return model;
    }

    @Test
    public void testVariableCreationAndRetrieval() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c").integer();

        assertEquals(3, model.countVariables());
        assertSame(a, model.getVariable(0));
        assertSame(b, model.getVariable(1));
        assertSame(c, model.getVariable(2));

        // indexOf should match the variable's internal index
        assertEquals(0, model.indexOf(a));
        assertEquals(1, model.indexOf(b));
        assertEquals(2, model.indexOf(c));
    }

    @Test
    public void testExpressionCreationAndLookup() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x");
        Expression expr = model.addExpression("myExpr");
        expr.set(x, BigDecimal.ONE);
        expr.upper(BigDecimal.TEN);

        assertEquals(1, model.countExpressions());
        assertSame(expr, model.getExpression("myExpr"));
        assertTrue(expr.isConstraint());
        assertFalse(expr.isObjective());
    }

    @Test
    public void testDescribeCounts() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v1 = model.newVariable("v1"); // free, positive
        Variable v2 = model.newVariable("v2").integer(); // integer, free
        Variable v3 = model.newVariable("v3");
        v3.lower(BigDecimal.valueOf(-5)); // negative bound

        // expression with lower and upper bound
        Expression e1 = model.newExpression("e1");
        e1.set(v1, BigDecimal.ONE);
        e1.lower(BigDecimal.ZERO);
        e1.upper(BigDecimal.valueOf(10));

        // expression as objective
        Expression e2 = model.newExpression("e2");
        e2.set(v2, BigDecimal.ONE);
        v2.weight(BigDecimal.ONE); // make v2 contribute to objective

        Description desc = model.describe();

        assertEquals(3, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(1, desc.nbNegativeVariables); // v3 has lower bound < 0
        assertEquals(2, desc.nbPositiveVariables); // v1 and v2 (no negative bound)
        assertEquals(1, desc.nbLowerConstraints);
        assertEquals(1, desc.nbUpperConstraints);
        assertEquals(0, desc.nbEqualityConstraints);
        assertEquals(0, desc.nbEqualityBounds);
    }

    @Test
    public void testIndexOfFreeAndIntegerVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.newVariable("a"); // free
        Variable b = model.newVariable("b").integer(); // integer
        Variable c = model.newVariable("c");
        c.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO); // fixed

        // Force categorisation update
        model.getFreeVariables();

        // Free indices
        assertEquals(0, model.indexOfFreeVariable(a));
        assertEquals(1, model.indexOfFreeVariable(b));
        assertEquals(-1, model.indexOfFreeVariable(c)); // fixed -> not free

        // Integer indices
        assertEquals(-1, model.indexOfIntegerVariable(a));
        assertEquals(0, model.indexOfIntegerVariable(b));
        assertEquals(-1, model.indexOfIntegerVariable(c));
    }

    @Test
    public void testIsAnyVariableFixedAndInteger() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Variable v3 = model.newVariable("v3").integer();

        // No variable fixed yet
        assertFalse(model.isAnyVariableFixed());
        assertTrue(model.isAnyVariableInteger());

        // Fix v2
        v2.lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed());

        // Make all integer variables fixed
        v3.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        assertFalse(model.isAnyVariableInteger()); // because relaxed flag is false but all integer vars are fixed
    }

    @Test
    public void testQuadraticDetection() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");

        // Linear expression (no quadratic)
        Expression lin = model.newExpression("lin");
        lin.set(x, BigDecimal.ONE);
        lin.upper(BigDecimal.TEN);
        assertFalse(model.isAnyExpressionQuadratic());
        assertFalse(model.isAnyObjectiveQuadratic());
        assertFalse(model.isAnyConstraintQuadratic());

        // Quadratic expression
        Expression quad = model.newExpression("quad");
        quad.add(x, y, BigDecimal.valueOf(2)); // x*y term
        quad.upper(BigDecimal.valueOf(5));
        assertTrue(model.isAnyExpressionQuadratic());
        assertFalse(model.isAnyObjectiveQuadratic()); // not marked as objective
        assertTrue(model.isAnyConstraintQuadratic());

        // Mark quadratic expression as objective
        quad.objective();
        assertTrue(model.isAnyObjectiveQuadratic());
    }

    @Test
    public void testObjectiveAggregation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.newVariable("a").weight(BigDecimal.valueOf(3));
        Variable b = model.newVariable("b"); // not part of objective

        // Expression that contributes to objective with weight 2
        Expression expr = model.newExpression("expr");
        expr.set(a, BigDecimal.ONE);
        expr.set(b, BigDecimal.ONE);
        expr.weight(BigDecimal.valueOf(2));
        expr.objective();

        Expression objective = model.objective();

        // Expected: 3*a (from variable) + 2*(a + b) = (3+2)*a + 2*b
        assertEquals(BigDecimal.valueOf(5), objective.get(a));
        assertEquals(BigDecimal.valueOf(2), objective.get(b));

        // Constant part should be zero initially
        assertEquals(BigDecimal.ZERO, objective.getConstant());
    }

    @Test
    public void testLimitObjectiveCreatesConstraint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").weight(BigDecimal.ONE);
        model.objective(); // create objective

        // Apply limits to objective
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limit);
        assertTrue(limit.isConstraint());
        assertEquals(BigDecimal.ZERO, limit.lower());
        assertEquals(BigDecimal.TEN, limit.upper());

        // The original objective should remain unchanged
        Expression obj = model.objective();
        assertEquals(BigDecimal.ONE, obj.get(x));
    }

    @Test
    public void testGetVariableValuesAndValidate() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable y = model.newVariable("y").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        Variable z = model.newVariable("z"); // no bounds

        // Assign explicit values
        x.setValue(3);
        // y left unset, should fall back to midpoint of bounds (3)
        // z left unset, should default to 0

        Optimisation.Result result = model.getVariableValues();

        // Expect feasible solution
        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // Verify values
        assertEquals(BigDecimal.valueOf(3), result.get(0));
        assertEquals(BigDecimal.valueOf(3), result.get(1)); // (1+5)/2 = 3
        assertEquals(BigDecimal.ZERO, result.get(2));

        // Validate the solution against the model
        assertTrue(model.validate(result));
    }

    @Test
    public void testFileFormatDetection() {
        // Simulate file paths
        File mpsFile = new File("model.mps");
        File sifFile = new File("model.sif");
        File ebmFile = new File("model.ebm");
        File unknownFile = new File("model.txt");

        assertEquals(FileFormat.MPS, FileFormat.from(mpsFile));
        assertEquals(FileFormat.MPS, FileFormat.from(sifFile));
        assertEquals(FileFormat.EBM, FileFormat.from(ebmFile));

        try {
            FileFormat.from(unknownFile);
            fail("Expected IllegalArgumentException for unknown extension");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testIntegrationSwitchAndRelaxedFlag() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // By default both flags are false
        assertFalse(model.isIntegrationSwitch());
        assertFalse(model.isRelaxed());

        // Toggle via protected methods via the Integration class
        ExpressionsBasedModel.Integration<?> dummyIntegration = new ExpressionsBasedModel.Integration<Object>() {
            @Override
            public boolean isCapable(ExpressionsBasedModel model) { return false; }
        };
        dummyIntegration.setSwitch(model, true);
        assertTrue(model.isIntegrationSwitch());

        model.relax(true);
        assertTrue(model.isRelaxed());
    }

    @Test
    public void testValidateWithCustomSolution() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(4));
        Variable b = model.newVariable("b").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(3));

        // Simple constraint a + b <= 5
        Expression cons = model.newExpression("cons");
        cons.set(a, BigDecimal.ONE);
        cons.set(b, BigDecimal.ONE);
        cons.upper(BigDecimal.valueOf(5));

        // Provide a valid solution manually
        Access1D<BigDecimal> solution = Access1D.wrap(Arrays.asList(
                BigDecimal.valueOf(2), // a
                BigDecimal.valueOf(2)  // b
        ));

        assertTrue(model.validate(solution));

        // Provide an invalid solution (violates upper bound of a)
        Access1D<BigDecimal> badSolution = Access1D.wrap(Arrays.asList(
                BigDecimal.valueOf(5), // a > upper bound
                BigDecimal.valueOf(0)  // b < lower bound
        ));

        assertFalse(model.validate(badSolution));
    }

    @Test
    public void testIsUnboundedAndIsInfeasible() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable free = model.newVariable("free"); // unbounded
        Variable bounded = model.newVariable("bounded").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Variable fixed = model.newVariable("fixed");
        fixed.lower(BigDecimal.TEN).upper(BigDecimal.TEN); // fixed at 10

        assertTrue(model.isUnbounded());

        // Create an infeasible constraint: free >= 1 and free <= 0
        Expression infeas = model.newExpression("infeas");
        infeas.set(free, BigDecimal.ONE);
        infeas.lower(BigDecimal.ONE);
        infeas.upper(BigDecimal.ZERO);

        // After presolve the model should detect infeasibility
        model.presolve();
        assertTrue(model.isInfeasible());
    }
}
