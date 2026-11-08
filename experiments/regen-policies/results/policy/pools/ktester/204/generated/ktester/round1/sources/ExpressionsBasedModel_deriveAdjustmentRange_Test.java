package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_deriveAdjustmentRange_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
        model = new ExpressionsBasedModel();
    }

    /**
     * Creates a mock {@link Expression} that returns the supplied key sets.
     */
    private Expression mockExpression(Set<IntIndex> linear, Set<IntRowColumn> quadratic) {
        Expression expr = mock(Expression.class);
        when(expr.getLinearKeySet()).thenReturn(linear);
        when(expr.getQuadraticKeySet()).thenReturn(quadratic);
        return expr;
    }

    /**
     * Inserts a {@code null} into the private {@code myVariables} list at the given index.
     */
    private void injectNullVariableAt(int index) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.ArrayList<Variable> list = (java.util.ArrayList<Variable>) f.get(model);
        // Ensure the list is large enough
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, null);
    }

    /**
     * When an expression contains no variables the adjustment range must be zero.
     */
    @Test
    void testEmptyExpression() {
        Expression expr = mockExpression(Collections.emptySet(), Collections.emptySet());
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(0, range, "Empty expression should yield range 0");
    }

    /**
     * Linear expression – the range is the absolute adjustment exponent of the single variable.
     */
    @Test
    void testLinearExpression() {
        Variable v = model.newVariable("v").weight(BigMath.ONE);
        // set a non‑default adjustment exponent
        v.setAdjustmentExponent(3);
        Expression expr = model.newExpression("lin");
        // linear term
        expr.add(v, BigMath.ONE);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(3, range, "Linear expression should return the variable's exponent");
    }

    /**
     * Quadratic expression – the range is the maximum absolute exponent of the two variables.
     */
    @Test
    void testQuadraticExpression() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // exponent -4
        a.setAdjustmentExponent(-4);
        // exponent  2
        b.setAdjustmentExponent(2);
        Expression expr = model.newExpression("quad");
        // create a quadratic term a * b
        expr.set(a, b, BigMath.ONE);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(4, range, "Quadratic expression should return max absolute exponent");
    }

    /**
     * Mixed linear & quadratic – the method must consider all variables and return the largest
     * absolute exponent.
     */
    @Test
    void testMixedExpression() {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        Variable z = model.newVariable("z");
        x.setAdjustmentExponent(1);
        // largest absolute value
        y.setAdjustmentExponent(-7);
        z.setAdjustmentExponent(3);
        Expression expr = model.newExpression("mixed");
        // linear part uses x
        expr.add(x, BigMath.ONE);
        // quadratic part uses y and z
        expr.set(y, z, BigMath.ONE);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(7, range, "Mixed expression should return the greatest absolute exponent");
    }

    /**
     * Verify that the method works when the same variable appears both linearly and quadratically.
     */
    @Test
    void testVariableAppearsMultipleTimes() {
        Variable v = model.newVariable("v");
        v.setAdjustmentExponent(-5);
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(0));
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(0, 0));
        Expression expr = mockExpression(linear, quad);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(5, range, "Variable appearing both linearly and quadratically should yield its exponent");
    }

    @Test
    void testLinearTermsOnly() {
        // variable0 exponent = 2, variable1 exponent = -5 -> max abs = 5
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        v0.setAdjustmentExponent(2);
        v1.setAdjustmentExponent(-5);
        Set<IntIndex> linear = new HashSet<>();
        linear.add(model.toIntIndex(0));
        linear.add(model.toIntIndex(1));
        Expression expr = mockExpression(linear, Collections.emptySet());
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(5, range, "Maximum absolute exponent among linear variables should be 5");
    }

    @Test
    void testLinearSingleZeroExponent() {
        Variable v2 = model.newVariable("v2");
        v2.setAdjustmentExponent(0);
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(2));
        Expression expr = mockExpression(linear, Collections.emptySet());
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(0, range, "Single linear variable with exponent 0 should yield range 0");
    }

    @Test
    void testQuadraticTermsOnly() {
        // variable0 exponent = -3, variable1 exponent = 4 -> max abs = 4
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        v0.setAdjustmentExponent(-3);
        v1.setAdjustmentExponent(4);
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(0, 1));
        Expression expr = mockExpression(Collections.emptySet(), quad);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(4, range, "Maximum absolute exponent among quadratic variables should be 4");
    }

    @Test
    void testQuadraticSameVariable() {
        Variable v2 = model.newVariable("v2");
        v2.setAdjustmentExponent(-7);
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(2, 2));
        Expression expr = mockExpression(Collections.emptySet(), quad);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(7, range, "Quadratic term with same variable should use its absolute exponent");
    }

    @Test
    void testMixedLinearAndQuadratic() {
        // variable0 exponent = 1, variable1 exponent = -8, variable2 exponent = 3 -> max abs = 8
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        v0.setAdjustmentExponent(1);
        v1.setAdjustmentExponent(-8);
        v2.setAdjustmentExponent(3);
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(0));
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(1, 2));
        Expression expr = mockExpression(linear, quad);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(8, range, "Mixed expression should return the greatest absolute exponent");
    }

    @Test
    void testMixedAnotherScenario() {
        // variable3 exponent = 6, variable4 exponent = -2, variable5 exponent = 9 -> max abs = 9
        // filler
        model.newVariable("v0");
        model.newVariable("v1");
        model.newVariable("v2");
        Variable v3 = model.newVariable("v3");
        Variable v4 = model.newVariable("v4");
        Variable v5 = model.newVariable("v5");
        v3.setAdjustmentExponent(6);
        v4.setAdjustmentExponent(-2);
        v5.setAdjustmentExponent(9);
        Set<IntIndex> linear = new HashSet<>();
        linear.add(model.toIntIndex(3));
        linear.add(model.toIntIndex(4));
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(4, 5));
        Expression expr = mockExpression(linear, quad);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(9, range, "Variable appearing only in quadratic part can dominate the range");
    }

    @Test
    void testOutOfBoundsLinearIndex() {
        // Model has 5 variables (indices 0‑4)
        for (int i = 0; i < 5; i++) {
            model.newVariable("v" + i);
        }
        // out of range
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(10));
        Expression expr = mockExpression(linear, Collections.emptySet());
        assertThrows(IndexOutOfBoundsException.class, () -> model.deriveAdjustmentRange(expr), "Accessing a non‑existent linear variable should throw IndexOutOfBoundsException");
    }

    @Test
    void testOutOfBoundsQuadraticIndex() {
        for (int i = 0; i < 5; i++) {
            model.newVariable("v" + i);
        }
        // column out of range
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(2, 8));
        Expression expr = mockExpression(Collections.emptySet(), quad);
        assertThrows(IndexOutOfBoundsException.class, () -> model.deriveAdjustmentRange(expr), "Out‑of‑bounds quadratic index should throw IndexOutOfBoundsException");
    }

    @Test
    void testExtremeAdjustmentExponent_MinValue() {
        Variable v = model.newVariable("v");
        // -2147483648
        v.setAdjustmentExponent(Integer.MIN_VALUE);
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(0));
        Expression expr = mockExpression(linear, Collections.emptySet());
        int range = model.deriveAdjustmentRange(expr);
        // Due to overflow Math.abs(Integer.MIN_VALUE) == Integer.MIN_VALUE (negative),
        // Math.max(0, Integer.MIN_VALUE) yields 0. The test documents this behaviour.
        assertEquals(0, range, "Overflow of Math.abs on Integer.MIN_VALUE results in 0 as the maximum range");
    }

    @Test
    void testExtremeAdjustmentExponent_MaxValue() {
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        v1.setAdjustmentExponent(2147483647);
        v2.setAdjustmentExponent(-2147483647);
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(1, 2));
        Expression expr = mockExpression(Collections.emptySet(), quad);
        int range = model.deriveAdjustmentRange(expr);
        assertEquals(2147483647, range, "Maximum absolute exponent fitting in int range should be returned");
    }

    @Test
    void testEmptyModelWithNonEmptyExpression() {
        // No variables added to the model
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(0));
        Expression expr = mockExpression(linear, Collections.emptySet());
        assertThrows(IndexOutOfBoundsException.class, () -> model.deriveAdjustmentRange(expr), "Empty model but expression references index 0 should throw IndexOutOfBoundsException");
    }

    @Test
    void testNullExpression() {
        assertThrows(NullPointerException.class, () -> model.deriveAdjustmentRange(null), "Passing null expression should result in NullPointerException");
    }

    @Test
    void testNullLinearKeySet() {
        Expression expr = mock(Expression.class);
        when(expr.getLinearKeySet()).thenReturn(null);
        when(expr.getQuadraticKeySet()).thenReturn(Collections.emptySet());
        assertThrows(NullPointerException.class, () -> model.deriveAdjustmentRange(expr), "Null linear key set should cause NullPointerException");
    }

    @Test
    void testNullQuadraticKeySet() {
        Expression expr = mock(Expression.class);
        when(expr.getLinearKeySet()).thenReturn(Collections.emptySet());
        when(expr.getQuadraticKeySet()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> model.deriveAdjustmentRange(expr), "Null quadratic key set should cause NullPointerException");
    }

    @Test
    void testVariableNullInModel() throws Exception {
        // Ensure the variable list has at least one entry (null)
        injectNullVariableAt(0);
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(0));
        Expression expr = mockExpression(linear, Collections.emptySet());
        assertThrows(NullPointerException.class, () -> model.deriveAdjustmentRange(expr), "Null variable in model should cause NullPointerException when accessed");
    }

    @Test
    void testNegativeLinearIndex() {
        // Model with at least one variable
        model.newVariable("v0");
        Set<IntIndex> linear = Collections.singleton(model.toIntIndex(-1));
        Expression expr = mockExpression(linear, Collections.emptySet());
        assertThrows(IndexOutOfBoundsException.class, () -> model.deriveAdjustmentRange(expr), "Negative linear index should throw IndexOutOfBoundsException");
    }

    @Test
    void testNegativeQuadraticColumnIndex() {
        // Model with enough variables
        for (int i = 0; i < 4; i++) {
            model.newVariable("v" + i);
        }
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(2, -3));
        Expression expr = mockExpression(Collections.emptySet(), quad);
        assertThrows(IndexOutOfBoundsException.class, () -> model.deriveAdjustmentRange(expr), "Negative quadratic column index should throw IndexOutOfBoundsException");
    }

    @Test
    void testNegativeQuadraticRowIndex() {
        for (int i = 0; i < 2; i++) {
            model.newVariable("v" + i);
        }
        Set<IntRowColumn> quad = Collections.singleton(model.toIntRowColumn(-5, 1));
        Expression expr = mockExpression(Collections.emptySet(), quad);
        assertThrows(IndexOutOfBoundsException.class, () -> model.deriveAdjustmentRange(expr), "Negative quadratic row index should throw IndexOutOfBoundsException");
    }
}
