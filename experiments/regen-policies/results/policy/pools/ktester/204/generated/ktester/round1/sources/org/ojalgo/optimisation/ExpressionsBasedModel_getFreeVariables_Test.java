package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;

class ExpressionsBasedModel_getFreeVariables_Test {

    /**
     * The model used in the tests.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly discard the model reference – helps the GC in long test suites.
        model = null;
    }

    /**
     * Clears the internal variable list – useful for tests that inject malformed data.
     */
    private void clearInternalVariableList() throws Exception {
        getInternalVariableList().clear();
    }

    /**
     * Verify that the list returned by {@code getFreeVariables()} contains exactly the
     * variables that are not fixed and that the list is unmodifiable.
     */
    @Test
    void testGetFreeVariablesContentsAndUnmodifiable() {
        // ----- arrange -------------------------------------------------------
        // varFree1 – completely free (no bounds)
        Variable varFree1 = model.newVariable("free1");
        // varFixed – fixed by setting a level (both lower & upper become the same)
        Variable varFixed = model.newVariable("fixed").level(BigDecimal.valueOf(5));
        // varFree2 – has an upper bound but still free because lower bound is not set
        Variable varFree2 = model.newVariable("free2").upper(BigDecimal.valueOf(10));
        // varIntegerFree – integer variable, still free because not fixed
        Variable varIntegerFree = model.newVariable("intFree").integer(true);
        // ----- act -----------------------------------------------------------
        List<Variable> freeVars = model.getFreeVariables();
        // ----- assert --------------------------------------------------------
        // The list must contain the three free variables and not the fixed one.
        assertTrue(freeVars.contains(varFree1), "Free list should contain varFree1");
        assertTrue(freeVars.contains(varFree2), "Free list should contain varFree2");
        assertTrue(freeVars.contains(varIntegerFree), "Free list should contain varIntegerFree");
        assertFalse(freeVars.contains(varFixed), "Free list must not contain the fixed variable");
        // The size must be exactly 3.
        assertEquals(3, freeVars.size(), "Unexpected number of free variables");
        // The returned list must be unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> freeVars.add(varFixed), "The free‑variables list should be unmodifiable");
    }

    /**
     * Verify that the list reflects subsequent changes to the model because the
     * returned list is a view of the internal collection.
     */
    @Test
    void testFreeVariablesListReflectsModelChanges() {
        // ----- arrange -------------------------------------------------------
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2").level(BigDecimal.ONE);
        List<Variable> freeBefore = model.getFreeVariables();
        assertEquals(1, freeBefore.size(), "Initially only v1 should be free");
        // ----- act -----------------------------------------------------------
        Variable v3 = model.newVariable("v3").upper(BigDecimal.valueOf(7));
        // ----- assert --------------------------------------------------------
        assertEquals(2, freeBefore.size(), "List should reflect newly added free variable");
        assertTrue(freeBefore.contains(v3), "List should contain the newly added free variable v3");
    }

    /**
     * Retrieves the private {@code myVariables} field from the model.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Variable> getInternalVariableList() throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        return (ArrayList<Variable>) f.get(model);
    }

    @Test
    void testEmptyModelReturnsEmptyFreeList() {
        List<Variable> free = model.getFreeVariables();
        assertTrue(free.isEmpty(), "Free‑variables list must be empty when no variables exist");
    }

    @Test
    void testSingleFixedVariableIsNotReturned() {
        model.newVariable("fixed").level(BigDecimal.valueOf(3));
        List<Variable> free = model.getFreeVariables();
        assertTrue(free.isEmpty(), "Fixed variable must not appear in free‑variables list");
    }

    @Test
    void testSingleFreeVariableNoLimits() {
        Variable v = model.newVariable("free");
        List<Variable> free = model.getFreeVariables();
        assertEquals(Collections.singletonList(v), free, "Variable without limits should be free");
    }

    @Test
    void testFreeVariableWithNegativeUpperLimitIsFree() {
        Variable v = model.newVariable("negUpper").upper(BigDecimal.valueOf(-5));
        List<Variable> free = model.getFreeVariables();
        assertEquals(Collections.singletonList(v), free, "Variable with negative upper limit is still free");
    }

    @Test
    void testFreeVariableWithPositiveLowerLimitIsFree() {
        Variable v = model.newVariable("posLower").lower(BigDecimal.valueOf(5));
        List<Variable> free = model.getFreeVariables();
        assertEquals(Collections.singletonList(v), free, "Variable with positive lower limit is still free");
    }

    @Test
    void testFreeIntegerVariableIsReturned() {
        Variable v = model.newVariable("intFree").integer(true);
        List<Variable> free = model.getFreeVariables();
        assertEquals(Collections.singletonList(v), free, "Free integer variable must appear in free‑variables list");
    }

    @Test
    void testMixtureOfFreeAndFixedVariablesPreservesOrder() {
        // order of addition matters
        Variable f1 = model.newVariable("f1");
        Variable f2 = model.newVariable("f2").upper(BigDecimal.ONE);
        Variable f3 = model.newVariable("f3").lower(BigDecimal.ONE.negate());
        Variable fixed1 = model.newVariable("fixed1").level(BigDecimal.TEN);
        Variable fixed2 = model.newVariable("fixed2").level(BigDecimal.ZERO);
        Variable f4 = model.newVariable("f4").upper(BigDecimal.valueOf(-2));
        List<Variable> expected = List.of(f1, f2, f3, f4);
        List<Variable> actual = model.getFreeVariables();
        assertEquals(expected, actual, "Free‑variables list must contain only free variables in the order added");
    }

    @Test
    void testGetFreeVariablesUnmodifiableAddThrows() {
        Variable v = model.newVariable("free");
        List<Variable> free = model.getFreeVariables();
        assertThrows(UnsupportedOperationException.class, () -> free.add(v), "Adding to the free‑variables list must throw UnsupportedOperationException");
    }

    @Test
    void testFreeVariablesListReflectsNewVariableAfterFirstCall() {
        Variable v1 = model.newVariable("v1");
        List<Variable> first = model.getFreeVariables();
        Variable v2 = model.newVariable("v2");
        List<Variable> second = model.getFreeVariables();
        assertEquals(2, second.size(), "Second call must see the newly added free variable");
        assertTrue(second.contains(v2), "Newly added variable must be present");
    }

    @Test
    void testFreeVariableWithUpperLimitZeroIsReturned() {
        Variable v = model.newVariable("zeroUpper").upper(BigDecimal.ZERO);
        List<Variable> free = model.getFreeVariables();
        assertEquals(Collections.singletonList(v), free, "Variable with upper limit zero must still be free");
    }

    @Test
    void testFreeVariableWithLowerLimitZeroIsReturned() {
        Variable v = model.newVariable("zeroLower").lower(BigDecimal.ZERO);
        List<Variable> free = model.getFreeVariables();
        assertEquals(Collections.singletonList(v), free, "Variable with lower limit zero must still be free");
    }

    @Test
    void testFixedIntegerVariableIsNotReturned() {
        model.newVariable("intFixed").integer(true).level(BigDecimal.valueOf(7));
        List<Variable> free = model.getFreeVariables();
        assertTrue(free.isEmpty(), "Fixed integer variable must not be reported as free");
    }

    @Test
    void testCachingReturnsSameInstanceIfModelUnchanged() {
        model.newVariable("a");
        List<Variable> first = model.getFreeVariables();
        List<Variable> second = model.getFreeVariables();
        assertSame(first, second, "Repeated calls without model changes must return the same list instance");
    }

    @Test
    void testUnmodifiableListRemoveThrows() {
        model.newVariable("x");
        List<Variable> free = model.getFreeVariables();
        assertThrows(UnsupportedOperationException.class, () -> free.remove(0), "Removing from the free‑variables list must throw UnsupportedOperationException");
    }

    @Test
    void testVariableFixedAfterFirstCallInvalidatesCache() {
        Variable v = model.newVariable("v");
        List<Variable> first = model.getFreeVariables();
        v.level(BigDecimal.ONE);
        List<Variable> second = model.getFreeVariables();
        assertTrue(second.isEmpty(), "After fixing, free‑variables list must be empty");
        assertNotSame(first, second, "Cache must be refreshed after variable state changes");
    }

    @Test
    void testFourFreeVariablesMixedLimitsPreserveOrder() {
        Variable vA = model.newVariable("vA");
        Variable vB = model.newVariable("vB").upper(BigDecimal.valueOf(-3));
        Variable vC = model.newVariable("vC").lower(BigDecimal.valueOf(4));
        Variable vD = model.newVariable("vD").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        List<Variable> expected = List.of(vA, vB, vC, vD);
        List<Variable> actual = model.getFreeVariables();
        assertEquals(expected, actual, "Free‑variables list must preserve the order of addition for mixed configurations");
    }

    @Test
    void testNullVariableInModelThrowsNPE() throws Exception {
        // Insert a null entry directly into the internal list
        ArrayList<Variable> internal = getInternalVariableList();
        internal.add(null);
        assertThrows(NullPointerException.class, () -> model.getFreeVariables(), "Iterating over a null variable must throw NullPointerException");
    }

//     @Test
//     void testVariableWithNullUpperLimitThrowsNPE() throws Exception {
//         Variable mock = Mockito.mock(Variable.class);
//         Mockito.when(mock.isFixed()).thenReturn(false);
//         Mockito.when(mock.isUpperLimitSet()).thenReturn(true);
//         Mockito.when(mock.getUpperLimit()).thenReturn(null);
//         Mockito.when(mock.isLowerLimitSet()).thenReturn(false);
//         Mockito.when(mock.isInteger()).thenReturn(false);
//         // Insert mock into model
//         getInternalVariableList().add(mock);
//         assertThrows(NullPointerException.class, () -> model.getFreeVariables(), "Null upper limit should cause NullPointerException");
//     }

//     @Test
//     void testVariableWithNullLowerLimitThrowsNPE() throws Exception {
//         Variable mock = Mockito.mock(Variable.class);
//         Mockito.when(mock.isFixed()).thenReturn(false);
//         Mockito.when(mock.isUpperLimitSet()).thenReturn(false);
//         Mockito.when(mock.isLowerLimitSet()).thenReturn(true);
//         Mockito.when(mock.getLowerLimit()).thenReturn(null);
//         Mockito.when(mock.isInteger()).thenReturn(false);
//         // Insert mock into model
//         getInternalVariableList().add(mock);
//         assertThrows(NullPointerException.class, () -> model.getFreeVariables(), "Null lower limit should cause NullPointerException");
//     }

//     @Test
//     void testConcurrentModificationDuringIterationThrowsCME() throws Exception {
//         // Create a mock variable whose isFixed() method adds another variable to the model.
//         Variable trigger = new Variable() {
// 
//             @Override
//             public boolean isFixed() {
//                 // Adding a variable while the original list is being iterated.
//                 model.newVariable("concurrent");
//                 return false;
//             }
//         };
//         clearInternalVariableList();
//         getInternalVariableList().add(trigger);
//         // Add a second normal variable to guarantee iteration proceeds past the first.
//         model.newVariable("normal");
//         // The concurrent modification should surface as a ConcurrentModificationException.
//         assertThrows(ConcurrentModificationException.class, () -> model.getFreeVariables(), "Concurrent modification during getFreeVariables() must throw CME");
//     }

    @Test
    void testRepeatedCallsWithoutChangesReturnSameOrderedList() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        List<Variable> first = model.getFreeVariables();
        List<Variable> second = model.getFreeVariables();
        assertSame(first, second, "Same instance expected on repeated calls");
        assertEquals(List.of(a, b), second, "Ordering must stay consistent");
    }
}