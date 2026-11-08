package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setIntegrationSwitch_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model instance
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current implementation
        model = null;
    }

    @Test
    void testDefaultIntegrationSwitchIsFalse() {
        // The integration switch is initialised to false in the constructor
        assertFalse(model.isIntegrationSwitch(), "Integration switch should be false by default");
    }

    @Test
    void testSetIntegrationSwitchTrue() {
        // Set the switch to true using the package‑private method
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Integration switch should be true after setting to true");
    }

    @Test
    void testSetIntegrationSwitchFalse() {
        // First turn it on, then turn it off again
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Pre‑condition failed: switch should be true");
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "Integration switch should be false after setting to false");
    }

    @ParameterizedTest(name = "setIntegrationSwitch({0}) twice should stay {0}")
    @CsvSource({ "true, true", "false, false" })
    void testSetIntegrationSwitchIdempotent(boolean firstCall, boolean expected) {
        // Call the method twice with the same value
        model.setIntegrationSwitch(firstCall);
        model.setIntegrationSwitch(firstCall);
        assertEquals(expected, model.isIntegrationSwitch(), "Integration switch should remain " + expected + " after two identical calls");
    }

    @Test
    void testSetIntegrationSwitchTrueThenTrueAgain() {
        // Explicit test for the true‑true case (mirrors JSON description)
        model.setIntegrationSwitch(true);
        model.setIntegrationSwitch(true);
        assertTrue(model.isIntegrationSwitch(), "Integration switch should stay true after setting true twice");
    }

    @Test
    void testSetIntegrationSwitchFalseThenFalseAgain() {
        // Explicit test for the false‑false case (mirrors JSON description)
        model.setIntegrationSwitch(false);
        model.setIntegrationSwitch(false);
        assertFalse(model.isIntegrationSwitch(), "Integration switch should stay false after setting false twice");
    }
}
