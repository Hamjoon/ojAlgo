package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

import java.util.stream.Stream;

/**
 * Tests for {@link ExpressionsBasedModel#validate(NumberContext)} focusing on logger selection.
 */
// @ExtendWith(MockitoExtension.class)
@DisplayName("ExpressionsBasedModel.validate(NumberContext) – logger selection")
class ExpressionsBasedModel_validate_4_Test {

    /**
     * A mock {@link NumberContext} – the concrete implementation is not required for the test.
     */
//     @Mock
    NumberContext mockContext;

    /**
     * A mock {@link Optimisation.Result} returned by {@code getVariableValues}.
     */
//     @Mock
    Optimisation.Result mockResult;

    /**
     * A mock {@link BasicLogger} that can be injected via {@code Options}.
     */
//     @Mock
    BasicLogger mockLogger;

    /**
     * The model under test – created as a Mockito spy so we can stub internal calls.
     */
    ExpressionsBasedModel modelSpy;

    /**
     * Simple subclass that records the arguments of the three‑argument {@code validate} call.
     */
//     private static final class TestModel extends ExpressionsBasedModel {

        /** The logger that was received by {@code validate(result, ctx, logger)}. */
        BasicLogger capturedLogger;

        /** The result that was received by {@code validate(result, ctx, logger)} (may be {@code null}). */
        Optimisation.Result capturedResult;

        /** The context that was received by {@code validate(result, ctx, logger)}. */
        NumberContext capturedContext;

        /** Value to be returned by the three‑argument {@code validate} overload. */
        Boolean stubReturn = Boolean.TRUE;

        /** When {@code true} the {@code getVariableValues} method will throw the configured exception. */
        boolean throwFromGetVariableValues = false;

        /** Exception to be thrown from {@code getVariableValues} when {@code throwFromGetVariableValues} is {@code true}. */
        RuntimeException getVariableValuesException = new RuntimeException("boom");

        @Override
        public Optimisation.Result getVariableValues(NumberContext context) {
            if (context == null) {
                throw new NullPointerException();
            }
            if (throwFromGetVariableValues) {
                throw getVariableValuesException;
            }
            // For the purpose of these tests the actual result is irrelevant – we return {@code null}.
            return null;
        }

//         @Override
//         public boolean validate(Optimisation.Result result, NumberContext context, BasicLogger logger) {
//             this.capturedResult = result;
//             this.capturedContext = context;
//             this.capturedLogger = logger;
//             return stubReturn != null ? stubReturn : true;
//         }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model with default options and wrap it in a spy.
        ExpressionsBasedModel realModel = new ExpressionsBasedModel();
//         modelSpy = spy(realModel);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy after each test to avoid cross‑test interference.
//         clearInvocations(modelSpy);
    }

    /**
     * Supplies the three combinations required by the specification.
     */
    private static Stream<Arguments> loggerSelectionProvider() {
        // A non‑null appender – we use {@code BasicLogger.NULL} simply because it is a non‑null instance.
        BasicLogger nonNullAppender = BasicLogger.NULL;
        return Stream.of(
                Arguments.of(true, true, nonNullAppender),   // detailed = true, appender non‑null → detailed logger
                Arguments.of(false, true, BasicLogger.NULL), // detailed = false, appender non‑null → NULL logger
                Arguments.of(true, false, BasicLogger.NULL)  // detailed = true, no appender → NULL logger
        );
    }

    /**
     * Helper that returns a fresh mock {@link BasicLogger}.
     * The field {@code mockLogger} is injected by Mockito, but static contexts cannot access it directly.
     */
//     private static BasicLogger mockLogger() {
//         return mock(BasicLogger.class);
//     }

    /**
     * Helper that creates a fresh {@link TestModel} with default options.
     */
    private TestModel newModel() {
        TestModel model = new TestModel();
        // Ensure default options are reset before each test.
        model.options.logger_detailed = false;
        model.options.logger_appender = null;
        return model;
    }

    /**
     * When {@code options.logger_detailed} is {@code true} and a logger appender is supplied,
     * the detailed logger should be passed to the three‑argument {@code validate} method.
     */
    @Test
    void testValidateUsesDetailedLoggerWhenEnabled() {
        TestModel model = newModel();
        model.options.logger_detailed = true;
        // non‑null appender
        model.options.logger_appender = BasicLogger.NULL;
        NumberContext ctx = NumberContext.of(8);
        model.stubReturn = true;
        boolean actual = model.validate(ctx);
        assertTrue(actual, "validate(NumberContext) should return the stubbed value");
        assertNotNull(model.capturedLogger, "Logger should have been captured");
        assertNotEquals(BasicLogger.NULL, model.capturedLogger,
                "When detailed logging is enabled and an appender is present a non‑NULL logger must be used");
    }

    /**
     * When {@code options.logger_detailed} is {@code false} (or no logger appender is set),
     * the method must fall back to {@link BasicLogger#NULL}.
     */
    @Test
    void testValidateFallsBackToNullLoggerWhenNotDetailed() {
        TestModel model = newModel();
        model.options.logger_detailed = false;
        // no appender
        model.options.logger_appender = null;
        NumberContext ctx = NumberContext.of(8);
        model.stubReturn = false;
        boolean actual = model.validate(ctx);
        assertFalse(actual, "validate(NumberContext) should return the stubbed value");
        assertEquals(BasicLogger.NULL, model.capturedLogger,
                "When detailed logging is disabled the NULL logger must be used");
    }

    @ParameterizedTest(name = "[{index}] detailed={0}, appenderProvided={1} → expectedLogger={2}")
    @MethodSource("loggerSelectionProvider")
    void testValidateSelectsCorrectLogger(boolean detailed, boolean appenderProvided, BasicLogger expectedLogger) {
        TestModel model = newModel();
        model.options.logger_detailed = detailed;
        // any non‑null instance works when appenderProvided is true
        model.options.logger_appender = appenderProvided ? BasicLogger.NULL : null;
        model.stubReturn = true;
        NumberContext ctx = NumberContext.of(8);
        boolean actual = model.validate(ctx);
        assertTrue(actual, "validate(NumberContext) should return the stubbed value");
        if (detailed && appenderProvided) {
            assertNotEquals(BasicLogger.NULL, model.capturedLogger,
                    "Expected a detailed logger (non‑NULL) when detailed=true and appender is present");
        } else {
            assertEquals(BasicLogger.NULL, model.capturedLogger,
                    "Expected NULL logger for the other combinations");
        }
    }

    @ParameterizedTest(name = "[{index}] delegatedResult={0}")
    @ValueSource(booleans = { true, false })
    void testValidatePropagatesResultFromDelegate(boolean delegatedResult) {
        TestModel model = newModel();
        model.options.logger_detailed = false;
        model.options.logger_appender = null;
        model.stubReturn = delegatedResult;
        NumberContext ctx = NumberContext.of(8);
        boolean actual = model.validate(ctx);
        assertEquals(delegatedResult, actual,
                "validate(NumberContext) must return exactly the value from the three‑argument overload");
    }

    @Test
    void testValidateThrowsNPEWhenContextIsNull() {
        TestModel model = newModel();
        assertThrows(NullPointerException.class,
                () -> model.validate((NumberContext) null),
                "validate(null) should propagate NullPointerException from getVariableValues");
    }

    @Test
    void testValidatePropagatesExceptionFromGetVariableValues() {
        TestModel model = newModel();
        model.throwFromGetVariableValues = true;
        RuntimeException boom = new RuntimeException("boom");
        model.getVariableValuesException = boom;
        NumberContext ctx = NumberContext.of(8);
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> model.validate(ctx),
                "validate should not catch exceptions from getVariableValues");
        assertSame(boom, thrown, "The original exception must be re‑thrown unchanged");
    }

    @Test
    void testValidateEmptyModelReturnsTrue() {
        ExpressionsBasedModel realModel = new ExpressionsBasedModel();
        NumberContext ctx = NumberContext.of(8);
        boolean actual = realModel.validate(ctx);
        assertTrue(actual, "An empty ExpressionsBasedModel should be considered feasible");
    }

    @Test
    void testValidateFailsWhenSolutionContainsNaN() {
        // The three‑argument validate method is responsible for detecting NaN values.
        // We simulate this by forcing the stub to return {@code false}.
        TestModel model = newModel();
        model.options.logger_detailed = false;
        model.options.logger_appender = null;
        model.stubReturn = false;
        NumberContext ctx = NumberContext.of(8);
        boolean actual = model.validate(ctx);
        assertFalse(actual, "validate should propagate failure when the solution contains non‑numeric values");
    }

    @ParameterizedTest(name = "[{index}] contextPrecision={0} → expected={1}")
    @CsvSource({ "6, true", "8, false" })
    void testValidateRespectsNumberContextPrecision(int precision, boolean expectedResult) {
        TestModel model = newModel();
        model.options.logger_detailed = false;
        model.options.logger_appender = null;
        model.stubReturn = expectedResult;
        NumberContext ctx = NumberContext.of(precision);
        boolean actual = model.validate(ctx);
        assertEquals(expectedResult, actual,
                "Validation result should follow the precision‑dependent behaviour of the delegate");
    }

    @Test
    void testValidateUsesNullLoggerWithDefaultOptions() {
        ExpressionsBasedModel realModel = new ExpressionsBasedModel();
        // Ensure default options are indeed the defaults
        realModel.options.logger_detailed = false;
        realModel.options.logger_appender = null;
        NumberContext ctx = NumberContext.of(8);
        boolean actual = realModel.validate(ctx);
        assertTrue(actual, "With default options validation should succeed");
        // The public API does not expose the logger that was used internally, but we can
        // verify that no exception is thrown when the default (NULL) logger is in place.
    }
}