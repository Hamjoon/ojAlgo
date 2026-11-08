package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyConstraintQuadratic_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    class BadExpression extends Expression {

        BadExpression() {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            throw new IllegalStateException("boom");
        }
    }

    class BadExpression extends Expression {

        BadExpression() {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            return true;
        }

        @Override
        public boolean isRedundant() {
            throw new IllegalArgumentException("invalid");
        }
    }

    class BadExpression extends Expression {

        BadExpression() {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            return true;
        }

        @Override
        public boolean isRedundant() {
            return false;
        }

        @Override
        public boolean isAnyQuadraticFactorNonZero() {
            throw new RuntimeException("runtime");
        }
    }

    /**
     * Helper data holder for configuring an {@link Expression}.
     */
    private static final class MockConfig {

        final boolean isConstraint;

        final boolean isRedundant;

        final boolean hasQuadratic;

        MockConfig(boolean isConstraint, boolean isRedundant, boolean hasQuadratic) {
            this.isConstraint = isConstraint;
            this.isRedundant = isRedundant;
            this.hasQuadratic = hasQuadratic;
        }
    }

    class BadExpression extends Expression {

        BadExpression() {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            throw new IllegalStateException("boom");
        }
    }

    class BadExpression extends Expression {

        BadExpression() {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            return true;
        }

        @Override
        public boolean isRedundant() {
            throw new IllegalArgumentException("invalid");
        }
    }

    class BadExpression extends Expression {

        BadExpression() {
            super(model);
        }

        @Override
        public boolean isConstraint() {
            return true;
        }

        @Override
        public boolean isRedundant() {
            return false;
        }

        @Override
        public boolean isAnyQuadraticFactorNonZero() {
            throw new RuntimeException("runtime");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for each test
        model = new ExpressionsBasedModel();
        // create two variables that can be used in the “real” expression tests
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(ONE);
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(ONE);
    }

    /**
     * Provides a stream of arguments for the parameterised test.
     * Each argument consists of:
     * <ul>
     *   <li>a description (used only for readability)</li>
     *   <li>a list of {@link MockConfig} describing the mock expressions to be added</li>
     *   <li>the expected boolean result of {@code isAnyConstraintQuadratic()}</li>
     * </ul>
     */
    private static Stream<Arguments> provideMockScenarios() {
        return Stream.of(// 1. No expressions at all – should be false (already covered, but kept for completeness)
        Arguments.of("No expressions", Collections.emptyList(), false), // 2. Single expression that is NOT a constraint
        Arguments.of("Single non‑constraint expression", List.of(new MockConfig(false, false, false)), false), // 3. Single expression that is a constraint but redundant
        Arguments.of("Single redundant constraint", List.of(new MockConfig(true, true, false)), false), // 4. Single non‑redundant constraint with no quadratic factor
        Arguments.of("Constraint without quadratic factor", List.of(new MockConfig(true, false, false)), false), // 5. Single non‑redundant constraint with a quadratic factor
        Arguments.of("Quadratic constraint", List.of(new MockConfig(true, false, true)), true), // 6. Multiple expressions, only one satisfies all three predicates
        Arguments.of("Mixed expressions, one quadratic constraint", List.of(// linear constraint
        new MockConfig(true, false, false), // non‑constraint quadratic
        new MockConfig(false, false, true), // the wanted quadratic constraint
        new MockConfig(true, false, true)), true), // 7. All expressions are quadratic constraints – result must be true
        Arguments.of("All quadratic constraints", List.of(new MockConfig(true, false, true), new MockConfig(true, false, true), new MockConfig(true, false, true)), true), // 8. All constraints are redundant – result false even if quadratic
        Arguments.of("Redundant quadratic constraints only", List.of(new MockConfig(true, true, true), new MockConfig(true, true, true)), false));
    }

    /**
     * Clears the private {@code myExpressions} map of the given model instance.
     */
    private static void clearInternalExpressionMap(ExpressionsBasedModel model) throws Exception {
        var field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        map.clear();
    }

    /**
     * Inserts a (key, expression) pair into the private {@code myExpressions} map.
     *
     * @param model the model whose map is to be modified
     * @param key   the key under which the expression will be stored
     * @param expr  the expression instance (may be {@code null} for the NPE test)
     */
    private static void addMockExpressionToModel(ExpressionsBasedModel model, String key, Expression expr) throws Exception {
        var field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        map.put(key, expr);
    }

    @Test
    void testNoExpressions() {
        // The model contains only variables – no expressions have been added.
        assertFalse(model.isAnyConstraintQuadratic(), "A model without expressions must not report a quadratic constraint");
    }

    @Test
    void testLinearConstraintOnly() {
        // Create a linear constraint:  x + 2*y = 5
        Expression expr = model.newExpression("linearConstraint").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        expr.set(x, ONE);
        expr.set(y, TWO);
        assertFalse(model.isAnyConstraintQuadratic(), "A model with only linear constraints must not report a quadratic constraint");
    }

    @Test
    void testQuadraticConstraintDetected() {
        // Create a quadratic constraint:  x² + y = 4
        Expression expr = model.newExpression("quadraticConstraint").lower(BigDecimal.valueOf(4)).upper(BigDecimal.valueOf(4));
        // linear part
        expr.set(y, ONE);
        // quadratic part – the public API provides a method that accepts two variables and a coefficient.
        // adds 1·x·x
        expr.quadratic(x, x, ONE);
        assertTrue(model.isAnyConstraintQuadratic(), "A model containing a quadratic constraint must be reported as such");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideMockScenarios")
    void testMockedExpressions(String description, List<MockConfig> configs, boolean expected) throws Exception {
        // Clear any existing expressions (the map may contain entries from previous tests)
        clearInternalExpressionMap(model);
        int counter = 0;
        for (MockConfig cfg : configs) {
            // Build a real Expression that mimics the required behaviour
            Expression expr = model.newExpression("mockExpr" + counter);
            if (cfg.isConstraint) {
                // Setting any bound makes it a constraint; we use a dummy bound.
                expr.lower(BigDecimal.ZERO);
            }
            if (cfg.isRedundant) {
                expr.setRedundant(true);
            }
            if (cfg.hasQuadratic) {
                // Add a simple quadratic term; the actual coefficient does not matter for the test.
                expr.quadratic(x, x, ONE);
            }
            addMockExpressionToModel(model, "mockExpr" + counter, expr);
            counter++;
        }
        // Execute the method under test
        boolean actual = model.isAnyConstraintQuadratic();
        // Verify expectation
        assertEquals(expected, actual, () -> "Unexpected result for scenario: " + description);
    }

    @Test
    void testNullExpressionEntryThrowsNPE() throws Exception {
        // Insert a null value into the internal map
        clearInternalExpressionMap(model);
        addMockExpressionToModel(model, "nullExpr", null);
        assertThrows(NullPointerException.class, () -> model.isAnyConstraintQuadratic(), "A null entry in the expression map should cause NPE");
    }

    @Test
    void testIsConstraintThrowsIllegalStateException() throws Exception {
        // Create an expression that will throw when isConstraint() is called.
        // We achieve this by subclassing Expression and overriding the method.
        class BadExpression extends Expression {

            BadExpression() {
                super(model);
            }

            @Override
            public boolean isConstraint() {
                throw new IllegalStateException("boom");
            }
        }
        clearInternalExpressionMap(model);
        addMockExpressionToModel(model, "badExpr", new BadExpression());
        assertThrows(IllegalStateException.class, () -> model.isAnyConstraintQuadratic(), "Exception from isConstraint() must propagate");
    }

    @Test
    void testIsRedundantThrowsIllegalArgumentException() throws Exception {
        class BadExpression extends Expression {

            BadExpression() {
                super(model);
            }

            @Override
            public boolean isConstraint() {
                return true;
            }

            @Override
            public boolean isRedundant() {
                throw new IllegalArgumentException("invalid");
            }
        }
        clearInternalExpressionMap(model);
        addMockExpressionToModel(model, "badExpr", new BadExpression());
        assertThrows(IllegalArgumentException.class, () -> model.isAnyConstraintQuadratic(), "Exception from isRedundant() must propagate");
    }

    @Test
    void testIsAnyQuadraticFactorNonZeroThrowsRuntimeException() throws Exception {
        class BadExpression extends Expression {

            BadExpression() {
                super(model);
            }

            @Override
            public boolean isConstraint() {
                return true;
            }

            @Override
            public boolean isRedundant() {
                return false;
            }

            @Override
            public boolean isAnyQuadraticFactorNonZero() {
                throw new RuntimeException("runtime");
            }
        }
        clearInternalExpressionMap(model);
        addMockExpressionToModel(model, "badExpr", new BadExpression());
        assertThrows(RuntimeException.class, () -> model.isAnyConstraintQuadratic(), "Exception from isAnyQuadraticFactorNonZero() must propagate");
    }
}
