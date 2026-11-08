package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.type.context.NumberContext;
import java.util.Arrays;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_simplify_Test {

    private ExpressionsBasedModel original;

//     @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test – the default constructor creates a model with default options.
        original = new ExpressionsBasedModel();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources that might be allocated
        // by the underlying solvers. This is safe even if the model has already been disposed.
        if (original != null) {
            original.dispose();
        }
    }

    /**
     * Verify that {@code simplify()} returns a new instance and that the simplified model
     * yields the same optimal solution as the original model for a simple feasible problem.
     */
//     @Test
//     void testSimplifyReturnsNewModelAndPreservesSolution() {
//         // Build a tiny linear programme:
//         // maximise  x + 2*y
//         // 0 ≤ x ≤ 5
//         // 0 ≤ y ≤ 4
//         // x + y ≤ 6
//         Variable x = original.newVariable("x").lower(0).upper(5).weight(1);
//         Variable y = original.newVariable("y").lower(0).upper(4).weight(2);
//         // Constraint x + y ≤ 6
//         original.newExpression("c1").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).upper(BigDecimal.valueOf(6));
//         // Solve the original model (maximise) – we keep the reference result for later comparison.
//         Optimisation.Result originalResult = original.maximise();
//         // Simplify the model.
//         ExpressionsBasedModel simplified = original.simplify();
//         // The simplified model must be a *different* object.
//         assertNotSame(original, simplified, "simplify() should return a new model instance");
//         // The options object is public and final; it should be the same instance because the copy
//         // constructor re‑uses the original options reference.
//         assertSame(original.options, simplified.options, "Options should be shared between copies");
//         // The simplified model should produce the same optimal value and variable values.
//         Optimisation.Result simplifiedResult = simplified.maximise();
//         assertTrue(original.validate(originalResult), "Original result must be valid");
//         assertTrue(simplified.validate(simplifiedResult), "Simplified result must be valid");
//         // Compare objective values (allowing the tiny numerical tolerance used by ojAlgo).
//         NumberContext precision = NumberContext.of(7);
//         assertEquals(originalResult.getValue(), simplifiedResult.getValue(), precision.getPrecision());
//         // Compare each variable value.
//         assertEquals(originalResult.get(original.indexOf(x)), simplifiedResult.get(simplified.indexOf(x)));
//         assertEquals(originalResult.get(original.indexOf(y)), simplifiedResult.get(simplified.indexOf(y)));
//     }

    /**
     * Verify that the presolver detects an infeasible model and that the simplified model
     * reports the infeasibility without invoking a solver.
     */
//     @Test
//     void testSimplifyDetectsInfeasibility() {
//         // Variable fixed to 5 (level = lower = upper = 5)
//         Variable x = original.newVariable("x").level(BigDecimal.valueOf(5));
//         // Add a contradictory constraint: x must be at least 6.
//         original.newExpression("c_infeas").lower(BigDecimal.valueOf(6)).set(x, BigDecimal.ONE);
//         // The original model would need a solver to discover infeasibility; we are interested
//         // in the behaviour *after* simplify().
//         ExpressionsBasedModel simplified = original.simplify();
//         // The simplified model should already know that the problem is infeasible.
//         Optimisation.Result infeasibleResult = simplified.minimise();
//         assertEquals(Optimisation.State.INFEASIBLE, infeasibleResult.getState(), "Simplified model should report INFEASIBLE without solving");
//     }

    /**
     * A regression test that ensures the copy‑constructor flags (shallow, prune) used by
     * {@code simplify()} do not unintentionally drop variables or expressions that are
     * required for a correct solution.
     *
     * The model contains a redundant expression that should be removed by the presolver,
     * but the remaining problem must stay solvable.
     */
//     @Test
//     void testSimplifyPrunesRedundantExpressions() {
//         // Variable with a wide bound.
//         Variable v = original.newVariable("v").lower(0).upper(10).weight(1);
//         // Redundant expression: v ≤ 10 (already implied by the variable bound).
//         original.newExpression("redundant").set(v, BigDecimal.ONE).upper(BigDecimal.TEN);
//         // Non‑redundant constraint: v ≥ 3
//         original.newExpression("essential").set(v, BigDecimal.ONE).lower(BigDecimal.valueOf(3));
//         // Solve before simplification – should be feasible with v = 3 (minimum) or v = 10 (maximum).
//         Optimisation.Result before = original.minimise();
//         assertTrue(before.getState().isOptimal(), "Original model should be optimal");
//         // Simplify – the redundant expression should be dropped.
//         ExpressionsBasedModel simplified = original.simplify();
//         // The simplified model must still be feasible and give the same optimal value.
//         Optimisation.Result after = simplified.minimise();
//         assertTrue(after.getState().isOptimal(), "Simplified model should remain optimal");
//         // The optimal value is simply the weight (1) times the lower bound (3) = 3.
//         assertEquals(BigDecimal.valueOf(3), after.getValue());
//     }

//     @Test
//     void testSimplifyEmptyModel() {
//         // No variables / expressions added.
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(0, simplified.countVariables(), "Empty model must have zero variables");
//         assertEquals(0, simplified.countExpressions(), "Empty model must have zero expressions");
//         assertFalse(simplified.isInfeasible(), "Empty model must not be infeasible");
//     }

//     @Test
//     void testSimplifyModelWithSingleFreeVariable() {
//         // default unbounded
//         Variable x = original.newVariable("x");
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(1, simplified.countVariables(), "Model must contain exactly one variable");
//         assertEquals(0, simplified.countExpressions(), "Model must contain no expressions");
//         // Verify the variable name is preserved
//         boolean nameFound = simplified.variables().anyMatch(v -> "x".equals(v.getName()));
//         assertTrue(nameFound, "Simplified model must contain variable named 'x'");
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelWithFixedVariable() {
//         Variable y = original.newVariable("y").level(BigDecimal.valueOf(5));
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(1, simplified.countVariables(), "One variable expected");
//         assertEquals(1, simplified.getFixedVariables().size(), "Variable must be reported as fixed");
//         assertEquals(0, simplified.getFreeVariables().size(), "No free variables expected");
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelWithQuadraticExpression() {
//         Variable x = original.newVariable("x");
//         Variable y = original.newVariable("y");
//         // Quadratic term x*x + y*y  (using the quadratic API of Expression)
//         original.newExpression("quad").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).quadratic(x, x, // x²
//         BigDecimal.ONE).quadratic(y, y, // y²
//         BigDecimal.ONE).weight(// just to keep it in the model
//         BigDecimal.ONE);
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(1, simplified.countExpressions(), "Quadratic expression must be retained");
//         assertTrue(simplified.isAnyExpressionQuadratic(), "Expression must be recognised as quadratic");
//         assertTrue(simplified.isAnyConstraintQuadratic(), "Quadratic flag for constraints must be true");
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelWithInfeasibleConstraint() {
//         Variable z = original.newVariable("z").lower(0).upper(10);
//         original.newExpression("bad").lower(BigDecimal.valueOf(20)).upper(BigDecimal.valueOf(10)).set(z, BigDecimal.ONE);
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(1, simplified.countExpressions(), "One infeasible expression expected");
//         assertTrue(simplified.isInfeasible(), "Simplified model must be marked infeasible");
//     }

//     @Test
//     void testSimplifyModelWithIntegerVariablesAndNoRelaxation() {
//         Variable i1 = original.newVariable("i1").integer(true);
//         Variable i2 = original.newVariable("i2").integer(true);
//         original.newExpression("c").set(i1, BigDecimal.ONE).set(i2, BigDecimal.ONE).upper(BigDecimal.valueOf(5));
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(2, simplified.getIntegerVariables().size(), "Both integer variables must be retained");
//         assertFalse(simplified.isRelaxed(), "Model must not be automatically relaxed");
//     }

//     @Test
//     void testSimplifyModelWithLinearObjective() {
//         Variable x = original.newVariable("x");
//         Variable y = original.newVariable("y");
//         // Linear objective: minimise 3*x + 4*y
//         original.objective().set(x, BigDecimal.valueOf(3)).set(y, BigDecimal.valueOf(4)).weight(// weight 1 => pure objective
//         BigDecimal.ONE);
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(2, simplified.countVariables(), "Two variables must be present");
//         assertEquals(1, simplified.countExpressions(), "Exactly one expression (the objective) must be present");
//         assertFalse(simplified.isAnyExpressionQuadratic(), "Objective must be linear");
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelWithMultipleLinearConstraints() {
//         Variable a = original.newVariable("a");
//         Variable b = original.newVariable("b");
//         Variable c = original.newVariable("c");
//         original.newExpression("c1").set(a, BigDecimal.ONE).set(b, BigDecimal.ONE).upper(BigDecimal.valueOf(10));
//         original.newExpression("c2").set(b, BigDecimal.ONE).set(c, BigDecimal.ONE.negate()).lower(BigDecimal.valueOf(2));
//         original.newExpression("c3").set(a, BigDecimal.ONE).set(c, BigDecimal.valueOf(2)).level(BigDecimal.valueOf(7));
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(3, simplified.countVariables(), "Three variables must be retained");
//         assertEquals(3, simplified.countExpressions(), "All three constraints must be retained");
//         assertFalse(simplified.isAnyExpressionQuadratic(), "No quadratic expressions expected");
//         assertFalse(simplified.isAnyConstraintQuadratic(), "No quadratic constraints expected");
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelWithVariableBoundsPreserved() {
//         Variable x = original.newVariable("x").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8));
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(1, simplified.countVariables(), "One variable expected");
//         Variable sx = simplified.getVariable(0);
//         assertEquals(BigDecimal.valueOf(2), sx.lower(), "Lower bound must be preserved");
//         assertEquals(BigDecimal.valueOf(8), sx.upper(), "Upper bound must be preserved");
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelAfterRelaxation() {
//         Variable i1 = original.newVariable("i1").integer(true);
//         Variable i2 = original.newVariable("i2").integer(true);
//         original.newExpression("c").set(i1, BigDecimal.ONE).set(i2, BigDecimal.ONE).upper(BigDecimal.valueOf(5));
//         // mark model as relaxed
//         original.relax();
//         assertTrue(original.isRelaxed(), "Original model must be relaxed before simplify");
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(2, simplified.getIntegerVariables().size(), "Integer variables must remain");
//         assertTrue(simplified.isRelaxed(), "Relaxed flag must survive simplification");
//     }

//     @Test
//     void testSimplifyModelWithRedundantConstraintRemovedByPresolver() {
//         // Ensure the default presolvers are active (they are by default)
//         Variable p = original.newVariable("p");
//         Variable q = original.newVariable("q");
//         original.newExpression("dup1").set(p, BigDecimal.ONE).set(q, BigDecimal.ONE).upper(BigDecimal.valueOf(10));
//         original.newExpression("dup2").set(p, BigDecimal.ONE).set(q, BigDecimal.ONE).upper(BigDecimal.valueOf(10));
//         assertEquals(2, original.countExpressions(), "Pre‑simplify expression count must be 2");
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(1, simplified.countExpressions(), "Duplicate constraint must be eliminated");
//         // The presolver flag is not directly exposed; the reduction in expression count is sufficient evidence.
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyModelWithSpecialOrderedSet() {
//         Variable s1 = original.newVariable("s1");
//         Variable s2 = original.newVariable("s2");
//         Variable s3 = original.newVariable("s3");
//         // Type 1 = SOS1 (at most one variable can be non‑zero)
//         original.addSpecialOrderedSet(Arrays.asList(s1, s2, s3), 1, null);
//         ExpressionsBasedModel simplified = original.simplify();
//         assertNotSame(original, simplified);
//         assertEquals(3, simplified.countVariables(), "Three variables must be present");
//         // The SOS information is stored internally; we verify that the model still reports a non‑empty set.
//         // The public API does not expose SOS directly, so we rely on the fact that no exception was thrown
//         // and the model remains feasible.
//         assertFalse(simplified.isInfeasible(), "Model must stay feasible");
//     }

//     @Test
//     void testSimplifyDisposedModelThrowsException() {
//         original.dispose();
//         assertThrows(IllegalStateException.class, () -> original.simplify(), "Calling simplify() on a disposed model must throw IllegalStateException");
//     }

//     @Test
//     void testSimplifyModelWithNullVariableNameThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> {
//             original.addVariable(null);
//             original.simplify();
//         }, "Null variable name should cause NullPointerException during simplify()");
//     }

//     @Test
//     void testSimplifyModelWithNullExpressionNameThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> {
//             original.addExpression(null);
//             original.simplify();
//         }, "Null expression name should cause NullPointerException during simplify()");
//     }

//     @Test
//     void testSimplifyModelWithOrphanVariableInExpressionThrowsIAE() {
//         // Create a variable that does NOT belong to the model.
//         Variable orphan = new Variable("orphan");
//         assertThrows(IllegalArgumentException.class, () -> {
//             original.newExpression("e").set(orphan, BigDecimal.ONE);
//             original.simplify();
//         }, "Expression referencing an orphan variable must cause IllegalArgumentException");
//     }

//     @Test
//     void testSimplifyModelWithFaultyPresolverPropagatesException() {
//         // Register a presolver that throws a RuntimeException when invoked.
//         Presolver faulty = new Presolver() {
// 
//             @Override
//             public void presolve(ExpressionsBasedModel model) {
//                 throw new RuntimeException("Intentional failure in presolver");
//             }
// 
//             @Override
//             public int compareTo(Presolver o) {
//                 return 0;
//             }
//         };
//         ExpressionsBasedModel.addPresolver(faulty);
//         // Add a trivial expression so that presolve() is actually called.
//         original.newExpression("e").set(original.newVariable("x"), BigDecimal.ONE);
//         RuntimeException thrown = assertThrows(RuntimeException.class, () -> original.simplify(), "Faulty presolver must cause RuntimeException to propagate");
//         assertTrue(thrown.getMessage().contains("Intentional failure"), "Exception message must match");
//     }
}