package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.function.constant.BigMath;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_deriveAdjustmentRange_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    private static Expression mockExpression(Set<IntIndex> linear, Set<IntRowColumn> quadratic) {
        Expression expr = mock(Expression.class);
        when(expr.getLinearKeySet()).thenReturn(linear);
        when(expr.getQuadraticKeySet()).thenReturn(quadratic);
        return expr;
    }

    private static Expression mockExpressionNullLinear(Set<IntRowColumn> quadratic) {
        Expression expr = mock(Expression.class);
        when(expr.getLinearKeySet()).thenReturn(null);
        when(expr.getQuadraticKeySet()).thenReturn(quadratic);
        return expr;
    }

    private static Expression mockExpressionNullQuadratic(Set<IntIndex> linear) {
        Expression expr = mock(Expression.class);
        when(expr.getLinearKeySet()).thenReturn(linear);
        when(expr.getQuadraticKeySet()).thenReturn(null);
        return expr;
    }

    private static Stream<Arguments> provideNormalCases() {
        // convenience factories for index objects
        Consumer<ExpressionsBasedModel> NO_SETUP = m -> {
        };
        // ---------- group: deriveAdjustmentRange ----------
        // 1. empty expression
        Expression emptyExpr = mockExpression(Collections.emptySet(), Collections.emptySet());
        // 2. single linear variable, exponent 3
        Expression linPos = mockExpression(Set.of(new IntIndex(0)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> linPosSetup = m -> m.addVariable().setAdjustmentExponent(3);
        // 3. single linear variable, exponent -5
        Expression linNeg = mockExpression(Set.of(new IntIndex(1)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> linNegSetup = m -> {
            // index 0 (unused)
            m.addVariable();
            m.addVariable().setAdjustmentExponent(-5);
        };
        // 4. quadratic only (0,1) with exponents 2 and -4
        Expression quadOnly = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(0, 1)));
        Consumer<ExpressionsBasedModel> quadOnlySetup = m -> {
            // index 0
            m.addVariable().setAdjustmentExponent(2);
            // index 1
            m.addVariable().setAdjustmentExponent(-4);
        };
        // 5. mixed linear (2) and quadratic (3,4) with exponents 6,3,-8
        Expression mixed = mockExpression(Set.of(new IntIndex(2)), Set.of(new IntRowColumn(3, 4)));
        Consumer<ExpressionsBasedModel> mixedSetup = m -> {
            // indices 0‑4
            for (int i = 0; i < 5; i++) {
                m.addVariable();
            }
            m.getVariable(2).setAdjustmentExponent(6);
            m.getVariable(3).setAdjustmentExponent(3);
            m.getVariable(4).setAdjustmentExponent(-8);
        };
        // 6. multiple linear variables (5,-2) and (6,7)
        Expression multiLin = mockExpression(Set.of(new IntIndex(5), new IntIndex(6)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> multiLinSetup = m -> {
            for (int i = 0; i <= 6; i++) {
                m.addVariable();
            }
            m.getVariable(5).setAdjustmentExponent(-2);
            m.getVariable(6).setAdjustmentExponent(7);
        };
        // ---------- group: deriveAdjustmentRange_additional ----------
        // 7. multiple quadratic terms (0,1) (2,3) with exponents 1,-2,5,-6
        Expression multiQuad = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(0, 1), new IntRowColumn(2, 3)));
        Consumer<ExpressionsBasedModel> multiQuadSetup = m -> {
            for (int i = 0; i <= 3; i++) {
                m.addVariable();
            }
            m.getVariable(0).setAdjustmentExponent(1);
            m.getVariable(1).setAdjustmentExponent(-2);
            m.getVariable(2).setAdjustmentExponent(5);
            m.getVariable(3).setAdjustmentExponent(-6);
        };
        // 8. linear variables with a zero exponent
        Expression linZero = mockExpression(Set.of(new IntIndex(0), new IntIndex(1)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> linZeroSetup = m -> {
            m.addVariable().setAdjustmentExponent(0);
            m.addVariable().setAdjustmentExponent(4);
        };
        // 9. quadratic term where row == column (4,4) exponent -9
        Expression quadSelf = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(4, 4)));
        Consumer<ExpressionsBasedModel> quadSelfSetup = m -> {
            for (int i = 0; i <= 4; i++) {
                m.addVariable();
            }
            m.getVariable(4).setAdjustmentExponent(-9);
        };
        // 10. all exponents Integer.MIN_VALUE – Math.abs overflows, result should stay 0
        Expression allMin = mockExpression(Set.of(new IntIndex(0)), Set.of(new IntRowColumn(1, 2)));
        Consumer<ExpressionsBasedModel> allMinSetup = m -> {
            for (int i = 0; i <= 2; i++) {
                m.addVariable();
            }
            m.getVariable(0).setAdjustmentExponent(Integer.MIN_VALUE);
            m.getVariable(1).setAdjustmentExponent(Integer.MIN_VALUE);
            m.getVariable(2).setAdjustmentExponent(Integer.MIN_VALUE);
        };
        // 11. max exponent appears both linearly and quadratically (8)
        Expression maxBoth = mockExpression(Set.of(new IntIndex(5)), Set.of(new IntRowColumn(6, 7)));
        Consumer<ExpressionsBasedModel> maxBothSetup = m -> {
            for (int i = 0; i <= 7; i++) {
                m.addVariable();
            }
            m.getVariable(5).setAdjustmentExponent(8);
            m.getVariable(6).setAdjustmentExponent(-8);
            m.getVariable(7).setAdjustmentExponent(3);
        };
        // 12. many linear variables, highest absolute exponent 10
        Expression manyLin = mockExpression(Set.of(new IntIndex(0), new IntIndex(1), new IntIndex(2), new IntIndex(3), new IntIndex(4)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> manyLinSetup = m -> {
            for (int i = 0; i <= 4; i++) {
                m.addVariable();
            }
            m.getVariable(0).setAdjustmentExponent(1);
            m.getVariable(1).setAdjustmentExponent(2);
            m.getVariable(2).setAdjustmentExponent(3);
            m.getVariable(3).setAdjustmentExponent(10);
            m.getVariable(4).setAdjustmentExponent(5);
        };
        // ---------- group: additional edge cases ----------
        // 13. linearKeySet null (should NPE – handled in exception group, not here)
        // 14. quadraticKeySet null (handled in exception group)
        return Stream.of(// group: deriveAdjustmentRange
        Arguments.of(emptyExpr, NO_SETUP, 0, "When the expression contains no linear nor quadratic terms the method should return 0."), Arguments.of(linPos, linPosSetup, 3, "A single linear variable with a positive adjustment exponent."), Arguments.of(linNeg, linNegSetup, 5, "A single linear variable with a negative adjustment exponent."), Arguments.of(quadOnly, quadOnlySetup, 4, "Only quadratic terms are present – maximum absolute exponent is 4."), Arguments.of(mixed, mixedSetup, 8, "Combination of linear and quadratic terms – overall maximum absolute exponent is 8."), Arguments.of(multiLin, multiLinSetup, 7, "Multiple linear variables – greatest absolute exponent is 7."), // group: deriveAdjustmentRange_additional
        Arguments.of(multiQuad, multiQuadSetup, 6, "Multiple quadratic terms – greatest absolute exponent is 6."), Arguments.of(linZero, linZeroSetup, 4, "Linear variables include a zero exponent – maximum absolute exponent is 4."), Arguments.of(quadSelf, quadSelfSetup, 9, "Quadratic term where row and column refer to the same variable – exponent 9."), Arguments.of(allMin, allMinSetup, 0, "All adjustment exponents are Integer.MIN_VALUE – result stays 0."), Arguments.of(maxBoth, maxBothSetup, 8, "Maximum absolute exponent appears in both linear and quadratic parts – result 8."), Arguments.of(manyLin, manyLinSetup, 10, "Many linear variables – highest absolute exponent is 10."));
    }

    private static Stream<Arguments> provideExceptionCases() {
        // 1. null expression
        Arguments a1 = Arguments.of(null, null, NullPointerException.class, "Calling deriveAdjustmentRange with a null expression should throw NullPointerException.");
        // 2. linear key set is null
        Expression linNull = mockExpressionNullLinear(Collections.emptySet());
        Arguments a2 = Arguments.of(linNull, null, NullPointerException.class, "If the expression's linear key set is null, a NullPointerException is expected.");
        // 3. quadratic key set is null
        Expression quadNull = mockExpressionNullQuadratic(Collections.emptySet());
        Arguments a3 = Arguments.of(quadNull, null, NullPointerException.class, "If the expression's quadratic key set is null, a NullPointerException is expected.");
        // 4. linear index out of bounds (model has only 5 variables, index 10 used)
        Expression linOob = mockExpression(Set.of(new IntIndex(10)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> linOobSetup = m -> {
            for (int i = 0; i < 5; i++) {
                m.addVariable();
            }
        };
        Arguments a4 = Arguments.of(linOob, linOobSetup, IndexOutOfBoundsException.class, "Linear index beyond the size of myVariables should cause IndexOutOfBoundsException.");
        // 5. quadratic row out of bounds
        Expression quadRowOob = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(8, 2)));
        Consumer<ExpressionsBasedModel> quadRowOobSetup = m -> {
            for (int i = 0; i < 5; i++) {
                m.addVariable();
            }
        };
        Arguments a5 = Arguments.of(quadRowOob, quadRowOobSetup, IndexOutOfBoundsException.class, "Quadratic row index outside variable list bounds should cause IndexOutOfBoundsException.");
        // 6. quadratic column out of bounds
        Expression quadColOob = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(1, 9)));
        Consumer<ExpressionsBasedModel> quadColOobSetup = m -> {
            for (int i = 0; i < 5; i++) {
                m.addVariable();
            }
        };
        Arguments a6 = Arguments.of(quadColOob, quadColOobSetup, IndexOutOfBoundsException.class, "Quadratic column index outside variable list bounds should cause IndexOutOfBoundsException.");
        // 7. myVariables entry is null for a linear index
        Expression linNullVar = mockExpression(Set.of(new IntIndex(2)), Collections.emptySet());
        Consumer<ExpressionsBasedModel> linNullVarSetup = m -> {
            for (int i = 0; i <= 2; i++) {
                m.addVariable();
            }
            // replace variable at index 2 with null via reflection
            try {
                Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                ArrayList<Variable> list = (ArrayList<Variable>) f.get(m);
                list.set(2, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Arguments a7 = Arguments.of(linNullVar, linNullVarSetup, NullPointerException.class, "Null variable at a linear index should cause NullPointerException.");
        // 8. myVariables entry is null for a quadratic row variable
        Expression quadRowNullVar = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(3, 4)));
        Consumer<ExpressionsBasedModel> quadRowNullVarSetup = m -> {
            for (int i = 0; i <= 4; i++) {
                m.addVariable();
            }
            try {
                Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                ArrayList<Variable> list = (ArrayList<Variable>) f.get(m);
                list.set(3, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Arguments a8 = Arguments.of(quadRowNullVar, quadRowNullVarSetup, NullPointerException.class, "Null variable at the row position of a quadratic term should cause NullPointerException.");
        // 9. myVariables entry is null for a quadratic column variable
        Expression quadColNullVar = mockExpression(Collections.emptySet(), Set.of(new IntRowColumn(3, 4)));
        Consumer<ExpressionsBasedModel> quadColNullVarSetup = m -> {
            for (int i = 0; i <= 4; i++) {
                m.addVariable();
            }
            try {
                Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                ArrayList<Variable> list = (ArrayList<Variable>) f.get(m);
                list.set(4, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Arguments a9 = Arguments.of(quadColNullVar, quadColNullVarSetup, NullPointerException.class, "Null variable at the column position of a quadratic term should cause NullPointerException.");
        // 10. linear key set contains a null IntIndex
        Set<IntIndex> linWithNull = new HashSet<>();
        linWithNull.add(null);
        Expression linSetNullElement = mockExpression(linWithNull, Collections.emptySet());
        Arguments a10 = Arguments.of(linSetNullElement, null, NullPointerException.class, "A null IntIndex inside the linear key set should cause NullPointerException.");
        // 11. quadratic key set contains a null IntRowColumn
        Set<IntRowColumn> quadWithNull = new HashSet<>();
        quadWithNull.add(null);
        Expression quadSetNullElement = mockExpression(Collections.emptySet(), quadWithNull);
        Arguments a11 = Arguments.of(quadSetNullElement, null, NullPointerException.class, "A null IntRowColumn inside the quadratic key set should cause NullPointerException.");
        return Stream.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
    }

    /**
     * Verify that the method returns the maximum absolute adjustment exponent of all variables
     * referenced linearly or quadratically in the expression.
     */
    @Test
    void testDeriveAdjustmentRangeWithLinearAndQuadraticTerms() {
        // Create three variables with distinct adjustment exponents
        Variable v0 = model.addVariable().setName("v0").setAdjustmentExponent(2);
        Variable v1 = model.addVariable().setName("v1").setAdjustmentExponent(-3);
        Variable v2 = model.addVariable().setName("v2").setAdjustmentExponent(5);
        // Build an expression that references the variables both linearly and quadratically
        Expression expr = model.addExpression();
        // linear term with v0
        expr.set(v0, BigMath.ONE);
        // linear term with v1
        expr.set(v1, BigMath.ONE);
        // quadratic term (v0 * v2)
        expr.set(v0, v2, BigMath.ONE);
        // The expected maximum absolute exponent is 5 (from v2)
        int expected = 5;
        int actual = model.deriveAdjustmentRange(expr);
        assertEquals(expected, actual, "Maximum absolute adjustment exponent should be 5");
    }

    /**
     * When the expression does not reference any variable the adjustment range must be zero.
     */
    @Test
    void testDeriveAdjustmentRangeWithEmptyExpression() {
        // no variables added
        Expression empty = model.addExpression();
        assertEquals(0, model.deriveAdjustmentRange(empty), "Adjustment range of an empty expression must be 0");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideNormalCases")
    void testDeriveAdjustmentRange_normal(Expression expression, Consumer<ExpressionsBasedModel> modelSetup, int expected, String description) {
        // configure the model (add variables, set exponents, possibly inject nulls)
        if (modelSetup != null) {
            modelSetup.accept(model);
        }
        int actual = model.deriveAdjustmentRange(expression);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideExceptionCases")
    void testDeriveAdjustmentRange_exceptions(Expression expression, Consumer<ExpressionsBasedModel> modelSetup, Class<? extends Throwable> expectedException, String description) {
        if (modelSetup != null) {
            modelSetup.accept(model);
        }
        assertThrows(expectedException, () -> model.deriveAdjustmentRange(expression), description);
    }
}
