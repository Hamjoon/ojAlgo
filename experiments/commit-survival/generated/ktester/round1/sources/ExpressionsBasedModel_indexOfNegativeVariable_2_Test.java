package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfNegativeVariable_2_Test {

    private ExpressionsBasedModel model;

    // a variable with a positive weight
    private Variable posVar;

    // a variable with a negative weight
    private Variable negVar;

    /* --------------------------------------------------------------
     *  Parameterised tests for the normal (non‑exceptional) behaviour.
     * -------------------------------------------------------------- */
    @Nested
    @DisplayName("Normal behaviour")
    class NormalBehaviour {

        /**
         * Provides test arguments for the normal scenarios.
         *
         * @return a stream of {@link Arguments}:
         *         model configuration, index to query, expected integer result, test description
         */
        static Stream<Arguments> normalCases() {
            return Stream.of(// 1️⃣ Model has no variables – any index is non‑negative → -1
            Arguments.of((Consumer<ExpressionsBasedModel>) null, new IntIndex(0), -1, "Empty model – any index should return -1"), // 2️⃣ Single positive variable – not negative → -1
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> m.newVariable("v").weight(1), new IntIndex(0), -1, "Single positive variable – should return -1"), // 3️⃣ Single negative variable – first (and only) negative → 0
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> m.newVariable("v").weight(-1), new IntIndex(0), 0, "Single negative variable – should return 0"), // 4️⃣ Three variables: +, -, - – query the third (global index 2) → negative list index 1
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> {
                m.newVariable("p").weight(1);
                m.newVariable("n1").weight(-1);
                m.newVariable("n2").weight(-2);
            }, new IntIndex(2), 1, "Third variable in (+, -, -) model – should be second negative (index 1)"), // 5️⃣ Variable with zero coefficient – not negative → -1
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> m.newVariable("zero").weight(0), new IntIndex(0), -1, "Zero‑coefficient variable – should return -1"), // 6️⃣ First negative variable in a mixed model – should be index 0
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> {
                m.newVariable("p").weight(1);
                m.newVariable("n1").weight(-1);
                m.newVariable("n2").weight(-2);
            }, new IntIndex(1), 0, "First negative variable in a mixed model – should be index 0"));
        }

        @ParameterizedTest(name = "[{index}] {3}")
        @MethodSource("normalCases")
        void testIndexOfNegativeVariable_Normal(Consumer<ExpressionsBasedModel> configurator, IntIndex queryIndex, int expected, String description) {
            configureModel(model, configurator);
            int actual = model.indexOfNegativeVariable(queryIndex);
            assertEquals(expected, actual, () -> String.format("Expected %d but got %d for query %s (%s)", expected, actual, queryIndex, description));
        }
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for exceptional behaviour.
     * -------------------------------------------------------------- */
    @Nested
    @DisplayName("Exceptional behaviour")
    class ExceptionalBehaviour {

        /**
         * Provides test arguments for the exception scenarios.
         *
         * @return a stream of {@link Arguments}:
         *         model configuration, index to query, expected exception class, test description
         */
        static Stream<Arguments> exceptionCases() {
            return Stream.of(// Negative index
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> {
                m.newVariable("a").weight(1);
                m.newVariable("b").weight(-1);
            }, new IntIndex(-1), IndexOutOfBoundsException.class, "Negative IntIndex should trigger IndexOutOfBoundsException"), // Index equal to size (out of range)
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> {
                m.newVariable("a").weight(1);
                m.newVariable("b").weight(-1);
            }, new IntIndex(2), IndexOutOfBoundsException.class, "IntIndex equal to number of variables should trigger IndexOutOfBoundsException"), // Very large index
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> m.newVariable("only").weight(1), new IntIndex(Integer.MAX_VALUE), IndexOutOfBoundsException.class, "Huge IntIndex should trigger IndexOutOfBoundsException"), // Null IntIndex
            Arguments.of((Consumer<ExpressionsBasedModel>) m -> m.newVariable("v").weight(-1), null, NullPointerException.class, "Null IntIndex should trigger NullPointerException"));
        }

        @ParameterizedTest(name = "[{index}] {3}")
        @MethodSource("exceptionCases")
        void testIndexOfNegativeVariable_Exceptions(Consumer<ExpressionsBasedModel> configurator, IntIndex queryIndex, Class<? extends Exception> expectedException, String description) {
            configureModel(model, configurator);
            assertThrows(expectedException, () -> model.indexOfNegativeVariable(queryIndex), () -> String.format("Expected %s for query %s (%s)", expectedException.getSimpleName(), queryIndex, description));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
        // Positive variable – weight +1 (default)
        posVar = model.newVariable("pos").weight(1);
        // Negative variable – weight -1 makes the variable “negative” for the model
        negVar = model.newVariable("neg").weight(-1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the model will be garbage‑collected.
        model = null;
    }

    /* --------------------------------------------------------------
     *  Helper to create a model according to a supplied configuration.
     * -------------------------------------------------------------- */
    private static void configureModel(ExpressionsBasedModel m, Consumer<ExpressionsBasedModel> configurator) {
        if (configurator != null) {
            configurator.accept(m);
        }
    }

    @Test
    void testIndexOfNegativeVariableReturnsMinusOneForPositiveVariable() {
        // Global index of the positive variable
        int globalIdx = model.indexOf(posVar);
        // The variable is not part of the negative‑variable list → expect -1
        int negIdx = model.indexOfNegativeVariable(globalIdx);
        assertEquals(-1, negIdx, "Positive variable should not appear in negative list");
        // Same test using the IntIndex overload
        IntIndex intIdx = IntIndex.of(globalIdx);
        assertEquals(-1, model.indexOfNegativeVariable(intIdx), "Positive variable should return -1 via IntIndex overload");
    }

    @Test
    void testIndexOfNegativeVariableFindsFirstNegativeVariable() {
        // Global index of the negative variable
        int globalNegIdx = model.indexOf(negVar);
        // It should be the first (and only) entry in the negative list → index 0
        int negIdx = model.indexOfNegativeVariable(globalNegIdx);
        assertEquals(0, negIdx, "First negative variable should have index 0");
        // Verify the IntIndex overload yields the same result
        IntIndex intIdx = IntIndex.of(globalNegIdx);
        assertEquals(0, model.indexOfNegativeVariable(intIdx), "IntIndex overload should also return 0 for first negative variable");
    }

    @Test
    void testIndexOfNegativeVariableWithMultipleNegatives() {
        // Add another negative variable – it should appear after the first one
        Variable secondNeg = model.newVariable("neg2").weight(-2);
        int globalSecondNeg = model.indexOf(secondNeg);
        // First negative variable should still be at position 0
        assertEquals(0, model.indexOfNegativeVariable(model.indexOf(negVar)), "First negative variable should remain at index 0");
        // Second negative variable should be at position 1
        assertEquals(1, model.indexOfNegativeVariable(globalSecondNeg), "Second negative variable should be at index 1");
        // IntIndex version for the second negative variable
        assertEquals(1, model.indexOfNegativeVariable(IntIndex.of(globalSecondNeg)), "IntIndex overload should return 1 for the second negative variable");
    }
}