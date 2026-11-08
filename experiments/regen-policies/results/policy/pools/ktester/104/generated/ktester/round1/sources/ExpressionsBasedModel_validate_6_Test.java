package org.ojalgo.optimisation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Access1D;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import org.ojalgo.array.Array1D;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_6_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private NumberContext ctx;

    @BeforeEach
    void setupBeforeEach() {
        // -------------------------------------------------
        // Build a minimal model:  x ∈ [0,10] ,  x = 5
        // -------------------------------------------------
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Equality constraint: x = 5  (lower = upper = 5)
        model.newExpression("eq").set(x, // coefficient 1·x
        BigDecimal.ONE).lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // A NumberContext with a reasonable precision/scale.
        // The constructor is package‑private, which is accessible here.
        ctx = new NumberContext(new DecimalFormat(), new MathContext(10), 10);
    }

    private static Stream<Arguments> loggerSelectionProvider() {
        Access1D<BigDecimal> sol1 = Array1D.of(BigDecimal.ONE, BigDecimal.TEN);
        Access1D<BigDecimal> sol2 = Array1D.of(BigDecimal.ZERO);
        Access1D<BigDecimal> sol3 = Array1D.of(new BigDecimal("3.14"));
        Access1D<BigDecimal> sol4 = Array1D.of(BigDecimal.valueOf(-1));
        Access1D<BigDecimal> empty = Array1D.of();
        BasicLogger appender1 = Mockito.mock(BasicLogger.class);
        BasicLogger appender2 = Mockito.mock(BasicLogger.class);
        BasicLogger appender3 = Mockito.mock(BasicLogger.class);
        BasicLogger appender4 = Mockito.mock(BasicLogger.class);
        return Stream.of(// true / true -> user appender
        Arguments.of("Both flags true, non‑null appender", sol1, NumberContext.of(10, 10), true, appender1, appender1, null), // true / null -> fallback NULL
        Arguments.of("logger_detailed true, appender null", sol2, NumberContext.of(5, 5), true, null, BasicLogger.NULL, null), // false / non‑null -> fallback NULL
        Arguments.of("logger_detailed false, appender non‑null", sol3, NumberContext.of(8, 8), false, appender2, BasicLogger.NULL, null), // false / null -> fallback NULL
        Arguments.of("Both flags false", sol4, NumberContext.of(2, 2), false, null, BasicLogger.NULL, null), // null solution -> NPE before logger selection
        Arguments.of("Null solution argument", null, NumberContext.of(10, 10), true, appender3, null, NullPointerException.class), // null context -> NPE before logger selection
        Arguments.of("Null context argument", sol1, null, true, appender4, null, NullPointerException.class));
    }

    private static Stream<Arguments> equalityToleranceProvider() {
        return Stream.of(Arguments.of("Within tolerance (0.0004)", new BigDecimal("1.0004"), true), Arguments.of("Outside tolerance (0.001)", new BigDecimal("1.001"), false));
    }

    /**
     * Validation should succeed when the supplied solution matches the model.
     */
    @Test
    void testValidateWithCorrectSolution() {
        Access1D<BigDecimal> correctSolution = new Access1D<>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public BigDecimal get(int index) {
                return BigDecimal.valueOf(5);
            }
        };
        boolean ok = model.validate(correctSolution, ctx);
        assertTrue(ok, "The model should validate a correct solution vector.");
    }

    /**
     * Validation should fail when the supplied solution does not satisfy the model.
     */
    @Test
    void testValidateWithIncorrectSolution() {
        Access1D<BigDecimal> wrongSolution = new Access1D<>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public BigDecimal get(int index) {
                // violates the equality constraint
                return BigDecimal.valueOf(4);
            }
        };
        boolean ok = model.validate(wrongSolution, ctx);
        assertFalse(ok, "The model should not validate an incorrect solution vector.");
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("loggerSelectionProvider")
    void testValidateLoggerSelection(String description, Access1D<BigDecimal> solution, NumberContext context, boolean loggerDetailed, BasicLogger loggerAppender, BasicLogger expectedAppender, Class<? extends Throwable> expectedException) {
        // Build an empty model (no variables, no constraints) – any solution is valid.
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        // Configure the options as required for the test case.
        emptyModel.options.logger_detailed = loggerDetailed;
        emptyModel.options.logger_appender = loggerAppender;
        // Spy the model so we can verify the three‑argument overload call.
        ExpressionsBasedModel spy = Mockito.spy(emptyModel);
        if (expectedException != null) {
            // Expect an exception before the three‑argument method is invoked.
            assertThrows(expectedException, () -> spy.validate(solution, context));
            // No interaction with the three‑argument overload should have happened.
            Mockito.verify(spy, Mockito.never()).validate(Mockito.any(), Mockito.any(), Mockito.any());
        } else {
            // Normal execution – the three‑argument overload must be called with the
            // expected appender (either the user‑provided one or BasicLogger.NULL).
            boolean result = spy.validate(solution, context);
            assertTrue(result, "Validation should succeed for an empty model.");
            Mockito.verify(spy, Mockito.times(1)).validate(Mockito.eq(solution), Mockito.eq(context), Mockito.same(expectedAppender));
        }
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateSolutionSizeMismatch() {
        // Model with a single variable (already created in @BeforeEach)
        Access1D<BigDecimal> mismatched = Array1D.of(BigDecimal.ONE, BigDecimal.TEN);
        NumberContext context = NumberContext.of(10, 10);
        boolean ok = model.validate(mismatched, context);
        assertFalse(ok, "Validation must fail when solution size does not match variable count.");
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateVariableBoundViolation() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3));
        // No constraints, only bounds.
        Access1D<BigDecimal> sol = Array1D.of(BigDecimal.valueOf(5));
        NumberContext ctx = NumberContext.of(8, 8);
        assertFalse(m.validate(sol, ctx), "Solution exceeding variable upper bound must be invalid.");
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateIntegerVariableNonIntegerSolution() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable iv = m.newVariable("i").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Access1D<BigDecimal> sol = Array1D.of(new BigDecimal("2.7"));
        NumberContext ctx = NumberContext.of(6, 6);
        assertFalse(m.validate(sol, ctx), "Non‑integral value for an integer variable must be rejected.");
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("equalityToleranceProvider")
    void testValidateEqualityConstraintWithinTolerance(String description, BigDecimal solutionValue, boolean expectedResult) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable var = m.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Equality constraint x = 1.0
        m.newExpression("eq").set(var, BigDecimal.ONE).lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        Access1D<BigDecimal> sol = Array1D.of(solutionValue);
        // tolerance ≈ 10⁻⁴
        NumberContext ctx = NumberContext.of(4, 4);
        assertEquals(expectedResult, m.validate(sol, ctx), description);
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateEmptyModelWithEmptySolution() {
        // no variables, no constraints
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        Access1D<BigDecimal> emptySolution = Array1D.of();
        NumberContext ctx = NumberContext.of(5, 5);
        assertTrue(empty.validate(emptySolution, ctx), "An empty model should accept an empty solution vector.");
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateSolutionContainingNullElement() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // one variable, no bounds
        m.newVariable("v");
        Access1D<BigDecimal> sol = Array1D.of((BigDecimal) null);
        NumberContext ctx = NumberContext.of(6, 6);
        assertThrows(NullPointerException.class, () -> m.validate(sol, ctx), "A null element inside the solution vector should cause NPE.");
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateZeroCoefficientConstraint() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable var = m.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Constraint: 0·x ≤ 5   (always true)
        m.newExpression("zero").set(var, BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        Access1D<BigDecimal> sol = Array1D.of(BigDecimal.ONE);
        NumberContext ctx = NumberContext.of(8, 8);
        assertTrue(m.validate(sol, ctx), "Constraint with zero coefficient must not cause validation failure.");
    }

    // -------------------------------------------------------------------------
    @Test
    void testValidateSolutionShorterThanVariableCount() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.newVariable("a");
        m.newVariable("b");
        // only one entry
        Access1D<BigDecimal> sol = Array1D.of(BigDecimal.ONE);
        NumberContext ctx = NumberContext.of(5, 5);
        assertThrows(IndexOutOfBoundsException.class, () -> m.validate(sol, ctx), "Providing fewer entries than variables should raise IndexOutOfBoundsException.");
    }
}
