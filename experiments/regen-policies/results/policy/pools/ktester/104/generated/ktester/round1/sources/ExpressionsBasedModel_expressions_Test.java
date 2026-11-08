package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_expressions_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // Fresh model for every test case.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        model = null;
        // help GC
        model = null;
    }

    @Test
    void testExpressionsStreamInitiallyEmpty() {
        List<Expression> expressions = model.expressions().collect(Collectors.toList());
        assertTrue(expressions.isEmpty(), "The expressions stream should be empty for a newly created model.");
    }

    @Test
    void testExpressionsStreamAfterAddingExpressions() {
        // Add two distinct expressions with explicit names.
        Expression expr1 = model.newExpression("expr1");
        Expression expr2 = model.newExpression("expr2");
        // Collect the stream into a list for easier assertions.
        List<Expression> expressions = model.expressions().collect(Collectors.toList());
        // Verify the expected size.
        assertEquals(2, expressions.size(), "The expressions stream should contain exactly two elements after adding two expressions.");
        // Verify that both added expressions are present (by name).
        boolean containsExpr1 = expressions.stream().anyMatch(e -> "expr1".equals(e.getName()));
        boolean containsExpr2 = expressions.stream().anyMatch(e -> "expr2".equals(e.getName()));
        assertTrue(containsExpr1, "Stream should contain the expression named 'expr1'.");
        assertTrue(containsExpr2, "Stream should contain the expression named 'expr2'.");
    }

    @Test
    void testExpressionsStreamReflectsSubsequentAdditions() {
        // Initially empty.
        assertTrue(model.expressions().collect(Collectors.toList()).isEmpty(), "Model should start with an empty expressions stream.");
        // Add one expression.
        model.newExpression("first");
        List<Expression> afterFirst = model.expressions().collect(Collectors.toList());
        assertEquals(1, afterFirst.size(), "After adding one expression the stream should contain exactly one element.");
        // Add another expression.
        model.newExpression("second");
        List<Expression> afterSecond = model.expressions().collect(Collectors.toList());
        assertEquals(2, afterSecond.size(), "After adding a second expression the stream should contain exactly two elements.");
        // Ensure both names are present.
        assertTrue(afterSecond.stream().anyMatch(e -> "first".equals(e.getName())), "Stream should contain the expression named 'first'.");
        assertTrue(afterSecond.stream().anyMatch(e -> "second".equals(e.getName())), "Stream should contain the expression named 'second'.");
    }

    @Test
    void testExpressionsStreamSingleExpression() {
        // Add a single expression with a known name.
        Expression expr = model.addExpression("expr1");
        List<Expression> expressions = model.expressions().collect(Collectors.toList());
        assertEquals(1, expressions.size(), "Exactly one expression should be present after adding a single expression.");
        assertEquals("expr1", expressions.get(0).getName(), "The expression present in the stream must have the expected name.");
        assertSame(expr, expressions.get(0), "The instance returned by the stream should be the same object that was added.");
    }

    @Test
    void testExpressionsStreamMultipleExpressions() {
        String[] names = { "a", "b", "c" };
        for (String name : names) {
            model.addExpression(name);
        }
        List<Expression> expressions = model.expressions().collect(Collectors.toList());
        assertEquals(names.length, expressions.size(), "The stream size must match the number of added expressions.");
        // Verify that each expected name is present (order is irrelevant).
        for (String expectedName : names) {
            assertTrue(expressions.stream().anyMatch(e -> expectedName.equals(e.getName())), "Stream must contain an expression named '" + expectedName + "'.");
        }
    }

    @Test
    void testExpressionsStreamLazyEvaluation() {
        // Obtain the stream *before* adding the second expression.
        model.addExpression("first");
        // lazy – no terminal operation yet
        var lazyStream = model.expressions();
        // Add a second expression after the stream has been obtained.
        model.addExpression("second");
        // Collect – the stream should now see both expressions.
        List<Expression> collected = lazyStream.collect(Collectors.toList());
        assertEquals(2, collected.size(), "Lazy stream must reflect the state of the model at terminal operation time.");
        assertTrue(collected.stream().anyMatch(e -> "first".equals(e.getName())), "Stream must contain the first expression.");
        assertTrue(collected.stream().anyMatch(e -> "second".equals(e.getName())), "Stream must contain the second expression.");
    }

    @Test
    void testExpressionsStreamTwoIndependentTerminalOperations() {
        // Add two expressions.
        model.addExpression("x");
        model.addExpression("y");
        // First terminal operation.
        List<Expression> firstCollect = model.expressions().collect(Collectors.toList());
        // Second terminal operation on a *new* stream instance.
        List<Expression> secondCollect = model.expressions().collect(Collectors.toList());
        assertEquals(2, firstCollect.size(), "First collection must contain both expressions.");
        assertEquals(2, secondCollect.size(), "Second collection must also contain both expressions.");
        // Verify that both collections contain the same names.
        for (String name : new String[] { "x", "y" }) {
            assertTrue(firstCollect.stream().anyMatch(e -> name.equals(e.getName())), "First collection must contain '" + name + "'.");
            assertTrue(secondCollect.stream().anyMatch(e -> name.equals(e.getName())), "Second collection must contain '" + name + "'.");
        }
    }

    @Test
    void testExpressionsStreamAfterRemovalSingle() {
        model.addExpression("temp");
        // Obtain a stream *before* removal.
        var stream = model.expressions();
        // Remove the only expression.
        model.removeExpression("temp");
        // Collect – should be empty.
        List<Expression> result = stream.collect(Collectors.toList());
        assertTrue(result.isEmpty(), "After removing the only expression, the stream must be empty.");
    }

    @Test
    void testExpressionsStreamAfterRemovalMultiple() {
        model.addExpression("first");
        model.addExpression("second");
        // Obtain the stream before removal.
        var stream = model.expressions();
        // Remove one expression.
        model.removeExpression("first");
        // Collect – must contain only the remaining expression.
        List<Expression> result = stream.collect(Collectors.toList());
        assertEquals(1, result.size(), "Stream must contain exactly one expression after removal.");
        assertEquals("second", result.get(0).getName(), "The remaining expression must be 'second'.");
    }

    @Test
    void testExpressionsStreamConcurrentModificationWhenAdding() {
        model.addExpression("e1");
        // Obtain the stream.
        var stream = model.expressions();
        // Modify the underlying map *after* the stream has been created.
        model.addExpression("e2");
        // The terminal operation must fail with CME.
        assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Adding an expression after obtaining the stream should trigger ConcurrentModificationException.");
    }

    @Test
    void testExpressionsStreamConcurrentModificationWhenRemoving() {
        model.addExpression("e1");
        // Obtain the stream.
        var stream = model.expressions();
        // Remove an expression after the stream has been created.
        model.removeExpression("e1");
        // The terminal operation must fail with CME.
        assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Removing an expression after obtaining the stream should trigger ConcurrentModificationException.");
    }
}
