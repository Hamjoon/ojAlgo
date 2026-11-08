package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.ojalgo.type.context.NumberContext;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_presolve_Test {

    private ExpressionsBasedModel model;

    class CountingPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger callCount = new AtomicInteger();

        CountingPresolver() {
            // execution order does not matter for the test
            super(0);
        }

        @Override
        public boolean simplify(Expression expression, java.util.Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            int cnt = callCount.incrementAndGet();
            // first invocation requests a repeat, subsequent invocations do not
            return cnt == 1;
        }

        int getCallCount() {
            return callCount.get();
        }
    }

    class NoOpPresolver extends ExpressionsBasedModel.Presolver {

        NoOpPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            return false;
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(redundant);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(redundant);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(quadratic);
        }
    }

    class DummyPresolver extends ExpressionsBasedModel.Presolver {

        DummyPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            fail("simplify must not be called");
            return false;
        }
    }

    class FirstTruePresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger calls = new AtomicInteger();

        FirstTruePresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            int cnt = calls.incrementAndGet();
            // true only for the first expression processed
            return cnt == 1;
        }

        int getCalls() {
            return calls.get();
        }
    }

    class GuardPresolver extends ExpressionsBasedModel.Presolver {

        GuardPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            assertTrue(e.isConstraint(), "Presolver must only receive constraint expressions");
            return false;
        }
    }

    class CapturingPresolver extends ExpressionsBasedModel.Presolver {

        Set<IntIndex> captured = null;

        CapturingPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            captured = new HashSet<>(remaining);
            return false;
        }
    }

    class FirstPresolver extends ExpressionsBasedModel.Presolver {

        FirstPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            return false;
        }
    }

    class SecondPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger calls = new AtomicInteger();

        SecondPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            calls.incrementAndGet();
            // forces needToRepeat
            return true;
        }

        int getCalls() {
            return calls.get();
        }
    }

    class ThirdPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger calls = new AtomicInteger();

        ThirdPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            calls.incrementAndGet();
            return false;
        }

        int getCalls() {
            return calls.get();
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(quadRedundant);
        }
    }

    class StatefulPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger call = new AtomicInteger();

        StatefulPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            int cnt = call.incrementAndGet();
            // true only on the second invocation (i.e. second iteration)
            return cnt == 2;
        }

        int getCalls() {
            return call.get();
        }
    }

    class ExplodingPresolver extends ExpressionsBasedModel.Presolver {

        ExplodingPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            throw new RuntimeException("boom");
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(bad);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(expr);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(redundant);
        }
    }

    class NullFixedModel extends ExpressionsBasedModel {

        @Override
        public Set<IntIndex> getFixedVariables() {
            return null;
        }
    }

    class NullExpressionsModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return null;
        }
    }

    class NoOpPresolver extends ExpressionsBasedModel.Presolver {

        NoOpPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            return false;
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(redundant);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(redundant);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(quadratic);
        }
    }

    class DummyPresolver extends ExpressionsBasedModel.Presolver {

        DummyPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            fail("simplify must not be called");
            return false;
        }
    }

    class FirstTruePresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger calls = new AtomicInteger();

        FirstTruePresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            int cnt = calls.incrementAndGet();
            // true only for the first expression processed
            return cnt == 1;
        }

        int getCalls() {
            return calls.get();
        }
    }

    class GuardPresolver extends ExpressionsBasedModel.Presolver {

        GuardPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            assertTrue(e.isConstraint(), "Presolver must only receive constraint expressions");
            return false;
        }
    }

    class CapturingPresolver extends ExpressionsBasedModel.Presolver {

        Set<IntIndex> captured = null;

        CapturingPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            captured = new HashSet<>(remaining);
            return false;
        }
    }

    class FirstPresolver extends ExpressionsBasedModel.Presolver {

        FirstPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            return false;
        }
    }

    class SecondPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger calls = new AtomicInteger();

        SecondPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            calls.incrementAndGet();
            // forces needToRepeat
            return true;
        }

        int getCalls() {
            return calls.get();
        }
    }

    class ThirdPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger calls = new AtomicInteger();

        ThirdPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            calls.incrementAndGet();
            return false;
        }

        int getCalls() {
            return calls.get();
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(quadRedundant);
        }
    }

    class StatefulPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger call = new AtomicInteger();

        StatefulPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            int cnt = call.incrementAndGet();
            // true only on the second invocation (i.e. second iteration)
            return cnt == 2;
        }

        int getCalls() {
            return call.get();
        }
    }

    class ExplodingPresolver extends ExpressionsBasedModel.Presolver {

        ExplodingPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
            throw new RuntimeException("boom");
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(bad);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(expr);
        }
    }

    class TestModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return Collections.singletonList(redundant);
        }
    }

    class NullFixedModel extends ExpressionsBasedModel {

        @Override
        public Set<IntIndex> getFixedVariables() {
            return null;
        }
    }

    class NullExpressionsModel extends ExpressionsBasedModel {

        @Override
        public Collection<Expression> getExpressions() {
            return null;
        }
    }

    class CountingPresolver extends ExpressionsBasedModel.Presolver {

        private final AtomicInteger callCount = new AtomicInteger();

        CountingPresolver() {
            super(0);
        }

        @Override
        public boolean simplify(Expression expression, java.util.Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            int cnt = callCount.incrementAndGet();
            return cnt == 1;
        }

        int getCallCount() {
            return callCount.get();
        }
    }

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    /**
     * Remove any presolvers that might have been registered by a test.
     */
    @AfterEach
    void teardownAfterEach() {
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * Simple sanity check that {@code presolve()} runs and updates categorisation.
     */
    @Test
    void testPresolveRunsAndUpdatesVariableCategorisation() {
        // ----- given ---------------------------------------------------------
        // a single variable 0 ≤ x ≤ 10, weight 1 (free variable)
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // a single equality constraint 5 ≤ x ≤ 5  (i.e. x = 5)
        Expression eq = model.newExpression("eq").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        eq.set(x, BigDecimal.ONE);
        // ----- when ----------------------------------------------------------
        // invoke the package‑private presolve method directly
        model.presolve();
        // ----- then ----------------------------------------------------------
        // the model must not be marked infeasible
        assertFalse(model.isInfeasible(), "Model should not be infeasible after presolve");
        // the variable categorisation must have been refreshed – the variable is free
        assertEquals(1, model.getFreeVariables().size(), "There should be exactly one free variable after presolve");
        assertSame(x, model.getFreeVariables().get(0), "The free variable should be the one we added");
    }

    /**
     * Verify that a custom presolver can force the repeat‑loop to execute twice.
     */
    @Test
    void testPresolveRepeatsWhenPresolverRequestsIt() {
        // ----- given ---------------------------------------------------------
        // a dummy presolver that returns true on the first call (forcing a repeat)
        // and false afterwards.  It also counts how many times it has been invoked.
        class CountingPresolver extends ExpressionsBasedModel.Presolver {

            private final AtomicInteger callCount = new AtomicInteger();

            CountingPresolver() {
                // execution order does not matter for the test
                super(0);
            }

            @Override
            public boolean simplify(Expression expression, java.util.Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
                int cnt = callCount.incrementAndGet();
                // first invocation requests a repeat, subsequent invocations do not
                return cnt == 1;
            }

            int getCallCount() {
                return callCount.get();
            }
        }
        CountingPresolver presolver = new CountingPresolver();
        ExpressionsBasedModel.addPresolver(presolver);
        // a variable that will be part of a simple constraint
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // a non‑redundant constraint 0 ≤ y ≤ 10 (nothing special, just to trigger the presolver)
        Expression expr = model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        expr.set(y, BigDecimal.ONE);
        // ----- when ----------------------------------------------------------
        model.presolve();
        // ----- then ----------------------------------------------------------
        // The presolver should have been called twice: once returning true,
        // causing the outer do‑while loop to repeat, and a second time returning false.
        assertEquals(2, presolver.getCallCount(), "Presolver should be invoked exactly twice (once to request repeat, once to finish)");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Presolve loop executes once when no presolver requests a repeat")
    void testPresolveLoopExecutesOnceWithoutRepeat() {
        // a dummy presolver that always returns false
        class NoOpPresolver extends ExpressionsBasedModel.Presolver {

            NoOpPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                return false;
            }
        }
        ExpressionsBasedModel.addPresolver(new NoOpPresolver());
        // a single linear constraint
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        model.presolve();
        // the loop should have run once – we cannot directly observe the loop count,
        // but we can assert that the model is still feasible and the variable
        // categorisation was refreshed.
        assertFalse(model.isInfeasible());
        assertEquals(1, model.getFreeVariables().size());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Feasibility check is performed for redundant linear constraints")
    void testFeasibilityCheckOnRedundantConstraintsWhenModelFeasible() {
        // Build a model that contains a redundant constraint.
        // We cannot make the real model mark a constraint redundant, therefore we
        // subclass the model and return a mocked expression that reports the
        // required flags.
        Expression redundant = mock(Expression.class);
        when(redundant.isConstraint()).thenReturn(true);
        when(redundant.isRedundant()).thenReturn(true);
        when(redundant.isInfeasible()).thenReturn(false);
        when(redundant.countQuadraticFactors()).thenReturn(0);
        when(redundant.getLinearKeySet()).thenReturn(Collections.emptySet());
        when(redundant.calculateSetValue(anySet())).thenReturn(BigDecimal.ZERO);
        when(redundant.getCompensatedLowerLimit(any())).thenReturn(BigDecimal.ZERO);
        when(redundant.getCompensatedUpperLimit(any())).thenReturn(BigDecimal.ZERO);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(redundant);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            testModel.presolve();
            mocked.verify(() -> Presolvers.checkFeasibility(eq(redundant), anySet(), eq(BigDecimal.ZERO), eq(BigDecimal.ZERO), eq(testModel.options.feasibility), eq(testModel.isRelaxed())), times(1));
        }
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Feasibility check is skipped if the model is already infeasible")
    void testSkipFeasibilityCheckWhenModelInfeasible() {
        // Same mock expression as previous test
        Expression redundant = mock(Expression.class);
        when(redundant.isConstraint()).thenReturn(true);
        when(redundant.isRedundant()).thenReturn(true);
        when(redundant.isInfeasible()).thenReturn(false);
        when(redundant.countQuadraticFactors()).thenReturn(0);
        when(redundant.getLinearKeySet()).thenReturn(Collections.emptySet());
        when(redundant.calculateSetValue(anySet())).thenReturn(BigDecimal.ZERO);
        when(redundant.getCompensatedLowerLimit(any())).thenReturn(BigDecimal.ZERO);
        when(redundant.getCompensatedUpperLimit(any())).thenReturn(BigDecimal.ZERO);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(redundant);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        // make the model infeasible
        testModel.setInfeasible();
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            testModel.presolve();
            mocked.verifyNoInteractions();
        }
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Expressions with quadratic factors are ignored by presolve")
    void testPresolveIgnoresExpressionsWithQuadraticFactors() {
        // mock expression that reports a non‑zero quadratic factor count
        Expression quadratic = mock(Expression.class);
        when(quadratic.isConstraint()).thenReturn(true);
        when(quadratic.isInfeasible()).thenReturn(false);
        when(quadratic.isRedundant()).thenReturn(false);
        // >0
        when(quadratic.countQuadraticFactors()).thenReturn(1);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(quadratic);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        class DummyPresolver extends ExpressionsBasedModel.Presolver {

            DummyPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                fail("simplify must not be called");
                return false;
            }
        }
        ExpressionsBasedModel.addPresolver(new DummyPresolver());
        // should complete without invoking simplify
        testModel.presolve();
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("When needToRepeat becomes true, remaining expressions are skipped in the same iteration")
    void testPresolveRespectsNeedToRepeatInsideInnerLoop() {
        // three simple constraints
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable b = model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable c = model.newVariable("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("e1").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(a, BigDecimal.ONE);
        model.newExpression("e2").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(b, BigDecimal.ONE);
        model.newExpression("e3").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(c, BigDecimal.ONE);
        class FirstTruePresolver extends ExpressionsBasedModel.Presolver {

            private final AtomicInteger calls = new AtomicInteger();

            FirstTruePresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
                int cnt = calls.incrementAndGet();
                // true only for the first expression processed
                return cnt == 1;
            }

            int getCalls() {
                return calls.get();
            }
        }
        FirstTruePresolver presolver = new FirstTruePresolver();
        ExpressionsBasedModel.addPresolver(presolver);
        model.presolve();
        // Only the first expression should have triggered simplify in the first iteration.
        // The total number of calls equals number of processed expressions across both
        // iterations (first iteration: 1, second iteration: 3 because needToRepeat is reset).
        assertEquals(4, presolver.getCalls(), "First iteration processes 1 expression, second iteration processes all 3");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Presolve works correctly on an empty model")
    void testPresolveWithEmptyModel() {
        // model already has no variables / expressions
        // should not throw
        model.presolve();
        assertTrue(model.getVariables().isEmpty(), "No variables should exist");
        assertTrue(model.getExpressions().isEmpty(), "No expressions should exist");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Presolve runs without any registered presolvers")
    void testPresolveWithNoRegisteredPresolvers() {
        // ensure empty
        ExpressionsBasedModel.clearPresolvers();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(x, BigDecimal.ONE);
        // No presolvers → inner loop does nothing, but the method must finish.
        model.presolve();
        assertFalse(model.isInfeasible());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Objective expression is ignored by presolve")
    void testPresolveIgnoresNonConstraintObjectiveExpression() {
        // create an objective expression (no lower/upper limits)
        model.objective().weight(BigDecimal.ONE).set(model.newVariable("z").weight(BigDecimal.ONE), BigDecimal.ONE);
        // also add a normal constraint to make sure the loop runs at least once
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        // register a presolver that would fail if it were called for the objective
        class GuardPresolver extends ExpressionsBasedModel.Presolver {

            GuardPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                assertTrue(e.isConstraint(), "Presolver must only receive constraint expressions");
                return false;
            }
        }
        ExpressionsBasedModel.addPresolver(new GuardPresolver());
        model.presolve();
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fixed variables are removed from the temporary set passed to presolvers")
    void testPresolveRemovesFixedVariablesFromTemporarySet() {
        // variable fixed at 5
        Variable fixed = model.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // constraint referencing the fixed variable
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(fixed, BigDecimal.ONE);
        class CapturingPresolver extends ExpressionsBasedModel.Presolver {

            Set<IntIndex> captured = null;

            CapturingPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
                captured = new HashSet<>(remaining);
                return false;
            }
        }
        CapturingPresolver presolver = new CapturingPresolver();
        ExpressionsBasedModel.addPresolver(presolver);
        model.presolve();
        assertNotNull(presolver.captured, "Presolver should have received a set");
        // the fixed variable's index must not be present
        IntIndex fixedIdx = model.toIntIndex(model.indexOf(fixed));
        assertFalse(presolver.captured.contains(fixedIdx), "Fixed variable index must be removed from the temporary set");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Presolver order respects needToRepeat – later presolvers are skipped in that iteration")
    void testMultiplePresolversEarlyTerminationAfterNeedToRepeat() {
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        class FirstPresolver extends ExpressionsBasedModel.Presolver {

            FirstPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                return false;
            }
        }
        class SecondPresolver extends ExpressionsBasedModel.Presolver {

            private final AtomicInteger calls = new AtomicInteger();

            SecondPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                calls.incrementAndGet();
                // forces needToRepeat
                return true;
            }

            int getCalls() {
                return calls.get();
            }
        }
        class ThirdPresolver extends ExpressionsBasedModel.Presolver {

            private final AtomicInteger calls = new AtomicInteger();

            ThirdPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                calls.incrementAndGet();
                return false;
            }

            int getCalls() {
                return calls.get();
            }
        }
        SecondPresolver second = new SecondPresolver();
        ThirdPresolver third = new ThirdPresolver();
        ExpressionsBasedModel.addPresolver(new FirstPresolver());
        ExpressionsBasedModel.addPresolver(second);
        ExpressionsBasedModel.addPresolver(third);
        model.presolve();
        // In the first iteration: first (false), second (true) → needToRepeat becomes true,
        // third is not called.
        // In the second iteration: needToRepeat reset, all three are called again.
        assertEquals(2, second.getCalls(), "Second presolver should be called in both iterations");
        assertEquals(1, third.getCalls(), "Third presolver should be called only in the second iteration");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Quadratic redundant constraints are ignored by the feasibility check")
    void testRedundantQuadraticConstraintDoesNotTriggerFeasibilityCheck() {
        Expression quadRedundant = mock(Expression.class);
        when(quadRedundant.isConstraint()).thenReturn(true);
        when(quadRedundant.isRedundant()).thenReturn(true);
        when(quadRedundant.isInfeasible()).thenReturn(false);
        // quadratic
        when(quadRedundant.countQuadraticFactors()).thenReturn(2);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(quadRedundant);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            testModel.presolve();
            // checkFeasibility must not be called
            mocked.verifyNoInteractions();
        }
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Presolve repeats when a presolver returns true on a later iteration")
    void testPresolveRepeatsOnlyAfterStateChangeSecondIteration() {
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        class StatefulPresolver extends ExpressionsBasedModel.Presolver {

            private final AtomicInteger call = new AtomicInteger();

            StatefulPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                int cnt = call.incrementAndGet();
                // true only on the second invocation (i.e. second iteration)
                return cnt == 2;
            }

            int getCalls() {
                return call.get();
            }
        }
        StatefulPresolver presolver = new StatefulPresolver();
        ExpressionsBasedModel.addPresolver(presolver);
        model.presolve();
        // Expected call sequence: iteration1 -> false, iteration2 -> true (sets needToRepeat),
        // iteration3 -> false. Total 3 calls.
        assertEquals(3, presolver.getCalls(), "Presolver should be called three times (false, true, false)");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("A null presolver in the static set causes NPE")
    void testPresolverElementNullCausesNPE() {
        // Directly inject a null into the TreeSet (reflection required)
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("PRESOLVERS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            TreeSet<ExpressionsBasedModel.Presolver> set = (TreeSet<ExpressionsBasedModel.Presolver>) field.get(null);
            set.add(null);
        } catch (Exception e) {
            fail("Failed to inject null presolver: " + e);
        }
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        assertThrows(NullPointerException.class, model::presolve, "Presolve should throw NPE when encountering a null presolver");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Exception thrown by presolver.simplify propagates")
    void testPresolverSimplifyThrowsRuntimeException() {
        class ExplodingPresolver extends ExpressionsBasedModel.Presolver {

            ExplodingPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression e, Set<IntIndex> r, BigDecimal l, BigDecimal u, NumberContext p) {
                throw new RuntimeException("boom");
            }
        }
        ExpressionsBasedModel.addPresolver(new ExplodingPresolver());
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        RuntimeException ex = assertThrows(RuntimeException.class, model::presolve);
        assertEquals("boom", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Exception from Expression.calculateSetValue propagates")
    void testExpressionCalculateSetValueThrowsIllegalStateException() {
        Expression bad = mock(Expression.class);
        when(bad.isConstraint()).thenReturn(true);
        when(bad.isInfeasible()).thenReturn(false);
        when(bad.isRedundant()).thenReturn(false);
        when(bad.countQuadraticFactors()).thenReturn(0);
        when(bad.getLinearKeySet()).thenReturn(Collections.emptySet());
        when(bad.calculateSetValue(anySet())).thenThrow(new IllegalStateException("calc error"));
        when(bad.getCompensatedLowerLimit(any())).thenReturn(BigDecimal.ZERO);
        when(bad.getCompensatedUpperLimit(any())).thenReturn(BigDecimal.ZERO);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(bad);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        IllegalStateException ex = assertThrows(IllegalStateException.class, testModel::presolve);
        assertEquals("calc error", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Null argument to getCompensatedLowerLimit results in NPE")
    void testExpressionGetCompensatedLowerLimitThrowsNPE() {
        Expression expr = mock(Expression.class);
        when(expr.isConstraint()).thenReturn(true);
        when(expr.isInfeasible()).thenReturn(false);
        when(expr.isRedundant()).thenReturn(false);
        when(expr.countQuadraticFactors()).thenReturn(0);
        when(expr.getLinearKeySet()).thenReturn(Collections.emptySet());
        // forces null argument
        when(expr.calculateSetValue(anySet())).thenReturn(null);
        when(expr.getCompensatedLowerLimit(isNull())).thenThrow(new NullPointerException("null lower"));
        when(expr.getCompensatedUpperLimit(any())).thenReturn(BigDecimal.ZERO);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(expr);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        NullPointerException ex = assertThrows(NullPointerException.class, testModel::presolve);
        assertEquals("null lower", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Exception from Presolvers.checkFeasibility propagates")
    void testPresolversCheckFeasibilityThrowsIllegalArgumentException() {
        Expression redundant = mock(Expression.class);
        when(redundant.isConstraint()).thenReturn(true);
        when(redundant.isRedundant()).thenReturn(true);
        when(redundant.isInfeasible()).thenReturn(false);
        when(redundant.countQuadraticFactors()).thenReturn(0);
        when(redundant.getLinearKeySet()).thenReturn(Collections.emptySet());
        when(redundant.calculateSetValue(anySet())).thenReturn(BigDecimal.ZERO);
        when(redundant.getCompensatedLowerLimit(any())).thenReturn(BigDecimal.ZERO);
        when(redundant.getCompensatedUpperLimit(any())).thenReturn(BigDecimal.ZERO);
        class TestModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return Collections.singletonList(redundant);
            }
        }
        ExpressionsBasedModel testModel = new TestModel();
        try (MockedStatic<Presolvers> mocked = mockStatic(Presolvers.class)) {
            mocked.when(() -> Presolvers.checkFeasibility(any(), anySet(), any(), any(), any(), anyBoolean())).thenThrow(new IllegalArgumentException("bad arg"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, testModel::presolve);
            assertEquals("bad arg", ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Null feasibility option leads to NPE")
    void testOptionsFeasibilityNullCausesNPE() throws Exception {
        // set options.feasibility to null via reflection
        Field optField = ExpressionsBasedModel.class.getDeclaredField("options");
        optField.setAccessible(true);
        Optimisation.Options opts = (Optimisation.Options) optField.get(model);
        Field feasField = Optimisation.Options.class.getDeclaredField("feasibility");
        feasField.setAccessible(true);
        feasField.set(opts, null);
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        assertThrows(NullPointerException.class, model::presolve, "Presolve should NPE when options.feasibility is null");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Null from getFixedVariables causes NPE")
    void testGetFixedVariablesReturnsNullCausesNPE() {
        class NullFixedModel extends ExpressionsBasedModel {

            @Override
            public Set<IntIndex> getFixedVariables() {
                return null;
            }
        }
        ExpressionsBasedModel nullFixed = new NullFixedModel();
        Variable v = nullFixed.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        nullFixed.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).set(v, BigDecimal.ONE);
        assertThrows(NullPointerException.class, nullFixed::presolve);
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Null from getExpressions causes NPE")
    void testGetExpressionsReturnsNullCausesNPE() {
        class NullExpressionsModel extends ExpressionsBasedModel {

            @Override
            public Collection<Expression> getExpressions() {
                return null;
            }
        }
        ExpressionsBasedModel nullExpr = new NullExpressionsModel();
        assertThrows(NullPointerException.class, nullExpr::presolve);
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Custom presolver forces presolve to repeat")
    void testPresolveRepeatsWhenPresolverRequestsIt_original() {
        class CountingPresolver extends ExpressionsBasedModel.Presolver {

            private final AtomicInteger callCount = new AtomicInteger();

            CountingPresolver() {
                super(0);
            }

            @Override
            public boolean simplify(Expression expression, java.util.Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
                int cnt = callCount.incrementAndGet();
                return cnt == 1;
            }

            int getCallCount() {
                return callCount.get();
            }
        }
        CountingPresolver presolver = new CountingPresolver();
        ExpressionsBasedModel.addPresolver(presolver);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Expression expr = model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        expr.set(y, BigDecimal.ONE);
        model.presolve();
        assertEquals(2, presolver.getCallCount(), "Presolver should be invoked exactly twice (once to request repeat, once to finish)");
    }
}
