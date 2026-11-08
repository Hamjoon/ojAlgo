package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.util.stream.IntStream;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addExpression_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the model reference (helps GC in large test suites)
        model = null;
    }

    @Test
    void testAddExpressionCreatesExpressionWithDefaultName() {
        // When
        Expression expr = model.addExpression();
        // Then
        assertNotNull(expr, "addExpression() should return a non‑null Expression");
        // The default name is "EXPR" followed by the current number of expressions (which is 0 before addition)
        String expectedName = "EXPR0";
        assertEquals(expectedName, expr.getName(), "Expression name should follow the default naming scheme");
        // The expression must be present in the model's collection
        assertSame(expr, model.getExpression(expectedName), "Expression should be retrievable by its name");
    }

    @Test
    void testAddExpressionIncrementsExpressionCount() {
        // Initial state
        assertEquals(0, model.countExpressions(), "Model should start with zero expressions");
        // Add first expression
        Expression first = model.addExpression();
        assertEquals(1, model.countExpressions(), "Expression count should be 1 after first addition");
        assertEquals("EXPR0", first.getName());
        // Add second expression
        Expression second = model.addExpression();
        assertEquals(2, model.countExpressions(), "Expression count should be 2 after second addition");
        assertEquals("EXPR1", second.getName());
        // Verify both expressions are stored and distinct
        assertNotSame(first, second, "Each call to addExpression() should create a distinct Expression");
        assertSame(first, model.getExpression("EXPR0"));
        assertSame(second, model.getExpression("EXPR1"));
    }

    @Test
    void testExpressionCanBeUsedInModel() {
        // Create a variable to use in the expression
        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        // Add an expression that constrains x to be exactly 5
        Expression expr = model.addExpression().level(BigDecimal.valueOf(5));
        // 1 * x = 5  →  x = 5
        expr.set(x, BigDecimal.ONE);
        // Solve the model
        Optimisation.Result result = model.minimise();
        // Verify the solution satisfies the constraint
        assertTrue(model.validate(result), "Result should satisfy model validation");
        assertEquals(BigDecimal.valueOf(5), result.get(model.indexOf(x)), "Variable x should take the value 5");
    }

    @Test
    void testAddExpressionWithExistingCustomNames() {
        // Add two custom‑named expressions
        Expression a = model.addExpression("CustomA");
        Expression b = model.addExpression("CustomB");
        assertEquals(2, model.countExpressions(), "Model should contain two custom expressions before auto‑naming");
        // Auto‑generated expression should be named "EXPR2"
        Expression auto = model.addExpression();
        assertEquals("EXPR2", auto.getName(), "Auto‑generated name must be based on current map size (2)");
        assertEquals(3, model.countExpressions(), "Expression count should increase to three after auto‑generated addition");
        assertSame(auto, model.getExpression("EXPR2"), "New expression must be retrievable by its generated name");
    }

    @Test
    void testAddExpressionSequentialNaming() {
        Expression e0 = model.addExpression();
        Expression e1 = model.addExpression();
        Expression e2 = model.addExpression();
        assertEquals("EXPR0", e0.getName());
        assertEquals("EXPR1", e1.getName());
        assertEquals("EXPR2", e2.getName());
        assertEquals(3, model.countExpressions(), "Three successive calls must result in three stored expressions");
    }

    @Test
    void testAddExpressionAfterRemovalReusesName() {
        // Create three automatic expressions
        // EXPR0
        Expression e0 = model.addExpression();
        // EXPR1
        Expression e1 = model.addExpression();
        // EXPR2
        Expression e2 = model.addExpression();
        // Remove the middle one
        model.removeExpression(e1.getName());
        assertNull(model.getExpression(e1.getName()), "Removed expression should no longer be present");
        assertEquals(2, model.countExpressions(), "Count after removal must be two");
        // Adding a new expression now generates "EXPR2" again (size is 2)
        Expression eNew = model.addExpression();
        assertEquals("EXPR2", eNew.getName(), "Generated name should be based on current map size (2) → EXPR2");
        // The map still contains only two entries because the key collides and the new expression replaces the old one
        assertEquals(2, model.countExpressions(), "Count should stay at two after replacement of the colliding name");
        assertSame(eNew, model.getExpression("EXPR2"), "The stored expression for EXPR2 must be the newly created one");
        assertNotSame(e2, eNew, "The new expression must be a different instance from the original EXPR2");
    }

    @Test
    void testAddExpressionMultipleCallsStress() {
        // Create 15 expressions
        IntStream.range(0, 15).forEach(i -> model.addExpression());
        assertEquals(15, model.countExpressions(), "After 15 calls countExpressions() must be 15");
        // Verify a few random names
        assertNotNull(model.getExpression("EXPR0"));
        assertNotNull(model.getExpression("EXPR10"));
        assertNotNull(model.getExpression("EXPR14"));
        assertEquals("EXPR10", model.getExpression("EXPR10").getName(), "Retrieved expression must have the correct name");
    }

    @Test
    void testAddExpressionDoesNotAffectExistingVariables() {
        // Add two variables
        model.addVariable("v1");
        model.addVariable("v2");
        assertEquals(2, model.countVariables(), "Two variables should be present before adding an expression");
        // Add an expression
        model.addExpression();
        assertEquals(2, model.countVariables(), "Variable count must remain unchanged after adding an expression");
        assertEquals(1, model.countExpressions(), "One expression should have been added");
    }

    @Test
    void testAddExpressionDuplicateNameAfterRemoval_throwsException() {
        // Create three automatic expressions
        // EXPR0
        model.addExpression();
        // EXPR1
        model.addExpression();
        // EXPR2
        model.addExpression();
        // Remove EXPR1, leaving EXPR0 and EXPR2
        model.removeExpression("EXPR1");
        assertEquals(2, model.countExpressions(), "After removal count must be two");
        // The next auto‑generated name will be "EXPR2" which already exists.
        // According to the specification this should raise a ProgrammingError (runtime exception).
        assertThrows(ProgrammingError.class, () -> model.addExpression(), "Adding an expression that would duplicate an existing auto‑generated name must throw ProgrammingError");
    }

    @Test
    void testAddExpressionAfterDispose_throwsIllegalStateException() {
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.addExpression(), "Calling addExpression() on a disposed model must throw IllegalStateException");
    }
}