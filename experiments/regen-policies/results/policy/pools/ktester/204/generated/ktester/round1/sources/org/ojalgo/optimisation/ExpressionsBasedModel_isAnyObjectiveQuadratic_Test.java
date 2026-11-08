package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyObjectiveQuadratic_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
        x = null;
        y = null;
    }

    @Test
    void testNoExpressions() {
        // The model contains no expressions – therefore no quadratic objective can exist.
        assertFalse(model.isAnyObjectiveQuadratic(), "Expected false when the model has no expressions");
    }

    @Test
    void testQuadraticConstraintOnly() {
        // Create a quadratic expression that is a *constraint* (no weight → not an objective).
        Expression expr = model.addExpression("quadraticConstraint").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
        // Add a non‑zero quadratic factor (x * y).
        expr.setQuadraticFactor(x, y, ONE);
        // Even though the expression is quadratic, it is not an objective.
        assertFalse(model.isAnyObjectiveQuadratic(), "Expected false when only quadratic constraints are present");
    }

    @Test
    void testLinearObjectiveOnly() {
        // Create an objective expression that is purely linear.
        Expression expr = model.addExpression("linearObjective").weight(ONE);
        // Linear term only.
        expr.set(x, ONE);
        expr.set(y, ONE.negate());
        assertFalse(model.isAnyObjectiveQuadratic(), "Expected false when the objective is linear only");
    }

    @Test
    void testObjectiveWithQuadraticFactor() {
        // Create an objective expression that contains a quadratic term.
        Expression expr = model.addExpression("quadraticObjective").weight(ONE);
        // Add a non‑zero quadratic factor (x * y).
        expr.setQuadraticFactor(x, y, ONE);
        // The method should now detect a quadratic objective.
        assertTrue(model.isAnyObjectiveQuadratic(), "Expected true when an objective expression has a non‑zero quadratic factor");
    }

    @ParameterizedTest
    @CsvSource({ // description, hasObjective, hasQuadratic, expectedResult
    "'single linear objective', true, false, false", "'single quadratic constraint', false, true, false", "'objective + quadratic', true, true, true", "'multiple, none qualifying', false, false, false", "'multiple, one qualifying', true, true, true" })
    void testMixedExpressions(String description, boolean objective, boolean quadratic, boolean expected) {
        // Helper to create an expression according to the parameters
        Expression e = model.addExpression("expr-" + description.replace(' ', '-'));
        if (objective) {
            // mark as objective
            e.weight(ONE);
        }
        if (quadratic) {
            e.setQuadraticFactor(x, y, ONE);
        }
        // For the "multiple" cases we add extra expressions that do NOT satisfy both conditions
        if (description.startsWith("multiple")) {
            // a pure linear objective
            Expression linObj = model.addExpression("linObj").weight(ONE);
            linObj.set(x, ONE);
            // a pure quadratic constraint
            Expression quadCon = model.addExpression("quadCon").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
            quadCon.setQuadraticFactor(x, y, ONE);
        }
        assertEquals(expected, model.isAnyObjectiveQuadratic(), "Failed scenario: " + description);
    }

    /**
     * Inserts a {@code null} value into the private {@code myExpressions} map.
     * The method under test does not guard against null entries, so a
     * {@link NullPointerException} is expected.
     */
    @Test
    void testNullExpressionEntryThrowsNPE() throws Exception {
        // Access the private map via reflection
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) f.get(model);
        map.put("nullExpr", null);
        assertThrows(NullPointerException.class, () -> model.isAnyObjectiveQuadratic(), "Expected NullPointerException when a null expression is present");
    }

    /**
     * Mocks an {@link Expression} that throws {@link IllegalStateException}
     * from {@code isObjective()}. The exception should propagate unchanged.
     */
    @Test
    void testExpressionThrowsIllegalStateException() throws Exception {
        Expression mockExpr = mock(Expression.class);
        when(mockExpr.isObjective()).thenThrow(new IllegalStateException("forced"));
        // The other method is never called, but we provide a safe default.
        when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        // Insert the mock into the private map
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) f.get(model);
        map.put("badExpr", mockExpr);
        assertThrows(IllegalStateException.class, () -> model.isAnyObjectiveQuadratic(), "Expected IllegalStateException to propagate from expression");
    }

    /**
     * Simulates a concurrent modification by having an expression modify the model
     * while {@code isAnyObjectiveQuadratic()} iterates over the expression map.
     * This should result in a {@link ConcurrentModificationException}.
     */
    @Test
    void testConcurrentModificationDuringIteration() throws Exception {
        Expression modifyingExpr = mock(Expression.class);
        // When isObjective() is called, add a new expression to the model.
        when(modifyingExpr.isObjective()).thenAnswer(invocation -> {
            model.addExpression("newDuringIteration").weight(ONE);
            return false;
        });
        when(modifyingExpr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        // Insert the mock into the private map
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) f.get(model);
        map.put("modifyingExpr", modifyingExpr);
        assertThrows(ConcurrentModificationException.class, () -> model.isAnyObjectiveQuadratic(), "Expected ConcurrentModificationException when the model is modified during iteration");
    }
}