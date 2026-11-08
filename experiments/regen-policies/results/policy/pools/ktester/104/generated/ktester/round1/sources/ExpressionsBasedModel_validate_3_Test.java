package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.netio.BasicLogger;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.structure.Access1D;
import org.ojalgo.optimisation.Optimisation.Result;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_3_Test {

    /**
     * Model created for each test.
     */
    private ExpressionsBasedModel model;

    /**
     * Mocked logger – the {@code validate} method may write to it, but we do not need any real output.
     */
    @Mock
    private BasicLogger logger;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
        // Create a real instance and then spy on it.
        model = spy(new ExpressionsBasedModel());
    }

    /**
     * A feasible model (simple linear minimisation) should be validated
     * successfully – the method must return {@code true}.
     */
    @Test
    void testValidateReturnsTrueForFeasibleSolution() {
        // variable x with bounds [0,10] and weight 1 (objective coefficient)
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // minimise the objective (which is simply x)
        Optimisation.Result result = model.minimise();
        // the result must be optimal and the variable value must be at its lower bound (0)
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        assertEquals(0, result.doubleValue(0), 1e-12, "Variable x should be 0");
        // validate the solution using the mocked logger
        boolean valid = model.validate(logger);
        assertTrue(valid, "The feasible solution should be reported as valid");
    }

    /**
     * An infeasible model (contradictory bounds) should be reported as invalid.
     */
    @Test
    void testValidateReturnsFalseForInfeasibleModel() {
        // variable y with contradictory bounds: lower > upper
        model.newVariable("y").lower(// lower bound 5
        5).upper(// upper bound 4 -> infeasible
        4).weight(1);
        // Attempt to solve – the solver will recognise infeasibility.
        Optimisation.Result result = model.minimise();
        // The state must indicate infeasibility.
        assertTrue(result.getState().isInfeasible(), "Result should be infeasible");
        // Validation should return false because the solution does not satisfy the model.
        boolean valid = model.validate(logger);
        assertFalse(valid, "The infeasible model should be reported as invalid");
    }

    /* --------------------------------------------------------------
     *  1. Inner validation succeeds – the public method must return true.
     * -------------------------------------------------------------- */
    @Test
    void testValidatePropagatesTrueFromInnerValidate() {
        // ----- arrange ------------------------------------------------
        // a dummy result that will be returned by getVariableValues(...)
        Result dummyResult = mock(Result.class);
        // default feasibility context
        NumberContext defaultCtx = model.options.feasibility;
        // stub the two internal calls
        doReturn(dummyResult).when(model).getVariableValues(defaultCtx);
        doReturn(true).when(model).validate(eq(dummyResult), eq(defaultCtx), eq(logger));
        // ----- act ----------------------------------------------------
        boolean ok = model.validate(logger);
        // ----- assert -------------------------------------------------
        assertTrue(ok, "validate(BasicLogger) should return true when inner validate returns true");
        // verify the delegation order
        verify(model).getVariableValues(defaultCtx);
        verify(model).validate(dummyResult, defaultCtx, logger);
    }

    /* --------------------------------------------------------------
     *  2. Inner validation fails – the public method must return false.
     * -------------------------------------------------------------- */
    @Test
    void testValidatePropagatesFalseFromInnerValidate() {
        Result dummyResult = mock(Result.class);
        NumberContext defaultCtx = model.options.feasibility;
        doReturn(dummyResult).when(model).getVariableValues(defaultCtx);
        doReturn(false).when(model).validate(eq(dummyResult), eq(defaultCtx), eq(logger));
        boolean ok = model.validate(logger);
        assertFalse(ok, "validate(BasicLogger) should return false when inner validate returns false");
        verify(model).getVariableValues(defaultCtx);
        verify(model).validate(dummyResult, defaultCtx, logger);
    }

    /* --------------------------------------------------------------
     *  3. Null appender – should be forwarded unchanged.
     * -------------------------------------------------------------- */
    @Test
    void testValidateWithNullAppender() {
        Result dummyResult = mock(Result.class);
        NumberContext defaultCtx = model.options.feasibility;
        doReturn(dummyResult).when(model).getVariableValues(defaultCtx);
        doReturn(true).when(model).validate(eq(dummyResult), eq(defaultCtx), isNull());
        boolean ok = model.validate(null);
        assertTrue(ok, "validate(null) should propagate the inner boolean result");
        verify(model).validate(dummyResult, defaultCtx, null);
    }

    /* --------------------------------------------------------------
     *  4. Custom feasibility context is correctly propagated.
     * -------------------------------------------------------------- */
    @Test
    void testValidateUsesCustomFeasibilityContext() {
        // custom context with low precision – just an example
        NumberContext customCtx = new NumberContext(5, 2);
        // inject the custom context into the model's options
        model.options.feasibility = customCtx;
        Result dummyResult = mock(Result.class);
        doReturn(dummyResult).when(model).getVariableValues(customCtx);
        doReturn(true).when(model).validate(eq(dummyResult), eq(customCtx), eq(logger));
        boolean ok = model.validate(logger);
        assertTrue(ok, "validate should use the custom feasibility context");
        verify(model).getVariableValues(customCtx);
        verify(model).validate(dummyResult, customCtx, logger);
    }

    /* --------------------------------------------------------------
     *  5. getVariableValues returns null → NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void testValidateThrowsWhenGetVariableValuesReturnsNull() {
        NumberContext ctx = model.options.feasibility;
        // make getVariableValues return null
        doReturn(null).when(model).getVariableValues(ctx);
        assertThrows(NullPointerException.class, () -> model.validate(logger), "When getVariableValues returns null, validate should throw NPE");
        verify(model).getVariableValues(ctx);
        // inner validate must never be called
        verify(model, never()).validate(any(), any(), any());
    }

    /* --------------------------------------------------------------
     *  6. options.feasibility is null → NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void testValidateThrowsWhenFeasibilityContextIsNull() throws Exception {
        // set the feasibility field to null via reflection
        Field feasibilityField = Optimisation.Options.class.getDeclaredField("feasibility");
        feasibilityField.setAccessible(true);
        feasibilityField.set(model.options, null);
        // the call to getVariableValues(null) is expected to cause NPE (or similar)
        assertThrows(NullPointerException.class, () -> model.validate(logger), "Calling validate when options.feasibility is null should throw NPE");
    }

    /* --------------------------------------------------------------
     *  7. Appender throws during inner validation → exception propagates.
     * -------------------------------------------------------------- */
    @Test
    void testValidatePropagatesExceptionFromAppender() {
        Result dummyResult = mock(Result.class);
        NumberContext ctx = model.options.feasibility;
        doReturn(dummyResult).when(model).getVariableValues(ctx);
        // inner validate will throw IllegalStateException (simulating a logger failure)
        doThrow(new IllegalStateException("logger failure")).when(model).validate(eq(dummyResult), eq(ctx), eq(logger));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.validate(logger), "Exception from inner validate should propagate unchanged");
        assertEquals("logger failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  8. Model.options is null → NullPointerException before any call.
     * -------------------------------------------------------------- */
    @Test
    void testValidateThrowsWhenModelOptionsIsNull() throws Exception {
        // set the whole options field to null via reflection (final field)
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        assertThrows(NullPointerException.class, () -> model.validate(logger), "When model.options is null, validate should throw NPE");
    }

    /* --------------------------------------------------------------
     *  9. getVariableValues throws IllegalStateException → propagate.
     * -------------------------------------------------------------- */
    @Test
    void testValidatePropagatesExceptionFromGetVariableValues() {
        NumberContext ctx = model.options.feasibility;
        doThrow(new IllegalStateException("solver broken")).when(model).getVariableValues(ctx);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.validate(logger), "Exception from getVariableValues must propagate");
        assertEquals("solver broken", ex.getMessage());
    }

    /* --------------------------------------------------------------
     * 10. Inner validate throws IllegalArgumentException → propagate.
     * -------------------------------------------------------------- */
    @Test
    void testValidatePropagatesExceptionFromInnerValidate() {
        Result dummyResult = mock(Result.class);
        NumberContext ctx = model.options.feasibility;
        doReturn(dummyResult).when(model).getVariableValues(ctx);
        doThrow(new IllegalArgumentException("bad solution")).when(model).validate(eq(dummyResult), eq(ctx), eq(logger));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> model.validate(logger), "Exception from inner validate must propagate");
        assertEquals("bad solution", ex.getMessage());
    }
}
