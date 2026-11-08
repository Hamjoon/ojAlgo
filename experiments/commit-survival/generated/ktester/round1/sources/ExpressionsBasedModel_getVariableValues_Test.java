package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.ojalgo.type.context.NumberContext;
// import org.ojalgo.exception.ProgrammingError;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getVariableValues_Test {

    /**
     * A fresh model is created for each test to avoid cross‑test contamination.
     */
    private ExpressionsBasedModel model;

//     @BeforeEach
    void setupBeforeEach() {
        // initialise a fresh model for every test
        model = new ExpressionsBasedModel();
    }

//     @AfterEach
    void teardownAfterEach() {
        // release reference – no external resources to clean up
        model = null;
    }

    static Stream<Arguments> dummyProvider() {
        // placeholder – not used in current suite
        return Stream.empty();
    }

    /**
     * A simple feasible problem:
     *
     * <pre>
     *   minimise  x
     *   subject to 5 ≤ x ≤ 5   (i.e. x = 5)
     *   0 ≤ x ≤ 10            (variable bounds)
     * </pre>
     *
     * The optimal solution is x = 5 and the state must be {@link Optimisation.State#OPTIMAL}.
     */
//     @Test
//     void testGetVariableValues_Optimal() {
//         // Create a variable with reasonable bounds and a weight (objective contribution)
//         Variable x = model.newVariable("x").lower(// 0 ≤ x
//         BigDecimal.ZERO).upper(// x ≤ 10
//         BigDecimal.TEN).weight(// minimise x
//         BigDecimal.ONE);
//         // Add a constraint that forces x to be exactly 5
//         model.newExpression("force_x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).set(x, // 1·x = 5
//         BigDecimal.ONE);
//         // Solve the model
//         Optimisation.Result result = model.getVariableValues();
//         // Verify that the solver reports an optimal solution
//         assertTrue(result.getState().isOptimal(), () -> "Expected OPTIMAL state but got " + result.getState());
//         // Verify the variable value (index lookup via model)
//         int idx = model.indexOf(x);
//         BigDecimal value = result.get(idx);
//         assertEquals(BigDecimal.valueOf(5), value, () -> "Variable x should be 5, but was " + value);
//     }

    /**
     * An infeasible problem:
     *
     * <pre>
     *   minimise  x
     *   subject to 5 ≤ x ≤ 5   (i.e. x = 5)
     *   0 ≤ x ≤ 1              (variable bounds – contradictory)
     * </pre>
     *
     * The model cannot satisfy both constraints, therefore the returned
     * {@link Optimisation.Result} must have state {@link Optimisation.State#INFEASIBLE}.
     */
//     @Test
//     void testGetVariableValues_Infeasible() {
//         // Variable with a tight upper bound that conflicts with the constraint below
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(// x ≤ 1
//         BigDecimal.ONE).weight(// objective does not matter for infeasibility
//         BigDecimal.ONE);
//         // Constraint that forces x to be 5, which is impossible given the variable bound
//         model.newExpression("force_x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).set(x, BigDecimal.ONE);
//         // Solve the model
//         Optimisation.Result result = model.getVariableValues();
//         // The solver should report infeasibility
//         assertEquals(Optimisation.State.INFEASIBLE, result.getState(), () -> "Expected INFEASIBLE state but got " + result.getState());
//     }

//     @Test
//     void testFeasible_SingleVariable_OptimalAtUpperBound() {
//         // Variable with default lower bound 0, upper bound 10, objective minimise x.
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// minimise x
//         BigDecimal.ONE);
//         // Constraint fixing x to 5
//         model.newExpression("fix_x").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)).set(x, BigDecimal.ONE);
//         Optimisation.Result result = model.getVariableValues();
//         assertTrue(result.getState().isOptimal(), () -> "Expected OPTIMAL state but got " + result.getState());
//         int idx = model.indexOf(x);
//         assertEquals(BigDecimal.valueOf(5), result.get(idx), () -> "Variable x should be 5, but was " + result.get(idx));
//     }

//     @Test
//     void testFeasible_TwoVariables_ObjectiveWithUpperBound() {
//         // Variables
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).weight(// not part of objective
//         BigDecimal.ZERO);
//         Variable y = model.newVariable("y").lower(BigDecimal.ZERO).weight(// maximise y (will switch sense later)
//         BigDecimal.ONE);
//         // Constraint: x + y = 6
//         model.newExpression("sum").lower(BigDecimal.valueOf(6)).upper(BigDecimal.valueOf(6)).set(x, BigDecimal.ONE).set(y, BigDecimal.ONE);
//         // Upper bound on y forces optimum y = 3 (and consequently x = 3)
//         model.newExpression("y_upper").upper(BigDecimal.valueOf(3)).set(y, BigDecimal.ONE);
//         // Switch to maximisation (negative weight for minimise)
//         model.options.sense = Optimisation.Sense.MAX;
//         Optimisation.Result result = model.getVariableValues();
//         assertTrue(result.getState().isOptimal(), () -> "Expected OPTIMAL state but got " + result.getState());
//         assertEquals(BigDecimal.valueOf(3), result.get(model.indexOf(y)), () -> "Variable y should be 3");
//         assertEquals(BigDecimal.valueOf(3), result.get(model.indexOf(x)), () -> "Variable x should be 3");
//     }

//     @Test
//     void testInfeasible_Model() {
//         Variable x = model.newVariable("x").lower(// x ≥ 1
//         BigDecimal.ONE).upper(// x ≤ 0  (contradiction)
//         BigDecimal.ZERO).weight(BigDecimal.ONE);
//         Optimisation.Result result = model.getVariableValues();
//         assertEquals(Optimisation.State.INFEASIBLE, result.getState(), () -> "Expected INFEASIBLE state but got " + result.getState());
//     }

//     @Test
//     void testUnbounded_Model() {
//         // Free variable, no bounds, minimise x → unbounded below
//         Variable x = model.newVariable("x").weight(// minimise x
//         BigDecimal.ONE);
//         Optimisation.Result result = model.getVariableValues();
//         assertEquals(Optimisation.State.UNBOUNDED, result.getState(), () -> "Expected UNBOUNDED state but got " + result.getState());
//     }

//     @Test
//     void testIntegerVariable_Optimal() {
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).integer(// make it integer
//         true).weight(// maximise x (negated weight)
//         BigDecimal.ONE.negate());
//         Optimisation.Result result = model.getVariableValues();
//         assertTrue(result.getState().isOptimal(), () -> "Expected OPTIMAL state but got " + result.getState());
//         assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)), () -> "Integer variable x should be 5");
//     }

//     @Test
//     void testKnownSolution_Shortcut() {
//         // Build a trivial model (won't actually be solved)
//         Variable x = model.newVariable("x").weight(BigDecimal.ONE);
//         // Create a simple known solution that returns the desired state and value
//         Optimisation.Result known = new Optimisation.Result() {
// 
//             @Override
//             public Optimisation.State getState() {
//                 return Optimisation.State.OPTIMAL;
//             }
// 
//             @Override
//             public BigDecimal get(int index) {
//                 // Only one variable in this model, index 0 corresponds to x
//                 return BigDecimal.valueOf(2);
//             }
//         };
//         // Register the known solution
//         model.setKnownSolution(known);
//         // The call should return the known result directly, without invoking a solver
//         Optimisation.Result result = model.getVariableValues();
//         assertSame(known, result, "Expected the known solution to be returned");
//         assertEquals(BigDecimal.valueOf(2), result.get(model.indexOf(x)), "Variable x should be 2 as per the known solution");
//     }

//     @Test
//     void testStrictValidationContext_FailsValidation() {
//         // Simple feasible model where optimal x = 0
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(// minimise x
//         BigDecimal.ONE);
//         // Solve normally
//         Optimisation.Result result = model.getVariableValues();
//         assertTrue(result.getState().isOptimal());
//         // Set a strict (zero‑tolerance) validation context.
//         NumberContext strict = NumberContext.of(0);
//         model.options.feasibility = strict;
//         // Validation should now fail because the default tolerance used by the solver
//         // is larger than zero.
//         boolean valid = model.validate(result, strict);
//         assertFalse(valid, "Validation should fail under a zero‑tolerance context");
//     }

//     @Test
//     void testGetVariableValues_NullValidationContext_ThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> model.getVariableValues(null), "Calling getVariableValues(null) should throw NullPointerException");
//     }

//     @Test
//     void testDisposedModel_ThrowsProgrammingError() {
//         model.dispose();
//         assertThrows(ProgrammingError.class, () -> model.getVariableValues(), "Operations on a disposed model must raise ProgrammingError");
//     }

//     @Test
//     void testQuadraticObjectiveWithoutSolver_ThrowsProgrammingError() {
//         Variable x = model.newVariable("x").weight(BigDecimal.ONE);
//         // Create a quadratic expression: x²
//         Expression quad = model.newExpression("quad_obj").weight(BigDecimal.ONE);
//         // Add quadratic factor (x * x) with coefficient 1
//         quad.setQuadraticFactor(x, x, BigDecimal.ONE);
//         // No quadratic solver is registered in the default test environment.
//         assertThrows(ProgrammingError.class, () -> model.getVariableValues(), "Quadratic objectives without a solver must raise ProgrammingError");
//     }

//     @Test
//     void testIntegerVariableWithoutSolver_ThrowsProgrammingError() {
//         // Attempt to clear any integer solver registration – if the API provides such a method.
//         try {
//             Class<?> intSolverCls = Class.forName("org.ojalgo.optimisation.integer.IntegerSolver");
//             intSolverCls.getMethod("clear").invoke(null);
//         } catch (Exception ignored) {
//             // If the clear method does not exist, continue – the library will still throw
//             // ProgrammingError when it cannot find a solver.
//         }
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).integer(// integer variable
//         true).weight(// maximise
//         BigDecimal.ONE.negate());
//         assertThrows(ProgrammingError.class, () -> model.getVariableValues(), "Missing integer solver should cause ProgrammingError");
//     }

//     @Test
//     void testNullOptionsFeasibility_ThrowsNPE() throws Exception {
//         // Use reflection to set the public field `feasibility` to null.
//         Field feasibilityField = Optimisation.Options.class.getDeclaredField("feasibility");
//         feasibilityField.setAccessible(true);
//         feasibilityField.set(model.options, null);
//         assertThrows(NullPointerException.class, () -> model.getVariableValues(), "Null options.feasibility should result in NullPointerException");
//     }

//     @Test
//     void testInvalidIntegrationSwitch_ThrowsIllegalStateException() {
//         // Enable integration without having registered any integration.
//         model.setIntegrationSwitch(true);
//         assertThrows(IllegalStateException.class, () -> model.getVariableValues(), "Enabling integration without a registered integration must throw IllegalStateException");
//     }
}