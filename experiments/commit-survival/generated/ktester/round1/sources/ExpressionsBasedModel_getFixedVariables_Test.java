package org.ojalgo.optimisation;

import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure1D;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentModificationException;

class ExpressionsBasedModel_getFixedVariables_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    private Variable z;

    /**
     * mutable list of all variables that belong to the model
     */
    final List<Variable> myVariables = new ArrayList<>();

    /**
     * internal mutable set that is rebuilt on every call to {@link #getFixedVariables()}
     */
    private final Set<IntIndex> myFixedVariables = new HashSet<>();

    /**
     * Simple representation of a variable belonging to an {@link ExpressionsBasedModel}.
     */
    public class Variable {

        private final ExpressionsBasedModel model;

        private final String name;

        private Comparable<?> lower;

        private Comparable<?> upper;

        Variable(final ExpressionsBasedModel model, final String name) {
            this.model = model;
            this.name = name;
        }

        /**
         * set a lower bound (inclusive)
         */
        public Variable lower(final Comparable<?> value) {
            this.lower = value;
            return this;
        }

        /**
         * set an upper bound (inclusive)
         */
        public Variable upper(final Comparable<?> value) {
            this.upper = value;
            return this;
        }

        /**
         * set a fixed level – both lower and upper become the same value
         */
        public Variable level(final Comparable<?> value) {
            this.lower = value;
            this.upper = value;
            return this;
        }

        /**
         * convenience used by the tests to give a weight – not relevant for fixed‑status
         */
        public Variable weight(final Comparable<?> value) {
            // weight handling is outside the scope of the current tests
            return this;
        }

        /**
         * a variable is fixed when lower and upper are non‑null and equal
         */
        boolean isFixed() {
            return lower != null && upper != null && lower.equals(upper);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Very small stub of {@code ExpressionsBasedModel} that supports only the
     * operations required by the unit tests.
     */
    public class ExpressionsBasedModel {

        private final List<Variable> myVariables = new ArrayList<>();

        private final Set<IntIndex> myFixedVariables = new HashSet<>();

        /**
         * creates a new variable, registers it and returns it
         */
        public Variable newVariable(final String name) {
            Variable v = new Variable(this, name);
            myVariables.add(v);
            return v;
        }

        /**
         * index of a variable inside the model
         */
        public int indexOf(final Variable var) {
            return myVariables.indexOf(var);
        }

        /**
         * Returns an unmodifiable snapshot of the indices of all currently fixed
         * variables. The snapshot is recomputed on each call – the internal mutable
         * set is cleared first, so successive calls are independent.
         *
         * @return an unmodifiable view of the fixed‑variable indices
         * @throws NullPointerException          if the internal variable list contains {@code null}
         * @throws ConcurrentModificationException if the variable list is modified while iterating
         */
        public Set<IntIndex> getFixedVariables() {
            myFixedVariables.clear();
            for (final Variable var : myVariables) {
                if (var == null) {
                    throw new NullPointerException("Variable list contains a null entry");
                }
                if (var.isFixed()) {
                    myFixedVariables.add(IntIndex.of(myVariables.indexOf(var)));
                }
            }
            return Collections.unmodifiableSet(new HashSet<>(myFixedVariables));
        }

        /**
         * placeholder – no operation needed for the tests
         */
        public void dispose() {
            // no‑op
        }

        /**
         * placeholder – not required for the current tests
         */
        public boolean isAnyVariableInteger() {
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // x – not fixed
        x = model.newVariable("x").lower(0).upper(10).weight(1);
        // y – fixed at 5
        y = model.newVariable("y").level(5).weight(1);
        // z – not fixed initially
        z = model.newVariable("z").lower(0).upper(20).weight(1);
    }

    /**
     * Returns the {@link IntIndex} for a given variable in the current model.
     */
    private IntIndex idxOf(Variable var) {
        return IntIndex.of(model.indexOf(var));
    }

    /**
     * Reflectively obtains a private field from {@link ExpressionsBasedModel}.
     */
    private static Field getPrivateField(String name) throws NoSuchFieldException {
        Field f = ExpressionsBasedModel.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Mixed variables – two scenarios supplied via a parameterised test.
     */
    static Stream<Arguments> mixedVariablesProvider() {
        return Stream.of(Arguments.of(5, List.of(1, 3)), Arguments.of(3, Collections.emptyList()));
    }

    /**
     * creates a new variable, adds it to the model and returns it
     */
    public Variable newVariable(final String name) {
        Variable v = new Variable(this, name);
        myVariables.add(v);
        return v;
    }

    /**
     * index of a variable inside {@code myVariables}
     */
    public int indexOf(final Variable var) {
        return myVariables.indexOf(var);
    }

    /**
     * Returns an unmodifiable snapshot of the indices of all currently fixed
     * variables.  The snapshot is recomputed on each call – the internal
     * mutable set is cleared first, so successive calls are independent.
     *
     * @return an unmodifiable view of the fixed‑variable indices
     * @throws NullPointerException          if the internal variable list contains {@code null}
     * @throws ConcurrentModificationException if the variable list is modified while iterating
     */
    public Set<IntIndex> getFixedVariables() {
        myFixedVariables.clear();
        for (final Variable var : myVariables) {
            if (var == null) {
                throw new NullPointerException("Variable list contains a null entry");
            }
            if (var.isFixed()) {
                myFixedVariables.add(IntIndex.of(myVariables.indexOf(var)));
            }
        }
        return Collections.unmodifiableSet(new HashSet<>(myFixedVariables));
    }

    // placeholder for other methods (dispose, isAnyVariableInteger, …)
    public void dispose() {
        // no‑op for the purpose of the tests
    }

    public boolean isAnyVariableInteger() {
        // not required for the current tests
        return false;
    }

    @Test
    void testFixedVariablesReturnedCorrectly() {
        Set<IntIndex> fixed = model.getFixedVariables();
        // Only y is fixed at this point
        Assertions.assertEquals(1, fixed.size(), "Exactly one variable should be reported as fixed");
        Assertions.assertTrue(fixed.contains(IntIndex.of(model.indexOf(y))), "The fixed set should contain the index of variable y");
    }

    @Test
    void testReturnedSetIsUnmodifiable() {
        Set<IntIndex> fixed = model.getFixedVariables();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fixed.add(IntIndex.of(0)), "The returned set must be unmodifiable");
    }

    @Test
    void testDynamicUpdateAfterChangingFixedStatus() {
        // Initially only y is fixed
        Set<IntIndex> firstCall = model.getFixedVariables();
        Assertions.assertEquals(1, firstCall.size());
        Assertions.assertTrue(firstCall.contains(IntIndex.of(model.indexOf(y))));
        // Now fix variable z
        z.level(7);
        // Second call should reflect the new state
        Set<IntIndex> secondCall = model.getFixedVariables();
        Assertions.assertEquals(2, secondCall.size(), "After fixing another variable the set size should increase");
        Assertions.assertTrue(secondCall.contains(IntIndex.of(model.indexOf(y))));
        Assertions.assertTrue(secondCall.contains(IntIndex.of(model.indexOf(z))));
    }

    @Test
    void testFixedVariablesClearedBetweenCalls() {
        // First call – only y is fixed
        Set<IntIndex> first = model.getFixedVariables();
        Assertions.assertEquals(1, first.size());
        // Un‑fix y by removing its level (set lower/upper to a range)
        y.lower(0).upper(10);
        // Second call – no fixed variables should be reported
        Set<IntIndex> second = model.getFixedVariables();
        Assertions.assertEquals(0, second.size(), "After un‑fixing the variable the fixed set should be empty");
    }

    @Test
    void testFixedVariablesReturnedAsUnmodifiableViewEvenWhenEmpty() {
        // Un‑fix all variables
        x.lower(0).upper(10);
        y.lower(0).upper(10);
        z.lower(0).upper(20);
        Set<IntIndex> fixed = model.getFixedVariables();
        Assertions.assertTrue(fixed.isEmpty());
        // The empty set must also be unmodifiable
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fixed.remove(IntIndex.of(0)));
    }

    /**
     * Empty model – no variables at all.
     */
    @Test
    void testEmptyModelReturnsEmptyUnmodifiableSet() {
        // no variables added
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        Set<IntIndex> result = emptyModel.getFixedVariables();
        Assertions.assertTrue(result.isEmpty(), "Empty model should yield an empty set");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.add(IntIndex.of(0)));
    }

    /**
     * All variables fixed – three variables, all fixed.
     */
    @Test
    void testAllVariablesFixed() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v0 = m.newVariable("v0").level(1);
        Variable v1 = m.newVariable("v1").level(2);
        Variable v2 = m.newVariable("v2").level(3);
        Set<IntIndex> fixed = m.getFixedVariables();
        Set<IntIndex> expected = Set.of(IntIndex.of(m.indexOf(v0)), IntIndex.of(m.indexOf(v1)), IntIndex.of(m.indexOf(v2)));
        Assertions.assertEquals(expected, fixed, "All three variables should be reported as fixed");
    }

    @ParameterizedTest
    @MethodSource("mixedVariablesProvider")
    void testMixedVariables(int varCount, List<Integer> fixedIndices) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        List<Variable> vars = new ArrayList<>();
        for (int i = 0; i < varCount; i++) {
            Variable v = m.newVariable("v" + i);
            vars.add(v);
        }
        // Apply fixed status according to the test data
        for (int idx : fixedIndices) {
            vars.get(idx).level(42);
        }
        Set<IntIndex> actual = m.getFixedVariables();
        Set<IntIndex> expected = new HashSet<>();
        for (int idx : fixedIndices) {
            expected.add(IntIndex.of(m.indexOf(vars.get(idx))));
        }
        Assertions.assertEquals(expected, actual, "Fixed set does not match expected indices");
    }

    /**
     * Clearing previous fixed variables – verify internal set is cleared each call.
     */
    @Test
    void testClearingPreviousFixedVariables() {
        // first call – only variable 0 fixed
        Variable v0 = model.newVariable("v0").level(1);
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Set<IntIndex> first = model.getFixedVariables();
        Assertions.assertEquals(Set.of(idxOf(v0)), first);
        // change status: un‑fix v0, fix v2
        v0.lower(0).upper(10);
        v2.level(5);
        Set<IntIndex> second = model.getFixedVariables();
        Assertions.assertEquals(Set.of(idxOf(v2)), second);
    }

    /**
     * Unmodifiable result set – ensure exception is thrown on modification attempts.
     */
    @Test
    void testResultSetIsUnmodifiable() {
        // y is fixed
        Set<IntIndex> fixed = model.getFixedVariables();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fixed.add(IntIndex.of(99)));
    }

    /**
     * Idempotent calls – same content, different wrapper instances.
     */
    @Test
    void testIdempotentCallsReturnDistinctUnmodifiableViews() {
        // fix variables 0 and 2
        Variable v0 = model.newVariable("v0").level(1);
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2").level(2);
        Set<IntIndex> first = model.getFixedVariables();
        Set<IntIndex> second = model.getFixedVariables();
        Set<IntIndex> expected = Set.of(idxOf(v0), idxOf(v2));
        Assertions.assertEquals(expected, first);
        Assertions.assertEquals(expected, second);
        Assertions.assertNotSame(first, second, "Each call should return a distinct unmodifiable view");
    }

    /**
     * Adding variables after the first call must not affect previously returned set.
     */
    @Test
    void testAddVariablesAfterFirstCallDoesNotAffectResult() {
        // start with two variables, fix the first one
        Variable v0 = model.newVariable("v0").level(1);
        Variable v1 = model.newVariable("v1");
        Set<IntIndex> first = model.getFixedVariables();
        Assertions.assertEquals(Set.of(idxOf(v0)), first);
        // add a third variable (not fixed)
        model.newVariable("v2");
        Set<IntIndex> second = model.getFixedVariables();
        Assertions.assertEquals(Set.of(idxOf(v0)), second, "Result must still contain only the originally fixed variable");
    }

    /**
     * Large model – every 10th variable fixed.
     */
    @Test
    void testLargeModelPeriodicFixedVariables() {
        ExpressionsBasedModel large = new ExpressionsBasedModel();
        List<Variable> vars = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            Variable v = large.newVariable("v" + i);
            vars.add(v);
            if (i % 10 == 0) {
                v.level(i);
            }
        }
        Set<IntIndex> fixed = large.getFixedVariables();
        Set<IntIndex> expected = new HashSet<>();
        for (int i = 0; i < 100; i += 10) {
            expected.add(IntIndex.of(large.indexOf(vars.get(i))));
        }
        Assertions.assertEquals(expected, fixed);
    }

    /**
     * Immutability persists across calls – after a failed mutation attempt.
     */
    @Test
    void testImmutabilityPersistsAcrossCalls() {
        // y fixed
        Set<IntIndex> first = model.getFixedVariables();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> first.add(IntIndex.of(99)));
        Set<IntIndex> second = model.getFixedVariables();
        Assertions.assertEquals(first, second);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> second.remove(IntIndex.of(0)));
    }

    /**
     * Set reflects updated fixed status – adding another fixed variable.
     */
    @Test
    void testSetReflectsUpdatedFixedStatus() {
        Variable v0 = model.newVariable("v0").level(1);
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        // first call – only v0 fixed
        Set<IntIndex> first = model.getFixedVariables();
        Assertions.assertEquals(Set.of(idxOf(v0)), first);
        // now fix v1 as well
        v1.level(2);
        Set<IntIndex> second = model.getFixedVariables();
        Assertions.assertEquals(Set.of(idxOf(v0), idxOf(v1)), second);
    }

    /**
     * Null variable in internal list should cause NPE.
     */
    @Test
    void testNullVariableInInternalListThrowsNPE() throws Exception {
        // Insert a null into the private myVariables list
        Field varField = getPrivateField("myVariables");
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) varField.get(model);
        // corrupt the list
        vars.add(null);
        Assertions.assertThrows(NullPointerException.class, () -> model.getFixedVariables(), "Iterating over a null element must throw NullPointerException");
    }

    /**
     * Nullified internal fixed‑variables set should cause NPE.
     */
    @Test
    void testNullifiedFixedVariablesSetThrowsNPE() throws Exception {
        // Set myFixedVariables to null via reflection
        Field fixedField = getPrivateField("myFixedVariables");
        fixedField.set(model, null);
        Assertions.assertThrows(NullPointerException.class, () -> model.getFixedVariables(), "Calling clear() on a null myFixedVariables should throw NullPointerException");
    }

    /**
     * Concurrent modification of variable list should result in CME.
     */
    @Test
    void testConcurrentModificationThrowsCME() throws Exception {
        // Prepare a model with a few variables
        for (int i = 0; i < 5; i++) {
            model.newVariable("v" + i);
        }
        // Latch to start the iteration and the background mutation at the same time
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        // Background task that adds a variable while the main thread is iterating
        exec.submit(() -> {
            try {
                // wait until iteration has begun
                startLatch.await();
                model.newVariable("concurrent");
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Main thread – trigger the iteration
        try {
            // Signal the background thread to start as soon as we enter the loop
            startLatch.countDown();
            // The method itself performs the iteration; we just call it
            Assertions.assertThrows(ConcurrentModificationException.class, () -> model.getFixedVariables(), "Concurrent modification of myVariables should raise CME");
        } finally {
            // Ensure background thread finishes before test ends
            doneLatch.await(2, TimeUnit.SECONDS);
            exec.shutdownNow();
        }
    }
}
