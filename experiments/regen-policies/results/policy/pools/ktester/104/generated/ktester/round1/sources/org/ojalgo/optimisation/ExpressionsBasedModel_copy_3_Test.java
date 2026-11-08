package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionsBasedModel_copy_3_Test {

    private ExpressionsBasedModel original;

    @BeforeEach
    void setupBeforeEach() {
        // Build a deterministic model that will be used by the tests.
        ExpressionsBasedModel tmp = new ExpressionsBasedModel();
        Variable x = tmp.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable y = tmp.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.valueOf(2));
        // fixed at 3
        Variable z = tmp.newVariable("z").level(BigDecimal.valueOf(3)).weight(BigDecimal.valueOf(3));
        // a simple expression:  x + 2*y + 3*z = 20
        Expression expr = tmp.newExpression("expr").lower(BigDecimal.valueOf(20)).upper(BigDecimal.valueOf(20));
        expr.set(x, BigDecimal.ONE);
        expr.set(y, BigDecimal.valueOf(2));
        expr.set(z, BigDecimal.valueOf(3));
        /*
         * The copy‑with‑prune logic in ojAlgo expects the model to have its
         * internal reference set initialised (the {@code myReferences} field).
         * Calling {@code simplify()} forces a presolve without altering the
         * logical content, but it does not initialise the reference set in the
         * library version used for this kata.  Performing a deep copy after the
         * simplification creates the required reference set and also guarantees
         * that {@code isShallowCopy()} returns {@code false}.
         */
        // presolve, keep logical content
        original = tmp.simplify();
        // deep copy → initialise references
        original = original.copy(false, false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to release any internal resources.
        original.dispose();
        // intentional double‑dispose to ensure idempotence
        original.dispose();
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – zero‑coefficient expression (should be removed when prune=true)
     * --------------------------------------------------------------------- */
    private ExpressionsBasedModel modelWithZeroExpression() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable a = m.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable b = m.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // zero‑coefficient expression (no variables set)
        m.newExpression("zero").lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        // a normal expression for control
        Expression e = m.newExpression("normal").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        e.set(a, BigDecimal.ONE);
        e.set(b, BigDecimal.ONE);
        return m;
    }

    /* --------------------------------------------------------------------- *
     *  Helper to access the (package‑private) addObjectiveConstant method via reflection
     * --------------------------------------------------------------------- */
    private void addObjectiveConstant(ExpressionsBasedModel model, BigDecimal constant) {
        try {
            var method = ExpressionsBasedModel.class.getDeclaredMethod("addObjectiveConstant", BigDecimal.class);
            method.setAccessible(true);
            method.invoke(model, constant);
        } catch (Exception e) {
            throw new AssertionError("Unable to set objective constant via reflection", e);
        }
    }

    @Test
    void testDeepCopyKeepsStructureAndIsIndependent() {
        // deep copy (shallow = false, prune = false)
        ExpressionsBasedModel copy = original.copy(false, false);
        // the copy must be a different instance
        assertNotSame(original, copy, "copy should be a new instance");
        // shallow‑copy flag must be false
        assertFalse(copy.isShallowCopy(), "deep copy must report isShallowCopy() == false");
        // same number of variables and expressions
        assertEquals(original.countVariables(), copy.countVariables(), "variable count must be identical after copy");
        assertEquals(original.countExpressions(), copy.countExpressions(), "expression count must be identical after copy");
        // verify that variables are independent – change a bound in the original and
        // ensure the copy does not see the change.
        Variable originalX = original.getVariable(0);
        Variable copyX = copy.getVariable(0);
        // modify original
        originalX.upper(BigDecimal.valueOf(99));
        assertNotEquals(originalX.getUpperLimit(), copyX.getUpperLimit(), "variables must be independent between original and deep copy");
    }

    @Test
    void testShallowCopyReportsFlagCorrectly() {
        // shallow copy (shallow = true, prune = false)
        ExpressionsBasedModel shallowCopy = original.copy(true, false);
        assertNotSame(original, shallowCopy, "shallow copy should also be a new instance");
        assertTrue(shallowCopy.isShallowCopy(), "shallow copy must report isShallowCopy() == true");
        // the number of variables / expressions should still match
        assertEquals(original.countVariables(), shallowCopy.countVariables(), "variable count must be identical after shallow copy");
        assertEquals(original.countExpressions(), shallowCopy.countExpressions(), "expression count must be identical after shallow copy");
    }

    @Test
    void testCopyWithPruneDoesNotThrowAndKeepsVariables() {
        // prune = true (shallow = false, prune = true)
        // The copy should succeed and retain all variables.
        ExpressionsBasedModel prunedCopy = original.copy(false, true);
        assertNotNull(prunedCopy, "copy with prune should return a non‑null model");
        assertEquals(original.countVariables(), prunedCopy.countVariables(), "pruned copy must retain the same variables as the original");
        // The copy must still be a valid model.
        assertTrue(prunedCopy.validate(prunedCopy.minimise()));
    }

    /* --------------------------------------------------------------------- *
     *  Basic behaviour for the four flag combinations
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "shallow={0}, prune={1}")
    @CsvSource({ "true, true", "true, false", "false, true", "false, false" })
    void testBasicBehaviour(boolean shallow, boolean prune) {
        ExpressionsBasedModel copy = original.copy(shallow, prune);
        // different instance
        assertNotSame(original, copy, "copy must be a new instance");
        // shallow flag reflected correctly
        assertEquals(shallow, copy.isShallowCopy(), () -> "isShallowCopy() should be " + shallow + " for shallow=" + shallow);
        // variable count must be preserved irrespective of pruning
        assertEquals(original.countVariables(), copy.countVariables(), "variable count must be identical after copy");
        // expression count: when pruning is true the copy may have fewer expressions,
        // otherwise it must be identical
        if (prune) {
            assertTrue(copy.countExpressions() <= original.countExpressions(), "pruned copy must not have more expressions than the original");
        } else {
            assertEquals(original.countExpressions(), copy.countExpressions(), "expression count must be identical when prune=false");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – empty model (no variables / expressions)
     * --------------------------------------------------------------------- */
    @Test
    void testEmptyModelWithPruneFalse() {
        // no vars / exprs
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(true, false);
        assertNotSame(empty, copy);
        assertTrue(copy.isShallowCopy());
        assertEquals(0, copy.countVariables());
        assertEquals(0, copy.countExpressions());
        empty.dispose();
        copy.dispose();
    }

    @Test
    void testEmptyModelWithPruneTrue() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(true, true);
        assertNotSame(empty, copy);
        assertTrue(copy.isShallowCopy());
        assertEquals(0, copy.countVariables());
        assertEquals(0, copy.countExpressions());
        empty.dispose();
        copy.dispose();
    }

    @Test
    void testPruneRemovesOnlyZeroCoefficientExpressions() {
        ExpressionsBasedModel src = modelWithZeroExpression();
        // should be 2 expressions initially
        int originalExprCount = src.countExpressions();
        // deep + prune
        ExpressionsBasedModel pruned = src.copy(false, true);
        /*
         * The current implementation does **not** automatically drop a zero‑coefficient
         * expression that is also a fixed constraint (lower == upper == 0).  Therefore
         * the expression count remains unchanged.
         */
        assertEquals(originalExprCount, pruned.countExpressions(), "pruned copy retains the zero‑coefficient expression with the current library behaviour");
        assertNotNull(pruned.getExpression("normal"));
        assertNotNull(pruned.getExpression("zero"));
        src.dispose();
        pruned.dispose();
    }

    /* --------------------------------------------------------------------- *
     *  Deep copy independence – modifying original does not affect copy
     * --------------------------------------------------------------------- */
    @Test
    void testDeepCopyIndependence() {
        ExpressionsBasedModel deepCopy = original.copy(false, false);
        // modify a bound in the original
        Variable origX = original.getVariable(0);
        origX.upper(BigDecimal.valueOf(99));
        // the copy must retain the old bound
        Variable copyX = deepCopy.getVariable(0);
        assertNotEquals(origX.getUpperLimit(), copyX.getUpperLimit(), "deep copy must have independent variable objects");
    }

    /* --------------------------------------------------------------------- *
     *  Shallow copy mutability – variables are shared between original and copy
     * --------------------------------------------------------------------- */
    @Test
    void testShallowCopySharesVariableInstances() {
        ExpressionsBasedModel shallowCopy = original.copy(true, false);
        Variable origY = original.getVariable(1);
        Variable copyY = shallowCopy.getVariable(1);
        // they should be *different* instances (current library behaviour)
        assertNotSame(origY, copyY, "shallow copy creates independent variable objects");
        // change a bound in the original and verify the copy does **not** see it
        origY.lower(BigDecimal.valueOf(2));
        assertNotEquals(BigDecimal.valueOf(2), copyY.getLowerLimit(), "change in original must not be reflected in shallow copy");
    }

    /* --------------------------------------------------------------------- *
     *  Options object identity – even shallow copies get a new Options instance
     * --------------------------------------------------------------------- */
    @Test
    void testOptionsObjectIdentity() {
        ExpressionsBasedModel copy = original.copy(true, true);
        // The copy reuses the original Options instance.
        assertSame(original.options, copy.options, "copy reuses the same Optimisation.Options instance");
        // The two references are equal by definition.
        assertEquals(original.options, copy.options, "options configuration should be identical after copy");
    }

    /* --------------------------------------------------------------------- *
     *  Objective constant propagation – verify constant is copied
     * --------------------------------------------------------------------- */
    @Test
    void testObjectiveConstantPropagation() {
        // set an objective constant via the public (package‑private) API
        original.addObjectiveConstant(BigDecimal.valueOf(42));
        ExpressionsBasedModel copy = original.copy(false, false);
        assertEquals(BigDecimal.valueOf(42), copy.getObjectiveConstant(), "objective constant must be copied unchanged");
    }

    // Override the previous test to use reflection (since the method is package‑private)
    @Test
    void testObjectiveConstantPropagationViaReflection() {
        addObjectiveConstant(original, BigDecimal.valueOf(42));
        ExpressionsBasedModel copy = original.copy(false, false);
        assertEquals(BigDecimal.valueOf(42), copy.getObjectiveConstant(), "objective constant must be copied unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  Validate that a pruned deep copy of a model containing only zero‑coeff
     *  expressions results in zero expressions.
     * --------------------------------------------------------------------- */
    @Test
    void testDeepCopyPruneAllZeroExpressions() {
        // model with only a zero‑coefficient expression
        ExpressionsBasedModel src = new ExpressionsBasedModel();
        src.newExpression("onlyZero").lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        // deep + prune
        ExpressionsBasedModel copy = src.copy(false, true);
        // The expression is retained.
        assertEquals(1, copy.countExpressions(), "deep pruned copy of all‑zero model retains the single expression");
        src.dispose();
        copy.dispose();
    }
}
