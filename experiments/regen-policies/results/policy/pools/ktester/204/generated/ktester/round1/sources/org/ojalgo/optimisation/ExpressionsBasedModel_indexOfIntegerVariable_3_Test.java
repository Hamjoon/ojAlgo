package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_3_Test {

    private ExpressionsBasedModel model;

    private Variable intVar1;

    private Variable intVar2;

    private Variable contVar;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Integer variables – the `integer(boolean)` method is part of the public API
        intVar1 = model.newVariable("intVar1").integer(true);
        intVar2 = model.newVariable("intVar2").integer(true);
        // A continuous (non‑integer) variable
        contVar = model.newVariable("contVar");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required
        model = null;
        intVar1 = null;
        intVar2 = null;
        contVar = null;
    }

    @Test
    void testIndexOfIntegerVariableReturnsCorrectIndices() {
        // The first integer variable added should have index 0
        int idx1 = model.indexOfIntegerVariable(intVar1);
        assertEquals(0, idx1, "First integer variable should have index 0");
        // The second integer variable should have index 1
        int idx2 = model.indexOfIntegerVariable(intVar2);
        assertEquals(1, idx2, "Second integer variable should have index 1");
        // A non‑integer variable should return -1 (not found among integer variables)
        int idxCont = model.indexOfIntegerVariable(contVar);
        assertEquals(-1, idxCont, "Non‑integer variable should return -1");
    }

    @Test
    void testIndexOfIntegerVariableWithVariableNotInModel() {
        // Create a variable that does NOT belong to the current model
        Variable external = new Variable("external", 999).integer(true);
        // The method should return -1 because the variable is unknown to the model
        int idx = model.indexOfIntegerVariable(external);
        assertEquals(-1, idx, "Variable not part of the model should return -1");
    }

    @Test
    void testValidIntegerVariableAtPositionOne() {
        // Add a third integer variable – its global index will be 2,
        // but it should be the second integer variable (position 1)
        Variable intVar3 = model.newVariable("intVar3").integer(true);
        int idx = model.indexOfIntegerVariable(intVar3);
        assertEquals(2, intVar3.getIndex().index, "Global index of third integer variable should be 2");
        assertEquals(2, idx, "Method should return the global index, not the position in integer list");
        // The integer‑list position can be obtained via the overloaded method,
        // but here we verify that the public method delegates correctly.
        int idxViaList = model.indexOfIntegerVariable(intVar3.getIndex().index);
        assertEquals(2, idxViaList, "Delegate method should also return 2");
    }

    @Test
    void testNonIntegerVariableReturnsMinusOne() {
        int idx = model.indexOfIntegerVariable(contVar);
        assertEquals(-1, idx, "Non‑integer variable must yield -1");
    }

    @Test
    void testVariableWithNegativeIndexReturnsMinusOne() {
        // Create a variable with a negative internal index
        Variable negativeIdxVar = new Variable("neg", -1).integer(true);
        int result = model.indexOfIntegerVariable(negativeIdxVar);
        assertEquals(-1, result, "Variable with negative internal index should yield -1");
    }

    @Test
    void testVariableWithOutOfRangeIndexReturnsMinusOne() {
        // Create a variable whose internal index is far beyond the current number of variables
        Variable outOfRangeVar = new Variable("outOfRange", 1_000).integer(true);
        int result = model.indexOfIntegerVariable(outOfRangeVar);
        assertEquals(-1, result, "Variable with out‑of‑range index should yield -1");
    }

    @Test
    void testNullVariableArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable(null), "Passing null should throw NullPointerException");
    }

    @Test
    void testVariableWithNullInternalIndexThrowsNPE() {
        // Create a subclass that returns null for getIndex()
        Variable varWithNullIndex = new Variable("nullIdx", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable(varWithNullIndex), "Variable whose getIndex() returns null should throw NullPointerException");
    }
}