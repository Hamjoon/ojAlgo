package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Access1D;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setKnownSolution_Test {

    private ExpressionsBasedModel model;

    private Optimisation.Result optimalResult;

    @BeforeEach
    void setupBeforeEach() {
        // Build a tiny feasible model: minimise  x  subject to 0 ≤ x ≤ 10 and x = 5
        model = new ExpressionsBasedModel();
        Variable x = model.addVariable().lower(0).upper(10).weight(// objective weight = 1 (minimise)
        1);
        // Constraint: x = 5  (lower = upper = 5)
        model.addExpression().set(x, BigDecimal.ONE).level(BigDecimal.valueOf(5));
        // Solve once – the result will be the optimal solution we later store as “known”.
        optimalResult = model.minimise();
        // Sanity check that the model solved correctly; this also guarantees that the
        // result is non‑null and contains a feasible solution.
        assertTrue(optimalResult.getState().isOptimal(), "Pre‑condition: the model must solve to an optimal state before we can store the result.");
        // setKnownSolution behaviour, therefore we keep it minimal.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up; the model is discarded automatically.
        model = null;
        optimalResult = null;
        // help GC
        model = null;
    }

    /* --------------------------------------------------------------------- *
     *  Helper to create Optimisation.Result instances from primitive arrays.
     *  The ojAlgo library provides a static factory method {@code Result.of(double...)}.
     * --------------------------------------------------------------------- */
    private static Optimisation.Result resultOf(double... values) {
        return Optimisation.Result.of(values);
    }

    @Test
    void testSetKnownSolutionStoresResult() {
        // Act: store the previously obtained optimal result
        model.setKnownSolution(optimalResult);
        // Assert: the stored known solution is exactly the object we passed in
        Optimisation.Result retrieved = model.getKnownSolution();
        assertSame(optimalResult, retrieved, "The known solution returned by getKnownSolution() should be the same instance that was set.");
        // The default validation‑failure handler should be the NO‑OP implementation
        assertEquals(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "When using the single‑argument overload the handler must be Validator.NULL.");
    }

    @Test
    void testSetKnownSolutionWithCustomHandler(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> mockHandler) {
        // Act: store the known solution together with a custom handler
        model.setKnownSolution(optimalResult, mockHandler);
        // Assert: the known solution is stored
        assertSame(optimalResult, model.getKnownSolution(), "The known solution should be stored unchanged when a custom handler is supplied.");
        // Assert: the custom handler is stored and returned by the getter
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrievedHandler = model.getValidationFailureHandler();
        assertSame(mockHandler, retrievedHandler, "The custom validation‑failure handler must be returned by getValidationFailureHandler().");
    }

    /* --------------------------------------------------------------------- *
     *  2. Null handler – should be replaced by the default no‑op handler.
     * --------------------------------------------------------------------- */
    @Test
    void testSetKnownSolutionWithNullHandler() {
        Optimisation.Result known = resultOf(0.0, 0.0);
        model.setKnownSolution(known, null);
        assertSame(known, model.getKnownSolution(), "The known solution must be stored even when the supplied handler is null.");
        assertEquals(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "A null handler must be replaced with the default Validator.NULL handler.");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null known solution – expect a NullPointerException.
     * --------------------------------------------------------------------- */
    @Test
    void testSetKnownSolutionNullKnownSolution(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> mockHandler) {
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null, mockHandler), "Passing a null knownSolution should raise NullPointerException.");
    }

    /* --------------------------------------------------------------------- *
     *  4. Overload without handler – should delegate to the two‑argument version
     *     using the default handler.
     * --------------------------------------------------------------------- */
    @Test
    void testSetKnownSolutionSingleArgumentDelegatesToDefaultHandler() {
        Optimisation.Result known = resultOf(5.0);
        // single‑argument overload
        model.setKnownSolution(known);
        assertSame(known, model.getKnownSolution(), "The known solution must be stored by the single‑argument overload.");
        assertEquals(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "The single‑argument overload must use Validator.NULL as the handler.");
    }

    /* --------------------------------------------------------------------- *
     *  5. Replace existing known solution and handler.
     * --------------------------------------------------------------------- */
    @Test
    void testReplaceExistingKnownSolution(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> firstHandler, @Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> secondHandler) {
        Optimisation.Result first = resultOf(1.0, 2.0);
        Optimisation.Result second = resultOf(3.0, 4.0);
        // first registration
        model.setKnownSolution(first, firstHandler);
        assertSame(first, model.getKnownSolution());
        assertSame(firstHandler, model.getValidationFailureHandler());
        // second registration – should overwrite both fields
        model.setKnownSolution(second, secondHandler);
        assertSame(second, model.getKnownSolution(), "After a second call the stored known solution must be the new instance.");
        assertSame(secondHandler, model.getValidationFailureHandler(), "After a second call the stored handler must be the new handler.");
    }

    /* --------------------------------------------------------------------- *
     *  6. Empty result (zero‑length variable array).
     * --------------------------------------------------------------------- */
    @Test
    void testSetKnownSolutionEmptyResult() {
        // creates an empty result
        Optimisation.Result empty = resultOf();
        model.setKnownSolution(empty, null);
        assertSame(empty, model.getKnownSolution(), "The empty Result must be stored unchanged.");
        assertEquals(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "A null handler for an empty result must still be replaced by the default handler.");
    }

    /* --------------------------------------------------------------------- *
     *  7. Handler that throws when invoked – should NOT be executed during setKnownSolution.
     * --------------------------------------------------------------------- */
    @Test
    void testHandlerThatThrowsIsNotInvokedDuringSet() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> throwingHandler = (m, s) -> {
            throw new RuntimeException("Handler should not be invoked during setKnownSolution");
        };
        Optimisation.Result known = resultOf(5.0);
        // No exception should be thrown here – the handler is merely stored.
        assertDoesNotThrow(() -> model.setKnownSolution(known, throwingHandler), "setKnownSolution must not invoke the supplied handler.");
        assertSame(throwingHandler, model.getValidationFailureHandler(), "The throwing handler must be stored for later use.");
    }

    /* --------------------------------------------------------------------- *
     *  8. Result with extreme numeric values.
     * --------------------------------------------------------------------- */
    @Test
    void testSetKnownSolutionWithExtremeValues() {
        Optimisation.Result extreme = resultOf(1e30, -1e30, Double.MAX_VALUE);
        model.setKnownSolution(extreme, null);
        assertSame(extreme, model.getKnownSolution(), "The result containing extreme values must be stored unchanged.");
        assertEquals(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "When a null handler is supplied, the default handler must be used even for extreme values.");
    }

    /* --------------------------------------------------------------------- *
     *  9. Verify that getKnownSolution returns the exact instance supplied.
     * --------------------------------------------------------------------- */
    @Test
    void testGetKnownSolutionReturnsExactInstance() {
        Optimisation.Result known = resultOf(7.0, 8.0);
        model.setKnownSolution(known, (m, s) -> {
            // no‑op handler
        });
        assertSame(known, model.getKnownSolution(), "getKnownSolution should return the exact instance that was set (reference equality).");
    }
}
