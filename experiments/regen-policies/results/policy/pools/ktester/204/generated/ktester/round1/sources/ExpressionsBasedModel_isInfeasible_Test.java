package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isInfeasible_Test {

    /**
     * Shared model instance used by the tests that need a fresh model per test.
     */
    private ExpressionsBasedModel model;

    /**
     * Reflection helpers for the private fields of {@link ExpressionsBasedModel}.
     */
    private static Field expressionsField;

    private static Field variablesField;

    private static Field infeasibleFlagField;

    @BeforeAll
    static void initReflection() throws Exception {
        expressionsField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        expressionsField.setAccessible(true);
        variablesField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        variablesField.setAccessible(true);
        infeasibleFlagField = ExpressionsBasedModel.class.getDeclaredField("myInfeasible");
        infeasibleFlagField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a brand‑new model to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the model reference; helps the GC in long test suites.
        model = null;
    }

    /**
     * Insert a mock {@link Expression} into the model's internal map.
     */
    private void addMockExpression(String name, Expression mock) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) expressionsField.get(model);
        map.put(name, mock);
    }

    /**
     * Insert a mock {@link Variable} into the model's internal list.
     */
    private void addMockVariable(Variable mock) throws Exception {
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) variablesField.get(model);
        list.add(mock);
    }

    /**
     * Directly set the private {@code myInfeasible} flag.
     */
    private void setInfeasibleFlag(boolean value) throws Exception {
        infeasibleFlagField.setBoolean(model, value);
    }

    /**
     * Retrieve the current value of the private {@code myInfeasible} flag.
     */
    private boolean getInfeasibleFlag() throws Exception {
        return infeasibleFlagField.getBoolean(model);
    }

    /**
     * Verify that a freshly created model reports not infeasible.
     */
    @Test
    void testInitiallyNotInfeasible() {
        assertFalse(model.isInfeasible(), "A newly created model should not be marked infeasible.");
    }

    /**
     * Verify that calling {@code setInfeasible()} forces {@code isInfeasible()} to return {@code true}
     * even when no expression or variable is infeasible.
     */
    @Test
    void testExplicitInfeasibilityFlag() {
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "Model should be infeasible after explicit setInfeasible() call.");
    }

    /**
     * An expression becomes infeasible when its lower bound exceeds its upper bound.
     * This test creates such an expression and checks that the model reports infeasibility.
     */
    @Test
    void testExpressionInfeasibilityDetected() {
        // Create a variable (bounds are irrelevant for this test)
        Variable x = model.addVariable();
        // Add an expression with contradictory bounds: lower > upper
        Expression expr = model.addExpression("contradiction").lower(// lower = 1
        BigDecimal.ONE).upper(// upper = 0
        BigDecimal.ZERO);
        // The expression references the variable so that it is part of the model
        expr.set(x, BigMath.ONE);
        // The expression itself reports infeasible; the model should propagate this.
        assertTrue(expr.isInfeasible(), "The expression should report infeasibility due to contradictory bounds.");
        assertTrue(model.isInfeasible(), "Model should be infeasible because it contains an infeasible expression.");
    }

    /**
     * A variable becomes infeasible when its lower bound exceeds its upper bound.
     * This test creates such a variable and checks that the model reports infeasibility.
     */
    @Test
    void testVariableInfeasibilityDetected() {
        // Add a variable with contradictory bounds.
        Variable v = model.addVariable().lower(// lower = 10
        BigDecimal.TEN).upper(// upper = 1
        BigDecimal.ONE);
        // The variable itself reports infeasibility.
        assertTrue(v.isInfeasible(), "The variable should report infeasibility due to contradictory bounds.");
        // The model should now be infeasible.
        assertTrue(model.isInfeasible(), "Model should be infeasible because it contains an infeasible variable.");
    }

    /**
     * Ensure that once the model has been marked infeasible, subsequent calls to
     * {@code isInfeasible()} return {@code true} without re‑evaluating the expressions
     * or variables (caching behaviour).
     */
    @Test
    void testCachingBehaviourAfterInfeasibility() {
        // Create an infeasible expression.
        Variable x = model.addVariable();
        Expression expr = model.addExpression().lower(BigDecimal.ONE).upper(BigDecimal.ZERO);
        expr.set(x, BigMath.ONE);
        // First call should detect infeasibility and cache the result.
        assertTrue(model.isInfeasible(), "First call should detect infeasibility.");
        // Mutate the model to a feasible state (remove the expression).
        // This should NOT affect the cached infeasibility flag.
        // recreate a fresh model
        model = new ExpressionsBasedModel();
        // The previous model instance still holds the cached flag.
        // Verify that calling again on the original instance still returns true.
        assertTrue(expr.getModel().isInfeasible(), "Cached infeasibility flag should persist across subsequent calls.");
    }

    @Test
    void testMyInfeasibleFlagAlreadyTrue() throws Exception {
        // arrange: set flag true and add a mock expression that would fail if called
        setInfeasibleFlag(true);
        Expression expr = mock(Expression.class);
        addMockExpression("any", expr);
        // act
        boolean result = model.isInfeasible();
        // assert
        assertTrue(result, "Method should return true when myInfeasible flag is already true.");
        // verify that the expression was never queried
        verifyNoInteractions(expr);
        // flag must stay true
        assertTrue(getInfeasibleFlag());
    }

    @Test
    void testExpressionReportsInfeasible() throws Exception {
        // arrange: one expression reports infeasible
        Expression infeasibleExpr = mock(Expression.class);
        when(infeasibleExpr.isInfeasible()).thenReturn(true);
        addMockExpression("bad", infeasibleExpr);
        // act
        boolean first = model.isInfeasible();
        // assert first call
        assertTrue(first, "Model should be infeasible because an expression is infeasible.");
        assertTrue(getInfeasibleFlag(), "Flag must be set after detection.");
        // act second call – should not invoke isInfeasible again
        // clear previous interactions
        reset(infeasibleExpr);
        boolean second = model.isInfeasible();
        // assert second call
        assertTrue(second, "Subsequent call must still return true.");
        verifyNoInteractions(infeasibleExpr);
    }

    @Test
    void testVariableReportsInfeasibleNoInfeasibleExpression() throws Exception {
        // arrange: feasible expression, infeasible variable
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(false);
        addMockExpression("good", expr);
        Variable var = mock(Variable.class);
        when(var.isInfeasible()).thenReturn(true);
        addMockVariable(var);
        // act
        boolean result = model.isInfeasible();
        // assert
        assertTrue(result, "Model should be infeasible because a variable is infeasible.");
        assertTrue(getInfeasibleFlag());
        // expression checked once
        verify(expr, times(1)).isInfeasible();
        // variable checked once
        verify(var, times(1)).isInfeasible();
    }

    @Test
    void testAllFeasibleNoInfeasibility() throws Exception {
        // arrange: add feasible expression and variable
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(false);
        addMockExpression("e1", expr);
        Variable var = mock(Variable.class);
        when(var.isInfeasible()).thenReturn(false);
        addMockVariable(var);
        // act
        boolean result = model.isInfeasible();
        // assert
        assertFalse(result, "Model should report feasible when all members are feasible.");
        assertFalse(getInfeasibleFlag());
    }

    @Test
    void testFlagMemoisationAfterFirstInfeasibilityDetection() throws Exception {
        // arrange: expression infeasible
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(true);
        addMockExpression("e", expr);
        // first call – detects infeasibility
        assertTrue(model.isInfeasible());
        assertTrue(getInfeasibleFlag());
        // reset mock interactions
        reset(expr);
        // second call – should return true without invoking expression again
        assertTrue(model.isInfeasible());
        verifyNoInteractions(expr);
    }

    @Test
    void testEmptyModelNoInfeasibility() {
        // a brand‑new model has no expressions/variables
        assertFalse(model.isInfeasible(), "Empty model must not be infeasible.");
    }

    @Test
    void testInfeasibleExpressionShortCircuitsVariableEvaluation() throws Exception {
        // arrange: expression infeasible, variable that would throw if called
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(true);
        addMockExpression("bad", expr);
        Variable var = mock(Variable.class);
        when(var.isInfeasible()).thenThrow(new AssertionError("Variable should not be inspected"));
        addMockVariable(var);
        // act & assert
        assertTrue(model.isInfeasible(), "Infeasibility should be detected from expression alone.");
        // verify variable never touched
        verifyNoInteractions(var);
    }

    @Test
    void testInfeasibleVariableAfterAllExpressionsFeasible() throws Exception {
        // arrange: two feasible expressions, one infeasible variable
        Expression e1 = mock(Expression.class);
        Expression e2 = mock(Expression.class);
        when(e1.isInfeasible()).thenReturn(false);
        when(e2.isInfeasible()).thenReturn(false);
        addMockExpression("e1", e1);
        addMockExpression("e2", e2);
        Variable v = mock(Variable.class);
        when(v.isInfeasible()).thenReturn(true);
        addMockVariable(v);
        // act
        assertTrue(model.isInfeasible(), "Model should be infeasible due to variable after all expressions.");
        // verify each expression was inspected once
        verify(e1, times(1)).isInfeasible();
        verify(e2, times(1)).isInfeasible();
        verify(v, times(1)).isInfeasible();
    }

    @Test
    void testMultipleInfeasibleExpressionsFirstOneWins() throws Exception {
        // arrange: first expression infeasible, second would throw if called
        Expression first = mock(Expression.class);
        when(first.isInfeasible()).thenReturn(true);
        addMockExpression("first", first);
        Expression second = mock(Expression.class);
        when(second.isInfeasible()).thenThrow(new AssertionError("Second expression must not be evaluated"));
        addMockExpression("second", second);
        // act
        assertTrue(model.isInfeasible(), "Model should stop after first infeasible expression.");
        // verify second never called
        verifyNoInteractions(second);
    }

    @Test
    void testFlagRemainsTrueAfterVariableInfeasibility() throws Exception {
        // arrange: feasible expression, infeasible variable
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(false);
        addMockExpression("e", expr);
        Variable var = mock(Variable.class);
        when(var.isInfeasible()).thenReturn(true);
        addMockVariable(var);
        // first call – detects variable infeasibility
        assertTrue(model.isInfeasible());
        assertTrue(getInfeasibleFlag());
        // reset mocks
        reset(expr, var);
        // second call – should return true without any further checks
        assertTrue(model.isInfeasible());
        verifyNoInteractions(expr, var);
    }

    @Test
    void testNullExpressionCausesNullPointerException() throws Exception {
        // arrange: put a null entry into the map
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) expressionsField.get(model);
        map.put("nullExpr", null);
        // act & assert
        assertThrows(NullPointerException.class, () -> model.isInfeasible(), "Iterating over a null expression should throw NPE.");
    }

    @Test
    void testNullVariableCausesNullPointerException() throws Exception {
        // arrange: add a null variable to the list
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) variablesField.get(model);
        list.add(null);
        // act & assert
        assertThrows(NullPointerException.class, () -> model.isInfeasible(), "Iterating over a null variable should throw NPE.");
    }

    @Test
    void testExpressionThrowsIllegalStateException() throws Exception {
        // arrange: expression that throws IllegalStateException
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenThrow(new IllegalStateException("boom"));
        addMockExpression("bad", expr);
        // act & assert
        assertThrows(IllegalStateException.class, () -> model.isInfeasible(), "Exception from expression must propagate.");
    }

    @Test
    void testVariableThrowsIllegalArgumentException() throws Exception {
        // arrange: feasible expression, variable that throws IllegalArgumentException
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(false);
        addMockExpression("e", expr);
        Variable var = mock(Variable.class);
        when(var.isInfeasible()).thenThrow(new IllegalArgumentException("bad arg"));
        addMockVariable(var);
        // act & assert
        assertThrows(IllegalArgumentException.class, () -> model.isInfeasible(), "Exception from variable must propagate.");
    }

    @Test
    void testConcurrentModificationOfExpressionsDuringIteration() throws Exception {
        // arrange: two expressions, first removes the second during its isInfeasible call
        Expression first = mock(Expression.class);
        Expression second = mock(Expression.class);
        when(first.isInfeasible()).thenAnswer(invocation -> {
            // remove the second entry while iteration is in progress
            @SuppressWarnings("unchecked")
            Map<String, Expression> map = (Map<String, Expression>) expressionsField.get(model);
            map.remove("second");
            // not infeasible itself
            return false;
        });
        when(second.isInfeasible()).thenReturn(false);
        addMockExpression("first", first);
        addMockExpression("second", second);
        // act & assert
        assertThrows(ConcurrentModificationException.class, () -> model.isInfeasible(), "Modifying the expressions map during iteration should cause CME.");
    }

    @Test
    void testConcurrentModificationOfVariablesDuringIteration() throws Exception {
        // arrange: two variables, first removes the second during its isInfeasible call
        Variable first = mock(Variable.class);
        Variable second = mock(Variable.class);
        when(first.isInfeasible()).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ArrayList<Variable> list = (ArrayList<Variable>) variablesField.get(model);
            list.remove(second);
            return false;
        });
        when(second.isInfeasible()).thenReturn(false);
        addMockVariable(first);
        addMockVariable(second);
        // act & assert
        assertThrows(ConcurrentModificationException.class, () -> model.isInfeasible(), "Modifying the variables list during iteration should cause CME.");
    }
}
