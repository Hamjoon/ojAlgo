package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class ExpressionsBasedModel_isAnyVariableInteger_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> normalScenarioProvider() {
        return Stream.of(// 1) relaxed model → false
        Arguments.of(true, List.of(), false, "Relaxed model must return false even if variables exist"), // 2) not relaxed, empty list → false
        Arguments.of(false, List.of(), false, "Empty variable list should yield false"), // 3) not relaxed, mixed but no qualifying integer → false
        Arguments.of(false, List.of(// non‑integer
        Variable.make("c1").lower(0).upper(5), // integer fixed
        Variable.make("iFixed").integer(true).level(2), // non‑integer fixed (irrelevant)
        Variable.make("c2").lower(-1).upper(1)), false, "No non‑fixed integer variable present"), // 4) first variable qualifies → true
        Arguments.of(false, List.of(Variable.make("iFree").integer(true).lower(0).upper(3)), true, "First variable is non‑fixed integer"), // 5) first integer fixed, second qualifies → true
        Arguments.of(false, List.of(Variable.make("iFixed").integer(true).level(4), Variable.make("iFree").integer(true).lower(0).upper(2)), true, "Second variable is the qualifying non‑fixed integer"), // 6) qualifying integer after a non‑integer → true
        Arguments.of(false, List.of(Variable.make("c1").lower(0).upper(1), Variable.make("iFree").integer(true).lower(0).upper(10), Variable.make("iFree2").integer(true).lower(5).upper(15)), true, "Qualifying integer appears after a non‑integer"));
    }

    /**
     * The model contains an integer variable that is not fixed.
     * Expected result: {@code true}
     */
    @Test
    void testAnyVariableIntegerTrue() {
        // integer, not fixed (lower/upper differ)
        Variable intVar = Variable.make("x").integer(true).lower(0).upper(10);
        model.addVariable(intVar);
        // also add a continuous variable to prove the method scans all variables
        Variable contVar = Variable.make("y").lower(0).upper(5);
        model.addVariable(contVar);
        assertTrue(model.isAnyVariableInteger(), "Model should report that it has at least one non‑fixed integer variable");
    }

    /**
     * The model contains an integer variable but the model is relaxed.
     * Expected result: {@code false}
     */
    @Test
    void testAnyVariableIntegerFalseWhenRelaxed() {
        Variable intVar = Variable.make("x").integer(true).lower(0).upper(10);
        model.addVariable(intVar);
        // switch the model to relaxed mode
        model.relax();
        assertFalse(model.isAnyVariableInteger(), "Relaxed model must never report an integer variable");
    }

    /**
     * The model contains only integer variables that are fixed (level set).
     * Expected result: {@code false}
     */
    @Test
    void testAnyVariableIntegerFalseWhenAllIntegerFixed() {
        // a fixed integer variable – level sets both lower and upper to the same value
        Variable fixedInt = Variable.make("x").integer(true).level(5);
        model.addVariable(fixedInt);
        // another fixed integer variable
        Variable fixedInt2 = Variable.make("y").integer(true).level(3);
        model.addVariable(fixedInt2);
        assertFalse(model.isAnyVariableInteger(), "Model with only fixed integer variables should report false");
    }

    /**
     * The model contains no integer variables at all.
     * Expected result: {@code false}
     */
    @Test
    void testAnyVariableIntegerFalseWhenNoIntegerVariables() {
        Variable cont1 = Variable.make("a").lower(0).upper(4);
        Variable cont2 = Variable.make("b").lower(-2).upper(2);
        model.addVariable(cont1);
        model.addVariable(cont2);
        assertFalse(model.isAnyVariableInteger(), "Model without integer variables should report false");
    }

    /**
     * Mixed case: at least one non‑fixed integer variable among other variables.
     * Expected result: {@code true}
     */
    @Test
    void testAnyVariableIntegerTrueMixedVariables() {
        // fixed integer variable – should be ignored
        model.addVariable(Variable.make("fixedInt").integer(true).level(1));
        // non‑fixed integer variable – should trigger true
        model.addVariable(Variable.make("freeInt").integer(true).lower(0).upper(5));
        // continuous variable – irrelevant for the check
        model.addVariable(Variable.make("cont").lower(0).upper(10));
        assertTrue(model.isAnyVariableInteger(), "Presence of a non‑fixed integer variable should make the method return true");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalScenarioProvider")
    void testIsAnyVariableIntegerParameterized(boolean relaxed, List<Variable> variables, boolean expected, String description) throws Exception {
        // Populate the model
        for (Variable v : variables) {
            model.addVariable(v);
        }
        // Apply relaxation if required
        if (relaxed) {
            model.relax();
        }
        assertEquals(expected, model.isAnyVariableInteger(), description);
    }

    /**
     * If the internal {@code myVariables} list is {@code null} the method
     * should throw a {@link NullPointerException}.
     */
    @Test
    void testNullVariablesListThrowsNPE() throws Exception {
        // Use reflection to set the private final field to null
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        field.set(model, null);
        assertThrows(NullPointerException.class, () -> model.isAnyVariableInteger(), "Calling isAnyVariableInteger() with a null variable list must throw NPE");
    }

    /**
     * If the list contains a {@code null} element the method should throw a
     * {@link NullPointerException} when it tries to invoke {@code isInteger()} on it.
     */
    @Test
    void testNullElementInVariablesThrowsNPE() throws Exception {
        // Create a list with a single null entry
        List<Variable> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        // Inject it via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        field.set(model, listWithNull);
        assertThrows(NullPointerException.class, () -> model.isAnyVariableInteger(), "A null variable inside the list must cause NPE");
    }

    /**
     * Simulate a concurrent modification where the size captured at the start of the
     * method is larger than the actual list size during iteration.
     *
     * The custom list reports a size of 2 but throws {@link IndexOutOfBoundsException}
     * on the second {@code get} call.
     */
    @Test
    void testConcurrentModificationThrowsIOOBE() throws Exception {
        // Custom list implementation
        List<Variable> flakyList = new AbstractList<Variable>() {

            private final Variable first = Variable.make("x").integer(true).lower(0).upper(1);

            @Override
            public Variable get(int index) {
                if (index == 0) {
                    return first;
                }
                // Simulate shrinkage after the size was read
                throw new IndexOutOfBoundsException("Simulated concurrent modification");
            }

            @Override
            public int size() {
                // The method reads this value before the loop, so we claim there are 2 elements
                return 2;
            }
        };
        // Inject the flaky list
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        field.set(model, flakyList);
        // The method is expected to propagate the exception
        assertThrows(IndexOutOfBoundsException.class, () -> model.isAnyVariableInteger(), "Concurrent modification should result in IndexOutOfBoundsException");
    }
}