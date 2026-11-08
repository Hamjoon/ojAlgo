package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOf_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /**
     * Provides a stream of arguments where the first element is a mocked {@link Variable},
     * the second element is the expected integer index, and the third element is a description
     * used for test readability.
     */
    private static Stream<Arguments> provideVariablesForSuccessfulIndex() {
        return Stream.of(Arguments.of(mockVariableWithIndex(5), 5, "Typical case – positive index 5"), Arguments.of(mockVariableWithIndex(0), 0, "Edge case – index zero"), Arguments.of(mockVariableWithIndex(-3), -3, "Negative index – -3"), Arguments.of(mockVariableWithIndex(Integer.MAX_VALUE), Integer.MAX_VALUE, "Extreme positive index – Integer.MAX_VALUE"), Arguments.of(mockVariableWithIndex(Integer.MIN_VALUE), Integer.MIN_VALUE, "Extreme negative index – Integer.MIN_VALUE"), Arguments.of(mockVariableWithIndex(123_456_789), 123_456_789, "Large positive index – 123456789"));
    }

    /**
     * Provides variables that cause the method to throw an exception.
     */
    private static Stream<Arguments> provideVariablesForException() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Null variable should cause NPE"), Arguments.of(mockVariableWithNullIndex(), NullPointerException.class, "Variable with getIndex() == null should cause NPE"), Arguments.of(mockVariableWithGetIndexException(new IllegalArgumentException("bad arg")), IllegalArgumentException.class, "Variable#getIndex() throws IllegalArgumentException"), Arguments.of(mockVariableWithGetIndexException(new NullPointerException("null")), NullPointerException.class, "Variable#getIndex() throws NullPointerException"));
    }

    /**
     * Creates a mock {@link Variable} whose {@code getIndex()} returns an {@link IntIndex}
     * wrapping the supplied integer.
     */
    private static Variable mockVariableWithIndex(int indexValue) {
        Variable mockVar = Mockito.mock(Variable.class);
        // IntIndex is a simple value holder; we instantiate a real one.
        IntIndex intIndex = new IntIndex(indexValue);
        Mockito.when(mockVar.getIndex()).thenReturn(intIndex);
        return mockVar;
    }

    /**
     * Creates a mock {@link Variable} whose {@code getIndex()} returns {@code null}.
     */
    private static Variable mockVariableWithNullIndex() {
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenReturn(null);
        return mockVar;
    }

    /**
     * Creates a mock {@link Variable} whose {@code getIndex()} throws the supplied exception.
     */
    private static Variable mockVariableWithGetIndexException(RuntimeException toThrow) {
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenThrow(toThrow);
        return mockVar;
    }

    @Test
    void testIndexOfSequentialVariables() {
        // Add three variables in a known order.
        Variable v0 = model.newVariable("v0");
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        // Verify that the indices are assigned sequentially starting at 0.
        Assertions.assertEquals(0, model.indexOf(v0), "First variable should have index 0");
        Assertions.assertEquals(1, model.indexOf(v1), "Second variable should have index 1");
        Assertions.assertEquals(2, model.indexOf(v2), "Third variable should have index 2");
    }

    @Test
    void testIndexOfRetrievedVariableMatchesOriginal() {
        // Add a variable and capture its index.
        Variable original = model.newVariable("original");
        int idx = model.indexOf(original);
        // Retrieve the variable by its index and compare.
        Variable retrieved = model.getVariable(idx);
        Assertions.assertNotNull(retrieved, "Variable retrieved by index should not be null");
        Assertions.assertSame(original, retrieved, "Variable retrieved by index should be the same instance");
    }

    @Test
    void testIndexOfVariableAfterAddingMoreVariables() {
        // Add a variable, then a few more, and ensure its index stays constant.
        Variable first = model.newVariable("first");
        int firstIdx = model.indexOf(first);
        Assertions.assertEquals(0, firstIdx, "First variable should be index 0");
        // Add additional variables.
        model.newVariable("second");
        model.newVariable("third");
        model.newVariable("fourth");
        // The original variable's index must remain unchanged.
        Assertions.assertEquals(firstIdx, model.indexOf(first), "Index of the first variable must remain unchanged after adding more variables");
    }

    @Test
    void testIndexOfVariableNotInModelThrowsException() {
        // Create a variable that is not added to the model.
        Variable stray = Variable.make("stray");
        // The variable has no index assigned by the model; accessing its index should
        // result in an IllegalStateException (as per Variable implementation). We
        // assert that the exception propagates.
        Assertions.assertThrows(IllegalStateException.class, () -> model.indexOf(stray), "Calling indexOf on a variable that is not part of the model should throw IllegalStateException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideVariablesForSuccessfulIndex")
    void testIndexOfWithMockedVariable(Variable variableMock, int expected, String description) {
        // The model instance is not used for the core logic, but we keep it for consistency.
        assertEquals(expected, model.indexOf(variableMock), "indexOf should forward the index from Variable#getIndex()");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideVariablesForException")
    void testIndexOfExceptionPropagation(Variable variableMock, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> model.indexOf(variableMock), "indexOf should propagate the expected exception");
    }
}
