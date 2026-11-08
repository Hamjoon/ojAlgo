package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.ojalgo.function.constant.BigMath;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
// import static org.mockito.Mockito.*;
// import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_dispose_Test {

    private ExpressionsBasedModel model;

    //     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test.
        model = new ExpressionsBasedModel();
        // Populate it with a few variables and an expression so that we have
        // something to dispose.
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        Variable y = model.newVariable("y").lower(0).upper(5).weight(1);
        Expression expr = model.newExpression("expr").lower(5).upper(5);
        expr.set(x, BigMath.ONE);
        expr.set(y, BigMath.TWO);
        // Do NOT replace the model with a new empty instance – we want the
        // populated model for the tests.
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Ensure the model reference is cleared after each test.
        model = null;
    }

    /**
     * Inserts a mock {@link Expression} into the model's private {@code myExpressions}
     * map under the supplied key.
     */
    private void injectExpression(String key, Expression mockExpression) throws Exception {
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprField.get(model);
        exprMap.put(key, mockExpression);
    }

    /**
     * Adds a mock {@link Variable} to the model's private {@code myVariables} list.
     */
    private void injectVariable(Variable mockVariable) throws Exception {
        Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> varList = (List<Variable>) varField.get(model);
        varList.add(mockVariable);
    }

    @Test
    void testDisposeClearsAllInternalCollections() {
        // Pre‑condition checks – the model must contain data before disposal.
        assertTrue(model.countVariables() > 0, "Model should contain variables before dispose");
        assertTrue(model.countExpressions() > 0, "Model should contain expressions before dispose");
        assertFalse(model.getExpressions().isEmpty(), "Expressions collection must not be empty");
        assertTrue(model.getFixedVariables().isEmpty(), "Fixed‑variable set should be empty initially (no fixed vars added)");
        // Act
        model.dispose();
        // ----- Assertions after dispose -----
        // All public counters must report zero.
        assertEquals(0, model.countVariables(), "Variable count must be zero after dispose");
        assertEquals(0, model.countExpressions(), "Expression count must be zero after dispose");
        // Collections exposed via public API must be empty.
        assertTrue(model.getExpressions().isEmpty(), "Expression map must be empty after dispose");
        assertTrue(model.getFixedVariables().isEmpty(), "Fixed‑variable set must be empty after dispose");
        // The stream based APIs must also yield no elements.
        Stream<Variable> variableStream = model.variables();
        assertEquals(0L, variableStream.count(), "Variable stream must be empty after dispose");
        // After a reset, asking for free/integer/positive/negative lists should return empty.
        assertTrue(model.getFreeVariables().isEmpty(), "Free variable list must be empty after dispose");
        assertTrue(model.getIntegerVariables().isEmpty(), "Integer variable list must be empty after dispose");
        assertTrue(model.getPositiveVariables().isEmpty(), "Positive variable list must be empty after dispose");
        assertTrue(model.getNegativeVariables().isEmpty(), "Negative variable list must be empty after dispose");
        // The model should still be usable – adding a new variable must succeed.
        Variable z = model.newVariable("z").lower(0).upper(1).weight(1);
        assertNotNull(z, "Model must allow adding new variables after dispose");
        assertEquals(1, model.countVariables(), "Variable count should reflect the newly added variable");
    }

    /* -------------------------------------------------------------
     * 1. Dispose on an empty model
     * ------------------------------------------------------------- */
    @Test
    void testDisposeOnEmptyModel() {
        // The model is freshly created – it contains no variables or expressions.
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        // Act
        assertDoesNotThrow(() -> model.dispose(), "dispose() on an empty model should not throw");
        // Verify that all collections are still empty.
        assertTrue(model.getExpressions().isEmpty(), "Expressions map must be empty");
        assertTrue(model.getFixedVariables().isEmpty(), "Fixed‑variable set must be empty");
        assertEquals(0, model.countVariables(), "Variable count must be zero");
        assertEquals(0, model.countExpressions(), "Expression count must be zero");
        assertTrue(model.getFreeVariables().isEmpty(), "Free variable list must be empty");
        assertTrue(model.getIntegerVariables().isEmpty(), "Integer variable list must be empty");
        assertTrue(model.getPositiveVariables().isEmpty(), "Positive variable list must be empty");
        assertTrue(model.getNegativeVariables().isEmpty(), "Negative variable list must be empty");
    }

    /* -------------------------------------------------------------
     * 2. Dispose on a populated model – verify destroy() calls
     * ------------------------------------------------------------- */
    @Test
    void testDisposeOnPopulatedModelCallsDestroy() throws Exception {
        // Create mock Expression and Variable instances.
        Expression mockExpr1 = mock(Expression.class);
        Expression mockExpr2 = mock(Expression.class);
        Variable mockVar1 = mock(Variable.class);
        Variable mockVar2 = mock(Variable.class);
        // Insert the mocks into the private collections via reflection.
        injectExpression("e1", mockExpr1);
        injectExpression("e2", mockExpr2);
        injectVariable(mockVar1);
        injectVariable(mockVar2);
        // Sanity check – the model now reports the expected sizes.
        assertEquals(2, model.countExpressions());
        assertEquals(2, model.countVariables());
        // Act
        model.dispose();
        // Verify destroy() was called exactly once on each mock.
        verify(mockExpr1, times(1)).destroy();
        verify(mockExpr2, times(1)).destroy();
        verify(mockVar1, times(1)).destroy();
        verify(mockVar2, times(1)).destroy();
        // All internal collections must be cleared.
        assertTrue(model.getExpressions().isEmpty(), "Expressions map must be empty after dispose");
        assertTrue(model.getFixedVariables().isEmpty(), "Fixed‑variable set must be empty after dispose");
        assertEquals(0, model.countVariables(), "Variable count must be zero after dispose");
        assertEquals(0, model.countExpressions(), "Expression count must be zero after dispose");
    }

    /* -------------------------------------------------------------
     * 3. Dispose idempotency – second call does not re‑invoke destroy()
     * ------------------------------------------------------------- */
    @Test
    void testDisposeIdempotent() throws Exception {
        // Insert a real expression and variable via reflection to simulate existing entries.
        Expression expr = model.newExpression("tempExpr");
        Variable var = model.newVariable("tempVar");
        injectExpression("temp", expr);
        injectVariable(var);
        // First dispose – should clear the model.
        model.dispose();
        // Second dispose – should not throw and must leave the model in a cleared state.
        assertDoesNotThrow(() -> model.dispose(), "Second dispose() call must not throw");
        // Verify that the model remains empty.
        assertTrue(model.getExpressions().isEmpty(), "Expressions map must be empty after second dispose");
        assertEquals(0, model.countVariables(), "Variable count must be zero after second dispose");
        assertEquals(0, model.countExpressions(), "Expression count must be zero after second dispose");
    }

    /* -------------------------------------------------------------
     * 4. All public query methods reflect cleared state
     * ------------------------------------------------------------- */
    @Test
    void testQueryMethodsAfterDispose() {
        // Populate with real objects (no mocking needed for this test).
        model.newVariable("a").lower(0).upper(10);
        model.newVariable("b").lower(0).upper(5);
        model.newExpression("c").lower(1).upper(1);
        // Ensure the model is not empty before disposal.
        assertTrue(model.countVariables() > 0);
        assertTrue(model.countExpressions() > 0);
        // Dispose.
        model.dispose();
        // Verify counters.
        assertEquals(0, model.countVariables(), "Variable count must be zero");
        assertEquals(0, model.countExpressions(), "Expression count must be zero");
        // Verify collection‑based getters.
        assertTrue(model.getExpressions().isEmpty(), "Expressions collection must be empty");
        assertTrue(model.getFixedVariables().isEmpty(), "Fixed‑variable set must be empty");
        assertTrue(model.getFreeVariables().isEmpty(), "Free variable list must be empty");
        assertTrue(model.getIntegerVariables().isEmpty(), "Integer variable list must be empty");
        assertTrue(model.getPositiveVariables().isEmpty(), "Positive variable list must be empty");
        assertTrue(model.getNegativeVariables().isEmpty(), "Negative variable list must be empty");
        // Stream based API must yield no elements.
        assertEquals(0L, model.variables().count(), "Variable stream must be empty");
    }

    /* -------------------------------------------------------------
     * 5. VariablesCategorisation reset (indirect verification)
     * ------------------------------------------------------------- */
    @Test
    void testVariablesCategorisationReset() {
        // Add variables with different characteristics.
        // positive
        model.newVariable("pos").lower(0).upper(10);
        // negative
        model.newVariable("neg").lower(-10).upper(0);
        // integer
        model.newVariable("int").integer(true).lower(0).upper(5);
        // Force categorisation to be built.
        assertFalse(model.getFreeVariables().isEmpty());
        // Dispose clears the categorisation.
        model.dispose();
        // After reset, all categorisation getters must return empty lists.
        assertTrue(model.getFreeVariables().isEmpty(), "Free variables list must be empty after reset");
        assertTrue(model.getPositiveVariables().isEmpty(), "Positive variables list must be empty after reset");
        assertTrue(model.getNegativeVariables().isEmpty(), "Negative variables list must be empty after reset");
        assertTrue(model.getIntegerVariables().isEmpty(), "Integer variables list must be empty after reset");
    }

    /* -------------------------------------------------------------
     * 6. Model remains usable after dispose
     * ------------------------------------------------------------- */
    @Test
    void testModelUsableAfterDispose() {
        // Populate and dispose.
        model.newVariable("x").lower(0).upper(1);
        model.dispose();
        // Adding new entities must succeed.
        Variable v = model.newVariable("newVar").lower(0).upper(2);
        Expression e = model.newExpression("newExpr").lower(0).upper(10);
        // Use the variable in the expression
        e.set(v, BigMath.ONE);
        // Verify counts reflect the newly added objects.
        assertEquals(1, model.countVariables(), "One variable should exist after reuse");
        assertEquals(1, model.countExpressions(), "One expression should exist after reuse");
    }

    /* -------------------------------------------------------------
     * 7. Dispose with a null expression entry – expect NPE
     * ------------------------------------------------------------- */
    @Test
    void testDisposeWithNullExpressionEntryThrowsNPE() throws Exception {
        // Insert a null value into the private myExpressions map.
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprField.get(model);
        exprMap.put("nullEntry", null);
        // The dispose implementation iterates over values and calls destroy(),
        // which should result in a NullPointerException.
        assertThrows(NullPointerException.class, () -> model.dispose(), "Dispose should throw NPE when a null expression is present");
    }
}