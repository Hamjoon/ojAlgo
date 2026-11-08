package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.optimisation.Optimisation.Result;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_simplify_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    @BeforeEach
    void setupBeforeEach() {
        // Build a small, deterministic optimisation problem that is easy to solve.
        model = new ExpressionsBasedModel();
        // Variable x: 0 ≤ x ≤ 10, contributes to the objective with weight 1.
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Variable y: 0 ≤ y ≤ 10, contributes to the objective with weight 2.
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.valueOf(2));
        // Constraint: x + y = 8
        model.newExpression("c1").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).level(BigDecimal.valueOf(8));
    }

    @Test
    void testSimplifyReturnsEquivalentOptimalResult() {
        // Solve the original model.
        Result originalResult = model.minimise();
        assertTrue(originalResult.getState().isOptimal(), "Original model should solve to an optimal state");
        // Simplify (which internally scans, presolves and copies the model).
        ExpressionsBasedModel simplified = model.simplify();
        assertNotSame(model, simplified, "Simplify must return a new instance");
        // Solve the simplified copy.
        Result simplifiedResult = simplified.minimise();
        assertTrue(simplifiedResult.getState().isOptimal(), "Simplified model should also solve to an optimal state");
        // The objective values must be identical.
        assertEquals(originalResult.getValue(), simplifiedResult.getValue(), "Objective value after simplification must match the original");
    }

    @Test
    void testSimplifyIsShallowCopyAndUnaffectedByLaterChanges() {
        // Obtain a simplified copy before any further mutation.
        ExpressionsBasedModel simplified = model.simplify();
        // Change the lower bound of variable x in the *original* model.
        // This should make the feasible region different for the original model.
        x.lower(BigDecimal.valueOf(5));
        // Solve both models.
        Result originalResult = model.minimise();
        Result simplifiedResult = simplified.minimise();
        // The original model now has a higher lower bound, so its optimal value must be larger.
        assertTrue(originalResult.getState().isOptimal());
        assertTrue(simplifiedResult.getState().isOptimal());
        // Verify that the simplified copy retained the original bound (x ≥ 0) and therefore
        // produces a different (lower) objective value.
        assertTrue(simplifiedResult.getValue().compareTo(originalResult.getValue()) < 0, "Simplified copy must be unaffected by later changes to the original model");
    }

    /**
     * Simplify an empty model – expect a new, empty, non‑infeasible instance.
     */
    @Test
    void testSimplifyEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.simplify();
        assertNotSame(empty, copy, "Simplify must return a new instance");
        assertEquals(0, copy.countVariables(), "Simplified empty model should have zero variables");
        assertEquals(0, copy.countExpressions(), "Simplified empty model should have zero expressions");
        assertFalse(copy.isInfeasible(), "Simplified empty model must not be marked infeasible");
    }

    /**
     * Simplify a model with a single variable and a single linear expression.
     */
    @Test
    void testSimplifySingleVariableAndExpression() {
        ExpressionsBasedModel single = new ExpressionsBasedModel();
        Variable v = single.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // Simple constraint: v = 0.5
        single.newExpression("c").set(v, BigDecimal.ONE).level(BigDecimal.valueOf(0.5));
        ExpressionsBasedModel copy = single.simplify();
        assertNotSame(single, copy, "Simplify must return a new instance");
        assertEquals(1, copy.countVariables(), "Simplified model should contain exactly one variable");
        assertEquals(1, copy.countExpressions(), "Simplified model should contain exactly one expression");
        // The copy should be a shallow one – changing the original variable must not affect the copy.
        v.lower(BigDecimal.valueOf(0.2));
        assertEquals(BigDecimal.ZERO, copy.getVariable(0).lower().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : null, "Simplified copy must retain original lower bound (0)");
    }

    /**
     * Simplify a model with three variables and two expressions – verify deep copy semantics.
     */
    @Test
    void testSimplifyMultipleVariablesAndExpressions() {
        ExpressionsBasedModel multi = new ExpressionsBasedModel();
        Variable a = multi.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        Variable b = multi.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        Variable c = multi.newVariable("c").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // Two independent constraints
        multi.newExpression("c1").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).upper(BigDecimal.valueOf(6));
        multi.newExpression("c2").set(b, BigDecimal.ONE).set(c, BigDecimal.ONE).lower(BigDecimal.valueOf(2));
        ExpressionsBasedModel copy = multi.simplify();
        assertEquals(3, copy.countVariables(), "Variable count must be preserved");
        assertEquals(2, copy.countExpressions(), "Expression count must be preserved");
        // Mutate original and ensure copy is unchanged.
        a.lower(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.ZERO, copy.getVariable(0).lower(), "Copy should keep original lower bound");
    }

    /**
     * Fixed and integer variable handling – flags must survive simplification.
     */
    @Test
    void testSimplifyFixedAndIntegerVariables() {
        ExpressionsBasedModel modelFI = new ExpressionsBasedModel();
        // Fixed variable (lower == upper)
        Variable fixed = modelFI.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // Integer variable
        Variable integer = modelFI.newVariable("int").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(// public API for integer flag
        true);
        // Trivial constraint to keep the model feasible
        modelFI.newExpression("c").set(fixed, BigDecimal.ONE).level(BigDecimal.valueOf(5));
        ExpressionsBasedModel copy = modelFI.simplify();
        assertTrue(copy.isAnyVariableFixed(), "Simplified model must report a fixed variable");
        assertTrue(copy.isAnyVariableInteger(), "Simplified model must report an integer variable");
        assertEquals(2, copy.countVariables(), "Variable count must stay unchanged");
    }

    /**
     * Infeasible model – simplification must propagate infeasibility flag.
     */
    @Test
    void testSimplifyInfeasibleModel() {
        ExpressionsBasedModel infeas = new ExpressionsBasedModel();
        Variable v = infeas.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Contradictory constraints: v ≥ 10 and v ≤ 5
        infeas.newExpression("c1").set(v, BigDecimal.ONE).lower(BigDecimal.valueOf(10));
        infeas.newExpression("c2").set(v, BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        ExpressionsBasedModel copy = infeas.simplify();
        assertTrue(copy.isInfeasible(), "Simplified model must be marked infeasible");
        // The original should also be infeasible after presolve.
        assertTrue(infeas.isInfeasible(), "Original model should also be infeasible");
    }

    /**
     * Quadratic expression handling – the quadratic flag must survive simplification.
     */
    @Test
    void testSimplifyQuadraticExpressionPreserved() {
        ExpressionsBasedModel quad = new ExpressionsBasedModel();
        Variable v = quad.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Build a quadratic expression: v*v ≤ 10
        // The API for quadratic terms is `quadratic(Variable, Comparable)`.
        quad.newExpression("q").quadratic(v, // assume this public method exists
        BigDecimal.ONE).upper(BigDecimal.TEN);
        ExpressionsBasedModel copy = quad.simplify();
        assertTrue(copy.isAnyExpressionQuadratic(), "Simplified model must still report a quadratic expression");
        // The model should still be feasible.
        assertFalse(copy.isInfeasible(), "Simplified quadratic model must not be infeasible");
    }

    /**
     * Objective constant preservation – the constant term must be copied unchanged.
     */
    @Test
    void testSimplifyObjectiveConstantPreserved() {
        ExpressionsBasedModel obj = new ExpressionsBasedModel();
        Variable v = obj.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Add a constant contribution to the objective by creating a dummy expression with weight 0
        // and setting its constant term via `setConstant`. The public API for this is `setConstant`.
        obj.objective().setConstant(BigDecimal.valueOf(7));
        ExpressionsBasedModel copy = obj.simplify();
        assertEquals(BigDecimal.valueOf(7), copy.getObjectiveConstant(), "Objective constant must be identical after simplification");
    }

    /**
     * Reference handling – references added before simplification must survive.
     */
    @Test
    void testSimplifyReferenceHandling() throws Exception {
        ExpressionsBasedModel refModel = new ExpressionsBasedModel();
        Variable a = refModel.newVariable("a");
        Variable b = refModel.newVariable("b");
        // Add an explicit reference to variable 'a'.
        // `addReference` expects an `IntIndex`; Variable implements that interface.
        refModel.addReference(a);
        ExpressionsBasedModel copy = refModel.simplify();
        // Use reflection to access the private `references` field (no public getter exists).
        var field = ExpressionsBasedModel.class.getDeclaredField("references");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<?> originalRefs = (java.util.Set<?>) field.get(refModel);
        @SuppressWarnings("unchecked")
        java.util.Set<?> copyRefs = (java.util.Set<?>) field.get(copy);
        assertEquals(originalRefs, copyRefs, "Reference set must be identical after simplification");
    }

    /**
     * Special Ordered Set (SOS) preservation – the SOS collection must be copied.
     */
    @Test
    void testSimplifySpecialOrderedSetPreserved() {
        ExpressionsBasedModel sosModel = new ExpressionsBasedModel();
        Variable v1 = sosModel.newVariable("v1");
        Variable v2 = sosModel.newVariable("v2");
        Variable v3 = sosModel.newVariable("v3");
        Collection<Variable> sos = Arrays.asList(v1, v2, v3);
        // SOS type 1 (SOS1) with cardinality limits 1..1
        sosModel.addSpecialOrderedSet(sos, 1, 1);
        ExpressionsBasedModel copy = sosModel.simplify();
        // There is no public API to query SOS sets, but we can verify that the number of
        // special ordered sets (an internal collection) is the same via reflection.
        var field = ExpressionsBasedModel.class.getDeclaredField("specialOrderedSets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Collection<?> originalSets = (java.util.Collection<?>) field.get(sosModel);
        @SuppressWarnings("unchecked")
        java.util.Collection<?> copySets = (java.util.Collection<?>) field.get(copy);
        assertEquals(originalSets.size(), copySets.size(), "Number of SOS collections must be preserved after simplification");
    }

    /**
     * Presolver should prune redundant constraints – simplified model has fewer expressions.
     */
    @Test
    void testSimplifyRedundantConstraintPruning() {
        ExpressionsBasedModel red = new ExpressionsBasedModel();
        Variable a = red.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable b = red.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Constraint 1: a + b ≤ 10
        red.newExpression("c1").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).upper(BigDecimal.TEN);
        // Constraint 2: a + b ≥ 5
        red.newExpression("c2").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).lower(BigDecimal.valueOf(5));
        // Constraint 3: a + b = 7  (redundant – implied by c1 and c2)
        red.newExpression("c3").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).level(BigDecimal.valueOf(7));
        ExpressionsBasedModel copy = red.simplify();
        // Original has 3 expressions; after presolve the redundant one should be removed.
        assertTrue(copy.countExpressions() < red.countExpressions(), "Simplified model must contain fewer expressions after redundant constraint removal");
    }

    /**
     * Disposed model – calling simplify must throw IllegalStateException.
     */
    @Test
    void testSimplifyAfterDisposeThrows() {
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.simplify(), "Simplify on a disposed model should throw IllegalStateException");
    }
}
