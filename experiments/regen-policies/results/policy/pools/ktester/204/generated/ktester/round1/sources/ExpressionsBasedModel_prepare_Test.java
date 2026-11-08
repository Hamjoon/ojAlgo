import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_prepare_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple dummy solver used in some tests.
     */
    class DummySolver {

        final ExpressionsBasedModel receivedModel;

        DummySolver(ExpressionsBasedModel receivedModel) {
            this.receivedModel = receivedModel;
        }
    }

    /**
     * A concrete subclass used to test returning a concrete subclass from the factory.
     */
    static final class AdvancedSolver {

        final ExpressionsBasedModel receivedModel;

        AdvancedSolver(ExpressionsBasedModel model) {
            this.receivedModel = model;
        }
    }

    class RecordingFactory implements Function<ExpressionsBasedModel, DummySolver> {

        int callCount = 0;

        ExpressionsBasedModel receivedModel = null;

        @Override
        public DummySolver apply(ExpressionsBasedModel m) {
            callCount++;
            receivedModel = m;
            return new DummySolver(m);
        }
    }

    class RecordingFactory implements Function<ExpressionsBasedModel, DummySolver> {

        int callCount = 0;

        ExpressionsBasedModel receivedModel = null;

        @Override
        public DummySolver apply(ExpressionsBasedModel m) {
            callCount++;
            receivedModel = m;
            return new DummySolver(m);
        }
    }

    /**
     * Simple dummy solver used in some tests.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources, but we clear any
        // static state that could affect other tests.
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.clearIntegrations();
    }

    /**
     * Verify that a Mockito mock {@link Function} is invoked with the
     * correct {@link ExpressionsBasedModel} instance and that the
     * value returned by {@code prepare} is the value returned by the
     * factory.
     */
    @Test
    void testPrepareWithMockFactory() {
        // Arrange – a mock factory and a dummy solver (also a mock)
        @SuppressWarnings("unchecked")
        Function<ExpressionsBasedModel, ExpressionsBasedModel.IntermediateSolver> mockFactory = mock(Function.class);
        ExpressionsBasedModel.IntermediateSolver dummySolver = mock(ExpressionsBasedModel.IntermediateSolver.class);
        when(mockFactory.apply(any())).thenReturn(dummySolver);
        // Act
        ExpressionsBasedModel.IntermediateSolver result = model.prepare(mockFactory);
        // Assert
        assertSame(dummySolver, result, "prepare should return exactly what the factory returns");
        verify(mockFactory, times(1)).apply(model);
    }

    /**
     * Verify that a real lambda implementation works as expected.
     * The lambda creates a simple concrete implementation of
     * {@link ExpressionsBasedModel.IntermediateSolver} that keeps a reference
     * to the model it received.  The test checks that the reference points
     * to the original model.
     */
    @Test
    void testPrepareWithRealLambda() {
        DummySolver solver = model.prepare(DummySolver::new);
        assertNotNull(solver, "Solver should not be null");
        assertSame(model, solver.receivedModel, "Factory must receive the original model instance");
    }

    /**
     * Factory returns {@code null}; {@code prepare} must propagate the null.
     */
    @Test
    void testPrepareFactoryReturnsNull() {
        Function<ExpressionsBasedModel, DummySolver> factory = m -> null;
        assertNull(model.prepare(factory), "prepare should return null when the factory returns null");
    }

    /**
     * Factory throws an {@link IllegalStateException}; the exception must propagate.
     */
    @Test
    void testPrepareFactoryThrowsIllegalStateException() {
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            throw new IllegalStateException("factory failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.prepare(factory), "Exception from factory must propagate");
        assertEquals("factory failure", ex.getMessage());
    }

    /**
     * Passing a {@code null} factory should result in a {@link NullPointerException}.
     */
    @Test
    void testPrepareWithNullFactory() {
        assertThrows(NullPointerException.class, () -> model.prepare(null), "Calling prepare with a null factory must throw NPE");
    }

    /**
     * Using a constructor method reference as the factory.
     */
    @Test
    void testPrepareWithMethodReferenceFactory() {
        DummySolver solver = model.prepare(DummySolver::new);
        assertNotNull(solver, "Solver created via method reference must not be null");
        assertSame(model, solver.receivedModel, "Method‑reference factory must receive the original model");
    }

    /**
     * Factory performs a side‑effect on the model before returning the solver.
     */
    @Test
    void testPrepareFactoryWithSideEffect() {
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            m.addVariable("sideEffectVar");
            return new DummySolver(m);
        };
        DummySolver solver = model.prepare(factory);
        assertNotNull(solver, "Solver should not be null");
        assertSame(model, solver.receivedModel, "Solver must reference the original model");
        // The model should now contain exactly one variable (the side‑effect).
        assertEquals(1, model.countVariables(), "Model must contain the variable added by the factory");
    }

    /**
     * Factory returns a concrete subclass of {@link IntermediateSolver}.
     */
    @Test
    void testPrepareFactoryReturnsSubclass() {
        Function<ExpressionsBasedModel, AdvancedSolver> factory = AdvancedSolver::new;
        AdvancedSolver solver = model.prepare(factory);
        assertNotNull(solver, "AdvancedSolver instance must not be null");
        assertSame(model, solver.receivedModel, "AdvancedSolver must receive the original model");
    }

    /**
     * Factory throws after performing a side‑effect; the exception propagates and side‑effect remains.
     */
    @Test
    void testPrepareFactoryThrowsAfterSideEffect() {
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            m.addVariable("partialVar");
            throw new IllegalArgumentException("factory error");
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> model.prepare(factory), "Exception after side‑effect must propagate");
        assertEquals("factory error", ex.getMessage());
        // Verify that the side‑effect (added variable) is still present.
        assertEquals(1, model.countVariables(), "Variable added before the exception must remain in the model");
    }

    /**
     * Factory throws a {@link NullPointerException} with a custom message; it must propagate unchanged.
     */
    @Test
    void testPrepareFactoryThrowsNullPointerException() {
        Function<ExpressionsBasedModel, DummySolver> factory = m -> {
            throw new NullPointerException("factory NPE");
        };
        NullPointerException ex = assertThrows(NullPointerException.class, () -> model.prepare(factory), "NullPointerException from factory must propagate");
        assertEquals("factory NPE", ex.getMessage());
    }

    /**
     * Verify that a factory is invoked with the correct {@link ExpressionsBasedModel}
     * instance and that the value returned by {@code prepare} is the value returned by the
     * factory.
     */
    @Test
    void testPrepareWithRecordingFactory() {
        // Arrange – a factory that records its invocation
        class RecordingFactory implements Function<ExpressionsBasedModel, DummySolver> {

            int callCount = 0;

            ExpressionsBasedModel receivedModel = null;

            @Override
            public DummySolver apply(ExpressionsBasedModel m) {
                callCount++;
                receivedModel = m;
                return new DummySolver(m);
            }
        }
        RecordingFactory factory = new RecordingFactory();
        // Act
        DummySolver result = model.prepare(factory);
        // Assert
        assertNotNull(result, "Factory must return a non‑null solver");
        assertSame(factory.receivedModel, model, "Factory must receive the original model instance");
        assertEquals(1, factory.callCount, "Factory must be invoked exactly once");
        assertSame(result, factory.apply(model), "prepare should return exactly what the factory returns");
    }
}
