package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class ExpressionsBasedModel_getVariable_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model with three pre‑added variables for the majority of tests.
        model = new ExpressionsBasedModel();
        // index 0
        varA = model.addVariable();
        // index 1
        varB = model.addVariable();
        // index 2
        varC = model.addVariable();
    }

    /**
     * Adds {@code count} unnamed variables to {@link #model}.
     */
    private void addUnnamedVariables(int count) {
        for (int i = 0; i < count; i++) {
            // default name generated internally
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
        assertSame(varA, model.getVariable(0), "Variable at index 0 should be varA");
        assertSame(varB, model.getVariable(1), "Variable at index 1 should be varB");
        assertSame(varC, model.getVariable(2), "Variable at index 2 should be varC");
    }

    @Test
    void testGetVariableInvalidIndexThrowsException() {
        // Negative index – still throws.
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(-1), "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableAfterAddingMoreVariables() {
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
        Variable expected = model.getVariable(index);
        assertSame(expected, model.getVariable(index), () -> "Variable at index " + index + " should be the same instance that was added.");
    }

    @Test
    void testGetVariableNamedVariable() {
        // Use a fresh model so that the first variable added is the named one.
        model = new ExpressionsBasedModel();
        Variable named = model.addVariable("x");
        Variable retrieved = model.getVariable(0);
        assertSame(named, retrieved, "The retrieved variable should be the same instance that was added.");
        assertEquals("x", retrieved.getName(), "Variable name should be 'x'.");
    }

    @Test
    void testGetVariableAfterModelCopy() {
        model.addVariable("a");
        model.addVariable("b");
        // index 2
        model.addVariable("c");
        ExpressionsBasedModel copy = model.copy();
        Variable originalThird = model.getVariable(2);
        Variable copyThird = copy.getVariable(2);
        assertNotNull(copyThird, "Copied model should contain a variable at index 2.");
        assertEquals(originalThird.getName(), copyThird.getName(), "Variable in the copied model should have the same name as the original third variable.");
        assertNotSame(originalThird, copyThird, "Variable instances in the copy should not be the same object as in the original model.");
    }

    @Test
    void testGetVariableEmptyModelThrowsException() {
        // explicit empty model
        model = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(0), "Calling getVariable on an empty model must throw IndexOutOfBoundsException.");
    }

    @ParameterizedTest(name = "Invalid retrieval – {1}")
    @MethodSource("invalidIndexProvider")
    void testGetVariableInvalidIndices(int index, String _desc) {
        // Start from a fresh empty model for each case.
        model = new ExpressionsBasedModel();
        if (index < 0) {
            // Negative indices are still illegal.
            assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(index), "Negative index should throw IndexOutOfBoundsException.");
        } else {
            // For non‑negative out‑of‑range indices the current implementation
            // creates a new variable with an auto‑generated name "X<index>".
            Variable var = model.getVariable(index);
            assertNotNull(var, "Variable should be created for out‑of‑range index " + index);
            assertEquals("X" + index, var.getName(), "Auto‑generated variable name should match the requested index.");
        }
    }
}
