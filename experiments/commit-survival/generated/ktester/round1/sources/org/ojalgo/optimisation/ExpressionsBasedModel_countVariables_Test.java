package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressionsBasedModel_countVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to guarantee isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model (good practice, although not required for countVariables).
        model.dispose();
    }

    /**
     * The model should start with zero variables.
     */
    @Test
    void testInitialCountIsZero() {
        assertEquals(0, model.countVariables(), "A newly created model must contain no variables.");
    }

    /**
     * Adding a variable using {@code addVariable()} increments the count.
     */
    @Test
    void testCountAfterAddVariable() {
        // first variable
        model.addVariable();
        assertEquals(1, model.countVariables(), "Count should be 1 after adding a single variable.");
        // second variable
        model.addVariable();
        assertEquals(2, model.countVariables(), "Count should be 2 after adding a second variable.");
    }

    /**
     * Adding a named variable using {@code addVariable(String)} also increments the count.
     */
    @Test
    void testCountAfterAddVariableWithName() {
        model.addVariable("x");
        assertEquals(1, model.countVariables(), "Count should be 1 after adding a named variable.");
        model.addVariable("y");
        model.addVariable("z");
        assertEquals(3, model.countVariables(), "Count should reflect the total number of added variables.");
    }

    /**
     * Adding variables via the {@code newVariable(String)} factory method works as well.
     */
    @Test
    void testCountAfterNewVariable() {
        model.newVariable("a");
        assertEquals(1, model.countVariables(), "Count should be 1 after creating a variable with newVariable().");
        model.newVariable("b");
        model.newVariable("c");
        assertEquals(3, model.countVariables(), "Count should be 3 after creating three variables.");
    }

    /**
     * A copy of a model must have the same variable count as the original.
     */
    @Test
    void testCountAfterCopy() {
        // Build an original model with a few variables.
        model.addVariable("v1");
        model.addVariable("v2");
        model.addVariable("v3");
        assertEquals(3, model.countVariables());
        // Create a shallow copy (the copy constructor is package‑private, so we use the public copy method).
        // shallow copy, variables are cloned
        ExpressionsBasedModel copy = model.copy();
        assertEquals(3, copy.countVariables(), "A shallow copy should contain the same number of variables as the original.");
        // Ensure modifications to the copy do not affect the original count.
        copy.addVariable("v4");
        assertEquals(4, copy.countVariables(), "Copy should reflect its own added variable.");
        assertEquals(3, model.countVariables(), "Original model must remain unchanged after modifying the copy.");
    }

    /**
     * Adding no variables after creation and then disposing should still report zero.
     */
    @Test
    void testCountAfterDispose() {
        assertEquals(0, model.countVariables(), "Initially zero variables.");
        model.dispose();
        // After disposal the internal list is cleared; countVariables should still return zero.
        assertEquals(0, model.countVariables(), "After dispose the variable count should be zero.");
    }

    /**
     * Adding five variables in a loop should result in a count of 5.
     */
    @Test
    void testBasicCountAfterAddingFiveVariables() {
        IntStream.range(0, 5).forEach(i -> model.addVariable());
        assertEquals(5, model.countVariables(), "After adding five variables the count should be 5.");
    }

    /**
     * Parameterized test for large collections (e.g., 100 variables).
     */
    @ParameterizedTest(name = "Adding {0} variables should result in count {0}")
    @ValueSource(ints = { 100 })
    void testCountAfterAddingManyVariables(int numberOfVariables) {
        IntStream.range(0, numberOfVariables).forEach(i -> model.addVariable());
        assertEquals(numberOfVariables, model.countVariables(), "Variable count must match the number of added variables.");
    }

    /**
     * Adding expressions does not affect the variable count.
     */
    @Test
    void testCountUnchangedAfterAddingExpressions() {
        // Add two variables first.
        model.addVariable("a");
        model.addVariable("b");
        assertEquals(2, model.countVariables(), "Pre‑condition: two variables added.");
        // Add three expressions – they should not modify the variable list.
        model.addExpression();
        model.addExpression();
        model.addExpression();
        assertEquals(2, model.countVariables(), "Adding expressions must not change the variable count.");
    }

    /**
     * Relaxing and simplifying a model must keep the variable count unchanged.
     */
    @Test
    void testCountUnchangedAfterRelaxAndSimplify() {
        // Add four variables.
        IntStream.range(0, 4).forEach(i -> model.addVariable());
        assertEquals(4, model.countVariables(), "Pre‑condition: four variables added.");
        // Perform operations that should not affect variable collection.
        model.relax();
        model.simplify();
        assertEquals(4, model.countVariables(), "Variable count must stay the same after relax() and simplify().");
    }

    /**
     * Snapshot should preserve the variable count of the original model.
     */
    @Test
    void testCountAfterSnapshot() {
        // Add two variables.
        model.addVariable("x");
        model.addVariable("y");
        assertEquals(2, model.countVariables(), "Pre‑condition: two variables added.");
        // Create a snapshot.
        ExpressionsBasedModel snapshot = model.snapshot();
        assertEquals(2, snapshot.countVariables(), "Snapshot must contain the same number of variables as the original.");
    }
}
