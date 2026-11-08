package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.ojalgo.structure.Structure1D.IntIndex;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfPositiveVariable_2_Test {

    private ExpressionsBasedModel model;

    /* -------------------------------------------------------------
     *  Helper functional interface for building models.
     * ------------------------------------------------------------- */
    @FunctionalInterface
    private interface ModelBuilder {

        /**
         * Populate the supplied {@code ExpressionsBasedModel} with variables,
         * weights, etc., to satisfy the scenario under test.
         *
         * @param model the model to configure
         */
        void build(ExpressionsBasedModel model);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources (if any).
        model.dispose();
    }

    /* -------------------------------------------------------------
     *  Forwarding & normal operation
     * ------------------------------------------------------------- */
    private static Stream<Arguments> forwardingAndNormalProvider() {
        // Arguments: description, global index, expected positive position, model builder
        return Stream.of(// Case 1 – first variable is positive, position 0
        Arguments.of("first variable positive", 0, 0, (ModelBuilder) m -> {
            m.addVariable();
        }), // Case 2 – global index 3 should be the third positive variable (position 2)
        // Build: 0‑positive, 1‑non‑positive, 2‑positive, 3‑positive
        Arguments.of("global index 3 maps to positive position 2", 3, 2, (ModelBuilder) m -> {
            m.addVariable();
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable();
            m.addVariable();
        }));
    }

    /* -------------------------------------------------------------
     *  Non‑positive variables
     * ------------------------------------------------------------- */
    private static Stream<Arguments> nonPositiveProvider() {
        return Stream.of(// Variable at index 5 is non‑positive → -1
        Arguments.of("non‑positive variable at index 5", 5, -1, (ModelBuilder) m -> {
            m.addVariable();
            m.addVariable();
            m.addVariable();
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable().weight(BigDecimal.ZERO);
        }), // Variable at index 7 is negative → -1
        Arguments.of("negative variable at index 7", 7, -1, (ModelBuilder) m -> {
            m.addVariable();
            m.addVariable();
            m.addVariable();
            m.addVariable();
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable().weight(BigDecimal.valueOf(-1));
            m.addVariable().weight(BigDecimal.valueOf(-1));
        }));
    }

    /* -------------------------------------------------------------
     *  Boundary conditions
     * ------------------------------------------------------------- */
    private static Stream<Arguments> boundaryProvider() {
        return Stream.of(// Single positive variable, index 0 → position 0
        Arguments.of("single positive variable", 0, 0, (ModelBuilder) m -> {
            m.addVariable();
        }), // Model with three variables: first two fixed (weight 0), third positive
        Arguments.of("first positive at index 2 after two fixed", 2, 0, (ModelBuilder) m -> {
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable();
        }), // Five variables: 0‑2 positive, 3 fixed, 4 positive → index 4 → position 2
        Arguments.of("last positive after a fixed variable", 4, 2, (ModelBuilder) m -> {
            m.addVariable();
            m.addVariable();
            m.addVariable();
            m.addVariable().weight(BigDecimal.ZERO);
            m.addVariable();
        }));
    }

    /**
     * Verify that the index returned by {@code indexOfPositiveVariable} matches the
     * position of the variable in {@code model.getPositiveVariables()}.
     */
    @Test
    void testIndexOfPositiveVariableMatchesPositiveListOrder() {
        // Create three positive variables (default weight is +1).
        // index 0
        Variable v0 = model.addVariable();
        // index 1
        Variable v1 = model.addVariable();
        // index 2
        Variable v2 = model.addVariable();
        List<Variable> positiveVars = model.getPositiveVariables();
        assertEquals(3, positiveVars.size(), "Model should report three positive variables");
        for (int expectedPos = 0; expectedPos < positiveVars.size(); expectedPos++) {
            Variable var = positiveVars.get(expectedPos);
            int globalIdx = model.indexOf(var);
            IntIndex intIdx = new IntIndex(globalIdx);
            int actualPos = model.indexOfPositiveVariable(intIdx);
            assertEquals(expectedPos, actualPos, () -> "Variable " + var + " expected positive position " + expectedPos + " but got " + actualPos);
        }
    }

    /**
     * Verify that a variable that is not positive (e.g. weight set to a negative value)
     * yields {@code -1} when {@code indexOfPositiveVariable} is invoked.
     */
    @Test
    void testIndexOfPositiveVariableReturnsMinusOneForNonPositiveVariable() {
        // positive, index 0
        Variable posVar = model.addVariable();
        // negative, index 1
        Variable negVar = model.addVariable().weight(-1);
        List<Variable> positiveVars = model.getPositiveVariables();
        assertTrue(positiveVars.contains(posVar));
        assertFalse(positiveVars.contains(negVar));
        int posIdx = model.indexOfPositiveVariable(new IntIndex(model.indexOf(posVar)));
        assertEquals(0, posIdx, "Positive variable should map to position 0");
        int negIdx = model.indexOfPositiveVariable(new IntIndex(model.indexOf(negVar)));
        assertEquals(-1, negIdx, "Non‑positive variable should return -1");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("forwardingAndNormalProvider")
    void testIndexOfPositiveVariableForwardingAndNormal(String description, int globalIdx, int expectedPos, ModelBuilder builder) {
        builder.build(model);
        List<Variable> positiveVars = model.getPositiveVariables();
        assertTrue(positiveVars.size() > expectedPos, () -> "Positive list should be large enough for expected position " + expectedPos);
        int actualPos = model.indexOfPositiveVariable(new IntIndex(globalIdx));
        assertEquals(expectedPos, actualPos, () -> "For global index " + globalIdx + " expected positive position " + expectedPos + " but got " + actualPos);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonPositiveProvider")
    void testIndexOfPositiveVariableNonPositive(String description, int globalIdx, int expected, ModelBuilder builder) {
        builder.build(model);
        int actual = model.indexOfPositiveVariable(new IntIndex(globalIdx));
        assertEquals(expected, actual, () -> "Expected -1 for non‑positive variable at global index " + globalIdx);
    }

    /* -------------------------------------------------------------
     *  Error handling
     * ------------------------------------------------------------- */
    @Test
    void testIndexOfPositiveVariableNegativeIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(new IntIndex(-1)), "Negative global index should trigger IndexOutOfBoundsException");
    }

    @Test
    void testIndexOfPositiveVariableTooLargeIndexThrows() {
        model.addVariable();
        model.addVariable();
        model.addVariable();
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(new IntIndex(1000)), "Index far beyond defined variables should trigger IndexOutOfBoundsException");
    }

    @Test
    void testIndexOfPositiveVariableNullArgumentThrows() {
        assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(null), "Passing null should trigger NullPointerException");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaryProvider")
    void testIndexOfPositiveVariableBoundaryConditions(String description, int globalIdx, int expectedPos, ModelBuilder builder) {
        builder.build(model);
        int actual = model.indexOfPositiveVariable(new IntIndex(globalIdx));
        assertEquals(expectedPos, actual, () -> "Boundary test failed for " + description);
    }
}