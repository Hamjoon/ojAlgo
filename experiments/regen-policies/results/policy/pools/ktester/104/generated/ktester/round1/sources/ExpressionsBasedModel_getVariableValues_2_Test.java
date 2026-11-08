package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import org.ojalgo.type.context.NumberContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariableValues_2_Test {

    private static NumberContext VALIDATION_CONTEXT;

    @FunctionalInterface
    private interface ModelConfigurer {

        void configure(ExpressionsBasedModel model);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // A very permissive NumberContext (high precision, no scaling) – sufficient for validation.
        VALIDATION_CONTEXT = new NumberContext(NumberFormat.getInstance(), new MathContext(20), -1);
    }

    private static Stream<Arguments> variableValueSelectionCases() {
        return Stream.of(// 1️⃣  explicit value 5 → branch “value != null”
        Arguments.of("Explicit value (5)", NumberContext.of(6), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).setValue(BigDecimal.valueOf(5)), Optimisation.State.FEASIBLE, List.of(BigDecimal.valueOf(5)), 5.0), // 2️⃣  equality constraint (lower == upper == 3)
        Arguments.of("Equality constraint (level 3)", NumberContext.of(6), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).level(BigDecimal.valueOf(3)), Optimisation.State.FEASIBLE, List.of(BigDecimal.valueOf(3)), 3.0), // 3️⃣  both limits → midpoint (4+5)/2 = 4.5
        Arguments.of("Both limits (4,5) → midpoint", NumberContext.of(6), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).lower(BigDecimal.valueOf(4)).upper(BigDecimal.valueOf(5)), Optimisation.State.FEASIBLE, List.of(BigDecimal.valueOf(4.5)), 4.5), // 4️⃣  only lower limit → value = lower
        Arguments.of("Only lower limit (2)", NumberContext.of(6), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).lower(BigDecimal.valueOf(2)), Optimisation.State.FEASIBLE, List.of(BigDecimal.valueOf(2)), 2.0), // 5️⃣  only upper limit → value = upper
        Arguments.of("Only upper limit (7)", NumberContext.of(6), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).upper(BigDecimal.valueOf(7)), Optimisation.State.FEASIBLE, List.of(BigDecimal.valueOf(7)), 7.0), // 6️⃣  no information → fallback branch, INFEASIBLE
        Arguments.of("No information (fallback)", NumberContext.of(6), (ModelConfigurer) m -> m.newVariable("v"), Optimisation.State.INFEASIBLE, List.of(BigDecimal.ZERO), Double.NaN));
    }

    private static Stream<Arguments> multipleVariablesCases() {
        return Stream.of(// both variables provide enough information → FEASIBLE, sum = 6
        Arguments.of("Two variables: explicit 3 & limits (2,4)", NumberContext.of(6), (ModelConfigurer) m -> {
            m.newVariable("v0").weight(BigDecimal.ONE).setValue(BigDecimal.valueOf(3));
            m.newVariable("v1").weight(BigDecimal.ONE).lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(4));
        }, Optimisation.State.FEASIBLE, List.of(BigDecimal.valueOf(3), BigDecimal.valueOf(3)), 6.0), // one variable lacks any info → INFEASIBLE, solution contains ZERO for the missing one
        Arguments.of("Two variables: upper only (5) & missing info", NumberContext.of(6), (ModelConfigurer) m -> {
            m.newVariable("v0").weight(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
            m.newVariable("v1");
        }, Optimisation.State.INFEASIBLE, List.of(BigDecimal.valueOf(5), BigDecimal.ZERO), Double.NaN));
    }

    private static Stream<Arguments> validationFailureCases() {
        return Stream.of(// explicit non‑integer value with a context that allows 0 decimal places
        Arguments.of("Explicit 1.5 with 0‑decimal‑place context", NumberContext.of(0), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).setValue(BigDecimal.valueOf(1.5)), Optimisation.State.APPROXIMATE, List.of(BigDecimal.valueOf(1.5)), Double.NaN), // average of limits yields 1.0015, context allows only 2 decimal places
        Arguments.of("Midpoint 1.0015 with 2‑decimal‑place context", NumberContext.of(2), (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).lower(BigDecimal.valueOf(1.001)).upper(BigDecimal.valueOf(1.002)), Optimisation.State.APPROXIMATE, List.of(BigDecimal.valueOf(1.0015)), Double.NaN));
    }

    private static Stream<Arguments> nullContextCases() {
        return Stream.of(Arguments.of("Null NumberContext", (ModelConfigurer) m -> m.newVariable("v").weight(BigDecimal.ONE).setValue(BigDecimal.ONE)));
    }

    /**
     * All variables provide a value (or a bound) and the model validates successfully.
     * Expected state: {@code FEASIBLE} and the objective value equals the weighted sum.
     */
    @Test
    void testAllInfoAndValidationPasses() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.newVariable("v0").weight(BigDecimal.ONE);
        v0.setValue(BigDecimal.valueOf(2));
        Variable v1 = model.newVariable("v1").weight(BigDecimal.ONE);
        v1.setValue(BigDecimal.valueOf(3));
        Optimisation.Result result = model.getVariableValues(VALIDATION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "When all variables have information and validation succeeds the state must be FEASIBLE.");
        assertEquals(5.0, result.getValue(), 1e-12, "Objective value should be the weighted sum of the variables.");
        assertEquals(BigDecimal.valueOf(2), result.get(0));
        assertEquals(BigDecimal.valueOf(3), result.get(1));
    }

    /**
     * All variables provide a value, but the solution violates a constraint.
     * Expected state: {@code APPROXIMATE} and the returned value is NaN.
     */
    @Test
    void testAllInfoButValidationFails() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").weight(BigDecimal.ONE);
        x.setValue(BigDecimal.valueOf(10));
        Expression constr = model.newExpression("x ≤ 5").upper(BigDecimal.valueOf(5));
        constr.set(x, BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(VALIDATION_CONTEXT);
        assertEquals(Optimisation.State.APPROXIMATE, result.getState(), "When validation fails the state must be APPROXIMATE.");
        assertTrue(Double.isNaN(result.getValue()), "Result value should be NaN when not feasible.");
    }

    /**
     * At least one variable lacks any information (no value, no bounds, not an equality constraint).
     * Expected state: {@code INFEASIBLE}.
     */
    @Test
    void testMissingVariableInformationLeadsToInfeasible() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable known = model.newVariable("known").weight(BigDecimal.ONE);
        known.setValue(BigDecimal.valueOf(1));
        model.newVariable("unknown");
        Optimisation.Result result = model.getVariableValues(VALIDATION_CONTEXT);
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "If any variable lacks information the state must be INFEASIBLE.");
        assertTrue(Double.isNaN(result.getValue()), "Result value should be NaN for INFEASIBLE state.");
    }

    /**
     * Variable with both lower and upper limits set – the method should use the midpoint.
     * Expected solution entry equals (lower + upper) / 2.
     */
    @Test
    void testVariableWithBothLimitsUsesMidpoint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("mid").lower(BigDecimal.valueOf(4)).upper(BigDecimal.valueOf(8)).weight(BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(VALIDATION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "All variables have limits, validation should succeed.");
        BigDecimal expected = BigDecimal.valueOf(6);
        assertEquals(expected, result.get(0));
        assertEquals(6.0, result.getValue(), 1e-12);
    }

    /**
     * Variable that is an equality constraint (lower == upper) should return that bound.
     */
    @Test
    void testEqualityConstraintVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("eq").level(BigDecimal.valueOf(7)).weight(BigDecimal.ONE);
        Optimisation.Result result = model.getVariableValues(VALIDATION_CONTEXT);
        assertEquals(Optimisation.State.FEASIBLE, result.getState(), "Equality‑constrained variable should be feasible.");
        assertEquals(BigDecimal.valueOf(7), result.get(0));
        assertEquals(7.0, result.getValue(), 1e-12);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("variableValueSelectionCases")
    void testVariableValueSelection(String description, NumberContext validationContext, ModelConfigurer configurer, Optimisation.State expectedState, List<BigDecimal> expectedSolution, Double expectedObjective) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        configurer.configure(model);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(expectedState, result.getState(), () -> "Unexpected state for case: " + description);
        for (int i = 0; i < expectedSolution.size(); i++) {
            assertEquals(expectedSolution.get(i), result.get(i), () -> "Solution entry " + i + " differs for case: " + description);
        }
        if (expectedObjective == null || expectedObjective.isNaN()) {
            assertTrue(Double.isNaN(result.getValue()), () -> "Expected NaN objective for case: " + description);
        } else {
            assertEquals(expectedObjective, result.getValue(), 1e-12, () -> "Unexpected objective value for case: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multipleVariablesCases")
    void testMultipleVariables(String description, NumberContext validationContext, ModelConfigurer configurer, Optimisation.State expectedState, List<BigDecimal> expectedSolution, Double expectedObjective) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        configurer.configure(model);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(expectedState, result.getState(), () -> "Unexpected state for case: " + description);
        for (int i = 0; i < expectedSolution.size(); i++) {
            assertEquals(expectedSolution.get(i), result.get(i), () -> "Solution entry " + i + " differs for case: " + description);
        }
        if (expectedObjective == null || expectedObjective.isNaN()) {
            assertTrue(Double.isNaN(result.getValue()), () -> "Expected NaN objective for case: " + description);
        } else {
            assertEquals(expectedObjective, result.getValue(), 1e-12, () -> "Unexpected objective value for case: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validationFailureCases")
    void testValidationFailureLeadsToApproximate(String description, NumberContext validationContext, ModelConfigurer configurer, Optimisation.State expectedState, List<BigDecimal> expectedSolution, Double expectedObjective) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        configurer.configure(model);
        Optimisation.Result result = model.getVariableValues(validationContext);
        assertEquals(expectedState, result.getState(), () -> "Unexpected state for case: " + description);
        for (int i = 0; i < expectedSolution.size(); i++) {
            assertEquals(expectedSolution.get(i), result.get(i), () -> "Solution entry " + i + " differs for case: " + description);
        }
        assertTrue(Double.isNaN(result.getValue()), () -> "Expected NaN objective for APPROXIMATE state in case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullContextCases")
    void testNullValidationContextThrows(String description, ModelConfigurer configurer) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        configurer.configure(model);
        assertThrows(NullPointerException.class, () -> model.getVariableValues(null), () -> "Expected NullPointerException for case: " + description);
    }
}
