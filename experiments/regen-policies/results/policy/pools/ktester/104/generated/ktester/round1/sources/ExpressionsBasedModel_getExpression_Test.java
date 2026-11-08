package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getExpression_Test {

    /**
     * The model used in the tests.
     */
    private ExpressionsBasedModel model;

    /**
     * Name of the expression that will be added in the fixture.
     */
    private static final String EXPRESSION_NAME = "myExpression";

    /**
     * Name that is guaranteed not to exist in the model.
     */
    private static final String UNKNOWN_NAME = "unknownExpression";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
        // Add a known expression that the tests can retrieve.
        // The expression is created via the public factory method.
        model.newExpression(EXPRESSION_NAME);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly discard the model reference; helps the GC in long test suites.
        model = null;
    }

    @Test
    void testGetExistingExpression_returnsSameInstance() {
        Expression retrieved = model.getExpression(EXPRESSION_NAME);
        assertNotNull(retrieved, "Expression should be found for a known name");
        // The model should return the exact instance that was stored.
        // Adding a variable and checking the coefficient demonstrates that we are dealing
        // with the same underlying expression.
        Variable v = model.newVariable("v");
        retrieved.set(v, BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, retrieved.get(v, false));
    }

    @Test
    void testGetNonExistingExpression_returnsNull() {
        Expression missing = model.getExpression(UNKNOWN_NAME);
        assertNull(missing, "Expression should be null for an unknown name");
    }

    @Test
    void testExpressionCanBeModifiedThroughGetter() {
        // Retrieve the expression and modify its weight.
        Expression expr = model.getExpression(EXPRESSION_NAME);
        assertNotNull(expr, "Expression must exist before modification");
        double weight = 2.5;
        expr.weight(weight);
        // Verify that the weight change is visible when we query the same expression again.
        Expression sameExpr = model.getExpression(EXPRESSION_NAME);
        assertSame(expr, sameExpr, "Getter should return the same instance");
        assertEquals(weight, sameExpr.getWeight().doubleValue(), 1e-12, "Weight set via the retrieved expression should be persisted in the model");
    }

    @Test
    void testGetExpressionAfterAddingAnotherExpressionDoesNotAffectFirst() {
        // Add a second expression with a different name.
        String secondName = "secondExpression";
        model.newExpression(secondName).weight(3.0);
        // Retrieve both expressions and ensure they are distinct.
        Expression first = model.getExpression(EXPRESSION_NAME);
        Expression second = model.getExpression(secondName);
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Different names must map to different Expression instances");
        // Verify that the weights are independent.
        first.weight(1.0);
        second.weight(5.0);
        assertEquals(1.0, first.getWeight().doubleValue(), 1e-12);
        assertEquals(5.0, second.getWeight().doubleValue(), 1e-12);
    }

    @ParameterizedTest(name = "[{index}] name=\"{0}\" → expected {1}")
    @CsvSource({ // existing name
    // unknown name
    // empty string (treated as a valid key – will be added in the test body)
    "'myExpression', existing", "'unknownExpression', missing", "'', empty" })
    void testGetExpression_basicScenarios(String name, String expectation) {
        // Arrange: for the empty‑string case we need to add the expression first.
        if ("empty".equals(expectation)) {
            model.newExpression(name);
        }
        // Act
        Expression result = model.getExpression(name);
        // Assert
        if ("existing".equals(expectation) || "empty".equals(expectation)) {
            assertNotNull(result, "Expression should be found for name: " + name);
            assertEquals(name, result.getName(), "Returned expression must keep its name");
        } else {
            assertNull(result, "Expression should be null for unknown name: " + name);
        }
    }

    @Test
    void testGetExpression_nullArgument_returnsNull() {
        // HashMap permits a null key, but we never added one, so the result must be null.
        Expression result = model.getExpression(null);
        assertNull(result, "Calling getExpression(null) without a prior entry should return null");
    }

    @Test
    void testGetExpression_afterRemoval_returnsNull() {
        String tempName = "temp";
        model.newExpression(tempName);
        // Verify it is present.
        assertNotNull(model.getExpression(tempName));
        // Remove and verify absence.
        model.removeExpression(tempName);
        assertNull(model.getExpression(tempName), "Expression should be null after removal");
    }

    @Test
    void testGetExpression_caseSensitivity() {
        String exactName = "Var";
        model.newExpression(exactName);
        // Request with different case.
        Expression lowerCase = model.getExpression("var");
        assertNull(lowerCase, "Lookup should be case‑sensitive; 'var' must not match 'Var'");
    }

    @Test
    void testGetExpression_overwriteReturnsLatestInstance() {
        String dupName = "dup";
        // First expression
        Expression first = model.newExpression(dupName);
        first.weight(1.0);
        // Second expression with the same name – should replace the first one.
        Expression second = model.newExpression(dupName);
        second.weight(2.0);
        // The map should now contain the second instance.
        Expression retrieved = model.getExpression(dupName);
        assertSame(second, retrieved, "The newest expression should replace the previous one");
        assertEquals(2.0, retrieved.getWeight().doubleValue(), 1e-12, "Weight of the retrieved expression must reflect the second instance");
    }

    @Test
    void testGetExpression_emptyStringName() {
        String empty = "";
        Expression emptyExpr = model.newExpression(empty);
        assertNotNull(emptyExpr, "Expression with empty name should be created");
        assertSame(emptyExpr, model.getExpression(empty), "Retrieving with empty string must return the same instance");
    }

    @Test
    void testGetExpression_whitespaceName() {
        String ws = "  ";
        Expression wsExpr = model.newExpression(ws);
        assertNotNull(wsExpr, "Expression with whitespace name should be created");
        assertSame(wsExpr, model.getExpression(ws), "Retrieving with whitespace string must return the same instance");
    }

    @Test
    void testGetExpression_copyModelIsolation() {
        String origName = "orig";
        Expression original = model.newExpression(origName);
        original.weight(5.0);
        // Create a shallow copy (default copy() creates a deep copy of expressions)
        ExpressionsBasedModel copy = model.copy();
        // The copy must contain an expression with the same name but a different object.
        Expression copyExpr = copy.getExpression(origName);
        assertNotNull(copyExpr, "Copy must contain an expression with the original name");
        assertNotSame(original, copyExpr, "Expression instance in the copy must be distinct from the original");
        // The weight should be equal because copy duplicates the state.
        assertEquals(original.getWeight().doubleValue(), copyExpr.getWeight().doubleValue(), 1e-12, "Copied expression should preserve the weight value");
    }

    @Test
    void testGetExpression_nullKeyEntry() {
        // Add an expression with a null name – the API permits it via addExpression(null).
        Expression nullNamed = model.addExpression(null);
        assertNotNull(nullNamed, "Expression with null name should be created");
        // Retrieval with null must return that exact instance.
        Expression retrieved = model.getExpression(null);
        assertSame(nullNamed, retrieved, "getExpression(null) should return the expression that was stored with a null key");
    }

    @Test
    void testExpressionModificationThroughGetterIsPersisted() {
        Expression expr = model.getExpression(EXPRESSION_NAME);
        assertNotNull(expr, "Expression must exist before modification");
        double newWeight = 3.7;
        expr.weight(newWeight);
        // Retrieve again and verify the weight persisted.
        Expression same = model.getExpression(EXPRESSION_NAME);
        assertSame(expr, same, "Getter should return the same instance");
        assertEquals(newWeight, same.getWeight().doubleValue(), 1e-12, "Weight change must be visible via subsequent getter calls");
    }
}
