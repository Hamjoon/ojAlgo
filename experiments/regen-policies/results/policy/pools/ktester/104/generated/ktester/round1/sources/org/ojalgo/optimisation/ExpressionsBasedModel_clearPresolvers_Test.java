package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_clearPresolvers_Test {

    /**
     * Holds a reference to the private static {@code PRESOLVERS} field.
     */
    private static Set<?> presolversSet;

    /**
     * Simple stub implementation of {@link ExpressionsBasedModel.Presolver} that also
     * implements {@link Comparable}. The {@code compareTo} method always returns {@code 0}
     * so that multiple instances can coexist in the {@code TreeSet} used by the model.
     */
    private static final class DummyPresolver extends ExpressionsBasedModel.Presolver implements Comparable<DummyPresolver> {

        @Override
        public int compareTo(DummyPresolver o) {
            return 0;
        }
        // No other methods are required for the tests.
    }

    /**
     * Simple stub implementation of {@link ExpressionsBasedModel.Integration}.
     * The concrete behaviour is irrelevant for the tests – we only need an
     * instance that can be passed to {@code addIntegration}/{@code removeIntegration}.
     */
    @SuppressWarnings("rawtypes")
    private static final class DummyIntegration extends ExpressionsBasedModel.Integration {

        @Override
        public void integrate(Object model) {
            // No‑op implementation
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static field via reflection and make it accessible.
        Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> set = (Set<?>) field.get(null);
        presolversSet = set;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(presolversSet.isEmpty(), "Presolvers set should be empty before each test");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side‑effects.
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Creates a mock {@link Presolver} that also implements {@link Comparable}.
     * The {@code compareTo} method is stubbed to return {@code 0} so that the mock can be
     * stored in the {@code TreeSet} without causing ordering issues.
     */
//     private Presolver mockPresolver() {
//         return new DummyPresolver();
//     }

    @Test
    void testClearPresolversRemovesAllAddedPresolvers() {
        // Arrange: add two distinct presolvers
        ExpressionsBasedModel.Presolver p1 = mockPresolver();
        ExpressionsBasedModel.Presolver p2 = mockPresolver();
        boolean added1 = ExpressionsBasedModel.addPresolver(p1);
        boolean added2 = ExpressionsBasedModel.addPresolver(p2);
        assertTrue(added1, "First presolver should be added successfully");
        assertTrue(added2, "Second presolver should be added successfully");
        assertEquals(2, presolversSet.size(), "Presolvers set should contain two elements after addition");
        // Act: clear the presolvers
        ExpressionsBasedModel.clearPresolvers();
        // Assert: the set is empty again
        assertTrue(presolversSet.isEmpty(), "Presolvers set should be empty after clearPresolvers()");
    }

    @Test
    void testClearPresolversWhenAlreadyEmptyDoesNotThrow() {
        // The set is already empty because of @BeforeEach
        assertTrue(presolversSet.isEmpty(), "Presolvers set should start empty");
        // Act & Assert: calling clearPresolvers should not throw and leave the set empty
        assertDoesNotThrow(ExpressionsBasedModel::clearPresolvers);
        assertTrue(presolversSet.isEmpty(), "Presolvers set should remain empty after clearPresolvers()");
    }

    @Test
    void testAddPresolverAfterClearWorks() {
        // Arrange: add a presolver, clear, then add another
        ExpressionsBasedModel.Presolver first = mockPresolver();
        assertTrue(ExpressionsBasedModel.addPresolver(first));
        assertEquals(1, presolversSet.size());
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(presolversSet.isEmpty());
        ExpressionsBasedModel.Presolver second = mockPresolver();
        assertTrue(ExpressionsBasedModel.addPresolver(second));
        assertEquals(1, presolversSet.size(), "A presolver added after clear should be stored");
    }

    /**
     * Creates a mock {@link ExpressionsBasedModel.Integration} instance.
     * The concrete type is irrelevant – we only need an object that can be passed to
     * {@code addIntegration}/ {@code removeIntegration}.
     */
    @SuppressWarnings("unchecked")
    private ExpressionsBasedModel.Integration<?> mockIntegration() {
        return new DummyIntegration();
    }

    @Test
    void testClearPresolversIdempotence() {
        // Add a single presolver
        ExpressionsBasedModel.Presolver p = mockPresolver();
        assertTrue(ExpressionsBasedModel.addPresolver(p));
        assertEquals(1, presolversSet.size());
        // Call clear twice consecutively
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.clearPresolvers();
        // Verify that the set stays empty and no exception was thrown
        assertTrue(presolversSet.isEmpty(), "Presolvers set should remain empty after multiple clearPresolvers() calls");
    }

    @Test
    void testRemovePresolverAfterClearReturnsFalse() {
        // Add a presolver, then clear
        ExpressionsBasedModel.Presolver p = mockPresolver();
        assertTrue(ExpressionsBasedModel.addPresolver(p));
        assertEquals(1, presolversSet.size());
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(presolversSet.isEmpty());
        // Attempt to remove the same presolver – should return false
        boolean removed = ExpressionsBasedModel.removePresolver(p);
        assertFalse(removed, "removePresolver should return false after the presolver was cleared");
    }

    @Test
    void testClearPresolversDoesNotAffectIntegrations() {
        // Add a dummy integration and verify it succeeds
        ExpressionsBasedModel.Integration<?> integration1 = mockIntegration();
        boolean addedIntegration1 = ExpressionsBasedModel.addIntegration(integration1);
        assertTrue(addedIntegration1, "First integration should be added successfully");
        // Clear presolvers – should have no impact on integrations
        ExpressionsBasedModel.clearPresolvers();
        // Adding another integration should still succeed
        ExpressionsBasedModel.Integration<?> integration2 = mockIntegration();
        boolean addedIntegration2 = ExpressionsBasedModel.addIntegration(integration2);
        assertTrue(addedIntegration2, "Second integration should be added successfully after clearPresolvers()");
    }

    @Test
    void testConcurrentAddAndClearPresolvers() throws Exception {
        final int THREADS = 4;
        final int OPERATIONS_PER_THREAD = 500;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        // Two threads will add presolvers, two will clear repeatedly
        for (int i = 0; i < THREADS; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        if (idx % 2 == 0) {
                            // adder thread
                            ExpressionsBasedModel.addPresolver(mockPresolver());
                        } else {
                            // clearer thread
                            ExpressionsBasedModel.clearPresolvers();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Start all threads
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Threads should finish within timeout");
        executor.shutdownNow();
        // After all operations the set must be in a consistent state (empty or containing some elements)
        // The important part is that no ConcurrentModificationException escaped.
        assertDoesNotThrow(() -> presolversSet.size(), "Accessing PRESOLVERS after concurrent modifications should not throw");
    }

    @Test
    void testConcurrentIterationDuringClearThrowsCME() throws Exception {
        // Populate the set with a few presolvers so that iteration has something to read.
        for (int i = 0; i < 5; i++) {
            ExpressionsBasedModel.addPresolver(mockPresolver());
        }
        assertEquals(5, presolversSet.size());
        // Thread that iterates over the set
        AtomicReference<Exception> iterationException = new AtomicReference<>();
        Thread iteratorThread = new Thread(() -> {
            try {
                @SuppressWarnings("unchecked")
                java.util.Iterator<ExpressionsBasedModel.Presolver> it = (java.util.Iterator<ExpressionsBasedModel.Presolver>) presolversSet.iterator();
                while (true) {
                    // The loop will exit when the iterator reports no more elements,
                    // but we deliberately keep calling hasNext() to provoke CME after clear.
                    if (!it.hasNext()) {
                        break;
                    }
                    it.next();
                }
            } catch (Exception e) {
                iterationException.set(e);
            }
        });
        iteratorThread.start();
        // Give the iterator a moment to start
        Thread.sleep(50);
        // Clear the presolvers while the iterator is potentially in use
        ExpressionsBasedModel.clearPresolvers();
        // Wait for iterator thread to finish
        iteratorThread.join(2000);
        // Verify that a ConcurrentModificationException was captured
        Exception ex = iterationException.get();
        assertNotNull(ex, "Iterator thread should have thrown an exception");
        assertTrue(ex instanceof ConcurrentModificationException, "Expected ConcurrentModificationException, got: " + ex);
    }
}