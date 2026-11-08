package org.ojalgo.optimisation;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;

class ExpressionsBasedModel_getIntegerVariables_Test {

    @Test
    void testGetIntegerVariablesReturnsCorrectList() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Create variables: two integer, one non‑integer
        Variable intVar1 = model.addVariable().integer(true);
        // default: not integer
        Variable nonIntVar = model.addVariable();
        Variable intVar2 = model.addVariable().integer(true);
        List<Variable> integerVariables = model.getIntegerVariables();
        // Verify the list contains exactly the two integer variables
        assertEquals(2, integerVariables.size(), "Expected exactly two integer variables in the returned list");
        assertTrue(integerVariables.contains(intVar1), "The first integer variable should be present");
        assertTrue(integerVariables.contains(intVar2), "The second integer variable should be present");
        assertFalse(integerVariables.contains(nonIntVar), "A non‑integer variable must not be present");
    }

    @Test
    void testGetIntegerVariablesIsUnmodifiable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable intVar = model.addVariable().integer(true);
        List<Variable> integerVariables = model.getIntegerVariables();
        // The list returned by the API must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> integerVariables.add(intVar), "The list returned by getIntegerVariables() should be unmodifiable");
    }

    @Test
    void testGetIntegerVariablesReflectsModelChanges() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable intVar1 = model.addVariable().integer(true);
        Variable intVar2 = model.addVariable().integer(true);
        // First snapshot
        List<Variable> firstSnapshot = model.getIntegerVariables();
        assertEquals(2, firstSnapshot.size(), "Initially the model should contain two integer variables");
        // Add another integer variable after the first call
        Variable intVar3 = model.addVariable().integer(true);
        // Second snapshot must reflect the new variable
        List<Variable> secondSnapshot = model.getIntegerVariables();
        assertEquals(3, secondSnapshot.size(), "After adding another integer variable the list size must increase");
        assertTrue(secondSnapshot.contains(intVar3), "The newly added integer variable must be present in the list");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testInitialCallIndicesNull() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // one integer variable, one non‑integer variable
        Variable intVar = model.addVariable().integer(true);
        // default: not integer
        model.addVariable();
        List<Variable> integerVariables = model.getIntegerVariables();
        // The list must contain exactly the integer variable and be unmodifiable
        assertEquals(1, integerVariables.size(), "Expected exactly one integer variable in the returned list");
        assertTrue(integerVariables.contains(intVar), "The integer variable should be present");
        assertThrows(UnsupportedOperationException.class, () -> integerVariables.add(intVar), "The returned list must be unmodifiable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testSubsequentCallUsesCachedIndices() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable intVar = model.addVariable().integer(true);
        // non‑integer
        model.addVariable();
        // First call – forces an update of the internal indices
        List<Variable> firstCall = model.getIntegerVariables();
        assertEquals(1, firstCall.size());
        // Second call without any model modification
        List<Variable> secondCall = model.getIntegerVariables();
        // The content must stay identical and the list must stay unmodifiable
        assertEquals(firstCall, secondCall, "A second call without model changes must return an equal list");
        assertThrows(UnsupportedOperationException.class, () -> secondCall.add(intVar), "The returned list must stay unmodifiable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testMixedVariableTypesAndFixedVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // (1) integer and free
        Variable intFree = model.addVariable().integer(true);
        // (2) integer but fixed (set a level/value)
        Variable intFixed = model.addVariable().integer(true).level(BigDecimal.ONE);
        // (3) non‑integer free
        Variable nonIntFree = model.addVariable();
        List<Variable> integerVariables = model.getIntegerVariables();
        // Only the free integer variable must be present
        assertEquals(1, integerVariables.size(), "Only free integer variables should be returned");
        assertTrue(integerVariables.contains(intFree), "Free integer variable must be present");
        assertFalse(integerVariables.contains(intFixed), "Fixed integer variable must be excluded");
        assertFalse(integerVariables.contains(nonIntFree), "Non‑integer variable must be excluded");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testEmptyModelReturnsEmptyUnmodifiableList() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        List<Variable> integerVariables = model.getIntegerVariables();
        assertTrue(integerVariables.isEmpty(), "An empty model should yield an empty list");
        assertThrows(UnsupportedOperationException.class, () -> integerVariables.add(null), "The empty list must still be unmodifiable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNullVariableInModelThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Inject a null entry directly into the private myVariables list via reflection
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) varsField.get(model);
        // now the internal list contains a null element
        vars.add(null);
        // The next call must try to invoke isFixed() on the null entry and throw NPE
        assertThrows(NullPointerException.class, model::getIntegerVariables, "A null variable in the internal list should cause NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testReturnedListIsUnmodifiable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable intVar = model.addVariable().integer(true);
        List<Variable> integerVariables = model.getIntegerVariables();
        assertThrows(UnsupportedOperationException.class, () -> integerVariables.add(intVar), "Attempting to modify the returned list must throw UnsupportedOperationException");
    }
}
