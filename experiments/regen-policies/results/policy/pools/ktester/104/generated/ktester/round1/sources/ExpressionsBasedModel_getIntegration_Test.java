package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getIntegration_Test {

    /**
     * Simple subclass that allows us to control {@code isAnyVariableInteger()}.
     */
    private static class TestModel extends ExpressionsBasedModel {

        private final boolean anyInteger;

        TestModel(boolean anyInteger) {
            this.anyInteger = anyInteger;
        }

        @Override
        public boolean isAnyVariableInteger() {
            return anyInteger;
        }
    }

    /**
     * Ensure a clean static state before any test runs.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // The library provides a public helper to clear the integrations list.
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Reset the static integrations list before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Clean up after each test – again clear the integrations list.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Final clean‑up after the whole test suite.
     */
    @AfterAll
    static void teardownAfterAll() {
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Reflectively replace a static field (may be final) with a new value.
     */
    private static void setStaticField(Class<?> clazz, String fieldName, Object newValue) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        // Remove final modifier if present
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(null, newValue);
    }

    /**
     * Restore a static field to its original value after a test.
     */
    private static AutoCloseable restoreStaticField(Class<?> clazz, String fieldName, Object original) {
        return () -> setStaticField(clazz, fieldName, original);
    }

    private static Object getStaticField(Class<?> clazz, String fieldName) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(null);
    }

    /**
     * When the first integration in the static list reports {@code true} for
     * {@code isCapable(this)}, it must be returned.
     */
    @Test
    void testReturnsFirstCapableIntegration() {
        // -----------------------------------------------------------------
        // GIVEN – two mock integrations, the first one capable, the second
        // also capable (but should never be consulted).
        // -----------------------------------------------------------------
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> first = mock(ExpressionsBasedModel.Integration.class);
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> second = mock(ExpressionsBasedModel.Integration.class);
        when(first.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        when(second.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        // Register them – the order of registration defines the iteration order.
        assertTrue(ExpressionsBasedModel.addIntegration(first));
        assertTrue(ExpressionsBasedModel.addIntegration(second));
        // -----------------------------------------------------------------
        // WHEN – a fresh model asks for its integration.
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Integration<?> actual = model.getIntegration();
        // -----------------------------------------------------------------
        // THEN – the first capable integration is returned.
        // -----------------------------------------------------------------
        assertSame(first, actual, "The first capable integration should be selected");
    }

    /**
     * If the first integration is not capable but a later one is, the method must
     * skip the non‑capable one and return the first capable integration.
     */
    @Test
    void testSkipsNonCapableAndReturnsNextCapable() {
        // -----------------------------------------------------------------
        // GIVEN – first integration not capable, second integration capable.
        // -----------------------------------------------------------------
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> notCapable = mock(ExpressionsBasedModel.Integration.class);
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> capable = mock(ExpressionsBasedModel.Integration.class);
        when(notCapable.isCapable(any(ExpressionsBasedModel.class))).thenReturn(false);
        when(capable.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        assertTrue(ExpressionsBasedModel.addIntegration(notCapable));
        assertTrue(ExpressionsBasedModel.addIntegration(capable));
        // -----------------------------------------------------------------
        // WHEN – request the integration.
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Integration<?> actual = model.getIntegration();
        // -----------------------------------------------------------------
        // THEN – the capable integration is returned, not the first one.
        // -----------------------------------------------------------------
        assertSame(capable, actual, "The first capable integration (the second entry) should be selected");
    }

    /**
     * When no integration in the static list is capable and none of the built‑in
     * solver integrations can handle the model, a {@link ProgrammingError}
     * must be thrown.
     *
     * The built‑in integrations (IntegerSolver, ConvexSolver, LinearSolver) are
     * left untouched; the test ensures that the static list is empty and that
     * the model does not contain any integer variables, which makes the
     * IntegerSolver branch irrelevant.  The default LinearSolver integration is
     * assumed to be *not* capable for an empty model – if the library changes
     * this behaviour the test will fail, indicating a change in the selection
     * logic that needs to be reviewed.
     */
    @Test
    void testThrowsWhenNoCapableIntegration() {
        // -----------------------------------------------------------------
        // GIVEN – an empty static integration list.
        // -----------------------------------------------------------------
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        // Create a model that has no integer variables and no constraints.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // -----------------------------------------------------------------
        // WHEN / THEN – expect a ProgrammingError.
        // -----------------------------------------------------------------
        ProgrammingError thrown = assertThrows(ProgrammingError.class, model::getIntegration, "When no integration can handle the model a ProgrammingError must be thrown");
        assertEquals("No solver integration available that can handle this model!", thrown.getMessage());
    }

    /**
     * Fallback to {@code IntegerSolver} when the model contains integer variables
     * and the built‑in integer integration reports capability.
     */
    @Test
    void testFallbackToIntegerSolver() throws Exception {
        // No user‑provided integrations
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        // Mock the IntegerSolver integration
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> intInt = mock(ExpressionsBasedModel.Integration.class);
        when(intInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        // Preserve original value and replace it with the mock
        Object original = getStaticField(IntegerSolver.class, "INTEGRATION");
        try (AutoCloseable ignored = restoreStaticField(IntegerSolver.class, "INTEGRATION", intInt)) {
            // reports integer variables
            TestModel model = new TestModel(true);
            ExpressionsBasedModel.Integration<?> actual = model.getIntegration();
            assertSame(intInt, actual, "IntegerSolver integration should be selected as fallback");
        } finally {
            // restore original value
            setStaticField(IntegerSolver.class, "INTEGRATION", original);
        }
    }

    /**
     * Fallback to {@code ConvexSolver} when integer variables are present but the
     * integer integration is not capable.
     */
    @Test
    void testFallbackToConvexWhenIntegerNotCapable() throws Exception {
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        // IntegerSolver integration – not capable
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> intInt = mock(ExpressionsBasedModel.Integration.class);
        when(intInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(false);
        Object origInt = getStaticField(IntegerSolver.class, "INTEGRATION");
        // ConvexSolver integration – capable
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> convexInt = mock(ExpressionsBasedModel.Integration.class);
        when(convexInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        Object origConvex = getStaticField(ConvexSolver.class, "INTEGRATION");
        try (AutoCloseable ignored1 = restoreStaticField(IntegerSolver.class, "INTEGRATION", intInt);
            AutoCloseable ignored2 = restoreStaticField(ConvexSolver.class, "INTEGRATION", convexInt)) {
            // integer variables present
            TestModel model = new TestModel(true);
            ExpressionsBasedModel.Integration<?> actual = model.getIntegration();
            assertSame(convexInt, actual, "ConvexSolver integration should be selected when IntegerSolver is not capable");
        } finally {
            setStaticField(IntegerSolver.class, "INTEGRATION", origInt);
            setStaticField(ConvexSolver.class, "INTEGRATION", origConvex);
        }
    }

    /**
     * Fallback to {@code ConvexSolver} when the model has no integer variables
     * and the convex integration reports capability.
     */
    @Test
    void testFallbackToConvexWhenNoIntegerVariables() throws Exception {
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> convexInt = mock(ExpressionsBasedModel.Integration.class);
        when(convexInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        Object origConvex = getStaticField(ConvexSolver.class, "INTEGRATION");
        try (AutoCloseable ignored = restoreStaticField(ConvexSolver.class, "INTEGRATION", convexInt)) {
            // no integer variables
            TestModel model = new TestModel(false);
            ExpressionsBasedModel.Integration<?> actual = model.getIntegration();
            assertSame(convexInt, actual, "ConvexSolver integration should be selected as fallback");
        } finally {
            setStaticField(ConvexSolver.class, "INTEGRATION", origConvex);
        }
    }

    /**
     * Fallback to {@code LinearSolver} when convex integration is not capable.
     */
    @Test
    void testFallbackToLinearWhenConvexNotCapable() throws Exception {
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        // Convex not capable
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> convexInt = mock(ExpressionsBasedModel.Integration.class);
        when(convexInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(false);
        Object origConvex = getStaticField(ConvexSolver.class, "INTEGRATION");
        // Linear capable
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> linearInt = mock(ExpressionsBasedModel.Integration.class);
        when(linearInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(true);
        Object origLinear = getStaticField(LinearSolver.class, "INTEGRATION");
        try (AutoCloseable ignored1 = restoreStaticField(ConvexSolver.class, "INTEGRATION", convexInt);
            AutoCloseable ignored2 = restoreStaticField(LinearSolver.class, "INTEGRATION", linearInt)) {
            // no integer vars
            TestModel model = new TestModel(false);
            ExpressionsBasedModel.Integration<?> actual = model.getIntegration();
            assertSame(linearInt, actual, "LinearSolver integration should be selected when ConvexSolver is not capable");
        } finally {
            setStaticField(ConvexSolver.class, "INTEGRATION", origConvex);
            setStaticField(LinearSolver.class, "INTEGRATION", origLinear);
        }
    }

    /**
     * A {@code null} element inside {@code INTEGRATIONS} should cause a
     * {@link NullPointerException} when the method attempts to call
     * {@code isCapable(this)} on it.
     */
    @Test
    void testNullElementInIntegrationsThrowsNPE() {
        // Directly manipulate the private static list via reflection
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        try {
            Field listField = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) listField.get(null);
            // insert null entry
            list.add(null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            assertThrows(NullPointerException.class, model::getIntegration, "Iterating over a null integration should throw NPE");
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    /**
     * Null {@code IntegerSolver.INTEGRATION} during fallback must result in a
     * {@link NullPointerException}.
     */
    @Test
    void testNullIntegerSolverIntegrationThrowsNPE() throws Exception {
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        // Preserve original and set to null
        Object original = getStaticField(IntegerSolver.class, "INTEGRATION");
        try (AutoCloseable ignored = restoreStaticField(IntegerSolver.class, "INTEGRATION", null)) {
            // integer variables present
            TestModel model = new TestModel(true);
            assertThrows(NullPointerException.class, model::getIntegration, "Null IntegerSolver.INTEGRATION should cause NPE");
        } finally {
            setStaticField(IntegerSolver.class, "INTEGRATION", original);
        }
    }

    /**
     * Null {@code ConvexSolver.INTEGRATION} during fallback must result in a
     * {@link NullPointerException}.
     */
    @Test
    void testNullConvexSolverIntegrationThrowsNPE() throws Exception {
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        Object original = getStaticField(ConvexSolver.class, "INTEGRATION");
        try (AutoCloseable ignored = restoreStaticField(ConvexSolver.class, "INTEGRATION", null)) {
            // no integer vars
            TestModel model = new TestModel(false);
            assertThrows(NullPointerException.class, model::getIntegration, "Null ConvexSolver.INTEGRATION should cause NPE");
        } finally {
            setStaticField(ConvexSolver.class, "INTEGRATION", original);
        }
    }

    /**
     * Null {@code LinearSolver.INTEGRATION} during fallback must result in a
     * {@link NullPointerException}.
     */
    @Test
    void testNullLinearSolverIntegrationThrowsNPE() throws Exception {
        assertTrue(ExpressionsBasedModel.clearIntegrations());
        // Convex not capable, Linear null
        @SuppressWarnings("unchecked")
        ExpressionsBasedModel.Integration<?> convexInt = mock(ExpressionsBasedModel.Integration.class);
        when(convexInt.isCapable(any(ExpressionsBasedModel.class))).thenReturn(false);
        Object origConvex = getStaticField(ConvexSolver.class, "INTEGRATION");
        Object origLinear = getStaticField(LinearSolver.class, "INTEGRATION");
        try (AutoCloseable ignored1 = restoreStaticField(ConvexSolver.class, "INTEGRATION", convexInt);
            AutoCloseable ignored2 = restoreStaticField(LinearSolver.class, "INTEGRATION", null)) {
            TestModel model = new TestModel(false);
            assertThrows(NullPointerException.class, model::getIntegration, "Null LinearSolver.INTEGRATION should cause NPE");
        } finally {
            setStaticField(ConvexSolver.class, "INTEGRATION", origConvex);
            setStaticField(LinearSolver.class, "INTEGRATION", origLinear);
        }
    }
}
