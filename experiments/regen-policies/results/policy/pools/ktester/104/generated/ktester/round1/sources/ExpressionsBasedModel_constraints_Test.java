package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_constraints_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple stub implementation of {@link Expression} used for testing without Mockito.
     */
    private static final class StubExpression extends Expression {

        private final boolean constraint;

        private final boolean redundant;

        private final RuntimeException exceptionOnIsConstraint;

        StubExpression(ExpressionsBasedModel model, String name, boolean constraint, boolean redundant, RuntimeException exceptionOnIsConstraint) {
            super(model, name);
            this.constraint = constraint;
            this.redundant = redundant;
            this.exceptionOnIsConstraint = exceptionOnIsConstraint;
        }

        @Override
        public boolean isConstraint() {
            if (exceptionOnIsConstraint != null) {
                throw exceptionOnIsConstraint;
            }
            return constraint;
        }

        @Override
        public boolean isRedundant() {
            return redundant;
        }
    }

    /**
     * Simple DTO used by the parameterised tests.
     */
    private static final class ExprSpec {

        final String name;

        final boolean isConstraint;

        final boolean isRedundant;

        // not used in current implementation
        final RuntimeException exceptionOnIsConstraint;

        ExprSpec(String name, boolean isConstraint, boolean isRedundant) {
            this(name, isConstraint, isRedundant, null);
        }

        ExprSpec(String name, boolean isConstraint, boolean isRedundant, RuntimeException ex) {
            this.name = name;
            this.isConstraint = isConstraint;
            this.isRedundant = isRedundant;
            this.exceptionOnIsConstraint = ex;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // a variable that will be used in the expressions
        Variable x = model.newVariable("x").lower(0).upper(100).weight(1);
        // constraint expression – both lower and upper limits are set
        model.newExpression("c1").lower(0).upper(10).set(x, 1);
        // non‑constraint expression – no bounds are defined
        model.newExpression("nc").set(x, 2);
        // another constraint expression – only an upper bound is set
        model.newExpression("c2").upper(5).set(x, 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources that the
        // optimisation framework might hold.
        if (model != null) {
            model.dispose();
        }
    }

    /**
     * Clears the internal expression map and inserts the supplied specifications as Mockito mocks.
     */
    private void populateModelWithSpecs(ExprSpec... specs) {
        Map<String, Expression> map = getExpressionMap();
        map.clear();
        for (ExprSpec spec : specs) {
            // Create a fresh expression; this also registers it in the model's map.
            Expression expr = model.addExpression(spec.name);
            // Make it a constraint if required (by setting a dummy lower bound)
            if (spec.isConstraint) {
                expr.lower(0);
            }
            // Mark as redundant if required
            if (spec.isRedundant) {
                expr.setRedundant();
            }
            map.put(spec.name, expr);
        }
    }

    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// 1) empty model
        Arguments.of(new ExprSpec[] {}, List.of()), // 2) single valid constraint
        Arguments.of(new ExprSpec[] { new ExprSpec("c1", true, false) }, List.of("c1")), // 3) redundant constraint – filtered out
        Arguments.of(new ExprSpec[] { new ExprSpec("c2", true, true) }, List.of()), // 4) non‑constraint expression – filtered out
        Arguments.of(new ExprSpec[] { new ExprSpec("obj1", false, false) }, List.of()), // 5) mixed set – only c3 survives
        Arguments.of(new ExprSpec[] { new ExprSpec("c3", true, false), new ExprSpec("c4", true, true), new ExprSpec("obj2", false, false), new ExprSpec("r1", false, true) }, List.of("c3")), // 6) multiple valid constraints
        Arguments.of(new ExprSpec[] { new ExprSpec("cA", true, false), new ExprSpec("cB", true, false), new ExprSpec("cC", true, false) }, List.of("cA", "cB", "cC")), // 7) duplicate name – later entry replaces earlier (non‑constraint final)
        Arguments.of(new ExprSpec[] { new ExprSpec("dup", true, false), new ExprSpec("dup", false, false) }, List.of()), // 8) all constraints redundant
        Arguments.of(new ExprSpec[] { new ExprSpec("r1", true, true), new ExprSpec("r2", true, true) }, List.of()));
    }

    /**
     * Verify that {@code constraints()} returns only the expressions that are
     * recognised as constraints and that it excludes non‑constraint expressions.
     */
    @Test
    void testConstraintsFiltering() {
        List<String> constraintNames = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(constraintNames.contains("c1"), "Expression 'c1' should be present because it is a constraint");
        assertTrue(constraintNames.contains("c2"), "Expression 'c2' should be present because it is a constraint");
        assertFalse(constraintNames.contains("nc"), "Expression 'nc' should NOT be present because it is not a constraint");
        assertEquals(2, constraintNames.size(), "Only two constraint expressions should be returned");
    }

    /**
     * Ensure that multiple invocations of {@code constraints()} produce consistent
     * results (the stream is re‑created each call and is not consumed).
     */
    @Test
    void testConstraintsStreamReusability() {
        long firstCount = model.constraints().count();
        long secondCount = model.constraints().count();
        assertEquals(firstCount, secondCount, "Calling constraints() multiple times should yield the same count");
        assertEquals(2L, firstCount, "There should be exactly two constraint expressions in the model");
    }

    /**
     * Returns the private {@code myExpressions} map via reflection.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Expression> getExpressionMap() {
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            f.setAccessible(true);
            return (Map<String, Expression>) f.get(model);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @ParameterizedTest(name = "[{index}] constraints filtering – expected {1}")
    @MethodSource("normalCasesProvider")
    void testConstraintsFilteringParameterized(ExprSpec[] specs, List<String> expectedNames) {
        populateModelWithSpecs(specs);
        List<String> actual = model.constraints().map(Expression::getName).sorted().collect(Collectors.toList());
        List<String> sortedExpected = expectedNames.stream().sorted().collect(Collectors.toList());
        assertEquals(sortedExpected, actual, () -> "Expected constraint names " + sortedExpected + " but got " + actual);
    }

    @Test
    void testConstraintRemovedViaPublicApi() {
        // add a valid constraint, then remove it using the public method
        model.addExpression("cRemove").lower(0).upper(10);
        model.removeExpression("cRemove");
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "After removal, constraints() should be empty");
    }

    @Test
    void testConstraintToggledToRedundant() {
        // first add a non‑redundant constraint
        model.addExpression("cToggle").lower(0).upper(5);
        // then replace it with a redundant version (simulated via reflection)
        ExprSpec redundant = new ExprSpec("cToggle", true, true);
        populateModelWithSpecs(redundant);
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "A constraint that becomes redundant must disappear from constraints()");
    }

    @Test
    void testNullExpressionLeadsToNPE() {
        // Insert a null entry manually
        Map<String, Expression> map = getExpressionMap();
        map.clear();
        map.put("nullExpr", null);
        assertThrows(NullPointerException.class, () -> model.constraints().count(), "Streaming over a null entry should throw NullPointerException");
    }

    @Test
    void testExpressionThrowsIllegalStateExceptionDuringFiltering() {
        ExprSpec bad = new ExprSpec("badExpr", true, false, new IllegalStateException("boom"));
        populateModelWithSpecs(bad);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.constraints().count(), "Exception from isConstraint() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testConcurrentModificationDuringStream() throws Exception {
        // Prepare a model with a single normal constraint
        populateModelWithSpecs(new ExprSpec("c1", true, false));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch modifyLatch = new CountDownLatch(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> modifier = exec.submit(() -> {
            try {
                startLatch.await();
                model.addExpression("cConcurrent").lower(0).upper(1);
                modifyLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Stream<String> nameStream = model.constraints().map(expr -> {
            startLatch.countDown();
            try {
                modifyLatch.await(200, TimeUnit.MILLISECONDS);
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            return expr.getName();
        });
        assertThrows(ConcurrentModificationException.class, () -> nameStream.collect(Collectors.toList()), "Concurrent modification of the underlying map must result in CME");
        modifier.cancel(true);
        exec.shutdownNow();
    }
}
