package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getOptimisationSense_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    /**
     * Sets the private field {@code myOptimisationSense} via reflection.
     *
     * @param model the model whose sense should be changed
     * @param sense the new sense (may be {@code null})
     */
    private static void setSenseViaReflection(ExpressionsBasedModel model, Optimisation.Sense sense) {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myOptimisationSense");
            field.setAccessible(true);
            field.set(model, sense);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set optimisation sense via reflection", e);
        }
    }

    /**
     * Adds a minimal variable and a dummy objective expression so that
     * {@code minimise()} / {@code maximise()} can be called without triggering
     * solver‑related validation errors.
     */
    private static void addDummyData(ExpressionsBasedModel model) {
        // A single variable with bounds.
        model.newVariable("x").lower(0).upper(10);
        // An expression that simply references the variable and contributes to the objective.
        model.newExpression("obj").weight(1).setLinearFactor(0, 1);
    }

    private static Stream<Arguments> provideReflectionCases() {
        return Stream.of(Arguments.of(Optimisation.Sense.MINIMISE), Arguments.of(Optimisation.Sense.MAXIMISE));
    }

    private static Stream<Arguments> provideCopyCases() {
        return Stream.of(Arguments.of(Optimisation.Sense.MINIMISE), Arguments.of(Optimisation.Sense.MAXIMISE));
    }

    private static Stream<Arguments> provideSnapshotCases() {
        return Stream.of(Arguments.of(Optimisation.Sense.MINIMISE), Arguments.of(Optimisation.Sense.MAXIMISE));
    }

    @Test
    void testDefaultOptimisationSenseIsNull() {
        // By default the optimisation sense has not been defined.
        assertNull(model.getOptimisationSense(), "A newly created ExpressionsBasedModel should have a null optimisation sense.");
    }

    @Test
    void testSetOptimisationSense() {
        // The setter is package‑private; the test class resides in the same package,
        // therefore it can be invoked directly.
        // Set to MAX and verify.
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense should be MAX after setting it.");
        // Change to MIN and verify.
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Optimisation sense should be MIN after resetting it.");
    }

    @Test
    void testCopyPreservesOptimisationSense() {
        // Prepare an original model with a defined sense.
        model.setOptimisationSense(Optimisation.Sense.MAX);
        model.newVariable("x").lower(0).upper(10).weight(1);
        model.newExpression("obj").weight(1);
        // Perform a shallow copy (the copy constructor is package‑private, but the public
        // copy() method delegates to it). The copy should retain the optimisation sense.
        ExpressionsBasedModel copy = model.copy();
        assertNotNull(copy.getOptimisationSense(), "Copied model should retain a non‑null optimisation sense.");
        assertEquals(Optimisation.Sense.MAX, copy.getOptimisationSense(), "Copied model should have the same optimisation sense as the original.");
    }

    @Test
    void testCopyWithRelaxedAndShallowFlags() {
        // Verify that the copy constructor (used internally by copy()) also copies the sense
        // when the model is shallow‑copied or relaxed.
        model.setOptimisationSense(Optimisation.Sense.MIN);
        model.newVariable("y").lower(1).upper(5).weight(2);
        model.newExpression("obj2").weight(2);
        // Explicit shallow copy with prune = false.
        ExpressionsBasedModel shallowCopy = new ExpressionsBasedModel(model, true, false);
        assertEquals(Optimisation.Sense.MIN, shallowCopy.getOptimisationSense(), "Shallow copy should preserve the optimisation sense.");
        // Explicit deep copy with prune = true.
        ExpressionsBasedModel deepCopy = new ExpressionsBasedModel(model, false, true);
        assertEquals(Optimisation.Sense.MIN, deepCopy.getOptimisationSense(), "Deep copy (pruned) should also preserve the optimisation sense.");
    }

    @ParameterizedTest(name = "{index}: set via reflection -> {0}")
    @MethodSource("provideReflectionCases")
    void testSenseSetViaReflection(Optimisation.Sense expectedSense) {
        setSenseViaReflection(model, expectedSense);
        assertEquals(expectedSense, model.getOptimisationSense(), "Optimisation sense should match the value set via reflection.");
    }

    @Test
    void testSenseAfterCallingMinimise() {
        addDummyData(model);
        // should set sense to MINIMISE
        model.minimise();
        assertEquals(Optimisation.Sense.MINIMISE, model.getOptimisationSense(), "Calling minimise() must set the optimisation sense to MINIMISE.");
    }

    @Test
    void testSenseAfterCallingMaximise() {
        addDummyData(model);
        // should set sense to MAXIMISE
        model.maximise();
        assertEquals(Optimisation.Sense.MAXIMISE, model.getOptimisationSense(), "Calling maximise() must set the optimisation sense to MAXIMISE.");
    }

    @Test
    void testSequentialMinThenMax() {
        addDummyData(model);
        model.minimise();
        // second call overwrites the previous sense
        model.maximise();
        assertEquals(Optimisation.Sense.MAXIMISE, model.getOptimisationSense(), "After minimise() then maximise(), the sense must be MAXIMISE.");
    }

    @Test
    void testSequentialMaxThenMin() {
        addDummyData(model);
        model.maximise();
        // second call overwrites the previous sense
        model.minimise();
        assertEquals(Optimisation.Sense.MINIMISE, model.getOptimisationSense(), "After maximise() then minimise(), the sense must be MINIMISE.");
    }

    @Test
    void testIdempotentMinimise() {
        addDummyData(model);
        model.minimise();
        Optimisation.Sense first = model.getOptimisationSense();
        // second call should keep the same sense
        model.minimise();
        assertEquals(first, model.getOptimisationSense(), "Calling minimise() twice should leave the sense unchanged (MINIMISE).");
    }

    @Test
    void testIdempotentMaximise() {
        addDummyData(model);
        model.maximise();
        Optimisation.Sense first = model.getOptimisationSense();
        // second call should keep the same sense
        model.maximise();
        assertEquals(first, model.getOptimisationSense(), "Calling maximise() twice should leave the sense unchanged (MAXIMISE).");
    }

    @ParameterizedTest(name = "copy preserves sense {0}")
    @MethodSource("provideCopyCases")
    void testOptimisationSenseAfterCopy(Optimisation.Sense initialSense) {
        model.setOptimisationSense(initialSense);
        // Adding dummy data ensures the copy operation does not fail on empty models.
        addDummyData(model);
        ExpressionsBasedModel copy = model.copy();
        assertEquals(initialSense, copy.getOptimisationSense(), "A copy of the model must retain the original optimisation sense.");
    }

    @ParameterizedTest(name = "snapshot preserves sense {0}")
    @MethodSource("provideSnapshotCases")
    void testOptimisationSenseAfterSnapshot(Optimisation.Sense initialSense) {
        model.setOptimisationSense(initialSense);
        addDummyData(model);
        ExpressionsBasedModel snapshot = model.snapshot();
        assertEquals(initialSense, snapshot.getOptimisationSense(), "A snapshot of the model must preserve the optimisation sense.");
    }

    @Test
    void testOptimisationSenseResetToNull() {
        // Set a non‑null sense first.
        model.setOptimisationSense(Optimisation.Sense.MINIMISE);
        assertNotNull(model.getOptimisationSense(), "Sense should be non‑null after setting it.");
        // Reset to null using the package‑private setter.
        model.setOptimisationSense(null);
        assertNull(model.getOptimisationSense(), "After resetting the optimisation sense to null, getOptimisationSense() must return null.");
    }
}