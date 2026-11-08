package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariableValues_2_Test {

    /**
     * A generic {@code NumberContext} with high precision (34 digits) and unlimited scale.
     */
    private static final NumberContext HIGH_PRECISION_CONTEXT = new NumberContext(// unlimited scale
    NumberFormat.getInstance(), new MathContext(34), -1);

    private static class ModelWithFailedValidate extends ExpressionsBasedModel {

        @Override
        public boolean validate(final Access1D<BigDecimal> solution, final NumberContext context, final BasicLogger appender) {
            // force validation failure
            return false;
        }
    }

    private static class ModelWithNullObjective extends ExpressionsBasedModel {

        @Override
        public Expression objective() {
            // break objective evaluation
            return null;
        }
    }

    /**
     * Inject a list of variables (or mocks) directly into the private field {@code myVariables}.
     */
    private static void setModelVariables(ExpressionsBasedModel model, List<Variable> vars) throws Exception {
        java.lang.reflect.Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) f.get(model);
        list.clear();
        list.addAll(vars);
    }

    /**
     * Convenience to create a model with a single variable of a given type.
     */
    private static ExpressionsBasedModel createModelWithSingleVariable(String type, String lower, String upper, String value, boolean equality) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable();
        if ("explicit".equals(type)) {
            v.setValue(new BigDecimal(value));
        }
        if ("equality".equals(type) || equality) {
            // Equality is modelled by setting lower == upper
            BigDecimal l = new BigDecimal(lower);
            v.lower(l).upper(l);
        }
        if ("bounded".equals(type)) {
            v.lower(new BigDecimal(lower)).upper(new BigDecimal(upper));
        }
        if ("lowerOnly".equals(type)) {
            v.lower(new BigDecimal(lower));
        }
        if ("upperOnly".equals(type)) {
            v.upper(new BigDecimal(upper));
        }
        return model;
    }

    private static Stream<Arguments> provideNormalScenarios() {
        // explicit value
        ExpressionsBasedModel m1 = createModelWithSingleVariable("explicit", null, null, "5.5", false);
        // equality constraint
        ExpressionsBasedModel m2 = createModelWithSingleVariable("equality", "3", null, null, false);
        // bounded (average)
        ExpressionsBasedModel m3 = createModelWithSingleVariable("bounded", "2", "8", null, false);
        // lower only
        ExpressionsBasedModel m4 = createModelWithSingleVariable("lowerOnly", "-4", null, null, false);
        // upper only
        ExpressionsBasedModel m5 = createModelWithSingleVariable("upperOnly", null, "7", null, false);
        // no information (INFEASIBLE)
        ExpressionsBasedModel m6 = new ExpressionsBasedModel();
        m6.addVariable();
        // mixed variables (5 vars)
        ExpressionsBasedModel mixed = new ExpressionsBasedModel();
        Variable v0 = mixed.addVariable();
        v0.setValue(BigDecimal.ONE);
        Variable v1 = mixed.addVariable();
        v1.lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3));
        Variable v2 = mixed.addVariable();
        v2.lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8));
        Variable v3 = mixed.addVariable();
        v3.lower(BigDecimal.valueOf(-4));
        Variable v4 = mixed.addVariable();
        v4.upper(BigDecimal.valueOf(7));
        // empty model
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        return Stream.of(Arguments.of("Variable value handling – explicit value", m1, Optimisation.State.FEASIBLE, List.of(new BigDecimal("5.5")), null), Arguments.of("Variable value handling – equality constraint", m2, Optimisation.State.FEASIBLE, List.of(new BigDecimal("3")), null), Arguments.of("Variable value handling – both limits set (average)", m3, Optimisation.State.FEASIBLE, List.of(new BigDecimal("5")), null), Arguments.of("Variable value handling – only lower limit", m4, Optimisation.State.FEASIBLE, List.of(new BigDecimal("-4")), null), Arguments.of("Variable value handling – only upper limit", m5, Optimisation.State.FEASIBLE, List.of(new BigDecimal("7")), null), Arguments.of("Variable value handling – no information (ZERO)", m6, Optimisation.State.INFEASIBLE, List.of(BigDecimal.ZERO), Double.NaN), Arguments.of("Overall result – all info present, validation succeeds", mixed, Optimisation.State.FEASIBLE, List.of(BigDecimal.ONE, new BigDecimal("3"), new BigDecimal("5"), new BigDecimal("-4"), new BigDecimal("7")), null), Arguments.of("Empty model – no variables", empty, Optimisation.State.FEASIBLE, List.of(), null));
    }

    /**
     * All variables provide enough information (value, bounds or both) and the solution
     * validates against the model – the returned state must be {@code FEASIBLE}.
     */
    @Test
    void testAllVariablesHaveInfoAndValidatePasses() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.addVariable();
        v0.setValue(BigDecimal.valueOf(5));
        Variable v1 = model.addVariable();
        v1.lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3));
        Variable v2 = model.addVariable();
        v2.lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8));
        Variable v3 = model.addVariable();
        v3.lower(BigDecimal.valueOf(1));
        Variable v4 = model.addVariable();
        v4.upper(BigDecimal.valueOf(9));
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "All variables have enough information and the solution should be feasible.");
        assertEquals(0.0, result.getValue(), 1e-12, "Objective value should be zero when no objective is defined.");
        assertEquals(BigDecimal.valueOf(5), result.get(0));
        assertEquals(BigDecimal.valueOf(3), result.get(1));
        assertEquals(BigDecimal.valueOf(5), result.get(2));
        assertEquals(BigDecimal.valueOf(1), result.get(3));
        assertEquals(BigDecimal.valueOf(9), result.get(4));
    }

    /**
     * All variables provide enough information, but the solution does **not** satisfy the model
     * (validation fails). The method must return {@code APPROXIMATE}.
     */
    @Test
    void testAllVariablesHaveInfoButValidateFails() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable();
        x.lower(BigDecimal.TEN);
        Expression expr = model.addExpression("x <= 5");
        expr.set(x, BigDecimal.ONE);
        expr.upper(BigDecimal.valueOf(5));
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.APPROXIMATE, result.getState(), "All variables have information but the solution does not validate, so the state must be APPROXIMATE.");
    }

    /**
     * At least one variable lacks any information (no value, no bounds). The method must return
     * {@code INFEASIBLE}.
     */
    @Test
    void testVariableWithoutInfoLeadsToInfeasible() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "When a variable has no information the method should report INFEASIBLE.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNormalScenarios")
    void testNormalScenarios(String description, ExpressionsBasedModel model, Optimisation.State expectedState, List<BigDecimal> expectedSolution, Double expectedValue) {
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(expectedState, result.getState(), description);
        for (int i = 0; i < expectedSolution.size(); i++) {
            assertEquals(expectedSolution.get(i), result.get(i), "solution element " + i + " differs");
        }
        if (expectedValue != null) {
            assertEquals(expectedValue, result.getValue(), 1e-12, "objective value differs");
        } else {
            assertTrue(Double.isNaN(result.getValue()), "objective value should be NaN");
        }
    }

    @Test
    void testOverrideValidateToFail() {
        ModelWithFailedValidate model = new ModelWithFailedValidate();
        Variable v = model.addVariable();
        // explicit value – enough info
        v.setValue(BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.APPROXIMATE, result.getState(), "Overridden validate returns false → APPROXIMATE");
    }

    @Test
    void testObjectiveReturnsNullLeadsToNPE() {
        ModelWithNullObjective model = new ModelWithNullObjective();
        Variable v = model.addVariable();
        v.setValue(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Calling getVariableValues when objective() returns null should throw NPE");
    }

    @Test
    void testNullValidationContext() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, () -> model.getVariableValues(null), "Passing null NumberContext must result in NPE");
    }

    @Test
    void testNullVariableInModel() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        List<Variable> list = new ArrayList<>();
        list.add(null);
        setModelVariables(model, list);
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Null variable in myVariables should cause NPE");
    }

    @Test
    void testEqualityWithoutLowerLimit() throws Exception {
        // Create a mock Variable that reports equality constraint but returns null lower limit
        Variable mockVar = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(mockVar.getValue()).thenReturn(null);
        org.mockito.Mockito.when(mockVar.isEqualityConstraint()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.getLowerLimit()).thenReturn(null);
        // other flags are irrelevant for this path
        org.mockito.Mockito.when(mockVar.isLowerLimitSet()).thenReturn(false);
        org.mockito.Mockito.when(mockVar.isUpperLimitSet()).thenReturn(false);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        setModelVariables(model, List.of(mockVar));
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Equality constraint without lower limit should cause NPE");
    }

    @Test
    void testBothLimitsSetLowerNull() throws Exception {
        Variable mockVar = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(mockVar.getValue()).thenReturn(null);
        org.mockito.Mockito.when(mockVar.isEqualityConstraint()).thenReturn(false);
        org.mockito.Mockito.when(mockVar.isLowerLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.isUpperLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.getLowerLimit()).thenReturn(null);
        org.mockito.Mockito.when(mockVar.getUpperLimit()).thenReturn(BigDecimal.valueOf(5));
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        setModelVariables(model, List.of(mockVar));
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Both limits set but lower null should cause NPE");
    }

    @Test
    void testBothLimitsSetUpperNull() throws Exception {
        Variable mockVar = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(mockVar.getValue()).thenReturn(null);
        org.mockito.Mockito.when(mockVar.isEqualityConstraint()).thenReturn(false);
        org.mockito.Mockito.when(mockVar.isLowerLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.isUpperLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.getLowerLimit()).thenReturn(BigDecimal.valueOf(2));
        org.mockito.Mockito.when(mockVar.getUpperLimit()).thenReturn(null);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        setModelVariables(model, List.of(mockVar));
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Both limits set but upper null should cause NPE");
    }

    @Test
    void testUpperFlagSetButValueNull() throws Exception {
        Variable mockVar = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(mockVar.getValue()).thenReturn(null);
        org.mockito.Mockito.when(mockVar.isEqualityConstraint()).thenReturn(false);
        org.mockito.Mockito.when(mockVar.isUpperLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.getUpperLimit()).thenReturn(null);
        // lower flag false
        org.mockito.Mockito.when(mockVar.isLowerLimitSet()).thenReturn(false);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        setModelVariables(model, List.of(mockVar));
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Upper flag set but value null should cause NPE");
    }

    @Test
    void testLowerFlagSetButValueNull() throws Exception {
        Variable mockVar = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(mockVar.getValue()).thenReturn(null);
        org.mockito.Mockito.when(mockVar.isEqualityConstraint()).thenReturn(false);
        org.mockito.Mockito.when(mockVar.isLowerLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(mockVar.getLowerLimit()).thenReturn(null);
        // upper flag false
        org.mockito.Mockito.when(mockVar.isUpperLimitSet()).thenReturn(false);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        setModelVariables(model, List.of(mockVar));
        assertThrows(NullPointerException.class, () -> model.getVariableValues(HIGH_PRECISION_CONTEXT), "Lower flag set but value null should cause NPE");
    }

    @Test
    void testExplicitOverridesBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable();
        v.setValue(BigDecimal.valueOf(9));
        v.lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState());
        assertEquals(BigDecimal.valueOf(9), result.get(0));
    }

    @Test
    void testExplicitOverridesEquality() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable();
        v.setValue(BigDecimal.valueOf(4));
        v.lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2));
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState());
        assertEquals(BigDecimal.valueOf(4), result.get(0));
    }

    @Test
    void testBothLimitsEqual() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable();
        v.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState());
        assertEquals(BigDecimal.valueOf(5), result.get(0));
    }

    @Test
    void testReversedLimitsAverage() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable();
        v.lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(2));
        Optimisation.Result result = model.getVariableValues(HIGH_PRECISION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState());
        assertEquals(BigDecimal.valueOf(6), result.get(0));
    }
}
