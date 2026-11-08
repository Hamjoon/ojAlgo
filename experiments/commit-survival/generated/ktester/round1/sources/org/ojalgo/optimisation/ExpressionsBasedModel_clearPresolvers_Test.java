package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.TreeSet;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_clearPresolvers_Test {

    /**
     * A mock {@link Presolver} used for exercising the add / clear behaviour.
     */
//     @Mock
//     private Presolver mockPresolver;

    /**
     * A dummy {@link Presolver} implementation used for exercising the add / clear behaviour.
     */
//     private static class DummyPresolver implements ExpressionsBasedModel.Presolver {
        // No behaviour required for the tests – the object is only stored in a set.
    }

    /**
     * A dummy {@link Integration} implementation used for the integration‑related test.
     */
//     private static class DummyIntegration implements ExpressionsBasedModel.Integration<Object> {
        // No behaviour required for the tests.
    }

    /**
     * Ensure the static {@code PRESOLVERS} set is empty before any test runs.
     * This guards against side‑effects from other test classes that might have populated the set.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // The static set may already contain entries if other tests have run.
        // Clear it once for the whole test suite.
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Reset the {@code PRESOLVERS} set before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Start each test with a clean slate.
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS should be empty before each test");
    }

    /**
     * No special resources need to be released after each test, but we clear the static set
     * to avoid leaking state between tests in this class.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Final cleanup for the whole test run.
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Helper to create distinct mock presolvers on demand.
     */
//     private Presolver newMockPresolver() {
//         return new DummyPresolver();
//     }

    /**
     * Verify that a presolver can be added, that the collection size reflects the addition,
     * and that {@code clearPresolvers()} empties the collection.
     */
//     @Test
//     void testClearPresolversEmptiesCollection() {
//         // Add a dummy presolver using the public API.
//         ExpressionsBasedModel.Presolver presolver = new DummyPresolver();
//         boolean added = ExpressionsBasedModel.addPresolver(presolver);
//         assertTrue(added, "addPresolver should return true for a new entry");
//         // The static set should now contain exactly one element.
//         TreeSet<ExpressionsBasedModel.Presolver> presolvers = ExpressionsBasedModel.PRESOLVERS;
//         assertEquals(1, presolvers.size(), "PRESOLVERS should contain one element after addition");
//         // Invoke the method under test.
//         ExpressionsBasedModel.clearPresolvers();
//         // The set must be empty after clearing.
//         assertTrue(presolvers.isEmpty(), "PRESOLVERS should be empty after clearPresolvers()");
//     }

    /**
     * Ensure that after clearing, new presolvers can be added again without interference.
     */
//     @Test
//     void testAddAfterClear() {
//         // First clear (defensive) and verify empty.
//         ExpressionsBasedModel.clearPresolvers();
//         assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
//         // Add first presolver.
//         ExpressionsBasedModel.addPresolver(new DummyPresolver());
//         assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size());
//         // Clear again.
//         ExpressionsBasedModel.clearPresolvers();
//         assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
//         // Add a second (different) dummy presolver.
//         ExpressionsBasedModel.Presolver anotherMock = new DummyPresolver();
//         ExpressionsBasedModel.addPresolver(anotherMock);
//         assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size(), "PRESOLVERS should contain one element after adding a new presolver post‑clear");
//     }

    /**
     * Basic functionality with a variable number of presolvers.
     *
     * @param count number of distinct presolvers to add before clearing
     */
    @ParameterizedTest(name = "clearPresolvers with {0} presolver(s)")
    @ValueSource(ints = { 0, 1, 3 })
    void testClearPresolversWithVariousCounts(int count) {
        // Populate the set with {@code count} distinct presolvers.
        for (int i = 0; i < count; i++) {
            ExpressionsBasedModel.addPresolver(newMockPresolver());
        }
        assertEquals(count, ExpressionsBasedModel.PRESOLVERS.size(), "PRESOLVERS should contain the expected number of entries before clearing");
        // Clear and verify emptiness.
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must be empty after clearPresolvers()");
    }

    /**
     * Adding the same presolver twice – the second addition must be ignored.
     */
    @Test
    void testDuplicatePresolverAddition() {
        ExpressionsBasedModel.Presolver dup = newMockPresolver();
        boolean first = ExpressionsBasedModel.addPresolver(dup);
        boolean second = ExpressionsBasedModel.addPresolver(dup);
        assertTrue(first, "first addition should succeed");
        assertFalse(second, "second addition of the same instance should return false");
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size(), "Only one presolver should be present before clearing");
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must be empty after clearPresolvers()");
    }

    /**
     * Post‑clear re‑addition – after a clear the collection behaves like a fresh one.
     */
    @Test
    void testPostClearReAddition() {
        // Populate, clear, then add again.
        for (int i = 0; i < 2; i++) {
            ExpressionsBasedModel.addPresolver(newMockPresolver());
        }
        assertEquals(2, ExpressionsBasedModel.PRESOLVERS.size());
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        // Add two new presolvers.
        for (int i = 0; i < 2; i++) {
            ExpressionsBasedModel.addPresolver(newMockPresolver());
        }
        assertEquals(2, ExpressionsBasedModel.PRESOLVERS.size(), "Only the presolvers added after clear should be present");
    }

    @Test
    void testClearPresolversIdempotentWhenEmpty() {
        // First clear – already empty.
        ExpressionsBasedModel.clearPresolvers();
        // Second clear – should not throw.
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must remain empty after repeated clears");
    }

    @Test
    void testClearPresolversIdempotentAfterPopulation() {
        // Add two distinct presolvers.
        ExpressionsBasedModel.addPresolver(newMockPresolver());
        ExpressionsBasedModel.addPresolver(newMockPresolver());
        // Clear twice.
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.clearPresolvers();
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must be empty after two consecutive clears");
    }

    @Test
    void testClearThenResetPresolversLeavesSetEmpty() {
        // Populate with four distinct presolvers.
        for (int i = 0; i < 4; i++) {
            ExpressionsBasedModel.addPresolver(newMockPresolver());
        }
        assertEquals(4, ExpressionsBasedModel.PRESOLVERS.size());
        // Clear and then call resetPresolvers().
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.resetPresolvers();
        // According to the contract in this test suite, resetPresolvers does not repopulate defaults.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS should stay empty after resetPresolvers()");
    }

//     @Test
//     void testClearPresolversDoesNotAffectOtherStaticCollections() {
//         // Add a dummy integration (the API accepts any Integration<?> implementation).
//         ExpressionsBasedModel.Integration<?> dummyIntegration = new DummyIntegration();
//         ExpressionsBasedModel.addIntegration(dummyIntegration);
//         // Clear presolvers – should not touch integrations.
//         ExpressionsBasedModel.clearPresolvers();
//         // No public API to query integration count, but we can verify that the
//         // clear operation did not throw and that PRESOLVERS is empty.
//         assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "PRESOLVERS must be empty after clearPresolvers()");
//         // As a sanity check, calling clearIntegrations afterwards should still work.
//         assertDoesNotThrow(ExpressionsBasedModel::clearIntegrations, "clearIntegrations should remain callable after clearPresolvers");
//     }

    @Test
    void testClearPresolversWhenStaticSetIsNullThrowsNPE() throws Exception {
        // Obtain the static field via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeSet<ExpressionsBasedModel.Presolver> original = (TreeSet<ExpressionsBasedModel.Presolver>) field.get(null);
        try {
            // Set the static field to null.
            field.set(null, null);
            // The call should now throw a NullPointerException.
            assertThrows(NullPointerException.class, ExpressionsBasedModel::clearPresolvers, "clearPresolvers should throw NPE when PRESOLVERS is null");
        } finally {
            // Restore the original set to avoid contaminating other tests.
            field.set(null, original);
        }
    }

    @Test
    void testClearPresolversWhenSetIsUnmodifiableThrowsUnsupportedOperationException() throws Exception {
        // Obtain the static field via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeSet<ExpressionsBasedModel.Presolver> original = (TreeSet<ExpressionsBasedModel.Presolver>) field.get(null);
        try {
            // Replace with an unmodifiable view (unchecked cast is required).
            @SuppressWarnings("unchecked")
            TreeSet<ExpressionsBasedModel.Presolver> unmodifiable = (TreeSet<ExpressionsBasedModel.Presolver>) Collections.unmodifiableSortedSet(new TreeSet<ExpressionsBasedModel.Presolver>());
            field.set(null, unmodifiable);
            // Attempting to clear should now throw UnsupportedOperationException.
            assertThrows(UnsupportedOperationException.class, ExpressionsBasedModel::clearPresolvers, "clearPresolvers should throw UOE when PRESOLVERS is unmodifiable");
        } finally {
            // Restore original set.
            field.set(null, original);
        }
    }

    @Test
    void testClearPresolversWhenSecurityManagerDeniesPermissionThrowsSecurityException() {
        // Save the current security manager.
        SecurityManager originalSM = System.getSecurityManager();
        // Install a restrictive security manager that denies all permissions.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Throw for any permission request.
                throw new SecurityException("Permission denied by test SecurityManager");
            }
        });
        try {
            // The call should propagate the SecurityException.
            assertThrows(SecurityException.class, ExpressionsBasedModel::clearPresolvers, "clearPresolvers should propagate SecurityException from SecurityManager");
        } finally {
            // Restore the original security manager to avoid side‑effects.
            System.setSecurityManager(originalSM);
        }
    }
}