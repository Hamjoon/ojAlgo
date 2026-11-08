package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_expressions_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates a fresh model with default options.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC; not strictly necessary but keeps the pattern consistent.
        model = null;
    }

    @Test
    void testExpressionsStreamIsEmptyInitially() {
        List<Expression> exprList = model.expressions().collect(Collectors.toList());
        assertTrue(exprList.isEmpty(), "A newly created model should have no expressions");
    }

    @Test
    void testExpressionsStreamContainsAddedExpressions() {
        // Add three distinct expressions with names to make verification easy.
        Expression e1 = model.addExpression("expr1");
        Expression e2 = model.addExpression("expr2");
        Expression e3 = model.addExpression("expr3");
        // Populate each expression with a simple term so they are not just placeholders.
        Variable v = model.addVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        e1.set(v, BigDecimal.ONE);
        e2.set(v, BigDecimal.valueOf(2));
        e3.set(v, BigDecimal.valueOf(3));
        List<Expression> exprList = model.expressions().sorted(// deterministic order
        (a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList());
        assertEquals(3, exprList.size(), "Exactly three expressions should be present");
        // Verify that the collected expressions are the same instances we added.
        assertTrue(exprList.contains(e1), "Stream should contain expr1");
        assertTrue(exprList.contains(e2), "Stream should contain expr2");
        assertTrue(exprList.contains(e3), "Stream should contain expr3");
    }

    @Test
    void testExpressionsStreamIsIndependentOfLaterModifications() {
        // Add an expression and obtain a stream reference.
        Expression first = model.addExpression("first");
        Variable v = model.addVariable("v");
        first.set(v, BigDecimal.ONE);
        // Collect the stream now.
        List<Expression> snapshot = model.expressions().collect(Collectors.toList());
        // Add another expression after the snapshot was taken.
        Expression second = model.addExpression("second");
        second.set(v, BigDecimal.TEN);
        // The previously collected snapshot must not contain the newly added expression.
        assertEquals(1, snapshot.size(), "Snapshot should contain only the expression that existed at collection time");
        assertTrue(snapshot.contains(first), "Snapshot must contain the first expression");
        assertFalse(snapshot.contains(second), "Snapshot must not contain the second expression added later");
    }

    @Test
    void testExpressionsStreamReflectsRemovalViaModelMethods() {
        // Add two expressions.
        Expression a = model.addExpression("a");
        Expression b = model.addExpression("b");
        Variable v = model.addVariable("v");
        a.set(v, BigDecimal.ONE);
        b.set(v, BigDecimal.ONE);
        // Ensure both are present.
        assertEquals(2, model.expressions().count(), "Both expressions should be present before removal");
        // Remove one expression using the public API.
        model.removeExpression("a");
        // Verify the stream now reflects the removal.
        List<String> remainingNames = model.expressions().map(Expression::getName).collect(Collectors.toList());
        assertEquals(1, remainingNames.size(), "Only one expression should remain after removal");
        assertEquals("b", remainingNames.get(0), "The remaining expression should be 'b'");
    }

    @Test
    void testSingleExpressionReturnedByStream() {
        Expression e = model.addExpression("single");
        Variable v = model.addVariable("v");
        e.set(v, BigDecimal.ONE);
        List<Expression> list = model.expressions().collect(Collectors.toList());
        assertEquals(1, list.size(), "Exactly one expression should be present");
        assertEquals("single", list.get(0).getName(), "The expression name must match the added one");
    }

    @Test
    void testExpressionsStreamReflectsLateAdditionWhenTerminalOperationIsDeferred() {
        // Obtain the stream but do NOT terminate it yet.
        Stream<Expression> lazyStream = model.expressions();
        // Add a new expression before the terminal operation.
        Expression later = model.addExpression("later");
        Variable v = model.addVariable("v");
        later.set(v, BigDecimal.ONE);
        // Now perform the terminal operation.
        List<String> names = lazyStream.map(Expression::getName).collect(Collectors.toList());
        // The newly added expression must be present because the stream is evaluated lazily.
        assertTrue(names.contains("later"), "Stream should reflect expressions added before terminal operation");
    }

    @Test
    void testTwoSuccessiveCallsReturnIndependentStreams() {
        model.addExpression("e1");
        model.addExpression("e2");
        List<String> first = model.expressions().map(Expression::getName).sorted().collect(Collectors.toList());
        List<String> second = model.expressions().map(Expression::getName).sorted().collect(Collectors.toList());
        assertEquals(first, second, "Both streams should yield the same current set of expressions");
        assertNotSame(first, second, "Collected lists must be distinct objects");
    }

    @Test
    void testStreamSupportsStandardOperations() {
        model.addExpression("a");
        model.addExpression("b");
        long count = model.expressions().count();
        assertEquals(2, count, "Count operation should work without exception");
        long filtered = model.expressions().filter(e -> e.getName().startsWith("a")).count();
        assertEquals(1, filtered, "Filter operation should work correctly");
    }

    @Test
    void testDuplicateNameOverwritesPreviousExpression() {
        // First addition
        Expression first = model.addExpression("dup");
        Variable v = model.addVariable("v");
        first.set(v, BigDecimal.ONE);
        // make it distinguishable
        first.weight(BigDecimal.ONE);
        // Second addition with same name
        Expression second = model.addExpression("dup");
        second.set(v, BigDecimal.valueOf(2));
        second.weight(BigDecimal.valueOf(2));
        List<Expression> exprs = model.expressions().filter(e -> "dup".equals(e.getName())).collect(Collectors.toList());
        assertEquals(1, exprs.size(), "Only one expression with the duplicate name should exist");
        Expression retained = exprs.get(0);
        // The retained expression should be the second one (the map entry was replaced)
        assertEquals(BigDecimal.valueOf(2), retained.getWeight(), "The retained expression must reflect the second addition");
    }

    @Test
    void testFilterQuadraticExpressions() {
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        // Linear expression
        Expression linear = model.addExpression("lin");
        linear.set(x, BigDecimal.ONE);
        // Quadratic expression (using the quadratic API – assumes such a method exists)
        Expression quad = model.addExpression("quad");
        // make it quadratic
        quad.quadratic(x, y, BigDecimal.ONE);
        List<Expression> quadraticOnly = model.expressions().filter(Expression::isQuadratic).collect(Collectors.toList());
        assertEquals(1, quadraticOnly.size(), "Only the quadratic expression should pass the filter");
        assertEquals("quad", quadraticOnly.get(0).getName(), "Filtered expression must be the quadratic one");
    }

    @Test
    void testConcurrentModificationExceptionIsThrown() {
        // Obtain a stream, then modify the underlying map before a terminal operation.
        Stream<Expression> stream = model.expressions();
        // Add a new expression after the stream has been created.
        model.addExpression("newOne");
        // The next terminal operation should detect the structural change.
        assertThrows(ConcurrentModificationException.class, stream::count, "A ConcurrentModificationException is expected when the model is modified after obtaining the stream");
    }

    @Test
    void testNullElementInStreamThrowsNullPointerException() throws Exception {
        // Insert a null entry into the private myExpressions map via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> map = (Map<String, Expression>) field.get(model);
        // deliberately insert null
        map.put("nullEntry", null);
        // Access the stream and trigger a downstream operation that dereferences the element.
        assertThrows(NullPointerException.class, () -> model.expressions().map(// this will be called on the null entry
        Expression::getName).collect(Collectors.toList()), "Mapping over a null expression should throw NullPointerException");
    }

    @ParameterizedTest
    @ValueSource(ints = { 10, 100, 1000 })
    void testExpressionsCountScalesWithNumberOfAddedExpressions(int numberOfExpressions) {
        // Add a large number of distinct expressions.
        IntStream.range(0, numberOfExpressions).forEach(i -> {
            Expression e = model.addExpression("e" + i);
            Variable v = model.addVariable("v" + i);
            e.set(v, BigDecimal.ONE);
        });
        long count = model.expressions().count();
        assertEquals(numberOfExpressions, count, "The stream count must equal the number of expressions added");
    }

    @Test
    void testParallelStreamProcessing() {
        // Add several expressions.
        int total = 200;
        IntStream.range(0, total).forEach(i -> {
            Expression e = model.addExpression("p" + i);
            Variable v = model.addVariable("v" + i);
            e.set(v, BigDecimal.ONE);
        });
        // Parallel processing: collect names into a Set.
        Set<String> names = model.expressions().parallel().map(Expression::getName).collect(Collectors.toSet());
        assertEquals(total, names.size(), "Parallel stream must collect all expression names");
        // Spot‑check a few names.
        assertTrue(names.contains("p0"));
        assertTrue(names.contains("p" + (total - 1)));
    }
}
