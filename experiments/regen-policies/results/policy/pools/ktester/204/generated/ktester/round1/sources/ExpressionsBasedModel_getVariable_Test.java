package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariable_Test {

    /**
     * Model under test – created fresh for each test method.
     */
    private ExpressionsBasedModel model;

    /**
     * Variables added to the model; kept for reference in assertions.
     */
    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new model and populate it with three variables.
        model = new ExpressionsBasedModel();
        // Use the fluent API to give the variables identifiable names.
        varA = model.newVariable("A").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        varB = model.newVariable("B").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        varC = model.newVariable("C").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern tidy.
        model = null;
        varA = varB = varC = null;
    }

    private static Stream<Arguments> multipleVariableIndices() {
        return Stream.of(Arguments.of(1, "B"), Arguments.of(2, "C"));
    }

    /**
     * Verify that a valid index returns the exact Variable instance that was added.
     */
    @Test
    void testGetVariableByValidIndex() {
        // Index 0 should return the first variable (varA)
        assertSame(varA, model.getVariable(0), "Variable at index 0 should be varA");
        // Index 1 should return the second variable (varB)
        assertSame(varB, model.getVariable(1), "Variable at index 1 should be varB");
        // Index 2 should return the third variable (varC)
        assertSame(varC, model.getVariable(2), "Variable at index 2 should be varC");
    }

    /**
     * Verify that requesting a negative index throws the expected exception.
     */
    @Test
    void testGetVariableWithNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(-1), "Calling getVariable with a negative index must throw IndexOutOfBoundsException");
    }

    /**
     * Verify that requesting an index equal to the size of the model throws the expected exception.
     */
    @Test
    void testGetVariableWithIndexEqualToSize() {
        int size = model.countVariables();
        assertEquals(3, size, "Model should contain exactly three variables");
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(size), "Calling getVariable with an index equal to the number of variables must throw IndexOutOfBoundsException");
    }

    /**
     * Verify that requesting an index greater than the size of the model throws the expected exception.
     */
    @Test
    void testGetVariableWithIndexGreaterThanSize() {
        int size = model.countVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(size + 5), "Calling getVariable with an index greater than the number of variables must throw IndexOutOfBoundsException");
    }

    /**
     * Single‑variable model – the only valid index is 0.
     */
    @Test
    void testBoundaryIndexSingleElement() {
        ExpressionsBasedModel single = new ExpressionsBasedModel();
        Variable only = single.newVariable("only").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertSame(only, single.getVariable(0), "The sole variable must be returned for index 0");
    }

    /**
     * Valid index retrieval for a model that contains exactly one variable named "x".
     */
    @Test
    void testValidIndexRetrievalSingleVariable() {
        ExpressionsBasedModel one = new ExpressionsBasedModel();
        Variable x = one.addVariable();
        x.setName("x");
        assertSame(x, one.getVariable(0), "Variable at index 0 should be the variable named \"x\"");
    }

    /**
     * Verify that indices 1 and 2 return the expected variables (\"y\" and \"z\")
     * when the model contains variables \"x\", \"y\", \"z\" in that order.
     */
    @ParameterizedTest(name = "index {0} should return variable \"{1}\"")
    @MethodSource("multipleVariableIndices")
    void testMultipleVariablesRetrieval(int index, String expectedName) {
        // Build a fresh model with the exact ordering required by the test case.
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable x = m.addVariable();
        x.setName("x");
        Variable y = m.addVariable();
        y.setName("y");
        Variable z = m.addVariable();
        z.setName("z");
        Variable result = m.getVariable(index);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedName, result.getName(), () -> "Variable at index " + index + " should be named \"" + expectedName + "\"");
    }

    /**
     * Empty model – any non‑negative index must fail.
     */
    @Test
    void testEmptyModelRetrieval() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.getVariable(0), "Empty model must throw IndexOutOfBoundsException for any index");
    }

    /**
     * Shallow copy should retain the same Variable instances.
     */
    @Test
    void testRetrievalAfterShallowCopy() {
        // Original model with a single variable "a"
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable a = original.addVariable();
        a.setName("a");
        // Shallow copy (shallow = true, prune = false)
        ExpressionsBasedModel shallowCopy = original.copy(true, false);
        // The copy must contain the same Variable object (identity equality)
        assertEquals(1, shallowCopy.countVariables(), "Shallow copy should contain one variable");
        assertSame(a, shallowCopy.getVariable(0), "Variable instance in shallow copy must be the same as in the original model");
    }

    /**
     * Pruned copy removes all variables – subsequent retrieval must fail.
     */
    @Test
    void testRetrievalAfterPruneCopy() {
        // Original model with a single variable (will be pruned)
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.addVariable();
        // Copy with shallow = true, prune = true – all variables are removed
        ExpressionsBasedModel prunedCopy = original.copy(true, true);
        assertEquals(0, prunedCopy.countVariables(), "Pruned copy should contain no variables");
        assertThrows(IndexOutOfBoundsException.class, () -> prunedCopy.getVariable(0), "Retrieving from an empty pruned copy must throw IndexOutOfBoundsException");
    }
}
