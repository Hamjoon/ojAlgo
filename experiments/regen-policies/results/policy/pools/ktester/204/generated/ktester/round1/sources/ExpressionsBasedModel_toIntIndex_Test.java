package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toIntIndex_Test {

    private ExpressionsBasedModel model;

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // add three variables – the order of addition defines the internal index
        // index 0
        model.addVariable();
        // index 1
        model.addVariable();
        // index 2
        model.addVariable();
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> validIndexProvider() {
        return Stream.of(Arguments.of(0, 0), Arguments.of(1, 1), Arguments.of(2, 2));
    }

    /**
     * Verify that a valid index returns the expected {@link IntIndex}.
     */
    @Test
    void testToIntIndexReturnsCorrectIntIndex() {
        // index 0
        IntIndex idx0 = model.toIntIndex(0);
        Assertions.assertEquals(IntIndex.of(0), idx0, "toIntIndex(0) should return IntIndex.of(0)");
        // index 1
        IntIndex idx1 = model.toIntIndex(1);
        Assertions.assertEquals(IntIndex.of(1), idx1, "toIntIndex(1) should return IntIndex.of(1)");
        // index 2
        IntIndex idx2 = model.toIntIndex(2);
        Assertions.assertEquals(IntIndex.of(2), idx2, "toIntIndex(2) should return IntIndex.of(2)");
    }

    /**
     * Verify that an out‑of‑range index throws the expected exception.
     */
    @Test
    void testToIntIndexOutOfBounds() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(-1), "Negative index should throw IndexOutOfBoundsException");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(3), "Index equal to size should throw IndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "toIntIndex({0}) → IntIndex.of({1})")
    @MethodSource("validIndexProvider")
    void testValidIndices(int request, int expected) {
        IntIndex actual = model.toIntIndex(request);
        assertEquals(IntIndex.of(expected), actual, () -> "Expected IntIndex.of(" + expected + ") but got " + actual);
    }

    // -----------------------------------------------------------------
    @Test
    void testNegativeIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(-1), "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testIndexEqualToSizeThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(3), "Index equal to list size should throw IndexOutOfBoundsException");
    }

    @Test
    void testExtremelyLargePositiveIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(Integer.MAX_VALUE), "Very large index must throw IndexOutOfBoundsException");
    }

    // -----------------------------------------------------------------
    @Test
    void testEmptyModelThrows() {
        // no variables added
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> emptyModel.toIntIndex(0), "Calling toIntIndex on an empty model must throw IndexOutOfBoundsException");
    }

    // -----------------------------------------------------------------
    @Test
    void testValidIndexLastVariableInFiveVariableModel() {
        ExpressionsBasedModel fiveVarModel = new ExpressionsBasedModel();
        for (int i = 0; i < 5; i++) {
            fiveVarModel.addVariable();
        }
        IntIndex idx = fiveVarModel.toIntIndex(4);
        assertEquals(IntIndex.of(4), idx, "toIntIndex(4) on a five‑variable model should return IntIndex.of(4)");
    }

    // -----------------------------------------------------------------
    @Test
    void testValidIndexAfterSequentialAdditions() {
        ExpressionsBasedModel sequentialModel = new ExpressionsBasedModel();
        // first addition
        sequentialModel.addVariable();
        assertEquals(IntIndex.of(0), sequentialModel.toIntIndex(0), "After first addition, index 0 should be valid");
        // second addition
        sequentialModel.addVariable();
        assertEquals(IntIndex.of(1), sequentialModel.toIntIndex(1), "After second addition, index 1 should be valid");
    }

    // -----------------------------------------------------------------
    @Test
    void testNullVariableElementThrowsNPE() throws Exception {
        // Build a normal model with at least one variable
        ExpressionsBasedModel corruptedModel = new ExpressionsBasedModel();
        // ensures myVariables size >= 1
        corruptedModel.addVariable();
        // Use reflection to replace the first entry with null
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.ArrayList<Variable> list = (java.util.ArrayList<Variable>) field.get(corruptedModel);
        // corrupt the internal state
        list.set(0, null);
        // The call must now throw NullPointerException because getIndex() is invoked on null
        assertThrows(NullPointerException.class, () -> corruptedModel.toIntIndex(0), "When myVariables[0] is null, toIntIndex(0) must throw NullPointerException");
    }
}
