package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ojalgo.ProgrammingError;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addVariable_2_Test {

    /**
     * Model used in the tests.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern tidy.
        model = null;
    }

    @Test
    void testAddVariableWithName() {
        // GIVEN a fresh model
        assertEquals(0, model.countVariables(), "Model should start with zero variables");
        // WHEN a variable is added with a specific name
        Variable var = model.addVariable("myVar");
        // THEN the variable is not null and has the expected name
        assertNotNull(var, "Returned variable must not be null");
        assertEquals("myVar", var.getName(), "Variable name should match the supplied name");
        // AND the model now contains exactly one variable
        assertEquals(1, model.countVariables(), "Model should contain one variable after addition");
        // AND the variable can be retrieved by its index
        int index = model.indexOf(var);
        assertTrue(index >= 0, "Variable index should be non‑negative");
        assertSame(var, model.getVariable(index), "Variable retrieved by index should be the same instance");
        // AND the variable can be retrieved by name via the expression factory (if needed)
        // The model does not expose a direct getVariableByName, but we can verify that the
        // variable appears in the stream of variables.
        boolean found = model.variables().anyMatch(v -> v.getName().equals("myVar"));
        assertTrue(found, "Variable should be present in the model's variable stream");
    }

    @Test
    void testAddMultipleVariablesWithDifferentNames() {
        // GIVEN a fresh model
        assertEquals(0, model.countVariables());
        // WHEN several variables are added
        Variable a = model.addVariable("A");
        Variable b = model.addVariable("B");
        Variable c = model.addVariable("C");
        // THEN the model reports the correct count
        assertEquals(3, model.countVariables());
        // AND each variable can be located by its index
        assertEquals(a, model.getVariable(model.indexOf(a)));
        assertEquals(b, model.getVariable(model.indexOf(b)));
        assertEquals(c, model.getVariable(model.indexOf(c)));
        // AND the names are preserved
        assertEquals("A", a.getName());
        assertEquals("B", b.getName());
        assertEquals("C", c.getName());
    }

    @Test
    void testAddVariableDoesNotAffectExistingVariables() {
        // GIVEN a model with one variable
        Variable first = model.addVariable("first");
        int firstIndex = model.indexOf(first);
        assertEquals(1, model.countVariables());
        // WHEN a second variable is added
        Variable second = model.addVariable("second");
        // THEN both variables are still present and their indices are stable
        assertEquals(2, model.countVariables());
        assertEquals(firstIndex, model.indexOf(first), "First variable index should remain unchanged");
        assertEquals(first, model.getVariable(firstIndex), "First variable should be retrievable by its original index");
        assertNotNull(second);
        assertEquals("second", second.getName());
    }

    @Test
    void testAddVariableNullNameThrows() {
        assertThrows(NullPointerException.class, () -> model.addVariable(null), "Adding a variable with a null name should throw NullPointerException");
    }

    @Test
    void testAddVariableEmptyNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> model.addVariable(""), "Adding a variable with an empty name should be rejected");
    }

    @Test
    void testAddVariableWhitespaceOnlyThrows() {
        assertThrows(IllegalArgumentException.class, () -> model.addVariable("   "), "A name consisting only of whitespace must be rejected");
    }

    @Test
    void testAddVariableDuplicateNameThrows() {
        model.addVariable("dup");
        assertThrows(ProgrammingError.class, () -> model.addVariable("dup"), "Adding a variable with a duplicate name should raise ProgrammingError");
    }

    @Test
    void testAddVariableDuplicateAfterTrimThrows() {
        // first variable with surrounding spaces – the implementation trims the name
        Variable v1 = model.addVariable("  duplicateAfterTrim  ");
        assertEquals("duplicateAfterTrim", v1.getName().trim());
        // second variable with the trimmed name should be considered a duplicate
        assertThrows(ProgrammingError.class, () -> model.addVariable("duplicateAfterTrim"), "Duplicate detection must consider trimmed names");
    }

    @Test
    void testAddVariableLeadingTrailingSpacesAreTrimmed() {
        Variable v = model.addVariable("   spaced   ");
        assertEquals("spaced", v.getName(), "Leading/trailing whitespace should be trimmed from the variable name");
        assertEquals(1, model.countVariables(), "Variable count must increase even when the supplied name contains surrounding spaces");
    }

    @ParameterizedTest(name = "[{index}] name=\"{0}\"")
    @CsvSource({ // name,            expectedResult (true = success, false = exception)
    "'var-1',            true", "'var with spaces', true", "'var_123',          true", "'变量',             true" })
    void testAddVariableAllowedNames(String name, boolean expectedSuccess) {
        if (expectedSuccess) {
            Variable v = assertDoesNotThrow(() -> model.addVariable(name), "Variable with name '" + name + "' should be accepted");
            assertEquals(name, v.getName(), "Returned variable must preserve the supplied name");
        } else {
            fail("All names in this test are expected to succeed; adjust the CSV data if needed.");
        }
    }

    @Test
    void testAddVariableHyphenAccepted() {
        Variable v = model.addVariable("var-1");
        assertNotNull(v);
        assertEquals("var-1", v.getName());
    }

    @Test
    void testAddVariableUnicodeNameAccepted() {
        Variable v = model.addVariable("变量");
        assertNotNull(v);
        assertEquals("变量", v.getName());
    }

    @Test
    void testAddVariableVeryLongName() {
        // 1024‑character name
        StringBuilder sb = new StringBuilder(1024);
        for (int i = 0; i < 1024; i++) {
            sb.append('a');
        }
        String longName = sb.toString();
        Variable v = assertDoesNotThrow(() -> model.addVariable(longName), "A 1024‑character name should be accepted unless the implementation imposes a limit");
        assertEquals(longName, v.getName());
        assertEquals(1, model.countVariables());
    }

    @Test
    void testAddVariableControlCharactersThrow() {
        // newline
        assertThrows(IllegalArgumentException.class, () -> model.addVariable("\nnewline"), "Names containing newline characters must be rejected");
        // tab
        assertThrows(IllegalArgumentException.class, () -> model.addVariable("var\tTab"), "Names containing tab characters must be rejected");
    }

    @Test
    void testAddVariableAfterDisposeThrows() {
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.addVariable("stateVar"), "Adding a variable after the model has been disposed must fail");
    }

    @Test
    void testAddVariableAfterSolveThrows() {
        // Build a trivially solvable model:
        //   minimise 0 * x  subject to 0 <= x <= 1
        Variable x = model.addVariable("x");
        x.lower(BigDecimal.ZERO);
        x.upper(BigDecimal.ONE);
        // No explicit objective – the default is zero, which is solvable.
        // triggers solving
        model.minimise();
        // After solving the model should be immutable.
        assertThrows(IllegalStateException.class, () -> model.addVariable("postSolveVar"), "Adding a variable after the model has been solved must be prohibited");
    }
}
