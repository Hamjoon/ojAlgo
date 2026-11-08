package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.netio.BasicLogger;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.ojalgo.optimisation.Optimisation.Options;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_3_Test {

    private ExpressionsBasedModel model;

    private BasicLogger logger;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures isolation.
        model = new ExpressionsBasedModel();
        // default no‑op logger
        logger = new BasicLogger();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model – good practice for classes that implement {@link AutoCloseable}
        // or hold native resources. The dispose method is a no‑op for the current implementation,
        // but calling it makes the test robust against future changes.
        model.dispose();
    }

    /**
     * A simple feasible model:
     *
     * <pre>
     *   minimise  x
     *   subject to 0 ≤ x ≤ 10
     * </pre>
     *
     * The optimal solution is x = 0, which satisfies the feasibility check.
     */
    @Test
    void testValidateReturnsTrueForFeasibleModel() {
        // Define a single variable with bounds.
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// objective weight = 1
        BigDecimal.ONE);
        // No additional constraints are required – the variable bounds already define a feasible region.
        // The model’s objective is to minimise the weighted sum of variables (here just x).
        // The validate method internally solves the model using the options.feasibility context.
        boolean isValid = model.validate(logger);
        assertTrue(isValid, "The model is feasible and should validate to true");
    }

    /**
     * An infeasible model:
     *
     * <pre>
     *   minimise  x
     *   subject to 0 ≤ x ≤ 5
     *              x ≥ 10
     * </pre>
     *
     * The bounds are contradictory, therefore validation must fail.
     */
    @Test
    void testValidateReturnsFalseForInfeasibleModel() {
        // Variable with contradictory bounds.
        Variable x = model.newVariable("x").lower(// lower bound 10
        BigDecimal.TEN).upper(// upper bound 5  -> infeasible
        BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // The model is now infeasible by construction.
        boolean isValid = model.validate(logger);
        assertFalse(isValid, "The model is infeasible and should validate to false");
    }

    /**
     * Verify that the logger instance is passed through to the internal validation routine.
     * The {@link BasicLogger} used in the test does not produce output, but we can ensure that
     * no exception is thrown when it is supplied.
     */
    @Test
    void testValidateDoesNotThrowWhenLoggerIsProvided() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertDoesNotThrow(() -> model.validate(logger), "validate(BasicLogger) should not throw when a logger is supplied");
    }

    /**
     * The underlying overloaded {@code validate} method returns {@code true};
     * the outer {@code validate(BasicLogger)} must propagate that value.
     */
    @Test
    void testValidateReturnsTrueWhenUnderlyingValidationSucceeds() {
        // Create a spy so we can stub internal calls while keeping the rest of the behaviour real.
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        // Mock the result returned by getVariableValues(NumberContext).
        Result mockResult = mock(Result.class);
        when(spy.getVariableValues(any(NumberContext.class))).thenReturn(mockResult);
        // Stub the overloaded validate(Access1D<BigDecimal>, NumberContext, BasicLogger) to return true.
        when(spy.validate(eq(mockResult), any(NumberContext.class), any(BasicLogger.class))).thenReturn(true);
        // Use a mock logger – no interactions are required for this scenario.
        BasicLogger mockLogger = mock(BasicLogger.class);
        boolean valid = spy.validate(mockLogger);
        assertTrue(valid, "validate should return true when the delegated validation succeeds");
    }

    /**
     * The underlying overloaded {@code validate} method returns {@code false};
     * the outer {@code validate(BasicLogger)} must propagate that value.
     */
    @Test
    void testValidateReturnsFalseWhenUnderlyingValidationFails() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        Result mockResult = mock(Result.class);
        when(spy.getVariableValues(any(NumberContext.class))).thenReturn(mockResult);
        when(spy.validate(eq(mockResult), any(NumberContext.class), any(BasicLogger.class))).thenReturn(false);
        BasicLogger mockLogger = mock(BasicLogger.class);
        boolean valid = spy.validate(mockLogger);
        assertFalse(valid, "validate should return false when the delegated validation fails");
    }

    /**
     * If the {@code options} field of the model is {@code null},
     * accessing {@code options.feasibility} must throw a {@link NullPointerException}.
     */
    @Test
    void testValidateThrowsNPEWhenOptionsIsNull() throws Exception {
        // Use reflection to null‑out the final field 'options'.
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        BasicLogger mockLogger = mock(BasicLogger.class);
        assertThrows(NullPointerException.class, () -> model.validate(mockLogger), "A null 'options' field should cause NPE");
    }

    /**
     * If {@code options.feasibility} is {@code null}, the call to
     * {@code getVariableValues(null)} should result in a {@link NullPointerException}.
     */
    @Test
    void testValidateThrowsNPEWhenFeasibilityIsNull() throws Exception {
        // non‑null by default
        Options opts = model.options;
        Field feasField = Options.class.getDeclaredField("feasibility");
        feasField.setAccessible(true);
        // make feasibility null
        feasField.set(opts, null);
        BasicLogger mockLogger = mock(BasicLogger.class);
        assertThrows(NullPointerException.class, () -> model.validate(mockLogger), "A null feasibility context should cause NPE");
    }

    /**
     * When {@code getVariableValues(context)} returns {@code null},
     * the subsequent call to {@code validate(null, ...)} must throw a {@link NullPointerException}.
     */
    @Test
    void testValidateThrowsNPEWhenGetVariableValuesReturnsNull() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        // Stub getVariableValues to return null.
        when(spy.getVariableValues(any(NumberContext.class))).thenReturn(null);
        BasicLogger mockLogger = mock(BasicLogger.class);
        assertThrows(NullPointerException.class, () -> spy.validate(mockLogger), "A null Result from getVariableValues should cause NPE");
    }

    /**
     * If the overloaded {@code validate} method throws an {@link IllegalStateException},
     * the outer {@code validate(BasicLogger)} must let it propagate unchanged.
     */
    @Test
    void testValidatePropagatesExceptionFromOverloadedValidate() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        Result mockResult = mock(Result.class);
        when(spy.getVariableValues(any(NumberContext.class))).thenReturn(mockResult);
        // Make the delegated validate throw.
        when(spy.validate(eq(mockResult), any(NumberContext.class), any(BasicLogger.class))).thenThrow(new IllegalStateException("validation error"));
        BasicLogger mockLogger = mock(BasicLogger.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spy.validate(mockLogger), "Exception from the overloaded validate should propagate");
        assertEquals("validation error", ex.getMessage());
    }

    /**
     * Passing a {@code null} {@link BasicLogger} must result in a {@link NullPointerException}
     * (the method dereferences the logger when delegating to the overloaded version).
     */
    @Test
    void testValidateThrowsNPEWhenAppenderIsNull() {
        // Use a normal model – the internal state is valid.
        assertThrows(NullPointerException.class, () -> model.validate(null), "Providing a null logger should cause NPE");
    }

    /**
     * If {@code getVariableValues(context)} itself throws an exception,
     * the outer {@code validate(BasicLogger)} must not catch it.
     */
    @Test
    void testValidatePropagatesExceptionFromGetVariableValues() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        // Stub getVariableValues to throw.
        when(spy.getVariableValues(any(NumberContext.class))).thenThrow(new IllegalStateException("cannot compute variable values"));
        BasicLogger mockLogger = mock(BasicLogger.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spy.validate(mockLogger), "Exception from getVariableValues should propagate");
        assertEquals("cannot compute variable values", ex.getMessage());
    }
}
