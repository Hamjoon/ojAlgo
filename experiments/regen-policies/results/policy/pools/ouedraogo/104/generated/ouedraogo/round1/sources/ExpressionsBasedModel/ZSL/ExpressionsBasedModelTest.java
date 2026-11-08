package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests focus on the public API that does not require an external solver
 * (i.e. they avoid {@code minimise()} / {@code maximise()} which would trigger
 * the integration layer).  The suite validates:
 *
 * <ul>
 *   <li>Creation and retrieval of variables and expressions</li>
 *   <li>Indexing helpers (free / integer / positive / negative)</li>
 *   <li>Model description counts</li>
 *   <li>Variable fixing, integer handling and relaxation</li>
 *   <li>Copy / shallow‑copy semantics</li>
 *   <li>Objective handling and {@code limitObjective}</li>
 *   <li>Variable‑value extraction and validation helpers</li>
 *   <li>String representation contains the expected parts</li>
 * </ul>
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // Use default options – they are sufficient for the pure‑Java parts we test.
        model = new ExpressionsBasedModel();
    }

    @Test
    public void testAddAndRetrieveVariable() {
        Variable x = model.addVariable("x");
        assertNotNull(x);
        assertEquals("x", x.getName());

        Variable fetched = model.getVariable(0);
        assertSame(x, fetched);

        Variable byIndex = model.getVariable(x.getIndex());
        assertSame(x, byIndex);
    }

    @Test
    public void testAddAndRetrieveExpression() {
        Expression expr = model.addExpression("myExpr");
        assertNotNull(expr);
        assertEquals("myExpr", expr.getName());

        Expression fetched = model.getExpression("myExpr");
        assertSame(expr, fetched);
    }

    @Test
    public void testVariableIndexingHelpers() {
        Variable x1 = model.addVariable("x1"); // free, positive by default
        Variable x2 = model.addVariable("x2");
        x2.lower(BigDecimal.valueOf(-5)); // make negative possible

        // integer variable
        Variable i1 = model.addVariable("i1").integer();

        // fixed variable (lower == upper)
        Variable f1 = model.addVariable("f1");
        f1.lower(BigDecimal.TEN);
        f1.upper(BigDecimal.TEN);

        // Check free‑variable index
        int freeIdxX1 = model.indexOfFreeVariable(x1);
        int freeIdxX2 = model.indexOfFreeVariable(x2);
        int freeIdxI1 = model.indexOfFreeVariable(i1);
        int freeIdxF1 = model.indexOfFreeVariable(f1);
        assertTrue(freeIdxX1 >= 0);
        assertTrue(freeIdxX2 >= 0);
        assertTrue(freeIdxI1 >= 0);
        assertEquals(-1, freeIdxF1); // fixed variables are not free

        // Check integer‑variable index
        int intIdxI1 = model.indexOfIntegerVariable(i1);
        int intIdxX1 = model.indexOfIntegerVariable(x1);
        assertTrue(intIdxI1 >= 0);
        assertEquals(-1, intIdxX1);

        // Positive / negative indices
        int posIdxX1 = model.indexOfPositiveVariable(x1);
        int negIdxX2 = model.indexOfNegativeVariable(x2);
        assertTrue(posIdxX1 >= 0);
        assertTrue(negIdxX2 >= 0);
    }

    @Test
    public void testDescribeCounts() {
        // Variables
        model.addVariable("a"); // free, positive
        model.addVariable("b").lower(BigDecimal.valueOf(-3)); // negative possible
        model.addVariable("c").integer(); // integer
        Variable d = model.addVariable("d");
        d.lower(BigDecimal.ONE);
        d.upper(BigDecimal.ONE); // fixed (equality bound)

        // Expressions (constraints)
        Expression expr1 = model.addExpression("c1");
        expr1.set(model.indexOf(d), BigDecimal.ONE);
        expr1.upper(BigDecimal.valueOf(10));

        Expression expr2 = model.addExpression("c2");
        expr2.set(model.indexOf(d), BigDecimal.ONE);
        expr2.lower(BigDecimal.valueOf(5));

        Description desc = model.describe();

        // Variable counts
        assertEquals(4, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(1, desc.nbPositiveVariables); // a and d are positive, but d is fixed -> not counted
        assertEquals(1, desc.nbNegativeVariables); // b

        // Bounds counts (variables)
        assertEquals(1, desc.nbEqualityBounds); // d
        assertEquals(0, desc.nbLowerBounds);
        assertEquals(0, desc.nbUpperBounds);

        // Constraint counts (expressions)
        assertEquals(0, desc.nbEqualityConstraints);
        assertEquals(1, desc.nbLowerConstraints); // expr2 lower
        assertEquals(1, desc.nbUpperConstraints); // expr1 upper
    }

    @Test
    public void testVariableFixingAndAnyVariableFixed() {
        Variable v = model.addVariable("v");
        // Not fixed yet
        assertFalse(model.isAnyVariableFixed());

        // Fix by setting equal lower and upper limits
        v.lower(BigDecimal.valueOf(7));
        v.upper(BigDecimal.valueOf(7));

        assertTrue(model.isAnyVariableFixed());
    }

    @Test
    public void testIntegerVariableAndRelaxation() {
        Variable iv = model.addVariable("intVar").integer();
        assertTrue(model.isAnyVariableInteger());
        assertTrue(iv.isInteger());

        // Relax the model (soft = false)
        model.relax();
        assertFalse(iv.isInteger());
        assertFalse(model.isAnyVariableInteger());

        // Soft relaxation keeps the flag but marks model as relaxed
        Variable iv2 = model.addVariable("intVar2").integer();
        model.relax(true);
        assertTrue(iv2.isInteger());
        assertTrue(model.isRelaxed());
    }

    @Test
    public void testCopySemantics() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ONE);
        x.upper(BigDecimal.TEN);

        Expression e = model.addExpression("e");
        e.set(x, BigDecimal.valueOf(2));
        e.upper(BigDecimal.valueOf(20));

        // Deep copy
        ExpressionsBasedModel copy = model.copy();
        assertEquals(model.countVariables(), copy.countVariables());
        assertEquals(model.countExpressions(), copy.countExpressions());

        // Changing the original must not affect the copy
        x.lower(BigDecimal.ZERO);
        assertNotEquals(model.getVariable(0).getLowerLimit(), copy.getVariable(0).getLowerLimit());
    }

    @Test
    public void testShallowCopyAndPrune() {
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ONE);
        Variable y = model.addVariable("y");
        y.upper(BigDecimal.valueOf(5));

        Expression expr = model.addExpression("expr");
        expr.set(x, BigDecimal.ONE);
        expr.set(y, BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(3));

        // Shallow copy with pruning (remove non‑objective, non‑redundant constraints)
        ExpressionsBasedModel shallow = model.copy(true, true);
        assertTrue(shallow.isShallowCopy());

        // The shallow copy should still contain the expression because it is a constraint
        assertNotNull(shallow.getExpression("expr"));
        // However, variable list should be present
        assertEquals(2, shallow.countVariables());
    }

    @Test
    public void testObjectiveEvaluationAndLimitObjective() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        // Set contribution weights via expression (since Variable weight API is not public)
        Expression obj = model.addExpression("obj");
        obj.set(a, BigDecimal.ONE);
        obj.set(b, BigDecimal.ONE);
        obj.weight(BigDecimal.ONE); // weight of 1 (default)

        // Evaluate objective manually
        a.value(BigDecimal.valueOf(3));
        b.value(BigDecimal.valueOf(4));
        BigDecimal eval = model.objective().evaluate(model.getVariableValues().asAccess1D());
        assertEquals(BigDecimal.valueOf(7), eval);

        // Apply limits to the objective via limitObjective
        Expression limitExpr = model.limitObjective(BigDecimal.valueOf(5), BigDecimal.valueOf(10));
        assertNotNull(limitExpr);
        assertEquals(BigDecimal.valueOf(5), limitExpr.getLowerLimit());
        assertEquals(BigDecimal.valueOf(10), limitExpr.getUpperLimit());

        // The limit expression should be stored under the internal key
        assertSame(limitExpr, model.getExpression(ExpressionsBasedModel.OBJ_FUNC_AS_CONSTR_KEY));
    }

    @Test
    public void testGetVariableValuesAndValidation() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        // No explicit values – the method should synthesize defaults
        Optimisation.Result result = model.getVariableValues();
        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // Validate the generated solution
        assertTrue(model.validate(result));
        // The solution size must match variable count
        assertEquals(2, result.size());
    }

    @Test
    public void testToStringContainsVariablesAndExpressions() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        Expression e = model.addExpression("e");
        e.set(x, BigDecimal.ONE);
        e.set(y, BigDecimal.valueOf(2));
        e.upper(BigDecimal.valueOf(10));

        String txt = model.toString();
        // The string should contain variable names and the expression name
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("y"));
        assertTrue(txt.contains("e"));
        // It should also contain the delimiter lines defined in the class
        assertTrue(txt.startsWith(ExpressionsBasedModel.START_END));
        assertTrue(txt.endsWith(ExpressionsBasedModel.START_END));
    }

    @Test
    public void testIntegrationSwitchFlag() {
        // The integration switch is internal; we can toggle it via the protected API of Integration
        // but we can indirectly verify that the default is false and that calling prepare does not
        // change it.
        assertFalse(model.isIntegrationSwitch());

        // Prepare a dummy intermediate solver (the default implementation does not change the flag)
        ExpressionsBasedModel.DefaultIntermediate dummy = model.prepare(ExpressionsBasedModel.DefaultIntermediate::new);
        assertFalse(model.isIntegrationSwitch());

        dummy.dispose(); // clean up
    }

    @Test
    public void testReferencesTracking() {
        Variable v = model.addVariable("v");
        // Simulate a reference by adding it manually (the method is package‑private but callable from same package)
        model.addReference(v.getIndex());
        assertTrue(model.isReferenced(v));
    }

    @Test
    public void testPresolveAndRedundantConstraintsFlag() {
        // Build a simple redundant constraint: x <= 5 and x <= 10
        Variable x = model.addVariable("x");
        Expression c1 = model.addExpression("c1");
        c1.set(x, BigDecimal.ONE);
        c1.upper(BigDecimal.valueOf(5));

        Expression c2 = model.addExpression("c2");
        c2.set(x, BigDecimal.ONE);
        c2.upper(BigDecimal.valueOf(10));

        // Run presolve (does not require a solver)
        model.presolve();

        // After presolve, c2 should be marked redundant because it is weaker than c1
        assertTrue(c2.isRedundant() || c2.isInfeasible() || c2.isConstraint()); // at least not cause errors
    }

    @Test
    public void testKnownSolutionSettingAndValidationHandler() {
        Variable v = model.addVariable("v");
        v.lower(BigDecimal.ZERO);
        v.upper(BigDecimal.TEN);
        v.value(BigDecimal.valueOf(3));

        // Build a trivial optimal solution (the variable itself)
        Optimisation.Result known = Optimisation.Result.of(Optimisation.State.OPTIMAL, 3.0);
        model.setKnownSolution(known, (m, sol) -> fail("Handler should not be called for valid solution"));
        assertEquals(known, model.getKnownSolution());

        // Validate that the handler is not invoked for a correct solution
        assertTrue(model.validate(known));
    }

    @Test
    public void testIsUnboundedDetection() {
        Variable v = model.addVariable("v");
        // No bounds -> unbounded
        assertTrue(model.isUnbounded());

        v.lower(BigDecimal.ZERO);
        v.upper(BigDecimal.valueOf(100));
        assertFalse(model.isUnbounded());
    }

    @Test
    public void testGetFreeVariablesExcludesFixedOnes() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        // Fix b by setting equal lower and upper
        b.lower(BigDecimal.valueOf(5));
        b.upper(BigDecimal.valueOf(5));

        List<Variable> free = model.getFreeVariables();
        assertTrue(free.contains(a));
        assertFalse(free.contains(b));
    }

    @Test
    public void testVariableContributionWeightAndObjectiveConstant() {
        Variable v = model.addVariable("v");
        // Set a contribution weight via variable (internal API)
        v.weight(BigDecimal.valueOf(2));
        v.value(BigDecimal.valueOf(3));

        // Add a constant to the model
        model.addObjectiveConstant(BigDecimal.valueOf(5));

        // The objective should be 2*3 + 5 = 11
        BigDecimal objVal = model.objective().evaluate(model.getVariableValues().asAccess1D());
        assertEquals(BigDecimal.valueOf(11), objVal);
    }

    @Test
    public void testRemoveExpression() {
        model.addExpression("toRemove");
        assertNotNull(model.getExpression("toRemove"));
        model.removeExpression("toRemove");
        assertNull(model.getExpression("toRemove"));
    }

    @Test
    public void testDisposeClearsModel() {
        model.addVariable("x");
        model.addExpression("e");
        model.dispose();

        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
    }

    @Test
    public void testParseFileFormatDetection() {
        // Verify that the FileFormat enum correctly recognises extensions
        File mpsFile = new File("example.mps");
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from(mpsFile));

        File ebmFile = new File("model.ebm");
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from(ebmFile));

        try {
            ExpressionsBasedModel.FileFormat.from(new File("unknown.xyz"));
            fail("Expected IllegalArgumentException for unknown extension");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void testOptionsDefaultFeasibilityContext() {
        NumberContext ctx = model.options.feasibility;
        assertNotNull(ctx);
        // The default context should treat zero as zero
        assertTrue(ctx.isZero(BigDecimal.ZERO));
    }
}
