package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.TestUtils;
import org.ojalgo.function.constant.BigMath;
import static org.mockito.Mockito.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setKnownSolution_Test {

    private ExpressionsBasedModel model;

    private Optimisation.Result optimalResult;

    /**
     * Simple {@link BiConsumer} that records whether it has been invoked.
     */
    private static class RecordingHandler implements BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> {

        boolean invoked = false;

        @Override
        public void accept(ExpressionsBasedModel m, Access1D<BigDecimal> a) {
            invoked = true;
        }
    }

    class MyResultSubclass extends Optimisation.Result {

        private final BigDecimal value;

        MyResultSubclass(BigDecimal v) {
            this.value = v;
        }

        @Override
        public BigDecimal get(int index) {
            return value;
        }
        // other abstract members can be left with default (or dummy) implementations
    }

    class MyResultSubclass extends Optimisation.Result {

        private final BigDecimal value;

        MyResultSubclass(BigDecimal v) {
            this.value = v;
        }

        @Override
        public BigDecimal get(int index) {
            return value;
        }
        // other abstract members can be left with default (or dummy) implementations
    }

    /**
     * Build a tiny optimisation problem whose optimal solution is known
     * (x = 5). The model is recreated before each test to keep the tests
     * independent.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // a single variable 0 ≤ x ≤ 10, weight 1 → contributes to the objective
        Variable x = model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.TEN).weight(BigMath.ONE);
        // a constraint forcing x to be exactly 5
        model.newExpression("c").set(x, BigMath.ONE).lower(BigMath.valueOf(5)).upper(BigMath.valueOf(5));
        // minimise the model – the result is deterministic and will be used as the
        // “known solution” in the tests.
        optimalResult = model.minimise();
        TestUtils.assertTrue(model.validate(optimalResult));
        TestUtils.assertTrue(optimalResult.getState().isOptimal());
        // sanity‑check the solution value (should be 5)
        BigDecimal xValue = optimalResult.get(model.indexOf(x));
        assertEquals(BigDecimal.valueOf(5), xValue);
        model = new ExpressionsBasedModel();
    }

    /**
     * Creates a Mockito mock of {@link Optimisation.Result} with a readable name.
     */
    private Optimisation.Result mockResult(String name) {
        return mock(Optimisation.Result.class, name);
    }

    @Test
    void testSetKnownSolutionStoresResult() {
        // Build a tiny deterministic model (same as the original template)
        Variable x = model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.TEN).weight(BigMath.ONE);
        model.newExpression("c").set(x, BigMath.ONE).lower(BigMath.valueOf(5)).upper(BigMath.valueOf(5));
        Optimisation.Result optimalResult = model.minimise();
        assertTrue(model.validate(optimalResult));
        assertTrue(optimalResult.getState().isOptimal());
        assertEquals(BigDecimal.valueOf(5), optimalResult.get(model.indexOf(x)));
        // Act
        model.setKnownSolution(optimalResult);
        // Assert – the stored known solution must be the exact same instance
        Optimisation.Result stored = model.getKnownSolution();
        assertSame(optimalResult, stored, "The known solution should be stored unchanged");
    }

    @Test
    void testSetKnownSolutionNullThrows() {
        // Act / Assert
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null), "Calling setKnownSolution(null) must throw NPE");
    }

    @Test
    void testSetKnownSolutionWithCustomHandler() {
        Optimisation.Result result = mockResult("ResultStub-5.5");
        @SuppressWarnings("unchecked")
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler = mock(BiConsumer.class);
        model.setKnownSolution(result, customHandler);
        assertSame(result, model.getKnownSolution(), "Getter must return the supplied result");
        assertSame(customHandler, model.getValidationFailureHandler(), "Getter must return the supplied custom handler");
    }

    @Test
    void testSetKnownSolutionNullHandlerThrows() {
        Optimisation.Result result = mockResult("ResultStub-0.0");
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(result, null), "Passing a null handler must result in NPE");
    }

    @Test
    void testReplaceExistingSolution() {
        Optimisation.Result first = mockResult("ResultStub-1.0");
        Optimisation.Result second = mockResult("ResultStub-2.0");
        model.setKnownSolution(first);
        assertSame(first, model.getKnownSolution(), "First call stores first result");
        model.setKnownSolution(second);
        assertSame(second, model.getKnownSolution(), "Second call replaces with new result");
    }

    @Test
    void testHandlerNotInvokedDuringSet() {
        Optimisation.Result result = mockResult("ResultStub-3.14");
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> throwingHandler = (m, a) -> {
            throw new AssertionError("Handler must not be invoked by setKnownSolution");
        };
        // No exception should be thrown even though the handler would explode if called
        model.setKnownSolution(result, throwingHandler);
        assertSame(result, model.getKnownSolution(), "Result must be stored");
        assertSame(throwingHandler, model.getValidationFailureHandler(), "Handler must be stored unchanged");
    }

    @Test
    void testIdempotentSameResultAndHandler() {
        Optimisation.Result result = mockResult("ResultStub-7.0");
        @SuppressWarnings("unchecked")
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = mock(BiConsumer.class);
        model.setKnownSolution(result, handler);
        Optimisation.Result afterFirst = model.getKnownSolution();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> afterFirstHandler = model.getValidationFailureHandler();
        model.setKnownSolution(result, handler);
        Optimisation.Result afterSecond = model.getKnownSolution();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> afterSecondHandler = model.getValidationFailureHandler();
        assertSame(afterFirst, afterSecond, "Result reference must stay identical");
        assertSame(afterFirstHandler, afterSecondHandler, "Handler reference must stay identical");
        // No interaction with the handler should have happened
        verify(handler, never()).accept(any(), any());
    }

    @Test
    void testReplaceHandlerWhileKeepingSameSolution() {
        Optimisation.Result result = mockResult("ResultStub-2.5");
        // first call – default handler
        model.setKnownSolution(result);
        assertSame(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "Default handler must be Validator.NULL");
        // second call – custom handler, same result
        @SuppressWarnings("unchecked")
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> anotherHandler = mock(BiConsumer.class);
        model.setKnownSolution(result, anotherHandler);
        assertSame(result, model.getKnownSolution(), "Result must stay the same");
        assertSame(anotherHandler, model.getValidationFailureHandler(), "Handler must be replaced with the newly supplied one");
    }

    @Test
    void testStoreCustomSubclassOfResult() {
        // Create a mock that pretends to be a user‑defined subclass.
        class MyResultSubclass extends Optimisation.Result {

            private final BigDecimal value;

            MyResultSubclass(BigDecimal v) {
                this.value = v;
            }

            @Override
            public BigDecimal get(int index) {
                return value;
            }
            // other abstract members can be left with default (or dummy) implementations
        }
        MyResultSubclass myResult = new MyResultSubclass(BigDecimal.valueOf(9.99));
        model.setKnownSolution(myResult);
        Optimisation.Result stored = model.getKnownSolution();
        assertSame(myResult, stored, "The stored instance must be the exact object supplied");
        assertEquals(MyResultSubclass.class, stored.getClass(), "The class of the stored result must be the custom subclass");
        assertSame(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "Default handler must be used when the single‑argument overload is called");
    }

    @Test
    void testStoreResultWithNaNValue() {
        // A mock result that returns a NaN BigDecimal when queried.
        Optimisation.Result nanResult = mockResult("ResultStub-NaN");
        // BigDecimal does not support NaN directly; we simulate by returning null
        // (the actual numeric content is irrelevant for the setKnownSolution contract).
        when(nanResult.get(anyInt())).thenReturn(null);
        model.setKnownSolution(nanResult);
        assertSame(nanResult, model.getKnownSolution(), "Result containing special numeric values must be stored unchanged");
        assertSame(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "Default handler must be used");
    }
}
