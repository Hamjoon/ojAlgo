package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_copy_3_Test {

    private ExpressionsBasedModel originalModel;

    private Variable x;

    private Expression objective;

    private Expression constraint;

    //     @BeforeEach
    void setupBeforeEach() {
        // Build a simple model with one variable, one objective expression and one constraint
        originalModel = new ExpressionsBasedModel();
        // Variable x in [0, 10] with weight 1 (contributes to the objective)
        x = originalModel.newVariable("x").lower(0).upper(10).weight(1);
        // Objective: minimise 1·x, target value 5 (level)
        objective = originalModel.newExpression("obj").weight(1).set(x, 1).level(5);
        // Simple constraint: 0 ≤ x ≤ 10 (already covered by variable bounds, but still an expression)
        constraint = originalModel.newExpression("c1").lower(0).upper(10).set(x, 1);
    }

    //     @Test
    void testPruneHasNoEffectWhenAllExpressionsUsed() {
        // The originalModel built in @BeforeEach already has all expressions used
        ExpressionsBasedModel pruned = originalModel.copy(false, true);
        assertEquals(originalModel.countExpressions(), pruned.countExpressions(), "Prune on a fully‑used model must not change the expression count");
    }

    @Test
    void testCopyShallowFlag() {
        // shallow = true, prune = false
        ExpressionsBasedModel shallowCopy = originalModel.copy(true, false);
        // The copy reports that it is a shallow copy
        assertTrue(shallowCopy.isShallowCopy(), "Shallow copy should report true for isShallowCopy()");
        // The original model is not a shallow copy
        assertFalse(originalModel.isShallowCopy(), "Original model should not be a shallow copy");
        // Variables must be cloned – they must not be the same instance
        Variable originalVar = originalModel.getVariable(0);
        Variable copiedVar = shallowCopy.getVariable(0);
        assertNotSame(originalVar, copiedVar, "Variables should be cloned, not shared");
        // The number of expressions must be identical (no pruning took place)
        assertEquals(originalModel.countExpressions(), shallowCopy.countExpressions(), "Expression count should be preserved in shallow copy");
    }

    @Test
    void testCopyDeepFlag() {
        // shallow = false, prune = false (deep copy)
        ExpressionsBasedModel deepCopy = originalModel.copy(false, false);
        // The deep copy must report that it is not a shallow copy
        assertFalse(deepCopy.isShallowCopy(), "Deep copy should report false for isShallowCopy()");
        // Variables are still cloned – they must not be the same instance
        Variable originalVar = originalModel.getVariable(0);
        Variable copiedVar = deepCopy.getVariable(0);
        assertNotSame(originalVar, copiedVar, "Variables should be cloned in deep copy as well");
        // Expression count must stay the same
        assertEquals(originalModel.countExpressions(), deepCopy.countExpressions(), "Expression count should be preserved in deep copy");
    }

    @Test
    void testCopyPruneFlagWhenNoRedundantExpressions() {
        // shallow = false, prune = true – there are no redundant expressions in the model
        ExpressionsBasedModel prunedCopy = originalModel.copy(false, true);
        // Since nothing is redundant, the copy should contain exactly the same number of expressions
        assertEquals(originalModel.countExpressions(), prunedCopy.countExpressions(), "When no redundant expressions exist, prune flag must not change expression count");
    }

//     @Test
//     void testShallowCopySharesVariableState() {
//         ExpressionsBasedModel shallowCopy = originalModel.copy(true, false);
//         // Change lower bound of the original variable
//         originalModel.getVariable(0).lower(2);
//         // The change must be visible in the copy (shared instance)
//         assertEquals(BigDecimal.valueOf(2), shallowCopy.getVariable(0).lower(), "Modification of a variable in the original model must be reflected in a shallow copy");
//     }

//     @Test
//     void testDeepCopyIsolatesVariableState() {
//         ExpressionsBasedModel deepCopy = originalModel.copy(false, false);
//         // Change lower bound of the original variable
//         originalModel.getVariable(0).lower(3);
//         // The copy must retain its original bound (no sharing)
//         assertNotEquals(BigDecimal.valueOf(3), deepCopy.getVariable(0).lower(), "Modification of a variable in the original model must NOT affect a deep copy");
//     }

    @Test
    void testPruneRemovesUnusedExpressions() {
        // Build a model with an unused expression (weight 0, no bounds, not referenced)
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable y = model.newVariable("y").lower(0).upper(5).weight(1);
        // Used expression – contributes to objective
        model.newExpression("used").weight(1).set(y, 1);
        // Unused expression – weight zero, coefficient zero (does nothing)
        model.newExpression("unused").weight(0).set(y, 0);
        int before = model.countExpressions();
        assertTrue(before > 0, "Model must contain at least one expression before pruning");
        ExpressionsBasedModel pruned = model.copy(false, true);
        int after = pruned.countExpressions();
        assertTrue(after < before, "Pruned copy must contain fewer expressions than the original when unused expressions exist");
    }

    @Test
    void testCopyOfEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(true, false);
        assertEquals(0, copy.countVariables(), "Copied empty model must have zero variables");
        assertEquals(0, copy.countExpressions(), "Copied empty model must have zero expressions");
    }

    @Test
    void testShallowCopyRetainsOptimisationSense() {
        // The default sense is MINIMISE; we just verify both models report the same value
        ExpressionsBasedModel shallowCopy = originalModel.copy(true, false);
        assertEquals(originalModel.getOptimisationSense(), shallowCopy.getOptimisationSense(), "Optimisation sense must be identical between original and shallow copy");
    }

    @Test
    void testDeepCopyObjectiveConstantIndependence() throws Exception {
        // Set an objective constant on the original model via reflection
        Field constField = ExpressionsBasedModel.class.getDeclaredField("objectiveConstant");
        constField.setAccessible(true);
        constField.set(originalModel, new BigDecimal("10"));
        ExpressionsBasedModel deepCopy = originalModel.copy(false, false);
        // Change the constant in the original model
        constField.set(originalModel, new BigDecimal("20"));
        // The copy must retain the original constant value (10)
        assertEquals(new BigDecimal("10"), deepCopy.getObjectiveConstant(), "Deep copy must have an independent objective constant");
    }

    @Test
    void testCopyOnDisposedModelThrowsProgrammingError() {
        originalModel.dispose();
        assertThrows(ProgrammingError.class, () -> originalModel.copy(true, false), "Copying a disposed model must throw ProgrammingError");
    }

    @Test
    void testCopyOnDisposedModelWithPruneThrowsProgrammingError() {
        originalModel.dispose();
        assertThrows(ProgrammingError.class, () -> originalModel.copy(false, true), "Copying a disposed model (even with prune) must throw ProgrammingError");
    }

    @Test
    void testCopyWithNullOptionsThrowsNPE() throws Exception {
        // Force the internal options field to null via reflection
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(originalModel, null);
        assertThrows(NullPointerException.class, () -> originalModel.copy(true, true), "Copying a model with a null options field must throw NullPointerException");
    }
}