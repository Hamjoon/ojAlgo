package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_Test {

    private ExpressionsBasedModel model;

    private static class MixedPatternHolder {

        // ThreadLocal ensures each test execution gets its own model instance.
        static final ThreadLocal<ExpressionsBasedModel> MODEL = ThreadLocal.withInitial(() -> {
            ExpressionsBasedModel m = new ExpressionsBasedModel();
            // 0 – integer
            Variable v0 = m.addVariable();
            v0.integer(true);
            // 1 – non‑integer
            m.addVariable();
            // 2 – integer
            Variable v2 = m.addVariable();
            v2.integer(true);
            // 3 – non‑integer
            m.addVariable();
            // 4 – integer
            Variable v4 = m.addVariable();
            v4.integer(true);
            // 5 – non‑integer
            m.addVariable();
            return m;
        });
    }

    @FunctionalInterface
    interface VariableConfigurer {

        void configure(Variable v);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures independence.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    private static Stream<Arguments> mixedPatternProvider() {
        return Stream.of(Arguments.of(0, 0), Arguments.of(1, -1), Arguments.of(2, 1), Arguments.of(3, -1), Arguments.of(4, 2), Arguments.of(5, -1));
    }

    private static Stream<Arguments> upperLowerLimitsProvider() {
        VariableConfigurer cfg0 = v -> {
            v.integer(true);
            v.upper(BigDecimal.valueOf(-5));
            v.lower(BigDecimal.ONE);
        };
        // no limits
        VariableConfigurer cfg1 = v -> v.integer(true);
        return Stream.of(Arguments.of(0, 0, cfg0), Arguments.of(1, 1, cfg1));
    }

    private static Stream<Arguments> bothLimitsProvider() {
        VariableConfigurer cfg0 = v -> {
            v.integer(true);
            v.upper(BigDecimal.valueOf(10));
            v.lower(BigDecimal.valueOf(-5));
        };
        VariableConfigurer cfg1 = v -> {
            v.integer(true);
            v.upper(BigDecimal.valueOf(20));
            v.lower(BigDecimal.valueOf(-10));
        };
        return Stream.of(Arguments.of(0, 0, cfg0), Arguments.of(1, 1, cfg1));
    }

    /**
     * Verify that an integer variable is assigned index {@code 0} when it is the first
     * integer variable added to the model.
     */
    @Test
    void testFirstIntegerVariableIndex() {
        Variable intVar = model.addVariable();
        intVar.integer(true);
        int idx = model.indexOfIntegerVariable(intVar);
        Assertions.assertEquals(0, idx, "First integer variable should have index 0");
    }

    /**
     * Verify that a non‑integer variable yields {@code -1}.
     */
    @Test
    void testNonIntegerVariableReturnsMinusOne() {
        Variable nonIntVar = model.addVariable();
        int idx = model.indexOfIntegerVariable(nonIntVar);
        Assertions.assertEquals(-1, idx, "Non‑integer variable should return -1");
    }

    /**
     * Verify the ordering when several variables are added, mixing integer and non‑integer ones.
     */
    @Test
    void testMixedVariablesOrdering() {
        // non‑integer
        Variable v0 = model.addVariable();
        // integer
        Variable v1 = model.addVariable();
        // integer
        v1.integer(true);
        // integer
        Variable v2 = model.addVariable();
        // integer
        v2.integer(true);
        // non‑integer
        Variable v3 = model.addVariable();
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(v0), "v0 should not be integer");
        Assertions.assertEquals(0, model.indexOfIntegerVariable(v1), "v1 should be first integer variable");
        Assertions.assertEquals(1, model.indexOfIntegerVariable(v2), "v2 should be second integer variable");
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(v3), "v3 should not be integer");
    }

    /**
     * Directly exercise the {@code int} overload using the global index.
     */
    @Test
    void testIndexOfIntegerVariableWithGlobalIndex() {
        // index 0 – non‑integer
        model.addVariable();
        // index 1 – integer
        Variable intVar = model.addVariable();
        intVar.integer(true);
        // index 2 – non‑integer
        model.addVariable();
        int result = model.indexOfIntegerVariable(1);
        Assertions.assertEquals(0, result, "Global index 1 corresponds to first integer variable");
    }

    @Test
    void testEmptyModelThrowsException() {
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(0), "Calling on an empty model should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void testSingleIntegerVariable() {
        Variable v = model.addVariable();
        v.integer(true);
        Assertions.assertEquals(0, model.indexOfIntegerVariable(0), "Single free integer variable must map to integer index 0");
    }

    @Test
    void testSingleNonIntegerVariable() {
        model.addVariable();
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(0), "Non‑integer variable must return -1");
    }

    @Test
    void testFixedIntegerVariableReturnsMinusOne() {
        Variable v = model.addVariable();
        v.integer(true);
        v.fix();
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(0), "Fixed integer variable must return -1");
    }

    @Test
    void testUpperLowerLimitBranches() {
        Variable v0 = model.addVariable();
        v0.integer(true);
        v0.upper(BigDecimal.valueOf(-5));
        v0.lower(BigDecimal.ONE);
        Variable v1 = model.addVariable();
        v1.integer(true);
        Assertions.assertEquals(0, model.indexOfIntegerVariable(0), "Variable 0 should be first integer variable (index 0)");
        Assertions.assertEquals(1, model.indexOfIntegerVariable(1), "Variable 1 should be second integer variable (index 1)");
    }

    @Test
    void testCacheReuseSecondCall() {
        Variable v = model.addVariable();
        v.integer(true);
        int first = model.indexOfIntegerVariable(0);
        int second = model.indexOfIntegerVariable(0);
        Assertions.assertEquals(0, first, "First call must return 0");
        Assertions.assertEquals(0, second, "Second call must return the same cached value");
    }

    @Test
    void testOutOfBoundsIndexThrowsException() {
        model.addVariable();
        model.addVariable();
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(3), "Index larger than variable count must throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void testIntegerVariablesWithBothPositiveUpperAndNegativeLowerLimits() {
        Variable v0 = model.addVariable();
        v0.integer(true);
        v0.upper(BigDecimal.valueOf(10));
        v0.lower(BigDecimal.valueOf(-5));
        Variable v1 = model.addVariable();
        v1.integer(true);
        v1.upper(BigDecimal.valueOf(20));
        v1.lower(BigDecimal.valueOf(-10));
        Assertions.assertEquals(0, model.indexOfIntegerVariable(0), "First integer variable with both limits should be index 0");
        Assertions.assertEquals(1, model.indexOfIntegerVariable(1), "Second integer variable with both limits should be index 1");
    }

    @Test
    void testCacheStaleAfterChangingIntegerFlag() {
        // non‑integer initially
        model.addVariable();
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(0), "Initial call should return -1 for non‑integer variable");
        Variable v = model.getVariable(0);
        v.integer(true);
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(0), "After flag change, stale cache should still return -1");
    }

    @Test
    void testAddingNewIntegerVariableAfterFirstCategorisation() {
        Variable v0 = model.addVariable();
        v0.integer(true);
        Assertions.assertEquals(0, model.indexOfIntegerVariable(0), "First integer variable should be index 0");
        Variable v1 = model.addVariable();
        v1.integer(true);
        Assertions.assertEquals(1, model.indexOfIntegerVariable(1), "Second integer variable added later should be index 1");
    }

    @Test
    void testMixedPatternManyVariables() {
        Variable v0 = model.addVariable();
        v0.integer(true);
        // non‑integer
        model.addVariable();
        Variable v2 = model.addVariable();
        v2.integer(true);
        // non‑integer
        model.addVariable();
        Variable v4 = model.addVariable();
        v4.integer(true);
        // non‑integer
        model.addVariable();
        Assertions.assertEquals(0, model.indexOfIntegerVariable(0), "Global 0 → integer index 0");
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(1), "Global 1 → non‑integer");
        Assertions.assertEquals(1, model.indexOfIntegerVariable(2), "Global 2 → integer index 1");
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(3), "Global 3 → non‑integer");
        Assertions.assertEquals(2, model.indexOfIntegerVariable(4), "Global 4 → integer index 2");
        Assertions.assertEquals(-1, model.indexOfIntegerVariable(5), "Global 5 → non‑integer");
    }

    @Test
    void testNegativeIndexThrowsException() {
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(-1), "Negative global index must throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void testExtremeOutOfBoundsIndexThrowsException() {
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(Integer.MAX_VALUE), "Extremely large index must throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Parameterised test for the "Mixed pattern – many variables" group.
     * The data provider builds the model once and supplies the global index
     * together with the expected result.
     */
    @ParameterizedTest(name = "globalIndex={0} → expected={1}")
    @MethodSource("mixedPatternProvider")
    void testMixedPatternParameterized(int globalIndex, int expected) {
        ExpressionsBasedModel localModel = MixedPatternHolder.MODEL.get();
        Assertions.assertEquals(expected, localModel.indexOfIntegerVariable(globalIndex), "Unexpected integer index for global index " + globalIndex);
    }

    /**
     * Parameterised test for the "Upper / lower limit branches – integer variable with limits"
     * group. The model is rebuilt for each argument set to keep the test isolated.
     */
    @ParameterizedTest(name = "globalIndex={0} → expected={1}")
    @MethodSource("upperLowerLimitsProvider")
    void testUpperLowerLimitsParameterized(int globalIndex, int expected, VariableConfigurer configurer) {
        ExpressionsBasedModel localModel = new ExpressionsBasedModel();
        Variable v0 = localModel.addVariable();
        Variable v1 = localModel.addVariable();
        if (globalIndex == 0) {
            configurer.configure(v0);
        } else if (globalIndex == 1) {
            configurer.configure(v1);
        }
        Assertions.assertEquals(expected, localModel.indexOfIntegerVariable(globalIndex), "Unexpected result for global index " + globalIndex);
    }

    /**
     * Parameterised test for the "Integer variables with both positive upper and negative lower limits"
     * group.
     */
    @ParameterizedTest(name = "globalIndex={0} → expected={1}")
    @MethodSource("bothLimitsProvider")
    void testBothLimitsParameterized(int globalIndex, int expected, VariableConfigurer configurer) {
        ExpressionsBasedModel localModel = new ExpressionsBasedModel();
        Variable v0 = localModel.addVariable();
        Variable v1 = localModel.addVariable();
        if (globalIndex == 0) {
            configurer.configure(v0);
        } else if (globalIndex == 1) {
            configurer.configure(v1);
        }
        Assertions.assertEquals(expected, localModel.indexOfIntegerVariable(globalIndex), "Unexpected integer index for global index " + globalIndex);
    }
}
