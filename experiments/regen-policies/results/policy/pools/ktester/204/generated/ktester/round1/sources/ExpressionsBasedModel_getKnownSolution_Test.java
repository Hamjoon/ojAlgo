package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getKnownSolution_Test {

    private ExpressionsBasedModel model;

    @Mock
    private Optimisation.Result // a dummy optimisation result
    mockResult;

    @Mock
    private BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> // dummy handler
    mockHandler;

    @Mock
    private Optimisation.Result // first dummy optimisation result
    mockResult1;

    @Mock
    private Optimisation.Result // second dummy optimisation result
    mockResult2;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks (required when not using the MockitoExtension on the class level)
        MockitoAnnotations.openMocks(this);
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    /**
     * Supplies the boolean flag indicating which overload to use for the parameterised test.
     */
    private static Stream<Boolean> nullClearingProvider() {
        return Stream.of(Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    void testGetKnownSolutionIsNullByDefault() {
        // The model is newly created – no known solution should be stored.
        assertNull(model.getKnownSolution(), "Expected getKnownSolution() to return null for a fresh model.");
    }

    @Test
    void testSetKnownSolutionSingleArgument() {
        // Store a mock result.
        model.setKnownSolution(mockResult);
        // The same instance should be returned.
        Optimisation.Result retrieved = model.getKnownSolution();
        assertSame(mockResult, retrieved, "getKnownSolution() should return the exact instance passed to setKnownSolution.");
    }

    @Test
    void testSetKnownSolutionWithHandler() {
        // Store a mock result together with a handler.
        model.setKnownSolution(mockResult, mockHandler);
        // Retrieval should still give the result (the handler is only used on validation failures).
        Optimisation.Result retrieved = model.getKnownSolution();
        assertSame(mockResult, retrieved, "getKnownSolution() should return the result supplied to the two‑argument overload.");
    }

    /**
     * Setting {@code null} via the single‑argument overload clears the stored solution.
     */
    @Test
    void testSetKnownSolutionToNullSingleArgument() {
        // First store a non‑null value to ensure the clearing really happens.
        model.setKnownSolution(mockResult1);
        assertNotNull(model.getKnownSolution(), "Precondition failed – result should be stored.");
        // Now clear it.
        model.setKnownSolution(null);
        assertNull(model.getKnownSolution(), "After setKnownSolution(null) the known solution should be null.");
    }

    /**
     * Setting {@code null} via the two‑argument overload also clears the stored solution.
     */
    @Test
    void testSetKnownSolutionToNullWithHandler() {
        // Store a non‑null value first.
        model.setKnownSolution(mockResult1, mockHandler);
        assertNotNull(model.getKnownSolution(), "Precondition failed – result should be stored.");
        // Clear with null + handler.
        model.setKnownSolution(null, mockHandler);
        assertNull(model.getKnownSolution(), "After setKnownSolution(null, handler) the known solution should be null.");
    }

    /**
     * Replacing an existing solution with a different instance should return the new one.
     */
    @Test
    void testReplaceExistingSolution() {
        // Store the first result.
        model.setKnownSolution(mockResult1);
        assertSame(mockResult1, model.getKnownSolution(), "First stored result should be returned.");
        // Replace with a second result.
        model.setKnownSolution(mockResult2);
        Optimisation.Result retrieved = model.getKnownSolution();
        assertSame(mockResult2, retrieved, "After replacement, getKnownSolution() should return the newly set instance.");
        assertNotSame(mockResult1, retrieved, "The new instance must be different from the previous one.");
    }

    /**
     * Parameterised test that covers both overloads for clearing the known solution.
     * Demonstrates use of {@code @MethodSource}.
     */
    @ParameterizedTest(name = "{index} => clearWithNull={0}")
    @MethodSource("nullClearingProvider")
    void testClearingKnownSolutionWithNull(boolean useHandler) {
        // Store an initial solution.
        model.setKnownSolution(mockResult1);
        assertNotNull(model.getKnownSolution(), "Precondition failed – result should be stored.");
        // Clear using the selected overload.
        if (useHandler) {
            model.setKnownSolution(null, mockHandler);
        } else {
            model.setKnownSolution(null);
        }
        assertNull(model.getKnownSolution(), "After clearing with null (handler=" + useHandler + "), getKnownSolution() must return null.");
    }
}
