package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isShallowCopy_Test {

    /**
     * Supplies the data for {@link #testCopyScenarios(Boolean, Boolean, boolean)}.
     * <ul>
     *   <li>(null, null, false) – default construction, not shallow.</li>
     *   <li>(true, false, true) – explicit shallow copy.</li>
     *   <li>(false, false, false) – explicit deep copy.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> copyScenarioProvider() {
        return Stream.of(// default construction (no copy)
        org.junit.jupiter.params.provider.Arguments.of(null, null, false), // shallow copy requested
        org.junit.jupiter.params.provider.Arguments.of(true, false, true), // deep copy requested
        org.junit.jupiter.params.provider.Arguments.of(false, false, false));
    }

    /**
     * The default constructor must create a model that is *not* a shallow copy.
     */
    @Test
    void testDefaultConstructionIsNotShallowCopy() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isShallowCopy(), "A freshly constructed model should not be marked as a shallow copy.");
    }

    /**
     * A copy created with the shallow flag set to true must report true for {@code isShallowCopy()}.
     */
    @Test
    void testExplicitShallowCopy() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // prune flag irrelevant here
        ExpressionsBasedModel shallowCopy = original.copy(true, false);
        assertTrue(shallowCopy.isShallowCopy(), "copy(true, false) should produce a shallow copy.");
    }

    /**
     * A copy created with the shallow flag set to false must report false when the source is not shallow.
     */
    @Test
    void testExplicitNonShallowCopy() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        ExpressionsBasedModel nonShallowCopy = original.copy(false, false);
        assertFalse(nonShallowCopy.isShallowCopy(), "copy(false, false) on a non‑shallow source should not be a shallow copy.");
    }

    /**
     * When copying a model that is already a shallow copy, the resulting model must also be a shallow
     * copy even if the shallow flag passed to {@code copy(...)} is false. This mirrors the behaviour
     * of the internal copy constructor: {@code myShallowCopy = shallow || modelToCopy.isShallowCopy();}.
     */
    @Test
    void testShallowFlagPropagatesFromSource() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // First create a shallow copy
        ExpressionsBasedModel shallow = original.copy(true, false);
        assertTrue(shallow.isShallowCopy(), "First shallow copy should be shallow.");
        // Now copy the shallow instance without requesting a shallow copy
        ExpressionsBasedModel secondCopy = shallow.copy(false, false);
        assertTrue(secondCopy.isShallowCopy(), "Copying a shallow model should preserve the shallow flag even when shallow=false.");
    }

    /**
     * The {@code prune} argument does not influence the shallow‑copy flag. Verify that both
     * {@code prune=true} and {@code prune=false} give the same result for the shallow flag.
     */
    @Test
    void testPruneDoesNotAffectShallowFlag() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        ExpressionsBasedModel shallowPruneTrue = original.copy(true, true);
        ExpressionsBasedModel shallowPruneFalse = original.copy(true, false);
        assertTrue(shallowPruneTrue.isShallowCopy(), "Shallow copy with prune=true must be shallow.");
        assertTrue(shallowPruneFalse.isShallowCopy(), "Shallow copy with prune=false must be shallow.");
    }

    /**
     * Parameterised test that drives the three basic scenarios (default, shallow, non‑shallow)
     * from a single data provider. Demonstrates an alternative, concise way to express the same
     * expectations as the individual tests above.
     */
    @ParameterizedTest(name = "{index}: shallow={0}, prune={1} => expected={2}")
    @MethodSource("copyScenarioProvider")
    void testCopyScenarios(Boolean shallow, Boolean prune, boolean expected) {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // When both parameters are null we are testing the default constructor case
        ExpressionsBasedModel model;
        if (shallow == null) {
            // default model, no copy performed
            model = original;
        } else {
            model = original.copy(shallow, prune);
        }
        assertEquals(expected, model.isShallowCopy(), () -> String.format("copy(shallow=%s, prune=%s) should yield isShallowCopy=%s", shallow, prune, expected));
    }
}
