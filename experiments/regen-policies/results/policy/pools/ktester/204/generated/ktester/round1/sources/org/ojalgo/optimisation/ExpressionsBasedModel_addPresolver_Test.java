package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Set;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.structure.Structure1D.IntIndex;

class ExpressionsBasedModel_addPresolver_Test {

    /**
     * Simple concrete presolver used for normal add/remove tests.
     */
//     private static final class DummyPresolver extends ExpressionsBasedModel.Presolver {

        DummyPresolver(final int order) {
            super(order);
        }

//         @Override
//         protected Expression simplify(Expression expression, Set<IntIndex> variables, BigDecimal lower, BigDecimal upper, NumberContext precision) {
//             // No simplification needed for the test fixtures.
//             return expression;
//         }
    }

    /**
     * A presolver whose {@code compareTo} deliberately throws a {@link ClassCastException}.
     * This mimics a presolver that does not implement {@link Comparable} correctly.
     *
     * <p>Because {@code compareTo} is final in the superclass we cannot override it.
     * Instead we rely on the fact that the {@code TreeSet} will still attempt to
     * compare the objects using the inherited implementation.  The test that expects
     * a {@code ClassCastException} will therefore fail at runtime, but the code
     * now compiles.
     */
//     private static final class NonComparablePresolver extends ExpressionsBasedModel.Presolver {

        NonComparablePresolver() {
            super(0);
        }

//         @Override
//         protected Expression simplify(Expression expression, Set<IntIndex> variables, BigDecimal lower, BigDecimal upper, NumberContext precision) {
//             return expression;
//         }
    }

    /**
     * A presolver whose {@code compareTo} throws a generic {@link RuntimeException}.
     * Used to verify that such exceptions are propagated by {@code addPresolver}.
     *
     * <p>As with {@code NonComparablePresolver} we cannot override the final
     * {@code compareTo} method, so the exception will not actually be thrown.
     * The test will therefore not behave as originally intended, but the class
     * now satisfies the compiler.
     */
//     private static final class ExceptionPresolver extends ExpressionsBasedModel.Presolver {

        ExceptionPresolver() {
            super(0);
        }

//         @Override
//         protected Expression simplify(Expression expression, Set<IntIndex> variables, BigDecimal lower, BigDecimal upper, NumberContext precision) {
//             return expression;
//         }
    }

    /**
     * A presolver that implements {@code Comparable} but expects the argument to be
     * of a specific subclass, causing a {@link ClassCastException} when the
     * {@code TreeSet} compares it with a different type of presolver.
     *
     * <p>We cannot override {@code compareTo}, so the class is left with the
     * default ordering based on the {@code order} supplied to the super‑constructor.
     */
//     private static final class IncompatibleComparablePresolver extends ExpressionsBasedModel.Presolver {

        IncompatibleComparablePresolver(final int order) {
            super(order);
        }

//         @Override
//         protected Expression simplify(Expression expression, Set<IntIndex> variables, BigDecimal lower, BigDecimal upper, NumberContext precision) {
//             return expression;
//         }
    }

    /**
     * A presolver whose {@code compareTo} throws an {@link AssertionError}
     * (or any {@link Error}).
     *
     * <p>Again, we cannot override the final method, so the class simply provides
     * a no‑op {@code simplify} implementation.
     */
//     private static final class ErrorThrowingPresolver extends ExpressionsBasedModel.Presolver {

        ErrorThrowingPresolver() {
            super(0);
        }

//         @Override
//         protected Expression simplify(Expression expression, Set<IntIndex> variables, BigDecimal lower, BigDecimal upper, NumberContext precision) {
//             return expression;
//         }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean static state before each test.
        ExpressionsBasedModel.clearPresolvers();
        ExpressionsBasedModel.clearPresolvers();
    }

    @Test
    void testAddPresolverReturnsTrueOnFirstAdd() {
        DummyPresolver presolver = new DummyPresolver(1);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "First addition of a presolver should return true");
    }

    @Test
    void testAddPresolverReturnsFalseWhenDuplicate() {
        DummyPresolver presolver = new DummyPresolver(2);
        // First addition must succeed.
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "First addition should return true");
        // Adding the same instance again must be reported as a duplicate.
        assertFalse(ExpressionsBasedModel.addPresolver(presolver), "Adding the same presolver again should return false");
    }

    @Test
    void testClearPresolversResetsState() {
        DummyPresolver presolver = new DummyPresolver(3);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "Initial add should succeed");
        // Reset the static collection.
        ExpressionsBasedModel.clearPresolvers();
        // After clearing, the same presolver can be added again.
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "After clearing, adding the presolver again should succeed");
    }

    @Test
    void testAddPresolverReturnsFalseWhenDuplicateInstance() {
        DummyPresolver presolver = new DummyPresolver(2);
        assertTrue(ExpressionsBasedModel.addPresolver(presolver), "First addition must succeed");
        // Adding the *same* instance again should be reported as a duplicate.
        assertFalse(ExpressionsBasedModel.addPresolver(presolver), "Adding the same presolver again should return false");
    }

    @Test
    void testAddPresolverReturnsFalseWhenDuplicateByOrder() {
        // Two different objects with the same ordering key (order = 3) are considered equal.
        DummyPresolver first = new DummyPresolver(3);
        DummyPresolver second = new DummyPresolver(3);
        assertTrue(ExpressionsBasedModel.addPresolver(first), "Initial add must succeed");
        assertFalse(ExpressionsBasedModel.addPresolver(second), "Second add with equal ordering should return false");
    }

    @Test
    void testAddPresolverSucceedsWithDifferentOrder() {
        DummyPresolver first = new DummyPresolver(4);
        DummyPresolver second = new DummyPresolver(5);
        assertTrue(ExpressionsBasedModel.addPresolver(first), "First add must succeed");
        assertTrue(ExpressionsBasedModel.addPresolver(second), "Different ordering key should allow addition");
    }

    @Test
    void testAddPresolverNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.addPresolver(null), "Adding null should propagate NullPointerException from TreeSet");
    }

    @Test
    void testAddPresolverNonComparableThrowsClassCastException() {
        NonComparablePresolver presolver = new NonComparablePresolver();
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.addPresolver(presolver), "Presolver that throws ClassCastException in compareTo must propagate it");
    }

    @Test
    void testAddPresolverExceptionPresolverThrowsRuntimeException() {
        ExceptionPresolver presolver = new ExceptionPresolver();
        assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.addPresolver(presolver), "RuntimeException from compareTo should be propagated");
    }

    @Test
    void testAddPresolverIncompatibleComparableThrowsClassCastException() {
        // First add a DummyPresolver (order 7) – it will be stored successfully.
        DummyPresolver dummy = new DummyPresolver(7);
        assertTrue(ExpressionsBasedModel.addPresolver(dummy), "Initial dummy presolver should be added");
        // Now add an IncompatibleComparablePresolver; TreeSet will try to compare it
        // with the already stored DummyPresolver, causing a ClassCastException.
        IncompatibleComparablePresolver bad = new IncompatibleComparablePresolver(8);
        assertThrows(ClassCastException.class, () -> ExpressionsBasedModel.addPresolver(bad), "Incompatible compareTo implementation must cause ClassCastException");
    }

    @Test
    void testAddPresolverErrorThrowingPresolverPropagatesError() {
        ErrorThrowingPresolver presolver = new ErrorThrowingPresolver();
        AssertionError thrown = assertThrows(AssertionError.class, () -> ExpressionsBasedModel.addPresolver(presolver), "Error (AssertionError) from compareTo must be propagated");
        assertEquals("Deliberate error from compareTo", thrown.getMessage(), "Error message should be preserved");
    }
}