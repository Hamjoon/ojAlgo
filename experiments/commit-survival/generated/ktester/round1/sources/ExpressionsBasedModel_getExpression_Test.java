package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_getExpression_Test {

    private ExpressionsBasedModel model;

    private Expression exprAlpha;

    private Expression exprBeta;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to guarantee isolation.
        model = new ExpressionsBasedModel();
        // Add two named expressions that will be used in several tests.
        exprAlpha = model.newExpression("alpha");
        exprBeta = model.newExpression("beta");
        // Populate the expressions with a simple coefficient so they are not empty.
        Variable v = model.newVariable("v");
        exprAlpha.set(v, 1);
        exprBeta.set(v, 2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required but keeps the pattern clear.
        model = null;
        exprAlpha = null;
        exprBeta = null;
    }

    @Test
    void testGetExistingExpressionReturnsSameInstance() {
        Expression retrievedAlpha = model.getExpression("alpha");
        Expression retrievedBeta = model.getExpression("beta");
        // The retrieved objects must be the exact same instances that were added.
        assertSame(exprAlpha, retrievedAlpha, "Expression 'alpha' should be retrieved as the same instance");
        assertSame(exprBeta, retrievedBeta, "Expression 'beta' should be retrieved as the same instance");
    }

    @Test
    void testGetNonExistingExpressionReturnsNull() {
        assertNull(model.getExpression("nonexistent"), "Requesting an unknown expression name must return null");
    }

    @Test
    void testGetExpressionWithNullNameReturnsNull() {
        assertNull(model.getExpression(null), "Requesting an expression with a null name must return null");
    }

    @Test
    void testModifyingRetrievedExpressionAffectsModel() {
        // Retrieve the expression and modify it.
        Expression retrieved = model.getExpression("alpha");
        Variable extra = model.newVariable("extra");
        retrieved.set(extra, 5);
        // The model should now contain the new term because the same instance is stored.
        Expression again = model.getExpression("alpha");
        assertEquals(5, again.get(extra).doubleValue(), 0.0, "Changes made via the retrieved expression must be reflected in the model");
    }

    @Test
    void testEmptyStringNameReturnsNull() {
        // No expression with an empty name has been added.
        assertNull(model.getExpression(""), "Calling getExpression(\"\") when no such expression exists must return null");
    }

    @Test
    void testCaseSensitivityInLookup() {
        // Add an expression with a capitalised name.
        Expression capitalX = model.newExpression("X");
        // Lookup using a different case.
        assertNull(model.getExpression("x"), "Lookup should be case‑sensitive; requesting \"x\" must return null when only \"X\" exists");
        // Ensure the original expression is still reachable with the exact key.
        assertSame(capitalX, model.getExpression("X"), "Exact key lookup must still succeed");
    }

    @Test
    void testRemovalEffectOnGetExpression() {
        // Add then remove an expression.
        Expression toRemove = model.newExpression("toRemove");
        assertSame(toRemove, model.getExpression("toRemove"), "Expression should be present immediately after creation");
        model.removeExpression("toRemove");
        assertNull(model.getExpression("toRemove"), "After removal, getExpression should return null for the same name");
    }

    @Test
    void testRetrievalAfterAddExpressionString() {
        // Use the public addExpression(String) API (different from newExpression).
        Expression added = model.addExpression("addedViaAddExpression");
        // Verify that the returned instance is stored and can be retrieved.
        assertSame(added, model.getExpression("addedViaAddExpression"), "Expression returned by addExpression(String) must be the same instance retrieved later");
    }

    @Test
    void testNonExistentNameReturnsNull() {
        // Explicitly verify a name that has never been added.
        assertNull(model.getExpression("unknown"), "Requesting a name that was never added must return null");
    }
}
