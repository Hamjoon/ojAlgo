package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.MathContext;
import java.text.NumberFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.optimisation.Optimisation.Result;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_4_Test {

    private static final NumberContext DEFAULT_CONTEXT = new NumberContext(NumberFormat.getInstance(), new MathContext(10), 10);

    @Nested
    class LoggerAppenderSelection {

        private static Stream<Arguments> loggerSelectionProvider() {
            return Stream.of(// logger_detailed = true, logger_appender != null
            Arguments.of(true, mock(BasicLogger.class), true), // logger_detailed = true, logger_appender == null
            Arguments.of(true, null, true), // logger_detailed = false (logger_appender value irrelevant)
            Arguments.of(false, mock(BasicLogger.class), true));
        }

        @ParameterizedTest(name = "[{index}] detailed={0}, appenderPresent={1}")
        @MethodSource("loggerSelectionProvider")
        void testLoggerAppenderSelection(boolean detailed, BasicLogger appender, boolean stubResult) {
            NumberContext ctx = NumberContext.of(6, 2);
            ExpressionsBasedModel model = spyModelWithOptions(opts -> {
                opts.logger_detailed = detailed;
                opts.logger_appender = appender;
            });
            Result mockResult = mock(Result.class);
            stubModelBehaviour(model, ctx, mockResult, stubResult, null);
            // Act
            boolean actual = model.validate(ctx);
            // Assert that the overloaded validate was called with the expected logger
            BasicLogger expectedLogger = (detailed && appender != null) ? appender : BasicLogger.NULL;
            verify(model).validate(eq(mockResult), eq(ctx), eq(expectedLogger));
            assertEquals(stubResult, actual);
        }
    }

    @Nested
    class SolutionPropagation {

        private static Stream<Arguments> solutionPropagationProvider() {
            return Stream.of(// normal context, stub returns true
            Arguments.of(NumberContext.of(5, 2), true, null), // different context, stub returns false
            Arguments.of(NumberContext.of(10, 5), false, null), // null context -> NPE
            Arguments.of(null, null, NullPointerException.class), // getVariableValues throws IllegalStateException
            Arguments.of(NumberContext.of(6, 2), null, IllegalStateException.class));
        }

        @ParameterizedTest(name = "[{index}] context={0}")
        @MethodSource("solutionPropagationProvider")
        void testSolutionPropagation(NumberContext ctx, Boolean stubResult, Class<? extends Throwable> expectedException) {
            ExpressionsBasedModel model = spyModelWithOptions(opts -> {
                opts.logger_detailed = false;
                opts.logger_appender = null;
            });
            Result mockResult = mock(Result.class);
            if (expectedException == IllegalStateException.class) {
                // Force getVariableValues to throw
                doThrow(new IllegalStateException("forced")).when(model).getVariableValues(eq(ctx));
                assertThrows(IllegalStateException.class, () -> model.validate(ctx));
                return;
            }
            if (expectedException == NullPointerException.class) {
                // Passing null context should cause NPE inside the method
                assertThrows(NullPointerException.class, () -> model.validate(null));
                return;
            }
            // Normal flow
            stubModelBehaviour(model, ctx, mockResult, stubResult, null);
            boolean actual = model.validate(ctx);
            assertEquals(stubResult, actual);
            verify(model).validate(eq(mockResult), eq(ctx), eq(BasicLogger.NULL));
        }
    }

    @Nested
    class LoggerSelectionWithSolutionVerification {

        private static Stream<Arguments> loggerVerificationProvider() {
            return Stream.of(// detailed true, custom logger
            Arguments.of(true, mock(BasicLogger.class), true), // detailed true, null logger
            Arguments.of(true, null, true), // detailed false, any logger (ignored)
            Arguments.of(false, mock(BasicLogger.class), true));
        }

        @ParameterizedTest(name = "[{index}] detailed={0}")
        @MethodSource("loggerVerificationProvider")
        void testLoggerSelectionWithSolutionVerification(boolean detailed, BasicLogger appender, boolean stubResult) {
            NumberContext ctx = NumberContext.of(4, 1);
            ExpressionsBasedModel model = spyModelWithOptions(opts -> {
                opts.logger_detailed = detailed;
                opts.logger_appender = appender;
            });
            Result mockResult = mock(Result.class);
            stubModelBehaviour(model, ctx, mockResult, stubResult, null);
            boolean actual = model.validate(ctx);
            assertEquals(stubResult, actual);
            BasicLogger expectedLogger = (detailed && appender != null) ? appender : BasicLogger.NULL;
            verify(model).validate(eq(mockResult), eq(ctx), eq(expectedLogger));
        }
    }

    @BeforeEach
    void resetStaticPresolvers() {
        // Ensure static state does not interfere with tests.
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Creates a spy of {@link ExpressionsBasedModel} with a freshly created {@link Optimisation.Options}
     * instance. The options can be customised via the provided lambda.
     */
    private static ExpressionsBasedModel spyModelWithOptions(java.util.function.Consumer<Optimisation.Options> optConfigurer) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Options is a public final field – we need reflection to replace it.
        try {
            Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
            optionsField.setAccessible(true);
            Optimisation.Options opts = new Optimisation.Options();
            optConfigurer.accept(opts);
            optionsField.set(model, opts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return spy(model);
    }

    /**
     * Stubs {@code getVariableValues(context)} and the overloaded {@code validate(solution, context, appender)}.
     *
     * @param spy          the spied model
     * @param context      the context argument used for stubbing
     * @param solution     the {@link Result} that {@code getVariableValues} should return (may be null)
     * @param validateRet the boolean value the overloaded {@code validate} should return (ignored if exception is supplied)
     * @param exception    optional exception that the overloaded {@code validate} should throw
     */
    private static void stubModelBehaviour(ExpressionsBasedModel spy, NumberContext context, Result solution, Boolean validateRet, RuntimeException exception) {
        // Stub getVariableValues(context)
        doReturn(solution).when(spy).getVariableValues(eq(context));
        // Stub the overloaded validate(...)
        if (exception != null) {
            doThrow(exception).when(spy).validate(any(Access1D.class), eq(context), any(BasicLogger.class));
        } else {
            doReturn(validateRet).when(spy).validate(any(Access1D.class), eq(context), any(BasicLogger.class));
        }
    }

    /**
     * A tiny feasible model: one variable constrained to a single value.
     * The validation should succeed.
     */
    @Test
    void testValidateFeasibleModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable x ∈ [0,10] with weight 1 (contributes to the objective)
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // Constraint: x = 5
        Expression expr = model.newExpression("x_eq_5").lower(5).upper(5);
        // coefficient 1·x
        expr.set(x, 1);
        // NumberContext – precision/scale are not critical for this test
        NumberContext context = new NumberContext(NumberFormat.getInstance(), new MathContext(10), 10);
        assertTrue(model.validate(context), "Feasible model should validate successfully");
    }

    /**
     * An infeasible model: the variable bounds contradict the constraint.
     * The validation should fail.
     */
    @Test
    void testValidateInfeasibleModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable y ∈ [0,1]
        Variable y = model.newVariable("y").lower(0).upper(1).weight(1);
        // Constraint: y ≥ 2 (i.e. lower bound 2, no upper bound)
        Expression expr = model.newExpression("y_ge_2").lower(// upper is unbounded
        2);
        expr.set(y, 1);
        NumberContext context = new NumberContext(NumberFormat.getInstance(), new MathContext(10), 10);
        assertFalse(model.validate(context), "Infeasible model should not validate");
    }

    @Test
    void testNullOptionsHandling() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use reflection to set the final field `options` to null
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        // The method should throw NPE when accessing options.logger_detailed
        assertThrows(NullPointerException.class, () -> model.validate(NumberContext.of(6, 2)));
    }

    @Test
    void testNullSolutionHandling() {
        NumberContext ctx = NumberContext.of(5, 2);
        ExpressionsBasedModel model = spyModelWithOptions(opts -> {
            opts.logger_detailed = false;
            opts.logger_appender = null;
        });
        // Stub getVariableValues to return null
        doReturn(null).when(model).getVariableValues(eq(ctx));
        // The overloaded validate will receive a null solution – we let it throw NPE
        doCallRealMethod().when(model).validate(isNull(), eq(ctx), any());
        assertThrows(NullPointerException.class, () -> model.validate(ctx));
    }

    @Test
    void testOverloadedValidateExceptionPropagation() {
        NumberContext ctx = NumberContext.of(4, 1);
        ExpressionsBasedModel model = spyModelWithOptions(opts -> {
            opts.logger_detailed = true;
            opts.logger_appender = mock(BasicLogger.class);
        });
        Result mockResult = mock(Result.class);
        stubModelBehaviour(model, ctx, mockResult, null, new IllegalStateException("forced"));
        // The IllegalStateException should propagate unchanged
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.validate(ctx));
        assertEquals("forced", thrown.getMessage());
    }
}
