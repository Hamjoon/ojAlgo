package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isFixed_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the model will be garbage‑collected
        model = null;
    }

    private static Stream<Arguments> mixedFixedFreeProvider() {
        return Stream.of(// all fixed → true
        Arguments.of(new int[] { 5, 5, 5 }, true, "All variables fixed"), // mixed → false
        Arguments.of(new int[] { 0, 10, 5, 5 }, false, "At least one free variable"));
    }

    /**
     * When the model contains no variables {@code isFixed()} should return {@code true}
     * because the underlying {@code allMatch} on an empty stream evaluates to {@code true}.
     */
    @Test
    void testIsFixedWithNoVariables() {
        assertTrue(model.isFixed(), "A model without variables should be considered fixed.");
    }

    /**
     * All variables are explicitly fixed (lower bound equals upper bound). The method
     * must therefore return {@code true}.
     */
    @Test
    void testIsFixedWhenAllVariablesAreFixed() {
        // three independent variables, each fixed to a concrete value
        Variable v1 = model.addVariable().lower(5).upper(5);
        Variable v2 = model.addVariable().lower(-3).upper(-3);
        // level sets both bounds to 0
        Variable v3 = model.addVariable().level(0);
        // sanity check that the variables report themselves as fixed
        assertTrue(v1.isFixed());
        assertTrue(v2.isFixed());
        assertTrue(v3.isFixed());
        assertTrue(model.isFixed(), "Model should be fixed when every variable is fixed.");
    }

    /**
     * At least one variable is not fixed (its lower and upper bounds differ). The method
     * must therefore return {@code false}.
     */
    @Test
    void testIsFixedWhenAtLeastOneVariableIsNotFixed() {
        // fixed variable
        Variable fixed = model.addVariable().lower(2).upper(2);
        // non‑fixed variable (range 0 … 10)
        Variable free = model.addVariable().lower(0).upper(10);
        // another fixed variable using level()
        Variable alsoFixed = model.addVariable().level(7);
        assertTrue(fixed.isFixed());
        assertFalse(free.isFixed());
        assertTrue(alsoFixed.isFixed());
        assertFalse(model.isFixed(), "Model should not be fixed when at least one variable is free.");
    }

    /**
     * Single variable that is fixed.
     */
    @Test
    void testIsFixedSingleVariableFixed() {
        model.addVariable().lower(10).upper(10);
        assertTrue(model.isFixed(), "A model with a single fixed variable must be reported as fixed.");
    }

    /**
     * Single variable that is free (different lower/upper).
     */
    @Test
    void testIsFixedSingleVariableFree() {
        model.addVariable().lower(0).upper(5);
        assertFalse(model.isFixed(), "A model with a single free variable must be reported as not fixed.");
    }

    /**
     * Integer variable that is not fixed (range 1 … 4).
     */
    @Test
    void testIsFixedIntegerVariableNotFixed() {
        // The integer flag does not influence the fixed‑ness logic, we only need a range.
        model.addVariable().lower(1).upper(4);
        assertFalse(model.isFixed(), "An integer variable with a range is not fixed.");
    }

    /**
     * Variable with contradictory bounds (lower > upper).
     */
    @Test
    void testIsFixedContradictoryBounds() {
        model.addVariable().lower(8).upper(3);
        assertFalse(model.isFixed(), "A variable whose lower bound exceeds its upper bound is not fixed.");
    }

    /**
     * Dynamic bounds change – fixed → free → fixed again.
     */
    @Test
    void testIsFixedDynamicBoundsChange() {
        Variable v = model.addVariable().lower(7).upper(7);
        assertTrue(model.isFixed(), "Initially the variable is fixed.");
        // make it free
        v.upper(9);
        assertFalse(model.isFixed(), "After widening the upper bound the model is not fixed.");
        // fix it again
        v.lower(9);
        assertTrue(model.isFixed(), "When lower == upper again the model becomes fixed.");
    }

    /**
     * Mixed types after removal – only a fixed variable remains.
     */
    @Test
    void testIsFixedMixedTypesAfterRemoval() throws Exception {
        // add two free variables and one fixed variable
        Variable free1 = model.addVariable().lower(0).upper(5);
        Variable free2 = model.addVariable().lower(-2).upper(2);
        Variable fixed = model.addVariable().lower(3).upper(3);
        // remove the two free variables via reflection (simulating a “removal” operation)
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        list.remove(free1);
        list.remove(free2);
        assertTrue(model.isFixed(), "After removing the free variables only the fixed one remains, so isFixed() must be true.");
    }

    /**
     * Null internal variable list – should throw NullPointerException.
     */
    @Test
    void testIsFixedNullVariablesList() throws Exception {
        // set the private field myVariables to null
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, null);
        assertThrows(NullPointerException.class, model::isFixed, "Calling isFixed() when the internal variable list is null must throw NPE.");
    }

    /**
     * Concurrent modification while streaming – expect ConcurrentModificationException.
     */
    @Test
    void testIsFixedConcurrentModification() throws Exception {
        // Create a mock Variable that blocks inside isFixed()
        Variable blockingVar = mock(Variable.class);
        CountDownLatch latch = new CountDownLatch(1);
        when(blockingVar.isFixed()).thenAnswer(invocation -> {
            // signal that we entered isFixed()
            latch.countDown();
            // keep the thread occupied
            Thread.sleep(200);
            return true;
        });
        // Insert the blocking variable as the first element in the internal list
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) f.get(model);
        // now the list has one element that will block
        vars.add(blockingVar);
        // Start a thread that will modify the list while the main thread evaluates isFixed()
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> modifier = executor.submit(() -> {
            try {
                // Wait until the main thread has entered the blocking isFixed()
                latch.await();
                // Modify the list – this should trigger a CME in the stream iterator
                model.addVariable().lower(0).upper(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        // The call should throw ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, model::isFixed, "Modifying the variable list during stream processing must cause CME.");
        // Clean up the executor
        executor.shutdownNow();
    }

    /**
     * Variable whose isFixed() throws a RuntimeException – the exception must propagate.
     */
    @Test
    void testIsFixedVariableThrowsException() throws Exception {
        // Create a mock Variable that throws when isFixed() is invoked
        Variable explodingVar = mock(Variable.class);
        when(explodingVar.isFixed()).thenThrow(new RuntimeException("boom"));
        // Insert the mock into the model's internal list
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) f.get(model);
        vars.add(explodingVar);
        RuntimeException ex = assertThrows(RuntimeException.class, model::isFixed, "The RuntimeException from a variable's isFixed() must propagate.");
        assertEquals("boom", ex.getMessage());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("mixedFixedFreeProvider")
    void testIsFixedMixedScenarios(int[] bounds, boolean expected, String description) {
        // bounds are interpreted as lower,upper pairs (size must be even)
        for (int i = 0; i < bounds.length; i += 2) {
            model.addVariable().lower(bounds[i]).upper(bounds[i + 1]);
        }
        if (expected) {
            assertTrue(model.isFixed(), description);
        } else {
            assertFalse(model.isFixed(), description);
        }
    }
}
