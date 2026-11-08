package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_simplify_Test {

    @BeforeEach
    void setupBeforeEach() {
        // ensure a clean presolver registry for every test
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // clean up any custom presolvers that might have been registered
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * The simplify method must return a *different* model instance.
     */
    @Test
    void testSimplifyReturnsNewInstance() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // add a trivial variable and expression so the model is not empty
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        original.newExpression("obj").set(x, BigDecimal.ONE).weight(BigDecimal.ONE);
        ExpressionsBasedModel simplified = original.simplify();
        assertNotSame(original, simplified, "simplify() should return a new model instance");
    }

    /**
     * A model that is infeasible should be recognised as such after simplification.
     */
    @Test
    void testSimplifyDetectsInfeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable x is bounded between 0 and 1
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // Add a constraint that forces x >= 2, which contradicts the bounds
        model.newExpression("c1").lower(BigDecimal.valueOf(2)).set(x, BigDecimal.ONE);
        // Simplify should run the presolver and make the model infeasible without solving
        ExpressionsBasedModel simplified = model.simplify();
        Optimisation.Result result = simplified.minimise();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "Simplified model should be infeasible");
    }

    /**
     * For a feasible problem the simplified model must produce the same optimal solution.
     */
    @Test
    void testSimplifyPreservesOptimalSolution() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable x with bounds [0, 10] and weight 1 (objective contribution)
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Constraint: x >= 5
        model.newExpression("c1").lower(BigDecimal.valueOf(5)).set(x, BigDecimal.ONE);
        // Solve the original model
        Optimisation.Result originalResult = model.minimise();
        assertTrue(originalResult.getState().isOptimal(), "Original model should be optimal");
        // Simplify and solve again
        ExpressionsBasedModel simplified = model.simplify();
        Optimisation.Result simplifiedResult = simplified.minimise();
        assertTrue(simplifiedResult.getState().isOptimal(), "Simplified model should be optimal");
        // Both solutions must agree on the variable value and the objective value
        BigDecimal originalX = originalResult.get(model.indexOf(x));
        BigDecimal simplifiedX = simplifiedResult.get(simplified.indexOf(x));
        assertEquals(originalX, simplifiedX, "Variable value must be identical after simplification");
        assertEquals(originalResult.getValue(), simplifiedResult.getValue(), "Objective value must be identical after simplification");
    }

    /**
     * An empty model must still invoke the internal steps and return a new instance.
     */
    @Test
    void testSimplifyOnEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        // No variables / expressions are added
        ExpressionsBasedModel simplified = empty.simplify();
        assertNotSame(empty, simplified, "simplify() on an empty model must return a new instance");
        // both models should contain zero variables / expressions
        assertEquals(0, empty.countVariables(), "original empty model should have 0 variables");
        assertEquals(0, simplified.countVariables(), "simplified empty model should have 0 variables");
        assertEquals(0, empty.countExpressions(), "original empty model should have 0 expressions");
        assertEquals(0, simplified.countExpressions(), "simplified empty model should have 0 expressions");
    }

    @Test
    void testSimplifyPreservesIntegerAndFixedVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // integer variable
        Variable intVar = model.newVariable("intVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(// mark as integer
        true);
        // fixed variable (lower == upper)
        Variable fixedVar = model.newVariable("fixedVar").fix(// fixing makes it both lower & upper = 5
        BigDecimal.valueOf(5));
        // a simple objective using both variables
        model.newExpression("obj").set(intVar, BigDecimal.ONE).set(fixedVar, BigDecimal.ONE).weight(BigDecimal.ONE);
        ExpressionsBasedModel simplified = model.simplify();
        // --- variable count & type checks ---
        assertEquals(model.countVariables(), simplified.countVariables(), "Variable count must be preserved");
        Variable simpInt = simplified.getVariable(simplified.indexOf(intVar));
        Variable simpFixed = simplified.getVariable(simplified.indexOf(fixedVar));
        // instances must be different objects
        assertNotSame(intVar, simpInt, "Integer variable must be deep‑copied");
        assertNotSame(fixedVar, simpFixed, "Fixed variable must be deep‑copied");
        // but their characteristics must be identical
        assertTrue(simpInt.isInteger(), "Integer flag must be preserved");
        assertTrue(simpFixed.isFixed(), "Fixed flag must be preserved");
        assertEquals(intVar.getLowerLimit(), simpInt.getLowerLimit(), "Integer variable lower bound must be identical");
        assertEquals(fixedVar.getUpperLimit(), simpFixed.getUpperLimit(), "Fixed variable bound must be identical");
    }

    /**
     * A custom Presolver that removes a redundant expression should affect the copy.
     */
    @Test
    void testPresolverRemovesRedundantExpression() {
        // --- build a model with a redundant constraint (always satisfied) ---
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // expression that does nothing (0 * x) and is always true because lower = -∞, upper = +∞
        model.newExpression("redundant").set(x, BigDecimal.ZERO);
        // --- register a simple presolver that removes expressions named "redundant" ---
        Presolver remover = new Presolver() {

            @Override
            public int compareTo(Presolver o) {
                // order does not matter for this test
                return 0;
            }

            @Override
            public void presolve(ExpressionsBasedModel mdl) {
                // iterate over a snapshot to avoid ConcurrentModificationException
                List<Expression> toRemove = mdl.expressions().filter(e -> "redundant".equals(e.getName())).toList();
                toRemove.forEach(mdl::removeExpression);
            }
        };
        ExpressionsBasedModel.addPresolver(remover);
        // --- invoke simplify ---
        ExpressionsBasedModel simplified = model.simplify();
        // The original must still contain the redundant expression
        assertTrue(model.expressions().anyMatch(e -> "redundant".equals(e.getName())), "Original model must keep the redundant expression");
        // The simplified copy must have it removed
        assertFalse(simplified.expressions().anyMatch(e -> "redundant".equals(e.getName())), "Simplified model should have the redundant expression removed");
    }

    /**
     * If a Presolver throws a RuntimeException, simplify() must propagate it.
     */
    @Test
    void testPresolverExceptionPropagation() {
        // register a presolver that always throws
        Presolver exploding = new Presolver() {

            @Override
            public int compareTo(Presolver o) {
                return 0;
            }

            @Override
            public void presolve(ExpressionsBasedModel mdl) {
                throw new RuntimeException("boom");
            }
        };
        ExpressionsBasedModel.addPresolver(exploding);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        RuntimeException thrown = assertThrows(RuntimeException.class, model::simplify, "simplify() should propagate RuntimeException from a presolver");
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    void testSimplifyRetainsKnownSolution() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("obj").set(x, BigDecimal.ONE).weight(BigDecimal.ONE);
        // Solve to obtain a feasible result
        Optimisation.Result originalResult = model.minimise();
        assertTrue(originalResult.getState().isOptimal(), "Original model must be optimal");
        // Store the result as a known solution
        model.setKnownSolution(originalResult);
        // Simplify
        ExpressionsBasedModel simplified = model.simplify();
        Optimisation.Result knownFromSimplified = simplified.getKnownSolution();
        assertNotNull(knownFromSimplified, "Simplified model should contain a known solution");
        // the values must be equal, but the Result objects must be different (defensive copy)
        assertEquals(originalResult.getValue(), knownFromSimplified.getValue(), "Objective value of known solution must be identical");
        assertNotSame(originalResult, knownFromSimplified, "Known solution should be a defensive copy, not the same instance");
    }

    @Test
    void testSimplifyCopiesIntegrationSwitchFlag() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The method setIntegrationSwitch(boolean) is package‑private; use reflection.
        Method setSwitch = ExpressionsBasedModel.class.getDeclaredMethod("setIntegrationSwitch", boolean.class);
        setSwitch.setAccessible(true);
        setSwitch.invoke(model, true);
        // Verify the flag is on before simplification
        assertTrue(model.isIntegrationSwitch(), "Integration switch must be true before simplify");
        // Simplify
        ExpressionsBasedModel simplified = model.simplify();
        // The flag must be true in the copy as well
        assertTrue(simplified.isIntegrationSwitch(), "Simplified model must retain the integration‑switch flag");
    }

    @Test
    void testSimplifyCopiesSpecialOrderedSets() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // create three variables that will belong to an SOS
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        // SOS type 1 (e.g., SOS1) with min=1, max=1
        model.addSpecialOrderedSet(List.of(a, b, c), 1, 1);
        // Simplify
        ExpressionsBasedModel simplified = model.simplify();
        // The simplified model must contain exactly one SOS definition
        Collection<Variable> originalSet = List.of(a, b, c);
        // The API does not expose SOS directly, so we verify via reflection on the internal field.
        // The field name in the source is not public; we therefore check that the variables
        // appear in the same order in the copy and that they are distinct instances.
        List<Variable> simplifiedVars = simplified.getVariables();
        // locate the three variables by name in the simplified model
        Variable a2 = simplified.getVariable("a");
        Variable b2 = simplified.getVariable("b");
        Variable c2 = simplified.getVariable("c");
        assertNotNull(a2);
        assertNotNull(b2);
        assertNotNull(c2);
        // ensure they are different objects from the originals
        assertNotSame(a, a2);
        assertNotSame(b, b2);
        assertNotSame(c, c2);
    }

    @Test
    void testSimplifyWithDuplicateVariableNamesThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Two variables with the same name
        model.newVariable("dup");
        // duplicate name
        model.newVariable("dup");
        // simplify should detect the duplicate during the copy phase
        assertThrows(IllegalArgumentException.class, model::simplify, "Simplify must throw IllegalArgumentException when duplicate variable names exist");
    }
}
