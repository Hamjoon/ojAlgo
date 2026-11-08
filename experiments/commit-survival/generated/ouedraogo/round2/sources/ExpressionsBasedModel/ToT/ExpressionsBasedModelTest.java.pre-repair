package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests focus on the public API that can be exercised without requiring a concrete
 * solver integration (which would make the test suite depend on external native code).
 * They cover typical usage, edge cases and error handling.
 */
public class ExpressionsBasedModelTest {

    /** Simple helper to create a model with two variables. */
    private ExpressionsBasedModel createTwoVariableModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x");
        model.newVariable("y");
        return model;
    }

    /** --------------------------------------------------------------------- *
     *  1. Variable creation, indexing and categorisation
     *  --------------------------------------------------------------------- */
    @Test
    public void testVariableCreationAndIndices() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // add three variables, one of them integer and fixed
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        Variable z = model.newVariable("z").integer(true).lower(0).upper(10);
        z.setValue(5); // fixing z

        // indices must be consecutive starting at 0
        assertEquals(0, model.indexOf(x));
        assertEquals(1, model.indexOf(y));
        assertEquals(2, model.indexOf(z));

        // free variable indices (z is fixed)
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));
        assertEquals(-1, model.indexOfFreeVariable(z));

        // integer variable indices (only z is integer but fixed -> not free)
        assertEquals(-1, model.indexOfIntegerVariable(x));
        assertEquals(-1, model.indexOfIntegerVariable(y));
        assertEquals(0, model.indexOfIntegerVariable(z));

        // positive / negative categorisation
        // by default variables have no bounds, therefore they belong to both
        assertTrue(model.getPositiveVariables().contains(x));
        assertTrue(model.getNegativeVariables().contains(x));
        assertTrue(model.getPositiveVariables().contains(y));
        assertTrue(model.getNegativeVariables().contains(y));

        // after fixing z its bounds are known (0..10) -> positive only
        assertTrue(model.getPositiveVariables().contains(z));
        assertFalse(model.getNegativeVariables().contains(z));
    }

    /** --------------------------------------------------------------------- *
     *  2. Expression creation, linear combination and evaluation
     *  --------------------------------------------------------------------- */
    @Test
    public void testExpressionLinearEvaluation() {
        ExpressionsBasedModel model = createTwoVariableModel();

        Variable x = model.getVariable(0);
        Variable y = model.getVariable(1);

        // set concrete values for the variables
        x.setValue(2);
        y.setValue(3);

        // build expression 1*x + 2*y
        Expression expr = model.newExpression("e1");
        expr.add(x, BigDecimal.ONE);
        expr.add(y, new BigDecimal("2"));

        // evaluate – should be 2*1 + 3*2 = 8
        BigDecimal value = expr.evaluate(model.getVariableValues().asPrimitive1D());
        assertEquals(new BigDecimal("8"), value);
    }

    /** --------------------------------------------------------------------- *
     *  3. limitObjective creates a constraint expression linked to the objective
     *  --------------------------------------------------------------------- */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // one variable, weight 1 (objective contribution)
        Variable x = model.newVariable("x").weight(BigDecimal.ONE);
        x.setValue(5);

        // limit the objective between 4 and 10
        Expression limit = model.limitObjective(new BigDecimal("4"), new BigDecimal("10"));

        // the generated constraint should have the same linear term as the objective
        assertNotNull(limit);
        assertEquals(BigDecimal.ONE, limit.get(x.getIndex()));
        assertEquals(new BigDecimal("4"), limit.getLowerLimit());
        assertEquals(new BigDecimal("10"), limit.getUpperLimit());

        // the original objective remains unchanged
        Expression obj = model.objective();
        assertEquals(BigDecimal.ONE, obj.get(x.getIndex()));
    }

    /** --------------------------------------------------------------------- *
     *  4. Description counts variables / constraints correctly
     *  --------------------------------------------------------------------- */
    @Test
    public void testDescribeCounts() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // three variables: one binary, one integer, one free
        model.newVariable("a").binary();
        model.newVariable("b").integer();
        model.newVariable("c");

        // two constraints
        model.newExpression("c1").add(model.getVariable(0), BigDecimal.ONE).upper(BigDecimal.ONE);
        model.newExpression("c2").add(model.getVariable(1), BigDecimal.ONE).lower(BigDecimal.ZERO);

        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(3, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(1, desc.nbPositiveVariables); // all variables have no lower bound -> positive
        assertEquals(1, desc.nbNegativeVariables); // all variables have no upper bound -> negative

        assertEquals(0, desc.nbEqualityBounds);
        assertEquals(1, desc.nbUpperBounds);
        assertEquals(1, desc.nbLowerBounds);

        assertEquals(0, desc.nbEqualityConstraints);
        assertEquals(1, desc.nbUpperConstraints);
        assertEquals(1, desc.nbLowerConstraints);
    }

    /** --------------------------------------------------------------------- *
     *  5. Variable values retrieval and feasibility checking
     *  --------------------------------------------------------------------- */
    @Test
    public void testVariableValuesAndFeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(new BigDecimal("5"));
        Variable y = model.newVariable("y"); // unrestricted

        // give concrete values within bounds
        x.setValue(3);
        y.setValue(-7);

        Optimisation.Result result = model.getVariableValues();

        // all variables have a value → state should be FEASIBLE
        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // objective value (no objective defined → 0)
        assertEquals(0.0, result.getValue(), 1e-9);
        assertEquals(BigDecimal.valueOf(3), result.get(0));
        assertEquals(BigDecimal.valueOf(-7), result.get(1));
    }

    /** --------------------------------------------------------------------- *
     *  6. Relaxation removes integer flag
     *  --------------------------------------------------------------------- */
    @Test
    public void testRelaxationRemovesIntegerFlag() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable iVar = model.newVariable("i").integer(true);
        assertTrue(iVar.isInteger());

        // before relaxation the model reports integer variables
        assertTrue(model.isAnyVariableInteger());

        model.relax(); // hard relaxation – removes integer flag from all variables
        assertFalse(iVar.isInteger());
        assertFalse(model.isAnyVariableInteger());
    }

    /** --------------------------------------------------------------------- *
     *  7. FileFormat detection from path strings
     *  --------------------------------------------------------------------- */
    @Test
    public void testFileFormatFromPath() {
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.mps"));
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.SIF"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("model.ebm"));

        // unsupported extension should throw IllegalArgumentException
        try {
            ExpressionsBasedModel.FileFormat.from("model.txt");
            fail("Expected IllegalArgumentException for unknown extension");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    /** --------------------------------------------------------------------- *
     *  8. addSpecialOrderedSet validates input (type and binary variables)
     *  --------------------------------------------------------------------- */
    @Test
    public void testAddSpecialOrderedSetInvalidParameters() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v1 = model.newVariable("v1"); // default not binary
        Variable v2 = model.newVariable("v2").binary();

        // type must be > 0
        try {
            model.addSpecialOrderedSet(Collections.singleton(v2), 0, model.newExpression("dummy"));
            fail("Expected ProgrammingError for non‑positive SOS type");
        } catch (Exception e) {
            // ProgrammingError extends RuntimeException – any exception is fine
        }

        // linked expression must be a constraint
        Expression nonConstraint = model.newExpression("nonConstr");
        try {
            model.addSpecialOrderedSet(Collections.singleton(v2), 1, nonConstraint);
            fail("Expected ProgrammingError because linked expression is not a constraint");
        } catch (Exception e) {
            // ok
        }

        // ordered set must contain binary variables only
        try {
            model.addSpecialOrderedSet(Collections.singleton(v1), 1, model.newExpression("c").upper(BigDecimal.ONE));
            fail("Expected ProgrammingError because variable is not binary");
        } catch (Exception e) {
            // ok
        }
    }

    /** --------------------------------------------------------------------- *
     *  9. Copy and shallow copy behaviour
     *  --------------------------------------------------------------------- */
    @Test
    public void testCopyAndShallowCopyBehaviour() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable a = original.newVariable("a");
        Variable b = original.newVariable("b");
        a.lower(BigDecimal.ZERO).upper(new BigDecimal("10"));
        b.lower(BigDecimal.ONE).upper(new BigDecimal("5"));

        // deep copy – modifications to original must not affect the copy
        ExpressionsBasedModel copy = original.copy();
        a.upper(BigDecimal.ONE); // change a's upper bound only in original

        assertNotEquals(original.getVariable(0).getUpperLimit(), copy.getVariable(0).getUpperLimit());

        // shallow copy – still shares variable objects, but is flagged as shallow
        ExpressionsBasedModel shallow = original.copy(true, false);
        assertTrue(shallow.isShallowCopy());

        // attempting to add a new variable to a shallow copy must fail
        try {
            shallow.newVariable("c");
            fail("Expected IllegalStateException when modifying a shallow copy");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    /** --------------------------------------------------------------------- *
     * 10. Snapshot creates a relaxed shallow copy
     *  --------------------------------------------------------------------- */
    @Test
    public void testSnapshotCreatesRelaxedShallowCopy() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").integer(true);
        model.newVariable("y");

        ExpressionsBasedModel snapshot = model.snapshot();

        // snapshot must be shallow and relaxed
        assertTrue(snapshot.isShallowCopy());
        assertTrue(snapshot.isRelaxed());

        // integer flag should still be visible (soft relaxation)
        assertTrue(snapshot.getVariable(0).isInteger());
        // but model reports no integer variables after soft relaxation
        assertFalse(snapshot.isAnyVariableInteger());
    }

    /** --------------------------------------------------------------------- *
     * 11. getVariableValues respects variable bounds and returns INFEASIBLE when unknown
     *  --------------------------------------------------------------------- */
    @Test
    public void testGetVariableValuesInfeasibleWhenNoInfo() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("free");            // no bounds, no value
        model.newVariable("fixed").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        // fixed variable has implicit value 1

        Optimisation.Result result = model.getVariableValues();

        // because the first variable has no info, the overall state should be INFEASIBLE
        assertEquals(Optimisation.State.INFEASIBLE, result.getState());

        // the second variable should be set to its bound (1)
        assertEquals(BigDecimal.ONE, result.get(1));
    }

    /** --------------------------------------------------------------------- *
     * 12. validate method works for a simple feasible model
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidateSimpleFeasibleModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);

        // constraint: x + y <= 12
        model.newExpression("c1")
                .add(x, BigDecimal.ONE)
                .add(y, BigDecimal.ONE)
                .upper(new BigDecimal("12"));

        // set feasible values
        x.setValue(5);
        y.setValue(6);

        // validation should succeed
        assertTrue(model.validate());
    }

    /** --------------------------------------------------------------------- *
     * 13. validate fails when a variable violates its bound
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidateFailsWhenBoundViolated() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        x.setValue(2); // violates upper bound

        // no constraints – only variable bound matters
        assertFalse(model.validate());
    }
}
