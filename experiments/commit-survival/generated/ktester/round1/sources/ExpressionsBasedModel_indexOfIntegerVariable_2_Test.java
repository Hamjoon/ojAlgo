package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_2_Test {

    private ExpressionsBasedModel model;

    private Variable intVar1;

    private Variable intVar2;

    private Variable contVar;

    @Nested
    @DisplayName("Mixed integer / non‑integer variable ordering")
    class MixedIntegerNonIntegerTests {

        private ExpressionsBasedModel mixedModel;

        @BeforeEach
        void setUpMixedModel() {
            mixedModel = new ExpressionsBasedModel();
            // Two continuous variables first (indices 0,1)
            mixedModel.addVariable(Variable.make("c0").lower(0).upper(5));
            mixedModel.addVariable(Variable.make("c1").lower(0).upper(5));
            // Followed by two integer variables (indices 2,3)
            mixedModel.addVariable(Variable.make("i0").integer(true).lower(0).upper(5));
            mixedModel.addVariable(Variable.make("i1").integer(true).lower(0).upper(5));
        }

        @Test
        @DisplayName("First integer variable after non‑integer variables should have position 0")
        void testFirstIntegerAfterNonIntegerReturnsZero() {
            // Global index 2 corresponds to the first integer variable (i0)
            int pos = mixedModel.indexOfIntegerVariable(new IntIndex(2));
            assertEquals(0, pos, "The first integer variable (global index 2) should map to integer position 0");
        }

        @Test
        @DisplayName("Second integer variable after non‑integer variables should have position 1")
        void testSecondIntegerAfterNonIntegerReturnsOne() {
            // Global index 3 corresponds to the second integer variable (i1)
            int pos = mixedModel.indexOfIntegerVariable(new IntIndex(3));
            assertEquals(1, pos, "The second integer variable (global index 3) should map to integer position 1");
        }

        @Test
        @DisplayName("Non‑integer variable that appears after integer variables should return -1")
        void testNonIntegerAfterIntegerReturnsMinusOne() {
            // Add another continuous variable after the integer ones (global index 4)
            mixedModel.addVariable(Variable.make("c2").lower(0).upper(5));
            int pos = mixedModel.indexOfIntegerVariable(new IntIndex(4));
            assertEquals(-1, pos, "A non‑integer variable (global index 4) should not be found among integer variables");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // Integer variables – they will be part of the integer‑variable list.
        intVar1 = model.addVariable(Variable.make("intVar1").integer(true).lower(0).upper(10));
        intVar2 = model.addVariable(Variable.make("intVar2").integer(true).lower(0).upper(10));
        // A continuous (non‑integer) variable.
        contVar = model.addVariable(Variable.make("contVar").lower(0).upper(10));
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources; simply discard the reference.
        model = null;
    }

    @Test
    void testIndexOfFirstIntegerVariable() {
        // global position in the model
        int globalIdx = model.indexOf(intVar1);
        // wrap as IntIndex
        IntIndex intIdx = new IntIndex(globalIdx);
        int integerPos = model.indexOfIntegerVariable(intIdx);
        assertEquals(0, integerPos, "The first integer variable should have integer position 0");
    }

    @Test
    void testIndexOfSecondIntegerVariable() {
        int globalIdx = model.indexOf(intVar2);
        IntIndex intIdx = new IntIndex(globalIdx);
        int integerPos = model.indexOfIntegerVariable(intIdx);
        assertEquals(1, integerPos, "The second integer variable should have integer position 1");
    }

    @Test
    void testIndexOfNonIntegerVariableReturnsMinusOne() {
        int globalIdx = model.indexOf(contVar);
        IntIndex intIdx = new IntIndex(globalIdx);
        int integerPos = model.indexOfIntegerVariable(intIdx);
        assertEquals(-1, integerPos, "A non‑integer variable should not be found in the integer variable list");
    }

    @Test
    @DisplayName("Null IntIndex should cause NullPointerException")
    void testNullIntIndexThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable((IntIndex) null), "Calling indexOfIntegerVariable with a null IntIndex should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index} => idx={0}")
    @ValueSource(ints = { 5, -1 })
    @DisplayName("Out‑of‑range IntIndex should cause IndexOutOfBoundsException")
    void testIndexOutOfRangeThrows(int illegalIdx) {
        // The model created in @BeforeEach contains exactly three variables (indices 0‑2).
        IntIndex intIdx = new IntIndex(illegalIdx);
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(intIdx), "Index " + illegalIdx + " is out of bounds and should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Model without any integer variables should return -1 for any valid index")
    void testNoIntegerVariablesReturnsMinusOne() {
        // Build a fresh model that only contains continuous variables.
        ExpressionsBasedModel onlyContinuous = new ExpressionsBasedModel();
        Variable c1 = onlyContinuous.addVariable(Variable.make("c1").lower(0).upper(5));
        Variable c2 = onlyContinuous.addVariable(Variable.make("c2").lower(0).upper(5));
        // Test the first variable (global index 0)
        int pos0 = onlyContinuous.indexOfIntegerVariable(new IntIndex(0));
        assertEquals(-1, pos0, "With no integer variables, index 0 should map to -1");
        // Test the second variable (global index 1)
        int pos1 = onlyContinuous.indexOfIntegerVariable(new IntIndex(1));
        assertEquals(-1, pos1, "With no integer variables, index 1 should map to -1");
    }
}
