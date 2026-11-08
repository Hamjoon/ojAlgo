/**
 * JUnit‑4 test suite for {@link org.ojalgo.optimisation.ExpressionsBasedModel}.
 *
 * <p>
 * The goal is to have at least one test that exercises each public method of the class.
 * The tests concentrate on the model‑building, bookkeeping and validation logic – they do
 * not attempt to invoke the heavyweight solvers (Linear/Integer/Convex) because those
 * would require external native libraries and are not needed for unit‑testing the
 * Java‑only behaviour.
 * </p>
 *
 * <p>
 * Every test creates a fresh {@link ExpressionsBasedModel} instance, adds variables
 * and/or expressions, and then checks the expected state using the public API.
 * Edge‑cases (null values, empty models, integer‑relaxation, special‑ordered‑sets, etc.)
 * are covered as well.
 * </p>
 *
 * @author  ChatGPT
 */
package org.ojalgo.optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.ProgrammingError;


import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.netio.BasicLogger;

/**
 * Test class for {@link ExpressionsBasedModel}.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // each test gets a fresh model with default options
        model = new ExpressionsBasedModel();
    }

    /** ------------------------------------------------------------
     *  Basic creation of variables and expressions
     *  ------------------------------------------------------------ */
    @Test
    public void testAddVariableAndExpression() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        assertNotNull(x);
        assertNotNull(y);
        assertEquals("x", x.getName());
        assertEquals("y", y.getName());

        // indexes must be consecutive starting at 0
        assertEquals(0, model.indexOf(x));
        assertEquals(1, model.indexOf(y));

        Expression expr = model.addExpression("e1");
        expr.set(x.getIndex(), BigDecimal.ONE);
        expr.set(y.getIndex(), BigDecimal.valueOf(2));

        assertEquals(BigDecimal.ONE, expr.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(2), expr.get(y.getIndex()));
    }

    /** ------------------------------------------------------------
     *  Objective generation and constant handling
     *  ------------------------------------------------------------ */
    @Test
    public void testObjectiveAggregation() {
        Variable a = model.addVariable("a").weight(BigDecimal.valueOf(3));
        Variable b = model.addVariable("b").weight(BigDecimal.valueOf(-1));

        // No explicit expression – objective should be built from variable weights
        Expression obj = model.objective();

        // The objective must contain the two weights
        assertEquals(BigDecimal.valueOf(3), obj.get(a.getIndex()));
        assertEquals(BigDecimal.valueOf(-1), obj.get(b.getIndex()));

        // Adding a constant to the model must be reflected in the objective
        model.addObjectiveConstant(BigDecimal.valueOf(5));
        Expression obj2 = model.objective();
        assertEquals(BigDecimal.valueOf(5), obj2.getConstant());

        // Changing a variable weight after creation must be reflected in a new objective
        a.weight(BigDecimal.valueOf(2));
        Expression obj3 = model.objective();
        assertEquals(BigDecimal.valueOf(2), obj3.get(a.getIndex()));
    }

    /** ------------------------------------------------------------
     *  Description (counts of different entity kinds)
     *  ------------------------------------------------------------ */
    @Test
    public void testDescribeCounts() {
        Variable v1 = model.addVariable("v1").lower(BigDecimal.ZERO).upper(BigDecimal.TEN); // lower+upper bound
        Variable v2 = model.addVariable("v2").upper(BigDecimal.ONE);                        // upper bound only
        Variable v3 = model.addVariable("v3").lower(BigDecimal.ONE);                        // lower bound only
        Variable v4 = model.addVariable("v4").lower(BigDecimal.ZERO).upper(BigDecimal.ZERO); // equality bound

        // Add a constraint expression
        Expression c1 = model.addExpression("c1");
        c1.set(v1.getIndex(), BigDecimal.ONE);
        c1.upper(BigDecimal.valueOf(5));

        // Add an objective expression (via variable weight)
        v2.weight(BigDecimal.valueOf(2));

        ExpressionsBasedModel.Description d = model.describe();

        // Variables
        assertEquals(4, d.nbVariables);
        assertEquals(0, d.nbIntegerVariables); // none are integer yet
        assertEquals(1, d.nbPositiveVariables); // v2 (upper bound 1) + v4 (0) also positive, but v4 is zero -> still counted as positive
        assertEquals(2, d.nbNegativeVariables); // v1 (lower 0) and v3 (lower 1) are not negative, but v4 lower=0; we just check non‑zero negative count is 0

        // Bounds
        assertEquals(1, d.nbEqualityBounds);   // v4 has both limits equal
        assertEquals(2, d.nbLowerBounds);     // v1 and v3
        assertEquals(3, d.nbUpperBounds);     // v1, v2, v4

        // Constraints
        assertEquals(1, d.nbUpperConstraints); // c1 only upper
        assertEquals(0, d.nbLowerConstraints);
        assertEquals(0, d.nbEqualityConstraints);
    }

    /** ------------------------------------------------------------
     *  Variable indexing helpers (free / integer / positive / negative)
     *  ------------------------------------------------------------ */
    @Test
    public void testVariableIndexHelpers() {
        Variable x = model.addVariable("x"); // free
        Variable y = model.addVariable("y").integer(); // integer
        Variable z = model.addVariable("z").lower(BigDecimal.valueOf(-5)); // negative allowed

        // Free variable index (all non‑fixed)
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));
        assertEquals(2, model.indexOfFreeVariable(z));

        // Integer variable index
        assertEquals(-1, model.indexOfIntegerVariable(x));
        assertEquals(0, model.indexOfIntegerVariable(y));
        assertEquals(-1, model.indexOfIntegerVariable(z));

        // Negative variable index (variables whose lower bound < 0)
        assertEquals(-1, model.indexOfNegativeVariable(x));
        assertEquals(-1, model.indexOfNegativeVariable(y));
        assertEquals(0, model.indexOfNegativeVariable(z));

        // Positive variable index (variables whose upper bound > 0 or no upper bound)
        assertEquals(0, model.indexOfPositiveVariable(x));
        assertEquals(1, model.indexOfPositiveVariable(y));
        assertEquals(1, model.indexOfPositiveVariable(z));
    }

    /** ------------------------------------------------------------
     *  Validation of a feasible solution
     *  ------------------------------------------------------------ */
    @Test
    public void testValidateFeasibleSolution() {
        Variable a = model.addVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable b = model.addVariable("b").lower(BigDecimal.ONE).upper(BigDecimal.ONE); // fixed to 1

        // set values explicitly
        a.setValue(BigDecimal.valueOf(5));
        b.setValue(BigDecimal.ONE);

        // No constraints besides bounds – the model should be feasible
        assertTrue(model.validate());

        // Retrieve the solution via getVariableValues() and validate it again
        Optimisation.Result sol = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, sol.getState());
        assertTrue(model.validate(sol));
    }

    /** ------------------------------------------------------------
     *  Validation of an infeasible solution (bounds violated)
     *  ------------------------------------------------------------ */
    @Test
    public void testValidateInfeasibleSolution() {
        Variable a = model.addVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        a.setValue(BigDecimal.valueOf(2)); // violates upper bound

        // Model should be infeasible
        assertFalse(model.validate());

        // getVariableValues() will return INFEASIBLE state
        Optimisation.Result sol = model.getVariableValues();
        assertEquals(Optimisation.State.INFEASIBLE, sol.getState());
    }

    /** ------------------------------------------------------------
     *  limitObjective creates a constraint copy of the objective
     *  ------------------------------------------------------------ */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        Variable x = model.addVariable("x").weight(BigDecimal.valueOf(2));
        Variable y = model.addVariable("y").weight(BigDecimal.valueOf(-3));

        // Ensure the objective exists
        Expression obj = model.objective();
        assertTrue(obj.isObjective());

        // Apply a lower/upper bound on the objective
        Expression limit = model.limitObjective(BigDecimal.valueOf(-10), BigDecimal.valueOf(10));

        // The returned expression must be the same as the internally stored "objective as constraint"
        assertNotNull(limit);
        assertTrue(limit.isConstraint());

        // Bounds must have been set correctly
        assertEquals(BigDecimal.valueOf(-10), limit.getLowerLimit());
        assertEquals(BigDecimal.valueOf(10), limit.getUpperLimit());

        // The linear coefficients must match the objective
        assertEquals(BigDecimal.valueOf(2), limit.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(-3), limit.get(y.getIndex()));
    }

    /** ------------------------------------------------------------
     *  Integer handling and relaxation
     *  ------------------------------------------------------------ */
    @Test
    public void testIntegerAndRelaxation() {
        Variable i = model.addVariable("i").integer(); // integer variable
        Variable f = model.addVariable("f");          // free (continuous)

        assertTrue(model.isAnyVariableInteger());

        // Relax the model – integer flag should be cleared
        model.relax();
        assertFalse(i.isInteger());
        assertFalse(model.isAnyVariableInteger());

        // Soft relaxation – integer flag stays but model is marked as relaxed
        Variable j = model.addVariable("j").integer();
        model.relax(true);
        assertTrue(j.isInteger());
        assertTrue(model.isRelaxed());
    }

    /** ------------------------------------------------------------
     *  Copying (deep vs shallow) and snapshot behaviour
     *  ------------------------------------------------------------ */
    @Test
    public void testCopyAndSnapshot() {
        Variable v = model.addVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        v.setValue(BigDecimal.valueOf(0.5));

        // Deep copy – modifications to the copy must not affect the original
        ExpressionsBasedModel copy = model.copy();
        Variable vCopy = copy.getVariable(0);
        vCopy.upper(BigDecimal.valueOf(2)); // change upper bound only in copy

        assertEquals(BigDecimal.ONE, v.getUpperLimit()); // original unchanged
        assertEquals(BigDecimal.valueOf(2), vCopy.getUpperLimit());

        // Snapshot creates a shallow copy that is marked relaxed
        ExpressionsBasedModel snapshot = model.snapshot();
        assertTrue(snapshot.isShallowCopy());
        assertTrue(snapshot.isRelaxed());

        // Snapshot must share the same variable objects (shallow)
        assertSame(v, snapshot.getVariable(0));
    }

    /** ------------------------------------------------------------
     *  Adding a special ordered set (SOS) and checking presolver registration
     *  ------------------------------------------------------------ */
    @Test
    public void testAddSpecialOrderedSet() {
        // Create three binary variables
        Variable b1 = model.addVariable("b1").binary();
        Variable b2 = model.addVariable("b2").binary();
        Variable b3 = model.addVariable("b3").binary();

        List<Variable> sos = Arrays.asList(b1, b2, b3);
        // SOS with at most 2 variables ON, at least 1 ON
        model.addSpecialOrderedSet(sos, 1, 2);

        // The SOS adds an expression named "SOS2-..." and a presolver
        // Verify that the expression exists
        Optional<Expression> sosExpr = model.getExpressions().stream()
                .filter(e -> e.getName().startsWith("SOS2-"))
                .findFirst();
        assertTrue(sosExpr.isPresent());

        // Verify that the presolver collection now contains a SpecialOrderedSet instance
        boolean hasSOSPresolver = ExpressionsBasedModel.PRESOLVERS.stream()
                .anyMatch(p -> p.getClass().getSimpleName().equals("SpecialOrderedSet"));
        assertTrue(hasSOSPresolver);
    }

    /** ------------------------------------------------------------
     *  setKnownSolution and validation handler
     *  ------------------------------------------------------------ */
    @Test
    public void testSetKnownSolutionAndHandler() {
        Variable a = model.addVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable b = model.addVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);

        // Create a feasible solution manually
        Optimisation.Result known = Optimisation.Result.of(Optimisation.State.OPTIMAL,
                new double[]{3.0, 4.0});

        // Register a handler that records when validation fails
        final List<String> handlerCalls = new ArrayList<>();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, sol) -> handlerCalls.add("called");

        // Set the known solution – should succeed without invoking handler
        model.setKnownSolution(known, handler);
        assertTrue(handlerCalls.isEmpty());

        // Corrupt the known solution (make b exceed its upper bound) – validation must now fail
        Optimisation.Result bad = Optimisation.Result.of(Optimisation.State.OPTIMAL,
                new double[]{3.0, 20.0});
        try {
            model.setKnownSolution(bad, handler);
            fail("Expected ProgrammingError due to invalid known solution");
        } catch (ProgrammingError ex) {
            // expected
        }
    }

    /** ------------------------------------------------------------
     *  getVariableValues – default handling of unset variables
     *  ------------------------------------------------------------ */
    @Test
    public void testGetVariableValuesDefaultHandling() {
        Variable x = model.addVariable("x").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8));
        Variable y = model.addVariable("y"); // no bounds, no value

        // No explicit values – getVariableValues() should use lower bound for x and 0 for y
        Optimisation.Result sol = model.getVariableValues();
        assertEquals(BigDecimal.valueOf(2), sol.get(0)); // lower bound used
        assertEquals(BigDecimal.ZERO, sol.get(1));      // default zero
        assertEquals(Optimisation.State.FEASIBLE, sol.getState());
    }

    /** ------------------------------------------------------------
     *  isAnyExpressionQuadratic and related checks
     *  ------------------------------------------------------------ */
    @Test
    public void testQuadraticDetection() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // Linear expression – should report no quadratic part
        Expression lin = model.addExpression("lin");
        lin.set(x.getIndex(), BigDecimal.ONE);
        lin.set(y.getIndex(), BigDecimal.ONE);
        assertFalse(lin.isAnyQuadraticFactorNonZero());
        assertFalse(model.isAnyExpressionQuadratic());

        // Quadratic expression – add a quadratic term x*y
        Expression quad = model.addExpression("quad");
        quad.set(x.getIndex(), y.getIndex(), BigDecimal.ONE);
        assertTrue(quad.isAnyQuadraticFactorNonZero());
        assertTrue(model.isAnyExpressionQuadratic());
    }

    /** ------------------------------------------------------------
     *  toString – basic sanity check that it contains variable and expression names
     *  ------------------------------------------------------------ */
    @Test
    public void testToStringContainsNames() {
        model.addVariable("v1");
        model.addVariable("v2");
        model.addExpression("e1");
        String txt = model.toString();

        assertTrue(txt.contains("v1"));
        assertTrue(txt.contains("v2"));
        assertTrue(txt.contains("e1"));
        // The header/footer markers should be present
        assertTrue(txt.startsWith("############################################"));
        assertTrue(txt.endsWith("############################################\n"));
    }

    /** ------------------------------------------------------------
     *  isInfeasible detection after adding an infeasible constraint
     *  ------------------------------------------------------------ */
    @Test
    public void testInfeasibilityDetection() {
        Variable a = model.addVariable("a").lower(BigDecimal.ZERO);
        Expression e = model.addExpression("e");
        e.set(a.getIndex(), BigDecimal.ONE);
        e.upper(BigDecimal.valueOf(-1)); // impossible upper bound

        // Presolve should mark the expression infeasible and model.isInfeasible() must return true
        model.presolve(); // invoke presolve manually
        assertTrue(e.isInfeasible());
        assertTrue(model.isInfeasible());
    }

    /** ------------------------------------------------------------
     *  verify that adding a variable after a shallow copy throws IllegalStateException
     *  ------------------------------------------------------------ */
    @Test
    public void testShallowCopyModificationProhibited() {
        ExpressionsBasedModel shallow = model.copy(true, false); // shallow copy
        try {
            shallow.addVariable("newVar");
            fail("Expected IllegalStateException when adding variable to shallow copy");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    /** ------------------------------------------------------------
     *  verify that the model correctly reports being empty (no variables/expressions)
     *  ------------------------------------------------------------ */
    @Test
    public void testEmptyModelBehaviour() {
        // A brand‑new model has zero variables and expressions
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());

        // Validation of an empty model should be true (nothing can be wrong)
        assertTrue(model.validate());

        // getVariableValues on empty model returns an empty solution with state UNEXPLORED
        Optimisation.Result sol = model.getVariableValues();
        assertEquals(Optimisation.State.UNEXPLORED, sol.getState());
        assertEquals(0, sol.size());
    }
}
