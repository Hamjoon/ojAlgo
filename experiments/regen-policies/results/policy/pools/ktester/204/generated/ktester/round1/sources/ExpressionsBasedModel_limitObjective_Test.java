package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_limitObjective_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> nullBoundsProvider() {
        return Stream.of(// lower = null, upper = 5
        Arguments.of(null, new BigDecimal("5")), // lower = -3, upper = null
        Arguments.of(new BigDecimal("-3"), null), // both null
        Arguments.of(null, null));
    }

    @Test
    void testLimitObjectiveCreatesConstraintAndUpdatesBounds() {
        // -----------------------------------------------------------------
        // GIVEN – a model with a simple linear objective (no quadratic part)
        // -----------------------------------------------------------------
        Variable x = model.newVariable("x").weight(BigDecimal.ONE);
        // The objective expression is automatically created when a variable gets a weight.
        Expression objective = model.objective();
        assertNotNull(objective, "Objective expression should be present");
        // -----------------------------------------------------------------
        // WHEN – limitObjective is invoked for the first time
        // -----------------------------------------------------------------
        BigDecimal lower1 = BigDecimal.valueOf(5);
        BigDecimal upper1 = BigDecimal.valueOf(10);
        Expression firstCall = model.limitObjective(lower1, upper1);
        // -----------------------------------------------------------------
        // THEN – a new constraint expression is created (different instance)
        // -----------------------------------------------------------------
        assertNotNull(firstCall, "Returned expression must not be null");
        assertNotSame(objective, firstCall, "A copy of the objective should be used as a constraint");
        assertEquals(lower1, firstCall.lower(), "Lower bound should be set on the constraint");
        assertEquals(upper1, firstCall.upper(), "Upper bound should be set on the constraint");
        // -----------------------------------------------------------------
        // WHEN – limitObjective is invoked a second time with different bounds
        // -----------------------------------------------------------------
        BigDecimal lower2 = BigDecimal.valueOf(6);
        BigDecimal upper2 = BigDecimal.valueOf(12);
        Expression secondCall = model.limitObjective(lower2, upper2);
        // -----------------------------------------------------------------
        // THEN – the same constraint expression instance is returned and its bounds are updated
        // -----------------------------------------------------------------
        assertSame(firstCall, secondCall, "Subsequent calls must reuse the same constraint expression");
        assertEquals(lower2, secondCall.lower(), "Lower bound should be updated on the constraint");
        assertEquals(upper2, secondCall.upper(), "Upper bound should be updated on the constraint");
    }

    /**
     * Retrieves the private {@code myExpressions} map via reflection.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Expression> getInternalExpressionMap() {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            field.setAccessible(true);
            return (Map<String, Expression>) field.get(model);
        } catch (Exception e) {
            throw new AssertionError("Unable to access myExpressions map via reflection", e);
        }
    }

    @Test
    void testLinearObjectiveCreatesConstraintAndSetsBounds() {
        // GIVEN – a simple linear objective (no quadratic part)
        model.newVariable("x").weight(BigDecimal.ONE);
        Expression objective = model.objective();
        assertNotNull(objective, "Objective expression should be present");
        // WHEN – limitObjective is invoked for the first time
        BigDecimal lower = BigDecimal.ZERO;
        BigDecimal upper = BigDecimal.TEN;
        Expression constraint = model.limitObjective(lower, upper);
        // THEN – a new constraint expression is created and stored
        assertNotNull(constraint, "Returned expression must not be null");
        assertNotSame(objective, constraint, "Constraint must be a copy of the objective");
        assertEquals(lower, constraint.lower(), "Lower bound should be set on the constraint");
        assertEquals(upper, constraint.upper(), "Upper bound should be set on the constraint");
        // internal map should contain the entry
        Map<String, Expression> map = getInternalExpressionMap();
        assertTrue(map.containsValue(constraint), "Constraint expression should be stored internally");
    }

    @Test
    void testLinearObjectiveReusesConstraintAndUpdatesBounds() {
        // GIVEN – linear objective and an initial constraint
        model.newVariable("x").weight(BigDecimal.ONE);
        Expression first = model.limitObjective(BigDecimal.ZERO, BigDecimal.TEN);
        assertNotNull(first, "First call must create a constraint");
        // WHEN – limitObjective is called again with different bounds
        BigDecimal newLower = BigDecimal.valueOf(5);
        BigDecimal newUpper = BigDecimal.valueOf(15);
        Expression second = model.limitObjective(newLower, newUpper);
        // THEN – the same instance is returned and its bounds are updated
        assertSame(first, second, "Subsequent calls must return the same constraint instance");
        assertEquals(newLower, second.lower(), "Lower bound should be updated");
        assertEquals(newUpper, second.upper(), "Upper bound should be updated");
    }

    @Test
    void testQuadraticObjectiveReturnsOriginalObjective() {
        // GIVEN – a quadratic objective (non‑zero quadratic factor)
        model.newVariable("x").weight(BigDecimal.ONE);
        Expression obj = model.objective();
        // Introduce a quadratic term; the exact API may differ but this is the typical signature.
        obj.setQuadraticFactor(0, 0, BigDecimal.ONE);
        assertTrue(model.isAnyObjectiveQuadratic(), "Objective must be recognised as quadratic");
        // WHEN – limitObjective is invoked
        Expression result = model.limitObjective(BigDecimal.valueOf(-5), BigDecimal.valueOf(5));
        // THEN – the original objective is returned and no constraint is stored
        assertSame(obj, result, "Method must return the original objective for quadratic case");
        Map<String, Expression> map = getInternalExpressionMap();
        assertFalse(map.containsValue(result), "No constraint expression should be stored for quadratic objective");
    }

    @ParameterizedTest
    @MethodSource("nullBoundsProvider")
    void testNullBoundsCreateOrUpdateConstraint(BigDecimal lower, BigDecimal upper) {
        // GIVEN – linear objective
        model.newVariable("x").weight(BigDecimal.ONE);
        Expression objective = model.objective();
        // WHEN – first call with possibly null bounds
        Expression first = model.limitObjective(lower, upper);
        // THEN – a constraint is created (different from objective)
        assertNotNull(first, "Constraint expression must be created");
        assertNotSame(objective, first, "Constraint must be a copy of the objective");
        assertEquals(lower, first.lower(), "Lower bound should match the supplied value (or null)");
        assertEquals(upper, first.upper(), "Upper bound should match the supplied value (or null)");
        // WHEN – second call with swapped bounds to ensure reuse
        BigDecimal newLower = (lower == null) ? BigDecimal.ONE : lower.add(BigDecimal.ONE);
        BigDecimal newUpper = (upper == null) ? BigDecimal.TEN : upper.add(BigDecimal.ONE);
        Expression second = model.limitObjective(newLower, newUpper);
        // THEN – same instance is reused and bounds are updated
        assertSame(first, second, "Same constraint instance must be reused");
        assertEquals(newLower, second.lower(), "Lower bound should be updated on reuse");
        assertEquals(newUpper, second.upper(), "Upper bound should be updated on reuse");
    }

    @Test
    void testQuadraticObjectiveWithNullBoundsReturnsOriginal() {
        // GIVEN – quadratic objective
        model.newVariable("x").weight(BigDecimal.ONE);
        Expression obj = model.objective();
        obj.setQuadraticFactor(0, 0, BigDecimal.ONE);
        assertTrue(model.isAnyObjectiveQuadratic(), "Objective must be quadratic");
        // WHEN – limitObjective called with null bounds
        Expression result = model.limitObjective(null, null);
        // THEN – original objective is returned, no constraint stored
        assertSame(obj, result, "Should return the original objective when quadratic");
        Map<String, Expression> map = getInternalExpressionMap();
        assertFalse(map.containsValue(result), "No constraint should be stored for quadratic objective");
    }
}
