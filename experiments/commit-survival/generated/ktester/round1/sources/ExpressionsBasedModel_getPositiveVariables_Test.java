package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getPositiveVariables_Test {

    private ExpressionsBasedModel model;

    @Nested
    @DisplayName("Empty model")
    class EmptyModel {

        @Test
        @DisplayName("When the model contains no variables, the list is empty")
        void emptyModelReturnsEmptyList() {
            List<Variable> positives = model.getPositiveVariables();
            assertTrue(positives.isEmpty(), "Expected empty list for a model without variables");
        }
    }

    @Nested
    @DisplayName("Single variable scenarios")
    class SingleVariableScenarios {

        @Test
        @DisplayName("Variable with no upper limit is positive")
        void variableNoUpperIsPositive() {
            Variable x1 = model.newVariable("x1").lower(0);
            List<Variable> positives = model.getPositiveVariables();
            assertEquals(Collections.singletonList(x1), positives);
        }

        @Test
        @DisplayName("Variable with positive upper limit is positive")
        void variablePositiveUpperIsPositive() {
            Variable x2 = model.newVariable("x2").lower(0).upper(10);
            List<Variable> positives = model.getPositiveVariables();
            assertEquals(Collections.singletonList(x2), positives);
        }

        @Test
        @DisplayName("Variable with zero upper limit is NOT positive")
        void variableZeroUpperIsNotPositive() {
            Variable x3 = model.newVariable("x3").lower(-5).upper(0);
            List<Variable> positives = model.getPositiveVariables();
            assertTrue(positives.isEmpty(), "Zero upper limit should exclude the variable");
        }

        @Test
        @DisplayName("Fixed variable is NOT positive")
        void fixedVariableIsNotPositive() {
            Variable x4 = model.newVariable("x4").level(5);
            List<Variable> positives = model.getPositiveVariables();
            assertTrue(positives.isEmpty(), "Fixed variable must be excluded");
        }
    }

    @Nested
    @DisplayName("Mixed scenario")
    class MixedScenario {

        @Test
        @DisplayName("Only variables x5 and x6 are positive")
        void mixedVariables() {
            // no upper → positive
            Variable x5 = model.newVariable("x5").lower(-2);
            // positive upper → positive
            Variable x6 = model.newVariable("x6").lower(0).upper(5);
            // negative upper → not positive
            Variable x7 = model.newVariable("x7").lower(-3).upper(-3);
            // fixed → not positive
            Variable x8 = model.newVariable("x8").level(7);
            List<Variable> positives = model.getPositiveVariables();
            assertEquals(2, positives.size());
            assertTrue(positives.contains(x5));
            assertTrue(positives.contains(x6));
            assertFalse(positives.contains(x7));
            assertFalse(positives.contains(x8));
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("List returned is unmodifiable")
        void listIsUnmodifiable() {
            model.newVariable("v").lower(0).upper(10);
            List<Variable> positives = model.getPositiveVariables();
            assertThrows(UnsupportedOperationException.class, () -> positives.add(null));
        }

        @Test
        @DisplayName("Iterator remove throws UnsupportedOperationException")
        void iteratorRemoveIsUnsupported() {
            model.newVariable("a").lower(0);
            model.newVariable("b").lower(0);
            List<Variable> positives = model.getPositiveVariables();
            Iterator<Variable> it = positives.iterator();
            assertTrue(it.hasNext());
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }
    }

    @Nested
    @DisplayName("Ordering")
    class OrderingTests {

        @Test
        @DisplayName("Insertion order is preserved")
        void insertionOrderPreserved() {
            Variable a = model.newVariable("a").lower(0);
            Variable b = model.newVariable("b").lower(0);
            Variable c = model.newVariable("c").lower(0);
            List<Variable> positives = model.getPositiveVariables();
            assertEquals(List.of(a, b, c), positives, "The order of variables must match insertion order");
        }
    }

    @Nested
    @DisplayName("State change after retrieval")
    class StateChangeTests {

        @Test
        @DisplayName("Variable removed from list after upper limit becomes negative")
        void variableStateChangeReflectsInList() {
            Variable v1 = model.newVariable("v1").lower(0).upper(10);
            assertTrue(model.getPositiveVariables().contains(v1));
            // Change upper limit to a negative value
            v1.upper(-5);
            List<Variable> afterChange = model.getPositiveVariables();
            assertFalse(afterChange.contains(v1), "Variable should disappear after upper limit becomes negative");
        }
    }

    @Nested
    @DisplayName("Integer variable handling")
    class IntegerVariableTests {

        @Test
        @DisplayName("Integer variable with positive upper limit is still positive")
        void integerVariableIsPositive() {
            Variable intPos = model.newVariable("intPos").lower(0).upper(10);
            // Mark as integer – the exact API may differ; using the generic integer(true) method
            intPos.integer(true);
            List<Variable> positives = model.getPositiveVariables();
            assertTrue(positives.contains(intPos), "Integer status must not affect positive classification");
        }
    }

    @Nested
    @DisplayName("Mixed upper‑limit presence")
    class MixedUpperLimitPresence {

        @Test
        @DisplayName("Variables with and without upper limits are both positive")
        void mixedUpperLimits() {
            Variable noUpper = model.newVariable("noUpper").lower(-1);
            Variable posUpper = model.newVariable("posUpper").lower(0).upper(5);
            List<Variable> positives = model.getPositiveVariables();
            assertEquals(2, positives.size());
            assertTrue(positives.containsAll(List.of(noUpper, posUpper)));
        }
    }

    @Nested
    @DisplayName("Large set count")
    class LargeSetCount {

        @Test
        @DisplayName("Exactly 50 positive variables out of 100")
        void fiftyPositiveOutOfHundred() {
            IntStream.range(0, 100).forEach(i -> {
                Variable v = model.newVariable("v" + i).lower(-10);
                if (i < 50) {
                    // positive upper limit
                    v.upper(10);
                } else {
                    // negative upper limit
                    v.upper(-10);
                }
            });
            List<Variable> positives = model.getPositiveVariables();
            assertEquals(50, positives.size(), "Should return exactly 50 positive variables");
        }
    }

    @Nested
    @DisplayName("Robustness against null entries")
    class NullHandlingTests {

        @Test
        @DisplayName("Null variable inside internal list triggers NullPointerException")
        void nullVariableInList() throws Exception {
            // Insert a null entry directly into the private myVariables list
            Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
            varsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ArrayList<Variable> internalList = (ArrayList<Variable>) varsField.get(model);
            internalList.add(null);
            assertThrows(NullPointerException.class, () -> model.getPositiveVariables(), "A null entry should cause NullPointerException");
        }

        @Test
        @DisplayName("Variable with null upper limit causes NullPointerException")
        void variableWithNullUpperLimit() throws Exception {
            Variable v = model.newVariable("nullUpper").lower(0);
            // Force the internal upper‑limit field to null.
            Field upperField = null;
            for (String name : new String[] { "myUpperLimit", "upper", "upperLimit" }) {
                try {
                    upperField = v.getClass().getDeclaredField(name);
                    upperField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            assertNotNull(upperField, "Unable to locate upper‑limit field via reflection");
            // Set the field to null while keeping the flag that indicates the limit is set.
            upperField.set(v, null);
            // Some implementations have a separate boolean flag; try to set it if present.
            try {
                Field flag = v.getClass().getDeclaredField("myUpperLimitSet");
                flag.setAccessible(true);
                flag.setBoolean(v, true);
            } catch (NoSuchFieldException ignored) {
                // Flag not present – rely on the null check inside getUpperLimit()
            }
            assertThrows(NullPointerException.class, () -> model.getPositiveVariables(), "Null upper limit should cause NullPointerException");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    /**
     * Verify that only variables that are not fixed and have a positive (or undefined) upper limit
     * appear in the list returned by {@code getPositiveVariables()}.
     */
    @Test
    void testPositiveVariablesFiltering() {
        // 1. Positive upper limit, not fixed  → should be present
        Variable vPos = model.newVariable("vPos").lower(0).upper(10).weight(1);
        // 2. Upper limit zero (non‑positive), not fixed → should NOT be present
        Variable vZero = model.newVariable("vZero").lower(-5).upper(0).weight(1);
        // 3. Upper limit negative, not fixed → should NOT be present
        Variable vNeg = model.newVariable("vNeg").lower(-10).upper(-1).weight(1);
        // 4. No upper limit set, not fixed → should be present
        // only lower bound
        Variable vNoUpper = model.newVariable("vNoUpper").lower(-5).weight(1);
        // 5. Fixed variable (lower == upper == 5) → should NOT be present even though upper is positive
        Variable vFixed = model.newVariable("vFixed").level(5).weight(1);
        List<Variable> positives = model.getPositiveVariables();
        // The list must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> positives.add(vPos));
        // Expected variables are vPos and vNoUpper (order corresponds to insertion order in the model)
        assertEquals(2, positives.size(), "Exactly two variables should be classified as positive");
        assertTrue(positives.contains(vPos), "Variable with positive upper limit must be present");
        assertTrue(positives.contains(vNoUpper), "Variable with no upper limit must be present");
        // Ensure that variables that should be excluded are indeed absent
        assertFalse(positives.contains(vZero), "Variable with zero upper limit must be absent");
        assertFalse(positives.contains(vNeg), "Variable with negative upper limit must be absent");
        assertFalse(positives.contains(vFixed), "Fixed variable must be absent");
    }

    /**
     * Verify that the returned list reflects the current state of the model after
     * adding additional variables.
     */
    @Test
    void testPositiveVariablesAfterAddingMoreVariables() {
        // positive → should be present
        Variable a = model.newVariable("a").lower(0).upper(5);
        // fixed (negative) → absent
        Variable b = model.newVariable("b").lower(-2).upper(-2);
        // no upper → present
        Variable c = model.newVariable("c").lower(-1);
        List<Variable> firstCall = model.getPositiveVariables();
        assertEquals(2, firstCall.size());
        assertTrue(firstCall.contains(a));
        assertTrue(firstCall.contains(c));
        // Add another variable that satisfies the positive criteria
        Variable d = model.newVariable("d").lower(1).upper(100);
        List<Variable> secondCall = model.getPositiveVariables();
        assertEquals(3, secondCall.size());
        assertTrue(secondCall.contains(d));
    }
}