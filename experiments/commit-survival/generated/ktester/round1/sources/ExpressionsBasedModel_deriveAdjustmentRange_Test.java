package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.mockito.Mock;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_deriveAdjustmentRange_Test {

    private ExpressionsBasedModel model;

    private Method deriveAdjustmentRangeMethod;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        model = new ExpressionsBasedModel();
        // Obtain a reference to the package‑private method via reflection.
        deriveAdjustmentRangeMethod = ExpressionsBasedModel.class.getDeclaredMethod("deriveAdjustmentRange", Expression.class);
        deriveAdjustmentRangeMethod.setAccessible(true);
        model = new ExpressionsBasedModel();
        // Obtain the package‑private method via reflection.
        deriveAdjustmentRangeMethod = ExpressionsBasedModel.class.getDeclaredMethod("deriveAdjustmentRange", Expression.class);
        deriveAdjustmentRangeMethod.setAccessible(true);
    }

    /**
     * Helper that invokes the private method and returns the int result.
     */
    private int invokeRange(Expression expression) throws Exception {
        return (int) deriveAdjustmentRangeMethod.invoke(model, expression);
    }

    /**
     * An empty expression (no linear or quadratic keys) must yield a range of 0.
     */
    @Test
    void testEmptyExpressionReturnsZero() throws Exception {
        Expression empty = model.newExpression("empty");
        int range = (int) deriveAdjustmentRangeMethod.invoke(model, empty);
        assertEquals(0, range, "Empty expression should have adjustment range 0");
    }

    /**
     * Linear expression where variables have different adjustment exponents.
     * The method should return the maximum absolute exponent.
     */
    @Test
    void testLinearExpressionAdjustmentRange() throws Exception {
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Variable v3 = model.newVariable("v3");
        // Configure adjustment exponents (positive and negative values)
        // |2|
        v1.setAdjustmentExponent(2);
        // |5|
        v2.setAdjustmentExponent(-5);
        // |3|
        v3.setAdjustmentExponent(3);
        Expression expr = model.newExpression("linear");
        expr.set(v1, BigDecimal.ONE);
        expr.set(v2, BigDecimal.ONE);
        // v3 is deliberately omitted to verify that only referenced variables are considered
        int range = (int) deriveAdjustmentRangeMethod.invoke(model, expr);
        assertEquals(5, range, "Maximum absolute adjustment exponent should be 5");
    }

    /**
     * Quadratic expression where both row and column variables have adjustment exponents.
     * The method must examine both dimensions of the quadratic key set.
     */
    @Test
    void testQuadraticExpressionAdjustmentRange() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        // |1|
        a.setAdjustmentExponent(1);
        // |4|
        b.setAdjustmentExponent(-4);
        // |3|
        c.setAdjustmentExponent(3);
        Expression expr = model.newExpression("quadratic");
        // Quadratic term a * b
        expr.set(a, b, BigDecimal.ONE);
        // Linear term c (to ensure mixed linear/quadratic handling)
        expr.set(c, BigDecimal.ONE);
        int range = (int) deriveAdjustmentRangeMethod.invoke(model, expr);
        // The largest absolute exponent among a, b, c is 4 (from b)
        assertEquals(4, range, "Maximum absolute adjustment exponent should be 4");
    }

    @Test
    void testLinearOnlyAdjustmentRangeZero() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        // all exponents are 0 by default
        Expression expr = model.newExpression("lin0");
        expr.set(a, BigDecimal.ONE);
        expr.set(b, BigDecimal.ONE);
        expr.set(c, BigDecimal.ONE);
        assertEquals(0, invokeRange(expr), "All adjustment exponents are 0 → range 0");
    }

    @Test
    void testLinearOnlyAdjustmentRangeFive() throws Exception {
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Variable v3 = model.newVariable("v3");
        // |5|
        v1.setAdjustmentExponent(-5);
        // |3|
        v2.setAdjustmentExponent(3);
        // |5|
        v3.setAdjustmentExponent(5);
        Expression expr = model.newExpression("lin5");
        expr.set(v1, BigDecimal.ONE);
        expr.set(v2, BigDecimal.ONE);
        expr.set(v3, BigDecimal.ONE);
        assertEquals(5, invokeRange(expr), "Maximum absolute exponent should be 5");
    }

    @Test
    void testLinearOnlyAdjustmentRangeNegSeven() throws Exception {
        Variable v = model.newVariable("v");
        v.setAdjustmentExponent(-7);
        Expression expr = model.newExpression("linNeg7");
        expr.set(v, BigDecimal.ONE);
        assertEquals(7, invokeRange(expr), "Absolute value of -7 is 7");
    }

    @Test
    void testQuadraticOnlyAdjustmentRangeZero() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // both exponents default 0
        Expression expr = model.newExpression("quad0");
        expr.set(a, b, BigDecimal.ONE);
        assertEquals(0, invokeRange(expr), "All exponents are 0 → range 0");
    }

    @Test
    void testQuadraticOnlyAdjustmentRangeFour() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        // |4|
        a.setAdjustmentExponent(-4);
        // |2|
        b.setAdjustmentExponent(2);
        // |4|
        c.setAdjustmentExponent(4);
        Expression expr = model.newExpression("quad4");
        // uses a and b
        expr.set(a, b, BigDecimal.ONE);
        // uses b and c
        expr.set(b, c, BigDecimal.ONE);
        assertEquals(4, invokeRange(expr), "Maximum absolute exponent should be 4");
    }

    @Test
    void testQuadraticOnlyAdjustmentRangeNegNine() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // |9|
        a.setAdjustmentExponent(-9);
        // b stays at 0
        Expression expr = model.newExpression("quadNeg9");
        // a appears as both row and column
        expr.set(a, a, BigDecimal.ONE);
        expr.set(a, b, BigDecimal.ONE);
        assertEquals(9, invokeRange(expr), "Maximum absolute exponent should be 9");
    }

    @Test
    void testMixedLinearQuadraticAdjustmentRangeThree() throws Exception {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        Variable z = model.newVariable("z");
        x.setAdjustmentExponent(1);
        y.setAdjustmentExponent(-2);
        z.setAdjustmentExponent(3);
        Expression expr = model.newExpression("mixed3");
        // linear exponent 1
        expr.set(x, BigDecimal.ONE);
        // linear exponent -2
        expr.set(y, BigDecimal.ONE);
        // quadratic uses z (row) exponent 3 and y (col) exponent -2
        expr.set(z, y, BigDecimal.ONE);
        assertEquals(3, invokeRange(expr), "Maximum absolute exponent should be 3");
    }

    @Test
    void testMixedLinearQuadraticAdjustmentRangeEight() throws Exception {
        Variable v = model.newVariable("v");
        Variable w = model.newVariable("w");
        Variable u = model.newVariable("u");
        // |8|
        v.setAdjustmentExponent(-8);
        w.setAdjustmentExponent(2);
        u.setAdjustmentExponent(5);
        Expression expr = model.newExpression("mixed8");
        // linear -8
        expr.set(v, BigDecimal.ONE);
        // quadratic max 5
        expr.set(w, u, BigDecimal.ONE);
        assertEquals(8, invokeRange(expr), "Linear part dominates with exponent 8");
    }

    @Test
    void testMixedLinearQuadraticAdjustmentRangeTen() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        a.setAdjustmentExponent(4);
        // quadratic row
        b.setAdjustmentExponent(10);
        c.setAdjustmentExponent(3);
        Expression expr = model.newExpression("mixed10");
        // linear 4
        expr.set(a, BigDecimal.ONE);
        // quadratic row b exponent 10
        expr.set(b, c, BigDecimal.ONE);
        assertEquals(10, invokeRange(expr), "Quadratic part provides exponent 10");
    }

    @Test
    void testDuplicateVariableReferencesAdjustmentRangeSix() throws Exception {
        Variable v = model.newVariable("v");
        v.setAdjustmentExponent(6);
        Expression expr = model.newExpression("dup6");
        // linear
        expr.set(v, BigDecimal.ONE);
        // quadratic (row and column both v)
        expr.set(v, v, BigDecimal.ONE);
        // another quadratic with same variable
        expr.set(v, v, BigDecimal.ONE);
        assertEquals(6, invokeRange(expr), "Duplicate references must still yield max 6");
    }

    @Test
    void testMixedPositiveNegativeAdjustmentRangeTwelve() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        Variable d = model.newVariable("d");
        Variable e = model.newVariable("e");
        a.setAdjustmentExponent(-12);
        b.setAdjustmentExponent(5);
        c.setAdjustmentExponent(3);
        d.setAdjustmentExponent(-8);
        e.setAdjustmentExponent(7);
        Expression expr = model.newExpression("mixed12");
        expr.set(a, BigDecimal.ONE);
        expr.set(b, BigDecimal.ONE);
        expr.set(c, BigDecimal.ONE);
        // quadratic uses -8 and 7
        expr.set(d, e, BigDecimal.ONE);
        assertEquals(12, invokeRange(expr), "Largest absolute exponent is 12");
    }

    @Test
    void testQuadraticSingleNegativeAdjustmentRangeFifteen() throws Exception {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        // all zero except one
        a.setAdjustmentExponent(0);
        b.setAdjustmentExponent(-15);
        c.setAdjustmentExponent(0);
        Expression expr = model.newExpression("quadNeg15");
        // uses b with -15
        expr.set(a, b, BigDecimal.ONE);
        expr.set(b, c, BigDecimal.ONE);
        assertEquals(15, invokeRange(expr), "Single negative exponent -15 → range 15");
    }

    @Test
    void testLinearLargeAdjustmentExponentThousand() throws Exception {
        Variable v = model.newVariable("v");
        v.setAdjustmentExponent(1000);
        Expression expr = model.newExpression("lin1000");
        expr.set(v, BigDecimal.ONE);
        assertEquals(1000, invokeRange(expr), "Very large exponent should be handled correctly");
    }

    @Test
    void testEmptyVariablesButKeysReturnsZero() throws Exception {
        // Model has no variables at all.
        // Mock an expression that reports empty key sets (so the loops are entered but contain nothing).
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Collections.emptySet());
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Collections.emptySet());
        assertEquals(0, invokeRange(mockExpr), "With no variables in model, range should be 0");
    }

    @Test
    void testNullExpressionThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> invokeRange(null), "Passing null should throw NullPointerException");
    }

    @Test
    void testLinearKeySetNullThrowsNullPointerException() {
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(null);
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Collections.emptySet());
        assertThrows(NullPointerException.class, () -> invokeRange(mockExpr), "Null linear key set should cause NPE");
    }

    @Test
    void testQuadraticKeySetNullThrowsNullPointerException() {
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Collections.emptySet());
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> invokeRange(mockExpr), "Null quadratic key set should cause NPE");
    }

    @Test
    void testLinearKeyOutOfBoundsThrowsIndexOutOfBoundsException() {
        // Model has only two variables (indices 0 and 1)
        model.newVariable("v0");
        model.newVariable("v1");
        // Mock expression with a linear key referencing index 5 (out of bounds)
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        Set<IntIndex> linearSet = new HashSet<>();
        linearSet.add(new IntIndex(5));
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(linearSet);
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Collections.emptySet());
        assertThrows(IndexOutOfBoundsException.class, () -> invokeRange(mockExpr), "Out‑of‑bounds linear index should trigger IndexOutOfBoundsException");
    }

    @Test
    void testQuadraticRowOutOfBoundsThrowsIndexOutOfBoundsException() {
        // Model has a single variable at index 0
        model.newVariable("v0");
        // Mock expression with quadratic key having row index 3 (out of bounds)
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        Set<IntRowColumn> quadSet = new HashSet<>();
        quadSet.add(new IntRowColumn(3, 0));
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Collections.emptySet());
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(quadSet);
        assertThrows(IndexOutOfBoundsException.class, () -> invokeRange(mockExpr), "Out‑of‑bounds quadratic row should trigger IndexOutOfBoundsException");
    }

    @Test
    void testQuadraticColumnOutOfBoundsThrowsIndexOutOfBoundsException() {
        // Model has a single variable at index 0
        model.newVariable("v0");
        // Mock expression with quadratic key having column index 2 (out of bounds)
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        Set<IntRowColumn> quadSet = new HashSet<>();
        quadSet.add(new IntRowColumn(0, 2));
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Collections.emptySet());
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(quadSet);
        assertThrows(IndexOutOfBoundsException.class, () -> invokeRange(mockExpr), "Out‑of‑bounds quadratic column should trigger IndexOutOfBoundsException");
    }

    @Test
    void testVariableEntryNullInModelThrowsNullPointerException() throws Exception {
        // Use reflection to insert a null entry into the private myVariables list.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Variable> vars = (java.util.List<Variable>) varsField.get(model);
        // index 0 will be null
        vars.add(null);
        // Add a normal variable at index 1 for completeness.
        Variable v1 = model.newVariable("v1");
        // Create expression that references the null variable (index 0).
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        Set<IntIndex> linearSet = new HashSet<>();
        // points to the null entry
        linearSet.add(new IntIndex(0));
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(linearSet);
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Collections.emptySet());
        assertThrows(NullPointerException.class, () -> invokeRange(mockExpr), "Null variable in model should cause NullPointerException");
    }

    @Test
    void testNullLinearKeyElementThrowsNullPointerException() {
        // Model has at least one variable so that the loop is entered.
        model.newVariable("v0");
        // Mock expression with a linear key set containing a null element.
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        Set<IntIndex> linearSet = new HashSet<>();
        // null element
        linearSet.add(null);
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(linearSet);
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Collections.emptySet());
        assertThrows(NullPointerException.class, () -> invokeRange(mockExpr), "Null element inside linear key set should cause NullPointerException");
    }

    @Test
    void testNullQuadraticKeyElementThrowsNullPointerException() {
        // Model has at least one variable.
        model.newVariable("v0");
        // Mock expression with a quadratic key set containing a null element.
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        Set<IntRowColumn> quadSet = new HashSet<>();
        // null element
        quadSet.add(null);
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Collections.emptySet());
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(quadSet);
        assertThrows(NullPointerException.class, () -> invokeRange(mockExpr), "Null element inside quadratic key set should cause NullPointerException");
    }
}
