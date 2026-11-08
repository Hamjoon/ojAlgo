package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
// import java.util.concurrent.ConcurrentModificationException;
// import java.util.concurrent.ConcurrentModificationException;
// import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_constraints_Test {

    private ExpressionsBasedModel model;

    //     @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test case.
        model = new ExpressionsBasedModel();
    }

    /**
     * Verify that an expression with a bound is reported as a constraint,
     * while an expression that only contributes to the objective is not.
     */
    @Test
    void testOnlyConstraintExpressionsAreReturned() {
        // --- a regular constraint -------------------------------------------------
        Expression c1 = model.addExpression("c1");
        // makes it a constraint
        c1.lower(0).upper(10);
        // not part of the objective
        c1.weight(0);
        // --- an objective‑only expression -----------------------------------------
        Expression obj = model.addExpression("obj");
        // contributes to the objective
        obj.weight(1);
        // no lower/upper → not a constraint
        // --- a constraint that is also part of the objective -----------------------
        Expression c2 = model.addExpression("c2");
        // equality constraint
        c2.lower(5).upper(5);
        // also contributes to the objective (still a constraint)
        c2.weight(2);
        // Collect the names of the expressions returned by constraints()
        List<String> constraintNames = model.constraints().map(Expression::getName).collect(Collectors.toList());
        // Expected: c1 and c2 (obj must be absent)
        assertTrue(constraintNames.contains("c1"), "c1 should be present as a constraint");
        assertTrue(constraintNames.contains("c2"), "c2 should be present as a constraint");
        assertFalse(constraintNames.contains("obj"), "obj must not be reported as a constraint");
        assertEquals(2, constraintNames.size(), "Exactly two constraints should be returned");
    }

    /**
     * Verify that when the model contains no constraints the stream is empty.
     */
    @Test
    void testEmptyWhenNoConstraints() {
        // Only objective expressions, no bounds.
        model.addExpression("obj1").weight(1);
        model.addExpression("obj2").weight(2);
        List<Expression> constraints = model.constraints().collect(Collectors.toList());
        assertTrue(constraints.isEmpty(), "constraints() should return an empty stream when there are no constraints");
    }

    /**
     * Verify that a redundant constraint (explicitly marked as redundant) is filtered out.
     */
    @Test
    void testRedundantConstraintsAreFilteredOut() {
        // Normal constraint
        Expression normal = model.addExpression("normal");
        normal.lower(0).upper(5);
        // Redundant constraint – we mark it as redundant via the public API.
        Expression redundant = model.addExpression("redundant");
        redundant.lower(1).upper(1);
        // public method that flags the expression as redundant
        redundant.setRedundant(true);
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.contains("normal"), "Normal constraint must be present");
        assertFalse(names.contains("redundant"), "Redundant constraint must be filtered out");
        assertEquals(1, names.size(), "Only the non‑redundant constraint should be returned");
    }

    @Test
    void testBasicSingleConstraint() {
        // Single non‑redundant constraint
        Expression expr1 = model.addExpression("expr1");
        // makes it a constraint
        expr1.lower(0).upper(10);
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("expr1"), names, "A single non‑redundant constraint must be returned");
    }

    @Test
    void testBasicRedundantConstraintFiltered() {
        Expression expr = model.addExpression("expr");
        expr.lower(0).upper(5);
        // mark as redundant
        expr.setRedundant(true);
        List<Expression> constraints = model.constraints().collect(Collectors.toList());
        assertTrue(constraints.isEmpty(), "Redundant constraint must be filtered out, leaving an empty stream");
    }

    @Test
    void testBasicNonConstraintNotReturned() {
        // Expression without any bound – not a constraint
        Expression expr = model.addExpression("expr");
        // only contributes to objective
        expr.weight(1);
        List<Expression> constraints = model.constraints().collect(Collectors.toList());
        assertTrue(constraints.isEmpty(), "Expression that is not a constraint must not appear in the stream");
    }

    @Test
    void testMixedExpressionsFiltering() {
        // exprA – constraint, not redundant
        Expression exprA = model.addExpression("exprA");
        exprA.lower(0).upper(5);
        // exprB – constraint, redundant
        Expression exprB = model.addExpression("exprB");
        exprB.lower(1).upper(1);
        exprB.setRedundant(true);
        // exprC – constraint, not redundant
        Expression exprC = model.addExpression("exprC");
        exprC.lower(-10).upper(10);
        // exprD – not a constraint (objective only)
        Expression exprD = model.addExpression("exprD");
        exprD.weight(2);
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.containsAll(List.of("exprA", "exprC")), "Only non‑redundant constraints should be present");
        assertFalse(names.contains("exprB"), "Redundant constraint must be filtered out");
        assertFalse(names.contains("exprD"), "Non‑constraint must not be present");
        assertEquals(2, names.size(), "Exactly two expressions should be returned");
    }

    @Test
    void testEmptyModelReturnsEmptyStream() {
        List<Expression> constraints = model.constraints().collect(Collectors.toList());
        assertTrue(constraints.isEmpty(), "An empty model must yield an empty constraints stream");
    }

    @Test
    void testAddConstraintThenMarkRedundant() {
        // Add a constraint
        Expression c1 = model.addExpression("c1");
        c1.lower(0).upper(10);
        assertEquals(List.of("c1"), model.constraints().map(Expression::getName).collect(Collectors.toList()), "c1 should be present after creation");
        // Mark it redundant
        c1.setRedundant(true);
        List<Expression> after = model.constraints().collect(Collectors.toList());
        assertTrue(after.isEmpty(), "After marking redundant, constraints() must be empty");
    }

    @Test
    void testCopyRetainsConstraintsIndependently() {
        // Original model with two constraints
        Expression origA = model.addExpression("origA");
        origA.lower(0).upper(5);
        Expression origB = model.addExpression("origB");
        origB.lower(1).upper(3);
        // Deep copy
        ExpressionsBasedModel copy = model.copy();
        // Verify copy contains both constraints
        List<String> copyNames = copy.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(copyNames.containsAll(List.of("origA", "origB")), "Copy must contain both original constraints");
        // Change original after copy
        origB.setRedundant(true);
        // Original now has only origA
        List<String> originalNames = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("origA"), originalNames, "Original model should reflect the redundancy change");
        // Copy must stay unchanged
        List<String> copyNamesAfter = copy.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(copyNamesAfter.containsAll(List.of("origA", "origB")), "Copy must remain unchanged after original is modified");
    }

    @Test
    void testExpressionBecomesConstraintAfterCreation() {
        // Initially not a constraint
        Expression e1 = model.addExpression("e1");
        assertTrue(model.constraints().collect(Collectors.toList()).isEmpty(), "Initially, e1 is not a constraint");
        // Turn it into a constraint by adding bounds
        e1.lower(0).upper(5);
        List<String> names = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("e1"), names, "After adding bounds, e1 must appear as a constraint");
    }

    @Test
    void testLargeModelExactlyFiftyConstraints() {
        final int TOTAL = 100;
        for (int i = 0; i < TOTAL; i++) {
            Expression expr = model.addExpression("expr" + i);
            if (i % 2 == 0) {
                // even index → non‑redundant constraint
                expr.lower(0).upper(10);
            } else {
                // odd index → either non‑constraint or redundant constraint
                if (i % 4 == 1) {
                    // non‑constraint (no bounds)
                } else {
                    // redundant constraint
                    expr.lower(1).upper(1);
                    expr.setRedundant(true);
                }
            }
        }
        long count = model.constraints().count();
        assertEquals(50L, count, "Exactly 50 non‑redundant constraints should be present in a 100‑expression model");
    }

    @Test
    void testLiveViewReflectsNewConstraints() {
        // First constraint
        model.addExpression("first").lower(0).upper(5);
        // Capture current list
        List<String> firstCapture = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("first"), firstCapture, "Initial capture must contain only 'first'");
        // Add second constraint
        model.addExpression("second").lower(1).upper(3);
        // Re‑evaluate
        List<String> secondCapture = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(secondCapture.containsAll(List.of("first", "second")), "After addition, both constraints must be present");
        assertEquals(2, secondCapture.size(), "Exactly two constraints should be returned after the second addition");
    }

    @Test
    void testRemovalOfExpressionUpdatesConstraints() {
        model.addExpression("keep").lower(0).upper(10);
        model.addExpression("remove").lower(5).upper(15);
        // Ensure both are present initially
        List<String> before = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertTrue(before.containsAll(List.of("keep", "remove")));
        // Remove one
        model.removeExpression("remove");
        List<String> after = model.constraints().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("keep"), after, "After removal, only 'keep' must remain");
    }

    @Test
    void testNullExpressionEntryThrowsNPE() throws Exception {
        // Insert a null value into the private myExpressions map via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        map.put("nullExpr", null);
        // The stream should attempt to call isConstraint() on the null entry → NPE
        assertThrows(NullPointerException.class, () -> model.constraints().count(), "A null entry in the expressions map must cause NullPointerException");
    }

    @Test
    void testConcurrentModificationDuringStream() {
        // Add a couple of constraints first
        model.addExpression("c0").lower(0).upper(1);
        model.addExpression("c1").lower(2).upper(3);
        java.util.stream.Stream<Expression> stream = model.constraints();
        // While consuming the stream, add a new expression – should trigger CME
        assertThrows(ConcurrentModificationException.class, () -> stream.forEach(expr -> {
            // Mutate the underlying map during traversal
            model.addExpression("new").lower(0).upper(5);
        }), "Modifying the model while the stream is being consumed must throw ConcurrentModificationException");
    }
}