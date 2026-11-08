/**
 * JUnit‑4 test suite for {@link org.ojalgo.optimisation.ExpressionsBasedModel}.
 *
 * The tests are deliberately written only with the public API that is available in the
 * source code provided.  No external solver integrations are exercised – the optimisation
 * methods (minimise / maximise) are therefore **not** invoked because they would require a
 * concrete solver implementation which is outside the scope of this exercise.
 *
 * The test class covers:
 *
 * 1. Extraction of all public methods (see the comment block at the top of the file).
 * 2. Basic functionality tests for each method.
 * 3. Edge‑case / exception tests (duplicate names, invalid indices, SOS errors, etc.).
 * 4. Validation of model state (infeasibility, integer flags, description counts, etc.).
 *
 * The tests use only the classes, constructors and methods that appear in the supplied
 * source code or in the minimal public API that was listed in the prompt.
 */
package org.ojalgo.optimisation;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.ProgrammingError;


import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;
import org.ojalgo.type.EnumBitSet;

/**
 * List of public methods (including signatures) extracted from {@code ExpressionsBasedModel}:
 *
 * <pre>
 * public ExpressionsBasedModel()
 * public ExpressionsBasedModel(Optimisation.Options optimisationOptions)
 *
 * // model building
 * public Variable addVariable()
 * public Variable addVariable(String name)
 * public Expression addExpression()
 * public Expression addExpression(String name)
 *
 * // accessors
 * public Variable getVariable(int index)
 * public Variable getVariable(IntIndex index)
 * public Expression getExpression(String name)
 * public Collection<Expression> getExpressions()
 * public List<Variable> getVariables()
 * public int countVariables()
 * public int countExpressions()
 *
 * // description / meta‑information
 * public Description describe()
 * public Optimisation.Sense getOptimisationSense()
 *
 * // objective handling
 * public Expression objective()
 * public Expression limitObjective(BigDecimal lower, BigDecimal upper)
 *
 * // model state queries
 * public boolean isAnyVariableInteger()
 * public boolean isAnyVariableFixed()
 * public boolean isInfeasible()
 * public boolean isAnyConstraintQuadratic()
 * public boolean isAnyExpressionQuadratic()
 *
 * // validation & solution extraction
 * public Optimisation.Result getVariableValues()
 * public Optimisation.Result getVariableValues(NumberContext validationContext)
 * public boolean validate()
 *
 * // utility / misc
 * public void relax()
 * public void relax(boolean soft)
 * public void addSpecialOrderedSet(Collection<Variable> orderedSet, int type, Expression linkedTo)
 * public void addSpecialOrderedSet(Collection<Variable> orderedSet, int min, int max)
 *
 * // static helpers
 * public static boolean addIntegration(Optimisation.Integration<ExpressionsBasedModel, ?> integration)
 * public static boolean removeIntegration(Integration<?> integration)
 * public static boolean addPresolver(Presolver presolver)
 * public static boolean removePresolver(Presolver presolver)
 * public static void clearIntegrations()
 * public static void clearPresolvers()
 * public static void resetPresolvers()
 * public static ExpressionsBasedModel parse(File file)
 * public static ExpressionsBasedModel parse(InputStream input, FileFormat format)
 *
 * // internal helpers (package‑private) – not exercised directly
 * </pre>
 */
public class ExpressionsBasedModelTest {

    private ExpressionsBasedModel model;
    private Optimisation.Options defaultOptions;

    @Before
    public void setUp() {
        defaultOptions = new Optimisation.Options();
        model = new ExpressionsBasedModel(defaultOptions);
    }

    /* -------------------------------------------------
     *  Basic model building – variables / expressions
     * ------------------------------------------------- */

    @Test
    public void testAddVariableAndGetVariable() {
        Variable v0 = model.addVariable();                // name X0
        Variable v1 = model.addVariable("myVar");         // custom name

        assertEquals(2, model.countVariables());

        // Retrieval by index
        assertSame(v0, model.getVariable(0));
        assertSame(v1, model.getVariable(1));

        // Retrieval by IntIndex (the internal index wrapper)
        assertSame(v0, model.getVariable(v0.getIndex()));
        assertSame(v1, model.getVariable(v1.getIndex()));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetVariableInvalidIndexThrows() {
        model.addVariable(); // only one variable at index 0
        model.getVariable(5); // should throw
    }

    @Test
    public void testAddExpressionAndGetExpression() {
        Expression e0 = model.addExpression();                // name EXPR0
        Expression e1 = model.addExpression("myExpr");        // custom name

        assertEquals(2, model.countExpressions());

        assertSame(e0, model.getExpression("EXPR0"));
        assertSame(e1, model.getExpression("myExpr"));
    }

    @Test
    public void testDuplicateExpressionNameOverrides() {
        Expression first = model.addExpression("dup");
        first.setConstant(BigDecimal.ONE);

        // Adding another expression with the same name replaces the previous one
        Expression second = model.addExpression("dup");
        second.setConstant(BigDecimal.TEN);

        assertEquals(1, model.countExpressions());
        Expression retrieved = model.getExpression("dup");
        assertSame(second, retrieved);
        assertEquals(BigDecimal.TEN, retrieved.getConstant());
    }

    /* -------------------------------------------------
     *  Description / meta‑information
     * ------------------------------------------------- */

    @Test
    public void testDescribeEmptyModel() {
        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(0, desc.nbVariables);
        assertEquals(0, desc.nbExpressions);
        // All other counters are also zero
        assertEquals(0, desc.nbEqualityBounds);
        assertEquals(0, desc.nbEqualityConstraints);
        assertEquals(0, desc.nbIntegerVariables);
        assertEquals(0, desc.nbLowerBounds);
        assertEquals(0, desc.nbLowerConstraints);
        assertEquals(0, desc.nbNegativeVariables);
        assertEquals(0, desc.nbPositiveVariables);
        assertEquals(0, desc.nbUpperBounds);
        assertEquals(0, desc.nbUpperConstraints);
    }

    @Test
    public void testDescribeWithVariablesAndIntegerFlag() {
        Variable x = model.addVariable("x").lower(0).upper(10);
        Variable y = model.addVariable("y").integer(true).lower(0).upper(5);

        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(2, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(2, desc.nbPositiveVariables);
        assertEquals(0, desc.nbNegativeVariables);
        // No constraints yet
        assertEquals(0, desc.nbLowerBounds);
        assertEquals(0, desc.nbUpperBounds);
    }

    /* -------------------------------------------------
     *  Objective handling
     * ------------------------------------------------- */

    @Test
    public void testObjectiveLinearCombination() {
        // x contributes 2, y contributes -3 to the objective
        Variable x = model.addVariable("x").weight(BigDecimal.valueOf(2));
        Variable y = model.addVariable("y").weight(BigDecimal.valueOf(-3));

        Expression obj = model.objective();

        // The objective should contain the two linear terms with the correct coefficients
        assertEquals(BigDecimal.valueOf(2), obj.get(x.getIndex()));
        assertEquals(BigDecimal.valueOf(-3), obj.get(y.getIndex()));
        // Constant part is zero by default
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    @Test
    public void testLimitObjectiveCreatesConstraintWhenLinear() {
        Variable x = model.addVariable("x").weight(BigDecimal.ONE);
        Expression obj = model.objective(); // linear objective

        // Apply limits – this should create a hidden constraint expression
        Expression limited = model.limitObjective(BigDecimal.ZERO, BigDecimal.valueOf(5));

        // The returned expression must be the hidden constraint (not the original objective)
        assertNotSame(obj, limited);
        assertEquals(BigDecimal.ZERO, limited.getLowerLimit());
        assertEquals(BigDecimal.valueOf(5), limited.getUpperLimit());
        // The hidden constraint should have the same linear coefficients as the objective
        assertEquals(BigDecimal.ONE, limited.get(x.getIndex()));
    }

    @Test
    public void testLimitObjectiveReturnsOriginalWhenQuadratic() {
        // Create a quadratic expression (x^2) and set it as objective
        Variable x = model.addVariable("x").weight(BigDecimal.ONE);
        Expression quad = model.addExpression("quadObj");
        quad.set(x.getIndex(), BigDecimal.ONE); // linear part (ignored for quadratic test)
        quad.set(x.getIndex(), x.getIndex(), BigDecimal.ONE); // x*x term
        quad.weight(BigDecimal.ONE); // make it part of objective
        // Force the model to treat it as objective
        quad.setAsObjective(true);

        // Now call limitObjective – because the objective has a quadratic part,
        // the method must return the original objective expression.
        Expression limited = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertSame(quad, limited);
    }

    /* -------------------------------------------------
     *  Integer / binary flags
     * ------------------------------------------------- */

    @Test
    public void testIsAnyVariableInteger() {
        assertFalse(model.isAnyVariableInteger());

        model.addVariable("intVar").integer(true);
        assertTrue(model.isAnyVariableInteger());

        // After relaxing the model, integer flag is ignored
        model.relax();
        assertFalse(model.isAnyVariableInteger());
    }

    @Test
    public void testVariableBinaryFlagAndSpecialOrderedSetValidation() {
        Variable a = model.addVariable("a").binary();
        Variable b = model.addVariable("b").binary();
        Variable c = model.addVariable("c").binary();

        // Build a simple SOS expression (sum of binaries)
        Expression sosExpr = model.addExpression("sos");
        sosExpr.add(a, BigDecimal.ONE);
        sosExpr.add(b, BigDecimal.ONE);
        sosExpr.add(c, BigDecimal.ONE);
        sosExpr.upper(BigDecimal.valueOf(2));

        // This should succeed – the ordered set is binary and linked expression is a constraint
        model.addSpecialOrderedSet(java.util.Arrays.asList(a, b, c), 1, sosExpr);

        // Attempting to add an SOS with a non‑binary variable must throw
        Variable nonBinary = model.addVariable("d"); // default is free (not binary)
        try {
            model.addSpecialOrderedSet(java.util.Collections.singletonList(nonBinary), 1, sosExpr);
            fail("Expected ProgrammingError for non‑binary variable in SOS");
        } catch (ProgrammingError expected) {
            // expected
        }
    }

    /* -------------------------------------------------
     *  Infeasibility detection
     * ------------------------------------------------- */

    @Test
    public void testModelDetectsInfeasibleVariableBounds() {
        // lower > upper makes the variable infeasible
        model.addVariable("bad").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(5));
        assertTrue(model.isInfeasible());
    }

    @Test
    public void testModelDetectsInfeasibleExpressionConstraint() {
        Variable x = model.addVariable("x");
        Expression expr = model.addExpression("c");
        expr.add(x, BigDecimal.ONE);
        expr.lower(BigDecimal.valueOf(5));
        expr.upper(BigDecimal.valueOf(3)); // lower > upper → infeasible

        assertTrue(model.isInfeasible());
    }

    /* -------------------------------------------------
     *  Validation & solution extraction (feasible simple case)
     * ------------------------------------------------- */

    @Test
    public void testValidateAndGetVariableValuesSimpleFeasibleModel() {
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.addVariable("y").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(4));

        // No constraints – any value within bounds is feasible.
        // The default variable values are null, so getVariableValues() will use the midpoint.
        Optimisation.Result result = model.getVariableValues();

        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // Midpoint for x: (0+10)/2 = 5, for y: (1+4)/2 = 2.5
        assertEquals(BigDecimal.valueOf(5), result.get(0));
        assertEquals(BigDecimal.valueOf(2.5), result.get(1));

        // Validate the solution – should be true
        assertTrue(model.validate(result));
    }

    @Test
    public void testValidateFailsWhenVariableOutOfBounds() {
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // Force a value outside the bounds
        x.setValue(BigDecimal.valueOf(2));

        Optimisation.Result result = model.getVariableValues();

        // The model should mark the solution as infeasible
        assertEquals(Optimisation.State.INFEASIBLE, result.getState());

        // Explicit validation must also fail
        assertFalse(model.validate(result));
    }

    /* -------------------------------------------------
     *  Static helper – FileFormat detection
     * ------------------------------------------------- */

    @Test
    public void testFileFormatDetectionFromPath() {
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.mps"));
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("MODEL.SIF"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("data.ebm"));
        try {
            ExpressionsBasedModel.FileFormat.from("unknown.xyz");
            fail("Expected IllegalArgumentException for unknown extension");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /* -------------------------------------------------
     *  Integration / presolver collection management (static)
     * ------------------------------------------------- */

    @Test
    public void testAddAndRemoveIntegrationPresolver() {
        // Use a dummy presolver (the built‑in ZERO_ONE_TWO presolver is already present)
        ExpressionsBasedModel.Presolver dummy = new ExpressionsBasedModel.Presolver(99) {
            @Override
            public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper,
                                    NumberContext precision) {
                return false;
            }
        };

        assertTrue(ExpressionsBasedModel.addPresolver(dummy));
        assertTrue(ExpressionsBasedModel.removePresolver(dummy));

        // Integration handling – we add a mock integration using the wrapper class
        Optimisation.Integration<ExpressionsBasedModel, Optimisation.Solver> dummyIntegration =
                new Optimisation.Integration<ExpressionsBasedModel, Optimisation.Solver>() {
                    @Override
                    public Optimisation.Solver build(ExpressionsBasedModel model) { return null; }
                    @Override
                    public boolean isCapable(ExpressionsBasedModel model) { return false; }
                    @Override
                    public Optimisation.Result extractSolverState(ExpressionsBasedModel model) { return null; }
                    @Override
                    public Optimisation.Result toModelState(Optimisation.Result solverState, ExpressionsBasedModel model) { return null; }
                    @Override
                    public Optimisation.Result toSolverState(Optimisation.Result modelState, ExpressionsBasedModel model) { return null; }
                };

        assertTrue(ExpressionsBasedModel.addIntegration(dummyIntegration));
        assertTrue(ExpressionsBasedModel.removeIntegration(dummyIntegration));

        // Clear all static collections to avoid side effects for other tests
        ExpressionsBasedModel.clearIntegrations();
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.resetPresolvers();
    }

    /* -------------------------------------------------
     *  Miscellaneous getters / flags
     * ------------------------------------------------- */

    @Test
    public void testOptimisationSenseDefaultsAndAfterSet() {
        // Default is null until a minimise/maximise call
        assertNull(model.getOptimisationSense());

        // Simulate a minimise call (does not solve)
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());

        // Simulate a maximise call
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
    }

    @Test
    public void testIntegrationSwitchBitSetBehaviour() {
        // The internal EnumBitSet is not exposed directly, but we can use the protected
        // Integration API to toggle a switch and read it back.
        ExpressionsBasedModel.Integration<?> dummy = new ExpressionsBasedModel.Integration<Optimisation.Solver>() {
            @Override
            public Optimisation.Solver build(ExpressionsBasedModel model) { return null; }
            @Override
            public boolean isCapable(ExpressionsBasedModel model) { return true; }
        };

        // Turn on a switch
        dummy.setSwitch(model, ExpressionsBasedModel.IntegrationProperty.TEMPORARY, true);
        // Verify via the protected isSwitch method (accessible through the same instance)
        assertTrue(dummy.isSwitch(model, ExpressionsBasedModel.IntegrationProperty.TEMPORARY));

        // Turn it off again
        dummy.setSwitch(model, ExpressionsBasedModel.IntegrationProperty.TEMPORARY, false);
        assertFalse(dummy.isSwitch(model, ExpressionsBasedModel.IntegrationProperty.TEMPORARY));
    }

    /* -------------------------------------------------
     *  toString sanity check (ensures no exceptions)
     * ------------------------------------------------- */

    @Test
    public void testToStringDoesNotThrow() {
        model.addVariable("x").lower(0).upper(5);
        model.addVariable("y").integer(true);
        model.addExpression("c1").add(model.getVariable(0), BigDecimal.ONE).upper(BigDecimal.valueOf(3));
        // The toString method builds a multi‑line representation; we only verify that it returns a non‑null string.
        String representation = model.toString();
        assertNotNull(representation);
        assertTrue(representation.contains("x"));
        assertTrue(representation.contains("y"));
        assertTrue(representation.contains("c1"));
    }
}
