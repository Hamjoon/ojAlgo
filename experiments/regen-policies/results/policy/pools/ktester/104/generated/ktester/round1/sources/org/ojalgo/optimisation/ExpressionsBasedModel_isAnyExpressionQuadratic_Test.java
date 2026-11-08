package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;
import java.util.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyExpressionQuadratic_Test {

    /* ---------------------------------------------------------------------- */
    private static void injectExpressionsMap(ExpressionsBasedModel model, Map<String, Expression> map) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        f.set(model, map);
    }

    /**
     * An empty model has no expressions, therefore the method must return {@code false}.
     */
    @Test
    void testEmptyModelReturnsFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isAnyExpressionQuadratic(), "A model without any expressions should not be quadratic");
    }

    /**
     * Adding only linear expressions (objective or constraint) must not make the model quadratic.
     */
    @Test
    void testOnlyLinearExpressionsReturnsFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Linear objective
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Expression obj = model.newExpression("obj");
        // linear term only
        obj.set(x, BigDecimal.ONE);
        // make it part of the objective
        obj.weight(BigDecimal.ONE);
        // Linear constraint
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Expression cons = model.newExpression("c1");
        cons.set(y, BigDecimal.ONE);
        cons.lower(BigDecimal.ZERO);
        cons.upper(BigDecimal.valueOf(5));
        assertFalse(model.isAnyExpressionQuadratic(), "Model with only linear expressions should not be quadratic");
    }

    /**
     * Adding a quadratic term to an expression that is part of the objective
     * must cause {@code isAnyExpressionQuadratic()} to return {@code true}.
     */
    @Test
    void testQuadraticObjectiveReturnsTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Two variables that will appear in a quadratic term
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Quadratic objective: minimise  x*y
        Expression obj = model.newExpression("quadraticObj");
        // non‑zero quadratic coefficient
        obj.setQuadratic(x, y, BigDecimal.ONE);
        // mark as objective
        obj.weight(BigDecimal.ONE);
        assertTrue(model.isAnyExpressionQuadratic(), "Model with a quadratic objective should be reported as quadratic");
    }

    /**
     * Adding a quadratic term to a non‑redundant constraint must also cause the method
     * to return {@code true}.
     */
    @Test
    void testQuadraticConstraintReturnsTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable b = model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Quadratic constraint:  a*a + b <= 8
        Expression cons = model.newExpression("quadraticCons");
        // a² term
        cons.setQuadratic(a, a, BigDecimal.ONE);
        // linear term
        cons.set(b, BigDecimal.ONE);
        // make it a proper (non‑redundant) constraint
        cons.upper(BigDecimal.valueOf(8));
        assertTrue(model.isAnyExpressionQuadratic(), "Model with a quadratic constraint should be reported as quadratic");
    }

    @Test
    void testQuadraticObjectiveExpression() throws Exception {
        // expression is quadratic, objective, not a constraint
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(true);
        when(expr.isConstraint()).thenReturn(false);
        when(expr.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic objective expression should make the method return true");
    }

    @Test
    void testQuadraticNonRedundantConstraintExpression() throws Exception {
        // quadratic, constraint, non‑redundant
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("c", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic non‑redundant constraint should make the method return true");
    }

    @Test
    void testQuadraticRedundantConstraintExpression() throws Exception {
        // quadratic, constraint, redundant -> should be ignored
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isRedundant()).thenReturn(true);
        Map<String, Expression> map = new HashMap<>();
        map.put("c", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Quadratic redundant constraint must not be counted");
    }

    @Test
    void testQuadraticNonObjectiveNonConstraintExpression() throws Exception {
        // quadratic but neither objective nor constraint
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenReturn(false);
        when(expr.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Quadratic expression that is neither objective nor constraint must not trigger true");
    }

    @Test
    void testNonQuadraticObjectiveExpression() throws Exception {
        // non‑quadratic objective
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        when(expr.isObjective()).thenReturn(true);
        when(expr.isConstraint()).thenReturn(false);
        when(expr.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("obj", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Non‑quadratic objective must not make the method return true");
    }

    @Test
    void testMultipleExpressionsMixed() throws Exception {
        // expr1: linear constraint (non‑quadratic)
        Expression expr1 = mock(Expression.class);
        when(expr1.isAnyQuadraticFactorNonZero()).thenReturn(false);
        when(expr1.isObjective()).thenReturn(false);
        when(expr1.isConstraint()).thenReturn(true);
        when(expr1.isRedundant()).thenReturn(false);
        // expr2: quadratic non‑redundant constraint
        Expression expr2 = mock(Expression.class);
        when(expr2.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr2.isObjective()).thenReturn(false);
        when(expr2.isConstraint()).thenReturn(true);
        when(expr2.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("c1", expr1);
        map.put("c2", expr2);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "At least one quadratic non‑redundant constraint should cause true");
    }

    @Test
    void testQuadraticObjectiveRedundantConstraintExpression() throws Exception {
        // expression is both objective and constraint, redundant flag true,
        // quadratic factor non‑zero -> true because objective condition is enough
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(true);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isRedundant()).thenReturn(true);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic expression that is objective should return true even if redundant constraint");
    }

    @Test
    void testNonQuadraticConstraintExpression() throws Exception {
        // non‑quadratic constraint, non‑redundant
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("c", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Non‑quadratic constraint must not make the method return true");
    }

    @Test
    void testNonQuadraticRedundantConstraintExpression() throws Exception {
        // non‑quadratic, constraint, redundant
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isRedundant()).thenReturn(true);
        Map<String, Expression> map = new HashMap<>();
        map.put("c", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Non‑quadratic redundant constraint must not affect the result");
    }

    @Test
    void testMultipleExpressionsAllNonQuadratic() throws Exception {
        // expr1: quadratic false, objective true
        Expression expr1 = mock(Expression.class);
        when(expr1.isAnyQuadraticFactorNonZero()).thenReturn(false);
        when(expr1.isObjective()).thenReturn(true);
        when(expr1.isConstraint()).thenReturn(false);
        when(expr1.isRedundant()).thenReturn(false);
        // expr2: quadratic false, constraint true, non‑redundant
        Expression expr2 = mock(Expression.class);
        when(expr2.isAnyQuadraticFactorNonZero()).thenReturn(false);
        when(expr2.isObjective()).thenReturn(false);
        when(expr2.isConstraint()).thenReturn(true);
        when(expr2.isRedundant()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("obj", expr1);
        map.put("c", expr2);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "All expressions non‑quadratic should lead to false");
    }

    @Test
    void testMultipleExpressionsQuadraticObjectiveAndRedundantConstraint() throws Exception {
        // expr1: quadratic objective
        Expression expr1 = mock(Expression.class);
        when(expr1.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr1.isObjective()).thenReturn(true);
        when(expr1.isConstraint()).thenReturn(false);
        when(expr1.isRedundant()).thenReturn(false);
        // expr2: quadratic constraint but redundant
        Expression expr2 = mock(Expression.class);
        when(expr2.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr2.isObjective()).thenReturn(false);
        when(expr2.isConstraint()).thenReturn(true);
        when(expr2.isRedundant()).thenReturn(true);
        Map<String, Expression> map = new HashMap<>();
        map.put("obj", expr1);
        map.put("c", expr2);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic objective alone should make the result true even if other constraint is redundant");
    }

    @Test
    void testNullExpressionInMapThrowsNPE() throws Exception {
        Map<String, Expression> map = new HashMap<>();
        // deliberately insert a null value
        map.put("nullExpr", null);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        assertThrows(NullPointerException.class, model::isAnyExpressionQuadratic, "A null entry in the expression map should cause NullPointerException");
    }

    @Test
    void testExpressionIsAnyQuadraticFactorNonZeroThrows() throws Exception {
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenThrow(new RuntimeException("boom"));
        // other methods are irrelevant because the first call already throws
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        RuntimeException thrown = assertThrows(RuntimeException.class, model::isAnyExpressionQuadratic, "Exception from isAnyQuadraticFactorNonZero should be propagated");
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    void testExpressionIsObjectiveThrows() throws Exception {
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenThrow(new RuntimeException("objective failure"));
        // other methods will not be reached
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        RuntimeException thrown = assertThrows(RuntimeException.class, model::isAnyExpressionQuadratic, "Exception from isObjective should be propagated");
        assertEquals("objective failure", thrown.getMessage());
    }

    @Test
    void testExpressionIsConstraintThrows() throws Exception {
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenThrow(new RuntimeException("constraint failure"));
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        RuntimeException thrown = assertThrows(RuntimeException.class, model::isAnyExpressionQuadratic, "Exception from isConstraint should be propagated");
        assertEquals("constraint failure", thrown.getMessage());
    }

    @Test
    void testExpressionIsRedundantThrows() throws Exception {
        Expression expr = mock(Expression.class);
        when(expr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        when(expr.isObjective()).thenReturn(false);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isRedundant()).thenThrow(new RuntimeException("redundant failure"));
        Map<String, Expression> map = new HashMap<>();
        map.put("e", expr);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        injectExpressionsMap(model, map);
        RuntimeException thrown = assertThrows(RuntimeException.class, model::isAnyExpressionQuadratic, "Exception from isRedundant should be propagated");
        assertEquals("redundant failure", thrown.getMessage());
    }

    @Test
    void testConcurrentModificationDuringIteration() throws Exception {
        // Build a real expression that will remove itself from the map when queried.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Expression expr = model.newExpression("e");
        // linear term – not quadratic
        expr.set(v, BigDecimal.ONE);
        // Replace the internal map with a custom one that removes the entry
        // when {@code isAnyQuadraticFactorNonZero()} is called.
        Map<String, Expression> map = new HashMap<>();
        map.put("e", new Expression(v.getModel(), "e") {

            @Override
            public boolean isAnyQuadraticFactorNonZero() {
                // Remove the entry during iteration – this will trigger CME.
                map.remove("e");
                return false;
            }
        });
        injectExpressionsMap(model, map);
        assertThrows(ConcurrentModificationException.class, model::isAnyExpressionQuadratic, "Modifying the expression map during iteration should cause ConcurrentModificationException");
    }
}