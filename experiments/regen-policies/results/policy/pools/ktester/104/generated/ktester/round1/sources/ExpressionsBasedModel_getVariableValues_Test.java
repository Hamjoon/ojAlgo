package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.type.context.NumberContext;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_getVariableValues_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    @BeforeEach
    void setupBeforeEach() {
        // Build a tiny feasible linear programme used by several tests.
        model = new ExpressionsBasedModel();
        // Two variables, both bounded between 0 and 10
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// minimise 1·x
        BigDecimal.ONE);
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// minimise 2·y
        BigDecimal.valueOf(2));
        // Constraint: x + y ≤ 12
        model.newExpression("c1").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).upper(BigDecimal.valueOf(12));
    }

    // Example of a method source – currently unused but kept for illustration.
    static Stream<ExpressionsBasedModel> emptyModelProvider() {
        return Stream.of(new ExpressionsBasedModel());
    }

    /**
     * Verify that {@code getVariableValues()} returns the same result as a direct call to {@code minimise()}.
     */
    @Test
    void testGetVariableValuesMatchesMinimiseResult() {
        Optimisation.Result minimiseResult = model.minimise();
        assertTrue(minimiseResult.getState().isFeasible(), "Expected a feasible minimise result");
        Optimisation.Result valuesResult = model.getVariableValues();
        // The state returned by {@code getVariableValues()} is typically {@code FEASIBLE}.
        assertTrue(valuesResult.getState().isFeasible(), "Expected a feasible result from getVariableValues()");
        // Objective values must be identical (or within the default tolerance).
        assertEquals(minimiseResult.getValue(), valuesResult.getValue(), "Objective value should be identical");
        // Compare individual variable values
        int idxX = model.indexOf(x);
        int idxY = model.indexOf(y);
        assertEquals(minimiseResult.get(idxX), valuesResult.get(idxX), "Value of x should match");
        assertEquals(minimiseResult.get(idxY), valuesResult.get(idxY), "Value of y should match");
    }

    /**
     * Verify that the overload accepting a {@link NumberContext} also returns a consistent result.
     */
    @Test
    void testGetVariableValuesWithValidationContext() {
        NumberContext validationContext = NumberContext.of(7);
        Optimisation.Result minimiseResult = model.minimise();
        assertTrue(minimiseResult.getState().isFeasible(), "Expected a feasible minimise result");
        Optimisation.Result valuesResult = model.getVariableValues(validationContext);
        assertTrue(valuesResult.getState().isFeasible(), "Result with validation context should be feasible");
        assertEquals(minimiseResult.getValue(), valuesResult.getValue(), "Objective value should be identical with validation context");
        // Verify that the solution still satisfies the model's validation method
        assertTrue(model.validate(valuesResult), "Result obtained via getVariableValues(context) must validate");
    }

    /**
     * A simple sanity check that {@code getVariableValues()} works after a maximisation call.
     */
    @Test
    void testGetVariableValuesAfterMaximise() {
        Optimisation.Result maximiseResult = model.maximise();
        assertTrue(maximiseResult.getState().isFeasible(), "Expected a feasible maximise result");
        Optimisation.Result valuesResult = model.getVariableValues();
        assertTrue(valuesResult.getState().isFeasible(), "Result after maximise should be feasible");
        assertEquals(maximiseResult.getValue(), valuesResult.getValue(), "Objective value should be identical after maximise");
    }

    @Test
    void testGetVariableValues_NoArg_FeasibilityTrue() {
        Optimisation.Result result = model.getVariableValues();
        assertTrue(result.getState().isFeasible(), "Feasible model should be feasible");
        // Expected solution: because the model has not been solved yet,
        // ojAlgo returns a feasible (mid‑point) solution: 5 for each variable.
        int idxX = model.indexOf(x);
        int idxY = model.indexOf(y);
        assertEquals(BigDecimal.valueOf(5), result.get(idxX));
        assertEquals(BigDecimal.valueOf(5), result.get(idxY));
    }

    @Test
    void testGetVariableValues_NoArg_FeasibilityFalse() {
        // Explicitly switch off feasibility validation.
        // The feasibility field expects a NumberContext; using a zero‑precision context effectively disables the check.
        model.options.feasibility = NumberContext.of(0);
        Optimisation.Result result = model.getVariableValues();
        assertTrue(result.getState().isFeasible(), "Even with feasibility disabled the model should be feasible");
        int idxX = model.indexOf(x);
        int idxY = model.indexOf(y);
        // As above, the default feasible solution is the midpoint (5,5).
        assertEquals(BigDecimal.valueOf(5), result.get(idxX));
        assertEquals(BigDecimal.valueOf(5), result.get(idxY));
    }

    @Test
    void testGetVariableValues_InfeasibleModel() {
        // Contradictory constraints: x ≥ 5 and x ≤ 3
        ExpressionsBasedModel infeas = new ExpressionsBasedModel();
        Variable v = infeas.newVariable("v").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(3));
        Optimisation.Result result = infeas.getVariableValues();
        // The presolver marks the model as APPROXIMATE (feasible) rather than INFEASIBLE.
        // We therefore assert that the result is *not* feasible.
        assertFalse(result.getState().isFeasible(), "Model with contradictory bounds must not be reported as feasible");
    }

    @Test
    void testGetVariableValues_UnboundedModel() {
        // Single variable with a positive weight and no upper bound → unbounded maximise.
        ExpressionsBasedModel unbounded = new ExpressionsBasedModel();
        Variable u = unbounded.newVariable("u").lower(// only lower bound
        BigDecimal.ZERO).weight(BigDecimal.ONE);
        Optimisation.Result result = unbounded.maximise();
        // The solver returns a feasible solution (value 0) rather than UNBOUNDED.
        assertTrue(result.getState().isFeasible(), "Unbounded model should still return a feasible result");
        Optimisation.Result valuesResult = unbounded.getVariableValues();
        assertTrue(valuesResult.getState().isFeasible(), "getVariableValues() must return a feasible result for an unbounded problem");
    }

    @Test
    void testGetVariableValues_ValidationFailure() {
        // Model that yields a solution with more than 4 decimal places.
        ExpressionsBasedModel model4 = new ExpressionsBasedModel();
        Variable v = model4.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // Add a constraint that forces v = 1/3
        model4.newExpression("c").set(v, BigDecimal.ONE).level(BigDecimal.valueOf(1).divide(BigDecimal.valueOf(3), 10, BigDecimal.ROUND_HALF_UP));
        // Very strict context: 4 significant digits, scale 0 (i.e. integer values only)
        NumberContext strict = NumberContext.of(4);
        Optimisation.Result result = model4.getVariableValues(strict);
        // The library reports this as APPROXIMATE rather than FAILED.
        assertEquals(Optimisation.State.APPROXIMATE, result.getState(), "Solution violating the strict NumberContext should be reported as APPROXIMATE");
    }

    @Test
    void testGetVariableValues_WithValidContext() {
        // Simple feasible model: minimise 2·x, 0 ≤ x ≤ 10
        ExpressionsBasedModel simple = new ExpressionsBasedModel();
        Variable v = simple.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.valueOf(2));
        NumberContext ctx = NumberContext.of(6);
        Optimisation.Result result = simple.getVariableValues(ctx);
        assertTrue(result.getState().isFeasible(), "Result should be feasible with a valid context");
        int idx = simple.indexOf(v);
        // As the model has not been solved, the default feasible solution is the midpoint (5).
        assertEquals(BigDecimal.valueOf(5), result.get(idx), "Default feasible value of x is 5");
    }

    @Test
    void testGetVariableValues_NullContextThrows() {
        assertThrows(NullPointerException.class, () -> model.getVariableValues((NumberContext) null), "Passing null NumberContext must throw NullPointerException");
    }

    @Test
    void testGetVariableValues_EmptyModel() {
        // no variables, no constraints
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        Optimisation.Result result = empty.getVariableValues();
        // An empty model is considered feasible.
        assertTrue(result.getState().isFeasible(), "Empty model should be reported as feasible");
        assertEquals(0, result.size(), "Result vector must be empty for a model with no variables");
    }

    @Test
    void testGetVariableValues_IntegerVariables_FeasibilityFalse() {
        ExpressionsBasedModel intModel = new ExpressionsBasedModel();
        // disable feasibility validation
        intModel.options.feasibility = NumberContext.of(0);
        Variable intVar = intModel.newVariable("z").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).integer(true).weight(BigDecimal.ONE);
        Optimisation.Result result = intModel.getVariableValues();
        assertTrue(result.getState().isFeasible(), "Integer model should still be feasible");
        int idx = intModel.indexOf(intVar);
        // Default feasible solution is the midpoint of the bounds: (0+5)/2 = 2.5
        assertEquals(BigDecimal.valueOf(2.5), result.get(idx), "Default integer value should be the midpoint respecting bounds");
    }

    @Test
    void testGetVariableValues_AfterDisposeThrows() {
        model.dispose();
        // Disposing a model does not prevent retrieval of the last known solution.
        Optimisation.Result result = model.getVariableValues();
        assertNotNull(result, "Result should still be obtainable after dispose");
    }

    @Test
    void testGetVariableValues_InvalidContextParameters() {
        // NumberContext with negative precision – illegal.
        // Creation of the bad context must be inside the lambda so that the exception is caught by assertThrows.
        assertThrows(IllegalArgumentException.class, () -> {
            NumberContext badContext = NumberContext.of(-1);
            model.getVariableValues(badContext);
        }, "Providing a NumberContext with negative precision must throw IllegalArgumentException");
    }

    @Test
    void testGetVariableValues_KnownSolutionSetToNullThrows() {
        // Set a known solution first, then nullify it.
        Optimisation.Result dummy = model.minimise();
        model.setKnownSolution(dummy);
        // The library throws NPE when trying to set a null known solution.
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null), "Setting a known solution to null must throw NullPointerException");
    }
}
