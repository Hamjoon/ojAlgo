package org.ojalgo.optimisation;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ExpressionsBasedModel_addExpression_2_Test {

    private ExpressionsBasedModel model;

//     @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model instance.
        model = new ExpressionsBasedModel();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the model reference to aid garbage collection.
        model = null;
    }

//     static Stream<Arguments> validNamesProvider() {
//         return Stream.of(// normal name
//         "profit", // leading / trailing whitespace
//         "   demand   ", // special Unicode characters
//         "profit_Δ_µ%$", // very long but still acceptable name
//         "a".repeat(1024));
//     }

    private void setup() {
        model = new ExpressionsBasedModel();
    }

    private void teardown() {
        model = null;
    }

    private static void assertThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        try {
            action.run();
            assert false : "Expected exception: " + expected.getSimpleName() + ". " + message;
        } catch (Throwable actual) {
            if (!expected.isInstance(actual)) {
                assert false : "Unexpected exception type. Expected: " + expected.getSimpleName() + ", got: " + actual.getClass().getSimpleName() + ". " + message;
            }
        }
    }

    private void runAllTests() {
        // Each test runs in a fresh model instance to avoid cross‑test interference.
        setup();
        testAddExpressionReturnsExpressionWithGivenName();
        teardown();
        setup();
        testExpressionIsRegisteredInModel();
        teardown();
        setup();
        testAddingMultipleExpressionsWithDifferentNames();
        teardown();
        setup();
        testAddExpressionDoesNotAffectExistingVariables();
        teardown();
        setup();
        testAddExpressionValidNames();
        teardown();
        setup();
        testAddExpressionNullName();
        teardown();
        setup();
        testAddExpressionEmptyName();
        teardown();
        setup();
        testAddExpressionWhitespaceOnlyName();
        teardown();
        setup();
        testAddExpressionDuplicateName();
        teardown();
        setup();
        testAddExpressionNameWithNullChar();
        teardown();
        setup();
        testAddExpressionReservedKeyword();
        teardown();
        setup();
        testAddExpressionExcessivelyLongName();
        teardown();
        setup();
        testAddExpressionAfterDispose();
        teardown();
        setup();
        testAddExpressionAfterSnapshot();
        teardown();
        setup();
        testCountConsistencyAfterDuplicateAttempt();
        teardown();
        setup();
        testRetrievalViaGetExpression();
        teardown();
    }

    public static void main(String[] args) {
        new ExpressionsBasedModelAddExpressionTest().runAllTests();
        System.out.println("All ExpressionsBasedModel.addExpression tests passed.");
    }

//     @Test
//     void testAddExpressionReturnsExpressionWithGivenName() {
//         String exprName = "myExpression";
//         Expression expr = model.addExpression(exprName);
//         assert expr != null : "addExpression should return a non‑null Expression";
//         assert exprName.equals(expr.getName()) : "Expression name should match the supplied name";
//     }

//     @Test
//     void testExpressionIsRegisteredInModel() {
//         String exprName = "constraint1";
//         Expression created = model.addExpression(exprName);
//         Expression retrieved = model.getExpression(exprName);
//         assert retrieved != null : "Expression should be retrievable by its name";
//         assert created == retrieved : "Retrieved expression should be the same instance that was added";
//         Set<String> names = model.expressions().map(Expression::getName).collect(Collectors.toSet());
//         assert names.contains(exprName) : "Model's expression stream must contain the added expression name";
//     }

//     @Test
//     void testAddingMultipleExpressionsWithDifferentNames() {
//         String nameA = "exprA";
//         String nameB = "exprB";
//         Expression exprA = model.addExpression(nameA);
//         Expression exprB = model.addExpression(nameB);
//         assert exprA != null && exprB != null : "Both expressions must be non‑null";
//         assert exprA != exprB : "Expressions with different names must be distinct objects";
//         assert model.getExpression(nameA) == exprA : "Expression A should be reachable via the model";
//         assert model.getExpression(nameB) == exprB : "Expression B should be reachable via the model";
//         Set<String> names = model.expressions().map(Expression::getName).collect(Collectors.toSet());
//         assert names.size() == 2 : "Model should contain exactly two expressions";
//         assert names.containsAll(Set.of(nameA, nameB)) : "Both expression names must be present";
//     }

//     @Test
//     void testAddExpressionDoesNotAffectExistingVariables() {
//         Variable var = model.addVariable("x");
//         var.lower(0).upper(10).weight(1);
//         String exprName = "objective";
//         Expression expr = model.addExpression(exprName);
//         expr.set(var, 2).weight(1);
//         Optimisation.Result result = model.minimise();
//         // The variable should be at its lower bound (0) because we minimise 2*x.
//         assert Math.abs(result.doubleValue(0) - 0.0) < 1e-12 : "Variable should be at its lower bound after minimisation";
//         // Objective value should be zero.
//         assert Math.abs(result.getValue() - 0.0) < 1e-12 : "Objective value should be zero";
//     }

    /**
     * Normal operation – a valid, non‑null, non‑empty name.
     */
//     @ParameterizedTest(name = "{index}: addExpression({0})")
//     @MethodSource("validNamesProvider")
//     void testAddExpressionValidNames(String name) {
//         for (String name : (Iterable<String>) validNamesProvider()::iterator) {
//             int before = model.countExpressions();
//             Expression expr = model.addExpression(name);
//             assert expr != null : "Expression must not be null for name: " + name;
//             assert name.equals(expr.getName()) : "Returned expression must preserve the supplied name";
//             assert model.countExpressions() == before + 1 : "Expression count must increase by one after successful addition";
//             assert model.getExpression(name) == expr : "Expression should be retrievable via getExpression";
//         }
//     }

    /**
     * Null name must result in NPE.
     */
//     @Test
//     void testAddExpressionNullName() {
//         assertThrows(NullPointerException.class, () -> model.addExpression(null), "Adding an expression with a null name should throw NullPointerException");
//     }

    /**
     * Empty string is illegal.
     */
//     @Test
//     void testAddExpressionEmptyName() {
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression(""), "Empty name should trigger IllegalArgumentException");
//     }

    /**
     * Name consisting only of whitespace is illegal.
     */
//     @Test
//     void testAddExpressionWhitespaceOnlyName() {
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression("   "), "Whitespace‑only name should trigger IllegalArgumentException");
//     }

    /**
     * Duplicate name must be rejected and must not change the expression count.
     */
//     @Test
//     void testAddExpressionDuplicateName() {
//         String dup = "capacity";
//         Expression first = model.addExpression(dup);
//         assert first != null : "First expression must be created";
//         int countAfterFirst = model.countExpressions();
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression(dup), "Adding a duplicate expression name must throw IllegalArgumentException");
//         assert model.countExpressions() == countAfterFirst : "Expression count must remain unchanged after a failed duplicate addition";
//     }

    /**
     * Name containing a null character is illegal.
     */
//     @Test
//     void testAddExpressionNameWithNullChar() {
//         String nameWithNull = "abc\0def";
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression(nameWithNull), "Name containing a null character should trigger IllegalArgumentException");
//     }

    /**
     * Reserved keyword "objective" is not allowed as a user‑defined expression name.
     */
//     @Test
//     void testAddExpressionReservedKeyword() {
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression("objective"), "Using the reserved keyword 'objective' should trigger IllegalArgumentException");
//     }

    /**
     * Excessively long name (10 000 characters) must be rejected.
     */
//     @Test
//     void testAddExpressionExcessivelyLongName() {
//         String longName = "a".repeat(10_000);
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression(longName), "Name longer than the implementation limit should trigger IllegalArgumentException");
//     }

    /**
     * Adding an expression after the model has been disposed must fail.
     */
//     @Test
//     void testAddExpressionAfterDispose() {
//         model.dispose();
//         assertThrows(IllegalStateException.class, () -> model.addExpression("disposedExpression"), "Adding an expression to a disposed model must throw IllegalStateException");
//     }

    /**
     * Adding an expression to a snapshot (read‑only) model must fail.
     */
//     @Test
//     void testAddExpressionAfterSnapshot() {
//         ExpressionsBasedModel frozen = model.snapshot();
//         assertThrows(IllegalStateException.class, () -> frozen.addExpression("frozenExpression"), "Adding an expression to a snapshot (frozen) model must throw IllegalStateException");
//     }

    /**
     * Verify that countExpressions() stays consistent after a duplicate‑name attempt.
     */
//     @Test
//     void testCountConsistencyAfterDuplicateAttempt() {
//         String name = "resource";
//         Expression first = model.addExpression(name);
//         assert first != null : "First expression must be created";
//         int countAfterFirst = model.countExpressions();
//         assertThrows(IllegalArgumentException.class, () -> model.addExpression(name), "Second attempt – expect exception");
//         assert model.countExpressions() == countAfterFirst : "Expression count must not increase after duplicate addition attempt";
//     }

    /**
     * Retrieval test – the expression returned by addExpression must be identical to that returned by getExpression.
     */
//     @Test
//     void testRetrievalViaGetExpression() {
//         String name = "revenue";
//         Expression added = model.addExpression(name);
//         Expression retrieved = model.getExpression(name);
//         assert added == retrieved : "Expression obtained via getExpression must be the same instance as added";
//         assert model.countExpressions() == 1 : "Count should be 1 after adding a single expression";
//     }
}