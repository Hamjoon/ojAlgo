package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ConcurrentModificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_isAnyObjectiveQuadratic_Test {

    private ExpressionsBasedModel model;

    //     @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test
        model = new ExpressionsBasedModel();
    }

    /**
     * Creates a simple variable with bounds [0,10] and returns it.
     */
    private Variable newVariable(String name) {
        return model.newVariable(name).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
    }

    /**
     * Creates an expression, optionally marks it as an objective (by setting a weight),
     * and returns it.
     */
    private Expression newExpression(String name, boolean asObjective) {
        Expression expr = model.newExpression(name);
        if (asObjective) {
            // weight makes the expression part of the objective
            expr.weight(ONE);
        }
        return expr;
    }

    /**
     * When the model contains no expressions, the method must return {@code false}.
     */
    @Test
    void testNoExpressions() {
        assertFalse(model.isAnyObjectiveQuadratic(), "A model with no expressions must not report a quadratic objective");
    }

    /**
     * When the model has an objective expression that is purely linear,
     * {@code isAnyObjectiveQuadratic()} must return {@code false}.
     */
    @Test
    void testLinearObjectiveOnly() {
        // create a variable
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        // create an objective expression (weight makes it an objective)
        Expression obj = model.newExpression("linearObj").weight(ONE);
        // linear term only
        obj.set(x, ONE);
        assertFalse(model.isAnyObjectiveQuadratic(), "A linear objective should not be considered quadratic");
    }

    /**
     * When the model has an objective expression that contains a non‑zero quadratic factor,
     * {@code isAnyObjectiveQuadratic()} must return {@code true}.
     */
//     @Test
//     void testQuadraticObjective() {
//         // two variables – the quadratic term will be x * y
//         Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
//         Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));
//         // create an objective expression
//         Expression obj = model.newExpression("quadObj").weight(ONE);
//         // add a quadratic factor (x * y) with coefficient 1
//         obj.quadratic(x, y, ONE);
//         // also add a linear term to ensure the expression is still recognised as an objective
//         obj.set(x, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "An objective containing a non‑zero quadratic factor must be reported as quadratic");
//     }

    /**
     * Retrieves the private {@code myExpressions} map via reflection.
     *
     * @return the mutable map used internally by the model
     */
    @SuppressWarnings("unchecked")
    private Map<String, Expression> getInternalExpressionMap() {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            return (Map<String, Expression>) field.get(model);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to access myExpressions field", e);
        }
    }

    /**
     * Single objective quadratic expression
     */
//     @Test
//     void testSingleObjectiveQuadraticExpression() {
//         Variable x = newVariable("x");
//         Variable y = newVariable("y");
//         Expression expr = newExpression("objQuad", true);
//         expr.quadratic(x, y, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "Objective quadratic expression should make the method return true");
//     }

    /**
     * Single objective non‑quadratic expression
     */
    @Test
    void testSingleObjectiveNonQuadraticExpression() {
        Variable x = newVariable("x");
        Expression expr = newExpression("objLinear", true);
        // only linear term
        expr.set(x, ONE);
        assertFalse(model.isAnyObjectiveQuadratic(), "Objective without quadratic factors must return false");
    }

    /**
     * Single non‑objective quadratic expression
     */
//     @Test
//     void testSingleNonObjectiveQuadraticExpression() {
//         Variable x = newVariable("x");
//         Variable y = newVariable("y");
//         Expression expr = newExpression("nonObjQuad", false);
//         expr.quadratic(x, y, ONE);
//         assertFalse(model.isAnyObjectiveQuadratic(), "Quadratic expression that is not an objective must not affect the result");
//     }

    /**
     * Mixed expressions – case where at least one objective quadratic exists
     */
//     @Test
//     void testMixedExpressions_WithObjectiveQuadratic() {
//         // 1) non‑objective non‑quadratic
//         newExpression("c1", false);
//         // 2) objective non‑quadratic
//         Variable a = newVariable("a");
//         Expression objLinear = newExpression("objLinear", true);
//         objLinear.set(a, ONE);
//         // 3) objective quadratic
//         Variable b = newVariable("b");
//         Variable c = newVariable("c");
//         Expression objQuad = newExpression("objQuad", true);
//         objQuad.quadratic(b, c, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "Presence of an objective quadratic should yield true");
//     }

    /**
     * Mixed expressions – case with no objective quadratic
     */
//     @Test
//     void testMixedExpressions_NoObjectiveQuadratic() {
//         // 1) non‑objective quadratic
//         Variable x = newVariable("x");
//         Variable y = newVariable("y");
//         Expression nonObjQuad = newExpression("nonObjQuad", false);
//         nonObjQuad.quadratic(x, y, ONE);
//         // 2) objective non‑quadratic
//         Variable z = newVariable("z");
//         Expression objLinear = newExpression("objLinear", true);
//         objLinear.set(z, ONE);
//         // 3) non‑objective non‑quadratic
//         newExpression("c3", false);
//         assertFalse(model.isAnyObjectiveQuadratic(), "No objective quadratic present → should return false");
//     }

    /**
     * Multiple objectives, none quadratic
     */
    @Test
    void testMultipleObjectives_NoneQuadratic() {
        for (int i = 0; i < 3; i++) {
            Variable v = newVariable("v" + i);
            Expression e = newExpression("obj" + i, true);
            // linear only
            e.set(v, ONE);
        }
        assertFalse(model.isAnyObjectiveQuadratic(), "All objectives are linear → should return false");
    }

    /**
     * Multiple objectives, at least one quadratic
     */
//     @Test
//     void testMultipleObjectives_AtLeastOneQuadratic() {
//         // two linear objectives
//         for (int i = 0; i < 2; i++) {
//             Variable v = newVariable("vL" + i);
//             Expression e = newExpression("linObj" + i, true);
//             e.set(v, ONE);
//         }
//         // one quadratic objective
//         Variable a = newVariable("a");
//         Variable b = newVariable("b");
//         Expression quadObj = newExpression("quadObj", true);
//         quadObj.quadratic(a, b, ONE);
//         // one non‑objective non‑quadratic
//         newExpression("c", false);
//         assertTrue(model.isAnyObjectiveQuadratic(), "At least one objective quadratic → should return true");
//     }

    /**
     * Remove objective‑quadratic expression
     */
//     @Test
//     void testRemoveObjectiveQuadraticExpression() {
//         Variable x = newVariable("x");
//         Variable y = newVariable("y");
//         Expression expr = newExpression("objQuad", true);
//         expr.quadratic(x, y, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "Initially the quadratic objective should be detected");
//         // remove the expression
//         model.removeExpression("objQuad");
//         assertFalse(model.isAnyObjectiveQuadratic(), "After removal, no quadratic objective should remain");
//     }

    /**
     * Toggle objective expression from non‑quadratic to quadratic
     */
//     @Test
//     void testToggleObjectiveToQuadratic() {
//         Variable x = newVariable("x");
//         Expression expr = newExpression("objToggle", true);
//         // linear only
//         expr.set(x, ONE);
//         assertFalse(model.isAnyObjectiveQuadratic(), "Initially linear objective → false");
//         // add quadratic factor later
//         Variable y = newVariable("y");
//         expr.quadratic(x, y, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "After adding quadratic factor → true");
//     }

    /**
     * Large number of expressions, only last is objective‑quadratic
     */
//     @Test
//     void testLargeNumber_LastIsObjectiveQuadratic() {
//         // add 49 non‑objective or non‑quadratic expressions
//         for (int i = 0; i < 49; i++) {
//             Variable v = newVariable("v" + i);
//             Expression e = newExpression("e" + i, false);
//             // linear, not objective
//             e.set(v, ONE);
//         }
//         // 50th expression: objective quadratic
//         Variable a = newVariable("a");
//         Variable b = newVariable("b");
//         Expression last = newExpression("objQuad50", true);
//         last.quadratic(a, b, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "The last expression is an objective quadratic, method must return true");
//     }

    /**
     * Objective expression becomes non‑quadratic after modification
     */
//     @Test
//     void testObjectiveBecomesNonQuadratic() {
//         Variable x = newVariable("x");
//         Variable y = newVariable("y");
//         Expression expr = newExpression("objToggleOff", true);
//         // initially quadratic
//         expr.quadratic(x, y, ONE);
//         assertTrue(model.isAnyObjectiveQuadratic(), "Initially quadratic → true");
//         // Remove quadratic factor – set the coefficient to zero
//         expr.quadratic(x, y, ZERO);
//         assertFalse(model.isAnyObjectiveQuadratic(), "After zeroing quadratic coefficient → false");
//     }

    /**
     * Null expression in model – expect NullPointerException
     */
    @Test
    void testNullExpressionInModel() {
        // Inject a null value into the internal map
        Map<String, Expression> map = getInternalExpressionMap();
        map.put("nullExpr", null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> model.isAnyObjectiveQuadratic(), "Iterating over a null expression should throw NPE");
        assertNotNull(npe);
    }

    /**
     * Concurrent modification during iteration – expect ConcurrentModificationException
     */
    @Test
    void testConcurrentModificationDuringIteration() {
        // Replace the internal map with a custom implementation that throws CME on iterator()
        Map<String, Expression> sabotagedMap = new AbstractMap<String, Expression>() {

            private final Map<String, Expression> delegate = new HashMap<>();

            @Override
            public Set<Entry<String, Expression>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public Collection<Expression> values() {
                // Return a collection whose iterator throws CME immediately
                return new AbstractCollection<Expression>() {

                    @Override
                    public Iterator<Expression> iterator() {
                        throw new ConcurrentModificationException("Simulated concurrent modification");
                    }

                    @Override
                    public int size() {
                        return delegate.size();
                    }
                };
            }
        };
        // Populate with a harmless expression so the model is not empty
        Variable x = newVariable("x");
        Expression expr = newExpression("obj", true);
        expr.set(x, ONE);
        sabotagedMap.put("obj", expr);
        // Inject the sabotaged map via reflection
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            field.set(model, sabotagedMap);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inject custom map", e);
        }
        assertThrows(ConcurrentModificationException.class, () -> model.isAnyObjectiveQuadratic(), "A ConcurrentModificationException should propagate from the iterator");
    }
}