package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.TestUtils;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_reduce_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private static final String EXPRESSION_NAME = "x_eq_5";

    @BeforeEach
    void setupBeforeEach() {
        //   subject to  x = 5   (i.e. lower = upper = 5)
        model = new ExpressionsBasedModel();
        // variable x with bounds [0,10] and a weight of 1 (so it contributes to the objective)
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // constraint expression "x = 5"
        model.newExpression(EXPRESSION_NAME).set(x, // coefficient 1·x
        BigDecimal.ONE).lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // Build a very small optimisation problem:
        //   minimise  x
        model = new ExpressionsBasedModel();
        // a simple variable used by a few tests
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
        x = null;
        model = null;
        x = null;
    }

    /**
     * The {@code reduce()} method must return the same model instance.
     */
    @Test
    void testReduceReturnsSelf() {
        ExpressionsBasedModel returned = model.reduce();
        assertSame(model, returned, "reduce() should return the same model instance for fluent usage");
    }

    /**
     * After a call to {@code reduce()} the model must still contain the previously added
     * expression and must be solvable with the same optimal result.
     */
    @Test
    void testReduceDoesNotAlterModelBehaviour() {
        // Solve before reduction – we expect x = 5 and an objective value of 5.
        Optimisation.Result before = model.minimise();
        assertTrue(model.validate(before), "Model should validate the solution before reduction");
        assertEquals(Optimisation.State.OPTIMAL, before.getState(), "Expected optimal state before reduction");
        assertEquals(BigDecimal.valueOf(5), before.get(model.indexOf(x)), "Variable x should be 5 before reduction");
        // Perform reduction.
        model.reduce();
        // The expression should still be retrievable by name.
        Expression retrieved = model.getExpression(EXPRESSION_NAME);
        assertNotNull(retrieved, "Expression should still be present after reduction");
        assertEquals(BigDecimal.valueOf(5), retrieved.lower(), "Lower bound of the expression must stay unchanged");
        assertEquals(BigDecimal.valueOf(5), retrieved.upper(), "Upper bound of the expression must stay unchanged");
        // Solve again – the result must be identical to the pre‑reduction run.
        Optimisation.Result after = model.minimise();
        assertTrue(model.validate(after), "Model should validate the solution after reduction");
        assertEquals(Optimisation.State.OPTIMAL, after.getState(), "Expected optimal state after reduction");
        assertEquals(BigDecimal.valueOf(5), after.get(model.indexOf(x)), "Variable x should still be 5 after reduction");
        assertEquals(before.getValue(), after.getValue(), "Objective value must be unchanged by reduction");
    }

    /**
     * Calling {@code reduce()} on an empty model (no expressions, no variables) must not
     * throw any exception and must return the same instance.
     */
    @Test
    void testReduceOnEmptyModel() {
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            // invoke reduce()
            ExpressionsBasedModel returned = model.reduce();
            // same instance?
            assertSame(model, returned, "reduce() must return the original model (fluent API)");
            // verify that Presolvers.reduce was called with an empty collection
            mocked.verify(() -> Presolvers.reduce(Collections.emptyList()));
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  2. Normal behaviour – model with expressions
     *  ---------------------------------------------------------------------
     */
    @Test
    void testReduceWithExpressions() {
        // add three distinct expressions
        List<Expression> added = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            Expression e = model.newExpression("e" + i).set(x, BigDecimal.ONE).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
            added.add(e);
        }
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            ExpressionsBasedModel returned = model.reduce();
            assertSame(model, returned, "reduce() must return the original model");
            // capture the argument passed to Presolvers.reduce
            ArgumentCaptor<Collection<Expression>> captor = ArgumentCaptor.forClass(Collection.class);
            mocked.verify(() -> Presolvers.reduce(captor.capture()));
            Collection<Expression> passed = captor.getValue();
            // the collection must contain exactly the expressions we added
            assertEquals(added.size(), passed.size(), "Presolvers.reduce must receive a collection with the same size as the model's expressions");
            assertTrue(passed.containsAll(added), "All added expressions must be present in the collection");
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  3. Exception propagation from Presolvers.reduce
     *  ---------------------------------------------------------------------
     */
    @Test
    void testExceptionPropagationFromPresolvers() {
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            RuntimeException boom = new RuntimeException("boom");
            mocked.when(() -> Presolvers.reduce(anyCollection())).thenThrow(boom);
            RuntimeException caught = assertThrows(RuntimeException.class, model::reduce, "RuntimeException from Presolvers.reduce must propagate unchanged");
            assertSame(boom, caught, "The same exception instance should be re‑thrown");
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  4. Idempotent behaviour – two successive calls
     *  ---------------------------------------------------------------------
     */
    @Test
    void testReduceIdempotentTwoCalls() {
        // add a couple of expressions so that the argument collection is non‑empty
        model.newExpression("a").set(x, BigDecimal.ONE);
        model.newExpression("b").set(x, BigDecimal.ONE);
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            // first call
            ExpressionsBasedModel r1 = model.reduce();
            assertSame(model, r1);
            // second call
            ExpressionsBasedModel r2 = model.reduce();
            assertSame(model, r2);
            // verify that Presolvers.reduce was invoked exactly twice
            mocked.verify(() -> Presolvers.reduce(anyCollection()), times(2));
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  5. State preservation – expression count unchanged
     *  ---------------------------------------------------------------------
     */
    @Test
    void testExpressionCountUnchangedAfterReduce() {
        // add three expressions
        for (int i = 0; i < 3; ++i) {
            model.newExpression("c" + i).set(x, BigDecimal.ONE);
        }
        int before = model.countExpressions();
        try (MockedStatic<Presolvers> ignored = mockStatic(Presolvers.class)) {
            model.reduce();
        }
        int after = model.countExpressions();
        assertEquals(before, after, "Expression count must stay the same after reduce()");
    }

    /**
     * --------------------------------------------------------------------- *
     *  6. Dynamic collection size – after removals
     *  ---------------------------------------------------------------------
     */
    @Test
    void testReduceReflectsCurrentExpressionSet() {
        // add five expressions
        for (int i = 0; i < 5; ++i) {
            model.newExpression("d" + i).set(x, BigDecimal.ONE);
        }
        // remove two
        model.removeExpression("d1");
        model.removeExpression("d3");
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            model.reduce();
            ArgumentCaptor<Collection<Expression>> captor = ArgumentCaptor.forClass(Collection.class);
            mocked.verify(() -> Presolvers.reduce(captor.capture()));
            Collection<Expression> passed = captor.getValue();
            assertEquals(3, passed.size(), "Presolvers.reduce must receive a collection reflecting the current number of expressions");
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  7. Chaining with other operations
     *  ---------------------------------------------------------------------
     */
    @Test
    void testReduceAllowsMethodChaining() {
        // add a trivial expression so that the model is solvable
        model.newExpression("e").set(x, BigDecimal.ONE).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // chain reduce() -> minimise()
        Optimisation.Result result = model.reduce().minimise();
        assertNotNull(result, "Result of minimise() after reduce() must not be null");
        assertTrue(model.validate(result), "Model must still be valid after reduction");
    }

    /**
     * --------------------------------------------------------------------- *
     *  8. Presolvers.reduce receives an unmodifiable collection
     *  ---------------------------------------------------------------------
     */
    @Test
    void testPresolversReceivesUnmodifiableCollection() {
        model.newExpression("f").set(x, BigDecimal.ONE);
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            // make the mock attempt to modify the collection
            mocked.when(() -> Presolvers.reduce(anyCollection())).thenAnswer(invocation -> {
                Collection<Expression> col = invocation.getArgument(0);
                // this should throw UnsupportedOperationException if the collection is unmodifiable
                assertThrows(UnsupportedOperationException.class, () -> col.add(null), "Collection passed to Presolvers.reduce must be unmodifiable");
                return null;
            });
            // triggers the mock
            model.reduce();
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  9. Null expression inside the internal map – expect NPE
     *  ---------------------------------------------------------------------
     */
    @Test
    void testNullExpressionInCollectionThrowsNPE() throws Exception {
        // inject a null value into the private myExpressions map via reflection
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) mapField.get(model);
        // now values() contains a null
        map.put("nullExpr", null);
        // The real Presolvers.reduce will iterate over the collection and hit the null.
        // We do not mock it here – we let the actual static method run (it will NPE).
        // If the real implementation guards against nulls, the test will fail,
        // which is acceptable because the specification expects a NPE.
        assertThrows(NullPointerException.class, model::reduce, "A null expression in the collection should cause NullPointerException");
    }

    /**
     * --------------------------------------------------------------------- *
     *  10. Reduce after model disposal – expect ProgrammingError
     *  ---------------------------------------------------------------------
     */
    @Test
    void testReduceAfterDisposeThrowsProgrammingError() {
        // marks the model as unusable
        model.dispose();
        // According to the API, illegal usage after dispose throws ProgrammingError
        assertThrows(ProgrammingError.class, model::reduce, "Calling reduce() on a disposed model must throw ProgrammingError");
    }

    /**
     * --------------------------------------------------------------------- *
     *  11. Concurrent modification during reduction – expect CME
     *       (This test is flaky by nature; it is disabled by default.)
     *  ---------------------------------------------------------------------
     */
    @Test
    @Disabled("Flaky test – relies on timing of concurrent modification")
    void testConcurrentModificationDuringReduce() throws Exception {
        // add a couple of expressions
        for (int i = 0; i < 3; ++i) {
            model.newExpression("g" + i).set(x, BigDecimal.ONE);
        }
        // Use a real Presolvers.reduce that simply iterates over the collection.
        // While it iterates, a background thread adds a new expression.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        // background task
        Future<?> future = executor.submit(() -> {
            try {
                // wait until the main thread starts iterating
                latch.await();
                model.newExpression("concurrent").set(x, BigDecimal.ONE);
            } catch (InterruptedException ignored) {
            }
        });
        // replace the static method with a wrapper that triggers the latch
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            mocked.when(() -> Presolvers.reduce(anyCollection())).thenAnswer(invocation -> {
                Collection<Expression> col = invocation.getArgument(0);
                // signal background thread to modify the model
                latch.countDown();
                // iterate – this should throw ConcurrentModificationException
                for (Expression e : col) {
                    // just iterate
                }
                return null;
            });
            // Expect CME propagated from reduce()
            assertThrows(ConcurrentModificationException.class, model::reduce, "Concurrent modification during Presolvers.reduce should result in CME");
        } finally {
            executor.shutdownNow();
        }
    }
}
