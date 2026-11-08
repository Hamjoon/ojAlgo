package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.MathContext;
import java.text.NumberFormat;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_7_Test {

    /**
     * The model under test.
     */
    private ExpressionsBasedModel model;

    /**
     * A {@link NumberContext} used for validation (package‑private constructor is accessible because the test is in the same package).
     */
    private NumberContext validationContext;

    /**
     * Mocked logger – the validator only writes to it, we do not need real output.
     */
    @Mock
    private BasicLogger loggerMock;

    /**
     * Initialise a fresh model and validation context before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required for @Mock fields).
        MockitoAnnotations.openMocks(this);
        // Simple NumberContext: 10‑digit precision, scale 10.
        validationContext = new NumberContext(NumberFormat.getInstance(), // precision = 10, default rounding mode
        new MathContext(10), // scale
        10);
        // Build a tiny optimisation problem:
        //   minimise  x
        //   subject to   x = 5   and   0 ≤ x ≤ 10
        model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(// lower bound
        0).upper(// upper bound
        10).weight(// objective weight (minimise)
        1);
        // Equality constraint x = 5
        model.newExpression("eq").set(x, // coefficient 1·x
        1).level(// exact level 5
        5);
    }

    /* -----------------------------------------------------------------------
     * Helper to create a mocked Optimisation.Result that returns a single
     * variable value (as BigDecimal) and reports an OPTIMAL state.
     * --------------------------------------------------------------------- */
    private static Optimisation.Result mockResultWithValue(double value) {
        @SuppressWarnings("unchecked")
        Optimisation.Result mockResult = mock(Optimisation.Result.class, CALLS_REAL_METHODS);
        // State must be OPTIMAL for the validator to accept the solution.
        when(mockResult.getState()).thenReturn(Optimisation.State.OPTIMAL);
        // Access1D<BigDecimal> contract – size() and get(index)
        when(mockResult.size()).thenReturn(1);
        when(mockResult.get(anyInt())).thenAnswer(invocation -> BigDecimal.valueOf(value));
        // Helper methods used by the test code.
        when(mockResult.doubleValue(0)).thenReturn(value);
        when(mockResult.bigDecimalValue(0)).thenReturn(BigDecimal.valueOf(value));
        return mockResult;
    }

    private static Stream<Arguments> validSolutionProvider() {
        NumberContext ctx = NumberContext.of(1E-9, 10);
        // logs to System.out
        BasicLogger logger = new BasicLogger();
        return Stream.of(Arguments.of(ctx, logger));
    }

    private static Stream<Arguments> outOfBoundsProvider() {
        NumberContext ctx = NumberContext.of(1E-9, 10);
        BasicLogger logger = new BasicLogger();
        return Stream.of(Arguments.of(ctx, logger));
    }

    /**
     * A basic positive test: after solving the model the {@code validate} method should return {@code true}
     * when supplied with a proper {@link NumberContext} and a logger.
     */
    @Test
    void testValidateReturnsTrueForFeasibleSolution() {
        // Solve the model – the optimum should satisfy the constraint x = 5.
        Optimisation.Result result = model.minimise();
        // The solver should report an optimal state.
        assertTrue(result.getState().isOptimal(), () -> "Expected an optimal result, but got: " + result.getState());
        // The variable value must be exactly 5 (within the default epsilon of the context).
        assertEquals(5, result.doubleValue(0), validationContext.getEpsilon(), () -> "Variable x does not have the expected value.");
        // Now invoke the method under test.
        boolean valid = model.validate(validationContext, loggerMock);
        // The validation should succeed because the solution respects all bounds and constraints.
        assertTrue(valid, "Model.validate(NumberContext, BasicLogger) should return true for a feasible solution.");
    }

    /**
     * A negative test: if the model is deliberately put into an infeasible state,
     * {@code validate} must return {@code false}.
     */
    @Test
    void testValidateReturnsFalseForInfeasibleModel() {
        // Make the model infeasible by adding a contradictory constraint: x = 7 while the existing one forces x = 5.
        Variable x = model.getVariable(0);
        model.newExpression("conflict").set(x, 1).level(// contradictory level
        7);
        // The model should now be infeasible.
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isInfeasible(), () -> "Expected an infeasible result after adding contradictory constraint.");
        // Validation should report false.
        boolean valid = model.validate(validationContext, loggerMock);
        assertFalse(valid, "Model.validate(NumberContext, BasicLogger) should return false for an infeasible model.");
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("validSolutionProvider")
    void validate_withValidSolution(NumberContext context, BasicLogger logger) {
        // Model with a single variable fixed to 5 (no extra constraints).
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.newVariable("x").lower(5).upper(// fixed at 5
        5).weight(// objective weight 0 (irrelevant)
        0);
        // The solver will return the fixed value 5, which satisfies all bounds.
        boolean ok = m.validate(context, logger);
        assertTrue(ok, "Validation should succeed for a feasible solution.");
    }

    @ParameterizedTest(name = "{index} => out‑of‑bounds")
    @MethodSource("outOfBoundsProvider")
    void validate_withInvalidSolution_OutOfBounds(NumberContext context, BasicLogger logger) {
        // Variable y bounded [0,10] but we force a contradictory equality y = 12.
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable y = m.newVariable("y").lower(0).upper(10).weight(0);
        // Equality constraint that cannot be satisfied.
        m.newExpression("bad").set(y, 1).level(12);
        boolean ok = m.validate(context, logger);
        assertFalse(ok, "Validation must detect out‑of‑bounds (infeasible) solution and return false.");
    }

    @Test
    void validate_withNullLogger() {
        NumberContext ctx = NumberContext.of(1E-9, 10);
        // Same model as the “valid solution” case but with a null logger.
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.newVariable("x").lower(5).upper(5).weight(0);
        // The method must handle a null logger without throwing.
        assertDoesNotThrow(() -> {
            boolean ok = m.validate(ctx, null);
            assertTrue(ok, "Validation should succeed even when logger is null.");
        });
    }

    @Test
    void validate_withStrictContext_ToleranceFailure() {
        // We inject a solution that is 5.000000000001 while the model expects x = 5.
        NumberContext strictCtx = NumberContext.of(1E-12, 10);
        BasicLogger logger = new BasicLogger();
        // Build a model with an equality constraint x = 5.
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable x = m.newVariable("x").lower(0).upper(10).weight(1);
        m.newExpression("eq").set(x, 1).level(5);
        // Spy the model and force it to return a custom result.
        ExpressionsBasedModel spy = spy(m);
        Optimisation.Result badResult = mockResultWithValue(5.000000000001);
        // The spy will return our handcrafted result regardless of the context.
        doReturn(badResult).when(spy).getVariableValues(any(NumberContext.class));
        boolean ok = spy.validate(strictCtx, logger);
        assertFalse(ok, "With a strict tolerance the slight deviation should cause validation to fail.");
    }

    @Test
    void validate_withNullContext() {
        BasicLogger logger = new BasicLogger();
        // Any model works – we use the one built in @BeforeEach.
        assertThrows(NullPointerException.class, () -> model.validate(null, logger), "Passing a null NumberContext should throw NullPointerException.");
    }

    @Test
    void validate_withNoVariables() {
        // Empty model – no variables, no constraints.
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        NumberContext ctx = NumberContext.of(1E-9, 10);
        BasicLogger logger = new BasicLogger();
        boolean ok = empty.validate(ctx, logger);
        assertTrue(ok, "Validation of an empty model should trivially succeed.");
    }
}
