package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_relax_2_Test {

    private ExpressionsBasedModel model;

    private Variable integerVariable;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
        // create an integer variable that will be used in the “hard” relax test
        integerVariable = Variable.make("x").lower(0).upper(10).weight(1).integer(true);
        model.addVariable(integerVariable);
    }

    /**
     * Clears all variables from the model (used for the “no variables” test case).
     */
    private void clearVariablesFromModel() throws Exception {
        List<Variable> vars = getVariableListViaReflection();
        vars.clear();
    }

    /**
     * Replaces the first variable in the model's internal list with the supplied one.
     * Used to inject a Mockito spy or mock.
     */
    private void replaceVariableInModel(Variable replacement) throws Exception {
        List<Variable> vars = getVariableListViaReflection();
        vars.clear();
        vars.add(replacement);
    }

    /**
     * When {@code soft} is {@code true} the model should be marked as relaxed
     * without touching the variables.
     */
    @Test
    void testRelaxSoftTrueSetsModelRelaxedFlag() {
        assertFalse(model.isRelaxed(), "Model should start non‑relaxed");
        model.relax(true);
        assertTrue(model.isRelaxed(), "Model should be marked relaxed after soft relax");
        // Variables must stay unchanged – the integer flag should still be true
        assertTrue(model.isAnyVariableInteger(), "Integer variable must remain integer when soft‑relax is used");
    }

    /**
     * When {@code soft} is {@code false} each variable must be relaxed.
     * For an integer variable this means it is no longer considered integer.
     */
    @Test
    void testRelaxHardFalseRelaxesVariables() {
        // pre‑condition: the model contains an integer variable
        assertTrue(model.isAnyVariableInteger(), "Model should contain at least one integer variable before hard relax");
        // perform hard relax
        model.relax(false);
        // the model itself must not be flagged as “soft” relaxed
        assertFalse(model.isRelaxed(), "Model should not be marked as soft‑relaxed after hard relax");
        // after relaxing, no integer variables should remain
        assertFalse(model.isAnyVariableInteger(), "All integer variables should have been relaxed to continuous");
    }

    /**
     * Verify that a soft relaxation does **not** invoke {@code Variable.relax()}
     * on any variable.  A Mockito spy is used to detect unwanted interactions.
     */
    @Test
    void testRelaxSoftTrueDoesNotCallVariableRelax() throws Exception {
        // Replace the real variable with a spy
        Variable spyVar = Mockito.spy(integerVariable);
        replaceVariableInModel(spyVar);
        model.relax(true);
        // No interaction with Variable.relax() should have happened
        verify(spyVar, never()).relax();
        assertTrue(model.isRelaxed(), "Model must be flagged as relaxed");
    }

    /**
     * Verify that a hard relaxation invokes {@code Variable.relax()} exactly once
     * for each variable present in the model.
     */
    @Test
    void testRelaxHardFalseCallsVariableRelaxOnce() throws Exception {
        Variable spyVar = Mockito.spy(integerVariable);
        replaceVariableInModel(spyVar);
        model.relax(false);
        verify(spyVar, times(1)).relax();
        assertFalse(model.isRelaxed(), "Model must not be soft‑relaxed");
    }

    /**
     * Calling {@code relax(false)} on a model with **no** variables should leave
     * the model non‑relaxed and must not throw or attempt to relax anything.
     */
    @Test
    void testRelaxHardFalseWithNoVariables() throws Exception {
        // Remove all variables via reflection
        clearVariablesFromModel();
        assertFalse(model.isRelaxed(), "Model should start non‑relaxed");
        // No exception expected
        model.relax(false);
        assertFalse(model.isRelaxed(), "Model should remain non‑relaxed after hard relax with no variables");
    }

    /**
     * After a soft relaxation, a subsequent hard relaxation must keep the
     * model's relaxed flag true while still relaxing each variable.
     */
    @Test
    void testRelaxSoftThenHardKeepsRelaxedFlagAndRelaxesVariables() throws Exception {
        Variable spyVar = Mockito.spy(integerVariable);
        replaceVariableInModel(spyVar);
        // First soft relax
        model.relax(true);
        assertTrue(model.isRelaxed(), "Model should be soft‑relaxed after first call");
        // Then hard relax
        model.relax(false);
        // The soft‑relaxed flag must stay true
        assertTrue(model.isRelaxed(), "Model should remain soft‑relaxed after hard relax");
        // Variable.relax() must have been called exactly once
        verify(spyVar, times(1)).relax();
    }

    /**
     * When {@code soft} is {@code false} and the internal variable list
     * contains a {@code null} entry, a {@link NullPointerException}
     * should be thrown.
     */
    @Test
    void testRelaxHardFalseWithNullVariableThrowsNPE() throws Exception {
        // Inject a null into the private myVariables list
        List<Variable> vars = getVariableListViaReflection();
        // now list contains a null entry
        vars.add(null);
        assertThrows(NullPointerException.class, () -> model.relax(false), "Relax(false) should throw NullPointerException when a null variable is present");
    }

    /**
     * If a variable's {@code relax()} method throws an {@link IllegalStateException},
     * the exception must propagate out of {@code ExpressionsBasedModel.relax(boolean)}.
     */
    @Test
    void testRelaxHardFalsePropagatesExceptionFromVariableRelax() throws Exception {
        // Create a mock Variable that throws IllegalStateException on relax()
        Variable badVar = mock(Variable.class);
        doThrow(new IllegalStateException("forced")).when(badVar).relax();
        // Add the mock variable to the model via reflection
        List<Variable> vars = getVariableListViaReflection();
        vars.add(badVar);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.relax(false), "Exception from Variable.relax() must propagate");
        assertEquals("forced", thrown.getMessage());
    }

    /**
     * Retrieves the private {@code myVariables} list from the model instance.
     */
    @SuppressWarnings("unchecked")
    private List<Variable> getVariableListViaReflection() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        return (List<Variable>) field.get(model);
    }
}
