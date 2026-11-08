package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;

class ExpressionsBasedModel_getVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        model = null;
    }

    @Test
    void testGetVariablesInitiallyEmpty() {
        List<Variable> vars = model.getVariables();
        assertNotNull(vars, "Returned list must not be null");
        assertTrue(vars.isEmpty(), "New model should have no variables");
    }

    @Test
    void testGetVariablesReflectsAddedVariablesInOrder() {
        // name not required for the test
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        List<Variable> vars = model.getVariables();
        assertEquals(3, vars.size(), "List size should match number of added variables");
        assertSame(v1, vars.get(0), "First variable should be the first added");
        assertSame(v2, vars.get(1), "Second variable should be the second added");
        assertSame(v3, vars.get(2), "Third variable should be the third added");
    }

    @Test
    void testGetVariablesIsUnmodifiable() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        Variable dummy = new Variable("dummy", 0);
        assertThrows(UnsupportedOperationException.class, () -> vars.add(dummy), "The list returned by getVariables() must be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> vars.remove(0), "The list returned by getVariables() must be unmodifiable");
    }

    @Test
    void testModifyingReturnedListDoesNotAffectModel() {
        Variable v = model.addVariable();
        List<Variable> vars = model.getVariables();
        // Subsequent calls should return the same unmodifiable view
        List<Variable> secondCall = model.getVariables();
        assertSame(vars, secondCall, "Subsequent calls should return the same unmodifiable view");
        assertEquals(1, secondCall.size());
        assertSame(v, secondCall.get(0));
    }

    @Test
    void testSingleVariableAdded() {
        Variable v = model.addVariable();
        List<Variable> vars = model.getVariables();
        assertEquals(1, vars.size(), "List should contain exactly one element");
        assertSame(v, vars.get(0), "The element must be the variable that was added");
    }

    @Test
    void testViewReflectsUnderlyingChanges() {
        Variable v1 = model.addVariable();
        List<Variable> view = model.getVariables();
        // Add another variable after the view has been obtained
        Variable v2 = model.addVariable();
        // The view must now contain the newly added variable as well
        assertEquals(2, view.size(), "View should reflect the added variable");
        assertSame(v2, view.get(1), "Second element in view must be the newly added variable");
    }

    @TestFactory
    Stream<DynamicTest> unmodifiableBulkOperations() {
        // initially empty view
        List<Variable> vars = model.getVariables();
        Variable dummy = new Variable("dummy", 0);
        return Stream.of(DynamicTest.dynamicTest("addAll", () -> assertThrows(UnsupportedOperationException.class, () -> vars.addAll(Collections.singletonList(dummy)))), DynamicTest.dynamicTest("removeAll", () -> assertThrows(UnsupportedOperationException.class, () -> vars.removeAll(Collections.singleton(dummy)))), DynamicTest.dynamicTest("retainAll", () -> assertThrows(UnsupportedOperationException.class, () -> vars.retainAll(Collections.singleton(dummy)))), DynamicTest.dynamicTest("clear", () -> assertThrows(UnsupportedOperationException.class, vars::clear)), DynamicTest.dynamicTest("set(int, E)", () -> assertThrows(UnsupportedOperationException.class, () -> vars.set(0, dummy))), DynamicTest.dynamicTest("add(int, E)", () -> assertThrows(UnsupportedOperationException.class, () -> vars.add(0, dummy))));
    }

    @Test
    void testIteratorRemoveUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        Iterator<Variable> it = vars.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // move to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must be unsupported on the unmodifiable list");
    }

    @Test
    void testListIteratorSetUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        ListIterator<Variable> li = vars.listIterator();
        assertTrue(li.hasNext(), "ListIterator should have at least one element");
        // advance to first element
        li.next();
        Variable dummy = new Variable("dummy", 0);
        assertThrows(UnsupportedOperationException.class, () -> li.set(dummy), "ListIterator.set() must be unsupported on the unmodifiable list");
    }

    @Test
    void testListIteratorAddUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        ListIterator<Variable> li = vars.listIterator();
        Variable dummy = new Variable("dummy", 0);
        assertThrows(UnsupportedOperationException.class, () -> li.add(dummy), "ListIterator.add() must be unsupported on the unmodifiable list");
    }

    @Test
    void testSubListUnmodifiableAdd() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        List<Variable> sub = vars.subList(0, 1);
        Variable dummy = new Variable("dummy", 0);
        assertThrows(UnsupportedOperationException.class, () -> sub.add(dummy), "subList.add() must be unsupported");
    }

    @Test
    void testSubListUnmodifiableClear() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        List<Variable> sub = vars.subList(0, 1);
        assertThrows(UnsupportedOperationException.class, sub::clear, "subList.clear() must be unsupported");
    }

    @Test
    void testSubListRemoveByIndexUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        List<Variable> sub = vars.subList(0, 1);
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(0), "subList.remove(int) must be unsupported");
    }

    @Test
    void testSubListRemoveObjectUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        List<Variable> sub = vars.subList(0, 1);
        Variable v = vars.get(0);
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(v), "subList.remove(Object) must be unsupported");
    }

    @Test
    void testDirectRemoveObjectUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        Variable v = vars.get(0);
        assertThrows(UnsupportedOperationException.class, () -> vars.remove(v), "remove(Object) must be unsupported");
    }

    @Test
    void testRemoveByIndexUnsupported() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(UnsupportedOperationException.class, () -> vars.remove(0), "remove(int) must be unsupported");
    }

    @Test
    void testListContentMatchesAddedVariables() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        List<Variable> vars = model.getVariables();
        // size & order
        assertEquals(3, vars.size(), "List size must equal number of added variables");
        assertSame(v1, vars.get(0));
        assertSame(v2, vars.get(1));
        assertSame(v3, vars.get(2));
        // contains()
        assertTrue(vars.contains(v1));
        assertTrue(vars.contains(v2));
        assertTrue(vars.contains(v3));
        assertFalse(vars.contains(new Variable("notAdded", 0)), "List must not contain a variable that was never added");
        // indexOf()
        assertEquals(0, vars.indexOf(v1));
        assertEquals(1, vars.indexOf(v2));
        assertEquals(2, vars.indexOf(v3));
        assertEquals(-1, vars.indexOf(new Variable("notAdded", 0)));
    }

    @Test
    void testGetNegativeIndexThrows() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.get(-1), "Accessing a negative index must throw IndexOutOfBoundsException");
    }

    @Test
    void testGetIndexEqualToSizeThrows() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        int size = vars.size();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.get(size), "Accessing index == size must throw IndexOutOfBoundsException");
    }

    @Test
    void testSubListFromGreaterThanToThrows() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IllegalArgumentException.class, () -> vars.subList(2, 1), "subList(toIndex, fromIndex) with fromIndex > toIndex must throw IllegalArgumentException");
    }

    @Test
    void testSubListNegativeFromIndexThrows() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.subList(-1, 0), "subList with negative fromIndex must throw IndexOutOfBoundsException");
    }

    @Test
    void testSubListToIndexBeyondSizeThrows() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        int size = vars.size();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.subList(0, size + 1), "subList with toIndex > size must throw IndexOutOfBoundsException");
    }

    @Test
    void testIteratorDetectsConcurrentModification() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        Iterator<Variable> it = vars.iterator();
        // structural change after iterator creation
        model.addVariable();
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must be fail‑fast and detect concurrent modifications");
    }

    @Test
    void testListIteratorDetectsConcurrentModification() {
        model.addVariable();
        List<Variable> vars = model.getVariables();
        ListIterator<Variable> li = vars.listIterator();
        // structural change after iterator creation
        model.addVariable();
        assertThrows(ConcurrentModificationException.class, li::next, "ListIterator must be fail‑fast and detect concurrent modifications");
    }
}
