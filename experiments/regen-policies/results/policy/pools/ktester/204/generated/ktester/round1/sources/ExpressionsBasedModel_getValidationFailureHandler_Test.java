package org.ojalgo.optimisation;

import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.structure.Access1D;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getValidationFailureHandler_Test {

    /**
     * By default a newly created {@code ExpressionsBasedModel} should return the
     * {@code Validator.NULL} handler.
     */
    @Test
    void testDefaultHandlerIsValidatorNull() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        // The default handler is a specific static instance; we can safely use identity comparison.
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "The default validation failure handler must be Validator.NULL");
        // Invoking the handler must not throw and must not produce side‑effects.
        assertDoesNotThrow(() -> handler.accept(model, null), "Invoking the default handler should not throw any exception");
    }

    /**
     * When a custom handler is supplied via {@code setKnownSolution(..., handler)},
     * {@code getValidationFailureHandler()} must return that same instance.
     */
    @Test
    void testCustomHandlerIsReturned(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The API allows a known solution to be {@code null}; we only care about the handler.
        model.setKnownSolution(null, customHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(customHandler, retrieved, "The handler set via setKnownSolution should be returned by getValidationFailureHandler");
    }

    /**
     * A copy of a model (shallow or deep) must preserve the validation failure handler.
     */
    @Test
    void testHandlerIsPreservedInCopy(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler) {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.setKnownSolution(null, customHandler);
        // Create both a shallow and a deep copy and verify the handler is the same.
        ExpressionsBasedModel shallowCopy = new ExpressionsBasedModel(original, true, false);
        ExpressionsBasedModel deepCopy = new ExpressionsBasedModel(original, false, false);
        assertSame(customHandler, shallowCopy.getValidationFailureHandler(), "Shallow copy must retain the custom validation failure handler");
        assertSame(customHandler, deepCopy.getValidationFailureHandler(), "Deep copy must retain the custom validation failure handler");
    }

    /**
     * When a custom handler is supplied via {@code setKnownSolution(..., handler)},
     * {@code getValidationFailureHandler()} must return that same instance.
     */
    @Test
    void testCustomHandlerReturnedViaSetKnownSolution(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The API allows a known solution to be {@code null}; we only care about the handler.
        model.setKnownSolution(null, customHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(customHandler, retrieved, "The handler set via setKnownSolution should be returned by getValidationFailureHandler");
    }

    /**
     * Set a custom handler directly via reflection and verify that the getter returns it.
     * The handler toggles an {@link AtomicBoolean} – invoking it must change the flag.
     */
    @Test
    void testCustomHandlerSetViaReflection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        AtomicBoolean called = new AtomicBoolean(false);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler = (m, s) -> called.set(true);
        // Inject the custom handler into the private field.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        field.setAccessible(true);
        field.set(model, customHandler);
        // Verify getter returns the exact instance.
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(customHandler, retrieved, "Getter must return the handler set via reflection");
        // Invoke and assert side‑effect.
        retrieved.accept(model, null);
        assertTrue(called.get(), "Custom handler should have been invoked and set the flag");
    }

    /**
     * After a custom handler has been set via reflection, resetting the field to {@code null}
     * must make the getter fall back to {@link ExpressionsBasedModel.Validator#NULL}.
     */
    @Test
    void testExplicitNullAfterCustomHandler() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Set a custom handler first.
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler = (m, s) -> {
        };
        Field field = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        field.setAccessible(true);
        field.set(model, customHandler);
        assertSame(customHandler, model.getValidationFailureHandler(), "Custom handler should be visible before resetting to null");
        // Reset to null.
        field.set(model, null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> afterReset = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, afterReset, "After resetting the field to null the getter must return Validator.NULL");
        assertDoesNotThrow(() -> afterReset.accept(model, null), "Invoking the fallback handler must not throw");
    }

    /**
     * When {@code setKnownSolution(..., null)} is used the getter must return {@code Validator.NULL}.
     */
    @Test
    void testHandlerSetViaSetKnownSolutionWithNullHandler() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Pass a null handler – the model should internally store null and the getter fallback to NULL.
        model.setKnownSolution(null, null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, retrieved, "Getter must return Validator.NULL when a null handler is supplied via setKnownSolution");
    }

    /**
     * A handler that throws an exception must be returned unchanged and invoking it must propagate the exception.
     */
    @Test
    void testHandlerThatThrowsException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        RuntimeException expected = new RuntimeException("boom");
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> throwingHandler = (m, s) -> {
            throw expected;
        };
        // Inject via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        field.setAccessible(true);
        field.set(model, throwingHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(throwingHandler, retrieved, "Getter must return the throwing handler");
        RuntimeException actual = assertThrows(RuntimeException.class, () -> retrieved.accept(model, null), "Invoking the throwing handler must propagate the RuntimeException");
        assertSame(expected, actual, "The propagated exception must be the same instance");
    }

    /**
     * Verify that the handler returned by the getter receives exactly the arguments supplied to it.
     */
    @Test
    void testHandlerCapturesArgumentsCorrectly(@Mock Access1D<BigDecimal> mockSolution) throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        AtomicReference<ExpressionsBasedModel> capturedModel = new AtomicReference<>();
        AtomicReference<Access1D<BigDecimal>> capturedSolution = new AtomicReference<>();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> capturingHandler = (m, s) -> {
            capturedModel.set(m);
            capturedSolution.set(s);
        };
        // Inject via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        field.setAccessible(true);
        field.set(model, capturingHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        // Use a fresh model instance to ensure the handler receives the exact instance we pass.
        ExpressionsBasedModel anotherModel = new ExpressionsBasedModel();
        retrieved.accept(anotherModel, mockSolution);
        assertSame(anotherModel, capturedModel.get(), "Handler must receive the exact model instance passed to it");
        assertSame(mockSolution, capturedSolution.get(), "Handler must receive the exact solution instance passed to it");
    }

    /**
     * Multiple calls to the getter must return the same instance (idempotent).
     */
    @Test
    void testIdempotentGetter(@Mock BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler) throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Set a custom handler via public API.
        model.setKnownSolution(null, customHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> first = model.getValidationFailureHandler();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> second = model.getValidationFailureHandler();
        assertSame(first, second, "Multiple invocations of getValidationFailureHandler must return the same instance");
    }
}
