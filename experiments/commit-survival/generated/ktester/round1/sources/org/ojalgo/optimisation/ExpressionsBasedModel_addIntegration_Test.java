package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addIntegration_Test {

    /**
     * A mock {@link Optimisation.Integration} used in the tests.
     */
//     @Mock
//     private Optimisation.Integration<Object> mockIntegration;

    /**
     * A simple concrete implementation of {@link ExpressionsBasedModel.Integration}
     * used in the test cases.
     */
//     private static final class TestIntegration implements ExpressionsBasedModel.Integration<Object> {

        private final String name;

        TestIntegration(String name) {
            this.name = name;
        }

//         @Override
//         public boolean isCapable(ExpressionsBasedModel model) {
//             // For testing we simply claim to be capable.
//             return true;
//         }

        @Override
        public String toString() {
            return "TestIntegration{" + name + '}';
        }
    }

    /**
     * Clears the static integration list before every test.
     */
    @BeforeEach
    void setupBeforeEach() {
        ExpressionsBasedModel.clearIntegrations();
        mockIntegration = new TestIntegration("mock");
    }

    /**
     * Also clear after each test as a safety net.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Returns the current size of the private static {@code INTEGRATIONS} list
     * via reflection.
     */
    private static int getIntegrationsSize() {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) field.get(null);
            return list.size();
        } catch (ReflectiveOperationException e) {
            fail("Unable to access INTEGRATIONS field via reflection", e);
            // unreachable
            return -1;
        }
    }

    static Stream<Arguments> distinctIntegrationsProvider() {
        TestIntegration first = new TestIntegration("first");
        TestIntegration second = new TestIntegration("second");
        TestIntegration third = new TestIntegration("third");
        return Stream.of(Arguments.of(first, 1), Arguments.of(second, 2), Arguments.of(third, 3));
    }

    /**
     * Verifies that {@code addIntegration} returns {@code true} when a new integration is added.
     */
    @Test
    void testAddIntegrationReturnsTrue() {
        boolean added = ExpressionsBasedModel.addIntegration(mockIntegration);
        assertTrue(added, "addIntegration should return true for a new integration");
        assertEquals(1, getIntegrationsSize(), "List size must be 1 after adding the mock");
    }

    /**
     * After adding an integration it should be removable via {@code removeIntegration},
     * and subsequent removal attempts should return {@code false}.
     */
    @Test
    void testAddThenRemoveIntegration() {
        boolean added = ExpressionsBasedModel.addIntegration(mockIntegration);
        assertTrue(added, "Integration should be added successfully");
        assertEquals(1, getIntegrationsSize());
        boolean removed = ExpressionsBasedModel.removeIntegration(mockIntegration);
        assertTrue(removed, "removeIntegration should return true for an existing integration");
        assertEquals(0, getIntegrationsSize());
        boolean removedAgain = ExpressionsBasedModel.removeIntegration(mockIntegration);
        assertFalse(removedAgain, "removeIntegration should return false when the integration is absent");
    }

    /**
     * Adding the same integration instance twice should still return {@code true}
     * for each call (the underlying list permits duplicates).  The test confirms
     * that two removals are required to clear both entries.
     */
    @Test
    void testAddDuplicateIntegration() {
        assertTrue(ExpressionsBasedModel.addIntegration(mockIntegration), "First addition should succeed");
        assertEquals(1, getIntegrationsSize());
        assertTrue(ExpressionsBasedModel.addIntegration(mockIntegration), "Second addition (duplicate) should also succeed");
        assertEquals(2, getIntegrationsSize());
        assertTrue(ExpressionsBasedModel.removeIntegration(mockIntegration), "First removal should succeed");
        assertEquals(1, getIntegrationsSize());
        assertTrue(ExpressionsBasedModel.removeIntegration(mockIntegration), "Second removal should also succeed");
        assertEquals(0, getIntegrationsSize());
        assertFalse(ExpressionsBasedModel.removeIntegration(mockIntegration), "Further removals should fail as the integration is no longer present");
    }

    /**
     * Adding a {@code null} integration should not throw an exception; the method
     * forwards the call to {@code List.add}, which accepts {@code null}.  The test
     * ensures that the method returns {@code true} and that the {@code null}
     * entry can be removed afterwards.
     */
    @Test
    void testAddNullIntegration() {
        boolean added = ExpressionsBasedModel.addIntegration(null);
        assertTrue(added, "addIntegration should return true when adding null");
        assertEquals(1, getIntegrationsSize());
        boolean removed = ExpressionsBasedModel.removeIntegration(null);
        assertTrue(removed, "removeIntegration should succeed for the previously added null");
        assertEquals(0, getIntegrationsSize());
        assertFalse(ExpressionsBasedModel.removeIntegration(null), "removeIntegration should return false after null has been removed");
    }

    /**
     * Normal case – adding a regular (non‑null) integration should succeed and
     * increase the internal list size to 1.
     */
//     @Test
//     void testAddIntegrationNormalCase() {
//         TestIntegration integration = new TestIntegration("first");
//         boolean added = ExpressionsBasedModel.addIntegration(integration);
//         assertTrue(added, "addIntegration should return true for a regular integration");
//         assertEquals(1, getIntegrationsSize(), "List size must be 1 after adding the integration");
//         // Clean‑up
//         ExpressionsBasedModel.removeIntegration(integration);
//     }

    /**
     * Parameterised test for adding multiple distinct integrations.
     * After each addition the list size must reflect the number of added items.
     */
//     @ParameterizedTest(name = "[{index}] add {0}")
//     @MethodSource("distinctIntegrationsProvider")
//     void testAddMultipleDistinctIntegrations(TestIntegration<?> integration, int expectedSizeAfterAdd) {
//         boolean added = ExpressionsBasedModel.addIntegration(integration);
//         assertTrue(added, "addIntegration should return true for distinct integration");
//         assertEquals(expectedSizeAfterAdd, getIntegrationsSize(), "List size should match the number of distinct integrations added");
//     }

    /**
     * Duplicate integration – adding the same instance twice must succeed both
     * times and the list size must increase accordingly.
     */
//     @Test
//     void testAddDuplicateIntegrationSizeCheck() {
//         TestIntegration dup = new TestIntegration("dup");
//         assertTrue(ExpressionsBasedModel.addIntegration(dup));
//         assertEquals(1, getIntegrationsSize());
//         assertTrue(ExpressionsBasedModel.addIntegration(dup));
//         assertEquals(2, getIntegrationsSize());
//         // clean‑up
//         ExpressionsBasedModel.removeIntegration(dup);
//         ExpressionsBasedModel.removeIntegration(dup);
//         assertEquals(0, getIntegrationsSize());
//     }

    /**
     * Verify that {@code clearIntegrations()} really empties the internal list,
     * and that a subsequent addition works as expected.
     */
//     @Test
//     void testClearIntegrationsAndAddAfterClear() {
//         ExpressionsBasedModel.addIntegration(new TestIntegration("one"));
//         ExpressionsBasedModel.addIntegration(new TestIntegration("two"));
//         assertEquals(2, getIntegrationsSize());
//         ExpressionsBasedModel.clearIntegrations();
//         assertEquals(0, getIntegrationsSize(), "clearIntegrations should reset the list size to 0");
//         TestIntegration afterClear = new TestIntegration("afterClear");
//         boolean added = ExpressionsBasedModel.addIntegration(afterClear);
//         assertTrue(added, "addIntegration should succeed after clearIntegrations");
//         assertEquals(1, getIntegrationsSize(), "List size must be 1 after adding post‑clear");
//     }

    /**
     * Adding {@code null} integration – the list should contain a single {@code null}
     * element and removal must behave correctly.
     */
    @Test
    void testAddNullIntegrationSizeCheck() {
        assertTrue(ExpressionsBasedModel.addIntegration(null), "addIntegration should accept null");
        assertEquals(1, getIntegrationsSize(), "List size must be 1 after adding null");
        assertTrue(ExpressionsBasedModel.removeIntegration(null), "removeIntegration should succeed for null");
        assertEquals(0, getIntegrationsSize(), "List size must be 0 after removing null");
    }
}