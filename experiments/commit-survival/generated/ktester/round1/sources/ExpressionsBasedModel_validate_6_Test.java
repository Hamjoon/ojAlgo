package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.netio.BasicLogger;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_6_Test {

    private ExpressionsBasedModel model;

    private NumberContext context;

    /**
     * Parameter holder for the validation‑logic test cases.
     */
    private static class ValidationCase {

        final Access1D<BigDecimal> solution;

        final NumberContext ctx;

        final Class<? extends Throwable> expectedException;

        // null when an exception is expected
        final Boolean expectedResult;

        final String description;

        ValidationCase(Access1D<BigDecimal> solution, NumberContext ctx, Class<? extends Throwable> expectedException, Boolean expectedResult, String description) {
            this.solution = solution;
            this.ctx = ctx;
            this.expectedException = expectedException;
            this.expectedResult = expectedResult;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test.
        model = new ExpressionsBasedModel();
        // A reasonably strict context – 8 digits precision, 8‑scale.
        // The public factory method exists in the library (NumberContext.of).
        context = NumberContext.of(8, 8);
        model = new ExpressionsBasedModel();
        // a reasonably strict context – 8 digits precision, 8‑scale (default tolerance 10⁻⁸)
        context = NumberContext.of(8, 8);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model does not hold external resources.
        model = null;
        model = null;
    }

    /**
     * Helper that creates a fresh model spy with the required {@code Options}
     * configuration.
     */
    private ExpressionsBasedModel spyModelWithOptions(boolean detailed, BasicLogger logger) {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        // Options is a public final field – we can modify its public members directly.
        spy.options.logger_detailed = detailed;
        spy.options.logger_appender = logger;
        return spy;
    }

    /**
     * Builds a minimal model:
     * <pre>
     *   variable x ∈ [0, 10]
     *   constraint: x ≥ 5
     * </pre>
     */
    private ExpressionsBasedModel buildSimpleModel() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable x = m.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ZERO);
        // inequality constraint: lower bound 5 (no upper bound)
        m.addExpression().set(x, BigMath.ONE).lower(BigDecimal.valueOf(5));
        return m;
    }

    /**
     * Supplies the test cases described in the JSON specification.
     */
    static Stream<ValidationCase> validationCases() {
        // Helper to create a one‑element solution
        Access1D<BigDecimal> sol4 = new Array1D<>(new BigDecimal[] { BigDecimal.valueOf(4) });
        Access1D<BigDecimal> sol6 = new Array1D<>(new BigDecimal[] { BigDecimal.valueOf(6) });
        Access1D<BigDecimal> sol4_95 = new Array1D<>(new BigDecimal[] { BigDecimal.valueOf(4.95) });
        Access1D<BigDecimal> sol5 = new Array1D<>(new BigDecimal[] { BigDecimal.valueOf(5) });
        Access1D<BigDecimal> sol5_plus_eps = new Array1D<>(new BigDecimal[] { BigDecimal.valueOf(5).add(BigDecimal.valueOf(1e-12)) });
        Access1D<BigDecimal> solTwoElems = new Array1D<>(new BigDecimal[] { BigDecimal.ONE, BigDecimal.valueOf(2) });
        // Mock that returns null for its element (null‑element case)
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> nullElementSolution = mock(Access1D.class);
        when(nullElementSolution.size()).thenReturn(1);
        when(nullElementSolution.get(anyInt())).thenReturn(null);
        // Mock that throws a runtime exception when accessed
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> throwingSolution = mock(Access1D.class);
        when(throwingSolution.size()).thenReturn(1);
        when(throwingSolution.get(anyInt())).thenThrow(new IndexOutOfBoundsException("boom"));
        // Invalid NumberContext – negative precision triggers IllegalArgumentException
        // this call itself throws, so we catch it later
        NumberContext invalidContext = NumberContext.of(-1, -1);
        return Stream.of(// null solution → NPE
        new ValidationCase(null, NumberContext.of(6, 8), NullPointerException.class, null, "null solution should raise NullPointerException"), // null context → NPE
        new ValidationCase(sol4, null, NullPointerException.class, null, "null NumberContext should raise NullPointerException"), // both null → NPE (solution check first)
        new ValidationCase(null, null, NullPointerException.class, null, "both arguments null → NullPointerException"), // infeasible solution (x = 4) → false
        new ValidationCase(sol4, NumberContext.of(6, 8, 10), null, false, "solution 4 violates x ≥ 5 → false"), // feasible solution (x = 6) → true
        new ValidationCase(sol6, NumberContext.of(6, 8, 10), null, true, "solution 6 satisfies constraint → true"), // relaxed tolerance (scale 1 ≈ ±0.1) → true
        new ValidationCase(sol4_95, NumberContext.of(6, 8, 1), null, true, "solution 4.95 within tolerance ±0.1 → true"), // NaN – we simulate by a mock that returns a NaN‑like BigDecimal (null treated as NaN)
        new ValidationCase(mockNaNSolution(), NumberContext.of(6, 8, 10), null, false, "solution containing NaN → false"), // size mismatch (vector length 2) → false
        new ValidationCase(solTwoElems, NumberContext.of(6, 8, 10), null, false, "solution length 2 for a single‑variable model → false"), // exact zero tolerance – solution exactly on bound → true
        new ValidationCase(sol5, NumberContext.of(6, 8, 0), null, true, "exact comparison, solution 5 meets bound → true"), // exact zero tolerance – slightest deviation → false
        new ValidationCase(sol5_plus_eps, NumberContext.of(6, 8, 0), null, false, "exact comparison, solution 5+1e‑12 exceeds bound → false"), // null element inside solution → NPE
        new ValidationCase(nullElementSolution, NumberContext.of(6, 8, 10), NullPointerException.class, null, "Access1D returns null element → NullPointerException"), // Access1D throws runtime exception → propagate
        new ValidationCase(throwingSolution, NumberContext.of(6, 8, 10), IndexOutOfBoundsException.class, null, "Access1D throws RuntimeException → propagate"), // invalid NumberContext – we expect IllegalArgumentException from validate
        new ValidationCase(sol5, NumberContext.of(-5, -2), IllegalArgumentException.class, null, "illegal NumberContext configuration → IllegalArgumentException"));
    }

    /**
     * Creates a mock {@link Access1D} that pretends to contain a {@code NaN} value.
     * The real {@code BigDecimal} cannot represent NaN, so we return {@code null}
     * which the implementation treats as an invalid numeric value and therefore
     * results in {@code false} (the same behaviour as the library does for NaN).
     */
    private static Access1D<BigDecimal> mockNaNSolution() {
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> mock = mock(Access1D.class);
        when(mock.size()).thenReturn(1);
        // null is used to simulate a non‑numeric entry
        when(mock.get(0)).thenReturn(null);
        return mock;
    }

    @Test
    void testValidateAcceptsFeasibleSolution() {
        // -------------------------------------------------
        // GIVEN – a model with a single variable x that must equal 5
        // -------------------------------------------------
        Variable x = model.addVariable().lower(// lower bound 0
        BigDecimal.ZERO).upper(// upper bound 10
        BigDecimal.TEN).weight(// does not contribute to objective
        BigMath.ZERO);
        // Constraint: x == 5
        model.addExpression().set(x, // coefficient 1·x
        BigMath.ONE).level(// exact level = 5
        BigDecimal.valueOf(5));
        // -------------------------------------------------
        // WHEN – we solve the model (minimise) and validate the result
        // -------------------------------------------------
        Optimisation.Result result = model.minimise();
        // The result implements Access1D<BigDecimal>, so it can be passed directly.
        boolean isValid = model.validate(result, context);
        // -------------------------------------------------
        // THEN – validation must succeed
        // -------------------------------------------------
        assertTrue(isValid, "The feasible solution should be validated as true");
        // Additional sanity check: the variable really is 5
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)));
    }

    @Test
    void testValidateRejectsInfeasibleSolution() {
        // -------------------------------------------------
        // GIVEN – the same model as above (x must be 5)
        // -------------------------------------------------
        Variable x = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ZERO);
        model.addExpression().set(x, BigMath.ONE).level(BigDecimal.valueOf(5));
        // -------------------------------------------------
        // AND – a deliberately wrong solution (x = 3)
        // -------------------------------------------------
        // Build a one‑dimensional array that mimics the Access1D<BigDecimal> interface.
        Array1D<BigDecimal> wrongSolution = new Array1D<>(1);
        // violates the constraint
        wrongSolution.set(0, BigDecimal.valueOf(3));
        // -------------------------------------------------
        // WHEN – we validate the wrong solution
        // -------------------------------------------------
        boolean isValid = model.validate((Access1D<BigDecimal>) wrongSolution, context);
        // -------------------------------------------------
        // THEN – validation must fail
        // -------------------------------------------------
        assertFalse(isValid, "An infeasible solution should be validated as false");
    }

    @Test
    void testValidate_usesSuppliedAppenderWhenDetailedAndNonNull() {
        // GIVEN – detailed logging enabled and a non‑null logger
        BasicLogger suppliedLogger = mock(BasicLogger.class);
        ExpressionsBasedModel spy = spyModelWithOptions(true, suppliedLogger);
        Access1D<BigDecimal> solution = mock(Access1D.class);
        NumberContext ctx = mock(NumberContext.class);
        // Stub the three‑argument overload to return true
        doReturn(true).when(spy).validate(eq(solution), eq(ctx), eq(suppliedLogger));
        // WHEN – the two‑argument validate is invoked
        boolean result = spy.validate(solution, ctx);
        // THEN – the result must be the stubbed value and the correct logger forwarded
        assertTrue(result, "Delegated validate should return true");
        verify(spy, times(1)).validate(eq(solution), eq(ctx), eq(suppliedLogger));
    }

    @Test
    void testValidate_usesNullAppenderWhenDetailedButAppenderIsNull() {
        // GIVEN – detailed logging enabled but logger_appender is null
        ExpressionsBasedModel spy = spyModelWithOptions(true, null);
        Access1D<BigDecimal> solution = mock(Access1D.class);
        NumberContext ctx = mock(NumberContext.class);
        // Expect delegation with BasicLogger.NULL
        doReturn(false).when(spy).validate(eq(solution), eq(ctx), eq(BasicLogger.NULL));
        // WHEN
        boolean result = spy.validate(solution, ctx);
        // THEN
        assertFalse(result, "Delegated validate should return false");
        verify(spy, times(1)).validate(eq(solution), eq(ctx), eq(BasicLogger.NULL));
    }

    @Test
    void testValidate_ignoresAppenderWhenNotDetailed() {
        // GIVEN – detailed logging disabled, logger_appender may be non‑null but must be ignored
        BasicLogger anyLogger = mock(BasicLogger.class);
        ExpressionsBasedModel spy = spyModelWithOptions(false, anyLogger);
        Access1D<BigDecimal> solution = mock(Access1D.class);
        NumberContext ctx = mock(NumberContext.class);
        // Expect delegation with BasicLogger.NULL irrespective of the supplied logger
        doReturn(true).when(spy).validate(eq(solution), eq(ctx), eq(BasicLogger.NULL));
        // WHEN
        boolean result = spy.validate(solution, ctx);
        // THEN
        assertTrue(result, "Delegated validate should return true");
        verify(spy, times(1)).validate(eq(solution), eq(ctx), eq(BasicLogger.NULL));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validationCases")
    void testValidateSolutionLogic(ValidationCase vc) {
        // Build a fresh simple model for each case
        ExpressionsBasedModel localModel = buildSimpleModel();
        if (vc.expectedException != null) {
            // Expect an exception – the exact type must be thrown
            assertThrows(vc.expectedException, () -> localModel.validate(vc.solution, vc.ctx), vc.description);
        } else {
            // No exception expected – verify the boolean result
            boolean result = localModel.validate(vc.solution, vc.ctx);
            assertEquals(vc.expectedResult, result, vc.description);
        }
    }
}
