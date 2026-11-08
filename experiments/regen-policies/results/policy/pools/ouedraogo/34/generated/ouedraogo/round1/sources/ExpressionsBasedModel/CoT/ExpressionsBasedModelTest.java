package org.ojalgo.optimisation;
import org.ojalgo.ProgrammingError;


import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests focus on the public API – constructing models, adding variables /
 * expressions, querying indices, description information, copying behaviour,
 * simple objective evaluation and the handling of edge‑cases that are expected
 * to throw {@link ProgrammingError} or {@link IllegalStateException}.
 *
 * The tests deliberately avoid invoking any solver – they only verify the
 * model bookkeeping logic.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    /** -----------------------------------------------------------------------
     *  Basic creation helpers – add a few variables and give them simple
     *  bounds/weights that will be used by many tests.
     * ----------------------------------------------------------------------- */
    private void createSimpleVariables() {
        Variable x = model.addVariable("x");
        x.lower(0).upper(10).weight(BigDecimal.ONE); // weight = 1

        Variable y = model.addVariable("y");
        y.lower(0).upper(5).weight(BigDecimal.valueOf(2)); // weight = 2
    }

    /** -----------------------------------------------------------------------
     *  Test that a newly created model reports zero variables / expressions.
     * ----------------------------------------------------------------------- */
    @Test
    public void testInitialCounts() {
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
    }

    /** -----------------------------------------------------------------------
     *  Adding variables and checking basic getters.
     * ----------------------------------------------------------------------- */
    @Test
    public void testAddVariableAndGetters() {
        Variable v = model.addVariable("v1");
        assertNotNull(v);
        assertEquals("v1", v.getName());

        // index related queries
        int idx = model.indexOf(v);
        assertEquals(0, idx);
        assertEquals(0, model.indexOfFreeVariable(idx));
        assertEquals(0, model.indexOfIntegerVariable(idx));
        assertEquals(0, model.indexOfNegativeVariable(idx));
        assertEquals(0, model.indexOfPositiveVariable(idx));
    }

    /** -----------------------------------------------------------------------
     *  Adding an expression, setting a linear coefficient and a bound.
     * ----------------------------------------------------------------------- */
    @Test
    public void testAddExpressionAndLinearCoefficient() {
        createSimpleVariables();
        Variable x = model.getVariable(0);
        Variable y = model.getVariable(1);

        Expression e = model.addExpression("c1");
        e.set(x.getIndex(), BigDecimal.ONE);
        e.upper(BigDecimal.valueOf(8));

        assertTrue(e.isConstraint());
        assertEquals(BigDecimal.ONE, e.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(8), e.getUpperLimit());
    }

    /** -----------------------------------------------------------------------
     *  Verify that the aggregated objective reflects variable weights.
     * ----------------------------------------------------------------------- */
    @Test
    public void testObjectiveAggregation() {
        createSimpleVariables();

        // set concrete values for the variables
        model.getVariable(0).value(BigDecimal.valueOf(2)); // x = 2
        model.getVariable(1).value(BigDecimal.valueOf(3)); // y = 3

        Expression obj = model.objective();
        // objective = 1*x + 2*y = 2 + 6 = 8
        BigDecimal value = obj.evaluate(model.getVariableValues().getSolution());
        assertEquals(BigDecimal.valueOf(8), value);
    }

    /** -----------------------------------------------------------------------
     *  Test {@code getVariableValues()} – the returned solution must contain the
     *  values that were explicitly set on the variables.
     * ----------------------------------------------------------------------- */
    @Test
    public void testGetVariableValues() {
        createSimpleVariables();

        model.getVariable(0).value(BigDecimal.valueOf(4));
        model.getVariable(1).value(BigDecimal.valueOf(1));

        Optimisation.Result sol = model.getVariableValues();
        assertEquals(2, sol.size());
        assertEquals(BigDecimal.valueOf(4), sol.get(0));
        assertEquals(BigDecimal.valueOf(1), sol.get(1));
    }

    /** -----------------------------------------------------------------------
     *  Verify description counters after a small model is built.
     * ----------------------------------------------------------------------- */
    @Test
    public void testDescriptionCounts() {
        createSimpleVariables();

        // one linear constraint
        Expression e = model.addExpression("c1");
        e.set(model.getVariable(0).getIndex(), BigDecimal.ONE);
        e.upper(BigDecimal.valueOf(5));

        ExpressionsBasedModel.Description desc = model.describe();

        // variables
        assertEquals(2, desc.nbVariables);
        assertEquals(0, desc.nbIntegerVariables);
        assertEquals(2, desc.nbPositiveVariables); // both have lower bound 0
        assertEquals(0, desc.nbNegativeVariables);

        // constraints
        assertEquals(1, desc.nbUpperConstraints);
        assertEquals(0, desc.nbLowerConstraints);
        assertEquals(0, desc.nbEqualityConstraints);
    }

    /** -----------------------------------------------------------------------
     *  The {@code copy(boolean shallow, boolean prune)} method must create a
     *  shallow copy that forbids further variable creation.
     * ----------------------------------------------------------------------- */
    @Test(expected = IllegalStateException.class)
    public void testShallowCopyPreventsAddingVariable() {
        createSimpleVariables();
        ExpressionsBasedModel shallow = model.copy(true, false);
        // the following must fail because the copy is a shallow work‑copy
        shallow.addVariable("z");
    }

    /** -----------------------------------------------------------------------
     *  {@code relax()} without argument makes all variables non‑integer.
     * ----------------------------------------------------------------------- */
    @Test
    public void testRelaxRemovesIntegerProperty() {
        // create integer variables
        Variable v = model.addVariable("i");
        v.integer(true);
        assertTrue(v.isInteger());

        model.relax(); // hard relax

        // after hard relax the variable should no longer be integer
        assertFalse(v.isInteger());
    }

    /** -----------------------------------------------------------------------
     *  Soft relax keeps the integer flag but makes {@code isAnyVariableInteger()}
     *  report false.
     * ----------------------------------------------------------------------- */
    @Test
    public void testSoftRelax() {
        Variable v = model.addVariable("i");
        v.integer(true);
        assertTrue(v.isInteger());

        model.relax(true); // soft relax

        // variable still reports integer, but model reports none
        assertTrue(v.isInteger());
        assertFalse(model.isAnyVariableInteger());
    }

    /** -----------------------------------------------------------------------
     *  Edge case – calling {@code addSpecialOrderedSet} with an illegal SOS type
     *  must throw {@link ProgrammingError}.
     * ----------------------------------------------------------------------- */
    @Test(expected = ProgrammingError.class)
    public void testAddSpecialOrderedSetInvalidType() {
        Variable a = model.addVariable("a").binary();
        Variable b = model.addVariable("b").binary();
        // SOS type must be > 0, here we pass 0 -> expect error
        model.addSpecialOrderedSet(Collections.<Variable>emptyList(), 0, model.addExpression("dummy"));
    }

    /** -----------------------------------------------------------------------
     *  Edge case – calling {@code addSpecialOrderedSet} with a non‑binary variable
     *  must throw {@link ProgrammingError}.
     * ----------------------------------------------------------------------- */
    @Test(expected = ProgrammingError.class)
    public void testAddSpecialOrderedSetNonBinaryVariable() {
        Variable a = model.addVariable("a"); // not binary
        model.addSpecialOrderedSet(Collections.singletonList(a), 1, 2);
    }

    /** -----------------------------------------------------------------------
     *  {@code limitObjective} should create (or reuse) a constraint expression
     *  that mirrors the objective.
     * ----------------------------------------------------------------------- */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        createSimpleVariables(); // gives an objective with weight 1 and 2

        // No explicit constraint exists yet
        assertNull(model.getExpression(ExpressionsBasedModel.OBJ_FUNC_AS_CONSTR_KEY));

        Expression limiting = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limiting);
        // After limiting, the internal map must contain the generated expression
        assertNotNull(model.getExpression(ExpressionsBasedModel.OBJ_FUNC_AS_CONSTR_KEY));
        // The limits must be set as requested
        assertEquals(BigDecimal.ZERO, limiting.getLowerLimit());
        assertEquals(BigDecimal.TEN, limiting.getUpperLimit());
    }

    /** -----------------------------------------------------------------------
     *  Verify that {@code isAnyVariableFixed()} reflects a variable that has a
     *  concrete value set.
     * ----------------------------------------------------------------------- */
    @Test
    public void testIsAnyVariableFixed() {
        Variable v = model.addVariable("v");
        assertFalse(model.isAnyVariableFixed());

        v.value(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed());
    }

    /** -----------------------------------------------------------------------
     *  The {@code setKnownSolution} method must reject a non‑optimal result.
     * ----------------------------------------------------------------------- */
    @Test(expected = ProgrammingError.class)
    public void testSetKnownSolutionRejectsNonOptimal() {
        // Build a trivial model so that a Result can be created
        createSimpleVariables();
        Optimisation.Result notOptimal = Optimisation.Result.of(Optimisation.State.FEASIBLE, 0.0, 0.0, 0.0);
        model.setKnownSolution(notOptimal);
    }

    /** -----------------------------------------------------------------------
     *  Verify that {@code maximise()} and {@code minimise()} set the correct
     *  optimisation sense.
     * ----------------------------------------------------------------------- */
    @Test
    public void testOptimisationSense() {
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());

        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
    }

    /** -----------------------------------------------------------------------
     *  Simple sanity test for {@code variables()} stream – it must contain all
     *  non‑fixed variables.
     * ----------------------------------------------------------------------- */
    @Test
    public void testVariablesStream() {
        createSimpleVariables();
        long count = model.variables().count();
        assertEquals(2L, count);
    }

    /** -----------------------------------------------------------------------
     *  Test {@code bounds()} – it should return variables that are constraints
     *  (i.e. have a bound) and are not fixed.
     * ----------------------------------------------------------------------- */
    @Test
    public void testBoundsStream() {
        createSimpleVariables();
        long boundCount = model.bounds().count();
        assertEquals(2L, boundCount);

        // fix one variable and ensure it disappears from the bounds stream
        model.getVariable(0).value(BigDecimal.ONE);
        assertEquals(1L, model.bounds().count());
    }

    /** -----------------------------------------------------------------------
     *  Verify that {@code toString()} produces a non‑empty string containing the
     *  model name sections.
     * ----------------------------------------------------------------------- */
    @Test
    public void testToStringNotEmpty() {
        createSimpleVariables();
        String txt = model.toString();
        assertNotNull(txt);
        assertTrue(txt.contains("############################################"));
    }

    /** -----------------------------------------------------------------------
     *  Validate that {@code prepare(Function)} returns the object produced by the
     *  supplied factory.
     * ----------------------------------------------------------------------- */
    @Test
    public void testPrepareFactory() {
        Function<ExpressionsBasedModel, ExpressionsBasedModel.DefaultIntermediate> factory =
                ExpressionsBasedModel.DefaultIntermediate::new;

        ExpressionsBasedModel.DefaultIntermediate intermediate = model.prepare(factory);
        assertNotNull(intermediate);
        // the intermediate should reference the same model
        assertEquals(model, intermediate.model);
    }

    /** -----------------------------------------------------------------------
     *  Verify that {@code writeTo(File)} does not throw when given a writable
     *  temporary file. The content itself is not inspected.
     * ----------------------------------------------------------------------- */
    @Test
    public void testWriteToFile() throws Exception {
        createSimpleVariables();
        File tmp = File.createTempFile("ebm_test", ".ebm");
        tmp.deleteOnExit();
        model.writeTo(tmp);
        assertTrue(tmp.length() > 0);
    }
}
