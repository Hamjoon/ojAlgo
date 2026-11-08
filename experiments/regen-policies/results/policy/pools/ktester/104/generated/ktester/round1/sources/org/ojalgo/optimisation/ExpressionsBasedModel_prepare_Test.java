package org.ojalgo.optimisation;

import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_prepare_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple concrete implementation of {@link IntermediateSolver} used for the test.
     */
    private static final class DummySolver extends IntermediateSolver {
        // marker implementation – no behaviour required for the tests
    }

    /**
     * A factory that records how many times it has been invoked.
     */
    private static final class RecordingFactory<T extends IntermediateSolver> implements Function<ExpressionsBasedModel, T> {

        private final T result;

        private final AtomicInteger counter = new AtomicInteger();

        RecordingFactory(T result) {
            this.result = result;
        }

        @Override
        public T apply(ExpressionsBasedModel model) {
            counter.incrementAndGet();
            return result;
        }

        int getInvocationCount() {
            return counter.get();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> exceptionFactories() {
        return Stream.of(Arguments.of("NullPointerException from factory", (Function<ExpressionsBasedModel, DummySolver>) m -> {
            throw new NullPointerException("factory NPE");
        }, NullPointerException.class, "factory NPE"), Arguments.of("IllegalArgumentException from factory", (Function<ExpressionsBasedModel, DummySolver>) m -> {
            throw new IllegalArgumentException("bad argument");
        }, IllegalArgumentException.class, "bad argument"), Arguments.of("ArithmeticException from factory", (Function<ExpressionsBasedModel, DummySolver>) m -> {
            throw new ArithmeticException("divide by zero");
        }, ArithmeticException.class, "divide by zero"), Arguments.of("ProgrammingError from factory", (Function<ExpressionsBasedModel, DummySolver>) m -> {
            throw new ProgrammingError("programming error");
        }, ProgrammingError.class, "programming error"), Arguments.of("AssertionError from factory", (Function<ExpressionsBasedModel, DummySolver>) m -> {
            throw new AssertionError("assertion failed");
        }, AssertionError.class, "assertion failed"));
    }

    /**
     * Helper method to create a list of variables – the {@code ExpressionsBasedModel}
     * API returns an immutable list, so we copy it to an {@link ArrayList} for
     * convenient iteration.
     */
    private static List<Variable> copyVariables(ExpressionsBasedModel model) {
        return new ArrayList<>(model.getVariables());
    }

    @Test
    void testPrepareInvokesFactoryAndReturnsResult() {
        DummySolver expectedSolver = new DummySolver();
        RecordingFactory<DummySolver> factory = new RecordingFactory<>(expectedSolver);
        DummySolver actualSolver = model.prepare(factory);
        assertSame(expectedSolver, actualSolver, "prepare should return the solver created by the factory");
        assertEquals(1, factory.getInvocationCount(), "factory must be invoked exactly once");
    }

    @Test
    void testPrepareFactoryReturnsConstantString() {
        Function<ExpressionsBasedModel, String> factory = m -> "prepared";
        String result = model.prepare(factory);
        assertEquals("prepared", result);
    }

    @Test
    void testPrepareFactoryReturnsSameModelInstance() {
        Function<ExpressionsBasedModel, ExpressionsBasedModel> factory = m -> m;
        ExpressionsBasedModel result = model.prepare(factory);
        assertSame(model, result);
    }

    @Test
    void testPrepareFactoryThrowsRuntimeExceptionIsPropagated() {
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            throw new IllegalStateException("factory failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.prepare(factory));
        assertEquals("factory failure", ex.getMessage());
    }

    @Test
    void testPrepareNullFactoryThrowsNPE() {
        Function<ExpressionsBasedModel, ? extends IntermediateSolver> factory = null;
        assertThrows(NullPointerException.class, () -> model.prepare(factory));
    }

    @Test
    void testPrepareFactoryReturnsNullIsForwarded() {
        RecordingFactory<DummySolver> factory = new RecordingFactory<>(null);
        DummySolver result = model.prepare(factory);
        assertNull(result, "null result from factory must be forwarded unchanged");
        assertEquals(1, factory.getInvocationCount(), "factory must be invoked exactly once");
    }

    @Test
    void testPrepareFactoryReturnsArbitraryObject() {
        Object expected = new Object();
        Function<ExpressionsBasedModel, Object> factory = m -> expected;
        Object result = model.prepare(factory);
        assertSame(expected, result);
    }

    @Test
    void testPrepareFactoryReturnsDummySolver() {
        DummySolver expected = new DummySolver();
        Function<ExpressionsBasedModel, DummySolver> factory = m -> expected;
        DummySolver result = model.prepare(factory);
        assertSame(expected, result);
    }

    @Test
    void testPrepareFactoryWithSideEffectAddsVariable() {
        assertEquals(0, model.countVariables(), "model should start with no variables");
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            m.addVariable("tempVar");
            return new DummySolver();
        };
        DummySolver result = model.prepare(factory);
        assertNotNull(result);
        assertEquals(1, model.countVariables(), "model must contain the variable added by the factory");
        List<Variable> vars = model.getVariables();
        assertTrue(vars.stream().anyMatch(v -> "tempVar".equals(v.getName())), "Variable named 'tempVar' should be present");
    }

    @Test
    void testPrepareFactoryMethodReferenceAtomicInteger() {
        // Use a method reference that increments its own counter
        Function<ExpressionsBasedModel, Integer> incFactory = new AtomicInteger(0)::incrementAndGet;
        Integer first = model.prepare(incFactory);
        Integer second = model.prepare(incFactory);
        assertEquals(1, first);
        assertEquals(2, second);
    }

    @Test
    void testPrepareReturnsOptimisationResult() {
        // Build a simple Optimisation.Result using the public factory (state OPTIMAL)
        Optimisation.Result expected = Optimisation.Result.FACTORY.makeResult(Optimisation.State.OPTIMAL, null, null);
        Function<ExpressionsBasedModel, Optimisation.Result> factory = m -> expected;
        Optimisation.Result actual = model.prepare(factory);
        assertSame(expected, actual);
        assertEquals(Optimisation.State.OPTIMAL, actual.getState());
    }

    @Test
    void testPrepareReturnsLinearSolver() {
        // LinearSolver implements IntermediateSolver
        LinearSolver linearSolver = new LinearSolver();
        Function<ExpressionsBasedModel, LinearSolver> factory = m -> linearSolver;
        LinearSolver result = model.prepare(factory);
        assertSame(linearSolver, result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionFactories")
    void testPreparePropagatesExceptions(String description, Function<ExpressionsBasedModel, ?> factory, Class<? extends Throwable> expectedType, String expectedMessage) {
        Throwable thrown = assertThrows(expectedType, () -> model.prepare(factory));
        assertEquals(expectedMessage, thrown.getMessage());
    }
}