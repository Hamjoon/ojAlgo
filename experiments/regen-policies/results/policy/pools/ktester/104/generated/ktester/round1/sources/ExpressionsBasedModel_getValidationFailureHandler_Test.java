package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getValidationFailureHandler_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now
        model = null;
    }

    /* --------------------------------------------------------------------- *
     *  Helper to force the private field {@code myValidationFailureHandler}
     *  to a given value via reflection.
     * --------------------------------------------------------------------- */
    private void setInternalHandler(BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler) throws ReflectiveOperationException {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        field.setAccessible(true);
        field.set(model, handler);
    }

    /**
     * By default the model uses {@link ExpressionsBasedModel.Validator#NULL} as the validation‑failure handler.
     */
    @Test
    void testDefaultHandlerIsValidatorNull() {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        // The default handler should be the constant NULL defined in the nested Validator class
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "The default validation failure handler should be Validator.NULL");
    }

    /**
     * When a custom handler is supplied via {@link ExpressionsBasedModel#setKnownSolution(Optimisation.Result, BiConsumer)},
     * the same handler must be returned by {@code getValidationFailureHandler()}.
     */
    @Test
    void testCustomHandlerReturnedAfterSettingKnownSolution(@Mock Optimisation.Result mockResult) {
        // Create a custom (no‑op) handler
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler = (m, s) -> {
            // intentionally empty – just a distinct instance
        };
        // Register the custom handler with the model
        model.setKnownSolution(mockResult, customHandler);
        // Retrieve the handler and verify it is the one we set
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(customHandler, retrieved, "The custom validation failure handler should be returned after being set");
    }

    /**
     * Calling {@code setKnownSolution(Result)} (no handler argument) must leave the
     * internal handler unchanged – it stays the default {@code Validator.NULL}.
     */
    @Test
    void testHandlerRemainsDefaultWhenSettingKnownSolutionWithoutHandler(@Mock Optimisation.Result mockResult) {
        // invoke the overload that does NOT accept a handler
        model.setKnownSolution(mockResult);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "When setKnownSolution(Result) is used, the handler should stay as Validator.NULL");
    }

    /**
     * If the private field {@code myValidationFailureHandler} is manually set to {@code null}
     * (simulating an unexpected state), the getter must fall back to {@code Validator.NULL}.
     */
    @Test
    void testNullHandlerFallbackWhenFieldManuallySetToNull() throws Exception {
        // Force the internal field to null via reflection
        setInternalHandler(null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "When internal handler is null, getter should fall back to Validator.NULL");
    }

    /**
     * Setting a handler twice should replace the previous one – the latest handler wins.
     */
    @Test
    void testOverrideHandlerWithNewCustomHandler(@Mock Optimisation.Result mockResult) {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler1 = (m, s) -> {
        };
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler2 = (m, s) -> {
        };
        model.setKnownSolution(mockResult, handler1);
        // overwrite
        model.setKnownSolution(mockResult, handler2);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(handler2, retrieved, "The most recently set custom handler should be returned after multiple setKnownSolution calls");
    }

    /**
     * Explicitly passing {@code null} as the handler argument to {@code setKnownSolution}
     * stores a {@code null} value internally, causing the getter to return {@code Validator.NULL}.
     */
    @Test
    void testSetHandlerToNullViaSetKnownSolution(@Mock Optimisation.Result mockResult) {
        // Pass null as the handler
        model.setKnownSolution(mockResult, null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "Explicit null handler should result in getter returning Validator.NULL");
    }

    /**
     * Sequence: set handler1, then set null, then set handler3.
     * The getter must return the last non‑null handler (handler3).
     */
    @Test
    void testHandlerAfterMultipleCallsIncludingNull(@Mock Optimisation.Result mockResult) {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler1 = (m, s) -> {
        };
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler3 = (m, s) -> {
        };
        model.setKnownSolution(mockResult, handler1);
        // intermediate null
        model.setKnownSolution(mockResult, null);
        // final non‑null
        model.setKnownSolution(mockResult, handler3);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(handler3, retrieved, "After a null in the middle of calls, the getter should return the latest non‑null handler");
    }

    /**
     * Manually set the internal field to {@code null} via reflection, then call
     * {@code setKnownSolution} with a custom handler. The getter must now return
     * the newly supplied handler (not the default).
     */
    @Test
    void testHandlerAfterReflectionNullThenSetKnownSolution(@Mock Optimisation.Result mockResult) throws Exception {
        // Ensure the field is null first
        setInternalHandler(null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> newHandler = (m, s) -> {
        };
        model.setKnownSolution(mockResult, newHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(newHandler, retrieved, "After reflection‑based null and a subsequent setKnownSolution, the getter should return the newly set handler");
    }
}
