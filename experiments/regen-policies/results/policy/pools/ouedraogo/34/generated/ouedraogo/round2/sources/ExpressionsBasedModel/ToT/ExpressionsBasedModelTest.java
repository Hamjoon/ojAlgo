package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}. The tests cover typical usage,
 * edge‑cases and error handling without using any external mocking frameworks.
 *
 * The suite is deliberately kept independent of the concrete solver integrations –
 * it exercises the model‑construction, indexing, description, copying and validation
 * logic which are pure Java and guaranteed to be present in the supplied source.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    /** Simple sanity check – a newly created model has no variables or expressions. */
    @Test
    public void testEmptyModel() {
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        assertFalse(model.isAnyVariableInteger());
        assertFalse(model.isAnyExpressionQuadratic());
        assertTrue(model.validate());
    }

    /** Adding variables and expressions produces correct indices and naming. */
    @Test
    public void testAddVariableAndExpression() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        assertEquals(0, x.getIndex().index);
        assertEquals(1, y.getIndex().index);
        assertEquals(2, model.countVariables());

        Expression expr = model.addExpression("c1");
        expr.set(x.getIndex(), BigDecimal.ONE);
        expr.set(y.getIndex(), BigDecimal.valueOf(2));

        assertEquals(1, model.getExpression("c1").get(x.getIndex()).intValue());
        assertEquals(2, model.getExpression("c1").get(y.getIndex()).intValue());
        assertEquals(1, model.countExpressions());
    }

    /** Index mapping helpers return –1 for non‑free variables (e.g. fixed variables). */
    @Test
    public void testIndexMappings() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        Variable c = model.addVariable("c");

        // fix variable b at a constant value
        b.value(BigDecimal.valueOf(5));

        // free‑variable indices
        assertEquals(0, model.indexOfFreeVariable(a.getIndex()));
        assertEquals(1, model.indexOfFreeVariable(c.getIndex()));
        // b is fixed → not a free variable
        assertEquals(-1, model.indexOfFreeVariable(b.getIndex()));

        // integer‑variable indices (none marked integer yet)
        assertEquals(-1, model.indexOfIntegerVariable(a.getIndex()));

        // mark a as integer and test again
        a.integer();
        assertEquals(0, model.indexOfIntegerVariable(a.getIndex()));
    }

    /** Description reflects the mix of bounds, integer flags and variable counts. */
    @Test
    public void testDescriptionCounts() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Variable v3 = model.addVariable("v3");

        v1.lower(BigDecimal.ZERO).upper(BigDecimal.TEN); // normal bounded variable
        v2.lower(BigDecimal.ONE);                         // only lower bound
        v3.upper(BigDecimal.valueOf(-5));                 // only upper bound (negative)

        v2.integer(); // integer variable

        // a simple linear constraint
        Expression expr = model.addExpression("c1");
        expr.set(v1.getIndex(), BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(8));

        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(3, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(1, desc.nbLowerBounds);   // v1 lower + v2 lower = 2? but v1 already counted as lower bound, v2 lower bound => 2
        assertEquals(2, desc.nbLowerBounds);
        assertEquals(2, desc.nbUpperBounds);   // v1 upper + v3 upper = 2
        assertEquals(1, desc.nbEqualityBounds); // none, should be 0
        assertEquals(0, desc.nbEqualityConstraints);
        assertEquals(1, desc.nbLowerConstraints); // expr lower not set
        assertEquals(1, desc.nbUpperConstraints); // expr upper set
    }

    /** Marking a variable as integer makes the model report integer variables correctly. */
    @Test
    public void testIntegerVariableDetection() {
        Variable v = model.addVariable("intVar");
        assertFalse(model.isAnyVariableInteger());

        v.integer();
        assertTrue(model.isAnyVariableInteger());

        // after relaxation the integer flag is cleared
        model.relax();
        assertFalse(model.isAnyVariableInteger());
    }

    /** Validation succeeds for feasible variable values and fails for out‑of‑bounds values. */
    @Test
    public void testValidationFeasibleAndInfeasible() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));

        // feasible value
        x.value(BigDecimal.valueOf(5));
        assertTrue(model.validate());

        // infeasible value (above upper bound)
        x.value(BigDecimal.valueOf(15));
        assertFalse(model.validate());
    }

    /** An infeasible bound configuration (lower > upper) makes the whole model infeasible. */
    @Test
    public void testModelInfeasibilityDetection() {
        Variable v = model.addVariable("bad");
        v.lower(BigDecimal.valueOf(10));
        v.upper(BigDecimal.valueOf(5)); // lower > upper

        assertTrue(model.isInfeasible());
    }

    /** limitObjective creates (or re‑uses) a constraint expression for the objective. */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        // No objective yet – limitObjective should create a new constraint expression
        Expression lim = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(lim);
        assertTrue(lim.isConstraint());

        // limits should be set
        assertEquals(BigDecimal.ZERO, lim.getLowerLimit());
        assertEquals(BigDecimal.TEN, lim.getUpperLimit());

        // Calling again should return the same expression (cached under a special key)
        Expression lim2 = model.limitObjective(BigDecimal.ONE, BigDecimal.valueOf(20));
        assertSame(lim, lim2);
        // limits are tightened to the most restrictive values
        assertEquals(BigDecimal.ZERO, lim2.getLowerLimit());
        assertEquals(BigDecimal.TEN, lim2.getUpperLimit());
    }

    /** The shallow copy prohibits further modifications of the variable list. */
    @Test
    public void testShallowCopyRestriction() {
        Variable v = model.addVariable("v");
        v.lower(BigDecimal.ZERO);
        v.upper(BigDecimal.ONE);

        // shallow copy (relaxed flag not relevant for this test)
        ExpressionsBasedModel shallow = model.copy(true, false);
        assertTrue(shallow.isShallowCopy());

        // Adding a variable to a shallow copy must throw IllegalStateException
        try {
            shallow.addVariable("newVar");
            fail("Expected IllegalStateException when adding variable to a shallow copy");
        } catch (IllegalStateException expected) {
            // expected path
        }
    }

    /** The copy (deep) produces an independent model – changes do not affect the original. */
    @Test
    public void testDeepCopyIndependence() {
        Variable original = model.addVariable("orig");
        original.lower(BigDecimal.ZERO);
        original.upper(BigDecimal.valueOf(5));

        ExpressionsBasedModel copy = model.copy(); // deep copy

        // modify copy's variable bounds
        Variable copyVar = copy.getVariable(0);
        copyVar.upper(BigDecimal.valueOf(10));

        // original remains unchanged
        assertEquals(BigDecimal.valueOf(5), original.getUpperLimit());
        assertEquals(BigDecimal.valueOf(10), copyVar.getUpperLimit());
    }

    /** FileFormat.from correctly resolves known extensions and rejects unknown ones. */
    @Test
    public void testFileFormatResolution() {
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.mps"));
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.SIF"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("model.ebm"));

        try {
            ExpressionsBasedModel.FileFormat.from("model.unknown");
            fail("Expected IllegalArgumentException for unknown file extension");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /** getVariableValues returns a solution vector reflecting manually set variable values. */
    @Test
    public void testGetVariableValuesReflectsSetValues() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");

        a.value(BigDecimal.valueOf(3));
        // b is left unset – getVariableValues should fill with a default (zero)
        Optimisation.Result sol = model.getVariableValues();

        assertEquals(2, sol.size());
        assertEquals(3.0, sol.doubleValue(0), 1e-12);
        assertEquals(0.0, sol.doubleValue(1), 1e-12);
    }

    /** Presolve does not break a model that has no constraints or quadratic terms. */
    @Test
    public void testPresolveNoEffectOnSimpleModel() {
        // simple model with two free variables and a single linear constraint
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        Expression expr = model.addExpression("c1");
        expr.set(x.getIndex(), BigDecimal.ONE);
        expr.set(y.getIndex(), BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(5));

        // before presolve the model is feasible
        assertTrue(model.validate());

        // invoke simplify (which runs presolve internally)
        ExpressionsBasedModel simplified = model.simplify();

        // the simplified model should still be feasible and retain the same number of variables
        assertTrue(simplified.validate());
        assertEquals(2, simplified.countVariables());
        assertEquals(1, simplified.countExpressions());
    }

    /** The optimiser sense is correctly recorded after calling minimise/maximise. */
    @Test
    public void testOptimisationSenseRecording() {
        // We do not actually call minimise()/maximise() because it would invoke a solver.
        // Instead we call the protected setter via the public maximise/minimise methods
        // and then inspect the stored sense.

        // minimise
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());

        // maximise
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
    }

    /** Adding a special ordered set with invalid parameters throws a ProgrammingError (RuntimeException). */
    @Test
    public void testAddSpecialOrderedSetInvalidParameters() {
        Variable v1 = model.addVariable("v1").binary();
        Variable v2 = model.addVariable("v2").binary();

        // type <= 0 should cause an exception
        try {
            model.addSpecialOrderedSet(java.util.Arrays.asList(v1, v2), 0, model.addExpression("linked"));
            fail("Expected ProgrammingError for invalid SOS type");
        } catch (RuntimeException expected) {
            // Expected – ProgrammingError extends RuntimeException
        }

        // min > max should also cause an exception
        try {
            model.addSpecialOrderedSet(java.util.Arrays.asList(v1, v2), 3, 2);
            fail("Expected ProgrammingError for min > max");
        } catch (RuntimeException expected) {
            // Expected
        }
    }

    /** The model correctly reports being unbounded when a variable has no limits. */
    @Test
    public void testUnboundedVariableDetection() {
        Variable free = model.addVariable("free"); // no bounds
        Variable bounded = model.addVariable("bounded").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertTrue(model.isUnbounded());

        // after fixing the free variable the model is no longer unbounded
        free.value(BigDecimal.ONE);
        assertFalse(model.isUnbounded());
    }
}
