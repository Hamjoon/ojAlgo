package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getFixedVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    @Test
    void testSingleFixedVariable() {
        Variable fixed = model.newVariable("fixed").level(5).weight(1);
        Variable free = model.newVariable("free").lower(0).upper(10).weight(1);
        Set<IntIndex> fixedSet = model.getFixedVariables();
        Assertions.assertEquals(1, fixedSet.size(), "Only the variable with equal lower/upper bounds should be reported as fixed.");
        Assertions.assertTrue(fixedSet.contains(IntIndex.of(model.indexOf(fixed))), "The fixed variable's index should be present in the returned set.");
        Assertions.assertFalse(fixedSet.contains(IntIndex.of(model.indexOf(free))), "A non‑fixed variable must not appear in the returned set.");
    }

    @Test
    void testMultipleFixedVariables() {
        Variable f1 = model.newVariable("f1").level(3).weight(1);
        Variable f2 = model.newVariable("f2").level(7).weight(1);
        Variable notFixed = model.newVariable("n").lower(0).upper(5).weight(1);
        Set<IntIndex> fixedSet = model.getFixedVariables();
        Assertions.assertEquals(2, fixedSet.size(), "Two variables are fixed; the set size must reflect that.");
        Assertions.assertTrue(fixedSet.contains(IntIndex.of(model.indexOf(f1))));
        Assertions.assertTrue(fixedSet.contains(IntIndex.of(model.indexOf(f2))));
        Assertions.assertFalse(fixedSet.contains(IntIndex.of(model.indexOf(notFixed))));
    }

    @Test
    void testReturnedSetIsUnmodifiable() {
        model.newVariable("x").level(1);
        Set<IntIndex> fixedSet = model.getFixedVariables();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fixedSet.add(IntIndex.of(0)), "The set returned by getFixedVariables() must be unmodifiable.");
    }

    @Test
    void testDynamicUpdateAfterInitialCall() {
        Variable firstFixed = model.newVariable("first").level(2);
        // First call – should contain only the first variable.
        Set<IntIndex> firstCall = model.getFixedVariables();
        Assertions.assertEquals(1, firstCall.size());
        Assertions.assertTrue(firstCall.contains(IntIndex.of(model.indexOf(firstFixed))));
        // Add another fixed variable after the first call.
        Variable secondFixed = model.newVariable("second").level(4);
        // Second call – must now contain both variables.
        Set<IntIndex> secondCall = model.getFixedVariables();
        Assertions.assertEquals(2, secondCall.size(), "After adding another fixed variable the returned set must reflect the new state.");
        Assertions.assertTrue(secondCall.contains(IntIndex.of(model.indexOf(firstFixed))));
        Assertions.assertTrue(secondCall.contains(IntIndex.of(model.indexOf(secondFixed))));
    }

    @Test
    void testNoVariables() {
        // Model has never had a variable added.
        Set<IntIndex> result = model.getFixedVariables();
        assertTrue(result.isEmpty(), "Empty model must return an empty set.");
        assertThrows(UnsupportedOperationException.class, () -> result.add(IntIndex.of(0)), "Returned set must be unmodifiable even when empty.");
    }

    @Test
    void testNoFixedVariables() {
        // Add several free variables (different lower/upper bounds).
        model.newVariable("a").lower(0).upper(10);
        model.newVariable("b").lower(-5).upper(5);
        model.newVariable("c").lower(2).upper(8);
        Set<IntIndex> result = model.getFixedVariables();
        assertTrue(result.isEmpty(), "When no variable is fixed the set must be empty.");
    }

    @Test
    void testSomeFixedVariables() {
        // Index 0, 1, 2 will be created in this order.
        // fixed
        Variable v0 = model.newVariable("v0").level(1);
        // not fixed
        model.newVariable("v1").lower(0).upper(10);
        // fixed
        Variable v2 = model.newVariable("v2").level(3);
        Set<IntIndex> result = model.getFixedVariables();
        // Expected indices are 0 and 2.
        Set<IntIndex> expected = new HashSet<>(Arrays.asList(IntIndex.of(model.indexOf(v0)), IntIndex.of(model.indexOf(v2))));
        assertEquals(expected, result, "Only the fixed variables (indices 0 and 2) should be reported.");
    }

    @Test
    void testAllFixedVariables() {
        Variable v0 = model.newVariable("v0").level(5);
        Variable v1 = model.newVariable("v1").level(7);
        Set<IntIndex> result = model.getFixedVariables();
        Set<IntIndex> expected = new HashSet<>(Arrays.asList(IntIndex.of(model.indexOf(v0)), IntIndex.of(model.indexOf(v1))));
        assertEquals(expected, result, "All variables are fixed; the set must contain both indices.");
    }

    @Test
    void testDynamicFixing() {
        // not fixed initially
        Variable v = model.newVariable("v").lower(0).upper(10);
        Set<IntIndex> first = model.getFixedVariables();
        assertTrue(first.isEmpty(), "Initially the variable is not fixed, so the set must be empty.");
        // Now fix the variable by setting a level (lower == upper)
        v.level(3);
        Set<IntIndex> second = model.getFixedVariables();
        assertEquals(1, second.size(), "After fixing the variable the set must contain exactly one index.");
        assertTrue(second.contains(IntIndex.of(model.indexOf(v))));
    }

    @Test
    void testClearingBetweenCalls() {
        // fixed
        Variable v0 = model.newVariable("v0").level(1);
        // not fixed
        model.newVariable("v1").lower(0).upper(10);
        Set<IntIndex> first = model.getFixedVariables();
        assertEquals(1, first.size());
        assertTrue(first.contains(IntIndex.of(model.indexOf(v0))));
        // Fix the second variable as well.
        Variable v1 = model.getVariable(1);
        v1.level(5);
        Set<IntIndex> second = model.getFixedVariables();
        assertEquals(2, second.size(), "After fixing the second variable the returned set must contain both indices.");
    }

    @Test
    void testNullVariableInListThrowsNPE() throws Exception {
        // Inject a null entry into the private myVariables list via reflection.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) varsField.get(model);
        // deliberate corruption
        vars.add(null);
        // The method iterates over the list and calls isFixed() on each entry,
        // which should now throw a NullPointerException.
        assertThrows(NullPointerException.class, () -> model.getFixedVariables(), "A null entry in the internal variable list must cause NPE.");
    }

    @Test
    void testConcurrentModificationDuringIteration() throws Exception {
        // Create a mock Variable whose isFixed() method mutates the model's variable list.
        Variable mutatingMock = Mockito.mock(Variable.class);
        Mockito.when(mutatingMock.isFixed()).thenAnswer(invocation -> {
            // Adding a new variable while the original list is being iterated.
            model.newVariable("addedDuringIteration");
            // not fixed – we only care about the side‑effect.
            return false;
        });
        // Ensure getIndex() would not be called (since isFixed returns false).
        Mockito.when(mutatingMock.getIndex()).thenReturn(IntIndex.of(-1));
        // Insert the mock into the internal list via reflection.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) varsField.get(model);
        vars.add(mutatingMock);
        // Expect a ConcurrentModificationException because the underlying list is structurally
        // modified during iteration inside getFixedVariables().
        assertThrows(ConcurrentModificationException.class, () -> model.getFixedVariables(), "Modifying the variable list during iteration must trigger CME.");
    }
}