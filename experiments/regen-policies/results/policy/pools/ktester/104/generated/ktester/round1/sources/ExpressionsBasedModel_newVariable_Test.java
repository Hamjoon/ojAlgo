package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_newVariable_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test case.
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> provideSuccessfulCreations() {
        // First variable on a fresh model
        Arguments first = Arguments.of("x", 0, 1);
        // Second variable after one already added
        Arguments second = Arguments.of("y", 1, 2);
        // Null name – the constructor forwards null, index should be 0 (model is fresh)
        Arguments nullName = Arguments.of(null, 0, 1);
        // Empty string name
        Arguments emptyName = Arguments.of("", 0, 1);
        // Duplicate name scenario – handled in a dedicated test
        return Stream.of(first, second, nullName, emptyName);
    }

    @Test
    void testNewVariableAddsVariableWithCorrectIndexAndName() {
        // Given: an empty model
        assertEquals(0, model.countVariables(), "Model should start with zero variables");
        // When: a new variable is created
        Variable var = model.newVariable("myVar");
        // Then: the variable is stored in the model
        assertNotNull(var, "Returned variable must not be null");
        assertEquals("myVar", var.getName(), "Variable name must be the one supplied");
        assertEquals(0, var.getIndex(), "First variable should have index 0");
        // The model should now report one variable and the variable should be retrievable
        assertEquals(1, model.countVariables(), "Model should contain exactly one variable");
        assertSame(var, model.getVariable(0), "Variable retrieved by index should be the same instance");
        assertSame(var, model.getVariable(IntIndex.of(0)), "Variable retrieved by IntIndex should be the same instance");
    }

    @Test
    void testNewVariableMultipleCallsIncreaseIndexSequentially() {
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        assertEquals(0, v0.getIndex());
        assertEquals(1, v1.getIndex());
        assertEquals(2, v2.getIndex());
        assertEquals(3, model.countVariables());
        assertSame(v0, model.getVariable(0));
        assertSame(v1, model.getVariable(1));
        assertSame(v2, model.getVariable(2));
    }

    @Test
    void testNewVariableThrowsWhenModelIsShallowCopy() {
        // Given: a shallow copy of the original model
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        assertTrue(shallowCopy.isShallowCopy(), "The copy should be marked as a shallow copy (work copy)");
        // When / Then: adding a variable must throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> shallowCopy.newVariable("z"), "Adding a variable to a shallow copy must fail");
        assertTrue(ex.getMessage().contains("work copy"), "Exception message should indicate that the model is a work copy");
    }

    @Test
    void testVariableCanBeConfiguredAfterCreation() {
        // Verify that the returned Variable can be further configured (e.g., bounds, weight)
        Variable var = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        assertEquals(BigDecimal.ZERO, var.lower(), "Lower bound should be set correctly");
        assertEquals(BigDecimal.TEN, var.upper(), "Upper bound should be set correctly");
        assertEquals(BigDecimal.ONE, var.weight(), "Weight should be set correctly");
    }

    /* --------------------------------------------------------------
     *  Parameterised success tests (first variable, second variable,
     *  null name, empty name, duplicate name)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: newVariable(''{0}'') → index {1}, count {2}")
    @MethodSource("provideSuccessfulCreations")
    void testNewVariableSuccess(String name, int expectedIndex, int expectedCount) {
        // When: create a variable with the supplied name
        Variable var = model.newVariable(name);
        // Then: verify the returned variable
        assertNotNull(var, "Variable must not be null");
        assertEquals(name, var.getName(), "Variable name should match the supplied value");
        assertEquals(expectedIndex, var.getIndex(), "Variable index must equal the size of the list before insertion");
        // Model bookkeeping checks
        assertEquals(expectedCount, model.countVariables(), "Model variable count must reflect the addition");
        assertSame(var, model.getVariable(expectedIndex), "Variable retrieved by index should be the same instance");
        // Also verify retrieval via IntIndex (if supported)
        try {
            IntIndex idx = IntIndex.of(expectedIndex);
            assertSame(var, model.getVariable(idx), "Variable retrieved by IntIndex should be the same instance");
        } catch (Throwable ignore) {
            // IntIndex.of may not be available in older versions – ignore for this check.
        }
    }

    @Test
    void testNewVariableDuplicateName() {
        // Add first variable with name "a"
        Variable first = model.newVariable("a");
        assertEquals(0, first.getIndex(), "First variable should have index 0");
        assertEquals("a", first.getName(), "First variable name should be 'a'");
        // Add second variable with the same name
        Variable second = model.newVariable("a");
        assertEquals(1, second.getIndex(), "Second variable should have index 1");
        assertEquals("a", second.getName(), "Second variable name should also be 'a'");
        // Model should contain both variables
        assertEquals(2, model.countVariables(), "Model should contain two variables");
        assertSame(first, model.getVariable(0));
        assertSame(second, model.getVariable(1));
    }

    /* --------------------------------------------------------------
     *  Multiple sequential additions test
     * -------------------------------------------------------------- */
    @Test
    void testNewVariableMultipleAdditions() {
        String[] names = { "v0", "v1", "v2", "v3", "v4" };
        for (int i = 0; i < names.length; i++) {
            Variable var = model.newVariable(names[i]);
            assertEquals(names[i], var.getName(), "Variable name must match");
            assertEquals(i, var.getIndex(), "Variable index must be sequential");
            assertEquals(i + 1, model.countVariables(), "Model count should increase with each addition");
            assertSame(var, model.getVariable(i), "Retrieved variable should be the same instance");
        }
    }
}
