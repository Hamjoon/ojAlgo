package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.type.context.NumberContext;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic model construction (variables, expressions, objective)</li>
 *   <li>optimisation (minimise / maximise) of a simple linear problem</li>
 *   <li>edge‑case handling (invalid SOS type, non‑binary SOS variable, illegal file format)</li>
 *   <li>model description, copying, relaxing and known‑solution handling</li>
 *   <li>validation of infeasible models and index utilities</li>
 * </ul>
 *
 * All tests use only the public API that is present in the supplied source code.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;
    private Variable x;
    private Variable y;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
        x = model.addVariable("X");
        y = model.addVariable("Y");
    }

    /** Helper to build a very small linear model:  minimise 2·X subject 0 ≤ X ≤ 10 */
    private ExpressionsBasedModel buildSimpleLinearModel() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.addVariable("V").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        v.weight(BigDecimal.valueOf(2)); // contribution to objective
        return m;
    }

    /** 1. Basic variable / expression creation and objective evaluation */
    @Test
    public void testAddVariableAndExpression() {
        // set contribution weight on variable X
        x.weight(BigDecimal.valueOf(3));

        // create a separate expression that adds Y with coefficient 4
        Expression expr = model.addExpression("E1");
        expr.set(y, BigDecimal.valueOf(4));

        // mark both variable and expression as part of the objective
        x.objective();
        expr.objective();

        // evaluate objective with X = 1, Y = 2
        x.setValue(BigDecimal.ONE);
        y.setValue(BigDecimal.valueOf(2));

        BigDecimal objValue = model.objective().evaluate(model.getVariableValues());
        assertEquals("Objective should be 3*1 + 4*2 = 11", BigDecimal.valueOf(11), objValue);
    }

    /** 2. Minimise a simple linear problem – solution should be at the lower bound */
    @Test
    public void testMinimiseSimpleLinear() {
        ExpressionsBasedModel m = buildSimpleLinearModel();
        Optimisation.Result result = m.minimise();

        assertTrue("Result must be feasible", result.getState().isFeasible());
        // variable V is at its lower bound 0
        assertEquals(0.0, result.doubleValue(0), 1e-9);
        // objective = 2 * 0 = 0
        assertEquals(0.0, result.getValue(), 1e-9);
    }

    /** 3. Maximise a simple linear problem – solution should be at the upper bound */
    @Test
    public void testMaximiseSimpleLinear() {
        ExpressionsBasedModel m = buildSimpleLinearModel();
        Optimisation.Result result = m.maximise();

        assertTrue("Result must be feasible", result.getState().isFeasible());
        // variable V is at its upper bound 10
        assertEquals(10.0, result.doubleValue(0), 1e-9);
        // objective = 2 * 10 = 20
        assertEquals(20.0, result.getValue(), 1e-9);
    }

    /** 4. limitObjective creates an auxiliary constraint expression */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        x.weight(BigDecimal.ONE);
        Expression aux = model.limitObjective(BigDecimal.ZERO, BigDecimal.ONE);
        assertNotNull("Auxiliary expression must be created", aux);
        assertTrue("Auxiliary expression must be a constraint", aux.isConstraint());

        // the auxiliary expression should have the same linear term as the objective (coefficient 1)
        assertEquals(BigDecimal.ONE, aux.get(x));
        // bounds are as we set
        assertEquals(BigDecimal.ZERO, aux.getLowerLimit());
        assertEquals(BigDecimal.ONE, aux.getUpperLimit());
    }

    /** 5. Adding a special ordered set with an illegal type must throw ProgrammingError (RuntimeException) */
    @Test(expected = RuntimeException.class)
    public void testAddSpecialOrderedSetInvalidType() {
        List<Variable> set = Arrays.asList(x);
        // illegal SOS type (<=0)
        model.addSpecialOrderedSet(set, 0, model.addExpression("dummy"));
    }

    /** 6. Adding a special ordered set where a variable is not binary must throw ProgrammingError */
    @Test(expected = RuntimeException.class)
    public void testAddSpecialOrderedSetNonBinaryVariable() {
        // make X non‑binary (default is continuous)
        List<Variable> set = Arrays.asList(x);
        // valid type but X is not binary -> exception
        model.addSpecialOrderedSet(set, 1, 2);
    }

    /** 7. Verify Description counts after creating variables with bounds and a constraint expression */
    @Test
    public void testDescriptionCounts() {
        // variable X: lower bound only
        x.lower(BigDecimal.ZERO);
        // variable Y: upper bound only
        y.upper(BigDecimal.valueOf(5));

        // constraint: X + Y ≤ 7
        Expression c = model.addExpression("c1");
        c.set(x, BigDecimal.ONE);
        c.set(y, BigDecimal.ONE);
        c.upper(BigDecimal.valueOf(7));

        ExpressionsBasedModel.Description d = model.describe();

        assertEquals("Two variables total", 2, d.nbVariables);
        assertEquals("One lower bound (X)", 1, d.nbLowerBounds);
        assertEquals("One upper bound (Y)", 1, d.nbUpperBounds);
        assertEquals("One inequality constraint (c1)", 1, d.nbUpperConstraints);
    }

    /** 8. Shallow copy with pruning should be independent of the original model */
    @Test
    public void testCopyShallowAndPrune() {
        // original model: give X a weight of 5
        x.weight(BigDecimal.valueOf(5));
        // also create a constraint
        Expression e = model.addExpression("E");
        e.set(x, BigDecimal.ONE).upper(BigDecimal.TEN);

        // shallow copy with pruning
        ExpressionsBasedModel copy = model.copy(true, true);

        // change original weight – copy must stay unchanged
        x.weight(BigDecimal.ZERO);
        assertEquals("Copy should retain original weight 5", BigDecimal.valueOf(5), copy.objective().get(x));

        // the constraint should also be present in the copy (pruned means only non‑redundant kept)
        assertNotNull("Constraint must be present in copy", copy.getExpression("E"));
    }

    /** 9. Relaxing an integer variable makes the model report no integer variables */
    @Test
    public void testRelaxationRemovesIntegerFlag() {
        Variable intVar = model.addVariable("I").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertTrue("Model initially has integer variables", model.isAnyVariableInteger());

        model.relax(); // hard relaxation removes integer flag from all variables
        assertFalse("After hard relaxation there must be no integer variables", model.isAnyVariableInteger());
    }

    /** 10. FileFormat.from with an unsupported extension must throw IllegalArgumentException */
    @Test(expected = IllegalArgumentException.class)
    public void testFileFormatFromInvalidExtension() {
        File unknown = new File("model.xyz");
        ExpressionsBasedModel.FileFormat.from(unknown);
    }

    /** 11. Validation fails when a variable has contradictory bounds (lower > upper) */
    @Test
    public void testValidateInfeasibleVariable() {
        // create contradictory bounds
        x.lower(BigDecimal.TEN);
        x.upper(BigDecimal.ONE);
        assertTrue("Model should detect infeasibility", model.isInfeasible());

        // validation should return false
        assertFalse("validate() must be false for infeasible model", model.validate());
    }

    /** 12. Index utilities return –1 for non‑free / non‑integer variables */
    @Test
    public void testIndexUtilities() {
        // X is free (no fixed value)
        int freeIdx = model.indexOfFreeVariable(x);
        assertTrue("Free index must be >= 0", freeIdx >= 0);

        // Y is not integer
        int intIdx = model.indexOfIntegerVariable(y);
        assertEquals("Y is not integer -> index should be -1", -1, intIdx);
    }

    /** 13. setKnownSolution must reject a non‑optimal result */
    @Test(expected = RuntimeException.class) // ProgrammingError extends RuntimeException
    public void testSetKnownSolutionRejectsNonOptimal() {
        // create a feasible but not optimal result (state = FEASIBLE)
        Optimisation.Result notOptimal = Optimisation.Result.of(Optimisation.State.FEASIBLE, 0.0);
        model.setKnownSolution(notOptimal);
    }

    /** 14. Simple toString sanity check – should contain variable and expression names */
    @Test
    public void testToStringContainsNames() {
        x.lower(BigDecimal.ZERO);
        Expression expr = model.addExpression("MyExpr");
        expr.set(x, BigDecimal.ONE).upper(BigDecimal.TEN);
        String txt = model.toString();

        assertTrue("String representation must contain variable name", txt.contains("X"));
        assertTrue("String representation must contain expression name", txt.contains("MyExpr"));
    }

    /** 15. prepare() with a trivial intermediate solver – ensure it returns a non‑null solver */
    @Test
    public void testPrepareReturnsIntermediateSolver() {
        ExpressionsBasedModel.DefaultIntermediate solver = model.prepare(ExpressionsBasedModel.DefaultIntermediate::new);
        assertNotNull("prepare() must return a non‑null intermediate solver", solver);
        solver.dispose(); // clean up
    }
}
