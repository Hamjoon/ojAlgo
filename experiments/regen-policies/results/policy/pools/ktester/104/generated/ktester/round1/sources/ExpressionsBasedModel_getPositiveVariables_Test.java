package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getPositiveVariables_Test {

    private ExpressionsBasedModel model;

    // free, upper limit > 0  → positive
    private Variable varFreePositive;

    // free, upper limit < 0  → NOT positive
    private Variable varFreeNegative;

    // fixed (level)          → NOT positive
    private Variable varFixedPositive;

    // free, no upper limit   → positive
    private Variable varFreeNoUpper;

    // integer, upper > 0   → positive
    private Variable varIntegerPositive;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // 1. free variable with a positive upper limit → should appear in the list
        varFreePositive = model.addVariable().lower(0).upper(10).weight(1);
        // 2. free variable with a negative upper limit → should NOT appear
        varFreeNegative = model.addVariable().lower(-20).upper(-5).weight(1);
        // 3. fixed variable (level sets both lower and upper to the same value) → NOT appear
        varFixedPositive = model.addVariable().level(3).weight(1);
        // 4. free variable with no upper limit set → should appear
        varFreeNoUpper = model.addVariable().lower(0).weight(1);
        // 5. integer variable with a positive upper limit → should appear
        varIntegerPositive = model.addVariable().integer(true).lower(0).upper(5).weight(1);
    }

    @Test
    void testPositiveVariablesFilteringAndOrder() {
        List<Variable> positives = model.getPositiveVariables();
        // Expected variables: varFreePositive, varFreeNoUpper, varIntegerPositive
        assertEquals(3, positives.size(), "Unexpected number of positive variables");
        // Verify order – it must follow the global index order (i.e. addition order)
        assertSame(varFreePositive, positives.get(0), "First positive variable is incorrect");
        assertSame(varFreeNoUpper, positives.get(1), "Second positive variable is incorrect");
        assertSame(varIntegerPositive, positives.get(2), "Third positive variable is incorrect");
        // Ensure that variables that should not be positive are indeed absent
        assertFalse(positives.contains(varFreeNegative), "Negative‑upper‑limit variable must not be present");
        assertFalse(positives.contains(varFixedPositive), "Fixed variable must not be present");
    }

    @Test
    void testReturnedListIsUnmodifiable() {
        List<Variable> positives = model.getPositiveVariables();
        assertThrows(UnsupportedOperationException.class, () -> positives.add(null), "The list returned by getPositiveVariables() must be unmodifiable");
        assertThrows(UnsupportedOperationException.class, positives::clear, "The list returned by getPositiveVariables() must be unmodifiable");
    }

    @Test
    void testListReflectsModelChangesAfterInitialCall() {
        // Capture the list before adding a new variable
        List<Variable> initialList = model.getPositiveVariables();
        int initialSize = initialList.size();
        // Add another free variable with a positive upper limit
        Variable newVar = model.addVariable().lower(0).upper(7).weight(1);
        // The underlying list is updated lazily; a fresh call must reflect the change
        List<Variable> refreshedList = model.getPositiveVariables();
        assertEquals(initialSize + 1, refreshedList.size(), "After adding a new positive variable the refreshed list size must increase");
        assertTrue(refreshedList.contains(newVar), "The refreshed list must contain the newly added variable");
    }

    @Test
    void testSingleFreeVariableNoUpperLimitIsPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.addVariable().lower(BigDecimal.ZERO).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertEquals(1, list.size(), "A free variable without an upper limit must be considered positive");
        assertSame(v, list.get(0), "The returned variable should be the one that was added");
    }

    @Test
    void testSingleFreeVariablePositiveUpperLimitIsPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable v = m.addVariable().lower(-5).upper(10).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertEquals(1, list.size(), "Variable with positive upper limit must be classified as positive");
        assertSame(v, list.get(0));
    }

    @Test
    void testSingleFreeVariableZeroUpperLimitIsNotPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.addVariable().lower(-5).upper(BigDecimal.ZERO).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertTrue(list.isEmpty(), "Upper limit zero should result in an empty positive‑variables list");
    }

    @Test
    void testSingleFreeVariableNegativeUpperLimitIsNotPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.addVariable().lower(-10).upper(-5).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertTrue(list.isEmpty(), "Negative upper limit must exclude the variable from the positive list");
    }

    @Test
    void testSingleFixedVariableIsNeverPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        m.addVariable().level(3).upper(10).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertTrue(list.isEmpty(), "Fixed variables must never appear in the positive‑variables list");
    }

    @Test
    void testMultipleVariablesMixedClassification() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // v6 – free, no upper limit → positive
        Variable v6 = m.addVariable().lower(0).weight(1);
        // v7 – free, negative upper limit → not positive
        m.addVariable().lower(-10).upper(-3).weight(1);
        // v8 – free, positive upper limit → positive
        Variable v8 = m.addVariable().lower(0).upper(2).weight(1);
        // v9 – fixed, upper limit positive → not positive
        m.addVariable().level(5).upper(5).weight(1);
        List<Variable> positives = m.getPositiveVariables();
        assertEquals(2, positives.size(), "Only v6 and v8 should be classified as positive");
        assertSame(v6, positives.get(0), "First positive variable should be v6 (insertion order)");
        assertSame(v8, positives.get(1), "Second positive variable should be v8");
    }

    @Test
    void testEmptyModelReturnsEmptyList() {
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        List<Variable> list = emptyModel.getPositiveVariables();
        assertTrue(list.isEmpty(), "A model with no variables must return an empty list");
    }

    @Test
    void testIntegerVariableIsConsideredPositiveIfUpperIsPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable vInt = m.addVariable().integer(true).lower(0).upper(5).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertEquals(1, list.size(), "Integer variable with positive upper limit must be present");
        assertSame(vInt, list.get(0));
    }

    @Test
    void testVariableWithPositiveLowerLimitIsStillPositiveWhenUpperIsPositive() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        Variable vMixed = m.addVariable().lower(3).upper(8).weight(1);
        List<Variable> list = m.getPositiveVariables();
        assertEquals(1, list.size(), "Variable should be positive regardless of its lower limit");
        assertSame(vMixed, list.get(0));
    }

    @Test
    void testOrderPreservationWithMixedUpperLimits() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // vA – free, no upper limit → positive
        Variable vA = m.addVariable().lower(0).weight(1);
        // vB – free, negative upper limit → not positive
        m.addVariable().lower(-5).upper(-2).weight(1);
        // vC – free, positive upper limit → positive
        Variable vC = m.addVariable().lower(0).upper(4).weight(1);
        List<Variable> positives = m.getPositiveVariables();
        assertEquals(2, positives.size(), "Only vA and vC should be returned");
        assertSame(vA, positives.get(0), "First element must be vA");
        assertSame(vC, positives.get(1), "Second element must be vC");
    }

    @Test
    void testCacheIsNotRefreshedWhenOnlyVariablePropertiesChange() {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // vStale – initially positive (no upper limit)
        Variable vStale = m.addVariable().lower(0).weight(1);
        // First call builds the cache and includes vStale
        List<Variable> firstCall = m.getPositiveVariables();
        assertTrue(firstCall.contains(vStale), "vStale must be present after first call");
        // Change the upper limit to a negative value – cache should NOT be rebuilt automatically
        vStale.upper(-10);
        // Second call should still return the stale list (still contains vStale)
        List<Variable> secondCall = m.getPositiveVariables();
        assertTrue(secondCall.contains(vStale), "Cache is not refreshed on property change; vStale should still be present");
    }

    @Test
    void testUnmodifiableListRemoveThrowsException() {
        List<Variable> positives = model.getPositiveVariables();
        assertThrows(UnsupportedOperationException.class, () -> positives.remove(0), "remove() on the unmodifiable list must throw UnsupportedOperationException");
    }

    @Test
    void testUnmodifiableListSetThrowsException() {
        List<Variable> positives = model.getPositiveVariables();
        // Create a variable via the model to use as replacement
        Variable another = model.addVariable().weight(1);
        assertThrows(UnsupportedOperationException.class, () -> positives.set(0, another), "set() on the unmodifiable list must throw UnsupportedOperationException");
    }

    @Test
    void testUnmodifiableListAddAllThrowsException() {
        List<Variable> positives = model.getPositiveVariables();
        Variable extra = model.addVariable().weight(1);
        assertThrows(UnsupportedOperationException.class, () -> positives.addAll(Collections.singletonList(extra)), "addAll() on the unmodifiable list must throw UnsupportedOperationException");
    }

    @Test
    void testIteratorRemoveThrowsException() {
        List<Variable> positives = model.getPositiveVariables();
        Iterator<Variable> it = positives.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "remove() on iterator of unmodifiable list must throw UnsupportedOperationException");
    }
}