package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.function.Executable;
import java.util.Iterator;
import java.util.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_clearIntegrations_Test {

//     @Mock
//     private Optimisation.Integration<?> // Mockito will initialise this mock
    mockIntegration;

    /**
     * Instance used in the tests – a fresh dummy integration for each test.
     */
    private ExpressionsBasedModel.Integration<?> testIntegration;

    /**
     * Simple dummy implementation of {@link ExpressionsBasedModel.Integration}.
     */
//     private static class DummyIntegration implements ExpressionsBasedModel.Integration<Optimisation.Solver> {

//         @Override
//         public void integrate(ExpressionsBasedModel model) {
//             // No‑op for testing purposes
//         }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure a clean state before any test runs.
        ExpressionsBasedModel.clearIntegrations();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Guarantee that each test starts with an empty integration list.
        ExpressionsBasedModel.clearIntegrations();
//         testIntegration = new DummyIntegration();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side‑effects on other tests.
        ExpressionsBasedModel.clearIntegrations();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Final clean‑up (good practice, though not strictly required here).
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Returns the current size of the internal INTEGRATIONS list.
     */
    private static int integrationsSize() throws Exception {
        return getIntegrationsList().size();
    }

    @Test
    void testClearIntegrationsEffect() {
        // 1. First addition should succeed.
        boolean firstAddResult = ExpressionsBasedModel.addIntegration(testIntegration);
        assertTrue(firstAddResult, "First addition of the integration should return true");
        // 2. Adding the same integration again should be rejected (duplicate).
        boolean secondAddResult = ExpressionsBasedModel.addIntegration(testIntegration);
        assertFalse(secondAddResult, "Adding the same integration again should return false");
        // 3. Clear the integrations.
        ExpressionsBasedModel.clearIntegrations();
        // 4. After clearing, adding the integration should succeed again.
        boolean thirdAddResult = ExpressionsBasedModel.addIntegration(testIntegration);
        assertTrue(thirdAddResult, "After clearing, adding the integration should return true again");
    }

    /**
     * Helper that uses reflection to obtain the private static INTEGRATIONS list.
     */
    @SuppressWarnings("unchecked")
    private static List<ExpressionsBasedModel.Integration<?>> getIntegrationsList() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        field.setAccessible(true);
        // The field is static, so pass null as the instance.
        return (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
    }

    @Test
    void testClearIntegrationsAfterAdd() throws Exception {
        // add once – should succeed
        assertTrue(ExpressionsBasedModel.addIntegration(testIntegration));
        assertEquals(1, integrationsSize(), "List should contain the added integration");
        // duplicate add – should be rejected
        assertFalse(ExpressionsBasedModel.addIntegration(testIntegration));
        assertEquals(1, integrationsSize(), "Duplicate should not increase size");
        // clear
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationsSize(), "List must be empty after clear");
    }

    @Test
    void testClearIntegrationsWhenAlreadyEmpty() throws Exception {
        // Ensure list is empty
        assertEquals(0, integrationsSize());
        // Call clear – should stay empty and not throw
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationsSize(), "Clearing an already empty list must keep size 0");
    }

    @Test
    void testIdempotentClearAndReuse() throws Exception {
        // first clear – already empty
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationsSize());
        // add integration
        assertTrue(ExpressionsBasedModel.addIntegration(testIntegration));
        assertEquals(1, integrationsSize());
        // clear again
        ExpressionsBasedModel.clearIntegrations();
        assertEquals(0, integrationsSize());
        // add again – should succeed
        assertTrue(ExpressionsBasedModel.addIntegration(testIntegration));
        assertEquals(1, integrationsSize());
    }

//     @Test
//     void testMultipleDistinctIntegrations() throws Exception {
//         ExpressionsBasedModel.Integration<?> i1 = new DummyIntegration();
//         ExpressionsBasedModel.Integration<?> i2 = new DummyIntegration();
//         ExpressionsBasedModel.Integration<?> i3 = new DummyIntegration();
//         assertTrue(ExpressionsBasedModel.addIntegration(i1));
//         assertTrue(ExpressionsBasedModel.addIntegration(i2));
//         assertTrue(ExpressionsBasedModel.addIntegration(i3));
//         assertEquals(3, integrationsSize(), "Three distinct integrations should be stored");
//         ExpressionsBasedModel.clearIntegrations();
//         assertEquals(0, integrationsSize(), "List must be empty after clear");
//     }

//     @Test
//     void testAddAfterClearWithDifferentIntegrations() throws Exception {
//         ExpressionsBasedModel.Integration<?> first = new DummyIntegration();
//         ExpressionsBasedModel.Integration<?> second = new DummyIntegration();
//         ExpressionsBasedModel.Integration<?> third = new DummyIntegration();
//         // add one, clear, then add two more
//         assertTrue(ExpressionsBasedModel.addIntegration(first));
//         assertEquals(1, integrationsSize());
//         ExpressionsBasedModel.clearIntegrations();
//         assertEquals(0, integrationsSize());
//         assertTrue(ExpressionsBasedModel.addIntegration(second));
//         assertTrue(ExpressionsBasedModel.addIntegration(third));
//         assertEquals(2, integrationsSize(), "After clear, two new integrations should result in size 2");
//     }

//     @Test
//     void testRemoveIntegrationAfterClear() throws Exception {
//         ExpressionsBasedModel.Integration<?> i1 = new DummyIntegration();
//         ExpressionsBasedModel.Integration<?> i2 = new DummyIntegration();
//         assertTrue(ExpressionsBasedModel.addIntegration(i1));
//         assertTrue(ExpressionsBasedModel.addIntegration(i2));
//         assertEquals(2, integrationsSize());
//         // remove one
//         assertTrue(ExpressionsBasedModel.removeIntegration(i1));
//         assertEquals(1, integrationsSize());
//         // clear should wipe remaining
//         ExpressionsBasedModel.clearIntegrations();
//         assertEquals(0, integrationsSize());
//     }

//     @Test
//     void testRemoveNonexistentIntegrationDoesNotAffectClear() throws Exception {
//         // list is empty
//         assertEquals(0, integrationsSize());
//         ExpressionsBasedModel.Integration<?> notPresent = new DummyIntegration();
//         assertFalse(ExpressionsBasedModel.removeIntegration(notPresent), "Removing non‑existent integration should return false");
//         // clear on empty list – must stay empty and not throw
//         ExpressionsBasedModel.clearIntegrations();
//         assertEquals(0, integrationsSize());
//     }

//     @Test
//     void testConcurrentClearInvocations() throws Exception {
//         // Populate with a few integrations first
//         for (int i = 0; i < 5; i++) {
//             ExpressionsBasedModel.Integration<?> mock = new DummyIntegration();
//             assertTrue(ExpressionsBasedModel.addIntegration(mock));
//         }
//         assertEquals(5, integrationsSize());
//         int threadCount = 10;
//         ExecutorService exec = Executors.newFixedThreadPool(threadCount);
//         CountDownLatch startLatch = new CountDownLatch(1);
//         List<Future<?>> futures = new ArrayList<>();
//         // Each thread will wait on the latch then call clearIntegrations()
//         for (int i = 0; i < threadCount; i++) {
//             futures.add(exec.submit(() -> {
//                 try {
//                     startLatch.await();
//                     ExpressionsBasedModel.clearIntegrations();
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                 }
//             }));
//         }
//         // Release all threads simultaneously
//         startLatch.countDown();
//         // Wait for completion
//         for (Future<?> f : futures) {
//             // re‑throws any exception
//             f.get();
//         }
//         exec.shutdownNow();
//         // After all clears, the list must be empty and no exception should have been thrown
//         assertEquals(0, integrationsSize(), "Concurrent clears must leave the list empty");
//     }

//     @Test
//     void testConcurrentAddAndClear() throws Exception {
//         ExecutorService exec = Executors.newFixedThreadPool(2);
//         CountDownLatch ready = new CountDownLatch(2);
//         CountDownLatch go = new CountDownLatch(1);
//         AtomicReference<Exception> exceptionRef = new AtomicReference<>();
//         // Thread 1 – adds an integration
//         Runnable adder = () -> {
//             ready.countDown();
//             try {
//                 go.await();
//                 ExpressionsBasedModel.Integration<?> i = new DummyIntegration();
//                 ExpressionsBasedModel.addIntegration(i);
//             } catch (Exception e) {
//                 exceptionRef.set(e);
//             }
//         };
//         // Thread 2 – clears the list
//         Runnable clearer = () -> {
//             ready.countDown();
//             try {
//                 go.await();
//                 ExpressionsBasedModel.clearIntegrations();
//             } catch (Exception e) {
//                 exceptionRef.set(e);
//             }
//         };
//         exec.submit(adder);
//         exec.submit(clearer);
//         // both threads are ready
//         ready.await();
//         // start them together
//         go.countDown();
//         exec.shutdown();
//         exec.awaitTermination(5, TimeUnit.SECONDS);
//         // No unexpected exception should have been thrown
//         assertNull(exceptionRef.get(), "No exception should be thrown during concurrent add/clear");
//         // Final size can be 0 or 1 depending on timing, but must be ≤ 1
//         int finalSize = integrationsSize();
//         assertTrue(finalSize == 0 || finalSize == 1, "Final size after concurrent add/clear should be 0 or 1, was " + finalSize);
//     }

    @Test
    void testClearIntegrationsWhenStaticListIsNull() throws Exception {
        // Use reflection to set the static field to null
        Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        field.setAccessible(true);
        field.set(null, null);
        // Expect NPE when clearIntegrations() tries to invoke clear()
        Executable callClear = ExpressionsBasedModel::clearIntegrations;
        assertThrows(NullPointerException.class, callClear, "Calling clearIntegrations on a null list should throw NPE");
        // Restore a non‑null list for subsequent tests
        field.set(null, new ArrayList<>());
    }

//     @Test
//     void testConcurrentModificationDuringIteration() throws Exception {
//         // Add a single integration to have something to iterate over
//         ExpressionsBasedModel.Integration<?> i = new DummyIntegration();
//         assertTrue(ExpressionsBasedModel.addIntegration(i));
//         assertEquals(1, integrationsSize());
//         // Obtain an iterator via reflection
//         List<ExpressionsBasedModel.Integration<?>> list = getIntegrationsList();
//         Iterator<ExpressionsBasedModel.Integration<?>> iterator = list.iterator();
//         // Start a thread that will call next() after a short pause
//         ExecutorService exec = Executors.newSingleThreadExecutor();
//         Future<?> future = exec.submit(() -> {
//             try {
//                 // Sleep to ensure main thread clears while iterator is alive
//                 Thread.sleep(50);
//                 // This should trigger CME after clear
//                 iterator.next();
//             } catch (ConcurrentModificationException e) {
//                 // Expected – rethrow to be caught by the test
//                 throw e;
//             } catch (InterruptedException e) {
//                 Thread.currentThread().interrupt();
//             }
//         });
//         // Give the iterator thread a moment to start and block on sleep
//         Thread.sleep(10);
//         // Clear the list while the iterator is still alive
//         ExpressionsBasedModel.clearIntegrations();
//         // The iterator thread should now throw ConcurrentModificationException
//         assertThrows(ExecutionException.class, () -> future.get(), "Iterator thread should encounter ConcurrentModificationException");
//         exec.shutdownNow();
//     }

//     @Test
//     void testConcurrentModificationDuringStream() throws Exception {
//         // Populate with three integrations
//         for (int i = 0; i < 3; i++) {
//             ExpressionsBasedModel.Integration<?> mock = new DummyIntegration();
//             assertTrue(ExpressionsBasedModel.addIntegration(mock));
//         }
//         assertEquals(3, integrationsSize());
//         // Stream that sleeps inside forEach to keep the pipeline open
//         ExecutorService exec = Executors.newSingleThreadExecutor();
//         Future<?> future = exec.submit(() -> {
//             try {
//                 getIntegrationsList().stream().forEach(integ -> {
//                     try {
//                         // keep the stream alive
//                         Thread.sleep(100);
//                     } catch (InterruptedException e) {
//                         Thread.currentThread().interrupt();
//                     }
//                 });
//             } catch (ConcurrentModificationException e) {
//                 // Propagate to allow test to capture it
//                 throw e;
//             }
//         });
//         // Give the stream a moment to start
//         Thread.sleep(20);
//         // Concurrently clear the list
//         ExpressionsBasedModel.clearIntegrations();
//         // The streaming thread should receive a CME
//         assertThrows(ExecutionException.class, () -> future.get(), "Streaming thread should encounter ConcurrentModificationException after clear");
//         exec.shutdownNow();
//     }
}