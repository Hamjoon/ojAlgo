package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_bounds_Test {

    private ExpressionsBasedModel model;

    // lower == upper -> fixed, should be filtered out
    private Variable varFixed;

    // only lower bound -> should be present
    private Variable varLower;

    // only upper bound -> should be present
    private Variable varUpper;

    // both bounds, not equal -> should be present
    private Variable varBoth;

    // no bounds -> not a constraint, filtered out
    private Variable varFree;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // Fixed variable (lower == upper)
        varFixed = model.newVariable("fixed").lower(5).upper(5).weight(0);
        // Variable with only a lower bound
        varLower = model.newVariable("lower").lower(1).weight(1);
        // Variable with only an upper bound
        varUpper = model.newVariable("upper").upper(10).weight(1);
        // Variable with both bounds, but not fixed (lower < upper)
        varBoth = model.newVariable("both").lower(2).upper(8).weight(1);
        // Variable with no bounds at all (free)
        varFree = model.newVariable("free").weight(1);
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    @Test
    void testBoundsFiltersCorrectly() {
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        // Expected variables: lower, upper, both
        Assertions.assertEquals(3, bounds.size(), "bounds() should return exactly three variables");
        Assertions.assertTrue(bounds.contains(varLower), "Variable with only lower bound must be present");
        Assertions.assertTrue(bounds.contains(varUpper), "Variable with only upper bound must be present");
        Assertions.assertTrue(bounds.contains(varBoth), "Variable with both bounds must be present");
        // Fixed and free variables must be absent
        Assertions.assertFalse(bounds.contains(varFixed), "Fixed variable must be filtered out");
        Assertions.assertFalse(bounds.contains(varFree), "Variable without any bound must be filtered out");
    }

    @Test
    void testBoundsStreamCanBeReused() {
        List<Variable> first = model.bounds().collect(Collectors.toList());
        List<Variable> second = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(first, second, "Repeated calls to bounds() must yield the same collection");
    }

    @Test
    void testBoundsDoesNotExposeFixedVariablesEvenWhenBothBoundsSet() {
        Variable alsoFixed = model.newVariable("alsoFixed").lower(3).upper(3).weight(0);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertFalse(bounds.contains(alsoFixed), "A variable with equal lower/upper bounds must be excluded");
    }

    @Test
    void testOnlyConstraintNotFixedIsReturned() {
        Variable c1 = model.newVariable("c1").lower(1).weight(1);
        Variable c2 = model.newVariable("c2").lower(2).upper(2).weight(0);
        Variable n1 = model.newVariable("n1").weight(1);
        Variable n2 = model.newVariable("n2").lower(5).upper(5).weight(0);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(1, bounds.size(), "Only one variable satisfies (constraint && !fixed)");
        Assertions.assertTrue(bounds.contains(c1), "c1 must be present");
        Assertions.assertFalse(bounds.contains(c2), "c2 is fixed and must be filtered out");
        Assertions.assertFalse(bounds.contains(n1), "n1 is not a constraint and must be filtered out");
        Assertions.assertFalse(bounds.contains(n2), "n2 is fixed and must be filtered out");
    }

    @Test
    void testEmptyWhenNoVariableMatches() {
        model.newVariable("v1").lower(3).upper(3).weight(0);
        model.newVariable("v2").weight(1);
        model.newVariable("v3").lower(5).upper(5).weight(0);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertTrue(bounds.isEmpty(), "No variable satisfies the filter, stream must be empty");
    }

    @Test
    void testMultipleConstraintsReturnedInInsertionOrder() {
        Variable c1 = model.newVariable("c1").lower(1).weight(1);
        Variable c2 = model.newVariable("c2").upper(10).weight(1);
        Variable f1 = model.newVariable("f1").lower(4).upper(4).weight(0);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(2, bounds.size(), "Two variables satisfy the filter");
        Assertions.assertEquals(Arrays.asList(c1, c2), bounds, "Order must follow insertion order");
    }

    @Test
    void testEmptyWhenModelHasNoVariables() {
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        List<Variable> bounds = emptyModel.bounds().collect(Collectors.toList());
        Assertions.assertTrue(bounds.isEmpty(), "Empty model must yield an empty stream");
    }

    @Test
    void testDynamicStateChangeFixedToNotFixed() {
        Variable v1 = model.newVariable("v1").lower(1).weight(1);
        Variable v2 = model.newVariable("v2").lower(2).upper(2).weight(0);
        v2.upper(5);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertTrue(bounds.contains(v1), "v1 must be present");
        Assertions.assertTrue(bounds.contains(v2), "v2 became not‑fixed and must now be present");
    }

    @Test
    void testVariableBecomesConstraintAfterCreation() {
        Variable v = model.newVariable("v");
        Assertions.assertTrue(model.bounds().collect(Collectors.toList()).isEmpty(), "Initially no constraints");
        v.lower(3);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(1, bounds.size(), "After adding a lower bound the variable becomes a constraint");
        Assertions.assertTrue(bounds.contains(v), "Variable must now appear in bounds()");
    }

    @Test
    void testVariableBecomesUnfixedAfterBeingFixed() {
        Variable v = model.newVariable("v").lower(4).upper(4).weight(0);
        Assertions.assertTrue(model.bounds().collect(Collectors.toList()).isEmpty(), "Fixed variable must not appear");
        v.upper(10);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(1, bounds.size(), "After changing upper bound the variable is no longer fixed");
        Assertions.assertTrue(bounds.contains(v), "Variable must now appear in bounds()");
    }

    @Test
    void testRemovalOfVariableIsReflectedInBounds() throws Exception {
        Variable v1 = model.newVariable("v1").lower(1).weight(1);
        Variable v2 = model.newVariable("v2").lower(2).weight(1);
        List<Variable> before = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(2, before.size());
        List<Variable> internal = getInternalVariableList();
        Assertions.assertTrue(internal.remove(v1), "Variable v1 should be removed from internal list");
        List<Variable> after = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(1, after.size());
        Assertions.assertFalse(after.contains(v1), "Removed variable must not be present");
        Assertions.assertTrue(after.contains(v2), "Remaining variable must still be present");
    }

    @Test
    void testFreshStreamReflectsNewlyAddedVariable() {
        Variable v1 = model.newVariable("v1").lower(1).weight(1);
        List<Variable> first = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(1, first.size());
        Variable v2 = model.newVariable("v2").lower(2).weight(1);
        List<Variable> second = model.bounds().collect(Collectors.toList());
        Assertions.assertEquals(2, second.size());
        Assertions.assertTrue(second.containsAll(Arrays.asList(v1, v2)));
    }

    @Test
    void testLazyEvaluationReflectsStateChangesBeforeTerminalOperation() {
        Variable v = model.newVariable("v");
        Stream<Variable> lazyStream = model.bounds();
        v.lower(5);
        v.upper(10);
        List<Variable> collected = lazyStream.collect(Collectors.toList());
        Assertions.assertEquals(1, collected.size(), "Lazy stream must evaluate the latest state");
        Assertions.assertTrue(collected.contains(v), "Variable must be present after it became a constraint");
    }

    @Test
    void testConcurrentModificationDuringTerminalOperation() {
        Variable v = model.newVariable("v").lower(1).weight(1);
        Stream<Variable> stream = model.bounds();
        model.newVariable("new").lower(2).weight(1);
        Assertions.assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Adding a variable while a stream is being consumed should trigger CME");
    }

    @Test
    void testBoundsAfterModelDisposedThrowsIllegalStateException() {
        model.dispose();
        Assertions.assertThrows(IllegalStateException.class, () -> model.bounds(), "Calling bounds() on a disposed model must throw IllegalStateException");
    }

    @Test
    void testNullVariableInModelCausesNullPointerException() throws Exception {
        List<Variable> internal = getInternalVariableList();
        internal.add(null);
        Assertions.assertThrows(NullPointerException.class, () -> model.bounds().collect(Collectors.toList()), "A null variable in the collection must cause NPE when bounds() processes it");
    }

    @Test
    void testVariableThrowingRuntimeExceptionPropagates() throws Exception {
        // Create a mock Variable that throws when isConstraint() is called
        Variable badVar = mock(Variable.class);
        when(badVar.isConstraint()).thenThrow(new IllegalArgumentException("boom"));
        // Add the mock to the internal list
        List<Variable> internal = getInternalVariableList();
        internal.add(badVar);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> model.bounds().collect(Collectors.toList()), "Exception from Variable.isConstraint() must propagate unchanged");
        assertEquals("boom", thrown.getMessage());
    }

    /**
     * Retrieves the internal {@code List<Variable>} used by {@link ExpressionsBasedModel}
     * via reflection. The exact field name is not part of the public API, therefore we
     * search for the first field of type {@code List} whose generic type is {@code Variable}.
     */
    @SuppressWarnings("unchecked")
    private List<Variable> getInternalVariableList() throws Exception {
        for (Field f : model.getClass().getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object value = f.get(model);
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        Object first = list.get(0);
                        if (first == null || first instanceof Variable) {
                            return (List<Variable>) list;
                        }
                    } else {
                        // Empty list – we cannot infer the generic type, but assume it is the variable list
                        return (List<Variable>) list;
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to locate internal variable list via reflection");
    }
}