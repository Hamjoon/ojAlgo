package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removePresolver_Test {

    /**
     * Simple presolver that uses an integer id for equality / ordering.
     */
    static class DummyPresolver extends ExpressionsBasedModel.Presolver {

        private final int id;

        // optional, used for the “equal but distinct” test
        private final String name;

        DummyPresolver(int id) {
            this(id, null);
        }

        DummyPresolver(int id, String name) {
            // the real Presolver constructor expects an int priority
            super(id);
            this.id = id;
            this.name = name;
        }

        @Override
        public int compareTo(ExpressionsBasedModel.Presolver other) {
            // Order by id; if ids are equal, use name to guarantee deterministic ordering
            if (other instanceof DummyPresolver) {
                DummyPresolver o = (DummyPresolver) other;
                int cmp = Integer.compare(this.id, o.id);
                if (cmp != 0) {
                    return cmp;
                }
                return Objects.compare(this.name, o.name, Comparator.nullsFirst(String::compareTo));
            }
            return super.compareTo(other);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DummyPresolver)) {
                return false;
            }
            DummyPresolver that = (DummyPresolver) obj;
            return this.id == that.id && Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "DummyPresolver{id=" + id + (name != null ? ", name='" + name + '\'' : "") + '}';
        }
    }

    /**
     * Presolver whose {@code compareTo} deliberately throws an {@link IllegalStateException}.
     */
    static class FaultyPresolver extends ExpressionsBasedModel.Presolver {

        FaultyPresolver(int id) {
            super(id);
        }

        @Override
        public int compareTo(ExpressionsBasedModel.Presolver o) {
            throw new IllegalStateException("compareTo is broken");
        }
    }

    /**
     * A class that does NOT implement {@link Comparable}.  It will be inserted into the
     * {@code PRESOLVERS} set via reflection; removal will trigger a {@link ClassCastException}.
     */
    static class NonComparablePresolver {

        final int id;

        NonComparablePresolver(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "NonComparablePresolver{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid cross‑test contamination
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Verify that a presolver that has been added can be removed successfully.
     */
    @Test
    void testRemoveExistingPresolver() {
        // GIVEN – a presolver added to the static collection
        ExpressionsBasedModel.Presolver presolver = new ExpressionsBasedModel.Presolver(1) {
        };
        boolean added = ExpressionsBasedModel.addPresolver(presolver);
        assertTrue(added, "Presolver should be added successfully");
        // WHEN – we attempt to remove it
        boolean removed = ExpressionsBasedModel.removePresolver(presolver);
        // THEN – the removal should succeed
        assertTrue(removed, "removePresolver should return true for an existing presolver");
    }

    /**
     * Verify that attempting to remove a presolver that is not present returns {@code false}.
     */
    @Test
    void testRemoveNonExistingPresolver() {
        // GIVEN – a presolver that has never been added
        ExpressionsBasedModel.Presolver presolver = new ExpressionsBasedModel.Presolver(99) {
        };
        // WHEN – we try to remove it
        boolean removed = ExpressionsBasedModel.removePresolver(presolver);
        // THEN – the method should indicate failure
        assertFalse(removed, "removePresolver should return false when the presolver is not present");
    }

    /**
     * Verify that after removal the internal collection no longer contains the presolver.
     * We rely on the return value of {@code addPresolver} and {@code removePresolver}
     * because the underlying {@code PRESOLVERS} set is package‑private.
     */
    @Test
    void testAddThenRemoveLeavesCollectionEmpty() {
        // GIVEN – a presolver added to the collection
        ExpressionsBasedModel.Presolver presolver = new ExpressionsBasedModel.Presolver(5) {
        };
        assertTrue(ExpressionsBasedModel.addPresolver(presolver));
        // WHEN – it is removed
        assertTrue(ExpressionsBasedModel.removePresolver(presolver));
        // THEN – attempting to remove it again should fail, indicating the collection is empty
        assertFalse(ExpressionsBasedModel.removePresolver(presolver));
    }

    /**
     * Verify that passing {@code null} propagates the {@link NullPointerException}
     * thrown by {@link TreeSet#remove(Object)}.
     */
    @Test
    void testRemoveNullPresolver() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.removePresolver(null), "removePresolver should propagate NullPointerException for null argument");
    }

    /**
     * Verify that a presolver can be removed only once; a second removal returns {@code false}.
     */
    @Test
    void testDuplicateRemoval() {
        DummyPresolver presolver = new DummyPresolver(3);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "Presolver should be added");
        assertTrue(ExpressionsBasedModel.removePresolver(presolver), "First removal should succeed");
        assertFalse(ExpressionsBasedModel.removePresolver(presolver), "Second removal should fail");
    }

    /**
     * Verify that after {@link ExpressionsBasedModel#clearPresolvers()} the removal fails.
     */
    @Test
    void testRemoveAfterClearPresolvers() {
        DummyPresolver presolver = new DummyPresolver(4);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "Presolver should be added");
        // clear the static set
        ExpressionsBasedModel.clearPresolvers();
        assertFalse(ExpressionsBasedModel.removePresolver(presolver), "Removal should fail because the set was cleared");
    }

    /**
     * Verify that two distinct {@code DummyPresolver} instances that are equal according to
     * {@code compareTo}/{@code equals} are treated as the same element by the {@code TreeSet}.
     */
    @Test
    void testEqualButDistinctInstanceRemoval() {
        DummyPresolver first = new DummyPresolver(5, "A");
        // equal by id & name
        DummyPresolver second = new DummyPresolver(5, "A");
        assertTrue(ExpressionsBasedModel.addPresolver(first), "First instance should be added");
        // The second instance is NOT added (TreeSet ignores duplicates), but we can still attempt removal.
        assertTrue(ExpressionsBasedModel.removePresolver(second), "Removal with an equal but distinct instance should succeed");
    }

    /**
     * Verify that a presolver which does not implement {@link Comparable} causes a
     * {@link ClassCastException} when {@code removePresolver} is invoked.
     *
     * <p>We insert the non‑comparable object into the static {@code PRESOLVERS}
     * set via reflection to avoid a compile‑time error.</p>
     */
    @Test
    void testNonComparablePresolverRemoval() throws Exception {
        // Insert a non‑comparable presolver directly into the raw TreeSet
        NonComparablePresolver nonComp = new NonComparablePresolver(6);
        TreeSet rawSet = getPresolversSet();
        // first element can be added without comparison
        rawSet.add(nonComp);
        // Now attempt removal – TreeSet will try to compare and fail
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.removePresolver((ExpressionsBasedModel.Presolver) (Object) nonComp), "Removal should throw ClassCastException for non‑comparable element");
    }

    /**
     * Verify that a presolver whose {@code compareTo} throws an {@link IllegalStateException}
     * propagates that exception during removal.
     *
     * <p>We add the faulty presolver via reflection to avoid the exception on {@code addPresolver}.</p>
     */
    @Test
    void testFaultyPresolverCompareToThrows() throws Exception {
        FaultyPresolver faulty = new FaultyPresolver(7);
        TreeSet rawSet = getPresolversSet();
        // first element added without comparison
        rawSet.add(faulty);
        assertThrows(IllegalStateException.class, () -> ExpressionsBasedModel.removePresolver(faulty), "Removal should propagate IllegalStateException thrown by compareTo");
    }

    /**
     * Retrieves the private static {@code PRESOLVERS} field via reflection.
     *
     * @return the raw {@link TreeSet} instance used by {@code ExpressionsBasedModel}
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static TreeSet getPresolversSet() throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        field.setAccessible(true);
        return (TreeSet) field.get(null);
    }
}