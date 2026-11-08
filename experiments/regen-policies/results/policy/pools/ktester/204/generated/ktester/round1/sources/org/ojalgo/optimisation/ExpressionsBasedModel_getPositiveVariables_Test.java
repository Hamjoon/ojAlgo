package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class ExpressionsBasedModel_getPositiveVariables_Test {

    private ExpressionsBasedModel model;

    /**
     * Helper class to describe how a variable should be configured for a test case.
     */
    private static final class VariableSetup {

        final String name;

        final BigDecimal lower;

        final BigDecimal upper;

        final BigDecimal fixed;

        final boolean integer;

        VariableSetup(String name, BigDecimal lower, BigDecimal upper, BigDecimal fixed, boolean integer) {
            this.name = name;
            this.lower = lower;
            this.upper = upper;
            this.fixed = fixed;
            this.integer = integer;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for every test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC, not strictly necessary
        model = null;
    }

    /**
     * Data provider for {@link #testPositiveVariablesSelection}.
     */
    private static Stream<Arguments> selectionCases() {
        return Stream.of(// Variable v1 – free, no upper limit
        Arguments.of("Variable v1 is free and has no upper limit set", new VariableSetup[] { new VariableSetup("v1", null, null, null, false) }, new String[] { "v1" }), // Variable v2 – free, upper = 10
        Arguments.of("Variable v2 is free and its upper limit is set to a positive value (10)", new VariableSetup[] { new VariableSetup("v2", null, BigDecimal.TEN, null, false) }, new String[] { "v2" }), // Variable v3 – free, upper = 0
        Arguments.of("Variable v3 is free but its upper limit is set to zero; signum == 0", new VariableSetup[] { new VariableSetup("v3", null, BigDecimal.ZERO, null, false) }, new String[] {}), // Variable v4 – free, upper = -5
        Arguments.of("Variable v4 is free but its upper limit is negative (-5)", new VariableSetup[] { new VariableSetup("v4", null, new BigDecimal("-5"), null, false) }, new String[] {}), // Variable v5 – fixed (level set)
        Arguments.of("Variable v5 is fixed (value set) regardless of its upper limit", new VariableSetup[] { new VariableSetup("v5", null, null, new BigDecimal("7"), false) }, new String[] {}), // Mixed model
        Arguments.of("Mixed model: v1 (no upper) and v2 (positive upper) are positive", new VariableSetup[] { new VariableSetup("v1", null, null, null, false), new VariableSetup("v2", null, BigDecimal.TEN, null, false), new VariableSetup("v3", null, BigDecimal.ZERO, null, false), new VariableSetup("v4", null, new BigDecimal("-5"), null, false), new VariableSetup("v5", null, null, new BigDecimal("3"), false) }, new String[] { "v1", "v2" }), // Integer variable without upper limit
        Arguments.of("A free integer variable without an upper limit set is still classified as positive", new VariableSetup[] { new VariableSetup("vInt", null, null, null, true) }, new String[] { "vInt" }), // Variable with lower limit only (negative lower, no upper)
        Arguments.of("A free variable with a lower limit set (negative) but no upper limit is positive", new VariableSetup[] { new VariableSetup("vLow", new BigDecimal("-3"), null, null, false) }, new String[] { "vLow" }), // Mixed integer and continuous positive variables
        Arguments.of("Both a free integer variable (vPosInt) with a positive upper limit and a free continuous variable (vPosCont) with no upper limit are positive", new VariableSetup[] { new VariableSetup("vPosInt", null, new BigDecimal("5"), null, true), new VariableSetup("vPosCont", null, null, null, false) }, new String[] { "vPosInt", "vPosCont" }));
    }

    /**
     * An empty model must yield an empty (but non‑null) list.
     */
    @Test
    void testEmptyModelReturnsEmptyList() {
        List<Variable> positives = model.getPositiveVariables();
        assertNotNull(positives, "The returned list must never be null");
        assertTrue(positives.isEmpty(), "A model with no variables should return an empty list");
        assertThrows(UnsupportedOperationException.class, () -> positives.add(null), "The list must be unmodifiable");
    }

    /**
     * Variables with various limit configurations are filtered correctly.
     */
    @Test
    void testPositiveVariablesFilteringAndOrder() {
        // 1. Positive: lower bound 0, upper bound 10  → meets the “upper > 0” rule
        Variable vPos1 = model.addVariable().lower(0).upper(10);
        // 2. Not positive: upper bound exactly 0 (signum == 0)
        Variable vZeroUpper = model.addVariable().upper(0);
        // 3. Not positive: upper bound negative
        Variable vNegUpper = model.addVariable().upper(-5);
        // 4. Positive: no limits at all (upper not set)
        Variable vPos2 = model.addVariable();
        // 5. Fixed variable (level) – should never appear in the positive list
        Variable vFixed = model.addVariable().level(5);
        List<Variable> positives = model.getPositiveVariables();
        // Expected order: vPos1, vPos2 (the only variables that satisfy the predicate,
        // in the order they were added)
        assertEquals(2, positives.size(), "Exactly two variables should be classified as positive");
        assertEquals(vPos1, positives.get(0), "First positive variable should be the first qualifying variable");
        assertEquals(vPos2, positives.get(1), "Second positive variable should be the second qualifying variable");
        // Verify immutability of the returned list
        assertThrows(UnsupportedOperationException.class, () -> positives.add(vZeroUpper), "Returned list must be unmodifiable");
    }

    /**
     * Changing a variable after the list has been obtained does not affect the previously returned list.
     */
    @Test
    void testListIsSnapshotNotLiveView() {
        // initially positive
        Variable v = model.addVariable().upper(10);
        List<Variable> firstCall = model.getPositiveVariables();
        assertEquals(1, firstCall.size());
        // Now make the variable non‑positive by setting an upper limit of 0
        v.upper(0);
        List<Variable> secondCall = model.getPositiveVariables();
        // The first list must stay unchanged (snapshot semantics)
        assertEquals(1, firstCall.size(), "First call list must remain unchanged");
        assertEquals(0, secondCall.size(), "After modification the new call should reflect the change");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("selectionCases")
    void testPositiveVariablesSelection(String description, VariableSetup[] setups, String[] expectedNames) {
        // create variables according to the supplied setups
        for (VariableSetup setup : setups) {
            Variable var = model.addVariable();
            if (setup.name != null) {
                var.setName(setup.name);
            }
            if (setup.lower != null) {
                var.lower(setup.lower);
            }
            if (setup.upper != null) {
                var.upper(setup.upper);
            }
            if (setup.fixed != null) {
                var.level(setup.fixed);
            }
            if (setup.integer) {
                var.integer(true);
            }
        }
        List<Variable> positives = model.getPositiveVariables();
        assertEquals(expectedNames.length, positives.size(), "Unexpected number of positive variables for case: " + description);
        for (int i = 0; i < expectedNames.length; i++) {
            assertEquals(expectedNames[i], positives.get(i).getName(), "Unexpected variable at position " + i + " for case: " + description);
        }
    }

    @Test
    void testUnmodifiableListAddThrows() {
        model.addVariable().upper(10);
        List<Variable> positives = model.getPositiveVariables();
        assertThrows(UnsupportedOperationException.class, () -> positives.add(null), "Adding to the returned list must throw UnsupportedOperationException");
    }

    @Test
    void testUnmodifiableListRemoveThrows() {
        model.addVariable().upper(10);
        List<Variable> positives = model.getPositiveVariables();
        assertThrows(UnsupportedOperationException.class, () -> positives.remove(0), "Removing from the returned list must throw UnsupportedOperationException");
    }

    @Test
    void testPositiveVariablesCachingSizeUnchanged() {
        // Variable initially negative upper → not positive
        Variable v = model.addVariable().upper(-5);
        List<Variable> first = model.getPositiveVariables();
        assertTrue(first.isEmpty(), "Initially there should be no positive variables");
        // Change upper to a positive value without adding/removing variables
        v.upper(10);
        List<Variable> second = model.getPositiveVariables();
        // Because the model size did not change, the categorisation cache is not refreshed
        assertTrue(second.isEmpty(), "Cache should not have been refreshed – list must still be empty");
    }

    @Test
    void testPositiveVariablesCachingAfterAdd() {
        // First call with a single positive variable
        Variable v1 = model.addVariable().upper(10);
        List<Variable> first = model.getPositiveVariables();
        assertEquals(1, first.size());
        assertEquals(v1, first.get(0));
        // Add another positive variable – size changes, cache must be recomputed
        // no upper limit → positive
        Variable v2 = model.addVariable();
        List<Variable> second = model.getPositiveVariables();
        assertEquals(2, second.size(), "After adding a variable the list must be recomputed");
        assertEquals(v1, second.get(0));
        assertEquals(v2, second.get(1));
    }

    @Test
    void testConcurrentModificationDuringIteration() {
        // ensure the list is non‑empty
        model.addVariable().upper(10);
        List<Variable> positives = model.getPositiveVariables();
        // Modify the model while holding the unmodifiable view
        assertThrows(ConcurrentModificationException.class, () -> {
            // captures modCount
            var iterator = positives.iterator();
            // structural modification
            model.addVariable().upper(5);
            while (iterator.hasNext()) {
                iterator.next();
            }
        });
    }

    @Test
    void testVariableUpperLimitNullNPE() throws Exception {
        // Create a mock Variable that pretends to have an upper limit set but returns null
        Variable malformed = org.mockito.Mockito.mock(Variable.class);
        org.mockito.Mockito.when(malformed.isFixed()).thenReturn(false);
        org.mockito.Mockito.when(malformed.isUpperLimitSet()).thenReturn(true);
        org.mockito.Mockito.when(malformed.getUpperLimit()).thenReturn(null);
        org.mockito.Mockito.when(malformed.isLowerLimitSet()).thenReturn(false);
        org.mockito.Mockito.when(malformed.isInteger()).thenReturn(false);
        org.mockito.Mockito.when(malformed.getName()).thenReturn("malformed");
        // Inject the mock into the private myVariables list via reflection
        Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> internal = (List<Variable>) varField.get(model);
        internal.add(malformed);
        // The call should now throw NPE because getUpperLimit() returns null
        assertThrows(NullPointerException.class, () -> model.getPositiveVariables(), "A null upper limit should cause NullPointerException");
    }
}