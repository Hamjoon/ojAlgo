package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_countExpressions_Test {

    private ExpressionsBasedModel model;

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    /**
     * Clear the reference after each test – helps the GC in long test suites.
     */
    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    /**
     * Provider for the "Adding expressions" group.
     */
    private static Stream<Arguments> addingExpressionsProvider() {
        return Stream.of(Arguments.of(1, "After a single addExpression() call the count must be 1."), Arguments.of(3, "After three addExpression() calls the count must be 3."));
    }

    /**
     * Provider for the "Adding named expressions" group.
     */
    private static Stream<Arguments> addingNamedExpressionsProvider() {
        return Stream.of(// two distinct named expressions
        Arguments.of(new String[] { "a", "b" }, 2, "Two distinct named expressions should increase the count to 2."), // duplicate name replacement: first add "expr1", then add another "expr1" (replaces),
        // then add "expr2". Expected final count = 2.
        Arguments.of(new String[] { "expr1", "expr1", "expr2" }, 2, "Adding a duplicate name replaces the previous one; final count should be 2."));
    }

    /**
     * Provider for the "Removing expressions" group.
     */
    private static Stream<Arguments> removingExpressionsProvider() {
        return Stream.of(// add two, remove one -> count 1
        Arguments.of(new String[] { "r1", "r2" }, "r1", 1, "After removing one of two named expressions the count should be 1."), // add one, remove it -> count 0
        Arguments.of(new String[] { "solo" }, "solo", 0, "After removing the only expression the count should be 0."));
    }

    /**
     * Provider for the "Large number of expressions" group.
     */
    private static Stream<Arguments> largeNumberProvider() {
        return Stream.of(Arguments.of(1000, "Adding 1,000 unnamed expressions should result in count 1,000."));
    }

    /**
     * Provider for the "Copy model (shallow copy)" group.
     */
    private static Stream<Arguments> copyModelProvider() {
        return Stream.of(Arguments.of(4, "Shallow copy should preserve 4 expressions."), Arguments.of(5, "Relaxed copy (copy(true)) should preserve 5 expressions."));
    }

    @Test
    void testInitialCountIsZero() {
        assertEquals(0, model.countExpressions(), "A newly created model must contain no expressions.");
    }

    @Test
    void testCountIncrementsWhenAddingExpressions() {
        // add three unnamed expressions
        model.addExpression();
        model.addExpression();
        model.addExpression();
        assertEquals(3, model.countExpressions(), "countExpressions() should reflect the number of added expressions.");
    }

    @Test
    void testCountWithNamedExpressions() {
        model.addExpression("first");
        model.addExpression("second");
        // unnamed
        model.addExpression();
        assertEquals(3, model.countExpressions(), "Both named and unnamed expressions contribute to the count.");
    }

    @Test
    void testCountDecrementsWhenRemovingExpression() {
        model.addExpression("toBeRemoved");
        model.addExpression("stay");
        assertEquals(2, model.countExpressions(), "Two expressions should be present before removal.");
        model.removeExpression("toBeRemoved");
        assertEquals(1, model.countExpressions(), "Removing an expression must decrease the count.");
    }

    @Test
    void testCountAfterCopyPreservesNumberOfExpressions() {
        // Build a source model with a few expressions and variables
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("expr1").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE);
        model.newExpression("expr2").set(x, BigDecimal.valueOf(2));
        // unnamed expression
        model.newExpression();
        assertEquals(3, model.countExpressions(), "Source model should have three expressions before copying.");
        // Perform a shallow copy (the public copy() method creates a deep copy,
        // but the protected constructor used by copy(boolean,boolean) is exercised
        // indirectly via the public API).
        ExpressionsBasedModel copy = model.copy();
        assertEquals(3, copy.countExpressions(), "A copied model must contain the same number of expressions as the original.");
    }

    @Test
    void testCountAfterRelaxedCopyPreservesExpressions() {
        // Populate model with five expressions
        for (int i = 0; i < 5; i++) {
            model.addExpression("e" + i);
        }
        assertEquals(5, model.countExpressions(), "Source model should have five expressions before relaxed copy.");
        // copy(true) creates a relaxed copy that still keeps all expressions
        ExpressionsBasedModel relaxedCopy = model.copy(true);
        assertEquals(5, relaxedCopy.countExpressions(), "Relaxed copy must retain the same number of expressions.");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("addingExpressionsProvider")
    void testAddingExpressionsParameterized(int expectedCount, String description) {
        for (int i = 0; i < expectedCount; i++) {
            model.addExpression();
        }
        assertEquals(expectedCount, model.countExpressions(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("addingNamedExpressionsProvider")
    void testAddingNamedExpressionsParameterized(String[] names, int expectedCount, String description) {
        for (String name : names) {
            model.addExpression(name);
        }
        assertEquals(expectedCount, model.countExpressions(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("removingExpressionsProvider")
    void testRemovingExpressionsParameterized(String[] toAdd, String toRemove, int expectedCount, String description) {
        for (String name : toAdd) {
            model.addExpression(name);
        }
        model.removeExpression(toRemove);
        assertEquals(expectedCount, model.countExpressions(), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("largeNumberProvider")
    void testLargeNumberOfExpressions(int amount, String description) {
        for (int i = 0; i < amount; i++) {
            model.addExpression();
        }
        assertEquals(amount, model.countExpressions(), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("copyModelProvider")
    void testCopyModelPreservesExpressionCount(int exprCount, String description) {
        // Populate the original model
        for (int i = 0; i < exprCount; i++) {
            model.addExpression("e" + i);
        }
        assertEquals(exprCount, model.countExpressions(), "Pre‑condition: source model must contain the expected number of expressions.");
        // Perform copy – for the second case we use the relaxed copy variant.
        ExpressionsBasedModel copy = (exprCount == 5) ? model.copy(true) : model.copy();
        assertEquals(exprCount, copy.countExpressions(), description);
    }
}
