package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addReference_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        model = null;
    }

    private static Stream<Arguments> singleIndexProvider() {
        // Using the public (package‑private) toIntIndex factory for readability
        ExpressionsBasedModel dummy = new ExpressionsBasedModel();
        return Stream.of(// typical positive index
        Arguments.of(dummy.toIntIndex(5)), // zero index
        Arguments.of(dummy.toIntIndex(0)), // negative index (allowed by the Set)
        Arguments.of(dummy.toIntIndex(-2)));
    }

    @Test
    void testAddSingleReference() {
        IntIndex index = new IntIndex(3);
        // package‑private method – accessible because test is in same package
        model.addReference(index);
        Set<IntIndex> refs = model.getReferences();
        assertNotNull(refs, "References set should never be null");
        assertEquals(1, refs.size(), "Exactly one reference should have been added");
        assertTrue(refs.contains(index), "The added index must be present in the references set");
    }

    @Test
    void testAddDuplicateReferenceDoesNotIncreaseSize() {
        IntIndex index = model.toIntIndex(3);
        // first insertion
        model.addReference(index);
        // duplicate insertion
        model.addReference(index);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Adding the same index twice must not create duplicates");
        assertTrue(refs.contains(index), "Reference set must still contain the index after duplicate add");
    }

    @Test
    void testAddMultipleDistinctReferences() {
        IntIndex idx1 = model.toIntIndex(1);
        IntIndex idx2 = model.toIntIndex(4);
        IntIndex idx3 = model.toIntIndex(7);
        model.addReference(idx1);
        model.addReference(idx2);
        model.addReference(idx3);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "All distinct indices should be stored");
        assertTrue(refs.contains(idx1));
        assertTrue(refs.contains(idx2));
        assertTrue(refs.contains(idx3));
    }

    @Test
    void testInitialReferencesEmpty() {
        Set<IntIndex> refs = model.getReferences();
        assertNotNull(refs, "Reference set should never be null after construction");
        assertEquals(0, refs.size(), "Newly created model must start with an empty reference set");
    }

    @Test
    void testAddReferenceNullThrows() {
        assertThrows(NullPointerException.class, () -> model.addReference(null), "Passing a null IntIndex should result in a NullPointerException");
    }
}
