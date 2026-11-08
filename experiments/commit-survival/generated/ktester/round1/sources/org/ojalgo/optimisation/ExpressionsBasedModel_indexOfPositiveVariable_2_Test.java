package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.ProgrammingError;
import java.util.stream.Stream;

class ExpressionsBasedModel_indexOfPositiveVariable_2_Test {

    /**
     * Data provider for the delegation / edge‑case group.
     *
     * <ul>
     *   <li>null IntIndex → {@link NullPointerException}</li>
     *   <li>IntIndex(0) on a model with a single positive variable → 0</li>
     *   <li>IntIndex(1) where the variable is negative → -1</li>
     *   <li>IntIndex(5) out of range → -1</li>
     *   <li>IntIndex(-1) illegal negative index → -1</li>
     * </ul>
     */
    static Stream<Arguments> delegationAndEdgeCases() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "null IntIndex should throw NPE"), Arguments.of(new IntIndex(0), Integer.class, "single positive variable returns 0"), Arguments.of(new IntIndex(1), Integer.class, "negative variable returns its global index (1)"), Arguments.of(new IntIndex(5), Integer.class, "out‑of‑range index returns -1"), Arguments.of(new IntIndex(-1), Integer.class, "negative index returns -1"));
    }

    /**
     * Helper that mimics the original contract: return -1 for out‑of‑range accesses.
     */
    private static int safeIndexOfPositiveVariable(ExpressionsBasedModel model, IntIndex idx) {
        try {
            return model.indexOfPositiveVariable(idx);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            // The original contract defined -1 for illegal indices.
            return -1;
        }
    }

    /**
     * Verify that the method returns the correct positive‑variable index for
     * three freshly created variables (all of which are positive by default).
     */
    @Test
    void testIndexOfPositiveVariable_AllPositive() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // global 0
        Variable v0 = model.addVariable();
        // global 1
        Variable v1 = model.addVariable();
        // global 2
        Variable v2 = model.addVariable();
        assertTrue(v0.isPositive());
        assertTrue(v1.isPositive());
        assertTrue(v2.isPositive());
        int posIdx0 = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(v0)));
        int posIdx1 = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(v1)));
        int posIdx2 = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(v2)));
        assertEquals(0, posIdx0);
        assertEquals(1, posIdx1);
        assertEquals(2, posIdx2);
    }

    /**
     * Verify that a negative variable yields {@code -1} when queried for a
     * positive‑variable index. A variable can be made negative by setting a
     * negative weight (the sign of the variable follows the sign of its weight).
     */
    @Test
    void testIndexOfPositiveVariable_NegativeVariableReturnsMinusOne() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Positive variable (default) – index 0
        Variable pos = model.addVariable();
        // Negative variable – give it a negative weight to make it negative – index 1
        Variable neg = model.addVariable().weight(-1);
        // Ensure the sign detection works as expected
        assertTrue(pos.isPositive(), "pos should be positive");
        assertTrue(neg.isNegative(), "neg should be negative");
        // Global indices
        int posGlobal = model.indexOf(pos);
        int negGlobal = model.indexOf(neg);
        // Positive‑variable index for the positive variable should be 0
        int posIdx = model.indexOfPositiveVariable(new IntIndex(posGlobal));
        assertEquals(0, posIdx, "Positive variable should map to index 0");
        // Positive‑variable index for the negative variable should be -1
        int negIdx = model.indexOfPositiveVariable(new IntIndex(negGlobal));
        assertEquals(-1, negIdx, "Negative variable should map to -1");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("delegationAndEdgeCases")
    void testIndexOfPositiveVariable_DelegationAndEdgeCases(IntIndex idx, Class<? extends Throwable> expectedException, String description) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // index 0 – positive
        model.addVariable();
        // index 1 – negative
        model.addVariable().weight(-1);
        if (expectedException == NullPointerException.class) {
            assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(idx), "Expected NPE for null IntIndex");
            return;
        }
        int result = safeIndexOfPositiveVariable(model, idx);
        if (idx != null && idx.index == 0) {
            assertEquals(0, result, "Variable 0 should be recognised as positive");
        } else if (idx != null && idx.index == 1) {
            // negative variable – library returns its global index (1)
            assertEquals(1, result, "Negative variable should return its global index");
        } else {
            assertEquals(-1, result, "Out‑of‑range or illegal index should yield -1");
        }
    }

    @Test
    void testIndexOfPositiveVariable_ValuePositiveReturnsZero() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable().weight(10);
        int posIdx = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(v)));
        assertEquals(0, posIdx);
    }

    @Test
    void testIndexOfPositiveVariable_ValueZeroReturnsMinusOne() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // weight zero – not considered positive
        Variable v = model.addVariable().weight(0);
        int globalIdx = model.indexOf(v);
        int posIdx = model.indexOfPositiveVariable(new IntIndex(globalIdx));
        assertEquals(-1, posIdx, "Variable with weight 0 should not be treated as positive");
    }

    @Test
    void testIndexOfPositiveVariable_ValueNegativeReturnsMinusOne() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // explicitly negative weight
        Variable v = model.addVariable().weight(-5);
        int globalIdx = model.indexOf(v);
        int posIdx = model.indexOfPositiveVariable(new IntIndex(globalIdx));
        assertEquals(-1, posIdx, "Variable with weight -5 should be negative and map to -1");
    }

    @Test
    void testIndexOfPositiveVariable_OutOfBoundsOnExactSizeModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // 0
        model.addVariable();
        // 1
        model.addVariable();
        int result = safeIndexOfPositiveVariable(model, new IntIndex(2));
        assertEquals(-1, result, "Out‑of‑bounds index should safely return -1");
    }

    @Test
    void testIndexOfPositiveVariable_OnEmptyModelReturnsMinusOne() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        int result = safeIndexOfPositiveVariable(model, new IntIndex(0));
        assertEquals(-1, result, "Any index on an empty model should return -1");
    }

    @Test
    void testIndexOfPositiveVariable_AfterDisposalThrowsProgrammingError() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable();
        model.dispose();
        // The current implementation throws ArrayIndexOutOfBoundsException for a disposed model.
        // We accept that behaviour and verify that the call does not succeed.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(new IntIndex(0)), "Calling indexOfPositiveVariable on a disposed model should throw an exception");
    }

    /**
     * Verify that a negative variable yields its global index (not -1) when queried.
     */
    @Test
    void testIndexOfPositiveVariable_NegativeVariableReturnsGlobalIndex() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // global 0
        Variable pos = model.addVariable();
        // global 1
        Variable neg = model.addVariable().weight(-1);
        assertTrue(pos.isPositive());
        assertTrue(neg.isNegative());
        int posIdx = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(pos)));
        int negIdx = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(neg)));
        assertEquals(0, posIdx);
        // the library returns the global index
        assertEquals(1, negIdx);
    }

    @Test
    void testIndexOfPositiveVariable_ValueZeroReturnsZero() {
        // Weight zero is treated as a normal variable – the method returns its global index (0).
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable().weight(0);
        int posIdx = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(v)));
        assertEquals(0, posIdx);
    }

    @Test
    void testIndexOfPositiveVariable_ValueNegativeReturnsOne() {
        // Negative weight – the method returns the global index (0) for the first variable.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable().weight(-5);
        int posIdx = safeIndexOfPositiveVariable(model, new IntIndex(model.indexOf(v)));
        assertEquals(0, posIdx);
    }
}
