package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Nested;

class ExpressionsBasedModel_isFixed_Test {

    @Test
    void testIsFixedOnEmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertTrue(model.isFixed(), "An empty model (no variables) should be considered fixed.");
    }

    @Test
    void testIsFixedWhenAllVariablesAreFixed() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable fixed via level (lower == upper)
        model.newVariable("x").level(5);
        // Variable fixed via explicit lower and upper bounds
        model.newVariable("y").lower(3).upper(3);
        // Another fixed variable using the same API for completeness
        model.newVariable("z").lower(10).upper(10);
        assertTrue(model.isFixed(), "All variables are fixed (lower == upper), so isFixed() must return true.");
    }

    @Test
    void testIsFixedWhenAtLeastOneVariableIsNotFixed() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Fixed variable
        model.newVariable("fixed").level(2);
        // Non‑fixed variable (different lower and upper bounds)
        model.newVariable("free").lower(0).upper(10);
        assertFalse(model.isFixed(), "Because not all variables are fixed, isFixed() must return false.");
    }

    @Test
    void testIsFixedAfterChangingVariableBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Initially fixed
        Variable v = model.newVariable("v").level(7);
        assertTrue(model.isFixed(), "Model should be fixed after creating a fixed variable.");
        // Change bounds to make it non‑fixed
        v.lower(0).upper(10);
        assertFalse(model.isFixed(), "Model should become non‑fixed after loosening the variable bounds.");
    }

    @Test
    void testDynamicVariableAddition() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add two fixed variables
        model.newVariable("a").level(1);
        model.newVariable("b").level(2);
        assertTrue(model.isFixed(), "Model should be fixed after adding two fixed variables.");
        // Add a third, non‑fixed variable
        model.newVariable("c").lower(0).upper(5);
        assertFalse(model.isFixed(), "Model should be non‑fixed after adding an unfixed variable.");
        // Fix the third variable as well
        model.getVariable("c").level(3);
        assertTrue(model.isFixed(), "Model should be fixed again after fixing the previously unfixed variable.");
    }

    @Test
    void testToggleFixedFlagOnSingleVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("single");
        // Explicitly set the fixed flag (the API provides setFixed)
        v.setFixed(true);
        assertTrue(model.isFixed(), "A single fixed variable should make the model fixed.");
        // Unset the flag
        v.setFixed(false);
        assertFalse(model.isFixed(), "A single unfixed variable should make the model non‑fixed.");
    }

    @Test
    void testCopyAndRelaxBehaviour() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // Two fixed variables and one unfixed variable
        original.newVariable("x").level(1);
        original.newVariable("y").level(2);
        // unfixed
        original.newVariable("z").lower(0).upper(10);
        // Shallow copy – the unfixed variable is still present
        ExpressionsBasedModel copy = original.copy();
        assertFalse(copy.isFixed(), "Copy should report false because it still contains an unfixed variable.");
        // Relax clears the fixed flag of all variables; we then fix them manually
        // after relax all variables become non‑fixed
        copy.relax();
        for (Variable var : copy.getVariables()) {
            // fixing each variable again
            var.level(5);
        }
        assertTrue(copy.isFixed(), "After relaxing and fixing all variables again, the copy must be fixed.");
    }

    @Test
    void testIsFixedWhenInternalListIsNull() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use reflection to set the private field `myVariables` to null
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        field.set(model, null);
        assertThrows(NullPointerException.class, model::isFixed, "Calling isFixed() when myVariables is null should throw NPE.");
    }

    @Test
    void testIsFixedWhenListContainsNullElement() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add a regular variable first
        model.newVariable("valid");
        // Replace the first element with null via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) field.get(model);
        // now the list contains a null entry
        vars.set(0, null);
        assertThrows(NullPointerException.class, model::isFixed, "A null element inside myVariables should cause NPE when isFixed() streams over it.");
    }
}
