package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getKnownSolution_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test contamination.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference – not strictly required but makes the intent clear.
        model = null;
    }

    /**
     * Provides pairs of mock results for the parameterised overwrite test.
     */
    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> overwriteScenarios() {
        Optimisation.Result r1 = Mockito.mock(Optimisation.Result.class);
        Optimisation.Result r2 = Mockito.mock(Optimisation.Result.class);
        Optimisation.Result r3 = Mockito.mock(Optimisation.Result.class);
        Optimisation.Result r4 = Mockito.mock(Optimisation.Result.class);
        return java.util.stream.Stream.of(org.junit.jupiter.params.provider.Arguments.of(r1, r2), org.junit.jupiter.params.provider.Arguments.of(r3, r4));
    }

    @Test
    void testDefaultKnownSolutionIsNull() {
        // When a newly created model has never had a known solution set,
        // getKnownSolution() must return null.
        assertNull(model.getKnownSolution(), "Expected getKnownSolution() to return null before any value is set.");
    }

    @Test
    void testSetAndGetKnownSolution() {
        // Use a Mockito mock for Optimisation.Result – we only need reference equality.
        Optimisation.Result mockResult = Mockito.mock(Optimisation.Result.class);
        model.setKnownSolution(mockResult);
        Optimisation.Result retrieved = model.getKnownSolution();
        assertSame(mockResult, retrieved, "The result returned by getKnownSolution() should be the same instance that was set.");
    }

    @Test
    void testSetKnownSolutionWithHandler() {
        Optimisation.Result mockResult = Mockito.mock(Optimisation.Result.class);
        @SuppressWarnings("unchecked")
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> mockHandler = (BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>>) Mockito.mock(BiConsumer.class);
        // Store the known solution together with a validation‑failure handler.
        model.setKnownSolution(mockResult, mockHandler);
        // Verify that the known solution is stored correctly.
        assertSame(mockResult, model.getKnownSolution(), "Known solution should be retrievable after using the overloaded setter.");
        // Verify that the handler is stored and can be retrieved.
        assertSame(mockHandler, model.getValidationFailureHandler(), "The validation‑failure handler should be the same instance that was supplied.");
    }

    @Test
    void testResetKnownSolutionToNull() {
        Optimisation.Result mockResult = Mockito.mock(Optimisation.Result.class);
        model.setKnownSolution(mockResult);
        assertNotNull(model.getKnownSolution(), "Pre‑condition check – known solution should be set.");
        // Reset to null (the API permits a null argument).
        model.setKnownSolution(null);
        assertNull(model.getKnownSolution(), "After resetting with null, getKnownSolution() should return null.");
    }

    @Test
    void testOverwriteKnownSolution() {
        Optimisation.Result firstResult = Mockito.mock(Optimisation.Result.class);
        Optimisation.Result secondResult = Mockito.mock(Optimisation.Result.class);
        model.setKnownSolution(firstResult);
        // Overwrite with a different result.
        model.setKnownSolution(secondResult);
        assertSame(secondResult, model.getKnownSolution(), "After overwriting, getKnownSolution() must return the most recent result.");
    }

    @Test
    void testSetKnownSolutionWithNullArgumentThrows() {
        // According to the specification, passing null is illegal and should
        // result in a NullPointerException (or ProgrammingError).  We assert
        // that an exception is thrown and that the internal state remains unchanged.
        // model starts with null
        Optimisation.Result original = null;
        assertNull(model.getKnownSolution(), "Pre‑condition: known solution must be null.");
        // Expect either NPE or ProgrammingError (both extend RuntimeException)
        assertThrows(RuntimeException.class, () -> model.setKnownSolution(null), "Calling setKnownSolution(null) should throw an exception.");
        // State must still be null.
        assertNull(model.getKnownSolution(), "After the failed call, getKnownSolution() should still return null.");
    }

    @Test
    void testCopyModelDoesNotRetainKnownSolution() {
        Optimisation.Result dummyResult = Mockito.mock(Optimisation.Result.class);
        model.setKnownSolution(dummyResult);
        assertNotNull(model.getKnownSolution(), "Pre‑condition: known solution must be set.");
        // Create a shallow copy (default copy() behaviour).
        ExpressionsBasedModel copy = model.copy();
        // The copy must not retain the known solution.
        assertNull(copy.getKnownSolution(), "A copied model should start with a null known solution.");
    }

    @Test
    void testKnownSolutionPersistsAfterRelaxation() {
        Optimisation.Result relaxedResult = Mockito.mock(Optimisation.Result.class);
        model.setKnownSolution(relaxedResult);
        assertSame(relaxedResult, model.getKnownSolution(), "Pre‑condition: known solution must be set before relaxation.");
        // Relax the model – this should not clear the known solution.
        model.relax();
        assertSame(relaxedResult, model.getKnownSolution(), "Known solution should remain unchanged after model.relax().");
    }

    @ParameterizedTest
    @MethodSource("overwriteScenarios")
    void testMultipleOverwriteScenarios(Optimisation.Result first, Optimisation.Result second) {
        model.setKnownSolution(first);
        model.setKnownSolution(second);
        assertSame(second, model.getKnownSolution(), "After multiple overwrites, the last set result must be returned.");
    }
}
