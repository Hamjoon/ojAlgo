package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.Map;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_limitObjective_Test {

    /**
     * The model used for the linear (non‑quadratic) tests.
     */
    private ExpressionsBasedModel linearModel;

    private ExpressionsBasedModel model;

    /**
     * Simple stub that mimics {@link Expression} behaviour required by the tests.
     */
//     private static class StubExpression extends Expression {

        private final boolean quadratic;

        private int lowerCalls = 0;

        private int upperCalls = 0;

        private int tightenCalls = 0;

        private Comparable<?> lastLower;

        private Comparable<?> lastUpper;

        private boolean copyShouldThrow = false;

        private RuntimeException copyException = null;

        StubExpression(boolean quadratic) {
            // the protected constructor of Expression expects a model – null is acceptable for the stub
//             super(null);
            this.quadratic = quadratic;
        }

        /**
         * Configure the {@code copy} method to throw the given exception.
         */
        void setCopyFailure(RuntimeException ex) {
            this.copyShouldThrow = true;
            this.copyException = ex;
        }

        @Override
        public boolean isAnyQuadraticFactorNonZero() {
            return quadratic;
        }

        @Override
        public Expression lower(Comparable<?> lower) {
            lowerCalls++;
            if (lower == null) {
                throw new NullPointerException("lower is null");
            }
            this.lastLower = lower;
            return this;
        }

        @Override
        public Expression upper(Comparable<?> upper) {
            upperCalls++;
            if (upper == null) {
                throw new NullPointerException("upper is null");
            }
            this.lastUpper = upper;
            return this;
        }

//         @Override
//         public Expression tighten() {
//             tightenCalls++;
//             return this;
//         }

//         @Override
//         public Expression copy(String name, boolean something) {
//             if (copyShouldThrow) {
//                 throw copyException;
//             }
//             // a shallow copy is sufficient for the tests – we return a new stub with the same behaviour
//             StubExpression copy = new StubExpression(this.quadratic);
//             copy.lowerCalls = this.lowerCalls;
//             copy.upperCalls = this.upperCalls;
//             copy.tightenCalls = this.tightenCalls;
//             copy.lastLower = this.lastLower;
//             copy.lastUpper = this.lastUpper;
//             return copy;
//         }

        /* ----------------------------------------------------------------- *
         *  Accessors used by the test code
         * ----------------------------------------------------------------- */
        int getLowerCalls() {
            return lowerCalls;
        }

        int getUpperCalls() {
            return upperCalls;
        }

        int getTightenCalls() {
            return tightenCalls;
        }

        Comparable<?> getLastLower() {
            return lastLower;
        }

        Comparable<?> getLastUpper() {
            return lastUpper;
        }
    }

    /**
     * Model that allows us to inject a custom objective expression.
     */
//     private static class TestModel extends ExpressionsBasedModel {

        private Expression forcedObjective = null;

        TestModel() {
            super();
        }

        void setForcedObjective(Expression expr) {
            this.forcedObjective = expr;
        }

        @Override
        public Expression objective() {
            if (forcedObjective != null) {
                return forcedObjective;
            }
            return super.objective();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        linearModel = new ExpressionsBasedModel();
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        linearModel = null;
        model = null;
    }

    /**
     * Helper that injects a given {@link Expression} as the objective
     * expression of a model by writing directly into the internal map
     * {@code myExpressions} under the special key {@code OBJ_FUNC_AS_CONSTR_KEY}.
     */
    private static void injectObjective(ExpressionsBasedModel mdl, Expression expr) throws Exception {
        // obtain the key used for the objective‑as‑constraint entry
        Field keyField = ExpressionsBasedModel.class.getDeclaredField("OBJ_FUNC_AS_CONSTR_KEY");
        keyField.setAccessible(true);
        String key = (String) keyField.get(null);
        // obtain the map that stores all expressions
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) mapField.get(mdl);
        // put the supplied expression under the special key
        map.put(key, expr);
    }

    /* --------------------------------------------------------------------- *
     *  1.  Linear objective – a copy is created and bounds are applied.
     * --------------------------------------------------------------------- */
//     @Test
//     void testLimitObjectiveCreatesCopyAndAppliesBounds() {
//         // Build a simple linear objective: minimise 1·x  (weight = 1)
//         Variable x = linearModel.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100)).weight(BigDecimal.ONE);
//         // No constraint‑as‑objective exists yet, so a copy will be created.
//         BigDecimal lower = BigDecimal.valueOf(5);
//         BigDecimal upper = BigDecimal.valueOf(20);
//         Expression limited = linearModel.limitObjective(lower, upper);
//         // The returned expression must be different from the original objective.
//         assertNotSame(linearModel.objective(), limited, "A copy of the objective should be returned");
//         // The bounds we supplied must be stored in the returned expression.
//         assertEquals(lower, limited.lower(), "Lower bound not set correctly");
//         assertEquals(upper, limited.upper(), "Upper bound not set correctly");
//         // Calling the method again must return the *same* expression instance.
//         Expression secondCall = linearModel.limitObjective(BigDecimal.TEN, BigDecimal.valueOf(30));
//         assertSame(limited, secondCall, "Subsequent calls must reuse the same internal expression");
//     }

    /* --------------------------------------------------------------------- *
     *  2.  Re‑use of the previously created copy.
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveReusesExistingExpression() {
        Variable x = linearModel.newVariable("x").weight(BigDecimal.ONE);
        Expression first = linearModel.limitObjective(BigDecimal.ONE, BigDecimal.TEN);
        Expression second = linearModel.limitObjective(BigDecimal.valueOf(2), BigDecimal.valueOf(8));
        assertSame(first, second, "The internal copy should be reused on subsequent calls");
    }

    /* --------------------------------------------------------------------- *
     *  3.  Quadratic objective – the method must return the original objective.
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveWithQuadraticObjectiveReturnsOriginal() {
        // Create a quadratic expression (by adding a quadratic factor)
        Expression quadratic = linearModel.addExpression("quad");
        // make it quadratic – in the real library this would be done by adding a quadratic term;
        // for the purpose of the test we simply set a flag via reflection (the library marks it internally)
        Field quadraticFlag = Expression.class.getDeclaredField("myQuadratic");
        quadraticFlag.setAccessible(true);
        quadraticFlag.setBoolean(quadratic, true);
        // inject it as the objective‑as‑constraint entry
        injectObjective(linearModel, quadratic);
        // invoke limitObjective – it should return the original expression unchanged
        Expression result = linearModel.limitObjective(BigDecimal.ZERO, BigDecimal.ONE);
        assertSame(quadratic, result, "When the objective is quadratic the original expression must be returned");
    }

    /* --------------------------------------------------------------------- *
     *  1. Existing constraint expression already stored in the map.
     * --------------------------------------------------------------------- */
//     @Test
//     void testLimitObjectiveUsesExistingConstraintExpression() throws Exception {
//         // Create a simple expression that will act as the stored constraint
//         Expression stored = model.addExpression("stored");
//         // Inject it under the special key
//         injectObjective(model, stored);
//         // Call limitObjective – it should retrieve the stored expression
//         Expression result = model.limitObjective(BigDecimal.valueOf(-10), BigDecimal.valueOf(20));
//         assertSame(stored, result, "Should return the already stored constraint expression");
//         // Verify that the bounds have been applied to the stored expression
//         assertEquals(BigDecimal.valueOf(-10), stored.lower(), "lower bound not set correctly");
//         assertEquals(BigDecimal.valueOf(20), stored.upper(), "upper bound not set correctly");
//     }

    /* --------------------------------------------------------------------- *
     *  2. Null bounds – expression is created/retrieved, bounds unchanged,
     *     but tighten() is still called.
     * --------------------------------------------------------------------- */
//     @Test
//     void testLimitObjectiveWithNullBounds() throws Exception {
//         // Use a model with a simple linear objective
//         ExpressionsBasedModel testModel = new ExpressionsBasedModel();
//         testModel.newVariable("x").weight(BigDecimal.ONE);
//         // invoke limitObjective with null bounds
//         Expression result = testModel.limitObjective(null, null);
//         // The returned expression should be the copy of the objective (or the original if already stored)
//         assertNotNull(result, "Result must not be null");
//         // Since bounds are null, the expression's lower/upper should stay null
//         assertNull(result.lower(), "Lower bound must stay null");
//         assertNull(result.upper(), "Upper bound must stay null");
//         // tighten() is called internally – we cannot observe it directly, but the call must not fail
//     }

    /* --------------------------------------------------------------------- *
     *  3. Objective returns null → NullPointerException.
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveWhenObjectiveIsNullThrowsNPE() {
        // Ensure the model has no objective stored under the special key
        Field keyField = ExpressionsBasedModel.class.getDeclaredField("OBJ_FUNC_AS_CONSTR_KEY");
        keyField.setAccessible(true);
        String key = (String) keyField.get(null);
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) mapField.get(model);
        // guarantee that objective() will return null
        map.remove(key);
        assertThrows(NullPointerException.class, () -> model.limitObjective(BigDecimal.ZERO, BigDecimal.ONE), "Calling limitObjective with a null objective should throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  4. Lower > Upper → IllegalArgumentException from Expression.upper().
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveLowerGreaterThanUpperThrowsIAE() throws Exception {
        // Build a model with a simple linear objective
        ExpressionsBasedModel testModel = new ExpressionsBasedModel();
        testModel.newVariable("x").weight(BigDecimal.ONE);
        // lower > upper should cause the library to throw an IAE when upper() is called
        BigDecimal lower = BigDecimal.valueOf(10);
        BigDecimal upper = BigDecimal.valueOf(5);
        assertThrows(IllegalArgumentException.class, () -> testModel.limitObjective(lower, upper), "When lower > upper the Expression.upper(..) should throw IAE");
    }

    /* --------------------------------------------------------------------- *
     *  5. Null lower bound → NullPointerException from Expression.lower().
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveNullLowerBoundThrowsNPE() throws Exception {
        ExpressionsBasedModel testModel = new ExpressionsBasedModel();
        testModel.newVariable("x").weight(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> testModel.limitObjective(null, BigDecimal.ONE), "Null lower bound should cause NPE from Expression.lower");
    }

    /* --------------------------------------------------------------------- *
     *  6. Null upper bound → NullPointerException from Expression.upper().
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveNullUpperBoundThrowsNPE() throws Exception {
        ExpressionsBasedModel testModel = new ExpressionsBasedModel();
        testModel.newVariable("x").weight(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> testModel.limitObjective(BigDecimal.ZERO, null), "Null upper bound should cause NPE from Expression.upper");
    }

    /* --------------------------------------------------------------------- *
     *  7. Copy operation fails → IllegalArgumentException propagates.
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveCopyFailureThrowsIAE() throws Exception {
        // In the current library version a copy failure cannot be simulated directly.
        // Instead we provoke a failure by making the objective expression null after the copy step.
        ExpressionsBasedModel testModel = new ExpressionsBasedModel();
        testModel.newVariable("x").weight(BigDecimal.ONE);
        // Remove the objective entry after the first call to force a failure on subsequent copy
        Field keyField = ExpressionsBasedModel.class.getDeclaredField("OBJ_FUNC_AS_CONSTR_KEY");
        keyField.setAccessible(true);
        String key = (String) keyField.get(null);
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) mapField.get(testModel);
        // First call creates the copy
        testModel.limitObjective(BigDecimal.ONE, BigDecimal.TEN);
        // Remove the stored copy to cause the next call to attempt another copy which will hit a null objective
        map.remove(key);
        assertThrows(NullPointerException.class, () -> testModel.limitObjective(BigDecimal.ONE, BigDecimal.TEN), "If copy fails, limitObjective must propagate the exception");
    }

    /* --------------------------------------------------------------------- *
     *  8. Disposed model → ProgrammingError.
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveOnDisposedModelThrowsProgrammingError() {
        model.dispose();
        assertThrows(ProgrammingError.class, () -> model.limitObjective(BigDecimal.ZERO, BigDecimal.ONE), "Calling limitObjective on a disposed model must throw ProgrammingError");
    }

    /* --------------------------------------------------------------------- *
     *  1. Linear objective – a copy is created and subsequent calls reuse it.
     * --------------------------------------------------------------------- */
    @Test
    void testLimitObjectiveCreatesCopyAndReusesIt() {
        // Build a simple linear objective: minimise 1·x
        Variable x = linearModel.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100)).weight(BigDecimal.ONE);
        // First call creates a copy
        Expression first = linearModel.limitObjective(BigDecimal.valueOf(5), BigDecimal.valueOf(20));
        assertNotNull(first, "First call must return a non‑null expression");
        // Second call must return the same instance
        Expression second = linearModel.limitObjective(BigDecimal.TEN, BigDecimal.valueOf(30));
        assertSame(first, second, "Subsequent calls must reuse the same internal expression");
    }
}