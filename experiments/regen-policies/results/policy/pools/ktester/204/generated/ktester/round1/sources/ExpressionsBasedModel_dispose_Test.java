import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Variable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_dispose_Test {

    private ExpressionsBasedModel model;

    class BadExpression extends Expression {

        BadExpression(String name) {
            super(name);
        }

        @Override
        public void destroy() {
            throw new RuntimeException("boom from Expression.destroy()");
        }
    }

    class BadVariable extends Variable {

        BadVariable() {
            super();
        }

        @Override
        public void destroy() {
            throw new RuntimeException("boom from Variable.destroy()");
        }
    }

    class ModifyingExpression extends Expression {

        private final ExpressionsBasedModel owner;

        ModifyingExpression(String name, ExpressionsBasedModel owner) {
            super(name);
            this.owner = owner;
        }

        @Override
        public void destroy() {
            // remove itself from the model – this will cause ConcurrentModificationException
            owner.removeExpression(this.getName());
        }
    }

    class ModifyingVariable extends Variable {

        private final ExpressionsBasedModel owner;

        ModifyingVariable(ExpressionsBasedModel owner) {
            super();
            this.owner = owner;
        }

        @Override
        public void destroy() {
            // remove itself from the variable list
            List<Variable> list = getField(owner, "myVariables");
            list.remove(this);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Sets the (normally private) field value of {@code target}.
     */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that {@code dispose()} clears all internal collections and resets the categorisation.
     */
    @Test
    void testDisposeClearsModelState() {
        // ----- GIVEN: a model with variables and an expression -----
        Variable freeVar = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable fixedVar = model.addVariable().lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        Expression expr = model.addExpression().lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
        expr.set(freeVar, BigDecimal.ONE);
        expr.set(fixedVar, BigDecimal.ONE);
        // sanity checks before disposal
        assertEquals(2, model.countVariables(), "Model should contain two variables before dispose");
        assertEquals(1, model.countExpressions(), "Model should contain one expression before dispose");
        assertFalse(model.getFixedVariables().isEmpty(), "There should be at least one fixed variable before dispose");
        assertFalse(model.getFreeVariables().isEmpty(), "Free variables list should not be empty before dispose");
        // ----- WHEN: disposing the model -----
        model.dispose();
        // ----- THEN: the model is cleared -----
        assertEquals(0, model.countVariables(), "All variables should be removed after dispose");
        assertEquals(0, model.countExpressions(), "All expressions should be removed after dispose");
        assertTrue(model.getFixedVariables().isEmpty(), "Fixed variables set should be empty after dispose");
        assertTrue(model.getFreeVariables().isEmpty(), "Free variables list should be empty after dispose");
        // Verify internal collections are empty via reflection
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = getField(model, "myExpressions");
        assertTrue(exprMap.isEmpty(), "Expression map should be empty after dispose");
        @SuppressWarnings("unchecked")
        List<Variable> varList = getField(model, "myVariables");
        assertTrue(varList.isEmpty(), "Variable list should be empty after dispose");
        @SuppressWarnings("unchecked")
        Set<?> fixedSet = getField(model, "myFixedVariables");
        assertTrue(fixedSet.isEmpty(), "Fixed‑variable set should be empty after dispose");
        // Adding a new variable after dispose should work without throwing.
        Variable newVar = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertEquals(1, model.countVariables(), "Model should accept new variables after dispose");
        assertSame(newVar, model.getVariable(0), "The newly added variable should be retrievable");
    }

    /**
     * Returns the (normally private) field value of {@code target}.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDisposeWithPopulatedModel() {
        // ----- GIVEN -------------------------------------------------------
        // two variables
        Variable v0 = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable v1 = model.addVariable().lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // two named expressions
        Expression eA = model.addExpression("A").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100));
        Expression eB = model.addExpression("B").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(200));
        // make sure expressions reference the variables (not strictly needed for dispose)
        eA.set(v0, BigDecimal.ONE);
        eA.set(v1, BigDecimal.ONE);
        eB.set(v0, BigDecimal.ONE);
        // Replace the real objects with Mockito spies so we can verify destroy()
        Expression spyA = spy(eA);
        Expression spyB = spy(eB);
        Variable spyV0 = spy(v0);
        Variable spyV1 = spy(v1);
        // inject spies via reflection
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = getField(model, "myExpressions");
        exprMap.put("A", spyA);
        exprMap.put("B", spyB);
        @SuppressWarnings("unchecked")
        List<Variable> varList = getField(model, "myVariables");
        varList.set(0, spyV0);
        varList.set(1, spyV1);
        // sanity checks before disposal
        assertEquals(2, model.countVariables());
        assertEquals(2, model.countExpressions());
        assertFalse(model.getFixedVariables().isEmpty());
        assertFalse(model.getFreeVariables().isEmpty());
        // ----- WHEN --------------------------------------------------------
        model.dispose();
        // ----- THEN --------------------------------------------------------
        // verify destroy() was called exactly once on each spy
        verify(spyA, times(1)).destroy();
        verify(spyB, times(1)).destroy();
        verify(spyV0, times(1)).destroy();
        verify(spyV1, times(1)).destroy();
        // internal collections must be empty
        assertTrue(getField(model, "myExpressions").isEmpty(), "Expression map should be empty after dispose");
        assertTrue(getField(model, "myVariables").isEmpty(), "Variable list should be empty after dispose");
        assertTrue(getField(model, "myFixedVariables").isEmpty(), "Fixed‑variable set should be empty after dispose");
        // categorisation must be reset (all internal lists cleared)
        ExpressionsBasedModel.VariablesCategorisation cat = getField(model, "myVariablesCategorisation");
        assertTrue(cat.getFreeVariables(new ArrayList<>()).isEmpty(), "Free‑variables list in categorisation must be empty");
        assertTrue(cat.getIntegerVariables(new ArrayList<>()).isEmpty(), "Integer‑variables list in categorisation must be empty");
    }

    @Test
    void testDisposeOnEmptyModel() {
        // Model is freshly created – no variables / expressions
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        // should not throw
        assertDoesNotThrow(() -> model.dispose());
        // still empty after call
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = getField(model, "myExpressions");
        assertTrue(exprMap.isEmpty(), "Expression map should stay empty");
        @SuppressWarnings("unchecked")
        List<Variable> varList = getField(model, "myVariables");
        assertTrue(varList.isEmpty(), "Variable list should stay empty");
        @SuppressWarnings("unchecked")
        Set<?> fixedSet = getField(model, "myFixedVariables");
        assertTrue(fixedSet.isEmpty(), "Fixed‑variable set should stay empty");
    }

    @Test
    void testDisposeWhenExpressionDestroyThrows() {
        // ----- GIVEN -------------------------------------------------------
        BadExpression bad = new BadExpression("bad");
        // inject into model's map via reflection
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = getField(model, "myExpressions");
        exprMap.put("bad", bad);
        // add a normal variable so the model is not completely empty
        Variable var = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // keep a snapshot of counts before dispose
        int varCountBefore = model.countVariables();
        // ----- WHEN & THEN -------------------------------------------------
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.dispose(), "dispose() should propagate the RuntimeException from Expression.destroy()");
        assertEquals("boom from Expression.destroy()", thrown.getMessage());
        // after exception, expressions map may be partially cleared (bad expression already removed)
        assertFalse(exprMap.containsKey("bad"), "Expression that threw should have been removed");
        // variables and fixed‑set must remain untouched
        assertEquals(varCountBefore, model.countVariables(), "Variable list must remain unchanged after exception");
        assertTrue(getField(model, "myFixedVariables").isEmpty(), "Fixed‑variable set must remain unchanged");
        // categorisation should NOT have been reset because the method exited early
        ExpressionsBasedModel.VariablesCategorisation cat = getField(model, "myVariablesCategorisation");
        // there is at least one free variable (the one we added)
        assertFalse(cat.getFreeVariables(new ArrayList<>()).isEmpty(), "Categorisation must not be reset after exception");
    }

    @Test
    void testDisposeWhenVariableDestroyThrows() {
        // ----- GIVEN -------------------------------------------------------
        BadVariable badVar = new BadVariable();
        // inject into model's variable list via reflection (position 0)
        @SuppressWarnings("unchecked")
        List<Variable> varList = getField(model, "myVariables");
        varList.add(badVar);
        // also add a normal expression so the first part of dispose runs
        model.addExpression().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // snapshot counts
        int exprCountBefore = model.countExpressions();
        // ----- WHEN & THEN -------------------------------------------------
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.dispose(), "dispose() should propagate the RuntimeException from Variable.destroy()");
        assertEquals("boom from Variable.destroy()", thrown.getMessage());
        // Expressions should have been destroyed and cleared
        assertEquals(0, model.countExpressions(), "Expressions must be cleared even when a Variable throws");
        // Variable list may be partially cleared – at least the bad variable remains
        @SuppressWarnings("unchecked")
        List<Variable> afterVarList = getField(model, "myVariables");
        assertTrue(afterVarList.contains(badVar), "Bad variable should still be present because destroy() failed");
        // Fixed‑variables set unchanged
        assertTrue(getField(model, "myFixedVariables").isEmpty());
        // Categorisation should have been reset because the method reached that point
        ExpressionsBasedModel.VariablesCategorisation cat = getField(model, "myVariablesCategorisation");
        assertTrue(cat.getFreeVariables(new ArrayList<>()).isEmpty(), "Categorisation must be reset even after Variable.destroy() exception");
    }

    @Test
    void testDisposeWhenExpressionDestroyModifiesModel() {
        // ----- GIVEN -------------------------------------------------------
        ModifyingExpression expr = new ModifyingExpression("mod", model);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = getField(model, "myExpressions");
        exprMap.put("mod", expr);
        // ----- WHEN & THEN -------------------------------------------------
        assertThrows(ConcurrentModificationException.class, () -> model.dispose(), "Modifying the expression map during iteration must cause CME");
    }

    @Test
    void testDisposeWhenVariableDestroyModifiesModel() {
        // ----- GIVEN -------------------------------------------------------
        ModifyingVariable var = new ModifyingVariable(model);
        @SuppressWarnings("unchecked")
        List<Variable> varList = getField(model, "myVariables");
        varList.add(var);
        // also add a dummy expression so the first loop runs
        model.addExpression().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // ----- WHEN & THEN -------------------------------------------------
        assertThrows(ConcurrentModificationException.class, () -> model.dispose(), "Modifying the variable list during iteration must cause CME");
    }

    @Test
    void testDisposeWhenExpressionMapContainsNull() throws Exception {
        // ----- GIVEN -------------------------------------------------------
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = getField(model, "myExpressions");
        // deliberately insert null
        exprMap.put("nullExpr", null);
        // ----- WHEN & THEN -------------------------------------------------
        assertThrows(NullPointerException.class, () -> model.dispose(), "Calling destroy() on a null expression must throw NPE");
    }

    @Test
    void testDisposeWhenVariableListContainsNull() throws Exception {
        // ----- GIVEN -------------------------------------------------------
        @SuppressWarnings("unchecked")
        List<Variable> varList = getField(model, "myVariables");
        // deliberately insert null
        varList.add(null);
        // ----- WHEN & THEN -------------------------------------------------
        assertThrows(NullPointerException.class, () -> model.dispose(), "Calling destroy() on a null variable must throw NPE");
    }
}
