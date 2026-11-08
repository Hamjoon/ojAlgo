package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import java.util.ConcurrentModificationException;

class ExpressionsBasedModel_getVariables_Test {

    @Test
    void testGetVariablesReturnsUnmodifiableList() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // default variable
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        List<Variable> vars = model.getVariables();
        // the list must contain exactly the two variables we added
        assertEquals(2, vars.size(), "Unexpected number of variables");
        assertTrue(vars.contains(v1), "List should contain first variable");
        assertTrue(vars.contains(v2), "List should contain second variable");
        // the returned list must be unmodifiable
        // use a freshly created Variable instance (name + dummy index) – it does not matter that it is not added to the model
        assertThrows(UnsupportedOperationException.class, () -> vars.add(new Variable("tmp", -1)), "List should be unmodifiable");
    }

    @Test
    void testListReflectsModelChanges() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.addVariable();
        List<Variable> vars = model.getVariables();
        // initially only one variable
        assertEquals(1, vars.size());
        // add another variable to the model after the view has been obtained
        Variable v2 = model.addVariable();
        // the same view must now show the new variable
        assertEquals(2, vars.size(), "View should reflect newly added variable");
        assertTrue(vars.contains(v2), "View should contain the newly added variable");
    }

    @Test
    void testEmptyModelReturnsEmptyUnmodifiableList() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        List<Variable> vars = model.getVariables();
        assertNotNull(vars, "Returned list must not be null");
        assertTrue(vars.isEmpty(), "New model should have no variables");
        // still unmodifiable even when empty
        assertThrows(UnsupportedOperationException.class, () -> vars.remove(0), "Empty list must also be unmodifiable");
    }

    @Test
    void testUnmodifiableListAddThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(UnsupportedOperationException.class, () -> vars.add(model.addVariable()), "add() must throw UnsupportedOperationException");
    }

    @Test
    void testUnmodifiableListRemoveThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(UnsupportedOperationException.class, () -> vars.remove(0), "remove() must throw UnsupportedOperationException");
    }

    @Test
    void testUnmodifiableListClearThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(UnsupportedOperationException.class, vars::clear, "clear() must throw UnsupportedOperationException");
    }

    @Test
    void testUnmodifiableListSetThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        // second variable for replacement
        Variable replacement = model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(UnsupportedOperationException.class, () -> vars.set(0, replacement), "set() must throw UnsupportedOperationException");
    }

    @Test
    void testWrapperIdentity() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.addVariable();
        Variable v1 = model.addVariable();
        List<Variable> list1 = model.getVariables();
        List<Variable> list2 = model.getVariables();
        // Different wrapper objects
        assertNotSame(list1, list2, "Two calls should return different wrapper instances");
        // Content equality (order and elements)
        assertEquals(list1, list2, "Both wrappers must be equal in content");
        assertEquals(2, list1.size());
        assertEquals(v0, list1.get(0));
        assertEquals(v1, list1.get(1));
    }

    @Test
    void testLargeNumberOfVariablesOrderPreserved() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        final int COUNT = 1_000;
        // Keep references to first and last variables for later comparison
        Variable first = null;
        Variable last = null;
        for (int i = 0; i < COUNT; i++) {
            Variable v = model.addVariable();
            if (i == 0) {
                first = v;
            }
            if (i == COUNT - 1) {
                last = v;
            }
        }
        List<Variable> vars = model.getVariables();
        assertEquals(COUNT, vars.size(), "List size should match number of added variables");
        assertSame(first, vars.get(0), "First element must be the first variable added");
        assertSame(last, vars.get(COUNT - 1), "Last element must be the last variable added");
    }

    @Test
    void testDuplicateVariableNamesAreBothPresent() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.addVariable("dup");
        // same name, different instance
        Variable v1 = model.addVariable("dup");
        List<Variable> vars = model.getVariables();
        assertEquals(2, vars.size(), "Both variables should be present despite identical names");
        assertTrue(vars.contains(v0), "List must contain first variable with duplicate name");
        assertTrue(vars.contains(v1), "List must contain second variable with duplicate name");
        assertNotSame(v0, v1, "Variables must be distinct instances");
    }

    @Test
    void testIteratorDetectsConcurrentModification() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        List<Variable> vars = model.getVariables();
        var iterator = vars.iterator();
        // Modify the underlying model before completing iteration
        model.addVariable();
        // The next call on the iterator should notice the structural change
        assertThrows(ConcurrentModificationException.class, iterator::next, "Iterator must throw ConcurrentModificationException after model modification");
    }

    @Test
    void testGetOnEmptyListThrowsIndexOutOfBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.get(0), "Getting element from empty list must throw IndexOutOfBoundsException");
    }

    @Test
    void testGetBeyondSizeThrowsIndexOutOfBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        model.addVariable();
        // size = 3
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.get(3), "Getting element at index equal to size must throw IndexOutOfBoundsException");
    }

    @Test
    void testGetNegativeIndexThrowsIndexOutOfBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // at least one element so list is not empty
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.get(-1), "Getting element with negative index must throw IndexOutOfBoundsException");
    }

    @Test
    void testSubListOnEmptyListThrowsIndexOutOfBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.subList(0, 1), "subList on empty list with toIndex > size must throw IndexOutOfBoundsException");
    }

    @Test
    void testSubListFromGreaterThanToThrowsIllegalArgument() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        // size = 2
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IllegalArgumentException.class, () -> vars.subList(1, 0), "subList with fromIndex > toIndex must throw IllegalArgumentException");
    }

    @Test
    void testSubListWithNegativeFromIndexThrowsIndexOutOfBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        // size = 2
        model.addVariable();
        List<Variable> vars = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> vars.subList(-1, 1), "subList with negative fromIndex must throw IndexOutOfBoundsException");
    }
}
