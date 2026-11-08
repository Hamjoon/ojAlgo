package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_constraints_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    /**
     * Helper class describing how an expression should be configured for a test case.
     */
    private static class ExprSpec {

        final String name;

        final boolean isConstraint;

        final boolean isRedundant;

        ExprSpec(String name, boolean isConstraint, boolean isRedundant) {
            this.name = name;
            this.isConstraint = isConstraint;
            this.isRedundant = isRedundant;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures isolation.
        model = new ExpressionsBasedModel();
        // Two variables are enough to build non‑trivial expressions.
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit disposal.
        model.dispose();
    }

    /**
     * Parameter source for simple filtering scenarios.
     */
    private static Stream<Arguments> simpleFilteringProvider() {
        return Stream.of(// 1) Empty model → empty stream
        Arguments.of(List.of(), List.of()), // 2) Single valid constraint c1
        Arguments.of(List.of(new ExprSpec("c1", true, false)), List.of("c1")), // 3) Single redundant constraint cRedundant
        Arguments.of(List.of(new ExprSpec("cRedundant", true, true)), List.of()), // 4) Single non‑constraint expression
        Arguments.of(List.of(new ExprSpec("nonConstraint", false, false)), List.of()), // 5) Mixed four expressions, expecting cA and cB in order
        Arguments.of(List.of(new ExprSpec("cA", true, false), new ExprSpec("cB", true, false), new ExprSpec("cRedundant", true, true), new ExprSpec("nonConstraint", false, false)), List.of("cA", "cB")));
    }

    /**
     * Parameter source for mixed‑constraint scenarios (order not important).
     */
    private static Stream<Arguments> mixedConstraintsProvider() {
        return Stream.of(// a) three constraints (c1,c2,c3) and two non‑constraints
        Arguments.of(List.of(new ExprSpec("c1", true, false), new ExprSpec("c2", true, false), new ExprSpec("c3", true, false), new ExprSpec("nr1", false, false), new ExprSpec("nr2", false, false)), Set.of("c1", "c2", "c3")), // b) all constraints redundant → empty
        Arguments.of(List.of(new ExprSpec("cA", true, true), new ExprSpec("cB", true, true), new ExprSpec("nr", false, false)), Set.of()), // c) add then remove a constraint
        Arguments.of(List.of(new ExprSpec("cX", true, false)), Set.of()));
    }

    /**
     * Verify that only expressions with a bound are returned by {@code constraints()}.
     */
    @Test
    void testConstraintsFiltering() {
        // Expression that is a genuine constraint (has both lower and upper bounds)
        Expression c1 = model.newExpression("c1").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8));
        c1.set(x, BigDecimal.ONE);
        c1.set(y, BigDecimal.ONE);
        // Expression that is only part of the objective (no bounds)
        Expression obj = model.newExpression("obj").weight(BigDecimal.ONE);
        obj.set(x, BigDecimal.valueOf(3));
        obj.set(y, BigDecimal.valueOf(4));
        // Collect the names of the constraints returned by the stream
        List<String> constraintNames = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(constraintNames.contains("c1"), "The constraint expression should be present in the stream");
        assertFalse(constraintNames.contains("obj"), "Objective‑only expressions must not appear in the constraints stream");
        assertEquals(1, constraintNames.size(), "Exactly one constraint should be reported");
    }

    /**
     * Verify that the stream returned by {@code constraints()} can be traversed
     * multiple times without side‑effects.
     */
    @Test
    void testConstraintsStreamReusability() {
        // Two distinct constraints
        Expression cA = model.newExpression("cA").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        cA.set(x, BigDecimal.ONE);
        Expression cB = model.newExpression("cB").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(10));
        cB.set(y, BigDecimal.ONE);
        // First traversal
        long firstCount = model.constraints().count();
        // Second traversal – should yield the same result
        long secondCount = model.constraints().count();
        assertEquals(2L, firstCount, "First traversal should count both constraints");
        assertEquals(firstCount, secondCount, "Second traversal must produce the same count");
    }

    /**
     * Ensure that a constraint marked as redundant is omitted.
     * An expression becomes redundant when it has no variables and both bounds are equal.
     * (In ojAlgo this situation is interpreted as a constant that is always satisfied.)
     */
    @Test
    void testRedundantConstraintIsFilteredOut() {
        // Redundant constraint: constant expression 5 == 5 (no variables)
        Expression redundant = model.newExpression("redundant").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // Normal constraint for comparison
        Expression normal = model.newExpression("normal").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        normal.set(x, BigDecimal.ONE);
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.contains("normal"), "Normal constraint must be present");
        assertFalse(names.contains("redundant"), "Redundant constant constraint must be filtered out");
        assertEquals(1, names.size(), "Only the non‑redundant constraint should be returned");
    }

    /**
     * Verify basic filtering and ordering for a variety of small models.
     */
    @ParameterizedTest(name = "[{index}] model with {0}")
    @MethodSource("simpleFilteringProvider")
    void testSimpleFiltering(List<ExprSpec> specs, List<String> expectedNames) throws Exception {
        // Populate the model according to the specification.
        for (ExprSpec spec : specs) {
            Expression expr = model.newExpression(spec.name);
            if (spec.isConstraint) {
                // Give the expression a non‑trivial bound to make it a constraint.
                expr.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
            }
            // Redundant flag is derived from the expression state:
            // - If redundant is requested, we create a constant expression with equal bounds.
            if (spec.isRedundant) {
                // Remove any variables and set equal bounds.
                expr.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
            }
            // Non‑constraint expressions simply have no bounds (or we could set a weight).
            if (!spec.isConstraint && !spec.isRedundant) {
                expr.weight(BigDecimal.ONE);
            }
        }
        List<String> actual = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertEquals(expectedNames, actual, () -> "Expected constraint names " + expectedNames + " but got " + actual);
    }

    /**
     * Tests for mixed constraints and the effect of removal.
     */
    @ParameterizedTest(name = "[{index}] mixed constraints")
    @MethodSource("mixedConstraintsProvider")
    void testMixedConstraints(List<ExprSpec> specs, Set<String> expectedNames) throws Exception {
        for (ExprSpec spec : specs) {
            Expression expr = model.newExpression(spec.name);
            if (spec.isConstraint) {
                expr.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
            }
            if (spec.isRedundant) {
                expr.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
            }
            if (!spec.isConstraint && !spec.isRedundant) {
                expr.weight(BigDecimal.ONE);
            }
        }
        // Special case: the third argument set expects removal of "cX"
        if (expectedNames.isEmpty() && specs.size() == 1 && specs.get(0).name.equals("cX")) {
            model.removeExpression("cX");
        }
        Set<String> actual = model.constraints().map(Expression::getName).collect(Collectors.toSet());
        assertEquals(expectedNames, actual, () -> "Expected constraint set " + expectedNames + " but got " + actual);
    }

    /**
     * Verify that a {@code null} entry inside the internal expression map causes a
     * {@link NullPointerException} when the stream is evaluated.
     */
    @Test
    void testNullExpressionEntryThrowsNPE() throws Exception {
        // Insert a valid constraint first – the test is about the null entry.
        model.newExpression("valid").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // Use reflection to put a null value into the private map.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        map.put("nullEntry", null);
        // The stream evaluation should trigger NPE when the filter accesses the null.
        assertThrows(NullPointerException.class, () -> model.constraints().count(), "Expected NullPointerException due to null map entry");
    }

    /**
     * Create a mock {@link Expression} whose {@code isConstraint()} throws a
     * {@link RuntimeException}. The exception must propagate out of {@code constraints()}.
     */
    @Test
    void testExpressionThrowsInIsConstraint() throws Exception {
        Expression badExpr = Mockito.mock(Expression.class);
        Mockito.when(badExpr.getName()).thenReturn("bad");
        Mockito.when(badExpr.isConstraint()).thenThrow(new RuntimeException("boom in isConstraint"));
        // isRedundant should not be called, but stub it anyway.
        Mockito.when(badExpr.isRedundant()).thenReturn(false);
        // Insert the mock into the model via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        map.put("bad", badExpr);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.constraints().count(), "Exception from isConstraint() should propagate");
        assertEquals("boom in isConstraint", ex.getMessage());
    }

    /**
     * Create a mock {@link Expression} whose {@code isRedundant()} throws a
     * {@link RuntimeException}. The exception must propagate out of {@code constraints()}.
     */
    @Test
    void testExpressionThrowsInIsRedundant() throws Exception {
        Expression badExpr = Mockito.mock(Expression.class);
        Mockito.when(badExpr.getName()).thenReturn("bad");
        Mockito.when(badExpr.isConstraint()).thenReturn(true);
        Mockito.when(badExpr.isRedundant()).thenThrow(new RuntimeException("boom in isRedundant"));
        // Insert the mock into the model via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        map.put("bad", badExpr);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.constraints().count(), "Exception from isRedundant() should propagate");
        assertEquals("boom in isRedundant", ex.getMessage());
    }

    /**
     * Verify that modifying the model after obtaining the stream leads to a
     * {@link ConcurrentModificationException} during a terminal operation.
     */
    @Test
    void testConcurrentModificationDuringStreamEvaluation() {
        // Obtain a stream before any modification.
        Stream<Expression> stream = model.constraints();
        // Add a new constraint after the stream has been created.
        model.newExpression("new").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // The terminal operation should detect the concurrent modification.
        assertThrows(ConcurrentModificationException.class, stream::count, "Expected ConcurrentModificationException after model modification");
    }

    /**
     * Additional sanity check: an empty model must yield an empty constraints stream.
     */
    @Test
    void testEmptyModelReturnsEmptyStream() {
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "Empty model should produce an empty constraints stream");
    }
}
