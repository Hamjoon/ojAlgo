package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Optimisation.Result;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.ProgrammingError;

class ExpressionsBasedModel_newExpression_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test guarantees isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources, but we clear references to help GC.
        model = null;
    }

    /**
     * Provides a stream of valid name values together with a short description.
     */
    static Stream<Arguments> validNameProvider() {
        return Stream.of(Arguments.of("cost", "normal non‑empty name"), Arguments.of("", "empty string name"), Arguments.of("   ", "whitespace‑only name"), Arguments.of("π_Δ-ßß🚀", "Unicode / emoji name"), Arguments.of("a".repeat(10_000), "very long name (10 000 'a' characters)"));
    }

    @Test
    void testNewExpressionReturnsNonNullAndIsRegistered() {
        String exprName = "myExpression";
        Expression expr = model.newExpression(exprName);
        // The factory must never return null.
        assertNotNull(expr, "newExpression should return a non‑null Expression");
        // The expression must be stored in the model under the given name.
        Expression retrieved = model.getExpression(exprName);
        assertSame(expr, retrieved, "Expression retrieved by name must be the same instance created");
        // The model's expression count must have increased.
        assertEquals(1, model.countExpressions(), "Model should contain exactly one expression after creation");
    }

    @Test
    void testNewExpressionCanBeUsedInAFeasibleModel() {
        // Create a variable that will be part of the optimisation problem.
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20)).weight(BigDecimal.ONE);
        // Create an expression that forces 10 * x == 100 (i.e. x == 10).
        Expression expr = model.newExpression("10*x = 100").lower(BigDecimal.valueOf(100)).upper(BigDecimal.valueOf(100));
        // Add the term 10 * x to the expression.
        expr.add(x, BigDecimal.TEN);
        // Solve the model (minimise is sufficient – the objective is zero).
        Result result = model.minimise();
        // Verify that the solution satisfies the constraint and that the variable value is 10.
        assertTrue(model.validate(result), "Result should be valid for the model");
        assertEquals(Optimisation.State.OPTIMAL, result.getState(), "Model should be solved to optimality");
        BigDecimal xValue = result.get(model.indexOf(x));
        TestUtils.assertEquals(BigDecimal.TEN, xValue);
    }

    @Test
    void testMultipleExpressionsAreStoredSeparately() {
        Expression e1 = model.newExpression("e1");
        Expression e2 = model.newExpression("e2");
        assertNotSame(e1, e2, "Each call to newExpression must create a distinct Expression instance");
        assertEquals(2, model.countExpressions(), "Model must contain two expressions after two creations");
        assertSame(e1, model.getExpression("e1"));
        assertSame(e2, model.getExpression("e2"));
    }

    @ParameterizedTest(name = "[{index}] newExpression with name: {1}")
    @MethodSource("validNameProvider")
    void testNewExpressionWithVariousValidNames(String name, String _description) {
        // Act
        Expression expr = model.newExpression(name);
        // Assert – basic contract
        assertNotNull(expr, "Expression must not be null for name: '" + name + "'");
        assertSame(expr, model.getExpression(name), "Expression retrieved by name must be the same instance created");
        assertEquals(1, model.countExpressions(), "Model should contain exactly one expression after creation");
    }

    @Test
    void testNewExpressionNullNameThrowsException() {
        // The Expression constructor requires a non‑null name, so a NPE (or similar) is expected.
        assertThrows(NullPointerException.class, () -> model.newExpression(null), "Calling newExpression(null) should throw NullPointerException");
        // The model must stay unchanged.
        assertEquals(0, model.countExpressions(), "Model should contain no expressions after a failed creation");
    }

    @Test
    void testDuplicateNameReplacesPreviousExpression() {
        String dupName = "capacity";
        // First creation
        Expression first = model.newExpression(dupName);
        assertNotNull(first);
        assertEquals(1, model.countExpressions(), "After first creation the model should contain one expression");
        // Second creation with the same name
        Expression second = model.newExpression(dupName);
        assertNotNull(second);
        // The new instance must be different from the first one
        assertNotSame(first, second, "Duplicate creation should return a new Expression instance");
        // Map size must stay 1 (replacement, not addition)
        assertEquals(1, model.countExpressions(), "Map size must stay 1 after duplicate name replacement");
        // Retrieval must yield the second instance
        assertSame(second, model.getExpression(dupName), "The name must map to the most recently created Expression");
    }

    @Test
    void testNewExpressionAfterModelDisposalThrowsException() {
        model.dispose();
        // The exact exception type depends on the implementation; ProgrammingError is used by ojAlgo.
        assertThrows(RuntimeException.class, () -> model.newExpression("postDispose"), "Creating an expression after disposal should throw a RuntimeException");
        // Ensure the internal map is still empty.
        assertEquals(0, model.countExpressions(), "No expressions should be added after the model has been disposed");
    }

    @Test
    void testNewExpressionWithControlCharacterNameThrowsException() {
        // NUL character
        String illegalName = "\u0000";
        // Expect some form of IllegalArgumentException (or a generic RuntimeException) from the Expression ctor.
        assertThrows(RuntimeException.class, () -> model.newExpression(illegalName), "Expression names containing control characters should cause an exception");
        // Model must remain unchanged.
        assertEquals(0, model.countExpressions(), "Model should contain no expressions after a failed creation with illegal name");
    }
}
