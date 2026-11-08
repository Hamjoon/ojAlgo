package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class ExpressionsBasedModel_countVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to guarantee isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to release any internal resources.
        model.dispose();
        model = null;
    }

    static Stream<Arguments> namedVariableProvider() {
        return Stream.of(// duplicate names are allowed
        Arguments.of(new String[] { "x", "y", "z" }, 3), Arguments.of(new String[] { "dup", "dup" }, 2));
    }

    @Test
    void testInitialCountIsZero() {
        assertEquals(0, model.countVariables(), "A newly created ExpressionsBasedModel should contain zero variables.");
    }

    @Test
    void testCountAfterAddingVariables() {
        // Add three variables using the public API.
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable("namedVar");
        Variable v3 = model.newVariable("newVar");
        // Verify that the internal list size matches the number of added variables.
        assertEquals(3, model.countVariables(), "countVariables() should reflect the number of variables added to the model.");
        // Additional sanity checks: the variables are retrievable and have distinct indices.
        assertEquals(0, model.indexOf(v1));
        assertEquals(1, model.indexOf(v2));
        assertEquals(2, model.indexOf(v3));
    }

    @Test
    void testCountAfterCopyingModel() {
        // Populate the original model.
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.addVariable().lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        model.addVariable().level(BigDecimal.valueOf(3));
        int originalCount = model.countVariables();
        assertEquals(3, originalCount, "Original model should have three variables.");
        // Deep copy (shallow = false, prune = false) – the public copy() method creates a deep copy.
        ExpressionsBasedModel deepCopy = model.copy();
        // Shallow copy via copy(true, false)
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        // Both copies must report the same number of variables as the original.
        assertEquals(originalCount, deepCopy.countVariables(), "Deep copy must preserve the variable count.");
        assertEquals(originalCount, shallowCopy.countVariables(), "Shallow copy must preserve the variable count.");
        // Clean up copies.
        deepCopy.dispose();
        shallowCopy.dispose();
    }

    @Test
    void testCountAfterRemovingVariablesIsUnchanged() {
        // The API does not provide a direct removeVariable method, but we can test that
        // countVariables() is unaffected by operations that do not modify the variable list.
        model.addVariable();
        model.addVariable();
        assertEquals(2, model.countVariables());
        // Adding an expression should not change the variable count.
        model.addExpression().set(model.getVariable(0), BigDecimal.ONE);
        assertEquals(2, model.countVariables(), "Adding expressions must not affect the variable count.");
    }

    @Test
    void testCountAfterAddingExpressionsDoesNotChangeVariableCount() {
        // Add three variables.
        model.addVariable();
        model.addVariable();
        model.addVariable();
        // Add two expressions – they must not affect the variable count.
        model.addExpression();
        model.addExpression();
        assertEquals(3, model.countVariables(), "Adding expressions must not modify the variable count.");
    }

    @Test
    void testCountAfterRelaxDoesNotChangeVariableCount() {
        // Populate model.
        for (int i = 0; i < 5; i++) {
            model.addVariable();
        }
        int before = model.countVariables();
        // Relax the model (may change variable types but not the collection size).
        model.relax();
        assertEquals(before, model.countVariables(), "Calling relax() must not alter the number of variables.");
    }

    @Test
    void testCountAfterSetInfeasibleDoesNotChangeVariableCount() {
        // Add two variables.
        model.addVariable();
        model.addVariable();
        int before = model.countVariables();
        // Mark model infeasible.
        model.setInfeasible();
        assertEquals(before, model.countVariables(), "setInfeasible() must not modify the variable count.");
    }

    @ParameterizedTest(name = "addVariable() called {0} time(s) → count = {0}")
    @ValueSource(ints = { 1, 5 })
    void testVariableAdditionCount(int addCalls) {
        for (int i = 0; i < addCalls; i++) {
            model.addVariable();
        }
        assertEquals(addCalls, model.countVariables(), "countVariables() should reflect the number of addVariable() calls.");
    }

    @ParameterizedTest(name = "addVariable(String) with names {0} → count = {1}")
    @MethodSource("namedVariableProvider")
    void testNamedVariableAdditionCount(String[] names, int expected) {
        for (String n : names) {
            model.addVariable(n);
        }
        assertEquals(expected, model.countVariables(), "Named variables must be counted individually, even if names duplicate.");
    }

    @ParameterizedTest(name = "newVariable(String) called {0} time(s) → count = {0}")
    @ValueSource(ints = { 4 })
    void testNewVariableCount(int calls) {
        for (int i = 0; i < calls; i++) {
            model.newVariable("v" + i);
        }
        assertEquals(calls, model.countVariables(), "newVariable(String) should increase the variable count.");
    }

    @Test
    void testShallowCopyRetainsVariableCount() {
        // Populate original model with four variables.
        for (int i = 0; i < 4; i++) {
            model.addVariable();
        }
        int original = model.countVariables();
        assertEquals(4, original);
        // Shallow copy via copy(true, false)
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        try {
            assertEquals(original, shallowCopy.countVariables(), "Shallow copy must preserve the variable count.");
        } finally {
            shallowCopy.dispose();
        }
    }

    @Test
    void testRelaxedCopyRetainsVariableCount() {
        // Populate original model with two variables.
        for (int i = 0; i < 2; i++) {
            model.addVariable();
        }
        int original = model.countVariables();
        // Relaxed copy via copy(true) – this creates a copy that is relaxed.
        ExpressionsBasedModel relaxedCopy = model.copy(true);
        try {
            assertEquals(original, relaxedCopy.countVariables(), "Relaxed copy must preserve the variable count.");
        } finally {
            relaxedCopy.dispose();
        }
    }

    @Test
    void testDeepCopyRetainsVariableCount() {
        // Populate original model with six variables.
        for (int i = 0; i < 6; i++) {
            model.addVariable();
        }
        int original = model.countVariables();
        // Deep copy via copy() (or copy(false, false))
        ExpressionsBasedModel deepCopy = model.copy();
        try {
            assertEquals(original, deepCopy.countVariables(), "Deep copy must preserve the variable count.");
        } finally {
            deepCopy.dispose();
        }
    }

    @Test
    void testDeepCopyIndependenceFromOriginalAfterModification() {
        // Original model with three variables.
        for (int i = 0; i < 3; i++) {
            model.addVariable();
        }
        // deep copy
        ExpressionsBasedModel deepCopy = model.copy();
        try {
            assertEquals(3, deepCopy.countVariables(), "Copy should initially have the same variable count as the original.");
            // Add two more variables to the original after copying.
            for (int i = 0; i < 2; i++) {
                model.addVariable();
            }
            assertEquals(5, model.countVariables(), "Original model should reflect the added variables.");
            assertEquals(3, deepCopy.countVariables(), "Deep copy must remain unchanged after the original is modified.");
        } finally {
            deepCopy.dispose();
        }
    }

    @Test
    void testIndicesAfterMultipleAdditions() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable("namedVar");
        Variable v3 = model.newVariable("newVar");
        assertAll(() -> assertEquals(0, model.indexOf(v1), "First variable should have index 0."), () -> assertEquals(1, model.indexOf(v2), "Second variable should have index 1."), () -> assertEquals(2, model.indexOf(v3), "Third variable should have index 2."), () -> assertEquals(3, model.countVariables(), "Total variable count must be 3."));
    }
}
