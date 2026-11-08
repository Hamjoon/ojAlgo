package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.lang.reflect.Constructor;
import org.ojalgo.structure.Structure1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOf_Test {

    private ExpressionsBasedModel model;

    /**
     * Custom subclass of {@link Structure1D.IntIndex} that simply forwards the
     * constructor to the superclass. This demonstrates that {@code indexOf}
     * works with subclasses of {@code IntIndex}.
     */
    static class CustomIntIndex extends Structure1D.IntIndex {

        CustomIntIndex(int index) {
            super(index);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test guarantees isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model – good practice for classes that hold
        // native resources or large data structures.
        model.dispose();
    }

    /**
     * Helper that creates a {@link Structure1D.IntIndex} instance with the
     * supplied {@code rawIndex}.  The constructor of {@code IntIndex} is
     * package‑private, therefore reflection is used.
     */
    private static Structure1D.IntIndex createIntIndex(int rawIndex) {
        try {
            Constructor<Structure1D.IntIndex> ctor = Structure1D.IntIndex.class.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            return ctor.newInstance(rawIndex);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate IntIndex via reflection", e);
        }
    }

    /**
     * Helper that creates a mock {@link Variable} returning the given
     * {@link Structure1D.IntIndex} from {@code getIndex()}.
     */
    private static Variable mockVariableWithIndex(Structure1D.IntIndex idx) {
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenReturn(idx);
        return mockVar;
    }

    /**
     * Helper that creates a {@link Variable} instance with the supplied raw
     * index. The constructor of {@code Variable} is package‑private, therefore
     * reflection is used.
     */
    private static Variable createVariableWithRawIndex(ExpressionsBasedModel mdl, int rawIndex) {
        try {
            Constructor<Variable> ctor = Variable.class.getDeclaredConstructor(ExpressionsBasedModel.class, int.class);
            ctor.setAccessible(true);
            return ctor.newInstance(mdl, rawIndex);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate Variable via reflection", e);
        }
    }

    @Test
    void testIndexOfSingleVariable() {
        // first variable → index 0
        Variable v = model.addVariable();
        int idx = model.indexOf(v);
        assertEquals(0, idx, "The first variable should have index 0");
        // Verify that the index matches the one exposed by the variable itself.
        assertEquals(v.getIndex().index, idx, "Returned index must equal Variable's internal index");
    }

    @Test
    void testIndexOfMultipleVariablesSequential() {
        // index 0
        Variable v0 = model.addVariable();
        // index 1
        Variable v1 = model.addVariable();
        // index 2
        Variable v2 = model.addVariable();
        assertAll(() -> assertEquals(0, model.indexOf(v0), "v0 should be index 0"), () -> assertEquals(1, model.indexOf(v1), "v1 should be index 1"), () -> assertEquals(2, model.indexOf(v2), "v2 should be index 2"));
    }

    @Test
    void testIndexOfVariableCreatedWithName() {
        Variable named = model.newVariable("myVar");
        int idx = model.indexOf(named);
        assertEquals(named.getIndex().index, idx, "Index returned by model.indexOf must equal the variable's own index");
    }

    @Test
    void testIndexOfVariableFromAnotherModelIsIndependent() {
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        // index 0 in its own model
        Variable otherVar = otherModel.addVariable();
        int idxFromFirstModel = model.indexOf(otherVar);
        assertEquals(0, idxFromFirstModel, "A variable's internal index is independent of the model used to query it");
        otherModel.dispose();
    }

    @Test
    void testIndexOfVariableAfterModelCopy() {
        // index 0
        Variable original = model.addVariable();
        ExpressionsBasedModel copy = model.copy();
        Variable copiedVar = copy.getVariable(0);
        assertNotSame(original, copiedVar, "Copy should contain a distinct Variable instance");
        assertEquals(original.getIndex().index, copiedVar.getIndex().index, "Indices of original and copied variables must match");
        assertEquals(copiedVar.getIndex().index, copy.indexOf(copiedVar), "indexOf on the copy must return the copied variable's index");
        copy.dispose();
    }

    /**
     * Verifies that {@code indexOf} returns the exact integer stored in the
     * variable's {@link Structure1D.IntIndex}.  The variables are created via
     * the public API so that they have predictable sequential indices.
     */
    @ParameterizedTest(name = "Variable at position {0} should have index {0}")
    @CsvSource({ "0", "5" })
    void testIndexOfSequentialIndices(int position) {
        Variable target = null;
        for (int i = 0; i <= position; i++) {
            target = model.addVariable();
        }
        assertNotNull(target, "Target variable should have been created");
        assertEquals(position, model.indexOf(target), "indexOf must return the sequential index assigned by the model");
    }

    @Test
    void testIndexOfVariableWithNegativeIndex() {
        Variable var = createVariableWithRawIndex(model, -1);
        assertEquals(-1, model.indexOf(var), "indexOf should return the negative index stored in the variable");
    }

    @Test
    void testIndexOfVariableWithIntegerMaxValue() {
        Variable var = createVariableWithRawIndex(model, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, model.indexOf(var), "indexOf must correctly propagate Integer.MAX_VALUE");
    }

    @Test
    void testIndexOfVariableWithIntegerMinValue() {
        Variable var = createVariableWithRawIndex(model, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, model.indexOf(var), "indexOf must correctly propagate Integer.MIN_VALUE");
    }

    @Test
    void testIndexOfVariableWithLargePositiveIndex() {
        int large = 123_456_789;
        Variable var = createVariableWithRawIndex(model, large);
        assertEquals(large, model.indexOf(var), "indexOf must return large positive indices unchanged");
    }

    @Test
    void testIndexOfVariableWithNull() {
        assertThrows(NullPointerException.class, () -> model.indexOf(null), "Calling indexOf with a null variable should throw NPE");
    }

    @Test
    void testIndexOfVariableWithNullIntIndex() {
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> model.indexOf(mockVar), "If Variable#getIndex() returns null, indexOf should throw NPE");
    }

    @Test
    void testIndexOfVariableWithCustomIntIndexSubclass() {
        Structure1D.IntIndex customIdx = new CustomIntIndex(42);
        Variable var = mockVariableWithIndex(customIdx);
        assertEquals(42, model.indexOf(var), "indexOf must correctly read the index from a custom IntIndex subclass");
    }
}
