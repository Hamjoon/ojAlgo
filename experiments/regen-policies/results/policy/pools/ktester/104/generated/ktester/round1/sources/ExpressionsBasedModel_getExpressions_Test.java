package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_getExpressions_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /**
     * Helper that creates three expressions (two auto‑named, one with a given name)
     * and returns them as a list for verification.
     */
    private static List<Expression> createThreeExpressions(ExpressionsBasedModel model) {
        // auto‑generated name
        Expression e1 = model.addExpression();
        // auto‑generated name
        Expression e2 = model.addExpression();
        // explicit name
        Expression e3 = model.addExpression("exprX");
        return List.of(e1, e2, e3);
    }

    /**
     * Verify that the collection returned by {@code getExpressions()} contains
     * exactly the expressions that have been added to the model.
     */
    @Test
    void testReturnedCollectionContainsAddedExpressions() {
        // Add two expressions with distinct names.
        Expression exprA = model.addExpression("exprA");
        Expression exprB = model.addExpression("exprB");
        Collection<Expression> expressions = model.getExpressions();
        // The collection should contain both expressions.
        assertEquals(2, expressions.size(), "Unexpected number of expressions");
        assertTrue(expressions.contains(exprA), "Collection does not contain exprA");
        assertTrue(expressions.contains(exprB), "Collection does not contain exprB");
    }

    /**
     * The collection must be a live view – changes to the model after obtaining the
     * collection should be reflected in the same collection instance.
     */
    @Test
    void testLiveViewReflectsSubsequentAdditions() {
        Collection<Expression> expressions = model.getExpressions();
        assertTrue(expressions.isEmpty(), "Initially the collection should be empty");
        // Add an expression after the collection has been obtained.
        Expression expr = model.addExpression("newExpr");
        // The same collection instance should now report the new size.
        assertEquals(1, expressions.size(), "Collection did not reflect newly added expression");
        assertTrue(expressions.contains(expr), "Collection does not contain the newly added expression");
    }

    /**
     * The returned collection must be unmodifiable. Any attempt to add, remove or
     * clear elements should result in {@link UnsupportedOperationException}.
     */
    @Test
    void testCollectionIsUnmodifiable() {
        model.addExpression("expr1");
        Collection<Expression> expressions = model.getExpressions();
        // Attempt to add via the collection's add method.
        assertThrows(UnsupportedOperationException.class, () -> expressions.add(model.addExpression("expr2")), "add() should throw UnsupportedOperationException");
        // Attempt to remove via iterator.
        Iterator<Expression> iterator = expressions.iterator();
        assertTrue(iterator.hasNext(), "Iterator should have at least one element");
        // advance to first element
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove, "Iterator.remove() should throw UnsupportedOperationException");
        // Attempt to clear the collection.
        assertThrows(UnsupportedOperationException.class, expressions::clear, "clear() should throw UnsupportedOperationException");
    }

    /**
     * Ensure that the collection returned is indeed a view of the internal map and
     * not a snapshot. Modifying the model (removing an expression) should be
     * reflected in the collection.
     */
    @Test
    void testRemovalIsReflectedInView() {
        Expression expr = model.addExpression("toBeRemoved");
        Collection<Expression> expressions = model.getExpressions();
        // Pre‑condition check.
        assertTrue(expressions.contains(expr), "Pre‑condition failed: view must contain the added expression");
        assertEquals(1, expressions.size(), "Pre‑condition failed: view size must be 1");
        // Remove through the model API.
        model.removeExpression("toBeRemoved");
        // The view must now be empty.
        assertFalse(expressions.contains(expr), "View should no longer contain the removed expression");
        assertEquals(0, expressions.size(), "View size must be zero after removal");
    }

    /**
     * Empty model – the collection must be empty and unmodifiable.
     */
    @Test
    void testEmptyModelReturnsEmptyUnmodifiableCollection() {
        Collection<Expression> expressions = model.getExpressions();
        // Expect an empty collection.
        assertTrue(expressions.isEmpty(), "A newly created model should have no expressions");
        assertEquals(0, expressions.size(), "Size must be zero for an empty model");
        // Verify unmodifiability.
        assertThrows(UnsupportedOperationException.class, expressions::clear, "clear() on the returned collection must throw UnsupportedOperationException");
    }

    /**
     * Model with three added expressions – the collection must contain exactly
     * those three expressions.
     */
    @Test
    void testModelWithThreeExpressionsContainsAll() {
        List<Expression> added = createThreeExpressions(model);
        Collection<Expression> expressions = model.getExpressions();
        assertEquals(3, expressions.size(), "Collection size should be 3 after adding three expressions");
        assertTrue(expressions.containsAll(added), "Collection must contain all added expressions");
    }
}
