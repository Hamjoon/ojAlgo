package org.ojalgo.optimisation;
import org.ojalgo.structure.Access1D;


import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 tests for {@link ExpressionsBasedModel}.
 *
 * The tests are deliberately written to exercise a wide variety of public behaviour:
 *
 * <ul>
 *   <li>Construction of variables and expressions</li>
 *   <li>Aggregation of the objective function</li>
 *   <li>Quadratic factor handling</li>
 *   <li>Integer / binary handling and relaxation</li>
 *   <li>Limit‑objective helper</li>
 *   <li>Validation of feasible and infeasible solutions</li>
 *   <li>Copy / shallow‑copy semantics</li>
 *   <li>Edge‑case error handling (e.g. adding a variable to a shallow copy)</li>
 * </ul>
 *
 * Only the public API that appears in the supplied source file (or in the minimal
 * dependency signatures) is used – no mocking, no reflection on private fields
 * (except for the deliberately‑exposed {@code isShallowCopy()} method which is
 * package‑private and therefore accessed via reflection).
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    /** Simple sanity test – a variable can be added and retrieved. */
    @Test
    public void testAddVariable() {
        Variable x = model.addVariable("x");
        assertNotNull(x);
        assertEquals("x", x.getName());

        // The index returned by the model must match the variable's internal index
        int idx = model.indexOf(x);
        assertEquals(idx, x.getIndex().index);
    }

    /** Adding an expression and checking that the generated objective aggregates correctly. */
    @Test
    public void testObjectiveAggregation() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // Give each variable a contribution weight
        x.weight(BigDecimal.valueOf(2));
        y.weight(BigDecimal.valueOf(3));

        // Create a separate expression that also contributes to the objective
        Expression expr = model.addExpression("extra");
        expr.weight(BigDecimal.valueOf(4));
        expr.add(x, BigDecimal.ONE);   // +1*x
        expr.add(y, BigDecimal.valueOf(2)); // +2*y

        // Build the aggregated objective
        Expression objective = model.objective();

        // Expected linear coefficients:
        //   x : 2 (from variable) + 4*1 = 6
        //   y : 3 (from variable) + 4*2 = 11
        assertEquals(BigDecimal.valueOf(6), objective.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(11), objective.get(y.getIndex()));
        // No constant part has been added
        assertEquals(BigDecimal.ZERO, objective.getConstant());
    }

    /** Verify that a quadratic expression is recognised as such. */
    @Test
    public void testQuadraticDetection() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");

        Expression quad = model.addExpression("quad");
        // add a quadratic term a*b
        quad.add(a, b, BigDecimal.ONE);

        assertTrue("Expression should have a quadratic factor", quad.isAnyQuadraticFactorNonZero());
        assertTrue("Model must report a quadratic expression", model.isAnyExpressionQuadratic());
    }

    /** Integer handling – integer flag, relaxation and the corresponding model queries. */
    @Test
    public void testIntegerAndRelaxation() {
        Variable i = model.addVariable("i");
        i.integer(true);
        assertTrue(i.isInteger());

        // Model should now report an integer variable
        assertTrue(model.isAnyVariableInteger());

        // Soft relaxation: the model is still flagged as relaxed, but integer flag stays on the variable
        model.relax(true);
        assertFalse("After soft relaxation the model must not consider itself integer", model.isAnyVariableInteger());

        // Hard relaxation: integer flag cleared from all variables
        model.relax(false);
        assertFalse(i.isInteger());
        assertFalse(model.isAnyVariableInteger());
    }

    /** limitObjective should create (or reuse) a constraint expression with the supplied bounds. */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        Variable x = model.addVariable("x");
        x.weight(BigDecimal.ONE);
        // No explicit objective expression – the model's aggregated objective will be just x

        // Apply limits to the objective: 0 ≤ obj ≤ 10
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);

        assertNotNull(limit);
        assertEquals(BigDecimal.ZERO, limit.getLowerLimit());
        assertEquals(BigDecimal.TEN, limit.getUpperLimit());

        // The limit expression should be linked to the same linear coefficients as the objective
        assertEquals(BigDecimal.ONE, limit.get(x.getIndex()));
    }

    /** Validation succeeds for a feasible solution and fails for an infeasible one. */
    @Test
    public void testValidateFeasibleAndInfeasibleSolutions() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // Simple constraint: 0 ≤ x + y ≤ 5
        Expression c = model.addExpression("c");
        c.add(x, BigDecimal.ONE);
        c.add(y, BigDecimal.ONE);
        c.lower(BigDecimal.ZERO);
        c.upper(BigDecimal.valueOf(5));

        // Feasible solution (x=2, y=2)
        Access1D<BigDecimal> feasible = Access1D.wrap(new BigDecimal[] { BigDecimal.valueOf(2), BigDecimal.valueOf(2) });
        assertTrue("Feasible solution should validate", model.validate(feasible));

        // Infeasible solution (x=4, y=4) violates upper bound 5
        Access1D<BigDecimal> infeasible = Access1D.wrap(new BigDecimal[] { BigDecimal.valueOf(4), BigDecimal.valueOf(4) });
        assertFalse("Infeasible solution should not validate", model.validate(infeasible));
    }

    /** getVariableValues should return a feasible solution when all variables have values set. */
    @Test
    public void testGetVariableValuesAllSet() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");

        a.setValue(BigDecimal.valueOf(3));
        b.setValue(BigDecimal.valueOf(7));

        Optimisation.Result result = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, result.getState());
        assertEquals(3.0, result.doubleValue(0), 1e-12);
        assertEquals(7.0, result.doubleValue(1), 1e-12);
    }

    /** Shallow copy semantics – adding a variable to a shallow copy must raise an exception. */
    @Test(expected = IllegalStateException.class)
    public void testShallowCopyCannotAddVariable() throws Exception {
        // Create a shallow copy (the first boolean = shallow, second = prune)
        ExpressionsBasedModel shallow = model.copy(true, false);

        // Verify (via reflection) that the copy is indeed shallow
        java.lang.reflect.Method isShallow = ExpressionsBasedModel.class.getDeclaredMethod("isShallowCopy");
        isShallow.setAccessible(true);
        assertTrue("Copy should be shallow", (boolean) isShallow.invoke(shallow));

        // Attempt to add a new variable – should throw IllegalStateException
        shallow.addVariable("newVar");
    }

    /** Copy produces an independent model – modifications to the original do not affect the copy. */
    @Test
    public void testCopyIndependence() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.TEN);

        ExpressionsBasedModel copy = model.copy();

        // Change the original variable bounds
        x.lower(BigDecimal.ONE);
        x.upper(BigDecimal.valueOf(9));

        // The copy must retain the original bounds
        Variable copyX = copy.getVariable(0);
        assertEquals(BigDecimal.ZERO, copyX.getLowerLimit());
        assertEquals(BigDecimal.TEN, copyX.getUpperLimit());
    }

    /** Adding a variable with a duplicate name is allowed (the model uses internal indices, not names). */
    @Test
    public void testDuplicateVariableNames() {
        Variable v1 = model.addVariable("dup");
        Variable v2 = model.addVariable("dup"); // same name, distinct variable
        assertNotEquals(v1.getIndex().index, v2.getIndex().index);
    }

    /** Simple presolver behaviour – a binary variable with a zero‑one‑two presolver should not cause errors. */
    @Test
    public void testBinaryVariablePresolverInteraction() {
        Variable b = model.addVariable("b");
        b.binary(); // mark as binary (0/1)

        // Constraint: b ≤ 1 (trivial, but triggers the ZERO_ONE_TWO presolver)
        Expression e = model.addExpression("binaryConstraint");
        e.add(b, BigDecimal.ONE);
        e.upper(BigDecimal.ONE);

        // No exception means the presolver handled the case; also the model should not be infeasible
        assertFalse("Model should not be marked infeasible", model.isInfeasible());
    }

    /** Test that isAnyVariableFixed reports correctly after fixing a variable. */
    @Test
    public void testIsAnyVariableFixed() {
        Variable v = model.addVariable("v");
        assertFalse(model.isAnyVariableFixed());

        v.setValue(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed());
    }

    /** Verify that calling minimise sets the optimisation sense to MIN. */
    @Test
    public void testMinimiseSetsSense() {
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());

        // The result state is UNEXPLORED because the model contains no variables / constraints
        assertEquals(Optimisation.State.UNEXPLORED, result.getState());
    }

    /** Verify that calling maximise sets the optimisation sense to MAX. */
    @Test
    public void testMaximiseSetsSense() {
        Optimisation.Result result = model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());

        // The result state is UNEXPLORED because the model contains no variables / constraints
        assertEquals(Optimisation.State.UNEXPLORED, result.getState());
    }

    /** Test that a model with an unbounded variable reports unboundedness. */
    @Test
    public void testIsUnbounded() {
        Variable v = model.addVariable("v"); // default: no lower/upper limits → unbounded
        assertTrue("Model should report unbounded variables", model.isUnbounded());
    }

    /** Test that the model correctly reports having no quadratic constraints. */
    @Test
    public void testIsAnyConstraintQuadratic() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");

        // Linear constraint
        Expression lin = model.addExpression("lin");
        lin.add(a, BigDecimal.ONE);
        lin.upper(BigDecimal.TEN);

        // Quadratic constraint
        Expression quad = model.addExpression("quad");
        quad.add(a, b, BigDecimal.ONE);
        quad.upper(BigDecimal.valueOf(100));

        assertFalse("Linear constraint should not be quadratic", lin.isAnyQuadraticFactorNonZero());
        assertTrue("Quadratic constraint should be quadratic", quad.isAnyQuadraticFactorNonZero());
        assertTrue("Model should report a quadratic constraint", model.isAnyConstraintQuadratic());
    }

    /** Test that the model's toString() produces a non‑empty string containing variable and expression names. */
    @Test
    public void testToStringContainsElements() {
        model.addVariable("x");
        model.addExpression("expr");
        String representation = model.toString();
        assertNotNull(representation);
        assertTrue(representation.contains("x"));
        assertTrue(representation.contains("expr"));
    }

    /** Test that disposing a model clears its internal collections. */
    @Test
    public void testDisposeClearsModel() {
        model.addVariable("x");
        model.addExpression("expr");
        model.dispose();

        // After dispose, internal collections should be empty (public getters return empty collections)
        assertTrue("Variables should be cleared after dispose", model.getVariables().isEmpty());
        assertTrue("Expressions should be cleared after dispose", model.getExpressions().isEmpty());
    }
}
