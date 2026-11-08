package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.ConcurrentModificationException;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.ojalgo.function.constant.BigMath.*;
import org.ojalgo.function.constant.BigMath;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyExpressionQuadratic_Test {

    /**
     * Simple stub implementation of {@link Expression} that returns the
     * supplied boolean values for the methods used by {@code isAnyExpressionQuadratic()}.
     */
//     private static class SimpleExpression extends Expression {

        private final boolean anyQuadratic;

        private final boolean objective;

        private final boolean constraint;

        private final boolean redundant;

        private final String methodToThrow;

        SimpleExpression(boolean anyQuadratic, boolean objective, boolean constraint, boolean redundant) {
            this(anyQuadratic, objective, constraint, redundant, null);
        }

        SimpleExpression(boolean anyQuadratic, boolean objective, boolean constraint, boolean redundant, String methodToThrow) {
            // Expression has a protected no‑arg constructor in the library.
//             super();
            this.anyQuadratic = anyQuadratic;
            this.objective = objective;
            this.constraint = constraint;
            this.redundant = redundant;
            this.methodToThrow = methodToThrow;
        }

        private void maybeThrow(String method) {
            if (methodToThrow != null && method.equals(methodToThrow)) {
                throw new IllegalStateException("faulty " + method);
            }
        }

        @Override
        public boolean isAnyQuadraticFactorNonZero() {
            maybeThrow("isAnyQuadraticFactorNonZero");
            return anyQuadratic;
        }

//         @Override
//         public boolean isObjective() {
//             maybeThrow("isObjective");
//             return objective;
//         }

//         @Override
//         public boolean isConstraint() {
//             maybeThrow("isConstraint");
//             return constraint;
//         }

        @Override
        public boolean isRedundant() {
            maybeThrow("isRedundant");
            return redundant;
        }

        // -----------------------------------------------------------------
        // The remaining methods of the Expression class are not needed for
        // the unit‑tests in this class. They are implemented as
        // UnsupportedOperationException to satisfy the compiler.
        // -----------------------------------------------------------------
//         @Override
//         public Expression set(Variable variable, Number value) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Expression quadraticFactor(Variable v1, Variable v2, double factor) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Expression weight(Number w) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Expression lower(Number l) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Expression upper(Number u) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Expression level(Number lvl) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Number getWeight() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Number getLowerLimit() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Number getUpperLimit() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Number getLevel() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public String getName() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public String toString() {
//             return "SimpleExpression";
//         }
    }

    /**
     * Replace the private {@code myExpressions} map of a model with the supplied map.
     */
    private static void setExpressionsMap(ExpressionsBasedModel model, Map<String, Expression> map) throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        field.set(model, map);
    }

    /**
     * Create a mock {@link Expression} that returns the given boolean values for the
     * methods used by {@code isAnyExpressionQuadratic()}.
     */
    private static Expression mockExpression(boolean anyQuadratic, boolean isObjective, boolean isConstraint, boolean isRedundant) {
        return new SimpleExpression(anyQuadratic, isObjective, isConstraint, isRedundant);
    }

    private static Expression mockFaultyExpression(String methodToThrow) {
        return new SimpleExpression(false, false, false, false, methodToThrow);
    }

    /**
     * Model without any expressions – should report “no quadratic expression”.
     */
    @Test
    void testNoExpressions() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isAnyExpressionQuadratic(), "A model with no expressions must return false");
    }

    /**
     * Model with a single linear expression (neither objective nor constraint).
     * The expression contains only linear terms, therefore the method must return {@code false}.
     */
    @Test
    void testLinearExpressionOnly() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Expression expr = model.addExpression("linear");
        expr.set(x, ONE);
        expr.set(y, TWO);
        assertFalse(model.isAnyExpressionQuadratic(), "A model with only linear expressions must return false");
    }

    /**
     * Model with a quadratic expression that participates in the objective.
     * The expression contains a non‑zero quadratic factor and is marked as an objective
     * (by giving it a weight). The method must therefore return {@code true}.
     */
//     @Test
//     void testQuadraticObjectiveExpression() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
//         Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
//         Expression expr = model.addExpression("quadraticObj");
//         expr.weight(ONE);
//         expr.set(x, ONE);
//         expr.quadraticFactor(x, x, 2.0);
//         expr.quadraticFactor(x, y, 1.5);
//         assertTrue(model.isAnyExpressionQuadratic(), "A model with a quadratic objective expression must return true");
//     }

    @Test
    void testSingleExpressionZeroQuadraticButObjective() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockExpression(false, true, false, false));
        setExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Zero quadratic factor should make the predicate false even if it is an objective");
    }

    @Test
    void testSingleExpressionQuadraticNotObjectiveNorConstraint() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockExpression(true, false, false, false));
        setExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Quadratic expression that is neither objective nor constraint must not trigger true");
    }

    @Test
    void testSingleExpressionQuadraticObjective() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockExpression(true, true, false, false));
        setExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic objective expression must make the method return true");
    }

    @Test
    void testSingleExpressionQuadraticNonRedundantConstraint() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockExpression(true, false, true, false));
        setExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic non‑redundant constraint must make the method return true");
    }

    @Test
    void testSingleExpressionQuadraticRedundantConstraint() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockExpression(true, false, true, true));
        setExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "Quadratic redundant constraint must not satisfy the predicate");
    }

    @Test
    void testMultipleExpressionsAllNonQualifying() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        // linear objective
        map.put("e1", mockExpression(false, true, false, false));
        // linear constraint
        map.put("e2", mockExpression(false, false, true, false));
        // quadratic redundant constraint
        map.put("e3", mockExpression(true, false, true, true));
        setExpressionsMap(model, map);
        assertFalse(model.isAnyExpressionQuadratic(), "No expression satisfies the quadratic‑objective/constraint condition");
    }

    @Test
    void testMultipleExpressionsFirstQualifies() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        // quadratic objective
        map.put("e1", mockExpression(true, true, false, false));
        // linear constraint
        map.put("e2", mockExpression(false, false, true, false));
        // quadratic redundant constraint
        map.put("e3", mockExpression(true, false, true, true));
        setExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "First expression qualifies, method must return true irrespective of later entries");
    }

    @Test
    void testMultipleExpressionsSecondQualifies() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockExpression(false, false, false, false));
        // quadratic non‑redundant constraint
        map.put("e2", mockExpression(true, false, true, false));
        map.put("e3", mockExpression(true, false, true, true));
        setExpressionsMap(model, map);
        assertTrue(model.isAnyExpressionQuadratic(), "Second expression qualifies, method must return true");
    }

    @Test
    void testNullExpressionEntryThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("nullExpr", null);
        setExpressionsMap(model, map);
        assertThrows(NullPointerException.class, model::isAnyExpressionQuadratic, "A null entry in the map should cause NPE when accessed");
    }

    @Test
    void testInternalMapNullViaReflectionThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        setExpressionsMap(model, null);
        assertThrows(NullPointerException.class, model::isAnyExpressionQuadratic, "If the internal map itself is null, a NPE must be thrown");
    }

    @Test
    void testFaultyIsAnyQuadraticFactorNonZeroPropagatesException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockFaultyExpression("isAnyQuadraticFactorNonZero"));
        setExpressionsMap(model, map);
        assertThrows(IllegalStateException.class, model::isAnyExpressionQuadratic, "Exception from isAnyQuadraticFactorNonZero must propagate");
    }

    @Test
    void testFaultyIsObjectivePropagatesException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockFaultyExpression("isObjective"));
        setExpressionsMap(model, map);
        assertThrows(IllegalStateException.class, model::isAnyExpressionQuadratic, "Exception from isObjective must propagate");
    }

    @Test
    void testFaultyIsConstraintPropagatesException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockFaultyExpression("isConstraint"));
        setExpressionsMap(model, map);
        assertThrows(IllegalStateException.class, model::isAnyExpressionQuadratic, "Exception from isConstraint must propagate");
    }

    @Test
    void testFaultyIsRedundantPropagatesException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Map<String, Expression> map = new HashMap<>();
        map.put("e1", mockFaultyExpression("isRedundant"));
        setExpressionsMap(model, map);
        assertThrows(IllegalStateException.class, model::isAnyExpressionQuadratic, "Exception from isRedundant must propagate");
    }

//     @Test
//     void testConcurrentModificationDuringIteration() throws Exception {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // First stub expression that removes the second entry while being inspected
//         Expression first = new SimpleExpression(false, false, false, false) {
// 
//             @Override
//             public boolean isAnyQuadraticFactorNonZero() {
//                 try {
//                     Map<String, Expression> currentMap = getExpressionsMap(model);
//                     currentMap.remove("second");
//                 } catch (Exception e) {
//                     throw new RuntimeException(e);
//                 }
//                 return false;
//             }
//         };
//         // Second stub expression – never accessed because the first mutates the map
//         Expression second = mockExpression(false, false, false, false);
//         Map<String, Expression> map = new HashMap<>();
//         map.put("first", first);
//         map.put("second", second);
//         setExpressionsMap(model, map);
//         // The iteration inside the method should now throw ConcurrentModificationException
//         assertThrows(ConcurrentModificationException.class, model::isAnyExpressionQuadratic, "Modifying the map during iteration must cause CME");
//     }

    /**
     * Helper to obtain the current private {@code myExpressions} map via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Expression> getExpressionsMap(ExpressionsBasedModel model) throws Exception {
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        return (Map<String, Expression>) field.get(model);
    }
}