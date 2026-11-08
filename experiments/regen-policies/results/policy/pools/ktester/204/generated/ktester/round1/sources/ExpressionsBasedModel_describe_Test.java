package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

class ExpressionsBasedModel_describe_Test {

    /**
     * Asserts that the supplied {@link Description} matches the expected counters.
     */
    private static void assertDescription(ExpressionsBasedModel.Description actual, int nbVariables, int nbPositive, int nbNegative, int nbInteger, int nbLoBound, int nbUpBound, int nbEqBound, int nbLoConstraint, int nbUpConstraint, int nbEqConstraint) {
        assertAll(() -> assertEquals(nbVariables, actual.nbVariables, "nbVariables"), () -> assertEquals(nbPositive, actual.nbPositiveVariables, "nbPositiveVariables"), () -> assertEquals(nbNegative, actual.nbNegativeVariables, "nbNegativeVariables"), () -> assertEquals(nbInteger, actual.nbIntegerVariables, "nbIntegerVariables"), () -> assertEquals(nbLoBound, getIntField(actual, "nbLoBound"), "nbLoBound"), () -> assertEquals(nbUpBound, getIntField(actual, "nbUpBound"), "nbUpBound"), () -> assertEquals(nbEqBound, getIntField(actual, "nbEqBound"), "nbEqBound"), () -> assertEquals(nbLoConstraint, getIntField(actual, "nbLoConstraint"), "nbLoConstraint"), () -> assertEquals(nbUpConstraint, getIntField(actual, "nbUpConstraint"), "nbUpConstraint"), () -> assertEquals(nbEqConstraint, getIntField(actual, "nbEqConstraint"), "nbEqConstraint"));
    }

    /**
     * Inserts a {@code null} entry into the private {@code myVariables} list of the model.
     */
    private static void injectNullVariable(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) f.get(model);
        list.add(null);
    }

    /**
     * Inserts a {@code null} entry into the private {@code myExpressions} map of the model.
     */
    private static void injectNullExpression(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) f.get(model);
        map.put("nullExpr", null);
    }

    /**
     * Replaces the first variable in the model with the supplied mock (via reflection).
     */
    private static void replaceFirstVariableWithMock(ExpressionsBasedModel model, Variable mock) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) f.get(model);
        list.set(0, mock);
    }

    /**
     * Helper to read an int field (even if it is not public) via reflection.
     */
    private static int getIntField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (int) f.get(obj);
        } catch (Exception e) {
            throw new AssertionError("Unable to read field '" + fieldName + "' from " + obj, e);
        }
    }

    /**
     * Verify that {@link ExpressionsBasedModel#describe()} correctly counts
     * variables, integer variables, bounds on variables and constraints on
     * expressions.
     */
    @Test
    void testDescribeCounts() {
        // GIVEN – a model with a mixture of variable and expression constraints
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variables:
        Variable varLo = model.newVariable("Lo").lower(0);
        Variable varUp = model.newVariable("Up").upper(10);
        Variable varEq = model.newVariable("Eq").level(5);
        // Expressions (constraints):
        Expression exprLo = model.newExpression("exprLo").lower(3);
        Expression exprUp = model.newExpression("exprUp").upper(7);
        Expression exprEq = model.newExpression("exprEq").level(4);
        // WHEN – we request a description of the model
        ExpressionsBasedModel.Description description = model.describe();
        // THEN – the description should reflect the model's composition
        assertEquals(3, description.nbVariables, "Total variables");
        assertEquals(0, description.nbIntegerVariables, "Integer variables");
        assertEquals(1, getIntField(description, "nbLoBound"), "Variables with lower bound");
        assertEquals(1, getIntField(description, "nbUpBound"), "Variables with upper bound");
        assertEquals(1, getIntField(description, "nbEqBound"), "Variables with equality bound");
        assertEquals(1, getIntField(description, "nbLoConstraint"), "Expressions with lower constraint");
        assertEquals(1, getIntField(description, "nbUpConstraint"), "Expressions with upper constraint");
        assertEquals(1, getIntField(description, "nbEqConstraint"), "Expressions with equality constraint");
    }

    /**
     * Empty model – all counters must be zero.
     */
    @Test
    void testEmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Variable constraints – single lower‑only variable.
     */
    @Test
    void testVariableLowerOnly() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").lower(BigDecimal.valueOf(-1));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0);
    }

    /**
     * Variable constraints – single integer variable with an upper constraint.
     */
    @Test
    void testIntegerVariableUpperOnly() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").integer(true).upper(BigDecimal.valueOf(10));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0);
    }

    /**
     * Variable constraints – single fixed variable with equality constraint.
     */
    @Test
    void testFixedVariableEqualityOnly() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").level(BigDecimal.valueOf(5)).weight(BigDecimal.ZERO);
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0);
    }

    /**
     * Expression constraints – one expression with both lower and upper bounds.
     */
    @Test
    void testExpressionLowerAndUpper() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newExpression("e").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(7));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0);
    }

    /**
     * Mixed scenario – two variables and two expressions as described in the JSON.
     */
    @Test
    void testMixedScenario() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable A – free, integer, upper constraint, positive upper limit
        model.newVariable("A").integer(true).upper(BigDecimal.valueOf(5));
        // Variable B – free, lower constraint, no limits set (use negative lower to trigger “positive” flag)
        model.newVariable("B").lower(BigDecimal.valueOf(-2));
        // Expressions
        model.newExpression("eqExpr").level(BigDecimal.valueOf(4));
        model.newExpression("loExpr").lower(BigDecimal.valueOf(1));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 2, 2, 2, 1, 1, 1, 0, 1, 0, 1);
    }

    /**
     * Variable with both lower and upper constraints, no limits set.
     */
    @Test
    void testVariableBothConstraintsNoLimits() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0);
    }

    /**
     * Variable with upper constraint but negative upper limit – should not be counted as positive.
     */
    @Test
    void testVariableUpperNegativeLimit() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").upper(BigDecimal.valueOf(-3));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0);
    }

    /**
     * Variable with lower constraint but positive lower limit – should not be counted as negative.
     */
    @Test
    void testVariableLowerPositiveLimit() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").lower(BigDecimal.valueOf(4));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0);
    }

    /**
     * Fixed variable with both lower and upper constraints – not counted as free/positive/negative.
     */
    @Test
    void testFixedVariableBothConstraints() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").level(BigDecimal.valueOf(8));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0);
    }

    /**
     * Expression equality only.
     */
    @Test
    void testExpressionEqualityOnly() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newExpression("e").level(BigDecimal.valueOf(6));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);
    }

    /**
     * Expression upper only.
     */
    @Test
    void testExpressionUpperOnly() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newExpression("e").upper(BigDecimal.valueOf(9));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0);
    }

    /**
     * Complex mixed scenario – three variables and three expressions covering all counters.
     */
    @Test
    void testComplexMixedScenario() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable 1 – free integer with lower constraint
        model.newVariable("V1").integer(true).lower(BigDecimal.valueOf(-5));
        // Variable 2 – free with upper constraint
        model.newVariable("V2").upper(BigDecimal.valueOf(12));
        // Variable 3 – fixed equality
        model.newVariable("V3").level(BigDecimal.valueOf(3));
        // Expressions
        model.newExpression("E1").lower(BigDecimal.valueOf(1));
        model.newExpression("E2").upper(BigDecimal.valueOf(4));
        model.newExpression("E3").level(BigDecimal.valueOf(2));
        ExpressionsBasedModel.Description d = model.describe();
        assertDescription(d, 3, 2, 2, 1, 1, 1, 1, 1, 1, 1);
    }

    /**
     * Null variable injected – expect NullPointerException when iterating.
     */
    @Test
    void testNullVariableInModel() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("good");
        injectNullVariable(model);
        assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when a null variable is present");
    }

    /**
     * Null expression injected – expect NullPointerException when iterating.
     */
    @Test
    void testNullExpressionInModel() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newExpression("good");
        injectNullExpression(model);
        assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when a null expression is present");
    }

    /**
     * Variable reports upper limit set but returns null – NPE during categorisation.
     */
//     @Test
//     void testVariableUpperLimitSetButNull() throws Exception {
//         // Create a mock Variable that pretends an upper limit is set but returns null
//         Variable mock = mock(Variable.class, CALLS_REAL_METHODS);
//         when(mock.isFixed()).thenReturn(false);
//         when(mock.isUpperLimitSet()).thenReturn(true);
//         when(mock.getUpperLimit()).thenReturn(null);
//         when(mock.isLowerLimitSet()).thenReturn(false);
//         when(mock.isInteger()).thenReturn(false);
//         when(mock.isLowerConstraint()).thenReturn(false);
//         when(mock.isUpperConstraint()).thenReturn(false);
//         when(mock.isEqualityConstraint()).thenReturn(false);
//         // Insert mock into model
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // ensure list exists
//         model.newVariable("placeholder");
//         replaceFirstVariableWithMock(model, mock);
//         // The NPE is expected inside VariablesCategorisation.update()
//         assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when upper limit is null");
//     }

    /**
     * Variable reports lower limit set but returns null – NPE during categorisation.
     */
//     @Test
//     void testVariableLowerLimitSetButNull() throws Exception {
//         // Mock variable with lower limit set but null value
//         Variable mock = mock(Variable.class, CALLS_REAL_METHODS);
//         when(mock.isFixed()).thenReturn(false);
//         when(mock.isLowerLimitSet()).thenReturn(true);
//         when(mock.getLowerLimit()).thenReturn(null);
//         when(mock.isUpperLimitSet()).thenReturn(false);
//         when(mock.isInteger()).thenReturn(false);
//         when(mock.isLowerConstraint()).thenReturn(false);
//         when(mock.isUpperConstraint()).thenReturn(false);
//         when(mock.isEqualityConstraint()).thenReturn(false);
//         // Insert mock into model
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("placeholder");
//         replaceFirstVariableWithMock(model, mock);
//         // Expect NPE during categorisation
//         assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when lower limit is null");
//     }
}