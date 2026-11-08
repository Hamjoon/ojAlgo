package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isInfeasible_Test {

    /**
     * A fresh model with no contradictory constraints must report not infeasible.
     */
    @Test
    void testInitiallyFeasible() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isInfeasible(), "A newly created model without any infeasible elements should return false.");
    }

    /**
     * Adding a variable whose lower bound exceeds its upper bound makes the model infeasible.
     */
    @Test
    void testVariableInfeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with contradictory bounds (lower > upper) → infeasible.
        model.newVariable("v").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3));
        assertTrue(model.isInfeasible(), "Model should be infeasible when a variable has lower > upper.");
    }

    /**
     * Adding an expression whose lower bound exceeds its upper bound makes the model infeasible.
     */
    @Test
    void testExpressionInfeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Expression with no terms but contradictory bounds.
        model.newExpression("badExpr").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(2));
        assertTrue(model.isInfeasible(), "Model should be infeasible when an expression has lower > upper.");
    }

    /**
     * Once the internal infeasibility flag is set, subsequent calls stay true even after adding feasible items.
     */
    @Test
    void testFlagPersistsAfterInitialInfeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // First create an infeasible variable to set the flag.
        model.newVariable("v1").lower(BigDecimal.valueOf(7)).upper(BigDecimal.valueOf(4));
        assertTrue(model.isInfeasible(), "Initial infeasibility should be detected.");
        // Add a feasible variable and expression – the model must still report infeasible.
        model.newVariable("v2").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.newExpression("okExpr").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
        assertTrue(model.isInfeasible(), "After the flag is set, further modifications must not clear the infeasible state.");
    }

    /**
     * A model that becomes infeasible only after adding a second contradictory element still reports true.
     */
    @Test
    void testMultipleInfeasibilities() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // First a feasible variable.
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // Model is still feasible.
        assertFalse(model.isInfeasible(), "Model should be feasible before any contradictions.");
        // Add a contradictory expression.
        model.newExpression("expr").lower(BigDecimal.valueOf(5)).upper(BigDecimal.ONE);
        assertTrue(model.isInfeasible(), "Model should become infeasible after adding a bad expression.");
        // Add another contradictory variable – still infeasible.
        model.newVariable("y").lower(BigDecimal.TEN).upper(BigDecimal.ONE);
        assertTrue(model.isInfeasible(), "Model must remain infeasible after additional contradictory elements.");
    }

    /**
     * The cached {@code myInfeasible} flag short‑circuits the method.
     */
    @Test
    void testInitialFlagTrueShortCircuits() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Force the private flag to true via reflection.
        Field flagField = ExpressionsBasedModel.class.getDeclaredField("myInfeasible");
        flagField.setAccessible(true);
        flagField.setBoolean(model, true);
        // Even if we inject a bogus expression that would throw, the method must return true.
        // (We do not need to verify the throw – the flag short‑circuits before iteration.)
        assertTrue(model.isInfeasible(), "When myInfeasible is true the method must return true immediately.");
    }

    /**
     * When the flag is false and the model is empty the method returns false.
     */
    @Test
    void testInitialFlagFalseAndEmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isInfeasible(), "With a false flag and no expressions/variables the model must be reported feasible.");
    }

    /**
     * An expression that reports infeasibility makes the whole model infeasible.
     */
    @Test
    void testExpressionInfeasibilityDetected() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use the public API – a contradictory bound makes the expression infeasible.
        model.newExpression("e").lower(BigDecimal.valueOf(8)).upper(BigDecimal.valueOf(2));
        assertTrue(model.isInfeasible(), "Expression infeasibility should be detected and cause the model to be infeasible.");
    }

    /**
     * A feasible expression does not affect the result.
     */
    @Test
    void testExpressionFeasibilityDetected() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Feasible bounds (lower <= upper).
        model.newExpression("e").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        assertFalse(model.isInfeasible(), "A feasible expression should not make the model infeasible.");
    }

    /**
     * An infeasible variable makes the model infeasible.
     */
    @Test
    void testVariableInfeasibilityDetected() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").lower(BigDecimal.valueOf(4)).upper(BigDecimal.valueOf(1));
        assertTrue(model.isInfeasible(), "Variable infeasibility should be detected and cause the model to be infeasible.");
    }

    /**
     * A feasible variable does not affect the result.
     */
    @Test
    void testVariableFeasibilityDetected() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertFalse(model.isInfeasible(), "A feasible variable should not make the model infeasible.");
    }

    /**
     * Both an infeasible expression and variable – the method returns true after the first detection.
     */
    @Test
    void testCombinedInfeasibilityExpressionFirst() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Infeasible expression (checked first by the method).
        model.newExpression("badExpr").lower(BigDecimal.valueOf(9)).upper(BigDecimal.valueOf(1));
        // Infeasible variable (should never be inspected because the expression already fails).
        model.newVariable("badVar").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(2));
        assertTrue(model.isInfeasible(), "When both expression and variable are infeasible, the method must return true.");
    }

    /**
     * Both expression and variable are feasible – the model stays feasible.
     */
    @Test
    void testCombinedFeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newExpression("goodExpr").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20));
        model.newVariable("goodVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertFalse(model.isInfeasible(), "When both expression and variable are feasible the model must be feasible.");
    }

    /**
     * After the first detection the cached flag prevents further iteration.
     */
    @Test
    void testCachingPreventsSecondIteration() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Infeasible variable – first call will set the flag.
        model.newVariable("v").lower(BigDecimal.valueOf(6)).upper(BigDecimal.valueOf(1));
        // Verify first call detects infeasibility.
        assertTrue(model.isInfeasible());
        // Insert a mock expression that would throw if its isInfeasible() is called.
        Expression explodingExpr = mock(Expression.class);
        when(explodingExpr.isInfeasible()).thenThrow(new IllegalStateException("Should not be called"));
        // Inject via reflection.
        Field exprMapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprMapField.get(model);
        exprMap.put("exploding", explodingExpr);
        // Second call must return true without invoking the mock expression.
        assertTrue(model.isInfeasible(), "Second call should return true based on the cached flag and not invoke expressions.");
    }

    /**
     * Manual invocation of {@code setInfeasible()} forces the method to always return true.
     */
    @Test
    void testManualSetInfeasibleOverridesFeasibility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Model is feasible at this point.
        model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.newExpression("e").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
        assertFalse(model.isInfeasible());
        // Directly set the flag (package‑private method, accessible because we are in the same package).
        model.setInfeasible();
        // Even after adding feasible elements the method must still return true.
        model.newVariable("v2").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.newExpression("e2").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertTrue(model.isInfeasible(), "After setInfeasible() the model must stay infeasible regardless of its contents.");
    }

    @Test
    void testNullEntryInExpressionsMapThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Insert a null value under a dummy key.
        Field exprMapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprMapField.get(model);
        exprMap.put("nullExpr", null);
        assertThrows(NullPointerException.class, model::isInfeasible, "Iterating over a null expression entry must result in NPE.");
    }

    @Test
    void testNullEntryInVariablesListThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Insert a null variable into the internal list.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Variable> vars = (java.util.List<Variable>) varsField.get(model);
        vars.add(null);
        assertThrows(NullPointerException.class, model::isInfeasible, "Iterating over a null variable entry must result in NPE.");
    }

    @Test
    void testExpressionThrowsUncheckedExceptionPropagates() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Mock expression that throws IllegalStateException.
        Expression badExpr = mock(Expression.class);
        when(badExpr.isInfeasible()).thenThrow(new IllegalStateException("boom"));
        // Insert via reflection.
        Field exprMapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprMapField.get(model);
        exprMap.put("bad", badExpr);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, model::isInfeasible, "The unchecked exception from an expression must propagate.");
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    void testVariableThrowsUncheckedExceptionPropagates() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Mock variable that throws IllegalArgumentException.
        Variable badVar = mock(Variable.class);
        when(badVar.isInfeasible()).thenThrow(new IllegalArgumentException("bad var"));
        // Insert via reflection.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Variable> vars = (java.util.List<Variable>) varsField.get(model);
        vars.add(badVar);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, model::isInfeasible, "The unchecked exception from a variable must propagate.");
        assertEquals("bad var", thrown.getMessage());
    }

    @Test
    void testCorruptedExpressionsMapIsNull() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Nullify the map.
        Field exprMapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprMapField.setAccessible(true);
        exprMapField.set(model, null);
        assertThrows(NullPointerException.class, model::isInfeasible, "A null myExpressions reference must cause NPE when isInfeasible() is called.");
    }

    @Test
    void testCorruptedVariablesListIsNull() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Nullify the list.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        varsField.set(model, null);
        assertThrows(NullPointerException.class, model::isInfeasible, "A null myVariables reference must cause NPE when isInfeasible() is called.");
    }
}
