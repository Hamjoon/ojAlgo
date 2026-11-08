package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_resetPresolvers_Test {

    /**
     * Preserve the original static PRESOLVERS reference so we can restore it after each test.
     */
    private static Set<ExpressionsBasedModel.Presolver> originalPresolvers;

    /**
     * Store the original default presolvers (they are static final fields).
     */
    private static ExpressionsBasedModel.Presolver originalZeroOneTwo;

    private static ExpressionsBasedModel.Presolver originalInteger;

    private static ExpressionsBasedModel.Presolver originalRedundant;

    /**
     * Save the original static state before any test runs.
     */
    @BeforeAll
    static void captureOriginalState() throws Exception {
        originalPresolvers = getPresolversSet();
        originalZeroOneTwo = Presolvers.ZERO_ONE_TWO;
        originalInteger = Presolvers.INTEGER;
        originalRedundant = Presolvers.REDUNDANT_CONSTRAINT;
    }

    /**
     * Clear the presolvers before each test so that the test is isolated.
     */
    @BeforeEach
    void setupBeforeEach() {
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Restore the original static state after each test to guarantee isolation.
     */
    @AfterEach
    void restoreOriginalState() throws Exception {
        // Restore the original PRESOLVERS set (clear it first to avoid leftover elements)
        setPresolversSet(originalPresolvers);
        // Ensure the default constants are back to their original values
        setDefaultPresolver("ZERO_ONE_TWO", originalZeroOneTwo);
        setDefaultPresolver("INTEGER", originalInteger);
        setDefaultPresolver("REDUNDANT_CONSTRAINT", originalRedundant);
        // Finally clear any modifications that may have been added during the test
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Helper to replace the static {@code PRESOLVERS} field with a supplied set.
     */
    private static void setPresolversSet(Set<ExpressionsBasedModel.Presolver> newSet) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        f.setAccessible(true);
        f.set(null, newSet);
    }

    /**
     * Helper to replace a default presolver constant via reflection.
     */
    private static void setDefaultPresolver(String fieldName, ExpressionsBasedModel.Presolver value) throws Exception {
        Field f = Presolvers.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * Verify that {@code resetPresolvers()} registers the default presolvers.
     */
    @Test
    void testResetPresolversAddsDefaultPresolvers() {
        // Preconditions: the set must be empty.
        Set<Presolver> before = ExpressionsBasedModel.PRESOLVERS;
        assertTrue(before.isEmpty(), "Presolvers set should be empty before reset");
        // Exercise the method under test.
        ExpressionsBasedModel.resetPresolvers();
        // Verify that the expected presolvers are now present.
        Set<Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, after.size(), "Presolvers set should contain exactly three entries after reset");
        // The three default presolvers are defined in the Presolvers utility class.
        assertTrue(after.contains(Presolvers.ZERO_ONE_TWO), "Missing Presolvers.ZERO_ONE_TWO");
        assertTrue(after.contains(Presolvers.INTEGER), "Missing Presolvers.INTEGER");
        assertTrue(after.contains(Presolvers.REDUNDANT_CONSTRAINT), "Missing Presolvers.REDUNDANT_CONSTRAINT");
    }

    /**
     * Helper to access the private static final field {@code PRESOLVERS}.
     */
    @SuppressWarnings("unchecked")
    private static Set<ExpressionsBasedModel.Presolver> getPresolversSet() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        f.setAccessible(true);
        return (Set<ExpressionsBasedModel.Presolver>) f.get(null);
    }

    /**
     * Idempotence: a second call does not increase the size.
     */
    @Test
    @DisplayName("resetPresolvers is idempotent")
    void testResetPresolversIdempotent() {
        // first call
        ExpressionsBasedModel.resetPresolvers();
        // second call
        ExpressionsBasedModel.resetPresolvers();
        Set<ExpressionsBasedModel.Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, after.size(), "Presolvers set size must remain 3 after second call");
        assertTrue(after.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)));
    }

    /**
     * Re‑population after a clear.
     */
    @Test
    @DisplayName("resetPresolvers repopulates after clearPresolvers")
    void testResetPresolversAfterClear() {
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Presolvers set should be empty after clear");
        ExpressionsBasedModel.resetPresolvers();
        Set<ExpressionsBasedModel.Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, after.size(), "Presolvers set should contain three defaults after reset");
    }

    /**
     * Adding a custom presolver before resetPresolvers retains it.
     */
    @Test
    @DisplayName("resetPresolvers does not remove existing custom presolvers")
    void testResetPresolversKeepsCustomPresolver(@Mock ExpressionsBasedModel.Presolver customPresolver) throws Exception {
        // Create a simple custom presolver.
        ExpressionsBasedModel.Presolver customPresolver = new ExpressionsBasedModel.Presolver() {

            @Override
            public int compareTo(ExpressionsBasedModel.Presolver o) {
                // Arbitrary ordering – ensure it is distinct from defaults.
                return System.identityHashCode(this) - System.identityHashCode(o);
            }
        };
        // Add the custom presolver manually.
        ExpressionsBasedModel.addPresolver(customPresolver);
        assertTrue(ExpressionsBasedModel.PRESOLVERS.contains(customPresolver));
        // Now invoke resetPresolvers.
        ExpressionsBasedModel.resetPresolvers();
        Set<ExpressionsBasedModel.Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(4, after.size(), "Set should contain the three defaults plus the custom presolver");
        assertTrue(after.contains(customPresolver), "Custom presolver must still be present");
    }

    /**
     * Partially populated set (only INTEGER present) becomes fully populated.
     */
    @Test
    @DisplayName("resetPresolvers fills missing defaults when set is partially populated")
    void testResetPresolversPartialPopulation() {
        // Start with only INTEGER
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.addPresolver(Presolvers.INTEGER);
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size());
        // Reset
        ExpressionsBasedModel.resetPresolvers();
        Set<ExpressionsBasedModel.Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, after.size(), "All three defaults should be present after reset");
        assertTrue(after.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)));
    }

    /**
     * Custom presolver equal (by compareTo) to a default does not create a duplicate.
     */
    @Test
    @DisplayName("resetPresolvers does not duplicate when custom presolver equals a default")
    void testResetPresolversCustomEqualToDefault() throws Exception {
        // Create a custom presolver that pretends to be equal to any default.
        ExpressionsBasedModel.Presolver equalPresolver = new ExpressionsBasedModel.Presolver() {

            @Override
            public int compareTo(ExpressionsBasedModel.Presolver o) {
                // Force equality with any default
                return 0;
            }
        };
        // Insert the custom equal presolver first.
        ExpressionsBasedModel.addPresolver(equalPresolver);
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size());
        // Reset – the TreeSet should treat the default as duplicate.
        ExpressionsBasedModel.resetPresolvers();
        Set<ExpressionsBasedModel.Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        // Still only three elements (the three defaults) because the equalPresolver is considered the same as ZERO_ONE_TWO.
        assertEquals(3, after.size(), "Size should be 3 – duplicates are not added");
        assertTrue(after.contains(Presolvers.INTEGER));
        assertTrue(after.contains(Presolvers.REDUNDANT_CONSTRAINT));
    }

    /**
     * Concurrency safety: multiple threads invoking resetPresolvers simultaneously.
     */
    @Test
    @DisplayName("resetPresolvers is thread‑safe under concurrent execution")
    void testResetPresolversConcurrency() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            Callable<Void> task = () -> {
                ExpressionsBasedModel.resetPresolvers();
                return null;
            };
            @SuppressWarnings("unchecked")
            Future<Void>[] futures = new Future[threadCount];
            for (int i = 0; i < threadCount; i++) {
                futures[i] = executor.submit(task);
            }
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            executor.shutdownNow();
        }
        Set<ExpressionsBasedModel.Presolver> after = ExpressionsBasedModel.PRESOLVERS;
        assertEquals(3, after.size(), "After concurrent calls, PRESOLVERS must contain exactly three defaults");
        assertTrue(after.containsAll(Set.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)));
    }

    /**
     * Verify natural ordering of the TreeSet after resetPresolvers.
     */
    @Test
    @DisplayName("resetPresolvers respects TreeSet natural ordering")
    void testResetPresolversOrdering() {
        ExpressionsBasedModel.resetPresolvers();
        Iterator<ExpressionsBasedModel.Presolver> it = ExpressionsBasedModel.PRESOLVERS.iterator();
        ExpressionsBasedModel.Presolver first = it.next();
        ExpressionsBasedModel.Presolver second = it.next();
        ExpressionsBasedModel.Presolver third = it.next();
        assertEquals(Presolvers.ZERO_ONE_TWO, first, "First element should be ZERO_ONE_TWO");
        assertEquals(Presolvers.INTEGER, second, "Second element should be INTEGER");
        assertEquals(Presolvers.REDUNDANT_CONSTRAINT, third, "Third element should be REDUNDANT_CONSTRAINT");
    }

    /**
     * Null PRESOLVERS field should cause NullPointerException.
     */
    @Test
    @DisplayName("resetPresolvers throws NPE when PRESOLVERS is null")
    void testResetPresolversNullPresolversSet() throws Exception {
        // Replace the static field with null
        setPresolversSet(null);
        assertThrows(NullPointerException.class, ExpressionsBasedModel::resetPresolvers, "Calling resetPresolvers when PRESOLVERS is null should throw NPE");
    }

    /**
     * Unmodifiable PRESOLVERS set should cause UnsupportedOperationException.
     */
    @Test
    @DisplayName("resetPresolvers throws UnsupportedOperationException for unmodifiable set")
    void testResetPresolversUnmodifiableSet() throws Exception {
        Set<ExpressionsBasedModel.Presolver> unmodifiable = Collections.unmodifiableSet(new TreeSet<>());
        setPresolversSet(unmodifiable);
        assertThrows(UnsupportedOperationException.class, ExpressionsBasedModel::resetPresolvers, "Adding to an unmodifiable PRESOLVERS set must throw UnsupportedOperationException");
    }

    /**
     * Faulty comparator that throws RuntimeException during add.
     */
    @Test
    @DisplayName("resetPresolvers propagates RuntimeException from faulty comparator")
    void testResetPresolversFaultyComparator() throws Exception {
        Comparator<ExpressionsBasedModel.Presolver> throwingComparator = (a, b) -> {
            throw new RuntimeException("compare failure");
        };
        Set<ExpressionsBasedModel.Presolver> faultySet = new TreeSet<>(throwingComparator);
        setPresolversSet(faultySet);
        RuntimeException ex = assertThrows(RuntimeException.class, ExpressionsBasedModel::resetPresolvers, "Comparator exception should propagate");
        assertEquals("compare failure", ex.getMessage());
    }

    /**
     * Null default presolver constant should cause NullPointerException inside addPresolver.
     */
    @Test
    @DisplayName("resetPresolvers throws NPE when a default presolver constant is null")
    void testResetPresolversNullDefaultPresolver() throws Exception {
        // Set ONE of the defaults to null
        setDefaultPresolver("ZERO_ONE_TWO", null);
        // The other defaults remain valid, but the call to addPresolver(null) should NPE.
        assertThrows(NullPointerException.class, ExpressionsBasedModel::resetPresolvers, "Adding a null default presolver must result in NullPointerException");
    }
}
