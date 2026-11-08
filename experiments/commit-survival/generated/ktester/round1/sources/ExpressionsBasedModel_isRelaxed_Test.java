package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isRelaxed_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh model instance
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up per‑test
        model = null;
    }

    private static Stream<Boolean> booleanRelaxArguments() {
        return Stream.of(Boolean.TRUE, Boolean.FALSE);
    }

    /**
     * By default a newly created model must not be in a relaxed state.
     */
    @Test
    void testDefaultIsRelaxedIsFalse() {
        // The method is package‑private, but the test class resides in the same package,
        // therefore it can be called directly.
        assertFalse(model.isRelaxed(), "A freshly constructed ExpressionsBasedModel should not be relaxed.");
    }

    /**
     * After invoking {@code relax()} the model must report a relaxed state.
     */
    @Test
    void testRelaxSetsIsRelaxedTrue() {
        // public API that should set myRelaxed = true
        model.relax();
        assertTrue(model.isRelaxed(), "Calling relax() should put the model into a relaxed state.");
    }

    /**
     * The relaxed flag is copied correctly when using the copy constructor.
     */
    @Test
    void testCopyPreservesRelaxedFlag() {
        // put the original model into a relaxed state
        model.relax();
        // shallow copy (the flag is copied regardless of shallow/prune settings)
        // public copy() uses shallow = false, prune = false
        ExpressionsBasedModel copy = model.copy();
        assertTrue(copy.isRelaxed(), "A copy of a relaxed model should also be relaxed.");
        // also test the protected copy constructor via the public API that allows specifying flags
        ExpressionsBasedModel deepCopy = new ExpressionsBasedModel(model, false, false);
        assertTrue(deepCopy.isRelaxed(), "Deep copy constructed via the protected constructor should preserve the relaxed flag.");
    }

    @Test
    void testRelaxIsIdempotent() {
        model.relax();
        // second call must leave the flag true
        model.relax();
        assertTrue(model.isRelaxed(), "Calling relax() on an already relaxed model must keep the flag true.");
    }

    @ParameterizedTest(name = "relax({0}) must set isRelaxed() true")
    @MethodSource("booleanRelaxArguments")
    void testRelaxBooleanSetsIsRelaxedTrue(boolean soft) {
        model.relax(soft);
        assertTrue(model.isRelaxed(), "Calling relax(" + soft + ") should mark the model as relaxed.");
    }

    @Test
    void testCopyWithRelaxationFlagTrue() {
        // copy(true) should create a relaxed copy regardless of the original state
        ExpressionsBasedModel copy = model.copy(true);
        assertTrue(copy.isRelaxed(), "ExpressionsBasedModel.copy(true) must produce a relaxed model.");
    }

    @Test
    void testCopyWithRelaxationFlagFalse() {
        // copy(false) should create a non‑relaxed copy
        ExpressionsBasedModel copy = model.copy(false);
        assertFalse(copy.isRelaxed(), "ExpressionsBasedModel.copy(false) must produce a non‑relaxed model.");
    }

    @Test
    void testRelaxationPersistsAfterAddingVariable() {
        // set relaxed
        model.relax();
        // modify model
        model.addVariable();
        assertTrue(model.isRelaxed(), "Adding a variable after relaxation must not reset the relaxed flag.");
    }

    @Test
    void testRelaxationPersistsAfterAddingExpression() {
        // set relaxed
        model.relax();
        // modify model
        model.addExpression();
        assertTrue(model.isRelaxed(), "Adding an expression after relaxation must not reset the relaxed flag.");
    }
}
