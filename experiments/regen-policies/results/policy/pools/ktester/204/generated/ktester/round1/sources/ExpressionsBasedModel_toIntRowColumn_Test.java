package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toIntRowColumn_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple holder for parameterised test data.
     */
    private static class ArgumentsHolder {

        final int row;

        final int column;

        final IntRowColumn expected;

        final Class<? extends Throwable> exception;

        ArgumentsHolder(int row, int column, IntRowColumn expected) {
            this.row = row;
            this.column = column;
            this.expected = expected;
            this.exception = null;
        }

        ArgumentsHolder(int row, int column, Class<? extends Throwable> exception) {
            this.row = row;
            this.column = column;
            this.expected = null;
            this.exception = exception;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add a few variables.
        model = new ExpressionsBasedModel();
        // index 0
        model.addVariable();
        // index 1
        model.addVariable();
        // index 2 – extra variable for out‑of‑bounds checks
        model.addVariable();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources (if any).
        model.dispose();
    }

    private static Stream<ArgumentsHolder> normalOperationProvider() {
        return Stream.of(new ArgumentsHolder(0, 1, new IntRowColumn(0, 1)), new ArgumentsHolder(2, 2, new IntRowColumn(2, 2)));
    }

    private static Stream<ArgumentsHolder> invalidRowProvider() {
        return Stream.of(new ArgumentsHolder(-1, 0, IndexOutOfBoundsException.class), // size = 3
        new ArgumentsHolder(5, 0, IndexOutOfBoundsException.class));
    }

    private static Stream<ArgumentsHolder> invalidColumnProvider() {
        return Stream.of(new ArgumentsHolder(0, -2, IndexOutOfBoundsException.class), // size = 3
        new ArgumentsHolder(0, 4, IndexOutOfBoundsException.class));
    }

    private static Stream<ArgumentsHolder> boundaryProvider() {
        return Stream.of(new ArgumentsHolder(0, 0, new IntRowColumn(0, 0)), new ArgumentsHolder(4, 4, new IntRowColumn(4, 4)), new ArgumentsHolder(4, 0, new IntRowColumn(4, 0)), new ArgumentsHolder(0, 4, new IntRowColumn(0, 4)));
    }

    private static Stream<ArgumentsHolder> mixedProvider() {
        return Stream.of(new ArgumentsHolder(3, -1, IndexOutOfBoundsException.class), new ArgumentsHolder(-2, 2, IndexOutOfBoundsException.class));
    }

    /**
     * Replaces the element at the given position in the private {@code myVariables}
     * list with the supplied {@code Variable} (may be {@code null}).
     */
    private void setVariableAt(int index, Variable replacement) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) f.get(model);
        // Ensure the list is large enough.
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, replacement);
    }

    @Test
    void testToIntRowColumnReturnsCorrectMapping() {
        // Arrange – obtain the variables that were added in the @BeforeEach method.
        Variable varRow = model.getVariable(0);
        Variable varCol = model.getVariable(1);
        // Act – invoke the package‑private method.
        IntRowColumn result = model.toIntRowColumn(0, 1);
        // Assert – the returned IntRowColumn must contain the indices of the two variables.
        IntRowColumn expected = new IntRowColumn(varRow.getIndex(), varCol.getIndex());
        assertEquals(expected, result, () -> "Expected IntRowColumn " + expected + " but got " + result);
    }

    @Test
    void testToIntRowColumnWithSameRowAndColumn() {
        Variable var = model.getVariable(2);
        IntRowColumn result = model.toIntRowColumn(2, 2);
        IntRowColumn expected = new IntRowColumn(var.getIndex(), var.getIndex());
        assertEquals(expected, result);
    }

    @Test
    void testToIntRowColumnThrowsWhenRowIsOutOfBounds() {
        // Row index 5 does not exist (only indices 0‑2 are present).
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(5, 0), "Expected IndexOutOfBoundsException for an invalid row index");
    }

    @Test
    void testToIntRowColumnThrowsWhenColumnIsOutOfBounds() {
        // Column index -1 is invalid.
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(0, -1), "Expected IndexOutOfBoundsException for an invalid column index");
    }

    @ParameterizedTest
    @MethodSource("normalOperationProvider")
    void testToIntRowColumnNormalOperation(ArgumentsHolder holder) {
        IntRowColumn result = model.toIntRowColumn(holder.row, holder.column);
        assertEquals(holder.expected, result, () -> "Expected " + holder.expected + " but got " + result);
    }

    @ParameterizedTest
    @MethodSource("invalidRowProvider")
    void testToIntRowColumnInvalidRow(ArgumentsHolder holder) {
        assertThrows(holder.exception, () -> model.toIntRowColumn(holder.row, holder.column));
    }

    @ParameterizedTest
    @MethodSource("invalidColumnProvider")
    void testToIntRowColumnInvalidColumn(ArgumentsHolder holder) {
        assertThrows(holder.exception, () -> model.toIntRowColumn(holder.row, holder.column));
    }

    @Test
    void testToIntRowColumnBothIndicesInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.toIntRowColumn(-3, 10), "Both indices are out of bounds – should fail on the row access.");
    }

    @Test
    void testToIntRowColumnEmptyModel() {
        // no variables added
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.toIntRowColumn(0, 0), "Any access on an empty model must throw IndexOutOfBoundsException.");
        empty.dispose();
    }

    @ParameterizedTest
    @MethodSource("boundaryProvider")
    void testToIntRowColumnBoundaryIndices(ArgumentsHolder holder) {
        // Build a model with exactly 5 variables.
        ExpressionsBasedModel five = new ExpressionsBasedModel();
        for (int i = 0; i < 5; i++) {
            five.addVariable();
        }
        IntRowColumn result = five.toIntRowColumn(holder.row, holder.column);
        assertEquals(holder.expected, result);
        five.dispose();
    }

    @Test
    void testToIntRowColumnSingleVariable() {
        ExpressionsBasedModel single = new ExpressionsBasedModel();
        // index 0
        single.addVariable();
        IntRowColumn result = single.toIntRowColumn(0, 0);
        assertEquals(new IntRowColumn(0, 0), result);
        single.dispose();
    }

    @Test
    void testToIntRowColumnLargeValidIndices() {
        ExpressionsBasedModel large = new ExpressionsBasedModel();
        for (int i = 0; i < 100; i++) {
            large.addVariable();
        }
        IntRowColumn result = large.toIntRowColumn(99, 98);
        assertEquals(new IntRowColumn(99, 98), result);
        large.dispose();
    }

    @Test
    void testToIntRowColumnLargeInvalidRow() {
        ExpressionsBasedModel large = new ExpressionsBasedModel();
        for (int i = 0; i < 100; i++) {
            large.addVariable();
        }
        assertThrows(IndexOutOfBoundsException.class, () -> large.toIntRowColumn(100, 99), "Row index equal to size must be out of bounds.");
        large.dispose();
    }

    @ParameterizedTest
    @MethodSource("mixedProvider")
    void testToIntRowColumnMixedValidInvalid(ArgumentsHolder holder) {
        // Model with at least 4 variables (indices 0‑3)
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        for (int i = 0; i < 4; i++) {
            m.addVariable();
        }
        assertThrows(holder.exception, () -> m.toIntRowColumn(holder.row, holder.column));
        m.dispose();
    }

    @Test
    void testToIntRowColumnNullVariableInListRow() throws Exception {
        // Replace variable at index 1 with null.
        setVariableAt(1, null);
        assertThrows(NullPointerException.class, () -> model.toIntRowColumn(1, 0), "Accessing a null variable should raise NPE.");
    }

    @Test
    void testToIntRowColumnNullVariableInListColumn() throws Exception {
        setVariableAt(2, null);
        assertThrows(NullPointerException.class, () -> model.toIntRowColumn(0, 2), "Accessing a null variable should raise NPE.");
    }

    @Test
    void testToIntRowColumnVariableWithNullIndex() throws Exception {
        // Create a mock Variable whose getIndex() returns null.
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenReturn(null);
        // Insert the mock at position 0.
        setVariableAt(0, mockVar);
        assertThrows(NullPointerException.class, () -> model.toIntRowColumn(0, 1), "Variable returning null index should cause NPE when constructing IntRowColumn.");
    }

    @Test
    void testToIntRowColumnInternalListNull() throws Exception {
        // Set the private final field myVariables to null via reflection.
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, null);
        assertThrows(NullPointerException.class, () -> model.toIntRowColumn(0, 0), "When the internal variable list is null, a NPE should be thrown.");
    }
}
