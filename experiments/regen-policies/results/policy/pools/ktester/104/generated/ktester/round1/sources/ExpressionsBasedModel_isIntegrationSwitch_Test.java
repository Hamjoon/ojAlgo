package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class ExpressionsBasedModel_isIntegrationSwitch_Test {

    /**
     * Data provider for simple set‑and‑check cases (single mutation).
     */
    private static Stream<Arguments> singleMutationProvider() {
        return Stream.of(Arguments.of(false, new boolean[] { false }, "Default false without mutation"), Arguments.of(true, new boolean[] { true }, "Set true once"), Arguments.of(false, new boolean[] { false }, "Set false once"));
    }

    /**
     * Data provider for toggle‑sequence cases.
     */
    private static Stream<Arguments> toggleSequenceProvider() {
        return Stream.of(Arguments.of(false, new boolean[] { true, false }, "true → false"), Arguments.of(true, new boolean[] { false, true }, "false → true"));
    }

    /**
     * Data provider for idempotent cases (same value repeated).
     */
    private static Stream<Arguments> idempotentProvider() {
        return Stream.of(Arguments.of(true, new boolean[] { true, true }, "idempotent true"), Arguments.of(false, new boolean[] { false, false }, "idempotent false"));
    }

    /**
     * The integration switch should be {@code false} for a freshly created model.
     */
    @Test
    void testDefaultIntegrationSwitchIsFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isIntegrationSwitch(), "Newly created ExpressionsBasedModel should have integration switch set to false");
    }

    /**
     * Setting the switch to {@code true} must be reflected by {@code isIntegrationSwitch()}.
     */
    @Test
    void testSetIntegrationSwitchTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Integration switch should be true after calling setIntegrationSwitch(true)");
    }

    /**
     * The flag can be toggled back to {@code false} after being set to {@code true}.
     */
    @Test
    void testToggleIntegrationSwitch() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // turn on
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Switch should be true after first set");
        // turn off again
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "Switch should be false after being set to false");
    }

    /**
     * Different model instances maintain independent switch states.
     */
    @Test
    void testIndependentModels() {
        ExpressionsBasedModel modelA = new ExpressionsBasedModel();
        ExpressionsBasedModel modelB = new ExpressionsBasedModel();
        modelA.setIntegrationSwitch(true);
        modelB.setIntegrationSwitch(false);
        assertTrue(modelA.isIntegrationSwitch(), "Model A should have switch true");
        assertFalse(modelB.isIntegrationSwitch(), "Model B should have switch false");
    }

    /**
     * Setting the switch to {@code false} must be reflected by {@code isIntegrationSwitch()}.
     */
    @Test
    void testSetIntegrationSwitchFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // first turn it on to ensure we really test a change
        model.setIntegrationSwitch(true);
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "Integration switch should be false after calling setIntegrationSwitch(false)");
    }

    /**
     * Repeated calls to {@code isIntegrationSwitch()} without mutation must be stable.
     */
    @Test
    void testRepeatedIsIntegrationSwitchCalls() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isIntegrationSwitch(), "First call should be false");
        assertFalse(model.isIntegrationSwitch(), "Second call should still be false");
        assertFalse(model.isIntegrationSwitch(), "Third call should still be false");
    }

    /**
     * Repeated calls to {@code setIntegrationSwitch(true)} keep the switch true (idempotent).
     */
    @Test
    void testIdempotentTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setIntegrationSwitch(true);
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Multiple setIntegrationSwitch(true) calls must leave the switch true");
    }

    /**
     * Repeated calls to {@code setIntegrationSwitch(false)} keep the switch false (idempotent).
     */
    @Test
    void testIdempotentFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setIntegrationSwitch(false);
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "Multiple setIntegrationSwitch(false) calls must leave the switch false");
    }

    /**
     * Toggle sequence: true then false results in false.
     */
    @Test
    void testToggleTrueThenFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setIntegrationSwitch(true);
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "After toggling true -> false the switch must be false");
    }

    /**
     * Toggle sequence: false then true results in true.
     */
    @Test
    void testToggleFalseThenTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setIntegrationSwitch(false);
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "After toggling false -> true the switch must be true");
    }

    /**
     * After setting the switch on the original model, a copy created via {@code copy()} retains the state.
     */
    @Test
    void testCopyRetainsIntegrationSwitch() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.setIntegrationSwitch(true);
        ExpressionsBasedModel copy = original.copy();
        assertTrue(copy.isIntegrationSwitch(), "Copy of a model with switch true must also report true");
    }

    /**
     * A freshly created model (independent of any copies) must have switch false.
     */
    @Test
    void testCopyDoesNotAffectNewInstance() {
        ExpressionsBasedModel fresh = new ExpressionsBasedModel();
        assertFalse(fresh.isIntegrationSwitch(), "Newly created model must have switch false regardless of previous copies");
    }

    @ParameterizedTest(name = "{index} => expected={0}, mutations={1}")
    @MethodSource("singleMutationProvider")
    void testSingleMutation(boolean expected, boolean[] mutations, String description) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        for (boolean val : mutations) {
            model.setIntegrationSwitch(val);
        }
        assertEquals(expected, model.isIntegrationSwitch(), description);
    }

    @ParameterizedTest(name = "{index} => finalExpected={0}, sequence={1}")
    @MethodSource("toggleSequenceProvider")
    void testToggleSequence(boolean expected, boolean[] sequence, String description) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        for (boolean val : sequence) {
            model.setIntegrationSwitch(val);
        }
        assertEquals(expected, model.isIntegrationSwitch(), description);
    }

    @ParameterizedTest(name = "{index} => expected={0}, repetitions={1}")
    @MethodSource("idempotentProvider")
    void testIdempotentBehavior(boolean expected, boolean[] repetitions, String description) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        for (boolean val : repetitions) {
            model.setIntegrationSwitch(val);
        }
        assertEquals(expected, model.isIntegrationSwitch(), description);
    }
}
