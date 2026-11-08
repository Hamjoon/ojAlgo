package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfNegativeVariable_3_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model instance.
        model = new ExpressionsBasedModel();
    }

    /**
     * A variable whose feasible region is entirely negative
     * (upper bound < 0) should be recognised as a “negative variable”.
     */
    @Test
    void testNegativeVariableReturnsNonNegativeIndex() {
        // Create a variable that can only take negative values.
        Variable negVar = model.newVariable("neg").lower(BigDecimal.valueOf(-10)).upper(BigDecimal.valueOf(-1)).weight(BigDecimal.ONE);
        int idx = model.indexOfNegativeVariable(negVar);
        // The index must be >= 0 because the variable is part of the negative‑variable list.
        assertTrue(idx >= 0, "Negative variable should have a non‑negative index, got " + idx);
    }

    /**
     * A variable whose feasible region is non‑negative (lower bound ≥ 0) must not be
     * considered a negative variable; the method should return {@code -1}.
     */
    @Test
    void testPositiveVariableReturnsMinusOne() {
        Variable pos = model.newVariable("pos").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        int idx = model.indexOfNegativeVariable(pos);
        assertEquals(-1, idx, "Positive variable should not be recognised as negative");
    }

    /**
     * A variable that is unrestricted (no lower/upper bound) is treated as
     * non‑negative for the purpose of this method, therefore the result must be {@code -1}.
     */
    @Test
    void testUnrestrictedVariableReturnsMinusOne() {
        Variable freeVar = model.newVariable("free").weight(BigDecimal.ONE);
        int idx = model.indexOfNegativeVariable(freeVar);
        assertEquals(-1, idx, "Unrestricted variable should not be recognised as negative");
    }

    /**
     * Passing {@code null} should result in a {@link NullPointerException}
     * because the implementation dereferences the argument.
     */
    @Test
    void testNullVariableThrowsException() {
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(null), "Calling with null should throw NullPointerException");
    }

    @Test
    void testVariableNotInModelReturnsMinusOne() {
        // Directly instantiate a Variable that has never been added to the model.
        Variable orphan = new Variable("orphan");
        int idx = model.indexOfNegativeVariable(orphan);
        assertEquals(-1, idx, "A variable not added to the model should yield -1");
    }

    @Test
    void testZeroWeightVariableReturnsMinusOne() {
        Variable zero = model.newVariable("zero").weight(BigDecimal.ZERO);
        int idx = model.indexOfNegativeVariable(zero);
        assertEquals(-1, idx, "Zero‑weight variable should not be recognised as negative");
    }

    @Test
    void testNegativeWeightVariableReturnsNonNegativeIndex() {
        Variable neg = model.newVariable("neg").weight(BigDecimal.valueOf(-3));
        int idx = model.indexOfNegativeVariable(neg);
        assertTrue(idx >= 0, "Negative weight variable should have a non‑negative index");
        // Verify that the index corresponds to its position in the negative‑variable list.
        List<Variable> negatives = model.getNegativeVariables();
        assertEquals(negatives.indexOf(neg), idx, "Returned index should match position in getNegativeVariables()");
    }

    @Test
    void testNegativeWeightIntegerVariableReturnsNonNegativeIndex() {
        Variable intNeg = model.newVariable("intNeg").weight(BigDecimal.valueOf(-5)).integer(true);
        int idx = model.indexOfNegativeVariable(intNeg);
        assertTrue(idx >= 0, "Negative integer variable should have a non‑negative index");
        List<Variable> negatives = model.getNegativeVariables();
        assertEquals(negatives.indexOf(intNeg), idx, "Index should match position in negative‑variable list");
    }

    @Test
    void testFixedNegativeVariableReturnsNonNegativeIndex() {
        Variable fixedNeg = model.newVariable("fixedNeg").weight(BigDecimal.valueOf(-2)).lower(BigDecimal.valueOf(-2)).upper(BigDecimal.valueOf(-2));
        int idx = model.indexOfNegativeVariable(fixedNeg);
        assertTrue(idx >= 0, "Fixed negative variable should have a non‑negative index");
        List<Variable> negatives = model.getNegativeVariables();
        assertEquals(negatives.indexOf(fixedNeg), idx, "Index should correspond to position in negative‑variable list");
    }

    @Test
    void testNegativeVariableWeightChangedToNonNegative() {
        Variable var = model.newVariable("negThenZero").weight(BigDecimal.valueOf(-4));
        int idxBefore = model.indexOfNegativeVariable(var);
        assertTrue(idxBefore >= 0, "Initially negative weight should yield non‑negative index");
        // Change weight to zero (non‑negative)
        var.weight(BigDecimal.ZERO);
        int idxAfter = model.indexOfNegativeVariable(var);
        assertEquals(-1, idxAfter, "After weight change to non‑negative the variable should no longer be recognised as negative");
    }

    @Test
    void testVariableFromAnotherModelThrowsProgrammingError() {
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        Variable foreign = otherModel.newVariable("foreign").weight(BigDecimal.valueOf(-1));
        // The variable belongs to 'otherModel', not to 'model'.
        assertThrows(ProgrammingError.class, () -> model.indexOfNegativeVariable(foreign), "Using a variable from another model should throw ProgrammingError");
    }

    @Test
    void testDisposedModelThrowsProgrammingError() {
        Variable someVar = model.newVariable("some").weight(BigDecimal.valueOf(-1));
        // render the model unusable
        model.dispose();
        assertThrows(ProgrammingError.class, () -> model.indexOfNegativeVariable(someVar), "Calling on a disposed model should throw ProgrammingError");
    }
}
