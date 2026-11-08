package org.ojalgo.optimisation;

import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.TestUtils;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Structure2D.IntRowColumn;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_objective_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for each test
        model = new ExpressionsBasedModel();
        // fresh model for each test
        model = new ExpressionsBasedModel();
    }

    /**
     * Helper to set a private field via reflection.
     */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Verify that the objective expression contains the contribution weights of the variables
     * and nothing else.
     */
    @Test
    void testObjectiveFromVariablesOnly() {
        // Create three variables; give the first two a contribution weight
        // weight = 1
        Variable x0 = model.newVariable("x0").weight(ONE);
        // weight = -2
        Variable x1 = model.newVariable("x1").weight(TWO.negate());
        // no weight → not part of objective
        Variable x2 = model.newVariable("x2");
        // Build the objective expression
        Expression obj = model.objective();
        // Expected coefficients (constant is zero by default)
        TestUtils.assertEquals(ONE, obj.get(IntIndex.of(0), false));
        TestUtils.assertEquals(TWO.negate(), obj.get(IntIndex.of(1), false));
        TestUtils.assertEquals(ZERO, obj.get(IntIndex.of(2), false));
        // default constant
        TestUtils.assertEquals(ZERO, obj.getConstant());
    }

    /**
     * Verify that the objective expression correctly aggregates contributions from
     * both weighted variables and weighted expressions (including linear terms).
     */
    @Test
    void testObjectiveFromVariablesAndExpressions() {
        // Variables
        // no direct weight
        Variable x0 = model.newVariable("x0");
        // weight = 2
        Variable x1 = model.newVariable("x1").weight(TWO);
        // will be weighted via a variable weight later
        Variable x2 = model.newVariable("x2");
        // Give x2 a direct contribution weight
        // weight = 1
        x2.weight(ONE);
        // Expression e1 with weight 3 and linear terms 5·x0 + 2·x1
        Expression e1 = model.newExpression("e1").weight(THREE);
        e1.set(x0, FIVE);
        e1.set(x1, TWO);
        // Expression e2 with weight 1 and linear term 4·x1
        Expression e2 = model.newExpression("e2").weight(ONE);
        e2.set(x1, FOUR);
        // Build the aggregated objective expression
        Expression obj = model.objective();
        // Expected linear coefficients:
        // x0 : 3 * 5 = 15
        // x1 : (3 * 2) + (1 * 4) + variable weight 2 = 6 + 4 + 2 = 12
        // x2 : variable weight 1 = 1
        TestUtils.assertEquals(FIFTEEN, obj.get(IntIndex.of(0), false));
        TestUtils.assertEquals(TWELVE, obj.get(IntIndex.of(1), false));
        TestUtils.assertEquals(ONE, obj.get(IntIndex.of(2), false));
        // No constant has been added, so it should be zero
        TestUtils.assertEquals(ZERO, obj.getConstant());
    }

    /* --------------------------------------------------------------
     *  1. No variables / expressions – the “empty” objective.
     * -------------------------------------------------------------- */
    @Test
    void objective_noVariablesNoExpressions() {
        Expression obj = model.objective();
        assertEquals("Generated/Aggregated Objective", obj.getName());
        assertEquals(ZERO, obj.getConstant());
        // All coefficients must be zero – the expression has no variables, so any get returns ZERO
        assertEquals(ZERO, obj.getConstant());
    }

    /* --------------------------------------------------------------
     *  2. The expression map contains a null value.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullExpressionInMap() throws Exception {
        Map<String, Expression> brokenMap = new HashMap<>();
        brokenMap.put("nullExpr", null);
        setField(model, "myExpressions", brokenMap);
        assertThrows(NullPointerException.class, model::objective, "A null entry in the expression map should cause NPE");
    }

    /* --------------------------------------------------------------
     *  3. An expression marked as objective returns a null contribution weight.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullContributionWeightInExpression() throws Exception {
        Expression mockExpr = Mockito.mock(Expression.class);
        Mockito.when(mockExpr.isObjective()).thenReturn(true);
        Mockito.when(mockExpr.getContributionWeight()).thenReturn(null);
        Mockito.when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(false);
        Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, model::objective, "Null contribution weight should trigger NPE when compareTo(ONE) is executed");
    }

    /* --------------------------------------------------------------
     *  4. Null old value during linear aggregation (retVal.get returns null).
     * -------------------------------------------------------------- */
    @Test
    void objective_nullOldValueInLinearAggregation() {
        // Create a variable and an expression that contributes linearly.
        Variable v = model.newVariable("v");
        // weight = 1
        Expression e = model.newExpression("e").weight(ONE);
        // linear term 5·v
        e.set(v, FIVE);
        // No need to tamper with internal maps – the expression is added normally.
        // The newly created objective expression does not yet contain a coefficient for v,
        // therefore retVal.get(v) returns null and .add(...) throws NPE.
        assertThrows(NullPointerException.class, model::objective, "Missing old coefficient should cause NPE during linear aggregation");
    }

    /* --------------------------------------------------------------
     *  5. Null diff (coefficient) during linear aggregation.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullDiffInLinearAggregation() throws Exception {
        Expression mockExpr = Mockito.mock(Expression.class);
        IntIndex idx = IntIndex.of(0);
        Mockito.when(mockExpr.isObjective()).thenReturn(true);
        Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        Mockito.when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(true);
        Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Set.of(idx));
        // The coefficient for the linear factor is null
        Mockito.when(mockExpr.get(idx)).thenReturn(null);
        Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, model::objective, "Null linear coefficient should cause NPE when multiplied/added");
    }

    /* --------------------------------------------------------------
     *  6. Null old value during quadratic aggregation.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullOldValueInQuadraticAggregation() throws Exception {
        // Mock expression with a quadratic term but no existing coefficient in the objective.
        Expression mockExpr = Mockito.mock(Expression.class);
        IntRowColumn qKey = Mockito.mock(IntRowColumn.class);
        Mockito.when(mockExpr.isObjective()).thenReturn(true);
        Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        Mockito.when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(false);
        Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Set.of(qKey));
        // The coefficient for the quadratic factor is a valid number (e.g., FIVE)
        Mockito.when(mockExpr.get(qKey)).thenReturn(FIVE);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setField(model, "myExpressions", map);
        // The newly created objective expression has no quadratic coefficient yet,
        // so retVal.get(qKey) returns null → NPE on .add(...)
        assertThrows(NullPointerException.class, model::objective, "Missing old quadratic coefficient should cause NPE");
    }

    /* --------------------------------------------------------------
     *  7. Null diff (coefficient) during quadratic aggregation.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullDiffInQuadraticAggregation() throws Exception {
        Expression mockExpr = Mockito.mock(Expression.class);
        IntRowColumn qKey = Mockito.mock(IntRowColumn.class);
        Mockito.when(mockExpr.isObjective()).thenReturn(true);
        Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        Mockito.when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(false);
        Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Set.of(qKey));
        // Coefficient is null
        Mockito.when(mockExpr.get(qKey)).thenReturn(null);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, model::objective, "Null quadratic coefficient should cause NPE when multiplied/added");
    }

    /* --------------------------------------------------------------
     *  8. getObjectiveConstant() returns null.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullObjectiveConstant() {
        // Spy the model and stub getObjectiveConstant() to return null.
        ExpressionsBasedModel spyModel = Mockito.spy(new ExpressionsBasedModel());
        Mockito.doReturn(null).when(spyModel).getObjectiveConstant();
        assertThrows(NullPointerException.class, spyModel::objective, "Null objective constant should cause NPE when set on the expression");
    }

    /* --------------------------------------------------------------
     *  9. Variable marked as objective returns null contribution weight.
     * -------------------------------------------------------------- */
    @Test
    void objective_nullVariableContributionWeight() throws Exception {
        // Create a variable and then force its internal contribution weight to null.
        Variable var = model.newVariable("x");
        // The internal field name is not part of the public API; we try the most likely name.
        // If the field name changes, the test will fail, which is acceptable for this edge case.
        Field weightField = var.getClass().getDeclaredField("myContributionWeight");
        weightField.setAccessible(true);
        // now var.getContributionWeight() returns null
        weightField.set(var, null);
        // The variable is part of the model's variable list, so objective() will process it.
        assertThrows(NullPointerException.class, model::objective, "Null variable contribution weight should cause NPE when set on the objective");
    }
}
