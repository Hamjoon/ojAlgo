package org.ojalgo.optimisation;

import java.math.BigDecimal;

class ExpressionsBasedModel_getExpression_Test {

    /**
     * Model used in the tests – created fresh for each test method.
     */
    private ExpressionsBasedModel model;

    /**
     * Name of the expression that will be added in {@link #setupBeforeEach()}.
     */
    private static final String EXPRESSION_NAME = "myExpr";

    /**
     * A generic variable used for building simple expressions.
     */
    private Variable x;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a new model for every test to guarantee isolation.
        model = new ExpressionsBasedModel();
        // Add a simple expression with a known name.
        // The expression is a constraint: 2 * x = 10
        x = model.newVariable("x").lower(0).upper(100).weight(BigDecimal.ZERO);
        Expression expr = model.newExpression(EXPRESSION_NAME).lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(10));
        expr.set(x, BigDecimal.valueOf(2));
    }

//     @AfterEach
    void teardownAfterEach() {
        // Explicitly drop references – helps the garbage collector in long test suites.
        model = null;
    }

    public static void main(String[] args) {
        // -----------------------------------------------------------------
        // 1) Build a tiny model with a single variable and a single expression
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable x : 0 ≤ x ≤ 100, not part of the objective (weight = 0)
        Variable x = model.newVariable("x").lower(0).upper(100).weight(BigDecimal.ZERO);
        // Expression myExpr : 2·x = 10
        Expression expr = model.newExpression(EXPRESSION_NAME).lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(10));
        expr.set(x, BigDecimal.valueOf(2));
        // -----------------------------------------------------------------
        // 2) Verify that an existing expression can be retrieved
        // -----------------------------------------------------------------
        Expression retrieved = model.getExpression(EXPRESSION_NAME);
        assert retrieved != null : "Expression should be present in the model";
        assert EXPRESSION_NAME.equals(retrieved.getName()) : "Retrieved expression must have the expected name";
        // -----------------------------------------------------------------
        // 3) Verify that a non‑existent expression returns null
        // -----------------------------------------------------------------
        assert model.getExpression("doesNotExist") == null : "Non‑existent expression should return null";
        // -----------------------------------------------------------------
        // 4) Ensure the same instance is returned on repeated calls
        // -----------------------------------------------------------------
        Expression firstCall = model.getExpression(EXPRESSION_NAME);
        Expression secondCall = model.getExpression(EXPRESSION_NAME);
        assert firstCall == secondCall : "Multiple calls must return the same instance";
        // -----------------------------------------------------------------
        // 5) Null name must be handled safely
        // -----------------------------------------------------------------
        assert model.getExpression(null) == null : "Null name should safely return null";
        // -----------------------------------------------------------------
        // 6) Names are case‑sensitive
        // -----------------------------------------------------------------
        model.newExpression("expr1");
        assert model.getExpression("EXPR1") == null : "Names are case‑sensitive; different case must return null";
        // -----------------------------------------------------------------
        // 7) Retrieval after removal
        // -----------------------------------------------------------------
        String tempName = "temp";
        model.newExpression(tempName);
        model.removeExpression(tempName);
        assert model.getExpression(tempName) == null : "Expression removed via removeExpression should no longer be retrievable";
        // -----------------------------------------------------------------
        // 8) Parameterised‑style checks for a few different names
        // -----------------------------------------------------------------
        String[] names = { "expr1", "", "@!#$", "  spaced  " };
        for (String name : names) {
            Expression e = model.newExpression(name).lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(20));
            e.set(x, BigDecimal.valueOf(2));
            Expression r = model.getExpression(name);
            assert r != null : "Expression should be present for name: \"" + name + "\"";
            assert r == e : "Returned instance must be the exact object stored in the model";
        }
        System.out.println("All checks passed.");
    }

    /**
     * Verify that an existing expression can be retrieved by its name.
     */
//     @Test
//     void testGetExistingExpression() {
//         Expression retrieved = model.getExpression(EXPRESSION_NAME);
//         assertNotNull(retrieved, "Expression should be present in the model");
//         assertEquals(EXPRESSION_NAME, retrieved.getName(), "Retrieved expression must have the expected name");
//     }

    /**
     * Verify that requesting a non‑existent expression returns {@code null}.
     */
//     @Test
//     void testGetNonExistingExpression() {
//         Expression retrieved = model.getExpression("doesNotExist");
//         assertNull(retrieved, "Non‑existent expression should return null");
//     }

    /**
     * Ensure that the returned instance is the exact same object stored inside the model.
     */
//     @Test
//     void testReturnedInstanceIdentity() {
//         Expression firstCall = model.getExpression(EXPRESSION_NAME);
//         Expression secondCall = model.getExpression(EXPRESSION_NAME);
//         assertSame(firstCall, secondCall, "Multiple calls must return the same instance");
//     }

//     @ParameterizedTest(name = "Retrieve existing expression with name \"{0}\"")
//     @CsvSource({ // empty string
//     // special characters
//     // whitespace inclusive
//     "'expr1'", "''", "'@!#$'", "'  spaced  '" })
//     void testRetrieveExistingExpression(String name) {
//         // Add an expression with the supplied name.
//         Expression expr = model.newExpression(name).lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(20));
//         expr.set(x, BigDecimal.valueOf(2));
//         // Retrieval must succeed and return the same instance.
//         Expression retrieved = model.getExpression(name);
//         assertNotNull(retrieved, "Expression should be present for name: \"" + name + "\"");
//         assertSame(expr, retrieved, "Returned instance must be the exact object stored in the model");
//     }

//     @Test
//     void testRetrieveNonExistingExpression() {
//         Expression retrieved = model.getExpression("unknown");
//         assertNull(retrieved, "Non‑existent name should yield null");
//     }

//     @Test
//     void testRetrieveWithNullName() {
//         // HashMap.get(null) returns null – the method should not throw.
//         Expression retrieved = model.getExpression(null);
//         assertNull(retrieved, "Null name should safely return null");
//     }

//     @Test
//     void testCaseSensitivity() {
//         // Add lower‑case name.
//         model.newExpression("expr1");
//         // Query with different case.
//         Expression retrieved = model.getExpression("EXPR1");
//         assertNull(retrieved, "Names are case‑sensitive; different case must return null");
//     }

//     @Test
//     void testRetrieveAfterRemoval() {
//         String name = "temp";
//         model.newExpression(name);
//         // Remove the expression.
//         model.removeExpression(name);
//         // Retrieval must now be null.
//         Expression retrieved = model.getExpression(name);
//         assertNull(retrieved, "Expression removed via removeExpression should no longer be retrievable");
//     }

//     @Test
//     void testReturnedInstanceIdentityMultipleCalls() {
//         String name = "myExpr";
//         model.newExpression(name);
//         Expression firstCall = model.getExpression(name);
//         Expression secondCall = model.getExpression(name);
//         assertSame(firstCall, secondCall, "Multiple calls must return the same instance");
//     }
}