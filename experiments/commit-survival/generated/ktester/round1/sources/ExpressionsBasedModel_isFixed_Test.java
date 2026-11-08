package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isFixed_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model (good practice, although not strictly required)
        model.dispose();
    }

    /**
     * Adds a variable to the model and fixes it at the given value.
     */
    private Variable addFixedVariable(BigDecimal value) {
        Variable v = model.addVariable();
        // fixing via level sets lower == upper
        v.level(value);
        return v;
    }

    /**
     * Adds a variable that remains unfixed (default bounds).
     */
    private Variable addUnfixedVariable() {
        // default bounds are not equal → not fixed
        return model.addVariable();
    }

    /**
     * Replaces the private {@code myVariables} field with the supplied list (or null).
     */
    private void setMyVariablesList(Object newValue) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, newValue);
    }

    /**
     * Calls the private {@code isFixed()} method via reflection (used for the
     *  cases where we deliberately corrupt the internal state).
     */
    private boolean invokeIsFixedViaReflection() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        // The method itself is package‑private, so we can call it directly
        return model.isFixed();
    }

    /**
     * An empty model (no variables) should be considered “fixed”.
     * The implementation uses {@code Stream#allMatch} which returns {@code true}
     * for an empty stream.
     */
    @Test
    void testIsFixedEmptyModel() {
        assertTrue(model.isFixed(), "A model with no variables should be reported as fixed (empty stream).");
    }

    /**
     * When all variables have identical lower and upper bounds (i.e. they are fixed),
     * {@code isFixed()} must return {@code true}.
     */
    @Test
    void testIsFixedAllVariablesFixed() {
        // Variable 1 fixed at 3
        Variable v1 = model.addVariable();
        v1.level(BigDecimal.valueOf(3));
        // Variable 2 fixed at -1.5
        Variable v2 = model.addVariable();
        v2.lower(BigDecimal.valueOf(-1.5)).upper(BigDecimal.valueOf(-1.5));
        // Variable 3 fixed via explicit lower/upper equality
        Variable v3 = model.addVariable();
        v3.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        assertTrue(model.isFixed(), "All variables are fixed, should return true");
    }

    /**
     * If at least one variable is not fixed, {@code isFixed()} must return {@code false}.
     */
    @Test
    void testIsFixedWithUnfixedVariable() {
        // Fixed variable
        Variable fixed = model.addVariable();
        fixed.level(BigDecimal.TEN);
        // Unfixed variable (different lower and upper bounds)
        Variable free = model.addVariable();
        free.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
        assertFalse(model.isFixed(), "Model contains an unfixed variable, should return false");
    }

    /**
     * Retrieves the private {@code myVariables} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Variable> getMyVariablesList() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        return (ArrayList<Variable>) f.get(model);
    }

    /**
     * Single variable that is fixed.
     */
    @Test
    void testIsFixedSingleFixedVariable() {
        addFixedVariable(BigDecimal.valueOf(5));
        assertTrue(model.isFixed(), "Model with a single fixed variable must return true.");
    }

    /**
     * Single variable that is not fixed.
     */
    @Test
    void testIsFixedSingleNonFixedVariable() {
        addUnfixedVariable();
        assertFalse(model.isFixed(), "Model with a single unfixed variable must return false.");
    }

    /**
     * Multiple variables – all fixed.
     */
    @Test
    void testIsFixedMultipleAllFixed() {
        addFixedVariable(BigDecimal.ONE);
        addFixedVariable(BigDecimal.TEN);
        addFixedVariable(BigDecimal.ZERO);
        assertTrue(model.isFixed(), "All variables fixed → isFixed() should be true.");
    }

    /**
     * Multiple variables – exactly one unfixed.
     */
    @Test
    void testIsFixedMultipleOneUnfixed() {
        addFixedVariable(BigDecimal.ONE);
        addFixedVariable(BigDecimal.TEN);
        // the offending variable
        addUnfixedVariable();
        assertFalse(model.isFixed(), "Presence of any unfixed variable must make isFixed() return false.");
    }

    /**
     * Variable list mutated after a previous successful call.
     */
    @Test
    void testIsFixedVariableListMutatedAfterCall() {
        // two fixed variables → true
        addFixedVariable(BigDecimal.ONE);
        addFixedVariable(BigDecimal.TWO);
        assertTrue(model.isFixed());
        // add an unfixed variable → false on second call
        addUnfixedVariable();
        assertFalse(model.isFixed(), "After adding an unfixed variable, isFixed() must reflect the new state.");
    }

    /**
     * Variable becomes unfixed after being fixed.
     */
    @Test
    void testIsFixedVariableBecomesUnfixed() throws Exception {
        Variable v1 = addFixedVariable(BigDecimal.ONE);
        Variable v2 = addFixedVariable(BigDecimal.TWO);
        assertTrue(model.isFixed());
        // Unfix one variable using the public API (setFixed(false) exists in Variable)
        // If the method does not exist, we fall back to reflection.
        try {
            v1.setFixed(false);
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            // Reflective fallback
            var m = Variable.class.getDeclaredMethod("setFixed", boolean.class);
            m.setAccessible(true);
            m.invoke(v1, false);
        }
        assertFalse(model.isFixed(), "After un‑fixing a variable, isFixed() must return false.");
    }

    /**
     * All variables become fixed after being unfixed.
     */
    @Test
    void testIsFixedAllBecomeFixed() throws Exception {
        // three unfixed variables
        Variable a = addUnfixedVariable();
        Variable b = addUnfixedVariable();
        Variable c = addUnfixedVariable();
        assertFalse(model.isFixed());
        // fix each one
        a.level(BigDecimal.ONE);
        b.level(BigDecimal.TEN);
        c.level(BigDecimal.ZERO);
        assertTrue(model.isFixed(), "After fixing every variable, isFixed() must return true.");
    }

    /**
     * Mixed variables become all fixed after updates.
     */
    @Test
    void testIsFixedMixedBecomeAllFixed() {
        // four variables, three fixed, one unfixed
        addFixedVariable(BigDecimal.ONE);
        addFixedVariable(BigDecimal.TWO);
        addFixedVariable(BigDecimal.THREE);
        // the remaining unfixed
        addUnfixedVariable();
        assertFalse(model.isFixed());
        // fix the last one
        model.getVariables().get(3).level(BigDecimal.valueOf(4));
        assertTrue(model.isFixed(), "After fixing the previously unfixed variable, isFixed() must be true.");
    }

    /**
     * Variable fixed status toggled multiple times.
     */
    @Test
    void testIsFixedVariableToggleMultipleTimes() throws Exception {
        Variable v = model.addVariable();
        // 1st toggle – fix
        v.level(BigDecimal.ONE);
        assertTrue(model.isFixed());
        // 2nd toggle – unfix via reflection (setFixed false)
        try {
            v.setFixed(false);
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            var m = Variable.class.getDeclaredMethod("setFixed", boolean.class);
            m.setAccessible(true);
            m.invoke(v, false);
        }
        assertFalse(model.isFixed());
        // 3rd toggle – fix again
        v.level(BigDecimal.ONE);
        assertTrue(model.isFixed(), "After an odd number of fixes, the variable ends up fixed → true.");
    }

    /**
     * Two variables with sequential fixes/unfixes.
     */
    @Test
    void testIsFixedTwoVariablesSequentialFixes() throws Exception {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        // fix first, second remains free → false
        v1.level(BigDecimal.ONE);
        assertFalse(model.isFixed());
        // fix second → true
        v2.level(BigDecimal.TEN);
        assertTrue(model.isFixed());
        // unfix first → false again
        try {
            v1.setFixed(false);
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            var m = Variable.class.getDeclaredMethod("setFixed", boolean.class);
            m.setAccessible(true);
            m.invoke(v1, false);
        }
        assertFalse(model.isFixed(), "After un‑fixing the first variable, isFixed() must be false.");
    }

    /**
     * Model with only integer‑type variables, all fixed.
     */
    @Test
    void testIsFixedOnlyIntegerVariablesAllFixed() throws Exception {
        Variable v1 = addFixedVariable(BigDecimal.ONE);
        Variable v2 = addFixedVariable(BigDecimal.TEN);
        // Mark them as integer
        v1.setInteger(true);
        v2.setInteger(true);
        assertTrue(model.isFixed(), "Integer flag must be ignored – all variables are fixed → true.");
    }

    /**
     * After removing a variable (simulated via reflection).
     */
    @Test
    void testIsFixedAfterRemovingVariableViaReflection() throws Exception {
        // two fixed variables
        addFixedVariable(BigDecimal.ONE);
        addFixedVariable(BigDecimal.TWO);
        assertTrue(model.isFixed());
        // Remove one variable by clearing the internal list and adding back only the first
        ArrayList<Variable> original = getMyVariablesList();
        Variable keep = original.get(0);
        ArrayList<Variable> newList = new ArrayList<>();
        newList.add(keep);
        setMyVariablesList(newList);
        assertTrue(model.isFixed(), "After removing the unfixed variable (none left), isFixed() should stay true.");
    }

    /**
     * Null variable injected into the internal list – expect {@link NullPointerException}.
     */
    @Test
    void testIsFixedNullVariableInList() throws Exception {
        // Ensure at least one proper variable exists
        addFixedVariable(BigDecimal.ONE);
        // Inject a null entry
        getMyVariablesList().add(null);
        assertThrows(NullPointerException.class, () -> model.isFixed(), "A null entry in the list should cause NullPointerException.");
    }

    /**
     * Internal list set to {@code null} – expect {@link NullPointerException}.
     */
    @Test
    void testIsFixedInternalListSetToNull() throws Exception {
        // Set the private field to null via reflection
        setMyVariablesList(null);
        assertThrows(NullPointerException.class, () -> model.isFixed(), "Setting myVariables to null should cause NullPointerException.");
    }

    /**
     * Concurrent modification during stream evaluation – expect {@link ConcurrentModificationException}.
     *
     * <p>
     * We mock a {@link Variable} whose {@code isFixed()} method adds a new variable to the model.
     * While {@code isFixed()} processes the stream, the underlying {@code ArrayList} is structurally
     * modified, which must result in a {@code ConcurrentModificationException}.
     * </p>
     */
    @Test
    void testIsFixedConcurrentModification() throws Exception {
        // Prepare a normal fixed variable
        addFixedVariable(BigDecimal.ONE);
        // Create a mock Variable that mutates the model during isFixed()
        Variable mutatingVar = Mockito.mock(Variable.class);
        Mockito.when(mutatingVar.isFixed()).thenAnswer(invocation -> {
            // Mutate the model: add another variable while the stream is being evaluated
            model.addVariable();
            return true;
        });
        // Insert the mock into the internal list via reflection
        getMyVariablesList().add(mutatingVar);
        // The stream should now throw ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, () -> model.isFixed(), "Modifying the variable list during stream evaluation must throw ConcurrentModificationException.");
    }
}
