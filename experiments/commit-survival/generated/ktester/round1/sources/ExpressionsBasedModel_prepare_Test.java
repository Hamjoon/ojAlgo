package org.ojalgo.optimisation;

import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_prepare_Test {

    /**
     * Create a fresh model before each test.
     */
    private ExpressionsBasedModel model;

    /**
     * Minimal concrete implementation of {@link IntermediateSolver}.
     */
    private static final class DummySolver implements IntermediateSolver {

        private final ExpressionsBasedModel model;

        DummySolver(ExpressionsBasedModel model) {
            this.model = model;
        }
        // No behaviour needed for the tests
    }

    /**
     * Another concrete implementation to test custom return types.
     */
    private static final class CustomSolver implements IntermediateSolver {

        private final ExpressionsBasedModel model;

        CustomSolver(ExpressionsBasedModel model) {
            this.model = model;
        }
    }

    private static class InvocationCounter {

        private static final AtomicInteger COUNT = new AtomicInteger(0);

        static void increment() {
            COUNT.incrementAndGet();
        }

        static int get() {
            return COUNT.get();
        }

        static void reset() {
            COUNT.set(0);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
        // default options
        model = new ExpressionsBasedModel();
    }

    /**
     * Verify that the factory is called once and its result is returned.
     */
    @Test
    void testPrepareInvokesFactoryAndReturnsResult() {
        // GIVEN a mock factory that returns a concrete IntermediateSolver
        @SuppressWarnings("unchecked")
        Function<ExpressionsBasedModel, DefaultIntermediate> factory = mock(Function.class);
        DefaultIntermediate expectedSolver = new DefaultIntermediate();
        when(factory.apply(model)).thenReturn(expectedSolver);
        // WHEN the model prepares a solver via the factory
        DefaultIntermediate actualSolver = model.prepare(factory);
        // THEN the returned instance is exactly the one produced by the factory
        assertSame(expectedSolver, actualSolver, "prepare should return the factory's result");
        // AND the factory was invoked exactly once with the current model instance
        verify(factory, times(1)).apply(model);
    }

    /**
     * Verify that a {@code null} factory causes a NullPointerException.
     */
    @Test
    void testPrepareNullFactoryThrowsNPE() {
        // GIVEN a null factory
        Function<ExpressionsBasedModel, ? extends IntermediateSolver> nullFactory = null;
        // WHEN/THEN a NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> model.prepare(nullFactory), "Calling prepare with a null factory must throw NullPointerException");
    }

    @Test
    void testPrepareReturnsDummySolverCreatedByFactory() {
        // GIVEN a factory that creates a new DummySolver using the supplied model
        Function<ExpressionsBasedModel, DummySolver> factory = m -> new DummySolver(m);
        // WHEN prepare is called
        DummySolver result = model.prepare(factory);
        // THEN the returned instance is a DummySolver and references the same model
        assertNotNull(result, "Factory should produce a non‑null DummySolver");
        assertSame(model, result.model, "DummySolver should hold the original model instance");
    }

    @Test
    void testPrepareReturnsModelWhenFactoryReturnsItself() {
        // GIVEN a factory that simply returns the model it receives
        Function<ExpressionsBasedModel, ExpressionsBasedModel> factory = m -> m;
        // WHEN prepare is called
        ExpressionsBasedModel result = model.prepare(factory);
        // THEN the same model instance is returned
        assertSame(model, result, "Factory returning the model should result in the same instance");
    }

    @Test
    void testPrepareFactoryThrowsRuntimeExceptionIsPropagated() {
        // GIVEN a factory that throws an unchecked exception
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            throw new IllegalStateException("factory failure");
        };
        // WHEN/THEN the same exception propagates unchanged
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.prepare(factory), "Exception from factory should propagate");
        assertEquals("factory failure", ex.getMessage());
    }

    @Test
    void testPrepareFactoryReturnsNullReturnsNull() {
        // GIVEN a factory that legitimately returns null
        Function<ExpressionsBasedModel, DummySolver> factory = m -> null;
        // WHEN prepare is called
        DummySolver result = model.prepare(factory);
        // THEN the result is null (no NPE)
        assertNull(result, "prepare should return null when factory returns null");
    }

    @Test
    void testPrepareFactoryAddsVariableSideEffect() {
        // GIVEN a factory that adds a variable before returning a solver
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            m.addVariable("x");
            return new DummySolver(m);
        };
        // WHEN prepare is executed
        DummySolver solver = model.prepare(factory);
        // THEN the model now contains exactly one variable named "x"
        assertNotNull(solver, "Solver should be created");
        assertEquals(1, model.countVariables(), "Model should have one variable after factory side‑effect");
        Variable var = model.getVariable(0);
        assertEquals("x", var.getName(), "Variable name should match the one added by the factory");
    }

    @Test
    void testFactoryInvocationCountIsExactlyOne() {
        // Reset static counter before the test
        InvocationCounter.reset();
        // GIVEN a factory that increments a static counter
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            InvocationCounter.increment();
            return new DummySolver(m);
        };
        // WHEN prepare is called
        DummySolver solver = model.prepare(factory);
        // THEN the solver is returned and the counter is exactly 1
        assertNotNull(solver, "Solver should be created");
        assertEquals(1, InvocationCounter.get(), "Factory must be invoked exactly once");
    }

    @Test
    void testPrepareReturnsCustomSolverInstance() {
        // GIVEN a factory that returns a CustomSolver
        Function<ExpressionsBasedModel, CustomSolver> factory = m -> new CustomSolver(m);
        // WHEN prepare is called
        CustomSolver solver = model.prepare(factory);
        // THEN the exact CustomSolver instance is returned
        assertNotNull(solver, "CustomSolver should be created");
        assertSame(model, solver.model, "CustomSolver should hold the original model");
    }

    @Test
    void testPrepareCalledOnNullModelReferenceThrowsNPE() {
        // GIVEN a null model reference
        ExpressionsBasedModel nullModel = null;
        Function<ExpressionsBasedModel, DummySolver> factory = m -> new DummySolver(m);
        // WHEN/THEN invoking prepare on a null reference throws NPE before any factory call
        assertThrows(NullPointerException.class, () -> nullModel.prepare(factory), "Calling prepare on a null model reference must throw NullPointerException");
    }
}
