package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.IntStream;
import org.ojalgo.ProgrammingError;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_2_Test {

    private ExpressionsBasedModel model;

    private Variable intVar0;

    private Variable intVar1;

    private Variable contVar2;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // first integer variable
        intVar0 = // mark as integer
        model.addVariable().// mark as integer
        integer(true).lower(// bounds are not relevant for the index test
        0).// bounds are not relevant for the index test
        upper(10);
        // second integer variable
        intVar1 = model.addVariable().integer(true).lower(0).upper(10);
        // a continuous (non‑integer) variable
        contVar2 = model.addVariable().lower(0).upper(10);
    }

    @Test
    void testIndexOfIntegerVariable_WithVariableArgument() {
        // The first integer variable should have index 0 among integer variables
        int idx0 = model.indexOfIntegerVariable(intVar0);
        assertEquals(0, idx0, "First integer variable should be at index 0");
        // The second integer variable should have index 1
        int idx1 = model.indexOfIntegerVariable(intVar1);
        assertEquals(1, idx1, "Second integer variable should be at index 1");
        // A non‑integer variable must return -1
        int idx2 = model.indexOfIntegerVariable(contVar2);
        assertEquals(-1, idx2, "Continuous variable should not be found among integer variables");
    }

    @Test
    void testIndexOfIntegerVariable_WithIntIndexArgument() {
        // Global index of the first integer variable
        int globalIdx0 = model.indexOf(intVar0);
        IntIndex intIndex0 = IntIndex.of(globalIdx0);
        assertEquals(0, model.indexOfIntegerVariable(intIndex0), "IntIndex wrapper for first integer variable should yield index 0");
        // Global index of the second integer variable
        int globalIdx1 = model.indexOf(intVar1);
        IntIndex intIndex1 = IntIndex.of(globalIdx1);
        assertEquals(1, model.indexOfIntegerVariable(intIndex1), "IntIndex wrapper for second integer variable should yield index 1");
        // Global index of the continuous variable – should return -1
        int globalIdx2 = model.indexOf(contVar2);
        IntIndex intIndex2 = IntIndex.of(globalIdx2);
        assertEquals(-1, model.indexOfIntegerVariable(intIndex2), "IntIndex wrapper for continuous variable should yield -1");
    }

    /* --------------------------------------------------------------
     *  Delegation & edge‑case tests for the IntIndex overload
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_NullIntIndex() {
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable((IntIndex) null), "Passing null IntIndex must throw NullPointerException");
    }

    @Test
    void testIndexOfIntegerVariable_NegativeIntIndex() {
        // toIntIndex validates the supplied index; a negative index should cause an exception
        assertThrows(RuntimeException.class, () -> model.indexOfIntegerVariable(IntIndex.of(-1)), "Negative IntIndex should result in an exception (IndexOutOfBounds/IllegalArgument)");
    }

    @Test
    void testIndexOfIntegerVariable_OutOfRangeIntIndex() {
        // Model currently has three variables (indices 0‑2). Index 10 is out of range.
        assertThrows(RuntimeException.class, () -> model.indexOfIntegerVariable(IntIndex.of(10)), "Out‑of‑range IntIndex should trigger an exception");
    }

    @Test
    void testIndexOfIntegerVariable_SecondIntegerVariable() {
        // Re‑use the second integer variable already added in @BeforeEach
        int idx = model.indexOfIntegerVariable(IntIndex.of(model.indexOf(intVar1)));
        assertEquals(1, idx, "Second integer variable should map to index 1 among integer variables");
    }

    @Test
    void testIndexOfIntegerVariable_IntegerAfterNonInteger() {
        // Create a model where the first variable is continuous and the second is integer
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // index 0, non‑integer
        Variable cont = m.addVariable();
        // index 1, integer
        Variable integer = m.addVariable().integer(true);
        int result = m.indexOfIntegerVariable(IntIndex.of(m.indexOf(integer)));
        assertEquals(0, result, "Integer variable after a non‑integer should be at position 0 in the integer list");
    }

    /* --------------------------------------------------------------
     *  Empty model case
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_EmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        // No variables have been added – any index is invalid
        assertThrows(RuntimeException.class, () -> empty.indexOfIntegerVariable(IntIndex.of(0)), "Calling on an empty model must throw an IndexOutOfBounds‑like exception");
    }

    /* --------------------------------------------------------------
     *  After relaxation – integer flag cleared
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_AfterRelaxation() {
        // Variable is integer before relaxation
        int globalIdx = model.indexOf(intVar0);
        IntIndex idx = IntIndex.of(globalIdx);
        assertEquals(0, model.indexOfIntegerVariable(idx), "Pre‑relaxation should return 0");
        // Relax the model – this clears the integer flag of all variables
        model.relax();
        // After relaxation the same variable is no longer considered integer
        assertEquals(-1, model.indexOfIntegerVariable(idx), "After relaxation the variable should no longer be recognised as integer");
    }

    /* --------------------------------------------------------------
     *  IntIndex originating from a different model
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_IntIndexFromAnotherModel() {
        ExpressionsBasedModel other = new ExpressionsBasedModel();
        other.addVariable().integer(true);
        // IntIndex tied to 'other'
        IntIndex foreignIdx = other.toIntIndex(0);
        assertThrows(IllegalArgumentException.class, () -> model.indexOfIntegerVariable(foreignIdx), "IntIndex from another model must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Fixed integer variable – still part of the integer list
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_FixedIntegerVariable() {
        Variable fixedInt = // also fixed
        model.addVariable().integer(true).// also fixed
        fix(BigDecimal.ONE);
        int globalIdx = model.indexOf(fixedInt);
        IntIndex idx = IntIndex.of(globalIdx);
        // It is the third integer variable (indices 0 and 1 are intVar0/intVar1)
        assertEquals(2, model.indexOfIntegerVariable(idx), "Fixed integer variable should still be counted among integer variables");
    }

    /* --------------------------------------------------------------
     *  Sparse integer variables among many non‑integers
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_SparseIntegers() {
        ExpressionsBasedModel sparse = new ExpressionsBasedModel();
        // create 20 variables, only 2,7,15 are integer
        IntStream.range(0, 20).forEach(i -> {
            Variable v = sparse.addVariable();
            if (i == 2 || i == 7 || i == 15) {
                v.integer(true);
            }
        });
        // helper to get integer index position
        int pos2 = sparse.indexOfIntegerVariable(IntIndex.of(2));
        int pos7 = sparse.indexOfIntegerVariable(IntIndex.of(7));
        int pos15 = sparse.indexOfIntegerVariable(IntIndex.of(15));
        assertEquals(0, pos2, "First integer (global 2) should be at position 0");
        assertEquals(1, pos7, "Second integer (global 7) should be at position 1");
        assertEquals(2, pos15, "Third integer (global 15) should be at position 2");
    }

    /* --------------------------------------------------------------
     *  After model disposal – illegal state
     * -------------------------------------------------------------- */
    @Test
    void testIndexOfIntegerVariable_AfterDisposal() {
        // Obtain an IntIndex before disposing
        IntIndex idx = model.toIntIndex(model.indexOf(intVar0));
        // Dispose the model
        model.dispose();
        // Any further call should fail – ojAlgo throws ProgrammingError (a RuntimeException)
        assertThrows(ProgrammingError.class, () -> model.indexOfIntegerVariable(idx), "Calling after model.dispose() must raise a ProgrammingError/IllegalStateException");
    }
}
