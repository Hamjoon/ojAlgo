package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.ArrayList;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_reduce_Test {

    private ExpressionsBasedModel model;

    /**
     * Holds the original PRESOLVERS set so we can restore it after each test.
     */
//     private static TreeSet<Presolver> originalPresolvers;

    /**
     * Simple presolver that captures the collection it receives.
     */
//     private static final class CapturePresolver implements Presolver {

        private Collection<Expression> captured;

        @Override
        public void reduce(Collection<Expression> expressions) {
            // make a defensive copy so later modifications do not affect the test data
            this.captured = new ArrayList<>(expressions);
        }

        Collection<Expression> getCaptured() {
            return captured;
        }
    }

    /**
     * Presolver that throws a given exception when invoked.
     */
//     private static final class ThrowingPresolver implements Presolver {

        private final RuntimeException toThrow;

        ThrowingPresolver(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public void reduce(Collection<Expression> expressions) {
            throw toThrow;
        }
    }

    /**
     * Presolver that counts how many times it has been called.
     */
//     private static final class CountingPresolver implements Presolver {

        private int count = 0;

        @Override
        public void reduce(Collection<Expression> expressions) {
            count++;
        }

        int getCount() {
            return count;
        }
    }

    @BeforeAll
    static void captureOriginalPresolvers() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeSet<Presolver> set = (TreeSet<Presolver>) f.get(null);
        originalPresolvers = new TreeSet<>(set);
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    @AfterAll
    static void restoreOriginalPresolvers() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        f.setAccessible(true);
        f.set(null, originalPresolvers);
    }

    /**
     * Utility to clear the static PRESOLVERS set.
     */
    private static void clearPresolvers() {
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            TreeSet<Presolver> set = (TreeSet<Presolver>) f.get(null);
            set.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to read the current size of the static PRESOLVERS set.
     */
    private static int getPresolverCount() {
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            TreeSet<Presolver> set = (TreeSet<Presolver>) f.get(null);
            return set.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReduceReturnsSameInstance() {
        ExpressionsBasedModel returned = model.reduce();
        assertSame(model, returned, "reduce() should return the same model instance");
    }

    @Test
    void testReducePreservesExistingExpression() {
        // GIVEN: a model with a single named expression
        Expression expr = model.addExpression("myExpr");
        expr.lower(0).upper(100).weight(1);
        // WHEN: reduce() is invoked
        ExpressionsBasedModel returned = model.reduce();
        // THEN: the same instance is returned
        assertSame(model, returned, "reduce() should return the same model instance");
        // AND: the expression is still accessible via the public API
        Expression retrieved = model.getExpression("myExpr");
        assertNotNull(retrieved, "Expression should still be present after reduce()");
        assertEquals(expr, retrieved, "Retrieved expression should be the one that was added");
    }

    @Test
    void testReduceOnEmptyModel() {
        // GIVEN: a freshly created model with no expressions
        // WHEN: reduce() is called
        ExpressionsBasedModel returned = model.reduce();
        // THEN: it returns the same instance and does not throw
        assertSame(model, returned, "reduce() should return the same model instance even when empty");
        // The model should still have zero expressions
        assertEquals(0, model.countExpressions(), "Empty model should still have zero expressions after reduce()");
    }

    @Test
    void testReduceForwardsNonEmptyCollection() {
        // GIVEN a model with two expressions
        Expression e1 = model.addExpression("e1");
        e1.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Expression e2 = model.addExpression("e2");
        e2.lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        // Register a capturing presolver
        CapturePresolver captor = new CapturePresolver();
        ExpressionsBasedModel.addPresolver(captor);
        // WHEN
        ExpressionsBasedModel returned = model.reduce();
        // THEN
        assertSame(model, returned);
        Collection<Expression> passed = captor.getCaptured();
        assertNotNull(passed, "Presolver should have been invoked and captured the collection");
        assertEquals(2, passed.size(), "Presolvers.reduce should receive a collection containing both expressions");
        assertTrue(passed.containsAll(model.getExpressions()), "The collection passed to Presolvers.reduce must match the model's expressions");
    }

    @Test
    void testReducePropagatesExceptionFromPresolvers() {
        // GIVEN a model with a single expression
        model.addExpression("bad");
        // Register a presolver that throws
        ThrowingPresolver throwing = new ThrowingPresolver(new RuntimeException("Presolver failure"));
        ExpressionsBasedModel.addPresolver(throwing);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.reduce(), "reduce() should propagate exceptions thrown by Presolvers.reduce");
        assertEquals("Presolver failure", ex.getMessage());
    }

    @Test
    void testReduceIdempotenceMultipleCalls() {
        // GIVEN a model with three expressions
        model.addExpression("a");
        model.addExpression("b");
        model.addExpression("c");
        CountingPresolver counter = new CountingPresolver();
        ExpressionsBasedModel.addPresolver(counter);
        // WHEN first call
        ExpressionsBasedModel first = model.reduce();
        // WHEN second call
        ExpressionsBasedModel second = model.reduce();
        // THEN both return the same instance
        assertSame(model, first);
        assertSame(model, second);
        // Presolvers.reduce should have been invoked twice
        assertEquals(2, counter.getCount(), "Presolver should have been called twice");
    }

    @Test
    void testReduceAfterExpressionRemoval() {
        // GIVEN three expressions
        model.addExpression("x1");
        model.addExpression("x2");
        model.addExpression("x3");
        // REMOVE one
        model.removeExpression("x2");
        CapturePresolver captor = new CapturePresolver();
        ExpressionsBasedModel.addPresolver(captor);
        // WHEN
        model.reduce();
        // Capture the collection passed to Presolvers.reduce
        Collection<Expression> passed = captor.getCaptured();
        assertNotNull(passed, "Presolver should have captured the collection");
        assertEquals(2, passed.size(), "Presolvers.reduce should receive a collection of size 2 after removal");
        assertFalse(passed.stream().anyMatch(e -> "x2".equals(e.getName())), "Removed expression must not be present in the collection");
    }

    @Test
    void testReduceWithPresolverThatMutatesCollection() {
        // GIVEN a model with two expressions
        Expression keep = model.addExpression("keep");
        Expression discard = model.addExpression("discard");
        // Register a custom Presolver that removes the expression named "discard"
        Presolver mutatingPresolver = expressions -> expressions.removeIf(e -> "discard".equals(e.getName()));
        ExpressionsBasedModel.addPresolver(mutatingPresolver);
        // WHEN
        model.reduce();
        // THEN the model should no longer contain the discarded expression
        assertNull(model.getExpression("discard"), "Expression removed by the presolver should no longer be present in the model");
        assertNotNull(model.getExpression("keep"), "Expression not removed by the presolver must still be present");
        assertEquals(1, model.countExpressions(), "Model should contain exactly one expression after the presolver mutation");
    }

    @Test
    void testReduceWithNoRegisteredPresolvers() {
        // Ensure PRESOLVERS set is empty (clearPresolvers() already called in @BeforeEach)
        assertEquals(0, getPresolverCount(), "Presolvers registry should be empty before the test");
        // Add a few expressions – they should simply be ignored by the empty presolver set
        model.addExpression("e1");
        model.addExpression("e2");
        // WHEN
        ExpressionsBasedModel returned = model.reduce();
        // THEN
        assertSame(model, returned);
        assertEquals(2, model.countExpressions(), "Expressions must remain unchanged when no presolvers are registered");
    }

    @Test
    void testReducePropagatesNullPointerExceptionWhenNullExpressionPresent() throws Exception {
        // Inject a null value into the private myExpressions map via reflection
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) mapField.get(model);
        // Ensure the map is mutable (it is a HashMap by default)
        map.put("nullExpr", null);
        // No need to mock Presolvers – the NPE will be thrown when it iterates over the collection
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> model.reduce(), "reduce() should propagate NullPointerException caused by a null expression");
    }
}