package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_copy_2_Test {

    /**
     * Verify that {@code copy(false)} creates an independent, non‑relaxed model
     * with the same structure as the original.
     */
    @Test
    void testCopyWithoutRelax() {
        // ----- given ---------------------------------------------------------
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // a simple variable
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // a simple constraint expression: x <= 5
        Expression expr = original.newExpression("c1").set(x, BigDecimal.ONE).upper(BigDecimal.valueOf(5));
        // ----- when ----------------------------------------------------------
        ExpressionsBasedModel copy = original.copy(false);
        // ----- then ----------------------------------------------------------
        // The copy is a different instance
        assertNotSame(original, copy, "Copy should be a different instance");
        // Same number of variables and expressions
        List<Variable> originalVars = original.getVariables();
        List<Variable> copyVars = copy.getVariables();
        assertEquals(originalVars.size(), copyVars.size(), "Copy should contain the same number of variables");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Copy should contain the same number of expressions");
        // The copy must not be a shallow copy
        assertFalse(copy.isShallowCopy(), "Copy should not be shallow");
        // The copy must not be relaxed
        assertFalse(copy.isRelaxed(), "Copy should not be relaxed when relax flag is false");
        // The expression should exist in the copy and be independent
        Expression copyExpr = copy.getExpression("c1");
        assertNotNull(copyExpr, "Expression 'c1' must exist in the copy");
        assertNotSame(expr, copyExpr, "Expression instance must be different");
        // Changing the weight of the variable in the copy must not affect the original
        Variable copyX = copyVars.get(0);
        // change weight only in the copy
        copyX.weight(BigDecimal.valueOf(2));
        assertEquals(BigDecimal.ONE, x.getWeight(), "Original variable weight must remain unchanged");
        assertEquals(BigDecimal.valueOf(2), copyX.getWeight(), "Copy variable weight must reflect the change");
    }

    /**
     * Verify that {@code copy(true)} creates a relaxed copy.
     */
    @Test
    void testCopyWithRelax() {
        // ----- given ---------------------------------------------------------
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable y = original.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(8)).weight(BigDecimal.ONE);
        original.newExpression("c2").set(y, BigDecimal.ONE).lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(6));
        // ----- when ----------------------------------------------------------
        ExpressionsBasedModel relaxedCopy = original.copy(true);
        // ----- then ----------------------------------------------------------
        assertNotSame(original, relaxedCopy, "Relaxed copy should be a different instance");
        assertTrue(relaxedCopy.isRelaxed(), "Copy created with relax=true must be in relaxed state");
        // The copy must still have the same structure
        assertEquals(original.getVariables().size(), relaxedCopy.getVariables().size(), "Relaxed copy must contain same number of variables");
        assertEquals(original.countExpressions(), relaxedCopy.countExpressions(), "Relaxed copy must contain same number of expressions");
    }

    /* --------------------------------------------------------------------- *
     *  1. Sanity check – the method must not throw for either boolean value.
     * --------------------------------------------------------------------- */
    @Test
    void testCopyDoesNotThrow() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // No exception expected for false
        assertDoesNotThrow(() -> model.copy(false));
        // No exception expected for true
        assertDoesNotThrow(() -> model.copy(true));
    }

    /* --------------------------------------------------------------------- *
     *  4. Deep‑copy integrity – verify that variables/expressions are distinct
     *     and that later modifications do not leak between models.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "deepCopyIntegrity(relax={0})")
    @ValueSource(booleans = { false, true })
    void testDeepCopyIntegrity(boolean relax) {
        // ----- given ---------------------------------------------------------
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable a = original.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        original.newExpression("exprA").set(a, BigDecimal.ONE).upper(BigDecimal.valueOf(7));
        // ----- when ----------------------------------------------------------
        ExpressionsBasedModel copy = original.copy(relax);
        // ----- then ----------------------------------------------------------
        // Distinct model instances
        assertNotSame(original, copy);
        // Distinct variable objects
        Variable originalVar = original.getVariable(0);
        Variable copyVar = copy.getVariable(0);
        assertNotSame(originalVar, copyVar, "Variable objects must be different");
        // Distinct expression objects
        Expression originalExpr = original.getExpression("exprA");
        Expression copyExpr = copy.getExpression("exprA");
        assertNotNull(copyExpr);
        assertNotSame(originalExpr, copyExpr, "Expression objects must be different");
        // Modify original variable lower bound – copy must stay unchanged
        originalVar.lower(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.ZERO, copyVar.getLowerLimit(), "Copy's variable lower bound must remain unchanged after original modification");
    }

    /* --------------------------------------------------------------------- *
     *  5. Relaxation flag handling – various source‑model states.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "relaxationFlagHandling(sourceRelaxed={0}, copyRelaxArg={1})")
    @ValueSource(booleans = { false, true })
    void testRelaxationFlagWhenSourceNotRelaxed(boolean copyRelaxArg) {
        // Source model is freshly created → not relaxed
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        assertFalse(original.isRelaxed(), "Fresh model must not be relaxed");
        ExpressionsBasedModel copy = original.copy(copyRelaxArg);
        // If copyRelaxArg == false → copy inherits false
        // If copyRelaxArg == true  → copy.relax(false) forces false
        assertFalse(copy.isRelaxed(), "Copy should be non‑relaxed regardless of copyRelaxArg when source is not relaxed");
    }

    @Test
    void testRelaxationFlagWhenSourceRelaxed() {
        // ----- given ---------------------------------------------------------
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // Explicitly relax the source model
        original.relax(true);
        assertTrue(original.isRelaxed(), "Source model should be relaxed after relax(true)");
        // ----- when ----------------------------------------------------------
        // a) copy with relax = false → should inherit relaxed state
        ExpressionsBasedModel copyInherit = original.copy(false);
        // b) copy with relax = true  → should be forced to non‑relaxed
        ExpressionsBasedModel copyForce = original.copy(true);
        // ----- then ----------------------------------------------------------
        assertTrue(copyInherit.isRelaxed(), "Copy with relax=false must inherit the source's relaxed state");
        assertFalse(copyForce.isRelaxed(), "Copy with relax=true must end up non‑relaxed regardless of source state");
    }

    /* --------------------------------------------------------------------- *
     *  6. Copy metadata – ensure shallow‑copy flag is false and counts match.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "copyMetadata(relax={0})")
    @ValueSource(booleans = { false, true })
    void testCopyMetadata(boolean relax) {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // Populate with a couple of variables and expressions
        original.newVariable("v1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        original.newVariable("v2").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        original.newExpression("e1").set(original.getVariable(0), BigDecimal.ONE);
        original.newExpression("e2").set(original.getVariable(1), BigDecimal.ONE);
        ExpressionsBasedModel copy = original.copy(relax);
        // Shallow‑copy flag must be false
        assertFalse(copy.isShallowCopy(), "copy(boolean) must produce a deep copy (isShallowCopy == false)");
        // Structural counts must be identical
        assertEquals(original.getVariables().size(), copy.getVariables().size(), "Variable count must be preserved");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count must be preserved");
    }
}