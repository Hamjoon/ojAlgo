package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ojalgo.structure.Structure1D.IntIndex;

class ExpressionsBasedModel_indexOfPositiveVariable_Test {

    @Nested
    @DisplayName("Positive Variable Indexing – edge and cache behaviour")
    class PositiveVariableIndexing {

        @Test
        @DisplayName("Calling indexOfPositiveVariable on an empty model throws ArrayIndexOutOfBoundsException")
        void testEmptyModelThrows() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(0), "Empty model should throw because internal array length is 0");
        }

        @Test
        @DisplayName("Single free variable with positive upper limit yields index 0")
        void testSinglePositiveVariable() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // default positive
            model.addVariable();
            assertEquals(0, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Single free variable with negative upper limit yields -1")
        void testSingleNegativeUpperLimit() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            v.upper(BigDecimal.valueOf(-5));
            assertEquals(-1, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Single fixed variable yields -1")
        void testSingleFixedVariable() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            v.lower(BigDecimal.ONE).upper(BigDecimal.ONE);
            assertEquals(-1, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Single free variable with explicit positive upper limit yields index 0")
        void testSinglePositiveUpperLimit() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            v.upper(BigDecimal.valueOf(10));
            assertEquals(0, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Three mixed variables produce expected positive indices [0, -1, 1]")
        void testThreeMixedVariables() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // var0 – free, no upper limit (positive)
            model.addVariable();
            // var1 – free, upper limit = -3 (negative)
            Variable var1 = model.addVariable();
            var1.upper(BigDecimal.valueOf(-3));
            // var2 – free, upper limit = 7 (positive)
            Variable var2 = model.addVariable();
            var2.upper(BigDecimal.valueOf(7));
            int[] actual = { model.indexOfPositiveVariable(0), model.indexOfPositiveVariable(1), model.indexOfPositiveVariable(2) };
            assertArrayEquals(new int[] { 0, -1, 1 }, actual, "Positive indices should match the expected ordering");
        }

        @Test
        @DisplayName("Cache refresh after model growth – newly added positive variable gets correct index")
        void testCacheRefreshAfterAddingVariable() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // Initial two variables: var0 positive, var1 negative
            // var0 – positive
            model.addVariable();
            Variable var1 = model.addVariable();
            // var1 – negative
            var1.upper(BigDecimal.valueOf(-2));
            // First call builds the internal cache
            assertEquals(0, model.indexOfPositiveVariable(0));
            // Add a third positive variable; internal arrays are now shorter than model size
            // positive by default
            Variable var2 = model.addVariable();
            // The next call must detect size mismatch and rebuild the cache
            assertEquals(1, model.indexOfPositiveVariable(2), "After adding a new positive variable the index should be 1");
        }

        @Test
        @DisplayName("Requesting positive index beyond current variable count throws ArrayIndexOutOfBoundsException")
        void testPositiveIndexOutOfBounds() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.addVariable();
            // two variables total
            model.addVariable();
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(2), "Accessing index 2 on a model with only 2 variables should throw");
        }

        @Test
        @DisplayName("Variable with upper limit exactly zero is not positive (returns -1)")
        void testZeroUpperLimitIsNotPositive() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            v.upper(BigDecimal.ZERO);
            assertEquals(-1, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Integer variable with positive upper limit is still classified as positive")
        void testIntegerVariablePositiveClassification() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            v.integer(true).upper(BigDecimal.valueOf(3));
            assertEquals(0, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Upper limit modification after first call does NOT refresh cache")
        void testUpperLimitModificationNotRefreshed() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            // initially negative → -1
            v.upper(BigDecimal.valueOf(-4));
            // First call builds cache and returns -1
            assertEquals(-1, model.indexOfPositiveVariable(0));
            // Change upper limit to positive; cache size unchanged, so result stays -1
            v.upper(BigDecimal.valueOf(5));
            assertEquals(-1, model.indexOfPositiveVariable(0), "Cache should not be refreshed when only bounds change");
        }

        @Test
        @DisplayName("Positive variable with positive lower limit is still positive")
        void testPositiveVariableWithPositiveLowerLimit() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable();
            v.lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(10));
            assertEquals(0, model.indexOfPositiveVariable(0));
        }

        @Test
        @DisplayName("Fixed variable ignored in positive indexing – mixed model")
        void testFixedVariableIgnoredInMixedModel() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // var0 – free, positive
            Variable var0 = model.addVariable();
            var0.upper(BigDecimal.valueOf(5));
            // var1 – fixed
            Variable var1 = model.addVariable();
            var1.lower(BigDecimal.ONE).upper(BigDecimal.ONE);
            // var2 – free, positive
            Variable var2 = model.addVariable();
            var2.upper(BigDecimal.valueOf(8));
            // Expected: var0 -> 0, var1 -> -1, var2 -> 1
            assertEquals(0, model.indexOfPositiveVariable(0), "var0 should be first positive");
            assertEquals(-1, model.indexOfPositiveVariable(1), "fixed var1 must return -1");
            assertEquals(1, model.indexOfPositiveVariable(2), "var2 should be second positive");
        }

        @Test
        @DisplayName("Null categorisation object leads to NullPointerException")
        void testNullCategorisationThrowsNPE() throws Exception {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // ensure myVariables is non‑empty
            model.addVariable();
            // Use reflection to set the private field myVariablesCategorisation to null
            Field catField = ExpressionsBasedModel.class.getDeclaredField("myVariablesCategorisation");
            catField.setAccessible(true);
            catField.set(model, null);
            assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(0), "Calling method with null categorisation should throw NPE");
        }
    }

    @Nested
    @DisplayName("Overloads handling")
    class OverloadTests {

        @Test
        @DisplayName("indexOfPositiveVariable(Variable) delegates correctly")
        void testOverloadWithVariable() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // positive
            Variable v = model.addVariable();
            assertEquals(0, model.indexOfPositiveVariable(v));
        }

        @Test
        @DisplayName("indexOfPositiveVariable(IntIndex) delegates correctly")
        void testOverloadWithIntIndex() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // creates index 0
            model.addVariable();
            assertEquals(0, model.indexOfPositiveVariable(IntIndex.of(0)));
        }
    }

    /**
     * Verify that a variable without any bounds is considered positive and receives index 0.
     */
    @Test
    void testDefaultVariableIsPositive() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // default: free, no upper limit -> positive
        Variable v0 = model.addVariable();
        int globalIdx = model.indexOf(v0);
        int positiveIdx = model.indexOfPositiveVariable(globalIdx);
        assertEquals(0, positiveIdx, "First positive variable should have index 0");
        assertEquals(0, model.indexOfPositiveVariable(v0), "Overload with Variable should match");
    }

    /**
     * Verify that a variable with an upper limit of zero (or negative) is NOT considered positive.
     */
    @Test
    void testUpperLimitZeroIsNotPositive() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // positive
        Variable v0 = model.addVariable();
        // will be made non‑positive
        Variable v1 = model.addVariable();
        // upper limit = 0 → not positive
        v1.upper(BigDecimal.ZERO);
        int idxV0 = model.indexOfPositiveVariable(v0);
        int idxV1 = model.indexOfPositiveVariable(v1);
        assertEquals(0, idxV0, "First positive variable should be index 0");
        assertEquals(-1, idxV1, "Variable with upper limit 0 must return -1");
    }

    /**
     * Verify that a fixed variable (lower == upper) is not considered positive.
     */
    @Test
    void testFixedVariableIsNotPositive() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable fixed = model.addVariable();
        // fixed at 1
        fixed.lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        int idx = model.indexOfPositiveVariable(fixed);
        assertEquals(-1, idx, "Fixed variable must not be part of positive variables");
    }

    /**
     * Verify ordering of positive indices when a mixture of positive, negative and fixed variables is present.
     */
    @Test
    void testPositiveIndexOrdering() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Positive variables
        // default positive
        Variable p0 = model.addVariable();
        // default positive
        Variable p1 = model.addVariable();
        // Negative variable (upper limit negative)
        Variable n0 = model.addVariable();
        // makes it negative, not positive
        n0.upper(BigDecimal.valueOf(-5));
        // Another positive variable with explicit positive upper limit
        Variable p2 = model.addVariable();
        // positive upper limit → positive
        p2.upper(BigDecimal.valueOf(10));
        // Expected positive order: p0, p1, p2
        assertEquals(0, model.indexOfPositiveVariable(p0), "p0 should be first positive");
        assertEquals(1, model.indexOfPositiveVariable(p1), "p1 should be second positive");
        assertEquals(-1, model.indexOfPositiveVariable(n0), "n0 should not be positive");
        assertEquals(2, model.indexOfPositiveVariable(p2), "p2 should be third positive");
    }

    /**
     * Verify that the overload taking an {@link IntIndex} works correctly.
     */
    @Test
    void testIndexOfPositiveVariableWithIntIndex() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // positive
        Variable v = model.addVariable();
        int globalIdx = model.indexOf(v);
        int positiveIdx = model.indexOfPositiveVariable(IntIndex.of(globalIdx));
        assertEquals(0, positiveIdx, "Positive variable via IntIndex should return 0");
    }
}