package org.ojalgo.optimisation;
import org.ojalgo.netio.InMemoryFile;


import static org.junit.Assert.*;
import static org.ojalgo.function.constant.BigMath.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel.Description;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.Optimisation.State;
import org.ojalgo.structure.Access1D;

/**
 * Comprehensive unit tests for {@link ExpressionsBasedModel}.
 *
 * The tests only use the public API that is visible in the source snippet
 * (and the minimal supporting classes that are listed in the prompt). No
 * external mocking frameworks are used – real objects are created through the
 * provided constructors and factory methods.
 *
 * The test suite covers:
 * <ul>
 *   <li>Variable creation, indexing and fixing.</li>
 *   <li>Expression creation, linear coefficients and constraint limits.</li>
 *   <li>Model description (counts of variables / constraints).</li>
 *   <li>Objective handling (default empty objective).</li>
 *   <li>Solution extraction and validation.</li>
 *   <li>Infeasibility detection.</li>
 *   <li>Integer handling and relaxation.</li>
 *   <li>Presolve‑related helpers (simplify / reduce) – exercised only for
 *       side‑effects (no exception).</li>
 *   <li>Utility methods such as {@code limitObjective} and {@code toString}.</li>
 * </ul>
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    /** Helper to create a variable with a given name and optional bounds. */
    private Variable createVariable(String name, BigDecimal lower, BigDecimal upper, boolean integer) {
        Variable var = model.newVariable(name);
        if (lower != null) {
            var.lower(lower);
        }
        if (upper != null) {
            var.upper(upper);
        }
        if (integer) {
            var.integer();
        }
        return var;
    }

    /** Helper to create a linear expression  a*X + b*Y . */
    private Expression createLinearExpression(String name, Variable a, BigDecimal coeffA,
                                              Variable b, BigDecimal coeffB) {
        Expression expr = model.newExpression(name);
        expr.set(a.getIndex(), coeffA);
        expr.set(b.getIndex(), coeffB);
        return expr;
    }

    @Test
    public void testAddVariablesAndIndices() {
        Variable x = model.newVariable("X");
        Variable y = model.newVariable("Y");

        assertEquals(0, model.indexOf(x));
        assertEquals(1, model.indexOf(y));

        // Both variables are free (not fixed) → free indices 0 and 1
        assertEquals(0, model.indexOfFreeVariable(x));
        assertEquals(1, model.indexOfFreeVariable(y));

        // Fix variable X by setting equal lower and upper bounds
        x.lower(ZERO).upper(ZERO);
        // After fixing, X is no longer a free variable
        assertEquals(-1, model.indexOfFreeVariable(x));
        assertEquals(0, model.indexOfFreeVariable(y));

        // The model should report that at least one variable is fixed
        assertTrue(model.isAnyVariableFixed());
    }

    @Test
    public void testAddExpressionAndLinearCoefficients() {
        Variable x = model.newVariable("X");
        Variable y = model.newVariable("Y");

        // Build expression: 3*X + 5*Y
        Expression expr = model.newExpression("E1");
        expr.set(x.getIndex(), new BigDecimal("3"));
        expr.set(y.getIndex(), new BigDecimal("5"));
        expr.lower(new BigDecimal("4")).upper(new BigDecimal("20"));

        // Retrieve the same expression via name
        Expression retrieved = model.getExpression("E1");
        assertNotNull(retrieved);
        assertEquals(new BigDecimal("3"), retrieved.get(x.getIndex()));
        assertEquals(new BigDecimal("5"), retrieved.get(y.getIndex()));
        assertEquals(new BigDecimal("4"), retrieved.getLowerLimit());
        assertEquals(new BigDecimal("20"), retrieved.getUpperLimit());

        // The expression should be recognised as a constraint
        assertTrue(retrieved.isConstraint());
    }

    @Test
    public void testDescriptionCounts() {
        // Variables: X (lower bound), Y (upper bound), Z (equality bound)
        Variable x = createVariable("X", new BigDecimal("-5"), null, false);
        Variable y = createVariable("Y", null, new BigDecimal("10"), false);
        Variable z = createVariable("Z", new BigDecimal("2"), new BigDecimal("2"), false);

        // Expressions: one lower, one upper, one equality constraint
        Expression lowerExpr = model.newExpression("L");
        lowerExpr.set(x.getIndex(), ONE);
        lowerExpr.lower(new BigDecimal("1"));
        Expression upperExpr = model.newExpression("U");
        upperExpr.set(y.getIndex(), ONE);
        upperExpr.upper(new BigDecimal("8"));
        Expression eqExpr = model.newExpression("E");
        eqExpr.set(z.getIndex(), ONE);
        eqExpr.level(new BigDecimal("2")); // level sets both lower and upper

        Description desc = model.describe();

        // Variables
        assertEquals(3, desc.nbVariables);
        // Bounds on variables
        assertEquals(1, desc.nbLowerBounds);   // X has lower bound
        assertEquals(1, desc.nbUpperBounds);   // Y has upper bound
        assertEquals(1, desc.nbEqualityBounds); // Z has equality bound

        // Constraints (expressions)
        assertEquals(1, desc.nbLowerConstraints);
        assertEquals(1, desc.nbUpperConstraints);
        assertEquals(1, desc.nbEqualityConstraints);
    }

    @Test
    public void testObjectiveIsEmptyWhenNoObjectiveSet() {
        // No variable or expression is marked as objective
        Expression obj = model.objective();

        // The generated objective should have zero constant and no linear terms
        assertEquals(ZERO, obj.getConstant());
        assertTrue(obj.isFunctionConstant());
        assertTrue(obj.isFunctionLinear() == false);
        assertEquals(0, obj.getLinearKeySet().size());
    }

    @Test
    public void testVariableValuesAndValidation() {
        Variable x = model.newVariable("X");
        Variable y = model.newVariable("Y");

        // Set concrete values
        x.setValue(new BigDecimal("3"));
        y.setValue(new BigDecimal("7"));

        // Extract solution vector
        Result solution = model.getVariableValues();

        // Verify state and values
        assertEquals(State.FEASIBLE, solution.getState());
        assertEquals(2, solution.size());
        assertEquals(3.0, solution.doubleValue(0), 1e-12);
        assertEquals(7.0, solution.doubleValue(1), 1e-12);

        // Validate the solution – should be true
        assertTrue(model.validate(solution, model.options.feasibility, model.options.logger_appender));
    }

    @Test
    public void testInfeasibilityDetectionViaVariableBounds() {
        // Create a variable with contradictory bounds (lower > upper)
        Variable v = model.newVariable("V");
        v.lower(new BigDecimal("10"));
        v.upper(new BigDecimal("5"));

        // The model should recognise it as infeasible
        assertTrue(model.isInfeasible());

        // The description should count one lower and one upper bound
        Description desc = model.describe();
        assertEquals(1, desc.nbLowerBounds);
        assertEquals(1, desc.nbUpperBounds);
    }

    @Test
    public void testIntegerVariableDetectionAndRelaxation() {
        // Integer variable
        Variable i = model.newVariable("I");
        i.integer();

        assertTrue(model.isAnyVariableInteger());

        // Relax the model – integer flag should be ignored
        model.relax();
        assertFalse(model.isAnyVariableInteger());

        // Re‑enable integer handling (soft relax does not clear the flag)
        model.relax(false);
        assertTrue(i.isInteger());
    }

    @Test
    public void testLimitObjectiveCreatesConstraintExpression() {
        // Create a simple objective by weighting a variable
        Variable x = model.newVariable("X");
        Expression objExpr = model.objective(); // empty for now

        // Add a contribution weight to X via an expression (the API does not expose a direct weight method,
        // so we create an expression that will become the objective.)
        Expression expr = model.newExpression("OBJ");
        expr.set(x.getIndex(), ONE);
        expr.weight(ONE); // weight() exists on Expression (inherited from ModelEntity)

        // Apply a limit on the generated objective
        Expression limited = model.limitObjective(new BigDecimal("0"), new BigDecimal("100"));
        assertNotNull(limited);
        assertEquals(new BigDecimal("0"), limited.getLowerLimit());
        assertEquals(new BigDecimal("100"), limited.getUpperLimit());

        // The limited expression should be present in the model under the internal key
        assertTrue(model.getExpressions().contains(limited));
    }

    @Test
    public void testSimplifyAndReduceDoNotThrow() {
        // Create a tiny model that triggers presolvers (binary variable + SOS)
        Variable a = model.newVariable("A");
        a.binary();
        Variable b = model.newVariable("B");
        b.binary();

        // Simple expression linking the two binary vars
        Expression expr = model.newExpression("SUM");
        expr.set(a.getIndex(), ONE);
        expr.set(b.getIndex(), ONE);
        expr.upper(new BigDecimal("1")); // at most one can be 1

        // Invoke simplify – should produce a shallow copy without exception
        ExpressionsBasedModel simplified = model.simplify();
        assertNotNull(simplified);
        assertEquals(2, simplified.countVariables());

        // Reduce – also should not throw
        model.reduce();
        // No explicit result to check; we only assert the model is still valid
        assertTrue(model.validate());
    }

    @Test
    public void testToStringContainsVariablesAndExpressions() {
        Variable x = model.newVariable("X");
        x.lower(ZERO).upper(new BigDecimal("10"));
        Variable y = model.newVariable("Y");
        y.lower(new BigDecimal("5")).upper(new BigDecimal("15"));

        Expression expr = model.newExpression("E");
        expr.set(x.getIndex(), ONE);
        expr.set(y.getIndex(), ONE);
        expr.upper(new BigDecimal("12"));

        String txt = model.toString();
        // The string should contain the variable names and the expression name
        assertTrue(txt.contains("X"));
        assertTrue(txt.contains("Y"));
        assertTrue(txt.contains("E"));
        // It should also contain the start/end delimiter defined in the model
        assertTrue(txt.startsWith("############################################"));
        assertTrue(txt.endsWith("############################################\n"));
    }

    @Test
    public void testWriteAndParseEBMFileRoundTrip() throws Exception {
        // Build a tiny model
        Variable v = model.newVariable("V");
        v.lower(ZERO).upper(ONE);
        Expression e = model.newExpression("E");
        e.set(v.getIndex(), ONE);
        e.upper(ONE);

        // Write to an in‑memory file
        InMemoryFile imf = new InMemoryFile();
        model.writeTo(imf);

        // Parse the same content back
        ExpressionsBasedModel parsed = ExpressionsBasedModel.parse(imf.getInputStream(), ExpressionsBasedModel.FileFormat.EBM);

        // The parsed model should contain the same variable and expression
        assertEquals(1, parsed.countVariables());
        assertEquals(1, parsed.countExpressions());
        Variable parsedVar = parsed.getVariable(0);
        assertEquals("V", parsedVar.getName());
        Expression parsedExpr = parsed.getExpression("E");
        assertNotNull(parsedExpr);
        assertEquals(ONE, parsedExpr.get(parsedVar.getIndex()));
    }
}
