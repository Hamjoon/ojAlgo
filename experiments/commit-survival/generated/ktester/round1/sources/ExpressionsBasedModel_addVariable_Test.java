package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.provider.ValueSource;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addVariable_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test guarantees isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly necessary but keeps the pattern clear.
        model = null;
    }

    /**
     * Verify that the default‑name variable is created and indexed correctly.
     */
    @Test
    void testAddVariable_DefaultNameAndIndex() {
        // First variable – should be named "X0"
        Variable v0 = model.addVariable();
        assertNotNull(v0, "addVariable() must not return null");
        assertEquals(0, model.indexOf(v0), "First variable should have index 0");
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable");
        assertEquals("X0", v0.name(), "Default generated name should be 'X0'");
        // Second variable – should be named "X1"
        Variable v1 = model.addVariable();
        assertNotNull(v1);
        assertEquals(1, model.indexOf(v1), "Second variable should have index 1");
        assertEquals(2, model.countVariables(), "Model should contain exactly two variables");
        assertEquals("X1", v1.name(), "Second default generated name should be 'X1'");
        // Ensure the model returns the same instances via getVariable(int)
        assertSame(v0, model.getVariable(0), "Model should return the original first variable");
        assertSame(v1, model.getVariable(1), "Model should return the original second variable");
    }

    /**
     * Verify that a variable created with an explicit name is stored correctly.
     */
    @Test
    void testAddVariable_WithCustomName() {
        String customName = "myCustomVar";
        Variable custom = model.addVariable(customName);
        assertNotNull(custom, "addVariable(String) must not return null");
        assertEquals(customName, custom.name(), "Variable should retain the supplied name");
        assertEquals(0, model.indexOf(custom), "First variable should have index 0");
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable");
        // Adding another variable without a name should not interfere with the custom name.
        Variable auto = model.addVariable();
        assertEquals("X1", auto.name(), "Automatically generated name should continue after a custom one");
        assertEquals(2, model.countVariables(), "Model should now contain two variables");
        assertEquals(1, model.indexOf(auto), "Automatically generated variable should have index 1");
    }

    /**
     * Verify that the list returned by {@code getVariables()} contains all added variables in order.
     */
    @Test
    void testGetVariables_ListOrder() {
        Variable a = model.addVariable("A");
        Variable b = model.addVariable("B");
        // auto‑named "X2"
        Variable c = model.addVariable();
        List<Variable> vars = model.getVariables();
        assertEquals(3, vars.size(), "Variable list size must match number of added variables");
        assertSame(a, vars.get(0), "First element must be the first added variable");
        assertSame(b, vars.get(1), "Second element must be the second added variable");
        assertSame(c, vars.get(2), "Third element must be the third added variable");
    }

    /**
     * Adding many variables should not break the internal naming sequence.
     */
    @Test
    void testAddVariable_ManySequentialNames() {
        final int N = 10;
        for (int i = 0; i < N; i++) {
            Variable v = model.addVariable();
            assertEquals("X" + i, v.name(), "Generated name should follow the sequence X0, X1, …");
        }
        assertEquals(N, model.countVariables(), "Model should contain N variables after the loop");
    }

    /**
     * Parameterised version of the many‑variables test for a range of sizes.
     */
    @ParameterizedTest(name = "addVariable generates correct name up to X{0}")
    @ValueSource(ints = { 0, 1, 5, 15, 30 })
    void testAddVariable_ParameterizedNames(int count) {
        for (int i = 0; i < count; i++) {
            Variable v = model.addVariable();
            assertEquals("X" + i, v.name(), () -> "Variable " + i + " should be named X" + i);
        }
        assertEquals(count, model.countVariables(), () -> "Model should contain exactly " + count + " variables");
    }

    /**
     * Adding a custom variable that collides with the auto‑generated naming scheme must fail.
     */
    @Test
    @DisplayName("Duplicate name after custom addition throws ProgrammingError")
    void testAddVariable_DuplicateNameException() {
        // Add a custom variable with the name that the next auto‑generated variable would use.
        Variable custom = model.addVariable("X0");
        assertNotNull(custom);
        // The model now contains one variable; the next auto‑generated name would also be "X0".
        // Expect a ProgrammingError (or IllegalArgumentException) when we call addVariable().
        assertThrows(ProgrammingError.class, () -> model.addVariable(), "Adding a variable that would duplicate an existing name must throw ProgrammingError");
    }

    /**
     * Corrupting the internal variable list should result in a NullPointerException.
     */
    @Test
    @DisplayName("Internal state corruption (myVariables = null) leads to NPE")
    void testAddVariable_InternalStateCorruption() throws Exception {
        // Use reflection to set the private field myVariables to null.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        field.set(model, null);
        // Now invoke addVariable() – it should attempt to read myVariables.size() and throw NPE.
        assertThrows(NullPointerException.class, () -> model.addVariable(), "Calling addVariable() when myVariables is null must throw NullPointerException");
    }

    /**
     * Ensure that each call to addVariable() creates a distinct instance.
     */
    @Test
    void testAddVariable_DistinctInstances() {
        Variable v0 = model.addVariable();
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        assertNotSame(v0, v1, "First and second variables must be different instances");
        assertNotSame(v0, v2, "First and third variables must be different instances");
        assertNotSame(v1, v2, "Second and third variables must be different instances");
        // The only equality should be via identity when fetched from the model.
        assertSame(v0, model.getVariable(0));
        assertSame(v1, model.getVariable(1));
        assertSame(v2, model.getVariable(2));
    }
}
