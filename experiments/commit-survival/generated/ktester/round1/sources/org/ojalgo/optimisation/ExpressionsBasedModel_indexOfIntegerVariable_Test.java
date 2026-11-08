package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionsBasedModel_indexOfIntegerVariable_Test {

    private ExpressionsBasedModel model;

    /**
     * Group: Out‑of‑range global index handling.
     *
     * The model contains four variables (indices 0‑3). Accessing -1 or 5 must throw
     * {@link ArrayIndexOutOfBoundsException}.
     */
    @Nested
    class OutOfRangeIndexTests {

        @BeforeEach
        void populateModel() {
            // 0 – integer
            model.addVariable().integer(true).lower(0).upper(5);
            // 1 – continuous
            model.addVariable().lower(0).upper(10);
            // 2 – integer
            model.addVariable().integer(true).lower(0).upper(5);
            // 3 – integer
            model.addVariable().integer(true).lower(0).upper(5);
        }

        @ParameterizedTest(name = "globalIndex={0} → throws ArrayIndexOutOfBoundsException")
        @CsvSource({ "-1", "5" })
        void testOutOfRangeIndices(int globalIndex) {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(globalIndex), "Accessing out‑of‑range index " + globalIndex + " should throw ArrayIndexOutOfBoundsException");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests
        model = null;
    }

    @Test
    void testIndexOfIntegerVariable_WithMixedVariables() {
        // Variable 0 – non‑integer
        Variable var0 = model.addVariable();
        // regular continuous variable
        var0.lower(0).upper(10);
        // Variable 1 – integer
        Variable var1 = model.addVariable();
        var1.integer(true).lower(0).upper(5);
        // Variable 2 – integer
        Variable var2 = model.addVariable();
        var2.integer(true).lower(0).upper(5);
        // Global indices are 0,1,2 respectively.
        // Expected integer‑variable indices:
        //   global 0 -> -1 (not integer)
        //   global 1 -> 0  (first integer variable)
        //   global 2 -> 1  (second integer variable)
        assertEquals(-1, model.indexOfIntegerVariable(0), "Non‑integer variable should map to -1");
        assertEquals(0, model.indexOfIntegerVariable(1), "First integer variable should map to index 0");
        assertEquals(1, model.indexOfIntegerVariable(2), "Second integer variable should map to index 1");
    }

    @Test
    void testIndexOfIntegerVariable_NoIntegerVariables() {
        // Add only continuous variables
        model.addVariable().lower(0).upper(10);
        model.addVariable().lower(0).upper(20);
        model.addVariable().lower(0).upper(30);
        // All global indices should return -1
        for (int i = 0; i < 3; i++) {
            assertEquals(-1, model.indexOfIntegerVariable(i), "When no integer variables exist, result should be -1");
        }
    }

    @Test
    void testIndexOfIntegerVariable_AllIntegerVariables() {
        // Add three integer variables
        for (int i = 0; i < 3; i++) {
            Variable v = model.addVariable();
            v.integer(true).lower(0).upper(5);
        }
        // Global index i should map to integer index i
        for (int i = 0; i < 3; i++) {
            assertEquals(i, model.indexOfIntegerVariable(i), "Integer variable at global index " + i + " should map to same integer index");
        }
    }

    /**
     * Group: Initial indexing of integer variables.
     *
     * Scenario:
     *   0 – integer, not fixed   → index 0
     *   1 – continuous           → -1
     *   2 – integer, fixed       → -1 (fixed variables are excluded)
     */
    @Test
    void testInitialIndexingOfIntegerVariables() {
        // 0 – integer, not fixed
        Variable v0 = model.addVariable();
        v0.integer(true).lower(0).upper(5);
        // 1 – continuous
        model.addVariable().lower(0).upper(10);
        // 2 – integer, fixed (level sets both bounds to the same value)
        Variable v2 = model.addVariable();
        // makes it fixed
        v2.integer(true).level(BigDecimal.valueOf(3));
        assertAll(() -> assertEquals(0, model.indexOfIntegerVariable(0), "First unfixed integer variable should map to index 0"), () -> assertEquals(-1, model.indexOfIntegerVariable(1), "Continuous variable should map to -1"), () -> assertEquals(-1, model.indexOfIntegerVariable(2), "Fixed integer variable should be excluded → -1"));
    }

    /**
     * Group: Multiple integer variables indexing.
     *
     * Variable layout:
     *   0 – integer (index 0)
     *   1 – continuous (‑1)
     *   2 – integer (index 1)
     *   3 – integer (index 2)
     */
    @Test
    void testMultipleIntegerVariablesIndexing() {
        // 0 – integer
        model.addVariable().integer(true).lower(0).upper(5);
        // 1 – continuous
        model.addVariable().lower(0).upper(10);
        // 2 – integer
        model.addVariable().integer(true).lower(0).upper(5);
        // 3 – integer
        model.addVariable().integer(true).lower(0).upper(5);
        assertAll(() -> assertEquals(0, model.indexOfIntegerVariable(0), "First integer variable should map to index 0"), () -> assertEquals(-1, model.indexOfIntegerVariable(1), "Continuous variable should map to -1"), () -> assertEquals(1, model.indexOfIntegerVariable(2), "Second integer variable should map to index 1"), () -> assertEquals(2, model.indexOfIntegerVariable(3), "Third integer variable should map to index 2"));
    }

    /**
     * Group: Integer indexing with fixed variables interleaved.
     *
     * Variable layout:
     *   0 – continuous
     *   1 – integer (unfixed) → index 0
     *   2 – integer (fixed)   → -1
     *   3 – integer (unfixed) → index 1
     */
    @Test
    void testIntegerIndexingWithFixedVariablesInterleaved() {
        // 0 – continuous
        model.addVariable().lower(0).upper(10);
        // 1 – integer, unfixed
        model.addVariable().integer(true).lower(0).upper(5);
        // 2 – integer, fixed
        model.addVariable().integer(true).level(BigDecimal.valueOf(2));
        // 3 – integer, unfixed
        model.addVariable().integer(true).lower(0).upper(5);
        assertAll(() -> assertEquals(0, model.indexOfIntegerVariable(1), "First unfixed integer (global 1) should map to integer index 0"), () -> assertEquals(-1, model.indexOfIntegerVariable(2), "Fixed integer (global 2) must be excluded → -1"), () -> assertEquals(1, model.indexOfIntegerVariable(3), "Second unfixed integer (global 3) should map to integer index 1"));
    }

    /**
     * Group: Empty model indexing.
     *
     * Any access on an empty model must result in {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void testEmptyModelIndexingThrows() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(0), "Empty model should throw ArrayIndexOutOfBoundsException for any index");
    }

    /**
     * Group: Deterministic result on repeated calls.
     *
     * Calls to {@code indexOfIntegerVariable(0)} must always return the same value.
     */
    @Test
    void testDeterministicResultOnRepeatedCalls() {
        // Single unfixed integer variable
        model.addVariable().integer(true).lower(0).upper(5);
        int first = model.indexOfIntegerVariable(0);
        int second = model.indexOfIntegerVariable(0);
        int third = model.indexOfIntegerVariable(0);
        assertAll(() -> assertEquals(0, first, "First call should return 0"), () -> assertEquals(0, second, "Second call should return the same value"), () -> assertEquals(0, third, "Third call should also return the same value"));
    }

    /**
     * Additional parameterised test demonstrating the mapping for a range of
     * configurations in a compact form.
     *
     * The CSV columns are:
     *   globalIndex, expectedResult, description
     */
    @ParameterizedTest(name = "{2}")
    @CsvSource({ // Configuration: integer, continuous, fixed integer, integer
    // Global indices: 0,1,2,3
    "0,0,'Integer at 0 → index 0'", "1,-1,'Continuous at 1 → -1'", "2,-1,'Fixed integer at 2 → -1'", "3,1,'Integer at 3 → index 1'" })
    void testParameterizedMapping(int globalIndex, int expected, String description) {
        // Build the same model as in the previous test (integer, continuous, fixed integer, integer)
        // reset (redundant due to @BeforeEach but explicit)
        model = new ExpressionsBasedModel();
        // 0
        model.addVariable().integer(true).lower(0).upper(5);
        // 1
        model.addVariable().lower(0).upper(10);
        // 2 (fixed)
        model.addVariable().integer(true).level(BigDecimal.valueOf(2));
        // 3
        model.addVariable().integer(true).lower(0).upper(5);
        assertEquals(expected, model.indexOfIntegerVariable(globalIndex), description);
    }
}
