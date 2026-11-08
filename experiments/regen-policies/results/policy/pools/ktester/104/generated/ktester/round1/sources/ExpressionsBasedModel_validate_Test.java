package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.ojalgo.netio.BasicLogger;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test so that state does not leak.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources, but we clear the reference
        // to help the garbage collector.
        model = null;
    }

    private static Stream<Arguments> loggerSelectionProvider() {
        // format: loggerDetailed, varResult, exprResult, expected
        return Stream.of(// both true → true
        Arguments.of(true, true, true, true), // detailed false → true (NULL logger)
        Arguments.of(false, true, true, true), // variable false, expression true → false
        Arguments.of(true, false, true, false), // variable true, expression false → false
        Arguments.of(false, true, false, false), // both false → false
        Arguments.of(true, false, false, false), // no variables / expressions, detailed false → true
        Arguments.of(false, null, null, true));
    }

    private static Stream<Arguments> multipleVariablesProvider() {
        // format: list of boolean results for variables, expected overall result
        return Stream.of(Arguments.of(Arrays.asList(true, true, true), true), Arguments.of(Arrays.asList(true, false, true), false));
    }

    private static Stream<Arguments> multipleExpressionsProvider() {
        // format: list of boolean results for expressions, expected overall result
        return Stream.of(Arguments.of(Arrays.asList(true, true, true, true), true), Arguments.of(Arrays.asList(true, true, false, true), false));
    }

    private static Stream<Arguments> mixedResultsProvider() {
        // Each entry: list of var results, list of expr results, expected overall
        return Stream.of(// 2 vars (true,true) + 3 expr (true,false,true) → false
        Arguments.of(Arrays.asList(true, true), Arrays.asList(true, false, true), false), // 3 vars (true,false,true) + 2 expr (true,true) → false
        Arguments.of(Arrays.asList(true, false, true), Arrays.asList(true, true), false), // 3 vars (false,true,true) + 3 expr (false,true,true) → false
        Arguments.of(Arrays.asList(false, true, true), Arrays.asList(false, true, true), false), // 5 vars (all true) + 5 expr (all true) → true
        Arguments.of(Arrays.asList(true, true, true, true, true), Arrays.asList(true, true, true, true, true), true));
    }

    private static Stream<Arguments> detailedLoggerPropagationProvider() {
        // format: list of variable results, list of expression results, oneVarFails?, expected
        return Stream.of(// all true → true
        Arguments.of(Arrays.asList(true, true, true), Arrays.asList(true, true), false, true), // one variable false → false
        Arguments.of(Arrays.asList(true, false, true), Arrays.asList(true, true), true, false));
    }

    /**
     * All variables and expressions are well‑formed, therefore {@code validate()}
     * must return {@code true}.
     */
    @Test
    void testValidateAllValidReturnsTrue() {
        // a valid variable (lower <= upper)
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // a valid expression (simple linear constraint)
        Expression expr = model.newExpression("expr").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20));
        // 1 * x
        expr.set(x, BigDecimal.ONE);
        // The model now contains one variable and one expression, both valid.
        assertTrue(model.validate(), "Model should be valid when all contained variables and expressions are valid");
    }

    /**
     * A variable with an illegal bound configuration (lower > upper) should cause
     * {@code validate()} to return {@code false}.
     */
    @Test
    void testValidateInvalidVariableReturnsFalse() {
        // Create a variable whose lower bound is greater than its upper bound.
        Variable y = model.newVariable("y").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ONE);
        // Add a simple expression that would be valid on its own.
        Expression expr = model.newExpression("expr").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        expr.set(y, BigDecimal.ONE);
        // Because the variable is invalid, the whole model validation must fail.
        assertFalse(model.validate(), "Model validation must fail when a variable has inconsistent bounds");
    }

    /**
     * An expression with contradictory bounds (lower > upper) should also make
     * {@code validate()} return {@code false}, even if all variables are valid.
     */
    @Test
    void testValidateInvalidExpressionReturnsFalse() {
        // Valid variable
        Variable z = model.newVariable("z").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        // Invalid expression: lower bound greater than upper bound.
        Expression badExpr = model.newExpression("bad").lower(BigDecimal.valueOf(8)).upper(BigDecimal.valueOf(2));
        badExpr.set(z, BigDecimal.ONE);
        // Model validation must fail because of the bad expression.
        assertFalse(model.validate(), "Model validation must fail when an expression has inconsistent bounds");
    }

    @SuppressWarnings("unchecked")
    private List<Variable> getVariableList() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        return (List<Variable>) f.get(model);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Expression> getExpressionMap() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        return (Map<String, Expression>) f.get(model);
    }

    @ParameterizedTest
    @MethodSource("loggerSelectionProvider")
    void testValidate_loggerSelectionAndAggregation(Boolean loggerDetailed, Boolean varResult, Boolean exprResult, boolean expected) throws Exception {
        // -----------------------------------------------------------------
        // Configure options (logger_detailed & logger_appender)
        // -----------------------------------------------------------------
        model.options.logger_detailed = loggerDetailed;
        BasicLogger customAppender = mock(BasicLogger.class);
        // may stay null for some cases – handled below
        model.options.logger_appender = customAppender;
        // -----------------------------------------------------------------
        // Prepare mocks for Variable / Expression (if required)
        // -----------------------------------------------------------------
        List<Variable> vars = getVariableList();
        Map<String, Expression> exprs = getExpressionMap();
        if (varResult != null) {
            Variable mockVar = mock(Variable.class);
            when(mockVar.validate(any())).thenReturn(varResult);
            vars.add(mockVar);
        }
        if (exprResult != null) {
            Expression mockExpr = mock(Expression.class);
            when(mockExpr.validate(any())).thenReturn(exprResult);
            exprs.put("e", mockExpr);
        }
        // -----------------------------------------------------------------
        // Execute validation
        // -----------------------------------------------------------------
        boolean result = model.validate();
        // -----------------------------------------------------------------
        // Verify the aggregated result
        // -----------------------------------------------------------------
        assertEquals(expected, result, () -> "Unexpected validation result with loggerDetailed=" + loggerDetailed);
        // -----------------------------------------------------------------
        // Verify that the correct appender instance was passed to each validate()
        // -----------------------------------------------------------------
        BasicLogger expectedAppender = loggerDetailed ? model.options.logger_appender : BasicLogger.NULL;
        // capture arguments only if we actually created mocks
        if (varResult != null) {
            ArgumentCaptor<BasicLogger> varCaptor = ArgumentCaptor.forClass(BasicLogger.class);
            verify(vars.get(0)).validate(varCaptor.capture());
            assertSame(expectedAppender, varCaptor.getValue(), "Variable.validate should receive the selected appender");
        }
        if (exprResult != null) {
            ArgumentCaptor<BasicLogger> exprCaptor = ArgumentCaptor.forClass(BasicLogger.class);
            verify(exprs.get("e")).validate(exprCaptor.capture());
            assertSame(expectedAppender, exprCaptor.getValue(), "Expression.validate should receive the selected appender");
        }
    }

    @ParameterizedTest
    @MethodSource("multipleVariablesProvider")
    void testValidate_multipleVariables(List<Boolean> varResults, boolean expected) throws Exception {
        List<Variable> vars = getVariableList();
        for (int i = 0; i < varResults.size(); i++) {
            Variable mockVar = mock(Variable.class);
            when(mockVar.validate(any())).thenReturn(varResults.get(i));
            vars.add(mockVar);
        }
        // No expressions added – map stays empty
        assertEquals(expected, model.validate(), "Aggregated variable validation result does not match expectation");
    }

    @ParameterizedTest
    @MethodSource("multipleExpressionsProvider")
    void testValidate_multipleExpressions(List<Boolean> exprResults, boolean expected) throws Exception {
        Map<String, Expression> exprs = getExpressionMap();
        int idx = 0;
        for (Boolean r : exprResults) {
            Expression mockExpr = mock(Expression.class);
            when(mockExpr.validate(any())).thenReturn(r);
            exprs.put("e" + idx++, mockExpr);
        }
        // No variables added – list stays empty
        assertEquals(expected, model.validate(), "Aggregated expression validation result does not match expectation");
    }

    @ParameterizedTest
    @MethodSource("mixedResultsProvider")
    void testValidate_mixedResults(List<Boolean> varResults, List<Boolean> exprResults, boolean expected) throws Exception {
        List<Variable> vars = getVariableList();
        for (Boolean r : varResults) {
            Variable mockVar = mock(Variable.class);
            when(mockVar.validate(any())).thenReturn(r);
            vars.add(mockVar);
        }
        Map<String, Expression> exprs = getExpressionMap();
        int idx = 0;
        for (Boolean r : exprResults) {
            Expression mockExpr = mock(Expression.class);
            when(mockExpr.validate(any())).thenReturn(r);
            exprs.put("e" + idx++, mockExpr);
        }
        assertEquals(expected, model.validate(), "Mixed validation aggregation produced unexpected result");
    }

    @ParameterizedTest
    @MethodSource("detailedLoggerPropagationProvider")
    void testValidate_detailedLoggerAppenderPropagation(List<Boolean> varResults, List<Boolean> exprResults, boolean oneVarFails, boolean expected) throws Exception {
        // Enable detailed logger and inject a mock appender
        model.options.logger_detailed = true;
        BasicLogger mockAppender = mock(BasicLogger.class);
        model.options.logger_appender = mockAppender;
        // Create variables
        List<Variable> vars = getVariableList();
        for (Boolean r : varResults) {
            Variable mockVar = mock(Variable.class);
            when(mockVar.validate(any())).thenReturn(r);
            vars.add(mockVar);
        }
        // Create expressions
        Map<String, Expression> exprs = getExpressionMap();
        int idx = 0;
        for (Boolean r : exprResults) {
            Expression mockExpr = mock(Expression.class);
            when(mockExpr.validate(any())).thenReturn(r);
            exprs.put("e" + idx++, mockExpr);
        }
        // Run validation
        boolean result = model.validate();
        assertEquals(expected, result, "Overall result mismatch");
        // Verify that every mock received the SAME custom logger instance
        ArgumentCaptor<BasicLogger> captor = ArgumentCaptor.forClass(BasicLogger.class);
        for (Variable v : vars) {
            verify(v).validate(captor.capture());
            assertSame(mockAppender, captor.getValue(), "Variable.validate should receive the custom appender");
        }
        for (Expression e : exprs.values()) {
            verify(e).validate(captor.capture());
            assertSame(mockAppender, captor.getValue(), "Expression.validate should receive the custom appender");
        }
    }

    @Test
    void testValidate_nullOptions_throwsNPE() throws Exception {
        // Set the private final field 'options' to null via reflection
        Field optField = ExpressionsBasedModel.class.getDeclaredField("options");
        optField.setAccessible(true);
        optField.set(model, null);
        assertThrows(NullPointerException.class, model::validate, "Calling validate() with a null Optimisation.Options should throw NPE");
    }

    @Test
    void testValidate_nullAppenderWhenDetailed_throwsNPE() throws Exception {
        // detailed logger = true, but logger_appender stays null (default)
        model.options.logger_detailed = true;
        // explicit for clarity
        model.options.logger_appender = null;
        // Insert a mock Variable that will throw NPE when validate receives null
        Variable mockVar = mock(Variable.class);
        when(mockVar.validate(isNull())).thenThrow(NullPointerException.class);
        getVariableList().add(mockVar);
        assertThrows(NullPointerException.class, model::validate, "Variable.validate should receive null appender and throw NPE");
    }

    @Test
    void testValidate_nullVariableInList_throwsNPE() throws Exception {
        // Insert a legitimate mock variable first (so the loop runs)
        Variable goodVar = mock(Variable.class);
        when(goodVar.validate(any())).thenReturn(true);
        getVariableList().add(goodVar);
        // Insert a null entry via reflection
        getVariableList().add(null);
        assertThrows(NullPointerException.class, model::validate, "Iterating over a null variable should cause NPE");
    }

    @Test
    void testValidate_nullExpressionInMap_throwsNPE() throws Exception {
        // Insert a legitimate mock expression first
        Expression goodExpr = mock(Expression.class);
        when(goodExpr.validate(any())).thenReturn(true);
        getExpressionMap().put("good", goodExpr);
        // Insert a null expression
        getExpressionMap().put("nullExpr", null);
        assertThrows(NullPointerException.class, model::validate, "Iterating over a null expression should cause NPE");
    }
}
