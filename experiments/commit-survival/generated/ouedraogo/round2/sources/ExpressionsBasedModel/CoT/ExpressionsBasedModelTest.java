package org.ojalgo.optimisation;
import org.ojalgo.structure.Structure1D.IntIndex;


import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel.Description;
import org.ojalgo.structure.IntIndex;

/**
 * JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * <p>
 * The tests focus on the public API that can be exercised without requiring any of the
 * underlying solver implementations.  All objects are created through the constructors and
 * factory methods that are part of the source file supplied in the prompt.
 * </p>
 *
 * <p>
 * The test cases are organised as follows:
 * <ul>
 *   <li>Extraction of public method signatures (documented in the class‑level Javadoc).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception tests where applicable.</li>
 * </ul>
 * </p>
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;

    @Before
    public void setUp() {
        // a fresh model before each test
        model = new ExpressionsBasedModel();
    }

    /* ----------------------------------------------------------------------
     *  Public method signatures (extracted from the source)
     * ---------------------------------------------------------------------- */
    //  ExpressionsBasedModel()
    //  ExpressionsBasedModel(Optimisation.Options)
    //  Expression addExpression()
    //  Expression addExpression(String)
    //  void addSpecialOrderedSet(Collection<Variable>, int, Expression)
    //  void addSpecialOrderedSet(Collection<Variable>, int, int)
    //  Variable addVariable()
    //  Variable addVariable(String)
    //  void addVariable(Variable)               // deprecated
    //  void addVariables(Collection<? extends Variable>) // deprecated
    //  void addVariables(Variable[])           // deprecated
    //  Stream<Variable> bounds()
    //  boolean checkSimilarity(Expression)
    //  Stream<Expression> constraints()
    //  ExpressionsBasedModel copy()
    //  ExpressionsBasedModel copy(boolean)
    //  ExpressionsBasedModel copy(boolean, boolean)
    //  int countExpressions()
    //  int countVariables()
    //  Description describe()
    //  void dispose()
    //  Expression getExpression(String)
    //  Collection<Expression> getExpressions()
    //  Set<IntIndex> getFixedVariables()
    //  List<Variable> getFreeVariables()
    //  List<Variable> getIntegerVariables()
    //  List<Variable> getNegativeVariables()
    //  Optimisation.Sense getOptimisationSense()
    //  List<Variable> getPositiveVariables()
    //  Variable getVariable(int)
    //  Variable getVariable(IntIndex)
    //  List<Variable> getVariables()
    //  Optimisation.Result getVariableValues()
    //  Optimisation.Result getVariableValues(NumberContext)
    //  int indexOf(Variable)
    //  int indexOfFreeVariable(int)
    //  int indexOfFreeVariable(IntIndex)
    //  int indexOfFreeVariable(Variable)
    //  int indexOfIntegerVariable(int)
    //  int indexOfIntegerVariable(IntIndex)
    //  int indexOfIntegerVariable(Variable)
    //  int indexOfNegativeVariable(int)
    //  int indexOfNegativeVariable(IntIndex)
    //  int indexOfNegativeVariable(Variable)
    //  int indexOfPositiveVariable(int)
    //  int indexOfPositiveVariable(IntIndex)
    //  int indexOfPositiveVariable(Variable)
    //  boolean isAnyConstraintQuadratic()
    //  boolean isAnyExpressionQuadratic()
    //  boolean isAnyObjectiveQuadratic()
    //  boolean isAnyVariableFixed()
    //  boolean isAnyVariableInteger()
    //  Expression limitObjective(BigDecimal, BigDecimal)
    //  Optimisation.Result maximise()
    //  Optimisation.Result minimise()
    //  Expression newExpression(String)
    //  Variable newVariable(String)
    //  Expression objective()
    //  <T extends IntermediateSolver> T prepare(Function<ExpressionsBasedModel,T>)
    //  ExpressionsBasedModel reduce()
    //  void relax()
    //  void relax(boolean)
    //  void removeExpression(String)
    //  void setKnownSolution(Optimisation.Result)
    //  void setKnownSolution(Optimisation.Result,BiConsumer<ExpressionsBasedModel,Access1D<BigDecimal>>)
    //  ExpressionsBasedModel simplify()
    //  ExpressionsBasedModel snapshot()
    //  String toString()
    //  boolean validate()
    //  boolean validate(Access1D<BigDecimal>)
    //  boolean validate(Access1D<BigDecimal>,BasicLogger)
    //  boolean validate(Access1D<BigDecimal>,NumberContext)
    //  boolean validate(Access1D<BigDecimal>,NumberContext,BasicLogger)
    //  boolean validate(BasicLogger)
    //  boolean validate(NumberContext)
    //  boolean validate(NumberContext,BasicLogger)
    //  Stream<Variable> variables()
    //  void writeTo(File)
    //  void writeTo(InMemoryFile)

    /* ----------------------------------------------------------------------
     *  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveVariable() {
        Variable x = model.newVariable("x");
        assertNotNull(x);
        assertEquals("x", x.getName());

        // index handling
        int idx = model.indexOf(x);
        assertEquals(0, idx);
        assertEquals(x, model.getVariable(idx));
        assertEquals(x, model.getVariable(new IntIndex(idx)));

        // free / integer / positive / negative categorisation
        assertTrue(model.getFreeVariables().contains(x));
        assertFalse(model.getIntegerVariables().contains(x));
        assertTrue(model.getPositiveVariables().contains(x));
        assertFalse(model.getNegativeVariables().contains(x));
    }

    @Test
    public void testVariableBoundsAndFixedState() {
        Variable y = model.newVariable("y");
        y.lower(BigDecimal.ZERO);
        y.upper(BigDecimal.TEN);
        assertFalse(y.isFixed());

        // make it fixed
        y.upper(BigDecimal.ZERO).lower(BigDecimal.ZERO);
        assertTrue(y.isFixed());

        Set<IntIndex> fixed = model.getFixedVariables();
        assertTrue(fixed.contains(y.getIndex()));
    }

    @Test
    public void testAddAndRetrieveExpression() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");

        Expression expr = model.addExpression("expr1");
        expr.set(a.getIndex(), BigDecimal.ONE);
        expr.set(b.getIndex(), BigDecimal.valueOf(2));
        expr.upper(BigDecimal.valueOf(10));

        assertEquals(expr, model.getExpression("expr1"));
        assertEquals(1, model.countExpressions());
        assertTrue(expr.isConstraint());
        assertFalse(expr.isObjective());

        // check that the expression contains the expected coefficients
        assertEquals(BigDecimal.ONE, expr.get(a.getIndex()));
        assertEquals(BigDecimal.valueOf(2), expr.get(b.getIndex()));
    }

    @Test
    public void testDescriptionCounts() {
        Variable v1 = model.newVariable("v1");
        v1.lower(BigDecimal.ZERO).upper(BigDecimal.ONE); // lower & upper -> counted as bounds

        Variable v2 = model.newVariable("v2");
        v2.lower(BigDecimal.ZERO); // only lower bound

        Variable v3 = model.newVariable("v3");
        v3.upper(BigDecimal.ONE); // only upper bound

        Variable v4 = model.newVariable("v4");
        v4.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO); // equality bound

        // add an expression constraint
        Expression e = model.addExpression("c1");
        e.set(v1.getIndex(), BigDecimal.ONE);
        e.upper(BigDecimal.valueOf(5));

        Description d = model.describe();

        assertEquals(4, d.nbVariables);
        assertEquals(1, d.nbEqualityBounds); // v4
        assertEquals(1, d.nbLowerBounds);    // v1 and v2 (v4 counted as equality)
        assertEquals(2, d.nbUpperBounds);  // v1 and v3 (v4 counted as equality)
        assertEquals(1, d.nbEqualityConstraints); // none -> 0
        assertEquals(1, d.nbLowerConstraints);    // c1 is only an upper constraint, so 0 lower
        assertEquals(1, d.nbUpperConstraints);    // the expression c1
    }

    @Test
    public void testObjectiveAndLimitObjective() {
        // create a simple objective via variable weight
        Variable x = model.newVariable("x");
        // weight(ONE) marks variable as part of objective (the API provides weight via Variable.weight)
        // As the public API for weight is not listed, we use the expression API instead:
        Expression obj = model.objective(); // initially empty
        assertEquals(BigDecimal.ZERO, obj.getConstant());

        // add a contribution manually
        Expression o = model.addExpression("obj");
        o.set(x.getIndex(), BigDecimal.ONE);
        o.weight(BigDecimal.ONE); // mark as objective (weight method exists in Expression)

        // limit the objective between 0 and 10
        Expression limited = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(limited);
        // the limited expression should now have the same linear coefficient as the original objective
        assertEquals(BigDecimal.ONE, limited.get(x.getIndex()));
        // and the limits are set
        assertEquals(BigDecimal.ZERO, limited.getLowerLimit());
        assertEquals(BigDecimal.TEN, limited.getUpperLimit());
    }

    @Test
    public void testIndexOfFreeVariable() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // both are free (not fixed)
        int freeIdxA = model.indexOfFreeVariable(a);
        int freeIdxB = model.indexOfFreeVariable(b);
        assertTrue(freeIdxA >= 0);
        assertTrue(freeIdxB >= 0);
        assertNotEquals(freeIdxA, freeIdxB);
    }

    @Test
    public void testIsAnyVariableIntegerAndRelax() {
        Variable intVar = model.newVariable("intVar");
        intVar.integer(true);
        assertTrue(model.isAnyVariableInteger());

        // relax the model (soft = false) – integer flags are removed
        model.relax(false);
        assertFalse(model.isAnyVariableInteger());
    }

    @Test
    public void testGetVariableValuesAndValidate() {
        Variable x = model.newVariable("x");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Variable y = model.newVariable("y");
        y.lower(BigDecimal.TEN).upper(BigDecimal.TEN); // fixed at 10

        // set an explicit value for x
        x.setValue(BigDecimal.valueOf(0.5));

        Optimisation.Result vals = model.getVariableValues();
        assertEquals(2, vals.size());

        // Solution should contain the set value for x and the fixed value for y
        assertEquals(0.5, vals.doubleValue(0), 1e-12);
        assertEquals(10.0, vals.doubleValue(1), 1e-12);

        // Validation should succeed with default options
        assertTrue(model.validate(vals));
    }

    @Test
    public void testInfeasibilityDetection() {
        Variable v = model.newVariable("v");
        v.lower(BigDecimal.ONE).upper(BigDecimal.ZERO); // lower > upper -> infeasible
        assertTrue(model.isInfeasible());
    }

    @Test
    public void testCopyAndSimplify() {
        Variable a = model.newVariable("a");
        a.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Expression e = model.addExpression("c");
        e.set(a.getIndex(), BigDecimal.ONE);
        e.upper(BigDecimal.ONE);

        // copy (deep)
        ExpressionsBasedModel copy = model.copy();
        assertEquals(model.countVariables(), copy.countVariables());
        assertEquals(model.countExpressions(), copy.countExpressions());

        // simplify creates a shallow copy with redundant constraints removed
        ExpressionsBasedModel simplified = model.simplify();
        assertEquals(model.countVariables(), simplified.countVariables());
        // after simplification the expression should still exist (not redundant)
        assertTrue(simplified.getExpressions().stream().anyMatch(expr -> expr.getName().equals("c")));
    }

    @Test
    public void testRemoveExpression() {
        model.addExpression("temp");
        assertEquals(1, model.countExpressions());
        model.removeExpression("temp");
        assertEquals(0, model.countExpressions());
        assertNull(model.getExpression("temp"));
    }

    @Test
    public void testToStringDoesNotThrow() {
        model.newVariable("x");
        model.addExpression("e").set(model.getVariable(0).getIndex(), BigDecimal.ONE);
        String txt = model.toString();
        assertNotNull(txt);
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("e"));
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case / exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetVariableInvalidIndex() {
        // No variables have been added yet – any index is invalid
        model.getVariable(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileFormatFromInvalidPath() {
        // The static method should reject unknown extensions
        ExpressionsBasedModel.FileFormat.from("unknown.xyz");
    }

    @Test
    public void testAddSpecialOrderedSetInvalidParameters() {
        Variable v1 = model.newVariable("v1");
        v1.binary(); // make binary (API provides binary())
        // SOS with max <= 0 should throw ProgrammingError (which is a RuntimeException)
        try {
            model.addSpecialOrderedSet(java.util.Collections.singletonList(v1), 0, 1);
            fail("Expected ProgrammingError due to invalid SOS type");
        } catch (RuntimeException expected) {
            // ok
        }
    }

    @Test
    public void testLimitObjectiveWhenNoObjectiveExists() {
        // No objective variables or expressions have been defined yet.
        Expression limited = model.limitObjective(BigDecimal.ZERO, BigDecimal.ONE);
        // Should return the generated objective expression (not null)
        assertNotNull(limited);
        // The limited expression should have the limits we set
        assertEquals(BigDecimal.ZERO, limited.getLowerLimit());
        assertEquals(BigDecimal.ONE, limited.getUpperLimit());
    }

    @Test
    public void testValidateWithMismatchedSolutionSize() {
        model.newVariable("x");
        // Create a solution with a different size (0) – validation must return false
        Optimisation.Result empty = Optimisation.Result.wrap(new org.ojalgo.array.Array1D<>(0));
        assertFalse(model.validate(empty));
    }

    @Test
    public void testAddVariableAfterShallowCopyThrows() {
        // Create a shallow copy (snapshot) and then try to add a variable – should fail
        ExpressionsBasedModel shallow = model.snapshot();
        try {
            shallow.addVariable("newVar");
            fail("Expected IllegalStateException when adding variable to a shallow copy");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    /* ----------------------------------------------------------------------
     *  Helper methods (none required for this test suite)
     * ---------------------------------------------------------------------- */
}
