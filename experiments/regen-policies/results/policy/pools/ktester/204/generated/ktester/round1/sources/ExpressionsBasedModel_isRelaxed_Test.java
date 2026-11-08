package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionsBasedModel_isRelaxed_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the model does not hold external resources
        model = null;
    }

    @Test
    void testDefaultIsRelaxedIsFalse() {
        // A newly constructed model must not be marked as relaxed
        assertFalse(model.isRelaxed(), "New model should not be relaxed");
    }

    @Test
    void testRelaxSetsIsRelaxedTrue() {
        // Calling relax() should flip the flag to true
        // the default (soft) relaxation
        model.relax();
        assertTrue(model.isRelaxed(), "Model should be relaxed after calling relax()");
    }

    @Test
    void testCopyPreservesRelaxedFlag() {
        // Relax the original model
        model.relax();
        // Create a copy using the no‑arg copy() method (which copies the relaxed state)
        ExpressionsBasedModel copy = model.copy();
        // The copy must report the same relaxed state as the original
        assertTrue(copy.isRelaxed(), "Copy should preserve the relaxed flag of the original model");
    }

    @Test
    void testCopyWithRelaxArgument() {
        // The copy(boolean relax) overload should produce a relaxed copy when the argument is true
        ExpressionsBasedModel relaxedCopy = model.copy(true);
        assertTrue(relaxedCopy.isRelaxed(), "copy(true) should produce a relaxed model");
        // And a non‑relaxed copy when the argument is false
        ExpressionsBasedModel nonRelaxedCopy = model.copy(false);
        assertFalse(nonRelaxedCopy.isRelaxed(), "copy(false) should produce a non‑relaxed model");
    }

    @Test
    void testRelaxWithFalseDoesNotSetRelaxed() {
        // relax(false) is defined to keep the model non‑relaxed
        model.relax(false);
        assertFalse(model.isRelaxed(), "Model should remain non‑relaxed after calling relax(false)");
    }

    @Test
    void testCopyPreservesRelaxedFlagWhenOriginalRelaxed() {
        // Relax the original model
        model.relax();
        // Create a copy using the no‑arg copy() method (which copies the relaxed state)
        ExpressionsBasedModel copy = model.copy();
        // The copy must report the same relaxed state as the original
        assertTrue(copy.isRelaxed(), "Copy should preserve the relaxed flag of the original model");
    }

    @Test
    void testCopyPreservesRelaxedFlagWhenOriginalNotRelaxed() {
        // Original model is freshly created → not relaxed
        ExpressionsBasedModel copy = model.copy();
        assertFalse(copy.isRelaxed(), "Copy of a non‑relaxed model should also be non‑relaxed");
    }

    @Test
    void testShallowCopyPreservesRelaxedFlagWhenOriginalRelaxed() {
        // Relax the original model first
        model.relax();
        // Create a shallow copy (shallow = true, prune = false)
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        assertTrue(shallowCopy.isRelaxed(), "Shallow copy of a relaxed model should retain the relaxed flag");
    }

    @Test
    void testShallowCopyDoesNotSetRelaxedFlagWhenOriginalNotRelaxed() {
        // Original model is not relaxed
        // Create a shallow copy (shallow = true, prune = false)
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        assertFalse(shallowCopy.isRelaxed(), "Shallow copy of a non‑relaxed model should not be relaxed");
    }
}
