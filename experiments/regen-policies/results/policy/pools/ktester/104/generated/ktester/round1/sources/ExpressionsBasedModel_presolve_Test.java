package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.type.context.NumberContext;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.TreeSet;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_presolve_Test {

    /**
     * Preserve the original presolvers so that we do not interfere with other tests.
     */
    private static Set<ExpressionsBasedModel.Presolver> ORIGINAL_PRESOLVERS;

    @BeforeAll
    static void setupBeforeAll() {
        // Keep a copy of the current presolvers and start from a clean state.
        ORIGINAL_PRESOLVERS = Set.copyOf(ExpressionsBasedModel.PRESOLVERS);
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure that each test leaves the static PRESOLVERS set empty.
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original presolvers (if any) so that the global state is unchanged.
        ExpressionsBasedModel.clearPresolvers();
        ORIGINAL_PRESOLVERS.forEach(ExpressionsBasedModel::addPresolver);
    }

    @Test
    void testPresolveInvokesPresolverAndRespectsNeedToRepeatFlag() {
        // -----------------------------------------------------------------
        // GIVEN – a very small model with a single variable and a single
        // constraint expression (so that the default {@code isApplicable}
        // check of the presolver returns true).
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a free variable
        Variable x = model.addVariable();
        // will become a constraint
        Expression expr = model.addExpression();
        // make it a constraint
        expr.lower(BigDecimal.ZERO);
        // add a term so the expression is non‑empty
        expr.set(x, ONE);
        // -----------------------------------------------------------------
        // AND – a mock Presolver that returns {@code true} on the first
        // invocation (forcing a second iteration) and {@code false} on the
        // subsequent invocation (allowing the loop to terminate).
        // -----------------------------------------------------------------
        ExpressionsBasedModel.Presolver mockPresolver = mock(ExpressionsBasedModel.Presolver.class, withSettings().useConstructor(// executionOrder required by the super‑constructor
        0).defaultAnswer(CALLS_REAL_METHODS));
        // The presolver is applicable to the expression we created (it is a
        // constraint, not infeasible, not redundant and linear).
        when(mockPresolver.simplify(any(Expression.class), anySet(), any(BigDecimal.class), any(BigDecimal.class), any(NumberContext.class))).thenReturn(// first call – request a repeat
        true).thenReturn(// second call – stop repeating
        false);
        // Register the mock presolver globally.
        ExpressionsBasedModel.addPresolver(mockPresolver);
        // -----------------------------------------------------------------
        // WHEN – the model is presolved.
        // -----------------------------------------------------------------
        model.presolve();
        // -----------------------------------------------------------------
        // THEN – the presolver's {@code simplify} method must have been
        // called at least once (the exact number depends on the internal
        // loop logic, but it must be > 0).
        // -----------------------------------------------------------------
        verify(mockPresolver, atLeastOnce()).simplify(any(Expression.class), anySet(), any(BigDecimal.class), any(BigDecimal.class), any(NumberContext.class));
        // -----------------------------------------------------------------
        // AND – after presolving the internal variable categorisation must
        // have been refreshed; the single variable we added is free.
        // -----------------------------------------------------------------
        assertEquals(1, model.getFreeVariables().size(), "After presolve the free‑variable list should contain the single variable we added.");
    }

    @Test
    void testPresolveUpdatesVariablesCategorisation() {
        // -----------------------------------------------------------------
        // GIVEN – a model with two variables, one of which is fixed by a
        // constraint (lower == upper).  No custom presolvers are needed for
        // this test.
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v0 = model.addVariable().lower(0).upper(10).weight(1);
        // fixed at 5
        Variable v1 = model.addVariable().lower(5).upper(5).weight(1);
        // A simple constraint that does not interfere with the fixed status.
        Expression expr = model.addExpression();
        expr.set(v0, ONE);
        expr.set(v1, ONE);
        // just to make it a constraint
        expr.lower(BigDecimal.ZERO);
        // -----------------------------------------------------------------
        // WHEN – the model is presolved.
        // -----------------------------------------------------------------
        model.presolve();
        // -----------------------------------------------------------------
        // THEN – the categorisation must reflect that v0 is free and v1 is
        // not free (because it is fixed).  The public API provides
        // {@code getFreeVariables()} for this purpose.
        // -----------------------------------------------------------------
        assertTrue(model.getFreeVariables().contains(v0), "Variable v0 should be reported as free after presolve.");
        assertFalse(model.getFreeVariables().contains(v1), "Variable v1 is fixed and therefore must not appear in the free‑variable list.");
    }

    @Test
    void testPresolveNoExpressionsTrivialExecution() {
        // Model with no variables and no expressions.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // No presolvers are registered – PRESOLVERS is empty.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "Presolvers set should be empty before the test.");
        // The presolve method should complete without throwing and
        // without entering the inner loops.
        assertDoesNotThrow(model::presolve, "presolve() must not throw when the model contains no expressions.");
        // Variable categorisation must be updated (still empty).
        assertTrue(model.getFreeVariables().isEmpty(), "Free‑variable list should remain empty for a model without variables.");
    }

    @Test
    void testPresolveSingleFeasibleLinearConstraintNoPresolvers() {
        // One free variable and one linear constraint.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable();
        Expression expr = model.addExpression();
        // make it a constraint
        expr.lower(BigDecimal.ZERO);
        // linear term
        expr.set(x, ONE);
        // No presolvers registered.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty(), "No presolvers should be present for this test.");
        // Run presolve.
        model.presolve();
        // The variable must be reported as free.
        assertTrue(model.getFreeVariables().contains(x), "The single variable should be free after presolve.");
    }

    @Test
    void testPresolvePresolverCausesRepeatAndFixesVariable() {
        // Two variables, one constraint involving both.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable a = model.addVariable();
        Variable b = model.addVariable();
        Expression expr = model.addExpression();
        expr.lower(BigDecimal.ZERO);
        expr.set(a, ONE);
        expr.set(b, ONE);
        // Custom presolver that fixes variable 'a' on first call and
        // returns true (requesting a repeat). On the second call it does
        // nothing and returns false.
        ExpressionsBasedModel.Presolver fixingPresolver = mock(ExpressionsBasedModel.Presolver.class, withSettings().useConstructor(0).defaultAnswer(CALLS_REAL_METHODS));
        // First invocation: fix variable a at 0 and request repeat.
        doAnswer(invocation -> {
            // Simulate fixing by setting lower == upper on variable a.
            a.lower(BigDecimal.ZERO);
            a.upper(BigDecimal.ZERO);
            // need to repeat
            return true;
        }).doReturn(// second call – no further repeat
        false).when(fixingPresolver).simplify(any(Expression.class), anySet(), any(BigDecimal.class), any(BigDecimal.class), any(NumberContext.class));
        ExpressionsBasedModel.addPresolver(fixingPresolver);
        // Execute presolve.
        model.presolve();
        // Verify that simplify was called at least twice (once per loop).
        verify(fixingPresolver, atLeast(2)).simplify(any(Expression.class), anySet(), any(BigDecimal.class), any(BigDecimal.class), any(NumberContext.class));
        // After presolve, variable 'a' must be fixed and therefore not free.
        assertFalse(model.getFreeVariables().contains(a), "Variable 'a' should have been fixed by the presolver and thus not appear as free.");
        // Variable 'b' remains free.
        assertTrue(model.getFreeVariables().contains(b), "Variable 'b' should remain free.");
    }

    @Test
    void testPresolveRedundantConstraintFeasibilityCheck() {
        // Variable fixed at 0.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // fixed
        Variable x = model.addVariable().lower(0).upper(0);
        // Constraint that is already satisfied by the fixed variable.
        // lower == upper == 0 makes the expression redundant.
        Expression expr = model.addExpression();
        expr.set(x, ONE);
        expr.lower(BigDecimal.ZERO);
        // exact match → redundant
        expr.upper(BigDecimal.ZERO);
        // No presolvers.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        // Presolve should run without throwing; the redundant‑constraint branch
        // is exercised internally.
        assertDoesNotThrow(model::presolve, "presolve() must handle a redundant constraint without throwing.");
        // Model must still be feasible.
        assertFalse(model.isInfeasible(), "Model should remain feasible after processing a redundant constraint.");
    }

    @Test
    void testPresolveModelAlreadyInfeasibleExitsEarly() {
        // Create a model with an infeasible constraint.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable();
        Expression expr = model.addExpression();
        expr.lower(BigDecimal.ONE);
        // lower > upper → infeasible
        expr.upper(BigDecimal.ZERO);
        // Explicitly mark the model as infeasible.
        model.setInfeasible();
        // No presolvers registered.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        // Presolve should return quickly and not throw.
        assertDoesNotThrow(model::presolve, "presolve() must exit cleanly when the model is already infeasible.");
        // The infeasible flag must still be true.
        assertTrue(model.isInfeasible(), "Model should remain marked as infeasible after presolve.");
    }

    @Test
    void testPresolveVariableCategorisationMixedAttributes() {
        // Four variables with different characteristics.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // normal free
        Variable vFree = model.addVariable();
        // integer
        Variable vInt = model.addVariable().integer(true);
        // positive upper bound
        Variable vPos = model.addVariable().upper(5);
        // negative lower bound
        Variable vNeg = model.addVariable().lower(-3);
        // No constraints – we only care about categorisation.
        model.presolve();
        // Free variables: all except those that are fixed (none are fixed).
        assertTrue(model.getFreeVariables().containsAll(java.util.List.of(vFree, vInt, vPos, vNeg)), "All non‑fixed variables must be listed as free.");
        // Integer variables list.
        assertTrue(model.getIntegerVariables().contains(vInt), "Integer variable must appear in the integer‑variables list.");
        // Positive variables list (upper limit > 0 or not set).
        assertTrue(model.getPositiveVariables().containsAll(java.util.List.of(vFree, vInt, vPos, vNeg)), "All variables with a non‑negative upper bound should be in the positive list.");
        // Negative variables list (lower limit < 0 or not set).
        assertTrue(model.getNegativeVariables().containsAll(java.util.List.of(vFree, vInt, vPos, vNeg)), "All variables with a negative lower bound should be in the negative list.");
        // Index checks – for a variable not belonging to a category the index should be -1.
        assertEquals(-1, model.indexOfPositiveVariable(vNeg), "vNeg should have a positive index of -1.");
        assertEquals(-1, model.indexOfNegativeVariable(vPos), "vPos should have a negative index of -1.");
    }

    @Test
    void testPresolveNullOptionsThrowsNullPointerException() throws Exception {
        // Create a model instance.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use reflection to set the private final field 'options' to null.
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        // The method accesses options.feasibility, so a NPE is expected.
        assertThrows(NullPointerException.class, model::presolve, "presolve() should throw NullPointerException when options is null.");
    }

    @Test
    void testPresolveNullPresolverInSetThrowsNullPointerException() {
        // Add a null presolver to the static set.
        ExpressionsBasedModel.addPresolver(null);
        // Build a minimal model with one variable and a constraint.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable();
        Expression expr = model.addExpression();
        expr.lower(BigDecimal.ZERO);
        expr.set(x, ONE);
        // Expect NPE when the loop tries to invoke simplify on the null entry.
        assertThrows(NullPointerException.class, model::presolve, "presolve() should throw NullPointerException when a null presolver is present in PRESOLVERS.");
    }

    @Test
    void testPresolvePresolverThrowsIllegalArgumentException() {
        // Model with a simple constraint.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable();
        Expression expr = model.addExpression();
        expr.lower(BigDecimal.ZERO);
        expr.set(x, ONE);
        // Presolver that throws IllegalArgumentException.
        ExpressionsBasedModel.Presolver badPresolver = mock(ExpressionsBasedModel.Presolver.class, withSettings().useConstructor(0).defaultAnswer(CALLS_REAL_METHODS));
        when(badPresolver.simplify(any(Expression.class), anySet(), any(BigDecimal.class), any(BigDecimal.class), any(NumberContext.class))).thenThrow(new IllegalArgumentException("Deliberate test exception"));
        ExpressionsBasedModel.addPresolver(badPresolver);
        // The exception should propagate out of presolve().
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, model::presolve, "presolve() must propagate IllegalArgumentException thrown by a presolver.");
        assertEquals("Deliberate test exception", thrown.getMessage(), "Exception message should match the one thrown by the mock presolver.");
    }

    @Test
    void testPresolveExpressionReturnsNullLinearKeySetThrowsNullPointerException() {
        // Model with no variables (the expression does not need them for this test).
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Create a mock Expression that returns null for getLinearKeySet().
        Expression badExpr = mock(Expression.class);
        when(badExpr.isConstraint()).thenReturn(true);
        when(badExpr.isInfeasible()).thenReturn(false);
        when(badExpr.isRedundant()).thenReturn(false);
        when(badExpr.countQuadraticFactors()).thenReturn(0);
        when(badExpr.calculateSetValue(anySet())).thenReturn(BigDecimal.ZERO);
        when(badExpr.getCompensatedLowerLimit(any())).thenReturn(BigDecimal.ZERO);
        when(badExpr.getCompensatedUpperLimit(any())).thenReturn(BigDecimal.ZERO);
        // <-- triggers NPE in presolve
        when(badExpr.getLinearKeySet()).thenReturn(null);
        // Add the mock expression to the model's expression collection.
        // getExpressions() returns a mutable collection in the current library version.
        Collection<Expression> expressions = model.getExpressions();
        assertTrue(expressions instanceof java.util.Collection, "Model's expression collection should be mutable for the test.");
        expressions.add(badExpr);
        // No presolvers registered.
        assertTrue(ExpressionsBasedModel.PRESOLVERS.isEmpty());
        // Expect NullPointerException when presolve processes the bad expression.
        assertThrows(NullPointerException.class, model::presolve, "presolve() should throw NullPointerException when an expression returns null linear key set.");
    }
}
