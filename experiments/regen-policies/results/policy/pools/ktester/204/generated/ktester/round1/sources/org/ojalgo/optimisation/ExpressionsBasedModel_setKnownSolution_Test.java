package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.ojalgo.array.Array1D;
import org.ojalgo.structure.Access1D;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setKnownSolution_Test {

    private ExpressionsBasedModel model;

    private Optimisation.Result knownResult;

    @BeforeEach
    void setupBeforeEach() {
        // Build a tiny optimisation problem: minimise x subject to 0 ≤ x ≤ 5
        model = new ExpressionsBasedModel();
        ExpressionsBasedModel.Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(// minimise x
        BigDecimal.ONE);
        // No additional constraints – the variable bounds are enough.
        // Solve the model to obtain a feasible (and optimal) result that we will treat as the "known" solution.
        knownResult = model.minimise();
        // Sanity‑check that the solver actually succeeded; otherwise the following tests would be meaningless.
        assertTrue(knownResult.getState().isOptimal(), "Pre‑condition: the model must produce an optimal result before it can be used as a known solution.");
    }

    @Test
    void testSetKnownSolutionStoresResult() {
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Pre‑condition: the model must produce an optimal result before it can be used as a known solution.");
        model.setKnownSolution(result);
        Optimisation.Result retrieved = model.getKnownSolution();
        assertNotNull(retrieved, "getKnownSolution() must not return null after setting a known solution.");
        assertSame(result, retrieved, "The stored known solution should be the same instance that was set.");
    }

    @Test
    void testSetKnownSolutionIsValidatedByModel() {
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Pre‑condition: the model must produce an optimal result before it can be used as a known solution.");
        model.setKnownSolution(result);
        assertTrue(model.validate(result), "The model should validate the known solution that was just set.");
    }

    @Test
    void testSetKnownSolutionWithCustomHandlerDoesNotThrow() {
        // Build a simple handler that records whether it was invoked.
        final boolean[] handlerInvoked = { false };
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> handlerInvoked[0] = true;
        // Act: use the overload that accepts a handler.
        model.setKnownSolution(knownResult, handler);
        // The handler should only be called when validation fails; in this happy path it must stay false.
        assertFalse(handlerInvoked[0], "Custom handler should not be invoked for a valid known solution.");
    }

    @Test
    void testSetKnownSolutionWithArtificialInvalidSolution() {
        // Create an intentionally invalid solution (e.g., a vector of the wrong size).
        Access1D<BigDecimal> badSolution = Array1D.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        Optimisation.Result badResult = new Optimisation.Result(Optimisation.State.FEASIBLE, badSolution);
        // Use a handler that will be triggered on validation failure.
        final boolean[] handlerInvoked = { false };
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> handlerInvoked[0] = true;
        // Act: set the bad known solution with the custom handler.
        model.setKnownSolution(badResult, handler);
        // Validation should now fail, causing the handler to be called.
        assertFalse(model.validate(badResult), "The deliberately bad solution must not validate.");
        assertTrue(handlerInvoked[0], "Custom handler must be invoked when validation of the known solution fails.");
    }

    @Test
    void testSetKnownSolutionDelegatesToTwoArgOverload() {
        // a simple optimal result (x = 0)
        Optimisation.Result result = new Optimisation.Result(Optimisation.State.OPTIMAL, Array1D.of(BigDecimal.ZERO));
        // act
        model.setKnownSolution(result);
        // assert – the same instance must be stored …
        assertSame(result, model.getKnownSolution(), "getKnownSolution() must return the exact instance that was set.");
        // … and the default no‑op handler must be used.
        assertSame(ExpressionsBasedModel.Validator.NULL, model.getValidationFailureHandler(), "The validation‑failure handler should be the default Validator.NULL after delegating.");
    }

    @Test
    void testSetKnownSolutionReplacesPreviousResult() {
        Optimisation.Result first = new Optimisation.Result(Optimisation.State.OPTIMAL, Array1D.of(BigDecimal.ONE));
        Optimisation.Result second = new Optimisation.Result(Optimisation.State.OPTIMAL, Array1D.of(BigDecimal.valueOf(2)));
        model.setKnownSolution(first);
        assertSame(first, model.getKnownSolution(), "First call must store the first result.");
        model.setKnownSolution(second);
        assertSame(second, model.getKnownSolution(), "Second call must replace the previously stored known solution.");
    }

    @Test
    void testSetKnownSolutionWithCustomHandlerStoresBothResultAndHandler() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler = (m, s) -> {
            // no‑op for the test – we only care about the reference being stored
        };
        Optimisation.Result result = new Optimisation.Result(Optimisation.State.OPTIMAL, Array1D.of(BigDecimal.TEN));
        model.setKnownSolution(result, customHandler);
        assertSame(result, model.getKnownSolution(), "The supplied result must be stored unchanged.");
        assertSame(customHandler, model.getValidationFailureHandler(), "The supplied handler must be stored as the validation‑failure handler.");
    }

    @Test
    void testSetKnownSolutionHandlerReplacement() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handlerA = (m, s) -> {
        };
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handlerB = (m, s) -> {
        };
        Optimisation.Result result = new Optimisation.Result(Optimisation.State.OPTIMAL, Array1D.of(BigDecimal.ONE));
        // first registration
        model.setKnownSolution(result, handlerA);
        assertSame(handlerA, model.getValidationFailureHandler(), "Handler A must be stored after the first call.");
        // second registration with a different handler
        model.setKnownSolution(result, handlerB);
        assertSame(handlerB, model.getValidationFailureHandler(), "Handler B must replace Handler A after the second call.");
    }

    @Test
    void testSetKnownSolutionNullResultThrows() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = ExpressionsBasedModel.Validator.NULL;
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null, handler), "Passing a null knownSolution must result in a NullPointerException.");
    }

    @Test
    void testSetKnownSolutionNullHandlerThrows() {
        Optimisation.Result result = new Optimisation.Result(Optimisation.State.OPTIMAL, Array1D.of(BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(result, null), "Passing a null handler must result in a NullPointerException.");
    }
}