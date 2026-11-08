package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.netio.InMemoryFile;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.keyvalue.EntryPair;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 * The tests cover:
 * <ul>
 *   <li>public API methods (basic functionality)</li>
 *   <li>edge cases and expected exceptions</li>
 *   <li>inter‑method contracts (e.g. counts, indices, description)</li>
 * </ul>
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    /* --------------------------------------------------------------------- *
     *  Static helper methods (add/remove integration, presolver handling) *
     * --------------------------------------------------------------------- */

    private static class DummySolver implements Optimisation.Solver {}

    private static class DummyIntegration extends ExpressionsBasedModel.Integration<DummySolver> {
        DummyIntegration() {
            super();
        }
        @Override
        public boolean isCapable(Optimisation.Model model) {
            return true;
        }
    }

    @Test
    public void testAddAndRemoveIntegration() {
        DummyIntegration dummy = new DummyIntegration();
        assertTrue(ExpressionsBasedModel.addIntegration(dummy));
        assertTrue(ExpressionsBasedModel.removeIntegration(dummy));
    }

    @Test
    public void testAddAndRemovePresolver() {
        // Use one of the built‑in presolvers for the test
        ExpressionsBasedModel.Presolver presolver = ExpressionsBasedModel.PRESOLVERS.iterator().next();
        assertTrue(ExpressionsBasedModel.addPresolver(presolver));
        assertTrue(ExpressionsBasedModel.removePresolver(presolver));
    }

    @Test
    public void testClearIntegrationsAndPresolvers() {
        DummyIntegration dummy = new DummyIntegration();
        ExpressionsBasedModel.addIntegration(dummy);
        ExpressionsBasedModel.clearIntegrations();
        assertFalse(ExpressionsBasedModel.removeIntegration(dummy));

        ExpressionsBasedModel.addPresolver(ExpressionsBasedModel.PRESOLVERS.iterator().next());
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        // reset to default for other tests
        ExpressionsBasedModel.resetPresolvers();
    }

    /* --------------------------- *
     *  Variable / Expression API *
     * --------------------------- */

    @Test
    public void testAddVariableAndExpressionCounts() {
        Variable v0 = model.addVariable("x0");
        Variable v1 = model.addVariable("x1");
        assertEquals(2, model.countVariables());

        Expression e0 = model.addExpression("c0");
        Expression e1 = model.addExpression();
        assertEquals(2, model.countExpressions());

        // verify that variables are retrievable by index and name
        assertSame(v0, model.getVariable(0));
        assertSame(v1, model.getVariable(1));
        assertSame(e0, model.getExpression("c0"));
        assertNotNull(e1);
    }

    @Test
    public void testIndexMethods() {
        Variable v0 = model.addVariable("x0");
        Variable v1 = model.addVariable("x1");
        v0.lower(0).upper(10); // make it a free variable
        v1.lower(-5).upper(5);

        // free variables indices
        assertEquals(0, model.indexOfFreeVariable(v0));
        assertEquals(1, model.indexOfFreeVariable(v1));

        // integer variable – none yet
        assertEquals(-1, model.indexOfIntegerVariable(v0));

        // make v1 integer and test integer index
        v1.integer(true);
        assertTrue(model.isAnyVariableInteger());
        int intIdx = model.indexOfIntegerVariable(v1);
        assertTrue(intIdx >= 0);
    }

    @Test
    public void testDescriptionCounts() {
        Variable x = model.addVariable("x");
        x.lower(0).upper(5);
        Variable y = model.addVariable("y");
        y.upper(10); // only upper bound

        Expression expr = model.addExpression("c");
        expr.set(x, 1).set(y, 2).upper(BigDecimal.valueOf(20));

        ExpressionsBasedModel.Description desc = model.describe();

        // 2 variables, both have upper bound, one has lower bound
        assertEquals(2, desc.nbVariables);
        assertEquals(1, desc.nbLowerBounds);
        assertEquals(2, desc.nbUpperBounds);
        assertEquals(0, desc.nbEqualityBounds);
        // one constraint (upper)
        assertEquals(1, desc.nbUpperConstraints);
        assertEquals(0, desc.nbLowerConstraints);
        assertEquals(0, desc.nbEqualityConstraints);
    }

    @Test
    public void testLimitObjectiveCreatesConstraint() {
        Variable x = model.addVariable("x");
        x.weight(BigDecimal.ONE);
        // default objective is just x
        Expression obj = model.objective();
        assertTrue(obj.isObjective());

        // limit the objective between 0 and 10
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limit);
        assertTrue(limit.isConstraint());
        assertEquals(BigDecimal.ZERO, limit.getLowerLimit());
        assertEquals(BigDecimal.TEN, limit.getUpperLimit());
    }

    @Test
    public void testAddSpecialOrderedSetInvalidParameters() {
        Variable a = model.addVariable("a");
        a.binary();

        // invalid type (<=0)
        try {
            model.addSpecialOrderedSet(Collections.singletonList(a), 0, model.addExpression("dummy"));
            fail("Expected ProgrammingError for invalid SOS type");
        } catch (RuntimeException expected) {
            // ok
        }

        // invalid min/max (max <=0 or min>max)
        try {
            model.addSpecialOrderedSet(Collections.singletonList(a), 1, 0);
            fail("Expected ProgrammingError for invalid min/max");
        } catch (RuntimeException expected) {
            // ok
        }
    }

    @Test
    public void testCopyAndSimplify() {
        Variable x = model.addVariable("x");
        x.lower(0).upper(5);
        model.addExpression("c").set(x, 1).upper(BigDecimal.valueOf(5));

        ExpressionsBasedModel copy = model.copy();
        assertNotSame(model, copy);
        assertEquals(model.countVariables(), copy.countVariables());

        // simplify produces a shallow copy flagged as such (internal flag not visible)
        ExpressionsBasedModel simplified = model.simplify();
        assertNotSame(model, simplified);
        assertEquals(model.countVariables(), simplified.countVariables());

        // modifications in original must not affect simplified (deep copy of variables)
        x.upper(BigDecimal.valueOf(10));
        Variable xCopy = simplified.getVariable(0);
        assertEquals(BigDecimal.valueOf(5), xCopy.getUpperLimit()); // unchanged
    }

    @Test
    public void testRelaxAndIntegerVariableFlags() {
        Variable i = model.addVariable("i");
        i.integer(true);
        assertTrue(i.isInteger());

        model.relax(); // hard relax – removes integer flag
        assertFalse(i.isInteger());

        Variable j = model.addVariable("j");
        j.integer(true);
        model.relax(true); // soft relax – keeps integer flag but model is marked relaxed
        assertTrue(j.isInteger());
        assertTrue(model.isRelaxed());
    }

    @Test
    public void testSetKnownSolutionInvalidThrows() {
        // create a non‑optimal result
        Optimisation.Result nonOptimal = Optimisation.Result.of(Optimisation.State.FEASIBLE, 0.0, new double[]{0.0});
        try {
            model.setKnownSolution(nonOptimal);
            fail("Expected ProgrammingError because solution is not optimal");
        } catch (RuntimeException expected) {
            // ok
        }
    }

    @Test
    public void testValidateSolutionAndInfeasibility() {
        Variable x = model.addVariable("x");
        // No limits, no value – getVariableValues should be INFEASIBLE
        Optimisation.Result result = model.getVariableValues();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState());

        // give x a feasible value and bounds
        x.lower(0).upper(1);
        x.value(BigDecimal.valueOf(0.5));
        Optimisation.Result feasible = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, feasible.getState());

        // validate with correct solution
        assertTrue(model.validate(feasible));

        // tamper solution to violate bound
        Access1D<BigDecimal> bad = Access1D.wrap(new BigDecimal[]{BigDecimal.valueOf(2)});
        assertFalse(model.validate(bad));
    }

    @Test
    public void testFileFormatFromInvalidPath() {
        try {
            ExpressionsBasedModel.FileFormat.from("unknown.xyz");
            fail("Expected IllegalArgumentException for unsupported extension");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void testToStringContainsVariablesAndExpressions() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        model.addExpression("expr").set(a, 1).set(b, 2).upper(BigDecimal.TEN);
        String txt = model.toString();
        assertTrue(txt.contains("a"));
        assertTrue(txt.contains("b"));
        assertTrue(txt.contains("expr"));
    }

    @Test
    public void testWriteToInMemoryFile() {
        InMemoryFile mem = new InMemoryFile("mem.ebm");
        model.addVariable("x");
        model.writeTo(mem);
        assertTrue(mem.size() > 0);
    }

    /* ------------------------------------------------ *
     *  Additional simple getters / boolean checks      *
     * ------------------------------------------------ */

    @Test
    public void testBooleanQueries() {
        assertFalse(model.isAnyVariableFixed());
        assertFalse(model.isAnyVariableInteger());
        assertFalse(model.isAnyExpressionQuadratic());
        assertFalse(model.isAnyConstraintQuadratic());
        assertFalse(model.isAnyObjectiveQuadratic());
        assertFalse(model.isInfeasible());
        assertFalse(model.isUnbounded());
    }
}
