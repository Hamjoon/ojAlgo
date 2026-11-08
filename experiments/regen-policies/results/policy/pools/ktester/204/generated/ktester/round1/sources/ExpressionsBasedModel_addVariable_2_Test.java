package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addVariable_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required.
        model = null;
    }

    /**
     * Provides a stream of name values covering various valid scenarios.
     */
    private static Stream<Arguments> validNameProvider() {
        // extremely long name
        String longName = "a".repeat(2000);
        return Stream.of(// regular name
        Arguments.of("x"), // empty string
        Arguments.of(""), // whitespace‑only
        Arguments.of("   "), // special characters
        Arguments.of("!@#$%^&*()_+[]{}|;:',.<>?"), // Unicode characters
        Arguments.of("变量"), // leading/trailing spaces
        Arguments.of("  leadingAndTrailing  "), // extremely long name
        Arguments.of(longName));
    }

    @Test
    void testAddVariableWithName() {
        Variable var = model.addVariable("x");
        assertNotNull(var, "addVariable should return a non‑null Variable instance");
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable after addition");
        // The variable returned by the model should be the same instance we received.
        assertSame(var, model.getVariable(0), "Variable retrieved by index should be the same instance");
        // The variable's name should match the supplied name.
        assertEquals("x", var.name(), "Variable name should be the one supplied to addVariable");
    }

    @Test
    void testAddMultipleVariables() {
        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        assertEquals(2, model.countVariables(), "Model should contain two variables after two additions");
        assertSame(a, model.getVariable(0), "First variable should be retrievable at index 0");
        assertSame(b, model.getVariable(1), "Second variable should be retrievable at index 1");
        assertEquals("a", a.name(), "First variable name should be 'a'");
        assertEquals("b", b.name(), "Second variable name should be 'b'");
    }

    @Test
    void testAddVariableWithNullName() {
        // The contract of addVariable(String) is to reject null names.
        // The implementation throws a NullPointerException, which we verify here.
        assertThrows(NullPointerException.class, () -> model.addVariable(null), "addVariable(null) should throw NullPointerException");
    }

    @ParameterizedTest(name = "addVariable with name \"{0}\"")
    @MethodSource("validNameProvider")
    void testAddVariableWithVariousValidNames(String name) {
        Variable var = model.addVariable(name);
        assertNotNull(var, "addVariable should return a non‑null Variable instance");
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable after addition");
        assertSame(var, model.getVariable(0), "Variable retrieved by index should be the same instance");
        assertEquals(name, var.name(), "Variable name should be the one supplied to addVariable");
    }

    @Test
    void testAddVariableWithDuplicateName() {
        Variable first = model.addVariable("y");
        // same name again
        Variable second = model.addVariable("y");
        assertEquals(2, model.countVariables(), "Model should contain two variables after adding duplicates");
        assertSame(first, model.getVariable(0), "First variable should be at index 0");
        assertSame(second, model.getVariable(1), "Second variable should be at index 1");
        // Both variables keep the supplied name.
        assertEquals("y", first.name(), "First variable name should be 'y'");
        assertEquals("y", second.name(), "Second variable name should also be 'y'");
    }

    @Test
    void testAddVariableAfterExistingVariables() {
        // Populate the model with a few variables first.
        model.addVariable("a");
        model.addVariable("b");
        int countBefore = model.countVariables();
        Variable newVar = model.addVariable("z");
        assertEquals(countBefore + 1, model.countVariables(), "Model variable count should increase by one");
        assertSame(newVar, model.getVariable(countBefore), "New variable should be placed at the next free index");
        assertEquals("z", newVar.name(), "New variable name should match the supplied name");
    }

    @Test
    void testAddVariableAfterDisposingModel() {
        // Put the model into a disposed state.
        model.dispose();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.addVariable("a"), "Calling addVariable on a disposed model should throw IllegalStateException");
        // Verify the exception message contains a hint about disposal.
        assertTrue(thrown.getMessage().toLowerCase().contains("disposed"), "Exception message should mention that the model is disposed");
    }
}
