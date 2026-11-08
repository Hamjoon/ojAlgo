package org.ojalgo.optimisation;

import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_objective_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test method
        model = new ExpressionsBasedModel();
    }

    /**
     * Injects a value into a private field of {@link ExpressionsBasedModel}.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the constant term of an {@link Expression}. The API provides
     * {@code getConstant()}, but if it is not public we fall back to reflection.
     */
    private static BigDecimal getExpressionConstant(Expression expr) {
        try {
            // Prefer a public getter if it exists
            return (BigDecimal) expr.getClass().getMethod("getConstant").invoke(expr);
        } catch (Exception ignored) {
            // Fallback to reflection on the private field named "myConstant"
            try {
                Field f = expr.getClass().getDeclaredField("myConstant");
                f.setAccessible(true);
                return (BigDecimal) f.get(expr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Verify that a variable marked with a contribution weight appears in the
     * generated objective expression with the same coefficient.
     */
    @Test
    void testVariableContributionIsPreserved() {
        // Variable x will contribute 1·x to the objective
        Variable x = model.newVariable("x").weight(ONE);
        // Variable y is *not* part of the objective
        model.newVariable("y");
        Expression objective = model.objective();
        // The coefficient for x (index 0) must be exactly ONE
        BigDecimal coeffX = objective.get(IntIndex.of(0));
        assertEquals(ONE, coeffX, "Variable contribution weight should be reflected in the objective");
        // y has no contribution weight, its coefficient must be zero (or null treated as zero)
        BigDecimal coeffY = objective.get(IntIndex.of(1));
        assertTrue(coeffY == null || coeffY.compareTo(ZERO) == 0, "Non‑objective variable must have zero coefficient in the objective");
    }

    /**
     * Verify that an expression marked as objective contributes its weighted
     * linear terms to the generated objective expression, in addition to any
     * variable‑level contributions.
     */
    @Test
    void testExpressionLinearContributionIsAggregated() {
        // Variable a contributes 1·a via its own weight
        Variable a = model.newVariable("a").weight(ONE);
        // Variable b does not contribute directly
        Variable b = model.newVariable("b");
        // Expression e: 3·a + 4·b, with a contribution weight of 2
        model.newExpression("e").weight(TWO).set(a, THREE).set(b, FOUR);
        // Build the aggregated objective
        Expression objective = model.objective();
        // Expected coefficient for a = variable weight (1) + expression weight*term (2*3) = 7
        BigDecimal expectedA = ONE.add(TWO.multiply(THREE));
        assertEquals(expectedA, objective.get(IntIndex.of(0)), "Coefficient for variable a should include variable and expression contributions");
        // Expected coefficient for b = expression weight*term (2*4) = 8 (no variable weight)
        BigDecimal expectedB = TWO.multiply(FOUR);
        assertEquals(expectedB, objective.get(IntIndex.of(1)), "Coefficient for variable b should reflect only the expression contribution");
    }

    /**
     * Verify that quadratic terms from an objective expression are also aggregated.
     * The test checks that the quadratic coefficient matrix entry is correctly
     * populated.
     */
    @Test
    void testExpressionQuadraticContributionIsAggregated() {
        // Two variables, none have a direct objective weight
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        // Expression q: weight = 5, quadratic term 3·x·y
        model.newExpression("q").weight(FIVE).quadratic(x, y, THREE);
        // Build the aggregated objective
        Expression objective = model.objective();
        // Expected quadratic coefficient for (x,y) = expression weight * term = 5 * 3 = 15
        BigDecimal expected = FIVE.multiply(THREE);
        var key = model.toIntRowColumn(0, 1);
        assertEquals(expected, objective.get(key), "Quadratic coefficient should be weighted by the expression's contribution weight");
    }

    @Test
    void testNoVariablesNoExpressions() {
        // No variables, no expressions – only the constant should be present
        Expression obj = model.objective();
        assertEquals(model.getObjectiveConstant(), getExpressionConstant(obj), "Constant must be propagated unchanged");
        // No coefficients should be set – all get() calls return null
        assertNull(obj.get(IntIndex.of(0)), "No coefficient should exist for any index");
    }

    @ParameterizedTest(name = "{index} => isObj={0}, weight={1}, expectedCoeff={2}")
    @CsvSource({ // variable is objective, weight = 5
    // variable not objective, weight ignored
    "true,5,5", "false,5,0" })
    void testVariableObjectiveFlag(boolean isObjective, int weight, int expectedCoeff) {
        Variable var = model.newVariable("v0");
        // Use reflection to set the internal fields that control objective flag and weight
        try {
            Field fIsObj = Variable.class.getDeclaredField("myIsObjective");
            fIsObj.setAccessible(true);
            fIsObj.set(var, isObjective);
            Field fWeight = Variable.class.getDeclaredField("myContributionWeight");
            fWeight.setAccessible(true);
            fWeight.set(var, BigDecimal.valueOf(weight));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Replace the internal variable list with our single variable
        List<Variable> vars = new ArrayList<>();
        vars.add(var);
        setPrivateField(model, "myVariables", vars);
        Expression obj = model.objective();
        BigDecimal coeff = obj.get(IntIndex.of(0));
        BigDecimal expected = expectedCoeff == 0 ? null : BigDecimal.valueOf(expectedCoeff);
        if (expected == null) {
            assertTrue(coeff == null || coeff.compareTo(ZERO) == 0, "Non‑objective variable must have zero coefficient");
        } else {
            assertEquals(expected, coeff, "Variable contribution weight should be reflected");
        }
    }

    @Test
    void testExpressionLinearContributionIsAggregated_weightOne() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // expression weight = ONE
        model.newExpression("e").weight(ONE).set(a, THREE).set(b, FOUR);
        Expression obj = model.objective();
        // a: only from expression (3)
        assertEquals(THREE, obj.get(IntIndex.of(0)), "Linear term with weight ONE should be added directly");
        // b: only from expression (4)
        assertEquals(FOUR, obj.get(IntIndex.of(1)), "Linear term with weight ONE should be added directly");
    }

    @Test
    void testExpressionLinearContributionIsAggregated_weightNotOne() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // expression weight = 2
        model.newExpression("e").weight(TWO).set(a, THREE).set(b, FOUR);
        Expression obj = model.objective();
        // a: 2*3 = 6
        assertEquals(TWO.multiply(THREE), obj.get(IntIndex.of(0)), "Linear term should be multiplied by weight");
        // b: 2*4 = 8
        assertEquals(TWO.multiply(FOUR), obj.get(IntIndex.of(1)), "Linear term should be multiplied by weight");
    }

    @Test
    void testExpressionQuadraticContributionIsAggregated_weightOne() {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        // weight ONE, quadratic term 7·x·y
        model.newExpression("q").weight(ONE).quadratic(x, y, SEVEN);
        Expression obj = model.objective();
        var key = model.toIntRowColumn(0, 1);
        assertEquals(SEVEN, obj.get(key), "Quadratic term with weight ONE should be added directly");
    }

    @Test
    void testExpressionQuadraticContributionIsAggregated_weightNotOne() {
        Variable x = model.newVariable("x");
        Variable y = model.newVariable("y");
        // weight = 3, quadratic term 5·x·y
        model.newExpression("q").weight(THREE).quadratic(x, y, FIVE);
        Expression obj = model.objective();
        var key = model.toIntRowColumn(0, 1);
        assertEquals(THREE.multiply(FIVE), obj.get(key), "Quadratic term should be multiplied by weight");
    }

    @Test
    void testExpressionNotObjectiveIsIgnored() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        // expression not marked as objective but has linear/quadratic parts
        model.newExpression("e").set(a, THREE).set(b, FOUR).quadratic(a, b, FIVE);
        Expression obj = model.objective();
        // No contributions should appear
        assertTrue(obj.get(IntIndex.of(0)) == null || obj.get(IntIndex.of(0)).compareTo(ZERO) == 0);
        assertTrue(obj.get(IntIndex.of(1)) == null || obj.get(IntIndex.of(1)).compareTo(ZERO) == 0);
        var key = model.toIntRowColumn(0, 1);
        assertTrue(obj.get(key) == null || obj.get(key).compareTo(ZERO) == 0);
    }

    @Test
    void testExpressionObjectiveZeroFactors() {
        Variable a = model.newVariable("a");
        // objective expression with weight 4 but no linear/quadratic factors
        model.newExpression("e").weight(FOUR);
        Expression obj = model.objective();
        // Only constant (zero) and possible variable contributions – none here
        assertTrue(obj.get(IntIndex.of(0)) == null || obj.get(IntIndex.of(0)).compareTo(ZERO) == 0);
    }

    @Test
    void testMultipleVariablesObjectiveContribution() {
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        // v0 weight 2, v2 weight -3
        v0.weight(TWO);
        v2.weight(NEGATIVE_THREE);
        // No expressions, constant 0
        Expression obj = model.objective();
        assertEquals(TWO, obj.get(IntIndex.of(0)), "v0 contribution");
        assertTrue(obj.get(IntIndex.of(1)) == null || obj.get(IntIndex.of(1)).compareTo(ZERO) == 0, "v1 is not objective");
        assertEquals(NEGATIVE_THREE, obj.get(IntIndex.of(2)), "v2 contribution");
        assertEquals(ZERO, getExpressionConstant(obj), "Constant should be zero");
    }

    @Test
    void testMultipleObjectiveExpressionsAccumulation() {
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        // e0: weight 2, linear 3·v0, quadratic 4·v0·v1
        model.newExpression("e0").weight(TWO).set(v0, THREE).quadratic(v0, v1, FOUR);
        // e1: weight 5, linear 1·v0, quadratic 2·v0·v1
        model.newExpression("e1").weight(FIVE).set(v0, ONE).quadratic(v0, v1, TWO);
        Expression obj = model.objective();
        // Linear coeff for v0 = 2*3 + 5*1 = 11
        assertEquals(TWO.multiply(THREE).add(FIVE.multiply(ONE)), obj.get(IntIndex.of(0)), "Accumulated linear coefficient");
        // Quadratic coeff (0,1) = 2*4 + 5*2 = 18
        var key = model.toIntRowColumn(0, 1);
        assertEquals(TWO.multiply(FOUR).add(FIVE.multiply(TWO)), obj.get(key), "Accumulated quadratic coefficient");
    }

    @Test
    void testMixedLinearQuadraticWeightNotOne() {
        Variable v0 = model.newVariable("v0");
        // expression weight 4, linear 2·v0, quadratic 5·v0·v0
        model.newExpression("e").weight(FOUR).set(v0, TWO).quadratic(v0, v0, FIVE);
        Expression obj = model.objective();
        // Linear: 4*2 = 8
        assertEquals(FOUR.multiply(TWO), obj.get(IntIndex.of(0)), "Linear part weighted");
        // Quadratic (0,0): 4*5 = 20
        var key = model.toIntRowColumn(0, 0);
        assertEquals(FOUR.multiply(FIVE), obj.get(key), "Quadratic part weighted");
    }

    @Test
    void testZeroContributionWeight() {
        Variable v0 = model.newVariable("v0");
        // expression weight 0, linear 7·v0, quadratic 9·v0·v1
        Variable v1 = model.newVariable("v1");
        model.newExpression("e").weight(ZERO).set(v0, SEVEN).quadratic(v0, v1, NINE);
        Expression obj = model.objective();
        // All coefficients must stay zero (or null)
        assertTrue(obj.get(IntIndex.of(0)) == null || obj.get(IntIndex.of(0)).compareTo(ZERO) == 0);
        var key = model.toIntRowColumn(0, 1);
        assertTrue(obj.get(key) == null || obj.get(key).compareTo(ZERO) == 0);
    }

    @Test
    void testNegativeContributionWeight() {
        Variable v0 = model.newVariable("v0");
        // weight -3, linear 4·v0
        model.newExpression("e").weight(NEGATIVE_THREE).set(v0, FOUR);
        Expression obj = model.objective();
        // Expected: -3 * 4 = -12
        assertEquals(NEGATIVE_THREE.multiply(FOUR), obj.get(IntIndex.of(0)), "Negative weight must flip sign");
    }

    @Test
    void testObjectiveConstantPropagation() {
        // Subclass to return a custom constant
        ExpressionsBasedModel custom = new ExpressionsBasedModel() {

            @Override
            public BigDecimal getObjectiveConstant() {
                return new BigDecimal("15");
            }
        };
        Expression obj = custom.objective();
        assertEquals(new BigDecimal("15"), getExpressionConstant(obj), "Constant must be propagated");
    }

    @Test
    void testVariableAndExpressionCombined() {
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        // variable contribution
        // 5·v0
        v0.weight(FIVE);
        // expression weight 2, linear 3·v0 + 4·v1
        model.newExpression("e").weight(TWO).set(v0, THREE).set(v1, FOUR);
        Expression obj = model.objective();
        // v0: 5 + 2*3 = 11
        assertEquals(FIVE.add(TWO.multiply(THREE)), obj.get(IntIndex.of(0)), "Combined variable and expression contribution for v0");
        // v1: 2*4 = 8
        assertEquals(TWO.multiply(FOUR), obj.get(IntIndex.of(1)), "Expression contribution for v1");
    }

    @Test
    void testNullVariableInList() throws Exception {
        // Inject a null entry into the variable list
        List<Variable> vars = new ArrayList<>();
        vars.add(null);
        setPrivateField(model, "myVariables", vars);
        assertThrows(NullPointerException.class, () -> model.objective(), "A null variable should cause NPE when accessed");
    }

    @Test
    void testNullVariableContributionWeight() throws Exception {
        Variable mockVar = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(mockVar.isObjective()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.getContributionWeight()).thenReturn(null);
        List<Variable> vars = new ArrayList<>();
        vars.add(mockVar);
        setPrivateField(model, "myVariables", vars);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null contribution weight should cause NPE");
    }

    @Test
    void testNullExpressionInMap() throws Exception {
        // Map with a null expression value
        Map<String, Expression> map = new HashMap<>();
        map.put("nullExpr", null);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null expression in map should cause NPE");
    }

    @Test
    void testNullExpressionContributionWeight() throws Exception {
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.isObjective()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getContributionWeight()).thenReturn(null);
        // Ensure other methods are not called before the weight check
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null contribution weight in expression should cause NPE");
    }

    @Test
    void testNullExpressionLinearKeySet() throws Exception {
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.isObjective()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        org.mockito.Mockito.when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(null);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null linear key set should cause NPE");
    }

    @Test
    void testNullExpressionLinearFactorValue() throws Exception {
        Variable v = model.newVariable("v");
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.isObjective()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        org.mockito.Mockito.when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(true);
        // Return a non‑empty key set containing the index of v
        org.mockito.Mockito.when(mockExpr.getLinearKeySet()).thenReturn(Set.of(IntIndex.of(0)));
        // Return null for the factor value
        org.mockito.Mockito.when(mockExpr.get(IntIndex.of(0))).thenReturn(null);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null linear factor value should cause NPE");
    }

    @Test
    void testNullExpressionQuadraticKeySet() throws Exception {
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.isObjective()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        org.mockito.Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(null);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null quadratic key set should cause NPE");
    }

    @Test
    void testNullExpressionQuadraticFactorValue() throws Exception {
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        Expression mockExpr = org.mockito.Mockito.mock(Expression.class);
        org.mockito.Mockito.when(mockExpr.isObjective()).thenReturn(true);
        org.mockito.Mockito.when(mockExpr.getContributionWeight()).thenReturn(ONE);
        org.mockito.Mockito.when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(true);
        // Provide a key set with a single pair (0,1)
        org.mockito.Mockito.when(mockExpr.getQuadraticKeySet()).thenReturn(Set.of(model.toIntRowColumn(0, 1)));
        // Return null for the factor value
        org.mockito.Mockito.when(mockExpr.get(model.toIntRowColumn(0, 1))).thenReturn(null);
        Map<String, Expression> map = new HashMap<>();
        map.put("e", mockExpr);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, () -> model.objective(), "Null quadratic factor value should cause NPE");
    }

    @Test
    void testNullObjectiveConstant() {
        // Subclass that returns null constant
        ExpressionsBasedModel custom = new ExpressionsBasedModel() {

            @Override
            public BigDecimal getObjectiveConstant() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> custom.objective(), "Null objective constant should cause NPE");
    }

    @Test
    void testNullRetValGetCoefficient() {
        // No variable contributes to index 0, but an objective expression does
        // index 0
        Variable v0 = model.newVariable("v0");
        // linear factor at index 0
        model.newExpression("e").weight(ONE).set(v0, ONE);
        // Ensure variable is NOT marked as objective (by default it is not)
        // The objective() implementation will try to add to a null old value
        assertThrows(NullPointerException.class, () -> model.objective(), "Missing previous coefficient should cause NPE when adding");
    }
}