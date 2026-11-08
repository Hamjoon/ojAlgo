package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getReferences_Test {

    @Test
    void testEmptyReferencesOnNewModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Set<IntIndex> refs = model.getReferences();
        assertNotNull(refs, "Reference set should never be null");
        assertTrue(refs.isEmpty(), "A freshly created model should have no references");
    }

    @Test
    void testAddReferenceAppearsInGetReferences() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        IntIndex idx = IntIndex.of(0);
        model.addReference(idx);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Reference set should contain exactly one element");
        assertTrue(refs.contains(idx), "Reference set should contain the added index");
    }

    @Test
    void testReturnedSetIsLive() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        IntIndex idx1 = IntIndex.of(1);
        model.addReference(idx1);
        // Obtain the live set reference
        Set<IntIndex> liveSet = model.getReferences();
        // Directly mutate the set
        IntIndex idx2 = IntIndex.of(2);
        liveSet.add(idx2);
        // The model should now report both indices
        Set<IntIndex> afterMutation = model.getReferences();
        assertEquals(2, afterMutation.size(), "Mutating the returned set should affect the model's internal set");
        assertTrue(afterMutation.contains(idx1), "Original index must still be present");
        assertTrue(afterMutation.contains(idx2), "Newly added index must be present");
    }

    @Test
    void testDuplicateAdditionResultsInSingleElement() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        IntIndex dup = IntIndex.of(7);
        model.addReference(dup);
        // duplicate addition
        model.addReference(dup);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Set must contain only one instance after duplicate addition");
        assertTrue(refs.contains(dup), "The set must contain the duplicated index");
    }

    @Test
    void testClearingReturnedSetEmptiesInternalCollection() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addReference(IntIndex.of(1));
        model.addReference(IntIndex.of(3));
        model.addReference(IntIndex.of(5));
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "Pre‑condition: three references added");
        // Clear via the returned reference
        refs.clear();
        Set<IntIndex> afterClear = model.getReferences();
        assertTrue(afterClear.isEmpty(), "Clearing the returned set must empty the internal collection");
    }

    @Test
    void testMutabilityAcrossMultipleCalls() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Set<IntIndex> firstCall = model.getReferences();
        firstCall.add(IntIndex.of(2));
        firstCall.add(IntIndex.of(4));
        Set<IntIndex> secondCall = model.getReferences();
        assertEquals(2, secondCall.size(), "Indices added via the first returned set must be visible on subsequent calls");
        assertTrue(secondCall.contains(IntIndex.of(2)));
        assertTrue(secondCall.contains(IntIndex.of(4)));
    }

    @Test
    void testReferencesAfterShallowCopy() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.addReference(IntIndex.of(0));
        original.addReference(IntIndex.of(8));
        // Shallow copy – should share the same internal Set instance
        ExpressionsBasedModel shallowCopy = original.copy();
        Set<IntIndex> originalRefs = original.getReferences();
        Set<IntIndex> copyRefs = shallowCopy.getReferences();
        assertSame(originalRefs, copyRefs, "Shallow copy must share the same reference Set instance");
        assertEquals(2, copyRefs.size());
        assertTrue(copyRefs.contains(IntIndex.of(0)));
        assertTrue(copyRefs.contains(IntIndex.of(8)));
    }

    @Test
    void testReferencesAfterDeepCopy() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.addReference(IntIndex.of(0));
        original.addReference(IntIndex.of(8));
        // Deep copy – independent Set (copy(false, false) is the deep‑copy variant)
        ExpressionsBasedModel deepCopy = original.copy(false, false);
        Set<IntIndex> originalRefs = original.getReferences();
        Set<IntIndex> copyRefs = deepCopy.getReferences();
        // They must contain the same elements initially
        assertEquals(originalRefs, copyRefs, "Deep copy must contain the same elements initially");
        // But must be different objects
        assertNotSame(originalRefs, copyRefs, "Deep copy must have its own independent Set instance");
        // Modify original after copying and verify copy is unchanged
        originalRefs.clear();
        assertTrue(originalRefs.isEmpty(), "Original set cleared");
        assertEquals(2, copyRefs.size(), "Deep‑copied set must remain unchanged after original is cleared");
    }

    @Test
    void testGetReferencesNeverReturnsNull() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Set<IntIndex> refs = model.getReferences();
        assertNotNull(refs, "getReferences() must never return null on a fresh model");
    }

    @Test
    void testGetReferencesThrowsWhenInternalSetIsNullViaReflection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use reflection to set the private field myReferences to null
        Field field = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        field.setAccessible(true);
        field.set(model, null);
        // Expect a NullPointerException when accessing the method
        assertThrows(NullPointerException.class, model::getReferences, "Calling getReferences() after myReferences is null should throw NPE");
    }

    @Test
    void testGetReferencesAfterDisposeThrowsNPE() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Dispose is expected to clear internal resources; we verify the behaviour
        model.dispose();
        assertThrows(NullPointerException.class, model::getReferences, "After dispose(), getReferences() should throw NullPointerException if internal set is cleared");
    }

    @Test
    void testAnonymousSubclassWithoutSuperInitThrowsNPE() {
        // Create an anonymous subclass that bypasses the super‑constructor initialisation.
        ExpressionsBasedModel malformed = new ExpressionsBasedModel() {

            {
                // Intentionally do NOT call super() or initialise myReferences.
                // The field remains null because the default constructor of the outer class
                // is not executed.
            }
        };
        // The field is still null; invoking getReferences() must throw NPE.
        assertThrows(NullPointerException.class, malformed::getReferences, "Malformed subclass without myReferences initialisation should throw NPE");
    }
}
