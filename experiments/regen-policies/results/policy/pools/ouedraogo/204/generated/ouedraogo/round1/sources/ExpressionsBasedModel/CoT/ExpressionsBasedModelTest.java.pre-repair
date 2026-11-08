package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API – creation, addition of variables / expressions, lookup and counting.</li>
 *   <li>Utility methods – index conversions, description, feasibility checks, etc.</li>
 *   <li>Edge‑case handling – contradictory bounds, shallow‑copy restrictions, invalid SOS usage.</li>
 * </ul>
 *
 * Only the public API that is present in the supplied source code is used – no mocking or
 * subclassing of internal abstract classes.
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel();
    }

    /* --------------------------------------------------------------------- */
    /*  List of public methods (non‑static) – extracted from the source file */
    /* --------------------------------------------------------------------- *
     *
     * ExpressionsBasedModel()
     * ExpressionsBasedModel(Optimisation.Options)
     * ExpressionsBasedModel copy()
     * ExpressionsBasedModel copy(boolean relax)
     * ExpressionsBasedModel copy(boolean shallow, boolean prune)
     * int countExpressions()
     * int countVariables()
     * Description describe()
     * void dispose()
     * Expression getExpression(String)
     * Collection<Expression> getExpressions()
     * Set<IntIndex> getFixedVariables()
     * List<Variable> getFreeVariables()
     * List<Variable> getIntegerVariables()
     * List<Variable> getNegativeVariables()
     * Optimisation.Sense getOptimisationSense()
     * List<Variable> getPositiveVariables()
     * Variable getVariable(int)
     * Variable getVariable(IntIndex)
     * List<Variable> getVariables()
     * Optimisation.Result getVariableValues()
     * Optimisation.Result getVariableValues(NumberContext)
     * int indexOf(Variable)
     * int indexOfFreeVariable(int)
     * int indexOfFreeVariable(IntIndex)
     * int indexOfFreeVariable(Variable)
     * int indexOfIntegerVariable(int)
     * int indexOfIntegerVariable(IntIndex)
     * int indexOfIntegerVariable(Variable)
     * int indexOfNegativeVariable(int)
     * int indexOfNegativeVariable(IntIndex)
     * int indexOfNegativeVariable(Variable)
     * int indexOfPositiveVariable(int)
     * int indexOfPositiveVariable(IntIndex)
     * int indexOfPositiveVariable(Variable)
     * boolean isAnyConstraintQuadratic()
     * boolean isAnyExpressionQuadratic()
     * boolean isAnyObjectiveQuadratic()
     * boolean isAnyVariableFixed()
     * boolean isAnyVariableInteger()
     * Expression limitObjective(BigDecimal, BigDecimal)
     * Optimisation.Result maximise()
     * Optimisation.Result minimise()
     * Expression newExpression(String)
     * Variable newVariable(String)
     * Expression objective()
     * <T extends IntermediateSolver> T prepare(Function<ExpressionsBasedModel,T>)
     * ExpressionsBasedModel reduce()
     * void relax()
     * void relax(boolean)
     * void removeExpression(String)
     * void setKnownSolution(Optimisation.Result)
     * void setKnownSolution(Optimisation.Result, BiConsumer<ExpressionsBasedModel,Access1D<BigDecimal>>)
     * ExpressionsBasedModel simplify()
     * ExpressionsBasedModel snapshot()
     * String toString()
     * boolean validate()
     * boolean validate(Access1D<BigDecimal>)
     * boolean validate(Access1D<BigDecimal>, BasicLogger)
     * boolean validate(Access1D<BigDecimal>, NumberContext)
     * boolean validate(Access1D<BigDecimal>, NumberContext, BasicLogger)
     * boolean validate(BasicLogger)
     * boolean validate(NumberContext)
     * boolean validate(NumberContext, BasicLogger)
     * Stream<Variable> variables()
     * void writeTo(File)
     * void writeTo(InMemoryFile)
     *
     * --------------------------------------------------------------------- */

    /* --------------------------------------------------------------------- */
    /*  1. Basic creation and counting                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddVariableAndExpressionCounts() {
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());

        Variable v1 = model.addVariable("x1");
        Variable v2 = model.addVariable("x2");
        assertEquals(2, model.countVariables());

        Expression e1 = model.addExpression("c1");
        Expression e2 = model.addExpression("c2");
        assertEquals(2, model.countExpressions());

        // Lookup by name
        assertSame(e1, model.getExpression("c1"));
        assertSame(v1, model.getVariable(0));
        assertSame(v2, model.getVariable(1));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Index conversion helpers                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIndexConversions() {
        Variable v = model.addVariable("x");
        int globalIdx = model.indexOf(v);
        assertEquals(0, globalIdx);
        assertEquals(0, model.indexOfFreeVariable(v));
        assertEquals(0, model.indexOfFreeVariable(globalIdx));
        assertEquals(-1, model.indexOfIntegerVariable(v)); // not integer yet
    }

    @Test
    public void testIntegerVariableIndices() {
        Variable v = model.addVariable("x").integer();
        assertTrue(v.isInteger());

        // after making integer the model should recognise it
        assertEquals(0, model.indexOfIntegerVariable(v));
        assertEquals(0, model.indexOfIntegerVariable(model.indexOf(v)));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Description counts                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDescribeCounts() {
        // variable 0 : lower bound only
        model.addVariable("a").lower(BigDecimal.ONE);
        // variable 1 : upper bound only
        model.addVariable("b").upper(BigDecimal.TEN);
        // variable 2 : both bounds (equality)
        model.addVariable("c").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // variable 3 : integer, free
        model.addVariable("d").integer();

        // one constraint expression
        Expression expr = model.addExpression("expr");
        expr.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));

        Description d = model.describe();

        assertEquals(4, d.nbVariables);
        assertEquals(1, d.nbIntegerVariables);
        assertEquals(2, d.nbLowerBounds);   // a and c
        assertEquals(2, d.nbUpperBounds);   // b and c
        assertEquals(1, d.nbEqualityBounds); // c (lower == upper)
        assertEquals(0, d.nbLowerConstraints);
        assertEquals(0, d.nbUpperConstraints);
        assertEquals(0, d.nbEqualityConstraints);
    }

    /* --------------------------------------------------------------------- */
    /*  4. Objective construction                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testObjectiveIsZeroWhenNoWeights() {
        // No variable contributes to the objective – the generated objective should be zero
        Expression obj = model.objective();
        assertNotNull(obj);
        // The constant part is zero (default)
        assertEquals(BigDecimal.ZERO, obj.getConstant());

        // Adding a variable with a weight should be reflected
        Variable v = model.addVariable("x");
        v.weight(BigDecimal.valueOf(2)); // weight method exists on Variable (via ModelEntity)
        Expression obj2 = model.objective();
        assertEquals(BigDecimal.valueOf(2), obj2.get(v.getIndex()));
    }

    /* --------------------------------------------------------------------- */
    /*  5. limitObjective creates a constraint expression                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testLimitObjectiveCreatesConstraint() {
        // No quadratic part – limitObjective should create a new expression
        Expression limit = model.limitObjective(BigDecimal.ONE, BigDecimal.TEN);
        assertNotNull(limit);
        assertEquals(BigDecimal.ONE, limit.getLowerLimit());
        assertEquals(BigDecimal.TEN, limit.getUpperLimit());
        // The returned expression should be the same as the one stored internally
        assertSame(limit, model.getExpression(limit.getName()));
    }

    /* --------------------------------------------------------------------- */
    /*  6. Feasibility detection (contradictory bounds)                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testModelDetectsInfeasibilityFromVariableBounds() {
        Variable v = model.addVariable("x");
        v.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3)); // lower > upper → infeasible

        assertTrue("Model should be infeasible because of contradictory bounds", model.isInfeasible());

        // getVariableValues should report an INFEASIBLE state
        Optimisation.Result result = model.getVariableValues(NumberContext.of(8));
        assertTrue("Result state must be INFEASIBLE", result.getState().isInfeasible());
    }

    /* --------------------------------------------------------------------- */
    /*  7. Shallow copy restrictions                                           */
    /* --------------------------------------------------------------------- */
    @Test(expected = IllegalStateException.class)
    public void testShallowCopyPreventsAddingVariables() {
        // Create a shallow copy (the copy method with (true, false) creates a shallow copy)
        ExpressionsBasedModel shallow = model.copy(true, false);
        // Adding a variable to a shallow copy must throw IllegalStateException
        shallow.addVariable("newVar");
    }

    /* --------------------------------------------------------------------- */
    /*  8. addSpecialOrderedSet – invalid usage throws ProgrammingError      */
    /* --------------------------------------------------------------------- */
    @Test(expected = RuntimeException.class) // ProgrammingError extends RuntimeException
    public void testAddSpecialOrderedSetInvalidThrows() {
        Variable v = model.addVariable("x"); // not binary
        // The collection must contain binary variables; this should fail
        model.addSpecialOrderedSet(Collections.singletonList(v), 0, 1);
    }

    /* --------------------------------------------------------------------- */
    /*  9. isAnyVariableInteger & isAnyVariableFixed                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testVariableIntegerAndFixedChecks() {
        Variable v1 = model.addVariable("i1").integer();
        Variable v2 = model.addVariable("i2").integer().value(BigDecimal.ONE);
        Variable v3 = model.addVariable("free");

        assertTrue(model.isAnyVariableInteger());

        // v2 is fixed (value set)
        assertTrue(model.isAnyVariableFixed());

        // After relaxing the model, integer flag should be ignored
        model.relax();
        assertFalse(model.isAnyVariableInteger());
    }

    /* --------------------------------------------------------------------- */
    /* 10. Validation of a trivial feasible model (no variables set)        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testValidateEmptyModelIsFeasible() {
        // An empty model (no variables, no expressions) is trivially feasible
        assertTrue("Empty model should validate", model.validate());
    }

    /* --------------------------------------------------------------------- */
    /* 11. getVariableValues returns INFEASIBLE when no variable info      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetVariableValuesInfeasibleWhenNoInfo() {
        model.addVariable("x"); // no bounds, no value
        Optimisation.Result res = model.getVariableValues(NumberContext.of(8));
        assertTrue(res.getState().isInfeasible());
    }

    /* --------------------------------------------------------------------- */
    /* 12. toString does not throw and contains variable names               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToStringContainsVariableNames() {
        model.addVariable("x1");
        model.addVariable("x2");
        String txt = model.toString();
        assertTrue(txt.contains("x1"));
        assertTrue(txt.contains("x2"));
    }

    /* --------------------------------------------------------------------- */
    /* 13. reduce() does not throw when there are no presolvers applicable   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testReduceDoesNotFailOnSimpleModel() {
        model.addVariable("x");
        model.addExpression("c").lower(BigDecimal.ZERO);
        // Should complete without exception
        model.reduce();
    }

    /* --------------------------------------------------------------------- */
    /* 14. relax(boolean) soft flag keeps integer flag but disables solver   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testRelaxSoftKeepsIntegerFlag() {
        Variable v = model.addVariable("i").integer();
        assertTrue(v.isInteger());

        model.relax(true); // soft relax
        // Integer flag stays, but model reports not integer for solver selection
        assertTrue(v.isInteger());
        assertFalse("soft relaxation should make model report no integer variables",
                model.isAnyVariableInteger());
    }

    /* --------------------------------------------------------------------- */
    /* 15. getVariableValues returns a feasible solution when bounds are set */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetVariableValuesFeasibleWhenBoundsDefined() {
        Variable v = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Optimisation.Result result = model.getVariableValues(NumberContext.of(8));
        assertTrue("Result should be feasible", result.getState().isFeasible());
        // With symmetric bounds the default value is the midpoint (5)
        assertEquals(BigDecimal.valueOf(5), result.get(0));
    }
}
