package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_limitObjective_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
        // a simple variable that participates in the objective (weight = 1)
        x = model.newVariable("x").weight(BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
        x = null;
    }

    /* -------------------------------------------------------------
     *  Helper methods to reach the private static key and the private map
     * ------------------------------------------------------------- */
    private static String getConstraintKey() throws Exception {
        Field keyField = ExpressionsBasedModel.class.getDeclaredField("OBJ_FUNC_AS_CONSTR_KEY");
        keyField.setAccessible(true);
        return (String) keyField.get(null);
    }

    @Test
    void testFirstCallCreatesConstraintExpression() {
        BigDecimal lower = new BigDecimal("1.5");
        BigDecimal upper = new BigDecimal("7.2");
        Expression expr = model.limitObjective(lower, upper);
        assertNotNull(expr, "limitObjective should return a non‑null Expression");
        assertEquals(lower, expr.lower(), "Lower bound should be set");
        assertEquals(upper, expr.upper(), "Upper bound should be set");
    }

    @Test
    void testSubsequentCallsReuseSameExpression() {
        Expression first = model.limitObjective(BigDecimal.ZERO, new BigDecimal("10"));
        Expression second = model.limitObjective(new BigDecimal("2"), new BigDecimal("8"));
        assertSame(first, second, "Subsequent calls must return the same Expression instance");
        assertEquals(new BigDecimal("2"), second.lower(), "Lower bound should be updated on reuse");
        assertEquals(new BigDecimal("8"), second.upper(), "Upper bound should be updated on reuse");
    }

    @Test
    void testNullBoundsAreHandledGracefully() {
        Expression onlyLower = model.limitObjective(new BigDecimal("3"), null);
        assertEquals(new BigDecimal("3"), onlyLower.lower(), "Lower bound should be set");
        assertNull(onlyLower.upper(), "Upper bound should be null when not supplied");
        Expression onlyUpper = model.limitObjective(null, new BigDecimal("9"));
        assertNull(onlyUpper.lower(), "Lower bound should become null when not supplied");
        assertEquals(new BigDecimal("9"), onlyUpper.upper(), "Upper bound should be set");
    }

    @Test
    void testLimitObjectiveReturnsObjectiveWhenNoCopyIsMade() {
        Expression obj = model.objective();
        obj.setQuadraticFactor(x, x, BigDecimal.ONE);
        Expression result = model.limitObjective(BigDecimal.ONE, BigDecimal.TEN);
        assertSame(obj, result, "When the objective is quadratic, limitObjective should return the original objective");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Expression> getExpressionMap(ExpressionsBasedModel model) throws Exception {
        Field mapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        mapField.setAccessible(true);
        return (Map<String, Expression>) mapField.get(model);
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for the “first call – linear objective” group
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ "-10,20", "0,0" })
    void testFirstCallCreatesConstraintWithVariousBounds(String lowerStr, String upperStr) throws Exception {
        BigDecimal lower = lowerStr == null ? null : new BigDecimal(lowerStr);
        BigDecimal upper = upperStr == null ? null : new BigDecimal(upperStr);
        Expression expr = model.limitObjective(lower, upper);
        assertNotNull(expr);
        assertEquals(lower, expr.lower(), "Lower bound must match the supplied value");
        assertEquals(upper, expr.upper(), "Upper bound must match the supplied value");
        String key = getConstraintKey();
        Map<String, Expression> map = getExpressionMap(model);
        assertTrue(map.containsKey(key), "Constraint expression should be cached");
        assertSame(expr, map.get(key), "Cached expression must be the same instance returned");
    }

    /* -------------------------------------------------------------
     *  Tests for the “existing constraint” group
     * ------------------------------------------------------------- */
    @Test
    void testExistingConstraintIsReusedAndUpdated() throws Exception {
        Expression created = model.limitObjective(new BigDecimal("-100"), new BigDecimal("100"));
        Expression reused = model.limitObjective(new BigDecimal("-50"), null);
        assertSame(created, reused, "The same cached expression must be reused");
        assertEquals(new BigDecimal("-50"), reused.lower(), "Lower bound must be updated");
        assertEquals(new BigDecimal("100"), reused.upper(), "Upper bound must stay unchanged");
    }

    @Test
    void testExistingConstraintNullBothBoundsKeepsPreviousValues() throws Exception {
        Expression first = model.limitObjective(new BigDecimal("5"), new BigDecimal("15"));
        Expression second = model.limitObjective(null, null);
        assertSame(first, second, "Same instance must be returned");
        assertEquals(new BigDecimal("5"), second.lower(), "Lower bound should stay as previously set");
        assertEquals(new BigDecimal("15"), second.upper(), "Upper bound should stay as previously set");
    }

    /* -------------------------------------------------------------
     *  Tests for mixed null bounds on first creation
     * ------------------------------------------------------------- */
    @Test
    void testFirstCallMixedNullUpper() throws Exception {
        Expression expr = model.limitObjective(null, new BigDecimal("5"));
        assertNull(expr.lower(), "Lower bound should stay null");
        assertEquals(new BigDecimal("5"), expr.upper(), "Upper bound must be set");
    }

    @Test
    void testFirstCallMixedNullLower() throws Exception {
        Expression expr = model.limitObjective(new BigDecimal("-5"), null);
        assertEquals(new BigDecimal("-5"), expr.lower(), "Lower bound must be set");
        assertNull(expr.upper(), "Upper bound should stay null");
    }

    /* -------------------------------------------------------------
     *  Test that a quadratic objective prevents constraint creation
     * ------------------------------------------------------------- */
    @Test
    void testQuadraticObjectiveDoesNotCreateConstraint() throws Exception {
        model.objective().setQuadraticFactor(x, x, BigDecimal.ONE);
        Expression result = model.limitObjective(new BigDecimal("-3"), new BigDecimal("3"));
        assertSame(model.objective(), result, "Should return the original objective when quadratic");
        Map<String, Expression> map = getExpressionMap(model);
        assertTrue(map.isEmpty(), "No constraint expression should be stored for a quadratic objective");
    }

    /* -------------------------------------------------------------
     *  Tests that verify exception handling for null bounds
     * ------------------------------------------------------------- */
//     @Test
//     void testNullLowerBoundOnNewConstraintThrows_NullPointerException(@Mock Expression mockCopy) throws Exception {
//         // mock the objective to return a mock that throws NPE on lower(null)
//         Expression mockObj = mock(Expression.class);
//         when(mockObj.isAnyQuadraticFactorNonZero()).thenReturn(false);
//         when(mockObj.copy(any(), eq(false))).thenReturn(mockCopy);
//         when(mockCopy.lower(isNull())).thenThrow(new NullPointerException("lower null"));
//         // spy the model to return the mocked objective
//         ExpressionsBasedModel spyModel = spy(model);
//         doReturn(mockObj).when(spyModel).objective();
//         assertThrows(NullPointerException.class, () -> spyModel.limitObjective(null, new BigDecimal("5")), "Calling lower(null) on a freshly copied expression should propagate NPE");
//     }

//     @Test
//     void testNullUpperBoundOnNewConstraintThrows_NullPointerException(@Mock Expression mockCopy) throws Exception {
//         Expression mockObj = mock(Expression.class);
//         when(mockObj.isAnyQuadraticFactorNonZero()).thenReturn(false);
//         when(mockObj.copy(any(), eq(false))).thenReturn(mockCopy);
//         when(mockCopy.upper(isNull())).thenThrow(new NullPointerException("upper null"));
//         ExpressionsBasedModel spyModel = spy(model);
//         doReturn(mockObj).when(spyModel).objective();
//         assertThrows(NullPointerException.class, () -> spyModel.limitObjective(new BigDecimal("-3"), null), "Calling upper(null) on a freshly copied expression should propagate NPE");
//     }

//     @Test
//     void testNullLowerBoundOnExistingConstraintThrows_NullPointerException(@Mock Expression mockExpr) throws Exception {
//         // put a mock expression into the internal map
//         String key = getConstraintKey();
//         Map<String, Expression> map = getExpressionMap(model);
//         map.put(key, mockExpr);
//         // configure the mock to throw on lower(null)
//         when(mockExpr.lower(isNull())).thenThrow(new NullPointerException("lower null"));
//         assertThrows(NullPointerException.class, () -> model.limitObjective(null, new BigDecimal("10")), "Updating an existing constraint with lower=null should propagate NPE");
//     }

//     @Test
//     void testNullUpperBoundOnExistingConstraintThrows_NullPointerException(@Mock Expression mockExpr) throws Exception {
//         String key = getConstraintKey();
//         Map<String, Expression> map = getExpressionMap(model);
//         map.put(key, mockExpr);
//         when(mockExpr.upper(isNull())).thenThrow(new NullPointerException("upper null"));
//         assertThrows(NullPointerException.class, () -> model.limitObjective(new BigDecimal("-10"), null), "Updating an existing constraint with upper=null should propagate NPE");
//     }

    /* -------------------------------------------------------------
     *  Test that an inconsistent interval triggers an exception during tighten()
     * ------------------------------------------------------------- */
    @Test
    void testInconsistentBoundsCauseExceptionDuringTighten() {
        assertThrows(RuntimeException.class, () -> model.limitObjective(new BigDecimal("10"), new BigDecimal("5")), "tighten() should reject lower > upper and throw a RuntimeException");
    }

    /* -------------------------------------------------------------
     *  Test that a null objective leads to NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void testNullObjectiveLeadsToNullPointerException() {
        ExpressionsBasedModel modelWithNullObjective = new ExpressionsBasedModel() {

            @Override
            public Expression objective() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> modelWithNullObjective.limitObjective(new BigDecimal("-1"), new BigDecimal("1")), "When objective() returns null, limitObjective should throw NPE");
    }

    /* -------------------------------------------------------------
     *  Additional sanity checks for bound updates when only one side changes
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ // only lower changes
    // only upper changes
    "-50,null, -50, 100", "null,75,  -5, 75" })
    void testUpdateSingleBoundOnExistingConstraint(String lowerStr, String upperStr, String expectedLowerStr, String expectedUpperStr) throws Exception {
        // create initial constraint
        model.limitObjective(new BigDecimal("-5"), new BigDecimal("100"));
        // perform the update
        BigDecimal lower = lowerStr == null ? null : new BigDecimal(lowerStr);
        BigDecimal upper = upperStr == null ? null : new BigDecimal(upperStr);
        Expression updated = model.limitObjective(lower, upper);
        assertEquals(new BigDecimal(expectedLowerStr), updated.lower(), "Lower bound after update must match expectation");
        assertEquals(new BigDecimal(expectedUpperStr), updated.upper(), "Upper bound after update must match expectation");
    }
}