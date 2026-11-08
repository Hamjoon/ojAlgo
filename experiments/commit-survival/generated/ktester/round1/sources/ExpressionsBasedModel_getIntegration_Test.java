package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getIntegration_Test {

    /**
     * Reset the static integration registry before every test.
     */
    @BeforeEach
    void setupBeforeEach() {
        ExpressionsBasedModel.clearIntegrations();
        // Ensure built‑in integrations are in a known (non‑null) state for the tests that need them.
        resetBuiltInIntegrations();
    }

    /**
     * Also clear after each test – a safety net in case a test forgets to clean up.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearIntegrations();
        resetBuiltInIntegrations();
    }

    /**
     * Helper that restores the original built‑in integrations (or sets them to null).
     */
    private void resetBuiltInIntegrations() {
        // Most tests rely on the real integrations, so we simply set them to a mock that returns true.
        // Tests that need a different behaviour will overwrite the field again.
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(true));
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", mockIntegrationReturning(true));
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(true));
    }

    /**
     * Set a static field (even if final) via reflection.
     */
    private static void setStaticIntegration(Class<?> clazz, String fieldName, Object value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            // Remove final modifier if present
            java.lang.reflect.Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            f.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that a custom integration that declares itself capable for the model is returned
     * by {@code getIntegration()}.
     */
    @Test
    void testCustomCapableIntegrationIsReturned() {
        // -----------------------------------------------------------------
        // GIVEN – a dummy integration that always reports “capable”.
        // -----------------------------------------------------------------
        ExpressionsBasedModel.Integration<Object> dummy = new ExpressionsBasedModel.Integration<>() {

            @Override
            public boolean isCapable(final ExpressionsBasedModel model) {
                // The dummy claims to be able to handle any model.
                return true;
            }

            @Override
            public String toString() {
                return "DummyIntegration";
            }
        };
        // Register the dummy integration.  The static method returns true on successful add.
        assertTrue(ExpressionsBasedModel.addIntegration(dummy), "Failed to add dummy integration");
        // -----------------------------------------------------------------
        // WHEN – the model asks for an integration.
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Integration<?> integration = model.getIntegration();
        // -----------------------------------------------------------------
        // THEN – the returned instance is exactly the dummy we registered.
        // -----------------------------------------------------------------
        assertSame(dummy, integration, "The integration returned should be the dummy one we added");
    }

    /**
     * Verify that {@code getIntegration()} does not return {@code null} when no custom
     * integration is present.  The method should fall back to one of the built‑in
     * integrations (integer, convex or linear).  The exact type depends on the internal
     * state of those solvers, which is outside the scope of this unit test; we only assert
     * that a non‑null integration is obtained and that no {@link ProgrammingError}
     * is thrown.
     */
    @Test
    void testFallbackToBuiltInIntegration() {
        // -----------------------------------------------------------------
        // GIVEN – an empty integration registry.
        // -----------------------------------------------------------------
        // (setupBeforeEach already cleared the list)
        // -----------------------------------------------------------------
        // WHEN – we request an integration from a freshly created model.
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The call must succeed – i.e. not throw a ProgrammingError.
        ExpressionsBasedModel.Integration<?> integration = assertDoesNotThrow(model::getIntegration, "getIntegration() threw an unexpected ProgrammingError");
        // -----------------------------------------------------------------
        // THEN – a non‑null integration is returned.
        // -----------------------------------------------------------------
        assertNotNull(integration, "Fallback integration should never be null");
    }

    /**
     * Create a mock {@code Integration} whose {@code isCapable} returns the supplied value.
     */
    @SuppressWarnings("unchecked")
    private static ExpressionsBasedModel.Integration<Object> mockIntegrationReturning(boolean capable) {
        ExpressionsBasedModel.Integration<Object> mock = mock(ExpressionsBasedModel.Integration.class);
        when(mock.isCapable(any())).thenReturn(capable);
        return mock;
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testPreferredIntegrationFound() {
        // GIVEN – a mock integration that reports capable
        ExpressionsBasedModel.Integration<Object> preferred = mock(ExpressionsBasedModel.Integration.class);
        when(preferred.isCapable(any())).thenReturn(true);
        assertTrue(ExpressionsBasedModel.addIntegration(preferred), "Failed to add preferred integration");
        // WHEN
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Integration<?> result = model.getIntegration();
        // THEN
        assertSame(preferred, result, "The first capable integration should be returned");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testFirstCapablePreferredIntegrationReturned() {
        // first integration – not capable
        ExpressionsBasedModel.Integration<Object> notCapable = mock(ExpressionsBasedModel.Integration.class);
        when(notCapable.isCapable(any())).thenReturn(false);
        // second integration – capable
        ExpressionsBasedModel.Integration<Object> capable = mock(ExpressionsBasedModel.Integration.class);
        when(capable.isCapable(any())).thenReturn(true);
        // add both (order matters)
        assertTrue(ExpressionsBasedModel.addIntegration(notCapable));
        assertTrue(ExpressionsBasedModel.addIntegration(capable));
        // WHEN
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Integration<?> result = model.getIntegration();
        // THEN
        assertSame(capable, result, "Should skip non‑capable and return the first capable integration");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testIntegerVariableCausesIntegerSolverIntegration() {
        // GIVEN – empty INTEGRATIONS (cleared by @BeforeEach)
        // Mock model to report integer variables
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        doReturn(true).when(model).isAnyVariableInteger();
        // Mock IntegerSolver integration to be capable
        ExpressionsBasedModel.Integration<Object> intInt = mockIntegrationReturning(true);
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", intInt);
        // Ensure convex/linear are not consulted (they can be anything)
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        // WHEN
        ExpressionsBasedModel.Integration<?> result = model.getIntegration();
        // THEN
        assertSame(intInt, result, "IntegerSolver integration should be selected when integer variables exist");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testConvexSolverSelectedWhenNoIntegerVariables() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        doReturn(false).when(model).isAnyVariableInteger();
        // Convex solver capable, integer solver either capable or not – should not be consulted
        ExpressionsBasedModel.Integration<Object> convexInt = mockIntegrationReturning(true);
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", convexInt);
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        ExpressionsBasedModel.Integration<?> result = model.getIntegration();
        assertSame(convexInt, result, "ConvexSolver integration should be selected when no integer variables");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testFallbackToConvexWhenIntegerSolverNotCapable() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        doReturn(true).when(model).isAnyVariableInteger();
        // IntegerSolver not capable, ConvexSolver capable
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        ExpressionsBasedModel.Integration<Object> convexInt = mockIntegrationReturning(true);
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", convexInt);
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        ExpressionsBasedModel.Integration<?> result = model.getIntegration();
        assertSame(convexInt, result, "Should fall back to ConvexSolver when IntegerSolver is not capable");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testFallbackToLinearWhenIntegerAndConvexNotCapable() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        doReturn(true).when(model).isAnyVariableInteger();
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        ExpressionsBasedModel.Integration<Object> linearInt = mockIntegrationReturning(true);
        setStaticIntegration(LinearSolver.class, "INTEGRATION", linearInt);
        ExpressionsBasedModel.Integration<?> result = model.getIntegration();
        assertSame(linearInt, result, "Should fall back to LinearSolver when both Integer and Convex are not capable");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testNoIntegrationAvailableThrowsProgrammingError() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        doReturn(false).when(model).isAnyVariableInteger();
        // All built‑in integrations report not capable
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        ProgrammingError thrown = assertThrows(ProgrammingError.class, model::getIntegration, "Expected ProgrammingError when no integration can handle the model");
        assertEquals("No solver integration available that can handle this model!", thrown.getMessage(), "Exception message should match the one defined in the source");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testNullIntegrationInListThrowsNPE() {
        // Directly manipulate the private static list via reflection
        try {
            Field listField = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ExpressionsBasedModel.Integration<?>> list = (List<ExpressionsBasedModel.Integration<?>>) listField.get(null);
            list.clear();
            // first element is null
            list.add(null);
        } catch (Exception e) {
            fail("Failed to inject null into INTEGRATIONS list: " + e);
        }
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, model::getIntegration, "A null entry in INTEGRATIONS should cause NPE when iterated");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testPreferredIntegrationRuntimeExceptionPropagated() {
        ExpressionsBasedModel.Integration<Object> bad = mock(ExpressionsBasedModel.Integration.class);
        when(bad.isCapable(any())).thenThrow(new RuntimeException("boom"));
        assertTrue(ExpressionsBasedModel.addIntegration(bad), "Failed to add bad integration");
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        RuntimeException thrown = assertThrows(RuntimeException.class, model::getIntegration, "RuntimeException from isCapable should be propagated");
        assertEquals("boom", thrown.getMessage());
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testNullIntegerSolverIntegrationLeadsToNPE() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        doReturn(true).when(model).isAnyVariableInteger();
        // Set IntegerSolver.INTEGRATION to null
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", null);
        // Convex and Linear can be anything (they won't be reached)
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        assertThrows(NullPointerException.class, model::getIntegration, "Null IntegerSolver integration should cause NPE");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testNullConvexSolverIntegrationLeadsToNPE() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        // no integer vars
        doReturn(false).when(model).isAnyVariableInteger();
        // ConvexSolver null
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", null);
        // Integer and Linear not consulted / not capable
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        setStaticIntegration(LinearSolver.class, "INTEGRATION", mockIntegrationReturning(true));
        assertThrows(NullPointerException.class, model::getIntegration, "Null ConvexSolver integration should cause NPE");
    }

    /**
     * ---------------------------------------------------------------------
     */
    @Test
    void testNullLinearSolverIntegrationLeadsToNPE() {
        ExpressionsBasedModel model = spy(new ExpressionsBasedModel());
        // no integer vars
        doReturn(false).when(model).isAnyVariableInteger();
        // Convex not capable
        setStaticIntegration(ConvexSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        // LinearSolver null
        setStaticIntegration(LinearSolver.class, "INTEGRATION", null);
        // Integer not capable
        setStaticIntegration(IntegerSolver.class, "INTEGRATION", mockIntegrationReturning(false));
        assertThrows(NullPointerException.class, model::getIntegration, "Null LinearSolver integration should cause NPE");
    }
}
