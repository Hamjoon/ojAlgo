package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addReference_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates an empty reference set.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model will be garbage‑collected.
        model = null;
    }

    /**
     * Supplies the {@link IntIndex} instances required for the basic test.
     * The JSON test case uses {@code model.toIntIndex(3)} – we create the same
     * index via a temporary model instance (the method is non‑static).
     */
    static Stream<IntIndex> basicReferenceProvider() {
        ExpressionsBasedModel tmp = new ExpressionsBasedModel();
        return Stream.of(tmp.toIntIndex(3));
    }

    @Test
    void testAddReferenceAddsIndex() {
        IntIndex idx = new IntIndex(3);
        // Pre‑condition: the reference set is empty.
        Set<IntIndex> before = model.getReferences();
        assertTrue(before.isEmpty(), "Reference set should start empty");
        // Invoke the method under test.
        model.addReference(idx);
        // Post‑condition: the set now contains the added index.
        Set<IntIndex> after = model.getReferences();
        assertEquals(1, after.size(), "Reference set should contain exactly one element");
        assertTrue(after.contains(idx), "Reference set should contain the added IntIndex");
    }

    @Test
    void testAddReferenceIsIdempotent() {
        IntIndex idx = model.toIntIndex(7);
        // Add the same index twice.
        model.addReference(idx);
        model.addReference(idx);
        // Because the backing collection is a Set, the index should appear only once.
        Set<IntIndex> refs = model.getReferences();
        assertEquals(1, refs.size(), "Adding the same IntIndex twice should not create duplicates");
        assertTrue(refs.contains(idx));
    }

    @Test
    void testAddMultipleDifferentReferences() {
        IntIndex idx1 = model.toIntIndex(1);
        IntIndex idx2 = model.toIntIndex(2);
        IntIndex idx3 = model.toIntIndex(5);
        model.addReference(idx1);
        model.addReference(idx2);
        model.addReference(idx3);
        Set<IntIndex> refs = model.getReferences();
        assertEquals(3, refs.size(), "All distinct indices should be stored");
        assertTrue(refs.containsAll(Set.of(idx1, idx2, idx3)));
    }

    /* -------------------------------------------------------------------------
     *  Basic behaviour – a single, valid IntIndex is added to an empty model.
     * ------------------------------------------------------------------------- */
    @ParameterizedTest(name = "addReference_basic case {index}")
    @MethodSource("basicReferenceProvider")
    void testAddReferenceBasic(IntIndex index) {
        // Pre‑condition: the reference set must be empty.
        Set<IntIndex> before = model.getReferences();
        assertTrue(before.isEmpty(), "Reference set should start empty");
        // Invoke the method under test.
        model.addReference(index);
        // Post‑condition: the set now contains exactly the supplied index.
        Set<IntIndex> after = model.getReferences();
        assertEquals(1, after.size(), "Reference set should contain exactly one element after adding a single index");
        assertTrue(after.contains(index), "Reference set should contain the added IntIndex");
    }

    /* -------------------------------------------------------------------------
     *  Null handling – the contract (as captured by the JSON test case) expects a
     *  NullPointerException when a null argument is supplied.
     * ------------------------------------------------------------------------- */
    @Test
    void testAddReferenceNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.addReference(null), "Adding a null IntIndex should throw NullPointerException");
    }
}
