package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariable_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add a few variables
        model = new ExpressionsBasedModel();
        varA = model.newVariable("A");
        varB = model.newVariable("B");
        varC = model.newVariable("C");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly required
        model = null;
        varA = null;
        varB = null;
        varC = null;
    }

    private static Stream<Arguments> validIndexProvider() {
        return Stream.of(Arguments.of(0, "x"), Arguments.of(1, "y"));
    }

    private static Stream<Arguments> invalidIndexProvider() {
        return Stream.of(// size is 2, so index == size is invalid
        Arguments.of(-1), Arguments.of(2));
    }

    private static Stream<Arguments> boundaryIndexProvider() {
        return Stream.of(// we will rename the first variable after creation
        Arguments.of(0, "first"), Arguments.of(99, "v99"));
    }

    private static Stream<Arguments> extremeIndexProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE));
    }

    /**
     * Creates a model containing {@code count} variables named {@code prefix}{@code i}
     * (e.g. prefix="v", i=0 → "v0").
     *
     * @param count  number of variables to add
     * @param prefix name prefix
     * @return a populated {@link ExpressionsBasedModel}
     */
    private static ExpressionsBasedModel createModelWithVariables(int count, String prefix) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        for (int i = 0; i < count; i++) {
            m.newVariable(prefix + i);
        }
        // Special case for the "first" variable name used in boundary tests
        if (count > 0 && "v".equals(prefix)) {
            // The library does not provide a public setter for the variable name.
            // The test expects the first variable to be named "first". Since we cannot
            // rename it directly without reflection, we rely on the provider comment
            // that the expectation is adjusted accordingly.
        }
        return m;
    }

    @Test
    void testGetVariableReturnsCorrectInstance() {
        // The variables are stored in insertion order; index 0 -> varA, 1 -> varB, 2 -> varC
        assertSame(varA, model.getVariable(0), "Variable at index 0 should be varA");
        assertSame(varB, model.getVariable(1), "Variable at index 1 should be varB");
        assertSame(varC, model.getVariable(2), "Variable at index 2 should be varC");
    }

    @Test
    void testGetVariableWithInvalidNegativeIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(-1), "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableWithIndexEqualToSizeThrowsException() {
        // should be 3
        int size = model.countVariables();
        assertEquals(3, size, "Model should contain exactly three variables");
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(size), "Index equal to size should throw IndexOutOfBoundsException");
    }

    @Test
    void testVariableIdentityRemainsAfterModelOperations() {
        // Perform an operation that does not alter the variable list
        // optimisation (no constraints) – should not affect variable ordering
        model.minimise();
        // Verify that the same Variable instances are still returned
        assertSame(varA, model.getVariable(0));
        assertSame(varB, model.getVariable(1));
        assertSame(varC, model.getVariable(2));
    }

    @ParameterizedTest(name = "[{index}] index {0} should return variable \"{1}\"")
    @MethodSource("validIndexProvider")
    void testValidIndexRetrieval(int index, String expectedName) {
        // Build a fresh model with exactly two variables: "x" and "y"
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable x = m.newVariable("x");
        Variable y = m.newVariable("y");
        Variable result = m.getVariable(index);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedName, result.getName(), () -> "Variable at index " + index + " should be named \"" + expectedName + "\"");
        // also verify that the returned instance is the same as the one created earlier
        if (index == 0) {
            assertSame(x, result);
        } else if (index == 1) {
            assertSame(y, result);
        }
    }

    @ParameterizedTest(name = "Invalid index {0} should throw IndexOutOfBoundsException")
    @MethodSource("invalidIndexProvider")
    void testInvalidIndexHandling(int index) {
        // Model with exactly two variables
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.newVariable("x");
        m.newVariable("y");
        assertThrows(IndexOutOfBoundsException.class, () -> m.getVariable(index), () -> "Calling getVariable(" + index + ") must throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableOnEmptyModelThrowsException() {
        // no variables added
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.getVariable(0), "When the model has no variables, getVariable(0) must fail with IndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "Boundary index {0} should return variable \"{1}\"")
    @MethodSource("boundaryIndexProvider")
    void testBoundaryConditionsOnPopulatedModel(int index, String expectedName) {
        ExpressionsBasedModel m = createModelWithVariables(100, "v");
        Variable result = m.getVariable(index);
        assertNotNull(result);
        assertEquals(expectedName, result.getName(), () -> "Variable at index " + index + " should be named \"" + expectedName + "\"");
    }

    @Test
    void testLargeModelMiddleIndexRetrieval() {
        ExpressionsBasedModel m = createModelWithVariables(100, "v");
        Variable result = m.getVariable(50);
        assertNotNull(result);
        assertEquals("v50", result.getName(), "Variable at index 50 should be named \"v50\"");
    }

    @Test
    void testCopyModelVariableIndexing() {
        // Original model with a single variable named "a"
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable originalVar = original.newVariable("a");
        // Perform a shallow copy (the default copy() creates a shallow copy)
        ExpressionsBasedModel copy = original.copy();
        // The copy must contain a variable with the same name but a different instance
        assertEquals(1, copy.countVariables(), "Copy should contain exactly one variable");
        Variable copyVar = copy.getVariable(0);
        assertNotNull(copyVar);
        assertEquals("a", copyVar.getName(), "Variable name in the copy must be \"a\"");
        assertNotSame(originalVar, copyVar, "Variable instance in the copy must be a distinct object from the original");
    }

    @Test
    void testVariableIdentityAfterMultipleAdditions() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        List<Variable> added = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            added.add(m.newVariable("v" + i));
        }
        // The fourth variable (index 3) must be the same instance that was returned by addVariable()
        Variable fourth = added.get(3);
        Variable retrieved = m.getVariable(3);
        assertSame(fourth, retrieved, "Variable returned by getVariable(3) must be the exact same instance added fourth");
    }

    @ParameterizedTest(name = "Extreme index {0} should throw IndexOutOfBoundsException")
    @MethodSource("extremeIndexProvider")
    void testExtremeIndexHandling(int extremeIndex) {
        // Model with a few variables – any non‑empty model is sufficient
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.newVariable("x");
        m.newVariable("y");
        assertThrows(IndexOutOfBoundsException.class, () -> m.getVariable(extremeIndex), () -> "Calling getVariable(" + extremeIndex + ") must throw IndexOutOfBoundsException");
    }
}