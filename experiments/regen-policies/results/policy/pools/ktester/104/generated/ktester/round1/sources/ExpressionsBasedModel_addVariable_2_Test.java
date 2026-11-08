package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addVariable_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to guarantee isolation
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly necessary but keeps the pattern clear
        model = null;
    }

    /**
     * A collection of valid variable names. The test verifies that the
     * variable is created, its name matches the supplied string and the
     * model variable count becomes 1.
     */
    static Stream<Arguments> validNamesProvider() {
        return Stream.of(// typical name
        Arguments.of("x1"), // empty name
        Arguments.of(""), // leading/trailing whitespace
        Arguments.of(" var "), // special characters
        Arguments.of("!@#$%^&*()_+"), // Unicode characters
        Arguments.of("变量"), // long name
        Arguments.of("a".repeat(200)));
    }

    @Test
    void testAddVariableWithName() {
        // GIVEN a fresh ExpressionsBasedModel
        // WHEN a variable with a specific name is added
        Variable var = model.addVariable("myVariable");
        // THEN the variable is not null and carries the supplied name
        assertNotNull(var, "Variable should not be null");
        assertEquals("myVariable", var.getName(), "Variable name should match the supplied name");
        // AND the model reflects the addition
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable");
        assertSame(var, model.getVariable(0), "The variable retrieved by index should be the same instance");
        assertEquals(0, model.indexOf(var), "Index of the variable should be 0");
        // AND the variable has default bounds (no explicit lower/upper limits)
        assertFalse(var.isFixed(), "New variable should not be fixed by default");
        assertFalse(var.isInteger(), "New variable should not be integer by default");
    }

    @Test
    void testAddMultipleVariablesWithDifferentNames() {
        // GIVEN a fresh model
        // WHEN two variables with distinct names are added
        Variable varA = model.addVariable("A");
        Variable varB = model.addVariable("B");
        // THEN both variables are present and have distinct indices
        assertEquals(2, model.countVariables(), "Model should contain two variables");
        assertEquals(0, model.indexOf(varA), "First variable should have index 0");
        assertEquals(1, model.indexOf(varB), "Second variable should have index 1");
        assertSame(varA, model.getVariable(0), "Variable A should be retrievable by its index");
        assertSame(varB, model.getVariable(1), "Variable B should be retrievable by its index");
    }

    @Test
    void testAddVariableDoesNotAffectExistingVariables() {
        // GIVEN a model with one variable already added
        Variable first = model.addVariable("first");
        int initialCount = model.countVariables();
        // WHEN another variable is added
        Variable second = model.addVariable("second");
        // THEN the original variable remains unchanged
        assertEquals(initialCount + 1, model.countVariables(), "Variable count should increase by one");
        assertEquals("first", first.getName(), "Original variable name should stay the same");
        assertEquals(0, model.indexOf(first), "Original variable index should stay the same");
        assertEquals(1, model.indexOf(second), "New variable should receive the next index");
    }

    @ParameterizedTest
    @MethodSource("validNamesProvider")
    void testAddVariableWithValidName(String name) {
        // WHEN a variable with the given name is added
        Variable var = model.addVariable(name);
        // THEN the variable is not null and carries the supplied name
        assertNotNull(var, "Variable should not be null");
        assertEquals(name, var.getName(), () -> "Variable name should match the supplied name [" + name + "]");
        // AND the model reflects the addition
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable after addition");
        assertSame(var, model.getVariable(0), "The variable retrieved by index should be the same instance");
        assertEquals(0, model.indexOf(var), "Index of the variable should be 0");
        // AND the variable has default properties (not fixed, not integer)
        assertFalse(var.isFixed(), "New variable should not be fixed by default");
        assertFalse(var.isInteger(), "New variable should not be integer by default");
    }

    @Test
    void testAddVariableWithNullName() {
        // The implementation is expected to reject a null name.
        assertThrows(NullPointerException.class, () -> model.addVariable(null), "Adding a variable with a null name should throw NullPointerException");
    }

    @Test
    void testAddVariableDuplicateName() {
        // GIVEN a variable with a specific name already present
        Variable first = model.addVariable("duplicate");
        assertEquals(1, model.countVariables());
        // WHEN a second variable with the same name is added
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> model.addVariable("duplicate"), "Adding a duplicate variable name should throw IllegalArgumentException");
        // THEN the exception message should indicate the name already exists
        assertTrue(thrown.getMessage().toLowerCase().contains("already exists"), "Exception message should contain 'already exists'");
    }

    @Test
    void testAddVariableWithNullCharInName() {
        // Name containing the Unicode null character should be rejected
        String nameWithNullChar = "\u0000";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> model.addVariable(nameWithNullChar), "Name containing null character should throw IllegalArgumentException");
        assertTrue(thrown.getMessage().toLowerCase().contains("invalid"), "Exception message should indicate the name is invalid");
    }

    @Test
    void testAddVariableWithNewlineOnlyName() {
        // Name consisting solely of a newline character should be rejected
        String newlineName = "\n";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> model.addVariable(newlineName), "Name consisting only of a newline should throw IllegalArgumentException");
        assertTrue(thrown.getMessage().toLowerCase().contains("invalid"), "Exception message should indicate the name is invalid");
    }

    @Test
    void testAddVariableAfterDispose() {
        // GIVEN a disposed model
        model.dispose();
        // WHEN attempting to add a variable
        ProgrammingError thrown = assertThrows(ProgrammingError.class, () -> model.addVariable("afterDispose"), "Adding a variable after dispose should throw ProgrammingError");
        // THEN the exception indicates the model is no longer mutable
        assertTrue(thrown.getMessage().toLowerCase().contains("disposed") || thrown.getMessage().toLowerCase().contains("illegal state"), "Exception message should mention disposed or illegal state");
    }

    @Test
    void testAddVariableAfterSnapshot() {
        // GIVEN a snapshot is taken – the snapshot is immutable, but the
        // original model may become read‑only according to the library contract.
        model.snapshot();
        // WHEN attempting to add a variable to the original model
        ProgrammingError thrown = assertThrows(ProgrammingError.class, () -> model.addVariable("afterSnapshot"), "Adding a variable after snapshot should throw ProgrammingError");
        // THEN the exception message should indicate an illegal state
        assertTrue(thrown.getMessage().toLowerCase().contains("snapshot") || thrown.getMessage().toLowerCase().contains("illegal state"), "Exception message should mention snapshot or illegal state");
    }
}
