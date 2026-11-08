package org.ojalgo.optimisation;

import static org.junit.Assert.*;
import static org.ojalgo.function.constant.BigMath.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel.Description;
import org.ojalgo.type.context.NumberContext;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests are organised in three logical groups:
 *
 * 1. **Basic functionality** – each public method gets a minimal sanity‑check.
 * 2. **Edge‑case / exception handling** – verifies behaviour on unusual inputs.
 * 3. **Combined scenarios** – small realistic models exercising several methods together.
 *
 * No external solvers are invoked; the suite therefore stays self‑contained
 * and runs on the pure Java implementation of the library.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    @After
    public void tearDown() {
        model.dispose();
    }

    /* -------------------------------------------------
     * 1. BASIC PUBLIC‑API TESTS
     * ------------------------------------------------- */

    @Test
    public void testAddVariableAndGetVariable() {
        Variable v = model.addVariable("x");
        assertNotNull(v);
        assertEquals("x", v.getName());
        assertSame(v, model.getVariable(0));
    }

    @Test
    public void testAddExpressionAndRetrieve() {
        Variable x = model.addVariable();
        Expression e = model.addExpression("myExpr");
        e.set(x.getIndex(), ONE);
        assertSame(e, model.getExpression("myExpr"));
    }

    @Test
    public void testObjectiveComposition() {
        Variable x = model.addVariable().weight(ONE);
        Variable y = model.addVariable().weight(TWO);
        Expression obj = model.objective();
        assertEquals(ONE, obj.get(x.getIndex()));
        assertEquals(TWO, obj.get(y.getIndex()));
    }

    @Test
    public void testLimitObjectiveCreatesConstraint() {
        model.addVariable().weight(ONE);
        Expression limit = model.limitObjective(BigDecimal.ONE, BigDecimal.TEN);
        assertNotNull(limit);
        assertEquals(BigDecimal.ONE, limit.getLowerLimit());
        assertEquals(BigDecimal.TEN, limit.getUpperLimit());
    }

    @Test
    public void testDescribeCounts() {
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.addVariable().integer();
        model.addExpression().lower(BigDecimal.ZERO);
        Description d = model.describe();
        assertEquals(2, d.nbVariables);
        assertEquals(1, d.nbIntegerVariables);
        assertEquals(1, d.nbLowerBounds);
    }

    @Test
    public void testIndexOfFreeVariable() {
        Variable x = model.addVariable(); // free
        Variable y = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ZERO); // fixed
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(-1, model.indexOfFreeVariable(y));
    }

    @Test
    public void testIsAnyVariableInteger() {
        model.addVariable();               // non‑integer
        assertFalse(model.isAnyVariableInteger());
        model.addVariable().integer();     // integer variable
        assertTrue(model.isAnyVariableInteger());
    }

    @Test
    public void testIsAnyVariableFixed() {
        model.addVariable();               // not fixed
        assertFalse(model.isAnyVariableFixed());
        model.addVariable().value(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed());
    }

    @Test
    public void testGetVariableValuesFeasible() {
        Variable x = model.addVariable()
                          .lower(BigDecimal.ZERO)
                          .upper(BigDecimal.ONE)
                          .value(BigDecimal.ONE);
        Optimisation.Result r = model.getVariableValues();
        assertEquals(State.FEASIBLE, r.getState());
        assertEquals(1.0, r.doubleValue(0), 1e-12);
    }

    @Test
    public void testGetVariableValuesInfeasible() {
        // No bounds nor value → model declares INFEASIBLE
        model.addVariable();
        Optimisation.Result r = model.getVariableValues();
        assertEquals(State.INFEASIBLE, r.getState());
    }

    @Test
    public void testFileFormatFromString() {
        assertEquals(FileFormat.EBM, FileFormat.from("model.ebm"));
        assertEquals(FileFormat.MPS, FileFormat.from("model.mps"));
        assertEquals(FileFormat.MPS, FileFormat.from("model.sif"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileFormatFromInvalidExtension() {
        FileFormat.from("model.txt"); // should throw
    }

    @Test(expected = IllegalStateException.class)
    public void testAddVariableOnShallowCopyThrows() {
        model.addVariable();
        ExpressionsBasedModel shallow = model.copy(true, false); // shallow copy
        shallow.addVariable(); // must fail
    }

    @Test
    public void testRelaxRemovesIntegerFlag() {
        Variable v = model.addVariable().integer();
        assertTrue(v.isInteger());
        model.relax();
        assertFalse(v.isInteger());
    }

    @Test
    public void testValidateEmptyModel() {
        assertTrue(model.validate()); // nothing to invalidate
    }

    @Test
    public void testToStringContainsVariableAndExpression() {
        Variable x = model.addVariable("x");
        Expression e = model.addExpression("e");
        e.set(x.getIndex(), ONE);
        String txt = model.toString();
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("e"));
    }

    /* -------------------------------------------------
     * 2. EDGE‑CASE & EXCEPTION TESTS
     * ------------------------------------------------- */

    @Test
    public void testContradictoryConstraintsMakeModelInfeasible() {
        Variable x = model.addVariable();
        model.addExpression("c1").set(x.getIndex(), ONE).upper(BigDecimal.ZERO);   // x ≤ 0
        model.addExpression("c2").set(x.getIndex(), ONE).lower(BigDecimal.ONE);   // x ≥ 1
        model.simplify(); // runs presolve & pruning
        assertTrue(model.isInfeasible());
    }

    @Test
    public void testRelaxSoftKeepsIntegerFlag() {
        Variable v = model.addVariable().integer();
        assertTrue(v.isInteger());
        model.relax(true); // soft relax
        assertTrue(model.isRelaxed());
        // integer flag is *not* cleared in soft mode
        assertTrue(v.isInteger());
    }

    @Test
    public void testDescribeAllCounters() {
        Variable i = model.addVariable().integer();
        Variable b = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.addExpression("eq").set(i.getIndex(), ONE).level(BigDecimal.valueOf(5));
        model.addExpression("ub").set(b.getIndex(), ONE).upper(BigDecimal.valueOf(8));
        Description d = model.describe();
        assertEquals(2, d.nbVariables);
        assertEquals(1, d.nbIntegerVariables);
        assertEquals(1, d.nbLowerBounds);
        assertEquals(1, d.nbUpperBounds);
        assertEquals(1, d.nbEqualityConstraints);
        assertEquals(1, d.nbUpperConstraints);
    }

    @Test
    public void testIsAnyVariableIntegerWhenRelaxed() {
        model.addVariable().integer();
        assertTrue(model.isAnyVariableInteger());
        model.relax();
        assertFalse(model.isAnyVariableInteger()); // relaxation clears integer flag
    }

    /* -------------------------------------------------
     * 3. COMBINED SCENARIOS (small realistic models)
     * ------------------------------------------------- */

    @Test
    public void testSimpleLinearModelFeasibilityAndObjective() {
        // x + 2*y  → minimise,  0 ≤ x ≤ 5,  0 ≤ y ≤ 3,  x + y ≥ 4
        Variable x = model.addVariable("x")
                           .lower(BigDecimal.ZERO)
                           .upper(BigDecimal.valueOf(5))
                           .weight(ONE);
        Variable y = model.addVariable("y")
                           .lower(BigDecimal.ZERO)
                           .upper(BigDecimal.valueOf(3))
                           .weight(TWO);
        // constraint: x + y ≥ 4
        model.addExpression("c1")
             .set(x.getIndex(), ONE)
             .set(y.getIndex(), ONE)
             .lower(BigDecimal.valueOf(4));

        // Validate model construction
        assertTrue(model.validate());

        // Because we do not invoke the real solver, we only check that the model
        // recognises the presence of a linear objective and constraints.
        assertTrue(model.isAnyExpressionQuadratic() == false);
        assertTrue(model.isAnyConstraintQuadratic() == false);
        assertTrue(model.isAnyObjectiveQuadratic() == false);
        assertTrue(model.isAnyVariableInteger() == false);
        assertFalse(model.isInfeasible());

        // The aggregated objective should contain the correct coefficients
        Expression obj = model.objective();
        assertEquals(ONE, obj.get(x.getIndex()));
        assertEquals(TWO, obj.get(y.getIndex()));
    }

    @Test
    public void testModelWithBinaryVariableAndSOS() {
        // Build three binary variables
        Variable a = model.addVariable("a").binary();
        Variable b = model.addVariable("b").binary();
        Variable c = model.addVariable("c").binary();

        // Create a SOS that allows at most one variable to be ON
        model.addSpecialOrderedSet(java.util.List.of(a, b, c), 1);
        // The internal presolver registration is not directly visible, but the call must not fail.

        // Verify that all three variables are recognised as binary
        assertTrue(a.isBinary() && b.isBinary() && c.isBinary());

        // The model should still be feasible (no contradictory constraints yet)
        assertFalse(model.isInfeasible());
    }

    @Test
    public void testWriteAndReadBackEBMFile() throws Exception {
        // Very small model – write to a temporary file and read it back
        Variable x = model.addVariable("x").weight(ONE);
        File tmp = File.createTempFile("ebmTest", ".ebm");
        tmp.deleteOnExit();

        model.writeTo(tmp);
        ExpressionsBasedModel readBack = ExpressionsBasedModel.parse(tmp);
        assertEquals(1, readBack.countVariables());
        Variable xr = readBack.getVariable(0);
        assertEquals("x", xr.getName());

        // Objective weight must be preserved
        Expression obj = readBack.objective();
        assertEquals(ONE, obj.get(xr.getIndex()));
    }

    /* -------------------------------------------------
     * 4. CLEAN‑UP
     * ------------------------------------------------- */

    @After
    public void afterEach() {
        // Ensure that every test disposes its model – this also checks that dispose()
        // does not throw any exception even when the model is empty.
        if (model != null) {
            model.dispose();
        }
    }
}
