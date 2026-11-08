package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
// import java.util.concurrent.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_relax_2_Test {

    private static final Field MY_VARIABLES_FIELD;

    private static final Field MY_RELAXED_FIELD;

    /**
     * Counts how many times {@code relax()} has been invoked.
     */
//     private static class CountingVariable extends Variable {

        int relaxCount = 0;

        boolean throwOnRelax = false;

        RuntimeException exceptionToThrow = null;

        CountingVariable(String name) {
//             super(name);
        }

//         @Override
//         public void relax() {
//             if (throwOnRelax) {
//                 throw exceptionToThrow;
//             }
//             relaxCount++;
//         }

        void reset() {
            relaxCount = 0;
        }
    }

    /**
     * A variable whose {@code relax()} removes itself from the model's list.
     */
//     private static class SelfRemovingVariable extends Variable {

        private final ExpressionsBasedModel model;

        SelfRemovingVariable(String name, ExpressionsBasedModel model) {
//             super(name);
            this.model = model;
        }

//         @Override
//         public void relax() {
//             try {
//                 List<Variable> list = (List<Variable>) MY_VARIABLES_FIELD.get(model);
//                 list.remove(this);
//             } catch (Exception e) {
//                 throw new RuntimeException(e);
//             }
//         }
    }

    /**
     * Helper to read the private {@code myRelaxed} flag.
     */
    private boolean getRelaxedFlag(ExpressionsBasedModel model) throws Exception {
        return (boolean) MY_RELAXED_FIELD.get(model);
    }

    /**
     * Clears the internal variable list – useful for isolation between tests.
     */
    private void clearVariables(ExpressionsBasedModel model) throws Exception {
        getInternalVariableList(model).clear();
    }

    /**
     * When {@code soft} is {@code true} the model should be marked as relaxed.
     */
    @Test
    void testRelaxSoftTrueSetsRelaxedFlag() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isRelaxed(), "Model should start non‑relaxed");
        model.relax(true);
        assertTrue(model.isRelaxed(), "Model should be relaxed after relax(true)");
    }

    /**
     * When {@code soft} is {@code false} the model itself must stay non‑relaxed.
     * The method also iterates over all variables and calls {@code relax()} on each.
     * We verify the flag remains false and that the call does not throw
     * even when the model contains variables.
     */
    @Test
    void testRelaxSoftFalseDoesNotSetRelaxedFlag() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.newVariable("v1").lower(0).upper(10).weight(1);
        Variable v2 = model.newVariable("v2").lower(0).upper(5).weight(2);
        assertFalse(model.isRelaxed(), "Model should start non‑relaxed");
        model.relax(false);
        assertFalse(model.isRelaxed(), "Model must remain non‑relaxed after relax(false)");
        assertEquals(0, v1.getLowerLimit().doubleValue(), 0.0);
        assertEquals(10, v1.getUpperLimit().doubleValue(), 0.0);
        assertEquals(0, v2.getLowerLimit().doubleValue(), 0.0);
        assertEquals(5, v2.getUpperLimit().doubleValue(), 0.0);
    }

    /**
     * The no‑argument {@code relax()} method delegates to {@code relax(false)}.
     * This test confirms the delegation behaviour by checking that the relaxed flag
     * stays false after calling the no‑arg version.
     */
    @Test
    void testRelaxNoArgDelegatesToRelaxFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").lower(0).upper(1).weight(1);
        assertFalse(model.isRelaxed());
        model.relax();
        assertFalse(model.isRelaxed(), "Model should remain non‑relaxed after calling relax()");
    }

    /**
     * Helper to obtain the internal variable list via reflection.
     */
    @SuppressWarnings("unchecked")
    private List<Variable> getInternalVariableList(ExpressionsBasedModel model) throws Exception {
        return (List<Variable>) MY_VARIABLES_FIELD.get(model);
    }

    @Test
    void testInitialRelaxedFlagIsFalse() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(getRelaxedFlag(model), "New model must start with myRelaxed == false");
    }

    @Test
    void testRelaxSoftTrueNoVariables() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        clearVariables(model);
        model.relax(true);
        assertTrue(model.isRelaxed());
    }

    @Test
    void testRelaxSoftTrueWithVariables_NoVariableRelaxInvoked() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable mock1 = new CountingVariable("m1");
        CountingVariable mock2 = new CountingVariable("m2");
        getInternalVariableList(model).add(mock1);
        getInternalVariableList(model).add(mock2);
        model.relax(true);
        assertTrue(model.isRelaxed(), "Relax(true) must set the flag");
        assertEquals(0, mock1.relaxCount, "Variable relax must not be called for soft=true");
        assertEquals(0, mock2.relaxCount, "Variable relax must not be called for soft=true");
    }

    @Test
    void testRelaxSoftFalseNoVariables() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        clearVariables(model);
        model.relax(false);
        assertFalse(model.isRelaxed(), "Relax(false) must leave flag false");
    }

    @Test
    void testRelaxSoftFalseWithVariables_InvokesRelaxOncePerVariable() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable v1 = new CountingVariable("v1");
        CountingVariable v2 = new CountingVariable("v2");
        getInternalVariableList(model).add(v1);
        getInternalVariableList(model).add(v2);
        model.relax(false);
        assertFalse(model.isRelaxed(), "Relax(false) must not set the flag");
        assertEquals(1, v1.relaxCount, "Variable should be relaxed exactly once");
        assertEquals(1, v2.relaxCount, "Variable should be relaxed exactly once");
    }

    @Test
    void testRepeatedCallsSoftTrueThenSoftFalse() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable v = new CountingVariable("v");
        getInternalVariableList(model).add(v);
        model.relax(true);
        assertTrue(model.isRelaxed());
        model.relax(false);
        assertTrue(model.isRelaxed(), "Flag must stay true after second call");
        assertEquals(1, v.relaxCount, "Variable.relax() should be called only on the soft‑false call");
    }

    @Test
    void testRepeatedCallsSoftFalseThenSoftTrue() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable v = new CountingVariable("v");
        getInternalVariableList(model).add(v);
        model.relax(false);
        assertFalse(model.isRelaxed());
        assertEquals(1, v.relaxCount, "First call should invoke relax()");
        // clear interaction count
        v.reset();
        model.relax(true);
        assertTrue(model.isRelaxed());
        assertEquals(0, v.relaxCount, "Second call (soft=true) must not invoke relax() again");
    }

    @Test
    void testMultipleSoftFalseCallsWithVariables() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable v1 = new CountingVariable("v1");
        CountingVariable v2 = new CountingVariable("v2");
        getInternalVariableList(model).add(v1);
        getInternalVariableList(model).add(v2);
        model.relax(false);
        model.relax(false);
        model.relax(false);
        assertFalse(model.isRelaxed());
        assertEquals(3, v1.relaxCount, "v1 should have been relaxed three times");
        assertEquals(3, v2.relaxCount, "v2 should have been relaxed three times");
    }

    @Test
    void testAddVariableAfterFirstRelaxFalseAndCallRelaxFalseAgain() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable v1 = new CountingVariable("v1");
        getInternalVariableList(model).add(v1);
        model.relax(false);
        assertEquals(1, v1.relaxCount, "First call should relax v1 once");
        CountingVariable v2 = new CountingVariable("v2");
        getInternalVariableList(model).add(v2);
        model.relax(false);
        assertEquals(2, v1.relaxCount, "v1 should be relaxed a second time");
        assertEquals(1, v2.relaxCount, "v2 should be relaxed once");
    }

    @Test
    void testRelaxSoftFalseWithNullVariableThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        getInternalVariableList(model).add(null);
        assertThrows(NullPointerException.class, () -> model.relax(false), "Relax(false) should propagate NullPointerException when a null variable is present");
    }

    @Test
    void testRelaxSoftFalseVariableRelaxThrowsUncheckedException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        CountingVariable badVar = new CountingVariable("bad");
        badVar.throwOnRelax = true;
        badVar.exceptionToThrow = new IllegalStateException("mocked failure");
        getInternalVariableList(model).add(badVar);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.relax(false), "Relax(false) must propagate unchecked exceptions from Variable.relax()");
        assertEquals("mocked failure", thrown.getMessage());
    }

//     @Test
//     void testRelaxSoftFalseConcurrentModificationDuringIteration() throws Exception {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Add a variable that removes itself during iteration to provoke CME
//         Variable selfRemoving = new Variable("self") {
// 
//             @Override
//             public Variable relax() {
//                 try {
//                     List<Variable> list = (List<Variable>) MY_VARIABLES_FIELD.get(model);
//                     list.remove(this);
//                 } catch (Exception e) {
//                     throw new RuntimeException(e);
//                 }
//                 return this;
//             }
//         };
//         getInternalVariableList(model).add(selfRemoving);
//         // Adding another normal variable
//         getInternalVariableList(model).add(new Variable("other"));
//         assertThrows(ConcurrentModificationException.class, () -> model.relax(false), "Modifying the variable list during iteration must cause ConcurrentModificationException");
//     }
}