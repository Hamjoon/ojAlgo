package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_2_Test {

    private ExpressionsBasedModel model;

    // will be fixed (lower == upper)
    private Variable varFixed;

    private Variable varFree1;

    private Variable varFree2;

    // free (used in some tests)
    private Variable varFree3;

//     @BeforeEach
    void setupBeforeEach() {
        // Default model used by most tests – can be re‑initialised inside a test if needed.
        model = new ExpressionsBasedModel();
        // Fixed variable (global index 0)
        varFixed = model.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ZERO);
        // Free variables (global indices 1, 2, 3)
        varFree1 = model.newVariable("free1").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        varFree2 = model.newVariable("free2").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        varFree3 = model.newVariable("free3").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
    }

//     @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    private static Stream<Arguments> provideMultipleFree() {
        return Stream.of(Arguments.of(0, 0), Arguments.of(1, 1), Arguments.of(2, 2));
    }

    private static Stream<Arguments> provideMixedVariables() {
        return Stream.of(// fixed, free, fixed, free
        // first (fixed)
        Arguments.of(0, -1), // first free
        Arguments.of(1, 0), // second fixed
        Arguments.of(2, -1), // second free
        Arguments.of(3, 1));
    }

    /**
     * Verify that a fixed variable is not considered a free variable.
     * The expected return value for a fixed variable is {@code -1}.
     */
//     @Test
//     void testFixedVariableReturnsMinusOne() {
//         // index of varFixed
//         IntIndex fixedIdx = IntIndex.of(0);
//         int result = model.indexOfFreeVariable(fixedIdx);
//         assertEquals(-1, result, "Fixed variables should return -1 when queried with indexOfFreeVariable");
//     }

    /**
     * Verify that the first free variable (global index 1) maps to free‑variable index 0.
     */
//     @Test
//     void testFirstFreeVariableMapping() {
//         // index of varFree1
//         IntIndex freeIdx = IntIndex.of(1);
//         int result = model.indexOfFreeVariable(freeIdx);
//         assertEquals(0, result, "The first free variable should map to free‑variable index 0");
//     }

    /**
     * Verify that the second free variable (global index 2) maps to free‑variable index 1.
     */
//     @Test
//     void testSecondFreeVariableMapping() {
//         // index of varFree2
//         IntIndex freeIdx = IntIndex.of(2);
//         int result = model.indexOfFreeVariable(freeIdx);
//         assertEquals(1, result, "The second free variable should map to free‑variable index 1");
//     }

    /**
     * Cross‑check the mapping against the list returned by {@link ExpressionsBasedModel#getFreeVariables()}.
     */
//     @Test
//     void testMappingConsistentWithGetFreeVariables() {
//         List<Variable> freeVars = model.getFreeVariables();
//         // The list should contain exactly the two free variables we created.
//         assertEquals(2, freeVars.size(), "Model should report exactly two free variables");
//         assertTrue(freeVars.contains(varFree1), "Free variables list should contain varFree1");
//         assertTrue(freeVars.contains(varFree2), "Free variables list should contain varFree2");
//         // Verify that the indices returned by indexOfFreeVariable correspond to the positions
//         // in the free‑variables list.
//         assertEquals(0, model.indexOfFreeVariable(IntIndex.of(model.indexOf(varFree1))), "varFree1 should map to position 0 in the free‑variables list");
//         assertEquals(1, model.indexOfFreeVariable(IntIndex.of(model.indexOf(varFree2))), "varFree2 should map to position 1 in the free‑variables list");
//     }

    /**
     * Ensure that calling the method with a {@code null} argument throws a {@link NullPointerException}.
     */
//     @Test
//     void testNullArgumentThrowsException() {
//         assertThrows(NullPointerException.class, () -> model.indexOfFreeVariable(null), "Calling indexOfFreeVariable with null should throw NullPointerException");
//     }

    /**
     * 1. Empty model – no variables at all.
     * Expect -1 for any supplied index because there is nothing to map.
     */
//     @Test
//     void indexOfFreeVariable_noVariables() {
//         // no variables added
//         ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
//         IntIndex idx = IntIndex.of(0);
//         int result = emptyModel.indexOfFreeVariable(idx);
//         assertEquals(-1, result, "Empty model should return -1 for any index");
//     }

    /**
     * 2. Single free variable – should map to free‑variable index 0.
     */
//     @Test
//     void indexOfFreeVariable_singleFree() {
//         ExpressionsBasedModel singleModel = new ExpressionsBasedModel();
//         singleModel.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
//         int result = singleModel.indexOfFreeVariable(IntIndex.of(0));
//         assertEquals(0, result, "Single free variable should map to index 0");
//     }

    /**
     * 3. Multiple free variables – three free variables, indices 0‑2 map to free indices 0‑2.
     */
    @ParameterizedTest(name = "Free variable at global index {0} → free index {1}")
    @MethodSource("provideMultipleFree")
    void indexOfFreeVariable_multipleFree(int globalIdx, int expectedFreeIdx) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // create three free variables
        for (int i = 0; i < 3; i++) {
            m.newVariable("v" + i).lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        }
        int actual = m.indexOfFreeVariable(IntIndex.of(globalIdx));
        assertEquals(expectedFreeIdx, actual);
    }

    /**
     * 4. Fixed variable – a model with a single fixed variable must return -1.
     */
//     @Test
//     void indexOfFreeVariable_fixedVariable() {
//         ExpressionsBasedModel fixedModel = new ExpressionsBasedModel();
//         fixedModel.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ZERO);
//         int result = fixedModel.indexOfFreeVariable(IntIndex.of(0));
//         assertEquals(-1, result, "Fixed variable should return -1");
//     }

    /**
     * 5. Mixed variables – fixed, free, fixed, free.
     * Expected mapping: 0→-1, 1→0, 2→-1, 3→1.
     */
    @ParameterizedTest(name = "Mixed model: global index {0} → expected {1}")
    @MethodSource("provideMixedVariables")
    void indexOfFreeVariable_mixedVariables(int globalIdx, int expected) {
        ExpressionsBasedModel mixed = new ExpressionsBasedModel();
        // index 0 – fixed
        mixed.newVariable("fixed0").lower(BigDecimal.valueOf(1)).upper(BigDecimal.valueOf(1)).weight(BigDecimal.ZERO);
        // index 1 – free
        mixed.newVariable("free1").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        // index 2 – fixed
        mixed.newVariable("fixed2").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ZERO);
        // index 3 – free
        mixed.newVariable("free3").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        int actual = mixed.indexOfFreeVariable(IntIndex.of(globalIdx));
        assertEquals(expected, actual);
    }

    /**
     * 6. Null argument – already covered above but retained for completeness.
     */
//     @Test
//     void indexOfFreeVariable_nullArgument() {
//         assertThrows(NullPointerException.class, () -> model.indexOfFreeVariable(null));
//     }

    /**
     * 7. Negative index – should raise {@link IllegalArgumentException}.
     */
//     @Test
//     void indexOfFreeVariable_negativeIndex() {
//         assertThrows(IllegalArgumentException.class, () -> model.indexOfFreeVariable(IntIndex.of(-1)));
//     }

    /**
     * 8. Index beyond the number of variables – should raise {@link IndexOutOfBoundsException}.
     * The default {@code model} created in {@link #setupBeforeEach()} contains four variables
     * (indices 0‑3). Index 10 is therefore out of range.
     */
//     @Test
//     void indexOfFreeVariable_indexBeyondVariables() {
//         assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfFreeVariable(IntIndex.of(10)));
//     }

    /**
     * 9. After {@link ExpressionsBasedModel#dispose()} – any further call must raise
     * {@link IllegalStateException}.
     */
//     @Test
//     void indexOfFreeVariable_afterDispose() {
//         model.dispose();
//         assertThrows(IllegalStateException.class, () -> model.indexOfFreeVariable(IntIndex.of(0)));
//     }
}