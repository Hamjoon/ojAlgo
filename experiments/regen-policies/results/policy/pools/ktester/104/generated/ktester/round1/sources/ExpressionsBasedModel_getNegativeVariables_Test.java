package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getNegativeVariables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @Test
    void testNegativeVariableIsReturned() {
        // Variable with a negative lower bound (and no upper bound) should be considered negative.
        Variable negVar = model.newVariable("neg").lower(-5).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(negVar), "Variable with negative lower bound must appear in getNegativeVariables()");
    }

    @Test
    void testVariableWithoutLowerLimitIsReturned() {
        // A free variable without any lower limit is also treated as negative.
        Variable noLower = model.newVariable("noLower").upper(10).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(noLower), "Variable without a lower limit must appear in getNegativeVariables()");
    }

    @Test
    void testPositiveLowerBoundVariableIsNotReturned() {
        // Variable with a non‑negative lower bound should NOT be in the negative list.
        Variable posVar = model.newVariable("pos").lower(0).upper(10).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertFalse(negatives.contains(posVar), "Variable with non‑negative lower bound must NOT appear in getNegativeVariables()");
    }

    @Test
    void testFixedVariableIsNotReturnedEvenIfNegative() {
        // A fixed variable (set via level) is excluded from the negative list,
        // even if its value is negative.
        Variable fixedNeg = model.newVariable("fixedNeg").level(-3).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertFalse(negatives.contains(fixedNeg), "Fixed variable must NOT appear in getNegativeVariables() regardless of its value");
    }

    @Test
    void testGetNegativeVariablesReturnsUnmodifiableList() {
        Variable v = model.newVariable("v").lower(-1).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertThrows(UnsupportedOperationException.class, () -> negatives.add(v), "Returned list must be unmodifiable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testEmptyModelReturnsEmptyList() {
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.isEmpty(), "An empty model must return an empty list of negative variables");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFixedVariableIsNotNegative() {
        // A fixed variable is created by setting the same lower and upper limit
        // (or by using level(...)). Here we use level for brevity.
        Variable fixed = model.newVariable("fixed").level(-3).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertFalse(negatives.contains(fixed), "Fixed variables must never appear in the negative‑variable list");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFreeVariableWithoutLowerLimitIsReturned() {
        Variable freeNoLower = model.newVariable("freeNoLower").upper(10).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(freeNoLower), "A free variable with no lower limit must be considered negative");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFreeVariableWithPositiveLowerLimitIsNotReturned() {
        Variable positive = model.newVariable("positive").lower(5).upper(15).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertFalse(negatives.contains(positive), "A free variable with a positive lower bound must NOT be in the list");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFreeVariableWithZeroLowerLimitIsNotReturned() {
        Variable zero = model.newVariable("zero").lower(0).upper(10).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertFalse(negatives.contains(zero), "A lower bound of zero is not negative and must not be returned");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFreeVariableWithNegativeLowerLimitIsReturned() {
        Variable negative = model.newVariable("negative").lower(-3).upper(10).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(negative), "A free variable with a negative lower bound must appear in the list");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testMixedVariablesOnlyNegativeReturned() {
        Variable fixed = model.newVariable("fixed").level(2).weight(1);
        Variable positive = model.newVariable("positive").lower(4).upper(8).weight(1);
        Variable negative = model.newVariable("negative").lower(-2).upper(5).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertEquals(1, negatives.size(), "Only one variable should be classified as negative");
        assertTrue(negatives.contains(negative), "The negative free variable must be present");
        assertFalse(negatives.contains(fixed), "Fixed variable must not be present");
        assertFalse(negatives.contains(positive), "Positive‑lower‑bound variable must not be present");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDynamicUpdateAfterAddingVariables() {
        // First call – model is empty
        List<Variable> firstCall = model.getNegativeVariables();
        assertTrue(firstCall.isEmpty(), "First call on an empty model must be empty");
        // Add a negative free variable
        Variable newlyAdded = model.newVariable("newNeg").lower(-5).weight(1);
        // Second call – should now contain the newly added variable
        List<Variable> secondCall = model.getNegativeVariables();
        assertEquals(1, secondCall.size(), "After adding a negative variable the list must contain exactly one element");
        assertTrue(secondCall.contains(newlyAdded), "The newly added negative variable must be present");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testIntegerVariableWithNegativeLowerLimitIsReturned() {
        Variable intNeg = model.newVariable("intNeg").lower(-2).integer(true).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(intNeg), "Integer variable with a negative lower bound must be returned");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testVariableNegativeLowerPositiveUpperIsReturned() {
        Variable var = model.newVariable("var").lower(-5).upper(10).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(var), "Negative lower bound qualifies the variable irrespective of a positive upper bound");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testVariableLowerUnsetUpperNegativeIsReturned() {
        Variable var = model.newVariable("var").upper(-3).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertTrue(negatives.contains(var), "A variable without a lower limit is considered negative even if the upper limit is negative");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFixedVariableWithNegativeLowerLimitIsNotReturned() {
        // Set lower and upper to the same negative value => fixed
        Variable fixedNeg = model.newVariable("fixedNeg").lower(-4).upper(-4).weight(1);
        List<Variable> negatives = model.getNegativeVariables();
        assertFalse(negatives.contains(fixedNeg), "Fixed variable must be excluded even if its lower limit is negative");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRepeatedCallsReturnConsistentUnmodifiableList() {
        Variable neg = model.newVariable("neg").lower(-1).weight(1);
        List<Variable> first = model.getNegativeVariables();
        List<Variable> second = model.getNegativeVariables();
        // The content must be equal
        assertEquals(first, second, "Successive calls must return equal lists");
        // Both must be unmodifiable
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> first.add(neg), "First list must be unmodifiable"), () -> assertThrows(UnsupportedOperationException.class, () -> second.add(neg), "Second list must be unmodifiable"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNegativeVariableAfterModelReset() {
        // Create a model, add a negative variable and verify it appears
        Variable neg = model.newVariable("neg").lower(-2).weight(1);
        assertTrue(model.getNegativeVariables().contains(neg), "Negative variable must be present before reset");
        // Reset by creating a fresh model instance
        model = new ExpressionsBasedModel();
        // The new model should have an empty negative list
        List<Variable> afterReset = model.getNegativeVariables();
        assertTrue(afterReset.isEmpty(), "After resetting the model, the negative‑variable list must be empty");
    }
}