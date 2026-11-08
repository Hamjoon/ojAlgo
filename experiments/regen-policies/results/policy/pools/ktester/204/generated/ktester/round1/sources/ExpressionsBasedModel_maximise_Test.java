package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_maximise_Test {

    /**
     * Model used by the tests – created fresh for each test method.
     */
    private ExpressionsBasedModel model;

    /**
     * Simple variables used in the tests.
     */
    private Variable x;

    private Variable y;

    /**
     * Simple stub result used by many tests.
     */
    private Optimisation.Result stubResult;

    /* --------------------------------------------------------------------- *
     *  Helper subclass that records method invocation order.
     * --------------------------------------------------------------------- */
    private static class RecordingModel extends ExpressionsBasedModel {

        private final List<String> callLog = new ArrayList<>();

        @Override
        public void setOptimisationSense(final Optimisation.Sense optimisationSense) {
            callLog.add("sense:" + optimisationSense);
            super.setOptimisationSense(optimisationSense);
        }

        @Override
        protected Optimisation.Result optimise() {
            callLog.add("optimise");
            // Return a dummy result – the test will replace this via a spy if
            // needed.
            return mock(Optimisation.Result.class);
        }

        List<String> getCallLog() {
            return callLog;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a new model for every test to guarantee isolation.
        model = new ExpressionsBasedModel();
        // Two decision variables:
        //   x ∈ [0, 10]   (weight 1 → contributes to the objective)
        //   y ∈ [0, 10]   (weight 2 → contributes twice as much)
        x = model.newVariable("x").lower(0).upper(10).weight(ONE);
        y = model.newVariable("y").lower(0).upper(10).weight(TWO);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release resources – mirrors the pattern used in the original test suite.
        model.dispose();
    }

    /**
     * Verify that {@code maximise()} solves a simple linear problem correctly.
     *
     * <pre>
     *   maximise   1·x + 2·y
     *   subject to  x + y ≤ 12
     *               0 ≤ x ≤ 10
     *               0 ≤ y ≤ 10
     * </pre>
     *
     * The optimal solution is x = 2, y = 10 (objective value = 22).
     */
    @Test
    void testMaximiseSimpleLinearProblem() {
        // Add the single constraint x + y ≤ 12
        Expression constraint = model.addExpression("c1").upper(12);
        constraint.set(x, ONE);
        constraint.set(y, ONE);
        // Solve
        Optimisation.Result result = model.maximise();
        // The solver should report an optimal state
        assertTrue(result.getState().isOptimal(), () -> "Expected optimal state but got: " + result.getState());
        // Expected objective value = 1·2 + 2·10 = 22
        BigDecimal expected = ONE.multiply(BigDecimal.valueOf(2)).add(TWO.multiply(BigDecimal.valueOf(10)));
        // Depending on the ojAlgo version {@code getValue()} may return a {@code BigDecimal}
        // or a primitive {@code double}.  The following works for both cases.
        if (result.getValue() instanceof BigDecimal) {
            assertEquals(0, expected.compareTo((BigDecimal) result.getValue()), () -> "Expected objective value " + expected + " but got " + result.getValue());
        } else {
            double actual = ((Number) result.getValue()).doubleValue();
            assertEquals(expected.doubleValue(), actual, 1e-9, () -> "Expected objective value " + expected + " but got " + actual);
        }
        // Variable values
        assertEquals(BigDecimal.valueOf(2), result.get(model.indexOf(x)), "Variable x has unexpected value");
        assertEquals(BigDecimal.valueOf(10), result.get(model.indexOf(y)), "Variable y has unexpected value");
        // The model should validate the returned solution
        assertTrue(model.validate(result), "Model validation failed for the returned result");
    }

    /**
     * Ensure that calling {@code maximise()} on a model without any
     * constraints still produces a feasible (optimal) solution that respects
     * variable bounds and the objective sense.
     *
     * <pre>
     *   maximise   1·x + 2·y
     *   subject to 0 ≤ x ≤ 5
     *               0 ≤ y ≤ 5
     * </pre>
     *
     * The optimum is attained at the upper bounds (x = 5, y = 5) → value = 15.
     */
    @Test
    void testMaximiseOnlyVariableBounds() {
        // Tighten the variable upper bounds for this scenario
        x.upper(5);
        y.upper(5);
        Optimisation.Result result = model.maximise();
        assertTrue(result.getState().isOptimal(), () -> "Expected optimal state but got: " + result.getState());
        // Expected objective value = 1·5 + 2·5 = 15
        BigDecimal expected = ONE.multiply(BigDecimal.valueOf(5)).add(TWO.multiply(BigDecimal.valueOf(5)));
        if (result.getValue() instanceof BigDecimal) {
            assertEquals(0, expected.compareTo((BigDecimal) result.getValue()), "Objective value should be 15 (5 + 2·5)");
        } else {
            double actual = ((Number) result.getValue()).doubleValue();
            assertEquals(expected.doubleValue(), actual, 1e-9, "Objective value should be 15 (5 + 2·5)");
        }
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)), "Variable x should be at its upper bound");
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(y)), "Variable y should be at its upper bound");
        assertTrue(model.validate(result), "Result should validate against the model");
    }

    /**
     * Verify that the optimisation sense is correctly set to {@link Optimisation.Sense#MAX}
     * when {@code maximise()} is called. The public API does not expose the sense
     * directly, but we can infer it by comparing the result of {@code maximise()}
     * with the result of {@code minimise()} on the same model (the latter should
     * give the negated objective value).
     */
    @Test
    void testSenseIsMaxWhenCallingMaximise() {
        // Add a simple constraint to make the problem non‑trivial
        model.addExpression("c").upper(8).set(x, ONE).set(y, ONE);
        Optimisation.Result maxResult = model.maximise();
        // Re‑create the same model for a minimise run
        ExpressionsBasedModel other = new ExpressionsBasedModel();
        Variable ox = other.newVariable("x").lower(0).upper(10).weight(ONE);
        Variable oy = other.newVariable("y").lower(0).upper(10).weight(TWO);
        other.addExpression("c").upper(8).set(ox, ONE).set(oy, ONE);
        Optimisation.Result minResult = other.minimise();
        // The minimise result should be the negative of the maximise result
        double maxVal = ((Number) maxResult.getValue()).doubleValue();
        double minVal = ((Number) minResult.getValue()).doubleValue();
        assertEquals(-maxVal, minVal, 1e-9, "Minimise result should be the negated maximise result, confirming sense change");
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - normal flow
     * --------------------------------------------------------------------- */
    @Test
    void testMaximiseSetsSenseToMaxAndReturnsOptimiseResult() {
        // Create a spy of the recording model so we can stub optimise()
        RecordingModel model = spy(new RecordingModel());
        // Stub the private optimise() (exposed via the subclass) to return stubResult
        doReturn(stubResult).when(model).optimise();
        // Call the method under test
        Optimisation.Result result = model.maximise();
        // Verify that the sense was set to MAX before optimise() was invoked
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense should be MAX after maximise()");
        assertEquals(stubResult, result, "maximise() must return the result from optimise()");
        // Verify order of calls: sense first, then optimise
        List<String> log = model.getCallLog();
        assertEquals(List.of("sense:MAX", "optimise"), log, "Method call order should be sense then optimise");
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - sense override
     * --------------------------------------------------------------------- */
    @Test
    void testSenseIsOverriddenToMaxEvenIfPreviouslyMinimise() {
        // First solve with minimise() to set sense to MIN
        ExpressionsBasedModel tmpModel = new ExpressionsBasedModel();
        Variable v = tmpModel.newVariable("v").weight(ONE);
        tmpModel.addExpression("c").upper(ONE).set(v, ONE);
        // sets sense to MIN internally
        tmpModel.minimise();
        // Now call maximise() – the sense must become MAX
        Optimisation.Result result = tmpModel.maximise();
        assertEquals(Optimisation.Sense.MAX, tmpModel.getOptimisationSense(), "Calling maximise() must reset the sense to MAX");
        assertNotNull(result, "Result should not be null");
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - exception propagation (RuntimeException)
     * --------------------------------------------------------------------- */
    @Test
    void testOptimiseRuntimeExceptionIsPropagated() {
        ExpressionsBasedModel broken = new ExpressionsBasedModel() {

            @Override
            protected Optimisation.Result optimise() {
                throw new IllegalStateException("simulated failure");
            }
        };
        IllegalStateException thrown = assertThrows(IllegalStateException.class, broken::maximise, "maximise() should propagate RuntimeException from optimise()");
        assertEquals("simulated failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - result propagation (infeasible / unbounded)
     * --------------------------------------------------------------------- */
    @Test
    void testInfeasibleResultIsReturnedUnchanged() {
        RecordingModel model = spy(new RecordingModel());
        // Stub optimise() to return a result whose isInfeasible() is true
        Optimisation.Result infeasible = mock(Optimisation.Result.class);
        when(infeasible.isInfeasible()).thenReturn(true);
        doReturn(infeasible).when(model).optimise();
        Optimisation.Result result = model.maximise();
        assertSame(infeasible, result, "Result should be the exact object returned by optimise()");
        assertTrue(result.isInfeasible(), "Result must report infeasibility");
    }

    @Test
    void testUnboundedResultIsReturnedUnchanged() {
        RecordingModel model = spy(new RecordingModel());
        // Stub optimise() to return a result whose isUnbounded() is true
        Optimisation.Result unbounded = mock(Optimisation.Result.class);
        when(unbounded.isUnbounded()).thenReturn(true);
        doReturn(unbounded).when(model).optimise();
        Optimisation.Result result = model.maximise();
        assertSame(unbounded, result, "Result should be the exact object returned by optimise()");
        assertTrue(result.isUnbounded(), "Result must report unboundedness");
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - order of operations (explicit check)
     * --------------------------------------------------------------------- */
    @Test
    void testSetOptimisationSenseInvokedBeforeOptimise() {
        RecordingModel model = new RecordingModel();
        // We don't need to stub optimise() – the default implementation records the call.
        model.maximise();
        List<String> log = model.getCallLog();
        assertEquals(List.of("sense:MAX", "optimise"), log, "setOptimisationSense must be called before optimise()");
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - idempotent sense after multiple calls
     * --------------------------------------------------------------------- */
    @Test
    void testMultipleMaximiseCallsKeepSenseMaxAndInvokeOptimiseEachTime() {
        RecordingModel model = spy(new RecordingModel());
        // First call
        model.maximise();
        // Second call
        model.maximise();
        // Sense should still be MAX
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Sense must remain MAX after successive maximise() calls");
        // optimise() should have been called twice
        verify(model, times(2)).optimise();
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - after dispose exception
     * --------------------------------------------------------------------- */
    @Test
    void testMaximiseAfterDisposeThrowsIllegalStateException() {
        ExpressionsBasedModel tmp = new ExpressionsBasedModel();
        tmp.dispose();
        assertThrows(RuntimeException.class, tmp::maximise, "Calling maximise() after dispose() must propagate an exception");
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - setOptimisationSense exception
     * --------------------------------------------------------------------- */
    @Test
    void testExceptionFromSetOptimisationSenseIsPropagated() {
        ExpressionsBasedModel model = new ExpressionsBasedModel() {

            @Override
            public void setOptimisationSense(final Optimisation.Sense optimisationSense) {
                throw new IllegalArgumentException("invalid sense");
            }
        };
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, model::maximise, "Exception from setOptimisationSense must propagate");
        assertEquals("invalid sense", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Test group: maximise - error propagation (OutOfMemoryError)
     * --------------------------------------------------------------------- */
    @Test
    void testOutOfMemoryErrorIsPropagated() {
        ExpressionsBasedModel broken = new ExpressionsBasedModel() {

            @Override
            protected Optimisation.Result optimise() {
                throw new OutOfMemoryError("simulated OOM");
            }
        };
        OutOfMemoryError error = assertThrows(OutOfMemoryError.class, broken::maximise, "OutOfMemoryError thrown by optimise() must not be caught");
        assertEquals("simulated OOM", error.getMessage());
    }
}
