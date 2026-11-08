package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionsBasedModel_newVariable_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
    }

    @Test
    void testNewVariableAddsVariableAndIsRetrievable() {
        // Given
        String varName = "myVar";
        // When
        Variable var = model.newVariable(varName);
        // Then
        assertNotNull(var, "newVariable should return a non‑null Variable instance");
        // The model should now contain exactly one variable
        assertEquals(1, model.countVariables(), "Model should contain one variable after creation");
        // The variable should be reachable by its index
        int index = model.indexOf(var);
        assertTrue(index >= 0, "Variable index should be non‑negative");
        assertSame(var, model.getVariable(index), "Variable retrieved by index should be the same instance");
        // The variable should appear in the stream returned by variables()
        assertTrue(model.variables().anyMatch(v -> v == var), "Variable should be present in the model's variable stream");
    }

    @Test
    void testAddVariableStringDelegatesToNewVariable() {
        // Given
        String name = "X1";
        // When
        // internally calls newVariable(name)
        Variable var = model.addVariable(name);
        // Then
        assertNotNull(var, "addVariable(String) should return a non‑null Variable");
        assertEquals(1, model.countVariables(), "Model should contain one variable after addVariable(String)");
        assertEquals(name, var.getName(), "Variable name should match the supplied name");
    }

    @Test
    void testAddVariableWithoutNameGeneratesSequentialName() {
        // When
        // should become "X0"
        Variable var0 = model.addVariable();
        // should become "X1"
        Variable var1 = model.addVariable();
        // Then
        assertEquals(2, model.countVariables(), "Two variables should have been added");
        assertEquals("X0", var0.getName(), "First auto‑generated name should be X0");
        assertEquals("X1", var1.getName(), "Second auto‑generated name should be X1");
        // Ensure they are distinct and correctly indexed
        assertNotEquals(var0, var1, "Variables must be distinct instances");
        assertEquals(0, model.indexOf(var0), "First variable should have index 0");
        assertEquals(1, model.indexOf(var1), "Second variable should have index 1");
    }

    @ParameterizedTest(name = "[{index}] name=\"{0}\"")
    @CsvSource({ // normal non‑null name
    "'x1', true", // empty string – allowed by the API
    "'',   true" })
    void testNewVariableBasicFunctionality(String name, boolean expectSuccess) {
        // When
        Variable var = model.newVariable(name);
        // Then
        assertNotNull(var, "newVariable should return a non‑null Variable instance");
        assertEquals(name, var.getName(), "Variable name must match the supplied name");
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable after creation");
        // Verify the variable can be retrieved via index and stream
        int idx = model.indexOf(var);
        assertTrue(idx >= 0, "Variable index should be non‑negative");
        assertSame(var, model.getVariable(idx), "Variable retrieved by index should be the same instance");
        assertTrue(model.variables().anyMatch(v -> v == var), "Variable should be present in the model's variable stream");
    }

    @Test
    void testNewVariableNullNameThrowsNPE() {
        // Given
        String name = null;
        // When / Then
        assertThrows(NullPointerException.class, () -> model.newVariable(name), "Calling newVariable(null) must throw NullPointerException");
    }

    @Test
    void testNewVariableDuplicateNameThrowsIllegalArgumentException() {
        // Given
        String dupName = "dupVar";
        // First variable is added successfully
        Variable first = model.newVariable(dupName);
        assertNotNull(first, "First variable must be created");
        // When / Then – second attempt with the same name must fail
        assertThrows(IllegalArgumentException.class, () -> model.newVariable(dupName), "Adding a variable with a duplicate name should throw IllegalArgumentException");
    }

    @Test
    void testNewVariableAfterDisposeThrowsIllegalStateException() {
        // Given
        // mark the model as no longer usable
        model.dispose();
        // When / Then
        assertThrows(IllegalStateException.class, () -> model.newVariable("afterDispose"), "Calling newVariable on a disposed model must throw IllegalStateException");
    }
}
