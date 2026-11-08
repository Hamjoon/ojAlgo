package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_setOptimisationSense_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector.
        model = null;
    }

    /**
     * Supplies the combinations of input sense and the expected stored sense.
     */
    private static Stream<Arguments> provideSenseValues() {
        return Stream.of(Arguments.of(Optimisation.Sense.MAX, Optimisation.Sense.MAX), Arguments.of(Optimisation.Sense.MIN, Optimisation.Sense.MIN), // Passing null should clear the stored sense (i.e., result is null)
        Arguments.of(null, null));
    }

    /**
     * {@code maximise()} should set the optimisation sense to {@link Optimisation.Sense#MAX}.
     */
    @Test
    void testMaximiseSetsOptimisationSenseToMax() {
        // Act
        model.maximise();
        // Assert
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "maximise() must set the optimisation sense to MAX");
    }

    /**
     * {@code minimise()} should set the optimisation sense to {@link Optimisation.Sense#MIN}.
     */
    @Test
    void testMinimiseSetsOptimisationSenseToMin() {
        // Act
        model.minimise();
        // Assert
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "minimise() must set the optimisation sense to MIN");
    }

    /**
     * Directly invoke the package‑private {@code setOptimisationSense} method via reflection
     * and verify that the internal state is updated accordingly.
     */
    @Test
    void testSetOptimisationSenseViaReflection() throws Exception {
        // Arrange
        Method setSense = ExpressionsBasedModel.class.getDeclaredMethod("setOptimisationSense", Optimisation.Sense.class);
        setSense.setAccessible(true);
        // Act – set to MIN
        setSense.invoke(model, Optimisation.Sense.MIN);
        // Assert
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Reflection call should set sense to MIN");
        // Act – set to MAX
        setSense.invoke(model, Optimisation.Sense.MAX);
        // Assert
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Reflection call should set sense to MAX");
    }

    /**
     * A newly created model has no optimisation sense set; the getter should return {@code null}.
     */
    @Test
    void testDefaultOptimisationSenseIsNull() {
        assertNull(model.getOptimisationSense(), "A fresh model must have a null optimisation sense");
    }

    /**
     * Parameterised test that sets the optimisation sense via reflection and checks the result.
     * Covers setting to {@code MAX}, {@code MIN} and clearing with {@code null}.
     */
    @ParameterizedTest(name = "setOptimisationSense({0}) → expected {1}")
    @MethodSource("provideSenseValues")
    void testSetOptimisationSenseParameterized(Optimisation.Sense input, Optimisation.Sense expected) throws Exception {
        // Arrange
        Method setSense = ExpressionsBasedModel.class.getDeclaredMethod("setOptimisationSense", Optimisation.Sense.class);
        setSense.setAccessible(true);
        // Act
        setSense.invoke(model, input);
        // Assert
        assertEquals(expected, model.getOptimisationSense(), () -> "After invoking setOptimisationSense(" + input + "), expected sense " + expected);
    }

    /**
     * Setting the sense twice should overwrite the previous value.
     * The test sets MAX first, then MIN, and verifies that the final sense is MIN.
     */
    @Test
    void testOverwritingOptimisationSense() throws Exception {
        // Arrange
        Method setSense = ExpressionsBasedModel.class.getDeclaredMethod("setOptimisationSense", Optimisation.Sense.class);
        setSense.setAccessible(true);
        // First set to MAX
        setSense.invoke(model, Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "After first set to MAX, sense should be MAX");
        // Overwrite with MIN
        setSense.invoke(model, Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "After overwriting with MIN, sense should be MIN");
    }
}
