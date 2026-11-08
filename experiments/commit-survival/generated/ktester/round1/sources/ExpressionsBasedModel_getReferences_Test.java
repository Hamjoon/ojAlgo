package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.ProgrammingError;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getReferences_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        model = new ExpressionsBasedModel();
    }

    /* -------------------------------------------------
     *  Helper: assertThrowsAnyOf (JUnit 5 does not provide this directly)
     * ------------------------------------------------- */
    private static void assertThrowsAnyOf(Class<? extends Throwable> first, Class<? extends Throwable> second, Executable executable, String message) {
        try {
            executable.execute();
            fail(message + " – no exception thrown");
        } catch (Throwable ex) {
            if (!first.isInstance(ex) && !second.isInstance(ex)) {
                fail(message + " – unexpected exception type: " + ex);
            }
            // Expected path – exception of allowed type thrown
        }
    }

    @Test
    void testEmptyReferences() {
        Set<IntIndex> refs = model.getReferences();
        assertNotNull(refs, "Reference set should never be null");
        assertTrue(refs.isEmpty(), "New model should have no references");
    }

    @Test
    void testAddReference() {
        // Create a variable so we have a valid index.
        Variable var = model.addVariable();
        int idx = model.indexOf(var);
        IntIndex intIdx = IntIndex.of(idx);
        // Add the reference.
        model.addReference(intIdx);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Reference set should contain exactly one entry");
        assertTrue(refs.contains(intIdx), "Reference set should contain the added index");
    }

    @Test
    void testCopyPreservesReferences() {
        // Add two references.
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        model.addReference(IntIndex.of(model.indexOf(v1)));
        model.addReference(IntIndex.of(model.indexOf(v2)));
        // Perform a copy.
        ExpressionsBasedModel copy = model.copy();
        // Verify both models have the same references.
        Set<IntIndex> originalRefs = model.getReferences();
        Set<IntIndex> copyRefs = copy.getReferences();
        assertEquals(originalRefs.size(), copyRefs.size(), "Copied model should have the same number of references");
        assertTrue(copyRefs.containsAll(originalRefs), "Copied model should contain all references from the original");
    }

    /* -------------------------------------------------
     *  2. Added references (distinct & duplicate)
     * ------------------------------------------------- */
    @ParameterizedTest(name = "{index} => indices={0}")
    @CsvSource({ "'0,5,10'", "'3,3,3'" })
    void testAddedReferences(String csvIndices) {
        // Parse the CSV list into int values
        String[] parts = csvIndices.split(",");
        for (String p : parts) {
            int idx = Integer.parseInt(p.trim());
            model.addReference(IntIndex.of(idx));
        }
        Set<IntIndex> refs = model.getReferences();
        // Build the expected set (duplicates automatically removed by Set)
        Set<IntIndex> expected = Stream.of(parts).map(String::trim).mapToInt(Integer::parseInt).mapToObj(IntIndex::of).collect(java.util.stream.Collectors.toSet());
        assertEquals(expected.size(), refs.size(), "Reference set size should match expected unique count");
        assertTrue(refs.containsAll(expected), "Reference set should contain all expected indices");
    }

    /* -------------------------------------------------
     *  3. Mutability of returned Set (remove / add)
     * ------------------------------------------------- */
    @Test
    void testReturnedSetIsMutable_Remove() {
        // Populate model with two references
        model.addReference(IntIndex.of(1));
        model.addReference(IntIndex.of(2));
        Set<IntIndex> liveSet = model.getReferences();
        assertEquals(2, liveSet.size());
        // Remove one element via the live set
        liveSet.remove(IntIndex.of(1));
        // The model's internal collection must reflect the removal
        assertEquals(1, model.getReferences().size());
        assertFalse(model.getReferences().contains(IntIndex.of(1)));
    }

    @Test
    void testReturnedSetIsMutable_Add() {
        Set<IntIndex> liveSet = model.getReferences();
        assertTrue(liveSet.isEmpty());
        // Directly add via the live set
        liveSet.add(IntIndex.of(7));
        // Model should now see the added reference
        assertEquals(1, model.getReferences().size());
        assertTrue(model.getReferences().contains(IntIndex.of(7)));
    }

    /* -------------------------------------------------
     *  4. Live view after subsequent additions
     * ------------------------------------------------- */
    @Test
    void testLiveViewAfterAddReference() {
        Set<IntIndex> liveSet = model.getReferences();
        assertTrue(liveSet.isEmpty());
        // Add a reference after the set has been obtained
        model.addReference(IntIndex.of(7));
        // The previously obtained set must reflect the new element
        assertEquals(1, liveSet.size());
        assertTrue(liveSet.contains(IntIndex.of(7)));
    }

    /* -------------------------------------------------
     *  5. Shallow copy shares reference collection
     * ------------------------------------------------- */
    @Test
    void testShallowCopySharesReferences() {
        // Add a reference to the original model
        model.addReference(IntIndex.of(4));
        // Create a shallow copy (shallow = true, prune = false)
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        // The reference set instances must be identical (==)
        assertSame(model.getReferences(), shallowCopy.getReferences(), "Shallow copy should share the same Set instance");
        // Adding to original must be visible in copy
        model.addReference(IntIndex.of(9));
        assertTrue(shallowCopy.getReferences().contains(IntIndex.of(9)));
        // Adding via copy must be visible in original
        shallowCopy.addReference(IntIndex.of(12));
        assertTrue(model.getReferences().contains(IntIndex.of(12)));
    }

    /* -------------------------------------------------
     *  6. Deep copy isolates reference collection
     * ------------------------------------------------- */
    @Test
    void testDeepCopyIsolatesReferences() {
        // Populate original model
        model.addReference(IntIndex.of(1));
        model.addReference(IntIndex.of(2));
        // Deep copy (shallow = false, prune = false)
        ExpressionsBasedModel deepCopy = model.copy(false, false);
        // The Set instances must be different objects
        assertNotSame(model.getReferences(), deepCopy.getReferences(), "Deep copy should have its own Set instance");
        // Both should contain the same initial elements
        assertEquals(model.getReferences(), deepCopy.getReferences(), "Deep copy should start with identical references");
        // Modify original – should NOT affect deep copy
        model.addReference(IntIndex.of(3));
        assertFalse(deepCopy.getReferences().contains(IntIndex.of(3)));
        // Modify deep copy – should NOT affect original
        deepCopy.addReference(IntIndex.of(8));
        assertFalse(model.getReferences().contains(IntIndex.of(8)));
    }

    /* -------------------------------------------------
     *  7. Null reference handling
     * ------------------------------------------------- */
    @Test
    void testAddNullReferenceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.addReference(null), "addReference(null) should throw NullPointerException");
        // Ensure the internal set is still empty
        assertTrue(model.getReferences().isEmpty(), "Reference set must remain empty after attempting to add null");
    }

    /* -------------------------------------------------
     *  8. Negative index acceptance
     * ------------------------------------------------- */
    @Test
    void testNegativeIndexReference() {
        IntIndex negative = IntIndex.of(-3);
        model.addReference(negative);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size());
        assertTrue(refs.contains(negative), "Reference set should contain the negative index");
    }

    /* -------------------------------------------------
     *  9. External modification clears internal collection
     * ------------------------------------------------- */
    @Test
    void testExternalClearAffectsModel() {
        model.addReference(IntIndex.of(5));
        model.addReference(IntIndex.of(6));
        Set<IntIndex> external = model.getReferences();
        assertEquals(2, external.size());
        // Clear via external reference
        external.clear();
        // Model's internal collection must now be empty
        assertTrue(model.getReferences().isEmpty(), "Clearing the external Set reference must clear the model's internal references");
    }

    /* -------------------------------------------------
     * 10. Disposed model behavior
     * ------------------------------------------------- */
    @Test
    void testGetReferencesAfterDisposeThrows() {
        model.dispose();
        // The concrete exception type used by ojAlgo is ProgrammingError,
        // but IllegalStateException is also acceptable according to the spec.
        assertThrowsAnyOf(ProgrammingError.class, IllegalStateException.class, () -> model.getReferences(), "Calling getReferences() on a disposed model should throw an exception");
    }

    @Test
    void testShallowCopyDisposedOriginalThrows() {
        // Populate and create shallow copy
        model.addReference(IntIndex.of(0));
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        // Dispose original
        model.dispose();
        // Both original and shallow copy should now be unusable
        assertAll(() -> assertThrowsAnyOf(ProgrammingError.class, IllegalStateException.class, () -> model.getReferences(), "Disposed original model should throw"), () -> assertThrowsAnyOf(ProgrammingError.class, IllegalStateException.class, () -> shallowCopy.getReferences(), "Shallow copy sharing the reference set should also throw after original is disposed"));
    }
}
