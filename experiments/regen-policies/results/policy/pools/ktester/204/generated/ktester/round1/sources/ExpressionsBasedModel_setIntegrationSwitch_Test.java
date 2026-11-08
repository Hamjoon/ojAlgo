package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setIntegrationSwitch_Test {

    private ExpressionsBasedModel model;

    private IntegrationProperty anyProperty;

    /**
     * Create a fresh model before each test and pick an arbitrary {@link IntegrationProperty}.
     */
    @BeforeEach
    void setupBeforeEach() {
        // use the default constructor
        model = new ExpressionsBasedModel();
        IntegrationProperty[] values = IntegrationProperty.values();
        // Guard against the (unlikely) situation that the enum is empty.
        assertTrue(values.length > 0, "IntegrationProperty enum must contain at least one constant");
        // pick the first constant – the actual name is irrelevant
        anyProperty = values[0];
        // default constructor
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> toggleSequenceProvider() {
        return Stream.of(Arguments.of(IntegrationProperty.INTEGRATION_C, true, false), Arguments.of(IntegrationProperty.INTEGRATION_C, false, true));
    }

    private static Stream<Arguments> idempotentProvider() {
        return Stream.of(Arguments.of(IntegrationProperty.INTEGRATION_A, true), Arguments.of(IntegrationProperty.INTEGRATION_B, false));
    }

    /**
     * Verify that a newly created model reports {@code false} for any integration switch.
     */
    @Test
    void testDefaultSwitchIsFalse() {
        // pick any property – the first enum constant is sufficient
        IntegrationProperty any = IntegrationProperty.values()[0];
        assertFalse(model.isIntegrationSwitch(any), "By default every integration switch must be false");
    }

    /**
     * Setting a switch to {@code true} must be reflected by the public accessor.
     */
    @Test
    void testSetSwitchTrue() {
        // package‑private method is accessible
        model.setIntegrationSwitch(anyProperty, true);
        assertTrue(model.isIntegrationSwitch(anyProperty), "Switch should be true after setting it to true");
    }

    /**
     * Setting a switch back to {@code false} must clear the flag.
     */
    @Test
    void testSetSwitchFalse() {
        // first set to true to ensure the flag can be cleared
        model.setIntegrationSwitch(anyProperty, true);
        assertTrue(model.isIntegrationSwitch(anyProperty));
        // now clear it
        model.setIntegrationSwitch(anyProperty, false);
        assertFalse(model.isIntegrationSwitch(anyProperty), "Switch should be false after setting it to false");
    }

    /**
     * Verify that calling the method with different properties does not interfere with each other.
     */
    @Test
    void testIndependentProperties() {
        IntegrationProperty[] all = IntegrationProperty.values();
        // If there are fewer than two constants we cannot test independence.
        assumeTrue(all.length >= 2, "Need at least two IntegrationProperty constants");
        IntegrationProperty p1 = all[0];
        IntegrationProperty p2 = all[1];
        // set first true, second false
        model.setIntegrationSwitch(p1, true);
        model.setIntegrationSwitch(p2, false);
        assertTrue(model.isIntegrationSwitch(p1), "p1 should be true");
        assertFalse(model.isIntegrationSwitch(p2), "p2 should be false");
        // flip both
        model.setIntegrationSwitch(p1, false);
        model.setIntegrationSwitch(p2, true);
        assertFalse(model.isIntegrationSwitch(p1), "p1 should be false after flip");
        assertTrue(model.isIntegrationSwitch(p2), "p2 should be true after flip");
    }

    @ParameterizedTest(name = "[{index}] set {0} to {1}")
    @CsvSource({ "INTEGRATION_A, true", "INTEGRATION_A, false" })
    void testSetSwitchBasic(IntegrationProperty property, boolean value) {
        // Act
        model.setIntegrationSwitch(property, value);
        // Assert
        assertEquals(value, model.isIntegrationSwitch(property), () -> "Switch for " + property + " should be " + value);
    }

    @ParameterizedTest(name = "[{index}] toggle {0}: {1} -> {2}")
    @MethodSource("toggleSequenceProvider")
    void testToggleSequence(IntegrationProperty property, boolean first, boolean second) {
        // first call
        model.setIntegrationSwitch(property, first);
        assertEquals(first, model.isIntegrationSwitch(property), () -> "After first call the switch must be " + first);
        // second call
        model.setIntegrationSwitch(property, second);
        assertEquals(second, model.isIntegrationSwitch(property), () -> "After second call the switch must be " + second);
    }

    @ParameterizedTest(name = "[{index}] idempotent set {0} to {1} twice")
    @MethodSource("idempotentProvider")
    void testIdempotentSet(IntegrationProperty property, boolean value) {
        // first set
        model.setIntegrationSwitch(property, value);
        assertEquals(value, model.isIntegrationSwitch(property), "First set should reflect the value");
        // second identical set
        model.setIntegrationSwitch(property, value);
        assertEquals(value, model.isIntegrationSwitch(property), "Second identical set must leave the state unchanged");
    }

    @Test
    void testNullPropertyThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.setIntegrationSwitch(null, true), "Passing a null IntegrationProperty must throw NullPointerException");
    }
}
