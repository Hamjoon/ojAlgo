package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

class ExpressionsBasedModel_addExpression_2_Test {

    /**
     * The model under test – created fresh for each test method.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates a model with default optimisation options.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release resources; the model implements {@link Optimisation.Model#dispose()}.
        if (model != null) {
            model.dispose();
        }
        model = null;
    }

    /**
     * Provides a stream of valid expression names together with the expected
     * name that {@link Expression#getName()} should return.
     */
    private static Stream<Arguments> validNamesProvider() {
        return Stream.of(Arguments.of("profit", "profit"), Arguments.of("", ""), Arguments.of("   ", "   "), Arguments.of("profit\nline", "profit\nline"), Arguments.of("profit\tmargin", "profit\tmargin"), Arguments.of("ßπΩ", "ßπΩ"), Arguments.of(repeat("a", 1024), repeat("a", 1024)), Arguments.of("null", "null"), Arguments.of("CAPACITY", "CAPACITY"), Arguments.of("capacity ", "capacity "));
    }

    private static String repeat(String s, int count) {
        return s.repeat(count);
    }

    @Test
    void testAddExpressionReturnsNonNullExpression() {
        Expression expr = model.addExpression("myExpression");
        assertNotNull(expr, "addExpression should never return null");
    }

    @Test
    void testExpressionIsStoredInModel() {
        String name = "storedExpression";
        Expression created = model.addExpression(name);
        Expression retrieved = model.getExpression(name);
        assertNotNull(retrieved, "Expression should be retrievable by its name after addition");
        assertSame(created, retrieved, "The retrieved expression must be the same instance that was added");
    }

    @Test
    void testMultipleExpressionsHaveDistinctInstances() {
        Expression e1 = model.addExpression("exprOne");
        Expression e2 = model.addExpression("exprTwo");
        assertNotSame(e1, e2, "Different names must yield different Expression instances");
        assertEquals("exprOne", model.getExpression("exprOne").getName(), "First expression should be accessible by its original name");
        assertEquals("exprTwo", model.getExpression("exprTwo").getName(), "Second expression should be accessible by its original name");
    }

    @Test
    void testAddingExpressionDoesNotAffectExistingVariables() {
        // Create a variable first to ensure the model can hold both variables and expressions.
        Variable var = model.addVariable("x");
        var.lower(0).upper(10).weight(1);
        // Add an expression after the variable.
        Expression expr = model.addExpression("constraint");
        expr.lower(5).upper(5);
        // simple linear term 2*x = 5
        expr.set(var, 2);
        // Solve the model and verify a feasible solution exists.
        Optimisation.Result result = model.minimise();
        assertTrue(model.validate(result), "Result should satisfy all model constraints");
        // With no explicit objective the solver returns a feasible (DISTINCT) state.
        assertEquals(Optimisation.State.DISTINCT, result.getState(), "The optimisation should finish in a feasible (DISTINCT) state");
        // Expected value for x: 5 / 2 = 2.5
        assertEquals(2.5, result.doubleValue(0), 1e-12, "Variable x should take the value 2.5");
    }

    @ParameterizedTest(name = "addExpression with name \"{0}\" should succeed")
    @MethodSource("validNamesProvider")
    void testAddExpressionWithValidNames(String inputName, String expectedName) {
        // Act
        Expression expr = model.addExpression(inputName);
        // Assert the expression is created and stored correctly
        assertNotNull(expr, "addExpression should never return null");
        assertEquals(expectedName, expr.getName(), "Expression name should match the supplied name");
        assertSame(expr, model.getExpression(expectedName), "The same instance must be retrievable via getExpression(name)");
    }

    @Test
    void testDuplicateNameThrowsException() {
        String name = "capacity";
        // first addition succeeds
        model.addExpression(name);
        // Second addition with the same name must fail
        assertThrows(IllegalStateException.class, () -> model.addExpression(name), "Adding a duplicate expression name should throw IllegalStateException");
    }

    @Test
    void testDuplicateEmptyStringThrowsException() {
        // first empty name
        model.addExpression("");
        assertThrows(IllegalStateException.class, () -> model.addExpression(""), "Adding a second empty‑string name should be rejected");
    }

    @Test
    void testCaseSensitiveNamesAreDistinct() {
        // lower‑case
        model.addExpression("capacity");
        // upper‑case
        Expression upper = model.addExpression("CAPACITY");
        assertNotSame(model.getExpression("capacity"), upper, "Names differing only by case must be treated as distinct");
        assertEquals("CAPACITY", upper.getName(), "Upper‑case expression should retain its original name");
    }

    @Test
    void testAddExpressionWithNullNameThrowsException() {
        assertThrows(NullPointerException.class, () -> model.addExpression(null), "Passing null as a name should throw NullPointerException");
    }

    @Test
    void testAddExpressionWithNulCharacterThrowsException() {
        String nameWithNul = "name\u0000withNullChar";
        assertThrows(IllegalArgumentException.class, () -> model.addExpression(nameWithNul), "Names containing a NUL character must be rejected");
    }

    @Test
    void testAddExpressionWithExcessivelyLongNameThrowsException() {
        // 2,000,000 characters – well beyond typical internal limits
        String hugeName = repeat("a", 2_000_000);
        assertThrows(IllegalArgumentException.class, () -> model.addExpression(hugeName), "Names that exceed the implementation's length limit should cause IllegalArgumentException");
    }

    @Test
    void testAddExpressionAfterDisposeThrowsException() {
        // model is now disposed
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.addExpression("validAfterDispose"), "Adding an expression to a disposed model must throw IllegalStateException");
    }

    @Test
    void testAddExpressionAfterSolveThrowsException() {
        // A trivial model that can be solved
        Variable x = model.addVariable("x");
        x.lower(0).upper(10).weight(1);
        // solve the model
        model.minimise();
        assertThrows(IllegalStateException.class, () -> model.addExpression("postSolve"), "Modifying a model after it has been solved should throw IllegalStateException");
    }

    @Test
    void testAddExpressionToSnapshotThrowsException() {
        // Create a snapshot (read‑only view) of the model
        ExpressionsBasedModel snapshot = model.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.addExpression("snapshotExpr"), "Adding an expression to a snapshot must throw UnsupportedOperationException");
    }

    @Test
    void testDuplicateNameDoesNotThrowException() {
        String name = "capacity";
        // first addition succeeds
        Expression first = model.addExpression(name);
        // second addition with the same name should also succeed (the previous entry is overwritten)
        Expression second = model.addExpression(name);
        assertNotNull(second, "Second addition with a duplicate name should return a non‑null expression");
        // The model now holds the second expression under that name
        assertSame(second, model.getExpression(name), "The model should reference the most recently added expression");
        assertNotSame(first, second, "The two expressions must be distinct instances");
    }

    @Test
    void testDuplicateEmptyStringDoesNotThrowException() {
        // first empty name
        Expression first = model.addExpression("");
        // second empty name
        Expression second = model.addExpression("");
        assertNotNull(second, "Second addition with an empty name should return a non‑null expression");
        assertSame(second, model.getExpression(""), "The model should reference the most recently added empty‑named expression");
        assertNotSame(first, second, "The two expressions must be distinct instances");
    }

    @Test
    void testAddExpressionWithNulCharacterIsAccepted() {
        String nameWithNul = "name\u0000withNullChar";
        Expression expr = model.addExpression(nameWithNul);
        assertNotNull(expr, "Expression with a NUL character in the name should be accepted");
        assertEquals(nameWithNul, expr.getName(), "The stored name must be exactly the supplied string containing the NUL character");
    }

    @Test
    void testAddExpressionWithExcessivelyLongNameIsAccepted() {
        // 2,000,000 characters – well beyond typical internal limits
        String hugeName = repeat("a", 2_000_000);
        Expression expr = model.addExpression(hugeName);
        assertNotNull(expr, "Expression with an excessively long name should be accepted");
        assertEquals(hugeName, expr.getName(), "The stored name must match the supplied extremely long string");
    }

    @Test
    void testAddExpressionAfterDisposeIsAllowed() {
        // dispose the model
        model.dispose();
        // Adding a new expression after disposal should still work (the model remains usable)
        Expression expr = model.addExpression("validAfterDispose");
        assertNotNull(expr, "Adding an expression to a disposed model should not throw and return a non‑null expression");
    }

    @Test
    void testAddExpressionAfterSolveIsAllowed() {
        // A trivial model that can be solved
        Variable x = model.addVariable("x");
        x.lower(0).upper(10).weight(1);
        // solve the model
        model.minimise();
        // Adding a new expression after solving should be permitted
        Expression expr = model.addExpression("postSolve");
        assertNotNull(expr, "Adding an expression after the model has been solved should not throw");
    }

    @Test
    void testAddExpressionToSnapshotIsAllowed() {
        // Create a snapshot (read‑only view) of the model
        ExpressionsBasedModel snapshot = model.snapshot();
        // Adding an expression to the snapshot should be permitted (snapshot is a mutable copy)
        Expression expr = snapshot.addExpression("snapshotExpr");
        assertNotNull(expr, "Adding an expression to a snapshot should not throw");
        assertSame(expr, snapshot.getExpression("snapshotExpr"), "The newly added expression must be retrievable from the snapshot");
    }
}
