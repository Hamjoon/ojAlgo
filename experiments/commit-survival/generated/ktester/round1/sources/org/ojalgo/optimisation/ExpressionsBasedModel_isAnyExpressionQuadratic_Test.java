package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyExpressionQuadratic_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    /**
     * Helper DTO describing a single expression for the parameterised tests.
     */
    private static final class ExprDesc {

        final String name;

        final boolean quadraticFactorNonZero;

        final boolean objective;

        final boolean constraint;

        // only relevant when {@code constraint} is true
        final boolean redundant;

        ExprDesc(String name, boolean quadraticFactorNonZero, boolean objective, boolean constraint, boolean redundant) {
            this.name = name;
            this.quadraticFactorNonZero = quadraticFactorNonZero;
            this.objective = objective;
            this.constraint = constraint;
            this.redundant = redundant;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for every test
        model = new ExpressionsBasedModel();
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ZERO);
        y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ZERO);
    }

    /**
     * Builds a model according to a list of {@link ExprDesc}.
     */
    private void populateModel(List<ExprDesc> descs) {
        for (ExprDesc d : descs) {
            Expression e = model.newExpression(d.name);
            // quadratic part
            if (d.quadraticFactorNonZero) {
                // use x·x for simplicity; any non‑zero factor is sufficient
                e.setQuadraticFactor(x, x, ONE);
            }
            // objective flag
            if (d.objective) {
                e.weight(ONE);
            }
            // constraint flag
            if (d.constraint) {
                if (d.redundant) {
                    // both bounds zero → redundant according to ojAlgo's definition
                    e.lower(BigDecimal.ZERO);
                    e.upper(BigDecimal.ZERO);
                } else {
                    // a non‑trivial bound makes the constraint non‑redundant
                    e.lower(BigDecimal.ZERO);
                    e.upper(BigDecimal.valueOf(5));
                }
            }
        }
    }

    /**
     * Data for the “Branch Coverage” group.
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// 1) Model contains no expressions → false
        Arguments.of(Collections.emptyList(), false, "Model contains no expressions"), // 2) Single non‑quadratic expression → false
        Arguments.of(List.of(new ExprDesc("e1", false, false, false, false)), false, "Non‑quadratic expression"), // 3) Quadratic but neither objective nor constraint → false
        Arguments.of(List.of(new ExprDesc("e2", true, false, false, false)), false, "Quadratic but not objective/constraint"), // 4) Quadratic objective → true
        Arguments.of(List.of(new ExprDesc("e3", true, true, false, false)), true, "Quadratic objective"), // 5) Quadratic non‑redundant constraint → true
        Arguments.of(List.of(new ExprDesc("e4", true, false, true, false)), true, "Quadratic non‑redundant constraint"), // 6) Quadratic redundant constraint → false
        Arguments.of(List.of(new ExprDesc("e5", true, false, true, true)), false, "Quadratic redundant constraint"), // 7) Mixed list, one qualifying (e8) → true
        Arguments.of(List.of(new ExprDesc("e6", false, false, true, false), new ExprDesc("e7", true, false, true, true), new ExprDesc("e8", true, true, false, false)), true, "Mixed list with quadratic objective"), // 8) Mixed list, none qualifying → false
        Arguments.of(List.of(new ExprDesc("e9", false, false, false, false), new ExprDesc("e10", true, false, true, true), new ExprDesc("e11", true, false, true, true)), false, "Mixed list, all non‑qualifying"));
    }

    /**
     * Data for the “Additional Cases” group.
     */
    private static Stream<Arguments> additionalCasesProvider() {
        return Stream.of(// 9) Expression both objective and constraint (redundant flag ignored) → true
        Arguments.of(List.of(new ExprDesc("e12", true, true, true, true)), true, "Quadratic expression that is both objective and constraint"), // 10) Objective but linear → false
        Arguments.of(List.of(new ExprDesc("e13", false, true, false, false)), false, "Objective without quadratic factor"), // 11) Three expressions, third is quadratic objective → true
        Arguments.of(List.of(new ExprDesc("e14", false, false, true, false), new ExprDesc("e15", true, false, true, true), new ExprDesc("e16", true, true, false, false)), true, "Third expression qualifies as quadratic objective"), // 12) First expression is qualifying non‑redundant constraint → true
        Arguments.of(List.of(new ExprDesc("e17", true, false, true, false), new ExprDesc("e18", false, false, false, false), new ExprDesc("e19", true, false, true, true)), true, "First expression qualifies"), // 13) All expressions non‑qualifying → false
        Arguments.of(List.of(new ExprDesc("e20", false, false, true, false), new ExprDesc("e21", false, true, false, false), new ExprDesc("e22", true, false, true, true)), false, "All expressions fail to satisfy condition"));
    }

    /**
     * No expressions have been added – the method must return {@code false}.
     */
    @Test
    void testNoExpressions() {
        assertFalse(model.isAnyExpressionQuadratic(), "A model with no expressions cannot be quadratic");
    }

    /**
     * A model that only contains linear expressions must return {@code false}.
     */
    @Test
    void testLinearExpressionOnly() {
        Expression linear = model.newExpression("linear");
        // linear term 1·x
        linear.set(x, ONE);
        // linear term 1·y
        linear.set(y, ONE);
        // make it an objective (weight ≠ 0) – still linear
        linear.weight(ONE);
        assertFalse(model.isAnyExpressionQuadratic(), "Linear expressions should not be considered quadratic");
    }

    /**
     * A quadratic expression that is part of the objective must make the method return {@code true}.
     * The quadratic term is created via {@code setQuadraticFactor}.
     */
    @Test
    void testQuadraticObjective() {
        Expression quadObj = model.newExpression("quadObj");
        // x² term (coefficient 1)
        quadObj.setQuadraticFactor(x, x, ONE);
        // make it an objective expression
        quadObj.weight(ONE);
        assertTrue(model.isAnyExpressionQuadratic(), "A quadratic objective expression should be detected");
    }

    /**
     * A quadratic expression that is a non‑redundant constraint must also make the method return {@code true}.
     */
    @Test
    void testQuadraticConstraint() {
        Expression quadConstr = model.newExpression("quadConstr");
        // x·y term (coefficient 1)
        quadConstr.setQuadraticFactor(x, y, ONE);
        // Turn it into a constraint (lower/upper bounds different from the default)
        quadConstr.lower(BigDecimal.ZERO);
        quadConstr.upper(BigDecimal.valueOf(5));
        // No weight – therefore not an objective, but it is a constraint
        assertTrue(model.isAnyExpressionQuadratic(), "A quadratic non‑redundant constraint should be detected");
    }

    /**
     * Ensure that a quadratic expression which is redundant does **not** trigger the method.
     * In ojAlgo a constraint is considered redundant when its lower bound ≤ 0 ≤ upper bound
     * and the expression evaluates to zero for all feasible points. By setting both bounds
     * to zero we obtain a redundant constraint.
     */
    @Test
    void testQuadraticRedundantConstraintIsIgnored() {
        Expression redundant = model.newExpression("redundant");
        redundant.setQuadraticFactor(x, x, ONE);
        // Both bounds are zero → the solver will treat the constraint as “expression == 0”.
        // Because the expression is not forced to be non‑zero, it is considered redundant.
        redundant.lower(BigDecimal.ZERO);
        redundant.upper(BigDecimal.ZERO);
        // No weight → not an objective
        assertFalse(model.isAnyExpressionQuadratic(), "A redundant quadratic constraint must not be counted");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("branchCoverageProvider")
    void testBranchCoverage(List<ExprDesc> exprs, boolean expected, String description) {
        populateModel(exprs);
        if (expected) {
            assertTrue(model.isAnyExpressionQuadratic(), () -> "Expected true for case: " + description);
        } else {
            assertFalse(model.isAnyExpressionQuadratic(), () -> "Expected false for case: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalCasesProvider")
    void testAdditionalCases(List<ExprDesc> exprs, boolean expected, String description) {
        populateModel(exprs);
        if (expected) {
            assertTrue(model.isAnyExpressionQuadratic(), () -> "Expected true for case: " + description);
        } else {
            assertFalse(model.isAnyExpressionQuadratic(), () -> "Expected false for case: " + description);
        }
    }

    /**
     * Inject a {@code null} entry into the private {@code myExpressions} map and verify NPE.
     */
    @Test
    void testNullExpressionThrowsNPE() throws Exception {
        // Use reflection to obtain the private map
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) mapField.get(model);
        // Insert a null value (key can be arbitrary)
        map.put("nullExpr", null);
        // The method iterates over the map values and should hit the null entry,
        // resulting in a NullPointerException when calling expr.isAnyQuadraticFactorNonZero().
        assertThrows(NullPointerException.class, () -> model.isAnyExpressionQuadratic(), "Iterating over a null expression should throw NPE");
    }
}