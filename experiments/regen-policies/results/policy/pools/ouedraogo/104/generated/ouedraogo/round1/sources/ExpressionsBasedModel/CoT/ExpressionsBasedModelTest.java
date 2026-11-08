package org.ojalgo.optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.ProgrammingError;


import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods (basic happy‑path usage)</li>
 *   <li>Edge‑case and exception scenarios identified from the source code</li>
 *   <li>Behaviour of helper classes such as {@link ExpressionsBasedModel.FileFormat}</li>
 * </ul>
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

    /* ----------------------------------------------------------------------
     *  Constructors and basic factory methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertEquals(0, empty.countVariables());
        assertEquals(0, empty.countExpressions());
    }

    @Test
    public void testConstructorWithOptionsUsesProvidedOptions() {
        Optimisation.Options opt = new Optimisation.Options();
        opt.logger_detailed = true;
        ExpressionsBasedModel withOpt = new ExpressionsBasedModel(opt);
        assertTrue(withOpt.options.logger_detailed);
    }

    @Test
    public void testAddVariableAndIndexing() {
        Variable z = model.addVariable("z");
        assertEquals(2, model.indexOf(z)); // x=0, y=1, z=2
        assertEquals(z, model.getVariable(2));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddVariableAfterShallowCopyThrows() {
        ExpressionsBasedModel shallow = model.copy(true, false);
        shallow.addVariable("shouldFail");
    }

    @Test
    public void testAddExpressionAndRetrieval() {
        Expression e = model.addExpression("e1");
        e.set(x.getIndex(), BigDecimal.ONE);
        assertSame(e, model.getExpression("e1"));
        assertEquals(1, model.countExpressions());
    }

    @Test
    public void testAddSpecialOrderedSetInvalidTypeThrowsProgrammingError() {
        try {
            model.addSpecialOrderedSet(List.of(x), 0, model.addExpression("dummy"));
            fail("Expected ProgrammingError due to invalid SOS type");
        } catch (ProgrammingError expected) {
            // ok
        }
    }

    @Test
    public void testAddSpecialOrderedSetInvalidBinaryVariablesThrowsProgrammingError() {
        // y is not binary yet
        try {
            model.addSpecialOrderedSet(List.of(y), 0, 1);
            fail("Expected ProgrammingError because variable is not binary");
        } catch (ProgrammingError expected) {
            // ok
        }
    }

    @Test
    public void testAddSpecialOrderedSetValidBinarySet() {
        Variable b1 = model.addVariable("b1").binary();
        Variable b2 = model.addVariable("b2").binary();
        model.addSpecialOrderedSet(List.of(b1, b2), 0, 1);
        // No exception means the SOS was added successfully
        assertTrue(true);
    }

    /* ----------------------------------------------------------------------
     *  Description and counting helpers
     * ---------------------------------------------------------------------- */

    @Test
    public void testCountVariablesAndExpressions() {
        assertEquals(2, model.countVariables());
        model.addExpression("e");
        assertEquals(1, model.countExpressions());
    }

    @Test
    public void testDescribeCounts() {
        // make x a lower bound, y an upper bound, and add one constraint expression
        x.lower(BigDecimal.ZERO);
        y.upper(BigDecimal.ONE);
        model.addExpression("c").set(x.getIndex(), BigDecimal.ONE).upper(BigDecimal.TEN);
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(2, d.nbVariables);
        assertEquals(1, d.nbLowerBounds);
        assertEquals(1, d.nbUpperBounds);
        assertEquals(1, d.nbUpperConstraints);
    }

    /* ----------------------------------------------------------------------
     *  Index conversion helpers
     * ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfFreeVariable() {
        // both variables are free (not fixed)
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));
    }

    @Test
    public void testIndexOfIntegerVariableWhenNone() {
        assertEquals(-1, model.indexOfIntegerVariable(x));
        // make x integer and test again
        x.integer();
        assertEquals(0, model.indexOfIntegerVariable(x));
    }

    @Test
    public void testIndexOfPositiveNegativeVariables() {
        x.lower(BigDecimal.valueOf(-5));
        x.upper(BigDecimal.valueOf(5));
        assertEquals(0, model.indexOfPositiveVariable(x));
        assertEquals(0, model.indexOfNegativeVariable(x));

        y.lower(BigDecimal.ZERO);
        y.upper(BigDecimal.valueOf(10));
        assertEquals(1, model.indexOfPositiveVariable(y));
        assertEquals(-1, model.indexOfNegativeVariable(y));
    }

    /* ----------------------------------------------------------------------
     *  Objective handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testObjectiveAggregation() {
        // set contribution weights on variables
        x.weight(BigDecimal.valueOf(2));
        y.weight(BigDecimal.valueOf(3));
        Expression obj = model.objective();
        assertEquals(BigDecimal.valueOf(2), obj.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(3), obj.get(y.getIndex()));
    }

    @Test
    public void testLimitObjectiveCreatesConstraintWhenNoQuadratic() {
        // No quadratic expression, limitObjective should create a constraint copy
        Expression obj = model.objective();
        obj.setConstant(BigDecimal.ONE);
        Expression limited = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limited);
        // limited expression should have same constant as objective
        assertEquals(BigDecimal.ONE, limited.getConstant());
        // limits should be set
        assertEquals(BigDecimal.ZERO, limited.getLowerLimit());
        assertEquals(BigDecimal.TEN, limited.getUpperLimit());
    }

    /* ----------------------------------------------------------------------
     *  Solver related methods (basic sanity – they should not throw)
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinimiseAndMaximiseReturnResult() {
        // trivial model – no constraints, objective is zero
        Optimisation.Result min = model.minimise();
        Optimisation.Result max = model.maximise();
        assertNotNull(min);
        assertNotNull(max);
        // both should be feasible with zero value
        assertEquals(Optimisation.State.FEASIBLE, min.getState());
        assertEquals(Optimisation.State.FEASIBLE, max.getState());
        assertEquals(0.0, min.getValue(), 1e-9);
        assertEquals(0.0, max.getValue(), 1e-9);
    }

    @Test
    public void testPrepareWithIdentityFactory() {
        // use a simple factory that returns a DefaultIntermediate (package‑private)
        // we can use a lambda that creates a new DefaultIntermediate via reflection
        // but easier: use the public prepare method with a factory that returns a dummy
        // implementation of IntermediateSolver (cannot instantiate directly). Instead,
        // test that prepare does not throw and returns the same type as factory.
        Function<ExpressionsBasedModel, ExpressionsBasedModel.DefaultIntermediate> factory =
                ExpressionsBasedModel.DefaultIntermediate::new;
        ExpressionsBasedModel.DefaultIntermediate intermediate = model.prepare(factory);
        assertNotNull(intermediate);
    }

    /* ----------------------------------------------------------------------
     *  Simplify / presolve / reduce
     * ---------------------------------------------------------------------- */

    @Test
    public void testSimplifyCreatesShallowCopy() {
        model.addExpression("c").set(x.getIndex(), BigDecimal.ONE).upper(BigDecimal.TEN);
        ExpressionsBasedModel simplified = model.simplify();
        // simplified should be a different instance but contain the same expression name
        assertNotSame(model, simplified);
        assertEquals(1, simplified.countExpressions());
        assertNotNull(simplified.getExpression("c"));
    }

    @Test
    public void testReduceDoesNotThrow() {
        model.addExpression("c").set(x.getIndex(), BigDecimal.ONE).upper(BigDecimal.TEN);
        model.reduce(); // no exception expected
        assertEquals(1, model.countExpressions());
    }

    /* ----------------------------------------------------------------------
     *  Relaxation handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testRelaxRemovesIntegerFlag() {
        Variable intVar = model.addVariable("intVar").integer();
        assertTrue(intVar.isInteger());
        model.relax();
        // after hard relaxation the variable should no longer be integer
        assertFalse(intVar.isInteger());
    }

    @Test
    public void testRelaxSoftKeepsIntegerFlagButMarksModelRelaxed() {
        Variable intVar = model.addVariable("intVar").integer();
        model.relax(true);
        assertTrue(intVar.isInteger());
        assertTrue(model.isRelaxed());
    }

    @Test
    public void testSnapshotCreatesRelaxedShallowCopy() {
        model.addVariable("intVar").integer();
        ExpressionsBasedModel snapshot = model.snapshot();
        assertTrue(snapshot.isRelaxed());
        // snapshot is shallow – modifications to original should reflect in snapshot's variable list size
        assertEquals(model.countVariables(), snapshot.countVariables());
    }

    /* ----------------------------------------------------------------------
     *  Validation helpers
     * ---------------------------------------------------------------------- */

    @Test
    public void testValidateWithCorrectSolution() {
        x.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        y.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // set a feasible solution manually
        x.setValue(BigDecimal.ONE);
        y.setValue(BigDecimal.valueOf(2));
        assertTrue(model.validate());
    }

    @Test
    public void testValidateFailsWhenVariableOutOfBounds() {
        x.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        x.setValue(BigDecimal.TEN); // out of upper bound
        assertFalse(model.validate());
    }

    @Test
    public void testValidateWithCustomSolutionArray() {
        x.lower(BigDecimal.ZERO).upper(BigDecimal.FIVE);
        // provide a solution that satisfies bounds
        Access1D<BigDecimal> sol = Access1D.wrap(new BigDecimal[] { BigDecimal.ONE, BigDecimal.ZERO });
        assertTrue(model.validate(sol));
    }

    @Test
    public void testValidateFailsWithMismatchedSize() {
        Access1D<BigDecimal> sol = Access1D.wrap(new BigDecimal[] { BigDecimal.ONE });
        assertFalse(model.validate(sol));
    }

    /* ----------------------------------------------------------------------
     *  Fixed / integer variable queries
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsAnyVariableFixed() {
        assertFalse(model.isAnyVariableFixed());
        x.value(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed());
    }

    @Test
    public void testIsAnyVariableInteger() {
        assertFalse(model.isAnyVariableInteger());
        Variable intVar = model.addVariable("intVar").integer();
        assertTrue(model.isAnyVariableInteger());
        // after relaxing the model, integer flag is ignored
        model.relax();
        assertFalse(model.isAnyVariableInteger());
    }

    /* ----------------------------------------------------------------------
     *  FileFormat utility tests (static)
     * ---------------------------------------------------------------------- */

    @Test
    public void testFileFormatFromString() {
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.mps"));
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.SIF"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("model.ebm"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileFormatFromInvalidPathThrows() {
        ExpressionsBasedModel.FileFormat.from("model.unknown");
    }

    /* ----------------------------------------------------------------------
     *  Miscellaneous getters / helpers
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetVariableValuesWhenAllVariablesHaveValues() {
        x.value(BigDecimal.valueOf(3));
        y.value(BigDecimal.valueOf(4));
        Optimisation.Result vals = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, vals.getState());
        assertEquals(3.0, vals.doubleValue(0), 1e-9);
        assertEquals(4.0, vals.doubleValue(1), 1e-9);
    }

    @Test
    public void testGetVariableValuesWhenMissingInfoLeadsToInfeasible() {
        // leave variables without any bounds or values
        Optimisation.Result vals = model.getVariableValues();
        assertEquals(Optimisation.State.INFEASIBLE, vals.getState());
    }

    @Test
    public void testToStringContainsVariableAndExpressionNames() {
        model.addExpression("expr1").set(x.getIndex(), BigDecimal.ONE);
        String txt = model.toString();
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("expr1"));
    }

    @Test
    public void testCopyPreservesVariableValues() {
        x.value(BigDecimal.valueOf(5));
        ExpressionsBasedModel copy = model.copy();
        assertEquals(BigDecimal.valueOf(5), copy.getVariable(0).getValue());
    }

    @Test
    public void testSetKnownSolutionValidatesAndStores() {
        // create a trivial feasible solution
        Optimisation.Result sol = Optimisation.Result.of(Optimisation.State.OPTIMAL, 0.0, 0.0, 0.0);
        model.setKnownSolution(sol);
        assertSame(sol, model.getKnownSolution());
    }

    @Test(expected = ProgrammingError.class)
    public void testSetKnownSolutionWithNonOptimalThrows() {
        Optimisation.Result bad = Optimisation.Result.of(Optimisation.State.FEASIBLE, 0.0, 0.0);
        model.setKnownSolution(bad);
    }

    @Test(expected = ProgrammingError.class)
    public void testSetKnownSolutionWithInvalidSolutionThrows() {
        // create a solution that violates a bound
        x.lower(BigDecimal.ONE);
        Optimisation.Result bad = Optimisation.Result.of(Optimisation.State.OPTIMAL, 0.0, 0.0);
        model.setKnownSolution(bad);
    }

    /* ----------------------------------------------------------------------
     *  Integration / presolver static management
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveIntegration() {
        ExpressionsBasedModel.Integration<?> dummy = new ExpressionsBasedModel.Integration<Optimisation.Solver>() {
            @Override public boolean isCapable(Optimisation.Model model) { return false; }
        };
        assertTrue(ExpressionsBasedModel.addIntegration(dummy));
        assertTrue(ExpressionsBasedModel.removeIntegration(dummy));
    }

    @Test
    public void testAddAndRemovePresolver() {
        ExpressionsBasedModel.Presolver dummy = new ExpressionsBasedModel.Presolver(0) {
            @Override public boolean simplify(Expression expression, Set<IntIndex> remaining,
                                              BigDecimal lower, BigDecimal upper, NumberContext precision) {
                return false;
            }
        };
        assertTrue(ExpressionsBasedModel.addPresolver(dummy));
        assertTrue(ExpressionsBasedModel.removePresolver(dummy));
    }

    @Test
    public void testClearIntegrationsAndPresolvers() {
        ExpressionsBasedModel.clearIntegrations();
        ExpressionsBasedModel.clearPresolvers();
        // after clearing, the default presolvers are re‑added in static init,
        // we just verify that the calls do not throw.
        assertTrue(true);
    }

}
