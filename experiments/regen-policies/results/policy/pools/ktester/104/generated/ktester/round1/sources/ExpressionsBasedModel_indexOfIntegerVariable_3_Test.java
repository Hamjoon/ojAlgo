package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_3_Test {

    private ExpressionsBasedModel model;

    private Variable integerVar;

    private Variable continuousVar;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Integer variable – the flag is set via the fluent API.
        integerVar = model.newVariable("intVar").integer(true).lower(0).upper(10).weight(1);
        // Continuous (non‑integer) variable
        continuousVar = model.newVariable("contVar").lower(0).upper(10).weight(1);
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
        integerVar = null;
        continuousVar = null;
    }

    @Test
    void testIndexOfIntegerVariable_ForIntegerVariable() {
        // The integer variable should be present in the integer‑variable list.
        int expected = model.indexOfIntegerVariable(integerVar.getIndex().index);
        int actual = model.indexOfIntegerVariable(integerVar);
        assertEquals(expected, actual, "indexOfIntegerVariable(Variable) should forward to the int‑based overload");
    }

    @Test
    void testIndexOfIntegerVariable_ForNonIntegerVariable() {
        // For a non‑integer variable the integer‑variable list does not contain it.
        // The contract of the method is to return the same value as the int‑based overload,
        // which is typically –1 for “not found”.
        int expected = model.indexOfIntegerVariable(continuousVar.getIndex().index);
        int actual = model.indexOfIntegerVariable(continuousVar);
        assertEquals(expected, actual, "Non‑integer variables must yield the same result as the int‑based overload");
    }

    /* -----------------------------------------------------------
     *  Edge cases – null, missing index, negative index
     * ----------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_NullVariable() {
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable((Variable) null), "Calling with null should throw NullPointerException");
    }

    @Test
    void testIndexOfIntegerVariable_VariableWithNullIndex() {
        // Create a real Variable and then force its index field to null via reflection
        Variable var = model.newVariable("temp").lower(0).upper(1).weight(1);
        // The getIndex() method returns a Structure1D.IntIndex; we replace the internal
        // reference with null by accessing the private field in Variable that holds the index.
        Field indexField = Variable.class.getDeclaredField("myIndex");
        indexField.setAccessible(true);
        indexField.set(var, null);
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable(var), "Variable with null IntIndex should cause NullPointerException");
    }

    @Test
    void testIndexOfIntegerVariable_VariableNotAddedToModel() {
        // Create a Variable that is not added to the model (stand‑alone)
        Variable stray = new Variable("stray", false);
        // Force a high global index via reflection
        Structure1D.IntIndex intIdx = stray.getIndex();
        Field f = intIdx.getClass().getSuperclass().getDeclaredField("index");
        f.setAccessible(true);
        f.setInt(intIdx, 999);
        // The model should treat it as absent and return -1
        int actual = model.indexOfIntegerVariable(stray);
        assertEquals(-1, actual, "Variable not added to model should return -1");
    }

    @Test
    void testIndexOfIntegerVariable_NegativeGlobalIndex() {
        // Create a Variable and set a negative global index via reflection
        Variable var = model.newVariable("negIdx").lower(0).upper(1).weight(1);
        Structure1D.IntIndex intIdx = var.getIndex();
        Field f = intIdx.getClass().getSuperclass().getDeclaredField("index");
        f.setAccessible(true);
        f.setInt(intIdx, -1);
        int actual = model.indexOfIntegerVariable(var);
        assertEquals(-1, actual, "Negative global index should be treated as absent and return -1");
    }

    /* -----------------------------------------------------------
     *  Multiple integer variables – counting only integer ones
     * ----------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_MultipleIntegerVariables() {
        // Add a second integer variable
        Variable secondInt = model.newVariable("intVar2").integer(true).lower(0).upper(5).weight(1);
        // The first integer variable should be at position 0, second at 1
        int idxFirst = model.indexOfIntegerVariable(integerVar);
        int idxSecond = model.indexOfIntegerVariable(secondInt);
        assertEquals(0, idxFirst, "First integer variable should have integer index 0");
        assertEquals(1, idxSecond, "Second integer variable should have integer index 1");
    }

    @Test
    void testIndexOfIntegerVariable_MixedVariableTypes() {
        // Reset model for a clear ordering
        model = new ExpressionsBasedModel();
        // non‑integer variable (global index 0)
        Variable nonInt = model.newVariable("cont0").lower(0).upper(10).weight(1);
        // first integer variable (global index 1)
        Variable int1 = model.newVariable("int1").integer(true).lower(0).upper(5).weight(1);
        // second integer variable (global index 2)
        Variable int2 = model.newVariable("int2").integer(true).lower(0).upper(5).weight(1);
        // int1 is the first integer variable -> index 0, int2 is second -> index 1
        assertEquals(0, model.indexOfIntegerVariable(int1), "First integer variable (global index 1) should map to integer index 0");
        assertEquals(1, model.indexOfIntegerVariable(int2), "Second integer variable (global index 2) should map to integer index 1");
    }

    /* -----------------------------------------------------------
     *  Exception scenarios – out‑of‑range, extreme values
     * ----------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_IndexOutOfRange() {
        // Mock a Variable whose global index equals the current variable count (out of range)
        Variable mockVar = Mockito.mock(Variable.class);
        Structure1D.IntIndex intIdx = Mockito.mock(Structure1D.IntIndex.class);
        // equal to size -> invalid
        int outOfRange = model.countVariables();
        try {
            Field f = intIdx.getClass().getSuperclass().getDeclaredField("index");
            f.setAccessible(true);
            f.setInt(intIdx, outOfRange);
        } catch (Exception e) {
            fail("Reflection error while setting out‑of‑range index", e);
        }
        Mockito.when(mockVar.getIndex()).thenReturn(intIdx);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(mockVar), "Variable with out‑of‑range index should cause IndexOutOfBoundsException");
    }

    @Test
    void testIndexOfIntegerVariable_ExtremeNegativeIndex() {
        // Mock a Variable with Integer.MIN_VALUE as its global index
        Variable mockVar = Mockito.mock(Variable.class);
        Structure1D.IntIndex intIdx = Mockito.mock(Structure1D.IntIndex.class);
        try {
            Field f = intIdx.getClass().getSuperclass().getDeclaredField("index");
            f.setAccessible(true);
            f.setInt(intIdx, Integer.MIN_VALUE);
        } catch (Exception e) {
            fail("Reflection error while setting Integer.MIN_VALUE index", e);
        }
        Mockito.when(mockVar.getIndex()).thenReturn(intIdx);
        assertThrows(IllegalArgumentException.class, () -> model.indexOfIntegerVariable(mockVar), "Extreme negative index should cause IllegalArgumentException");
    }
}
