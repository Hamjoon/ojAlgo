package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.array.Array1D;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_8_Test {

    /**
     * Re‑used logger – the {@link BasicLogger#NULL} implementation discards all output.
     */
    private static final BasicLogger NULL_LOGGER = BasicLogger.NULL;

    /**
     * Helper that creates a tiny model with two variables and a single equality expression.
     */
    private ExpressionsBasedModel createSimpleModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // two variables, both unrestricted apart from the bounds we set
        Variable x = model.addVariable().lower(0).upper(10).weight(1);
        Variable y = model.addVariable().lower(0).upper(10).weight(1);
        // expression: x + y == 5
        Expression expr = model.addExpression();
        expr.set(x, BigDecimal.ONE);
        expr.set(y, BigDecimal.ONE);
        expr.level(BigDecimal.valueOf(5));
        return model;
    }

    /**
     * Creates an {@link Access1D} backed by the given values.
     */
    private Access1D<BigDecimal> solution(BigDecimal... values) {
        return new Array1D<>(values);
    }

    /**
     * Creates a tiny model with two variables and a single equality expression (x + y = 5).
     */
    private ExpressionsBasedModel createTwoVarEqualityModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable().lower(0).upper(10).weight(1);
        Variable y = model.addVariable().lower(0).upper(10).weight(1);
        Expression expr = model.addExpression();
        expr.set(x, BigDecimal.ONE);
        expr.set(y, BigDecimal.ONE);
        expr.level(BigDecimal.valueOf(5));
        return model;
    }

    /**
     * Creates a model with a single variable that has bounds [0,5].
     */
    private ExpressionsBasedModel createSingleBoundedVariableModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(0).upper(5);
        return model;
    }

    /**
     * Injects a list of {@link Variable}s into the private {@code myVariables} field of the model.
     */
    private void injectVariables(ExpressionsBasedModel model, List<Variable> vars) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, new ArrayList<>(vars));
    }

    /**
     * Injects a map of {@link Expression}s into the private {@code myExpressions} field of the model.
     */
    private void injectExpressions(ExpressionsBasedModel model, Map<String, Expression> exprMap) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        f.set(model, new HashMap<>(exprMap));
    }

    /**
     * Sets the {@code myRelaxed} flag via reflection.
     */
    private void setRelaxed(ExpressionsBasedModel model, boolean relaxed) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myRelaxed");
        f.setAccessible(true);
        f.setBoolean(model, relaxed);
    }

    @Test
    void testValidateReturnsTrueWhenAllChecksPass() {
        ExpressionsBasedModel model = createSimpleModel();
        // a solution that satisfies variable bounds and the expression (2 + 3 = 5)
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(2), BigDecimal.valueOf(3));
        // standard feasibility context
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertTrue(ok, "Validation should succeed when solution matches all constraints");
    }

    @Test
    void testValidateFailsWhenSolutionSizeDoesNotMatchVariableCount() {
        ExpressionsBasedModel model = createSimpleModel();
        // only one value – the model has two variables, so size mismatch must cause failure
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(5));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertFalse(ok, "Validation must fail when solution size differs from variable count");
    }

    @Test
    void testValidateFailsWhenExpressionIsNotSatisfied() {
        ExpressionsBasedModel model = createSimpleModel();
        // values respect variable bounds but do NOT satisfy the expression (2 + 2 ≠ 5)
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(2), BigDecimal.valueOf(2));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertFalse(ok, "Validation must fail when an expression does not meet its constraint");
    }

    @Test
    void testValidateThrowsWhenAnyArgumentIsNull() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        NumberContext ctx = model.options.feasibility;
        // all three arguments null – ProgrammingError should throw NPE
        assertThrows(NullPointerException.class, () -> model.validate(null, null, null), "validate(null, null, null) must throw NullPointerException");
    }

    @Test
    void testValidateFailsWhenSolutionSizeMismatchWithThreeVariables() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // three variables
        model.addVariable();
        model.addVariable();
        model.addVariable();
        // only 2 values
        Access1D<BigDecimal> sol = solution(BigDecimal.ZERO, BigDecimal.ZERO);
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertFalse(ok, "Validation must fail when solution size (2) != variable count (3)");
    }

    @Test
    void testValidateFailsWhenFirstVariableInvalid() throws Exception {
        // two variables, first has lower bound 0, we pass -1
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // will reject -1
        model.addVariable().lower(0);
        // unrestricted
        model.addVariable();
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(-1), BigDecimal.valueOf(3));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertFalse(ok, "Validation must stop and return false when a variable fails validation");
    }

    @Test
    void testValidateFailsWhenExpressionValidationFails() {
        // reuse the equality model – solution respects bounds but violates the expression
        ExpressionsBasedModel model = createTwoVarEqualityModel();
        // sum = 4
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(2), BigDecimal.valueOf(2));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertFalse(ok, "Validation must return false when an expression's validation fails");
    }

    @Test
    void testValidateSuccessWhenModelHasNoVariablesOrExpressions() throws Exception {
        // empty model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // count == 0
        Access1D<BigDecimal> emptySolution = solution();
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(emptySolution, ctx, NULL_LOGGER);
        assertTrue(ok, "Empty model with empty solution should validate to true");
    }

    @Test
    void testValidateSuccessWithOnlyVariablesNoExpressions() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // unrestricted
        model.addVariable();
        // unrestricted
        model.addVariable();
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertTrue(ok, "Model with only variables (no expressions) should validate successfully");
    }

    @Test
    void testValidateFailsWhenVariableBoundViolatedUnderStrictContext() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // both variables have lower bound 0
        model.addVariable().lower(0);
        model.addVariable().lower(0);
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(5), BigDecimal.valueOf(-1));
        // strict context: zero tolerance – any deviation is considered invalid
        // zero precision, zero tolerance
        NumberContext strict = NumberContext.of(0);
        boolean ok = model.validate(sol, strict, NULL_LOGGER);
        assertFalse(ok, "Strict context should cause validation failure for negative bound violation");
    }

    @Test
    void testValidateSuccessWhenModelIsRelaxedEvenIfVariableOutOfBounds() throws Exception {
        ExpressionsBasedModel model = createSingleBoundedVariableModel();
        // mark model as relaxed
        setRelaxed(model, true);
        // out of [0,5] bound
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(10));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertTrue(ok, "Relaxed model should ignore variable bound violations");
    }

    @Test
    void testValidateFailsWhenNotRelaxedAndVariableOutOfBounds() throws Exception {
        // default not relaxed
        ExpressionsBasedModel model = createSingleBoundedVariableModel();
        // out of bounds
        Access1D<BigDecimal> sol = solution(BigDecimal.valueOf(10));
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(sol, ctx, NULL_LOGGER);
        assertFalse(ok, "Non‑relaxed model must reject out‑of‑bounds variable values");
    }

    @Test
    void testValidateSuccessWithConstantExpressionAndEmptySolution() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // constant expression: 0 == 0
        Expression expr = model.addExpression();
        // no variables, just a level of 0
        expr.level(BigDecimal.ZERO);
        // count == 0
        Access1D<BigDecimal> emptySol = solution();
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(emptySol, ctx, NULL_LOGGER);
        assertTrue(ok, "Constant expression that validates should allow empty solution");
    }

    @Test
    void testValidateFailsWithConstantExpressionThatDoesNotValidate() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // constant expression: 5 must be <= 0 (will fail)
        Expression expr = model.addExpression();
        expr.level(BigDecimal.valueOf(5));
        // constraint: value <= 0
        expr.upper(BigDecimal.ZERO);
        // count == 0
        Access1D<BigDecimal> emptySol = solution();
        NumberContext ctx = model.options.feasibility;
        boolean ok = model.validate(emptySol, ctx, NULL_LOGGER);
        assertFalse(ok, "Failing constant expression should cause overall validation to fail");
    }

    @Test
    void testValidatePropagatesExceptionFromSolutionGet() {
        // Mock Access1D: count() == 2, but get(i) throws IndexOutOfBoundsException
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> mockSolution = mock(Access1D.class);
        when(mockSolution.count()).thenReturn(2L);
        when(mockSolution.get(anyInt())).thenThrow(new IndexOutOfBoundsException("forced"));
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        model.addVariable();
        NumberContext ctx = model.options.feasibility;
        assertThrows(IndexOutOfBoundsException.class, () -> model.validate(mockSolution, ctx, NULL_LOGGER), "Exception from solution.get should propagate");
    }

    @Test
    void testValidatePropagatesExceptionFromVariableValidate_NullValue() throws Exception {
        // Mock Variable that throws NPE when validate is called
        Variable mockVar = mock(Variable.class);
        when(mockVar.validate(isNull(), any(), any(), anyBoolean())).thenThrow(new NullPointerException("variable.validate NPE"));
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectVariables(model, Collections.singletonList(mockVar));
        // Mock solution that returns null for index 0
        @SuppressWarnings("unchecked")
        Access1D<BigDecimal> mockSolution = mock(Access1D.class);
        when(mockSolution.count()).thenReturn(1L);
        when(mockSolution.get(0)).thenReturn(null);
        NumberContext ctx = model.options.feasibility;
        assertThrows(NullPointerException.class, () -> model.validate(mockSolution, ctx, NULL_LOGGER), "NullPointerException from Variable.validate must propagate");
    }

    @Test
    void testValidatePropagatesExceptionFromVariableValidate_Runtime() throws Exception {
        // Mock Variable that throws ArithmeticException during validation
        Variable mockVar = mock(Variable.class);
        when(mockVar.validate(any(), any(), any(), anyBoolean())).thenThrow(new ArithmeticException("forced arithmetic error"));
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectVariables(model, Collections.singletonList(mockVar));
        Access1D<BigDecimal> sol = solution(BigDecimal.ONE);
        NumberContext ctx = model.options.feasibility;
        assertThrows(ArithmeticException.class, () -> model.validate(sol, ctx, NULL_LOGGER), "ArithmeticException from Variable.validate must propagate");
    }

    @Test
    void testValidatePropagatesExceptionFromExpressionEvaluate() throws Exception {
        // Mock Expression that throws IllegalStateException on evaluate()
        Expression mockExpr = mock(Expression.class);
        when(mockExpr.evaluate(any())).thenThrow(new IllegalStateException("forced evaluate"));
        // validate() will never be called because evaluate throws first
        when(mockExpr.validate(any(), any(), any())).thenReturn(true);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Need at least one variable so size check passes
        model.addVariable();
        Map<String, Expression> exprMap = new HashMap<>();
        exprMap.put("badExpr", mockExpr);
        injectExpressions(model, exprMap);
        Access1D<BigDecimal> sol = solution(BigDecimal.ZERO);
        NumberContext ctx = model.options.feasibility;
        assertThrows(IllegalStateException.class, () -> model.validate(sol, ctx, NULL_LOGGER), "Exception from Expression.evaluate must propagate");
    }

    @Test
    void testValidatePropagatesExceptionFromExpressionValidate() throws Exception {
        // Mock Expression that evaluates fine but throws RuntimeException on validate()
        Expression mockExpr = mock(Expression.class);
        when(mockExpr.evaluate(any())).thenReturn(BigDecimal.ZERO);
        when(mockExpr.validate(any(), any(), any())).thenThrow(new RuntimeException("forced validate"));
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // ensure size match
        model.addVariable();
        Map<String, Expression> exprMap = new HashMap<>();
        exprMap.put("badValidate", mockExpr);
        injectExpressions(model, exprMap);
        Access1D<BigDecimal> sol = solution(BigDecimal.ZERO);
        NumberContext ctx = model.options.feasibility;
        assertThrows(RuntimeException.class, () -> model.validate(sol, ctx, NULL_LOGGER), "Exception from Expression.validate must propagate");
    }
}
