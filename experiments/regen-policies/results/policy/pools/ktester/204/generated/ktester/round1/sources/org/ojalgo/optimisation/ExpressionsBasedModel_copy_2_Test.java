package org.ojalgo.optimisation;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_copy_2_Test {

    private ExpressionsBasedModel originalModel;
    private Variable varX;
    private Expression expr;

    private static final long serialVersionUID = 1L;

    @BeforeEach
    void setupBeforeEach() {
        // Build a simple model with one integer variable and one constraint.
        originalModel = new ExpressionsBasedModel();
        // Integer variable (default is continuous, we make it integer explicitly)
        varX = originalModel.newVariable("x")
                .lower(BigDecimal.ZERO)
                .upper(BigDecimal.TEN)
                .weight(BigDecimal.ONE)
                .integer(true);
        // Simple expression: 2 * x = 4
        expr = originalModel.newExpression("c1")
                .lower(BigDecimal.valueOf(4))
                .upper(BigDecimal.valueOf(4));
        expr.set(varX, BigDecimal.valueOf(2));
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose models to release any native resources.
        if (originalModel != null) {
            originalModel.dispose();
        }
    }

    @Test
    void testCopyWithoutRelax() {
        ExpressionsBasedModel copy = originalModel.copy(false);
        // The copy must be a different object.
        assertNotSame(originalModel, copy, "Copy should be a distinct instance");
        // The copy should contain the same number of variables and expressions.
        assertEquals(originalModel.countVariables(), copy.countVariables(), "Variable count must be preserved");
        assertEquals(originalModel.countExpressions(), copy.countExpressions(), "Expression count must be preserved");
        // The copy must not be marked as relaxed.
        assertFalse(copy.isRelaxed(), "Copy without relaxation should not be relaxed");
        // Changing the copy must not affect the original.
        copy.newVariable("y").lower(BigDecimal.ONE);
        assertEquals(1, originalModel.countVariables(), "Original model must remain unchanged after modifying the copy");
        copy.dispose();
    }

    @Test
    void testCopyWithRelax() {
        ExpressionsBasedModel relaxedCopy = originalModel.copy(true);
        // The relaxed copy must be a different instance.
        assertNotSame(originalModel, relaxedCopy, "Relaxed copy should be a distinct instance");
        // It should still contain the same structural elements.
        assertEquals(originalModel.countVariables(), relaxedCopy.countVariables(), "Variable count must be preserved in relaxed copy");
        assertEquals(originalModel.countExpressions(), relaxedCopy.countExpressions(), "Expression count must be preserved in relaxed copy");
        // The copy should report being relaxed.
        assertTrue(relaxedCopy.isRelaxed(), "Copy with relax=true should be marked as relaxed");
        // The integer flag of the variable should be cleared after relaxation.
        Variable copiedVar = relaxedCopy.getVariable(0);
        assertFalse(copiedVar.isInteger(), "Variable should no longer be integer after relaxation");
        relaxedCopy.dispose();
    }

    /* -------------------------------------------------
     *  Deep copy integrity
     * ------------------------------------------------- */
    @Test
    void testDeepCopyIntegrityWithoutRelax() {
        ExpressionsBasedModel copy = originalModel.copy(false);
        // The copy must not be a shallow copy.
        assertFalse(copy.isShallowCopy(), "copy(false) should not be a shallow copy");
        assertFalse(originalModel.isShallowCopy(), "Original model should not be a shallow copy");
        // Internal mutable collections must be different objects.
        List<Variable> originalVars = originalModel.getVariables();
        List<Variable> copyVars = copy.getVariables();
        assertNotSame(originalVars, copyVars, "Variable list must be a different instance");
        List<Expression> originalExprs = originalModel.getExpressions();
        List<Expression> copyExprs = copy.getExpressions();
        assertNotSame(originalExprs, copyExprs, "Expression list must be a different instance");
        // Options field is public final – should also be a different instance.
        assertNotSame(originalModel.options, copy.options, "Options object must be copied");
        copy.dispose();
    }

    @Test
    void testDeepCopyIntegrityWithRelax() {
        ExpressionsBasedModel copy = originalModel.copy(true);
        assertTrue(copy.isRelaxed(), "Copy should be relaxed");
        assertFalse(copy.isShallowCopy(), "Relaxed copy should still be a deep copy");
        // Verify collections are independent.
        assertNotSame(originalModel.getVariables(), copy.getVariables(), "Variable collections must be independent");
        assertNotSame(originalModel.getExpressions(), copy.getExpressions(), "Expression collections must be independent");
        copy.dispose();
    }

    /* -------------------------------------------------
     *  Independence of subsequent modifications
     * ------------------------------------------------- */
    @Test
    void testModificationIndependenceWithoutRelax() {
        ExpressionsBasedModel copy = originalModel.copy(false);
        // Add a variable to the copy.
        copy.newVariable("y").lower(BigDecimal.ONE);
        assertEquals(1, originalModel.countVariables(), "Original model variable count must stay unchanged");
        assertEquals(2, copy.countVariables(), "Copy should reflect the added variable");
        // Remove the original expression from the copy.
        copy.removeExpression("c1");
        assertEquals(1, originalModel.countExpressions(), "Original expression count must stay unchanged");
        assertEquals(0, copy.countExpressions(), "Copy should reflect the removed expression");
        copy.dispose();
    }

    @Test
    void testModificationIndependenceWithRelax() {
        ExpressionsBasedModel copy = originalModel.copy(true);
        // Change objective constant on the copy.
        copy.addObjectiveConstant(BigDecimal.valueOf(5));
        assertEquals(BigDecimal.ZERO, originalModel.getObjectiveConstant(), "Original objective constant must stay unchanged");
        assertEquals(BigDecimal.valueOf(5), copy.getObjectiveConstant(), "Copy should have the new objective constant");
        copy.dispose();
    }

    /* -------------------------------------------------
     *  Preserving model data
     * ------------------------------------------------- */
    @Test
    void testPreserveModelDataWithoutRelax() {
        ExpressionsBasedModel copy = originalModel.copy(false);
        // Variables
        assertEquals(originalModel.countVariables(), copy.countVariables(), "Variable count must match");
        Variable origVar = originalModel.getVariable(0);
        Variable copyVar = copy.getVariable(0);
        assertEquals(origVar.getName(), copyVar.getName(), "Variable name must be preserved");
        assertEquals(origVar.getLowerLimit(), copyVar.getLowerLimit(), "Lower bound must be preserved");
        assertEquals(origVar.getUpperLimit(), copyVar.getUpperLimit(), "Upper bound must be preserved");
        assertEquals(origVar.isInteger(), copyVar.isInteger(), "Integer flag must be preserved");
        // Expressions
        assertEquals(originalModel.countExpressions(), copy.countExpressions(), "Expression count must match");
        Expression origExpr = originalModel.getExpression("c1");
        Expression copyExpr = copy.getExpression("c1");
        assertEquals(origExpr.getLowerLimit(), copyExpr.getLowerLimit(), "Expression lower limit must be preserved");
        assertEquals(origExpr.getUpperLimit(), copyExpr.getUpperLimit(), "Expression upper limit must be preserved");
        assertEquals(origExpr.get(varX), copyExpr.get(copyVar), "Expression coefficient must be preserved");
        copy.dispose();
    }

    @Test
    void testPreserveModelDataWithRelax() {
        ExpressionsBasedModel copy = originalModel.copy(true);
        // Before relaxation the data are identical – after relaxation integer flag cleared.
        Variable copyVar = copy.getVariable(0);
        assertEquals(varX.getName(), copyVar.getName(), "Variable name preserved");
        assertEquals(varX.getLowerLimit(), copyVar.getLowerLimit(), "Lower bound preserved");
        assertEquals(varX.getUpperLimit(), copyVar.getUpperLimit(), "Upper bound preserved");
        assertFalse(copyVar.isInteger(), "Integer flag cleared after relaxation");
        // Coefficients and limits stay the same.
        Expression copyExpr = copy.getExpression("c1");
        assertEquals(expr.getLowerLimit(), copyExpr.getLowerLimit(), "Expression lower limit preserved");
        assertEquals(expr.getUpperLimit(), copyExpr.getUpperLimit(), "Expression upper limit preserved");
        assertEquals(expr.get(varX), copyExpr.get(copyVar), "Expression coefficient preserved after relaxation");
        copy.dispose();
    }

    /* -------------------------------------------------
     *  Behaviour when original is already relaxed
     * ------------------------------------------------- */
    @Test
    void testCopyWhenOriginalAlreadyRelaxedWithoutRelax() {
        // make original relaxed
        originalModel.relax();
        assertTrue(originalModel.isRelaxed(), "Original should be relaxed before copy");
        ExpressionsBasedModel copy = originalModel.copy(false);
        assertFalse(copy.isRelaxed(), "copy(false) must not inherit relaxed state");
        copy.dispose();
    }

    @Test
    void testCopyWhenOriginalAlreadyRelaxedWithRelax() {
        originalModel.relax();
        assertTrue(originalModel.isRelaxed(), "Original should be relaxed before copy");
        ExpressionsBasedModel copy = originalModel.copy(true);
        assertTrue(copy.isRelaxed(), "copy(true) must be relaxed even if source already is");
        copy.dispose();
    }

    /* -------------------------------------------------
     *  Copying an empty model
     * ------------------------------------------------- */
    @Test
    void testCopyEmptyModelWithoutRelax() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(false);
        assertEquals(0, copy.countVariables(), "Empty copy must have zero variables");
        assertEquals(0, copy.countExpressions(), "Empty copy must have zero expressions");
        assertFalse(copy.isRelaxed(), "Empty copy without relax must not be relaxed");
        empty.dispose();
        copy.dispose();
    }

    @Test
    void testCopyEmptyModelWithRelax() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy(true);
        assertEquals(0, copy.countVariables(), "Empty relaxed copy must have zero variables");
        assertEquals(0, copy.countExpressions(), "Empty relaxed copy must have zero expressions");
        assertTrue(copy.isRelaxed(), "Empty copy with relax must be relaxed");
        empty.dispose();
        copy.dispose();
    }

    /* -------------------------------------------------
     *  Exception when source model is disposed
     * ------------------------------------------------- */
    @Test
    void testCopyAfterDisposeThrows() {
        originalModel.dispose();
        assertThrows(ProgrammingError.class, () -> originalModel.copy(false), "Copying a disposed model should throw ProgrammingError");
        assertThrows(ProgrammingError.class, () -> originalModel.copy(true), "Copying a disposed model with relax should also throw ProgrammingError");
    }

    /* -------------------------------------------------
     *  Exception when internal options field is null
     * ------------------------------------------------- */
    @Test
    void testCopyWithNullOptionsThrows() throws Exception {
        // Use reflection to set the final 'options' field to null.
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(originalModel, null);
        // The constructor of the copy will attempt to read this field and should NPE.
        assertThrows(NullPointerException.class, () -> originalModel.copy(false), "Copying when options is null should throw NullPointerException");
        assertThrows(NullPointerException.class, () -> originalModel.copy(true), "Copying with relax when options is null should also throw NullPointerException");
    }

    /* -------------------------------------------------
     *  Edge‑case: passing null for the primitive boolean via reflection
     * ------------------------------------------------- */
    @Test
    void testCopyWithNullArgumentViaReflection() throws Exception {
        // Obtain the Method object for copy(boolean)
        java.lang.reflect.Method copyMethod = ExpressionsBasedModel.class.getMethod("copy", boolean.class);
        // Invoke with a null argument – reflection will attempt to unbox and throw NPE.
        assertThrows(NullPointerException.class, () -> copyMethod.invoke(originalModel, new Object[]{null}), "Invoking copy(boolean) with null via reflection should throw NullPointerException");
    }

    /* -------------------------------------------------
     *  Disabled: relaxation prohibited scenario
     * ------------------------------------------------- */
    @Disabled("Requires knowledge of IntegrationProperty values that are not exposed in the public API.")
    @Test
    void testCopyWithRelaxWhenRelaxationDisabled() {
        // Placeholder for future implementation when the appropriate IntegrationProperty
        // constant is known. The test would configure the model to disallow relaxation,
        // then assert that copy(true) throws ProgrammingError.
    }
}