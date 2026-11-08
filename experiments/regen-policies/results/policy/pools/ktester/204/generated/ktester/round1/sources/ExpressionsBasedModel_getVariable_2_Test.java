package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_getVariable_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly discard the model reference; helps the GC in long test suites.
        model = null;
    }

    /**
     * Provides valid indices after three variables have been added.
     */
    static Stream<Arguments> validIndexProvider() {
        return Stream.of(Arguments.of(0), Arguments.of(2));
    }

    /**
     * Supplies combinations of (modelSize, requestedIndex) that are out of bounds.
     * <ul>
     *   <li>size 0, index -1  → negative index</li>
     *   <li>size 3, index 5   → greater than size</li>
     *   <li>size 5, index 5   → equal to size (list size is the first illegal index)</li>
     * </ul>
     */
    static Stream<Arguments> outOfBoundsProvider() {
        return Stream.of(Arguments.of(0, -1), Arguments.of(3, 5), Arguments.of(5, 5));
    }

    @Test
    void testGetVariableReturnsAddedVariable() {
        // Given: a model with a single variable added.
        Variable var = model.addVariable();
        int idx = model.indexOf(var);
        IntIndex intIndex = new IntIndex(idx);
        // When: retrieving the variable via getVariable(IntIndex).
        Variable retrieved = model.getVariable(intIndex);
        // Then: the retrieved variable should be the same instance that was added.
        assertSame(var, retrieved, "The variable returned by getVariable should be the same instance that was added.");
    }

    @Test
    void testGetVariableWithMultipleVariables() {
        // Given: a model with three distinct variables.
        Variable v0 = model.addVariable();
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        // When/Then: each index should map to the correct variable.
        assertSame(v0, model.getVariable(new IntIndex(model.indexOf(v0))), "Index 0 should return the first variable.");
        assertSame(v1, model.getVariable(new IntIndex(model.indexOf(v1))), "Index 1 should return the second variable.");
        assertSame(v2, model.getVariable(new IntIndex(model.indexOf(v2))), "Index 2 should return the third variable.");
    }

    @Test
    void testGetVariableWithInvalidIndexThrowsException() {
        // Given: an empty model (no variables added).
        // When: requesting a variable at index 0 (which does not exist).
        IntIndex invalidIndex = new IntIndex(0);
        // Then: an IndexOutOfBoundsException should be thrown.
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(invalidIndex), "Requesting a variable with an out‑of‑range index should throw IndexOutOfBoundsException.");
    }

    @Test
    void testGetVariableNegativeIndexThrowsException() {
        // Given: a model with one variable.
        model.addVariable();
        // When: using a negative index.
        IntIndex negativeIndex = new IntIndex(-1);
        // Then: an IndexOutOfBoundsException should be thrown.
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(negativeIndex), "Negative indices should result in IndexOutOfBoundsException.");
    }

    @ParameterizedTest(name = "valid index {0}")
    @MethodSource("validIndexProvider")
    void testGetVariableValidIndices(int index) {
        // Given: three variables added to the model.
        List<Variable> added = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            added.add(model.addVariable());
        }
        // When: retrieving the variable at the supplied index.
        Variable retrieved = model.getVariable(new IntIndex(index));
        // Then: the retrieved instance must be exactly the one added at that position.
        assertSame(added.get(index), retrieved, () -> "Variable at index " + index + " should be the exact instance added.");
    }

    @ParameterizedTest(name = "modelSize={0}, index={1}")
    @MethodSource("outOfBoundsProvider")
    void testGetVariableOutOfBounds(int modelSize, int index) {
        // Given: a model containing {@code modelSize} variables.
        for (int i = 0; i < modelSize; i++) {
            model.addVariable();
        }
        // When/Then: accessing an illegal index must throw IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(new IntIndex(index)), () -> "Accessing index " + index + " on a model of size " + modelSize + " should throw IndexOutOfBoundsException.");
    }

    @Test
    void testGetVariableNullIntIndexThrowsNPE() {
        // Given: a model with at least one variable (to ensure the method reaches the null check before list access).
        model.addVariable();
        // When/Then: passing null must result in NullPointerException.
        assertThrows(NullPointerException.class, () -> model.getVariable(null), "Calling getVariable with a null IntIndex should throw NullPointerException.");
    }

    @Test
    void testGetVariableLargeIndex() {
        final int total = 1000;
        List<Variable> added = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            added.add(model.addVariable());
        }
        // Index 999 is the last valid position.
        Variable retrieved = model.getVariable(new IntIndex(999));
        assertSame(added.get(999), retrieved, "Variable at large index 999 should be the exact instance added at that position.");
    }

    @Test
    void testGetVariableReferenceEqualityOnRepeatedCalls() {
        // Given: three variables.
        for (int i = 0; i < 3; i++) {
            model.addVariable();
        }
        IntIndex idx = new IntIndex(2);
        // When: the method is called twice with the same IntIndex.
        Variable firstCall = model.getVariable(idx);
        Variable secondCall = model.getVariable(idx);
        // Then: both calls must return the identical object reference.
        assertSame(firstCall, secondCall, "Repeated calls with the same IntIndex should return the same Variable instance.");
    }

    @Test
    void testGetVariableIdentityAfterMultipleAccesses() {
        // Given: several variables (more than one to ensure ordering is not accidental).
        for (int i = 0; i < 5; i++) {
            model.addVariable();
        }
        IntIndex idx = new IntIndex(0);
        // first retrieval
        Variable expected = model.getVariable(idx);
        // When/Then: retrieve the same index several times and verify identity.
        for (int i = 0; i < 4; i++) {
            Variable again = model.getVariable(idx);
            assertSame(expected, again, "Each retrieval of index 0 should return the original Variable instance.");
        }
    }

    @Test
    void testGetVariableUsingToIntIndexHelper() {
        // Given: at least four variables.
        for (int i = 0; i < 4; i++) {
            model.addVariable();
        }
        // The helper creates an IntIndex that wraps the raw integer.
        IntIndex helperIdx = model.toIntIndex(3);
        Variable expected = model.getVariable(new IntIndex(3));
        // When: retrieving via the helper‑produced IntIndex.
        Variable retrieved = model.getVariable(helperIdx);
        // Then: the same variable must be returned.
        assertSame(expected, retrieved, "Variable retrieved via toIntIndex(3) should match the variable at position 3.");
    }
}
