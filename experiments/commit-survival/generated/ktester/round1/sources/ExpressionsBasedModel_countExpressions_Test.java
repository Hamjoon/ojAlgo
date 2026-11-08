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
        // The model does not hold external resources that need explicit cleanup
        model = null;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> duplicateNamedProvider() {
        return Stream.of(Arguments.of("dup", 1), Arguments.of("unique", 2));
    }

    @Test
    void testInitialCountIsZero() {
        assertEquals(0, model.countExpressions(), "A newly created model should contain no expressions.");
    }

    @Test
    void testCountAfterAddingUnnamedExpressions() {
        // Add first expression
        Expression e1 = model.addExpression();
        assertNotNull(e1, "Expression should be created.");
        assertEquals(1, model.countExpressions(), "Model should contain exactly one expression after first addition.");
        // Add second expression
        Expression e2 = model.addExpression();
        assertNotNull(e2, "Second expression should be created.");
        assertEquals(2, model.countExpressions(), "Model should contain exactly two expressions after second addition.");
    }

    @Test
    void testCountAfterAddingNamedExpressions() {
        // Add a named expression
        Expression named = model.addExpression("myExpr");
        assertNotNull(named, "Named expression should be created.");
        assertEquals(1, model.countExpressions(), "Model should contain one expression after adding a named expression.");
        // Add another unnamed expression
        model.addExpression();
        assertEquals(2, model.countExpressions(), "Model should contain two expressions after adding another expression.");
    }

    @Test
    void testAddingVariablesDoesNotAffectExpressionCount() {
        // Add some variables only
        model.addVariable();
        model.addVariable("x");
        assertEquals(0, model.countExpressions(), "Adding variables alone must not increase the expression count.");
        // Now add an expression and verify the count increments correctly
        model.addExpression();
        assertEquals(1, model.countExpressions(), "Expression count should be 1 after adding a single expression.");
    }

    /**
     * Group: basic scenarios
     */
    @Test
    void testBasicScenario_AddSingleUnnamedExpression() {
        model.addExpression();
        assertEquals(1, model.countExpressions(), "After adding a single unnamed expression the count should be 1.");
    }

    @Test
    void testBasicScenario_AddThreeMixedExpressions() {
        // unnamed
        model.addExpression();
        // named
        model.addExpression("e1");
        // named
        model.addExpression("e2");
        assertEquals(3, model.countExpressions(), "Three mixed (named & unnamed) expressions should yield count 3.");
    }

    @Test
    void testBasicScenario_DuplicateNamedExpressionReplaces() {
        // first insertion
        model.addExpression("dup");
        // second insertion – should replace the first
        model.addExpression("dup");
        assertEquals(1, model.countExpressions(), "Adding a second expression with the same name must replace the first, keeping count 1.");
    }

    @Test
    void testBasicScenario_RemoveOneExpression() {
        model.addExpression("a");
        model.addExpression("b");
        model.addExpression("c");
        assertEquals(3, model.countExpressions(), "Pre‑condition: three expressions added.");
        model.removeExpression("b");
        assertEquals(2, model.countExpressions(), "After removing one expression the count should be 2.");
    }

    @Test
    void testBasicScenario_ShallowCopyRetainsExpressionCount() {
        model.addExpression();
        model.addExpression();
        assertEquals(2, model.countExpressions(), "Pre‑condition: original model has 2 expressions.");
        // shallow copy
        ExpressionsBasedModel copy = model.copy(true, false);
        assertEquals(2, copy.countExpressions(), "Shallow copy should contain the same number of expressions as the original.");
        assertEquals(2, model.countExpressions(), "Original model must remain unchanged after shallow copy.");
    }

    @Test
    void testBasicScenario_CopyUnchangedAfterOriginalRemoval() {
        model.addExpression("e1");
        model.addExpression("e2");
        // copy before removal
        ExpressionsBasedModel copy = model.copy(true, false);
        assertEquals(2, copy.countExpressions(), "Copy should initially have 2 expressions.");
        // mutate original
        model.removeExpression("e1");
        assertEquals(1, model.countExpressions(), "Original model should now have 1 expression after removal.");
        assertEquals(2, copy.countExpressions(), "Copy must still report the original count of 2 expressions.");
    }

    /**
     * Group: duplicate and distinct named expressions
     */
    @Test
    void testDuplicateAndDistinctNamedExpressions() {
        // named
        model.addExpression("e1");
        // unnamed
        model.addExpression();
        // another named
        model.addExpression("e2");
        // duplicate name – replaces first
        model.addExpression("e1");
        assertEquals(3, model.countExpressions(), "After adding duplicate name that replaces, total distinct expressions should be 3.");
    }

    /**
     * Group: removing a non‑existent expression
     */
    @Test
    void testRemoveNonExistentExpressionHasNoEffect() {
        model.addExpression("a");
        model.addExpression("b");
        assertEquals(2, model.countExpressions(), "Pre‑condition: two expressions added.");
        // name not present
        model.removeExpression("ghost");
        assertEquals(2, model.countExpressions(), "Removing a non‑existent expression must leave the count unchanged.");
    }

    /**
     * Group: deep copy retains expression count
     */
    @Test
    void testDeepCopyRetainsExpressionCount() {
        model.addExpression("x1");
        model.addExpression("x2");
        model.addExpression("x3");
        assertEquals(3, model.countExpressions(), "Pre‑condition: three expressions added.");
        // deep copy (parameter‑less)
        ExpressionsBasedModel deepCopy = model.copy();
        assertEquals(3, deepCopy.countExpressions(), "Deep copy should contain the same number of expressions as the original.");
        assertEquals(3, model.countExpressions(), "Original model should still report 3 expressions.");
    }

    /**
     * Group: addExpression with empty string name
     */
    @Test
    void testAddExpressionWithEmptyStringName() {
        model.addExpression("");
        assertEquals(1, model.countExpressions(), "Expression added with empty string name should be counted like any other.");
    }

    /**
     * Group: addExpression with null name throws
     */
    @Test
    void testAddExpressionWithNullNameThrows() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> model.addExpression(null), "Calling addExpression(null) should throw NullPointerException.");
        // Ensure the model state is unchanged
        assertEquals(0, model.countExpressions(), "Expression count must remain 0 after the exception.");
    }

    /**
     * Group: add and then remove all expressions sequentially
     */
    @Test
    void testAddAndRemoveAllExpressionsSequentially() {
        // Add five distinct expressions (mix of named & unnamed)
        model.addExpression("e1");
        model.addExpression("e2");
        model.addExpression("e3");
        // unnamed
        model.addExpression();
        // unnamed
        model.addExpression();
        assertEquals(5, model.countExpressions(), "Pre‑condition: five expressions added.");
        // Remove them one by one
        model.removeExpression("e1");
        model.removeExpression("e2");
        model.removeExpression("e3");
        // Unnamed expressions have generated names; retrieve them via the expressions() stream
        model.expressions().map(Expression::getName).forEach(name -> model.removeExpression(name));
        assertEquals(0, model.countExpressions(), "After removing all expressions the model should be empty.");
    }
}