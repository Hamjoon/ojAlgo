package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removeIntegration_Test {

    /**
     * Clears the static integration list before every test.
     */
    @BeforeEach
    void setupBeforeEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Clears the static integration list after every test (defensive).
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Directly adds an integration to the internal static list using reflection.
     * This is used for tests that need the element present but do not want to rely on
     * the public {@code addIntegration} method (e.g., when the element's {@code equals}
     * throws an exception).
     */
//     private static void addDirectlyToIntegrations(Integration<?> integration) {
//         try {
//             Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
//             field.setAccessible(true);
//             @SuppressWarnings("unchecked")
//             List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
//             list.add(integration);
//         } catch (ReflectiveOperationException e) {
//             throw new AssertionError("Failed to inject integration via reflection", e);
//         }
//     }

    /**
     * Simple mock integration used for exception‑scenario tests where the actual value is irrelevant.
     */
//     private static Integration<?> mockIntegration() {
//         // Use a raw type to avoid dealing with the generic Solver bound.
//         @SuppressWarnings({ "rawtypes", "unchecked" })
//         ExpressionsBasedModel.Integration integration = new ExpressionsBasedModel.Integration() {
// 
//             @Override
//             public boolean isCapable(ExpressionsBasedModel model) {
//                 // For testing purposes we simply claim capability.
//                 return true;
//             }
// 
//             @Override
//             public void build(ExpressionsBasedModel model) {
//                 // No‑op for the mock.
//             }
//         };
//         return integration;
//     }

    /**
     * Verify that removing an integration that was previously added returns {@code true}.
     */
//     @Test
//     void testRemoveExistingIntegration(@Mock Integration<?> integration) {
//         ExpressionsBasedModel.Integration<?> integration = mockIntegration();
//         // Add the integration – the method returns true if the list changed.
//         boolean added = ExpressionsBasedModel.addIntegration(integration);
//         assertTrue(added, "Integration should be added successfully");
//         // Now remove it – should return true and the list should be empty.
//         boolean removed = ExpressionsBasedModel.removeIntegration(integration);
//         assertTrue(removed, "removeIntegration should return true for an existing integration");
//         // Adding the same integration again should succeed, proving it was really removed.
//         boolean addedAgain = ExpressionsBasedModel.addIntegration(integration);
//         assertTrue(addedAgain, "Integration should be addable again after removal");
//     }

    /**
     * Verify that attempting to remove a never‑added integration returns {@code false}.
     */
//     @Test
//     void testRemoveNonExistingIntegration(@Mock Integration<?> integration) {
//         ExpressionsBasedModel.Integration<?> integration = mockIntegration();
//         // The integration has never been added.
//         boolean removed = ExpressionsBasedModel.removeIntegration(integration);
//         assertFalse(removed, "removeIntegration should return false when the integration is not present");
//     }

    /**
     * Verify that removing {@code null} is safely handled (returns {@code false}).
     */
    @Test
    void testRemoveNullIntegration() {
        boolean removed = ExpressionsBasedModel.removeIntegration(null);
        assertFalse(removed, "removeIntegration should return false when null is passed");
    }

    /**
     * Remove null after a null entry has been manually added to the list.
     */
    @Test
    void testRemoveNullAfterAddingNullEntry() throws Exception {
        // Directly inject a null element into the internal list.
        Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
        list.add(null);
        // First removal of null should succeed.
        assertTrue(ExpressionsBasedModel.removeIntegration(null), "First removal of null should return true");
        // Second removal should fail because the null element is gone.
        assertFalse(ExpressionsBasedModel.removeIntegration(null), "Second removal of null should return false");
    }

    /**
     * Remove two distinct integrations one after the other.
     */
//     @Test
//     void testRemoveMultipleDistinctIntegrations(@Mock Integration<?> integrationX, @Mock Integration<?> integrationY) {
//         ExpressionsBasedModel.Integration<?> integrationX = mockIntegration();
//         ExpressionsBasedModel.Integration<?> integrationY = mockIntegration();
//         assertTrue(ExpressionsBasedModel.addIntegration(integrationX), "Add X");
//         assertTrue(ExpressionsBasedModel.addIntegration(integrationY), "Add Y");
//         // Remove X
//         assertTrue(ExpressionsBasedModel.removeIntegration(integrationX), "Removing X should return true");
//         // Verify Y is still present by attempting to remove it – should succeed.
//         assertTrue(ExpressionsBasedModel.removeIntegration(integrationY), "Removing Y after X should also return true");
//     }

    /**
     * Integration whose equals throws a RuntimeException – the exception must propagate.
     */
    @Test
    void testRemoveIntegrationEqualsThrowsRuntimeException() {
        ExpressionsBasedModel.Integration<Object> bad = new ExpressionsBasedModel.Integration<Object>() {

            @Override
            public boolean isCapable(ExpressionsBasedModel model) {
                return true;
            }

            @Override
            public void build(ExpressionsBasedModel model) {
                // no‑op
            }

            @Override
            public boolean equals(Object obj) {
                throw new RuntimeException("boom");
            }
        };
        // Directly add to the internal list (addIntegration would reject null checks, but we want it present)
        addDirectlyToIntegrations(bad);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.removeIntegration(bad), "RuntimeException from equals should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Integration whose equals throws a NullPointerException – the exception must propagate.
     */
    @Test
    void testRemoveIntegrationEqualsThrowsNullPointerException() {
        ExpressionsBasedModel.Integration<Object> bad = new ExpressionsBasedModel.Integration<Object>() {

            @Override
            public boolean isCapable(ExpressionsBasedModel model) {
                return true;
            }

            @Override
            public void build(ExpressionsBasedModel model) {
                // no‑op
            }

            @Override
            public boolean equals(Object obj) {
                throw new NullPointerException("null ptr");
            }
        };
        addDirectlyToIntegrations(bad);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.removeIntegration(bad), "NullPointerException from equals should propagate");
        assertEquals("null ptr", ex.getMessage());
    }

    /**
     * When the static INTEGRATIONS list is {@code null}, a NullPointerException is expected.
     */
    @Test
    void testRemoveIntegrationWhenListIsNull() throws Exception {
        // Set the static field to null via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        field.setAccessible(true);
        Object original = field.get(null);
        try {
            field.set(null, null);
            assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.removeIntegration(mockIntegration()), "Calling removeIntegration with a null internal list should throw NPE");
        } finally {
            // Restore original list to keep other tests safe.
            field.set(null, original);
        }
    }

    /**
     * When the static INTEGRATIONS list is unmodifiable, removal should throw UnsupportedOperationException.
     */
    @Test
    void testRemoveIntegrationWhenListIsUnmodifiable() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ExpressionsBasedModel.Integration<?>> original = (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
        try {
            field.set(null, Collections.unmodifiableList(new ArrayList<>()));
            assertThrows(UnsupportedOperationException.class, () -> ExpressionsBasedModel.removeIntegration(mockIntegration()), "Removal from an unmodifiable list should throw UnsupportedOperationException");
        } finally {
            // Restore the original mutable list.
            field.set(null, original);
        }
    }
}