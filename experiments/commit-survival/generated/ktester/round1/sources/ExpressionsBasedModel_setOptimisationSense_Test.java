package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setOptimisationSense_Test {

    private ExpressionsBasedModel model;

    /**
     * Initialise a fresh model before each test. The default constructor creates a model
     * with a fresh {@link Optimisation.Options} instance and no optimisation sense set
     * (i.e. {@code null}).
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    /**
     * Clean up after each test – not strictly required for this class but kept for symmetry.
     */
    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    private static Stream<Arguments> singleCallProvider() {
        return Stream.of(Arguments.of(Optimisation.Sense.MIN, Optimisation.Sense.MIN), Arguments.of(Optimisation.Sense.MAX, Optimisation.Sense.MAX), Arguments.of(null, null));
    }

    private static Stream<Arguments> multipleCallProvider() {
        return Stream.of(// No calls – the model should retain its default (null)
        Arguments.of(Collections.emptyList(), null), // MIN then MAX → last wins (MAX)
        Arguments.of(Arrays.asList(Optimisation.Sense.MIN, Optimisation.Sense.MAX), Optimisation.Sense.MAX), // MIN then MIN → still MIN
        Arguments.of(Arrays.asList(Optimisation.Sense.MIN, Optimisation.Sense.MIN), Optimisation.Sense.MIN), // MIN then null → cleared to null
        Arguments.of(Arrays.asList(Optimisation.Sense.MIN, null), null), // Single null call on a fresh model → remains null
        Arguments.of(Collections.singletonList(null), null));
    }

    /**
     * Verify that a newly created model has no optimisation sense defined.
     */
    @Test
    void testDefaultOptimisationSenseIsNull() {
        assertNull(model.getOptimisationSense(), "A freshly created model should have a null optimisation sense");
    }

    /**
     * Directly invoke the package‑private {@code setOptimisationSense} method with {@code MAX}.
     */
    @Test
    void testSetOptimisationSenseToMax() {
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense should be set to MAX");
    }

    /**
     * Directly invoke the package‑private {@code setOptimisationSense} method with {@code MIN}.
     */
    @Test
    void testSetOptimisationSenseToMin() {
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Optimisation sense should be set to MIN");
    }

    /**
     * Ensure that the public {@code maximise()} method internally sets the sense to {@code MAX}.
     */
    @Test
    void testMaximiseSetsOptimisationSenseToMax() {
        // maximise() returns a Result; we are only interested in the side‑effect on the model.
        // The actual optimisation may fail for an empty model, but the sense must be set.
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "maximise() should set optimisation sense to MAX");
    }

    /**
     * Ensure that the public {@code minimise()} method internally sets the sense to {@code MIN}.
     */
    @Test
    void testMinimiseSetsOptimisationSenseToMin() {
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "minimise() should set optimisation sense to MIN");
    }

    /**
     * Passing {@code null} should simply store a {@code null} sense – no exception is expected.
     */
    @Test
    void testSetOptimisationSenseWithNull() {
        model.setOptimisationSense(null);
        assertNull(model.getOptimisationSense(), "Setting optimisation sense to null should result in a null sense");
    }

    /**
     * Single‑call matrix:
     * <ul>
     *   <li>MIN  → expect MIN</li>
     *   <li>MAX  → expect MAX</li>
     *   <li>null → expect null</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] setOptimisationSense({0}) → expect {1}")
    @MethodSource("singleCallProvider")
    void testSetOptimisationSense_SingleCall(Optimisation.Sense input, Optimisation.Sense expected) {
        model.setOptimisationSense(input);
        assertEquals(expected, model.getOptimisationSense(), () -> "After setting " + input + " the model should return " + expected);
    }

    /**
     * Multiple‑call matrix (each entry is a list of successive calls and the expected final sense):
     * <ul>
     *   <li>[] → null (default)</li>
     *   <li>[MIN, MAX] → MAX</li>
     *   <li>[MIN, MIN] → MIN</li>
     *   <li>[MIN, null] → null</li>
     *   <li>[null] → null</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] calls {0} → final {1}")
    @MethodSource("multipleCallProvider")
    void testSetOptimisationSense_MultipleCalls(List<Optimisation.Sense> calls, Optimisation.Sense expected) {
        // Apply each call in order
        for (Optimisation.Sense sense : calls) {
            model.setOptimisationSense(sense);
        }
        assertEquals(expected, model.getOptimisationSense(), () -> "After calls " + calls + " the final sense should be " + expected);
    }
}
