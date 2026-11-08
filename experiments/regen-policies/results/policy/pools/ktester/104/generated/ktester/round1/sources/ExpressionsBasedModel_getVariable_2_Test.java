package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariable_2_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add three variables.
        model = new ExpressionsBasedModel();
        // index 0
        varA = model.addVariable();
        // index 1
        varB = model.addVariable();
        // index 2
        varC = model.addVariable();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /* -------------------------------------------------------------
     *  Group: getVariable(IntIndex) - valid indices
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validIndicesProvider() {
        return Stream.of(// requires at least 5 variables
        Arguments.of(new IntIndex(0)), Arguments.of(new IntIndex(2)), Arguments.of(new IntIndex(4)));
    }

    /* -------------------------------------------------------------
     *  Group: getVariable(IntIndex) - boundary / error conditions
     * ------------------------------------------------------------- */
    private static Stream<Arguments> invalidIndicesProvider() {
        return Stream.of(// model currently has 3 vars
        Arguments.of(new IntIndex(-1), IndexOutOfBoundsException.class), Arguments.of(new IntIndex(5), IndexOutOfBoundsException.class), Arguments.of(new IntIndex(100), IndexOutOfBoundsException.class), Arguments.of(null, NullPointerException.class));
    }

    @Test
    void testGetVariableReturnsCorrectVariable() {
        // Retrieve each variable using an IntIndex and verify identity.
        Variable retrievedA = model.getVariable(new IntIndex(0));
        Variable retrievedB = model.getVariable(new IntIndex(1));
        Variable retrievedC = model.getVariable(new IntIndex(2));
        assertSame(varA, retrievedA, "Variable at index 0 should be varA");
        assertSame(varB, retrievedB, "Variable at index 1 should be varB");
        assertSame(varC, retrievedC, "Variable at index 2 should be varC");
    }

    @Test
    void testGetVariableWithNegativeIndexThrows() {
        // Negative indices are illegal – expect IndexOutOfBoundsException.
        IntIndex negativeIndex = new IntIndex(-1);
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(negativeIndex), "Calling getVariable with a negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableBeyondSizeThrows() {
        // The model currently holds three variables (indices 0‑2). Index 3 is out of range.
        IntIndex outOfRange = new IntIndex(3);
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(outOfRange), "Calling getVariable with an index beyond the variable list should throw IndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "Valid index {0}")
    @MethodSource("validIndicesProvider")
    void testGetVariableValidIndices(IntIndex index) {
        // Ensure the model contains enough variables for the requested index.
        int requiredSize = index.index + 1;
        while (model.getVariables().size() < requiredSize) {
            // add dummy variables as needed
            model.addVariable();
        }
        Variable expected = model.getVariable(index);
        Variable actual = model.getVariable(index);
        assertSame(expected, actual, () -> "Variable at index " + index.index + " should be the same instance returned by getVariable");
    }

    @ParameterizedTest(name = "Invalid index {0} expects {1}")
    @MethodSource("invalidIndicesProvider")
    void testGetVariableInvalidIndices(IntIndex index, Class<? extends Throwable> expectedException) {
        // For non‑null indices that are out of range we keep the model as‑is (size = 3)
        assertThrows(expectedException, () -> model.getVariable(index), () -> "Calling getVariable with index " + (index == null ? "null" : index.index) + " should throw " + expectedException.getSimpleName());
    }

    /* -------------------------------------------------------------
     *  Group: getVariable(IntIndex) - empty model
     * ------------------------------------------------------------- */
    @Test
    void testGetVariableOnEmptyModelThrows() {
        // no variables added
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> emptyModel.getVariable(new IntIndex(0)), "Calling getVariable on an empty model should throw IndexOutOfBoundsException");
    }

    /* -------------------------------------------------------------
     *  Group: getVariable(IntIndex) - after model copy
     * ------------------------------------------------------------- */
    @Test
    void testGetVariableAfterCopyReturnsDistinctInstance() {
        // The @BeforeEach already added three variables; we need at least two.
        // shallow copy – variables are duplicated
        ExpressionsBasedModel copy = model.copy();
        Variable original = model.getVariable(new IntIndex(1));
        Variable fromCopy = copy.getVariable(new IntIndex(1));
        assertNotSame(original, fromCopy, "Variable obtained from the copied model should be a different instance from the original");
    }

    /* -------------------------------------------------------------
     *  Group: getVariable(IntIndex) - using toIntIndex conversion
     * ------------------------------------------------------------- */
    @Test
    void testGetVariableUsingToIntIndex() {
        // Ensure we have at least four variables (indices 0‑3)
        while (model.getVariables().size() < 4) {
            model.addVariable();
        }
        IntIndex idx = model.toIntIndex(3);
        Variable viaIdx = model.getVariable(idx);
        Variable direct = model.getVariable(new IntIndex(3));
        assertSame(direct, viaIdx, "Variable retrieved via toIntIndex(3) should be identical to the one retrieved with new IntIndex(3)");
    }

    /* -------------------------------------------------------------
     *  Group: getVariable(IntIndex) - large model
     * ------------------------------------------------------------- */
    @Test
    void testGetVariableFromLargeModel() {
        ExpressionsBasedModel largeModel = new ExpressionsBasedModel();
        final int total = 1_000;
        for (int i = 0; i < total; i++) {
            largeModel.addVariable();
        }
        IntIndex target = new IntIndex(500);
        Variable var = largeModel.getVariable(target);
        assertNotNull(var, "Variable at index 500 in a large model should not be null");
        // Additional sanity check: the variable's index within the list matches the request.
        assertSame(var, largeModel.getVariable(new IntIndex(500)), "Direct retrieval should return the same instance as the earlier call");
    }
}