package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionsBasedModel_removeExpression_Test {

    /**
     * Model used in each test case.
     */
    private ExpressionsBasedModel model;

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    /**
     * Clear the reference after each test (good practice, not strictly required).
     */
    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Verify that an existing expression can be removed and that the model state updates.
     */
//     @Test
//     void testRemoveExistingExpression() {
//         // GIVEN: a model with two named expressions
//         Expression exprA = model.newExpression("exprA");
//         exprA.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
//         Expression exprB = model.newExpression("exprB");
//         exprB.lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));
//         // sanity check – both expressions are present
//         assertEquals("Model should contain two expressions before removal", 2, model.countExpressions());
//         assertNotNull(model.getExpression("exprA"));
//         assertNotNull(model.getExpression("exprB"));
//         // WHEN: removing one of them
//         model.removeExpression("exprA");
//         // THEN: only the other expression remains
//         assertEquals("Model should contain one expression after removal", 1, model.countExpressions());
//         assertNull("Removed expression should no longer be retrievable", model.getExpression("exprA"));
//         assertNotNull("Other expression must still be present", model.getExpression("exprB"));
//     }

    /**
     * Removing a name that does not exist must be a no‑op and must not throw.
     */
//     @Test
//     void testRemoveNonExistingExpression() {
//         // GIVEN: a model with a single expression
//         model.newExpression("onlyOne");
//         assertEquals("Pre‑condition: one expression present", 1, model.countExpressions());
//         // WHEN: attempting to remove a non‑existing expression
//         try {
//             model.removeExpression("doesNotExist");
//         } catch (Exception e) {
//             fail("removeExpression should not throw when the name is absent");
//         }
//         // THEN: the model is unchanged
//         assertEquals("Model should still contain the original expression", 1, model.countExpressions());
//         assertNotNull("Existing expression must remain untouched", model.getExpression("onlyOne"));
//     }

    /**
     * Removing an expression twice should leave the model unchanged after the first removal.
     */
//     @Test
//     void testRemoveExpressionTwice() {
//         // GIVEN: a model with a single expression
//         model.newExpression("exp4");
//         assertEquals("Pre‑condition: one expression should be present", 1, model.countExpressions());
//         // WHEN: first removal
//         model.removeExpression("exp4");
//         assertEquals("Expression should be removed after first call", 0, model.countExpressions());
//         // AND: second removal (idempotent)
//         try {
//             model.removeExpression("exp4");
//         } catch (Exception e) {
//             fail("Second removal must not throw");
//         }
//         assertEquals("Expression count must stay at zero after second removal", 0, model.countExpressions());
//     }

    /**
     * removeExpression - null name
     */
//     @Test
//     void testRemoveNullName() {
//         // GIVEN: a model with two expressions (none with a null key)
//         model.newExpression("a");
//         model.newExpression("b");
//         int beforeCount = model.countExpressions();
//         // WHEN: null is passed as the name
//         try {
//             model.removeExpression(null);
//         } catch (Exception e) {
//             fail("removeExpression should tolerate a null argument");
//         }
//         // THEN: internal map is unchanged
//         assertEquals("Expression count must remain unchanged after null removal", beforeCount, model.countExpressions());
//         assertNotNull(model.getExpression("a"));
//         assertNotNull(model.getExpression("b"));
//     }

    /**
     * removeExpression - empty string name
     */
//     @Test
//     void testRemoveEmptyStringName() {
//         // GIVEN: a model with an expression whose name is the empty string
//         model.newExpression("");
//         // also add a regular expression to ensure other entries stay untouched
//         model.newExpression("regular");
//         assertEquals("Pre‑condition: two expressions should be present", 2, model.countExpressions());
//         // WHEN: removing the empty‑string named expression
//         model.removeExpression("");
//         // THEN: only the regular expression remains
//         assertNull("Expression with empty name must be removed", model.getExpression(""));
//         assertNotNull("Other expressions must remain", model.getExpression("regular"));
//         assertEquals("Expression count must be decremented by one", 1, model.countExpressions());
//     }

    /**
     * removeExpression - multiple expressions
     */
//     @Test
//     void testRemoveExpressionAmongMultiple() {
//         // GIVEN: three distinct expressions
//         model.newExpression("exp1");
//         model.newExpression("exp2");
//         model.newExpression("exp3");
//         assertEquals("Pre‑condition: three expressions should be present", 3, model.countExpressions());
//         // WHEN: removing the middle one
//         model.removeExpression("exp2");
//         // THEN: the other two are still accessible and count is reduced
//         assertNull("Expression \"exp2\" must be removed", model.getExpression("exp2"));
//         assertNotNull("Expression \"exp1\" must remain", model.getExpression("exp1"));
//         assertNotNull("Expression \"exp3\" must remain", model.getExpression("exp3"));
//         assertEquals("Expression count must be reduced to two", 2, model.countExpressions());
//     }
}