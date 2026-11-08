package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_countExpressions_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        model = null;
    }

    /**
     * Helper that adds a given number of unnamed expressions.
     */
    private void addUnnamedExpressions(int count) {
        for (int i = 0; i < count; i++) {
            model.addExpression();
        }
    }

    /**
     * Helper that adds a given number of named expressions using a predictable pattern.
     */
    private void addNamedExpressions(int count) {
        for (int i = 0; i < count; i++) {
            model.addExpression("expr" + i);
        }
    }

    /**
     * Helper that adds a given number of variables (does not affect expression count).
     */
    private void addVariables(int count) {
        for (int i = 0; i < count; i++) {
            model.addVariable("var" + i);
        }
    }

    static Stream<Arguments> basicCountProvider() {
        return Stream.of(// No expressions added
        Arguments.of(0, 0, "A newly created model has no expressions; countExpressions() should return 0."), // Add 1 expression
        Arguments.of(1, 1, "After adding a single expression via addExpression(), countExpressions() should return 1."), // Add 5 expressions
        Arguments.of(5, 5, "After adding five expressions, countExpressions() should return 5."), // Add 3 expressions then remove one (named \"expr1\")
        Arguments.of(3, 2, "Add three expressions, then remove one by name; countExpressions() should reflect the remaining two."), // Add 2 expressions and 4 variables – variables must not affect the count
        Arguments.of(2, 2, "Adding variables does not affect the expression count; after adding two expressions and four variables, countExpressions() should still be 2."));
    }

    static Stream<Arguments> removalEdgeProvider() {
        return Stream.of(// Two expressions, attempt to remove a non‑existent one
        Arguments.of(2, "nonexistent", 2, "Attempt to remove an expression that does not exist; the count should remain unchanged."), // One expression, remove it successfully
        Arguments.of(1, "expr0", 0, "Add a single expression and then remove it; countExpressions() should return 0."));
    }

    @Test
    void testInitialCountIsZero() {
        assertEquals(0, model.countExpressions(), "A newly created model should contain no expressions.");
    }

    @Test
    void testCountIncrementsWhenAddingExpressions() {
        // Add first expression without a name
        model.addExpression();
        assertEquals(1, model.countExpressions(), "Count should be 1 after adding a single unnamed expression.");
        // Add a named expression
        model.addExpression("first");
        assertEquals(2, model.countExpressions(), "Count should be 2 after adding a second expression.");
        // Add another unnamed expression
        model.addExpression();
        assertEquals(3, model.countExpressions(), "Count should be 3 after adding a third expression.");
    }

    @Test
    void testCountDecrementsWhenRemovingExpressions() {
        // Populate the model with three expressions
        model.addExpression("e1");
        model.addExpression("e2");
        model.addExpression("e3");
        assertEquals(3, model.countExpressions());
        // Remove one expression by name
        model.removeExpression("e2");
        assertEquals(2, model.countExpressions(), "Count should decrease after removing an expression.");
        // Remove another expression
        model.removeExpression("e1");
        assertEquals(1, model.countExpressions(), "Count should reflect the remaining expressions.");
    }

    @Test
    void testCountIsPreservedInCopy() {
        // Build a model with two expressions
        model.addExpression("copy1");
        model.addExpression("copy2");
        assertEquals(2, model.countExpressions());
        // Create a shallow copy (the copy constructor is package‑private,
        // so we use the public copy() method)
        ExpressionsBasedModel copy = model.copy();
        assertEquals(2, copy.countExpressions(), "A copy of the model should have the same number of expressions as the original.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicCountProvider")
    void testBasicCountExpressions(int addExpressionCalls, int expected, String description) {
        // Add the requested number of *named* expressions so we can later remove by name if needed.
        addNamedExpressions(addExpressionCalls);
        // If the test case also mentions variable addition, we add them here.
        if (description.contains("variables")) {
            addVariables(4);
        }
        // For the case that also requires a removal, perform it after the adds.
        if (description.contains("remove one by name")) {
            model.removeExpression("expr1");
        }
        assertEquals(expected, model.countExpressions(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("removalEdgeProvider")
    void testRemovalEdgeCases(int addExpressionCalls, String removeName, int expected, String description) {
        addNamedExpressions(addExpressionCalls);
        model.removeExpression(removeName);
        assertEquals(expected, model.countExpressions(), description);
    }

    @Test
    void testDuplicateNameHandling() {
        // First addition
        model.addExpression("dupExpr");
        // Second addition with the same name – should replace the first
        model.addExpression("dupExpr");
        assertEquals(1, model.countExpressions(), "Add two expressions with the same name; the second addition should replace the first, leaving only one expression.");
    }

    @Test
    void testCopyModelIndependence() {
        // Original model with three expressions
        addNamedExpressions(3);
        ExpressionsBasedModel copy = model.copy();
        // Verify copy initially matches original
        assertEquals(3, copy.countExpressions(), "Copy should initially have the same count as the original.");
        // Modify original: remove one expression
        model.removeExpression("expr0");
        // Original count changes, copy must stay unchanged
        assertEquals(2, model.countExpressions(), "Original model count after removal should be 2.");
        assertEquals(3, copy.countExpressions(), "Copy should still report 3 expressions, proving independence.");
    }

    @Test
    void testCopyAndModifyOriginalDoesNotAffectCopy() {
        // Original model with two expressions
        addNamedExpressions(2);
        ExpressionsBasedModel copy = model.copy();
        // Remove an expression from the original
        model.removeExpression("expr0");
        // Original count is now 1, copy must still be 2
        assertEquals(1, model.countExpressions(), "Original model should have 1 expression after removal.");
        assertEquals(2, copy.countExpressions(), "Copy should still contain both original expressions (count 2).");
    }

    @Test
    void testCompleteRemovalViaLoop() {
        // Add four named expressions
        addNamedExpressions(4);
        // Remove each expression one‑by‑one
        for (int i = 0; i < 4; i++) {
            model.removeExpression("expr" + i);
        }
        assertEquals(0, model.countExpressions(), "Add four expressions and then remove each of them one by one; final count should be 0.");
    }
}