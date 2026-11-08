package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.stream.Stream;

class ExpressionsBasedModel_removeExpression_Test {

    private ExpressionsBasedModel model;

    private static final String NULL_KEY = "__NULL__";

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> validNameProvider() {
        return Stream.of(Arguments.of("expr1"), // empty string
        Arguments.of(""), // whitespace only
        Arguments.of("   "), // special characters
        Arguments.of("@!#%&*"));
    }

    @Test
    void testRemoveExistingExpression() {
        String exprName = "myExpr";
        Expression expr = model.newExpression(exprName);
        expr.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertNotNull(model.getExpression(exprName));
        int countBefore = model.countExpressions();
        model.removeExpression(exprName);
        assertNull(model.getExpression(exprName));
        assertEquals(countBefore - 1, model.countExpressions());
    }

    @Test
    void testRemoveNonExistingExpressionDoesNotThrow() {
        assertNull(model.getExpression("unknown"));
        int countBefore = model.countExpressions();
        assertDoesNotThrow(() -> model.removeExpression("unknown"));
        assertEquals(countBefore, model.countExpressions());
    }

    @Test
    void testRemoveExpressionMultipleTimesIsIdempotent() {
        String name = "temp";
        model.newExpression(name);
        assertNotNull(model.getExpression(name));
        model.removeExpression(name);
        assertNull(model.getExpression(name));
        assertDoesNotThrow(() -> model.removeExpression(name));
        assertNull(model.getExpression(name));
        assertTrue(model.countExpressions() >= 0);
    }

    @ParameterizedTest(name = "[{index}] remove existing expression with name=\"{0}\"")
    @MethodSource("validNameProvider")
    void testRemoveExistingExpression_WithVariousNames(String name) {
        Expression expr = model.newExpression(name);
        expr.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertNotNull(model.getExpression(name));
        int countBefore = model.countExpressions();
        model.removeExpression(name);
        assertNull(model.getExpression(name));
        assertEquals(countBefore - 1, model.countExpressions());
    }

    @Test
    void testRemoveFromEmptyModelIsNoOp() {
        assertEquals(0, model.countExpressions());
        assertDoesNotThrow(() -> model.removeExpression("anyName"));
        assertEquals(0, model.countExpressions());
    }

    @Test
    void testRemoveNullNameWhenNullKeyAbsent() {
        // No expression with the sentinel key present
        assertNull(model.getExpression(NULL_KEY));
        int countBefore = model.countExpressions();
        assertDoesNotThrow(() -> model.removeExpression(NULL_KEY));
        assertEquals(countBefore, model.countExpressions());
        assertNull(model.getExpression(NULL_KEY));
    }

    @Test
    void testRemoveNullNameWhenNullKeyPresent() {
        // Add an expression using the sentinel key
        Expression nullExpr = model.newExpression(NULL_KEY);
        nullExpr.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertNotNull(model.getExpression(NULL_KEY));
        int countBefore = model.countExpressions();
        model.removeExpression(NULL_KEY);
        assertNull(model.getExpression(NULL_KEY));
        assertEquals(countBefore - 1, model.countExpressions());
    }

    @Test
    void testRemoveDuplicateName() {
        model.newExpression("dup");
        // overwrite
        model.newExpression("dup");
        assertEquals(1, model.countExpressions());
        model.removeExpression("dup");
        assertNull(model.getExpression("dup"));
        assertEquals(0, model.countExpressions());
    }

    @Test
    void testCaseSensitivityDuringRemoval() {
        model.newExpression("Case");
        model.newExpression("case");
        assertEquals(2, model.countExpressions());
        model.removeExpression("Case");
        assertNull(model.getExpression("Case"));
        assertNotNull(model.getExpression("case"));
        assertEquals(1, model.countExpressions());
    }

    @Test
    void testRemoveNonNullDoesNotAffectNullKey() {
        // Add a sentinel‑key expression and a regular one
        model.newExpression(NULL_KEY);
        model.newExpression("nonNull");
        int countBefore = model.countExpressions();
        // Remove the regular entry only
        model.removeExpression("nonNull");
        assertNull(model.getExpression("nonNull"));
        assertNotNull(model.getExpression(NULL_KEY));
        assertEquals(countBefore - 1, model.countExpressions());
    }

    @Test
    void testRemoveExpressionDoesNotAffectCopy() {
        model.newExpression("toRemove");
        ExpressionsBasedModel copy = model.copy();
        assertNotNull(model.getExpression("toRemove"));
        assertNotNull(copy.getExpression("toRemove"));
        int originalCount = model.countExpressions();
        int copyCount = copy.countExpressions();
        model.removeExpression("toRemove");
        assertNull(model.getExpression("toRemove"));
        assertEquals(originalCount - 1, model.countExpressions());
        assertNotNull(copy.getExpression("toRemove"));
        assertEquals(copyCount, copy.countExpressions());
    }
}
