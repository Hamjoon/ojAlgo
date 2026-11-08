package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addReference_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to guarantee isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model will be garbage‑collected.
        model = null;
    }

    static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(Arguments.of(new IntIndex(-1)), Arguments.of(new IntIndex(Integer.MAX_VALUE)));
    }

    @Test
    void testAddReferenceAddsIndex() {
        // Given: a fresh model and a new IntIndex
        IntIndex idx = new IntIndex(5);
        Set<IntIndex> before = model.getReferences();
        // Pre‑condition: the references set should be empty
        assertTrue(before.isEmpty(), "References set should be empty before adding.");
        // When: we add the index via the package‑private method
        model.addReference(idx);
        // Then: the set contains exactly that index
        Set<IntIndex> after = model.getReferences();
        assertEquals(1, after.size(), "References set should contain one element after addition.");
        assertTrue(after.contains(idx), "References set should contain the added IntIndex.");
    }

    @Test
    void testAddReferenceIsIdempotent() {
        // Given: a model with an index already added
        IntIndex idx = new IntIndex(3);
        model.addReference(idx);
        Set<IntIndex> afterFirstAdd = model.getReferences();
        assertEquals(1, afterFirstAdd.size(), "Pre‑condition failed: set should contain one element.");
        // When: we add the same index again
        model.addReference(idx);
        // Then: the set size remains unchanged (no duplicates)
        Set<IntIndex> afterSecondAdd = model.getReferences();
        assertEquals(1, afterSecondAdd.size(), "Adding the same IntIndex again should not increase set size.");
        assertTrue(afterSecondAdd.contains(idx), "Set should still contain the original IntIndex.");
    }

    @Test
    void testAddReferenceAddsSecondDistinctIndex() {
        // Given: model already contains index 0
        IntIndex first = new IntIndex(0);
        model.addReference(first);
        assertEquals(1, model.getReferences().size());
        // When: add a distinct index 5
        IntIndex second = new IntIndex(5);
        model.addReference(second);
        // Then: both indices are present
        Set<IntIndex> refs = model.getReferences();
        assertEquals(2, refs.size(), "Set should contain two distinct indices.");
        assertTrue(refs.containsAll(Arrays.asList(first, second)));
    }

    @Test
    void testAddReferenceNullThrowsNPE() {
        // Null is not allowed by the underlying Set – expect NPE
        assertThrows(NullPointerException.class, () -> model.addReference(null), "Calling addReference with null should throw NullPointerException.");
    }

    @ParameterizedTest(name = "addReference with index {0}")
    @MethodSource("edgeCaseProvider")
    void testAddReferenceEdgeCases(IntIndex idx) {
        // When
        model.addReference(idx);
        // Then
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Set should contain exactly one element.");
        assertTrue(refs.contains(idx), "Set should contain the provided IntIndex.");
    }

    @Test
    void testAddReferenceMultipleDistinctIndices() {
        IntIndex[] indices = { new IntIndex(2), new IntIndex(4), new IntIndex(7) };
        // Add each index
        for (IntIndex idx : indices) {
            model.addReference(idx);
        }
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "Set should contain three distinct indices.");
        assertTrue(refs.containsAll(Arrays.asList(indices)));
    }

    @Test
    void testAddReferenceAfterExistingReferences() {
        // Initial indices 0 and 5
        IntIndex[] initial = { new IntIndex(0), new IntIndex(5) };
        for (IntIndex idx : initial) {
            model.addReference(idx);
        }
        assertEquals(2, model.getReferences().size());
        // New distinct index 9
        IntIndex newIdx = new IntIndex(9);
        model.addReference(newIdx);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "Set size should become 3 after adding a new distinct index.");
        assertTrue(refs.containsAll(Arrays.asList(initial[0], initial[1], newIdx)));
    }

    @Test
    void testAddReferenceDuplicateAfterManyEntries() {
        IntIndex[] distinct = { new IntIndex(1), new IntIndex(3), new IntIndex(5) };
        for (IntIndex idx : distinct) {
            model.addReference(idx);
        }
        assertEquals(3, model.getReferences().size());
        // Attempt to add a duplicate (3)
        IntIndex duplicate = new IntIndex(3);
        model.addReference(duplicate);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "Set size should remain unchanged after adding a duplicate.");
        assertTrue(refs.containsAll(Arrays.asList(distinct)));
    }
}
