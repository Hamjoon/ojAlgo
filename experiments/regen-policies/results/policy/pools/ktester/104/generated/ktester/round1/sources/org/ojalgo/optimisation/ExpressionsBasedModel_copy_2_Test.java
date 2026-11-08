package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionsBasedModel_copy_2_Test {

    private ExpressionsBasedModel original;

    @BeforeEach
    void setupBeforeEach() {
        // Build a small model that will be used for all tests
        original = new ExpressionsBasedModel();
        // Two variables with simple bounds and weights
        original.newVariable("x1").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        original.newVariable("x2").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20)).weight(BigDecimal.ONE);
        // One expression linking the two variables
        Expression expr = original.newExpression("x1 + 2*x2 = 30").lower(BigDecimal.valueOf(30)).upper(BigDecimal.valueOf(30));
        // x1 coefficient
        expr.set(original.getVariable(0), BigDecimal.ONE);
        // x2 coefficient
        expr.set(original.getVariable(1), BigDecimal.valueOf(2));
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources (if any)
        if (original != null) {
            original.dispose();
        }
    }

    @Test
    void testCopyWithoutRelax() {
        // Act
        ExpressionsBasedModel copy = original.copy(false);
        // Assert that a new instance is created
        assertNotSame(original, copy, "copy() should return a new instance");
        // Assert that the number of variables and expressions are identical
        assertEquals(original.countVariables(), copy.countVariables(), "Variable count should be preserved in the copy");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count should be preserved in the copy");
        // Verify that the copy is independent: adding a variable to the original does not affect the copy
        original.newVariable("x3").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        assertEquals(2, copy.countVariables(), "Copy should still contain the original number of variables after the source is modified");
        assertEquals(3, original.countVariables(), "Original now has three variables");
    }

    @Test
    void testCopyWithRelax() {
        // Act
        ExpressionsBasedModel copy = original.copy(true);
        // Assert that a new instance is created
        assertNotSame(original, copy, "copy(true) should return a new instance");
        // The copy should still contain the same structural information
        assertEquals(original.countVariables(), copy.countVariables(), "Variable count should be preserved when copying with relax");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count should be preserved when copying with relax");
        // The copy should be a valid model (validation should succeed)
        assertTrue(copy.validate(), "The copied model should be internally consistent");
        // Verify independence as in the non‑relax case
        original.newVariable("x4").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(7)).weight(BigDecimal.ONE);
        assertEquals(2, copy.countVariables(), "Copy should remain unchanged after the original model is mutated");
    }

    @ParameterizedTest(name = "copy({0}) preserves variable and expression counts")
    @CsvSource({ "false", "true" })
    void testVariableAndExpressionCountPreservation(boolean relax) {
        ExpressionsBasedModel copy = original.copy(relax);
        assertEquals(original.countVariables(), copy.countVariables(), "Variable count must be identical after copy");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count must be identical after copy");
    }

    @ParameterizedTest(name = "copy({0}) is not a shallow copy")
    @CsvSource({ "false", "true" })
    void testShallowCopyFlagIsFalse(boolean relax) {
        ExpressionsBasedModel copy = original.copy(relax);
        assertFalse(copy.isShallowCopy(), "copy(boolean) should always create a non‑shallow copy (isShallowCopy() == false)");
    }

    @ParameterizedTest(name = "copy({0}) shares the same Options instance")
    @CsvSource({ "false", "true" })
    void testOptionsReferenceIsShared(boolean relax) {
        ExpressionsBasedModel copy = original.copy(relax);
        assertSame(original.options, copy.options, "The Options object should be shared between original and copy");
    }

//     @Test
//     void testRelaxationStateWhenOriginalRelaxedAndRelaxFlagFalse() {
//         // Put the original model into a relaxed state.
//         // The relax() method returns a new relaxed model, so we need to keep that reference.
//         original = original.relax();
//         assertTrue(original.isRelaxed(), "Original should be relaxed before copying");
//         ExpressionsBasedModel copy = original.copy(false);
//         // No additional relax(false) call – copy should inherit the relaxed flag
//         assertTrue(copy.isRelaxed(), "When relax flag is false, the copy must retain the original's relaxation state");
//     }

//     @Test
//     void testRelaxationStateWhenOriginalRelaxedAndRelaxFlagTrue() {
//         // make original relaxed
//         original = original.relax();
//         assertTrue(original.isRelaxed(), "Original should be relaxed before copying");
//         ExpressionsBasedModel copy = original.copy(true);
//         // copy.relax(false) is invoked, forcing a non‑relaxed state
//         assertFalse(copy.isRelaxed(), "When relax flag is true, copy.relax(false) forces the copy to be non‑relaxed");
//     }

    @Test
    void testRelaxationStateWhenOriginalNotRelaxed() {
        // Ensure original is NOT relaxed
        assertFalse(original.isRelaxed(), "Original model should start non‑relaxed");
        // relax = false → copy should also be non‑relaxed
        ExpressionsBasedModel copyFalse = original.copy(false);
        assertFalse(copyFalse.isRelaxed(), "Copy with relax=false should keep the non‑relaxed state of the original");
        // relax = true → copy.relax(false) forces non‑relaxed as well
        ExpressionsBasedModel copyTrue = original.copy(true);
        assertFalse(copyTrue.isRelaxed(), "Copy with relax=true should also be non‑relaxed after internal relax(false) call");
    }

    @Test
    void testIndependenceWhenRemovingExpressionFromCopy() {
        ExpressionsBasedModel copy = original.copy(false);
        assertEquals(1, original.countExpressions(), "Pre‑condition: original model must contain exactly one expression");
        // Remove the expression from the copy only – use the actual expression name
        copy.removeExpression("x1 + 2*x2 = 30");
        // Original must remain unchanged
        assertEquals(1, original.countExpressions(), "Removing an expression from the copy must NOT affect the original model");
        // Copy should now have zero expressions
        assertEquals(0, copy.countExpressions(), "Copy should reflect the removal of the expression");
    }

    @Test
    void testIndependenceWhenAddingVariableToCopyDoesNotAffectOriginal() {
        ExpressionsBasedModel copy = original.copy(false);
        // Add a new variable only to the copy
        copy.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ONE);
        // Original must stay unchanged
        assertEquals(2, original.countVariables(), "Adding a variable to the copy must NOT change the original's variable count");
        assertEquals(3, copy.countVariables(), "Copy should reflect the newly added variable");
    }
}