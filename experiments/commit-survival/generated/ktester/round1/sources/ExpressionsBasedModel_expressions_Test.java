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
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_expressions_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern clear.
        model = null;
    }

    /**
     * When no expressions have been added the stream should be empty.
     */
    @Test
    void testExpressionsStreamIsEmptyInitially() {
        List<Expression> exprList = model.expressions().collect(Collectors.toList());
        assertTrue(exprList.isEmpty(), "The expressions stream should be empty for a newly created model");
    }

    /**
     * After adding expressions the stream should contain exactly those expressions,
     * preserving the insertion order (as the underlying map is a HashMap, order is not
     * guaranteed, so we only check for presence and count).
     */
    @Test
    void testExpressionsStreamContainsAddedExpressions() {
        // Add two distinct expressions with explicit names.
        Expression exprA = model.newExpression("exprA");
        Expression exprB = model.newExpression("exprB");
        // Verify that the stream now contains both expressions.
        List<Expression> exprList = model.expressions().collect(Collectors.toList());
        assertEquals(2, exprList.size(), "There should be exactly two expressions in the stream");
        // The stream may return the expressions in any order; check by name.
        boolean containsA = exprList.stream().anyMatch(e -> "exprA".equals(e.getName()));
        boolean containsB = exprList.stream().anyMatch(e -> "exprB".equals(e.getName()));
        assertTrue(containsA, "Stream should contain expression named 'exprA'");
        assertTrue(containsB, "Stream should contain expression named 'exprB'");
    }

    /**
     * The stream returned by {@code expressions()} should be a fresh view each time
     * it is requested; modifications to the model after obtaining a stream must not
     * affect previously collected results.
     */
    @Test
    void testExpressionsStreamIsIndependentBetweenCalls() {
        // First call – no expressions yet.
        List<Expression> firstSnapshot = model.expressions().collect(Collectors.toList());
        assertTrue(firstSnapshot.isEmpty(), "First snapshot should be empty");
        // Add an expression after the first snapshot.
        model.newExpression("newExpr");
        // Second call – should now contain the newly added expression.
        List<Expression> secondSnapshot = model.expressions().collect(Collectors.toList());
        assertEquals(1, secondSnapshot.size(), "Second snapshot should contain one expression");
        assertEquals("newExpr", secondSnapshot.get(0).getName(), "Expression name should match the added one");
    }

    @Test
    @DisplayName("Single expression present")
    void testSingleExpressionPresent() {
        // uses the public addExpression(String) API
        model.addExpression("expr1");
        List<String> names = model.expressions().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("expr1"), names, "Stream should contain exactly the added expression 'expr1'");
    }

    @Test
    @DisplayName("Multiple expressions present")
    void testMultipleExpressionsPresent() {
        model.addExpression("exprA");
        model.addExpression("exprB");
        model.addExpression("exprC");
        Set<String> expected = Set.of("exprA", "exprB", "exprC");
        Set<String> actual = model.expressions().map(Expression::getName).collect(Collectors.toSet());
        assertEquals(expected, actual, "Stream should contain all three expressions irrespective of order");
    }

    @Test
    @DisplayName("Stream reflects latest map state – addition after creation")
    void testStreamReflectsAdditionAfterCreation() {
        // Create a stream before any expressions are added.
        Stream<Expression> stream = model.expressions();
        // Add an expression after the stream has been obtained.
        model.addExpression("exprY");
        // Collect – the underlying HashMap is fail‑fast, but because the
        // modification happens before the terminal operation, the stream
        // sees the new element.
        List<String> names = stream.map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("exprY"), names, "The collected stream should include the expression added after the stream was created");
    }

    @Test
    @DisplayName("Empty after removal")
    void testEmptyAfterRemoval() {
        model.addExpression("temp");
        model.removeExpression("temp");
        List<Expression> exprList = model.expressions().collect(Collectors.toList());
        assertTrue(exprList.isEmpty(), "After removing the only expression, the stream should be empty");
    }

    @Test
    @DisplayName("Removal reflected in stream")
    void testRemovalReflectedInStream() {
        model.addExpression("exprA");
        model.addExpression("exprB");
        // Obtain a stream, then remove one expression before terminal operation.
        Stream<Expression> stream = model.expressions();
        model.removeExpression("exprA");
        List<String> names = stream.map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("exprB"), names, "Stream should reflect the removal performed before the terminal operation");
    }

    @Test
    @DisplayName("Multiple independent streams")
    void testMultipleIndependentStreams() {
        model.addExpression("expr1");
        // First snapshot – should contain only expr1.
        List<String> first = model.expressions().map(Expression::getName).collect(Collectors.toList());
        assertEquals(List.of("expr1"), first, "First snapshot must contain only 'expr1'");
        // Add a second expression and obtain a new stream.
        model.addExpression("expr2");
        List<String> second = model.expressions().map(Expression::getName).collect(Collectors.toList());
        // Order is not guaranteed; compare as sets.
        assertEquals(Set.of("expr1", "expr2"), Set.copyOf(second), "Second snapshot must contain both 'expr1' and 'expr2'");
    }

    @Test
    @DisplayName("Expression identity in stream")
    void testExpressionIdentityInStream() {
        Expression unique = model.addExpression("uniqueExpr");
        Expression fromStream = model.expressions().filter(e -> "uniqueExpr".equals(e.getName())).findFirst().orElseThrow(() -> new AssertionError("Expression not found"));
        assertSame(unique, fromStream, "The expression instance retrieved from the stream must be the same as the one stored in the model");
    }

    @Test
    @DisplayName("Stream after clearing all expressions")
    void testStreamAfterClearingAllExpressions() {
        model.addExpression("a");
        model.addExpression("b");
        model.addExpression("c");
        // Remove each one.
        model.removeExpression("a");
        model.removeExpression("b");
        model.removeExpression("c");
        List<Expression> exprList = model.expressions().collect(Collectors.toList());
        assertTrue(exprList.isEmpty(), "After removing all expressions, the stream should be empty");
    }

    @Test
    @DisplayName("Concurrent modification exception on addition")
    void testConcurrentModificationOnAddition() {
        model.addExpression("expr1");
        // Obtain a stream; do NOT perform a terminal operation yet.
        Stream<Expression> stream = model.expressions();
        // Modify the underlying map.
        model.addExpression("expr2");
        // The terminal operation should now fail fast.
        assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Adding an expression after obtaining a stream must cause ConcurrentModificationException");
    }

    @Test
    @DisplayName("Concurrent modification exception on removal")
    void testConcurrentModificationOnRemoval() {
        model.addExpression("exprA");
        model.addExpression("exprB");
        Stream<Expression> stream = model.expressions();
        // Remove an expression before the terminal operation.
        model.removeExpression("exprA");
        assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Removing an expression after obtaining a stream must cause ConcurrentModificationException");
    }

    @Test
    @DisplayName("Concurrent modification exception with parallel stream")
    void testConcurrentModificationWithParallelStream() {
        model.addExpression("pExpr");
        // Obtain a parallel stream.
        Stream<Expression> parallel = model.expressions().parallel();
        // While the stream is being processed, modify the map.
        // Using forEach to trigger processing; inside we modify the model.
        assertThrows(ConcurrentModificationException.class, () -> parallel.forEach(e -> {
            // Trigger modification exactly once.
            if ("pExpr".equals(e.getName())) {
                model.addExpression("pExpr2");
            }
        }), "Parallel stream should throw ConcurrentModificationException when the underlying map is modified during processing");
    }
}
