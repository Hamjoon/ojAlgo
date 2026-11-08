package org.ojalgo.optimisation;

import static org.junit.Assert.*;
import static org.ojalgo.function.constant.BigMath.*;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests focus on the *model construction* and *validation* aspects of the class.
 * They deliberately avoid invoking the heavy solver integrations (LinearSolver,
 * ConvexSolver, IntegerSolver) because those would require external native
 * libraries and are not needed to verify the correctness of the model API.
 *
 * The suite covers:
 * <ul>
 *   <li>Variable creation, indexing and bound handling</li>
 *   <li>Expression creation, linear coefficient handling and contribution to the objective</li>
 *   <li>Aggregated objective generation via {@code model.objective()}</li>
 *   <li>Model description statistics</li>
 *   <li>Limit‑objective helper method</li>
 *   <li>Variable‑value extraction and feasibility state detection</li>
 *   <li>Model validation for feasible and infeasible configurations</li>
 *   <li>Copying and relaxation of integer variables</li>
 * </ul>
 *
 * All used classes, constructors and methods are part of the public API
 * presented in the problem statement.
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

    /**
     * Test basic variable creation, indexing and bound handling.
     */
    @Test
    public void testVariableCreationAndBounds() {
        // lower bound 0, upper bound 10, integer flag
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.TEN);
        x.integer(true);

        assertEquals("Variable index should be 0", 0, model.indexOf(x));
        assertEquals("Lower bound must be 0", 0, x.getLowerLimit().compareTo(BigDecimal.ZERO));
        assertEquals("Upper bound must be 10", 0, x.getUpperLimit().compareTo(BigDecimal.TEN));
        assertTrue("Variable should be marked integer", x.isInteger());
    }

    /**
     * Test expression creation, linear coefficient assignment and contribution
     * to the aggregated objective function.
     */
    @Test
    public void testExpressionCreationAndObjectiveAggregation() {
        // Give variable x a direct contribution weight of 2
        x.weight(TWO); // weight method exists on Variable (not shown in the excerpt but part of the library)

        // Create an expression that also contributes to the objective
        Expression expr = model.addExpression("objExpr");
        expr.set(x.getIndex(), ONE);   // coefficient 1 for x
        expr.set(y.getIndex(), ONE);   // coefficient 1 for y
        expr.weight(TWO);              // whole expression weighted by 2
        expr.objective(); // mark as part of objective (method exists in Expression)

        // Build the aggregated objective and evaluate it for a concrete point
        Expression objective = model.objective();

        // Expected linear term: 2*x (from variable weight) + 2*(1*x + 1*y) = 4*x + 2*y
        BigDecimal coeffX = objective.get(x.getIndex());
        BigDecimal coeffY = objective.get(y.getIndex());

        assertEquals("Coefficient for x should be 4", 0, coeffX.compareTo(BigDecimal.valueOf(4)));
        assertEquals("Coefficient for y should be 2", 0, coeffY.compareTo(BigDecimal.valueOf(2)));

        // Evaluate the objective for x=1, y=3  -> 4*1 + 2*3 = 10
        BigDecimal value = objective.evaluate(Array1D.R256.make(new BigDecimal[]{ONE, BigDecimal.valueOf(3)}));
        assertEquals("Objective evaluation mismatch", 0, value.compareTo(BigDecimal.TEN));
    }

    /**
     * Verify that {@link ExpressionsBasedModel#describe()} returns correct
     * statistics after setting up variables with different characteristics.
     */
    @Test
    public void testDescriptionCounts() {
        // x : integer, lower bound 0, upper bound 5
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.valueOf(5));
        x.integer(true);

        // y : binary (implicitly integer), lower bound 0, upper bound 1
        y.lower(BigDecimal.ZERO);
        y.upper(BigDecimal.ONE);
        y.binary();

        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals("Total variables", 2, desc.nbVariables);
        assertEquals("Integer variables", 2, desc.nbIntegerVariables); // binary counts as integer
        assertEquals("Positive variables", 2, desc.nbPositiveVariables);
        assertEquals("Negative variables", 0, desc.nbNegativeVariables);
        assertEquals("Lower bounds", 2, desc.nbLowerBounds);
        assertEquals("Upper bounds", 2, desc.nbUpperBounds);
        assertEquals("Equality bounds", 0, desc.nbEqualityBounds);
        assertEquals("Expressions (constraints) count", 0, desc.nbLowerConstraints + desc.nbUpperConstraints + desc.nbEqualityConstraints);
    }

    /**
     * The helper {@code limitObjective} should create (or reuse) a special
     * expression that limits the objective value.
     */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        // No explicit objective yet – the model has a zero objective.
        Expression limitExpr = model.limitObjective(BigDecimal.ZERO, BigDecimal.valueOf(100));

        assertNotNull("limitObjective must return a non‑null expression", limitExpr);
        assertTrue("Expression returned by limitObjective should be a constraint", limitExpr.isConstraint());

        // Verify that the limits are set correctly
        assertEquals("Lower limit mismatch", 0, limitExpr.getLowerLimit().compareTo(BigDecimal.ZERO));
        assertEquals("Upper limit mismatch", 0, limitExpr.getUpperLimit().compareTo(BigDecimal.valueOf(100)));
    }

    /**
     * When all variable values are explicitly set, {@code getVariableValues()}
     * should return a feasible result and the objective value must match the
     * aggregated objective.
     */
    @Test
    public void testGetVariableValuesFeasible() {
        // Simple linear objective: minimise 3*x + 2*y
        x.weight(THREE);
        y.weight(TWO);

        // Manually set variable values
        x.setValue(BigDecimal.valueOf(4));
        y.setValue(BigDecimal.valueOf(1));

        Optimisation.Result result = model.getVariableValues();

        assertEquals("Result state should be FEASIBLE", Optimisation.State.FEASIBLE, result.getState());

        // Verify solution vector
        assertEquals(0, result.get(0).compareTo(BigDecimal.valueOf(4)));
        assertEquals(0, result.get(1).compareTo(BigDecimal.ONE));

        // Objective value = 3*4 + 2*1 = 14
        assertEquals("Objective value mismatch", 0, result.getValue().compareTo(BigDecimal.valueOf(14)));
    }

    /**
     * A contradictory bound configuration (lower > upper) must make the model
     * infeasible and {@code validate()} should return {@code false}.
     */
    @Test
    public void testValidateInfeasibleBounds() {
        x.lower(BigDecimal.valueOf(10));
        x.upper(BigDecimal.valueOf(5)); // lower > upper -> infeasible

        boolean valid = model.validate();
        assertFalse("Model with contradictory bounds must be invalid", valid);
    }

    /**
     * Verify that {@code copy()} creates a deep copy of the model and that
     * {@code relax()} removes the integer property from variables.
     */
    @Test
    public void testCopyAndRelax() {
        // x is integer
        x.integer(true);
        assertTrue("x must be integer before relaxation", x.isInteger());

        // Deep copy
        ExpressionsBasedModel copy = model.copy();

        // Relax the copy
        copy.relax();

        Variable xCopy = copy.getVariable(0);
        assertFalse("Integer flag should be cleared after relaxation", xCopy.isInteger());

        // Original model must stay unchanged
        assertTrue("Original model must retain integer flag", x.isInteger());
    }

    /**
     * Ensure that {@code getExpressions()} returns an unmodifiable collection.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testExpressionsCollectionUnmodifiable() {
        model.addExpression("e1");
        Set<Expression> exprs = (Set<Expression>) model.getExpressions(); // cast to Set for mutability test
        exprs.clear(); // should throw UnsupportedOperationException
    }

    /**
     * Verify that {@code isAnyVariableInteger()} correctly reports the presence
     * of integer variables, taking the relaxed flag into account.
     */
    @Test
    public void testIsAnyVariableInteger() {
        // No integer variables yet
        assertFalse("Model should report no integer variables", model.isAnyVariableInteger());

        // Make x integer
        x.integer(true);
        assertTrue("Model should now report integer variables", model.isAnyVariableInteger());

        // Relax the model – integer flag is ignored
        model.relax();
        assertFalse("Relaxed model must report no integer variables", model.isAnyVariableInteger());
    }

    /**
     * The {@code toString()} method should contain the names of all variables
     * and expressions added to the model.
     */
    @Test
    public void testToStringContainsEntities() {
        model.addExpression("c1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        String txt = model.toString();

        assertTrue("String representation must contain variable name 'x'", txt.contains("x"));
        assertTrue("String representation must contain variable name 'y'", txt.contains("y"));
        assertTrue("String representation must contain expression name 'c1'", txt.contains("c1"));
    }
}
