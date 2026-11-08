package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_reduce_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple test‑side helper that records the collection passed to {@code Presolvers.reduce}
     * and can be configured to throw a specific exception or execute custom behaviour.
     */
    static class PresolversTestHelper {

        static Collection<Expression> lastReceived;

        static RuntimeException toThrow;

        static int callCount = 0;

        static java.util.function.Consumer<Collection<Expression>> customBehaviour;

        static void reset() {
            lastReceived = null;
            toThrow = null;
            callCount = 0;
            customBehaviour = null;
        }
    }

    /**
     * Minimal replacement for the real {@code Presolvers} class used only in tests.
     *
     * <p>
     * The real library provides a richer implementation; for the purpose of these unit
     * tests we only need a static {@code reduce} method that forwards the collection to
     * {@link ExpressionsBasedModel_reduce_Test.PresolversTestHelper}.
     * </p>
     */
    static class Presolvers {

        static void reduce(Collection<Expression> expressions) {
            PresolversTestHelper.lastReceived = expressions;
            PresolversTestHelper.callCount++;
            // Execute any custom behaviour supplied by a test.
            if (PresolversTestHelper.customBehaviour != null) {
                PresolversTestHelper.customBehaviour.accept(expressions);
            }
            // Throw a configured exception if present.
            if (PresolversTestHelper.toThrow != null) {
                RuntimeException ex = PresolversTestHelper.toThrow;
                // Reset so that subsequent calls behave normally.
                PresolversTestHelper.toThrow = null;
                throw ex;
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add a couple of dummy expressions.
        model = new ExpressionsBasedModel();
        // Adding simple expressions – they do not need to be solved, only to be present in the
        // collection passed to Presolvers.reduce().
        model.addExpression("exprA");
        model.addExpression("exprB");
        // Reset the test helper before each test.
        PresolversTestHelper.reset();
    }

    @Test
    void testReduceReturnsSameInstanceAndInvokesPresolvers() {
        // Invoke the method under test.
        ExpressionsBasedModel returned = model.reduce();
        // Verify that the same instance is returned.
        assertSame(model, returned, "reduce() should return the original model instance");
        // The captured collection should contain the two expressions we added.
        Collection<Expression> passedExpressions = PresolversTestHelper.lastReceived;
        assertNotNull(passedExpressions, "Presolvers.reduce should receive a non‑null collection");
        assertEquals(2, passedExpressions.size(), "Presolvers.reduce should be called with a collection containing all model expressions");
    }

    /**
     * Helper to obtain the private {@code myExpressions} map via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Expression> getExpressionsMap(ExpressionsBasedModel mdl) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        return (Map<String, Expression>) f.get(mdl);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceSingleExpression() throws Exception {
        model.addExpression("expr1");
        ExpressionsBasedModel returned = model.reduce();
        assertSame(model, returned, "reduce() must return the original model instance");
        Collection<Expression> passed = PresolversTestHelper.lastReceived;
        assertNotNull(passed);
        assertEquals(1, passed.size(), "Exactly one expression should be forwarded");
        assertEquals("expr1", passed.iterator().next().getName());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceEmptyExpressionSet() {
        // Create a fresh model with no expressions.
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        PresolversTestHelper.reset();
        ExpressionsBasedModel returned = emptyModel.reduce();
        assertSame(emptyModel, returned);
        assertTrue(PresolversTestHelper.lastReceived.isEmpty(), "Presolvers.reduce should receive an empty collection");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceWithNullExpressionEntry() throws Exception {
        // Insert a null value into the private map.
        Map<String, Expression> map = getExpressionsMap(model);
        map.put("nullKey", null);
        ExpressionsBasedModel returned = model.reduce();
        assertSame(model, returned);
        Collection<Expression> passed = PresolversTestHelper.lastReceived;
        assertEquals(1, passed.size());
        assertTrue(passed.contains(null), "The forwarded collection must contain the null entry");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReducePropagatesRuntimeException() {
        model.addExpression("e");
        RuntimeException ex = new RuntimeException("boom");
        PresolversTestHelper.toThrow = ex;
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.reduce(), "RuntimeException should propagate unchanged");
        assertSame(ex, thrown);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceMultipleExpressions() {
        model.addExpression("a");
        model.addExpression("b");
        model.addExpression("c");
        ExpressionsBasedModel returned = model.reduce();
        assertSame(model, returned);
        Collection<Expression> passed = PresolversTestHelper.lastReceived;
        assertEquals(3, passed.size(), "All three expressions must be forwarded");
        Set<String> names = new HashSet<>();
        for (Expression e : passed) {
            names.add(e.getName());
        }
        assertTrue(names.containsAll(Arrays.asList("a", "b", "c")));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceIdempotentCalls() {
        model.addExpression("x");
        model.addExpression("y");
        // first call
        assertSame(model, model.reduce());
        // second call
        assertSame(model, model.reduce());
        // The helper should have recorded two separate calls.
        assertEquals(2, PresolversTestHelper.callCount);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceAfterRemoval() {
        model.addExpression("one");
        model.addExpression("two");
        model.addExpression("three");
        model.removeExpression("two");
        model.reduce();
        Collection<Expression> passed = PresolversTestHelper.lastReceived;
        assertEquals(2, passed.size());
        Set<String> names = new HashSet<>();
        for (Expression e : passed) {
            names.add(e.getName());
        }
        assertTrue(names.containsAll(Arrays.asList("one", "three")));
        assertFalse(names.contains("two"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceDuplicateExpressionInstances() throws Exception {
        Expression expr = model.addExpression("dup");
        // Insert the same instance under a different key via reflection.
        Map<String, Expression> map = getExpressionsMap(model);
        map.put("dupCopy", expr);
        model.reduce();
        Collection<Expression> passed = PresolversTestHelper.lastReceived;
        assertEquals(2, passed.size());
        Iterator<Expression> it = passed.iterator();
        Expression first = it.next();
        Expression second = it.next();
        assertSame(expr, first);
        assertSame(expr, second);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReducePropagatesNullPointerException() {
        model.addExpression("e");
        NullPointerException npe = new NullPointerException("null");
        PresolversTestHelper.toThrow = npe;
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> model.reduce());
        assertSame(npe, thrown);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReducePropagatesIllegalArgumentException() {
        model.addExpression("e");
        IllegalArgumentException iae = new IllegalArgumentException("bad");
        PresolversTestHelper.toThrow = iae;
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> model.reduce());
        assertSame(iae, thrown);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceConcurrentModificationException() throws Exception {
        model.addExpression("a");
        model.addExpression("b");
        PresolversTestHelper.customBehaviour = col -> {
            Iterator<Expression> it = col.iterator();
            if (it.hasNext()) {
                // advance once
                it.next();
                // Remove an entry via reflection – this will cause CME on next iteration.
                Map<String, Expression> map = getExpressionsMap(model);
                map.remove("a");
            }
            // Continue iterating to trigger the exception.
            while (it.hasNext()) {
                it.next();
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> model.reduce(), "Concurrent modification should propagate as ConcurrentModificationException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReduceClassCastExceptionFromNonExpressionEntry() throws Exception {
        // Insert a non‑Expression object.
        Map<String, Expression> map = getExpressionsMap(model);
        @SuppressWarnings("unchecked")
        Expression bad = (Expression) (Object) "I am not an Expression";
        map.put("bad", bad);
        PresolversTestHelper.customBehaviour = col -> {
            for (Object o : col) {
                // Force a cast to Expression – the bad entry will trigger ClassCastException.
                @SuppressWarnings("unused")
                Expression e = (Expression) o;
            }
        };
        assertThrows(ClassCastException.class, () -> model.reduce(), "A non‑Expression entry must cause ClassCastException");
    }
}