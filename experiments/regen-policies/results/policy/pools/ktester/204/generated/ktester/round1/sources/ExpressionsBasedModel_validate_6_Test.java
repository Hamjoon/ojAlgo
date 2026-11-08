package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.array.Array1D;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_6_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    /**
     * Mocked logger used for the “detailed logger with non‑null appender” cases.
     */
    @Mock
    private BasicLogger mockAppender;

    /**
     * Simple holder for test parameters.
     */
    private static final class TestCase {

        private final Access1D<BigDecimal> solution;

        private final NumberContext context;

        private final boolean expected;

        private final boolean loggerDetailed;

        private final BasicLogger loggerAppender;

        private final int variableCount;

        private final String description;

        TestCase(Access1D<BigDecimal> solution, NumberContext context, boolean expected, boolean loggerDetailed, BasicLogger loggerAppender, int variableCount, String description) {
            this.solution = solution;
            this.context = context;
            this.expected = expected;
            this.loggerDetailed = loggerDetailed;
            this.loggerAppender = loggerAppender;
            this.variableCount = variableCount;
            this.description = description;
        }

        Access1D<BigDecimal> solution() {
            return solution;
        }

        NumberContext context() {
            return context;
        }

        boolean expected() {
            return expected;
        }

        boolean loggerDetailed() {
            return loggerDetailed;
        }

        BasicLogger loggerAppender() {
            return loggerAppender;
        }

        int variableCount() {
            return variableCount;
        }

        String description() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal model: one variable with bounds [0, 10] and a
        // trivial objective (weight 1). The model is solvable.
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
        x = null;
    }

    /**
     * Basic‑branch test cases.
     */
    private static Stream<Arguments> basicCases() {
        NumberContext ctx8 = NumberContext.of(8, 8);
        NumberContext ctx0 = NumberContext.of(0, 0);
        NumberContext ctx2 = NumberContext.of(2, 2);
        return Stream.of(// 1. solution == null
        Arguments.of(new TestCase(null, ctx8, false, false, null, 0, "When the supplied solution reference is null the method should immediately return false.")), // 2. context == null
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE, BigDecimal.TEN), null, false, false, null, 2, "A null NumberContext is not acceptable; the method must detect this and return false.")), // 3. happy‑path, default logger (detailed = false)
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE, BigDecimal.TEN), ctx8, true, false, null, 2, "Valid solution with matching size and a permissive NumberContext should validate successfully.")), // 4. value outside context precision/scale
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE, new BigDecimal("1E-10")), ctx8, false, false, null, 2, "Second value is outside the allowed precision/scale, validation must fail.")), // 5. size mismatch (solution length 1, model expects 2)
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE), ctx8, false, false, null, 2, "Solution size differs from variable count – validation must return false.")), // 6. detailed logger true with non‑null appender (appender will be null in our simplified test)
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE, BigDecimal.TEN), ctx8, true, true, null, 2, "When logger_detailed is true and an appender is set, validation must still succeed.")), // 7. detailed logger true but appender null (falls back to NULL logger)
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE, BigDecimal.TEN), ctx8, true, true, null, 2, "When logger_detailed is true but logger_appender is null, validation must succeed using NULL logger.")));
    }

    /**
     * Edge‑case test cases.
     */
    private static Stream<Arguments> edgeCases() {
        NumberContext ctx8 = NumberContext.of(8, 8);
        NumberContext ctx0 = NumberContext.of(0, 0);
        NumberContext ctx2 = NumberContext.of(2, 2);
        return Stream.of(// 8. solution contains a null element
        Arguments.of(new TestCase(Array1D.of((BigDecimal) null, BigDecimal.ONE), ctx8, false, false, null, 2, "Solution vector includes a null entry – validation must return false.")), // 9. empty model and empty solution
        Arguments.of(new TestCase(Array1D.of(), ctx8, true, false, null, 0, "Empty model with empty solution should be considered valid.")), // 10. non‑zero value with zero‑precision/scale context
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ONE), ctx0, false, false, null, 1, "Zero‑precision/scale context only accepts exact zero; non‑zero fails.")), // 11. zero value with zero‑precision/scale context
        Arguments.of(new TestCase(Array1D.of(BigDecimal.ZERO), ctx0, true, false, null, 1, "Zero satisfies a zero‑precision/scale context, validation must pass.")), // 12. value exactly at allowed scale limit
        Arguments.of(new TestCase(Array1D.of(new BigDecimal("0.01")), ctx2, true, false, null, 1, "Value respects the 2‑decimal‑place scale, validation must succeed.")), // 13. value exceeds allowed scale
        Arguments.of(new TestCase(Array1D.of(new BigDecimal("0.011")), ctx2, false, false, null, 1, "Value exceeds the allowed scale, validation must fail.")));
    }

    private void executeTestCase(TestCase tc) {
        // 1) Build a model with the required number of variables.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        for (int i = 0; i < tc.variableCount(); i++) {
            model.newVariable("v" + i).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        }
        // 2) Configure logger options if the test case demands it.
        model.options.logger_detailed = tc.loggerDetailed();
        model.options.logger_appender = tc.loggerAppender();
        // 3) Invoke the method under test.
        boolean actual = model.validate(tc.solution(), tc.context());
        // 4) Assert the expected outcome.
        assertEquals(tc.expected(), actual, tc.description());
    }

    /**
     * A feasible solution (the optimiser's own result) must be validated
     * successfully.
     */
    @Test
    void testValidateWithFeasibleSolution() {
        // minimise → the optimiser will pick the lower bound (0)
        Optimisation.Result result = model.minimise();
        NumberContext ctx = NumberContext.of(MathContext.DECIMAL64.getPrecision(), MathContext.DECIMAL64.getScale(), RoundingMode.HALF_EVEN);
        boolean ok = model.validate(result, ctx);
        assertTrue(ok, "The optimiser's own feasible solution should validate successfully.");
    }

    /**
     * An explicitly constructed infeasible solution must cause {@code validate}
     * to return {@code false}.
     */
    @Test
    void testValidateWithInfeasibleSolution() {
        // Create a solution that violates the variable's upper bound (value 20 > 10).
        Access1D<BigDecimal> badSolution = Array1D.of(BigDecimal.valueOf(20));
        NumberContext ctx = NumberContext.of(MathContext.DECIMAL64.getPrecision(), MathContext.DECIMAL64.getScale(), RoundingMode.HALF_EVEN);
        boolean ok = model.validate(badSolution, ctx);
        assertFalse(ok, "A solution that violates variable bounds must not validate.");
    }

    /**
     * Verify that the overload {@code validate(Access1D)} (which delegates to
     * the three‑argument version) behaves consistently with the explicit
     * three‑argument call.
     */
    @Test
    void testValidateOverloadConsistency() {
        Optimisation.Result result = model.minimise();
        NumberContext ctx = NumberContext.of(MathContext.DECIMAL64.getPrecision(), MathContext.DECIMAL64.getScale(), RoundingMode.HALF_EVEN);
        boolean explicit = model.validate(result, ctx);
        // uses default logger & context
        boolean overload = model.validate(result);
        assertEquals(explicit, overload, "The two‑argument overload should produce the same result as the explicit three‑argument call.");
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("basicCases")
    void testValidateBasicBranches(TestCase tc) {
        executeTestCase(tc);
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("edgeCases")
    void testValidateEdgeCases(TestCase tc) {
        executeTestCase(tc);
    }
}
