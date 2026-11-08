package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getFixedVariables_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test case
        model = new ExpressionsBasedModel();
        // three variables – two will be fixed, one will stay free
        // fixed (lower == upper)
        varA = model.newVariable("A").level(BigDecimal.valueOf(5));
        // fixed
        varB = model.newVariable("B").level(BigDecimal.valueOf(10));
        // free
        varC = model.newVariable("C");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model will be garbage‑collected
        model = null;
    }

    /**
     * Convenience to obtain the {@link IntIndex} for a given variable.
     */
    private IntIndex indexOf(Variable v) {
        return model.toIntIndex(model.indexOf(v));
    }

    /**
     * Makes a variable fixed by setting identical lower and upper bounds.
     */
    private void fixVariable(Variable v, BigDecimal value) {
        // The public API provides several ways; using level() is the simplest.
        v.level(value);
    }

    /**
     * Un‑fixes a variable (removes the equality bound).
     */
    private void unfixVariable(Variable v) {
        // The Variable API offers an explicit unfix method; if not present,
        // resetting the lower/upper bounds to null also works.
        try {
            // try the explicit method
            v.unfix();
        } catch (Exception ignored) {
            // fallback – clear bounds (implementation‑specific)
            v.lower(null);
            v.upper(null);
        }
    }

    @Test
    void testFixedVariablesReturned() {
        Set<IntIndex> fixed = model.getFixedVariables();
        // Expect exactly the two fixed variables
        assertEquals(2, fixed.size(), "There should be two fixed variables");
        // Verify that the set contains the correct indices
        assertTrue(fixed.contains(IntIndex.of(model.indexOf(varA))), "Set should contain index of variable A");
        assertTrue(fixed.contains(IntIndex.of(model.indexOf(varB))), "Set should contain index of variable B");
        assertFalse(fixed.contains(IntIndex.of(model.indexOf(varC))), "Set should not contain index of variable C");
    }

    @Test
    void testReturnedSetIsUnmodifiableAndRecomputed() {
        Set<IntIndex> firstCall = model.getFixedVariables();
        // The set must be unmodifiable – any mutating operation should throw
        assertThrows(UnsupportedOperationException.class, () -> firstCall.add(IntIndex.of(0)), "Returned set should be unmodifiable");
        // Now fix the previously free variable
        varC.level(BigDecimal.valueOf(7));
        // A fresh call must reflect the new state
        Set<IntIndex> secondCall = model.getFixedVariables();
        assertEquals(3, secondCall.size(), "All three variables should now be fixed");
        assertTrue(secondCall.contains(IntIndex.of(model.indexOf(varC))), "Set should now contain index of variable C");
    }

    @Test
    void emptyModelReturnsEmptyUnmodifiableSet() {
        // Create an empty model (no variables)
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        Set<IntIndex> fixed = emptyModel.getFixedVariables();
        assertTrue(fixed.isEmpty(), "Empty model should yield an empty set");
        assertThrows(UnsupportedOperationException.class, () -> fixed.add(indexOf(varA)), "Returned set must be unmodifiable");
    }

    @Test
    void nonFixedVariableYieldsEmptySet() {
        // a free variable, not fixed
        model.addVariable();
        Set<IntIndex> fixed = model.getFixedVariables();
        assertTrue(fixed.isEmpty(), "No fixed variables should result in an empty set");
        assertThrows(UnsupportedOperationException.class, fixed::clear, "Returned set must be unmodifiable");
    }

    @Test
    void singleFixedVariableIsReturned() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ONE);
        Set<IntIndex> fixed = model.getFixedVariables();
        assertEquals(1, fixed.size(), "Exactly one variable should be reported as fixed");
        assertTrue(fixed.contains(indexOf(v)), "Set must contain the index of the fixed variable");
        assertThrows(UnsupportedOperationException.class, () -> fixed.remove(indexOf(v)), "Returned set must be unmodifiable");
    }

    @Test
    void multipleFixedVariablesOnlyReturned() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        fixVariable(v1, BigDecimal.valueOf(2));
        // v2 stays free
        fixVariable(v3, BigDecimal.valueOf(5));
        Set<IntIndex> fixed = model.getFixedVariables();
        assertEquals(2, fixed.size(), "Two variables are fixed");
        assertTrue(fixed.contains(indexOf(v1)));
        assertTrue(fixed.contains(indexOf(v3)));
        assertFalse(fixed.contains(indexOf(v2)));
    }

    @Test
    void returnedSetIsUnmodifiableAddOperation() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ONE);
        Set<IntIndex> fixed = model.getFixedVariables();
        assertThrows(UnsupportedOperationException.class, () -> fixed.add(indexOf(v)), "add() on the returned set must throw UnsupportedOperationException");
    }

    @Test
    void setRecomputedAfterStateChange() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        fixVariable(v1, BigDecimal.ONE);
        Set<IntIndex> first = model.getFixedVariables();
        assertEquals(1, first.size());
        assertTrue(first.contains(indexOf(v1)));
        // now fix the second variable
        fixVariable(v2, BigDecimal.ONE);
        Set<IntIndex> second = model.getFixedVariables();
        assertEquals(2, second.size());
        assertTrue(second.contains(indexOf(v2)));
    }

    @Test
    void unfixingVariableRemovesItFromReturnedSet() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ONE);
        assertTrue(model.getFixedVariables().contains(indexOf(v)));
        unfixVariable(v);
        Set<IntIndex> after = model.getFixedVariables();
        assertFalse(after.contains(indexOf(v)), "Variable should no longer appear after being unfixed");
        assertTrue(after.isEmpty(), "No other variables are fixed");
    }

    @Test
    void removeOperationThrowsUnsupportedOperationException() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ONE);
        Set<IntIndex> fixed = model.getFixedVariables();
        assertThrows(UnsupportedOperationException.class, () -> fixed.remove(indexOf(v)), "remove() must throw UnsupportedOperationException");
    }

    @Test
    void consecutiveCallsWithoutChangesReturnEqualSets() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        fixVariable(v1, BigDecimal.ONE);
        fixVariable(v2, BigDecimal.ONE);
        Set<IntIndex> set1 = model.getFixedVariables();
        Set<IntIndex> set2 = model.getFixedVariables();
        assertEquals(set1, set2, "Two consecutive calls should return equal sets");
        assertThrows(UnsupportedOperationException.class, set1::clear, "Both sets must be unmodifiable");
    }

    @Test
    void largeModelFixedEveryThirdVariable() {
        final int total = 50;
        List<Variable> vars = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            Variable v = model.addVariable("V" + i);
            vars.add(v);
            // fix every third variable (0,3,6,…)
            if (i % 3 == 0) {
                fixVariable(v, BigDecimal.valueOf(i));
            }
        }
        Set<IntIndex> fixed = model.getFixedVariables();
        int expectedSize = (int) Math.ceil(total / 3.0);
        assertEquals(expectedSize, fixed.size(), "Expected " + expectedSize + " fixed variables out of " + total);
        // verify each expected index is present
        for (int i = 0; i < total; i += 3) {
            assertTrue(fixed.contains(indexOf(vars.get(i))), "Fixed set must contain index of variable at position " + i);
        }
    }

    @Test
    void zeroValueIsConsideredFixed() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ZERO);
        Set<IntIndex> fixed = model.getFixedVariables();
        assertTrue(fixed.contains(indexOf(v)), "Variable fixed to ZERO must appear in the set");
    }

    @Test
    void nullEntryInInternalVariableListThrowsNPE() throws Exception {
        // Insert a null into the private myVariables list via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> internal = (List<Variable>) field.get(model);
        // deliberate null entry
        internal.add(null);
        // The method iterates over the list and should hit a NPE
        assertThrows(NullPointerException.class, () -> model.getFixedVariables(), "A null entry in myVariables must cause NullPointerException");
    }

    @Test
    void clearOperationThrowsUnsupportedOperationException() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ONE);
        Set<IntIndex> fixed = model.getFixedVariables();
        assertThrows(UnsupportedOperationException.class, fixed::clear, "clear() must throw UnsupportedOperationException");
    }

    @Test
    void addAllOperationThrowsUnsupportedOperationException() {
        Variable v = model.addVariable();
        fixVariable(v, BigDecimal.ONE);
        Set<IntIndex> fixed = model.getFixedVariables();
        Set<IntIndex> other = Collections.singleton(indexOf(v));
        assertThrows(UnsupportedOperationException.class, () -> fixed.addAll(other), "addAll() must throw UnsupportedOperationException");
    }

    @Test
    void originalThreeVariableScenario() {
        // fix A and B via level(), leave C free
        varA.level(BigDecimal.valueOf(5));
        varB.level(BigDecimal.valueOf(10));
        // varC stays free
        Set<IntIndex> fixed = model.getFixedVariables();
        assertEquals(2, fixed.size(), "Two variables should be reported as fixed");
        assertTrue(fixed.contains(indexOf(varA)));
        assertTrue(fixed.contains(indexOf(varB)));
        assertFalse(fixed.contains(indexOf(varC)));
    }
}
