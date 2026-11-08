package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isInteger_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// single integer variable → true
        Arguments.of(Set.of(IntIndex.of(0)), List.of(true), true), // single non‑integer variable → false
        Arguments.of(Set.of(IntIndex.of(0)), List.of(false), false), // mixed variables → false
        Arguments.of(Set.of(IntIndex.of(0), IntIndex.of(1)), List.of(true, false), false), // all integer variables (multiple) → true
        Arguments.of(Set.of(IntIndex.of(0), IntIndex.of(1), IntIndex.of(2)), List.of(true, true, true), true), // all non‑integer variables → false
        Arguments.of(Set.of(IntIndex.of(0), IntIndex.of(1), IntIndex.of(2)), List.of(false, false, false), false));
    }

    /**
     * When the set of indices is empty the method must return {@code false}
     * regardless of the variables present in the model.
     */
    @Test
    void testIsIntegerEmptySet() {
        // add a normal (non‑integer) variable – it should not affect the result
        model.addVariable();
        Set<IntIndex> empty = Collections.emptySet();
        assertFalse(model.isInteger(empty), "Empty set should return false");
    }

    /**
     * The method should return {@code true} only when every variable referenced by the
     * supplied {@link IntIndex} objects is an integer variable.
     */
    @Test
    void testIsIntegerMixedVariables() {
        // variable 0 – integer
        Variable intVar = model.addVariable();
        intVar.integer(true);
        // variable 1 – non‑integer
        Variable nonIntVar = model.addVariable();
        nonIntVar.integer(false);
        // set containing both indices – should be false
        Set<IntIndex> mixed = new HashSet<>();
        mixed.add(IntIndex.of(0));
        mixed.add(IntIndex.of(1));
        assertFalse(model.isInteger(mixed), "Mixed integer / non‑integer variables must return false");
        // set containing only the integer variable – should be true
        Set<IntIndex> onlyInt = new HashSet<>();
        onlyInt.add(IntIndex.of(0));
        assertTrue(model.isInteger(onlyInt), "All‑integer set must return true");
    }

    /**
     * When all variables referenced are integer variables the method must return {@code true}.
     */
    @Test
    void testIsIntegerAllIntegerVariables() {
        // create three integer variables
        for (int i = 0; i < 3; i++) {
            Variable v = model.addVariable();
            v.integer(true);
        }
        Set<IntIndex> allInt = new HashSet<>();
        allInt.add(IntIndex.of(0));
        allInt.add(IntIndex.of(1));
        allInt.add(IntIndex.of(2));
        assertTrue(model.isInteger(allInt), "All integer variables should yield true");
    }

    /**
     * Parameterised test covering the normal true/false outcomes for various
     * combinations of integer / non‑integer variables.
     */
    @ParameterizedTest
    @MethodSource("provideNormalCases")
    void testIsIntegerNormalBehaviour(Set<IntIndex> indices, List<Boolean> integerFlags, boolean expected) {
        // Build the model according to the supplied flags
        for (boolean flag : integerFlags) {
            Variable v = model.addVariable();
            v.integer(flag);
        }
        assertEquals(expected, model.isInteger(indices), () -> "Expected " + expected + " for indices " + indices);
    }

    @Test
    void testIsIntegerNullSet() {
        assertThrows(NullPointerException.class, () -> model.isInteger(null), "Passing null as the set should throw NullPointerException");
    }

    @Test
    void testIsIntegerSetContainsNullElement() {
        Set<IntIndex> set = new HashSet<>();
        set.add(IntIndex.of(0));
        // deliberately insert a null element
        set.add(null);
        // The method will attempt to read index.index on the null element → NPE
        assertThrows(NullPointerException.class, () -> model.isInteger(set), "Set containing a null element should cause NullPointerException");
    }

    @Test
    void testIsIntegerIndexOutOfBoundsWhenModelEmpty() {
        // Model has no variables; any non‑empty set must cause OOBE
        Set<IntIndex> set = Set.of(IntIndex.of(0));
        assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(set), "Calling with a non‑empty set on an empty model should throw IndexOutOfBoundsException");
    }

    @Test
    void testIsIntegerIndexOutOfBoundsLargeIndex() {
        // Add a single variable (index 0) then query index 5
        model.addVariable();
        Set<IntIndex> set = Set.of(IntIndex.of(5));
        assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(set), "Accessing a variable index that does not exist should throw IndexOutOfBoundsException");
    }

    @Test
    void testIsIntegerNegativeIndex() {
        // Add a variable to have at least one entry
        model.addVariable();
        Set<IntIndex> set = Set.of(IntIndex.of(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(set), "Negative index should result in IndexOutOfBoundsException");
    }

    @Test
    void testIsIntegerVariableThrowsIllegalStateException() throws Exception {
        // Create a variable at index 0 and replace it with a Mockito spy that throws
        // index 0
        Variable original = model.addVariable();
        Variable spy = Mockito.spy(original);
        Mockito.doThrow(new IllegalStateException("custom")).when(spy).isInteger();
        // Replace the entry in the private myVariables list via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) field.get(model);
        list.set(0, spy);
        Set<IntIndex> set = Set.of(IntIndex.of(0));
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.isInteger(set), "Variable throwing IllegalStateException should propagate the exception");
        assertEquals("custom", thrown.getMessage());
    }

    @Test
    void testIsIntegerAllIntegerVariablesUsingSetOfIndices() {
        // three integer variables, indices 0,2 (skip 1) – all referenced are integer
        for (int i = 0; i < 3; i++) {
            Variable v = model.addVariable();
            v.integer(true);
        }
        Set<IntIndex> indices = Set.of(IntIndex.of(0), IntIndex.of(2));
        assertTrue(model.isInteger(indices), "All referenced variables are integer → true");
    }

    @Test
    void testIsIntegerMixedVariablesEarlyReturn() {
        // variable 0 – integer, variable 1 – non‑integer, variable 2 – integer
        Variable v0 = model.addVariable();
        v0.integer(true);
        Variable v1 = model.addVariable();
        v1.integer(false);
        Variable v2 = model.addVariable();
        v2.integer(true);
        Set<IntIndex> indices = Set.of(IntIndex.of(0), IntIndex.of(1), IntIndex.of(2));
        // The method should stop at index 1 and return false
        assertFalse(model.isInteger(indices), "Method should return false as soon as a non‑integer variable is found");
    }
}
