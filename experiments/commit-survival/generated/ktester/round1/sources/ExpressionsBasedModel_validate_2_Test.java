package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_2_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private NumberContext feasibilityContext;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal model: 0 ≤ x ≤ 10, minimise x.
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// minimise x
        BigMath.ONE);
        // Ensure the model has a feasible solution.
        model.minimise();
        // Use the same feasibility context that the model will use internally.
        feasibilityContext = model.options.feasibility;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources.
        model.dispose();
    }

    private static Stream<Arguments> loggerBranchProvider() {
        // solution, logger_detailed, logger_appender (null = absent), expectedLoggerIsNull, expectedResult
        return Stream.of(// logger_detailed = true, appender present → supplied logger
        Arguments.of(Array1D.of(BigDecimal.ONE), true, mock(BasicLogger.class), false, true), // logger_detailed = false, appender present → NULL
        Arguments.of(Array1D.of(BigDecimal.ZERO), false, mock(BasicLogger.class), true, false), // logger_detailed = true, appender null → NULL (solution null, result true)
        Arguments.of(null, true, null, true, true), // logger_detailed = false, appender null → NULL (solution infeasible → false)
        Arguments.of(Array1D.of(new BigDecimal("-5")), false, null, true, false));
    }

    /**
     * The solution returned by the model should be considered valid.
     */
    @Test
    void testValidateWithFeasibleSolution() {
        // Solve the model – the result is guaranteed feasible by the previous minimise().
        Optimisation.Result result = model.minimise();
        // Extract the solution vector as an Access1D<BigDecimal>.
        Access1D<BigDecimal> solution = result.getValues();
        // The public validate method should return true.
        assertTrue(model.validate(solution), "The model should validate its own feasible solution.");
    }

    /**
     * Supplying a solution that violates the variable bounds must cause validation to fail.
     */
    @Test
    void testValidateWithInfeasibleSolution() {
        // Create a solution vector that puts x = 20 (outside the upper bound of 10).
        Array1D<BigDecimal> badSolution = new Array1D<>(1);
        badSolution.set(0, BigDecimal.valueOf(20));
        // Validation should be false because the solution is infeasible.
        assertFalse(model.validate(badSolution), "The model must reject a solution that violates variable bounds.");
    }

    /**
     * Validate also works when a custom logger is supplied via the options.
     * This test only checks that the overload does not throw and returns the same result.
     */
    @Test
    void testValidateWithCustomAppender() {
        // Solve to obtain a feasible solution.
        Optimisation.Result result = model.minimise();
        Access1D<BigDecimal> solution = result.getValues();
        // Use the detailed logger from the model options (may be {@code NULL}).
        BasicLogger appender = (model.options.logger_detailed && (model.options.logger_appender != null)) ? model.options.logger_appender : BasicLogger.NULL;
        // Directly invoke the three‑argument overload via reflection to avoid
        // accessing private members – we only need to ensure it behaves like the
        // public one.
        boolean viaPublic = model.validate(solution);
        boolean viaThreeArg = model.validate(solution, feasibilityContext, appender);
        assertEquals(viaPublic, viaThreeArg, "Both validate overloads should produce the same result.");
    }

    @ParameterizedTest
    @MethodSource("loggerBranchProvider")
    void testValidate_LoggerConfigurationBranches(Access1D<BigDecimal> solution, boolean loggerDetailed, BasicLogger appender, boolean expectNullLogger, boolean expectedReturn) throws Exception {
        // configure logger flags
        model.options.logger_detailed = loggerDetailed;
        // may be null
        model.options.logger_appender = appender;
        // spy the model to capture the three‑argument overload
        ExpressionsBasedModel spy = Mockito.spy(model);
        // stub the three‑arg method to return the expected boolean without executing real logic
        doReturn(expectedReturn).when(spy).validate(any(), any(), any());
        // invoke the one‑arg overload
        boolean actual = spy.validate(solution);
        // verify that the three‑arg overload was called with the correct logger
        BasicLogger expectedLogger = (loggerDetailed && appender != null) ? appender : BasicLogger.NULL;
        verify(spy, times(1)).validate(eq(solution), eq(model.options.feasibility), eq(expectedLogger));
        // and that the result propagates correctly
        assertEquals(expectedReturn, actual, "Public validate should return the value from the three‑arg overload.");
    }

    @Test
    void testValidate_StrictFeasibilityRejectsSmallNonZero() {
        // strict context: zero tolerance
        model.options.feasibility = NumberContext.ofExactScale(0);
        Access1D<BigDecimal> sol = Array1D.of(new BigDecimal("0.001"));
        // real validation should deem it infeasible
        assertFalse(model.validate(sol), "With a strict feasibility context, a small non‑zero value must be rejected.");
    }

    @Test
    void testValidate_LenientFeasibilityAcceptsSmallNonZero() {
        // lenient context: tolerance 1E‑2
        model.options.feasibility = NumberContext.of(1E-2);
        Access1D<BigDecimal> sol = Array1D.of(new BigDecimal("0.001"));
        assertTrue(model.validate(sol), "With a lenient feasibility context, a deviation of 0.001 should be accepted.");
    }

    @Test
    void testValidate_NullSolutionIsConsideredFeasibleEvenWithStrictContext() {
        model.options.feasibility = NumberContext.ofExactScale(0);
        assertTrue(model.validate(null), "A null solution must be treated as trivially feasible regardless of the context.");
    }

    @Test
    void testValidate_SolutionSizeMismatchFails() {
        // model with two variables
        ExpressionsBasedModel twoVarModel = new ExpressionsBasedModel();
        twoVarModel.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        twoVarModel.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // solution vector length 1
        Access1D<BigDecimal> shortSolution = Array1D.of(BigDecimal.ZERO);
        assertFalse(twoVarModel.validate(shortSolution), "A solution vector shorter than the number of variables must cause validation to fail.");
    }

    @Test
    void testValidate_EmptyModelAcceptsEmptySolution() {
        // no variables
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        // length 0
        Access1D<BigDecimal> emptySolution = Array1D.of();
        assertTrue(emptyModel.validate(emptySolution), "An empty model with an empty solution vector should be considered feasible.");
    }

    @Test
    void testValidate_SolutionWithNullEntryFails() {
        // model with two variables
        ExpressionsBasedModel twoVarModel = new ExpressionsBasedModel();
        twoVarModel.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        twoVarModel.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // solution contains a null for the second variable
        Array1D<BigDecimal> sol = new Array1D<>(2);
        sol.set(0, BigDecimal.ONE);
        sol.set(1, null);
        assertFalse(twoVarModel.validate(sol), "A solution containing a null entry for a defined variable must be rejected.");
    }

    @Test
    void testValidate_NullOptionsThrowsNPE() throws Exception {
        // use reflection to set the final field 'options' to null
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        Access1D<BigDecimal> sol = Array1D.of(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> model.validate(sol), "When 'options' is null, accessing options.feasibility must throw NPE.");
    }

    @Test
    void testValidate_NullFeasibilityThrowsNPE() throws Exception {
        // set feasibility to null via reflection (field is public, but we keep reflection for safety)
        Field feasibilityField = model.options.getClass().getDeclaredField("feasibility");
        feasibilityField.setAccessible(true);
        feasibilityField.set(model.options, null);
        Access1D<BigDecimal> sol = Array1D.of(BigDecimal.ZERO);
        assertThrows(NullPointerException.class, () -> model.validate(sol), "When 'options.feasibility' is null, the validate call must throw NPE.");
    }
}
