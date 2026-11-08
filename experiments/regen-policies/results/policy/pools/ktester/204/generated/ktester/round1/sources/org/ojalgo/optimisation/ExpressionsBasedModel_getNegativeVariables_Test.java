package org.ojalgo.optimisation;

import java.util.List;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.stream.Stream;
// import org.junit.*;
// import static org.junit.Assert.*;

class ExpressionsBasedModel_getNegativeVariables_Test {

    private ExpressionsBasedModel model;

    /**
     * Helper holder for the single‑variable scenarios.
     */
    private static class SingleVarScenario {

        final String name;

        final java.util.function.Function<Variable, Variable> builder;

        final boolean expectNegative;

        SingleVarScenario(String name, java.util.function.Function<Variable, Variable> builder, boolean expectNegative) {
            this.name = name;
            this.builder = builder;
            this.expectNegative = expectNegative;
        }

        @Override
        public String toString() {
            return name;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test guarantees isolation.
        model = new ExpressionsBasedModel();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required.
        model = null;
    }

    /* -----------------------------------------------------------------
     *  Parameterised tests for the simple single‑variable scenarios.
     *  ----------------------------------------------------------------- */
    private static Stream<SingleVarScenario> singleVarScenarios() {
        return Stream.of(// single variable without limits → classified as negative
        new SingleVarScenario("no limits", v -> v.weight(1), true), // explicit negative lower limit → classified as negative
        new SingleVarScenario("negative lower", v -> v.lower(-5).weight(1), true), // zero lower limit → NOT classified as negative
        new SingleVarScenario("zero lower", v -> v.lower(0).weight(1), false), // positive lower limit → NOT classified as negative
        new SingleVarScenario("positive lower", v -> v.lower(3).weight(1), false), // fixed variable with negative lower bound → NOT classified as negative
        new SingleVarScenario("fixed negative", v -> v.lower(-3).upper(-3).weight(1), false));
    }

    /**
     * -----------------------------------------------------------------
     *  Test 1 – an empty model must return an empty (but unmodifiable) list.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void testGetNegativeVariables_EmptyModel() {
//         List<Variable> negatives = model.getNegativeVariables();
//         assertNotNull("The returned list must never be null", negatives);
//         assertTrue("A model without variables should have no negative variables", negatives.isEmpty());
//         // The list must be unmodifiable – any attempt to change it should fail.
//         try {
//             negatives.add(null);
//             fail("Expected UnsupportedOperationException when adding to the list");
//         } catch (UnsupportedOperationException expected) {
//             // ok
//         }
//     }

    /**
     * -----------------------------------------------------------------
     *  Test 2 – verify that variables with a negative lower bound or with
     *           no lower bound are reported, while others are not.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void testGetNegativeVariables_VariousScenarios() {
//         // Variable with an explicit negative lower limit → should be reported.
//         Variable vNeg = model.addVariable().lower(-5).weight(1);
//         // Variable with a non‑negative lower limit → must NOT be reported.
//         Variable vNonNeg = model.addVariable().lower(0).weight(1);
//         // Variable without any lower limit set → should be reported.
//         Variable vNoLower = model.addVariable().weight(1);
//         // Fixed variable (lower == upper). Even if the lower bound is negative,
//         // it is *fixed* and therefore excluded from the negative list.
//         Variable vFixedNeg = model.addVariable().lower(-3).upper(-3).weight(1);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals("Exactly two variables should be classified as negative", 2, negatives.size());
//         assertTrue("Variable with negative lower bound must be present", negatives.contains(vNeg));
//         assertTrue("Variable without a lower bound must be present", negatives.contains(vNoLower));
//         assertFalse("Variable with non‑negative lower bound must not be present", negatives.contains(vNonNeg));
//         assertFalse("Fixed variable must not be present even if its lower bound is negative", negatives.contains(vFixedNeg));
//     }

    /**
     * -----------------------------------------------------------------
     *  Test 3 – the list returned by getNegativeVariables must be
     *           unmodifiable (again, but with a concrete element).
     *  -----------------------------------------------------------------
     */
//     @Test
//     void testGetNegativeVariables_UnmodifiableList() {
//         Variable v = model.addVariable().lower(-1).weight(1);
//         List<Variable> negatives = model.getNegativeVariables();
//         try {
//             negatives.remove(v);
//             fail("Expected UnsupportedOperationException when removing from the list");
//         } catch (UnsupportedOperationException expected) {
//             // ok
//         }
//         try {
//             negatives.clear();
//             fail("Expected UnsupportedOperationException when clearing the list");
//         } catch (UnsupportedOperationException expected) {
//             // ok
//         }
//     }

//     @ParameterizedTest(name = "Single variable scenario: {0}")
//     @MethodSource("singleVarScenarios")
//     void testGetNegativeVariables_SingleVariableScenarios(SingleVarScenario scenario) {
//         singleVarScenarios().forEach(scenario -> {
//             // Build the variable according to the scenario lambda
//             Variable var = scenario.builder.apply(model.addVariable());
//             List<Variable> negatives = model.getNegativeVariables();
//             if (scenario.expectNegative) {
//                 assertEquals("Expected exactly one negative variable for scenario: " + scenario.name, 1, negatives.size());
//                 assertTrue("Variable should be present in negative list for scenario: " + scenario.name, negatives.contains(var));
//             } else {
//                 assertTrue("Expected no negative variables for scenario: " + scenario.name, negatives.isEmpty());
//             }
//             // Reset model for the next scenario
//             model = new ExpressionsBasedModel();
//         });
//     }

    /* -----------------------------------------------------------------
     *  Mixed variables – order preservation.
     *  ----------------------------------------------------------------- */
//     @Test
//     void testGetNegativeVariables_MixedOrderPreservation() {
//         // vA – no limits (negative)
//         Variable vA = model.addVariable().weight(1);
//         // vB – lower = 2 (non‑negative)
//         Variable vB = model.addVariable().lower(2).weight(1);
//         // vC – lower = -3 (negative)
//         Variable vC = model.addVariable().lower(-3).weight(1);
//         // vD – fixed variable (ignored)
//         Variable vD = model.addVariable().lower(-1).upper(-1).weight(1);
//         // vE – no lower limit (negative)
//         Variable vE = model.addVariable().weight(1);
//         List<Variable> negatives = model.getNegativeVariables();
//         // Expected order: vA, vC, vE (the order they were added)
//         List<Variable> expected = List.of(vA, vC, vE);
//         assertEquals("Negative variables must be returned in the order of appearance", expected, negatives);
//     }

    /* -----------------------------------------------------------------
     *  Dynamic update after adding a new negative variable.
     *  ----------------------------------------------------------------- */
//     @Test
//     void testGetNegativeVariables_DynamicUpdate() {
//         // First variable – no limits (negative)
//         Variable v1 = model.addVariable().weight(1);
//         List<Variable> firstCall = model.getNegativeVariables();
//         assertEquals("First call should contain only v1", List.of(v1), firstCall);
//         // Second variable – explicit negative lower bound
//         Variable v2 = model.addVariable().lower(-4).weight(1);
//         List<Variable> secondCall = model.getNegativeVariables();
//         // The list must be freshly recomputed and contain both variables
//         assertEquals("Second call must reflect both v1 and v2 in insertion order", List.of(v1, v2), secondCall);
//     }

    /* -----------------------------------------------------------------
     *  Inject a null entry into the internal variable list and expect NPE.
     *  ----------------------------------------------------------------- */
//     @Test
//     void testGetNegativeVariables_NullVariableCausesNPE() throws Exception {
//         // Add a normal variable first so that the internal list is initialised.
//         model.addVariable().weight(1);
//         // Use reflection to obtain the private field `myVariables`.
//         Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
//         varField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         ArrayList<Variable> internalList = (ArrayList<Variable>) varField.get(model);
//         // Insert a null entry at the end of the list.
//         internalList.add(null);
//         // The categorisation logic does not guard against nulls, so a NPE is expected.
//         try {
//             model.getNegativeVariables();
//             fail("Expected NullPointerException because of a null variable in the internal list");
//         } catch (NullPointerException expected) {
//             // ok
//         }
//     }
}