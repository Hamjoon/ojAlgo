package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ExpressionsBasedModel_getExpressions_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test case.
        model = new ExpressionsBasedModel();
        // Two variables that will be used by the expressions.
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model – good practice for classes that implement
        // {@link Optimisation.Model#dispose()} although the current implementation
        // does not hold external resources.
        model.dispose();
    }

    @Test
    void testGetExpressionsInitiallyEmpty() {
        Collection<Expression> expressions = model.getExpressions();
        assertNotNull(expressions, "The returned collection must not be null");
        assertTrue(expressions.isEmpty(), "A newly created model should have no expressions");
    }

    @Test
    void testGetExpressionsReflectsAddedExpression() {
        // Add an expression to the model.
        Expression expr = model.newExpression("expr1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        expr.set(x, BigDecimal.ONE);
        expr.set(y, BigDecimal.ONE);
        Collection<Expression> expressions = model.getExpressions();
        // The collection should now contain exactly one element.
        assertEquals(1, expressions.size(), "Collection size should reflect the added expression");
        assertTrue(expressions.contains(expr), "The added expression must be present in the collection");
    }

    @Test
    void testGetExpressionsIsLiveView() {
        Collection<Expression> expressions = model.getExpressions();
        // Initially empty.
        assertTrue(expressions.isEmpty());
        // Add first expression.
        Expression e1 = model.newExpression("e1");
        e1.set(x, BigDecimal.ONE);
        assertEquals(1, expressions.size(), "Collection must update after first addition");
        // Add second expression.
        Expression e2 = model.newExpression("e2");
        e2.set(y, BigDecimal.ONE);
        assertEquals(2, expressions.size(), "Collection must update after second addition");
        // Verify both expressions are present.
        assertTrue(expressions.contains(e1));
        assertTrue(expressions.contains(e2));
    }

    @Test
    void testGetExpressionsIsUnmodifiable() {
        // Add an expression so that the collection is non‑empty.
        model.newExpression("immutable").set(x, BigDecimal.ONE);
        Collection<Expression> expressions = model.getExpressions();
        // Attempt to modify the collection via add – should throw.
        assertThrows(UnsupportedOperationException.class, () -> expressions.add(null), "The collection returned by getExpressions() must be unmodifiable (add)");
        // Attempt to modify via iterator.remove – should also throw.
        Iterator<Expression> it = expressions.iterator();
        if (it.hasNext()) {
            // advance to first element
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove, "The iterator of the collection must also be unmodifiable (remove)");
        }
    }

    @Test
    void testGetExpressionsAfterRemoval() {
        // Add two distinct expressions.
        Expression first = model.addExpression("first");
        first.set(x, BigDecimal.ONE);
        Expression second = model.addExpression("second");
        second.set(y, BigDecimal.ONE);
        // Verify both are present before removal.
        Collection<Expression> before = model.getExpressions();
        assertEquals(2, before.size());
        assertTrue(before.contains(first));
        assertTrue(before.contains(second));
        // Remove the first expression by name.
        model.removeExpression("first");
        // The collection must now contain only the second expression.
        Collection<Expression> after = model.getExpressions();
        assertEquals(1, after.size(), "After removal, collection size should be 1");
        assertTrue(after.contains(second), "The remaining expression must be the one not removed");
        assertFalse(after.contains(first), "The removed expression must no longer be present");
    }

    @Test
    void testGetExpressionsDuplicateNameHandling() {
        // First expression with name "dup".
        Expression firstDup = model.addExpression("dup");
        firstDup.set(x, BigDecimal.ONE);
        // Second expression with the same name – the library replaces the previous one.
        Expression secondDup = model.addExpression("dup");
        secondDup.set(y, BigDecimal.ONE);
        Collection<Expression> expressions = model.getExpressions();
        // Only one expression should be present (the later one replaces the earlier).
        assertEquals(1, expressions.size(), "Duplicate name should replace the previous expression, leaving a single entry");
        assertTrue(expressions.contains(secondDup), "The expression that was added last with the duplicate name must be present");
    }

    @Test
    void testGetExpressionsShallowCopy() {
        // Build original model with two expressions.
        Expression a = model.addExpression("a");
        a.set(x, BigDecimal.ONE);
        Expression b = model.addExpression("b");
        b.set(y, BigDecimal.ONE);
        // Perform a shallow copy.
        ExpressionsBasedModel copy = model.copy();
        try {
            Collection<Expression> originalExprs = model.getExpressions();
            Collection<Expression> copyExprs = copy.getExpressions();
            // Same number of expressions.
            assertEquals(originalExprs.size(), copyExprs.size(), "Shallow copy must expose the same number of expressions");
            // Ensure distinct object identities.
            Set<Expression> originalSet = originalExprs.stream().collect(Collectors.toSet());
            Set<Expression> copySet = copyExprs.stream().collect(Collectors.toSet());
            for (Expression orig : originalSet) {
                boolean foundSameInstance = copySet.stream().anyMatch(c -> c == orig);
                assertFalse(foundSameInstance, "Expressions in the shallow copy must be different instances from the original");
            }
        } finally {
            // Clean‑up the copy.
            copy.dispose();
        }
    }

    @Test
    void testGetExpressionsDeepCopyWithPrune() {
        // Build original model with two expressions.
        Expression a = model.addExpression("a");
        a.set(x, BigDecimal.ONE);
        // Make the expression a constraint so it is not pruned.
        a.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Expression b = model.addExpression("b");
        b.set(y, BigDecimal.ONE);
        b.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Perform a deep copy with pruning (shallow = false, prune = true).
        ExpressionsBasedModel deepCopy = model.copy(false, true);
        try {
            Collection<Expression> originalExprs = model.getExpressions();
            Collection<Expression> copyExprs = deepCopy.getExpressions();
            // Same number of expressions after pruning (both are used, so count stays 2).
            assertEquals(originalExprs.size(), copyExprs.size(), "Deep copy with prune must retain all used expressions");
            // Verify distinct object identities.
            Set<Expression> originalSet = originalExprs.stream().collect(Collectors.toSet());
            Set<Expression> copySet = copyExprs.stream().collect(Collectors.toSet());
            for (Expression orig : originalSet) {
                boolean foundSameInstance = copySet.stream().anyMatch(c -> c == orig);
                assertFalse(foundSameInstance, "Expressions in the deep copy must be different instances from the original");
            }
        } finally {
            deepCopy.dispose();
        }
    }

    @Test
    void testGetExpressionsLargeNumber() {
        // Add 100 uniquely named expressions.
        int count = 100;
        IntStream.rangeClosed(1, count).forEach(i -> {
            Expression expr = model.addExpression("expr_" + i);
            // Assign a coefficient to one of the variables to make the expression non‑empty.
            expr.set(x, BigDecimal.valueOf(i));
        });
        Collection<Expression> expressions = model.getExpressions();
        assertEquals(count, expressions.size(), "Collection size should equal the number of added expressions");
        // Verify that each expected name is present.
        for (int i = 1; i <= count; i++) {
            String expectedName = "expr_" + i;
            boolean found = expressions.stream().anyMatch(e -> expectedName.equals(e.getName()));
            assertTrue(found, "Expression with name " + expectedName + " must be present");
        }
    }
}
