package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toIntIndex_Test {

    private ExpressionsBasedModel model;

    private final List<Variable> addedVariables = new ArrayList<>();

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add a few variables.
        model = new ExpressionsBasedModel();
        // Adding variables with distinct names makes debugging easier.
        addedVariables.add(model.newVariable("v0"));
        addedVariables.add(model.newVariable("v1"));
        addedVariables.add(model.newVariable("v2"));
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
        addedVariables.clear();
    }

    private static Stream<Arguments> normalBehaviourProvider() {
        // (modelSize, index) – the model will contain exactly modelSize variables.
        return Stream.of(// single variable, index 0
        Arguments.of(1, 0), // three variables, index 2
        Arguments.of(3, 2));
    }

    @Test
    void testToIntIndexReturnsCorrectIndex() {
        // Verify that the IntIndex returned by toIntIndex matches the variable's own index.
        for (int i = 0; i < addedVariables.size(); i++) {
            Variable var = addedVariables.get(i);
            // the index stored inside the variable
            IntIndex expected = var.getIndex();
            // method under test
            IntIndex actual = model.toIntIndex(i);
            assertEquals(expected, actual, () -> "IntIndex mismatch at position " + i);
        }
    }

    @Test
    void testToIntIndexThrowsIndexOutOfBoundsForNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(-1), "Negative index should result in IndexOutOfBoundsException");
    }

    @Test
    void testToIntIndexThrowsIndexOutOfBoundsForTooLargeIndex() {
        // valid indices are 0 .. size-1
        int tooLarge = addedVariables.size();
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(tooLarge));
    }

    @ParameterizedTest(name = "[{index}] modelSize={0}, index={1}")
    @MethodSource("normalBehaviourProvider")
    void testToIntIndexNormalBehaviour(int modelSize, int index) {
        // Build a model with the required number of variables.
        ExpressionsBasedModel localModel = new ExpressionsBasedModel();
        for (int i = 0; i < modelSize; i++) {
            localModel.newVariable("v" + i);
        }
        // Expected IntIndex is the one stored inside the variable at the same position.
        Variable expectedVar = localModel.getVariable(index);
        IntIndex expected = expectedVar.getIndex();
        IntIndex actual = localModel.toIntIndex(index);
        assertEquals(expected, actual, () -> "Unexpected IntIndex for index " + index + " in a model of size " + modelSize);
    }

    @Test
    void testToIntIndexThrowsIndexOutOfBoundsWhenIndexEqualsSize() {
        // Model created by @BeforeEach has three variables (size = 3).
        // 3
        int outOfRange = addedVariables.size();
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(outOfRange), "Index equal to size must throw IndexOutOfBoundsException");
    }

    @Test
    void testToIntIndexThrowsWhenModelIsEmpty() {
        // no variables added
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> emptyModel.toIntIndex(0), "Calling toIntIndex on an empty model must throw IndexOutOfBoundsException");
    }

    @Test
    void testToIntIndexReturnsCorrectIndexForLastVariable() {
        // Build a model with five variables.
        ExpressionsBasedModel fiveVarModel = new ExpressionsBasedModel();
        for (int i = 0; i < 5; i++) {
            fiveVarModel.newVariable("v" + i);
        }
        int lastIdx = 4;
        Variable expectedVar = fiveVarModel.getVariable(lastIdx);
        IntIndex expected = expectedVar.getIndex();
        IntIndex actual = fiveVarModel.toIntIndex(lastIdx);
        assertEquals(expected, actual, () -> "IntIndex of the last variable (index 4) is incorrect");
    }

    @Test
    void testToIntIndexIdempotentSequentialCalls() {
        // Model from @BeforeEach already has three variables.
        int idx = 1;
        Variable var = model.getVariable(idx);
        IntIndex expected = var.getIndex();
        // First call
        IntIndex first = model.toIntIndex(idx);
        // Second call
        IntIndex second = model.toIntIndex(idx);
        assertAll(() -> assertEquals(expected, first, "First call returned wrong IntIndex"), () -> assertEquals(expected, second, "Second call returned wrong IntIndex"), () -> assertEquals(first, second, "Sequential calls returned different IntIndex instances"));
    }

    @Test
    void testToIntIndexThrowsNullPointerExceptionWhenVariableIsNull() throws Exception {
        // Replace the first entry of the private myVariables list with null via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> internalList = (List<Variable>) field.get(model);
        // corrupt the list deliberately
        internalList.set(0, null);
        assertThrows(NullPointerException.class, () -> model.toIntIndex(0), "Accessing a null variable should throw NullPointerException");
    }

    @Test
    void testToIntIndexThrowsWhenIndexFarBeyondSize() {
        // Model from @BeforeEach has three variables; we request index 100.
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntIndex(100), "A far‑out‑of‑range index must throw IndexOutOfBoundsException");
    }
}
