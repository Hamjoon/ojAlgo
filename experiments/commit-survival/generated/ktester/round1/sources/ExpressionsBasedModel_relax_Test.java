package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.ProgrammingError;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_relax_Test {

    private ExpressionsBasedModel model;

    /**
     * Initialise a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    /**
     * Helper to invoke the package‑private {@code relax(boolean)} via reflection.
     */
    private void invokeRelaxSoft(boolean soft) throws Exception {
        Method m = ExpressionsBasedModel.class.getDeclaredMethod("relax", boolean.class);
        m.setAccessible(true);
        m.invoke(model, soft);
    }

    /**
     * Parameter source for models that are already relaxed.
     */
    static Stream<ExpressionsBasedModel> alreadyRelaxedModels() {
        ExpressionsBasedModel m1 = new ExpressionsBasedModel();
        // default (soft = false)
        m1.relax();
        ExpressionsBasedModel m2 = new ExpressionsBasedModel();
        // continuous variables only
        m2.newVariable("a").lower(0).upper(1);
        m2.relax();
        return Stream.of(m1, m2);
    }

    /**
     * Verify that calling {@code relax()} flips the relaxed flag.
     */
    @Test
    void testRelaxSetsRelaxedFlag() {
        // pre‑condition: model is not relaxed
        assertFalse(model.isRelaxed(), "Model should start in a non‑relaxed state");
        // invoke the method under test
        model.relax();
        // post‑condition: relaxed flag must be true
        assertTrue(model.isRelaxed(), "Model should be marked as relaxed after calling relax()");
    }

    /**
     * Ensure that the relaxation does not change the number of variables or expressions.
     */
    @Test
    void testRelaxDoesNotAlterStructure() {
        // build a tiny model
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // simple constraint
        model.newExpression("c1").set(x, 1).level(5);
        int varsBefore = model.countVariables();
        int exprsBefore = model.countExpressions();
        // relax the model
        model.relax();
        // verify structural invariants
        assertEquals(varsBefore, model.countVariables(), "Number of variables must stay the same after relaxation");
        assertEquals(exprsBefore, model.countExpressions(), "Number of expressions must stay the same after relaxation");
    }

    /**
     * A basic optimisation after relaxation should still succeed.
     */
    @Test
    void testOptimisationAfterRelax() {
        // integer variable (the classic use‑case for relaxation)
        Variable i = model.newVariable("i").lower(0).upper(5).integer(true).weight(1);
        // minimise the objective (simply the variable itself)
        Optimisation.Result result = model.minimise();
        // the model is feasible and the result is optimal
        assertTrue(model.validate(result), "Result should validate against the model");
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        // after relaxation the same optimum is expected (0 in this case)
        model.relax();
        Optimisation.Result relaxedResult = model.minimise();
        assertTrue(model.validate(relaxedResult), "Relaxed result should validate");
        assertTrue(relaxedResult.getState().isOptimal(), "Relaxed result should be optimal");
        assertEquals(result.getValue(), relaxedResult.getValue(), "Relaxed optimisation should yield the same objective value");
    }

    /**
     * Group: relax() default (soft = false) – first call with integer variable.
     */
    @Test
    void testRelaxDefaultConvertsIntegerVariables() {
        // create a model with a single integer variable and a simple constraint
        Variable iv = model.newVariable("iv").lower(0).upper(3).integer(true).weight(1);
        // 0 ≤ iv ≤ 3 and iv = 2 (feasible)
        model.newExpression("c").set(iv, 1).level(2);
        assertTrue(model.isAnyVariableInteger(), "Pre‑condition: model must contain at least one integer variable");
        // act
        // delegates to relax(false)
        model.relax();
        // assert
        assertTrue(model.isRelaxed(), "Model should be marked as relaxed");
        assertFalse(model.isAnyVariableInteger(), "All integer variables must have been converted to continuous");
        // variable count must stay the same
        assertEquals(1, model.countVariables(), "Variable count must not change after relaxation");
    }

    /**
     * Group: relax() default (soft = false) – second (idempotent) call.
     */
    @Test
    void testRelaxDefaultIdempotent() {
        // first relaxation
        model.relax();
        assertTrue(model.isRelaxed(), "Model should be relaxed after first call");
        // capture state
        int varsBefore = model.countVariables();
        int exprsBefore = model.countExpressions();
        // second call – should be a no‑op and not throw
        assertDoesNotThrow(() -> model.relax(), "Second call to relax() must not throw any exception");
        // state must be unchanged
        assertTrue(model.isRelaxed(), "Relaxed flag must stay true");
        assertEquals(varsBefore, model.countVariables(), "Variable count must stay unchanged on second relax()");
        assertEquals(exprsBefore, model.countExpressions(), "Expression count must stay unchanged on second relax()");
    }

    /**
     * Group: relax() on model without integer variables – first call.
     */
    @Test
    void testRelaxOnContinuousModel() {
        // create a model with only continuous variables
        Variable v1 = model.newVariable("v1").lower(0).upper(5).weight(2);
        Variable v2 = model.newVariable("v2").lower(-3).upper(3).weight(-1);
        model.newExpression("c1").set(v1, 1).set(v2, 1).upper(4);
        assertFalse(model.isAnyVariableInteger(), "Pre‑condition: model must not contain integer variables");
        // act
        model.relax();
        // assert
        assertTrue(model.isRelaxed(), "Model should be marked as relaxed");
        assertFalse(model.isAnyVariableInteger(), "Model should still report no integer variables after relaxation");
        assertEquals(2, model.countVariables(), "Variable count must remain the same");
    }

    /**
     * Group: relax() on model without integer variables – second (idempotent) call.
     */
    @Test
    void testRelaxOnContinuousModelIdempotent() {
        // first call
        model.relax();
        assertTrue(model.isRelaxed());
        int varsBefore = model.countVariables();
        int exprsBefore = model.countExpressions();
        // second call
        assertDoesNotThrow(() -> model.relax(), "Second relax() on an already‑relaxed continuous model must not throw");
        assertTrue(model.isRelaxed());
        assertEquals(varsBefore, model.countVariables(), "Variable count unchanged after second relax()");
        assertEquals(exprsBefore, model.countExpressions(), "Expression count unchanged after second relax()");
    }

    /**
     * Group: relax() after dispose – expect ProgrammingError.
     */
    @Test
    void testRelaxAfterDisposeThrowsProgrammingError() {
        // release resources
        model.dispose();
        ProgrammingError thrown = assertThrows(ProgrammingError.class, () -> model.relax(), "Calling relax() after dispose() must throw ProgrammingError");
        assertNotNull(thrown.getMessage());
    }

    /**
     * Group: relax() after optimisation result obtained – expect IllegalStateException.
     */
    @Test
    void testRelaxAfterOptimisationThrowsIllegalStateException() {
        // simple feasible model
        Variable v = model.newVariable("v").lower(0).upper(10).weight(1);
        model.newExpression("c").set(v, 1).upper(5);
        // obtain a result – this locks the model for further modifications
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isFeasible(), "Pre‑condition: optimisation must succeed before calling relax()");
        // now relax should fail
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.relax(), "Calling relax() after a solution has been obtained must throw IllegalStateException");
        assertNotNull(thrown.getMessage());
    }

    /**
     * Group: relax(boolean) with soft flag true on model without constraints – expect IllegalArgumentException.
     */
    @Test
    void testSoftRelaxOnEmptyModelThrowsIllegalArgumentException() throws Exception {
        // empty model: no variables, no expressions
        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> invokeRelaxSoft(true), "Soft relaxation on an empty model must throw IllegalArgumentException");
        assertNotNull(thrown.getMessage());
    }

    /**
     * Verify that a second call to {@code relax()} does not change state for any already‑relaxed model.
     */
    @ParameterizedTest
    @MethodSource("alreadyRelaxedModels")
    void testRelaxIdempotentParameterized(ExpressionsBasedModel alreadyRelaxed) {
        // capture snapshot
        int vars = alreadyRelaxed.countVariables();
        int exprs = alreadyRelaxed.countExpressions();
        // second call
        assertDoesNotThrow(alreadyRelaxed::relax);
        assertTrue(alreadyRelaxed.isRelaxed());
        // invariants
        assertEquals(vars, alreadyRelaxed.countVariables(), "Variable count must stay unchanged");
        assertEquals(exprs, alreadyRelaxed.countExpressions(), "Expression count must stay unchanged");
    }
}
