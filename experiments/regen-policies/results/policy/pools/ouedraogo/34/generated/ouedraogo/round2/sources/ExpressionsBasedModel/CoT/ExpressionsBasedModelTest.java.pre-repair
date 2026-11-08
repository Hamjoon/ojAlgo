package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}. The tests cover:
 * <ul>
 *   <li>Public API method signatures and basic behaviour</li>
 *   <li>Edge‑case handling (invalid arguments, nulls, mismatched sizes)</li>
 *   <li>Exception throwing for illegal usage</li>
 * </ul>
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // use the default optimisation options
        model = new ExpressionsBasedModel();
    }

    /* ------------------------------------------------------------------------
     *  List of public methods (extracted from the source)
     * ------------------------------------------------------------------------
     * Constructors
     *   ExpressionsBasedModel()
     *   ExpressionsBasedModel(Optimisation.Options)
     *
     * Variable handling
     *   Variable addVariable()
     *   Variable addVariable(String)
     *   List<Variable> getVariables()
     *   Variable getVariable(int)
     *   Variable getVariable(IntIndex)
     *   int indexOf(Variable)
     *
     * Expression handling
     *   Expression addExpression()
     *   Expression addExpression(String)
     *   Collection<Expression> getExpressions()
     *   Expression getExpression(String)
     *
     * Model description / meta‑data
     *   Description describe()
     *   int countVariables()
     *   int countExpressions()
     *
     * Optimisation sense
     *   Optimisation.Sense getOptimisationSense()
     *
     * Objective handling
     *   Expression objective()
     *   Expression limitObjective(BigDecimal, BigDecimal)
     *
     * Solving
     *   Optimisation.Result minimise()
     *   Optimisation.Result maximise()
     *
     * Validation helpers
     *   boolean validate()
     *   boolean validate(Access1D<BigDecimal>)
     *
     * Miscellaneous
     *   Result getVariableValues()
     *   Result getVariableValues(NumberContext)
     *   void setKnownSolution(Result)
     *   void setKnownSolution(Result, BiConsumer)
     *   void relax()
     *   void relax(boolean)
     *   void removeExpression(String)
     *   void dispose()
     *   String toString()
     * ------------------------------------------------------------------------ */

    /* ==========================
       BASIC VARIABLE / EXPRESSION
       ========================== */

    @Test
    public void testAddAndRetrieveVariable() {
        Variable x = model.addVariable("x");
        assertNotNull("Variable should be created", x);
        assertEquals("Variable name must match", "x", x.getName());

        Variable fetched = model.getVariable(0);
        assertSame("Fetched variable must be the same instance", x, fetched);

        int idx = model.indexOf(x);
        assertEquals("Index returned by indexOf must equal internal index", 0, idx);
    }

    @Test
    public void testAddAndRetrieveExpression() {
        Expression e = model.addExpression("myExpr");
        assertNotNull("Expression should be created", e);
        assertEquals("Expression name must match", "myExpr", e.getName());

        Expression fetched = model.getExpression("myExpr");
        assertSame("Fetched expression must be the same instance", e, fetched);
    }

    @Test
    public void testVariableAndExpressionCounts() {
        assertEquals("Initially 0 variables", 0, model.countVariables());
        assertEquals("Initially 0 expressions", 0, model.countExpressions());

        model.addVariable("a");
        model.addVariable("b");
        model.addExpression("c1");
        model.addExpression("c2");

        assertEquals(2, model.countVariables());
        assertEquals(2, model.countExpressions());
    }

    /* ==========================
       OBJECTIVE FUNCTION
       ========================== */

    @Test
    public void testObjectiveAggregation() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // set contribution weights
        x.weight(BigDecimal.ONE);
        y.weight(BigDecimal.valueOf(2));

        Expression obj = model.objective();
        assertNotNull("Objective expression must be generated", obj);
        // The objective should contain the linear coefficients we set
        assertEquals(BigDecimal.ONE, obj.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(2), obj.get(y.getIndex()));
    }

    @Test
    public void testLimitObjectiveCreatesConstraintWhenNeeded() {
        // No explicit objective yet – limitObjective should create a constraint based on the
        // (implicit) objective expression.
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limit);
        assertTrue(limit.isConstraint());
        // The constraint should have the same limits we passed
        assertEquals(BigDecimal.ZERO, limit.getLowerLimit());
        assertEquals(BigDecimal.TEN, limit.getUpperLimit());
    }

    /* ==========================
       DESCRIPTION / META‑DATA
       ========================== */

    @Test
    public void testDescribeCounts() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        y.lower(BigDecimal.ZERO);

        model.addExpression("c1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.addExpression("c2"); // no bounds → not counted as constraint

        Description d = model.describe();
        assertEquals(2, d.nbVariables);
        assertEquals(2, d.nbPositiveVariables); // both have non‑negative ranges
        assertEquals(0, d.nbNegativeVariables);
        assertEquals(0, d.nbIntegerVariables);
        // bounds from variables
        assertEquals(2, d.nbLowerBounds);
        assertEquals(1, d.nbUpperBounds);
        // constraints from expressions
        assertEquals(1, d.nbLowerConstraints);
        assertEquals(1, d.nbUpperConstraints);
    }

    /* ==========================
       SOLVER INTERACTION (MIN / MAX)
       ========================== */

    @Test
    public void testMinimiseReturnsResult() {
        Variable x = model.addVariable("x");
        x.weight(BigDecimal.ONE); // objective: minimise x

        Optimisation.Result result = model.minimise(); // uses built‑in linear solver for a trivial problem
        assertNotNull("Result must not be null", result);
        assertNotNull("Result state must not be null", result.getState());
        // With no bounds the solver can set the variable to 0, giving objective value 0
        assertEquals(0.0, result.getValue(), 1e-8);
    }

    @Test
    public void testMaximiseReturnsResult() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        x.weight(BigDecimal.ONE); // objective: maximise x

        Optimisation.Result result = model.maximise();
        assertNotNull(result);
        // The optimal value should be at the upper bound (=10)
        assertEquals(10.0, result.getValue(), 1e-8);
    }

    /* ==========================
       VALIDATION EDGE CASES
       ========================== */

    @Test
    public void testValidateFailsWithMismatchedSolutionSize() {
        model.addVariable("x");
        model.addVariable("y");

        // Create a solution with only one entry (size mismatch)
        Optimisation.Result badSolution = Optimisation.Result.of(Optimisation.State.FEASIBLE, 0.0);
        boolean valid = model.validate(badSolution);
        assertFalse("Validation must fail when solution size does not match variable count", valid);
    }

    @Test
    public void testValidateInfeasibleWhenAllVariablesUnset() {
        // No bounds, no values – getVariableValues should report INFEASIBLE
        Optimisation.Result values = model.getVariableValues();
        assertEquals(Optimisation.State.INFEASIBLE, values.getState());
    }

    /* ==========================
       SET KNOWN SOLUTION – EXCEPTION PATHS
       ========================== */

    @Test(expected = RuntimeException.class) // ProgrammingError extends RuntimeException
    public void testSetKnownSolutionRejectsNonOptimalState() {
        Optimisation.Result nonOptimal = Optimisation.Result.of(Optimisation.State.FEASIBLE, 1.0);
        model.setKnownSolution(nonOptimal);
    }

    @Test
    public void testSetKnownSolutionAcceptsOptimalResult() {
        // Build a trivial optimal result (state = OPTIMAL)
        Optimisation.Result optimal = Optimisation.Result.of(Optimisation.State.OPTIMAL, 0.0);
        model.setKnownSolution(optimal);
        // Retrieval should give the same reference
        assertSame(optimal, model.getKnownSolution());
    }

    /* ==========================
       SPECIAL ORDERED SET – INVALID INPUT
       ========================== */

    @Test(expected = RuntimeException.class) // ProgrammingError extends RuntimeException
    public void testAddSpecialOrderedSetInvalidTypeThrows() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Expression dummy = model.addExpression("dummy").upper(BigDecimal.ONE);
        // type <= 0 triggers a ProgrammingError
        model.addSpecialOrderedSet(Collections.<Variable>emptyList(), 0, dummy);
    }

    /* ==========================
       RELAXATION BEHAVIOUR
       ========================== */

    @Test
    public void testRelaxRemovesIntegerFlag() {
        Variable i = model.addVariable("i");
        i.integer(true);
        assertTrue(i.isInteger());

        model.relax(); // hard relaxation – removes integer flag
        assertFalse(i.isInteger());
    }

    @Test
    public void testSoftRelaxKeepsIntegerFlagButMarksModelRelaxed() {
        Variable i = model.addVariable("i");
        i.integer(true);
        model.relax(true);
        // Integer flag is still true
        assertTrue(i.isInteger());
        // Model reports relaxed
        assertTrue(model.isRelaxed());
    }

    /* ==========================
       REMOVING EXPRESSIONS
       ========================== */

    @Test
    public void testRemoveExpression() {
        model.addExpression("toBeRemoved");
        assertNotNull(model.getExpression("toBeRemoved"));
        model.removeExpression("toBeRemoved");
        assertNull("Expression should be removed", model.getExpression("toBeRemoved"));
    }

    /* ==========================
       TO STRING REPRESENTATION
       ========================== */

    @Test
    public void testToStringContainsVariableAndExpressionNames() {
        model.addVariable("x");
        model.addExpression("expr");
        String txt = model.toString();
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("expr"));
    }

    /* ==========================
       DISPOSE CLEANS INTERNAL COLLECTIONS
       ========================== */

    @Test
    public void testDisposeClearsModel() {
        model.addVariable("v");
        model.addExpression("e");
        model.dispose();
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
    }

    /* ==========================
       PREPARE METHOD (factory function)
       ========================== */

    @Test
    public void testPrepareCreatesIntermediateSolver() {
        // The default factory simply creates a DefaultIntermediate instance
        Function<ExpressionsBasedModel, ExpressionsBasedModel.DefaultIntermediate> factory = ExpressionsBasedModel.DefaultIntermediate::new;
        ExpressionsBasedModel.DefaultIntermediate intermediate = model.prepare(factory);
        assertNotNull(intermediate);
        // The intermediate should hold a reference to the original model
        assertSame(model, intermediate.model);
    }

    /* ==========================
       BOUNDS STREAM (filters)
       ========================== */

    @Test
    public void testBoundsStreamFiltersCorrectly() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO); // becomes a bound
        Variable y = model.addVariable("y"); // free variable, no bounds

        long boundCount = model.bounds().count();
        assertEquals(1L, boundCount);
    }

    /* ==========================
       CONSTRAINTS STREAM (filters)
       ========================== */

    @Test
    public void testConstraintsStreamFiltersCorrectly() {
        model.addExpression("c1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.addExpression("c2"); // not a constraint

        long cnstrCount = model.constraints().count();
        assertEquals(1L, cnstrCount);
    }

    /* ==========================
       COPY / SNAPSHOT BEHAVIOUR
       ========================== */

    @Test
    public void testCopyCreatesIndependentModel() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        ExpressionsBasedModel copy = model.copy();
        // Change the original – copy must stay unchanged
        x.upper(BigDecimal.TEN);
        assertNull(copy.getVariable(0).getUpperLimit()); // copy should not see the new upper bound
    }

    @Test
    public void testSnapshotIsRelaxedCopy() {
        Variable i = model.addVariable("i");
        i.integer(true);
        ExpressionsBasedModel snapshot = model.snapshot();
        assertTrue(snapshot.isRelaxed());
        // Integer flag should still be present (soft relaxation)
        assertTrue(snapshot.getVariable(0).isInteger());
    }
}
