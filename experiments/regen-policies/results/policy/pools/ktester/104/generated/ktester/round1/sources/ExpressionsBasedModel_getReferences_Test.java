package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getReferences_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates an empty reference set.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        model = null;
    }

    @Test
    void testGetReferencesInitiallyEmpty() {
        Set<IntIndex> refs = model.getReferences();
        assertNotNull(refs, "The reference set should never be null.");
        assertTrue(refs.isEmpty(), "A newly created model must have no references.");
    }

    @Test
    void testGetReferencesAfterAddingSome() {
        // Create three distinct indices.
        IntIndex idx0 = model.toIntIndex(0);
        IntIndex idx5 = model.toIntIndex(5);
        IntIndex idx42 = model.toIntIndex(42);
        // Add them to the model's reference collection.
        model.addReference(idx0);
        model.addReference(idx5);
        model.addReference(idx42);
        Set<IntIndex> refs = model.getReferences();
        // Verify that the set contains exactly the added indices.
        assertEquals(3, refs.size(), "Reference set should contain three entries.");
        assertTrue(refs.contains(idx0), "Reference set should contain index 0.");
        assertTrue(refs.contains(idx5), "Reference set should contain index 5.");
        assertTrue(refs.contains(idx42), "Reference set should contain index 42.");
    }

    @Test
    void testGetReferencesReflectsFurtherAdds() {
        IntIndex idx1 = model.toIntIndex(1);
        model.addReference(idx1);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size());
        assertTrue(refs.contains(idx1));
        // Add another index after the initial call.
        IntIndex idx2 = model.toIntIndex(2);
        model.addReference(idx2);
        // The previously obtained set is the actual internal set, so it should now contain the new index.
        assertEquals(2, refs.size(), "The set returned earlier should reflect the new addition.");
        assertTrue(refs.contains(idx2), "Reference set should now contain index 2.");
    }

    @Test
    void testNonEmptyReferencesSet() {
        // add distinct indices 0,5,10
        IntIndex i0 = model.toIntIndex(0);
        IntIndex i5 = model.toIntIndex(5);
        IntIndex i10 = model.toIntIndex(10);
        model.addReference(i0);
        model.addReference(i5);
        model.addReference(i10);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "Should contain three distinct indices.");
        assertTrue(refs.containsAll(Set.of(i0, i5, i10)));
    }

    @Test
    void testDuplicateReferenceIsIgnored() {
        IntIndex dup = model.toIntIndex(7);
        model.addReference(dup);
        // duplicate addition
        model.addReference(dup);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Duplicate addition must not increase set size.");
        assertTrue(refs.contains(dup));
    }

    @Test
    void testModificationOfReturnedSetAffectsInternalState() {
        // Add an initial reference.
        IntIndex idx3 = model.toIntIndex(3);
        model.addReference(idx3);
        Set<IntIndex> returned = model.getReferences();
        assertTrue(returned.remove(idx3), "Removal from returned set should succeed.");
        // Subsequent call must see the change.
        Set<IntIndex> afterRemoval = model.getReferences();
        assertTrue(afterRemoval.isEmpty(), "Internal set should now be empty after removal.");
    }

    @Test
    void testGetReferencesReturnsSameInstanceOnRepeatedCalls() {
        Set<IntIndex> first = model.getReferences();
        Set<IntIndex> second = model.getReferences();
        assertSame(first, second, "Consecutive calls must return the identical Set instance.");
    }

    @Test
    void testNullInternalReferencesViaReflection() throws Exception {
        // Use reflection to set the private field myReferences to null.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myReferences");
        field.setAccessible(true);
        field.set(model, null);
        // The method does not guard against null, so we expect a null return.
        Set<IntIndex> refs = model.getReferences();
        assertNull(refs, "When internal myReferences is null, getReferences() should return null.");
    }

    @Test
    void testShallowCopySharesReferenceSet() {
        // Prepare original model with some references.
        model.addReference(model.toIntIndex(1));
        model.addReference(model.toIntIndex(3));
        // Shallow copy: first boolean = true (shallow), second = false (no pruning)
        ExpressionsBasedModel shallowCopy = model.copy(true, false);
        // Both should expose the exact same Set instance.
        assertSame(model.getReferences(), shallowCopy.getReferences(), "Shallow copy must share the same reference Set instance.");
    }

    @Test
    void testDeepCopyHasIndependentReferenceSet() {
        // Original model with references.
        model.addReference(model.toIntIndex(7));
        model.addReference(model.toIntIndex(9));
        // Deep copy: shallow = false, prune = false
        ExpressionsBasedModel deepCopy = model.copy(false, false);
        // The Sets must be equal in content but not the same object.
        Set<IntIndex> originalSet = model.getReferences();
        Set<IntIndex> copySet = deepCopy.getReferences();
        assertNotSame(originalSet, copySet, "Deep copy must have a different Set instance.");
        assertEquals(originalSet, copySet, "Content of the reference Sets must be equal.");
    }

    @Test
    void testConcurrentAdditionOfReferences() throws Exception {
        // Indices to be added concurrently.
        IntIndex idx5 = model.toIntIndex(5);
        IntIndex idx8 = model.toIntIndex(8);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        Runnable add5 = () -> {
            try {
                startLatch.await();
                model.addReference(idx5);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable add8 = () -> {
            try {
                startLatch.await();
                model.addReference(idx8);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        Thread t1 = new Thread(add5);
        Thread t2 = new Thread(add8);
        t1.start();
        t2.start();
        // Release both threads simultaneously.
        startLatch.countDown();
        // Wait for both to finish.
        doneLatch.await();
        Set<IntIndex> refs = model.getReferences();
        assertEquals(2, refs.size(), "Both indices must be present after concurrent adds.");
        assertTrue(refs.containsAll(Set.of(idx5, idx8)));
    }

    @Test
    void testAddNegativeIndexReference() {
        // The model does not validate index range, so negative values are accepted.
        IntIndex negative = model.toIntIndex(-1);
        model.addReference(negative);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Negative index should be stored.");
        assertTrue(refs.contains(negative));
    }
}
