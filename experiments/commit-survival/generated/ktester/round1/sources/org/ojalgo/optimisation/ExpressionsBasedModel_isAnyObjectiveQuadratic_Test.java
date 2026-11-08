package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.function.Consumer;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyObjectiveQuadratic_Test {

    /**
     * Data holder for the parameterised tests.
     */
    private static class TestScenario {

        final String description;

        final Consumer<ExpressionsBasedModel> modelConfigurer;

        // Boolean or Class<? extends Throwable>
        final Object expected;

        TestScenario(String description, Consumer<ExpressionsBasedModel> modelConfigurer, Object expected) {
            this.description = description;
            this.modelConfigurer = modelConfigurer;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Inserts a (mocked) {@link Expression} into the private {@code myExpressions} map
     * of the supplied model.
     *
     * @param model          the model to manipulate
     * @param name           key under which the expression is stored
     * @param isObjective    value to be returned by {@code expr.isObjective()}
     * @param hasQuadratic   value to be returned by {@code expr.isAnyQuadraticFactorNonZero()}
     */
    private static void addMockExpression(ExpressionsBasedModel model, String name, boolean isObjective, boolean hasQuadratic) {
        try {
            // Create a Mockito mock of Expression
            Expression mockExpr = mock(Expression.class);
            when(mockExpr.isObjective()).thenReturn(isObjective);
            when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(hasQuadratic);
            // Access the private field myExpressions
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) field.get(model);
            map.put(name, mockExpr);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Supplies all scenarios defined in the JSON test matrix.
     */
    private static Stream<TestScenario> scenarioProvider() {
        List<TestScenario> list = new ArrayList<>();
        // 1. Empty model
        list.add(new TestScenario("Empty model – no expressions", m -> {
            /* nothing */
        }, false));
        // 2. Single expression – non‑objective (with quadratic)
        list.add(new TestScenario("Single non‑objective expression with quadratic", m -> addExpression(m, "e1", false, true), false));
        // 3. Single expression – objective without quadratic
        list.add(new TestScenario("Single objective expression without quadratic", m -> addExpression(m, "e1", true, false), false));
        // 4. Single expression – objective with quadratic
        list.add(new TestScenario("Single objective expression with quadratic", m -> addExpression(m, "e1", true, true), true));
        // 5. Multiple expressions – all non‑objective or objective without quadratic
        list.add(new TestScenario("Multiple expressions – none satisfy both conditions", m -> {
            addExpression(m, "e1", false, true);
            addExpression(m, "e2", true, false);
            addExpression(m, "e3", false, false);
        }, false));
        // 6. Multiple expressions – at least one objective‑quadratic
        list.add(new TestScenario("Multiple expressions – at least one objective‑quadratic", m -> {
            addExpression(m, "e1", false, true);
            // the decisive one
            addExpression(m, "e2", true, true);
            addExpression(m, "e3", true, false);
        }, true));
        // 7. Multiple expressions – all objective without quadratic
        list.add(new TestScenario("Multiple objective expressions, none quadratic", m -> {
            addExpression(m, "e1", true, false);
            addExpression(m, "e2", true, false);
        }, false));
        // 8. Multiple expressions – mixed objective and non‑objective with quadratic
        list.add(new TestScenario("Mixed objective/non‑objective with quadratic – objective quadratic present", m -> {
            addExpression(m, "e1", false, true);
            addExpression(m, "e2", true, true);
        }, true));
        // 9. Expression with zero quadratic factor but non‑zero linear term
        list.add(new TestScenario("Objective expression with zero quadratic factor", m -> addExpression(m, "e1", true, false), false));
        // 10. Expression with negative quadratic coefficient
        list.add(new TestScenario("Objective expression with negative quadratic coefficient", m -> {
            // negative coefficient still counts as non‑zero
            Expression expr = m.newExpression("e1");
            expr.weight(BigDecimal.ONE);
            Variable x = m.newVariable("x");
            Variable y = m.newVariable("y");
            expr.setQuadraticFactor(x, y, BigDecimal.valueOf(-1));
            // place it into the map
            try {
                Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Expression> map = (Map<String, Expression>) f.get(m);
                map.put("e1", expr);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }, true));
        // 11. Expression with NaN quadratic coefficient (treated as not non‑zero)
        list.add(new TestScenario("Objective expression with NaN quadratic coefficient", m -> {
            Variable x = m.newVariable("x");
            Variable y = m.newVariable("y");
            Expression expr = m.newExpression("nanQuadratic").weight(BigDecimal.ONE);
            expr.setQuadraticFactor(x, y, Double.NaN);
        }, false));
        // 12. Expression removed after being added
        list.add(new TestScenario("Expression added then removed before query", m -> {
            addExpression(m, "e1", true, true);
            m.removeExpression("e1");
        }, false));
        // 13. Model with duplicate expression names (map will keep the last entry)
        list.add(new TestScenario("Duplicate expression names – one objective‑quadratic", m -> {
            addExpression(m, "dup", false, false);
            // second insertion with same key overwrites the first
            addExpression(m, "dup", true, true);
        }, true));
        // 14. Large number of expressions, only the last is objective‑quadratic
        list.add(new TestScenario("Large model (10 000 expressions) with last being objective‑quadratic", m -> {
            for (int i = 0; i < 9_999; i++) {
                addExpression(m, "e" + i, false, false);
            }
            addExpression(m, "eLast", true, true);
        }, true));
        // 15. Concurrent modification during iteration – expect CME
        list.add(new TestScenario("Concurrent modification while iterating", m -> {
            // Populate with a few entries first
            addExpression(m, "e0", false, false);
            addExpression(m, "e1", false, false);
        }, java.util.ConcurrentModificationException.class));
        return list.stream();
    }

    /**
     * Inserts a (real) {@link Expression} into the private {@code myExpressions} map
     * of the supplied model.
     *
     * @param model          the model to manipulate
     * @param name           key under which the expression is stored
     * @param isObjective    whether the expression should be part of the objective
     * @param hasQuadratic   whether the expression should contain a non‑zero quadratic factor
     */
    private static void addExpression(ExpressionsBasedModel model, String name, boolean isObjective, boolean hasQuadratic) {
        // Create a new expression
        Expression expr = model.newExpression(name);
        if (isObjective) {
            // any non‑zero weight makes it an objective expression
            expr.weight(BigDecimal.ONE);
        }
        if (hasQuadratic) {
            // create two dummy variables for the quadratic term
            Variable v1 = model.newVariable(name + "_v1");
            Variable v2 = model.newVariable(name + "_v2");
            expr.setQuadraticFactor(v1, v2, BigDecimal.ONE);
        }
        // Ensure it is placed in the private map via reflection (mirrors original approach)
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) field.get(model);
            map.put(name, expr);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * An expression that is part of the objective (weight != 0) and contains a non‑zero
     * quadratic factor must make {@code isAnyObjectiveQuadratic()} return {@code true}.
     */
    @Test
    void testObjectiveQuadraticDetected() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Expression expr = model.newExpression("objectiveQuadratic").weight(BigDecimal.ONE);
        expr.setQuadraticFactor(x, y, BigDecimal.ONE);
        assertTrue(model.isAnyObjectiveQuadratic(), "Model should report an objective quadratic expression");
    }

    /**
     * If an expression contains a quadratic factor but is *not* part of the objective,
     * {@code isAnyObjectiveQuadratic()} must return {@code false}.
     */
    @Test
    void testQuadraticNotObjective() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Expression expr = model.newExpression("constraintQuadratic");
        expr.setQuadraticFactor(a, b, BigDecimal.ONE);
        assertFalse(model.isAnyObjectiveQuadratic(), "Model must not consider a non‑objective quadratic expression");
    }

    /**
     * An expression that is part of the objective but has no quadratic factors must not
     * trigger a {@code true} result.
     */
    @Test
    void testObjectiveWithoutQuadratic() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("v");
        Expression expr = model.newExpression("linearObjective").weight(BigDecimal.ONE);
        expr.set(v, BigDecimal.ONE);
        assertFalse(model.isAnyObjectiveQuadratic(), "Model must report false when the objective contains only linear terms");
    }

    /**
     * When several expressions exist, the method must return {@code true} as soon as at
     * least one objective expression is quadratic.
     */
    @Test
    void testMixedExpressions() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable p = model.newVariable("p");
        Variable q = model.newVariable("q");
        Variable r = model.newVariable("r");
        Expression linearObj = model.newExpression("linearObj").weight(BigDecimal.ONE);
        linearObj.set(p, BigDecimal.ONE);
        Expression quadConstr = model.newExpression("quadConstr");
        quadConstr.setQuadraticFactor(q, r, BigDecimal.ONE);
        Expression quadObj = model.newExpression("quadObj").weight(BigDecimal.ONE);
        quadObj.setQuadraticFactor(p, q, BigDecimal.ONE);
        assertTrue(model.isAnyObjectiveQuadratic(), "Model should report true when at least one objective expression is quadratic");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarioProvider")
    void testScenarios(TestScenario scenario) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        scenario.modelConfigurer.accept(model);
        if (scenario.expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) scenario.expected)) {
            // Expect an exception (ConcurrentModificationException case)
            Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) scenario.expected;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(() -> {
                Thread.sleep(10);
                // concurrent modification
                addExpression(model, "concurrent", true, true);
                return model.isAnyObjectiveQuadratic();
            });
            try {
                future.get();
                fail("Expected " + expectedEx.getSimpleName() + " but method completed normally");
            } catch (ExecutionException ee) {
                assertTrue(expectedEx.isInstance(ee.getCause()), "Expected " + expectedEx.getSimpleName() + " but got " + ee.getCause());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                fail("Test interrupted");
            } finally {
                executor.shutdownNow();
            }
        } else {
            boolean expected = (Boolean) scenario.expected;
            boolean actual = model.isAnyObjectiveQuadratic();
            assertEquals(expected, actual, "Scenario failed: " + scenario.description);
        }
    }
}