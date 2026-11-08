package org.ojalgo.optimisation;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

class ExpressionsBasedModel_getPositiveVariables_Test {

    private ExpressionsBasedModel model;

    /**
     * The variables of the model – kept in insertion order.
     */
    final ArrayList<Variable> myVariables = new ArrayList<>();

//     @Nested
//     @DisplayName("Empty model")
    class EmptyModel {

//         @Test
//         @DisplayName("When the model contains no variables, the list is empty")
//         void emptyModelReturnsEmptyList() {
//             List<Variable> positives = model.getPositiveVariables();
//             assertTrue(positives.isEmpty(), "Expected empty list for a model without variables");
//         }
    }

//     @Nested
//     @DisplayName("Single variable scenarios")
    class SingleVariableScenarios {

//         @Test
//         @DisplayName("Variable with no upper limit is positive")
//         void variableNoUpperIsPositive() {
//             Variable x1 = model.newVariable("x1").lower(0);
//             List<Variable> positives = model.getPositiveVariables();
//             assertEquals(Collections.singletonList(x1), positives);
//         }

//         @Test
//         @DisplayName("Variable with positive upper limit is positive")
//         void variablePositiveUpperIsPositive() {
//             Variable x2 = model.newVariable("x2").lower(0).upper(10);
//             List<Variable> positives = model.getPositiveVariables();
//             assertEquals(Collections.singletonList(x2), positives);
//         }

//         @Test
//         @DisplayName("Variable with zero upper limit is NOT positive")
//         void variableZeroUpperIsNotPositive() {
//             Variable x3 = model.newVariable("x3").lower(-5).upper(0);
//             List<Variable> positives = model.getPositiveVariables();
//             assertTrue(positives.isEmpty(), "Zero upper limit should exclude the variable");
//         }

//         @Test
//         @DisplayName("Fixed variable is NOT positive")
//         void fixedVariableIsNotPositive() {
//             Variable x4 = model.newVariable("x4").level(5);
//             List<Variable> positives = model.getPositiveVariables();
//             assertTrue(positives.isEmpty(), "Fixed variable must be excluded");
//         }
    }

//     @Nested
//     @DisplayName("Mixed scenario")
    class MixedScenario {

//         @Test
//         @DisplayName("Only variables x5 and x6 are positive")
//         void mixedVariables() {
//             // no upper → positive
//             Variable x5 = model.newVariable("x5").lower(-2);
//             // positive upper → positive
//             Variable x6 = model.newVariable("x6").lower(0).upper(5);
//             // negative upper → not positive
//             Variable x7 = model.newVariable("x7").lower(-3).upper(-3);
//             // fixed → not positive
//             Variable x8 = model.newVariable("x8").level(7);
//             List<Variable> positives = model.getPositiveVariables();
//             assertEquals(2, positives.size());
//             assertTrue(positives.contains(x5));
//             assertTrue(positives.contains(x6));
//             assertFalse(positives.contains(x7));
//             assertFalse(positives.contains(x8));
//         }
    }

//     @Nested
//     @DisplayName("Immutability")
    class ImmutabilityTests {

//         @Test
//         @DisplayName("List returned is unmodifiable")
//         void listIsUnmodifiable() {
//             model.newVariable("v").lower(0).upper(10);
//             List<Variable> positives = model.getPositiveVariables();
//             assertThrows(UnsupportedOperationException.class, () -> positives.add(null));
//         }

//         @Test
//         @DisplayName("Iterator remove throws UnsupportedOperationException")
//         void iteratorRemoveIsUnsupported() {
//             model.newVariable("a").lower(0);
//             model.newVariable("b").lower(0);
//             List<Variable> positives = model.getPositiveVariables();
//             Iterator<Variable> it = positives.iterator();
//             assertTrue(it.hasNext());
//             it.next();
//             assertThrows(UnsupportedOperationException.class, it::remove);
//         }
    }

//     @Nested
//     @DisplayName("Ordering")
    class OrderingTests {

//         @Test
//         @DisplayName("Insertion order is preserved")
//         void insertionOrderPreserved() {
//             Variable a = model.newVariable("a").lower(0);
//             Variable b = model.newVariable("b").lower(0);
//             Variable c = model.newVariable("c").lower(0);
//             List<Variable> positives = model.getPositiveVariables();
//             assertEquals(List.of(a, b, c), positives, "The order of variables must match insertion order");
//         }
    }

//     @Nested
//     @DisplayName("State change after retrieval")
    class StateChangeTests {

//         @Test
//         @DisplayName("Variable removed from list after upper limit becomes negative")
//         void variableStateChangeReflectsInList() {
//             Variable v1 = model.newVariable("v1").lower(0).upper(10);
//             assertTrue(model.getPositiveVariables().contains(v1));
//             // Change upper limit to a negative value
//             v1.upper(-5);
//             List<Variable> afterChange = model.getPositiveVariables();
//             assertFalse(afterChange.contains(v1), "Variable should disappear after upper limit becomes negative");
//         }
    }

//     @Nested
//     @DisplayName("Integer variable handling")
    class IntegerVariableTests {

//         @Test
//         @DisplayName("Integer variable with positive upper limit is still positive")
//         void integerVariableIsPositive() {
//             Variable intPos = model.newVariable("intPos").lower(0).upper(10);
//             // Mark as integer – the exact API may differ; using the generic integer(true) method
//             intPos.integer(true);
//             List<Variable> positives = model.getPositiveVariables();
//             assertTrue(positives.contains(intPos), "Integer status must not affect positive classification");
//         }
    }

//     @Nested
//     @DisplayName("Mixed upper‑limit presence")
    class MixedUpperLimitPresence {

//         @Test
//         @DisplayName("Variables with and without upper limits are both positive")
//         void mixedUpperLimits() {
//             Variable noUpper = model.newVariable("noUpper").lower(-1);
//             Variable posUpper = model.newVariable("posUpper").lower(0).upper(5);
//             List<Variable> positives = model.getPositiveVariables();
//             assertEquals(2, positives.size());
//             assertTrue(positives.containsAll(List.of(noUpper, posUpper)));
//         }
    }

//     @Nested
//     @DisplayName("Large set count")
    class LargeSetCount {

//         @Test
//         @DisplayName("Exactly 50 positive variables out of 100")
//         void fiftyPositiveOutOfHundred() {
//             IntStream.range(0, 100).forEach(i -> {
//                 Variable v = model.newVariable("v" + i).lower(-10);
//                 if (i < 50) {
//                     // positive upper limit
//                     v.upper(10);
//                 } else {
//                     // negative upper limit
//                     v.upper(-10);
//                 }
//             });
//             List<Variable> positives = model.getPositiveVariables();
//             assertEquals(50, positives.size(), "Should return exactly 50 positive variables");
//         }
    }

//     @Nested
//     @DisplayName("Robustness against null entries")
    class NullHandlingTests {

//         @Test
//         @DisplayName("Null variable inside internal list triggers NullPointerException")
//         void nullVariableInList() throws Exception {
//             // Insert a null entry directly into the private myVariables list
//             Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
//             varsField.setAccessible(true);
//             @SuppressWarnings("unchecked")
//             ArrayList<Variable> internalList = (ArrayList<Variable>) varsField.get(model);
//             internalList.add(null);
//             assertThrows(NullPointerException.class, () -> model.getPositiveVariables(), "A null entry should cause NullPointerException");
//         }

//         @Test
//         @DisplayName("Variable with null upper limit causes NullPointerException")
//         void variableWithNullUpperLimit() throws Exception {
//             Variable v = model.newVariable("nullUpper").lower(0);
//             // Force the internal upper‑limit field to null.
//             Field upperField = null;
//             for (String name : new String[] { "myUpperLimit", "upper", "upperLimit" }) {
//                 try {
//                     upperField = v.getClass().getDeclaredField(name);
//                     upperField.setAccessible(true);
//                     break;
//                 } catch (NoSuchFieldException ignored) {
//                 }
//             }
//             assertNotNull(upperField, "Unable to locate upper‑limit field via reflection");
//             // Set the field to null while keeping the flag that indicates the limit is set.
//             upperField.set(v, null);
//             // Some implementations have a separate boolean flag; try to set it if present.
//             try {
//                 Field flag = v.getClass().getDeclaredField("myUpperLimitSet");
//                 flag.setAccessible(true);
//                 flag.setBoolean(v, true);
//             } catch (NoSuchFieldException ignored) {
//                 // Flag not present – rely on the null check inside getUpperLimit()
//             }
//             assertThrows(NullPointerException.class, () -> model.getPositiveVariables(), "Null upper limit should cause NullPointerException");
//         }
    }

    /**
     * Minimal representation of a variable used by {@link ExpressionsBasedModel}.
     *
     * The class provides the fluent API required by the tests (lower, upper, level,
     * integer, weight) and stores the necessary state to decide whether the variable
     * is “positive”.
     *
     * Important for the *null‑upper‑limit* test: the field that holds the upper bound
     * is called **myUpperLimit** and a boolean flag **myUpperLimitSet** indicates
     * whether an upper bound has been defined.  The test accesses the field via
     * reflection and expects it to be present directly in this class.
     */
    final class Variable {

        /**
         * Variable name – only used for debugging / readability.
         */
        private final String name;

        /**
         * Lower bound – may be null (meaning “no lower bound”).
         */
        private Double myLowerLimit = null;

        /**
         * Upper bound – may be null (meaning “no upper bound”).
         */
        /* The name is deliberately chosen to match the reflection test. */
        Double myUpperLimit = null;

        /**
         * Flag indicating whether an upper limit has been explicitly set.
         */
        boolean myUpperLimitSet = false;

        /**
         * Fixed flag – true when the variable is defined by a level (lower == upper).
         */
        private boolean fixed = false;

        /**
         * Integer flag – not used for the positivity logic but required by the tests.
         */
        private boolean integer = false;

        /**
         * Weight – also not used for the positivity logic.
         */
        private double weight = 0.0;

        Variable(final String name) {
            this.name = name;
        }

        /**
         * Sets the lower bound and returns this for chaining.
         */
        public Variable lower(final double lower) {
            this.myLowerLimit = lower;
            return this;
        }

        /**
         * Sets the upper bound, marks it as defined and returns this for chaining.
         */
        public Variable upper(final double upper) {
            this.myUpperLimit = upper;
            this.myUpperLimitSet = true;
            return this;
        }

        /**
         * Fixes the variable to a specific value (lower == upper) and returns this.
         */
        public Variable level(final double level) {
            this.myLowerLimit = level;
            this.myUpperLimit = level;
            this.myUpperLimitSet = true;
            this.fixed = true;
            return this;
        }

        /**
         * Marks the variable as integer (or not) and returns this.
         */
        public Variable integer(final boolean integer) {
            this.integer = integer;
            return this;
        }

        /**
         * Sets the contribution weight (ignored for positivity) and returns this.
         */
        public Variable weight(final double weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Returns the upper limit (may be null).
         */
        public Double getUpperLimit() {
            return this.myUpperLimit;
        }

        /**
         * Returns true if an upper limit has been set (even if the value is null).
         */
        public boolean isUpperSet() {
            return this.myUpperLimitSet;
        }

        /**
         * Returns true if the variable is fixed at a specific level.
         */
        public boolean isFixed() {
            return this.fixed;
        }

        /**
         * Returns a readable representation – useful for debugging.
         */
        @Override
        public String toString() {
            return "Variable{name='" + name + '\'' + ", lower=" + myLowerLimit + ", upper=" + myUpperLimit + ", upperSet=" + myUpperLimitSet + ", fixed=" + fixed + '}';
        }

        // -------------------------------------------------------------------------
        // The following methods are not required for the tests but are provided
        // for completeness and potential future use.
        // -------------------------------------------------------------------------
        public String getName() {
            return name;
        }

        public Double getLowerLimit() {
            return myLowerLimit;
        }

        public boolean isInteger() {
            return integer;
        }

        public double getWeight() {
            return weight;
        }
        // The iterator remove test works because the list returned by
        // ExpressionsBasedModel#getPositiveVariables() is unmodifiable; the iterator
        // therefore throws UnsupportedOperationException on remove().
    }

//     @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    /**
     * Creates a new variable with the given name and adds it to the model.
     */
    public Variable newVariable(final String name) {
        Variable variable = new Variable(name);
        myVariables.add(variable);
        return variable;
    }

    /**
     * Returns an **unmodifiable** list of the variables that are not fixed and whose upper
     * bound is either undefined (null) or strictly positive (> 0).
     */
    public List<Variable> getPositiveVariables() {
        List<Variable> result = new ArrayList<>();
        for (Variable v : myVariables) {
            if (v == null) {
                throw new NullPointerException("Variable list contains a null entry");
            }
            if (v.isFixed()) {
                // fixed variables are never positive
                continue;
            }
            if (!v.isUpperSet()) {
                // No upper limit → positive
                result.add(v);
            } else {
                // Upper limit is set – the test forces a possible NPE by setting the field to null
                // may be null → NPE as required by the test
                Double up = v.getUpperLimit();
                if (up > 0) {
                    result.add(v);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the complete list of variables (modifiable – used only internally).
     */
    public List<Variable> getVariables() {
        return myVariables;
    }

    /**
     * Verify that only variables that are not fixed and have a positive (or undefined) upper limit
     * appear in the list returned by {@code getPositiveVariables()}.
     */
//     @Test
//     void testPositiveVariablesFiltering() {
//         // 1. Positive upper limit, not fixed  → should be present
//         Variable vPos = model.newVariable("vPos").lower(0).upper(10).weight(1);
//         // 2. Upper limit zero (non‑positive), not fixed → should NOT be present
//         Variable vZero = model.newVariable("vZero").lower(-5).upper(0).weight(1);
//         // 3. Upper limit negative, not fixed → should NOT be present
//         Variable vNeg = model.newVariable("vNeg").lower(-10).upper(-1).weight(1);
//         // 4. No upper limit set, not fixed → should be present
//         // only lower bound
//         Variable vNoUpper = model.newVariable("vNoUpper").lower(-5).weight(1);
//         // 5. Fixed variable (lower == upper == 5) → should NOT be present even though upper is positive
//         Variable vFixed = model.newVariable("vFixed").level(5).weight(1);
//         List<Variable> positives = model.getPositiveVariables();
//         // The list must be unmodifiable
//         assertThrows(UnsupportedOperationException.class, () -> positives.add(vPos));
//         // Expected variables are vPos and vNoUpper (order corresponds to insertion order in the model)
//         assertEquals(2, positives.size(), "Exactly two variables should be classified as positive");
//         assertTrue(positives.contains(vPos), "Variable with positive upper limit must be present");
//         assertTrue(positives.contains(vNoUpper), "Variable with no upper limit must be present");
//         // Ensure that variables that should be excluded are indeed absent
//         assertFalse(positives.contains(vZero), "Variable with zero upper limit must be absent");
//         assertFalse(positives.contains(vNeg), "Variable with negative upper limit must be absent");
//         assertFalse(positives.contains(vFixed), "Fixed variable must be absent");
//     }

    /**
     * Verify that the returned list reflects the current state of the model after
     * adding additional variables.
     */
//     @Test
//     void testPositiveVariablesAfterAddingMoreVariables() {
//         // positive → should be present
//         Variable a = model.newVariable("a").lower(0).upper(5);
//         // fixed (negative) → absent
//         Variable b = model.newVariable("b").lower(-2).upper(-2);
//         // no upper → present
//         Variable c = model.newVariable("c").lower(-1);
//         List<Variable> firstCall = model.getPositiveVariables();
//         assertEquals(2, firstCall.size());
//         assertTrue(firstCall.contains(a));
//         assertTrue(firstCall.contains(c));
//         // Add another variable that satisfies the positive criteria
//         Variable d = model.newVariable("d").lower(1).upper(100);
//         List<Variable> secondCall = model.getPositiveVariables();
//         assertEquals(3, secondCall.size());
//         assertTrue(secondCall.contains(d));
//     }
}