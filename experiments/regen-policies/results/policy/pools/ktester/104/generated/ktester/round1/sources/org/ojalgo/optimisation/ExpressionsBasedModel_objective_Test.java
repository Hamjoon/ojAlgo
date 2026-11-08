package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.lang.reflect.Field;
import java.util.*;
import org.ojalgo.structure.Structure2D.IntRowColumn;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_objective_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
    }

    /**
     * Helper to access private fields via reflection.
     */
    private static <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        T value = (T) f.get(target);
        return value;
    }

    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * When the model has no variables and no expressions,
     * the objective should be an empty expression with a zero constant.
     */
    @Test
    void testEmptyModelObjective() {
        Expression obj = model.objective();
        // No linear terms
        assertEquals(0, obj.size());
        // Constant term must be zero (the default objective constant)
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    /**
     * A variable that is marked as objective contributes its weight
     * directly to the aggregated objective expression.
     */
    @Test
    void testVariableContribution() {
        // Create a variable with a contribution weight of 2
        Variable x = model.newVariable("x").lower(0).upper(10).weight(BigDecimal.valueOf(2));
        // No other expressions – the objective should consist solely of the variable contribution
        Expression obj = model.objective();
        // The model contains exactly one variable, therefore the index is 0
        IntIndex idx = IntIndex.of(0);
        assertEquals(BigDecimal.valueOf(2), obj.get(idx));
        // Constant must still be zero
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    /**
     * An expression that is marked as objective contributes its
     * weighted linear terms to the aggregated objective.
     *
     * The test also verifies that a variable contribution weight
     * is added on top of the expression contribution.
     */
    @Test
    void testExpressionAndVariableContributionCombined() {
        // Variable with its own objective weight
        Variable x = model.newVariable("x").lower(0).upper(10).weight(BigDecimal.valueOf(2));
        // Expression that also contributes to the objective
        Expression expr = model.newExpression("expr").weight(BigDecimal.valueOf(3));
        // Linear term: 5 * x
        expr.set(x, BigDecimal.valueOf(5));
        // Build the aggregated objective
        Expression obj = model.objective();
        // Expected linear coefficient for x:
        //   variable weight (2)  +  expression weight (3) * coefficient (5) = 2 + 15 = 17
        BigDecimal expectedCoeff = BigDecimal.valueOf(17);
        IntIndex idx = IntIndex.of(0);
        assertEquals(expectedCoeff, obj.get(idx));
        // No constant term added by the model
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    @Test
    void testSingleVariableObjectiveWeightFive() {
        // Variable marked as objective with weight 5
        model.newVariable("y").weight(BigDecimal.valueOf(5));
        Expression obj = model.objective();
        assertEquals(BigDecimal.valueOf(5), obj.get(IntIndex.of(0)));
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    @Test
    void testSingleVariableNotObjective() {
        // Variable created but never marked as objective
        model.newVariable("z").lower(0).upper(1);
        Expression obj = model.objective();
        // No coefficient should be present
        assertEquals(0, obj.size());
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    @Test
    void testMixedVariableObjectiveFlags() {
        // Variable 0 – objective weight 3
        model.newVariable("v0").weight(BigDecimal.valueOf(3));
        // Variable 1 – not an objective
        model.newVariable("v1");
        // Variable 2 – objective weight -2
        model.newVariable("v2").weight(BigDecimal.valueOf(-2));
        Expression obj = model.objective();
        // Expected coefficients only for indices 0 and 2
        assertEquals(BigDecimal.valueOf(3), obj.get(IntIndex.of(0)));
        assertEquals(BigDecimal.valueOf(-2), obj.get(IntIndex.of(2)));
        // Index 1 must be absent
        assertNull(obj.get(IntIndex.of(1)));
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    @Test
    void testAllVariablesObjectiveZeroWeight() {
        // Three variables, all objective but weight = 0
        model.newVariable("a").weight(BigDecimal.ZERO);
        model.newVariable("b").weight(BigDecimal.ZERO);
        model.newVariable("c").weight(BigDecimal.ZERO);
        Expression obj = model.objective();
        // All coefficients should be explicit zero values
        assertEquals(BigDecimal.ZERO, obj.get(IntIndex.of(0)));
        assertEquals(BigDecimal.ZERO, obj.get(IntIndex.of(1)));
        assertEquals(BigDecimal.ZERO, obj.get(IntIndex.of(2)));
        assertEquals(BigDecimal.ZERO, obj.getConstant());
    }

    @Test
    void testNullVariableInListThrowsNPE() throws Exception {
        // Insert a null entry into the private myVariables list
        List<Variable> vars = getPrivateField(model, "myVariables", List.class);
        vars.add(null);
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testVariableObjectiveTrueButWeightNullThrowsNPE() throws Exception {
        // Mock a Variable that reports objective = true but weight = null
        Variable mockVar = mock(Variable.class);
        when(mockVar.isObjective()).thenReturn(true);
        when(mockVar.getContributionWeight()).thenReturn(null);
        // Replace the internal variable list with a list containing the mock
        List<Variable> vars = new ArrayList<>();
        vars.add(mockVar);
        setPrivateField(model, "myVariables", vars);
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testNullExpressionInMapThrowsNPE() throws Exception {
        // Insert a null expression into the private myExpressions map
        Map<String, Expression> exprMap = getPrivateField(model, "myExpressions", Map.class);
        exprMap.put("nullExpr", null);
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testExpressionObjectiveWeightNullThrowsNPE() throws Exception {
        // Mock an Expression that is objective but its contribution weight is null
        Expression mockExpr = mock(Expression.class);
        when(mockExpr.isObjective()).thenReturn(true);
        when(mockExpr.getContributionWeight()).thenReturn(null);
        when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(false);
        when(mockExpr.isAnyQuadraticFactorNonZero()).thenReturn(false);
        Map<String, Expression> exprMap = new HashMap<>();
        exprMap.put("e", mockExpr);
        setPrivateField(model, "myExpressions", exprMap);
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testLinearKeySetContainsNullThrowsNPE() throws Exception {
        // Mock an Expression with a null linear key
        Expression mockExpr = mock(Expression.class);
        when(mockExpr.isObjective()).thenReturn(true);
        when(mockExpr.getContributionWeight()).thenReturn(BigMath.ONE);
        when(mockExpr.isAnyLinearFactorNonZero()).thenReturn(true);
        when(mockExpr.getLinearKeySet()).thenReturn(Collections.singleton((IntIndex) null));
        // The following methods are not reached because the NPE occurs earlier,
        // but we stub them to avoid unexpected interactions.
        when(mockExpr.get(any())).thenReturn(BigDecimal.ONE);
        Map<String, Expression> exprMap = new HashMap<>();
        exprMap.put("e", mockExpr);
        setPrivateField(model, "myExpressions", exprMap);
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testOldCoefficientNullLeadsToNPE() throws Exception {
        // Variable not marked as objective (so no coefficient initially)
        Variable v = model.newVariable("v");
        // Expression that is objective and references the variable
        model.newExpression("e").weight(BigMath.ONE).set(v, BigDecimal.ONE);
        // Since the variable is not an objective variable, retVal.get(v) will be null,
        // causing a NPE when add() is invoked.
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testExpressionLinearDiffNullThrowsNPE() throws Exception {
        // Variable not marked as objective (so no coefficient initially)
        Variable v = model.newVariable("v");
        // Expression that is objective and references the variable
        model.newExpression("e").weight(BigMath.ONE).set(v, BigDecimal.ONE);
        // Since the variable is not an objective variable, retVal.get(v) will be null,
        // causing a NPE when add() is invoked.
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testQuadraticKeySetContainsNullThrowsNPE() throws Exception {
        // Custom expression with a null quadratic key
        Expression expr = new Expression(model) {

            @Override
            public boolean isObjective() {
                return true;
            }

            @Override
            public BigDecimal getContributionWeight() {
                return BigMath.ONE;
            }

            @Override
            public boolean isAnyQuadraticFactorNonZero() {
                return true;
            }

            @Override
            public Set<IntRowColumn> getQuadraticKeySet() {
                return Collections.singleton((IntRowColumn) null);
            }

            @Override
            public BigDecimal get(IntRowColumn key) {
                return BigDecimal.ONE;
            }
        };
        Map<String, Expression> exprMap = new HashMap<>();
        exprMap.put("e", expr);
        setPrivateField(model, "myExpressions", exprMap);
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testQuadraticOldCoefficientNullThrowsNPE() throws Exception {
        // Variable not objective, quadratic term on it
        Variable v = model.newVariable("v");
        // Custom expression that references the variable quadratically
        Expression expr = new Expression(model) {

            @Override
            public boolean isObjective() {
                return true;
            }

            @Override
            public BigDecimal getContributionWeight() {
                return BigMath.ONE;
            }

            @Override
            public boolean isAnyQuadraticFactorNonZero() {
                return true;
            }

            @Override
            public Set<IntRowColumn> getQuadraticKeySet() {
                // Use the model's helper to create a proper key for (0,0)
                return Collections.singleton(model.toIntRowColumn(0, 0));
            }

            @Override
            public BigDecimal get(IntRowColumn key) {
                return BigDecimal.ONE;
            }
        };
        Map<String, Expression> exprMap = new HashMap<>();
        exprMap.put("e", expr);
        setPrivateField(model, "myExpressions", exprMap);
        // Since the variable is not an objective variable, the old coefficient in the
        // aggregated expression will be null → NPE.
        assertThrows(NullPointerException.class, model::objective);
    }

    @Test
    void testObjectiveConstantNullThrowsNPE() {
        // Spy the model and stub getObjectiveConstant() to return null
        ExpressionsBasedModel spyModel = spy(new ExpressionsBasedModel());
        doReturn(null).when(spyModel).getObjectiveConstant();
        // No variables/expressions needed – the NPE occurs when setConstant(null) is called.
        assertThrows(NullPointerException.class, spyModel::objective);
    }
}