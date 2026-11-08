package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ojalgo.array.Array1D;
import org.ojalgo.structure.Access1D;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getValidationFailureHandler_Test {

    private ExpressionsBasedModel model;

    /* --------------------------------------------------------------
     *  7. Subclass implementation – type and identity check
     * -------------------------------------------------------------- */
    private static final class MyHandler implements BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> {

        final AtomicBoolean called = new AtomicBoolean(false);

        @Override
        public void accept(ExpressionsBasedModel m, Access1D<BigDecimal> s) {
            called.set(true);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the model is disposed after each test
        model.dispose();
    }

    /**
     * When no validation‑failure handler has been set, the method must return the
     * default {@code Validator.NULL} instance.
     */
    @Test
    void testDefaultHandlerIsNull() {
        // The model is freshly created – no handler has been configured.
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        // The default handler is a no‑op lambda defined as Validator.NULL.
        // It is safe to compare by reference because the field is final.
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "Expected the default validation failure handler to be Validator.NULL");
    }

    /**
     * When a custom handler is supplied via {@code setKnownSolution(..., handler)},
     * {@code getValidationFailureHandler()} must return that exact handler.
     */
    @Test
    void testCustomHandlerIsReturned() {
        // Create a dummy result – the content is irrelevant for this test.
        Optimisation.Result dummyResult = Optimisation.Result.of(null, null, Optimisation.State.OPTIMAL);
        // Define a custom handler that records that it has been invoked.
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> customHandler = (m, s) -> {
            // No operation needed; the lambda exists solely for identity testing.
        };
        // Register the custom handler with the model.
        model.setKnownSolution(dummyResult, customHandler);
        // Retrieve the handler via the method under test.
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        // The retrieved handler must be the same instance that we supplied.
        assertSame(customHandler, retrieved, "The custom validation failure handler should be returned unchanged");
    }

    /* --------------------------------------------------------------
     *  2. Custom handler via the public API (setKnownSolution)
     * -------------------------------------------------------------- */
    @Test
    void testCustomHandlerReturnedViaApi() {
        // a dummy result – the concrete content is irrelevant for this test
        Optimisation.Result dummyResult = org.mockito.Mockito.mock(Optimisation.Result.class);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> custom = (m, s) -> {
            // no‑op, we only care about identity
        };
        model.setKnownSolution(dummyResult, custom);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(custom, retrieved, "The handler set via setKnownSolution should be returned unchanged");
    }

    /* --------------------------------------------------------------
     *  3. Custom handler injected via reflection – verify return & invoke
     * -------------------------------------------------------------- */
    @Test
    void testCustomHandlerInjectedByReflection() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> custom = (m, s) -> invoked.set(true);
        // inject into the private field
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        f.set(model, custom);
        // getter must return the exact instance
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(custom, retrieved, "Getter should return the injected handler");
        // invoke and verify side‑effect
        Access1D<BigDecimal> dummySolution = Array1D.of(BigDecimal.ZERO);
        retrieved.accept(model, dummySolution);
        assertTrue(invoked.get(), "Custom handler must have been executed");
    }

    /* --------------------------------------------------------------
     *  4. Null field – fallback to Validator.NULL
     * -------------------------------------------------------------- */
    @Test
    void testNullFieldFallsBackToValidatorNull() throws Exception {
        // force the private field to null
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        f.set(model, null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "When the internal field is null the getter must return Validator.NULL");
        // invoking the no‑op handler must not throw and must not change state
        Access1D<BigDecimal> dummy = Array1D.of(BigDecimal.ONE);
        // should complete silently
        handler.accept(model, dummy);
    }

    /* --------------------------------------------------------------
     *  5. Consistency – repeated calls return the same instance
     * -------------------------------------------------------------- */
    @Test
    void testRepeatedCallsReturnSameInstance() throws Exception {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> custom = (m, s) -> {
            // no‑op
        };
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        f.set(model, custom);
        for (int i = 0; i < 3; i++) {
            BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
            assertSame(custom, retrieved, "All calls must return the same injected instance");
        }
    }

    /* --------------------------------------------------------------
     *  6. Handler that throws an exception – ensure propagation
     * -------------------------------------------------------------- */
    @Test
    void testExceptionPropagatingHandler() throws Exception {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> throwing = (m, s) -> {
            throw new IllegalStateException("handler failure");
        };
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        f.set(model, throwing);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        Access1D<BigDecimal> dummy = Array1D.of(BigDecimal.ONE);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> retrieved.accept(model, dummy), "The exception from the handler must propagate unchanged");
        assertEquals("handler failure", ex.getMessage());
    }

    @Test
    void testSubclassHandlerReturned() throws Exception {
        MyHandler myHandler = new MyHandler();
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        f.set(model, myHandler);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        assertSame(myHandler, retrieved, "Returned handler must be the exact subclass instance");
        assertTrue(retrieved instanceof MyHandler, "Returned handler should be instance of MyHandler");
        // invoke to prove it works
        retrieved.accept(model, Array1D.of(BigDecimal.ZERO));
        assertTrue(myHandler.called.get(), "Subclass handler must have been executed");
    }

    /* --------------------------------------------------------------
     *  8. Side‑effect handler – verify getter itself is pure
     * -------------------------------------------------------------- */
    @Test
    void testHandlerSideEffectWithoutGetterSideEffect() throws Exception {
        List<String> log = new ArrayList<>();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> sideEffectHandler = (m, s) -> log.add("invoked");
        // inject handler
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        f.set(model, sideEffectHandler);
        // calling the getter must not modify the log
        model.getValidationFailureHandler();
        assertTrue(log.isEmpty(), "Getter should not produce side effects");
        // invoke twice
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> retrieved = model.getValidationFailureHandler();
        retrieved.accept(model, Array1D.of(BigDecimal.ZERO));
        retrieved.accept(model, Array1D.of(BigDecimal.ZERO));
        assertEquals(2, log.size(), "Handler must have been invoked exactly twice");
        assertEquals("invoked", log.get(0));
        assertEquals("invoked", log.get(1));
    }

    /* --------------------------------------------------------------
     *  9. Reset field to null after a custom handler – fallback again
     * -------------------------------------------------------------- */
    @Test
    void testResetToNullAfterCustomHandler() throws Exception {
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> custom = (m, s) -> {
            // no‑op
        };
        Field f = ExpressionsBasedModel.class.getDeclaredField("myValidationFailureHandler");
        f.setAccessible(true);
        // set custom
        f.set(model, custom);
        // now reset to null
        f.set(model, null);
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler = model.getValidationFailureHandler();
        assertSame(ExpressionsBasedModel.Validator.NULL, handler, "After resetting the field to null the getter must fall back to Validator.NULL");
    }
}
