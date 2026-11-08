package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.structure.Access1D;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.type.context.NumberContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_5_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    /**
     * A mock logger – the implementation of {@link BasicLogger} is not relevant for the test.
     */
//     @Mock
    private BasicLogger mockLogger;

    // an integer‑restricted variable (used in a few cases)
    private Variable intVar;

    /**
     * Simple wrapper around a var‑args array – used for the “normal” cases.
     */
    private static class SimpleArrayAccess1D implements Access1D<BigDecimal> {

        private final BigDecimal[] data;

        SimpleArrayAccess1D(BigDecimal... values) {
            this.data = values;
        }

        @Override
        public long count() {
            return data.length;
        }

        @Override
        public BigDecimal get(long index) {
            return data[(int) index];
        }
        // The remaining default methods are not used by the validator.
    }

    /**
     * Access1D that returns {@code null} for the first element.
     */
    private static class NullElementAccess1D implements Access1D<BigDecimal> {

        @Override
        public long count() {
            return 1L;
        }

        @Override
        public BigDecimal get(long index) {
            return null;
        }
    }

    /**
     * Access1D that throws {@code IndexOutOfBoundsException} on any element access.
     */
    private static class FaultyAccess1D implements Access1D<BigDecimal> {

        @Override
        public long count() {
            return 2L;
        }

        @Override
        public BigDecimal get(long index) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Access1D that throws {@code UnsupportedOperationException} when {@code count()} is called.
     */
    private static class SizeThrowingAccess1D implements Access1D<BigDecimal> {

        @Override
        public long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BigDecimal get(long index) {
            return BigDecimal.ONE;
        }
    }

    /**
     * Logger that throws a RuntimeException when any method is invoked.
     */
    private static class ThrowingLogger implements BasicLogger {

        @Override
        public void println(Object o) {
            throw new RuntimeException("logger failure");
        }
        // other BasicLogger methods can be left empty – they will never be called.
    }

    /**
     * Simple mock logger used in the parameterised test source.
     */
    private static class MockLogger implements BasicLogger {

        @Override
        public void println(Object o) {
            /* ignore */
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a very small model: 0 ≤ x ≤ 10 and the additional constraint x ≤ 5.
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ZERO);
        // Constraint: x ≤ 5
        model.newExpression("c1").set(x, BigMath.ONE).upper(BigDecimal.valueOf(5));
        // Re‑initialise model to have both a normal variable and an integer‑restricted variable.
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ZERO);
        intVar = model.newVariable("intVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(true).weight(BigDecimal.ZERO);
        model.newExpression("c1").set(x, BigMath.ONE).upper(BigDecimal.valueOf(5));
        // Initialise the mock logger used in most tests.
        mockLogger = new MockLogger();
    }

    /* --------------------------------------------------------------- *
     *  Parameter source for the bulk of the test cases (branch,       *
     *  boundary and special cases).                                   *
     * --------------------------------------------------------------- */
    private static Stream<Arguments> validationScenarios() {
        return Stream.of(// 1. Null solution – should return false without touching logger
        Arguments.of(null, new MockLogger(), false, "The solution argument is null – the method should immediately return false."), // 2. Valid solution, null logger – should succeed (logger optional)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.TEN), null, true, "A valid solution with a null logger should be accepted."), // 3. Size mismatch (model has 1 variable, solution size 2)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(5), BigDecimal.valueOf(7)), new MockLogger(), false, "Solution size does not match number of variables – validation must fail."), // 4. Lower‑bound violation (x = -1)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(-1)), new MockLogger(), false, "Solution violates variable lower bound – should be reported as infeasible."), // 5. Integer‑restriction breach (non‑integer value for intVar)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(3.5)), new MockLogger(), false, "Non‑integer value supplied for an integer‑restricted variable – must fail."), // 6. Fully feasible solution (x = 2)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(2)), new MockLogger(), true, "All constraints satisfied – validation should succeed."), // 7. Upper‑bound breach (x = 12, upper bound = 10)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(12)), new MockLogger(), false, "Solution exceeds variable upper bound – must be rejected."), // 8. Edge case: solution equals upper bound (x = 10)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(10)), new MockLogger(), true, "Solution exactly on the upper bound – should be accepted."), // 9. Fixed variable (lower == upper == 5) – wrong value (4)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(4)), new MockLogger(), false, "Variable fixed to 5 but solution provides 4 – must fail."), //10. Fixed variable – correct value (5)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(5)), new MockLogger(), true, "Variable fixed to 5 and solution provides 5 – should succeed."), //11. Integer‑restricted variable with integer value (7)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(7)), new MockLogger(), true, "Integer‑restricted variable receives an integer value inside bounds – succeed."), //12. Feasibility tolerance test – within tolerance (5.005)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(5.005)), new MockLogger(), true, "Solution 5.005 is within 0.01 tolerance of lower bound 5 – should be accepted."), //13. Feasibility tolerance test – outside tolerance (5.02)
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.valueOf(5.02)), new MockLogger(), false, "Solution 5.02 exceeds 0.01 tolerance – must be rejected."), //14. Empty model – empty solution
        Arguments.of(new SimpleArrayAccess1D(), new MockLogger(), true, "Empty model with empty solution should validate as true."), //15. Empty model – non‑empty solution
        Arguments.of(new SimpleArrayAccess1D(BigDecimal.ONE), new MockLogger(), false, "Empty model but solution contains an element – size mismatch should fail."));
    }

    @Test
    void testValidateWithFeasibleSolution() {
        // Solve the model – the optimiser will produce a feasible solution (x = 0).
        Optimisation.Result result = model.minimise();
        // The result implements Access1D<BigDecimal>, so we can pass it directly.
        boolean ok = model.validate(result, mockLogger);
        assertTrue(ok, "The optimiser's solution should be recognised as feasible.");
    }

    @Test
    void testValidateWithInfeasibleSolution() {
        // Construct a solution that violates the constraint x ≤ 5 (e.g. x = 6).
        Array1D<BigDecimal> infeasible = new Array1D<>(1);
        infeasible.set(0, BigDecimal.valueOf(6));
        // The model should reject this solution.
        boolean ok = model.validate((Access1D<BigDecimal>) infeasible, mockLogger);
        assertFalse(ok, "A solution that breaks the model's constraints must be reported as infeasible.");
    }

    /**
     * Parameterised test that executes the scenarios defined in {@link #validationScenarios()}.
     *
     * <p>
     * The test dynamically re‑configures the model where required (e.g. fixing a variable,
     * adding an integer‑restricted variable, changing the feasibility tolerance).
     * </p>
     */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("validationScenarios")
    void testValidateScenarios(Access1D<BigDecimal> solution, BasicLogger logger, Object expectedResult, String description) {
        // Special preparation for a few scenarios
        if (solution != null && solution.count() == 1) {
            // Detect the “fixed variable” cases by the description text.
            if (description.contains("fixed to 5")) {
                x.lower(BigDecimal.valueOf(5));
                x.upper(BigDecimal.valueOf(5));
            }
            // Detect the integer‑restriction case (solution supplied for intVar)
            if (description.contains("integer‑restricted variable")) {
                // Ensure the model contains only the integer variable for this case.
                model = new ExpressionsBasedModel();
                intVar = model.newVariable("intVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(true);
            }
        }
        // Adjust tolerance for the two tolerance‑related cases.
        if (description.contains("tolerance")) {
            // Set a feasibility context with a tolerance of 0.01.
            model.options.feasibility = NumberContext.of(0.01);
        }
        // Execute the method under test
        if (expectedResult instanceof Boolean) {
            boolean expected = (Boolean) expectedResult;
            boolean actual = model.validate(solution, logger);
            assertEquals(expected, actual, description);
        } else if (expectedResult instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> expectedException = (Class<? extends Throwable>) expectedResult;
            assertThrows(expectedException, () -> model.validate(solution, logger), description);
        }
    }

    @Test
    void testLoggerFailurePropagation() {
        Access1D<BigDecimal> solution = new SimpleArrayAccess1D(BigDecimal.valueOf(2));
        BasicLogger throwingLogger = new ThrowingLogger();
        // The validator must propagate the RuntimeException coming from the logger.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.validate(solution, throwingLogger));
        assertEquals("logger failure", ex.getMessage());
    }

    @Test
    void testNullElementInSolution() {
        Access1D<BigDecimal> solution = new NullElementAccess1D();
        assertThrows(NullPointerException.class, () -> model.validate(solution, mockLogger), "A null element inside the solution should cause a NullPointerException.");
    }

    @Test
    void testAccess1DThrowsOnElementAccess() {
        Access1D<BigDecimal> solution = new FaultyAccess1D();
        assertThrows(IndexOutOfBoundsException.class, () -> model.validate(solution, mockLogger), "Access1D that throws on element access must let the exception propagate.");
    }

    @Test
    void testAccess1DThrowsOnSizeCall() {
        Access1D<BigDecimal> solution = new SizeThrowingAccess1D();
        assertThrows(UnsupportedOperationException.class, () -> model.validate(solution, mockLogger), "Access1D that throws on size() must let the exception propagate.");
    }

    @Test
    void testModelOptionsNull() throws Exception {
        // Use reflection to set the options field to null (simulating a corrupted model).
        java.lang.reflect.Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        Access1D<BigDecimal> solution = new SimpleArrayAccess1D(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> model.validate(solution, mockLogger), "When model.options is null, validate should throw NullPointerException.");
    }
}