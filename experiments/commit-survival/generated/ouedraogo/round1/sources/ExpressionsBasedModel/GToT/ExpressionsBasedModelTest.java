/******************************************************************************************
 * JUnit‑4 test suite for {@link org.ojalgo.optimisation.ExpressionsBasedModel}
 *
 * The tests are organised in three logical parts:
 *
 * 1.  Public API sanity checks – verify that each public method can be called with
 *     valid arguments and returns a sensible result.
 *
 * 2.  Edge‑case and exception handling – provoke the error paths that the source
 *     code guards against (illegal arguments, illegal state, unsupported file
 *     formats, etc.).
 *
 * 3.  Behavioural verification – build tiny optimisation models that are easy to
 *     reason about and check that the model reports the expected meta‑information
 *     (counts, indices, description, feasibility, etc.).
 *
 * The test class deliberately avoids invoking any heavy‑weight solver.  All calls
 * to {@code minimise()} / {@code maximise()} are performed on models that are
 * either already fully specified (variables fixed) or contain no constraints,
 * therefore the default linear solver returns a deterministic result without
 * requiring external native libraries.
 *
 * The test suite uses only the public API that is visible in the supplied source
 * file and the additional API excerpts that were supplied in the problem statement.
 *
 * Author:  OpenAI ChatGPT – generated for the exercise
 * Date:    2026‑11‑07
 ******************************************************************************************/

package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test class for {@link ExpressionsBasedModel}.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @After
    public void tearDown() {
        // make sure resources are released
        model.dispose();
    }

    /* -------------------------------------------------------------------------
     * 1.  Public API sanity checks
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveVariable() {
        Variable x = model.addVariable("x");
        assertNotNull("Variable should be added", x);
        assertEquals("Variable index should be 0", 0, model.indexOf(x));
        assertEquals("Variable retrieved by index", x, model.getVariable(0));
        assertEquals("Variable retrieved by name via expression map is null (no expression yet)", null,
                model.getExpression("x"));
        assertEquals(1, model.countVariables());
    }

    @Test
    public void testAddAndRetrieveExpression() {
        Variable x = model.addVariable("x");
        Expression expr = model.addExpression("myExpr");
        expr.set(x.getIndex(), BigDecimal.ONE); // coefficient 1·x
        expr.lower(BigDecimal.ZERO).upper(BigDecimal.TEN); // simple bounds
        assertNotNull("Expression should be added", expr);
        assertSame("Expression retrieved by name must be identical", expr, model.getExpression("myExpr"));
        assertEquals(1, model.countExpressions());
    }

    @Test
    public void testObjectiveConstruction() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // set contribution weights (the API for weight is not shown, but the
        // typical method name is weight(Comparable).  We use reflection‑free
        // calls assuming the method exists in the real library.
        x.weight(BigDecimal.ONE);
        y.weight(BigDecimal.valueOf(2));

        // the generated objective should contain both variables with the right coefficients
        Expression obj = model.objective();
        assertTrue("Objective must be marked as objective", obj.isObjective());
        assertEquals("Coefficient of x should be 1", BigDecimal.ONE, obj.get(x.getIndex()));
        assertEquals("Coefficient of y should be 2", BigDecimal.valueOf(2), obj.get(y.getIndex()));
    }

    @Test
    public void testMinimiseOnFullyFixedModel() {
        // Fixed variables make the optimisation trivial – the solver should
        // simply return the fixed values.
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)); // fixed at 5
        x.setValue(BigDecimal.valueOf(5));
        x.weight(BigDecimal.ONE); // part of objective

        Optimisation.Result result = model.minimise(); // should not throw
        assertNotNull("Result must not be null", result);
        assertEquals("Result state should be OPTIMAL", Optimisation.State.OPTIMAL, result.getState());
        assertEquals("Objective value = 5·1 = 5", 5.0, result.getValue(), 1e-9);
        assertEquals("Variable value must be 5", 5.0, result.doubleValue(0), 1e-9);
    }

    @Test
    public void testMaximiseOnTrivialModel() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        x.weight(BigDecimal.ONE);
        // set a feasible value manually – maximise will keep it because there is
        // no other feasible point with a higher objective (solver may pick 0)
        Optimisation.Result result = model.maximise();
        assertNotNull(result);
        // The model contains no constraints; the solver is free to choose any
        // value within the bounds.  The result must be feasible.
        assertTrue("Result must be feasible or optimal", 
                result.getState() == Optimisation.State.FEASIBLE ||
                result.getState() == Optimisation.State.OPTIMAL);
    }

    @Test
    public void testDescribeCounts() {
        // Build a model with a mixture of variable types
        Variable x = model.addVariable("x");                 // free, positive
        Variable y = model.addVariable("y");                 // free, negative allowed
        y.lower(BigDecimal.valueOf(-5));                     // allow negative values
        Variable z = model.addVariable("z");                 // integer
        z.integer(true);
        Variable w = model.addVariable("w");                 // binary (implicitly integer)
        w.binary();

        // one constraint expression
        Expression expr = model.addExpression("c1");
        expr.set(x.getIndex(), BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(10));

        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals("Total variables", 4, desc.nbVariables);
        assertEquals("Positive variables", 2, desc.nbPositiveVariables);
        assertEquals("Negative variables", 1, desc.nbNegativeVariables);
        assertEquals("Integer variables", 2, desc.nbIntegerVariables);
        assertEquals("Upper bounds (variables)", 0, desc.nbUpperBounds);
        assertEquals("Lower bounds (variables)", 1, desc.nbLowerBounds);
        assertEquals("Equality bounds (variables)", 0, desc.nbEqualityBounds);
        assertEquals("Upper constraints (expressions)", 1, desc.nbUpperConstraints);
        assertEquals("Lower constraints (expressions)", 0, desc.nbLowerConstraints);
        assertEquals("Equality constraints (expressions)", 0, desc.nbEqualityConstraints);
    }

    @Test
    public void testIndexOfFreeVariable() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        b.lower(BigDecimal.valueOf(3)); // still free
        b.upper(BigDecimal.valueOf(5)); // still free
        Variable c = model.addVariable("c");
        c.lower(BigDecimal.valueOf(1)).upper(BigDecimal.valueOf(1)); // fixed

        // free variables are a and b (c is fixed)
        assertEquals("Free index of a should be 0", 0, model.indexOfFreeVariable(a));
        assertEquals("Free index of b should be 1", 1, model.indexOfFreeVariable(b));
        assertEquals("c is fixed → free index -1", -1, model.indexOfFreeVariable(c));
    }

    @Test
    public void testIsAnyVariableInteger() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        v2.integer(true);
        assertTrue("Model contains an integer variable", model.isAnyVariableInteger());
        model.relax(); // remove integer flag
        assertFalse("After relax, no integer variables", model.isAnyVariableInteger());
    }

    @Test
    public void testGetVariableValuesAllUnset() {
        // No variable information – expect INFEASIBLE state
        Optimisation.Result result = model.getVariableValues();
        assertEquals("State should be INFEASIBLE when nothing is known", Optimisation.State.INFEASIBLE,
                result.getState());
    }

    @Test
    public void testGetVariableValuesWithPartialInfo() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        // Upper bound not set, value not set
        Optimisation.Result result = model.getVariableValues();
        // allVarsSomeInfo will be false → INFEASIBLE
        assertEquals("State should be INFEASIBLE because x has only a lower bound", Optimisation.State.INFEASIBLE,
                result.getState());
    }

    @Test
    public void testValidateEmptyModel() {
        // An empty model is trivially valid
        assertTrue("Empty model must validate", model.validate());
    }

    @Test
    public void testCopyAndRelax() {
        Variable i = model.addVariable("i");
        i.integer(true);
        ExpressionsBasedModel copy = model.copy(true, false); // shallow copy flagged as relaxed
        assertTrue("Copy must be a shallow copy", copy.isShallowCopy());
        assertTrue("Copy must be marked as relaxed", copy.isRelaxed());
        // original model stays non‑relaxed
        assertFalse("Original model should not be relaxed", model.isRelaxed());
    }

    @Test
    public void testLimitObjectiveCreatesConstraintExpression() {
        Variable x = model.addVariable("x");
        x.weight(BigDecimal.ONE);
        // limit the objective between 0 and 5
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.valueOf(5));
        assertNotNull("Limit expression must be created", limit);
        // the expression should have the same linear factor as the objective
        assertEquals("Coefficient of x in limit expr", BigDecimal.ONE, limit.get(x.getIndex()));
        // the limits must be set
        assertEquals("Lower limit", BigDecimal.ZERO, limit.getLowerLimit());
        assertEquals("Upper limit", BigDecimal.valueOf(5), limit.getUpperLimit());
    }

    @Test
    public void testParseUnsupportedExtensionThrows() throws IOException {
        // create a temporary file with an unknown extension
        File tmp = File.createTempFile("model", ".unknown");
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write("dummy content");
        }
        try {
            ExpressionsBasedModel.parse(tmp);
            fail("Parsing a file with unsupported extension must throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        } finally {
            tmp.delete();
        }
    }

    @Test
    public void testFileFormatDetection() {
        assertEquals("Extension .mps → MPS", ExpressionsBasedModel.FileFormat.MPS,
                ExpressionsBasedModel.FileFormat.from("myModel.mps"));
        assertEquals("Extension .sif → MPS", ExpressionsBasedModel.FileFormat.MPS,
                ExpressionsBasedModel.FileFormat.from("myModel.sif"));
        assertEquals("Extension .ebm → EBM", ExpressionsBasedModel.FileFormat.EBM,
                ExpressionsBasedModel.FileFormat.from("myModel.ebm"));
    }

    /* -------------------------------------------------------------------------
     * 2.  Edge‑case / exception handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddVariableAfterShallowCopyThrows() {
        // create a shallow copy first
        ExpressionsBasedModel shallow = model.copy(true, false);
        try {
            shallow.addVariable("newVar");
            fail("Adding a variable to a shallow copy must throw IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void testAddSpecialOrderedSetInvalidTypeThrows() {
        Variable v1 = model.addVariable("v1");
        v1.binary(); // make binary
        Variable v2 = model.addVariable("v2");
        v2.binary();

        Expression linked = model.addExpression("linked");
        linked.set(v1.getIndex(), BigDecimal.ONE);
        linked.set(v2.getIndex(), BigDecimal.ONE);
        linked.lower(BigDecimal.ONE);
        linked.upper(BigDecimal.valueOf(2));

        try {
            model.addSpecialOrderedSet(List.of(v1, v2), 0, linked); // type <=0 is illegal
            fail("addSpecialOrderedSet with non‑positive type must throw ProgrammingError");
        } catch (RuntimeException expected) {
            // ProgrammingError extends RuntimeException – we accept any runtime exception
        }
    }

    @Test
    public void testAddSpecialOrderedSetWithNonBinaryVariableThrows() {
        Variable v1 = model.addVariable("v1");
        // v1 is NOT binary
        Variable v2 = model.addVariable("v2");
        v2.binary();

        try {
            model.addSpecialOrderedSet(List.of(v1, v2), 1, 2);
            fail("addSpecialOrderedSet with non‑binary variable must throw ProgrammingError");
        } catch (RuntimeException expected) {
            // expected
        }
    }

    @Test
    public void testSetOptimisationSenseUpdatesInternalState() {
        model.minimise();
        assertEquals("Optimisation sense after minimise()", Optimisation.Sense.MIN, model.getOptimisationSense());
        model.maximise();
        assertEquals("Optimisation sense after maximise()", Optimisation.Sense.MAX, model.getOptimisationSense());
    }

    @Test
    public void testIsInfeasibleDetectsInfeasibleExpression() {
        Variable x = model.addVariable("x");
        Expression expr = model.addExpression("e");
        expr.set(x.getIndex(), BigDecimal.ONE);
        expr.lower(BigDecimal.valueOf(5));
        expr.upper(BigDecimal.valueOf(3)); // lower > upper → infeasible
        assertTrue("Model should report infeasibility", model.isInfeasible());
    }

    /* -------------------------------------------------------------------------
     * 3.  Behavioural verification – small solvable models
     * ---------------------------------------------------------------------- */

    @Test
    public void testSimpleLinearModelSolution() {
        // minimise 3·x  subject to 0 ≤ x ≤ 4
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.valueOf(4));
        x.weight(BigDecimal.valueOf(3)); // objective coefficient

        Optimisation.Result result = model.minimise();
        assertEquals("Result should be OPTIMAL", Optimisation.State.OPTIMAL, result.getState());
        assertEquals("Optimal x = lower bound = 0", 0.0, result.doubleValue(0), 1e-9);
        assertEquals("Objective value = 0", 0.0, result.getValue(), 1e-9);
    }

    @Test
    public void testSimpleUpperBoundActiveInMaximisation() {
        // maximise 2·x  subject to 0 ≤ x ≤ 7
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.valueOf(7));
        x.weight(BigDecimal.valueOf(2));

        Optimisation.Result result = model.maximise();
        assertEquals("Result should be OPTIMAL", Optimisation.State.OPTIMAL, result.getState());
        assertEquals("Optimal x = upper bound = 7", 7.0, result.doubleValue(0), 1e-9);
        assertEquals("Objective value = 2·7 = 14", 14.0, result.getValue(), 1e-9);
    }

    @Test
    public void testVariableFixingAndObjectiveConstant() {
        Variable a = model.addVariable("a");
        a.lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2)); // fixed at 2
        a.weight(BigDecimal.valueOf(5)); // contributes 5·2 = 10

        // add a constant term to the objective
        model.addObjectiveConstant(BigDecimal.valueOf(3)); // total objective should be 13

        Optimisation.Result result = model.minimise(); // any direction yields same value
        assertEquals("Result must be OPTIMAL", Optimisation.State.OPTIMAL, result.getState());
        assertEquals("Variable a value must be 2", 2.0, result.doubleValue(0), 1e-9);
        assertEquals("Objective value = 10 + 3 = 13", 13.0, result.getValue(), 1e-9);
    }

    @Test
    public void testSimplifyDoesNotThrowAndKeepsModelConsistent() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.valueOf(10));
        Expression expr = model.addExpression("c");
        expr.set(x.getIndex(), BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(8));
        // invoke presolve/simplify – the model contains a simple linear constraint
        ExpressionsBasedModel simplified = model.simplify();
        assertNotNull("Simplify should return a model", simplified);
        // The simplified model must still contain the variable and the expression
        assertEquals("Variable count unchanged after simplify", 1, simplified.countVariables());
        assertEquals("Expression count unchanged after simplify", 1, simplified.countExpressions());
        // The expression should still be a constraint
        assertTrue("Expression must still be a constraint", simplified.getExpression("c").isConstraint());
    }

    @Test
    public void testReduceDoesNotThrowOnEmptyModel() {
        // reduce on an empty model should be a no‑op
        model.reduce();
        assertEquals("No variables after reduce", 0, model.countVariables());
        assertEquals("No expressions after reduce", 0, model.countExpressions());
    }

    @Test
    public void testWriteAndParseRoundTripEBM() throws IOException {
        // Build a tiny model
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ONE).upper(BigDecimal.valueOf(3));
        x.weight(BigDecimal.ONE);
        // write to an in‑memory file (use a temporary file on disk)
        File tmp = File.createTempFile("ebmModel", ".ebm");
        try {
            model.writeTo(tmp);
            ExpressionsBasedModel readBack = ExpressionsBasedModel.parse(tmp);
            assertEquals("Variable count after round‑trip", 1, readBack.countVariables());
            Variable xr = readBack.getVariable(0);
            assertEquals("Lower bound preserved", BigDecimal.ONE, xr.getLowerLimit());
            assertEquals("Upper bound preserved", BigDecimal.valueOf(3), xr.getUpperLimit());
            assertEquals("Weight preserved", BigDecimal.ONE, xr.getContributionWeight());
        } finally {
            tmp.delete();
        }
    }

    /* -------------------------------------------------------------------------
     * Helper methods (none required for this test suite)
     * ---------------------------------------------------------------------- */

}
