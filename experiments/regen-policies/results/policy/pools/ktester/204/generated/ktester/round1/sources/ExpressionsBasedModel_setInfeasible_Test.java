package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setInfeasible_Test {

    private ExpressionsBasedModel model;

    /**
     * Functional interface used to abstract the different copy methods.
     */
    @FunctionalInterface
    private interface CopyFunction {

        ExpressionsBasedModel copy(ExpressionsBasedModel source);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test case.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the model does not hold external resources.
        model = null;
    }

    /**
     * Provides the different copy operations that should preserve the infeasibility flag.
     * The first argument is a description for readability in the test report.
     */
    private static Stream<Arguments> copyOperationsProvider() {
        return Stream.of(Arguments.of("shallow copy via copy()", (CopyFunction) ExpressionsBasedModel::copy), Arguments.of("relaxed copy via copy(true)", (CopyFunction) m -> m.copy(true)));
    }

    @Test
    void testSetInfeasibleChangesState() {
        // Initially the model must not be marked as infeasible.
        assertFalse(model.isInfeasible(), "Model should start feasible");
        // Invoke the package‑private method (accessible because the test resides in the same package).
        model.setInfeasible();
        // After invocation the flag must be true.
        assertTrue(model.isInfeasible(), "Model should be marked infeasible after setInfeasible()");
    }

    @Test
    void testSetInfeasibleIdempotent() {
        // Call the method twice and ensure the state remains infeasible.
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "First call should set infeasible");
        // second call – should have no adverse effect
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "Second call should leave model infeasible");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("copyOperationsProvider")
    void testCopyRetainsInfeasibilityFlag(String description, CopyFunction copier) {
        // Arrange: mark the original model as infeasible.
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "Pre‑condition: original model must be infeasible");
        // Act: create a copy using the supplied copy function.
        ExpressionsBasedModel copy = copier.copy(model);
        // Assert: the copy must report infeasible as well.
        assertNotNull(copy, "Copy should not be null");
        assertTrue(copy.isInfeasible(), () -> "Copy created via " + description + " must retain infeasibility flag");
    }
}
