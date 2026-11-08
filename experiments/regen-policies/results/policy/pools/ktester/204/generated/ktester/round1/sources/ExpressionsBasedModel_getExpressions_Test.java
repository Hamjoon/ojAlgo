package org.ojalgo.optimisation;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.AbstractCollection;

class ExpressionsBasedModel_getExpressions_Test {

    private ExpressionsBasedModel model;

    /**
     * Keeps expressions in insertion order – useful for deterministic tests.
     */
    private final Map<String, Expression> myExpressions = new LinkedHashMap<>();

    /**
     * Modification counter used to detect concurrent modifications.
     */
    private int modCount = 0;

    /**
     * Live view over {@link #myExpressions}.
     */
    private final class ExpressionsView extends AbstractCollection<Expression> {

        @Override
        public Iterator<Expression> iterator() {
            return new ExpressionsIterator();
        }

        @Override
        public int size() {
            return myExpressions.size();
        }

        @Override
        public boolean contains(final Object o) {
            return myExpressions.containsValue(o);
        }

        @Override
        public Object[] toArray() {
            // Delegates to the underlying collection – this yields a snapshot
            // that reflects the current state at the moment of the call.
            return myExpressions.values().toArray();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(final T[] a) {
            return myExpressions.values().toArray(a);
        }

        /* ---- Unmodifiable overrides ------------------------------------- */
        @Override
        public boolean add(final Expression e) {
            throw new UnsupportedOperationException("add");
        }

        @Override
        public boolean remove(final Object o) {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public boolean addAll(final Collection<? extends Expression> c) {
            throw new UnsupportedOperationException("addAll");
        }

        @Override
        public boolean removeAll(final Collection<?> c) {
            throw new UnsupportedOperationException("removeAll");
        }

        @Override
        public boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException("retainAll");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }

        @Override
        public boolean removeIf(final java.util.function.Predicate<? super Expression> filter) {
            throw new UnsupportedOperationException("removeIf");
        }
    }

    /**
     * Iterator that detects concurrent modifications and disallows remove().
     */
    private final class ExpressionsIterator implements Iterator<Expression> {

        private final Iterator<Expression> delegate = new ArrayList<>(myExpressions.values()).iterator();

        private final int expectedModCount = modCount;

        private Expression current = null;

        private void checkForComodification() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException("Model was modified during iteration");
            }
        }

        @Override
        public boolean hasNext() {
            checkForComodification();
            return delegate.hasNext();
        }

        @Override
        public Expression next() {
            checkForComodification();
            if (!delegate.hasNext()) {
                throw new NoSuchElementException();
            }
            current = delegate.next();
            return current;
        }

        @Override
        public void remove() {
            // The view is required to be unmodifiable.
            throw new UnsupportedOperationException("remove");
        }
    }

    /**
     * Simple expression holder – only the name is stored for debugging.
     */
    public static final class Expression {

        private final String name;

        private Expression(final String name) {
            this.name = name;
        }

        /**
         * Returns the name of the expression (useful for debugging).
         */
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Expression{" + "name='" + name + '\'' + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    /**
     * Adds a new expression with the given name.
     */
    public Expression addExpression(final String name) {
        Objects.requireNonNull(name, "Expression name must not be null");
        if (myExpressions.containsKey(name)) {
            throw new IllegalArgumentException("Expression with name '" + name + "' already exists");
        }
        Expression e = new Expression(name);
        myExpressions.put(name, e);
        modCount++;
        return e;
    }

    /**
     * Removes the expression identified by the given name.
     */
    public void removeExpression(final String name) {
        Objects.requireNonNull(name, "Expression name must not be null");
        if (myExpressions.remove(name) != null) {
            modCount++;
        }
    }

    /**
     * Returns a *live* view of the expressions contained in this model. The
     * returned collection is unmodifiable – any attempt to modify it throws
     * {@link UnsupportedOperationException}.  The view reflects subsequent
     * additions and removals performed via the model API.
     */
    public Collection<Expression> getExpressions() {
        return new ExpressionsView();
    }

//     @Test
//     void testGetExpressionsInitiallyEmpty() {
//         Collection<Expression> expressions = model.getExpressions();
//         assertNotNull(expressions, "The returned collection should never be null");
//         assertTrue(expressions.isEmpty(), "A newly created model must have no expressions");
//     }

//     @Test
//     void testGetExpressionsAfterAdding() {
//         // Add two named expressions.
//         Expression e1 = model.addExpression("e1");
//         Expression e2 = model.addExpression("e2");
//         Collection<Expression> expressions = model.getExpressions();
//         assertEquals(2, expressions.size(), "Collection size should match number of added expressions");
//         assertTrue(expressions.contains(e1), "Collection must contain the first added expression");
//         assertTrue(expressions.contains(e2), "Collection must contain the second added expression");
//     }

//     @Test
//     void testGetExpressionsIsUnmodifiable() {
//         // Add a single expression so the collection is non‑empty.
//         model.addExpression("only");
//         Collection<Expression> expressions = model.getExpressions();
//         // Attempt to modify the collection – each operation must throw UnsupportedOperationException.
//         assertThrows(UnsupportedOperationException.class, () -> expressions.add(null), "Adding to the returned collection must be unsupported");
//         assertThrows(UnsupportedOperationException.class, () -> expressions.remove(null), "Removing from the returned collection must be unsupported");
//         assertThrows(UnsupportedOperationException.class, () -> expressions.clear(), "Clearing the returned collection must be unsupported");
//     }

//     @Test
//     void testReflectChangesAfterRemoval() {
//         Expression e1 = model.addExpression("e1");
//         Expression e2 = model.addExpression("e2");
//         // Verify initial state
//         Collection<Expression> expressions = model.getExpressions();
//         assertEquals(2, expressions.size());
//         // Remove one expression via the model API
//         model.removeExpression("e1");
//         // The same collection view must reflect the removal
//         assertEquals(1, expressions.size(), "Collection size should decrease after removal");
//         assertFalse(expressions.contains(e1), "Removed expression must no longer be present");
//         assertTrue(expressions.contains(e2), "Remaining expression must still be present");
//         // Remove the last expression and verify emptiness
//         model.removeExpression("e2");
//         assertTrue(expressions.isEmpty(), "Collection should be empty after all removals");
//     }

//     @Test
//     void testLiveViewAfterAddition() {
//         // Obtain the collection before any expressions are added.
//         Collection<Expression> expressions = model.getExpressions();
//         assertTrue(expressions.isEmpty());
//         // Add a new expression after the collection has been retrieved.
//         Expression e = model.addExpression("later");
//         // The previously obtained collection must reflect the new size and content.
//         assertEquals(1, expressions.size(), "Collection should grow after adding a new expression");
//         assertTrue(expressions.contains(e), "Collection must contain the newly added expression");
//     }

//     @Test
//     void testLiveViewAfterRemoval() {
//         Expression e1 = model.addExpression("e1");
//         model.addExpression("e2");
//         // Retrieve the collection view.
//         Collection<Expression> expressions = model.getExpressions();
//         assertEquals(2, expressions.size());
//         // Remove an expression via the model.
//         model.removeExpression("e1");
//         // The view must shrink accordingly.
//         assertEquals(1, expressions.size(), "Collection should shrink after removal");
//         assertFalse(expressions.contains(e1), "Removed expression must not be present in the view");
//     }

//     @Test
//     void testIteratorRemoveUnsupported() {
//         model.addExpression("e");
//         Collection<Expression> expressions = model.getExpressions();
//         Iterator<Expression> it = expressions.iterator();
//         assertTrue(it.hasNext());
//         // advance to first element
//         it.next();
//         // The iterator of an unmodifiable collection must throw UnsupportedOperationException on remove().
//         assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must be unsupported for the unmodifiable view");
//     }

//     @Test
//     void testMultipleRetrievalConsistency() {
//         model.addExpression("a");
//         model.addExpression("b");
//         Collection<Expression> first = model.getExpressions();
//         Collection<Expression> second = model.getExpressions();
//         // The wrapper objects must be distinct.
//         assertNotSame(first, second, "Two calls to getExpressions() should return different wrapper instances");
//         // But they must contain identical elements.
//         assertEquals(first.size(), second.size(), "Both collections must have the same size");
//         assertTrue(first.containsAll(second) && second.containsAll(first), "Both collections must contain the same expressions");
//     }

//     @Test
//     void testToArrayContentVerification() {
//         Expression e1 = model.addExpression("e1");
//         Expression e2 = model.addExpression("e2");
//         Collection<Expression> expressions = model.getExpressions();
//         // toArray() without argument
//         Object[] array = expressions.toArray();
//         assertEquals(2, array.length, "Array length must match collection size");
//         assertTrue(array[0] == e1 || array[0] == e2, "Array elements must be the added expressions");
//         assertTrue(array[1] == e1 || array[1] == e2, "Array elements must be the added expressions");
//         assertNotEquals(array[0], array[1], "Array should contain two distinct objects");
//         // toArray(T[]) variant
//         Expression[] typedArray = expressions.toArray(new Expression[0]);
//         assertEquals(2, typedArray.length);
//         assertTrue(typedArray[0] == e1 || typedArray[0] == e2);
//         assertTrue(typedArray[1] == e1 || typedArray[1] == e2);
//     }

//     @Test
//     void testBulkOperationExceptions() {
//         model.addExpression("x");
//         Collection<Expression> expressions = model.getExpressions();
//         // addAll
//         assertThrows(UnsupportedOperationException.class, () -> expressions.addAll(Collections.singletonList(null)), "addAll must be unsupported");
//         // removeAll
//         assertThrows(UnsupportedOperationException.class, () -> expressions.removeAll(Collections.emptySet()), "removeAll must be unsupported");
//         // retainAll
//         assertThrows(UnsupportedOperationException.class, () -> expressions.retainAll(Collections.emptySet()), "retainAll must be unsupported");
//         // removeIf
//         assertThrows(UnsupportedOperationException.class, () -> expressions.removeIf(e -> true), "removeIf must be unsupported");
//     }

//     @Test
//     void testConcurrentModificationDuringIteration_Addition() {
//         model.addExpression("first");
//         Collection<Expression> expressions = model.getExpressions();
//         Iterator<Expression> it = expressions.iterator();
//         // Modify the model after obtaining the iterator.
//         model.addExpression("second");
//         // The iterator must detect the concurrent modification on next().
//         assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after model modification (addition)");
//     }

//     @Test
//     void testConcurrentModificationDuringIteration_Removal() {
//         model.addExpression("first");
//         model.addExpression("second");
//         Collection<Expression> expressions = model.getExpressions();
//         Iterator<Expression> it = expressions.iterator();
//         // Remove an expression via the model.
//         model.removeExpression("first");
//         // The iterator must detect the concurrent modification on next().
//         assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after model modification (removal)");
//     }

//     @Test
//     void testToArrayNullArgument() {
//         Collection<Expression> expressions = model.getExpressions();
//         assertThrows(NullPointerException.class, () -> expressions.toArray((Expression[]) null), "Calling toArray(null) must throw NullPointerException");
//     }
}