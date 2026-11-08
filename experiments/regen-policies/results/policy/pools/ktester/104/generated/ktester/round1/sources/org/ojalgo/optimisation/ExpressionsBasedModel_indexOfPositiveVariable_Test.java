package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_indexOfPositiveVariable_Test {

    private ExpressionsBasedModel model;

    // free, no bounds → positive & negative
    private Variable varFreeNoBounds;

    // free, upper = 0 → not positive, negative
    private Variable varUpperZero;

    // free, lower = 0 → positive, not negative
    private Variable varLowerZero;

    // fixed (lower=upper) → not free → not considered
    private Variable varFixed;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // 1. Free variable with no bounds
        varFreeNoBounds = model.addVariable();
        // defaults: not fixed, no upper/lower → qualifies as positive
        // 2. Free variable with upper limit = 0 (non‑positive)
        varUpperZero = model.addVariable();
        // upper = 0 → signum = 0 → not positive
        varUpperZero.upper(BigDecimal.ZERO);
        // lower not set → still negative
        // 3. Free variable with lower limit = 0 (non‑negative)
        varLowerZero = model.addVariable();
        // lower = 0 → not negative
        varLowerZero.lower(BigDecimal.ZERO);
        // upper not set → positive
        // 4. Fixed variable (lower == upper)
        varFixed = model.addVariable();
        // fixes the variable → not free
        varFixed.level(BigDecimal.valueOf(5));
    }

    /**
     * Builds a model that matches the description of the JSON test cases.
     * The returned list contains the variables in the order they were added,
     * i.e. their global indices are 0 … list.size()-1.
     */
    private static List<Variable> createRichModel(ExpressionsBasedModel model) {
        List<Variable> vars = new ArrayList<>();
        // 0 – free, no upper limit (positive)
        vars.add(model.addVariable());
        // 1 – free, upper = 10 (positive)
        Variable v1 = model.addVariable();
        v1.upper(BigDecimal.TEN);
        vars.add(v1);
        // 2 – free, upper = 0 (not positive)
        Variable v2 = model.addVariable();
        v2.upper(BigDecimal.ZERO);
        vars.add(v2);
        // 3 – free, upper = -5 (not positive)
        Variable v3 = model.addVariable();
        v3.upper(BigDecimal.valueOf(-5));
        vars.add(v3);
        // 4 – free, no upper limit (positive)
        vars.add(model.addVariable());
        // 5 – fixed variable (level = 5)
        Variable v5 = model.addVariable();
        v5.level(BigDecimal.valueOf(5));
        vars.add(v5);
        // 6 – free, no upper limit (positive)
        vars.add(model.addVariable());
        // 7 – free, integer, no upper limit (positive)
        Variable v7 = model.addVariable();
        v7.integer(true);
        vars.add(v7);
        // 8 – free, integer, upper = -2 (not positive)
        Variable v8 = model.addVariable();
        v8.integer(true);
        v8.upper(BigDecimal.valueOf(-2));
        vars.add(v8);
        return vars;
    }

    /**
     * Provides (globalIndex, expectedPositiveIndex) pairs for the normal mapping tests.
     */
    private static Stream<Arguments> positiveMappingProvider() {
        return Stream.of(// Positive Variable Index Mapping
        Arguments.of(0, 0, "First variable is free and has no upper limit set."), Arguments.of(1, 1, "Second variable is free and its upper limit is positive (10)."), // Mixed Upper Limits
        Arguments.of(2, -1, "Third variable has an explicit upper limit of 0 (not positive)."), Arguments.of(3, -1, "Fourth variable has an explicit upper limit of -5 (not positive)."), Arguments.of(4, 2, "Fifth variable has no upper limit set (positive)."), // Fixed Variables
        Arguments.of(5, -1, "Sixth variable is fixed, therefore not positive."), Arguments.of(6, 3, "Seventh variable is free with no upper limit (positive)."), // Integer Variables
        Arguments.of(7, 4, "Eighth variable is free, integer, no upper limit (positive)."), Arguments.of(8, -1, "Ninth variable is free, integer, upper = -2 (not positive)."));
    }

    /**
     * Provides indices that are out of the defined range and should throw {@link ArrayIndexOutOfBoundsException}.
     */
    private static Stream<Arguments> outOfBoundsProvider() {
        return Stream.of(Arguments.of(20, "global index larger than the number of defined variables"), Arguments.of(-1, "negative global index"));
    }

    @Test
    void testPositiveVariableIndices() {
        // Global indices as seen by the model
        int idxFreeNoBounds = model.indexOf(varFreeNoBounds);
        int idxUpperZero = model.indexOf(varUpperZero);
        int idxLowerZero = model.indexOf(varLowerZero);
        int idxFixed = model.indexOf(varFixed);
        // Expected positive indices:
        //   varFreeNoBounds → first positive (index 0)
        //   varLowerZero    → second positive (index 1)
        //   varUpperZero & varFixed → not positive → -1
        assertEquals(0, model.indexOfPositiveVariable(idxFreeNoBounds), "Free variable without bounds should be the first positive variable");
        assertEquals(-1, model.indexOfPositiveVariable(idxUpperZero), "Variable with upper limit = 0 must not be considered positive");
        assertEquals(1, model.indexOfPositiveVariable(idxLowerZero), "Variable with only a non‑negative lower bound should be the second positive variable");
        assertEquals(-1, model.indexOfPositiveVariable(idxFixed), "Fixed variable must not be considered positive");
    }

    @Test
    void testPositiveVariableViaVariableIndexOverload() {
        // The overload that accepts a Variable should delegate correctly
        assertEquals(0, model.indexOfPositiveVariable(varFreeNoBounds), "Overload with Variable should return same index as int overload for free no‑bounds variable");
        assertEquals(-1, model.indexOfPositiveVariable(varUpperZero), "Overload with Variable should return -1 for non‑positive variable");
        assertEquals(1, model.indexOfPositiveVariable(varLowerZero), "Overload with Variable should return correct positive index for variable with lower=0");
        assertEquals(-1, model.indexOfPositiveVariable(varFixed), "Overload with Variable should return -1 for fixed variable");
    }

    @ParameterizedTest(name = "[{index}] globalIndex={0} → expected={1} :: {2}")
    @MethodSource("positiveMappingProvider")
    void testPositiveVariableIndexMapping(int globalIndex, int expected, String description) {
        ExpressionsBasedModel richModel = new ExpressionsBasedModel();
        // populates the model
        createRichModel(richModel);
        int actual = richModel.indexOfPositiveVariable(globalIndex);
        assertEquals(expected, actual, () -> "Unexpected positive index for globalIndex=" + globalIndex + ". " + description);
    }

    @ParameterizedTest(name = "[{index}] out‑of‑bounds globalIndex={0}")
    @MethodSource("outOfBoundsProvider")
    void testOutOfBounds(int globalIndex, String description) {
        ExpressionsBasedModel richModel = new ExpressionsBasedModel();
        // 9 variables (indices 0‑8)
        createRichModel(richModel);
        Executable call = () -> richModel.indexOfPositiveVariable(globalIndex);
        assertThrows(ArrayIndexOutOfBoundsException.class, call, () -> "Expected ArrayIndexOutOfBoundsException for " + description);
    }

    @Test
    void testNullVariableEntryThrowsNullPointerException() throws Exception {
        // Build a normal model with at least one variable
        ExpressionsBasedModel nullModel = new ExpressionsBasedModel();
        // index 0
        nullModel.addVariable();
        // Inject a null entry into the internal myVariables list via reflection
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> internalList = (ArrayList<Variable>) varsField.get(nullModel);
        // Replace the first element with null
        internalList.set(0, null);
        // The categorisation update will dereference the null Variable → NPE
        Executable call = () -> nullModel.indexOfPositiveVariable(0);
        assertThrows(NullPointerException.class, call, "Calling indexOfPositiveVariable on a model with a null variable entry should throw NullPointerException");
    }
}
