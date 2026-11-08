package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_isShallowCopy_Test {

    /**
     * A simple model used as a source for copy‑construction.
     */
    private ExpressionsBasedModel sourceModel;

    @BeforeEach
    void setupBeforeEach() {
        // Create a minimal model (one variable, one expression) to act as the source.
        sourceModel = new ExpressionsBasedModel();
        Variable x = sourceModel.newVariable("x").lower(0).upper(10).weight(1);
        Expression expr = sourceModel.newExpression("expr");
        expr.set(x, 1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; not strictly required but keeps the pattern clean.
        sourceModel = null;
    }

    private static Stream<Arguments> provideCopyShallowPruneCases() {
        return Stream.of(// shallow copy, no pruning
        Arguments.of(true, false, true), // deep copy, no pruning
        Arguments.of(false, false, false), // shallow copy with pruning
        Arguments.of(true, true, true), // deep copy with pruning
        Arguments.of(false, true, false));
    }

    /**
     * Default construction must yield a non‑shallow copy.
     */
    @Test
    void testIsShallowCopy_DefaultConstructor() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isShallowCopy(), "A model created via the public constructor should not be a shallow copy.");
    }

    /**
     * Copy‑construction with {@code shallow = true} must set the flag to {@code true}.
     */
    @Test
    void testIsShallowCopy_ExplicitShallowCopy() {
        // shallow = true, prune = false (prune does not affect the flag)
        ExpressionsBasedModel shallowCopy = new ExpressionsBasedModel(sourceModel, true, false);
        assertTrue(shallowCopy.isShallowCopy(), "A model created with shallow = true should report being a shallow copy.");
    }

    /**
     * If the source model is already a shallow copy, any further copy (even with {@code shallow = false})
     * must still report {@code true} because the flag is propagated.
     */
    @Test
    void testIsShallowCopy_PropagationFromSource() {
        // First create a shallow copy of the source.
        ExpressionsBasedModel firstShallow = new ExpressionsBasedModel(sourceModel, true, false);
        assertTrue(firstShallow.isShallowCopy(), "First shallow copy should be shallow.");
        // Now copy the shallow model with shallow = false.
        ExpressionsBasedModel secondCopy = new ExpressionsBasedModel(firstShallow, false, false);
        assertTrue(secondCopy.isShallowCopy(), "A copy of a shallow model must also be reported as shallow, regardless of the shallow flag.");
    }

    /**
     * Copy‑construction with {@code shallow = true} must set the flag to {@code true}.
     */
    @Test
    void testIsShallowCopy_ExplicitShallowCopy_Constructor() {
        // shallow = true, prune = false (prune does not affect the flag)
        ExpressionsBasedModel shallowCopy = new ExpressionsBasedModel(sourceModel, true, false);
        assertTrue(shallowCopy.isShallowCopy(), "A model created with shallow = true should report being a shallow copy.");
    }

    /**
     * Copy‑construction with {@code shallow = false} must keep the flag {@code false}.
     */
    @Test
    void testIsShallowCopy_ExplicitDeepCopy_Constructor() {
        ExpressionsBasedModel deepCopy = new ExpressionsBasedModel(sourceModel, false, false);
        assertFalse(deepCopy.isShallowCopy(), "A model created with shallow = false should not be a shallow copy.");
    }

    /**
     * Parameterised test covering the {@code copy(boolean shallow, boolean prune)} method.
     *
     * The JSON test suite defines four relevant combinations:
     * <ul>
     *   <li>shallow = true, prune = false  → true
     *   <li>shallow = false, prune = false → false
     *   <li>shallow = true, prune = true   → true
     *   <li>shallow = false, prune = true  → false
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] shallow={0}, prune={1} ⇒ expected={2}")
    @MethodSource("provideCopyShallowPruneCases")
    void testIsShallowCopy_copyMethod(boolean shallow, boolean prune, boolean expected) {
        ExpressionsBasedModel copy = sourceModel.copy(shallow, prune);
        assertEquals(expected, copy.isShallowCopy(), () -> String.format("copy(shallow=%s, prune=%s) should report isShallowCopy=%s", shallow, prune, expected));
    }

    /**
     * The no‑argument {@code copy()} method always performs a deep copy (shallow flag = false).
     */
    @Test
    void testIsShallowCopy_noArgCopy() {
        ExpressionsBasedModel deepCopy = sourceModel.copy();
        assertFalse(deepCopy.isShallowCopy(), "The no‑argument copy() method should produce a deep copy (isShallowCopy = false).");
    }

    /**
     * The {@code copy(boolean relax)} overload relaxes the model but never makes it shallow.
     */
    @Test
    void testIsShallowCopy_copyRelax() {
        // relax = true
        ExpressionsBasedModel relaxedCopy = sourceModel.copy(true);
        assertFalse(relaxedCopy.isShallowCopy(), "copy(relax=true) must not affect the shallow‑copy flag; expected false.");
    }

    /**
     * Creating a shallow copy and then deep‑copying that instance via {@code copy(false, false)}
     * must result in a model that is still shallow because the source was shallow.
     * This matches the propagation rule verified in {@link #testIsShallowCopy_PropagationFromSource()}.
     */
    @Test
    void testIsShallowCopy_shallowThenDeepCopy() {
        // Step 1: shallow copy of the original source
        ExpressionsBasedModel shallow = sourceModel.copy(true, false);
        assertTrue(shallow.isShallowCopy(), "Initial shallow copy should be shallow.");
        // Step 2: deep copy of the shallow copy (shallow flag false)
        ExpressionsBasedModel deepFromShallow = shallow.copy(false, false);
        assertTrue(deepFromShallow.isShallowCopy(), "Copying a shallow model (even with shallow=false) must still report shallow.");
    }

    /**
     * Additional sanity check: copying a shallow model with the no‑arg {@code copy()} method
     * also propagates the shallow flag.
     */
    @Test
    void testIsShallowCopy_shallowThenNoArgCopy() {
        ExpressionsBasedModel shallow = sourceModel.copy(true, false);
        assertTrue(shallow.isShallowCopy(), "Initial shallow copy should be shallow.");
        // no‑arg copy → deep copy semantics
        ExpressionsBasedModel copy = shallow.copy();
        assertTrue(copy.isShallowCopy(), "Copying a shallow model with copy() must retain the shallow flag.");
    }
}
