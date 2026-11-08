package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test contamination.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly discard the model reference; helps GC in long test suites.
        model = null;
    }

    static Stream<Runnable> structuralModificationProviders() {
        return Stream.of(// add()
        () -> {
            List<Variable> list = new ExpressionsBasedModel().getVariables();
            list.add(new Variable());
        }, // addAll()
        () -> {
            List<Variable> list = new ExpressionsBasedModel().getVariables();
            list.addAll(Collections.singletonList(new Variable()));
        }, // set()
        () -> {
            ExpressionsBasedModel m = new ExpressionsBasedModel();
            m.addVariable("x");
            List<Variable> list = m.getVariables();
            list.set(0, new Variable());
        });
    }

    /**
     * The model starts with no variables.
     */
    @Test
    void testEmptyModelReturnsEmptyList() {
        List<Variable> vars = model.getVariables();
        assertNotNull(vars, "The returned list must never be null");
        assertTrue(vars.isEmpty(), "A newly created model should have no variables");
        // The list must be unmodifiable – attempting to add should fail.
        assertThrows(UnsupportedOperationException.class, () -> vars.add(new Variable()));
    }

    /**
     * Adding variables via the public API should be reflected in the list returned by {@code getVariables()}.
     */
    @Test
    void testVariablesAddedAreVisibleAndOrdered() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Variable v3 = model.addVariable("v3");
        List<Variable> vars = model.getVariables();
        // The list should contain exactly the three variables in insertion order.
        assertEquals(3, vars.size(), "Expected three variables in the list");
        assertSame(v1, vars.get(0), "First variable should be v1");
        assertSame(v2, vars.get(1), "Second variable should be v2");
        assertSame(v3, vars.get(2), "Third variable should be v3");
        // The returned list must be unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> vars.remove(0));
    }

    /**
     * The view returned by {@code getVariables()} must reflect later additions to the model.
     */
    @Test
    void testViewReflectsSubsequentAdditions() {
        // Obtain the view before any variables are added.
        List<Variable> view = model.getVariables();
        assertTrue(view.isEmpty(), "Initial view should be empty");
        // Add a variable after the view has been obtained.
        Variable added = model.addVariable("newVar");
        // The view should now contain the newly added variable.
        assertEquals(1, view.size(), "View should reflect the added variable");
        assertSame(added, view.get(0), "The variable in the view must be the one just added");
    }

    /**
     * The returned list must be the exact unmodifiable wrapper used by the implementation.
     */
    @Test
    void testReturnedListIsUnmodifiableWrapper() {
        // The implementation uses Collections.unmodifiableList; we can verify the class type.
        List<Variable> vars = model.getVariables();
        assertTrue(vars.getClass().getName().startsWith("java.util.Collections$Unmodifiable"), "Expected an unmodifiable list wrapper from java.util.Collections");
    }

    @ParameterizedTest(name = "{index}: structural modification throws UnsupportedOperationException")
    @MethodSource("structuralModificationProviders")
    void testStructuralModificationsThrow(Runnable modification) {
        assertThrows(UnsupportedOperationException.class, modification::run);
    }

    @Test
    @DisplayName("Iterator.remove() throws UnsupportedOperationException")
    void testIteratorRemoveThrows() {
        model.addVariable("a");
        model.addVariable("b");
        Iterator<Variable> it = model.getVariables().iterator();
        assertTrue(it.hasNext(), "Iterator should have elements");
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove must be unsupported");
    }

    @Test
    @DisplayName("Iterator traverses variables in insertion order")
    void testIteratorTraversalOrder() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Variable v3 = model.addVariable("v3");
        Iterator<Variable> it = model.getVariables().iterator();
        assertEquals(v1, it.next(), "First iterator element");
        assertEquals(v2, it.next(), "Second iterator element");
        assertEquals(v3, it.next(), "Third iterator element");
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    @DisplayName("subList.clear() throws UnsupportedOperationException")
    void testSubListClearThrows() {
        model.addVariable("x");
        List<Variable> list = model.getVariables();
        List<Variable> sub = list.subList(0, 1);
        assertThrows(UnsupportedOperationException.class, sub::clear);
    }

    @Test
    @DisplayName("subList reflects the same underlying elements")
    void testSubListReflectsUnderlyingElements() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        List<Variable> list = model.getVariables();
        List<Variable> sub = list.subList(0, 2);
        assertEquals(2, sub.size(), "SubList size should be 2");
        assertSame(v1, sub.get(0), "First element must be v1");
        assertSame(v2, sub.get(1), "Second element must be v2");
    }

    @Test
    @DisplayName("toArray() returns Object[] with correct order")
    void testToArrayObjectContent() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Variable v3 = model.addVariable("v3");
        Object[] arr = model.getVariables().toArray();
        assertEquals(3, arr.length, "Array length must match number of variables");
        assertSame(v1, arr[0], "First element must be v1");
        assertSame(v2, arr[1], "Second element must be v2");
        assertSame(v3, arr[2], "Third element must be v3");
    }

    @Test
    @DisplayName("toArray(T[]) returns correctly typed array")
    void testToArrayTypedContent() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Variable[] typed = model.getVariables().toArray(new Variable[0]);
        assertEquals(2, typed.length, "Typed array length must match");
        assertSame(v1, typed[0], "First element must be v1");
        assertSame(v2, typed[1], "Second element must be v2");
    }

    @Test
    @DisplayName("Two successive calls return equal content but distinct wrappers")
    void testSuccessiveCallsEqualityAndDistinctness() {
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        List<Variable> listA = model.getVariables();
        List<Variable> listB = model.getVariables();
        assertEquals(listA, listB, "Contents must be equal");
        assertEquals(listA.size(), listB.size(), "Sizes must match");
        assertNotSame(listA, listB, "Wrapper instances must be distinct");
    }

    @Test
    @DisplayName("Both lists reflect later additions")
    void testBothListsReflectLaterAdditions() {
        List<Variable> listA = model.getVariables();
        Variable added = model.addVariable("newVar");
        List<Variable> listB = model.getVariables();
        assertEquals(1, listA.size(), "listA must see the new variable");
        assertEquals(1, listB.size(), "listB must see the new variable");
        assertSame(added, listA.get(0));
        assertSame(added, listB.get(0));
    }

    @Test
    @DisplayName("Changes to a Variable instance are visible through the list")
    void testVariableMutationIsVisible() throws Exception {
        Variable var = model.addVariable("original");
        List<Variable> list = model.getVariables();
        // Attempt to change the variable's name via reflection (avoids compile‑time dependency on a setter)
        Field nameField = Variable.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(var, "modified");
        // The same instance is stored in the list, so the change must be observable.
        Variable fromList = list.get(0);
        assertSame(var, fromList, "List must contain the same instance");
        assertEquals("modified", nameField.get(fromList), "Mutation must be reflected");
    }

    @Test
    @DisplayName("list.get(-1) throws IndexOutOfBoundsException")
    void testNegativeIndexThrows() {
        model.addVariable("a");
        model.addVariable("b");
        List<Variable> list = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    @DisplayName("list.get(size) throws IndexOutOfBoundsException")
    void testIndexEqualSizeThrows() {
        model.addVariable("a");
        model.addVariable("b");
        List<Variable> list = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(list.size()));
    }

    @Test
    @DisplayName("subList with toIndex > size throws IndexOutOfBoundsException")
    void testSubListToIndexOutOfBounds() {
        model.addVariable("only");
        List<Variable> list = model.getVariables();
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, 2));
    }

    @Test
    @DisplayName("subList with fromIndex > toIndex throws IllegalArgumentException")
    void testSubListFromIndexGreaterThanToIndex() {
        model.addVariable("only");
        List<Variable> list = model.getVariables();
        assertThrows(IllegalArgumentException.class, () -> list.subList(1, 0));
    }
}