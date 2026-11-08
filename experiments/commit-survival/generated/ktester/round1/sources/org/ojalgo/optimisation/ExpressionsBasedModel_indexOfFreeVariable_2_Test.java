package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.ProgrammingError;
import java.math.BigDecimal;
import org.ojalgo.optimisation.Variable;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit disposal,
        // but we call dispose() to be tidy.
        if (model != null) {
            model.dispose();
        }
    }

    /* -------------------------------------------------------------
     * Helper to create a simple IntIndex implementation.
     * ------------------------------------------------------------- */
    private static IntIndex intIndex(final int value) {
        return new IntIndex() {

            @Override
            public int index() {
                return value;
            }
        };
    }

    @Test
    void testIndexOfFreeVariable_AllFree() {
        // three free variables
        // index 0
        Variable v0 = model.addVariable();
        // index 1
        Variable v1 = model.addVariable();
        // index 2
        Variable v2 = model.addVariable();
        // verify the mapping from global index to free‑variable index
        assertEquals(0, model.indexOfFreeVariable(IntIndex.of(0)));
        assertEquals(1, model.indexOfFreeVariable(IntIndex.of(1)));
        assertEquals(2, model.indexOfFreeVariable(IntIndex.of(2)));
        // also check the overload that accepts an int directly
        assertEquals(0, model.indexOfFreeVariable(0));
        assertEquals(1, model.indexOfFreeVariable(1));
        assertEquals(2, model.indexOfFreeVariable(2));
    }

    @Test
    void testIndexOfFreeVariable_WithFixedVariable() {
        // first variable will be fixed (lower == upper)
        // index 0, fixed
        Variable fixed = model.addVariable().level(5);
        // the remaining variables stay free
        // index 1
        Variable free1 = model.addVariable();
        // index 2
        Variable free2 = model.addVariable();
        // The fixed variable is not part of the free‑variable list → -1
        assertEquals(-1, model.indexOfFreeVariable(IntIndex.of(0)));
        assertEquals(-1, model.indexOfFreeVariable(0));
        // The free variables are re‑indexed starting at 0
        assertEquals(0, model.indexOfFreeVariable(IntIndex.of(1)));
        assertEquals(1, model.indexOfFreeVariable(IntIndex.of(2)));
        assertEquals(0, model.indexOfFreeVariable(1));
        assertEquals(1, model.indexOfFreeVariable(2));
    }

    @Test
    void testIndexOfFreeVariable_NoVariables() {
        // an empty model – any request should return -1
        assertEquals(-1, model.indexOfFreeVariable(IntIndex.of(0)));
        assertEquals(-1, model.indexOfFreeVariable(0));
    }

    @Test
    void testValidIntIndexFreeVariable() {
        // One free variable at global index 0
        // index 0
        model.addVariable();
        // Expected free‑variable position is 0
        assertEquals(0, model.indexOfFreeVariable(intIndex(0)), "Free variable at global index 0 should map to free index 0");
    }

    @Test
    void testValidIntIndexNonFreeVariable() {
        // Create a fixed variable at index 5 (outside current variable count)
        // First add 6 variables, then fix the one at index 5
        for (int i = 0; i < 6; i++) {
            model.addVariable();
        }
        // fixed
        model.getVariable(5).level(BigDecimal.valueOf(10));
        // The method should return -1 for a fixed variable
        assertEquals(-1, model.indexOfFreeVariable(intIndex(5)), "Fixed variable should not be considered free");
    }

    @Test
    void testNullIntIndex() {
        assertThrows(NullPointerException.class, () -> model.indexOfFreeVariable(null), "Calling with null IntIndex must throw NullPointerException");
    }

    @Test
    void testNegativeIntIndex() {
        // Negative global index is treated as non‑existent → -1
        assertEquals(-1, model.indexOfFreeVariable(intIndex(-1)), "Negative IntIndex should return -1");
    }

    @Test
    void testOutOfBoundsIntIndex() {
        // Model with three variables (indices 0‑2)
        model.addVariable();
        model.addVariable();
        model.addVariable();
        // Request index 10, which is out of bounds
        assertEquals(-1, model.indexOfFreeVariable(intIndex(10)), "Out‑of‑bounds IntIndex should return -1");
    }

    @Test
    void testFreeVariableMappingWithMixedTypes() {
        // index 0 – free
        // v0
        model.addVariable();
        // index 1 – fixed
        // v1 fixed
        model.addVariable().level(BigDecimal.valueOf(5));
        // index 2 – free
        // v2
        model.addVariable();
        // Global index 2 is the second free variable → free index 1
        assertEquals(1, model.indexOfFreeVariable(intIndex(2)), "Global index 2 should map to free index 1 in mixed model");
    }

    @Test
    void testIntegerVariableNotConsideredFree() {
        // Need at least 4 variables; make the fourth an integer variable
        for (int i = 0; i < 4; i++) {
            model.addVariable();
        }
        // Mark variable at index 3 as integer
        model.getVariable(3).integer(true);
        // Integer variables are not part of the free list
        assertEquals(-1, model.indexOfFreeVariable(intIndex(3)), "Integer variable should not be considered free");
    }

    @Test
    void testFixedVariableNotConsideredFree() {
        // Need at least 5 variables; fix the fifth
        for (int i = 0; i < 5; i++) {
            model.addVariable();
        }
        // fixed at index 4
        model.getVariable(4).level(BigDecimal.valueOf(7));
        assertEquals(-1, model.indexOfFreeVariable(intIndex(4)), "Fixed variable should not be considered free");
    }

    @Test
    void testEmptyModel() {
        // No variables added – any index should yield -1
        assertEquals(-1, model.indexOfFreeVariable(intIndex(0)), "Empty model must return -1 for any IntIndex");
    }

    @Test
    void testIntIndexThrowsException() {
        IntIndex explodingIndex = new IntIndex() {

            @Override
            public int index() {
                throw new IllegalStateException("index failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.indexOfFreeVariable(explodingIndex), "Exception from IntIndex.index() must be propagated");
        assertEquals("index failure", ex.getMessage());
    }

    @Test
    void testDisposedModelThrowsProgrammingError() {
        // Dispose the model first
        model.dispose();
        // Any subsequent call should raise ProgrammingError
        ProgrammingError error = assertThrows(ProgrammingError.class, () -> model.indexOfFreeVariable(intIndex(0)), "Calling after dispose must raise ProgrammingError");
        // Optional: verify the message contains a hint about disposal
        assertTrue(error.getMessage().toLowerCase().contains("disposed"), "Error message should mention that the model is disposed");
    }
}