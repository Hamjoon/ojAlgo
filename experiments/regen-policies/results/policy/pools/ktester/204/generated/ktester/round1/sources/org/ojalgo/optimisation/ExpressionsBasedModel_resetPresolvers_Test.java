package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_resetPresolvers_Test {

    /**
     * Ensure a clean state before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Remove any presolvers that might have been added by previous tests.
        ExpressionsBasedModel.clearPresolvers();
        // Verify the clear really happened – helps catching accidental side‑effects.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS should be empty before each test");
    }

    /**
     * No per‑test teardown is required, but we clear again for safety.
     */
    @AfterEach
    void teardownAfterEach() {
        // Restore the original (empty) TreeSet in case a test replaced it via reflection.
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
            field.setAccessible(true);
            Object current = field.get(null);
            if (current == null || !(current instanceof Set)) {
                // reset to a fresh TreeSet if the field was tampered with
                field.set(null, new TreeSet<>());
            }
        } catch (Exception ignored) {
            // If reflection fails we simply continue – the next test's @BeforeEach will clean up.
        }
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Verify that {@code resetPresolvers()} populates the expected default presolvers.
     */
    @Test
    void testResetPresolversPopulatesDefaultSet() {
        // Pre‑condition: the set is empty (checked in @BeforeEach)
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must start empty for this test");
        // Invoke the method under test
        ExpressionsBasedModel.resetPresolvers();
        // The set should now contain exactly the three default presolvers
        Set<Presolver> presolvers = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, presolvers.size(), "PRESOLVERS should contain exactly three entries after resetPresolvers()");
        assertTrue(presolvers.contains(Presolvers.ZERO_ONE_TWO), "PRESOLVERS must contain Presolvers.ZERO_ONE_TWO");
        assertTrue(presolvers.contains(Presolvers.INTEGER), "PRESOLVERS must contain Presolvers.INTEGER");
        assertTrue(presolvers.contains(Presolvers.REDUNDANT_CONSTRAINT), "PRESOLVERS must contain Presolvers.REDUNDANT_CONSTRAINT");
    }

    /**
     * Calling {@code resetPresolvers()} repeatedly must not create duplicates.
     */
    @Test
    void testResetPresolversIdempotent() {
        // First call
        ExpressionsBasedModel.resetPresolvers();
        Set<Presolver> firstRun = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, firstRun.size(), "First run should yield three presolvers");
        // Second call – TreeSet guarantees uniqueness, size must stay the same
        ExpressionsBasedModel.resetPresolvers();
        Set<Presolver> secondRun = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, secondRun.size(), "Second run should still contain three presolvers");
        assertEquals(firstRun, secondRun, "The content of PRESOLVERS must be unchanged after a second reset");
    }

    /**
     * After a manual clear, resetPresolvers() should repopulate the defaults.
     */
    @Test
    void testResetPresolversAfterClearPresolvers() {
        // Clear explicitly (already done in @BeforeEach, but we repeat for clarity)
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must be empty before invoking resetPresolvers()");
        ExpressionsBasedModel.resetPresolvers();
        Set<Presolver> presolvers = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, presolvers.size(), "After clear + reset, PRESOLVERS should contain exactly three defaults");
        assertTrue(presolvers.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)), "All default presolvers must be present");
    }

    /**
     * resetPresolvers() must preserve custom presolvers already present.
     */
    @Test
    void testResetPresolversPreservesCustomPresolver() {
        // Create a simple custom presolver – a minimal concrete implementation
        Presolver custom = new Presolver() {
        };
        // Add the custom presolver before resetting defaults
        ExpressionsBasedModel.addPresolver(custom);
        // Verify the custom entry is present
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(custom), "Custom presolver should be present before reset");
        // Now invoke resetPresolvers()
        ExpressionsBasedModel.resetPresolvers();
        Set<Presolver> presolvers = ExpressionsBasedModel.PRESOLVERS;
        // Expect four entries: three defaults + the custom one
        assertEquals(4, presolvers.size(), "Custom presolver must be retained together with the three defaults");
        assertTrue(presolvers.contains(custom), "Custom presolver must still be present");
        assertTrue(presolvers.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)), "All default presolvers must be present");
    }

    /**
     * If only a subset of defaults is present, resetPresolvers() adds the missing ones.
     */
    @Test
    void testResetPresolversAddsMissingDefaultsWhenSubsetPresent() {
        // Pre‑populate with only ZERO_ONE_TWO
        ExpressionsBasedModel.addPresolver(Presolvers.ZERO_ONE_TWO);
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(Presolvers.ZERO_ONE_TWO));
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size());
        // Reset should add the other two defaults
        ExpressionsBasedModel.resetPresolvers();
        Set<Presolver> presolvers = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, presolvers.size(), "After reset, the set must contain exactly the three defaults");
        assertTrue(presolvers.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)), "All default presolvers must be present");
    }

    /**
     * Concurrent invocations of resetPresolvers() must not corrupt the set.
     */
    @Test
    void testResetPresolversConcurrentInvocations() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            Callable<Void> task = () -> {
                ExpressionsBasedModel.resetPresolvers();
                return null;
            };
            // Submit many tasks concurrently
            var futures = executor.invokeAll(Collections.nCopies(threadCount, task));
            // Ensure all completed without exception
            for (Future<Void> f : futures) {
                // will re‑throw any exception from the task
                f.get();
            }
        } finally {
            executor.shutdownNow();
        }
        Set<Presolver> presolvers = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, presolvers.size(), "Concurrent calls must leave exactly three defaults in the set");
        assertTrue(presolvers.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)), "All default presolvers must be present after concurrent execution");
    }

    /**
     * Idempotence check – size must stay unchanged when defaults already present.
     */
    @Test
    void testResetPresolversIdempotentSizeCheck() {
        // First call populates defaults
        ExpressionsBasedModel.resetPresolvers();
        int sizeAfterFirst = ExpressionsBasedModel.PRESOLVERS.size();
        assertEquals(3, sizeAfterFirst, "First call should result in size 3");
        // Second call must not change size
        ExpressionsBasedModel.resetPresolvers();
        int sizeAfterSecond = ExpressionsBasedModel.PRESOLVERS.size();
        assertEquals(sizeAfterFirst, sizeAfterSecond, "Second call must not modify the size of PRESOLVERS");
    }

    /**
     * If PRESOLVERS is {@code null}, resetPresolvers() should throw NPE.
     */
    @Test
    void testResetPresolversNullPresolversField() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        field.setAccessible(true);
        // Save original value to restore later
        @SuppressWarnings("unchecked")
        Set<Presolver> original = (Set<Presolver>) field.get(null);
        try {
            // Inject null
            field.set(null, null);
            // Expect NPE when trying to add to a null set
            assertThrows(NullPointerException.class, ExpressionsBasedModel::resetPresolvers, "resetPresolvers() should throw NullPointerException when PRESOLVERS is null");
        } finally {
            // Restore original set for other tests
            field.set(null, original);
        }
    }

    /**
     * If PRESOLVERS is an unmodifiable view, resetPresolvers() should throw UnsupportedOperationException.
     */
    @Test
    void testResetPresolversUnmodifiablePresolversField() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Presolver> original = (Set<Presolver>) field.get(null);
        try {
            // Replace with an unmodifiable empty set
            Set<Presolver> unmodifiable = Collections.unmodifiableSet(new TreeSet<>());
            field.set(null, unmodifiable);
            assertThrows(UnsupportedOperationException.class, ExpressionsBasedModel::resetPresolvers, "resetPresolvers() should throw UnsupportedOperationException when PRESOLVERS is unmodifiable");
        } finally {
            // Restore the original mutable TreeSet
            field.set(null, original);
        }
    }
}