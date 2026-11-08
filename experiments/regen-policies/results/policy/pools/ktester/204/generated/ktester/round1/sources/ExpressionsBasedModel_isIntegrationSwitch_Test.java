package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isIntegrationSwitch_Test {

    private ExpressionsBasedModel model;

    private ExpressionsBasedModel.IntegrationProperty testProperty;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // ensures the test compiles even if the actual enum constants change in future versions.
        ExpressionsBasedModel.IntegrationProperty[] all = ExpressionsBasedModel.IntegrationProperty.values();
        assertTrue(all.length > 0, "IntegrationProperty enum must contain at least one constant");
        testProperty = all[0];
        // Obtain an arbitrary IntegrationProperty value – using the first enum constant
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model will be garbage‑collected.
        model = null;
        model = null;
    }

    /**
     * Helper to obtain an enum constant by its name; throws an informative
     *  AssertionError if the constant does not exist (so the test fails fast).
     */
    private static ExpressionsBasedModel.IntegrationProperty enumByName(String name) {
        try {
            return ExpressionsBasedModel.IntegrationProperty.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("IntegrationProperty enum does not contain constant: " + name, e);
        }
    }

    static Stream<Arguments> defaultFalseProvider() {
        return Stream.of(// the JSON uses IntegrationProperty.SAMPLE
        Arguments.of("SAMPLE"));
    }

    static Stream<Arguments> enableProvider() {
        return Stream.of(// JSON: IntegrationProperty.ENABLE_FAST_MIP
        Arguments.of("ENABLE_FAST_MIP"));
    }

    static Stream<Arguments> disableProvider() {
        return Stream.of(// JSON: IntegrationProperty.DISABLE_PRESOLVE
        Arguments.of("DISABLE_PRESOLVE"));
    }

    @Test
    void testDefaultSwitchIsFalse() {
        // pick any existing enum constant – the first one is sufficient
        ExpressionsBasedModel.IntegrationProperty any = ExpressionsBasedModel.IntegrationProperty.values()[0];
        assertFalse(model.isIntegrationSwitch(any), "Integration switch should be false before any explicit setting");
    }

    @Test
    void testSwitchCanBeTurnedOnAndOff() {
        // Turn the switch on
        model.setIntegrationSwitch(testProperty, true);
        assertTrue(model.isIntegrationSwitch(testProperty), "Integration switch should be true after being set to true");
        // Turn the switch off again
        model.setIntegrationSwitch(testProperty, false);
        assertFalse(model.isIntegrationSwitch(testProperty), "Integration switch should be false after being set back to false");
    }

    @Test
    void testNullPropertyThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.isIntegrationSwitch(null), "Passing null should result in NullPointerException");
    }

    /* ---------- default false for a specific property ---------- */
    @ParameterizedTest(name = "[{index}] default false for {0}")
    @MethodSource("defaultFalseProvider")
    void testDefaultFalseForSpecificProperty(String propertyName) {
        ExpressionsBasedModel.IntegrationProperty prop = enumByName(propertyName);
        assertFalse(model.isIntegrationSwitch(prop), () -> "New model should report false for property " + propertyName);
    }

    /* ---------- true after explicit enable ---------- */
    @ParameterizedTest(name = "[{index}] true after enable for {0}")
    @MethodSource("enableProvider")
    void testTrueAfterEnable(String propertyName) {
        ExpressionsBasedModel.IntegrationProperty prop = enumByName(propertyName);
        model.setIntegrationSwitch(prop, true);
        assertTrue(model.isIntegrationSwitch(prop), () -> "Property " + propertyName + " should be true after being enabled");
    }

    /* ---------- false after explicit disable ---------- */
    @ParameterizedTest(name = "[{index}] false after disable for {0}")
    @MethodSource("disableProvider")
    void testFalseAfterDisable(String propertyName) {
        ExpressionsBasedModel.IntegrationProperty prop = enumByName(propertyName);
        // ensure the property is true first to prove the disable really flips it
        model.setIntegrationSwitch(prop, true);
        model.setIntegrationSwitch(prop, false);
        assertFalse(model.isIntegrationSwitch(prop), () -> "Property " + propertyName + " should be false after being disabled");
    }

    /* ---------- independence of different properties ---------- */
    @Test
    void testIndependentProperties() {
        // Property A set to true
        ExpressionsBasedModel.IntegrationProperty propA = enumByName("A");
        // Property B set to false (explicitly, to make the intent clear)
        ExpressionsBasedModel.IntegrationProperty propB = enumByName("B");
        model.setIntegrationSwitch(propA, true);
        model.setIntegrationSwitch(propB, false);
        assertTrue(model.isIntegrationSwitch(propA), "Property A must be true after being set to true");
        assertFalse(model.isIntegrationSwitch(propB), "Property B must be false after being set to false");
    }

    /* ---------- toggle sequence ---------- */
    @Test
    void testToggleSequenceEndsTrue() {
        ExpressionsBasedModel.IntegrationProperty toggle = enumByName("TOGGLE");
        // true -> false -> true
        model.setIntegrationSwitch(toggle, true);
        model.setIntegrationSwitch(toggle, false);
        model.setIntegrationSwitch(toggle, true);
        assertTrue(model.isIntegrationSwitch(toggle), "After true→false→true sequence, switch should be true");
    }

    @Test
    void testToggleSequenceEndsFalse() {
        ExpressionsBasedModel.IntegrationProperty toggle = enumByName("TOGGLE");
        // true -> false
        model.setIntegrationSwitch(toggle, true);
        model.setIntegrationSwitch(toggle, false);
        assertFalse(model.isIntegrationSwitch(toggle), "After true→false sequence, switch should be false");
    }
}
