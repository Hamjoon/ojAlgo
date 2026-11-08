package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.type.context.NumberContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariableValues_2_Test {

    private ExpressionsBasedModel model;

    private NumberContext validationContext;

    /**
     * Model that deliberately returns {@code null} for {@code objective()}.
     */
    private static final class ModelWithoutObjective extends ExpressionsBasedModel {

        @Override
        public Expression objective() {
            return null;
        }
    }

    /**
     * Model that pretends validation succeeds but whose objective evaluation throws
     * {@link IllegalArgumentException}.
     */
    private static final class ModelWithBadObjective extends ExpressionsBasedModel {

        private final Expression badExpression = new Expression(this) {

            @Override
            public BigDecimal evaluate(BigDecimal[] solution) {
                throw new IllegalArgumentException("Mismatched solution size");
            }
        };

        @Override
        public Expression objective() {
            return badExpression;
        }

        @Override
        protected boolean validate(NumberContext context, java.util.Set<Integer> variables, java.util.Set<Integer> expressions) {
            // Force validation to succeed
            return true;
        }
    }

    /**
     * Simple holder for a test case that only touches a single variable/branch.
     */
    static final class BranchCase {

        final String description;

        final java.util.function.Consumer<ExpressionsBasedModel> modelSetup;

        final Optimisation.State expectedState;

        // may be NaN
        final Double expectedValue;

        // may be null
        final BigDecimal expectedSolution;

        BranchCase(String description, java.util.function.Consumer<ExpressionsBasedModel> modelSetup, Optimisation.State expectedState, Double expectedValue, BigDecimal expectedSolution) {
            this.description = description;
            this.modelSetup = modelSetup;
            this.expectedState = expectedState;
            this.expectedValue = expectedValue;
            this.expectedSolution = expectedSolution;
        }
    }

    /**
     * Simple holder for a test case involving several variables with mixed info.
     */
    static final class MixedCase {

        final String description;

        final java.util.function.Consumer<ExpressionsBasedModel> modelSetup;

        final Optimisation.State expectedState;

        final double expectedValue;

        final List<BigDecimal> expectedSolution;

        MixedCase(String description, java.util.function.Consumer<ExpressionsBasedModel> modelSetup, Optimisation.State expectedState, double expectedValue, List<BigDecimal> expectedSolution) {
            this.description = description;
            this.modelSetup = modelSetup;
            this.expectedState = expectedState;
            this.expectedValue = expectedValue;
            this.expectedSolution = expectedSolution;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        validationContext = model.options.feasibility;
    }

    private static Stream<Arguments> branchCasesProvider() {
        return Stream.of(Arguments.of(new BranchCase("Variable with explicit value – feasible", m -> m.newVariable("v0").setValue(BigDecimal.valueOf(3)).weight(BigDecimal.ONE), Optimisation.State.FEASIBLE, 3.0, BigDecimal.valueOf(3))), Arguments.of(new BranchCase("Variable without any information – infeasible", m -> m.newVariable("v0"), Optimisation.State.INFEASIBLE, Double.NaN, null)), Arguments.of(new BranchCase("Variable lower bound violates an explicit upper constraint – approximate", m -> {
            Variable v = m.newVariable("v0").lower(BigDecimal.valueOf(5));
            m.addExpression("c0").set(v, BigDecimal.ONE).upper(BigDecimal.valueOf(3));
        }, Optimisation.State.APPROXIMATE, Double.NaN, null)));
    }

    private static Stream<Arguments> mixedCasesProvider() {
        return Stream.of(Arguments.of(new MixedCase("Two variables: explicit 5 and lower 2", m -> {
            m.newVariable("v0").setValue(BigDecimal.valueOf(5));
            m.newVariable("v1").lower(BigDecimal.valueOf(2));
        }, Optimisation.State.FEASIBLE, 0.0, List.of(BigDecimal.valueOf(5), BigDecimal.valueOf(2)))), Arguments.of(new MixedCase("First variable lower 1, upper 3 → avg 2; second variable upper 4", m -> {
            m.newVariable("v0").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(3));
            m.newVariable("v1").upper(BigDecimal.valueOf(4));
        }, Optimisation.State.FEASIBLE, 0.0, List.of(BigDecimal.valueOf(2), BigDecimal.valueOf(4)))), Arguments.of(new MixedCase("Both variables have lower=upper=5", m -> {
            m.newVariable("v0").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
            m.newVariable("v1").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        }, Optimisation.State.FEASIBLE, 0.0, List.of(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))));
    }

    @Test
    void testAllVariablesHaveInfoAndValidate() {
        Variable v0 = model.newVariable("v0").lower(BigDecimal.ZERO).weight(BigDecimal.valueOf(2));
        Variable v1 = model.newVariable("v1").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.valueOf(3));
        Variable v2 = model.newVariable("v2").upper(BigDecimal.valueOf(7)).weight(BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "Result state should be FEASIBLE when all variables have information and validation passes");
        assertEquals(22.0, result.getValue(), 1e-12, "Objective value should be the weighted sum of the derived variable values");
        assertEquals(BigDecimal.ZERO, result.get(0));
        assertEquals(BigDecimal.valueOf(5), result.get(1));
        assertEquals(BigDecimal.valueOf(7), result.get(2));
    }

    @Test
    void testVariableWithoutAnyInfoLeadsToInfeasible() {
        model.newVariable("v0").lower(BigDecimal.ONE).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newVariable("v1").weight(BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "Result state must be INFEASIBLE when a variable lacks any information");
        assertTrue(Double.isNaN(result.getValue()), "Result value should be NaN for INFEASIBLE state");
    }

    @Test
    void testValidationFailsLeadsToApproximate() {
        Variable v0 = model.newVariable("v0").lower(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        model.addExpression("c0").set(v0, BigDecimal.ONE).upper(BigDecimal.valueOf(3));
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(Optimisation.State.APPROXIMATE, result.getState(), "Result state should be APPROXIMATE when validation of the solution fails");
        assertTrue(Double.isNaN(result.getValue()), "Result value should be NaN for APPROXIMATE state");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("branchCasesProvider")
    void testBranchCoverage(BranchCase bc) {
        bc.modelSetup.accept(model);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(bc.expectedState, result.getState(), "Unexpected state for case: " + bc.description);
        if (bc.expectedValue.isNaN()) {
            assertTrue(Double.isNaN(result.getValue()), "Expected NaN value for case: " + bc.description);
        } else {
            assertEquals(bc.expectedValue.doubleValue(), result.getValue(), 1e-12, "Unexpected value for case: " + bc.description);
        }
        if (!result.getSolution(validationContext).isEmpty() && bc.expectedSolution != null) {
            assertEquals(bc.expectedSolution, result.get(0), "Unexpected solution entry for case: " + bc.description);
        }
    }

    @Test
    void testEmptyModelReturnsFeasibleWithEmptySolution() {
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "Empty model should be FEASIBLE");
        assertEquals(0.0, result.getValue(), 1e-12, "Empty model should have objective value 0.0");
        assertEquals(0, result.getSolution(validationContext).size(), "Solution array should be empty for a model with no variables");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mixedCasesProvider")
    void testMixedInformationMultipleVariables(MixedCase mc) {
        mc.modelSetup.accept(model);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(mc.expectedState, result.getState(), "Unexpected state for mixed case: " + mc.description);
        assertEquals(mc.expectedValue, result.getValue(), 1e-12, "Unexpected objective value for mixed case: " + mc.description);
        for (int i = 0; i < mc.expectedSolution.size(); i++) {
            assertEquals(mc.expectedSolution.get(i), result.get(i), "Unexpected solution element at index " + i + " for mixed case: " + mc.description);
        }
    }

    @Test
    void testObjectiveEvaluation() {
        Variable v0 = model.newVariable("v0").setValue(BigDecimal.ONE);
        Variable v1 = model.newVariable("v1").setValue(BigDecimal.valueOf(2));
        model.objective().set(v0, BigDecimal.ONE).set(v1, BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "Result should be FEASIBLE when objective can be evaluated");
        assertEquals(3.0, result.getValue(), 1e-12, "Objective value should be the sum 1 + 2 = 3");
        assertEquals(BigDecimal.ONE, result.get(0));
        assertEquals(BigDecimal.valueOf(2), result.get(1));
    }

    @Test
    void testValidationFailsWithTightContext() {
        model.newVariable("v").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(2));
        // tolerance, scale order as required by API
        NumberContext tight = NumberContext.of(10, 1e-12);
        Optimisation.Result result = model.getVariableValues(tight);
        assertEquals(Optimisation.State.APPROXIMATE, result.getState(), "Tight validation context should cause APPROXIMATE state");
        assertTrue(Double.isNaN(result.getValue()), "Result value should be NaN for APPROXIMATE state");
    }

    @Test
    void testNullValidationContextThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.getVariableValues(null), "Passing null NumberContext should throw NullPointerException");
    }

    @Test
    void testNullVariableInListThrowsNPE() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) f.get(model);
        vars.add(null);
        assertThrows(NullPointerException.class, () -> model.getVariableValues(validationContext), "A null variable in the internal list should cause NullPointerException");
    }

    @Test
    void testMissingObjectiveThrowsNPE() {
        ModelWithoutObjective brokenModel = new ModelWithoutObjective();
        brokenModel.newVariable("v").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> brokenModel.getVariableValues(validationContext), "Missing objective (null) should cause NullPointerException");
    }

    @Test
    void testEqualityConstraintWithoutLowerLimitThrowsNPE() throws Exception {
        Variable mockedVar = mock(Variable.class);
        when(mockedVar.getValue()).thenReturn(null);
        when(mockedVar.isEqualityConstraint()).thenReturn(true);
        when(mockedVar.getLowerLimit()).thenReturn(null);
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) f.get(model);
        vars.add(mockedVar);
        assertThrows(NullPointerException.class, () -> model.getVariableValues(validationContext), "Equality constraint without lower limit should cause NullPointerException");
    }

    @Test
    void testNumberContextWithIllegalPrecisionThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberContext.of(-1, 1e-9), "Negative precision should cause IllegalArgumentException");
    }

    @Test
    void testObjectiveEvaluationMismatchedSolutionSizeThrowsIllegalArgumentException() {
        ModelWithBadObjective model = new ModelWithBadObjective();
        model.newVariable("v").lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> model.getVariableValues(validationContext), "Mismatched solution size during objective evaluation should throw IllegalArgumentException");
    }
}
