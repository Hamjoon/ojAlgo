/**
 * JUnit‑4 test suite for {@link org.ojalgo.optimisation.ExpressionsBasedModel}.
 *
 * <p>
 * The test class follows the workflow described in the task:
 * <ol>
 *   <li>List all public methods (see the large comment block at the top of the file).</li>
 *   <li>Provide a basic test case for each method that exercises the normal behaviour.</li>
 *   <li>Identify edge‑cases / exceptional situations and add dedicated tests for them.</li>
 *   <li>All tests are merged into a single JUnit‑4 test file.</li>
 * </ol>
 *
 * <p>
 * The tests use only the public API that is present in the supplied source code and its
 * documented dependencies. No mocking framework is used – real objects are constructed.
 * </p>
 *
 * @author ChatGPT
 */
package org.ojalgo.optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.array.Array1D;
import org.ojalgo.optimisation.IntermediateSolver;
import org.ojalgo.ProgrammingError;


import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.InMemoryFile;
import org.ojalgo.type.context.NumberContext;

/**
 * Test class for {@link ExpressionsBasedModel}.
 */
public class ExpressionsBasedModelTest {

    /** A fresh model for each test method. */
    private ExpressionsBasedModel model;

    /** Temporary file used by {@code parse(File)} and {@code writeTo(File)} tests. */
    private File tempFile;

    @Before
    public void setUp() {
        model = new ExpressionsBasedModel(); // default options
        tempFile = null;
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Creates a tiny model with two variables and one simple expression. */
    private void createTinyModel() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        // make a simple constraint: x + y <= 10
        Expression expr = model.addExpression("c1");
        expr.add(x, BigMath.ONE);
        expr.add(y, BigMath.ONE);
        expr.upper(BigDecimal.valueOf(10));
        // set a weight for the objective (minimise x + 2*y)
        x.weight(BigMath.ONE);
        y.weight(BigMath.valueOf(2));
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Static factory / registration methods                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddIntegrationAndRemoveIntegration() {
        // Use a dummy integration – the library already provides a concrete one:
        ExpressionsBasedModel.Integration<?> intg = LinearSolver.INTEGRATION;
        assertTrue(ExpressionsBasedModel.addIntegration(intg));
        assertTrue(ExpressionsBasedModel.removeIntegration(intg));
    }

    @Test
    public void testAddPresolverAndRemovePresolver() {
        // Presolvers are concrete classes inside the library:
        ExpressionsBasedModel.Presolver p = Presolvers.ZERO_ONE_TWO;
        assertTrue(ExpressionsBasedModel.addPresolver(p));
        assertTrue(ExpressionsBasedModel.removePresolver(p));
    }

    @Test
    public void testClearIntegrationsAndPresolvers() {
        ExpressionsBasedModel.clearIntegrations();
        ExpressionsBasedModel.clearPresolvers();
        // After clearing, the default presolvers are re‑added via static init – we just
        // verify that the call does not throw.
        assertNotNull(ExpressionsBasedModel.PRESOLVERS);
    }

    @Test
    public void testResetPresolvers() {
        // Reset should re‑populate the default set
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.resetPresolvers();
        assertFalse(ExpressionsBasedModel.PRESOLVERS.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Construction / basic model manipulation                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructor() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        assertNotNull(m);
        assertEquals(0, m.countVariables());
        assertEquals(0, m.countExpressions());
    }

    @Test
    public void testConstructorWithOptions() {
        Optimisation.Options opt = new Optimisation.Options();
        opt.feasibility = NumberContext.of(8);
        ExpressionsBasedModel m = new ExpressionsBasedModel(opt);
        assertEquals(opt, m.options);
    }

    @Test
    public void testAddVariableAndGetVariable() {
        Variable v = model.addVariable("v1");
        assertNotNull(v);
        assertEquals("v1", v.getName());
        assertEquals(v, model.getVariable(0));
    }

    @Test
    public void testAddExpressionAndGetExpression() {
        Expression e = model.addExpression("e1");
        assertNotNull(e);
        assertEquals("e1", e.getName());
        assertEquals(e, model.getExpression("e1"));
    }

    @Test
    public void testAddExpressionWithoutNameGeneratesUniqueName() {
        Expression e1 = model.addExpression();
        Expression e2 = model.addExpression();
        assertNotEquals(e1.getName(), e2.getName());
    }

    @Test
    public void testAddSpecialOrderedSetInvalidType() {
        Variable v = model.addVariable("v");
        v.binary(); // make it binary
        Collection<Variable> set = Collections.singletonList(v);
        Expression dummy = model.addExpression("dummy");
        try {
            model.addSpecialOrderedSet(set, 0, dummy); // type <= 0 -> should throw
            fail("Expected ProgrammingError for invalid SOS type");
        } catch (ProgrammingError ex) {
            // expected
        }
    }

    @Test
    public void testAddSpecialOrderedSetInvalidMinMax() {
        Variable v = model.addVariable("v");
        v.binary();
        Collection<Variable> set = Collections.singletonList(v);
        try {
            model.addSpecialOrderedSet(set, 2, 1); // min > max
            fail("Expected ProgrammingError for invalid min/max");
        } catch (ProgrammingError ex) {
            // expected
        }
    }

    @Test
    public void testCopyAndRelax() {
        createTinyModel();
        ExpressionsBasedModel copy = model.copy();
        assertEquals(model.countVariables(), copy.countVariables());
        assertEquals(model.countExpressions(), copy.countExpressions());

        ExpressionsBasedModel relaxed = model.copy(true);
        relaxed.relax();
        assertTrue(relaxed.isRelaxed());
    }

    @Test
    public void testCopyShallowAndPrune() {
        createTinyModel();
        ExpressionsBasedModel shallow = model.copy(true, true);
        assertTrue(shallow.isShallowCopy());
        // The shallow copy should still contain the same number of variables (pruning does not remove them)
        assertEquals(model.countVariables(), shallow.countVariables());
    }

    @Test
    public void testCountMethods() {
        createTinyModel();
        assertEquals(2, model.countVariables());
        assertEquals(2, model.countExpressions()); // 1 user expression + the implicit objective expression
    }

    @Test
    public void testDescribe() {
        createTinyModel();
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(2, d.nbVariables);
        // No bounds, no integer variables etc. – just check that the object is not null
        assertNotNull(d.toString());
    }

    @Test
    public void testDisposeClearsData() {
        createTinyModel();
        model.dispose();
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Index and variable category queries                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIndexMethods() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        assertEquals(0, model.indexOf(a));
        assertEquals(1, model.indexOf(b));

        // All variables are free at this point
        assertEquals(0, model.indexOfFreeVariable(a));
        assertEquals(1, model.indexOfFreeVariable(b));
        // No integer variables yet
        assertEquals(-1, model.indexOfIntegerVariable(a));
    }

    @Test
    public void testVariableCategories() {
        Variable pos = model.addVariable("pos").upper(BigMath.ONE);
        Variable neg = model.addVariable("neg").lower(BigMath.ONE.negate());
        Variable intVar = model.addVariable("intVar").integer();

        List<Variable> free = model.getFreeVariables();
        assertTrue(free.contains(pos));
        assertTrue(free.contains(neg));
        assertTrue(free.contains(intVar));

        List<Variable> positive = model.getPositiveVariables();
        assertTrue(positive.contains(pos));
        assertFalse(positive.contains(neg));

        List<Variable> negative = model.getNegativeVariables();
        assertTrue(negative.contains(neg));
        assertFalse(negative.contains(pos));

        List<Variable> integer = model.getIntegerVariables();
        assertTrue(integer.contains(intVar));
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Objective, constraints and quadratic checks                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testObjectiveConstruction() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        x.weight(BigMath.ONE);
        y.weight(BigMath.valueOf(3));
        Expression obj = model.objective();
        // Objective should contain the two linear terms with the given weights
        assertEquals(BigMath.ONE, obj.get(x.getIndex()));
        assertEquals(BigMath.valueOf(3), obj.get(y.getIndex()));
    }

    @Test
    public void testQuadraticChecks() {
        // No quadratic terms added – all should be false
        assertFalse(model.isAnyExpressionQuadratic());
        assertFalse(model.isAnyConstraintQuadratic());
        assertFalse(model.isAnyObjectiveQuadratic());

        // Add a quadratic term and verify
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        Expression e = model.addExpression("quad");
        e.add(a, b, BigMath.ONE); // a*b term
        assertTrue(model.isAnyExpressionQuadratic());
        assertTrue(model.isAnyConstraintQuadratic()); // expression is also a constraint by default
    }

    @Test
    public void testLimitObjectiveCreatesConstraintWhenNeeded() {
        Variable x = model.addVariable("x");
        x.weight(BigMath.ONE);
        // No constraint yet – limitObjective should create a hidden constraint expression
        Expression limit = model.limitObjective(BigDecimal.ZERO, BigDecimal.ONE);
        assertNotNull(limit);
        assertTrue(limit.isConstraint());
        // Limits should be set correctly
        assertEquals(BigDecimal.ZERO, limit.getLowerLimit());
        assertEquals(BigDecimal.ONE, limit.getUpperLimit());
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Optimisation sense and optimise calls (maximise/minimise)         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOptimisationSenseAfterMinimise() {
        createTinyModel();
        Optimisation.Result r = model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
        // The result state should be at least feasible (the built‑in linear solver is used)
        assertNotNull(r);
    }

    @Test
    public void testOptimisationSenseAfterMaximise() {
        createTinyModel();
        Optimisation.Result r = model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
        assertNotNull(r);
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Validation and solution extraction                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValidateWithCorrectSolution() {
        createTinyModel();
        Optimisation.Result sol = model.getVariableValues();
        assertTrue(model.validate(sol));
    }

    @Test
    public void testValidateWithMismatchedSize() {
        createTinyModel();
        // Create a solution vector with wrong dimension
        Array1D<BigDecimal> wrong = Array1D.R256.make(1);
        wrong.set(0, BigMath.ZERO);
        assertFalse(model.validate(wrong));
    }

    @Test
    public void testValidateWithNullSolution() {
        createTinyModel();
        try {
            model.validate((Access1D<BigDecimal>) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetVariableValuesDefaultContext() {
        createTinyModel();
        Optimisation.Result res = model.getVariableValues();
        assertNotNull(res);
        // All variables should have a numeric value (defaults to zero when not set)
        assertEquals(2, res.size());
    }

    /* --------------------------------------------------------------------- */
    /* 7.  Streams and collections                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBoundsStream() {
        Variable v = model.addVariable("v");
        v.lower(BigMath.ZERO).upper(BigMath.ONE); // becomes a bound variable
        long cnt = model.bounds().count();
        assertEquals(1L, cnt);
    }

    @Test
    public void testConstraintsStream() {
        createTinyModel();
        long cnt = model.constraints().count();
        // one explicit constraint + the hidden objective constraint (if any)
        assertTrue(cnt >= 1);
    }

    @Test
    public void testVariablesStream() {
        model.addVariable("a");
        model.addVariable("b");
        long cnt = model.variables().count();
        assertEquals(2L, cnt);
    }

    @Test
    public void testCheckSimilarity() {
        Expression e1 = model.addExpression("e1");
        e1.add(model.addVariable("x"), BigMath.ONE);
        // A new expression that is identical should be reported as similar
        Expression e2 = new Expression("tmp", model);
        e2.add(model.getVariable(0), BigMath.ONE);
        assertTrue(model.checkSimilarity(e2));
    }

    /* --------------------------------------------------------------------- */
    /* 8.  File IO (parse / write)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWriteAndParseEBMFile() throws IOException {
        createTinyModel();
        tempFile = File.createTempFile("ebmTest", ".ebm");
        model.writeTo(tempFile);
        // Parse back – should not throw
        ExpressionsBasedModel parsed = ExpressionsBasedModel.parse(tempFile);
        assertEquals(model.countVariables(), parsed.countVariables());
        assertEquals(model.countExpressions(), parsed.countExpressions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileFormatFromUnknownExtension() {
        ExpressionsBasedModel.FileFormat.from("unknown.xyz");
    }

    /* --------------------------------------------------------------------- */
    /* 9.  Miscellaneous public methods                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrepareReturnsFactoryResult() {
        // Use the built‑in intermediate solver factory
        ExpressionsBasedModel.IntermediateSolver solver = model.prepare(ExpressionsBasedModel.DefaultIntermediate::new);
        assertNotNull(solver);
    }

    @Test
    public void testReduceDoesNotThrow() {
        createTinyModel();
        model.reduce(); // just verify that the call completes
    }

    @Test
    public void testSnapshotCreatesRelaxedShallowCopy() {
        createTinyModel();
        ExpressionsBasedModel snap = model.snapshot();
        assertTrue(snap.isRelaxed());
        assertTrue(snap.isShallowCopy());
    }

    @Test
    public void testToStringContainsVariableAndExpressionNames() {
        model.addVariable("v1");
        model.addExpression("e1");
        String txt = model.toString();
        assertTrue(txt.contains("v1"));
        assertTrue(txt.contains("e1"));
    }

    @Test
    public void testGetExpressionsReturnsUnmodifiableCollection() {
        model.addExpression("e1");
        Collection<Expression> col = model.getExpressions();
        try {
            col.clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetVariablesReturnsUnmodifiableList() {
        model.addVariable("v1");
        List<Variable> lst = model.getVariables();
        try {
            lst.add(model.addVariable("v2"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testSetKnownSolutionAndHandler() {
        createTinyModel();
        Optimisation.Result sol = model.getVariableValues();
        // mark the solution as optimal for the purpose of the test
        Optimisation.Result optimal = new Optimisation.Result(Optimisation.State.OPTIMAL, sol);
        model.setKnownSolution(optimal, (m, s) -> {
            // no‑op handler
        });
        assertEquals(optimal, model.getKnownSolution());
    }

    @Test(expected = ProgrammingError.class)
    public void testSetKnownSolutionWithNonOptimalState() {
        createTinyModel();
        Optimisation.Result nonOptimal = new Optimisation.Result(Optimisation.State.FEASIBLE, model.getVariableValues());
        model.setKnownSolution(nonOptimal);
    }
}
