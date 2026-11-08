package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

// Mockito not actually needed but kept from template
@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isUnbounded_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @Test
    void testIsUnboundedWithNoVariables() {
        // An empty model has no variables, therefore cannot be un‑bounded
        assertFalse(model.isUnbounded(), "Model with no variables should not be reported as unbounded");
    }

    @Test
    void testIsUnboundedWithOnlyBoundedVariables() {
        // Create a variable with explicit lower and upper limits → bounded
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertFalse(model.isUnbounded(), "Model with only bounded variables should not be reported as unbounded");
    }

    @Test
    void testIsUnboundedWithSingleUnboundedVariable() {
        // A variable created without any bounds is unbounded by definition
        model.newVariable("y");
        assertTrue(model.isUnbounded(), "Model containing an unbounded variable should be reported as unbounded");
    }

    @Test
    void testIsUnboundedWithMixedVariables() {
        // First variable bounded
        model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        // Second variable unbounded
        model.newVariable("b");
        assertTrue(model.isUnbounded(), "Model containing at least one unbounded variable should be reported as unbounded");
    }

    /**
     * Helper to access the private {@code myVariables} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Variable> getInternalVariableList() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        return (ArrayList<Variable>) f.get(model);
    }

    @Test
    void testIsUnboundedWhenFirstVariableIsUnbounded() {
        // First variable unbounded
        model.newVariable("u");
        // Second variable bounded
        model.newVariable("c").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(3));
        assertTrue(model.isUnbounded(), "Unbounded variable at the beginning of the list must cause true");
    }

    @Test
    void testIsUnboundedWithMultipleUnboundedVariables() {
        // Three unbounded variables
        model.newVariable("u1");
        model.newVariable("u2");
        model.newVariable("u3");
        assertTrue(model.isUnbounded(), "Presence of several unbounded variables must still return true");
    }

    @Test
    void testIsUnboundedWhenUpperBoundIsInfinity() {
        // Upper bound set to +Infinity (unbounded above)
        model.newVariable("v").lower(BigDecimal.ZERO).upper(// from BigMath
        POSITIVE_INFINITY);
        assertTrue(model.isUnbounded(), "Variable with upper = +Infinity should be considered unbounded");
    }

    @Test
    void testIsUnboundedWhenLowerBoundIsMinusInfinity() {
        // Lower bound set to -Infinity (unbounded below)
        model.newVariable("v").lower(// from BigMath
        NEGATIVE_INFINITY).upper(BigDecimal.TEN);
        assertTrue(model.isUnbounded(), "Variable with lower = -Infinity should be considered unbounded");
    }

    @Test
    void testIsUnboundedAfterBoundsAreAdjustedToBeFinite() throws Exception {
        // Initially unbounded
        Variable var = model.newVariable("temp");
        assertTrue(model.isUnbounded(), "Should be unbounded before fixing bounds");
        // Adjust bounds to finite values
        var.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertFalse(model.isUnbounded(), "After making the variable bounded, model must report not unbounded");
    }

    @Test
    void testIsUnboundedAfterVariableRemoval() throws Exception {
        // Add a single unbounded variable
        Variable var = model.newVariable("toRemove");
        assertTrue(model.isUnbounded(), "Model should be unbounded before removal");
        // Remove it via reflection (no public API for removal)
        ArrayList<Variable> list = getInternalVariableList();
        list.remove(var);
        assertFalse(model.isUnbounded(), "After removing the only variable, model must not be unbounded");
    }

    @Test
    void testIsUnbounded_NullVariable_ThrowsNullPointerException() throws Exception {
        // Inject a null entry into the internal variable list
        ArrayList<Variable> list = getInternalVariableList();
        list.add(null);
        assertThrows(NullPointerException.class, () -> model.isUnbounded(), "Encountering a null variable should propagate NullPointerException");
    }

    @Test
    void testIsUnbounded_CustomVariableThrowsIllegalStateException() throws Exception {
        // Create a mock Variable that throws IllegalStateException when isUnbounded() is called
        Variable badVar = Mockito.mock(Variable.class);
        Mockito.when(badVar.isUnbounded()).thenThrow(new IllegalStateException("bad var"));
        // Insert the mock into the model's variable list
        getInternalVariableList().add(badVar);
        assertThrows(IllegalStateException.class, () -> model.isUnbounded(), "Exception from a Variable should be propagated by isUnbounded()");
    }

    @Test
    void testIsUnbounded_ConcurrentModificationThrowsException() throws Exception {
        // Populate the model with many variables to increase the chance of iteration overlap
        for (int i = 0; i < 1_000; i++) {
            model.newVariable("v" + i);
        }
        // Obtain the internal list reference
        ArrayList<Variable> list = getInternalVariableList();
        // Start a thread that will modify the list shortly after iteration begins
        Thread modifier = new Thread(() -> {
            try {
                // Small pause to let the main thread start streaming
                Thread.sleep(5);
                // Structural change while the stream is active
                list.remove(0);
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        // The fail‑fast iterator of ArrayList should detect the concurrent modification
        assertThrows(ConcurrentModificationException.class, () -> model.isUnbounded(), "Concurrent modification of the variable list must result in CME");
        // Ensure thread cleanup
        modifier.join();
    }
}
