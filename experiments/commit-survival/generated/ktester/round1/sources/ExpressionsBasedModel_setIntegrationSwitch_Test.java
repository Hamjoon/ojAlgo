package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setIntegrationSwitch_Test {

    /**
     * Provides sequences of boolean values to be applied to {@code setIntegrationSwitch}
     * together with the expected final state.
     *
     * Each {@link Arguments} consists of:
     *   1. List&lt;Boolean&gt; – the values to set in order.
     *   2. Boolean – the expected result of {@code isIntegrationSwitch()} after the sequence.
     *   3. String – a short description (used only for readability in the test code).
     */
    static Stream<Arguments> integrationSwitchProvider() {
        return Stream.of(// {value:true} → true
        Arguments.of(List.of(true), true, "Set true once"), // {value:false} → false
        Arguments.of(List.of(false), false, "Set false once"), // {true, true} → true (idempotent true)
        Arguments.of(List.of(true, true), true, "Set true twice consecutively"), // {true, false, true} → true (toggle)
        Arguments.of(List.of(true, false, true), true, "Toggle true → false → true"));
    }

    /**
     * Additional sequences that also involve model modifications (adding variables/expressions)
     * to ensure the integration switch remains independent of those operations.
     */
    static Stream<Arguments> integrationSwitchAdditionalProvider() {
        return Stream.of(// {false, false} → false (idempotent false)
        Arguments.of(List.of(false, false), false, "Set false twice consecutively", false, false), // {false, true, false} → false (toggle back to false)
        Arguments.of(List.of(false, true, false), false, "Toggle false → true → false", false, false), // {true} → true, then add a variable (no change)
        Arguments.of(List.of(true), true, "Set true then add a variable", true, true), // {false} → false, then add an expression (no change)
        Arguments.of(List.of(false), false, "Set false then add an expression", false, false), // {true, true, true} → true (multiple true assignments)
        Arguments.of(List.of(true, true, true), true, "Set true three times", true, true));
    }

    /**
     * Verify the default state of the integration switch is {@code false}.
     */
    @Test
    void testDefaultIntegrationSwitchIsFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isIntegrationSwitch(), "The integration switch should be false by default.");
    }

    /**
     * Setting the integration switch to {@code true} should be reflected by {@link ExpressionsBasedModel#isIntegrationSwitch()}.
     */
    @Test
    void testSetIntegrationSwitchToTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // package‑private method – accessible because this test resides in the same package
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "The integration switch should be true after setting it to true.");
    }

    /**
     * Setting the integration switch back to {@code false} should be reflected correctly.
     */
    @Test
    void testSetIntegrationSwitchToFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // first set to true
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Pre‑condition failed: switch should be true.");
        // now reset
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "The integration switch should be false after setting it to false.");
    }

    /**
     * Executes the sequences defined in {@link #integrationSwitchProvider()} and asserts the final state.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("integrationSwitchProvider")
    void testIntegrationSwitchSequences(List<Boolean> values, boolean expected, String description) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        values.forEach(model::setIntegrationSwitch);
        assertEquals(expected, model.isIntegrationSwitch(), () -> "After sequence " + values + " the switch should be " + expected);
    }

    /**
     * Executes the additional sequences. The last two booleans in the {@link Arguments}
     * indicate whether the test should add a variable (addVariable) and/or an expression (addExpression)
     * after applying the switch values.
     *
     * @param values        sequence of switch values to apply
     * @param expected      expected final state of the switch
     * @param description   description for the test case
     * @param addVariable   if {@code true}, a variable is added after the switch sequence
     * @param addExpression if {@code true}, an expression is added after the switch sequence
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("integrationSwitchAdditionalProvider")
    void testIntegrationSwitchAdditionalSequences(List<Boolean> values, boolean expected, String description, boolean addVariable, boolean addExpression) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        values.forEach(model::setIntegrationSwitch);
        // Perform optional model modifications to verify independence
        if (addVariable) {
            // the returned Variable is not needed for this test
            model.addVariable();
        }
        if (addExpression) {
            // the returned Expression is not needed for this test
            model.addExpression();
        }
        assertEquals(expected, model.isIntegrationSwitch(), () -> "After sequence " + values + " and optional modifications, the switch should be " + expected);
    }
}
