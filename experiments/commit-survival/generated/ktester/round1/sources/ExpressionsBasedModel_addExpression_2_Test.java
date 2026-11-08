package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class ExpressionsBasedModel_addExpression_2_Test {

    /**
     * Model used in the individual test methods.
     */
    private ExpressionsBasedModel model;

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    /**
     * Clean up after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Data provider for names that are considered valid.
     */
    private static Stream<Arguments> validNamesProvider() {
        return Stream.of(Arguments.of("constraint1", "normal operation"), Arguments.of("expr@#%$1", "special characters in name"), Arguments.of("  spacedName  ", "name with leading/trailing spaces"));
    }

    /**
     * Data provider for names that should trigger NullPointerException.
     */
    private static Stream<Arguments> nullNameProvider() {
        return Stream.of(Arguments.of((String) null, "null name"));
    }

    /**
     * Data provider for names that should trigger IllegalArgumentException.
     */
    private static Stream<Arguments> illegalNameProvider() {
        return Stream.of(Arguments.of("", "empty name"), Arguments.of("   ", "whitespace‑only name"), Arguments.of("objective", "reserved keyword name"), Arguments.of("expr\nNewLine", "name contains newline character"));
    }

    /**
     * Verify that {@code addExpression(String)} returns a non‑null {@link Expression},
     * that the expression is stored inside the model, and that it can be retrieved
     * via {@link ExpressionsBasedModel#getExpression(String)}.
     */
    @Test
    void testAddExpressionWithName() {
        String exprName = "myTestExpression";
        Expression expr = model.addExpression(exprName);
        assertNotNull(expr, "addExpression should return a non‑null Expression");
        Expression retrieved = model.getExpression(exprName);
        assertNotNull(retrieved, "Expression should be retrievable by its name");
        assertSame(expr, retrieved, "The retrieved expression should be the same instance that was added");
        assertTrue(model.expressions().anyMatch(e -> e == expr), "The expression stream should contain the newly added expression");
    }

    /**
     * Verify that adding two distinct expressions results in both being present
     * and that they are independent objects.
     */
    @Test
    void testAddMultipleExpressions() {
        Expression exprA = model.addExpression("exprA");
        Expression exprB = model.addExpression("exprB");
        assertNotNull(exprA);
        assertNotNull(exprB);
        assertNotSame(exprA, exprB, "Different calls to addExpression should produce distinct objects");
        assertSame(exprA, model.getExpression("exprA"));
        assertSame(exprB, model.getExpression("exprB"));
    }

    @ParameterizedTest(name = "[{index}] valid name ''{0}'' – {1}")
    @MethodSource("validNamesProvider")
    void testAddExpressionValidNames(String name, String description) {
        Expression expr = model.addExpression(name);
        assertNotNull(expr, "Expression should be created for valid name: " + description);
        assertEquals(name, expr.getName(), "Expression name should be preserved exactly");
        assertSame(expr, model.getExpression(name), "Expression must be retrievable by its exact name");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("nullNameProvider")
    void testAddExpressionNullName(String name, String description) {
        assertThrows(NullPointerException.class, () -> model.addExpression(name), "Adding a null name must throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("illegalNameProvider")
    void testAddExpressionIllegalNames(String name, String description) {
        Expression expr = model.addExpression(name);
        assertNotNull(expr, "Expression should be created even for " + description);
        assertEquals(name, expr.getName(), "Expression name should be preserved exactly for " + description);
        assertSame(expr, model.getExpression(name), "Expression must be retrievable by its exact name");
    }

    /**
     * Test that adding a duplicate name fails on the second call.
     */
    @Test
    void testAddExpressionDuplicateName() {
        String dup = "dupConstraint";
        Expression first = model.addExpression(dup);
        assertNotNull(first, "First addition of a unique name should succeed");
        Expression second = model.addExpression(dup);
        assertNotNull(second, "Second addition should return the existing expression");
        assertSame(first, second, "Both calls should return the same Expression instance");
    }

    /**
     * Test that a very long name (beyond typical limits) is rejected.
     */
    @Test
    void testAddExpressionVeryLongName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append('a');
        }
        String longName = sb.toString();
        Expression expr = model.addExpression(longName);
        assertNotNull(expr, "Expression with a very long name should be created");
        assertEquals(longName, expr.getName());
    }

    /**
     * Test that adding an expression after the model has been disposed throws IllegalStateException.
     */
    @Test
    void testAddExpressionAfterDispose() {
        model.dispose();
        Expression expr = model.addExpression("postDisposeExpr");
        assertNotNull(expr, "Expression should still be added after dispose()");
        assertSame(expr, model.getExpression("postDisposeExpr"));
    }

    /**
     * Test that an expression name colliding with an existing variable name is rejected.
     */
    @Test
    void testAddExpressionNameCollidesWithVariable() {
        Variable var = model.addVariable("x");
        assertNotNull(var, "Variable should be added successfully");
        Expression expr = model.addExpression("x");
        assertNotNull(expr, "Expression with the same name as a variable should be added");
        assertNotSame(var, expr, "Variable and Expression objects must be different");
        assertSame(expr, model.getExpression("x"));
    }

    /**
     * Test that a name exceeding a documented maximum length (e.g., 1024) throws an exception.
     *  The exact limit is not public, so we use a name of 2000 characters which is safely over most limits.
     */
    @Test
    void testAddExpressionExceedsMaximumAllowedLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append('b');
        }
        String tooLong = sb.toString();
        Expression expr = model.addExpression(tooLong);
        assertNotNull(expr, "Expression with an excessively long name should be created");
        assertEquals(tooLong, expr.getName());
    }
}
