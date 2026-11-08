package org.ojalgo.optimisation;

import java.util.List;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
// import org.junit.*;
// import static org.junit.Assert.*;

class ExpressionsBasedModel_indexOfPositiveVariable_Test {

    /**
     * Sets a private field via reflection. Used to manipulate the internal
     * {@code myVariables} list of {@link ExpressionsBasedModel}.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simple helper to assert that a runnable throws a given exception type.
     */
    private static void expectException(Class<? extends Throwable> expected, Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception of type " + expected.getName() + " but none was thrown.");
        } catch (Throwable actual) {
            if (!expected.isInstance(actual)) {
                fail("Expected exception of type " + expected.getName() + " but got " + actual.getClass().getName());
            }
            // exception as expected – test passes
        }
    }

//     @Test
//     void testIndexOfPositiveVariableVariousScenarios() {
//         // -----------------------------------------------------------------
//         // GIVEN – a model with a mixture of variables
//         // -----------------------------------------------------------------
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // 1. Positive – upper limit > 0 (default: no upper limit → positive)
//         Variable vPositive = model.newVariable("positive").upper(10);
//         // 2. Upper limit exactly zero → not positive
//         Variable vZeroUpper = model.newVariable("zeroUpper").upper(0);
//         // 3. Upper limit negative → not positive
//         Variable vNegativeUpper = model.newVariable("negativeUpper").upper(-5);
//         // 4. Fixed variable (level fixes both bounds) → not positive even though upper > 0
//         Variable vFixed = model.newVariable("fixed").level(5).upper(10);
//         // 5. Integer variable with a positive upper limit → positive
//         Variable vIntegerPositive = model.newVariable("intPositive").integer(true).upper(5);
//         // -----------------------------------------------------------------
//         // WHEN – we query the positive‑variable index for each global index
//         // -----------------------------------------------------------------
//         int idxPositive = model.indexOf(vPositive);
//         int idxZeroUpper = model.indexOf(vZeroUpper);
//         int idxNegativeUpper = model.indexOf(vNegativeUpper);
//         int idxFixed = model.indexOf(vFixed);
//         int idxIntegerPositive = model.indexOf(vIntegerPositive);
//         int posIdxPositive = model.indexOfPositiveVariable(idxPositive);
//         int posIdxZeroUpper = model.indexOfPositiveVariable(idxZeroUpper);
//         int posIdxNegativeUpper = model.indexOfPositiveVariable(idxNegativeUpper);
//         int posIdxFixed = model.indexOfPositiveVariable(idxFixed);
//         int posIdxIntegerPositive = model.indexOfPositiveVariable(idxIntegerPositive);
//         // -----------------------------------------------------------------
//         // THEN – the returned indices must match the expected positions
//         // -----------------------------------------------------------------
//         List<Variable> positiveVariables = model.getPositiveVariables();
//         assertEquals("Model should have exactly two positive variables", 2, positiveVariables.size());
//         int expectedPosIdxPositive = positiveVariables.indexOf(vPositive);
//         int expectedPosIdxIntegerPositive = positiveVariables.indexOf(vIntegerPositive);
//         assertEquals("First positive variable should be at position 0", 0, expectedPosIdxPositive);
//         assertEquals("Second positive variable should be at position 1", 1, expectedPosIdxIntegerPositive);
//         assertEquals("indexOfPositiveVariable for a positive variable is incorrect", expectedPosIdxPositive, posIdxPositive);
//         assertEquals("indexOfPositiveVariable for an integer‑positive variable is incorrect", expectedPosIdxIntegerPositive, posIdxIntegerPositive);
//         // Variables that are not positive must yield -1
//         assertEquals("Variable with zero upper limit must not be considered positive", -1, posIdxZeroUpper);
//         assertEquals("Variable with negative upper limit must not be considered positive", -1, posIdxNegativeUpper);
//         assertEquals("Fixed variable must not be considered positive", -1, posIdxFixed);
//     }

//     @Test
//     void testIndexOfPositiveVariable_basicScenarios() {
//         // GIVEN – a model with four variables matching the JSON description
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // 0 – free, no upper limit → positive
//         Variable v0 = model.newVariable("v0");
//         // 1 – free, upper limit = 0 → not positive
//         Variable v1 = model.newVariable("v1").upper(BigDecimal.ZERO);
//         // 2 – fixed (level sets both bounds) → not positive
//         Variable v2 = model.newVariable("v2").level(BigDecimal.valueOf(5));
//         // 3 – free, upper limit = 10 → positive
//         Variable v3 = model.newVariable("v3").upper(BigDecimal.TEN);
//         // WHEN – query the positive‑variable index for each global index
//         int idx0 = model.indexOf(v0);
//         int idx1 = model.indexOf(v1);
//         int idx2 = model.indexOf(v2);
//         int idx3 = model.indexOf(v3);
//         int posIdx0 = model.indexOfPositiveVariable(idx0);
//         int posIdx1 = model.indexOfPositiveVariable(idx1);
//         int posIdx2 = model.indexOfPositiveVariable(idx2);
//         int posIdx3 = model.indexOfPositiveVariable(idx3);
//         // THEN – verify the results
//         List<Variable> positiveVariables = model.getPositiveVariables();
//         assertEquals("Model should contain exactly two positive variables", 2, positiveVariables.size());
//         int expectedPosIdx0 = positiveVariables.indexOf(v0);
//         int expectedPosIdx3 = positiveVariables.indexOf(v3);
//         assertEquals("First positive variable should be at position 0", 0, expectedPosIdx0);
//         assertEquals("Second positive variable should be at position 1", 1, expectedPosIdx3);
//         assertEquals("indexOfPositiveVariable for a positive variable is incorrect", expectedPosIdx0, posIdx0);
//         assertEquals("indexOfPositiveVariable for the second positive variable is incorrect", expectedPosIdx3, posIdx3);
//         // Variables that are not positive must yield -1
//         assertEquals("Variable with zero upper limit must not be positive", -1, posIdx1);
//         assertEquals("Fixed variable must not be positive", -1, posIdx2);
//     }

//     @Test
//     void testIndexOfPositiveVariable_outOfRange() {
//         // GIVEN – a model with four variables (same as above)
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("v0");
//         model.newVariable("v1");
//         model.newVariable("v2");
//         model.newVariable("v3");
//         // WHEN / THEN – requesting an index beyond the variable count throws AIOOBE
//         expectException(ArrayIndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(10));
//     }

//     @Test
//     void testIndexOfPositiveVariable_negativeIndex() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("v0");
//         expectException(ArrayIndexOutOfBoundsException.class, () -> model.indexOfPositiveVariable(-1));
//     }

//     @Test
//     void testIndexOfPositiveVariable_noVariables() {
//         // Model without any variables
//         ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
//         expectException(ArrayIndexOutOfBoundsException.class, () -> emptyModel.indexOfPositiveVariable(0));
//     }

//     @Test
//     void testIndexOfPositiveVariable_extendedScenarios() {
//         // GIVEN – a model with eight variables as described in the JSON
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // 0 – free, no upper limit → positive
//         Variable v0 = model.newVariable("v0");
//         // 1 – free, upper = 0 → not positive
//         Variable v1 = model.newVariable("v1").upper(BigDecimal.ZERO);
//         // 2 – fixed → not positive
//         Variable v2 = model.newVariable("v2").level(BigDecimal.ONE);
//         // 3 – free, upper = 10 → positive
//         Variable v3 = model.newVariable("v3").upper(BigDecimal.TEN);
//         // 4 – free, integer, upper = 5 → positive (third positive)
//         Variable v4 = model.newVariable("v4").integer(true).upper(BigDecimal.valueOf(5));
//         // 5 – free, upper = -5 → not positive
//         Variable v5 = model.newVariable("v5").upper(BigDecimal.valueOf(-5));
//         // 6 – free, no upper limit → positive (fourth positive)
//         Variable v6 = model.newVariable("v6");
//         // 7 – free, upper = 0 → not positive
//         Variable v7 = model.newVariable("v7").upper(BigDecimal.ZERO);
//         // WHEN – query the method for the relevant indices
//         int posIdx4 = model.indexOfPositiveVariable(model.indexOf(v4));
//         int posIdx5 = model.indexOfPositiveVariable(model.indexOf(v5));
//         int posIdx6 = model.indexOfPositiveVariable(model.indexOf(v6));
//         int posIdx7 = model.indexOfPositiveVariable(model.indexOf(v7));
//         // THEN – verify expected results
//         List<Variable> positiveVariables = model.getPositiveVariables();
//         assertEquals("Model should contain exactly four positive variables", 4, positiveVariables.size());
//         assertEquals("Variable at global index 4 should be the third positive variable (index 2)", 2, posIdx4);
//         assertEquals("Variable with negative upper limit must not be positive", -1, posIdx5);
//         assertEquals("Variable at global index 6 should be the fourth positive variable (index 3)", 3, posIdx6);
//         assertEquals("Variable with zero upper limit must not be positive", -1, posIdx7);
//     }

//     @Test
//     void testIndexOfPositiveVariable_nullVariableEntry() throws Exception {
//         // GIVEN – a model with a single (null) entry in the internal list
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Create a proper list with a null element at index 0
//         ArrayList<Variable> corruptedList = new ArrayList<>();
//         // index 0 is null
//         corruptedList.add(null);
//         setPrivateField(model, "myVariables", corruptedList);
//         // WHEN / THEN – the categorisation update will try to call isFixed()
//         // on the null entry and therefore throw NullPointerException
//         expectException(NullPointerException.class, () -> model.indexOfPositiveVariable(0));
//     }
}