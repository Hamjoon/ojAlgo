package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyConstraintQuadratic_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple {@link Expression} implementation that returns the supplied
     * predicate values.
     */
    private static final class DummyExpression extends Expression {

        private final boolean constraint;

        private final boolean redundant;

        private final boolean quadratic;

        DummyExpression(final ExpressionsBasedModel model, final boolean constraint, final boolean redundant, final boolean quadratic) {
            super(model);
            this.constraint = constraint;
            this.redundant = redundant;
            this.quadratic = quadratic;
        }

        @Override
        public boolean isConstraint() {
            return constraint;
        }

        @Override
        public boolean isRedundant() {
            return redundant;
        }

        @Override
        public boolean isAnyQuadraticFactorNonZero() {
            return quadratic;
        }
    }

    /**
     * {@link Expression} that throws a {@link RuntimeException} when
     * {@link #isConstraint()} is called. Used for the “exception propagation”
     * test.
     */
    private static final class ExceptionExpression extends Expression {

        ExceptionExpression(final ExpressionsBasedModel model) {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            throw new RuntimeException("boom");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Inserts a (mocked) {@link Expression} into the private {@code myExpressions}
     * map of the model using reflection.
     *
     * @param name          the key under which the expression will be stored
     * @param isConstraint  value to be returned by {@code expr.isConstraint()}
     * @param isRedundant   value to be returned by {@code expr.isRedundant()}
     * @param hasQuadratic  value to be returned by {@code expr.isAnyQuadraticFactorNonZero()}
     */
    private void injectMockExpression(String name, boolean isConstraint, boolean isRedundant, boolean hasQuadratic) {
        Expression mockExpr = Mockito.mock(Expression.class);
        Mockito.when(mockExpr.isConstraint()).thenReturn(isConstraint);
        Mockito.when(mockExpr.isRedundant()).thenReturn(isRedundant);
        Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(hasQuadratic);
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) field.get(model);
            // Ensure we are not sharing the same map across tests
            if (map == null) {
                map = new HashMap<>();
                field.set(model, map);
            }
            map.put(name, mockExpr);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a raw {@code null} entry into the internal map (used for the
     * “null entry” test case).
     */
    private void injectNullEntry(String name) {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) field.get(model);
            if (map == null) {
                map = new HashMap<>();
                field.set(model, map);
            }
            map.put(name, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates arguments for the parameterised test that covers most
     * combinations of the three predicates.
     *
     * Each argument consists of:
     * - a description (for readability),
     * - an array of expression specifications (each spec is a 3‑boolean array:
     *   {isConstraint, isRedundant, hasQuadratic}),
     * - the expected result (true/false).
     */
    private static Stream<Arguments> provideExpressionScenarios() {
        return Stream.of(// 1. Non‑constraint expression only
        Arguments.of("Non‑constraint expression only", new boolean[][] { { false, false, true } }, false), // 2. Constraint but redundant
        Arguments.of("Constraint but redundant", new boolean[][] { { true, true, true } }, false), // 3. Constraint, non‑redundant, quadratic factor zero
        Arguments.of("Constraint, non‑redundant, quadratic factor zero", new boolean[][] { { true, false, false } }, false), // 4. Constraint, non‑redundant, quadratic factor non‑zero
        Arguments.of("Constraint, non‑redundant, quadratic factor non‑zero", new boolean[][] { { true, false, true } }, true), // 5. Mixed expressions – one satisfying the condition
        Arguments.of("Mixed expressions – one satisfying", new boolean[][] { { true, false, false }, { true, false, true }, { false, false, true } }, true), // 6. Mixed expressions – none satisfying
        Arguments.of("Mixed expressions – none satisfying", new boolean[][] { { true, true, true }, { true, false, false }, { false, false, true } }, false), // 7. Redundant quadratic constraints only
        Arguments.of("Redundant quadratic constraints only", new boolean[][] { { true, true, true }, { true, true, true }, { false, false, true } }, false), // 8. Non‑redundant constraint without quadratic, redundant quadratic present
        Arguments.of("Non‑redundant constraint without quadratic, redundant quadratic present", new boolean[][] { { true, false, false }, { true, true, true }, { false, false, true } }, false), // 9. Multiple non‑redundant quadratic constraints
        Arguments.of("Multiple non‑redundant quadratic constraints", new boolean[][] { { true, false, true }, { true, false, true }, { false, false, false } }, true), // 10. Large model with only non‑constraint expressions
        Arguments.of("Large model with only non‑constraint expressions", new boolean[][] { { false, false, true }, { false, false, false }, { false, false, true }, { false, false, false }, { false, false, true } }, false));
    }

    /**
     * Inserts a (dummy) {@link Expression} into the private {@code myExpressions}
     * map of the model using reflection.
     *
     * @param name          the key under which the expression will be stored
     * @param isConstraint  value to be returned by {@code expr.isConstraint()}
     * @param isRedundant   value to be returned by {@code expr.isRedundant()}
     * @param hasQuadratic  value to be returned by {@code expr.isAnyQuadraticFactorNonZero()}
     */
    private void injectDummyExpression(final String name, final boolean isConstraint, final boolean isRedundant, final boolean hasQuadratic) {
        // Create a fresh expression via the model API – this automatically registers it.
        Expression expr = model.addExpression(name);
        // Make it a constraint if required.
        if (isConstraint) {
            // Use arbitrary bounds – they are only needed to turn the expression into a constraint.
            expr.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        }
        // Mark as redundant if required (the API provides a no‑arg method).
        if (isRedundant) {
            expr.setRedundant();
        }
        // Add a quadratic factor if required.
        if (hasQuadratic) {
            // Ensure there is a variable to use for the quadratic term.
            Variable var = model.addVariable("v_" + name).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
            expr.setQuadraticFactor(var, var, 1.0);
        }
        // The expression is already stored in the model's internal map, but we
        // make sure the map reference exists (some tests replace the map via reflection).
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) field.get(model);
            if (map == null) {
                map = new HashMap<>();
                field.set(model, map);
            }
            map.put(name, expr);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * No expressions at all → the method must return {@code false}.
     */
    @Test
    void testNoExpressions() {
        assertFalse(model.isAnyConstraintQuadratic(), "Empty model should not report any quadratic constraint");
    }

    /**
     * A single linear constraint → the method must return {@code false}.
     */
    @Test
    void testLinearConstraint() {
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Create a constraint expression: 2·x = 4
        Expression expr = model.addExpression("c1").lower(BigDecimal.valueOf(4)).upper(BigDecimal.valueOf(4));
        expr.set(x, BigDecimal.valueOf(2));
        assertFalse(model.isAnyConstraintQuadratic(), "Model with only linear constraints should not be considered quadratic");
    }

    /**
     * A constraint that contains a quadratic factor.
     * The expression  x² ≤ 9  (i.e. quadratic term with coefficient 1) is added.
     */
    @Test
    void testQuadraticConstraint() {
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Create a constraint expression: x² ≤ 9
        Expression expr = model.addExpression("quad").upper(BigDecimal.valueOf(9));
        expr.setQuadraticFactor(x, x, 1.0);
        assertTrue(model.isAnyConstraintQuadratic(), "Model with a quadratic constraint must be reported as such");
    }

    /**
     * A quadratic expression that is *not* a constraint (it contributes only to the objective).
     * The method should still return {@code false} because the quadratic term is not part of a constraint.
     */
    @Test
    void testQuadraticObjectiveOnly() {
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Objective expression: minimise x²
        Expression obj = model.addExpression("obj").weight(BigDecimal.ONE);
        obj.setQuadraticFactor(x, x, 1.0);
        assertFalse(model.isAnyConstraintQuadratic(), "Quadratic objective without any quadratic constraint must return false");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideExpressionScenarios")
    void testPredicateCombinations(String description, boolean[][] specs, boolean expected) {
        // Inject each specification as a real Expression.
        for (int i = 0; i < specs.length; i++) {
            boolean isConstraint = specs[i][0];
            boolean isRedundant = specs[i][1];
            boolean hasQuadratic = specs[i][2];
            injectDummyExpression("expr" + i, isConstraint, isRedundant, hasQuadratic);
        }
        assertEquals(expected, model.isAnyConstraintQuadratic(), "Unexpected result for scenario: " + description);
    }

    /**
     * A null entry in the map should be ignored and not cause NPE.
     */
    @Test
    void testNullEntryIgnoredWhenValidConstraintExists() {
        injectNullEntry("nullExpr");
        injectDummyExpression("valid", true, false, true);
        assertTrue(model.isAnyConstraintQuadratic(), "Presence of a valid non‑redundant quadratic constraint must yield true even with null entries");
    }

    /**
     * When the map contains only a null entry, a NullPointerException is expected.
     */
    @Test
    void testOnlyNullEntryThrowsNPE() {
        injectNullEntry("onlyNull");
        assertThrows(NullPointerException.class, () -> model.isAnyConstraintQuadratic(), "Method should throw NPE when encountering a null expression");
    }

    /**
     * If an expression throws a RuntimeException, it must propagate out of the method.
     */
    @Test
    void testExpressionMethodThrowsRuntimeException() {
        // Create an expression that throws when isConstraint() is called.
        Expression problematic = new ExceptionExpression(model);
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) field.get(model);
            if (map == null) {
                map = new HashMap<>();
                field.set(model, map);
            }
            map.put("bad", problematic);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.isAnyConstraintQuadratic(), "Exception from expression should propagate");
        assertEquals("boom", thrown.getMessage());
    }
}
