package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.array.Array1D;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_5_Test {

    private ExpressionsBasedModel model;

    @Mock
    private BasicLogger // a mocked logger – its behaviour is irrelevant for the test
    logger;

    @Mock
    private BasicLogger // normal logger – behaviour not important
    validLogger;

    @Mock
    private BasicLogger // logger used to verify that messages are written
    capturingLogger;

    @Mock
    private BasicLogger // logger that throws when used
    exceptionThrowingLogger;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of(null, null), Arguments.of(null, mock(BasicLogger.class)), Arguments.of(mock(Access1D.class), null));
    }

    /**
     * The model is a simple linear programme with a single variable x.
     * <pre>
     *   minimise   x
     *   subject to 5 ≤ x ≤ 10
     * </pre>
     * The optimiser returns x = 5, which satisfies the bounds, therefore
     * {@code validate(...)} must return {@code true}.
     */
    @Test
    void testValidateFeasibleSolution() {
        // Build the model
        model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10)).weight(// minimise x
        BigDecimal.ONE);
        // Solve – the returned result implements Access1D<BigDecimal>
        Optimisation.Result result = model.minimise();
        // The solution should be feasible
        boolean ok = model.validate(result, logger);
        assertTrue(ok, "The feasible solution should be validated as true");
    }

    /**
     * The same model as above, but we supply an explicit solution that violates the
     * lower bound (x = 0). Validation must therefore return {@code false}.
     */
    @Test
    void testValidateInfeasibleSolution() {
        // Build the model
        model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10)).weight(// minimise x
        BigDecimal.ONE);
        // Create a solution that does NOT respect the lower bound (x = 0)
        Access1D<BigDecimal> infeasibleSolution = Array1D.of(BigDecimal.ZERO);
        // Validation must fail
        boolean ok = model.validate(infeasibleSolution, logger);
        assertFalse(ok, "A solution violating the variable bounds should be validated as false");
    }

    /* -------------------------------------------------
       1. Null input handling
       ------------------------------------------------- */
    @ParameterizedTest(name = "{index}: solution={0}, logger={1}")
    @MethodSource("nullInputProvider")
    void testValidateNullInputs(Access1D<BigDecimal> solution, BasicLogger logger) {
        assertThrows(IllegalArgumentException.class, () -> model.validate(solution, logger), "Both arguments null or any one null must cause IllegalArgumentException");
    }

    /* -------------------------------------------------
       2. Solution size mismatch (shorter & longer)
       ------------------------------------------------- */
    @Test
    void testValidateSolutionSizeMismatchShorter() {
        // model with two variables
        model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // solution vector length = 1 (shorter)
        Access1D<BigDecimal> shortSolution = Array1D.of(BigDecimal.ONE);
        boolean ok = model.validate(shortSolution, validLogger);
        assertFalse(ok, "Solution shorter than variable count must be invalid");
    }

    @Test
    void testValidateSolutionSizeMismatchLonger() {
        // model with one variable
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // solution vector length = 3 (longer)
        Access1D<BigDecimal> longSolution = Array1D.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        boolean ok = model.validate(longSolution, validLogger);
        assertFalse(ok, "Solution longer than variable count must be invalid");
    }

    /* -------------------------------------------------
       4. Infeasible – bound violation
       ------------------------------------------------- */
    @Test
    void testValidateBoundViolation() {
        model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        // x = 0 violates lower bound
        Access1D<BigDecimal> infeasible = Array1D.of(BigDecimal.ZERO);
        assertFalse(model.validate(infeasible, validLogger), "Bound violation must be reported as false");
    }

    /* -------------------------------------------------
       5. Infeasible – constraint violation
       ------------------------------------------------- */
    @Test
    void testValidateConstraintViolation() {
        // two variables
        model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // constraint: a + b >= 15 (which is impossible given the bounds)
        Expression expr = model.addExpression("c1").setLinearFactor(model.getVariable(0), BigDecimal.ONE).setLinearFactor(model.getVariable(1), BigDecimal.ONE).lower(BigDecimal.valueOf(15));
        // a feasible solution respecting bounds but violating the constraint
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(5), BigDecimal.valueOf(5));
        assertFalse(model.validate(solution, validLogger), "Constraint violation must be reported as false");
    }

    /* -------------------------------------------------
       6. Logger output verification on failure
       ------------------------------------------------- */
    @Test
    void testLoggerReceivesMessagesOnFailure() {
        // simple model with one variable
        model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10));
        // infeasible solution
        Access1D<BigDecimal> infeasible = Array1D.of(BigDecimal.ZERO);
        // configure capturing logger to do nothing (just verify interaction)
        when(capturingLogger.append(anyString())).thenReturn(capturingLogger);
        boolean ok = model.validate(infeasible, capturingLogger);
        assertFalse(ok, "Validation should fail");
        // verify that the logger was used at least once
        verify(capturingLogger, atLeastOnce()).append(anyString());
    }

    /* -------------------------------------------------
       7. Boundary tolerance tests
       ------------------------------------------------- */
    @Test
    void testSolutionExactlyOnLowerBound() {
        model.newVariable("x").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(8));
        Access1D<BigDecimal> onLower = Array1D.of(BigDecimal.valueOf(3));
        assertTrue(model.validate(onLower, validLogger), "Exact lower bound must be accepted");
    }

    @Test
    void testSolutionExactlyOnUpperBound() {
        model.newVariable("x").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(8));
        Access1D<BigDecimal> onUpper = Array1D.of(BigDecimal.valueOf(8));
        assertTrue(model.validate(onUpper, validLogger), "Exact upper bound must be accepted");
    }

    @Test
    void testSolutionJustInsideFeasibilityTolerance() {
        // tighten feasibility tolerance to 1e-6
        model.options.feasibility = NumberContext.of(6);
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // lower bound is 0, solution = -5e-7 (inside tolerance)
        Access1D<BigDecimal> justInside = Array1D.of(new BigDecimal("-0.0000005"));
        assertTrue(model.validate(justInside, validLogger), "Violation smaller than tolerance should be accepted");
    }

    @Test
    void testSolutionJustOutsideFeasibilityTolerance() {
        // tolerance 1e-6
        model.options.feasibility = NumberContext.of(6);
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // lower bound is 0, solution = -2e-5 (outside tolerance)
        Access1D<BigDecimal> justOutside = Array1D.of(new BigDecimal("-0.00002"));
        assertFalse(model.validate(justOutside, validLogger), "Violation larger than tolerance must be rejected");
    }

    /* -------------------------------------------------
       8. Integer variable violation
       ------------------------------------------------- */
    @Test
    void testIntegerVariableNonIntegerValue() {
        // integer variable x ∈ [0,10]
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(// mark as integer
        true);
        // solution with non‑integer value 3.7
        Access1D<BigDecimal> nonInt = Array1D.of(new BigDecimal("3.7"));
        assertFalse(model.validate(nonInt, validLogger), "Non‑integer value for integer variable must be invalid");
    }

    /* -------------------------------------------------
       9. Empty model (no variables, no constraints)
       ------------------------------------------------- */
    @Test
    void testValidateEmptyModel() {
        // zero‑length vector
        Access1D<BigDecimal> emptySolution = Array1D.of();
        assertTrue(model.validate(emptySolution, validLogger), "Empty model with empty solution must be valid");
    }

    /* -------------------------------------------------
      10. Solution containing NaN values
      ------------------------------------------------- */
    @Test
    void testValidateSolutionWithNaN() {
        // custom Access1D that returns a BigDecimal whose doubleValue() is NaN
        Access1D<BigDecimal> nanSolution = new Access1D<BigDecimal>() {

            @Override
            public long count() {
                return 1;
            }

            @Override
            public BigDecimal get(long index) {
                // BigDecimal cannot represent NaN – we cheat by returning a value that
                // yields NaN when converted to double via doubleValue().
                return new BigDecimal(Double.NaN);
            }
        };
        assertFalse(model.validate(nanSolution, validLogger), "Solution containing NaN must be invalid");
    }

    /* -------------------------------------------------
      11. Solution containing Infinite values
      ------------------------------------------------- */
    @Test
    void testValidateSolutionWithInfinity() {
        Access1D<BigDecimal> infSolution = new Access1D<BigDecimal>() {

            @Override
            public long count() {
                return 1;
            }

            @Override
            public BigDecimal get(long index) {
                return new BigDecimal(Double.POSITIVE_INFINITY);
            }
        };
        assertFalse(model.validate(infSolution, validLogger), "Solution containing Infinity must be invalid");
    }

    /* -------------------------------------------------
      12. Validation failure handler invocation
      ------------------------------------------------- */
    @Test
    void testValidationFailureHandlerIsInvoked() throws Exception {
        // simple model with a bound violation
        model.newVariable("x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10));
        Access1D<BigDecimal> infeasible = Array1D.of(BigDecimal.ZERO);
        // create a mock handler
        @SuppressWarnings("unchecked")
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = mock(BiConsumer.class);
        // inject the handler via reflection (private field)
        Field f = ExpressionsBasedModel.class.getDeclaredField("validationFailureHandler");
        f.setAccessible(true);
        f.set(model, handler);
        boolean ok = model.validate(infeasible, validLogger);
        assertFalse(ok, "Validation must fail");
        verify(handler, times(1)).accept(eq(model), eq(infeasible));
    }

    /* -------------------------------------------------
      13. Model already marked infeasible
      ------------------------------------------------- */
    @Test
    void testValidateWhenModelAlreadyInfeasible() {
        // package‑private method, accessible because test is in same package
        model.setInfeasible();
        // any solution – even a correct one – must be rejected
        Access1D<BigDecimal> anySolution = Array1D.of(BigDecimal.ZERO);
        assertFalse(model.validate(anySolution, validLogger), "Model flagged infeasible must reject any solution");
    }

    /* -------------------------------------------------
      14. Null element inside solution vector
      ------------------------------------------------- */
    @Test
    void testValidateSolutionWithNullElement() {
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Access1D<BigDecimal> nullElementSolution = new Access1D<BigDecimal>() {

            @Override
            public long count() {
                return 1;
            }

            @Override
            public BigDecimal get(long index) {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> model.validate(nullElementSolution, validLogger), "Null element inside solution must cause NullPointerException");
    }

    /* -------------------------------------------------
      15. Solution accessor throws runtime exception
      ------------------------------------------------- */
    @Test
    void testValidateSolutionAccessorThrows() {
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Access1D<BigDecimal> throwingSolution = new Access1D<BigDecimal>() {

            @Override
            public long count() {
                return 1;
            }

            @Override
            public BigDecimal get(long index) {
                throw new RuntimeException("access failure");
            }
        };
        assertThrows(RuntimeException.class, () -> model.validate(throwingSolution, validLogger), "Exception from solution accessor must propagate");
    }

    /* -------------------------------------------------
      16. Logger throws exception during validation
      ------------------------------------------------- */
    @Test
    void testValidateLoggerThrowsException() {
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // configure the logger mock to throw when any method is called
        doThrow(new RuntimeException("logger failure")).when(exceptionThrowingLogger).append(anyString());
        Access1D<BigDecimal> feasible = Array1D.of(BigDecimal.ONE);
        assertThrows(RuntimeException.class, () -> model.validate(feasible, exceptionThrowingLogger), "Exception from logger must propagate");
    }

    /* -------------------------------------------------
      17. Model disposed before validation
      ------------------------------------------------- */
    @Test
    void testValidateAfterDispose() {
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // after this the model is unusable
        model.dispose();
        Access1D<BigDecimal> anySolution = Array1D.of(BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> model.validate(anySolution, validLogger), "Calling validate on a disposed model must throw IllegalStateException");
    }
}
