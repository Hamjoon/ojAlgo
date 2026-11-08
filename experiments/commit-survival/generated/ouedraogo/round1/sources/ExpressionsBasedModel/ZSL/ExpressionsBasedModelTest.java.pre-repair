package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.netio.InMemoryFile;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}. The tests exercise
 * the public API only – no solver integration is required.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    /** Test that a newly created model has no variables / expressions. */
    @Test
    public void testEmptyModel() {
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        assertFalse(model.isAnyVariableFixed());
        assertFalse(model.isAnyVariableInteger());
        assertFalse(model.isAnyExpressionQuadratic());
        assertFalse(model.isAnyObjectiveQuadratic());
        assertFalse(model.isInfeasible());
        assertTrue(model.validate());
    }

    /** Test variable creation, indexing and free‑variable handling. */
    @Test
    public void testVariableCreationAndIndexing() {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");

        // default indices are assigned sequentially
        assertEquals(0, model.indexOf(x));
        assertEquals(1, model.indexOf(y));

        // both are free (not fixed) -> free indices start at 0
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));

        // after fixing y (lower == upper) it is no longer free
        y.lower(5).upper(5);
        assertEquals(-1, model.indexOfFreeVariable(y));

        // the model should now report a fixed variable
        assertTrue(model.isAnyVariableFixed());
    }

    /** Test integer / binary variable handling and relaxation. */
    @Test
    public void testIntegerAndRelaxation() {
        Variable i = Variable.makeInteger("i");
        model.addVariable(i);
        assertTrue(model.isAnyVariableInteger());

        // after a soft relaxation integer flag is ignored for the model
        model.relax(true);
        assertFalse(model.isAnyVariableInteger());

        // hard relaxation removes the integer property from the variable itself
        Variable j = Variable.makeInteger("j");
        model.addVariable(j);
        model.relax(false);
        assertFalse(j.isInteger());
    }

    /** Test expression creation, contribution weights and objective aggregation. */
    @Test
    public void testExpressionAndObjective() {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");

        // give each variable a contribution weight
        x.weight(BigDecimal.valueOf(2));
        y.weight(BigDecimal.valueOf(1));

        // the generated objective should contain both variables with the correct coefficients
        Expression obj = model.objective();
        assertEquals(BigDecimal.valueOf(2), obj.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(1), obj.get(y.getIndex()));

        // add a separate expression that also contributes to the objective
        Expression extra = model.newExpression("extra");
        extra.weight(BigDecimal.valueOf(3));
        extra.set(x.getIndex(), BigDecimal.valueOf(4)); // 4*x with weight 3 => contribution 12*x
        extra.set(y.getIndex(), BigDecimal.valueOf(5)); // 5*y with weight 3 => contribution 15*y

        // recompute the aggregated objective
        Expression agg = model.objective();
        // existing variable contributions
        assertEquals(BigDecimal.valueOf(2), agg.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(1), agg.get(y.getIndex()));
        // contributions from the extra expression (weight 3)
        assertEquals(BigDecimal.valueOf(12), agg.get(x.getIndex()).add(BigDecimal.valueOf(2).negate()));
        assertEquals(BigDecimal.valueOf(15), agg.get(y.getIndex()).add(BigDecimal.valueOf(1).negate()));
    }

    /** Test limits on the objective via {@code limitObjective}. */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        Variable a = model.newVariable("a");
        a.weight(BigDecimal.ONE);
        // create a limit on the objective (0 <= obj <= 10)
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limit);
        assertEquals(BigDecimal.ZERO, limit.getLowerLimit());
        assertEquals(BigDecimal.TEN, limit.getUpperLimit());

        // the same constraint should be reachable via its internal key
        Expression fetched = model.getExpression(ExpressionsBasedModel.OBJ_FUNC_AS_CONSTR_KEY);
        assertSame(limit, fetched);
    }

    /** Test description counts for bounds and constraints. */
    @Test
    public void testDescriptionCounts() {
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Variable v3 = model.newVariable("v3");

        // set bounds on variables
        v1.lower(0).upper(5); // lower + upper
        v2.lower(2);          // lower only
        v3.upper(7);          // upper only

        // create a constraint expression
        Expression expr = model.newExpression("c1");
        expr.set(v1.getIndex(), BigDecimal.ONE);
        expr.set(v2.getIndex(), BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(6));

        ExpressionsBasedModel.Description desc = model.describe();

        // variables
        assertEquals(3, desc.nbVariables);
        // bounds
        assertEquals(2, desc.nbLowerBounds);   // v1, v2
        assertEquals(2, desc.nbUpperBounds);   // v1, v3
        // constraints
        assertEquals(1, desc.nbLowerConstraints);  // none (expr only upper)
        assertEquals(1, desc.nbUpperConstraints);
        assertEquals(0, desc.nbEqualityConstraints);
    }

    /** Test that {@code getVariableValues} returns INFEASIBLE when variables have no info. */
    @Test
    public void testVariableValuesInfeasibleWhenUndetermined() {
        model.newVariable("free"); // no bounds, no value
        Optimisation.Result result = model.getVariableValues();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState());
    }

    /** Test a feasible solution and validation of that solution. */
    @Test
    public void testFeasibleSolutionValidation() {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");

        // bounds
        x.lower(0).upper(10);
        y.lower(0).upper(10);

        // set a feasible solution manually
        x.setValue(3);
        y.setValue(4);

        Optimisation.Result sol = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, sol.getState());

        // validation should succeed
        assertTrue(model.validate(sol));
    }

    /** Test copy, shallow copy flag and that a simplified copy is independent. */
    @Test
    public void testCopyAndSimplify() {
        Variable v = model.newVariable("v");
        v.lower(0).upper(5);
        Expression e = model.newExpression("e");
        e.set(v.getIndex(), BigDecimal.ONE);
        e.upper(BigDecimal.valueOf(3));

        ExpressionsBasedModel copy = model.copy();
        assertNotSame(model, copy);
        assertEquals(model.countVariables(), copy.countVariables());
        assertEquals(model.countExpressions(), copy.countExpressions());

        // simplify creates a new model (pruned) – original must stay unchanged
        ExpressionsBasedModel simplified = model.simplify();
        assertNotSame(model, simplified);
        assertTrue(simplified.isShallowCopy()); // simplify uses shallow copy flag internally
    }

    /** Test toString contains variable and expression information. */
    @Test
    public void testToStringContainsModelData() {
        Variable a = model.newVariable("a");
        a.lower(0).upper(1);
        Expression expr = model.newExpression("expr");
        expr.set(a.getIndex(), BigDecimal.ONE);
        expr.upper(BigDecimal.TEN);

        String txt = model.toString();
        assertTrue(txt.contains("a"));
        assertTrue(txt.contains("expr"));
        assertTrue(txt.contains("############################################"));
    }

    /** Test writing to an in‑memory file does not throw. */
    @Test
    public void testWriteToInMemoryFile() {
        Variable v = model.newVariable("v");
        v.lower(0).upper(1);
        InMemoryFile file = new InMemoryFile("model.ebm");
        // The write operation should complete without exception
        model.writeTo(file);
        // Verify that something was written
        assertTrue(file.length() > 0);
    }

    /** Test that adding and then removing an expression works. */
    @Test
    public void testAddAndRemoveExpression() {
        model.newExpression("temp");
        assertNotNull(model.getExpression("temp"));
        model.removeExpression("temp");
        assertNull(model.getExpression("temp"));
    }

    /** Test that variables can be retrieved by index and name. */
    @Test
    public void testVariableRetrieval() {
        Variable v = model.newVariable("myVar");
        int idx = model.indexOf(v);
        assertEquals(v, model.getVariable(idx));
        assertEquals(v, model.getVariable(v.getIndex()));
    }

    /** Test that the model reports correct status for integer variables after a hard relaxation. */
    @Test
    public void testIntegerStatusAfterHardRelax() {
        Variable intVar = Variable.makeInteger("intVar");
        model.addVariable(intVar);
        assertTrue(model.isAnyVariableInteger());

        model.relax(false); // hard relaxation removes integer flag from the variable
        assertFalse(intVar.isInteger());
        assertFalse(model.isAnyVariableInteger());
    }

    /** Test that {@code isAnyVariableFixed} works after fixing a variable via equal bounds. */
    @Test
    public void testAnyVariableFixedDetection() {
        Variable v = model.newVariable("fixed");
        v.lower(5).upper(5); // equal bounds => fixed
        assertTrue(model.isAnyVariableFixed());
    }

    /** Test that the model correctly reports being unbounded when a variable has no limits. */
    @Test
    public void testUnboundedDetection() {
        model.newVariable("unbounded");
        assertTrue(model.isUnbounded());
    }
}
