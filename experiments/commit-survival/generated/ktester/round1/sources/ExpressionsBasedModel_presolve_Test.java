package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.structure.Structure1D.IntIndex;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_presolve_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Expression eqExpr;

    /**
     * Simple concrete implementation of {@link ExpressionsBasedModel.Presolver}
     * used in the tests. It returns a predefined sequence of boolean results from
     * {@link #simplify(Expression, Set, BigDecimal, BigDecimal, NumberContext)}.
     */
    private static class TestPresolver extends ExpressionsBasedModel.Presolver {

        private final List<Boolean> results;

        private final AtomicInteger index = new AtomicInteger(0);

        private final AtomicInteger invocationCount = new AtomicInteger(0);

        /**
         * @param executionOrder order used by the {@link TreeSet} of presolvers
         * @param results       sequence of boolean values to be returned on successive calls
         */
        TestPresolver(final int executionOrder, List<Boolean> results) {
            super(executionOrder);
            this.results = new ArrayList<>(results);
        }

        @Override
        public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
            invocationCount.incrementAndGet();
            int i = index.getAndIncrement();
            // If we run out of predefined results, default to {@code false}
            return i < results.size() ? results.get(i) : false;
        }

        int getInvocationCount() {
            return invocationCount.get();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure a clean static state before any test runs
        ExpressionsBasedModel.clearPresolvers();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Variable x with a wide domain – the presolver should tighten it to 5
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(// 10
        BigDecimal.TEN).weight(// not part of objective
        BigDecimal.ZERO);
        // Equality constraint: x == 5  (lower = upper = 5)
        eqExpr = model.newExpression("eq").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        eqExpr.set(x, BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset static presolvers after each test to avoid cross‑test interference
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Final clean‑up of static state
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * A simple model where a variable can be fixed by a single equality
     * constraint. After {@code presolve()} the variable must be marked as
     * fixed and the constraint must become redundant.
     */
    @Test
    void testPresolveFixesVariableAndMarksConstraintRedundant() {
        // Preconditions
        assertFalse(x.isFixed(), "Variable should not be fixed before presolve");
        assertFalse(eqExpr.isRedundant(), "Constraint should not be redundant before presolve");
        assertTrue(eqExpr.isConstraint(), "Expression must be a constraint");
        assertEquals(0, model.getFixedVariables().size(), "No fixed variables initially");
        // Invoke the package‑private method
        model.presolve();
        // After presolve the variable must be fixed at the value 5
        assertTrue(x.isFixed(), "Variable should be fixed after presolve");
        assertEquals(BigDecimal.valueOf(5), x.getValue(), "Fixed value must be 5");
        // The equality expression should now be recognised as redundant
        assertTrue(eqExpr.isRedundant(), "Equality constraint should be redundant after presolve");
        // The set of fixed variables reported by the model must contain the index of x
        Set<IntIndex> fixed = model.getFixedVariables();
        assertEquals(1, fixed.size(), "Exactly one variable should be fixed");
        assertTrue(fixed.contains(IntIndex.of(model.indexOf(x))), "Fixed set must contain x");
    }

    /**
     * A model that already satisfies all constraints and cannot be further
     * simplified. The presolver should leave the model unchanged.
     */
    @Test
    void testPresolveLeavesUnchangedModelAlone() {
        // Add a second variable y with a loose bound and a non‑binding constraint
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(100)).weight(BigDecimal.ZERO);
        Expression nonBinding = model.newExpression("nonBinding").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(200));
        nonBinding.set(x, BigDecimal.ONE);
        nonBinding.set(y, BigDecimal.ONE);
        // Capture state before presolve
        int varCountBefore = model.countVariables();
        int exprCountBefore = model.countExpressions();
        boolean xFixedBefore = x.isFixed();
        // Run presolve
        model.presolve();
        // Verify that nothing essential changed
        assertEquals(varCountBefore, model.countVariables(), "Variable count must stay the same");
        assertEquals(exprCountBefore, model.countExpressions(), "Expression count must stay the same");
        assertEquals(xFixedBefore, x.isFixed(), "Variable fixation state must stay the same");
        assertFalse(nonBinding.isRedundant(), "Non‑binding constraint must remain non‑redundant");
    }

    /**
     * After presolve, the model should still be solvable and the solution must
     * satisfy the original constraints. This also indirectly checks that the
     * internal categorisation update (the last line of {@code presolve}) works.
     */
    @Test
    void testPresolveFollowedByOptimisation() {
        // Run presolve first
        model.presolve();
        // Now solve the model (minimise – the objective weight is zero, any feasible point is optimal)
        Optimisation.Result result = model.minimise();
        // Validation should succeed
        assertTrue(model.validate(result), "Result should validate against the model");
        // The solution must respect the equality constraint x == 5
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)), "Optimisation result must fix x to 5");
    }

    /**
     * Empty model (no variables, no expressions). The outer {@code do‑while}
     * loop should execute once, do nothing, and exit cleanly.
     */
    @Test
    void testPresolveEmptyModel() {
        // no variables, no expressions
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        // should not throw
        empty.presolve();
        assertEquals(0, empty.countVariables(), "Empty model must have zero variables");
        assertEquals(0, empty.countExpressions(), "Empty model must have zero expressions");
        assertFalse(empty.isInfeasible(), "Empty model must be feasible");
    }

    /**
     * A single feasible linear constraint with a presolver that returns {@code true}
     * on the first call and {@code false} on the second. The presolver should be
     * invoked twice because {@code needToRepeat} becomes {@code true} after the
     * first iteration.
     */
    @Test
    void testPresolvePresolverReturnsTrueTriggersRepeat() {
        // Variable a with wide bounds
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ZERO);
        // Simple non‑binding constraint: a >= 0, a <= 10
        Expression expr = model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        expr.set(a, BigDecimal.ONE);
        // Create a presolver that returns true first, then false
        TestPresolver presolver = new TestPresolver(0, Arrays.asList(true, false));
        ExpressionsBasedModel.addPresolver(presolver);
        model.presolve();
        assertEquals(2, presolver.getInvocationCount(), "Presolver should have been invoked twice (first true, second false)");
    }

    /**
     * An infeasible constraint (lower > upper) must be skipped entirely – the
     * presolver should never be called.
     */
    @Test
    void testPresolveSkipsInfeasibleExpression() {
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Infeasible: lower = 10, upper = 0
        Expression infeasible = model.newExpression("inf").lower(BigDecimal.TEN).upper(BigDecimal.ZERO);
        infeasible.set(a, BigDecimal.ONE);
        // Presolver that would fail the test if invoked
        TestPresolver presolver = new TestPresolver(0, Collections.singletonList(false)) {

            @Override
            public boolean simplify(Expression expression, Set<IntIndex> remaining, BigDecimal lower, BigDecimal upper, NumberContext precision) {
                fail("Presolver should not be invoked for infeasible expressions");
                return false;
            }
        };
        ExpressionsBasedModel.addPresolver(presolver);
        // should complete without invoking the presolver
        model.presolve();
        assertEquals(0, presolver.getInvocationCount(), "Presolver must not be called for infeasible constraints");
    }

    /**
     * Two presolvers are registered. The first returns {@code true}, causing
     * {@code needToRepeat} to become {@code true}. The second presolver must be
     * skipped in that iteration but invoked in the following iteration.
     */
    @Test
    void testMultiplePresolversShortCircuitFirstIteration() {
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Expression expr = model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        expr.set(a, BigDecimal.ONE);
        // First presolver: true on first call, false afterwards
        TestPresolver first = new TestPresolver(0, Arrays.asList(true, false));
        // Second presolver: always false
        TestPresolver second = new TestPresolver(1, Collections.singletonList(false));
        ExpressionsBasedModel.addPresolver(first);
        ExpressionsBasedModel.addPresolver(second);
        model.presolve();
        // Expected calls:
        // iteration 1: first (true) -> second skipped
        // iteration 2: first (false) -> second (false)
        assertEquals(2, first.getInvocationCount(), "First presolver should be called twice (true then false)");
        assertEquals(1, second.getInvocationCount(), "Second presolver should be called once (only in second iteration)");
    }

    /**
     * Three presolvers with the following return pattern:
     * <pre>
     *   1st: false, false, false
     *   2nd: true , false, false
     *   3rd: true , true , false
     * </pre>
     * The test verifies the short‑circuit behaviour and that the total number
     * of invocations matches the specification.
     */
    @Test
    void testMultiplePresolversComplexOrder() {
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Expression expr = model.newExpression("c").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        expr.set(a, BigDecimal.ONE);
        // Presolver 1: always false
        TestPresolver p1 = new TestPresolver(0, Arrays.asList(false, false, false));
        // Presolver 2: true on first call, then false
        TestPresolver p2 = new TestPresolver(1, Arrays.asList(true, false, false));
        // Presolver 3: true on first two calls, then false
        TestPresolver p3 = new TestPresolver(2, Arrays.asList(true, true, false));
        ExpressionsBasedModel.addPresolver(p1);
        ExpressionsBasedModel.addPresolver(p2);
        ExpressionsBasedModel.addPresolver(p3);
        model.presolve();
        // Invocation expectations:
        // Iteration 1: p1(false) -> p2(true) -> p3 skipped
        // Iteration 2: p1(false) -> p2(false) -> p3(true)
        // Iteration 3: p1(false) -> p2(false) -> p3(false)
        assertEquals(3, p1.getInvocationCount(), "Presolver 1 should be called in all three iterations");
        assertEquals(2, p2.getInvocationCount(), "Presolver 2 should be called in iterations 1 and 2");
        assertEquals(2, p3.getInvocationCount(), "Presolver 3 should be called in iterations 2 and 3");
    }
}
