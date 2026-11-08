package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_copy_3_Test {

    private ExpressionsBasedModel originalModel;

    private Variable varX;

    private Expression exprObj;

    @BeforeEach
    void setupBeforeEach() {
        // Build a tiny optimisation model that has at least one variable and one objective expression.
        originalModel = new ExpressionsBasedModel();
        // Variable x with bounds and a weight (contributes to the objective).
        varX = originalModel.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Simple objective expression: minimise x.
        exprObj = originalModel.newExpression("objective").set(varX, // coefficient 1·x
        BigDecimal.ONE).weight(// makes this an objective expression
        BigDecimal.ONE).level(BigDecimal.ZERO);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose of the model to free any native resources that might be held.
        if (originalModel != null) {
            originalModel.dispose();
        }
    }

    private static void assertThrowsAnyOf(Class<? extends Throwable> first, Class<? extends Throwable> second, Executable executable) {
        try {
            executable.execute();
            fail("Expected exception of type " + first.getSimpleName() + " or " + second.getSimpleName());
        } catch (Throwable e) {
            if (!first.isInstance(e) && !second.isInstance(e)) {
                fail("Unexpected exception type: " + e.getClass().getSimpleName(), e);
            }
            // Expected – test passes.
        }
    }

    /**
     * Verify that a shallow copy reports {@code isShallowCopy()} as {@code true}
     * and that the copy is a distinct instance with the same public state.
     */
    @Test
    void testShallowCopyFlagIsHonoured() {
        ExpressionsBasedModel shallowCopy = originalModel.copy(true, false);
        // The copy must be a different object.
        assertNotSame(originalModel, shallowCopy, "copy should produce a new model instance");
        // The shallow flag is reflected by the public query method.
        assertTrue(shallowCopy.isShallowCopy(), "When shallow=true the copy must report isShallowCopy()==true");
        // Options are shared (the API copies the reference, not a deep clone).
        assertSame(originalModel.options, shallowCopy.options, "Options object should be the same instance in both models");
        // The number of variables and expressions must be identical.
        assertEquals(originalModel.countVariables(), shallowCopy.countVariables(), "Variable count must be preserved in a shallow copy");
        assertEquals(originalModel.countExpressions(), shallowCopy.countExpressions(), "Expression count must be preserved in a shallow copy");
    }

    /**
     * Verify that a non‑shallow copy reports {@code isShallowCopy()} as {@code false}
     * while still preserving the observable state.
     */
    @Test
    void testDeepCopyFlagIsHonoured() {
        ExpressionsBasedModel deepCopy = originalModel.copy(false, false);
        // The copy must be a different object.
        assertNotSame(originalModel, deepCopy, "copy should produce a new model instance");
        // The deep copy must report that it is *not* a shallow copy.
        assertFalse(deepCopy.isShallowCopy(), "When shallow=false the copy must report isShallowCopy()==false");
        // Options are still shared (as defined by the constructor implementation).
        assertSame(originalModel.options, deepCopy.options, "Options object should be the same instance in both models");
        // Variable and expression counts stay the same.
        assertEquals(originalModel.countVariables(), deepCopy.countVariables(), "Variable count must be preserved in a deep copy");
        assertEquals(originalModel.countExpressions(), deepCopy.countExpressions(), "Expression count must be preserved in a deep copy");
    }

    /**
     * Verify that the {@code prune} flag does not affect the basic copy behaviour
     * for a model that contains only essential expressions. The test simply ensures
     * that the method executes without error and that the resulting model still
     * contains the expected number of expressions.
     */
    @Test
    void testCopyWithPruneFlag() {
        // In this simple model there are no redundant or infeasible expressions,
        // therefore pruning should not remove anything.
        ExpressionsBasedModel prunedCopy = originalModel.copy(false, true);
        assertNotSame(originalModel, prunedCopy, "copy should produce a new model instance");
        assertEquals(originalModel.countExpressions(), prunedCopy.countExpressions(), "Prune flag should not remove expressions when none are redundant");
    }

    @Test
    void testNoArgCopyDelegatesToDeepUnprunedCopy() {
        ExpressionsBasedModel copyViaNoArg = originalModel.copy();
        ExpressionsBasedModel copyViaExplicit = originalModel.copy(false, false);
        assertNotSame(originalModel, copyViaNoArg);
        assertFalse(copyViaNoArg.isShallowCopy());
        assertEquals(originalModel.countVariables(), copyViaNoArg.countVariables());
        assertEquals(originalModel.countExpressions(), copyViaNoArg.countExpressions());
        // The two copies must be equivalent in observable state.
        assertEquals(copyViaExplicit.countVariables(), copyViaNoArg.countVariables());
        assertEquals(copyViaExplicit.countExpressions(), copyViaNoArg.countExpressions());
    }

    @Test
    void testDeepCopyPruneRemovesFixedVariable() {
        // Fixed variable: lower == upper
        Variable fixed = originalModel.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // Expression that only involves the fixed variable
        originalModel.newExpression("fixedExpr").set(fixed, BigDecimal.ONE).weight(BigDecimal.ONE);
        int originalVarCount = originalModel.countVariables();
        int originalExprCount = originalModel.countExpressions();
        // deep + prune
        ExpressionsBasedModel copy = originalModel.copy(false, true);
        assertFalse(copy.isShallowCopy());
        assertEquals(originalVarCount - 1, copy.countVariables(), "Fixed variable should be omitted in a pruned deep copy");
        // The expression that only referenced the fixed variable should also disappear.
        assertEquals(originalExprCount - 1, copy.countExpressions(), "Expression depending solely on a fixed variable should be pruned");
    }

    @Test
    void testShallowCopyPruneRemovesFixedVariableOnlyFromCopy() {
        Variable fixed = originalModel.newVariable("fixed").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3));
        originalModel.newExpression("fixedExpr").set(fixed, BigDecimal.ONE).weight(BigDecimal.ONE);
        int originalVarCount = originalModel.countVariables();
        int originalExprCount = originalModel.countExpressions();
        // shallow + prune
        ExpressionsBasedModel copy = originalModel.copy(true, true);
        assertTrue(copy.isShallowCopy());
        // Original keeps the fixed variable.
        assertEquals(originalVarCount, originalModel.countVariables(), "Original model must retain the fixed variable");
        // Copy should have it removed.
        assertEquals(originalVarCount - 1, copy.countVariables(), "Copy should prune the fixed variable");
        // Expression removal follows the same rule.
        assertEquals(originalExprCount - 1, copy.countExpressions(), "Copy should prune the expression that only uses the fixed variable");
    }

    @Test
    void testDeepCopyWithoutPruneKeepsFixedVariable() {
        Variable fixed = originalModel.newVariable("fixed").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2));
        originalModel.newExpression("fixedExpr").set(fixed, BigDecimal.ONE).weight(BigDecimal.ONE);
        int originalVarCount = originalModel.countVariables();
        int originalExprCount = originalModel.countExpressions();
        // deep, no prune
        ExpressionsBasedModel copy = originalModel.copy(false, false);
        assertEquals(originalVarCount, copy.countVariables(), "Fixed variable must be retained when prune=false");
        assertEquals(originalExprCount, copy.countExpressions(), "Expression must be retained when prune=false");
    }

    @Test
    void testCopyEmptyModelDeep() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(false, false);
        assertNotSame(empty, copy);
        assertEquals(0, empty.countVariables());
        assertEquals(0, empty.countExpressions());
        assertEquals(0, copy.countVariables());
        assertEquals(0, copy.countExpressions());
        empty.dispose();
        copy.dispose();
    }

    @Test
    void testCopyEmptyModelShallow() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(true, false);
        assertNotSame(empty, copy);
        assertEquals(0, copy.countVariables());
        assertEquals(0, copy.countExpressions());
        empty.dispose();
        copy.dispose();
    }

    @Test
    void testShallowCopyReflectsVariableBoundChange() {
        ExpressionsBasedModel shallow = originalModel.copy(true, false);
        // Change lower bound in original
        varX.lower(BigDecimal.valueOf(1));
        // The same Variable instance is shared, therefore the copy sees the change.
        Variable copyVar = shallow.getVariable(varX.index());
        assertEquals(BigDecimal.valueOf(1), copyVar.getLowerBound(), "Shallow copy must reflect changes to shared Variable objects");
        shallow.dispose();
    }

    @Test
    void testShallowCopyDoesNotShareExpressionCollection() {
        ExpressionsBasedModel shallow = originalModel.copy(true, false);
        // Add a new expression to the original after copying
        originalModel.newExpression("newExpr").set(varX, BigDecimal.valueOf(2)).weight(BigDecimal.ONE);
        // The shallow copy should still have the original expression count.
        assertEquals(1, shallow.countExpressions(), "Shallow copy must keep its own expression list independent of the original");
        shallow.dispose();
    }

    @Test
    void testDeepCopyIsIndependentOfVariableBoundChanges() {
        ExpressionsBasedModel deep = originalModel.copy(false, false);
        // Change upper bound in original
        varX.upper(BigDecimal.valueOf(20));
        // Deep copy should retain the original bound (10)
        Variable copyVar = deep.getVariable(varX.index());
        assertEquals(BigDecimal.TEN, copyVar.getUpperBound(), "Deep copy must keep its own independent Variable instances");
        deep.dispose();
    }

    @Test
    void testDeepCopyDoesNotSeeNewExpressions() {
        ExpressionsBasedModel deep = originalModel.copy(false, false);
        // Add a new expression to original after copying
        originalModel.newExpression("extra").set(varX, BigDecimal.ONE).weight(BigDecimal.ONE);
        assertEquals(1, deep.countExpressions(), "Deep copy must retain the expression list as it was at copy time");
        deep.dispose();
    }

    @Test
    void testDeepCopyPreservesSpecialOrderedSet() throws Exception {
        // Create a SOS (type 1 = SOS1) with variable varX
        originalModel.addSpecialOrderedSet(Collections.singletonList(varX), 1, 1);
        ExpressionsBasedModel deep = originalModel.copy(false, false);
        // Use reflection to access the internal SOS collection (named "specialOrderedSets")
        Field sosField = ExpressionsBasedModel.class.getDeclaredField("specialOrderedSets");
        sosField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> originalSos = (Set<?>) sosField.get(originalModel);
        @SuppressWarnings("unchecked")
        Set<?> copySos = (Set<?>) sosField.get(deep);
        assertNotNull(originalSos);
        assertNotNull(copySos);
        assertEquals(originalSos.size(), copySos.size(), "Deep copy must contain the same number of SOS structures");
        // Verify that at least one SOS in the copy contains varX.
        boolean copyContainsVarX = copySos.stream().anyMatch(sos -> {
            try {
                Field varsField = sos.getClass().getDeclaredField("variables");
                varsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Variable> vars = (List<Variable>) varsField.get(sos);
                return vars.contains(varX);
            } catch (Exception e) {
                return false;
            }
        });
        assertTrue(copyContainsVarX, "Copied SOS must reference the original variable instance");
        deep.dispose();
    }

    @Test
    void testShallowCopyPreservesSpecialOrderedSetSharing() throws Exception {
        originalModel.addSpecialOrderedSet(Collections.singletonList(varX), 1, 1);
        ExpressionsBasedModel shallow = originalModel.copy(true, false);
        Field sosField = ExpressionsBasedModel.class.getDeclaredField("specialOrderedSets");
        sosField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> shallowSos = (Set<?>) sosField.get(shallow);
        assertNotNull(shallowSos);
        assertEquals(1, shallowSos.size(), "Shallow copy must retain the SOS");
        // Verify that the Variable inside the SOS is the same instance as varX
        Object sos = shallowSos.iterator().next();
        Field varsField = sos.getClass().getDeclaredField("variables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) varsField.get(sos);
        assertTrue(vars.contains(varX), "Shallow copy SOS must reference the same Variable instance");
        shallow.dispose();
    }

    @Test
    void testDeepCopyObjectiveConstantIsDuplicated() {
        // add a non‑zero constant to the original model
        originalModel.addObjectiveConstant(BigDecimal.valueOf(42));
        ExpressionsBasedModel deep = originalModel.copy(false, false);
        assertEquals(BigDecimal.valueOf(42), deep.getObjectiveConstant(), "Deep copy must contain the same objective constant value");
        // Changing the constant in the original after copying must NOT affect the copy
        originalModel.addObjectiveConstant(BigDecimal.valueOf(1));
        assertEquals(BigDecimal.valueOf(42), deep.getObjectiveConstant(), "Deep copy's constant must remain unchanged after original is modified");
        deep.dispose();
    }

    @Test
    void testShallowCopyObjectiveConstantIsShared() {
        originalModel.addObjectiveConstant(BigDecimal.valueOf(7));
        ExpressionsBasedModel shallow = originalModel.copy(true, false);
        assertEquals(BigDecimal.valueOf(7), shallow.getObjectiveConstant(), "Shallow copy sees the original constant");
        // Adding another constant to the original should be visible in the shallow copy
        originalModel.addObjectiveConstant(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.valueOf(10), shallow.getObjectiveConstant(), "Shallow copy must reflect changes to the mutable constant field");
        shallow.dispose();
    }

    @Test
    void testCopyAfterDisposeThrows() {
        originalModel.dispose();
        // Both deep and shallow copies must fail after disposal.
        assertAll(() -> assertThrowsAnyOf(ProgrammingError.class, IllegalStateException.class, () -> originalModel.copy(false, false)), () -> assertThrowsAnyOf(ProgrammingError.class, IllegalStateException.class, () -> originalModel.copy(true, true)));
    }
}