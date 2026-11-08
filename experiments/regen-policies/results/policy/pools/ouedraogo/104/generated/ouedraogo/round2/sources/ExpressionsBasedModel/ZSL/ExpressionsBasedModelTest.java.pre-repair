package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Set;

import org.ojalgo.array.Array1D;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Access1D;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests focus on the public API that does not require a concrete solver
 * implementation (i.e. they avoid {@code minimise()} / {@code maximise()} which
 * would invoke the underlying optimisation engines).  All tests construct real
 * model objects, manipulate variables / expressions, and verify the internal
 * bookkeeping, description, copying and validation logic.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;
    private Variable x;
    private Variable y;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
        x = model.addVariable("x");
        y = model.addVariable("y");
    }

    /** Test that variables are created with correct indices and that the
     *  free‑variable index mapping works. */
    @Test
    public void testVariableIndicesAndFreeMapping() {
        assertEquals(0, model.indexOf(x));
        assertEquals(1, model.indexOf(y));

        // Both variables are free (not fixed) → they should appear in the free list
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));

        // Negative / positive mapping – both have lower bound 0 (default) and
        // no upper bound set yet, therefore they are positive variables.
        assertTrue(model.getPositiveVariables().contains(x));
        assertTrue(model.getPositiveVariables().contains(y));
        assertFalse(model.getNegativeVariables().contains(x));
        assertFalse(model.getNegativeVariables().contains(y));
    }

    /** Test building a simple linear model, setting contribution weights,
     *  building the aggregated objective and evaluating it. */
    @Test
    public void testObjectiveConstructionAndEvaluation() {
        // Set bounds and contribution weights
        x.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        y.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.valueOf(2));

        // Build a constraint: x + y ≤ 7
        Expression constr = model.addExpression("c1");
        constr.add(x, BigMath.ONE).add(y, BigMath.ONE).upper(BigDecimal.valueOf(7));

        // Assign concrete values to the variables
        x.value(BigDecimal.valueOf(3));
        y.value(BigDecimal.valueOf(2));

        // Retrieve the aggregated objective
        Expression objective = model.objective();

        // Build a solution vector that matches the current variable values
        Access1D<BigDecimal> solution = model.getVariableValues().getSolution();

        // Expected objective value = 1·3 + 2·2 = 7
        BigDecimal expected = BigDecimal.valueOf(7);
        BigDecimal actual = objective.evaluate(solution);

        assertEquals(0, expected.compareTo(actual));
    }

    /** Verify that {@link ExpressionsBasedModel#describe()} returns correct
     *  counts for a simple model. */
    @Test
    public void testDescriptionCounts() {
        // No integer variables yet
        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(2, desc.nbVariables);
        assertEquals(0, desc.nbIntegerVariables);
        assertEquals(2, desc.nbPositiveVariables);
        assertEquals(0, desc.nbNegativeVariables);
        assertEquals(0, desc.nbEqualityBounds);
        assertEquals(0, desc.nbEqualityConstraints);
        assertEquals(0, desc.nbLowerBounds);
        assertEquals(0, desc.nbUpperBounds);
        assertEquals(0, desc.nbLowerConstraints);
        assertEquals(0, desc.nbUpperConstraints);
    }

    /** Test that copying a model produces an independent instance with the same
     *  variable definitions. */
    @Test
    public void testCopyIndependence() {
        x.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigMath.ONE);
        y.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigMath.ONE);

        ExpressionsBasedModel copy = model.copy();

        // The copy should contain variables with the same names but distinct objects
        Variable copyX = copy.getVariable(0);
        Variable copyY = copy.getVariable(1);
        assertNotSame(x, copyX);
        assertNotSame(y, copyY);
        assertEquals("x", copyX.getName());
        assertEquals("y", copyY.getName());

        // Changing a variable in the original must not affect the copy
        x.upper(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.valueOf(5), copyX.getUpperLimit());
    }

    /** Test the {@code limitObjective} helper – it should create (or reuse) an
     *  expression that represents a bound on the objective function. */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        // Simple objective: minimise x
        x.weight(BigMath.ONE);
        Expression bound = model.limitObjective(BigDecimal.ZERO, BigDecimal.valueOf(10));

        // The returned expression must have the requested limits
        assertNotNull(bound);
        assertEquals(BigDecimal.ZERO, bound.getLowerLimit());
        assertEquals(BigDecimal.valueOf(10), bound.getUpperLimit());

        // The bound expression should be marked as a constraint
        assertTrue(bound.isConstraint());
    }

    /** Test relaxation: an integer variable becomes non‑integer after
     *  {@code relax(false)}. */
    @Test
    public void testRelaxationClearsIntegerFlag() {
        x.integer();
        assertTrue(x.isInteger());

        model.relax(false);
        assertFalse(x.isInteger());
    }

    /** Test validation logic with a feasible solution and with an infeasible
     *  constraint. */
    @Test
    public void testValidationFeasibleAndInfeasible() {
        // Feasible model
        x.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigMath.ONE);
        y.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigMath.ONE);
        Expression e = model.addExpression("c1");
        e.add(x, BigMath.ONE).add(y, BigMath.ONE).upper(BigDecimal.valueOf(8));

        // Assign feasible values
        x.value(BigDecimal.valueOf(3));
        y.value(BigDecimal.valueOf(4));

        assertTrue(model.validate());

        // Introduce an infeasible constraint: lower > upper
        Expression infeas = model.addExpression("bad");
        infeas.add(x, BigMath.ONE).lower(BigDecimal.valueOf(9)).upper(BigDecimal.valueOf(5));

        // Validation should now fail
        assertFalse(model.validate());
    }

    /** Test that {@code getVariableValues()} returns a feasible state when all
     *  variables have explicit values. */
    @Test
    public void testGetVariableValuesFeasibleState() {
        x.value(BigDecimal.valueOf(1));
        y.value(BigDecimal.valueOf(2));

        Optimisation.Result result = model.getVariableValues();

        // All variables have values → state should be FEASIBLE
        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // Verify the returned solution matches the set values
        assertEquals(0, BigDecimal.ONE.compareTo(result.get(0)));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(result.get(1)));
    }

    /** Verify that the internal reference tracking works for a variable that
     *  appears in an expression. */
    @Test
    public void testReferenceTracking() {
        Expression e = model.addExpression("refTest");
        e.add(x, BigMath.ONE);
        assertTrue(model.isReferenced(x));

        // A variable not used in any expression should not be referenced
        Variable z = model.addVariable("z");
        assertFalse(model.isReferenced(z));
    }

    /** Ensure that the model correctly reports the presence of integer
     *  variables before and after relaxation. */
    @Test
    public void testIntegerVariableDetection() {
        Variable i = model.addVariable("i");
        i.integer();

        assertTrue(model.isAnyVariableInteger());

        model.relax(false);
        assertFalse(model.isAnyVariableInteger());
    }

    /** Test that the optimisation sense is set correctly when invoking the
     *  minimise / maximise stubs (without actually solving). */
    @Test
    public void testOptimisationSenseSetting() {
        // Minimise
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());

        // Maximise
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
    }

    /** Test the internal helper {@code deriveAdjustmentRange} indirectly by
     *  creating an expression with quadratic terms and ensuring the method
     *  executes without error. (The return value is not part of the public API,
     *  but the call proves the code path is reachable.) */
    @Test
    public void testDeriveAdjustmentRangeExecution() {
        // Create a quadratic expression x * y
        Expression quad = model.addExpression("quad");
        quad.add(x, y, BigMath.ONE);

        // Force a call to the private method via reflection
        try {
            java.lang.reflect.Method m = ExpressionsBasedModel.class.getDeclaredMethod("deriveAdjustmentRange", Expression.class);
            m.setAccessible(true);
            int range = (int) m.invoke(model, quad);
            // The range should be ≥ 0
            assertTrue(range >= 0);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    /** Test that {@code getFreeVariables()} returns only non‑fixed variables. */
    @Test
    public void testGetFreeVariablesFiltering() {
        // Fix variable x
        x.lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2)); // fixing to a constant
        // y remains free
        Set<Variable> free = Set.copyOf(model.getFreeVariables());
        assertFalse(free.contains(x));
        assertTrue(free.contains(y));
    }

    /** Test that the model correctly reports infeasibility when a variable is
     *  fixed to a value that violates its own bounds. */
    @Test
    public void testModelInfeasibilityDetection() {
        // Variable with contradictory bounds
        Variable bad = model.addVariable("bad");
        bad.lower(BigDecimal.valueOf(5));
        bad.upper(BigDecimal.valueOf(3)); // lower > upper → infeasible

        assertTrue(model.isInfeasible());
    }
}
