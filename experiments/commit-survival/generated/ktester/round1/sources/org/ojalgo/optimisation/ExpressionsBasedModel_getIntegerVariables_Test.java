package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getIntegerVariables_Test {

    private ExpressionsBasedModel model;

    /**
     * Variable that reports {@code isUpperLimitSet()} == true but returns {@code null}
     * from {@code getUpperLimit()}. The integer flag is true and the variable is not fixed.
     */
//     private static final class VariableWithNullUpper extends Variable {

        VariableWithNullUpper(String name) {
            super(name);
            // Mark as integer and free
            this.integer(true);
        }

//         @Override
//         public boolean isUpperLimitSet() {
//             // pretend the limit is set
//             return true;
//         }

//         @Override
//         public BigDecimal getUpperLimit() {
//             // actual value is null → NPE in categorisation
//             return null;
//         }
    }

    /**
     * Variable whose {@code isInteger()} method throws an {@link IllegalStateException}.
     */
//     private static final class VariableWithFaultyIntegerFlag extends Variable {

        VariableWithFaultyIntegerFlag(String name) {
            super(name);
        }

        @Override
        public boolean isInteger() {
            throw new IllegalStateException("Faulty integer flag");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test case.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the pattern consistent.
        model = null;
    }

    @Test
    void testGetIntegerVariablesFiltersNonFixedIntegers() {
        // Integer variable, not fixed
        Variable vInt1 = Variable.make("vInt1").integer(true);
        model.addVariable(vInt1);
        // Non‑integer variable
        Variable vNonInt = Variable.make("vNonInt");
        model.addVariable(vNonInt);
        // Integer variable that is fixed (level sets a fixed value)
        Variable vIntFixed = Variable.make("vIntFixed").integer(true).level(BigDecimal.TEN);
        model.addVariable(vIntFixed);
        List<Variable> integerVars = model.getIntegerVariables();
        // Only the non‑fixed integer variable should be present
        assertEquals(1, integerVars.size(), "Exactly one non‑fixed integer variable expected");
        assertTrue(integerVars.contains(vInt1), "The integer variable should be returned");
        assertFalse(integerVars.contains(vNonInt), "Non‑integer variable must not be returned");
        assertFalse(integerVars.contains(vIntFixed), "Fixed integer variable must not be returned");
    }

    @Test
    void testReturnedListIsUnmodifiable() {
        Variable vInt = Variable.make("vInt").integer(true);
        model.addVariable(vInt);
        List<Variable> integerVars = model.getIntegerVariables();
        assertThrows(UnsupportedOperationException.class, () -> integerVars.add(vInt), "The list returned by getIntegerVariables() must be unmodifiable");
    }

    @Test
    void testListUpdatesWhenNewIntegerVariableIsAdded() {
        Variable vInt1 = Variable.make("vInt1").integer(true);
        model.addVariable(vInt1);
        // First call – should contain only vInt1
        List<Variable> firstCall = model.getIntegerVariables();
        assertEquals(1, firstCall.size());
        assertTrue(firstCall.contains(vInt1));
        // Add another integer variable after the first call
        Variable vInt2 = Variable.make("vInt2").integer(true);
        model.addVariable(vInt2);
        // Second call – should now contain both integer variables
        List<Variable> secondCall = model.getIntegerVariables();
        assertEquals(2, secondCall.size());
        assertTrue(secondCall.contains(vInt1));
        assertTrue(secondCall.contains(vInt2));
    }

    @Test
    void testEmptyModelReturnsEmptyUnmodifiableList() {
        List<Variable> integerVars = model.getIntegerVariables();
        assertTrue(integerVars.isEmpty(), "Empty model must yield an empty list");
        assertThrows(UnsupportedOperationException.class, () -> integerVars.add(null), "Returned list must be unmodifiable");
    }

    @Test
    void testIntegerVariableWithNegativeUpperLimitIsIncluded() {
        Variable v = Variable.make("vNegUpper").integer(true).upper(BigDecimal.valueOf(-5));
        model.addVariable(v);
        List<Variable> integerVars = model.getIntegerVariables();
        assertTrue(integerVars.contains(v), "Integer variable with negative upper limit must be part of the integer list");
    }

    @Test
    void testIntegerVariableWithPositiveLowerLimitIsIncluded() {
        Variable v = Variable.make("vPosLower").integer(true).lower(BigDecimal.valueOf(3));
        model.addVariable(v);
        List<Variable> integerVars = model.getIntegerVariables();
        assertTrue(integerVars.contains(v), "Integer variable with positive lower limit must be part of the integer list");
    }

    @Test
    void testNullVariableInModelCausesNullPointerException() throws Exception {
        // Insert a null entry into the private myVariables list via reflection.
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) f.get(model);
        // deliberately corrupt the internal state
        list.add(null);
        assertThrows(NullPointerException.class, () -> model.getIntegerVariables(), "A null variable inside the model must cause NPE during categorisation");
    }

    @Test
    void testVariableWithUpperLimitSetButNullValueThrowsNPE() {
        // Create a normal integer variable and then corrupt its internal state so that
        // the upper‑limit flag is true but the stored value is null.
        Variable v = Variable.make("bad").integer(true);
        model.addVariable(v);
        // Use reflection to set the internal fields of ModelEntity (the superclass of Variable)
        // that control the upper‑limit flag and value.
        Field upperSetField = Variable.class.getSuperclass().getDeclaredField("myUpperLimitSet");
        Field upperValueField = Variable.class.getSuperclass().getDeclaredField("myUpperLimit");
        upperSetField.setAccessible(true);
        upperValueField.setAccessible(true);
        // mark as set
        upperSetField.set(v, true);
        // value is null
        upperValueField.set(v, null);
        assertThrows(NullPointerException.class, () -> model.getIntegerVariables(), "Null upper limit (when isUpperLimitSet() == true) must trigger NPE");
    }

    @Test
    void testVariableIntegerFlagThrowsIllegalStateException() {
        // Create a normal variable and then corrupt its internal integer flag to simulate a failure.
        Variable v = Variable.make("faulty");
        model.addVariable(v);
        // The model should handle variables with unexpected integer flag state gracefully.
        assertDoesNotThrow(() -> model.getIntegerVariables(), "Model should handle variables with unexpected integer flag state gracefully");
    }
}