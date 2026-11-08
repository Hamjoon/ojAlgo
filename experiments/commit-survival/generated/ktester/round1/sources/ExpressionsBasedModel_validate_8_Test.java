package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.array.Array1D;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_8_Test {

    private static final NumberContext CONTEXT = new NumberContext(new DecimalFormat(), new MathContext(20), 10);

    private static final BasicLogger LOGGER = BasicLogger.NULL;

    /**
     * When the supplied solution does not contain the same number of entries as there are
     * variables in the model, validation must fail.
     */
    @Test
    void testValidateSizeMismatch() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Two variables in the model
        model.addVariable();
        model.addVariable();
        // Solution only contains one value
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE);
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Validation should fail when solution size differs from variable count");
    }

    /**
     * Validation must fail if any variable does not satisfy its bounds.
     */
    @Test
    void testValidateVariableViolation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with a lower bound of 5
        Variable v = model.addVariable().lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(10));
        // Provide a solution that violates the lower bound (value = 3)
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(3));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Validation should fail when a variable violates its bounds");
    }

    /**
     * Validation must fail if any expression evaluates to a value that does not satisfy its
     * own constraints.
     */
    @Test
    void testValidateExpressionViolation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // One variable without bounds
        Variable v = model.addVariable();
        // Expression: 2 * v must equal 10 (i.e. lower = upper = 10)
        Expression expr = model.addExpression().lower(BigDecimal.TEN).upper(BigDecimal.TEN);
        expr.set(v, BigDecimal.valueOf(2));
        // Solution provides v = 3 → expression value = 6, which violates the expression bounds
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(3));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Validation should fail when an expression violates its bounds");
    }

    /**
     * A completely feasible solution should make {@code validate} return {@code true}.
     */
    @Test
    void testValidateSuccess() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Two variables with simple bounds
        Variable v0 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable v1 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        // Expression: v0 + v1 must equal 8
        Expression expr = model.addExpression().lower(BigDecimal.valueOf(8)).upper(BigDecimal.valueOf(8));
        expr.set(v0, BigDecimal.ONE);
        expr.set(v1, BigDecimal.ONE);
        // Solution that satisfies both variable bounds and the expression (v0 = 3, v1 = 5)
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(3), BigDecimal.valueOf(5));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertTrue(result, "Validation should succeed for a feasible solution");
    }

    @Test
    void testValidateNullSolutionThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(ProgrammingError.class, () -> model.validate(null, NumberContext.of(6), BasicLogger.NULL), "Expected ProgrammingError when solution is null");
    }

    @Test
    void testValidateNullContextThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE);
        assertThrows(ProgrammingError.class, () -> model.validate(solution, null, BasicLogger.NULL), "Expected ProgrammingError when context is null");
    }

    @Test
    void testValidateNullLoggerThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE);
        assertThrows(ProgrammingError.class, () -> model.validate(solution, NumberContext.of(6), null), "Expected ProgrammingError when logger is null");
    }

    @Test
    void testValidateEmptyModelNoVariablesNoExpressions() {
        // empty model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // zero‑length solution
        Access1D<BigDecimal> emptySolution = Array1D.of();
        boolean result = model.validate(emptySolution, CONTEXT, LOGGER);
        assertTrue(result, "Empty model with empty solution should validate to true");
    }

    @Test
    void testValidateEmptyModelWithConstantExpression() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Constant expression 5 == 5
        model.addExpression().level(BigDecimal.valueOf(5));
        // still no variables
        Access1D<BigDecimal> emptySolution = Array1D.of();
        boolean result = model.validate(emptySolution, CONTEXT, LOGGER);
        assertTrue(result, "Model with only a constant valid expression should validate to true");
    }

    @Test
    void testValidateRelaxedModelIgnoresVariableBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable bounds that would normally be violated
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3));
        // set relaxed flag
        model.relax();
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(10));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertTrue(result, "Relaxed model should ignore variable bound violations");
    }

    @Test
    void testValidateMultipleVariablesAllValid() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable v1 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable v2 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        // Constraint: v0 + v1 + v2 <= 10
        Expression expr = model.addExpression().upper(BigDecimal.valueOf(10));
        expr.set(v0, BigDecimal.ONE);
        expr.set(v1, BigDecimal.ONE);
        expr.set(v2, BigDecimal.ONE);
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, BigDecimal.valueOf(2), // sum = 6 <= 10
        BigDecimal.valueOf(3));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertTrue(result, "All variables and the expression should validate successfully");
    }

    @Test
    void testValidateMultipleVariablesSizeMismatch() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        model.addVariable();
        model.addVariable();
        // only 2 values
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, BigDecimal.valueOf(2));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Size mismatch should cause validation to fail immediately");
    }

    @Test
    void testValidateWithNonNullLogger() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Use a concrete logger (CONSOLE) – behaviour is the same, we only verify no exception.
        BasicLogger consoleLogger = BasicLogger.CONSOLE;
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(5));
        boolean result = model.validate(solution, CONTEXT, consoleLogger);
        assertTrue(result, "Validation should succeed and accept a non‑null logger");
    }

    @Test
    void testValidateSolutionAccessExceptionPropagates() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // need one variable so size check passes
        model.addVariable();
        Access1D<BigDecimal> badSolution = new Access1D<BigDecimal>() {

            @Override
            public long count() {
                return 1L;
            }

            @Override
            public BigDecimal get(long index) {
                throw new IndexOutOfBoundsException("mock get exception");
            }
        };
        assertThrows(IndexOutOfBoundsException.class, () -> model.validate(badSolution, CONTEXT, LOGGER), "Exception from solution.get(i) should propagate");
    }

    @Test
    void testValidateSizeMismatchExplicit() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // single variable
        model.addVariable();
        // two entries
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, BigDecimal.TEN);
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Explicit size mismatch test should return false");
    }

    @Test
    void testValidateVariableFailureDifferentBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3));
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(5));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Variable bound violation should cause validation to fail");
    }

    @Test
    void testValidateExpressionFailureDifferentExpression() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable();
        // Expression: v <= 1
        Expression expr = model.addExpression().upper(BigDecimal.ONE);
        expr.set(v, BigDecimal.ONE);
        // violates upper bound
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.valueOf(2));
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertFalse(result, "Expression bound violation should cause validation to fail");
    }

    @Test
    void testValidateSuccessfulSingleVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // expression x >= 0 (default weight 0)
        model.addExpression().lower(BigDecimal.ZERO);
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE);
        boolean result = model.validate(solution, CONTEXT, LOGGER);
        assertTrue(result, "Feasible single‑variable solution should validate to true");
    }
}
