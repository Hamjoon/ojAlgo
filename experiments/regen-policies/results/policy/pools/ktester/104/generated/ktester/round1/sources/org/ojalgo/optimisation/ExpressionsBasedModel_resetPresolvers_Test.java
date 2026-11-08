package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_resetPresolvers_Test {

    /**
     * --------------------------------------------------------------------- *
     */
    private static class DummyPresolver extends ExpressionsBasedModel.Presolver implements Comparable<DummyPresolver> {

        private final String name;

        DummyPresolver(String name) {
            this.name = name;
        }

        @Override
        public int compareTo(DummyPresolver o) {
            // All dummy presolvers are ordered by their name.
            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return "DummyPresolver{" + name + '}';
        }

        // equals / hashCode based on name to avoid duplicate entries in the TreeSet
        @Override
        public boolean equals(Object obj) {
            return (obj instanceof DummyPresolver) && name.equals(((DummyPresolver) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private static class NonComparablePresolver extends ExpressionsBasedModel.Presolver {
        // No Comparable implementation – used to provoke ClassCastException.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        ExpressionsBasedModel.clearPresolvers();
        // Ensure the PRESOLVERS set is empty before each test.
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test so that other tests are not affected.
        ExpressionsBasedModel.clearPresolvers();
        //  restored it in a finally block; this is a safety net.)
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Helper to replace a static field in {@link Presolvers}.
     */
    private static Object replacePresolverField(String fieldName, Object newValue) throws Exception {
        Field field = Presolvers.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object old = field.get(null);
        field.set(null, newValue);
        return old;
    }

    @Test
    void testResetPresolversAddsDefaultPresolvers() throws Exception {
        // Pre‑condition: the presolver set must be empty.
        ExpressionsBasedModel.clearPresolvers();
        // Invoke the method under test.
        ExpressionsBasedModel.resetPresolvers();
        // Access the package‑private static PRESOLVERS field via reflection.
        Field presolversField = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
        presolversField.setAccessible(true);
        @SuppressWarnings("unchecked")
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = (TreeSet<ExpressionsBasedModel.Presolver>) presolversField.get(null);
        // Verify that exactly the three default presolvers are present.
        assertEquals(3, presolvers.size(), "PRESOLVERS should contain exactly three entries after resetPresolvers()");
        assertTrue(presolvers.contains(Presolvers.ZERO_ONE_TWO), "PRESOLVERS must contain Presolvers.ZERO_ONE_TWO");
        assertTrue(presolvers.contains(Presolvers.INTEGER), "PRESOLVERS must contain Presolvers.INTEGER");
        assertTrue(presolvers.contains(Presolvers.REDUNDANT_CONSTRAINT), "PRESOLVERS must contain Presolvers.REDUNDANT_CONSTRAINT");
    }

    /**
     * Helper to obtain the static {@code PRESOLVERS} set via reflection.
     */
//     @SuppressWarnings("unchecked")
//     private static TreeSet<Presolver> getPresolversSet() throws Exception {
//         Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
//         field.setAccessible(true);
//         return (TreeSet<ExpressionsBasedModel.Presolver>) field.get(null);
//     }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testIdempotenceOfResetPresolvers() throws Exception {
        ExpressionsBasedModel.resetPresolvers();
        // second call
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(3, presolvers.size(), "Calling resetPresolvers() twice must not increase the set size");
        assertTrue(presolvers.containsAll(java.util.List.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testInteractionWithExistingCustomPresolver() throws Exception {
        // add a custom presolver before resetting
        DummyPresolver custom = new DummyPresolver("CustomA");
        ExpressionsBasedModel.addPresolver(custom);
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(4, presolvers.size(), "Custom presolver should be preserved while defaults are added");
        assertTrue(presolvers.contains(custom));
        assertTrue(presolvers.contains(Presolvers.ZERO_ONE_TWO));
        assertTrue(presolvers.contains(Presolvers.INTEGER));
        assertTrue(presolvers.contains(Presolvers.REDUNDANT_CONSTRAINT));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testPartialDefaultsPresent() throws Exception {
        // only ZERO_ONE_TWO is present initially
        ExpressionsBasedModel.addPresolver(Presolvers.ZERO_ONE_TWO);
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(3, presolvers.size(), "Missing defaults must be added without duplicating existing ones");
        assertTrue(presolvers.containsAll(java.util.List.of(Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testReaddingRemovedDefaultPresolver() throws Exception {
        // add then remove INTEGER
        ExpressionsBasedModel.addPresolver(Presolvers.INTEGER);
        ExpressionsBasedModel.removePresolver(Presolvers.INTEGER);
        // now reset – INTEGER should be restored
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(3, presolvers.size(), "All three defaults must be present after reset even if one was removed before");
        assertTrue(presolvers.contains(Presolvers.INTEGER));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testCustomPresolverEqualToDefaultInstance() throws Exception {
        // add the default instance explicitly
        ExpressionsBasedModel.addPresolver(Presolvers.ZERO_ONE_TWO);
        // reset – should still be only three elements
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(3, presolvers.size(), "Adding a default presolver a second time must not increase the set size");
        assertTrue(presolvers.contains(Presolvers.ZERO_ONE_TWO));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testResetAfterMultipleCustomPresolvers() throws Exception {
        DummyPresolver a = new DummyPresolver("CustomA");
        DummyPresolver b = new DummyPresolver("CustomB");
        ExpressionsBasedModel.addPresolver(a);
        ExpressionsBasedModel.addPresolver(b);
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(5, presolvers.size(), "Two custom presolvers plus three defaults should be present");
        assertTrue(presolvers.containsAll(java.util.List.of(a, b, Presolvers.ZERO_ONE_TWO, Presolvers.INTEGER, Presolvers.REDUNDANT_CONSTRAINT)));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testIdempotenceAfterRemovalOfDefault() throws Exception {
        // remove one default
        ExpressionsBasedModel.removePresolver(Presolvers.REDUNDANT_CONSTRAINT);
        // first reset – missing default restored
        ExpressionsBasedModel.resetPresolvers();
        // second reset – should not add anything further
        ExpressionsBasedModel.resetPresolvers();
        TreeSet<ExpressionsBasedModel.Presolver> presolvers = getPresolversSet();
        assertEquals(3, presolvers.size(), "After restoring a missing default, subsequent resets must be idempotent");
        assertTrue(presolvers.contains(Presolvers.REDUNDANT_CONSTRAINT));
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testResetPresolversWithNullDefaultPresolver() throws Exception {
        // Replace ZERO_ONE_TWO with null, remember original to restore later
        Object original = replacePresolverField("ZERO_ONE_TWO", null);
        try {
            assertThrows(NullPointerException.class, ExpressionsBasedModel::resetPresolvers, "Adding a null presolver should result in NullPointerException");
        } finally {
            // restore original value
            replacePresolverField("ZERO_ONE_TWO", original);
        }
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Test
    void testResetPresolversWithNonComparableDefaultPresolver() throws Exception {
        // Create a presolver that does NOT implement Comparable
        NonComparablePresolver nonComp = new NonComparablePresolver();
        // Replace INTEGER with the non‑comparable instance
        Object original = replacePresolverField("INTEGER", nonComp);
        try {
            assertThrows(ClassCastException.class, ExpressionsBasedModel::resetPresolvers, "Adding a non‑comparable presolver to TreeSet should throw ClassCastException");
        } finally {
            // Restore the original default
            replacePresolverField("INTEGER", original);
        }
    }
}