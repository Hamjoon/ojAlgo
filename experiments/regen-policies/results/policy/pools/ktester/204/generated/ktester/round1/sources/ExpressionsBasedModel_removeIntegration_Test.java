package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removeIntegration_Test {

    /**
     * A mock integration used in the tests.
     */
    @Mock
    private Optimisation.Integration<ExpressionsBasedModel, Object> mockIntegration;

    /**
     * Mock integrations used in the various test scenarios.
     */
    @Mock
    private Optimisation.Integration<ExpressionsBasedModel, Object> dummyIntegration1;

    @Mock
    private Optimisation.Integration<ExpressionsBasedModel, Object> dummyIntegration2;

    @Mock
    private Optimisation.Integration<ExpressionsBasedModel, Object> dummyIntegrationNotAdded;

    @Mock
    private Optimisation.Integration<ExpressionsBasedModel, Object> duplicateIntegration;

    @Mock
    private Optimisation.Integration<ExpressionsBasedModel, Object> anyIntegration;

    /**
     * Clears the static integration registry once for the whole test run.
     */
    @BeforeAll
    static void setupBeforeAll() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Clears the static integration registry before each test to guarantee a clean state.
     */
    @BeforeEach
    void setupBeforeEach() {
        ExpressionsBasedModel.clearIntegrations();
        mockIntegration = createDummyIntegration();
        dummyIntegration1 = createDummyIntegration();
        dummyIntegration2 = createDummyIntegration();
        dummyIntegrationNotAdded = createDummyIntegration();
        duplicateIntegration = createDummyIntegration();
        anyIntegration = createDummyIntegration();
    }

    /**
     * Clears the static integration registry after each test as a safety net.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Clears the static integration registry after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() {
        ExpressionsBasedModel.clearIntegrations();
    }

    @Test
    void testRemoveIntegrationWhenPresent() {
        // Arrange: add the mock integration to the static list
        boolean added = ExpressionsBasedModel.addIntegration(mockIntegration);
        assertTrue(added, "Integration should have been added successfully");
        // Act: remove the integration
        boolean removed = ExpressionsBasedModel.removeIntegration(mockIntegration);
        // Assert: removal reported true and the integration is no longer present
        assertTrue(removed, "removeIntegration should return true when the integration existed");
        // Subsequent removal attempts must return false
        assertFalse(ExpressionsBasedModel.removeIntegration(mockIntegration), "removeIntegration should return false when the integration is no longer present");
    }

    @Test
    void testRemoveIntegrationWhenAbsent() {
        // Act: attempt to remove an integration that was never added
        boolean removed = ExpressionsBasedModel.removeIntegration(mockIntegration);
        // Assert: removal reported false
        assertFalse(removed, "removeIntegration should return false when the integration was never added");
    }

    /* Helper to obtain the private static INTEGRATIONS list via reflection. */
    @SuppressWarnings("unchecked")
    private static List<ExpressionsBasedModel.Integration<?>> getIntegrationsList() {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
            field.setAccessible(true);
            return (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access INTEGRATIONS field", e);
        }
    }

    /**
     * Successful removal when the integration is present.
     */
    @Test
    void testRemoveIntegrationSuccessfulRemoval() {
        // Arrange: add dummyIntegration1
        assertTrue(ExpressionsBasedModel.addIntegration(dummyIntegration1), "addIntegration should return true for a new integration");
        // Act
        boolean removed = ExpressionsBasedModel.removeIntegration(dummyIntegration1);
        // Assert
        assertTrue(removed, "removeIntegration should return true when the integration existed");
        assertFalse(getIntegrationsList().contains(dummyIntegration1), "Integration should no longer be present after removal");
    }

    /**
     * Removal attempt with a {@code null} argument.
     */
    @Test
    void testRemoveIntegrationNullArgument() {
        // Act
        boolean removed = ExpressionsBasedModel.removeIntegration(null);
        // Assert
        assertFalse(removed, "removeIntegration(null) should return false when list is empty");
        assertTrue(getIntegrationsList().isEmpty(), "INTEGRATIONS list should remain empty");
    }

    /**
     * Removal when the integration is not present in the list.
     */
    @Test
    void testRemoveIntegrationNotPresent() {
        // No addition performed
        // Act
        boolean removed = ExpressionsBasedModel.removeIntegration(dummyIntegrationNotAdded);
        // Assert
        assertFalse(removed, "removeIntegration should return false when integration was never added");
        assertTrue(getIntegrationsList().isEmpty(), "INTEGRATIONS list should still be empty after failed removal");
    }

    /**
     * Removal from a list that contains multiple distinct integrations.
     */
    @Test
    void testRemoveIntegrationFromMultiple() {
        // Arrange: add two different integrations
        assertTrue(ExpressionsBasedModel.addIntegration(dummyIntegration1));
        assertTrue(ExpressionsBasedModel.addIntegration(dummyIntegration2));
        // Act: remove the second one
        boolean removed = ExpressionsBasedModel.removeIntegration(dummyIntegration2);
        // Assert
        assertTrue(removed, "removeIntegration should return true for an existing integration");
        List<ExpressionsBasedModel.Integration<?>> list = getIntegrationsList();
        assertEquals(1, list.size(), "Exactly one integration should remain");
        assertTrue(list.contains(dummyIntegration1), "The remaining integration should be dummyIntegration1");
        assertFalse(list.contains(dummyIntegration2), "dummyIntegration2 should have been removed");
    }

    /**
     * Duplicate integrations: the same instance added twice.
     */
    @Test
    void testRemoveIntegrationDuplicateInstances() {
        // Arrange: add the same integration twice
        assertTrue(ExpressionsBasedModel.addIntegration(duplicateIntegration));
        assertTrue(ExpressionsBasedModel.addIntegration(duplicateIntegration));
        // Act: remove once
        boolean removed = ExpressionsBasedModel.removeIntegration(duplicateIntegration);
        // Assert
        assertTrue(removed, "removeIntegration should return true when duplicate instance is present");
        List<ExpressionsBasedModel.Integration<?>> list = getIntegrationsList();
        assertEquals(1, list.size(), "One duplicate should remain after a single removal");
        assertTrue(list.contains(duplicateIntegration), "The remaining instance should still be the duplicateIntegration");
    }

    /**
     * Removal after the list has been cleared (empty list case).
     */
    @Test
    void testRemoveIntegrationAfterClearIntegrations() {
        // Ensure list is empty (clearIntegrations already called in @BeforeEach)
        assertTrue(getIntegrationsList().isEmpty(), "INTEGRATIONS list should be empty before test");
        // Act
        boolean removed = ExpressionsBasedModel.removeIntegration(anyIntegration);
        // Assert
        assertFalse(removed, "removeIntegration should return false when list is empty");
    }

    /**
     * Helper that creates a dummy {@code Integration} instance.
     */
    @SuppressWarnings("unchecked")
    private static Optimisation.Integration<ExpressionsBasedModel, Object> createDummyIntegration() {
        // The cast is unchecked but safe for the purposes of these tests – the object is never used.
        return (Optimisation.Integration<ExpressionsBasedModel, Object>) (Optimisation.Integration) new Object();
    }
}
