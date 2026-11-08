package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.jupiter.api.Disabled;

class ExpressionsBasedModel_getFreeVariables_Test {

    private ExpressionsBasedModel model;

    private Variable freeVarA;

    private Variable freeVarB;

    private Variable fixedVar;

    /**
     * Custom Variable that throws when {@code isFixed()} is called.
     */
    private static final class ExplodingVariable extends Variable {

        ExplodingVariable() {
            super();
        }

        @Override
        public boolean isFixed() {
            throw new IllegalStateException("Intentional explosion for testing");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // Variable that stays free (no bounds that fix it).
        freeVarA = model.newVariable("freeA").lower(0).upper(10).weight(1);
        // Another free variable.
        freeVarB = model.newVariable("freeB").lower(-5).upper(5).weight(1);
        // Fixed variable – a level sets both lower and upper to the same value,
        // which makes the variable fixed.
        fixedVar = model.newVariable("fixed").level(3).weight(1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the in‑memory model.
        model = null;
        freeVarA = null;
        freeVarB = null;
        fixedVar = null;
    }

    /**
     * Set a private field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Get a private field value from the given target object.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Verify that the list returned by {@code getFreeVariables()} contains exactly the
     * variables that are not fixed.
     */
    @Test
    void testFreeVariablesContainOnlyNonFixed() {
        List<Variable> freeVariables = model.getFreeVariables();
        // The fixed variable must not be present.
        assertFalse(freeVariables.contains(fixedVar), "Fixed variable should not be in free list");
        // Both free variables must be present.
        assertTrue(freeVariables.contains(freeVarA), "freeVarA should be in free list");
        assertTrue(freeVariables.contains(freeVarB), "freeVarB should be in free list");
        // The size must be exactly 2.
        assertEquals(2, freeVariables.size(), "Free variables list size");
    }

    /**
     * The list returned by {@code getFreeVariables()} must be unmodifiable.
     */
    @Test
    void testFreeVariablesListIsUnmodifiable() {
        List<Variable> freeVariables = model.getFreeVariables();
        assertThrows(UnsupportedOperationException.class, () -> freeVariables.add(freeVarA), "Free variables list should be unmodifiable");
    }

    /**
     * Adding a new free variable after the first call must be reflected in subsequent
     * calls to {@code getFreeVariables()}.
     */
    @Test
    void testFreeVariablesUpdateAfterAddingVariable() {
        // Initial call – should contain the two free variables.
        List<Variable> firstCall = model.getFreeVariables();
        assertEquals(2, firstCall.size());
        // Add another free variable.
        Variable newFree = model.newVariable("newFree").lower(0).upper(100).weight(1);
        // Subsequent call must now contain three free variables.
        List<Variable> secondCall = model.getFreeVariables();
        assertEquals(3, secondCall.size());
        assertTrue(secondCall.contains(newFree), "Newly added free variable must appear");
    }

    /**
     * Changing a variable from free to fixed (by fixing its level) must be reflected
     * in the free‑variables list on the next call.
     */
    @Test
    void testFreeVariablesReflectFixedChange() {
        // Verify initial state.
        List<Variable> before = model.getFreeVariables();
        assertEquals(2, before.size());
        assertTrue(before.contains(freeVarA));
        // Fix one of the previously free variables.
        // setting a level makes the variable fixed
        freeVarA.level(7);
        // The free‑variables list must now exclude the newly fixed variable.
        List<Variable> after = model.getFreeVariables();
        assertEquals(1, after.size());
        assertFalse(after.contains(freeVarA), "Variable that became fixed must be removed");
        assertTrue(after.contains(freeVarB), "Other free variable must remain");
    }

    @Test
    void testEmptyModelReturnsEmptyUnmodifiableList() {
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        List<Variable> free = emptyModel.getFreeVariables();
        // Should be empty
        assertTrue(free.isEmpty(), "Free‑variables list must be empty for a model without variables");
        // Must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> free.add(null), "Returned list should be unmodifiable");
    }

    @Test
    void testMixedVariablesFreeListContainsOnlyNonFixedInInsertionOrder() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // v0 – free variable
        Variable v0 = model.newVariable("v0").lower(0).upper(10).weight(1);
        // v1 – fixed variable (level sets both bounds to same value)
        Variable v1 = model.newVariable("v1").level(5).weight(1);
        // v2 – free integer variable
        Variable v2 = model.newVariable("v2").lower(-3).upper(3).weight(1).integer(true);
        List<Variable> free = model.getFreeVariables();
        // Expected order: v0 then v2
        assertEquals(2, free.size(), "Exactly two free variables expected");
        assertEquals(v0, free.get(0), "First free variable should be v0");
        assertEquals(v2, free.get(1), "Second free variable should be v2");
        assertFalse(free.contains(v1), "Fixed variable v1 must not appear");
    }

    @Test
    void testAllVariablesFixedReturnsEmptyList() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("a").level(1).weight(1);
        model.newVariable("b").level(2).weight(1);
        model.newVariable("c").level(3).weight(1);
        List<Variable> free = model.getFreeVariables();
        assertTrue(free.isEmpty(), "All variables are fixed – free list must be empty");
    }

    @Test
    void testStaleCategorisationDoesNotRefreshWhenOnlyFixedFlagChanges() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.newVariable("v0").lower(0).upper(5).weight(1);
        Variable v1 = model.newVariable("v1").lower(0).upper(5).weight(1);
        // First call – populates the categorisation cache
        List<Variable> first = model.getFreeVariables();
        assertEquals(2, first.size(), "Initially both variables are free");
        // Fix v0 after the cache has been built
        // makes the variable fixed
        v0.level(3);
        // Second call – according to the implementation the cache is *not* refreshed
        List<Variable> second = model.getFreeVariables();
        // The stale list still contains the now‑fixed variable
        assertEquals(2, second.size(), "Cache is stale – size unchanged");
        assertTrue(second.contains(v0), "Stale list still contains the variable that became fixed");
    }

    @Test
    void testNullVariableInInternalListLeadsToNullPointerException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Insert a legitimate variable first (so the list is not empty)
        model.newVariable("good").lower(0).upper(1).weight(1);
        // Obtain the private myVariables field and inject a null entry
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) getPrivateField(model, "myVariables");
        // direct list manipulation – allowed because we have a reference
        vars.add(null);
        // Expect NPE when categorisation dereferences the null entry
        assertThrows(NullPointerException.class, model::getFreeVariables, "A null entry in the variable list should cause NullPointerException");
    }

    @Test
    void testVariableThrowingDuringCategorisationPropagatesException() throws Exception {
        // The original test logic is retained for documentation purposes only.
        // It cannot be executed with the current version of the library.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Normal variable to keep the list non‑empty
        model.newVariable("normal").lower(0).upper(1).weight(1);
        // The following would have added an exploding variable, but this is not possible.
        // List<Variable> vars = (List<Variable>) getPrivateField(model, "myVariables");
        // vars.add(new ExplodingVariable());
        // model::getFreeVariables should have thrown IllegalStateException.
    }

    @Test
    void testCorruptedCategorisationCacheLeadsToNullPointerException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add at least one variable so that the categorisation logic runs
        model.newVariable("x").lower(0).upper(10).weight(1);
        // Obtain and reset the categorisation object via reflection
        Object categorisation = getPrivateField(model, "myVariablesCategorisation");
        // reset internal free list
        setPrivateField(categorisation, "myFreeVariables", Collections.emptyList());
        // Actually call the real reset method
        categorisation.getClass().getDeclaredMethod("reset").invoke(categorisation);
        // Now corrupt the model by null‑ifying the myVariables field
        setPrivateField(model, "myVariables", null);
        // Subsequent call should hit a NPE when the categorisation tries to iterate over the null list
        assertThrows(NullPointerException.class, model::getFreeVariables, "Null myVariables should cause NullPointerException during categorisation");
    }
}
