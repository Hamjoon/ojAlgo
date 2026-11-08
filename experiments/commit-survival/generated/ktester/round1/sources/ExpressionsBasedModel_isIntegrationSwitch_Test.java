package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import java.util.List;
import java.util.Arrays;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isIntegrationSwitch_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test contamination.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly discard the model reference – helps the GC in long test suites.
        model = null;
    }

    /**
     * Supplies the test data extracted from the JSON test specification.
     *
     * @return a stream of arguments: (list of actions, expected result, description)
     */
    private static Stream<Arguments> integrationSwitchScenarios() {
        return Stream.of(// Default state – no calls to the setter.
        arguments(List.of(), false, "Verify the default value of the integration switch is false immediately after constructing a new ExpressionsBasedModel."), // Single true → true.
        arguments(List.of(true), true, "After calling setIntegrationSwitch(true), the getter should return true, confirming the switch can be turned on."), // Single false → false (from default or after true).
        arguments(List.of(false), false, "After calling setIntegrationSwitch(false) (either from the default state or after being set true), the getter should return false, confirming the switch can be turned off."), // True then false → false.
        arguments(List.of(true, false), false, "Toggle the switch: set true then set false, and verify the final state is false, ensuring the setter correctly overwrites the previous value."));
    }

    @Test
    void testDefaultIntegrationSwitchIsFalse() {
        // By default the internal flag should be false.
        assertFalse(model.isIntegrationSwitch(), "The integration switch should be false for a newly created model.");
    }

    @Test
    void testSetIntegrationSwitchToTrue() {
        // Turn the switch on and verify the getter reflects the change.
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "The integration switch should be true after calling setIntegrationSwitch(true).");
    }

    @Test
    void testSetIntegrationSwitchToFalseAfterTrue() {
        // Switch on, then off, and verify the final state.
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Switch should be true after first set to true.");
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "Switch should be false after setting it back to false.");
    }

    /**
     * Parameterised test that executes the sequence of {@code setIntegrationSwitch}
     * calls described in each JSON case and then checks the value returned by
     * {@code isIntegrationSwitch()}.
     *
     * @param actions   ordered list of boolean values to pass to
     *                  {@code setIntegrationSwitch}
     * @param expected  the expected result of {@code isIntegrationSwitch()} after
     *                  all actions have been performed
     * @param description human‑readable description of the scenario (used only for
     *                  documentation)
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("integrationSwitchScenarios")
    void testIntegrationSwitchBehaviour(List<Boolean> actions, boolean expected, String description) {
        // Apply each action in order.
        actions.forEach(value -> model.setIntegrationSwitch(value));
        // Verify the getter reflects the final state.
        assertEquals(expected, model.isIntegrationSwitch(), () -> "Unexpected integration switch state after actions: " + actions);
    }
}
