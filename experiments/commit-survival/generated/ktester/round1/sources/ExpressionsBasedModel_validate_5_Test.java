package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.array.Array1D;
import org.ojalgo.structure.Access1D;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_5_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    // second variable (when needed)
    private Variable y;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // A single variable with explicit lower/upper bounds.
        x = model.addVariable();
        x.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        model = new ExpressionsBasedModel();
        // By default create two variables with generous bounds; individual tests can tighten them.
        x = model.addVariable("x");
        y = model.addVariable("y");
        x.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
        y.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model – good practice for classes that hold native resources.
        model.dispose();
        model.dispose();
    }

    /**
     * Makes the second variable an integer variable.
     */
    private void makeYInteger() {
        y.integer(true);
    }

    /**
     * Adds a linear constraint  x + y ≤ limit.
     */
    private void addLinearConstraintLe(BigDecimal limit) {
        Expression expr = model.addExpression("c_le");
        expr.setLinearFactor(x, BigDecimal.ONE);
        expr.setLinearFactor(y, BigDecimal.ONE);
        expr.upper(limit);
    }

    /**
     * Adds two contradictory constraints: x ≤ low and x ≥ high.
     */
    private void addContradictoryConstraints(BigDecimal low, BigDecimal high) {
        Expression le = model.addExpression("c_le");
        le.setLinearFactor(x, BigDecimal.ONE);
        le.upper(low);
        Expression ge = model.addExpression("c_ge");
        ge.setLinearFactor(x, BigDecimal.ONE);
        ge.lower(high);
    }

    /**
     * Fixes variable x to the supplied value (by setting equal lower/upper).
     */
    private void fixX(BigDecimal fixedValue) {
        x.lower(fixedValue);
        x.upper(fixedValue);
    }

    /**
     * Adjusts the model's feasibility tolerance.
     */
    private void setFeasibilityTolerance(double eps) {
        model.options.feasibility = NumberContext.of(eps);
    }

    /**
     * A feasible solution (within the variable bounds) must be accepted by {@code validate}.
     */
    @Test
    void testValidateReturnsTrueForFeasibleSolution() {
        // Build a trivial optimisation problem that has a feasible optimum.
        // No objective is required – we only need a solution that respects the bounds.
        // triggers the internal solving machinery.
        model.minimise();
        // Retrieve the solution produced by the model.
        Optimisation.Result result = model.getVariableValues();
        // The result implements {@link Access1D<BigDecimal>}, so we can pass it directly.
        // logger is optional; null is allowed.
        boolean ok = model.validate(result, null);
        assertTrue(ok, "The model should validate its own feasible solution.");
    }

    /**
     * A solution that violates a variable bound must be rejected by {@code validate}.
     */
    @Test
    void testValidateReturnsFalseForInfeasibleSolution() {
        // Construct a solution that deliberately breaks the upper bound (5) of variable {@code x}.
        Array1D<BigDecimal> badSolution = new Array1D<>(1);
        // 10 > 5 → infeasible
        badSolution.set(0, BigDecimal.TEN);
        // The solution is an {@link Access1D<BigDecimal>}; we pass it to the validate method.
        boolean ok = model.validate((Access1D<BigDecimal>) badSolution, null);
        assertFalse(ok, "The model should reject a solution that violates variable bounds.");
    }

    @Test
    void testValidSolutionAllConstraintsSatisfied(@Mock BasicLogger mockLogger) {
        // x ∈ [0,5], y ∈ [0,5], both integer, and constraint x + y ≤ 5
        x.upper(BigDecimal.valueOf(5));
        y.upper(BigDecimal.valueOf(5));
        makeYInteger();
        addLinearConstraintLe(BigDecimal.valueOf(5));
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, new BigDecimal("2"));
        boolean ok = model.validate(solution, mockLogger);
        assertTrue(ok, "Feasible solution should be accepted.");
        verifyNoInteractions(mockLogger);
    }

    @Test
    void testInfeasibleSolutionVariableBoundViolation(@Mock BasicLogger mockLogger) {
        // lower bound of x is 0, supply -5
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("-5"), new BigDecimal("2"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution violating lower bound must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testInfeasibleSolutionIntegerVariableNonIntegral(@Mock BasicLogger mockLogger) {
        // y must be integer
        makeYInteger();
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("3"), new BigDecimal("2.7"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Non‑integral value for an integer variable must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testInfeasibleSolutionLinearConstraintViolation(@Mock BasicLogger mockLogger) {
        // constraint: x + y ≤ 15
        addLinearConstraintLe(BigDecimal.valueOf(15));
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("10"), new BigDecimal("10"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution violating a linear constraint must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testNullSolutionIsInfeasible(@Mock BasicLogger mockLogger) {
        boolean ok = model.validate((Access1D<BigDecimal>) null, mockLogger);
        assertFalse(ok, "Null solution must be treated as infeasible.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testNullLoggerWorksForFeasibleSolution(@Mock BasicLogger mockLogger) {
        // simple feasible solution, logger is null
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, new BigDecimal("2"));
        boolean ok = model.validate(solution, null);
        assertTrue(ok, "Feasible solution must be accepted even when logger is null.");
        // no logger to verify – just ensure no NPE occurs
    }

    @Test
    void testSolutionSizeMismatchFewerValues(@Mock BasicLogger mockLogger) {
        // only one value for two variables
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE);
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution with fewer entries than variables must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testSolutionSizeMismatchMoreValues(@Mock BasicLogger mockLogger) {
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, new BigDecimal("2"), new BigDecimal("3"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution with more entries than variables must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testSolutionContainsNullEntry(@Mock BasicLogger mockLogger) {
        // Array1D permits null entries; they should cause infeasibility.
        Array1D<BigDecimal> bad = new Array1D<>(2);
        bad.set(0, null);
        bad.set(1, new BigDecimal("2"));
        boolean ok = model.validate(bad, mockLogger);
        assertFalse(ok, "Solution containing a null entry must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testUpperBoundViolation(@Mock BasicLogger mockLogger) {
        // y upper bound 10, give 12
        y.upper(BigDecimal.valueOf(10));
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("5"), new BigDecimal("12"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution exceeding an upper bound must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testFixedVariableMismatch(@Mock BasicLogger mockLogger) {
        // x fixed at 2
        fixX(BigDecimal.valueOf(2));
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("3"), new BigDecimal("7"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution that does not respect a fixed variable must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testEmptyModelAcceptsEmptySolution() {
        // Dispose current variables and start with a fresh empty model
        model.dispose();
        // no variables added
        model = new ExpressionsBasedModel();
        // empty vector
        Access1D<BigDecimal> empty = Array1D.of();
        boolean ok = model.validate(empty, null);
        assertTrue(ok, "Empty model with empty solution must be feasible.");
    }

    @Test
    void testEmptyModelRejectsNonEmptySolution(@Mock BasicLogger mockLogger) {
        model.dispose();
        // still no variables
        model = new ExpressionsBasedModel();
        Access1D<BigDecimal> nonEmpty = Array1D.of(BigDecimal.ONE);
        boolean ok = model.validate(nonEmpty, mockLogger);
        assertFalse(ok, "Empty model with a non‑empty solution must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testFeasibilityToleranceExactlyOnBoundary(@Mock BasicLogger mockLogger) {
        // lower bound of x is 0, tolerance 1e‑6, solution = 1e‑6
        setFeasibilityTolerance(1e-6);
        x.lower(BigDecimal.ZERO);
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("0.000001"), new BigDecimal("2"));
        boolean ok = model.validate(solution, mockLogger);
        assertTrue(ok, "Solution exactly on tolerance boundary must be accepted.");
        verifyNoInteractions(mockLogger);
    }

    @Test
    void testFeasibilityToleranceJustOutsideBoundary(@Mock BasicLogger mockLogger) {
        setFeasibilityTolerance(1e-6);
        x.lower(BigDecimal.ZERO);
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("0.0000011"), new BigDecimal("2"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Solution just outside tolerance must be rejected.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testContradictoryConstraintsAlwaysInfeasible(@Mock BasicLogger mockLogger) {
        // x ≤ 1 and x ≥ 3 are contradictory
        addContradictoryConstraints(BigDecimal.ONE, new BigDecimal("3"));
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("2"), new BigDecimal("2"));
        boolean ok = model.validate(solution, mockLogger);
        assertFalse(ok, "Model with contradictory constraints must be infeasible.");
        verify(mockLogger, atLeastOnce()).append(anyString());
    }

    @Test
    void testIntegerVariableWithDecimalIntegralValue(@Mock BasicLogger mockLogger) {
        // y integer
        makeYInteger();
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("3"), new BigDecimal("4.0"));
        boolean ok = model.validate(solution, mockLogger);
        assertTrue(ok, "Integral decimal value for an integer variable must be accepted.");
        verifyNoInteractions(mockLogger);
    }
}
