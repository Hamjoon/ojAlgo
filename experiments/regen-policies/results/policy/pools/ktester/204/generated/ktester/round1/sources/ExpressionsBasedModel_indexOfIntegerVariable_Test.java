package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_Test {

    private ExpressionsBasedModel model;

    // index 0 – not integer, free
    private Variable vNonInt;

    // index 1 – integer, free
    private Variable vInt1;

    // index 2 – integer, free
    private Variable vInt2;

    // index 3 – integer but fixed
    private Variable vIntFixed;

    /**
     * Simple DTO describing a variable configuration for the parameterised tests.
     */
    private static class VariableSpec {

        final boolean isInteger;

        final boolean isFixed;

        final Number fixedValue;

        VariableSpec(boolean isInteger, boolean isFixed, Number fixedValue) {
            this.isInteger = isInteger;
            this.isFixed = isFixed;
            this.fixedValue = fixedValue;
        }

        static VariableSpec integerFree() {
            return new VariableSpec(true, false, null);
        }

        static VariableSpec integerFixed(Number val) {
            return new VariableSpec(true, true, val);
        }

        static VariableSpec nonIntegerFree() {
            return new VariableSpec(false, false, null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // 0 – non‑integer variable (free)
        vNonInt = model.addVariable().lower(0).upper(10);
        // 1 – free integer variable
        vInt1 = model.addVariable().integer(true).lower(0).upper(10);
        // 2 – another free integer variable
        vInt2 = model.addVariable().integer(true).lower(0).upper(10);
        // 3 – integer variable that is fixed (should be ignored)
        vIntFixed = model.addVariable().integer(true).level(5);
    }

    /**
     * Helper to create a fresh model for each parameterised test.
     */
    private static ExpressionsBasedModel createModelWithVariables(List<VariableSpec> specs) {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        for (VariableSpec spec : specs) {
            Variable v = m.addVariable();
            if (spec.isInteger) {
                v.integer(true);
            }
            if (spec.isFixed) {
                v.level(spec.fixedValue);
            } else {
                // give a non‑trivial bound so the variable stays “free”
                v.lower(BigDecimal.ZERO);
                v.upper(BigDecimal.TEN);
            }
        }
        return m;
    }

    /**
     * Data provider for the “Initial categorisation and integer index retrieval” group.
     */
    private static Stream<Arguments> initialCategorisationProvider() {
        // Model: [integer free]
        ExpressionsBasedModel m = createModelWithVariables(List.of(VariableSpec.integerFree()));
        return Stream.of(Arguments.of(m, 0, 0, "First variable is integer and not fixed, so its integer‑variable index is 0."));
    }

    /**
     * Data provider for the “Mixed integer and non‑integer variables” group.
     */
    private static Stream<Arguments> mixedVariablesProvider() {
        // Model: [integer free, non‑integer free, integer free, non‑integer free]
        ExpressionsBasedModel m = createModelWithVariables(List.of(VariableSpec.integerFree(), VariableSpec.nonIntegerFree(), VariableSpec.integerFree(), VariableSpec.nonIntegerFree()));
        return Stream.of(Arguments.of(m, 0, 0, "First variable is integer and not fixed → integer‑variable index 0."), Arguments.of(m, 1, -1, "Second variable is non‑integer → method returns -1."), Arguments.of(m, 2, 1, "Third variable is integer and not fixed → second integer‑variable, index 1."), Arguments.of(m, 3, -1, "Fourth variable is non‑integer → method returns -1."));
    }

    /**
     * Data provider for the “All variables non‑integer” group.
     */
    private static Stream<Arguments> allNonIntegerProvider() {
        // Model: three non‑integer free variables
        ExpressionsBasedModel m = createModelWithVariables(List.of(VariableSpec.nonIntegerFree(), VariableSpec.nonIntegerFree(), VariableSpec.nonIntegerFree()));
        return Stream.of(Arguments.of(m, 0, -1, "Model contains only non‑integer variables; any index should return -1."), Arguments.of(m, 2, -1, "Even for later positions, non‑integer variables yield -1."));
    }

    /**
     * Data provider for the “Fixed integer variable” group.
     */
    private static Stream<Arguments> fixedIntegerProvider() {
        // Model: one integer variable that is fixed
        ExpressionsBasedModel m = createModelWithVariables(List.of(VariableSpec.integerFixed(7)));
        return Stream.of(Arguments.of(m, 0, -1, "Variable is integer but marked as fixed; fixed variables are excluded from the integer list, so -1 is returned."));
    }

    /**
     * Data provider for the “Out‑of‑range index” group (expects IndexOutOfBoundsException).
     */
    private static Stream<Arguments> outOfRangeProvider() {
        // Model with two free variables (indices 0 and 1)
        ExpressionsBasedModel m = createModelWithVariables(List.of(VariableSpec.nonIntegerFree(), VariableSpec.integerFree()));
        return Stream.of(Arguments.of(m, -1), Arguments.of(m, 5));
    }

    /**
     * Data provider for the “Empty model” group (expects IndexOutOfBoundsException).
     */
    private static Stream<Arguments> emptyModelProvider() {
        // no variables added
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        return Stream.of(Arguments.of(m, 0));
    }

    /**
     * Data provider for the “Null variable handling” group (expects NullPointerException).
     */
    private static Stream<Arguments> nullVariableProvider() throws Exception {
        ExpressionsBasedModel m = new ExpressionsBasedModel();
        // Insert a null entry into the private myVariables list via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) field.get(m);
        // index 0 is null
        list.add(null);
        return Stream.of(Arguments.of(m, 0));
    }

    @Test
    void testIndexMappingForMixedVariables() {
        // Global indices: 0,1,2,3
        // Expected integer‑only indices: -1,0,1,-1
        assertEquals(-1, model.indexOfIntegerVariable(0), "Non‑integer variable should yield -1");
        assertEquals(0, model.indexOfIntegerVariable(1), "First free integer variable should map to 0");
        assertEquals(1, model.indexOfIntegerVariable(2), "Second free integer variable should map to 1");
        assertEquals(-1, model.indexOfIntegerVariable(3), "Fixed integer variable should be ignored and yield -1");
    }

    @Test
    void testOutOfRangeIndicesReturnMinusOne() {
        // current number of variables
        int maxIndex = model.countVariables();
        assertEquals(-1, model.indexOfIntegerVariable(maxIndex), "Index equal to size should be out of range and return -1");
        assertEquals(-1, model.indexOfIntegerVariable(-1), "Negative index should return -1");
    }

    @Test
    void testConsistencyWithVariableBasedLookup() {
        // The overloads that accept Variable or IntIndex should delegate to the same logic.
        assertEquals(model.indexOfIntegerVariable(vInt1.getIndex().index), model.indexOfIntegerVariable(vInt1), "Variable‑based overload must be consistent with int‑based overload");
        assertEquals(model.indexOfIntegerVariable(vIntFixed.getIndex().index), model.indexOfIntegerVariable(vIntFixed), "Fixed integer variable overload must also be consistent");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("initialCategorisationProvider")
    void testInitialCategorisation(ExpressionsBasedModel m, int globalIdx, int expected, String description) {
        assertEquals(expected, m.indexOfIntegerVariable(globalIdx), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mixedVariablesProvider")
    void testMixedVariables(ExpressionsBasedModel m, int globalIdx, int expected, String description) {
        assertEquals(expected, m.indexOfIntegerVariable(globalIdx), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("allNonIntegerProvider")
    void testAllNonIntegerVariables(ExpressionsBasedModel m, int globalIdx, int expected, String description) {
        assertEquals(expected, m.indexOfIntegerVariable(globalIdx), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("fixedIntegerProvider")
    void testFixedIntegerVariable(ExpressionsBasedModel m, int globalIdx, int expected, String description) {
        assertEquals(expected, m.indexOfIntegerVariable(globalIdx), description);
    }

    @ParameterizedTest(name = "[{index}] Out‑of‑range index {1}")
    @MethodSource("outOfRangeProvider")
    void testOutOfRangeIndicesThrow(ExpressionsBasedModel m, int globalIdx) {
        assertThrows(IndexOutOfBoundsException.class, () -> m.indexOfIntegerVariable(globalIdx), "Accessing out‑of‑range index should throw IndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "[{index}] Empty model index {1}")
    @MethodSource("emptyModelProvider")
    void testEmptyModelThrows(ExpressionsBasedModel m, int globalIdx) {
        assertThrows(IndexOutOfBoundsException.class, () -> m.indexOfIntegerVariable(globalIdx), "Empty model should throw IndexOutOfBoundsException for any index");
    }

    @ParameterizedTest(name = "[{index}] Null variable handling at index {1}")
    @MethodSource("nullVariableProvider")
    void testNullVariableThrows(ExpressionsBasedModel m, int globalIdx) {
        assertThrows(NullPointerException.class, () -> m.indexOfIntegerVariable(globalIdx), "Null entry in variable list should cause NullPointerException");
    }
}
