package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
// import static org.mockito.Mockito.*;
import org.ojalgo.function.constant.BigMath;
import static org.ojalgo.function.constant.BigMath.*;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_dispose_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the model is cleared even if a test fails early.
        if (model != null) {
            model.dispose();
        }
    }

    /* --------------------------------------------------------------
     *  Helper methods for reflection‑based manipulation.
     * -------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Replace a variable stored inside the model with a Mockito spy.
     */
    private void replaceVariableInModel(Variable original) throws Exception {
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) getPrivateField(model, "myVariables");
        int idx = vars.indexOf(original);
        Variable spy = spy(original);
        vars.set(idx, spy);
        // also replace the reference in the fixed‑variable set if present
        // No need to adjust the set; the spy still represents the same variable instance.
    }

    /**
     * Replace an expression stored inside the model with a Mockito spy.
     */
    private void replaceExpressionInModel(Expression original) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) getPrivateField(model, "myExpressions");
        // Find the key for the given expression.
        String key = null;
        for (Map.Entry<String, Expression> e : exprMap.entrySet()) {
            if (e.getValue() == original) {
                key = e.getKey();
                break;
            }
        }
        assertNotNull(key, "original expression should be present in the map");
        Expression spy = spy(original);
        exprMap.put(key, spy);
    }

    /**
     * Verify that {@code dispose()} removes all expressions, variables, fixed‑variable
     * information and resets the internal categorisation.
     */
    @Test
    void testDisposeClearsAllModelData() {
        // --- given: a model with variables, expressions and a fixed variable ---
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ONE);
        Variable y = model.addVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ONE);
        // make x a fixed variable
        x.fix(BigDecimal.valueOf(5));
        Expression expr = model.addExpression("expr");
        expr.set(x, BigMath.ONE);
        expr.set(y, BigMath.ONE);
        expr.upper(BigDecimal.valueOf(12));
        // sanity checks before disposal
        assertEquals(2, model.countVariables(), "pre‑condition: two variables expected");
        assertEquals(1, model.countExpressions(), "pre‑condition: one expression expected");
        assertFalse(model.getFixedVariables().isEmpty(), "pre‑condition: there should be a fixed variable");
        // --- when: dispose is invoked ---
        model.dispose();
        // --- then: all collections must be empty and categorisation reset ---
        assertEquals(0, model.countVariables(), "variables should be cleared after dispose");
        assertEquals(0, model.countExpressions(), "expressions should be cleared after dispose");
        assertTrue(model.getFixedVariables().isEmpty(), "fixed‑variable set should be empty after dispose");
        // The categorisation reset is indirectly verified by the fact that the model now reports
        // zero free/integer/positive/negative variables.
        assertTrue(model.getFreeVariables().isEmpty(), "free variables list should be empty after dispose");
        assertTrue(model.getIntegerVariables().isEmpty(), "integer variables list should be empty after dispose");
        assertTrue(model.getPositiveVariables().isEmpty(), "positive variables list should be empty after dispose");
        assertTrue(model.getNegativeVariables().isEmpty(), "negative variables list should be empty after dispose");
    }

    /**
     * Verify that calling {@code dispose()} more than once does not throw and leaves the model
     * in a cleared state.
     */
    @Test
    void testDisposeIsIdempotent() {
        // Populate the model minimally.
        model.addVariable("z");
        model.addExpression("e").level(BigDecimal.ZERO);
        // First dispose
        model.dispose();
        // Second dispose – should be safe and leave the model empty.
        assertDoesNotThrow(() -> model.dispose(), "second dispose should not throw");
        // Verify still empty.
        assertEquals(0, model.countVariables(), "variables should remain cleared after second dispose");
        assertEquals(0, model.countExpressions(), "expressions should remain cleared after second dispose");
    }

    /* --------------------------------------------------------------
     *  Verify that destroy() is called on every expression and variable.
     * -------------------------------------------------------------- */
    @Test
    void testDisposeCallsDestroyOnExpressionsAndVariables() throws Exception {
        // Create real objects, then replace them with spies so we can verify destroy().
        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Expression e1 = model.addExpression("e1");
        Expression e2 = model.addExpression("e2");
        // Replace the stored objects with Mockito spies.
        replaceVariableInModel(v1);
        replaceVariableInModel(v2);
        replaceExpressionInModel(e1);
        replaceExpressionInModel(e2);
        // Execute dispose.
        model.dispose();
        // Verify that destroy() was invoked exactly once on each spy.
        verify(v1, times(1)).destroy();
        verify(v2, times(1)).destroy();
        verify(e1, times(1)).destroy();
        verify(e2, times(1)).destroy();
    }

    /* --------------------------------------------------------------
     *  Edge case: dispose on an already empty model.
     * -------------------------------------------------------------- */
    @Test
    void testDisposeOnEmptyModelNoException() {
        // The model is freshly created and therefore empty.
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        // Should complete without throwing.
        assertDoesNotThrow(() -> model.dispose(), "dispose on empty model must not throw");
    }

    /* --------------------------------------------------------------
     *  Exception handling – expression.destroy() throws.
     * -------------------------------------------------------------- */
    @Test
    void testDisposePropagatesExceptionFromExpressionDestroy() throws Exception {
        // Mock an expression that throws when destroy() is called.
        Expression badExpr = mock(Expression.class);
        doThrow(new RuntimeException("boom")).when(badExpr).destroy();
        // Inject the mock directly into the private myExpressions map.
        setPrivateField(model, "myExpressions", new HashMap<String, Expression>() {

            {
                put("bad", badExpr);
            }
        });
        // Ensure there are no variables – we want the exception to arise from the expression loop.
        setPrivateField(model, "myVariables", new ArrayList<Variable>());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.dispose(), "dispose should propagate RuntimeException from expression.destroy()");
        assertEquals("boom", thrown.getMessage());
        // After the exception the map must still contain the entry (cleanup did not happen).
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) getPrivateField(model, "myExpressions");
        assertEquals(1, exprMap.size(), "expressions map should remain unchanged after exception");
    }

    /* --------------------------------------------------------------
     *  Exception handling – variable.destroy() throws.
     * -------------------------------------------------------------- */
    @Test
    void testDisposePropagatesExceptionFromVariableDestroy() throws Exception {
        // Mock a variable that throws when destroy() is called.
        Variable badVar = mock(Variable.class);
        doThrow(new RuntimeException("varBoom")).when(badVar).destroy();
        // Ensure there are no expressions.
        setPrivateField(model, "myExpressions", new HashMap<String, Expression>());
        // Inject the mock variable into the private myVariables list.
        setPrivateField(model, "myVariables", new ArrayList<Variable>() {

            {
                add(badVar);
            }
        });
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.dispose(), "dispose should propagate RuntimeException from variable.destroy()");
        assertEquals("varBoom", thrown.getMessage());
        // After the exception the variable list must still contain the mock.
        @SuppressWarnings("unchecked")
        List<Variable> varList = (List<Variable>) getPrivateField(model, "myVariables");
        assertEquals(1, varList.size(), "variables list should remain unchanged after exception");
    }

    /* --------------------------------------------------------------
     *  Model reuse after disposal.
     * -------------------------------------------------------------- */
    @Test
    void testModelCanBeReusedAfterDispose() {
        // Populate and dispose.
        model.addVariable("a");
        model.addExpression("expr").level(BigDecimal.ONE);
        model.dispose();
        // Re‑use the same instance.
        Variable v = model.addVariable("newVar");
        Expression e = model.addExpression("newExpr");
        e.set(v, BigMath.ONE);
        // Verify that the newly added objects are present.
        assertEquals(1, model.countVariables(), "one variable should be present after reuse");
        assertEquals(1, model.countExpressions(), "one expression should be present after reuse");
        assertSame(v, model.getVariable(0), "retrieved variable should match the added one");
        assertEquals(e, model.getExpression("newExpr"), "retrieved expression should match the added one");
    }

    /* --------------------------------------------------------------
     *  Fixed‑variable set cleared.
     * -------------------------------------------------------------- */
    @Test
    void testFixedVariableSetCleared() {
        Variable v = model.addVariable("fixed");
        v.fix(BigDecimal.valueOf(3));
        assertFalse(model.getFixedVariables().isEmpty(), "pre‑condition: fixed set must contain an entry");
        model.dispose();
        assertTrue(model.getFixedVariables().isEmpty(), "fixed‑variable set should be empty after dispose");
    }

    /* --------------------------------------------------------------
     *  VariablesCategorisation internal arrays are null after dispose.
     * -------------------------------------------------------------- */
    @Test
    void testVariablesCategorisationResetViaReflection() throws Exception {
        // Add variables to force categorisation to compute its index arrays.
        model.addVariable("v1");
        model.addVariable("v2");
        // Ensure categorisation has been populated.
        assertFalse(model.getFreeVariables().isEmpty());
        // Dispose the model.
        model.dispose();
        // Access the private VariablesCategorisation instance.
        Field catField = ExpressionsBasedModel.class.getDeclaredField("myVariablesCategorisation");
        catField.setAccessible(true);
        Object categorisation = catField.get(model);
        // Verify that all index arrays are null.
        for (String fieldName : new String[] { "myFreeIndices", "myPositiveIndices", "myNegativeIndices", "myIntegerIndices" }) {
            Field f = VariablesCategorisation.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            assertNull(f.get(categorisation), fieldName + " should be null after dispose");
        }
        // Verify that the internal variable lists are empty.
        for (String listName : new String[] { "myFreeVariables", "myPositiveVariables", "myNegativeVariables", "myIntegerVariables" }) {
            Field f = VariablesCategorisation.class.getDeclaredField(listName);
            f.setAccessible(true);
            List<?> list = (List<?>) f.get(categorisation);
            assertTrue(list.isEmpty(), listName + " should be empty after dispose");
        }
    }

    /* --------------------------------------------------------------
     *  Null expression handling – NPE should be propagated.
     * -------------------------------------------------------------- */
    @Test
    void testDisposeThrowsWhenExpressionIsNull() throws Exception {
        // Create a map with a null value.
        Map<String, Expression> mapWithNull = new HashMap<>();
        mapWithNull.put("nullExpr", null);
        setPrivateField(model, "myExpressions", mapWithNull);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> model.dispose(), "dispose should throw NPE when an expression entry is null");
        // Ensure that the variable collection is untouched (still empty).
        @SuppressWarnings("unchecked")
        List<Variable> varList = (List<Variable>) getPrivateField(model, "myVariables");
        assertTrue(varList.isEmpty(), "variables should remain untouched after NPE");
    }

    /* --------------------------------------------------------------
     *  Null variable handling – NPE should be propagated.
     * -------------------------------------------------------------- */
    @Test
    void testDisposeThrowsWhenVariableIsNull() throws Exception {
        // Create a list with a null element.
        List<Variable> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        setPrivateField(model, "myVariables", listWithNull);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> model.dispose(), "dispose should throw NPE when a variable entry is null");
        // Ensure that the expressions map is untouched (still empty).
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) getPrivateField(model, "myExpressions");
        assertTrue(exprMap.isEmpty(), "expressions should remain untouched after NPE");
    }
}