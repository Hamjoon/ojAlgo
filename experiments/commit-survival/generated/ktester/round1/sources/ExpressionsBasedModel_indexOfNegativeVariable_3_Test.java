package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfNegativeVariable_3_Test {

    /**
     * The model used by the tests.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test guarantees isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly required but keeps the pattern
        // consistent with the template.
        model = null;
    }

    /**
     * A variable whose feasible range is strictly negative should be recognised
     * as a “negative variable” and therefore receive a non‑negative index.
     */
    @Test
    void testNegativeVariableIsIndexed() {
        // lower = -5, upper = -1  →  definitely negative
        Variable negative = model.addVariable().lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(-1));
        int idx = model.indexOfNegativeVariable(negative);
        assertTrue(idx >= 0, "Negative variable should have a non‑negative index");
    }

    /**
     * A variable whose feasible range is strictly positive must not be considered
     * a negative variable – the method should return -1.
     */
    @Test
    void testPositiveVariableIsNotIndexed() {
        // lower = 0, upper = 10  →  definitely positive
        Variable positive = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        int idx = model.indexOfNegativeVariable(positive);
        assertEquals(-1, idx, "Positive variable must not be indexed as negative");
    }

    /**
     * A variable that is not part of the model should also yield -1, regardless of
     * its bounds.
     */
    @Test
    void testForeignVariableReturnsMinusOne() {
        // Create a variable that is *not* added to the model.
        Variable foreign = Variable.make("foreign").lower(BigDecimal.valueOf(-3)).upper(BigDecimal.valueOf(-1));
        int idx = model.indexOfNegativeVariable(foreign);
        assertEquals(-1, idx, "Variable not belonging to the model must return -1");
    }

    /**
     * Verify that the overload {@code indexOfNegativeVariable(Variable)} simply
     * delegates to the integer‑based version by checking that both produce the
     * same result for a known negative variable.
     */
    @Test
    void testDelegationConsistency() {
        Variable negative = model.addVariable().lower(BigDecimal.valueOf(-2)).upper(BigDecimal.valueOf(-0.5)).weight(BigDecimal.valueOf(-1));
        int viaVariable = model.indexOfNegativeVariable(negative);
        int viaInt = model.indexOfNegativeVariable(model.indexOf(negative));
        assertEquals(viaInt, viaVariable, "The Variable overload must return the same index as the int overload");
    }

    /**
     * Group: indexOfNegativeVariable - variable not present in model
     *
     * <p>Case 1 – {@code null} argument should raise {@link NullPointerException}
     * because the implementation internally calls {@code indexOf(null)}.</p>
     */
    @Test
    void testNullVariableThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(null), "Calling with null should throw NullPointerException");
    }

    /**
     * Group: indexOfNegativeVariable - single variable with non‑negative weight
     *
     * <p>A model containing a single variable whose objective coefficient (weight)
     * is non‑negative must not be considered a negative variable; the method should
     * therefore return {@code -1}.</p>
     */
    @Test
    void testSingleVariableWithZeroWeightIsNotNegative() {
        Variable v = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN).// non‑negative weight
        weight(BigDecimal.ZERO);
        int idx = model.indexOfNegativeVariable(v);
        assertEquals(-1, idx, "Variable with non‑negative weight must not be indexed as negative");
    }

    /**
     * Group: indexOfNegativeVariable - single negative variable
     *
     * <p>When the model contains exactly one variable whose weight is negative,
     * it should be the first entry in the internal negative‑variable list, i.e.
     * index {@code 0}.</p>
     */
    @Test
    void testSingleNegativeVariableIsIndexedZero() {
        Variable v = model.addVariable().lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(-1)).weight(BigDecimal.valueOf(-10));
        int idx = model.indexOfNegativeVariable(v);
        assertEquals(0, idx, "The sole negative variable must have index 0");
    }

    /**
     * Group: indexOfNegativeVariable - multiple variables with mixed signs
     *
     * <p>Two negative variables and one positive variable are added. The method must
     * return the correct index preserving insertion order among negative variables.</p>
     */
    @Test
    void testMultipleMixedSignVariables() {
        Variable neg1 = model.addVariable().lower(BigDecimal.valueOf(-10)).upper(BigDecimal.valueOf(-5)).// first negative
        weight(BigDecimal.valueOf(-5));
        Variable neg2 = model.addVariable().lower(BigDecimal.valueOf(-4)).upper(BigDecimal.valueOf(-1)).// second negative
        weight(BigDecimal.valueOf(-3));
        Variable pos = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).// positive
        weight(BigDecimal.valueOf(7));
        assertAll(() -> assertEquals(0, model.indexOfNegativeVariable(neg1), "First negative variable should have index 0"), () -> assertEquals(1, model.indexOfNegativeVariable(neg2), "Second negative variable should have index 1"), () -> assertEquals(-1, model.indexOfNegativeVariable(pos), "Positive variable must not be indexed as negative"));
    }

    /**
     * Group: indexOfNegativeVariable - after removing a negative variable
     *
     * <p>We add two negative variables, remove the first one via reflection (the API
     * does not expose a public removal method), and verify that the remaining
     * negative variable is re‑indexed to {@code 0}.</p>
     */
    @Test
    void testNegativeVariableRemovalReindexes() throws Exception {
        Variable negA = model.addVariable().lower(BigDecimal.valueOf(-8)).upper(BigDecimal.valueOf(-2)).weight(BigDecimal.valueOf(-5));
        Variable negB = model.addVariable().lower(BigDecimal.valueOf(-6)).upper(BigDecimal.valueOf(-1)).weight(BigDecimal.valueOf(-3));
        // Verify initial indices
        assertEquals(0, model.indexOfNegativeVariable(negA));
        assertEquals(1, model.indexOfNegativeVariable(negB));
        // ---- Remove the first negative variable using reflection ----
        // The internal list of variables is not part of the public API.
        // We locate the first List field that holds Variable instances.
        Field varListField = Stream.of(ExpressionsBasedModel.class.getDeclaredFields()).filter(f -> List.class.isAssignableFrom(f.getType())).filter(f -> f.getName().toLowerCase().contains("variable")).findFirst().orElseThrow(() -> new IllegalStateException("Unable to locate internal variable list"));
        varListField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> varList = (List<Variable>) varListField.get(model);
        // Remove the first negative variable (negA)
        assertTrue(varList.remove(negA), "Failed to remove the first negative variable via reflection");
        // After removal, the remaining negative variable should be at index 0
        int idxAfterRemoval = model.indexOfNegativeVariable(negB);
        assertEquals(0, idxAfterRemoval, "After removal of the first negative variable, the remaining one must be re‑indexed to 0");
    }

    /**
     * Group: indexOfNegativeVariable - variable with null weight
     *
     * <p>If a variable has a {@code null} weight (objective coefficient), the
     * internal implementation attempts to evaluate its sign and throws a
     * {@link NullPointerException}.</p>
     */
    @Test
    void testVariableWithNullWeightThrowsNPE() {
        Variable v = model.addVariable().lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(-1));
        // Do NOT set a weight – it stays {@code null}
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(v), "Variable with null weight should cause NullPointerException");
    }

    /**
     * Group: indexOfNegativeVariable - after model disposal
     *
     * <p>Calling the method on a disposed model must raise an
     * {@link IllegalStateException} because internal data structures have been cleared.</p>
     */
    @Test
    void testAfterModelDisposalThrowsIllegalStateException() {
        Variable v = model.addVariable().lower(BigDecimal.valueOf(-2)).upper(BigDecimal.valueOf(-1)).weight(BigDecimal.valueOf(-4));
        // put the model into a disposed state
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.indexOfNegativeVariable(v), "Calling after dispose() should throw IllegalStateException");
    }
}
