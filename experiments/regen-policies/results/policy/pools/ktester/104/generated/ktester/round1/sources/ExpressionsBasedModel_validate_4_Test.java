package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.MathContext;
import java.text.DecimalFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_4_Test {

    private ExpressionsBasedModel modelSpy;

    private Optimisation.Result dummyResult;

    @Mock
    private BasicLogger // used in several test cases
    customLogger;

    @Nested
    @DisplayName("Logger handling")
    class LoggerHandling {

        @Test
        @DisplayName("logger_detailed = true && logger_appender != null → custom logger is forwarded")
        void detailedTrueAppenderPresent() {
            setOptions(true, customLogger);
            NumberContext ctx = NumberContext.of(1E-6, 10);
            // stub the collaborators
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doReturn(true).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            // invoke method under test
            boolean ok = modelSpy.validate(ctx);
            // verify that the custom logger was passed to the overloaded validate
            ArgumentCaptor<BasicLogger> loggerCaptor = ArgumentCaptor.forClass(BasicLogger.class);
            verify(modelSpy).validate(eq(dummyResult), eq(ctx), loggerCaptor.capture());
            assertSame(customLogger, loggerCaptor.getValue(), "The custom logger should be forwarded");
            assertTrue(ok, "Result should be propagated");
        }

        @Test
        @DisplayName("logger_detailed = true && logger_appender == null → BasicLogger.NULL is used")
        void detailedTrueAppenderNull() {
            setOptions(true, null);
            NumberContext ctx = NumberContext.of(1E-6, 10);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doReturn(true).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            boolean ok = modelSpy.validate(ctx);
            ArgumentCaptor<BasicLogger> loggerCaptor = ArgumentCaptor.forClass(BasicLogger.class);
            verify(modelSpy).validate(eq(dummyResult), eq(ctx), loggerCaptor.capture());
            assertSame(BasicLogger.NULL, loggerCaptor.getValue(), "When appender is null, NULL logger must be used");
            assertTrue(ok);
        }

        @Test
        @DisplayName("logger_detailed = false (any appender) → BasicLogger.NULL is used")
        void detailedFalseIgnoresAppender() {
            setOptions(false, customLogger);
            NumberContext ctx = NumberContext.of(1E-6, 10);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doReturn(true).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            boolean ok = modelSpy.validate(ctx);
            ArgumentCaptor<BasicLogger> loggerCaptor = ArgumentCaptor.forClass(BasicLogger.class);
            verify(modelSpy).validate(eq(dummyResult), eq(ctx), loggerCaptor.capture());
            assertSame(BasicLogger.NULL, loggerCaptor.getValue(), "When logger_detailed is false, the appender must be ignored");
            assertTrue(ok);
        }
    }

    @Nested
    @DisplayName("Result propagation")
    class ResultPropagation {

        @Test
        @DisplayName("overloaded validate returns true → true is propagated")
        void overloadReturnsTrue() {
            setOptions(false, null);
            NumberContext ctx = NumberContext.of(1E-9, 5);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doReturn(true).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            assertTrue(modelSpy.validate(ctx), "True result must be propagated");
        }

        @Test
        @DisplayName("overloaded validate returns false → false is propagated")
        void overloadReturnsFalse() {
            setOptions(false, null);
            NumberContext ctx = NumberContext.of(1E-3, 2);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doReturn(false).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            assertFalse(modelSpy.validate(ctx), "False result must be propagated");
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("null NumberContext → NullPointerException")
        void nullContext() {
            setOptions(false, null);
            assertThrows(NullPointerException.class, () -> modelSpy.validate((NumberContext) null));
        }

        @Test
        @DisplayName("null options field → NullPointerException")
        void nullOptionsField() throws Exception {
            // Force the options field to null via reflection
            java.lang.reflect.Field f = ExpressionsBasedModel.class.getDeclaredField("options");
            f.setAccessible(true);
            f.set(modelSpy, null);
            NumberContext ctx = NumberContext.of(1E-6, 5);
            assertThrows(NullPointerException.class, () -> modelSpy.validate(ctx));
        }

        @Test
        @DisplayName("getVariableValues throws IllegalStateException → propagates")
        void getVariableValuesThrows() {
            setOptions(false, null);
            NumberContext ctx = NumberContext.of(1E-6, 5);
            doThrow(new IllegalStateException("model not ready")).when(modelSpy).getVariableValues(ctx);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> modelSpy.validate(ctx));
            assertEquals("model not ready", ex.getMessage());
        }

        @Test
        @DisplayName("getVariableValues returns null → downstream NullPointerException")
        void getVariableValuesReturnsNull() {
            setOptions(false, null);
            NumberContext ctx = NumberContext.of(1E-6, 5);
            doReturn(null).when(modelSpy).getVariableValues(ctx);
            // The overloaded validate will be called with a null solution -> NPE expected
            doCallRealMethod().when(modelSpy).validate(any(), any(), any());
            assertThrows(NullPointerException.class, () -> modelSpy.validate(ctx));
        }

        @Test
        @DisplayName("overloaded validate throws RuntimeException → propagates")
        void overloadThrowsRuntime() {
            setOptions(true, customLogger);
            NumberContext ctx = NumberContext.of(1E-4, 6);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doThrow(new RuntimeException("validation failed")).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> modelSpy.validate(ctx));
            assertEquals("validation failed", ex.getMessage());
        }

        @Test
        @DisplayName("overloaded validate throws IllegalArgumentException → propagates")
        void overloadThrowsIllegalArgument() {
            setOptions(false, null);
            NumberContext ctx = NumberContext.of(1E-6, 5);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doThrow(new IllegalArgumentException("bad dimensions")).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> modelSpy.validate(ctx));
            assertEquals("bad dimensions", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Interaction verification")
    class InteractionVerification {

        @Test
        @DisplayName("getVariableValues receives the exact NumberContext instance")
        void getVariableValuesCalledOnceWithSameContext() {
            setOptions(false, null);
            NumberContext ctx = NumberContext.of(1E-6, 8);
            doReturn(dummyResult).when(modelSpy).getVariableValues(ctx);
            doReturn(true).when(modelSpy).validate(any(Access1D.class), eq(ctx), any(BasicLogger.class));
            boolean ok = modelSpy.validate(ctx);
            assertTrue(ok);
            // verify exactly one invocation with the same instance
            verify(modelSpy, times(1)).getVariableValues(same(ctx));
        }
    }

    @BeforeEach
    void init() {
        // Create a real model and spy on it.
        modelSpy = spy(new ExpressionsBasedModel());
        // A dummy result that implements Access1D<BigDecimal>.
        dummyResult = mock(Optimisation.Result.class);
        // minimal stub
        when(dummyResult.get(anyInt())).thenReturn(BigDecimal.ZERO);
        // Ensure the model has a non‑null Options instance.
        assertNotNull(modelSpy.options, "Options must be initialised by the constructor");
    }

    /**
     * Helper that configures the Options fields via reflection (they are package‑private).
     */
    private void setOptions(boolean detailed, BasicLogger appender) {
        modelSpy.options.logger_detailed = detailed;
        modelSpy.options.logger_appender = appender;
    }

    /**
     * Simple linear model:
     *
     * minimise   x
     * subject to   0 ≤ x ≤ 10
     *
     * The optimal solution is x = 0. After solving we invoke
     * {@code model.validate(context)} and expect it to return {@code true}.
     */
    @Test
    void testValidateAfterMinimise() {
        // -----------------------------------------------------------------
        // 1) Build the model
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a single variable with bounds [0,10] and weight 1 (contributes to objective)
        Variable x = model.newVariable("x").lower(// lower bound
        0).upper(// upper bound
        10).weight(// contribution to the objective
        1);
        // the objective is simply the variable itself (minimise x)
        model.newExpression("obj").weight(// weight of the expression in the objective
        1).set(x, // expression = 1 * x
        1);
        // -----------------------------------------------------------------
        // 2) Solve the model – the optimum should be x = 0
        // -----------------------------------------------------------------
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Model should solve to an optimal state");
        // -----------------------------------------------------------------
        // 3) Create a NumberContext that will be used for validation.
        //    The constructor is package‑private, therefore the test class
        //    resides in the same package.
        // -----------------------------------------------------------------
        NumberContext validationContext = new NumberContext(// number format (irrelevant for the test)
        new DecimalFormat(), // precision
        new MathContext(10), // scale
        5);
        // -----------------------------------------------------------------
        // 4) Call the method under test and assert the expected outcome
        // -----------------------------------------------------------------
        boolean isValid = model.validate(validationContext);
        assertTrue(isValid, "The solution returned by the model should be valid");
    }
}
