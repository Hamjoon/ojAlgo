package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariable_Test {

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
        // (no re‑creation of the model here – we keep the added variables)
    }

    /**
     * Adds {@code count} unnamed variables to {@link #model}.
     */
    private void addUnnamedVariables(int count) {
        for (int i = 0; i < count; i++) {
            // default name is generated internally
            model.addVariable();
        }
    }

    /**
     * Provides valid index‑retrieval scenarios.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the number of variables to add before the retrieval</li>
     *   <li>the index to query</li>
     *   <li>a description used only for readability in the test name</li>
     * </ul>
     */
    private static Stream<Arguments> validRetrievalProvider() {
        return Stream.of(Arguments.of(1, 0, "single variable, index 0"), Arguments.of(3, 2, "third variable of three, index 2"), Arguments.of(3, 1, "middle variable of three, index 1"), Arguments.of(10, 9, "tenth variable of ten, index 9"));
    }

    /**
     * Provides illegal index scenarios together with a short description.
     */
    private static Stream<Arguments> invalidIndexProvider() {
        return Stream.of(Arguments.of(-1, "negative index"), Arguments.of(1, "index equal to size (single variable)"), Arguments.of(5, "index far beyond size (two variables)"), Arguments.of(Integer.MAX_VALUE, "Integer.MAX_VALUE"), Arguments.of(15, "just beyond size after many additions"));
    }

    @Test
    void testGetVariableReturnsAddedVariable() {
        // Verify that each index returns the exact Variable instance that was added.
        assertSame(varA, model.getVariable(0), "Variable at index 0 should be varA");
        assertSame(varB, model.getVariable(1), "Variable at index 1 should be varB");
        assertSame(varC, model.getVariable(2), "Variable at index 2 should be varC");
    }

    @Test
    void testGetVariableInvalidIndexThrowsException() {
        // Negative index
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(-1), "Negative index should throw IndexOutOfBoundsException");
        // Index equal to size (out of range)
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(3), "Index equal to size should throw IndexOutOfBoundsException");
        // Index far beyond the list size
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(100), "Large out‑of‑range index should throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableAfterAddingMoreVariables() {
        // Add additional variables and verify ordering is preserved.
        // index 3
        Variable varD = model.addVariable();
        // index 4
        Variable varE = model.addVariable();
        assertSame(varD, model.getVariable(3), "Variable at index 3 should be varD");
        assertSame(varE, model.getVariable(4), "Variable at index 4 should be varE");
    }

    @ParameterizedTest(name = "Valid retrieval – {2}")
    @MethodSource("validRetrievalProvider")
    void testGetVariableValidIndices(int variableCount, int index, String _desc) {
        addUnnamedVariables(variableCount);
        // retrieve once to keep reference
        Variable expected = model.getVariable(index);
        // The method should return the exact same instance that was stored.
        assertSame(expected, model.getVariable(index), () -> "Variable at index " + index + " should be the same instance that was added.");
    }

    @Test
    void testGetVariableNamedVariable() {
        // index 0
        Variable named = model.addVariable("x");
        Variable retrieved = model.getVariable(0);
        assertSame(named, retrieved, "The retrieved variable should be the same instance that was added.");
        // Assuming Variable exposes its name via getName()
        assertEquals("x", retrieved.getName(), "Variable name should be 'x'.");
    }

    @Test
    void testGetVariableAfterModelCopy() {
        // create original model with three distinct variables
        model.addVariable("a");
        model.addVariable("b");
        // index 2
        model.addVariable("c");
        // copy the model (shallow copy – variables are duplicated with same names)
        ExpressionsBasedModel copy = model.copy();
        // The copy must contain a variable at index 2 with the same name as the original third variable.
        Variable originalThird = model.getVariable(2);
        Variable copyThird = copy.getVariable(2);
        assertNotNull(copyThird, "Copied model should contain a variable at index 2.");
        assertEquals(originalThird.getName(), copyThird.getName(), "Variable in the copied model should have the same name as the original third variable.");
        // The instances are different objects (copy creates new Variable instances)
        assertNotSame(originalThird, copyThird, "Variable instances in the copy should not be the same object as in the original model.");
    }

    @Test
    void testGetVariableEmptyModelThrowsException() {
        // Create a fresh empty model (the @BeforeEach already does this, but we reset to be explicit)
        model = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(0), "Calling getVariable on an empty model must throw IndexOutOfBoundsException.");
    }

    @ParameterizedTest(name = "Invalid retrieval – {1}")
    @MethodSource("invalidIndexProvider")
    void testGetVariableInvalidIndices(int index, String _desc) {
        // Prepare the model so that the index is indeed out‑of‑bounds.
        // For the cases that need a specific size we add a suitable number of variables.
        // The largest size we need is 10 (for the 15‑index test we add 10 variables).
        int requiredSize = Math.max(0, Math.min(index, 10));
        addUnnamedVariables(requiredSize);
        // The call must throw IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(index), () -> "Index " + index + " (" + _desc + ") should throw IndexOutOfBoundsException.");
    }
}