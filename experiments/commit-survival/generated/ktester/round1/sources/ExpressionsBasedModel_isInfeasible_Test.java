package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isInfeasible_Test {

    private static Map<String, Expression> getExpressionsMap(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) f.get(model);
        return map;
    }

    private static List<Variable> getVariablesList(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        return list;
    }

    private static void setInfeasibleFlag(ExpressionsBasedModel model, boolean value) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myInfeasible");
        f.setAccessible(true);
        f.setBoolean(model, value);
    }

    private static boolean getInfeasibleFlag(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myInfeasible");
        f.setAccessible(true);
        return f.getBoolean(model);
    }

    /**
     * Verify that a model reports infeasibility when an expression has contradictory limits.
     */
    @Test
    void testIsInfeasibleDueToExpression() {
        // Create a fresh model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add a variable that will be used in the expression
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Create an expression whose lower bound is greater than its upper bound → infeasible
        Expression expr = model.newExpression("infeasibleExpr").lower(BigDecimal.valueOf(5)).upper(// lower > upper
        BigDecimal.valueOf(3));
        // Add a simple term so the expression is part of the model
        expr.set(x, BigDecimal.ONE);
        // The model should now recognise the infeasibility
        assertTrue(model.isInfeasible(), "Model should be infeasible because the added expression is contradictory");
    }

    /**
     * Verify that a model reports infeasibility when a variable has contradictory bounds.
     */
    @Test
    void testIsInfeasibleDueToVariable() {
        // Create a fresh model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add a variable with lower bound greater than upper bound → infeasible
        model.addVariable("y").lower(BigDecimal.valueOf(8)).upper(BigDecimal.valueOf(2)).weight(BigDecimal.ONE);
        // No need to add any expressions; the variable itself makes the model infeasible
        assertTrue(model.isInfeasible(), "Model should be infeasible because the added variable has contradictory bounds");
    }

    @Test
    void testMyInfeasibleFlagShortCircuit() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // manually mark as infeasible
        setInfeasibleFlag(model, true);
        // Even if we inject a malicious expression that would throw, it must not be visited
        Expression malicious = mock(Expression.class);
        when(malicious.isInfeasible()).thenThrow(new AssertionError("Should not be called"));
        getExpressionsMap(model).put("mal", malicious);
        assertTrue(model.isInfeasible(), "Method must return true immediately when myInfeasible flag is true");
    }

    @Test
    void testNoInfeasibilityEmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isInfeasible(), "Empty model must be feasible");
    }

    @Test
    void testSingleFeasibleExpression() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(false);
        getExpressionsMap(model).put("e1", expr);
        assertFalse(model.isInfeasible(), "Model with a single feasible expression should be feasible");
        verify(expr, times(1)).isInfeasible();
    }

    @Test
    void testSingleInfeasibleExpression() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(true);
        getExpressionsMap(model).put("e1", expr);
        assertTrue(model.isInfeasible(), "Model should be infeasible when the single expression reports infeasibility");
        assertTrue(getInfeasibleFlag(model), "myInfeasible flag must be set after detection");
        verify(expr, times(1)).isInfeasible();
    }

    @Test
    void testExpressionInfeasibilityShortCircuit() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression infeas = mock(Expression.class);
        when(infeas.isInfeasible()).thenReturn(true);
        Expression feas1 = mock(Expression.class);
        when(feas1.isInfeasible()).thenReturn(false);
        Expression feas2 = mock(Expression.class);
        when(feas2.isInfeasible()).thenReturn(false);
        Map<String, Expression> map = getExpressionsMap(model);
        map.put("e0", infeas);
        map.put("e1", feas1);
        map.put("e2", feas2);
        assertTrue(model.isInfeasible(), "First infeasible expression should cause true");
        verify(infeas, times(1)).isInfeasible();
        verify(feas1, never()).isInfeasible();
        verify(feas2, never()).isInfeasible();
    }

    @Test
    void testVariableInfeasibilityAfterFeasibleExpressions() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // three feasible expressions
        for (int i = 0; i < 3; i++) {
            Expression e = mock(Expression.class);
            when(e.isInfeasible()).thenReturn(false);
            getExpressionsMap(model).put("e" + i, e);
        }
        // one infeasible variable
        Variable vInfeas = mock(Variable.class);
        when(vInfeas.isInfeasible()).thenReturn(true);
        getVariablesList(model).add(vInfeas);
        assertTrue(model.isInfeasible(), "Infeasible variable after feasible expressions must make model infeasible");
        verify(vInfeas, times(1)).isInfeasible();
    }

    @Test
    void testVariableInfeasibilityShortCircuit() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // no expressions
        // two variables
        Variable v0 = mock(Variable.class);
        when(v0.isInfeasible()).thenReturn(true);
        Variable v1 = mock(Variable.class);
        when(v1.isInfeasible()).thenReturn(false);
        List<Variable> vars = getVariablesList(model);
        vars.add(v0);
        vars.add(v1);
        assertTrue(model.isInfeasible(), "First infeasible variable should short‑circuit");
        verify(v0, times(1)).isInfeasible();
        verify(v1, never()).isInfeasible();
    }

    @Test
    void testExpressionPrecedenceOverVariable() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(true);
        getExpressionsMap(model).put("badExpr", expr);
        Variable var = mock(Variable.class);
        // should never be called
        when(var.isInfeasible()).thenReturn(true);
        getVariablesList(model).add(var);
        assertTrue(model.isInfeasible(), "Expression infeasibility must be detected before checking variables");
        verify(expr, times(1)).isInfeasible();
        verify(var, never()).isInfeasible();
    }

    @Test
    void testFlagPersistenceAfterFirstDetection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression expr = mock(Expression.class);
        when(expr.isInfeasible()).thenReturn(true);
        getExpressionsMap(model).put("e", expr);
        // first call – detects infeasibility and sets flag
        assertTrue(model.isInfeasible());
        assertTrue(getInfeasibleFlag(model));
        // reset mock interactions
        reset(expr);
        // second call – should return true immediately without invoking expression
        assertTrue(model.isInfeasible());
        verify(expr, never()).isInfeasible();
    }

    @Test
    void testMixedCollectionEarlyDetection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Expression1 – feasible
        Expression e1 = mock(Expression.class);
        when(e1.isInfeasible()).thenReturn(false);
        // Variable1 – feasible
        Variable v1 = mock(Variable.class);
        when(v1.isInfeasible()).thenReturn(false);
        // Expression2 – infeasible (first infeasible element)
        Expression e2 = mock(Expression.class);
        when(e2.isInfeasible()).thenReturn(true);
        // Variable2 – infeasible but should never be checked
        Variable v2 = mock(Variable.class);
        when(v2.isInfeasible()).thenReturn(true);
        getExpressionsMap(model).put("e1", e1);
        getExpressionsMap(model).put("e2", e2);
        getVariablesList(model).add(v1);
        getVariablesList(model).add(v2);
        assertTrue(model.isInfeasible(), "First infeasible expression should stop further checks");
        verify(e1, times(1)).isInfeasible();
        verify(e2, times(1)).isInfeasible();
        verify(v1, never()).isInfeasible();
        verify(v2, never()).isInfeasible();
    }

    @Test
    void testNullExpressionInCollection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        getExpressionsMap(model).put("nullExpr", null);
        assertThrows(NullPointerException.class, model::isInfeasible, "Iterating over a null expression should throw NPE");
    }

    @Test
    void testNullVariableInCollection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        getVariablesList(model).add(null);
        assertThrows(NullPointerException.class, model::isInfeasible, "Iterating over a null variable should throw NPE");
    }

    @Test
    void testExpressionThrowsRuntimeException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression bad = mock(Expression.class);
        when(bad.isInfeasible()).thenThrow(new IllegalStateException("boom"));
        getExpressionsMap(model).put("bad", bad);
        IllegalStateException ex = assertThrows(IllegalStateException.class, model::isInfeasible);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testVariableThrowsRuntimeException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable bad = mock(Variable.class);
        when(bad.isInfeasible()).thenThrow(new IllegalArgumentException("bad var"));
        getVariablesList(model).add(bad);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, model::isInfeasible);
        assertEquals("bad var", ex.getMessage());
    }

    @Test
    void testConcurrentModificationDuringExpressionIteration() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Expression that will remove another expression while being evaluated
        Expression remover = mock(Expression.class);
        doAnswer(invocation -> {
            // remove the second expression during iteration
            getExpressionsMap(model).remove("e2");
            // not infeasible
            return false;
        }).when(remover).isInfeasible();
        Expression e2 = mock(Expression.class);
        when(e2.isInfeasible()).thenReturn(false);
        Map<String, Expression> map = getExpressionsMap(model);
        map.put("e1", remover);
        map.put("e2", e2);
        assertThrows(ConcurrentModificationException.class, model::isInfeasible, "Modifying the expression map during iteration must cause CME");
    }

    @Test
    void testConcurrentModificationDuringVariableIteration() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable remover = mock(Variable.class);
        doAnswer(invocation -> {
            // remove the second variable while iterating
            getVariablesList(model).remove(1);
            return false;
        }).when(remover).isInfeasible();
        Variable v2 = mock(Variable.class);
        when(v2.isInfeasible()).thenReturn(false);
        List<Variable> vars = getVariablesList(model);
        // index 0
        vars.add(remover);
        // index 1
        vars.add(v2);
        assertThrows(ConcurrentModificationException.class, model::isInfeasible, "Modifying the variable list during iteration must cause CME");
    }

    @Test
    void testFlagIsInitiallyFalse() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(getInfeasibleFlag(model), "myInfeasible flag must start as false");
    }
}
