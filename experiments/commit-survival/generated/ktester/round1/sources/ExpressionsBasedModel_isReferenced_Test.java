package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class ExpressionsBasedModel_isReferenced_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for each test case
        model = new ExpressionsBasedModel();
        // Two distinct variables – they obtain distinct IntIndex values (0 and 1)
        // index 0
        varA = model.addVariable();
        // index 1
        varB = model.addVariable();
        // Register a reference only for varA (index 0)
        model.addReference(varA.getIndex());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any internal structures
        model.dispose();
    }

    @Test
    void testIsReferencedReturnsTrueForRegisteredVariable() {
        // GIVEN the model has a reference to varA (added in setup)
        // WHEN checking the reference status
        boolean referenced = model.isReferenced(varA);
        // THEN it should be true
        assertTrue(referenced, "Variable that was added to references should be reported as referenced");
    }

    @Test
    void testIsReferencedReturnsFalseForUnregisteredVariable() {
        // GIVEN the model has no reference to varB
        // WHEN checking the reference status
        boolean referenced = model.isReferenced(varB);
        // THEN it should be false
        assertFalse(referenced, "Variable that was not added to references should not be reported as referenced");
    }

    @Test
    void testIsReferencedAfterCopyPreservesReferences() {
        // GIVEN a shallow copy of the model (the copy‑constructor exists in the library)
        ExpressionsBasedModel copy = new ExpressionsBasedModel(model, false, false);
        // WHEN checking the reference status on the copy
        boolean referencedInCopy = copy.isReferenced(varA);
        // THEN the reference should be preserved
        assertTrue(referencedInCopy, "References should be copied to the new model instance");
        // Clean up copy
        copy.dispose();
    }

    /**
     * 1. Null Variable – expect NPE ****************************************
     */
    @Test
    void testIsReferencedWithNullVariableThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.isReferenced(null), "Calling isReferenced(null) must throw NullPointerException");
    }

    /**
     * 2. Empty References – any variable should be reported as not referenced
     */
    @Test
    void testIsReferencedWithEmptyReferenceSet() throws Exception {
        // Clear the internal reference set via reflection
        Field refField = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        refField.setAccessible(true);
        Set<?> emptySet = Collections.emptySet();
        refField.set(model, emptySet);
        // Any variable (varA) should now be reported as not referenced
        assertFalse(model.isReferenced(varA), "With an empty reference set, isReferenced must return false for any variable");
    }

    @Test
    void testIsReferencedMultipleReferencesMixed() {
        // Add a second reference (index of varB)
        model.addReference(varB.getIndex());
        // Both variables are now referenced
        assertTrue(model.isReferenced(varA), "varA should be referenced after adding its index");
        assertTrue(model.isReferenced(varB), "varB should be referenced after adding its index");
        // Create a third variable that is NOT referenced
        // index 2
        Variable varC = model.addVariable();
        assertFalse(model.isReferenced(varC), "A variable whose index was never added should be reported as not referenced");
    }

    /**
     * 4. Equal Index Different Instance – mock variable with same IntIndex ***
     */
    @Test
    void testIsReferencedEqualIndexDifferentInstance() {
        // varA is already referenced (index 0)
        // Create a mock Variable that returns the *same* IntIndex instance as varA
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenReturn(varA.getIndex());
        // The model should treat the mock as referenced because Set.contains uses equals()
        assertTrue(model.isReferenced(mockVar), "A different Variable instance with the same logical index must be considered referenced");
    }

    /**
     * 5. Reference Integrity – size of myReferences must stay unchanged *******
     */
    @Test
    void testIsReferencedDoesNotModifyReferenceSet() throws Exception {
        // Capture initial size
        Field refField = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        refField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> refs = (Set<?>) refField.get(model);
        int initialSize = refs.size();
        // Call isReferenced (both true and false cases)
        // true
        model.isReferenced(varA);
        // false
        model.isReferenced(varB);
        // Verify size unchanged
        int afterSize = refs.size();
        assertEquals(initialSize, afterSize, "Calling isReferenced must not modify the internal reference set");
    }

    /**
     * 6. Index Out of Model Bounds – should simply return false *************
     */
    @Test
    void testIsReferencedWithOutOfBoundsIndexReturnsFalse() {
        // Create a mock Variable whose index is far beyond any created variable
        Variable outOfBounds = Mockito.mock(Variable.class);
        // Mock an IntIndex that returns a large index value (e.g., 9999)
        Structure1D.IntIndex largeIndex = Mockito.mock(Structure1D.IntIndex.class);
        Mockito.when(largeIndex.intValue()).thenReturn(9999);
        // Ensure equals works as expected (different object, same intValue)
        Mockito.when(largeIndex.equals(Mockito.any())).thenCallRealMethod();
        Mockito.when(largeIndex.hashCode()).thenCallRealMethod();
        Mockito.when(outOfBounds.getIndex()).thenReturn(largeIndex);
        // Since the large index was never added, result must be false
        assertFalse(model.isReferenced(outOfBounds), "Variable with an index not present in myReferences must be reported as not referenced");
    }

    /**
     * 7. Null Reference Set – simulate internal corruption *******************
     */
    @Test
    void testIsReferencedWithNullReferenceSetThrowsNPE() throws Exception {
        // Use reflection to set the private myReferences field to null
        Field refField = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        refField.setAccessible(true);
        refField.set(model, null);
        // Any call should now throw NPE because the method dereferences the set
        assertThrows(NullPointerException.class, () -> model.isReferenced(varA), "If myReferences is null, isReferenced must throw NullPointerException");
    }
}
