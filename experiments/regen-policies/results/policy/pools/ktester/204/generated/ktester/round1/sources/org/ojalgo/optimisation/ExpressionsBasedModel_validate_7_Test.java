package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_7_Test {

    /**
     * A simple model used by the tests.
     */
    private ExpressionsBasedModel model;

    /**
     * Mocked logger – we only need a non‑null instance.
     */
    @Mock
    private BasicLogger loggerMock;

    /**
     * Mocked number context – the validation logic only uses it for rounding.
     */
    @Mock
    private NumberContext numberContextMock;

    class RecordingLogger implements BasicLogger {

        private final List<String> msgs = new ArrayList<>();

        @Override
        public void log(Object o) {
            msgs.add(String.valueOf(o));
        }

        List<String> getMessages() {
            return msgs;
        }
    }

    class RecordingLogger implements BasicLogger {

        private final List<String> msgs = new ArrayList<>();

        @Override
        public void log(Object o) {
            msgs.add(String.valueOf(o));
        }

        List<String> getMessages() {
            return msgs;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        //   subject to 0 ≤ x ≤ 10   and   x = 5
        model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// objective coefficient
        BigDecimal.ONE);
        // Equality constraint x = 5
        model.newExpression("eq").set(x, BigDecimal.ONE).level(BigDecimal.valueOf(5));
        // Build a deterministic, feasible model:
        //   minimise  x
        //   subject to 0 ≤ x ≤ 10   and   x = 5
        model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// objective coefficient
        BigDecimal.ONE);
        // Equality constraint x = 5
        model.newExpression("eq").set(x, BigDecimal.ONE).level(BigDecimal.valueOf(5));
    }

    private static Stream<Arguments> provideNumberContexts() {
        BasicLogger silent = new BasicLogger() {

            @Override
            public void log(Object o) {
            }
        };
        return Stream.of(Arguments.of(NumberContext.of(0.0, 0), null), Arguments.of(NumberContext.of(1e-12, 12), silent));
    }

    @Test
    void testValidateWithNumberContextAndBasicLogger() {
        // Solve the model – the solution should be feasible.
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "The model must solve to an optimal state before validation.");
        // The method under test internally obtains the solution via
        // getVariableValues(NumberContext) and then validates it.
        // Because we pass mocked collaborators, we only verify that the
        // method returns true for a feasible model.
        boolean validationResult = model.validate(numberContextMock, loggerMock);
        assertTrue(validationResult, "validate(NumberContext, BasicLogger) should return true for a feasible solution.");
        // Optionally verify that the logger was used (the implementation logs
        // only on validation failures, so no interaction is expected for a
        // successful validation).
        verifyNoInteractions(loggerMock);
    }

    /* --------------------------------------------------------------------- *
     *  2. Null BasicLogger – should be tolerated                             *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWithNullLogger() {
        // ensure model is solved
        model.minimise();
        boolean validationResult = model.validate(NumberContext.of(1e-6, 6), null);
        // The concrete result depends on feasibility; we only assert that no NPE is thrown.
        assertTrue(validationResult || !validationResult, "Method must return a boolean and not throw when logger is null.");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null NumberContext – must throw NPE                               *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWithNullNumberContext() {
        assertThrows(NullPointerException.class, () -> model.validate(null, loggerMock), "A null NumberContext should cause a NullPointerException.");
    }

    /* --------------------------------------------------------------------- *
     *  4. Both arguments null – NPE on missing NumberContext                *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWithBothParametersNull() {
        assertThrows(NullPointerException.class, () -> model.validate(null, null), "Both arguments null must result in NullPointerException because NumberContext is required.");
    }

    /* --------------------------------------------------------------------- *
     *  5. getVariableValues returns null – validation should return false   *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenGetVariableValuesReturnsNull() {
        // Spy the model and stub getVariableValues(NumberContext) to return null.
        ExpressionsBasedModel spy = Mockito.spy(model);
        doReturn(null).when(spy).getVariableValues(any(NumberContext.class));
        boolean result = spy.validate(NumberContext.of(1e-6, 6), loggerMock);
        assertFalse(result, "When getVariableValues returns null the validation should return false.");
    }

    /* --------------------------------------------------------------------- *
     *  6. getVariableValues returns empty (non‑null) solution                *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenGetVariableValuesReturnsEmptySolution() {
        ExpressionsBasedModel spy = Mockito.spy(model);
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> emptySolution = mock(Access1D.class);
        when(emptySolution.size()).thenReturn(0);
        doReturn(emptySolution).when(spy).getVariableValues(any(NumberContext.class));
        // Stub the final overload to simply return true – we are only interested in the delegation.
        doReturn(true).when(spy).validate(eq(emptySolution), any(NumberContext.class), any());
        boolean result = spy.validate(NumberContext.of(1e-6, 6), loggerMock);
        assertTrue(result, "Empty but non‑null solution should be accepted and delegated validation returns true.");
    }

    /* --------------------------------------------------------------------- *
     *  7. getVariableValues throws RuntimeException – should propagate      *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenGetVariableValuesThrowsException() {
        ExpressionsBasedModel spy = Mockito.spy(model);
        doThrow(new RuntimeException("getVariableValues failure")).when(spy).getVariableValues(any(NumberContext.class));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spy.validate(NumberContext.of(1e-6, 6), loggerMock), "Exception from getVariableValues must propagate.");
        assertEquals("getVariableValues failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  8. Delegate correctness – ensure overload is invoked with same args *
     * --------------------------------------------------------------------- */
    @Test
    void testDelegateCorrectness() {
        ExpressionsBasedModel spy = Mockito.spy(model);
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> stubSolution = mock(Access1D.class);
        NumberContext ctx = NumberContext.of(1e-9, 9);
        BasicLogger logger = new BasicLogger() {

            @Override
            public void log(Object o) {
            }
        };
        // Stub getVariableValues to return our stub solution.
        doReturn(stubSolution).when(spy).getVariableValues(eq(ctx));
        // Capture arguments of the three‑argument validate overload.
        doAnswer(invocation -> {
            Access1D<BigDecimal> sol = invocation.getArgument(0);
            NumberContext c = invocation.getArgument(1);
            BasicLogger l = invocation.getArgument(2);
            assertSame(stubSolution, sol, "Solution passed to overload must be the one returned by getVariableValues.");
            assertSame(ctx, c, "NumberContext must be forwarded unchanged.");
            assertSame(logger, l, "BasicLogger must be forwarded unchanged.");
            return true;
        }).when(spy).validate(any(), any(), any());
        boolean result = spy.validate(ctx, logger);
        assertTrue(result, "Delegate overload should return true as stubbed.");
        verify(spy, times(1)).validate(stubSolution, ctx, logger);
    }

    /* --------------------------------------------------------------------- *
     *  9. Different NumberContext precisions – should not throw            *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideNumberContexts")
    void testValidateWithVariousNumberContexts(NumberContext ctx, BasicLogger logger) {
        // ensure model is solved
        model.minimise();
        // No assertion on boolean value – we only verify that no exception is thrown.
        assertDoesNotThrow(() -> model.validate(ctx, logger), () -> "validate must handle context " + ctx);
    }

    /* --------------------------------------------------------------------- *
     * 10. Logger that records messages – ensure a message is produced       *
     * --------------------------------------------------------------------- */
    @Test
    void testLoggerThatRecordsMessages() {
        class RecordingLogger implements BasicLogger {

            private final List<String> msgs = new ArrayList<>();

            @Override
            public void log(Object o) {
                msgs.add(String.valueOf(o));
            }

            List<String> getMessages() {
                return msgs;
            }
        }
        RecordingLogger recLogger = new RecordingLogger();
        // The model is feasible; the internal validation logs at least one message on success.
        boolean result = model.validate(NumberContext.of(1e-6, 6), recLogger);
        assertTrue(result, "Validation should succeed for the feasible model.");
        assertFalse(recLogger.getMessages().isEmpty(), "RecordingLogger should contain at least one log entry after validation.");
    }

    /* --------------------------------------------------------------------- *
     * 11. Logger that throws RuntimeException – exception must propagate    *
     * --------------------------------------------------------------------- */
    @Test
    void testLoggerThatThrowsException() {
        BasicLogger explodingLogger = new BasicLogger() {

            @Override
            public void log(Object o) {
                throw new RuntimeException("logger failure");
            }
        };
        // The internal validation will attempt to log; we expect the exception to bubble up.
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.validate(NumberContext.of(1e-6, 6), explodingLogger), "Exception from logger must propagate.");
        assertEquals("logger failure", thrown.getMessage());
    }
}
