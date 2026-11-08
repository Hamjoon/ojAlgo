package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_describe_Test {

    /**
     * An empty model must report zero for all counters.
     */
    @Test
    void testDescribeEmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Description description = model.describe();
        assertEquals(0, description.nbVariables, "total variables");
        assertEquals(0, description.nbPositiveVariables, "positive variables");
        assertEquals(0, description.nbNegativeVariables, "negative variables");
        assertEquals(0, description.nbIntegerVariables, "integer variables");
        assertEquals(0, description.nbLoBound, "lower‑bound variables");
        assertEquals(0, description.nbUpBound, "upper‑bound variables");
        assertEquals(0, description.nbEqBound, "equality‑bound variables");
        assertEquals(0, description.nbLoCnstr, "lower‑bound constraints");
        assertEquals(0, description.nbUpCnstr, "upper‑bound constraints");
        assertEquals(0, description.nbEqCnstr, "equality‑bound constraints");
    }

    /**
     * Build a model that contains a mixture of variables and constraints and verify that the
     * description reflects the expected numbers.
     *
     * <ul>
     *   <li>Four variables:
     *     <ul>
     *       <li>vFree – no limits (counts as free, positive, negative)</li>
     *       <li>vUpper – only an upper limit (counts as free, positive, negative)</li>
     *       <li>vLower – only a lower limit (counts as free, positive, negative)</li>
     *       <li>vInt   – integer flag set (counts as integer as well)</li>
     *     </ul>
     *   </li>
     *   <li>Three expressions:
     *     <ul>
     *       <li>eLo – lower‑bounded only</li>
     *       <li>eUp – upper‑bounded only</li>
     *       <li>eEq – equality (both lower and upper set to the same value)</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    @Test
    void testDescribeWithVariablesAndExpressions() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // ----- variables -------------------------------------------------------
        // free variable – no limits
        Variable vFree = model.addVariable();
        // only upper limit
        Variable vUpper = model.addVariable().upper(BigDecimal.valueOf(10));
        // only lower limit
        Variable vLower = model.addVariable().lower(BigDecimal.valueOf(-5));
        // integer variable (no limits)
        Variable vInt = model.addVariable();
        vInt.integer(true);
        // ----- expressions (constraints) --------------------------------------
        model.addExpression("eLo").lower(BigDecimal.ONE);
        model.addExpression("eUp").upper(BigDecimal.TEN);
        model.addExpression("eEq").level(BigDecimal.valueOf(3));
        Description description = model.describe();
        // ----- variable related counters ---------------------------------------
        assertEquals(4, description.nbVariables, "total variables");
        assertEquals(4, description.nbPositiveVariables, "positive variables");
        assertEquals(4, description.nbNegativeVariables, "negative variables");
        assertEquals(1, description.nbIntegerVariables, "integer variables");
        // ----- variable bound counters -----------------------------------------
        assertEquals(1, description.nbLoBound, "variables with a lower bound");
        assertEquals(1, description.nbUpBound, "variables with an upper bound");
        assertEquals(0, description.nbEqBound, "variables with an equality bound");
        // ----- expression (constraint) related counters -----------------------
        assertEquals(1, description.nbLoCnstr, "expressions with a lower bound");
        assertEquals(1, description.nbUpCnstr, "expressions with an upper bound");
        assertEquals(1, description.nbEqCnstr, "expressions with an equality bound");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeVariablesDiverseCategories() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a) fixed variable – excluded from all categorisations
        Variable vFixed = model.addVariable();
        vFixed.fix(BigDecimal.ZERO);
        // b) free variable – no bounds, contributes to positive & negative
        model.addVariable();
        // c) another free variable – no bounds
        model.addVariable();
        // d) integer variable with an equality bound (level)
        Variable vIntEq = model.addVariable();
        vIntEq.integer(true);
        vIntEq.level(BigDecimal.valueOf(5));
        Description d = model.describe();
        // ----- variable counters ------------------------------------------------
        assertEquals(4, d.nbVariables, "total variables");
        assertEquals(3, d.nbPositiveVariables, "positive variables (free1, free2, intEq)");
        assertEquals(2, d.nbNegativeVariables, "negative variables (free1, free2)");
        assertEquals(1, d.nbIntegerVariables, "integer variables (intEq)");
        // ----- variable bound counters -----------------------------------------
        assertEquals(1, d.nbLoBound, "variables with a lower bound (intEq)");
        assertEquals(1, d.nbUpBound, "variables with an upper bound (intEq)");
        assertEquals(1, d.nbEqBound, "variables with an equality bound (intEq)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeExpressionsDiverseConstraints() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addExpression("eLo").lower(BigDecimal.ONE);
        model.addExpression("eUp").upper(BigDecimal.TEN);
        model.addExpression("eEq").level(BigDecimal.valueOf(3));
        Description d = model.describe();
        assertEquals(0, d.nbVariables, "no variables");
        assertEquals(0, d.nbPositiveVariables, "no positive variables");
        assertEquals(0, d.nbNegativeVariables, "no negative variables");
        assertEquals(0, d.nbIntegerVariables, "no integer variables");
        assertEquals(0, d.nbLoBound, "no variable lower bounds");
        assertEquals(0, d.nbUpBound, "no variable upper bounds");
        assertEquals(0, d.nbEqBound, "no variable equality bounds");
        assertEquals(1, d.nbLoCnstr, "one lower‑bounded expression");
        assertEquals(1, d.nbUpCnstr, "one upper‑bounded expression");
        assertEquals(1, d.nbEqCnstr, "one equality‑bounded expression");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeCombinedVariablesAndExpressions() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // ---- variables (same setup as test 2) ---------------------------------
        Variable vFixed = model.addVariable();
        vFixed.fix(BigDecimal.ZERO);
        // vFree1
        model.addVariable();
        // vFree2
        model.addVariable();
        Variable vIntEq = model.addVariable();
        vIntEq.integer(true);
        vIntEq.level(BigDecimal.valueOf(5));
        // ---- expressions ------------------------------------------------------
        model.addExpression("eLo").lower(BigDecimal.ONE);
        model.addExpression("eUp").upper(BigDecimal.TEN);
        model.addExpression("eEq").level(BigDecimal.valueOf(3));
        Description d = model.describe();
        // ----- variable part (same expectations as test 2) --------------------
        assertEquals(4, d.nbVariables, "total variables");
        assertEquals(3, d.nbPositiveVariables, "positive variables");
        assertEquals(2, d.nbNegativeVariables, "negative variables");
        assertEquals(1, d.nbIntegerVariables, "integer variables");
        assertEquals(1, d.nbLoBound, "variables with a lower bound");
        assertEquals(1, d.nbUpBound, "variables with an upper bound");
        assertEquals(1, d.nbEqBound, "variables with an equality bound");
        // ----- expression part -------------------------------------------------
        assertEquals(1, d.nbLoCnstr, "one lower‑bounded expression");
        assertEquals(1, d.nbUpCnstr, "one upper‑bounded expression");
        assertEquals(1, d.nbEqCnstr, "one equality‑bounded expression");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeFixedVariableExclusion() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Fixed variable – no bounds, will be ignored in categorisation
        Variable vFixed = model.addVariable();
        vFixed.fix(BigDecimal.ZERO);
        // Free variable with a *positive* lower bound (so it is not counted as negative)
        model.addVariable().lower(BigDecimal.valueOf(7));
        Description d = model.describe();
        assertEquals(2, d.nbVariables, "total variables");
        assertEquals(1, d.nbPositiveVariables, "only the free variable is positive");
        assertEquals(0, d.nbNegativeVariables, "free variable has a positive lower bound → not negative");
        assertEquals(0, d.nbIntegerVariables, "no integer variables");
        assertEquals(1, d.nbLoBound, "one variable with a lower bound");
        assertEquals(0, d.nbUpBound, "no upper‑bound variables");
        assertEquals(0, d.nbEqBound, "no equality‑bound variables");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeVariableWithBothBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Single free variable with a negative lower limit and a positive upper limit
        model.addVariable().lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(10));
        Description d = model.describe();
        assertEquals(1, d.nbVariables, "one variable");
        assertEquals(1, d.nbPositiveVariables, "upper limit > 0 → positive");
        assertEquals(1, d.nbNegativeVariables, "lower limit < 0 → negative");
        assertEquals(0, d.nbIntegerVariables, "no integer flag");
        assertEquals(1, d.nbLoBound, "lower‑bound present");
        assertEquals(1, d.nbUpBound, "upper‑bound present");
        assertEquals(0, d.nbEqBound, "no equality bound");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeExpressionWithAllConstraints() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // One expression that defines lower, upper and equality simultaneously
        model.addExpression("eAll").lower(BigDecimal.ONE).upper(BigDecimal.TEN).level(// level sets both bounds to 5 → equality
        BigDecimal.valueOf(5));
        Description d = model.describe();
        assertEquals(0, d.nbVariables, "no variables");
        assertEquals(1, d.nbLoCnstr, "lower‑bound counted");
        assertEquals(1, d.nbUpCnstr, "upper‑bound counted");
        assertEquals(1, d.nbEqCnstr, "equality‑bound counted");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeMixedExpressionConstraints() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addExpression("eLo").lower(BigDecimal.ONE);
        model.addExpression("eUp").upper(BigDecimal.TEN);
        model.addExpression("eEq").level(BigDecimal.valueOf(3));
        model.addExpression("eBoth").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(8));
        Description d = model.describe();
        assertEquals(0, d.nbVariables, "no variables");
        assertEquals(2, d.nbLoCnstr, "lower‑only + both");
        assertEquals(2, d.nbUpCnstr, "upper‑only + both");
        assertEquals(1, d.nbEqCnstr, "equality‑only");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeMultipleIntegerVariablesNoBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        for (int i = 0; i < 3; i++) {
            Variable v = model.addVariable();
            v.integer(true);
        }
        Description d = model.describe();
        assertEquals(3, d.nbVariables, "three variables");
        assertEquals(3, d.nbPositiveVariables, "all positive (no upper limit)");
        assertEquals(3, d.nbNegativeVariables, "all negative (no lower limit)");
        assertEquals(3, d.nbIntegerVariables, "all integer");
        assertEquals(0, d.nbLoBound, "no lower bounds");
        assertEquals(0, d.nbUpBound, "no upper bounds");
        assertEquals(0, d.nbEqBound, "no equality bounds");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeNullVariableInList() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add a legitimate variable first
        model.addVariable();
        // Inject a null entry into the internal myVariables list via reflection
        Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.ArrayList<Variable> vars = (java.util.ArrayList<Variable>) varField.get(model);
        vars.add(null);
        assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when encountering a null variable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeNullExpressionInMap() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Normal expression to ensure the map exists
        model.addExpression("valid").lower(BigDecimal.ONE);
        // Insert a null expression value via reflection
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) exprField.get(model);
        map.put("nullExpr", null);
        assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when encountering a null expression");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeNullVariablesCollection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Set the private myVariables field to null via reflection
        Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varField.setAccessible(true);
        varField.set(model, null);
        assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when myVariables is null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeNullExpressionsCollection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Set the private myExpressions field to null via reflection
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprField.setAccessible(true);
        exprField.set(model, null);
        assertThrows(NullPointerException.class, model::describe, "describe() should throw NPE when myExpressions is null");
    }
}
