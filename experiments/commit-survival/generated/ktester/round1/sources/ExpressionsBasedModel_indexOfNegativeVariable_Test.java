package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfNegativeVariable_Test {

    private ExpressionsBasedModel model;

    /**
     * A tiny hand‑crafted mock that mimics a variable whose lower‑limit flag is set
     * but the actual lower limit value is {@code null}. This reproduces the behaviour
     * that the original Mockito mock provided.
     */
    private static final class MalformedVariable extends Variable {

        MalformedVariable() {
            super();
        }

        @Override
        public boolean isFixed() {
            return false;
        }

        @Override
        public boolean isLowerLimitSet() {
            return true;
        }

        @Override
        public BigDecimal getLowerLimit() {
            // deliberately malformed
            return null;
        }

        @Override
        public boolean isUpperLimitSet() {
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test case.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC.
        model = null;
    }

    static Stream<Arguments> negativeVariableDetectionProvider() {
        // Build a model that matches the description of the JSON test group.
        // 0 – negative lower limit, not fixed
        // 1 – no lower limit, not fixed (treated as negative)
        // 2 – lower limit = 0 (non‑negative)
        // 3 – positive lower limit (+3)
        // 4 – fixed variable with a negative lower limit
        // 5 – integer variable with a negative lower limit (integer flag does not affect negative classification)
        return Stream.of(Arguments.of(0, 0), Arguments.of(1, 1), Arguments.of(2, -1), Arguments.of(3, -1), Arguments.of(4, -1), Arguments.of(5, 2));
    }

    /**
     * Verify that variables which can take negative values are indexed correctly
     * and that non‑negative or fixed variables return {@code -1}.
     */
    @Test
    void testIndexOfNegativeVariable_MixedVariables() {
        // Variable 0: lower = -10, no upper → negative & free
        Variable v0 = model.addVariable().lower(BigDecimal.valueOf(-10));
        // Variable 1: lower = 0, upper = 5 → non‑negative
        Variable v1 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        // Variable 2: lower = -5, upper = 5 → negative & free
        Variable v2 = model.addVariable().lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5));
        // Variable 3: fixed at 3 (lower = upper = 3) → not free, thus not negative
        Variable v3 = model.addVariable().lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3));
        // Global indices are assigned in the order of creation.
        assertEquals(0, model.indexOf(v0));
        assertEquals(1, model.indexOf(v1));
        assertEquals(2, model.indexOf(v2));
        assertEquals(3, model.indexOf(v3));
        // Expected negative‑variable indices:
        // v0 → first negative → 0
        // v1 → not negative → -1
        // v2 → second negative → 1
        // v3 → fixed → -1
        assertEquals(0, model.indexOfNegativeVariable(0), "v0 should be first negative variable");
        assertEquals(-1, model.indexOfNegativeVariable(1), "v1 is non‑negative");
        assertEquals(1, model.indexOfNegativeVariable(2), "v2 should be second negative variable");
        assertEquals(-1, model.indexOfNegativeVariable(3), "v3 is fixed and therefore not negative");
    }

    /**
     * The method should throw {@link ArrayIndexOutOfBoundsException} when the supplied
     * global index is outside the range of defined variables.
     */
    @Test
    void testIndexOfNegativeVariable_IndexOutOfRange() {
        // Create a single variable so the valid index range is [0].
        model.addVariable();
        // Negative index
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(-1));
        // Index equal to size (out of range)
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(1));
    }

    /**
     * When no variables are present the method should still behave correctly
     * (i.e. throw an exception for any index because the internal array is empty).
     */
    @Test
    void testIndexOfNegativeVariable_EmptyModel() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(0));
    }

    /**
     * Negative variable detection – a comprehensive set of scenarios.
     *
     * The model is built once for the whole parameterised test; each
     * {@code globalIndex} is queried and compared with the expected result.
     */
    @ParameterizedTest(name = "[{index}] globalIndex={0} → expected={1}")
    @MethodSource("negativeVariableDetectionProvider")
    void testNegativeVariableDetection(int globalIndex, int expected) {
        // The model used in the provider is freshly built for each invocation,
        // therefore we need to recreate it here.
        ExpressionsBasedModel localModel = new ExpressionsBasedModel();
        // 0 – negative lower limit, not fixed
        localModel.addVariable().lower(BigDecimal.valueOf(-5));
        // 1 – no lower limit, not fixed (treated as negative)
        localModel.addVariable();
        // 2 – lower limit = 0 (non‑negative)
        localModel.addVariable().lower(BigDecimal.ZERO);
        // 3 – positive lower limit (+3)
        localModel.addVariable().lower(BigDecimal.valueOf(3));
        // 4 – fixed variable with a negative lower limit
        localModel.addVariable().lower(BigDecimal.valueOf(-2)).upper(BigDecimal.valueOf(-2));
        // 5 – integer variable with a negative lower limit
        localModel.addVariable().lower(BigDecimal.valueOf(-1));
        assertEquals(expected, localModel.indexOfNegativeVariable(globalIndex), "Unexpected negative‑variable index for global index " + globalIndex);
    }

    /**
     * Boundary and error handling – verify that illegal indices raise the correct exception.
     */
    @Test
    void testBoundaryAndErrorHandling() {
        // Populate model with a few variables so that the valid range is known.
        model.addVariable();
        model.addVariable();
        // negative index
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(-1));
        // index far beyond the size
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(10));
    }

    /**
     * Empty model handling – any index should cause an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void testEmptyModelHandling() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfNegativeVariable(0));
    }

    /**
     * Variable with both lower and upper limits negative – still classified as negative.
     */
    @Test
    void testNegativeLowerAndUpperLimits() {
        model.addVariable().lower(BigDecimal.valueOf(-4)).upper(BigDecimal.valueOf(-1));
        assertEquals(0, model.indexOfNegativeVariable(0), "Variable with negative lower & upper limits should be classified as negative");
    }

    /**
     * Fixed variable without a lower limit – must not appear in the negative list.
     */
    @Test
    void testFixedVariableWithoutLowerLimit() {
        // Fixed by setting both limits to the same positive value.
        model.addVariable().lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // This is the first (and only) variable, global index 0.
        assertEquals(-1, model.indexOfNegativeVariable(0), "Fixed variable should not be considered negative");
    }

    /**
     * Dynamic re‑classification after a limit change.
     *
     * Initially the variable is non‑negative; after lowering its lower bound
     * it becomes a negative variable and the internal categorisation must be refreshed.
     */
    @Test
    void testDynamicReclassificationAfterLimitChange() {
        // Create a variable with a non‑negative lower limit.
        Variable v = model.addVariable().lower(BigDecimal.ZERO);
        // Verify initial state: not a negative variable.
        assertEquals(-1, model.indexOfNegativeVariable(0), "Initially the variable is non‑negative");
        // Change the lower limit to a negative value.
        v.lower(BigDecimal.valueOf(-3));
        // The categorisation caches are refreshed lazily; a second call must now return 0.
        assertEquals(0, model.indexOfNegativeVariable(0), "After lowering the bound the variable should be re‑classified as negative");
    }

    /**
     * Null entry in the internal variable list – should result in a {@link NullPointerException}
     * when the categorisation logic attempts to inspect the variable.
     */
    @Test
    void testNullVariableHandling() throws Exception {
        // Ensure the list has at least one element.
        model.addVariable();
        // Replace the first entry with {@code null} via reflection.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) varsField.get(model);
        vars.set(0, null);
        // Reset the categorisation caches so that the next call recomputes them.
        Field catField = ExpressionsBasedModel.class.getDeclaredField("myVariablesCategorisation");
        catField.setAccessible(true);
        ExpressionsBasedModel.VariablesCategorisation cat = (ExpressionsBasedModel.VariablesCategorisation) catField.get(model);
        cat.reset();
        // The method should now throw NPE because it tries to call methods on the null entry.
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(0));
    }

    /**
     * Malformed lower limit – {@code isLowerLimitSet()} returns {@code true}
     * but {@code getLowerLimit()} returns {@code null}. This should also raise a
     * {@link NullPointerException} during categorisation.
     */
    @Test
    void testMalformedLowerLimitHandling() throws Exception {
        // Create a normal variable to have a list entry we can tamper with.
        model.addVariable();
        // Access the internal variable list.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) varsField.get(model);
        Variable v = vars.get(0);
        // Force the lower‑limit flag to true and set the lower limit to null.
        // The exact field names are implementation‑specific; we attempt the most common ones.
        // If the field names differ, the test will fail to compile, signalling a need for adjustment.
        try {
            Field lowerSetField = Variable.class.getDeclaredField("myLowerSet");
            lowerSetField.setAccessible(true);
            lowerSetField.setBoolean(v, true);
        } catch (NoSuchFieldException ignored) {
            // Alternative field name used in some versions.
            Field lowerSetField = Variable.class.getDeclaredField("myIsLowerSet");
            lowerSetField.setAccessible(true);
            lowerSetField.setBoolean(v, true);
        }
        try {
            Field lowerField = Variable.class.getDeclaredField("myLower");
            lowerField.setAccessible(true);
            lowerField.set(v, null);
        } catch (NoSuchFieldException ignored) {
            // Alternative field name.
            Field lowerField = Variable.class.getDeclaredField("myLowerLimit");
            lowerField.setAccessible(true);
            lowerField.set(v, null);
        }
        // Reset categorisation caches.
        Field catField = ExpressionsBasedModel.class.getDeclaredField("myVariablesCategorisation");
        catField.setAccessible(true);
        ExpressionsBasedModel.VariablesCategorisation cat = (ExpressionsBasedModel.VariablesCategorisation) catField.get(model);
        cat.reset();
        // Expect NPE because the code will attempt to use the null lower limit.
        assertThrows(NullPointerException.class, () -> model.indexOfNegativeVariable(0));
    }
}
