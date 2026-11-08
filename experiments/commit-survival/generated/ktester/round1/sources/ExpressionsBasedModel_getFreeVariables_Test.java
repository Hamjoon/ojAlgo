package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getFreeVariables_Test {

    /**
     * An empty model must report no free variables.
     */
//     @Test
//     void testEmptyModelReturnsEmptyList() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         List<Variable> free = model.getFreeVariables();
//         assertNotNull(free, "The returned list must not be null");
//         assertTrue(free.isEmpty(), "A newly created model should have no free variables");
//     }

    /**
     * Verify that only non‑fixed variables are returned.
     */
//     @Test
//     void testFreeVariablesFiltering() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // a – completely free variable
//         Variable a = model.addVariable().name("a");
//         // b – fixed by setting a level (lower == upper)
//         Variable b = model.addVariable().name("b").level(BigDecimal.TEN);
//         // c – free but with bounds (still not fixed)
//         Variable c = model.addVariable().name("c").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
//         // d – fixed because lower == upper (both 2)
//         Variable d = model.addVariable().name("d").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2));
//         List<Variable> free = model.getFreeVariables();
//         // Expected free variables are a and c
//         assertEquals(2, free.size(), "Exactly two variables should be free");
//         assertTrue(free.contains(a), "Variable a should be free");
//         assertTrue(free.contains(c), "Variable c should be free");
//         assertFalse(free.contains(b), "Variable b is fixed and must not appear");
//         assertFalse(free.contains(d), "Variable d is fixed and must not appear");
//     }

    /**
     * The list returned by {@code getFreeVariables()} must be unmodifiable.
     */
//     @Test
//     void testReturnedListIsUnmodifiable() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.addVariable().name("x");
//         List<Variable> free = model.getFreeVariables();
//         assertThrows(UnsupportedOperationException.class, () -> free.add(Variable.make("illegal")), "Attempting to modify the returned list should throw UnsupportedOperationException");
//     }

    /**
     * The unmodifiable list is a live view of the underlying collection.
     */
//     @Test
//     void testLiveViewReflectsModelChanges() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // initially one free variable
//         Variable first = model.addVariable().name("first");
//         List<Variable> viewBefore = model.getFreeVariables();
//         assertEquals(1, viewBefore.size());
//         // add another free variable
//         Variable second = model.addVariable().name("second");
//         // the same view object should now reflect the new size
//         assertEquals(2, viewBefore.size(), "The previously obtained view must reflect the added variable");
//         // a fresh call also returns a list of size 2
//         List<Variable> viewAfter = model.getFreeVariables();
//         assertEquals(2, viewAfter.size());
//         assertTrue(viewAfter.contains(first));
//         assertTrue(viewAfter.contains(second));
//     }

    /**
     * A model that only contains fixed variables must return an empty list.
     */
//     @Test
//     void testOnlyFixedVariables() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // fixed by level (lower == upper)
//         model.addVariable().name("x").level(BigDecimal.TEN);
//         // fixed by explicit lower/upper equality
//         model.addVariable().name("y").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2));
//         List<Variable> free = model.getFreeVariables();
//         assertTrue(free.isEmpty(), "Fixed variables must not appear in the free‑variables list");
//     }

    /**
     * A single free variable should be returned.
     */
//     @Test
//     void testSingleFreeVariable() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v = model.addVariable().name("x");
//         List<Variable> free = model.getFreeVariables();
//         assertEquals(1, free.size());
//         assertEquals(v, free.get(0));
//     }

    /**
     * The list returned by {@code getFreeVariables()} must be unmodifiable (add).
     */
//     @Test
//     void testReturnedListIsUnmodifiable_Add() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.addVariable().name("x");
//         List<Variable> free = model.getFreeVariables();
//         assertThrows(UnsupportedOperationException.class, () -> free.add(Variable.make("illegal")), "Attempting to modify the returned list should throw UnsupportedOperationException");
//     }

    /**
     * The list returned by {@code getFreeVariables()} must be unmodifiable (remove).
     */
//     @Test
//     void testReturnedListIsUnmodifiable_Remove() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.addVariable().name("x");
//         List<Variable> free = model.getFreeVariables();
//         assertThrows(UnsupportedOperationException.class, () -> free.remove(0), "Removing an element from the returned list must throw UnsupportedOperationException");
//     }

    /**
     * The list returned by {@code getFreeVariables()} must be unmodifiable (set).
     */
//     @Test
//     void testReturnedListIsUnmodifiable_Set() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.addVariable().name("x");
//         List<Variable> free = model.getFreeVariables();
//         assertThrows(UnsupportedOperationException.class, () -> free.set(0, Variable.make("other")), "Replacing an element in the returned list must throw UnsupportedOperationException");
//     }

    /**
     * After retrieving the free‑variables view, fixing a variable must remove it from the view.
     */
//     @Test
//     void testFixVariableAfterRetrieval() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable x = model.addVariable().name("x");
//         List<Variable> view = model.getFreeVariables();
//         assertEquals(1, view.size());
//         // fix the variable
//         x.fix(BigDecimal.ONE);
//         // a new call must now be empty; the previously obtained view is still unmodifiable,
//         // but its size must reflect the change because the underlying list changed.
//         List<Variable> afterFix = model.getFreeVariables();
//         assertTrue(afterFix.isEmpty(), "After fixing the only variable the free‑variables list must be empty");
//     }

    /**
     * Inject a null entry into the internal variable list and expect a NullPointerException.
     */
//     @Test
//     void testNullVariableInInternalList() throws Exception {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // add a normal variable so the internal list is initialised
//         model.addVariable().name("valid");
//         // use reflection to obtain the private myVariables field
//         Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
//         varField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         ArrayList<Variable> internal = (ArrayList<Variable>) varField.get(model);
//         // inject a null entry
//         internal.add(null);
//         assertThrows(NullPointerException.class, model::getFreeVariables, "A null entry in the variable list must cause a NullPointerException");
//     }

    /**
     * A variable that reports an upper limit is set but returns null must cause NPE.
     */
//     @Test
//     void testVariableWithNullUpperLimit() {
//         // Create a regular variable and then corrupt its internal state via reflection
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v = model.addVariable().name("corrupt");
//         // Force the variable to think its upper limit is set while the actual limit is null
//         Field upperSetField = Variable.class.getDeclaredField("myUpperLimitSet");
//         upperSetField.setAccessible(true);
//         upperSetField.setBoolean(v, true);
//         Field upperField = Variable.class.getDeclaredField("myUpperLimit");
//         upperField.setAccessible(true);
//         upperField.set(v, null);
//         // Ensure lower limit is not set to avoid being considered fixed
//         Field lowerSetField = Variable.class.getDeclaredField("myLowerLimitSet");
//         lowerSetField.setAccessible(true);
//         lowerSetField.setBoolean(v, false);
//         assertThrows(NullPointerException.class, model::getFreeVariables, "Variable with null upper limit must trigger NullPointerException");
//     }

    /**
     * A variable that reports a lower limit is set but returns null must cause NPE.
     */
//     @Test
//     void testVariableWithNullLowerLimit() {
//         // Create a regular variable and then corrupt its internal state via reflection
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v = model.addVariable().name("corrupt");
//         // Force the variable to think its lower limit is set while the actual limit is null
//         Field lowerSetField = Variable.class.getDeclaredField("myLowerLimitSet");
//         lowerSetField.setAccessible(true);
//         lowerSetField.setBoolean(v, true);
//         Field lowerField = Variable.class.getDeclaredField("myLowerLimit");
//         lowerField.setAccessible(true);
//         lowerField.set(v, null);
//         // Ensure upper limit is not set to avoid being considered fixed
//         Field upperSetField = Variable.class.getDeclaredField("myUpperLimitSet");
//         upperSetField.setAccessible(true);
//         upperSetField.setBoolean(v, false);
//         assertThrows(NullPointerException.class, model::getFreeVariables, "Variable with null lower limit must trigger NullPointerException");
//     }

    /**
     * Setting the internal variable list to null must cause NPE when getFreeVariables() is called.
     */
//     @Test
//     void testInternalVariableListSetToNull() throws Exception {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Use reflection to set myVariables to null
//         Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
//         varField.setAccessible(true);
//         varField.set(model, null);
//         assertThrows(NullPointerException.class, model::getFreeVariables, "Calling getFreeVariables() when internal list is null must throw NullPointerException");
//     }

    /**
     * Adding variables after the first call to getFreeVariables() must be reflected in the same view.
     */
//     @Test
//     void testViewReflectsLaterAdditions() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable a = model.addVariable().name("a");
//         Variable b = model.addVariable().name("b");
//         List<Variable> view = model.getFreeVariables();
//         assertEquals(2, view.size());
//         // add a third variable
//         Variable c = model.addVariable().name("c");
//         // the previously obtained view must now contain three elements
//         assertEquals(3, view.size());
//         assertTrue(view.contains(c));
//     }

    /**
     * Verify that free variables list preserves insertion order when mixed with integer/positive variables.
     */
//     @Test
//     void testOrderPreservationWithMixedTypes() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // a – free
//         Variable a = model.addVariable().name("a");
//         // b – fixed
//         model.addVariable().name("b").level(BigDecimal.ONE);
//         // c – free integer
//         Variable c = model.addVariable().name("c").integer(true);
//         // d – free with positive upper limit
//         Variable d = model.addVariable().name("d").upper(BigDecimal.valueOf(10));
//         List<Variable> free = model.getFreeVariables();
//         // Expected order: a, c, d (b is fixed)
//         assertEquals(List.of(a, c, d), free, "Free variables must be returned in insertion order");
//     }
}