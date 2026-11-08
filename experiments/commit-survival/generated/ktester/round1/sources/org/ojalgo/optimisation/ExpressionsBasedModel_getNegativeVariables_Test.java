package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

class ExpressionsBasedModel_getNegativeVariables_Test {

//     private static class Variable extends org.ojalgo.optimisation.Variable {

        // Expose a protected constructor via subclassing for test‑only usage
        Variable() {
//             super();
        }
    }

    @Nested
    @DisplayName("Unmodifiable list operation checks")
    class UnmodifiableListTests {

        private List<Variable> negatives;

        // needed to create dummy variables in the tests
        private ExpressionsBasedModel model;

        @BeforeEach
        void init() {
            model = new ExpressionsBasedModel();
            model.newVariable("neg").lower(-1).upper(5);
//             negatives = model.getNegativeVariables();
        }

//         @Test
//         void testAdd() {
//             assertThrows(UnsupportedOperationException.class, () -> negatives.add(model.newVariable("tmp")));
//         }

        @Test
        void testRemove() {
            assertThrows(UnsupportedOperationException.class, () -> negatives.remove(0));
        }

        @Test
        void testClear() {
            assertThrows(UnsupportedOperationException.class, negatives::clear);
        }

//         @Test
//         void testSet() {
//             assertThrows(UnsupportedOperationException.class, () -> negatives.set(0, model.newVariable("tmp")));
//         }

//         @Test
//         void testAddAll() {
//             assertThrows(UnsupportedOperationException.class, () -> negatives.addAll(Collections.singletonList(model.newVariable("tmp"))));
//         }

        @Test
        void testRemoveAll() {
            assertThrows(UnsupportedOperationException.class, () -> negatives.removeAll(Collections.singletonList(negatives.get(0))));
        }

        @Test
        void testRetainAll() {
            assertThrows(UnsupportedOperationException.class, () -> negatives.retainAll(Collections.emptyList()));
        }

        @Test
        void testIteratorRemove() {
            Iterator<Variable> it = negatives.iterator();
            assertTrue(it.hasNext());
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

//         @Test
//         void testListIteratorAdd() {
//             ListIterator<Variable> lit = negatives.listIterator();
//             assertThrows(UnsupportedOperationException.class, () -> lit.add(model.newVariable("tmp")));
//         }

//         @Test
//         void testListIteratorSet() {
//             ListIterator<Variable> lit = negatives.listIterator();
//             assertTrue(lit.hasNext());
//             lit.next();
//             assertThrows(UnsupportedOperationException.class, () -> lit.set(model.newVariable("tmp")));
//         }
    }

    /**
     * Basic scenario: a mix of variables with negative, zero and positive lower bounds.
     * Only the variables with a negative lower bound (or without a lower bound) must be
     * present in the returned list.
     */
//     @Test
//     void testGetNegativeVariablesBasic() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Variable with a negative lower bound – should be classified as negative
//         Variable vNeg1 = model.newVariable("vNeg1").lower(-5).upper(10);
//         // Variable with a non‑negative lower bound – must NOT be classified as negative
//         Variable vPos = model.newVariable("vPos").lower(0).upper(5);
//         // Variable with a negative lower bound and a negative upper bound – still negative
//         Variable vNeg2 = model.newVariable("vNeg2").lower(-2).upper(-1);
//         List<Variable> negatives = model.getNegativeVariables();
//         // Expected size and content
//         assertEquals(2, negatives.size(), "Exactly two variables should be reported as negative");
//         assertTrue(negatives.contains(vNeg1), "vNeg1 should be in the negative list");
//         assertTrue(negatives.contains(vNeg2), "vNeg2 should be in the negative list");
//         assertFalse(negatives.contains(vPos), "vPos must NOT be in the negative list");
//         // The returned list must be unmodifiable
//         assertThrows(UnsupportedOperationException.class, () -> negatives.add(vPos));
//     }

    /**
     * Fixed variables (lower == upper) must never be reported as negative,
     * even if the (common) bound is negative.
     */
//     @Test
//     void testFixedVariableIsNotReturned() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Fixed variable with a negative bound – should be ignored
//         Variable vFixedNeg = model.newVariable("vFixedNeg").lower(-3).upper(-3);
//         // Normal variable with a negative lower bound – should be reported
//         Variable vNeg = model.newVariable("vNeg").lower(-1).upper(4);
//         // Normal variable with a non‑negative lower bound – should not be reported
//         Variable vNonNeg = model.newVariable("vNonNeg").lower(0).upper(4);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals(1, negatives.size(), "Only the non‑fixed negative variable should be returned");
//         assertTrue(negatives.contains(vNeg), "vNeg must be present");
//         assertFalse(negatives.contains(vFixedNeg), "Fixed variable must NOT be present");
//         assertFalse(negatives.contains(vNonNeg), "Non‑negative variable must NOT be present");
//     }

    /**
     * When no variable fulfills the negative‑lower‑limit condition, the method
     * should return an empty, but still unmodifiable, list.
     */
//     @Test
//     void testNoNegativeVariables() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("a").lower(0).upper(10);
//         model.newVariable("b").lower(1).upper(5);
//         model.newVariable("c").lower(2).upper(8);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertTrue(negatives.isEmpty(), "The list should be empty when no variable is negative");
//         assertThrows(UnsupportedOperationException.class, () -> negatives.add(model.newVariable("tmp")));
//     }

//     @Test
//     @DisplayName("Empty model → empty unmodifiable list")
//     void testEmptyModel() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         List<Variable> negatives = model.getNegativeVariables();
//         assertTrue(negatives.isEmpty(), "Empty model must yield an empty list");
//         assertThrows(UnsupportedOperationException.class, () -> negatives.add(model.newVariable("tmp")));
//     }

//     @Test
//     @DisplayName("Variable not fixed, lower limit not set → appears in list")
//     void testVariableWithoutLowerLimit() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // no lower/upper limits set
//         Variable v1 = model.newVariable("v1");
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals(1, negatives.size(), "Variable without lower limit must be reported");
//         assertTrue(negatives.contains(v1));
//     }

//     @Test
//     @DisplayName("Variable not fixed, lower limit set to positive → not returned")
//     void testVariablePositiveLowerLimit() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("vPos").lower(5).upper(10);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertTrue(negatives.isEmpty(), "Positive lower limit must exclude variable");
//     }

//     @Test
//     @DisplayName("Variable not fixed, lower limit set to negative → returned")
//     void testVariableNegativeLowerLimit() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v2 = model.newVariable("v2").lower(-3).upper(7);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals(1, negatives.size(), "Negative lower limit must include variable");
//         assertTrue(negatives.contains(v2));
//     }

//     @Test
//     @DisplayName("Fixed variable (any limits) → never returned")
//     void testFixedVariableNeverReturned() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Fixed by giving identical lower and upper bounds (negative)
//         model.newVariable("fixedNeg").lower(-4).upper(-4);
//         // Fixed by giving identical lower and upper bounds (positive)
//         model.newVariable("fixedPos").lower(2).upper(2);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertTrue(negatives.isEmpty(), "Fixed variables must never appear");
//     }

//     @Test
//     @DisplayName("Mixed variables → only qualifying ones returned")
//     void testMixedVariables() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // qualifies
//         Variable v1 = model.newVariable("v1").lower(-2).upper(5);
//         // fixed → excluded
//         Variable v2 = model.newVariable("v2").lower(-1).upper(-1);
//         // qualifies
//         Variable v3 = model.newVariable("v3").lower(-5).upper(0);
//         // positive lower → excluded
//         Variable v4 = model.newVariable("v4").lower(3).upper(10);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals(2, negatives.size(), "Only v1 and v3 should be present");
//         assertTrue(negatives.contains(v1));
//         assertTrue(negatives.contains(v3));
//         assertFalse(negatives.contains(v2));
//         assertFalse(negatives.contains(v4));
//     }

//     @Test
//     @DisplayName("Lower limit set to zero → not considered negative")
//     void testLowerLimitZero() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("zero").lower(0).upper(5);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertTrue(negatives.isEmpty(), "Zero lower limit must not be classified as negative");
//     }

//     @Test
//     @DisplayName("Lower limit set to negative zero (BigDecimal \"-0\") → not negative")
//     void testNegativeZeroLowerLimit() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Using BigDecimal to explicitly create "-0"
//         Variable v = model.newVariable("negZero").lower(new BigDecimal("-0")).upper(5);
//         List<Variable> negatives = model.getNegativeVariables();
//         // signum of "-0" is 0, therefore it must be excluded
//         assertTrue(negatives.isEmpty(), "Variable with lower limit \"-0\" (signum == 0) must not be returned");
//     }

//     @Test
//     @DisplayName("Integer variable with negative lower limit → returned")
//     void testIntegerVariableNegativeLower() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable vInt = model.newVariable("vInt").integer(true).lower(-7).upper(3);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals(1, negatives.size(), "Integer variable with negative lower must be included");
//         assertTrue(negatives.contains(vInt));
//     }

//     @Test
//     @DisplayName("Variable becomes fixed after initial inclusion")
//     void testVariableBecomesFixedAfterInclusion() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable vTmp = model.newVariable("vTmp").lower(-4).upper(8);
//         List<Variable> first = model.getNegativeVariables();
//         assertEquals(1, first.size(), "Initially vTmp should be reported");
//         // Fix the variable by setting identical bounds
//         vTmp.lower(-4).upper(-4);
//         List<Variable> second = model.getNegativeVariables();
//         assertTrue(second.isEmpty(), "After fixing, the variable must disappear");
//     }

//     @Test
//     @DisplayName("Order of variables preserved")
//     void testOrderPreserved() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable vA = model.newVariable("vA").lower(-1).upper(5);
//         // no lower → qualifies
//         Variable vB = model.newVariable("vB");
//         Variable vC = model.newVariable("vC").lower(-3).upper(2);
//         // Add a non‑qualifying variable to ensure it does not affect order
//         model.newVariable("vD").lower(2).upper(10);
//         List<Variable> negatives = model.getNegativeVariables();
//         assertEquals(3, negatives.size(), "Three variables should be returned");
//         assertEquals(List.of(vA, vB, vC), negatives, "Variables must appear in the same order as they were added");
//     }

//     @Test
//     @DisplayName("Dynamic update after adding a new qualifying variable")
//     void testDynamicUpdateAfterAddingVariable() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v1 = model.newVariable("v1").lower(-2).upper(4);
//         List<Variable> first = model.getNegativeVariables();
//         assertEquals(1, first.size());
//         assertTrue(first.contains(v1));
//         Variable v2 = model.newVariable("v2").lower(-5).upper(3);
//         List<Variable> second = model.getNegativeVariables();
//         assertEquals(2, second.size());
//         assertTrue(second.contains(v1));
//         assertTrue(second.contains(v2));
//     }

//     @Test
//     @DisplayName("Multiple invocations return unmodifiable lists")
//     void testMultipleInvocationsUnmodifiable() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         model.newVariable("x").lower(-1).upper(2);
//         List<Variable> first = model.getNegativeVariables();
//         List<Variable> second = model.getNegativeVariables();
//         // Both must be unmodifiable
//         assertThrows(UnsupportedOperationException.class, () -> first.add(model.newVariable("tmp")));
//         assertThrows(UnsupportedOperationException.class, () -> second.remove(0));
//     }
}