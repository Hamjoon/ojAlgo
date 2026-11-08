package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isReferenced_Test {

    private ExpressionsBasedModel model;

    private Variable referencedVar;

    private Variable nonReferencedVar;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Two variables – one will be added to the reference set, the other will not
        // index 0
        referencedVar = model.addVariable();
        // index 1
        nonReferencedVar = model.addVariable();
        // Register the first variable as a reference in the model
        model.addReference(referencedVar.getIndex());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required because a new model is created per test
        model = null;
        referencedVar = null;
        nonReferencedVar = null;
    }

    static Stream<Arguments> positiveScenariosProvider() {
        return Stream.of(// single element set
        Arguments.of(3, Set.of(3)), // multiple elements, target present
        Arguments.of(7, Set.of(3, 7, 9)));
    }

    static Stream<Arguments> negativeScenariosProvider() {
        return Stream.of(// index not present
        Arguments.of(5, Set.of(1, 2, 4)), // empty reference set
        Arguments.of(10, Collections.emptySet()));
    }

    /**
     * Replace the private {@code myReferences} field with the supplied set.
     */
    private static void setInternalReferences(ExpressionsBasedModel target, Set<Structure1D.IntIndex> newRefs) throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        field.setAccessible(true);
        field.set(target, newRefs);
    }

    @Test
    void testIsReferencedReturnsTrueForRegisteredVariable() {
        assertTrue(model.isReferenced(referencedVar), "Variable that was added via addReference should be reported as referenced.");
    }

    @Test
    void testIsReferencedReturnsFalseForUnregisteredVariable() {
        assertFalse(model.isReferenced(nonReferencedVar), "Variable that was not added via addReference should not be reported as referenced.");
    }

    @ParameterizedTest
    @MethodSource("positiveScenariosProvider")
    void testIsReferencedPositiveScenarios(int targetIndex, Set<Integer> referenceIndices) throws Exception {
        // Build a fresh model and populate its reference set via reflection
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Set<Structure1D.IntIndex> intIndexes = new HashSet<>();
        for (int i : referenceIndices) {
            intIndexes.add(m.toIntIndex(i));
        }
        setInternalReferences(m, intIndexes);
        // Mock a Variable that returns the desired IntIndex
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(m.toIntIndex(targetIndex));
        assertTrue(m.isReferenced(var), "Expected true because the reference set contains the variable's index.");
    }

    @ParameterizedTest
    @MethodSource("negativeScenariosProvider")
    void testIsReferencedNegativeScenarios(int targetIndex, Set<Integer> referenceIndices) throws Exception {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Set<Structure1D.IntIndex> intIndexes = new HashSet<>();
        for (int i : referenceIndices) {
            intIndexes.add(m.toIntIndex(i));
        }
        setInternalReferences(m, intIndexes);
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(m.toIntIndex(targetIndex));
        assertFalse(m.isReferenced(var), "Expected false because the reference set does not contain the variable's index.");
    }

    @Test
    void testIsReferencedNullVariable() {
        assertThrows(NullPointerException.class, () -> model.isReferenced(null), "Passing null should result in NullPointerException.");
    }

    @Test
    void testIsReferencedVariableWithNullIndexReturnsFalseWhenSetHasNoNull() throws Exception {
        // Ensure the internal set does NOT contain null
        setInternalReferences(model, Collections.emptySet());
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(null);
        assertFalse(model.isReferenced(var), "Variable with null index should return false when the reference set does not contain null.");
    }

    @Test
    void testIsReferencedVariableWithNullIndexReturnsTrueWhenSetContainsNull() throws Exception {
        // Populate the internal set with a single null element
        Set<Structure1D.IntIndex> refs = new HashSet<>();
        refs.add(null);
        setInternalReferences(model, refs);
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(null);
        assertTrue(model.isReferenced(var), "When both variable index and a stored reference are null, contains(null) should be true.");
    }

    @Test
    void testIsReferencedEqualitySemantics() throws Exception {
        // Build reference set with an IntIndex of value 5
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Structure1D.IntIndex stored = m.toIntIndex(5);
        setInternalReferences(m, Set.of(stored));
        // Create a *different* IntIndex instance with the same underlying value
        Structure1D.IntIndex other = m.toIntIndex(5);
        assertNotSame(stored, other, "IntIndex instances should be distinct objects.");
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(other);
        assertTrue(m.isReferenced(var), "Set.contains should use equals, therefore a different IntIndex with the same value must be recognised.");
    }

    @Test
    void testIsReferencedDoesNotMutateReferenceSet() throws Exception {
        // Prepare a reference set with three indices
        Set<Integer> indices = Set.of(1, 2, 8);
        Set<Structure1D.IntIndex> refs = new HashSet<>();
        for (int i : indices) {
            refs.add(model.toIntIndex(i));
        }
        setInternalReferences(model, refs);
        // Capture a snapshot before the call
        Set<Structure1D.IntIndex> before = new HashSet<>(getInternalReferences(model));
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(model.toIntIndex(8));
        assertTrue(model.isReferenced(var), "Variable with index 8 should be referenced.");
        // Capture after the call and compare
        Set<Structure1D.IntIndex> after = getInternalReferences(model);
        assertEquals(before, after, "The internal reference set must remain unchanged after isReferenced.");
    }

    @Test
    void testIsReferencedVariableGetIndexThrowsException() {
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.isReferenced(var), "Exception from Variable#getIndex() must be propagated.");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testIsReferencedWhenInternalReferenceSetIsNull() throws Exception {
        // Force myReferences to null via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        field.setAccessible(true);
        field.set(model, null);
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(model.toIntIndex(1));
        assertThrows(NullPointerException.class, () -> model.isReferenced(var), "When myReferences is null, a NullPointerException is expected.");
    }

    /**
     * Retrieve the current content of the private {@code myReferences} field.
     */
    @SuppressWarnings("unchecked")
    private static Set<Structure1D.IntIndex> getInternalReferences(ExpressionsBasedModel target) throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        field.setAccessible(true);
        return (Set<Structure1D.IntIndex>) field.get(target);
    }
}
