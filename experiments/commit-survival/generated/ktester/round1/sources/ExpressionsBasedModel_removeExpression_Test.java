package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_removeExpression_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates a fresh, empty model
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources that a solver might have allocated.
        // The dispose method is part of the public API.
        model.dispose();
    }

    /**
     * Verify that an expression that has been added can be removed and is no longer reachable via the public API.
     */
    @Test
    void testRemoveExistingExpression() {
        // Given: a model with a named expression
        String exprName = "myExpression";
        Expression expr = model.addExpression(exprName);
        assertNotNull(expr, "Expression should be created");
        assertNotNull(model.getExpression(exprName), "Expression must be present before removal");
        int countBefore = model.countExpressions();
        // When: the expression is removed
        model.removeExpression(exprName);
        // Then: the expression is no longer present
        assertNull(model.getExpression(exprName), "Expression should be removed");
        assertEquals(countBefore - 1, model.countExpressions(), "Expression count should decrease by one after removal");
    }

    /**
     * Removing a non‑existent expression must be a no‑op and must not throw an exception.
     */
    @Test
    void testRemoveNonExistingExpression() {
        // Given: an empty model
        assertEquals(0, model.countExpressions(), "Model should start empty");
        // When: we attempt to remove a name that was never added
        assertDoesNotThrow(() -> model.removeExpression("doesNotExist"));
        // Then: the model remains unchanged
        assertEquals(0, model.countExpressions(), "Model should still be empty after no‑op removal");
    }

    /**
     * Removing the same expression twice should not cause any side effects after the first removal.
     */
    @Test
    void testRemoveExpressionTwice() {
        // Given: a model with one expression
        String exprName = "duplicateRemoval";
        model.addExpression(exprName);
        assertNotNull(model.getExpression(exprName));
        int countAfterAdd = model.countExpressions();
        // When: we remove it twice
        model.removeExpression(exprName);
        // second call – should be harmless
        model.removeExpression(exprName);
        // Then: the expression is gone and the count reflects a single removal only
        assertNull(model.getExpression(exprName));
        assertEquals(countAfterAdd - 1, model.countExpressions(), "Only one removal should affect the expression count");
    }

    /**
     * Removing a {@code null} name must be a no‑op and must not throw an exception.
     */
    @Test
    void testRemoveNullName() {
        // Given: a model with a single expression
        model.addExpression("someExpr");
        int countBefore = model.countExpressions();
        // When: we call removeExpression with null
        assertDoesNotThrow(() -> model.removeExpression(null));
        // Then: nothing changes
        assertEquals(countBefore, model.countExpressions(), "Expression count must stay unchanged when removing null");
        assertNull(model.getExpression(null), "getExpression(null) should return null");
    }

    /**
     * Removing an empty‑string name that is not present must be a no‑op.
     */
    @Test
    void testRemoveEmptyStringNameNotPresent() {
        // Given: a model without an empty‑string named expression
        model.addExpression("exprA");
        int countBefore = model.countExpressions();
        // When: we attempt to remove the empty string
        assertDoesNotThrow(() -> model.removeExpression(""));
        // Then: model state is unchanged
        assertEquals(countBefore, model.countExpressions(), "Expression count must stay unchanged when removing a non‑existent empty name");
        assertNull(model.getExpression(""), "getExpression(\"\") should return null");
    }

    /**
     * Removing an expression whose name is the empty string should succeed.
     */
    @Test
    void testRemoveEmptyStringNamePresent() {
        // Given: an expression with an empty name
        model.addExpression("");
        assertNotNull(model.getExpression(""), "Empty‑named expression must exist before removal");
        int countBefore = model.countExpressions();
        // When: we remove it
        model.removeExpression("");
        // Then: it is gone and the count decreased
        assertNull(model.getExpression(""), "Empty‑named expression should be removed");
        assertEquals(countBefore - 1, model.countExpressions(), "Expression count should decrease after removing empty‑named expression");
    }

    /**
     * Removing one expression from a model that contains several should only affect the targeted one.
     */
    @Test
    void testRemoveExpressionAmongMultiple() {
        // Given: three distinct expressions
        model.addExpression("exprA");
        model.addExpression("exprB");
        model.addExpression("exprC");
        assertEquals(3, model.countExpressions(), "Model should contain three expressions");
        // When: we remove 'exprB'
        model.removeExpression("exprB");
        // Then: only exprB is gone, others remain
        assertNull(model.getExpression("exprB"), "exprB should be removed");
        assertNotNull(model.getExpression("exprA"), "exprA must still be present");
        assertNotNull(model.getExpression("exprC"), "exprC must still be present");
        assertEquals(2, model.countExpressions(), "Expression count should be reduced by exactly one");
    }

    /**
     * Expression names are case‑sensitive – removing with a different case must not affect the original.
     */
    @Test
    void testRemoveExpressionCaseSensitivity() {
        // Given: an expression with mixed‑case name
        model.addExpression("ExprLower");
        assertNotNull(model.getExpression("ExprLower"));
        int countBefore = model.countExpressions();
        // When: we attempt to remove using a different case
        model.removeExpression("exprlower");
        // Then: original expression is untouched
        assertNotNull(model.getExpression("ExprLower"), "Expression with original case must remain after case‑mismatched removal");
        assertNull(model.getExpression("exprlower"), "getExpression with mismatched case should return null");
        assertEquals(countBefore, model.countExpressions(), "Expression count must remain unchanged when case does not match");
    }

    /**
     * Parameterised test covering both null and empty‑string removal as no‑ops.
     * Demonstrates use of {@code @ParameterizedTest} for similar scenarios.
     */
    @ParameterizedTest(name = "removeExpression with name=''{0}'' should be a no‑op")
    @CsvSource({ // special handling inside test
    "null", // empty string
    "''" })
    void testRemoveNoOpScenarios(String name) {
        // Normalise the CSV representation: the literal string "null" means a real null reference.
        String actualName = "null".equals(name) ? null : name;
        // Given: a model with a single regular expression
        model.addExpression("regular");
        int countBefore = model.countExpressions();
        // When: removeExpression is called with the test value
        assertDoesNotThrow(() -> model.removeExpression(actualName));
        // Then: model state must be unchanged
        assertEquals(countBefore, model.countExpressions(), "Expression count must stay unchanged for no‑op removal");
        assertNotNull(model.getExpression("regular"), "Existing expression must still be present");
        assertNull(model.getExpression(actualName), "Lookup for the removed name must return null");
    }
}
