package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_newExpression_Test {

    /**
     * Model used in the tests – created fresh for each test method.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh model for every test to guarantee isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector and make the intent explicit.
        model = null;
    }

    /**
     * Verify that {@code newExpression(String)} returns a non‑null {@link Expression},
     * that the expression can be retrieved by name, and that the name is preserved.
     */
    @Test
    void testExpressionCreatedAndRegistered() {
        String exprName = "myExpr";
        // Act: create a new expression via the focal method.
        Expression expr = model.newExpression(exprName);
        // Assert: the returned object is not null.
        assertNotNull(expr, "newExpression should never return null");
        // Assert: the expression is stored in the model and can be fetched by name.
        Expression fetched = model.getExpression(exprName);
        assertNotNull(fetched, "Expression should be retrievable via getExpression(name)");
        assertSame(expr, fetched, "Fetched expression should be the same instance that was created");
        // Assert: the name reported by the expression matches the supplied name.
        assertEquals(exprName, expr.getName(), "Expression name should match the argument passed to newExpression");
    }

    /**
     * A very small optimisation problem that uses an expression created with {@code newExpression}.
     * The problem is: minimise  x  subject to  5·x = 10  and  0 ≤ x ≤ 20.
     * The optimal solution is x = 2.
     */
    @Test
    void testExpressionCanBeUsedInAProblem() {
        // Arrange: create a variable and an expression that constrains it.
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20)).weight(BigDecimal.ONE);
        Expression expr = model.newExpression("constraint").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(10));
        // 5·x = 10  →  x = 2
        expr.add(x, BigDecimal.valueOf(5));
        // Act: solve the model.
        Optimisation.Result result = model.minimise();
        // Assert: the model should be feasible and optimal.
        assertTrue(model.validate(result), "Result should validate against the model");
        assertTrue(result.getState().isOptimal(), "Result state should be optimal");
        // Assert: the variable value is the expected optimum.
        BigDecimal xResult = result.get(model.indexOf(x));
        assertEquals(BigDecimal.valueOf(2), xResult, "Optimal value of x should be 2");
    }

    /**
     * Ensure that creating two expressions with the same name replaces the previous one.
     * The contract of {@code newExpression} (as used by the library) is to store the latest
     * instance under the given name.
     */
    @Test
    void testDuplicateNameOverridesPreviousExpression() {
        String duplicateName = "dup";
        Expression first = model.newExpression(duplicateName);
        first.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // Create a second expression with the same name.
        Expression second = model.newExpression(duplicateName);
        second.lower(BigDecimal.TEN).upper(BigDecimal.TEN);
        // The model should now return the second instance.
        Expression fetched = model.getExpression(duplicateName);
        assertSame(second, fetched, "The later expression should replace the earlier one");
        assertNotSame(first, fetched, "The earlier expression should no longer be registered");
    }

    /**
     * Parameterised test covering the normal and empty‑string name scenarios.
     *
     * <p>
     * For each supplied name we verify:
     * <ul>
     *   <li>An {@link Expression} is returned whose {@code getName()} equals the argument.</li>
     *   <li>The expression can be fetched from the model via {@code getExpression(name)}.</li>
     *   <li>The model's {@code countExpressions()} is increased by exactly one.</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "newExpression creates and registers expression with name=''{0}''")
    @CsvSource({ // normal non‑null name
    // empty string name – library accepts it
    "'profit', true", "'', true" })
    void testNewExpressionBasicFunctionality(String name, boolean expectSuccess) {
        int beforeCount = model.countExpressions();
        // Act
        Expression expr = model.newExpression(name);
        // Assert basic contract
        assertNotNull(expr, "Expression should not be null");
        assertEquals(name, expr.getName(), "Expression name must match the supplied argument");
        assertSame(expr, model.getExpression(name), "Model must return the same instance via getExpression");
        // Verify count increment
        int afterCount = model.countExpressions();
        assertEquals(beforeCount + 1, afterCount, "countExpressions() should increase by exactly one after creation");
    }

    /**
     * Verify that passing {@code null} as the name results in an exception.
     *
     * <p>
     * The exact exception type depends on the internal {@link Expression} constructor.
     * The library historically throws {@link NullPointerException}, but an {@link IllegalArgumentException}
     * would also be acceptable. The test therefore accepts either.
     * </p>
     */
    @Test
    void testNewExpressionWithNullNameThrowsException() {
        // The model should reject a null identifier.
        assertThrows(RuntimeException.class, () -> model.newExpression(null), "Calling newExpression(null) should throw a runtime exception (NPE or IAE)");
    }

    /**
     * Additional sanity check: after creating several expressions the model reports the correct total.
     */
    @Test
    void testCountExpressionsReflectsMultipleAdditions() {
        int initial = model.countExpressions();
        model.newExpression("e1");
        model.newExpression("e2");
        model.newExpression("e3");
        assertEquals(initial + 3, model.countExpressions(), "Model should report three additional expressions");
    }
}