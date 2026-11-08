import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionsBasedModel_describe_Test {

    private ExpressionsBasedModel model;

    class BadVariableLocal extends Variable {

        BadVariableLocal(String name) {
            super(name);
        }

        @Override
        public boolean isLowerConstraint() {
            throw new RuntimeException("bad variable");
        }
    }

    class BadExpressionLocal extends Expression {

        BadExpressionLocal(String name) {
            super(name);
        }

        @Override
        public boolean isUpperConstraint() {
            throw new RuntimeException("bad expression");
        }
    }

    /**
     * A variable that deliberately throws from {@code isLowerConstraint()}
     */
    class BadVariableLocal extends Variable {

        BadVariableLocal(String name) {
            super(name);
        }

        @Override
        public boolean isLowerConstraint() {
            throw new RuntimeException("bad variable");
        }
    }

    /**
     * An expression that deliberately throws from {@code isUpperConstraint()}
     */
    class BadExpressionLocal extends Expression {

        BadExpressionLocal(String name) {
            super(name);
        }

        @Override
        public boolean isUpperConstraint() {
            throw new RuntimeException("bad expression");
        }
    }

    /**
     * A variable that deliberately throws from {@code isLowerConstraint()}
     */
    class BadVariable extends Variable {

        BadVariable(String name) {
            super(name);
        }

        @Override
        public boolean isLowerConstraint() {
            throw new RuntimeException("bad variable");
        }
    }

    /**
     * An expression that deliberately throws from {@code isUpperConstraint()}
     */
    class BadExpression extends Expression {

        BadExpression(String name) {
            super(name);
        }

        @Override
        public boolean isUpperConstraint() {
            throw new RuntimeException("bad expression");
        }
    }

    class BadVariableLocal extends Variable {

        BadVariableLocal(String name) {
            super(name);
        }

        @Override
        public boolean isLowerConstraint() {
            throw new RuntimeException("bad variable");
        }
    }

    class BadExpressionLocal extends Expression {

        BadExpressionLocal(String name) {
            super(name);
        }

        @Override
        public boolean isUpperConstraint() {
            throw new RuntimeException("bad expression");
        }
    }

    /**
     * An expression that deliberately throws from {@code isUpperConstraint()}
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Verify that a freshly created model reports zero counts.
     */
    @Test
    void testDescribeEmptyModel() {
        ExpressionsBasedModel.Description description = model.describe();
        assertEquals(0, description.nbVariables);
        assertEquals(0, description.nbPositiveVariables);
        assertEquals(0, description.nbNegativeVariables);
        assertEquals(0, description.nbIntegerVariables);
        assertEquals(0, description.nbLoBound);
        assertEquals(0, description.nbUpBound);
        assertEquals(0, description.nbEqBound);
        assertEquals(0, description.nbLoCnstr);
        assertEquals(0, description.nbUpCnstr);
        assertEquals(0, description.nbEqCnstr);
    }

    /**
     * Build a model with a mixture of variable types and constraints and verify that
     * {@code describe()} returns the expected counts.
     */
    @Test
    void testDescribeMixedModel() {
        // ----- variables -------------------------------------------------------
        // x1 : lower 0, upper 10  -> free, positive
        Variable x1 = model.newVariable("x1").lower(0).upper(10);
        // x2 : lower -5, upper 5 -> free, positive & negative
        Variable x2 = model.newVariable("x2").lower(-5).upper(5);
        // x3 : fixed at 0 (lower == upper) -> not free, equality bound
        Variable x3 = model.newVariable("x3").level(0);
        // x4 : integer, lower 0, upper 10 -> free, positive, integer
        Variable x4 = model.newVariable("x4").integer(true).lower(0).upper(10);
        // ----- expressions (constraints) ---------------------------------------
        // c1 : lower constraint only
        model.newExpression("c1").lower(5);
        // c2 : upper constraint only
        model.newExpression("c2").upper(15);
        // c3 : equality constraint (level)
        model.newExpression("c3").level(7);
        // ----- description ------------------------------------------------------
        ExpressionsBasedModel.Description d = model.describe();
        // Expected counts
        int expectedTotalVars = 4;
        // x1, x2, x4
        int expectedPositiveVars = 3;
        // x2
        int expectedNegativeVars = 1;
        // x4
        int expectedIntegerVars = 1;
        // all have a lower bound (x3's lower == upper)
        int expectedLoBoundVars = 4;
        // all have an upper bound
        int expectedUpBoundVars = 4;
        // x3
        int expectedEqBoundVars = 1;
        // c1
        int expectedLoCnstr = 1;
        // c2
        int expectedUpCnstr = 1;
        // c3
        int expectedEqCnstr = 1;
        // ----- assertions -------------------------------------------------------
        assertEquals(expectedTotalVars, d.nbVariables, "total variables");
        assertEquals(expectedPositiveVars, d.nbPositiveVariables, "positive variables");
        assertEquals(expectedNegativeVars, d.nbNegativeVariables, "negative variables");
        assertEquals(expectedIntegerVars, d.nbIntegerVariables, "integer variables");
        assertEquals(expectedLoBoundVars, d.nbLoBound, "variables with lower bound");
        assertEquals(expectedUpBoundVars, d.nbUpBound, "variables with upper bound");
        assertEquals(expectedEqBoundVars, d.nbEqBound, "variables with equality bound");
        assertEquals(expectedLoCnstr, d.nbLoCnstr, "expressions with lower constraint");
        assertEquals(expectedUpCnstr, d.nbUpCnstr, "expressions with upper constraint");
        assertEquals(expectedEqCnstr, d.nbEqCnstr, "expressions with equality constraint");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeVariableLowerBoundOnly() {
        // x >= 0  (lower bound only)
        model.newVariable("x").lower(BigDecimal.ZERO);
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(1, d.nbVariables);
        // no upper limit → treated as positive
        assertEquals(1, d.nbPositiveVariables);
        assertEquals(0, d.nbNegativeVariables);
        assertEquals(0, d.nbIntegerVariables);
        assertEquals(1, d.nbLoBound);
        assertEquals(0, d.nbUpBound);
        assertEquals(0, d.nbEqBound);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeVariableUpperBoundOnly() {
        // x <= 10  (upper bound only)
        model.newVariable("x").upper(BigDecimal.TEN);
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(1, d.nbVariables);
        // upper > 0 → positive
        assertEquals(1, d.nbPositiveVariables);
        // no lower limit → counted as negative
        assertEquals(1, d.nbNegativeVariables);
        assertEquals(0, d.nbIntegerVariables);
        assertEquals(0, d.nbLoBound);
        assertEquals(1, d.nbUpBound);
        assertEquals(0, d.nbEqBound);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeVariableEqualityBound() {
        // x = 5  (lower == upper → fixed)
        model.newVariable("x").level(BigDecimal.valueOf(5));
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(1, d.nbVariables);
        // Fixed variables are not part of the free‑variable categorisation
        assertEquals(0, d.nbPositiveVariables);
        assertEquals(0, d.nbNegativeVariables);
        assertEquals(0, d.nbIntegerVariables);
        assertEquals(0, d.nbLoBound);
        assertEquals(0, d.nbUpBound);
        assertEquals(1, d.nbEqBound);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeIntegerVariableNoBounds() {
        // integer variable, no explicit bounds
        model.newVariable("i").integer(true);
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(1, d.nbVariables);
        // No bounds → both positive (no upper) and negative (no lower) by categorisation
        assertEquals(1, d.nbPositiveVariables);
        assertEquals(1, d.nbNegativeVariables);
        assertEquals(1, d.nbIntegerVariables);
        assertEquals(0, d.nbLoBound);
        assertEquals(0, d.nbUpBound);
        assertEquals(0, d.nbEqBound);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeMixedVariables() {
        // 1) free positive with lower & upper bound
        model.newVariable("v1").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // 2) free negative & positive (lower negative, upper positive)
        model.newVariable("v2").lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5));
        // 3) integer positive with lower bound only
        model.newVariable("v3").integer(true).lower(BigDecimal.ZERO);
        // 4) integer negative with upper bound only (upper negative)
        model.newVariable("v4").integer(true).upper(BigDecimal.valueOf(-3));
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(4, d.nbVariables);
        // Positive: v1, v2, v3  (v4 has upper < 0 → not positive)
        assertEquals(3, d.nbPositiveVariables);
        // Negative: v2, v4 (v1 lower = 0 → not negative, v3 lower = 0 → not negative)
        assertEquals(2, d.nbNegativeVariables);
        assertEquals(2, d.nbIntegerVariables);
        // Lower‑bound variables: v1, v2, v3
        assertEquals(3, d.nbLoBound);
        // Upper‑bound variables: v1, v2, v4
        assertEquals(3, d.nbUpBound);
        assertEquals(0, d.nbEqBound);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeExpressionConstraintsOnly() {
        model.newExpression("cLo").lower(BigDecimal.ONE);
        model.newExpression("cUp").upper(BigDecimal.TEN);
        model.newExpression("cEq").level(BigDecimal.valueOf(7));
        ExpressionsBasedModel.Description d = model.describe();
        assertEquals(0, d.nbVariables);
        assertEquals(0, d.nbPositiveVariables);
        assertEquals(0, d.nbNegativeVariables);
        assertEquals(0, d.nbIntegerVariables);
        assertEquals(0, d.nbLoBound);
        assertEquals(0, d.nbUpBound);
        assertEquals(0, d.nbEqBound);
        assertEquals(1, d.nbLoCnstr);
        assertEquals(1, d.nbUpCnstr);
        assertEquals(1, d.nbEqCnstr);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeNullVariableInList() throws Exception {
        // Insert a null into the private myVariables list via reflection
        Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) varField.get(model);
        vars.add(null);
        assertThrows(NullPointerException.class, () -> model.describe(), "describe() should throw NPE when encountering a null Variable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeNullExpressionInMap() throws Exception {
        // Insert a null Expression into the private myExpressions map via reflection
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprField.get(model);
        exprMap.put("nullExpr", null);
        assertThrows(NullPointerException.class, () -> model.describe(), "describe() should throw NPE when encountering a null Expression");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeVariableThrowsRuntimeException() throws Exception {
        // Create a subclass that throws from isLowerConstraint()
        class BadVariableLocal extends Variable {

            BadVariableLocal(String name) {
                super(name);
            }

            @Override
            public boolean isLowerConstraint() {
                throw new RuntimeException("bad variable");
            }
        }
        Field varField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) varField.get(model);
        vars.add(new BadVariableLocal("bad"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.describe(), "RuntimeException from Variable should propagate");
        assertTrue(ex.getMessage().contains("bad variable"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDescribeExpressionThrowsRuntimeException() throws Exception {
        // Subclass of Expression that throws from isUpperConstraint()
        class BadExpressionLocal extends Expression {

            BadExpressionLocal(String name) {
                super(name);
            }

            @Override
            public boolean isUpperConstraint() {
                throw new RuntimeException("bad expression");
            }
        }
        Field exprField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprField.get(model);
        exprMap.put("bad", new BadExpressionLocal("bad"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.describe(), "RuntimeException from Expression should propagate");
        assertTrue(ex.getMessage().contains("bad expression"));
    }
}
