package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.ojalgo.netio.BasicLogger;
import java.lang.reflect.Field;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.structure.Access1D;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_3_Test {

    private ExpressionsBasedModel model;

    /**
     * A very small logger implementation that does nothing.
     */
    private static final BasicLogger DUMMY_LOGGER = new BasicLogger() {
    };

    /**
     * A logger that throws a RuntimeException on any call.
     */
    private static final BasicLogger EXPLODING_LOGGER = new BasicLogger() {

        @Override
        public void println() {
            throw new RuntimeException("logger failure");
        }

        @Override
        public void println(Object obj) {
            throw new RuntimeException("logger failure");
        }
    };

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /**
     * A simple feasible model:
     *
     * <pre>
     * minimise   x
     * subject to 0 ≤ x ≤ 10
     * </pre>
     *
     * The optimal solution is x = 0, which should be validated successfully.
     */
    @Test
    void testValidateReturnsTrueForFeasibleModel_WithNullLogger() {
        model.newVariable("x").lower(0).upper(10).weight(1);
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Model should solve to an optimal state");
        // Validate using a null logger – the method must handle a null argument gracefully.
        boolean isValid = model.validate((BasicLogger) null);
        assertTrue(isValid, "Validation should succeed for a feasible solution");
    }

    /**
     * The same model as above, but this time a mock {@link BasicLogger}
     * is supplied to ensure that the logger can be passed without causing
     * any side‑effects.
     */
//     @Test
//     void testValidateReturnsTrueForFeasibleModel_WithMockLogger(@Mock BasicLogger mockLogger) {
//         // Define the variable exactly as in the previous test.
//         model.newVariable("x").lower(0).upper(10).weight(1);
//         Optimisation.Result result = model.minimise();
//         assertTrue(result.getState().isOptimal(), "Model should solve to an optimal state");
//         // Validate using the mocked logger.
//         boolean isValid = model.validate(mockLogger);
//         assertTrue(isValid, "Validation should succeed when a logger is provided");
//     }

    /* -------------------------------------------------------------
     *  2. Infeasible solution
     * ------------------------------------------------------------- */
//     @Test
//     void testValidateReturnsFalseForInfeasibleModel(@Mock BasicLogger mockLogger) {
//         // contradictory bounds: lower > upper
//         model.newVariable("x").lower(1).upper(0).weight(1);
//         // Validation must return false when the model is infeasible
//         assertFalse(model.validate(DUMMY_LOGGER), "Validation must return false when the model is infeasible");
//     }

    /* -------------------------------------------------------------
     *  3. Null options field
     * ------------------------------------------------------------- */
//     @Test
//     void testValidateThrowsWhenOptionsIsNull(@Mock BasicLogger mockLogger) throws Exception {
//         // Use reflection to set the final field `options` to null.
//         Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
//         optionsField.setAccessible(true);
//         optionsField.set(model, null);
//         assertThrows(NullPointerException.class, () -> model.validate(DUMMY_LOGGER), "Calling validate when `options` is null must throw NPE");
//     }

    /* -------------------------------------------------------------
     *  4. Null feasibility context inside options
     * ------------------------------------------------------------- */
//     @Test
//     void testValidateThrowsWhenFeasibilityContextIsNull(@Mock BasicLogger mockLogger) throws Exception {
//         // Ensure the options object exists but its feasibility field is null.
//         Field feasibilityField = Optimisation.Options.class.getDeclaredField("feasibility");
//         feasibilityField.setAccessible(true);
//         feasibilityField.set(model.options, null);
//         assertThrows(NullPointerException.class, () -> model.validate(DUMMY_LOGGER), "Calling validate when `options.feasibility` is null must throw NPE");
//     }

    /* -------------------------------------------------------------
     *  5. getVariableValues throws a RuntimeException
     * ------------------------------------------------------------- */
//     @Test
//     void testValidatePropagatesExceptionFromGetVariableValues(@Mock BasicLogger mockLogger) {
//         // Spy the model by subclassing and forcing getVariableValues(NumberContext) to throw.
//         ExpressionsBasedModel spy = new ExpressionsBasedModel() {
// 
//             @Override
//             public Access1D<BigDecimal> getVariableValues(NumberContext ctx) {
//                 throw new RuntimeException("forced");
//             }
//         };
//         spy.newVariable("x").lower(0).upper(10).weight(1);
//         spy.minimise();
//         assertThrows(RuntimeException.class, () -> spy.validate(DUMMY_LOGGER), "Exception from getVariableValues must propagate");
//     }

    /* -------------------------------------------------------------
     *  6. getVariableValues returns null
     * ------------------------------------------------------------- */
//     @Test
//     void testValidateThrowsWhenGetVariableValuesReturnsNull(@Mock BasicLogger mockLogger) {
//         // Spy and return null for the call that supplies the solution.
//         ExpressionsBasedModel spy = new ExpressionsBasedModel() {
// 
//             @Override
//             public Access1D<BigDecimal> getVariableValues(NumberContext ctx) {
//                 return null;
//             }
//         };
//         spy.newVariable("x").lower(0).upper(10).weight(1);
//         spy.minimise();
//         assertThrows(NullPointerException.class, () -> spy.validate(DUMMY_LOGGER), "A null solution should cause NPE inside validate");
//     }

    /* -------------------------------------------------------------
     *  7. Logger throws a RuntimeException while logging
     * ------------------------------------------------------------- */
    @Test
    void testValidatePropagatesExceptionFromLogger() {
        // Use a feasible model – the validation logic will attempt to log
        // (even if nothing is wrong) and the exception must bubble up.
        model.newVariable("x").lower(0).upper(10).weight(1);
        model.minimise();
        assertThrows(RuntimeException.class, () -> model.validate(EXPLODING_LOGGER), "Exception thrown by logger must propagate");
    }

    /* -------------------------------------------------------------
     *  8. Custom (strict) feasibility context causing failure
     * ------------------------------------------------------------- */
//     @Test
//     void testValidateFailsWithStrictFeasibilityContext(@Mock BasicLogger mockLogger) throws Exception {
//         // Build a model that is feasible with the default tolerance.
//         model.newVariable("x").lower(0).upper(1).weight(1);
//         model.minimise();
//         // Replace the feasibility context with a very strict one (effectively zero tolerance).
//         NumberContext strict = new NumberContext(1, 1, 1, 1, 1, 1);
//         Field feasibilityField = Optimisation.Options.class.getDeclaredField("feasibility");
//         feasibilityField.setAccessible(true);
//         feasibilityField.set(model.options, strict);
//         // The same solution now violates the strict tolerance, so validation must be false.
//         assertFalse(model.validate(DUMMY_LOGGER), "Validation should fail when a stricter NumberContext is used");
//     }

    /* -------------------------------------------------------------
     *  9. Null logger – already covered in test 1, but kept for completeness
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testValidateWithNullLoggerParameter(boolean useFeasibleModel) {
        if (useFeasibleModel) {
            model.newVariable("x").lower(0).upper(10).weight(1);
            Optimisation.Result r = model.minimise();
            assertTrue(r.getState().isOptimal(), "Feasible model must solve optimally");
        } else {
            model.newVariable("x").lower(1).upper(0).weight(1);
            // No need to solve; the model is already contradictory.
        }
        // validate(null) must never throw; for infeasible models it returns false,
        // for feasible models it returns true.
        boolean expected = useFeasibleModel;
        assertEquals(expected, model.validate((BasicLogger) null), "validate(null) must return " + expected + " for the given model");
    }

    /**
     * The same model as above, but this time a dummy {@link BasicLogger}
     * is supplied to ensure that the logger can be passed without causing
     * any side‑effects.
     */
    @Test
    void testValidateReturnsTrueForFeasibleModel_WithDummyLogger() {
        model.newVariable("x").lower(0).upper(10).weight(1);
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Model should solve to an optimal state");
        boolean isValid = model.validate(DUMMY_LOGGER);
        assertTrue(isValid, "Validation should succeed when a logger is provided");
    }
}