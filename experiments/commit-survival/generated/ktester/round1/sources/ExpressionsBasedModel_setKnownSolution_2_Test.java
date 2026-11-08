package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Access1D;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setKnownSolution_2_Test {

    /**
     * A simple model used by several tests.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // a single variable with a clear feasible region
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// contributes to the objective
        BigMath.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Successful registration of a known optimal and valid solution.
     */
    @Test
    void testSetKnownSolutionSuccess() {
        // obtain an optimal solution from the model itself
        Optimisation.Result optimal = model.minimise();
        assertTrue(optimal.getState().isOptimal(), "Result should be optimal");
        // a no‑op handler
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        model.setKnownSolution(optimal, handler);
        // the model must now expose the same result and handler
        assertSame(optimal, model.getKnownSolution(), "Known solution should be stored");
        assertSame(handler, model.getValidationFailureHandler(), "Handler should be stored");
    }

    /**
     * Passing a {@code null} known solution must raise {@link NullPointerException}.
     */
    @Test
    void testSetKnownSolutionNullResult() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null, handler));
    }

    /**
     * Passing a {@code null} handler must raise {@link NullPointerException}.
     */
    @Test
    void testSetKnownSolutionNullHandler() {
        Optimisation.Result optimal = model.minimise();
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(optimal, null));
    }

    /**
     * A non‑optimal solution must cause a {@link ProgrammingError}.
     */
    @Test
    void testSetKnownSolutionNonOptimal() {
        // mock a result whose state reports not optimal
        Optimisation.State nonOptimalState = mock(Optimisation.State.class);
        when(nonOptimalState.isOptimal()).thenReturn(false);
        @Mock
        Access1D<BigDecimal> dummySolution;
        Optimisation.Result nonOptimal = new Optimisation.Result(nonOptimalState, dummySolution);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.setKnownSolution(nonOptimal, handler));
        assertEquals("Must be an optimal solution!", ex.getMessage(), "Exception message must match the contract");
    }

    /**
     * A solution that fails validation must cause a {@link ProgrammingError}.
     */
    @Test
    void testSetKnownSolutionInvalidSolution() {
        // create a mock result that is optimal
        Optimisation.State optimalState = mock(Optimisation.State.class);
        when(optimalState.isOptimal()).thenReturn(true);
        @Mock
        Access1D<BigDecimal> solution;
        Optimisation.Result optimalResult = new Optimisation.Result(optimalState, solution);
        // spy the model and force validate(result) to return false
        ExpressionsBasedModel spyModel = spy(model);
        doReturn(false).when(spyModel).validate(optimalResult);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> spyModel.setKnownSolution(optimalResult, handler));
        assertEquals("Solution not valid!", ex.getMessage(), "Exception message must indicate validation failure");
    }

    /* ------------------------------------------------------------
     *  Null argument validation (parameterised)
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] knownSolution={0}, handler={1}")
    @CsvSource({ // knownSolution null
    "null,   notnull", // handler null
    "notnull, null" })
    void testNullArgumentValidation(String knownSolutionFlag, String handlerFlag) {
        Optimisation.Result result = null;
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = null;
        if (!"null".equals(knownSolutionFlag)) {
            // any optimal result
            result = model.minimise();
        }
        if (!"null".equals(handlerFlag)) {
            handler = (m, s) -> {
            };
        }
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(result, handler), "Both arguments must be non‑null");
    }

    /* ------------------------------------------------------------
     *  Null state handling – getState() returns null → NPE
     * ------------------------------------------------------------ */
    @Test
    void testSetKnownSolutionNullState() {
        // mock a result whose getState() returns null
        Optimisation.Result resultWithNullState = mock(Optimisation.Result.class);
        when(resultWithNullState.getState()).thenReturn(null);
        @Mock
        Access1D<BigDecimal> dummySolution;
        // not used but safe
        when(resultWithNullState.getSolution()).thenReturn(dummySolution);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(resultWithNullState, handler), "Dereferencing a null state must throw NPE");
    }

    /* ------------------------------------------------------------
     *  Handler replacement (idempotent registration)
     * ------------------------------------------------------------ */
    @Test
    void testHandlerReplacement() {
        // first registration
        Optimisation.Result first = model.minimise();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handlerA = (m, s) -> {
        };
        model.setKnownSolution(first, handlerA);
        assertSame(first, model.getKnownSolution());
        assertSame(handlerA, model.getValidationFailureHandler());
        // second registration with different objects
        // still optimal
        Optimisation.Result second = model.minimise();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handlerB = (m, s) -> {
        };
        model.setKnownSolution(second, handlerB);
        assertSame(second, model.getKnownSolution(), "Known solution should be replaced");
        assertSame(handlerB, model.getValidationFailureHandler(), "Handler should be replaced");
    }

    /* ------------------------------------------------------------
     *  Retrieval test – confirms getters after registration
     * ------------------------------------------------------------ */
    @Test
    void testRetrievingStoredSolutionAndHandler() {
        Optimisation.Result result = model.minimise();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        model.setKnownSolution(result, handler);
        assertSame(result, model.getKnownSolution(), "getKnownSolution must return the exact instance supplied");
        assertSame(handler, model.getValidationFailureHandler(), "getValidationFailureHandler must return the exact handler supplied");
    }

    /* ------------------------------------------------------------
     *  Propagation of unchecked exception from validate(...)
     * ------------------------------------------------------------ */
    @Test
    void testValidateExceptionPropagation() {
        // optimal result
        Optimisation.State optimalState = mock(Optimisation.State.class);
        when(optimalState.isOptimal()).thenReturn(true);
        @Mock
        Access1D<BigDecimal> dummySolution;
        Optimisation.Result optimal = new Optimisation.Result(optimalState, dummySolution);
        // spy and make validate throw an unchecked exception
        ExpressionsBasedModel spyModel = spy(model);
        doThrow(new IllegalStateException("validation failed")).when(spyModel).validate(optimal);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spyModel.setKnownSolution(optimal, handler));
        assertEquals("validation failed", ex.getMessage(), "Exception from validate must propagate unchanged");
    }

    /* ------------------------------------------------------------
     *  Propagation of unchecked exception from state.isOptimal()
     * ------------------------------------------------------------ */
    @Test
    void testStateIsOptimalExceptionPropagation() {
        // mock a state whose isOptimal() throws
        Optimisation.State flakyState = mock(Optimisation.State.class);
        when(flakyState.isOptimal()).thenThrow(new IllegalStateException("state broken"));
        @Mock
        Access1D<BigDecimal> dummySolution;
        Optimisation.Result result = new Optimisation.Result(flakyState, dummySolution);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = (m, s) -> {
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.setKnownSolution(result, handler));
        assertEquals("state broken", ex.getMessage(), "Exception from State.isOptimal must propagate");
    }
}
