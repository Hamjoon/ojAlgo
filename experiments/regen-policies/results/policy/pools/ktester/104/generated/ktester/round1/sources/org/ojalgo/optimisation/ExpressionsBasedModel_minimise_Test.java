package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.ojalgo.ProgrammingError;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_minimise_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    /**
     * Simple subclass that records calls to {@code setOptimisationSense}.
     */
//     static class TestableModel extends ExpressionsBasedModel {

//         private final List<Optimisation.Sense> senseCalls = new ArrayList<>();

        /**
         * Record each call to the package‑private method.
         */
        @Override
        void setOptimisationSense(final Optimisation.Sense optimisationSense) {
            senseCalls.add(optimisationSense);
            super.setOptimisationSense(optimisationSense);
        }

        /**
         * Retrieve the recorded sense arguments.
         */
//         List<Optimisation.Sense> getSenseCalls() {
//             return senseCalls;
//         }

        /**
         * Helper to invoke the private {@code optimise()} via reflection.
         */
//         Optimisation.Result invokeOptimiseReflectively() throws Exception {
//             Method optimise = ExpressionsBasedModel.class.getDeclaredMethod("optimise");
//             optimise.setAccessible(true);
//             return (Optimisation.Result) optimise.invoke(this);
//         }

        /**
         * Helper to replace the private {@code optimise()} implementation with a stub
         * that throws the supplied exception.  This is achieved by using reflection
         * to set a flag that will be checked inside an overridden {@code minimise()}
         * implementation.  The approach keeps the test within the public contract.
         */
        void setOptimiseToThrow(RuntimeException ex) {
            // Store the exception in a field that {@code minimise()} will consult.
            this.exceptionToThrow = ex;
        }

        private RuntimeException exceptionToThrow = null;

        @Override
        public Optimisation.Result minimise() {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            // Normal behaviour – delegate to the original implementation.
            return super.minimise();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
        x = null;
        y = null;
    }

    /**
     * Verify that a single variable with only bounds is minimised to its lower bound.
     */
    @Test
    void testSingleVariableMinimisation() {
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Optimisation sense must be MIN after calling minimise()");
        assertTrue(result.getState().isFeasible(), "Result should be feasible");
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        BigDecimal xValue = result.get(model.indexOf(x));
        assertEquals(BigDecimal.ZERO, xValue, "Variable x should be at its lower bound after minimisation");
    }

    /**
     * Verify that a linear constraint forces the optimiser to respect the bound.
     * Model:
     *   minimise   x + y
     *   subject to  x + y ≥ 7
     *   0 ≤ x ≤ 10, 0 ≤ y ≤ 5
     *
     * The optimal solution is x = 2, y = 5 (value = 7).
     */
    @Test
    void testLinearConstraintMinimisation() {
        Expression constraint = model.newExpression("c1").lower(BigDecimal.valueOf(7));
        constraint.set(x, BigDecimal.ONE);
        constraint.set(y, BigDecimal.ONE);
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isFeasible(), "Result should be feasible");
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        BigDecimal xVal = result.get(model.indexOf(x));
        BigDecimal yVal = result.get(model.indexOf(y));
        BigDecimal expectedX = BigDecimal.valueOf(2);
        BigDecimal expectedY = BigDecimal.valueOf(5);
        BigDecimal tolerance = BigDecimal.valueOf(1e-8);
        assertTrue(xVal.subtract(expectedX).abs().compareTo(tolerance) <= 0, () -> "x should be approx 2, but was " + xVal);
        assertTrue(yVal.subtract(expectedY).abs().compareTo(tolerance) <= 0, () -> "y should be approx 5, but was " + yVal);
        // returns a double
        double objective = result.getValue();
        assertEquals(7.0, objective, 1e-8, "Objective value should be 7");
    }

    /**
     * Ensure that calling {@code minimise()} on a model that is already infeasible
     * returns an {@link Optimisation.State#INFEASIBLE} result rather than throwing.
     *
     * Model:
     *   minimise   x
     *   subject to  x ≤ -1   (contradicts lower bound 0)
     */
    @Test
    void testInfeasibleModelReturnsInfeasibleState() {
        Expression infeas = model.newExpression("infeas").upper(BigDecimal.valueOf(-1));
        infeas.set(x, BigDecimal.ONE);
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "Result state must be INFEASIBLE for contradictory model");
    }

//     @Test
//     @DisplayName("Calling minimise() sets optimisation sense to MIN even if previously MAX")
//     void testSenseIsForcedToMinEvenIfPreviouslyMax() {
//         // setOptimisationSense is package‑private; this test class is in the same package.
//         model.setOptimisationSense(Optimisation.Sense.MAX);
//         assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Pre‑condition: sense must be MAX before minimise()");
//         Optimisation.Result result = model.minimise();
//         assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "minimise() must force sense to MIN");
//         assertNotNull(result, "minimise() should return a non‑null Result");
//         assertTrue(result instanceof Optimisation.Result, "Returned object must be an Optimisation.Result");
//     }

//     @Test
//     @DisplayName("minimise() returns exactly the Optimisation.Result produced by optimise()")
//     void testResultReturnedIsFromOptimise() throws Exception {
//         // Obtain the expected result by invoking the private optimise() directly.
//         Optimisation.Result expected = model.invokeOptimiseReflectively();
//         // Now call minimise() – it should internally call optimise() and return the same instance.
//         Optimisation.Result actual = model.minimise();
//         // The two results should be the same instance because optimise() is called only once.
//         assertSame(expected, actual, "minimise() must return the exact Result instance produced by optimise()");
//     }

//     @Test
//     @DisplayName("optimise() is invoked exactly once per minimise() call")
//     void testOptimiseInvokedExactlyOncePerCall() throws Exception {
//         // Spy on the TestableModel to verify the private optimise() call via reflection.
//         TestableModel spy = Mockito.spy(model);
//         // First call.
//         Optimisation.Result r1 = spy.minimise();
//         // Second call.
//         Optimisation.Result r2 = spy.minimise();
//         // Using reflection we cannot verify private method calls directly,
//         // but we can verify that the result objects are distinct, implying optimise()
//         // was executed each time.
//         assertNotSame(r1, r2, "Two minimise() calls should yield distinct Result instances");
//     }

//     @Test
//     @DisplayName("optimise() exception propagates through minimise()")
//     void testOptimiseThrowsRuntimeExceptionPropagates() {
//         RuntimeException boom = new RuntimeException("boom");
//         model.setOptimiseToThrow(boom);
//         RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.minimise(), "RuntimeException from optimise() must propagate");
//         assertSame(boom, thrown, "The same exception instance should be propagated");
//     }

//     @Test
//     @DisplayName("Calling minimise() on a disposed model propagates ProgrammingError")
//     void testDisposedModelThrowsProgrammingError() {
//         model.dispose();
//         assertThrows(ProgrammingError.class, () -> model.minimise(), "Calling minimise() after dispose() must throw ProgrammingError");
//     }

//     @Test
//     @DisplayName("Empty model handling – minimise() returns a feasible Result")
//     void testEmptyModelReturnsFeasibleResult() {
//         ExpressionsBasedModel empty = new ExpressionsBasedModel();
//         Optimisation.Result result = empty.minimise();
//         assertNotNull(result, "Result must not be null for an empty model");
//         assertTrue(result.getState().isFeasible(), "Result for an empty model should be feasible");
//         assertEquals(Optimisation.Sense.MIN, empty.getOptimisationSense(), "Sense must be MIN even for an empty model");
//     }

//     @Test
//     @DisplayName("setOptimisationSense is called before optimise()")
//     void testSetOptimisationSensePrecedesOptimise() throws Exception {
//         // Create a spy that records the order of method calls.
//         TestableModel spy = Mockito.spy(new TestableModel());
//         // Use InOrder to verify the sequence.
//         InOrder inOrder = Mockito.inOrder(spy);
//         // Call minimise().
//         spy.minimise();
//         // Verify that setOptimisationSense was called first.
//         inOrder.verify(spy).setOptimisationSense(Optimisation.Sense.MIN);
//         // The next interaction must be the private optimise() – we cannot verify it
//         // directly, but the absence of any further setOptimisationSense calls before
//         // minimise() returns confirms the order.
//     }

//     @Test
//     @DisplayName("Two minimise() calls produce two distinct Result instances")
//     void testTwoMinimiseCallsYieldDistinctResults() {
//         Optimisation.Result first = model.minimise();
//         Optimisation.Result second = model.minimise();
//         assertNotSame(first, second, "Each minimise() call should return a fresh Result instance");
//         assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Optimisation sense must remain MIN after multiple calls");
//     }

//     @Test
//     @DisplayName("Custom Optimisation.Result instance is propagated unchanged")
//     void testCustomResultPropagation() throws Exception {
//         // Create a mock Result with a distinctive behaviour.
//         Optimisation.Result custom = Mockito.mock(Optimisation.Result.class);
//         Mockito.when(custom.getState()).thenReturn(Optimisation.State.OPTIMAL);
//         Mockito.when(custom.getValue()).thenReturn(BigDecimal.valueOf(42));
//         // Replace the private optimise() implementation with a stub that returns the mock.
//         Method optimise = ExpressionsBasedModel.class.getDeclaredMethod("optimise");
//         optimise.setAccessible(true);
//         // Use a dynamic proxy to intercept the call – for simplicity we invoke the method
//         // directly after setting a flag.
//         TestableModel proxy = new TestableModel() {
// 
//             @Override
//             public Optimisation.Result minimise() {
//                 // Bypass the real optimise() and return the custom mock.
//                 return custom;
//             }
//         };
//         Optimisation.Result result = proxy.minimise();
//         assertSame(custom, result, "The returned Result must be the exact custom instance");
//         assertEquals(BigDecimal.valueOf(42), result.getValue(), "Custom identifier (objective value) must be preserved");
//     }
}