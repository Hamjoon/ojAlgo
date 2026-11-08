package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isRelaxed_Test {

    static Stream<Arguments> relaxBooleanProvider() {
        return Stream.of(// soft = true
        Arguments.of(true, true), // soft = false
        Arguments.of(false, true));
    }

    static Stream<Arguments> copyWithRelaxFlagProvider() {
        return Stream.of(// source non‑relaxed, copyRelaxFlag = true → relaxed
        Arguments.of(true, false, true), // source non‑relaxed, copyRelaxFlag = false → not relaxed
        Arguments.of(false, false, false));
    }

    static Stream<Arguments> copyShallowPruneProvider() {
        return Stream.of(// non‑relaxed source, any shallow/prune → non‑relaxed copy
        Arguments.of(false, false, false, false), // relaxed source, shallow+prune true → relaxed copy
        Arguments.of(true, true, true, true));
    }

    static Stream<Arguments> copyRelaxPrecedenceProvider() {
        return Stream.of(// source relaxed, copyRelaxFlag = false → still relaxed (source wins)
        Arguments.of(true, false, true), // source non‑relaxed, copyRelaxFlag = true → relaxed (copy flag wins)
        Arguments.of(false, true, true));
    }

    @Test
    void testDefaultIsRelaxedIsFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isRelaxed(), "A newly created model must not be relaxed");
    }

    @Test
    void testRelaxMethodSetsRelaxedFlag() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isRelaxed(), "Pre‑condition: model should start non‑relaxed");
        // public API that should switch the model to relaxed mode
        model.relax();
        assertTrue(model.isRelaxed(), "Calling relax() must set the relaxed flag to true");
    }

    @Test
    void testCopyPreservesRelaxedState() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // put the original into relaxed mode
        original.relax();
        assertTrue(original.isRelaxed(), "Original model must be relaxed before copying");
        // copy should retain the relaxed flag
        ExpressionsBasedModel copy = original.copy();
        assertTrue(copy.isRelaxed(), "A copy of a relaxed model must also be relaxed");
    }

    @Test
    void testCopyWithShallowAndPrunePreservesRelaxedState() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // ensure relaxed flag is true
        original.relax();
        assertTrue(original.isRelaxed(), "Original model must be relaxed before copy");
        // The protected copy‑constructor is exercised via the public copy(boolean,boolean) overload.
        ExpressionsBasedModel shallowCopy = original.copy(true, false);
        assertTrue(shallowCopy.isRelaxed(), "Shallow copy should preserve the relaxed flag of the source model");
        ExpressionsBasedModel prunedCopy = original.copy(false, true);
        assertTrue(prunedCopy.isRelaxed(), "Pruned copy should preserve the relaxed flag of the source model");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] relax({0}) → expected relaxed = {1}")
    @MethodSource("relaxBooleanProvider")
    void testRelaxBoolean(boolean soft, boolean expected) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.relax(soft);
        assertEquals(expected, model.isRelaxed(), "Calling relax(" + soft + ") should set relaxed flag to " + expected);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] copy(relax={0}) on model already relaxed = {1} → expected = {2}")
    @MethodSource("copyWithRelaxFlagProvider")
    void testCopyWithRelaxFlag(boolean copyRelaxFlag, boolean modelInitiallyRelaxed, boolean expected) {
        ExpressionsBasedModel source = new ExpressionsBasedModel();
        if (modelInitiallyRelaxed) {
            source.relax();
        }
        ExpressionsBasedModel copy = source.copy(copyRelaxFlag);
        assertEquals(expected, copy.isRelaxed(), "copy(relax=" + copyRelaxFlag + ") on a " + (modelInitiallyRelaxed ? "relaxed" : "non‑relaxed") + " model should result in relaxed=" + expected);
    }

    // -----------------------------------------------------------------
    @Test
    void testCopyWithoutArgumentsPreservesCurrentRelaxedState() {
        // case 1: source not relaxed
        ExpressionsBasedModel notRelaxed = new ExpressionsBasedModel();
        ExpressionsBasedModel copy1 = notRelaxed.copy();
        assertFalse(copy1.isRelaxed(), "Copy of a non‑relaxed model must also be non‑relaxed");
        // case 2: source relaxed
        ExpressionsBasedModel relaxed = new ExpressionsBasedModel();
        relaxed.relax();
        ExpressionsBasedModel copy2 = relaxed.copy();
        assertTrue(copy2.isRelaxed(), "Copy of a relaxed model must retain the relaxed flag");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] copy(shallow={0}, prune={1}) on model relaxed={2} → expected relaxed={3}")
    @MethodSource("copyShallowPruneProvider")
    void testCopyShallowPrune(boolean shallow, boolean prune, boolean sourceRelaxed, boolean expected) {
        ExpressionsBasedModel source = new ExpressionsBasedModel();
        if (sourceRelaxed) {
            source.relax();
        }
        ExpressionsBasedModel copy = source.copy(shallow, prune);
        assertEquals(expected, copy.isRelaxed(), String.format("copy(%b,%b) on a %s model should result in relaxed=%b", shallow, prune, sourceRelaxed ? "relaxed" : "non‑relaxed", expected));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] source relaxed={0}, copy(relax={1}) → expected relaxed={2}")
    @MethodSource("copyRelaxPrecedenceProvider")
    void testCopyRelaxPrecedence(boolean sourceRelaxed, boolean copyRelaxFlag, boolean expected) {
        ExpressionsBasedModel source = new ExpressionsBasedModel();
        if (sourceRelaxed) {
            source.relax();
        }
        ExpressionsBasedModel copy = source.copy(copyRelaxFlag);
        assertEquals(expected, copy.isRelaxed(), String.format("When source relaxed=%b and copy(relax=%b) → expected relaxed=%b", sourceRelaxed, copyRelaxFlag, expected));
    }

    // -----------------------------------------------------------------
    @Test
    void testRepeatedRelaxCallsKeepRelaxedTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // first call makes it relaxed
        model.relax();
        assertTrue(model.isRelaxed(), "Model should be relaxed after first relax()");
        // second call – same method
        model.relax();
        assertTrue(model.isRelaxed(), "Model must stay relaxed after a second relax()");
        // relax(true)
        model.relax(true);
        assertTrue(model.isRelaxed(), "Model must stay relaxed after relax(true)");
        // relax(false)
        model.relax(false);
        assertTrue(model.isRelaxed(), "Model must stay relaxed after relax(false)");
    }

    // -----------------------------------------------------------------
    @Test
    void testRelaxAfterCopyDoesNotAffectOriginal() {
        // non‑relaxed
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // also non‑relaxed
        ExpressionsBasedModel copy = original.copy();
        // relax the copy only
        copy.relax();
        assertTrue(copy.isRelaxed(), "Copy should be relaxed after calling relax()");
        assertFalse(original.isRelaxed(), "Original must remain non‑relaxed after the copy is relaxed");
    }
}
