package org.ojalgo.optimisation;

import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.ojalgo.access.Access1D;
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getKnownSolution_Test {

    private ExpressionsBasedModel model;

    @Mock
    private Optimisation.Result mockResult;

    @Mock
    private BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> mockHandler;

    @Mock
    private Optimisation.Result // used for several test cases
    mockResultA;

    @Mock
    private Optimisation.Result mockResultB;

    @Mock
    private Optimisation.Result mockResultC;

    @Mock
    private Optimisation.Result mockResultD;

    @Mock
    private Optimisation.Result mockResultE;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references – not strictly required but keeps the pattern clear.
        model = null;
    }

    @Test
    void testGetKnownSolutionIsNullByDefault() {
        assertNull(model.getKnownSolution(), "A newly created ExpressionsBasedModel should have a null known solution.");
    }

    @Test
    void testSetAndGetKnownSolution() {
        // Store a mocked result as the known solution.
        model.setKnownSolution(mockResult);
        // The getter must return the exact same instance that was set.
        assertSame(mockResult, model.getKnownSolution(), "getKnownSolution() should return the instance supplied via setKnownSolution(Result).");
    }

    @Test
    void testSetAndGetKnownSolutionWithHandler() {
        // Store a mocked result together with a mocked validation‑failure handler.
        model.setKnownSolution(mockResult, mockHandler);
        // The stored result must still be retrievable unchanged.
        assertSame(mockResult, model.getKnownSolution(), "getKnownSolution() should return the instance supplied via setKnownSolution(Result, BiConsumer).");
    }

    /**
     * Passing {@code null} clears the stored solution.
     */
    @Test
    void testSetKnownSolutionNullClears() {
        model.setKnownSolution(mockResultC);
        assertNotNull(model.getKnownSolution(), "Pre‑condition: known solution should be set.");
        model.setKnownSolution(null);
        assertNull(model.getKnownSolution(), "After calling setKnownSolution(null) the known solution must be cleared.");
    }

    /**
     * Setting a solution twice replaces the previous one.
     */
    @Test
    void testSetKnownSolutionTwiceReplacesPrevious() {
        model.setKnownSolution(mockResultA);
        assertSame(mockResultA, model.getKnownSolution(), "First stored solution should be returned.");
        model.setKnownSolution(mockResultB);
        assertSame(mockResultB, model.getKnownSolution(), "Second stored solution should replace the first one.");
    }

    /**
     * Mutating the model after storing a solution does not change the stored reference.
     */
    @Test
    void testKnownSolutionUnchangedAfterModelMutation() {
        model.setKnownSolution(mockResultC);
        assertSame(mockResultC, model.getKnownSolution(), "Stored solution must be present before mutation.");
        // Mutate the model – add a variable and an expression.
        // creates a new Variable
        model.addVariable();
        // creates a new Expression
        model.addExpression();
        assertSame(mockResultC, model.getKnownSolution(), "Stored solution reference must stay unchanged after model mutation.");
    }

    /**
     * A handler that throws an exception must not affect storage of the result.
     */
    @Test
    void testSetKnownSolutionWithHandlerThatThrows() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> throwingHandler = (m, solution) -> {
            throw new RuntimeException("Handler invoked");
        };
        // The handler is *not* invoked by setKnownSolution, so no exception should be thrown here.
        assertDoesNotThrow(() -> model.setKnownSolution(mockResultD, throwingHandler), "setKnownSolution should not execute the handler at call time.");
        assertSame(mockResultD, model.getKnownSolution(), "Even when the handler would throw, the result must still be stored.");
    }

    /**
     * Set a solution and then clear it with a subsequent null call.
     */
    @Test
    void testSetKnownSolutionThenClearWithNull() {
        model.setKnownSolution(mockResultE);
        assertNotNull(model.getKnownSolution(), "Pre‑condition: known solution should be stored.");
        model.setKnownSolution(null);
        assertNull(model.getKnownSolution(), "Calling setKnownSolution(null) after a previous set must result in a null known solution.");
    }
}
