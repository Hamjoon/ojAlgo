package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_newVariable_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    /**
     * Helper to read the (possibly private) name field of a Variable via reflection.
     */
//     private static String getVariableName(ExpressionsBasedModel.Variable var) {
//         try {
//             Field nameField = Variable.class.getDeclaredField("myName");
//             nameField.setAccessible(true);
//             Object value = nameField.get(var);
//             return (String) value;
//         } catch (NoSuchFieldException | IllegalAccessException e) {
//             // If the internal field name is different, fallback to null – the test will still
//             // verify that no exception is thrown for null/empty names.
//             return null;
//         }
//     }

    /**
     * Supplies arguments for {@link #testNewVariableNormalOperation(String, int, String)}.
     */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// first variable
        Arguments.of("x", 0, "first variable"), // second variable – sequential index
        Arguments.of("y", 1, "second variable"), // null name – allowed by Variable constructor
        Arguments.of(null, 2, "null name variable"), // empty string name
        Arguments.of("", 3, "empty name variable"), // duplicate name – first occurrence
        Arguments.of("dup", 4, "first duplicate name variable"), // duplicate name – second occurrence
        Arguments.of("dup", 5, "second duplicate name variable"));
    }

    /**
     * Verify that a normal call to {@code newVariable(String)} creates a variable,
     * assigns it the supplied name and the correct index, and that the model
     * registers the variable.
     */
    @Test
    void testNewVariableCreatesAndRegistersVariable() {
        // given
        String varName = "myVar";
        // when
        Variable var = model.newVariable(varName);
        // then
        assertNotNull(var, "Variable should not be null");
        // The variable should be the first (index 0) in the model
        assertEquals(0, var.index(), "Variable index should be 0 for the first variable");
        // The model should now contain exactly one variable
        assertEquals(1, model.countVariables(), "Model should contain one variable after creation");
        // Retrieving the variable by index should return the same instance
        assertSame(var, model.getVariable(0), "Variable retrieved from model should be the same instance");
        // Name stored in the variable must match the supplied name
        assertEquals(varName, getVariableName(var), "Variable name should be the one supplied");
    }

    /**
     * Verify that calling {@code newVariable(String)} on a shallow‑copy model
     * throws an {@link IllegalStateException} as documented.
     */
    @Test
    void testNewVariableThrowsWhenModelIsShallowCopy() {
        // given: a normal model with at least one variable (to make the copy meaningful)
        model.newVariable("base");
        // create a shallow copy – the package‑private constructor is accessible because
        // this test resides in the same package.
        ExpressionsBasedModel shallowCopy = new ExpressionsBasedModel(model, true, false);
        // when / then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> shallowCopy.newVariable("shouldFail"), "Calling newVariable on a shallow‑copy model must throw IllegalStateException");
        assertTrue(thrown.getMessage().contains("work copy"), "Exception message should indicate that the model is a work copy");
    }

    /**
     * Parameterised test covering normal creation of variables, including duplicate names.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the name to pass to {@code newVariable}</li>
     *   <li>the expected index of the created variable</li>
     *   <li>a description for readability</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'', expectedIndex={1}")
    @MethodSource("normalOperationProvider")
    void testNewVariableNormalOperation(String name, int expectedIndex, String description) {
        // when
        Variable var = model.newVariable(name);
        // then
        assertNotNull(var, description + " – variable should not be null");
        assertEquals(expectedIndex, var.index(), description + " – variable index should match expected");
        assertEquals(expectedIndex + 1, model.countVariables(), description + " – model variable count should increase accordingly");
        assertSame(var, model.getVariable(expectedIndex), description + " – variable retrieved by index should be the same instance");
        // name may be null/empty – we only assert that the stored name equals the supplied one
        assertEquals(name, getVariableName(var), description + " – stored name should match supplied name");
    }

    /**
     * Adds a large number of variables to verify that indexing works correctly
     * even when the internal list grows substantially.
     *
     * The test adds {@code BATCH_SIZE} variables; the last variable's index
     * must be {@code BATCH_SIZE - 1} and the model's variable count must equal
     * {@code BATCH_SIZE}.
     */
    @Test
    void testNewVariableLargeNumberOfVariables() {
        // large enough to test scaling but still fast
        final int BATCH_SIZE = 10_000;
        Variable lastVar = null;
        for (int i = 0; i < BATCH_SIZE; i++) {
            String name = "v" + i;
            lastVar = model.newVariable(name);
            assertEquals(i, lastVar.index(), "Variable created at iteration " + i + " should have index " + i);
        }
        assertNotNull(lastVar, "At least one variable should have been created");
        assertEquals(BATCH_SIZE - 1, lastVar.index(), "Index of the last variable must be count-1");
        assertEquals(BATCH_SIZE, model.countVariables(), "Model must report the correct total number of variables");
    }
}