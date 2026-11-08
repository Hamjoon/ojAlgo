package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_clearPresolvers_Test {

    private static Field PRESOLVERS_FIELD;

    /**
     * Minimal concrete implementation of {@link ExpressionsBasedModel.Presolver} that is comparable.
     * The actual behaviour of a presolver is irrelevant for these tests – we only need an instance
     * that can be stored in the {@code TreeSet}.
     */
    private static final class DummyPresolver extends ExpressionsBasedModel.Presolver implements Comparable<ExpressionsBasedModel.Presolver> {

        private final int id;

        DummyPresolver(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(ExpressionsBasedModel.Presolver o) {
            if (o instanceof DummyPresolver) {
                return Integer.compare(this.id, ((DummyPresolver) o).id);
            }
            // Fallback ordering – deterministic but arbitrary.
            return Integer.compare(this.id, o.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof DummyPresolver) && ((DummyPresolver) obj).id == this.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the package‑private static field that holds the presolvers.
        PRESOLVERS_FIELD = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        PRESOLVERS_FIELD.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side effects.
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Reset accessibility – tidy, though not strictly required.
        PRESOLVERS_FIELD.setAccessible(false);
    }

    /**
     * Helper that returns the current size of the internal PRESOLVERS set.
     */
    @SuppressWarnings("unchecked")
    private int getPresolversSize() throws IllegalAccessException {
        TreeSet<ExpressionsBasedModel.Presolver> set = (TreeSet<ExpressionsBasedModel.Presolver>) PRESOLVERS_FIELD.get(null);
        return set.size();
    }

    @Test
    void testClearPresolversEmptiesSet() throws Exception {
        // GIVEN a presolver has been added
        DummyPresolver presolver = new DummyPresolver(1);
        boolean added = ExpressionsBasedModel.addPresolver(presolver);
        assertTrue(added, "Presolver should be added successfully");
        assertEquals(1, getPresolversSize(), "Presolvers set should contain exactly one element");
        // WHEN clearPresolvers() is invoked
        ExpressionsBasedModel.clearPresolvers();
        // THEN the internal set must be empty
        assertEquals(0, getPresolversSize(), "Presolvers set should be empty after clearPresolvers()");
    }

    @Test
    void testClearPresolversWhenAlreadyEmpty() throws Exception {
        // GIVEN the presolvers set is already empty
        assertEquals(0, getPresolversSize(), "Presolvers set should start empty");
        // WHEN clearPresolvers() is called
        ExpressionsBasedModel.clearPresolvers();
        // THEN no exception is thrown and the set remains empty
        assertEquals(0, getPresolversSize(), "Presolvers set should remain empty after clearPresolvers()");
    }

    /**
     * Helper that returns the current {@code TreeSet} instance holding the presolvers.
     */
//     @SuppressWarnings("unchecked")
//     private TreeSet<Presolver> getPresolversSet() throws IllegalAccessException {
//         return (TreeSet<ExpressionsBasedModel.Presolver>) PRESOLVERS_FIELD.get(null);
//     }

    @Test
    void testClearPresolversRemovesMultipleEntries() throws Exception {
        // GIVEN two distinct presolvers have been added
        DummyPresolver p1 = new DummyPresolver(10);
        DummyPresolver p2 = new DummyPresolver(20);
        assertTrue(ExpressionsBasedModel.addPresolver(p1));
        assertTrue(ExpressionsBasedModel.addPresolver(p2));
        assertEquals(2, getPresolversSize(), "Presolvers set should contain two elements");
        // WHEN clearPresolvers() is invoked
        ExpressionsBasedModel.clearPresolvers();
        // THEN the set is empty
        assertEquals(0, getPresolversSize(), "Presolvers set should be empty after clearing multiple entries");
    }

    @Test
    void testClearPresolversIsIdempotent() throws Exception {
        // GIVEN an empty presolvers set
        assertEquals(0, getPresolversSize());
        // WHEN clearPresolvers() is called repeatedly
        ExpressionsBasedModel.clearPresolvers();
        // second call
        ExpressionsBasedModel.clearPresolvers();
        // THEN no exception is thrown and the set remains empty
        assertEquals(0, getPresolversSize(), "Presolvers set should stay empty after repeated clearPresolvers() calls");
    }

    @Test
    void testAddAfterClearPresolversKeepsOnlyNewEntries() throws Exception {
        // GIVEN two presolvers are added
        DummyPresolver p1 = new DummyPresolver(1);
        DummyPresolver p2 = new DummyPresolver(2);
        assertTrue(ExpressionsBasedModel.addPresolver(p1));
        assertTrue(ExpressionsBasedModel.addPresolver(p2));
        assertEquals(2, getPresolversSize(), "Presolvers set should contain two elements before clear");
        // WHEN clearPresolvers() is invoked and a new presolver is added afterwards
        ExpressionsBasedModel.clearPresolvers();
        DummyPresolver p3 = new DummyPresolver(3);
        assertTrue(ExpressionsBasedModel.addPresolver(p3));
        // THEN the set contains only the newly added presolver
        TreeSet<ExpressionsBasedModel.Presolver> set = getPresolversSet();
        assertEquals(1, set.size(), "Presolvers set should contain exactly one element after clear and add");
        assertTrue(set.contains(p3), "The remaining element should be the newly added presolver");
    }

    @Test
    void testClearPresolversWhenSingleEntryExists() throws Exception {
        // GIVEN a single presolver in the set
        DummyPresolver p = new DummyPresolver(42);
        assertTrue(ExpressionsBasedModel.addPresolver(p));
        assertEquals(1, getPresolversSize());
        // WHEN clearPresolvers() is called
        ExpressionsBasedModel.clearPresolvers();
        // THEN the set becomes empty
        assertEquals(0, getPresolversSize(), "Presolvers set should be empty after clearing a single entry");
    }
}