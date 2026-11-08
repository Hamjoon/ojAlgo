package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toIntRowColumn_Test {

    private ExpressionsBasedModel model;

    private Variable var0;

    private Variable var1;

    private Variable var2;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add three variables.
        model = new ExpressionsBasedModel();
        // addVariable() creates a variable with the next free index (0,1,2,…)
        // index 0
        var0 = model.addVariable();
        // index 1
        var1 = model.addVariable();
        // index 2
        var2 = model.addVariable();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /* -------------------------------------------------
     *  Parameterised tests for the “Valid indices” group
     * ------------------------------------------------- */
    private static Stream<Arguments> validIndicesProvider() {
        return Stream.of(// row, column, expected row index, expected column index
        Arguments.of(0, 1, 0, 1, "Both row and column refer to existing variables; should return IntRowColumn with corresponding indices."), Arguments.of(2, 2, 2, 2, "Row and column refer to the same variable; method should handle identical indices correctly."), Arguments.of(1, 0, 1, 0, "Row index greater than column index; order should be preserved in the returned IntRowColumn."), // boundary cases
        Arguments.of(0, 2, 0, 2, "First and last variable indices; should return correct IntRowColumn preserving the boundary order."), Arguments.of(1, 1, 1, 1, "Both row and column refer to the same middle variable."));
    }

    /* -------------------------------------------------
     *  Parameterised tests for the “Invalid indices” group
     * ------------------------------------------------- */
    private static Stream<Arguments> invalidIndicesProvider() {
        return Stream.of(// negative row
        Arguments.of(-1, 0, "Negative row index should cause IndexOutOfBoundsException."), // negative column
        Arguments.of(0, -5, "Negative column index should cause IndexOutOfBoundsException."), // row equal to size (3) – out of bounds
        Arguments.of(3, 0, "Row index equal to size of variable list (out of bounds) should throw IndexOutOfBoundsException."), // column greater than last index
        Arguments.of(0, 3, "Column index greater than last index should throw IndexOutOfBoundsException."), // far beyond size
        Arguments.of(5, 5, "Both indices far beyond the size of the variable list should throw IndexOutOfBoundsException."), // row valid, column invalid
        Arguments.of(2, 5, "Row index valid but column index exceeds the list size; should throw IndexOutOfBoundsException."));
    }

    @Test
    void testToIntRowColumnReturnsCorrectIndices() {
        // When we ask for the row/column pair (0,2) we expect the indices of var0 and var2.
        IntRowColumn expected = new IntRowColumn(var0.getIndex(), var2.getIndex());
        IntRowColumn actual = model.toIntRowColumn(0, 2);
        assertEquals(expected, actual, () -> "Expected IntRowColumn to contain the indices of var0 and var2");
    }

    @Test
    void testToIntRowColumnWithSameRowAndColumn() {
        // Using the same row and column should give a pair where both parts are the same index.
        IntRowColumn expected = new IntRowColumn(var1.getIndex(), var1.getIndex());
        IntRowColumn actual = model.toIntRowColumn(1, 1);
        assertEquals(expected, actual, () -> "Expected IntRowColumn to contain the same index for row and column when both arguments are equal");
    }

    @Test
    void testToIntRowColumnInvalidRowThrowsException() {
        // Row index out of range (negative)
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(-1, 0));
        // Row index out of range (greater than size)
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(3, 0));
    }

    @Test
    void testToIntRowColumnInvalidColumnThrowsException() {
        // Column index out of range (negative)
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(0, -1));
        // Column index out of range (greater than size)
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(0, 3));
    }

    @ParameterizedTest(name = "[{index}] row={0}, column={1} → expected ({2},{3})")
    @MethodSource("validIndicesProvider")
    void testValidIndices(int row, int column, int expectedRowIdx, int expectedColIdx, String description) {
        IntRowColumn expected = new IntRowColumn(expectedRowIdx, expectedColIdx);
        IntRowColumn actual = model.toIntRowColumn(row, column);
        assertEquals(expected, actual, () -> "Failed: " + description);
    }

    @ParameterizedTest(name = "[{index}] row={0}, column={1} → throws IndexOutOfBoundsException")
    @MethodSource("invalidIndicesProvider")
    void testInvalidIndices(int row, int column, String description) {
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(row, column), () -> "Failed: " + description);
    }

    /* -------------------------------------------------
     *  Empty model scenario
     * ------------------------------------------------- */
    @Test
    void testEmptyModelThrowsException() {
        // no variables added
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> emptyModel.toIntRowColumn(0, 0), "Any access on an empty model should result in IndexOutOfBoundsException.");
    }

    /* -------------------------------------------------
     *  No side‑effects verification
     * ------------------------------------------------- */
    @Test
    void testNoSideEffectsAfterCall() {
        // should be 3
        int initialSize = model.getVariables().size();
        model.toIntRowColumn(0, 1);
        int afterSize = model.getVariables().size();
        assertEquals(initialSize, afterSize, "Calling toIntRowColumn must not modify the internal variable list.");
    }

    /* -------------------------------------------------
     *  Null variable entry in the list
     * ------------------------------------------------- */
    @Test
    void testNullVariableInListThrowsNPE() throws Exception {
        // Replace the first entry with null via reflection
        Field listField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        listField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) listField.get(model);
        // now myVariables[0] == null
        vars.set(0, null);
        assertThrows(NullPointerException.class, () -> model.toIntRowColumn(0, 0), "Accessing a null variable entry should throw NullPointerException.");
    }

    /* -------------------------------------------------
     *  Null internal variable list
     * ------------------------------------------------- */
    @Test
    void testNullInternalVariableListThrowsNPE() throws Exception {
        // Set the whole myVariables field to null via reflection
        Field listField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        listField.setAccessible(true);
        listField.set(model, null);
        assertThrows(NullPointerException.class, () -> model.toIntRowColumn(0, 0), "When the internal variable list is null, calling toIntRowColumn should throw NullPointerException.");
    }
}
