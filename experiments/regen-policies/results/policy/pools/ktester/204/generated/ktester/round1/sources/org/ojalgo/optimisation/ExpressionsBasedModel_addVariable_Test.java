package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addVariable_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector
        model = null;
    }

    /**
     * Supplies a range of counts that cover single‑digit, double‑digit and
     * triple‑digit suffixes as required by the edge‑case specifications.
     */
    private static Stream<Integer> provideVariableCounts() {
        return Stream.of(// up to X2
        3, // up to X10
        11, // up to X20
        21, // up to X30
        31);
    }

    @Test
    void testAddVariableCreatesVariableWithSequentialName() {
        // First variable
        Variable v0 = model.addVariable();
        assertNotNull(v0, "addVariable() should return a non‑null Variable");
        assertEquals("X0", v0.getName(), "First variable should be named X0");
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable after first addition");
        // Second variable
        Variable v1 = model.addVariable();
        assertNotNull(v1);
        assertEquals("X1", v1.getName(), "Second variable should be named X1");
        assertEquals(2, model.countVariables(), "Model should contain exactly two variables after second addition");
        // Third variable
        Variable v2 = model.addVariable();
        assertNotNull(v2);
        assertEquals("X2", v2.getName(), "Third variable should be named X2");
        assertEquals(3, model.countVariables(), "Model should contain exactly three variables after third addition");
        // Verify that the variables are reachable via the model's accessor methods
        assertSame(v0, model.getVariable(0), "Variable at index 0 should be the first added variable");
        assertSame(v1, model.getVariable(1), "Variable at index 1 should be the second added variable");
        assertSame(v2, model.getVariable(2), "Variable at index 2 should be the third added variable");
    }

    @Test
    void testAddVariableDoesNotAlterExistingVariables() {
        // X0
        Variable first = model.addVariable();
        // X1
        Variable second = model.addVariable();
        // Change a property of the first variable (e.g., lower bound) and ensure it stays unchanged after adding another variable
        first.lower(5);
        // X2
        Variable third = model.addVariable();
        assertEquals(5, first.lower().doubleValue(), "Lower bound of the first variable should remain as set");
        assertEquals("X2", third.getName(), "Third variable should be named X2");
        assertEquals(3, model.countVariables(), "Model should contain three variables after three additions");
    }

    /**
     * Verify that after adding a custom‑named variable via {@code addVariable(String)},
     * the auto‑generated name continues from the current size of the internal list.
     */
    @Test
    void testAddVariableAfterCustomNamedVariable() {
        // Add a variable with an explicit name
        Variable custom = model.addVariable("customVar");
        assertEquals("customVar", custom.getName(), "Custom variable should retain its given name");
        assertEquals(1, model.countVariables(), "Model should contain one variable after custom addition");
        // Now add a variable using the no‑arg overload
        Variable generated = model.addVariable();
        assertEquals("X1", generated.getName(), "Auto‑generated name should be X1 because the list already contains one element");
        assertEquals(2, model.countVariables(), "Model should contain two variables after the second addition");
    }

    /**
     * Parameterized test that adds a given number of variables and checks the naming
     * pattern (X0, X1, …) as well as the final variable count.
     *
     * @param count number of variables to add
     */
    @ParameterizedTest(name = "addVariable sequential naming for {0} variables")
    @MethodSource("provideVariableCounts")
    void testSequentialNamingUpToCount(int count) {
        for (int i = 0; i < count; i++) {
            Variable v = model.addVariable();
            assertEquals("X" + i, v.getName(), () -> "Variable " + i + " should be named X" + i);
        }
        assertEquals(count, model.countVariables(), () -> "Model should report exactly " + count + " variables");
        // Verify that the last variable's name matches the expected suffix
        Variable last = model.getVariable(count - 1);
        assertEquals("X" + (count - 1), last.getName(), "The last variable's name must match its index");
    }

    /**
     * Adding 11 variables in a loop and checking that the last one is named X10.
     */
    @Test
    void testAddElevenVariables_LastNameIsX10() {
        for (int i = 0; i < 11; i++) {
            model.addVariable();
        }
        assertEquals(11, model.countVariables(), "Model should contain 11 variables");
        Variable last = model.getVariable(10);
        assertEquals("X10", last.getName(), "The 11th variable must be named X10");
    }

    /**
     * Adding 21 variables and ensuring the 21st (index 20) is named X20.
     */
    @Test
    void testAddTwentyOneVariables_LastNameIsX20() {
        for (int i = 0; i < 21; i++) {
            model.addVariable();
        }
        assertEquals(21, model.countVariables(), "Model should contain 21 variables");
        Variable last = model.getVariable(20);
        assertEquals("X20", last.getName(), "The 21st variable must be named X20");
    }

    /**
     * Adding 31 variables and ensuring the 31st (index 30) is named X30.
     */
    @Test
    void testAddThirtyOneVariables_LastNameIsX30() {
        for (int i = 0; i < 31; i++) {
            model.addVariable();
        }
        assertEquals(31, model.countVariables(), "Model should contain 31 variables");
        Variable last = model.getVariable(30);
        assertEquals("X30", last.getName(), "The 31st variable must be named X30");
    }

    /**
     * After disposing the model, any attempt to modify it (e.g. addVariable)
     * must throw a {@link ProgrammingError}.
     */
    @Test
    void testAddVariableAfterModelDisposalThrowsProgrammingError() {
        // Mark the model as disposed
        model.dispose();
        assertThrows(ProgrammingError.class, () -> model.addVariable(), "Calling addVariable() on a disposed model should throw ProgrammingError");
    }
}