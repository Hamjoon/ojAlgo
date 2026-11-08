package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariable_2_Test {

    private ExpressionsBasedModel model;

    private Variable var0;

    private Variable var1;

    private Variable var2;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test and add a few variables.
        model = new ExpressionsBasedModel();
        // index 0
        var0 = model.addVariable();
        // index 1
        var1 = model.addVariable();
        // index 2
        var2 = model.addVariable();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly required but keeps the pattern tidy.
        model = null;
        var0 = var1 = var2 = null;
    }

    private static Stream<Arguments> validIndexProvider() {
        // The model is created in @BeforeEach, therefore we cannot reference the instance variables directly.
        // Instead we create a temporary model here solely to obtain the expected Variable objects.
        ExpressionsBasedModel tmp = new ExpressionsBasedModel();
        // index 0
        Variable v0 = tmp.addVariable();
        // index 1
        Variable v1 = tmp.addVariable();
        // index 2
        Variable v2 = tmp.addVariable();
        return Stream.of(Arguments.of(new IntIndex(0), v0), Arguments.of(new IntIndex(1), v1), Arguments.of(new IntIndex(2), v2));
    }

    private static Stream<IntIndex> outOfBoundsProvider() {
        return Stream.of(// negative index
        new IntIndex(-1), // just beyond the last valid index (0‑2)
        new IntIndex(3), // far beyond the size
        new IntIndex(10));
    }

    @Test
    void testGetVariableReturnsCorrectInstance() {
        // GIVEN – a model with three variables (already prepared in @BeforeEach)
        // WHEN – retrieving each variable via its IntIndex
        Variable retrieved0 = model.getVariable(new IntIndex(0));
        Variable retrieved1 = model.getVariable(new IntIndex(1));
        Variable retrieved2 = model.getVariable(new IntIndex(2));
        // THEN – the returned objects are the exact same instances that were added
        assertSame(var0, retrieved0, "Variable at index 0 should be the same instance added earlier");
        assertSame(var1, retrieved1, "Variable at index 1 should be the same instance added earlier");
        assertSame(var2, retrieved2, "Variable at index 2 should be the same instance added earlier");
    }

    @Test
    void testGetVariableWithNegativeIndexThrowsException() {
        // GIVEN – a negative index
        IntIndex negative = new IntIndex(-1);
        // WHEN / THEN – an IndexOutOfBoundsException is expected
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(negative), "Calling getVariable with a negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableWithTooLargeIndexThrowsException() {
        // GIVEN – an index larger than the current number of variables (we only have 3 variables 0‑2)
        IntIndex tooLarge = new IntIndex(10);
        // WHEN / THEN – an IndexOutOfBoundsException is expected
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(tooLarge), "Calling getVariable with an out‑of‑range index should throw IndexOutOfBoundsException");
    }

    /* -------------------------------------------------
     *  Out‑of‑bounds indices – parameterised
     * ------------------------------------------------- */
    @ParameterizedTest(name = "{index} => index={0}")
    @MethodSource("outOfBoundsProvider")
    void testGetVariableWithOutOfBoundsIndexThrowsException(IntIndex index) {
        // THEN
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(index), () -> "Calling getVariable with index " + index.index + " should throw IndexOutOfBoundsException");
    }

    /* -------------------------------------------------
     *  Null argument handling
     * ------------------------------------------------- */
    @Test
    void testGetVariableWithNullIndexThrowsException() {
        // THEN
        assertThrows(NullPointerException.class, () -> model.getVariable(null), "Passing a null IntIndex should result in NullPointerException");
    }

    /* -------------------------------------------------
     *  Legacy explicit tests (kept for readability)
     * ------------------------------------------------- */
    @Test
    void testGetVariableReturnsCorrectInstance_legacy() {
        Variable retrieved0 = model.getVariable(new IntIndex(0));
        Variable retrieved1 = model.getVariable(new IntIndex(1));
        Variable retrieved2 = model.getVariable(new IntIndex(2));
        assertSame(var0, retrieved0, "Variable at index 0 should be the same instance added earlier");
        assertSame(var1, retrieved1, "Variable at index 1 should be the same instance added earlier");
        assertSame(var2, retrieved2, "Variable at index 2 should be the same instance added earlier");
    }

    @Test
    void testGetVariableWithNegativeIndexThrowsException_legacy() {
        IntIndex negative = new IntIndex(-1);
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(negative), "Calling getVariable with a negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testGetVariableWithTooLargeIndexThrowsException_legacy() {
        IntIndex tooLarge = new IntIndex(10);
        assertThrows(IndexOutOfBoundsException.class, () -> model.getVariable(tooLarge), "Calling getVariable with an out‑of‑range index should throw IndexOutOfBoundsException");
    }
}