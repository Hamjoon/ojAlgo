package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setKnownSolution_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @Test
    void testNullKnownSolutionThrowsNPE() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        // knownSolution is null → NPE from Objects.requireNonNull
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null, handler));
    }

    @Test
    void testNullHandlerThrowsNPE() {
        // Build a minimal optimal result using a trivial model.
        ExpressionsBasedModel trivial = new ExpressionsBasedModel();
        trivial.newVariable("x").weight(BigMath.ONE);
        // should be optimal
        Optimisation.Result optimal = trivial.minimise();
        // handler is null → NPE from Objects.requireNonNull
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(optimal, null));
    }

    @Test
    void testNonOptimalResultThrowsProgrammingError() {
        // Mock a Result whose state is not optimal.
        Optimisation.Result nonOptimal = Mockito.mock(Optimisation.Result.class);
        Mockito.when(nonOptimal.getState()).thenReturn(Optimisation.State.FAILED);
        // The handler can be any non‑null consumer.
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        // Expect a ProgrammingError because the result is not optimal.
        assertThrows(ProgrammingError.class, () -> model.setKnownSolution(nonOptimal, handler));
    }

    @Test
    void testInvalidSolutionThrowsProgrammingError() {
        // Spy the model so we can stub the validation result.
        ExpressionsBasedModel spyModel = Mockito.spy(new ExpressionsBasedModel());
        // Create a genuine optimal result (validation will be stubbed anyway).
        ExpressionsBasedModel trivial = new ExpressionsBasedModel();
        trivial.newVariable("x").weight(BigMath.ONE);
        Optimisation.Result optimal = trivial.minimise();
        // Force validation to fail.
        Mockito.doReturn(false).when(spyModel).validate(optimal);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        // The method must throw a ProgrammingError and must not modify internal fields.
        assertThrows(ProgrammingError.class, () -> spyModel.setKnownSolution(optimal, handler));
        assertNull(spyModel.getKnownSolution(), "Known solution must remain null after failure");
        assertEquals(Validator.NULL, spyModel.getValidationFailureHandler(), "Handler must remain the default after failure");
    }

    @Test
    void testSuccessfulSetKnownSolution() {
        // Build a simple model that yields an optimal solution.
        model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.TEN).weight(BigMath.ONE);
        // optimal by construction
        Optimisation.Result optimal = model.minimise();
        // A mutable flag to verify that the supplied handler can be invoked later.
        final boolean[] handlerInvoked = { false };
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> handlerInvoked[0] = true;
        // Invoke the method under test.
        model.setKnownSolution(optimal, handler);
        // Verify that the internal state was updated correctly.
        assertSame(optimal, model.getKnownSolution(), "The known solution should be stored unchanged");
        assertSame(handler, model.getValidationFailureHandler(), "The provided handler should be stored unchanged");
        // The handler is not automatically called by setKnownSolution; invoke it manually to ensure it works.
        handler.accept(model, optimal);
        assertTrue(handlerInvoked[0], "Handler should have been invoked");
    }
}
