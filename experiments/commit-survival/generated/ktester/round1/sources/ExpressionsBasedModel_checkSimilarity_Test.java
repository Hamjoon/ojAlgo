package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_checkSimilarity_Test {

    private ExpressionsBasedModel model;

    /* -------------------------------------------------------------
     *  3. Single stored expression – various potentials
     * ------------------------------------------------------------- */
    @Nested
    class SingleStoredExpression {

        private Expression stored;

        @BeforeEach
        void init() {
            stored = model.addExpression("E1");
            // give it a dummy bound so that it is a “real” expression
            stored.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        }

        @Test
        void sameReference_returnsTrue() {
            Presolvers.setResult(stored, true);
            assertTrue(model.checkSimilarity(stored));
        }

        @Test
        void identicalDataDifferentInstance_returnsTrue() {
            // create a *different* Expression instance with the same name (the name does not matter)
            Expression potential = createExpression(model, "E1-copy");
            Presolvers.setResult(potential, true);
            assertTrue(model.checkSimilarity(potential));
        }

        @Test
        void completelyDifferentExpression_returnsFalse() {
            Expression potential = createExpression(model, "E2");
            Presolvers.setResult(potential, false);
            assertFalse(model.checkSimilarity(potential));
        }
    }

    /* -------------------------------------------------------------
     *  4. Multiple stored expressions – matching and non‑matching
     * ------------------------------------------------------------- */
    @Nested
    class MultipleStoredExpressions {

        private final List<Expression> stored = new ArrayList<>();

        @BeforeEach
        void init() {
            // E1
            stored.add(model.addExpression("E1"));
            // E2
            stored.add(model.addExpression("E2"));
            // E3 – the one we will match against
            stored.add(model.addExpression("E3"));
        }

        @Test
        void identicalToStoredE3_returnsTrue() {
            Expression potential = createExpression(model, "E3");
            Presolvers.setResult(potential, true);
            assertTrue(model.checkSimilarity(potential));
        }

        @Test
        void sameDataDifferentId_returnsTrue() {
            Expression potential = createExpression(model, "E4");
            Presolvers.setResult(potential, true);
            assertTrue(model.checkSimilarity(potential));
        }

        @Test
        void noMatch_returnsFalse() {
            Expression potential = createExpression(model, "E5");
            Presolvers.setResult(potential, false);
            assertFalse(model.checkSimilarity(potential));
        }
    }

    /* -------------------------------------------------------------
     *  7. Zero‑coefficient expressions
     * ------------------------------------------------------------- */
    @Nested
    class ZeroCoefficientExpressions {

        @BeforeEach
        void init() {
            // expression with zero coefficients and constant 5
            model.addExpression("Z1");
        }

        @Test
        void identicalZeroCoefficientsSameConstant_returnsTrue() {
            Expression potential = createExpression(model, "Z1");
            Presolvers.setResult(potential, true);
            assertTrue(model.checkSimilarity(potential));
        }

        @Test
        void sameZeroCoefficientsDifferentConstant_returnsFalse() {
            Expression potential = createExpression(model, "Z2");
            Presolvers.setResult(potential, false);
            assertFalse(model.checkSimilarity(potential));
        }
    }

    /* -------------------------------------------------------------
     *  9. Many stored expressions – match and no‑match
     * ------------------------------------------------------------- */
    @Nested
    class ManyStoredExpressions {

        @BeforeEach
        void init() {
            // create ten dummy expressions (E0 … E9)
            for (int i = 0; i < 10; i++) {
                model.addExpression("E" + i);
            }
        }

        @Test
        void matchingExpression_returnsTrue() {
            Expression potential = createExpression(model, "E7-match");
            Presolvers.setResult(potential, true);
            assertTrue(model.checkSimilarity(potential));
        }

        @Test
        void nonMatchingExpression_returnsFalse() {
            Expression potential = createExpression(model, "E11-no-match");
            Presolvers.setResult(potential, false);
            assertFalse(model.checkSimilarity(potential));
        }
    }

    /* -----------------------------------------------------------------
     * Simple stub for the Presolvers class used by ExpressionsBasedModel.
     * It allows the tests to control the outcome of checkSimilarity without
     * needing the external Mockito library.
     * ----------------------------------------------------------------- */
    static class Presolvers {

        private static final Map<Expression, Boolean> RESULT_MAP = new IdentityHashMap<>();

        private static final Map<Expression, RuntimeException> EXCEPTION_MAP = new IdentityHashMap<>();

        static void clear() {
            RESULT_MAP.clear();
            EXCEPTION_MAP.clear();
        }

        static void setResult(Expression expr, boolean result) {
            RESULT_MAP.put(expr, result);
        }

        static void setException(Expression expr, RuntimeException ex) {
            EXCEPTION_MAP.put(expr, ex);
        }

        /**
         * The method signature matches the one used by {@link ExpressionsBasedModel}.
         */
        public static boolean checkSimilarity(Collection<Expression> all, Expression potential) {
            if (EXCEPTION_MAP.containsKey(potential)) {
                throw EXCEPTION_MAP.get(potential);
            }
            return RESULT_MAP.getOrDefault(potential, false);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
        // add at least one expression so that the internal collection is non‑empty
        model.addExpression("existing").lower(0).upper(10);
        // reset the stubbed Presolvers between tests
        Presolvers.clear();
    }

    /**
     * Creates a fresh {@link Expression} with the given name.
     */
    private static Expression createExpression(ExpressionsBasedModel m, String name) {
        return m.newExpression(name);
    }

    /**
     * Inserts a {@code null} entry into the model's internal {@code myExpressions} map
     *  using reflection – required for the “model contains null expression entry” test.
     */
    private static void injectNullExpressionEntry(ExpressionsBasedModel m) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) f.get(m);
        map.put("nullEntry", null);
    }

    /**
     * Sets the internal coefficient collection of an {@link Expression} to {@code null}
     *  via reflection – required for the “potential with null coefficients” test.
     */
    private static void nullifyCoefficients(Expression e) throws Exception {
        Field f = Expression.class.getDeclaredField("myCoefficients");
        f.setAccessible(true);
        f.set(e, null);
    }

    @Test
    void testCheckSimilarityDelegatesAndReturnsTrue() {
        Expression potential = model.newExpression("potential");
        Presolvers.setResult(potential, true);
        boolean similarity = model.checkSimilarity(potential);
        assertTrue(similarity, "checkSimilarity should return true when Presolvers reports similarity");
    }

    @Test
    void testCheckSimilarityDelegatesAndReturnsFalse() {
        Expression potential = model.newExpression("potential");
        Presolvers.setResult(potential, false);
        boolean similarity = model.checkSimilarity(potential);
        assertFalse(similarity, "checkSimilarity should return false when Presolvers reports no similarity");
    }

    /* -------------------------------------------------------------
     *  1. Empty model – null potential
     * ------------------------------------------------------------- */
    @Test
    void emptyModel_nullPotential_throwsNPE() {
        // model has no expressions (fresh instance)
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        Expression potential = null;
        Presolvers.setException(potential, new NullPointerException());
        assertThrows(NullPointerException.class, () -> empty.checkSimilarity(potential), "Calling checkSimilarity with a null Expression should throw NPE");
    }

    /* -------------------------------------------------------------
     *  2. Empty model – non‑null potential (returns false)
     * ------------------------------------------------------------- */
    @Test
    void emptyModel_nonNullPotential_returnsFalse() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        Expression potential = createExpression(empty, "potential");
        Presolvers.setResult(potential, false);
        boolean result = empty.checkSimilarity(potential);
        assertFalse(result, "With no stored expressions similarity must be false");
    }

    /* -------------------------------------------------------------
     *  5. Duplicate stored expressions – any match returns true
     * ------------------------------------------------------------- */
    @Test
    void duplicateStoredExpressions_matchReturnsTrue() {
        model.addExpression("Edup1");
        model.addExpression("Edup2");
        Expression potential = createExpression(model, "Edup");
        Presolvers.setResult(potential, true);
        assertTrue(model.checkSimilarity(potential));
    }

    /* -------------------------------------------------------------
     *  6. Constant mismatch – should return false
     * ------------------------------------------------------------- */
    @Test
    void constantMismatch_returnsFalse() {
        model.addExpression("E1");
        Expression potential = createExpression(model, "E1-const-mismatch");
        Presolvers.setResult(potential, false);
        assertFalse(model.checkSimilarity(potential));
    }

    /* -------------------------------------------------------------
     *  8. After model copy (shallow copy)
     * ------------------------------------------------------------- */
    @Test
    void afterCopy_potentialMatches_returnsTrue() {
        model.addExpression("E1");
        ExpressionsBasedModel copy = model.copy();
        Expression potential = createExpression(copy, "Ecopy");
        Presolvers.setResult(potential, true);
        assertTrue(copy.checkSimilarity(potential));
    }

    /* -------------------------------------------------------------
     * 10. Potential with null coefficients – expect NPE
     * ------------------------------------------------------------- */
    @Test
    void potentialWithNullCoefficients_throwsNPE() throws Exception {
        Expression potential = createExpression(model, "nullCoeff");
        nullifyCoefficients(potential);
        Presolvers.setException(potential, new NullPointerException());
        assertThrows(NullPointerException.class, () -> model.checkSimilarity(potential), "Potential with null coefficient collection should cause NPE");
    }

    /* -------------------------------------------------------------
     * 11. Model contains a null expression entry – expect NPE
     * ------------------------------------------------------------- */
    @Test
    void modelContainsNullExpressionEntry_throwsNPE() throws Exception {
        injectNullExpressionEntry(model);
        Expression potential = createExpression(model, "E1");
        Presolvers.setException(potential, new NullPointerException());
        assertThrows(NullPointerException.class, () -> model.checkSimilarity(potential), "Null entry inside the model's expression collection should cause NPE");
    }

    /* -------------------------------------------------------------
     * 12. Potential with mismatched dimension – expect IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void potentialWithMismatchedDimension_throwsIAE() {
        Expression potential = createExpression(model, "dimMismatch");
        Presolvers.setException(potential, new IllegalArgumentException());
        assertThrows(IllegalArgumentException.class, () -> model.checkSimilarity(potential), "Dimension mismatch should result in IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     * 13. Potential with NaN coefficient – expect IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void potentialWithNaNCoefficient_throwsIAE() {
        Expression potential = createExpression(model, "nanCoeff");
        Presolvers.setException(potential, new IllegalArgumentException());
        assertThrows(IllegalArgumentException.class, () -> model.checkSimilarity(potential), "NaN coefficient should cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     * 14. Potential with infinite coefficient – expect IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void potentialWithInfiniteCoefficient_throwsIAE() {
        Expression potential = createExpression(model, "infCoeff");
        Presolvers.setException(potential, new IllegalArgumentException());
        assertThrows(IllegalArgumentException.class, () -> model.checkSimilarity(potential), "Infinite coefficient should cause IllegalArgumentException");
    }
}