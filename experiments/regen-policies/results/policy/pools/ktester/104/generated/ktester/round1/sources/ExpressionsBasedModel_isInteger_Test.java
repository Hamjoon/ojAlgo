package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isInteger_Test {

    private ExpressionsBasedModel model;

    // a variable that will be marked as integer
    private Variable intVar;

    // a variable that remains non‑integer
    private Variable nonIntVar;

    class ConcurrentModificationSet extends AbstractSet<IntIndex> {

        private final Set<IntIndex> delegate = new HashSet<>();

        @Override
        public Iterator<IntIndex> iterator() {
            return new Iterator<IntIndex>() {

                private final Iterator<IntIndex> it = delegate.iterator();

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public IntIndex next() {
                    if (first) {
                        // Modify the underlying set during iteration to trigger CME.
                        delegate.add(IntIndex.of(99));
                        first = false;
                        throw new ConcurrentModificationException();
                    }
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean add(IntIndex e) {
            return delegate.add(e);
        }
    }

    class ConcurrentModificationSet extends AbstractSet<IntIndex> {

        private final Set<IntIndex> delegate = new HashSet<>();

        @Override
        public Iterator<IntIndex> iterator() {
            return new Iterator<IntIndex>() {

                private final Iterator<IntIndex> it = delegate.iterator();

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public IntIndex next() {
                    if (first) {
                        // Modify the underlying set during iteration to trigger CME.
                        delegate.add(IntIndex.of(99));
                        first = false;
                        throw new ConcurrentModificationException();
                    }
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean add(IntIndex e) {
            return delegate.add(e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // Add two variables; the first will be set to integer, the second will stay non‑integer.
        // index 0
        intVar = model.addVariable();
        // index 1
        nonIntVar = model.addVariable();
        // Mark the first variable as integer. The Variable API provides an {@code integer(boolean)} method.
        intVar.integer(true);
        // The second variable is left as the default (non‑integer).
    }

    @Test
    void testEmptySetReturnsFalse() {
        Set<IntIndex> empty = new HashSet<>();
        Assertions.assertFalse(model.isInteger(empty), "Empty set should yield false");
    }

    @Test
    void testAllIntegerVariablesReturnsTrue() {
        Set<IntIndex> onlyInt = new HashSet<>();
        // index of intVar
        onlyInt.add(IntIndex.of(0));
        Assertions.assertTrue(model.isInteger(onlyInt), "Set containing only integer variable should yield true");
    }

    @Test
    void testMixedIntegerAndNonIntegerReturnsFalse() {
        Set<IntIndex> mixed = new HashSet<>();
        // integer variable
        mixed.add(IntIndex.of(0));
        // non‑integer variable
        mixed.add(IntIndex.of(1));
        Assertions.assertFalse(model.isInteger(mixed), "Set containing a non‑integer variable should yield false");
    }

    @Test
    void testOnlyNonIntegerVariablesReturnsFalse() {
        Set<IntIndex> onlyNonInt = new HashSet<>();
        // index of nonIntVar
        onlyNonInt.add(IntIndex.of(1));
        Assertions.assertFalse(model.isInteger(onlyNonInt), "Set containing only non‑integer variables should yield false");
    }

    @Test
    void testSingleIntegerVariableReturnsTrue() {
        Set<IntIndex> single = new HashSet<>();
        single.add(IntIndex.of(0));
        Assertions.assertTrue(model.isInteger(single), "Set containing a single integer variable should yield true");
    }

    @Test
    void testNullVariableSetThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> model.isInteger(null), "Passing a null reference for the set should cause NullPointerException");
    }

    @Test
    void testIndexOutOfRangePositiveThrowsIndexOutOfBoundsException() {
        Set<IntIndex> outOfRange = new HashSet<>();
        // model currently has only 2 variables
        outOfRange.add(IntIndex.of(5));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(outOfRange), "Index larger than the number of variables should throw IndexOutOfBoundsException");
    }

    @Test
    void testNegativeIndexThrowsIndexOutOfBoundsException() {
        Set<IntIndex> negative = new HashSet<>();
        negative.add(IntIndex.of(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> model.isInteger(negative), "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testNullElementInSetThrowsNullPointerException() {
        Set<IntIndex> setWithNull = new HashSet<>();
        setWithNull.add(null);
        Assertions.assertThrows(NullPointerException.class, () -> model.isInteger(setWithNull), "Set containing a null element should cause NullPointerException");
    }

    @Test
    void testNullVariableInModelListThrowsNullPointerException() throws Exception {
        // Insert a null entry at index 0 of the internal myVariables list via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) field.get(model);
        // replace the previously added variable with null
        vars.set(0, null);
        Set<IntIndex> set = new HashSet<>();
        set.add(IntIndex.of(0));
        Assertions.assertThrows(NullPointerException.class, () -> model.isInteger(set), "Null variable in the model's list should cause NullPointerException");
    }

    @Test
    void testConcurrentModificationDuringIterationThrowsConcurrentModificationException() {
        // Custom Set that throws ConcurrentModificationException when its iterator is used.
        class ConcurrentModificationSet extends AbstractSet<IntIndex> {

            private final Set<IntIndex> delegate = new HashSet<>();

            @Override
            public Iterator<IntIndex> iterator() {
                return new Iterator<IntIndex>() {

                    private final Iterator<IntIndex> it = delegate.iterator();

                    private boolean first = true;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public IntIndex next() {
                        if (first) {
                            // Modify the underlying set during iteration to trigger CME.
                            delegate.add(IntIndex.of(99));
                            first = false;
                            throw new ConcurrentModificationException();
                        }
                        return it.next();
                    }
                };
            }

            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public boolean add(IntIndex e) {
                return delegate.add(e);
            }
        }
        Set<IntIndex> cmSet = new ConcurrentModificationSet();
        // initial element
        cmSet.add(IntIndex.of(0));
        Assertions.assertThrows(ConcurrentModificationException.class, () -> model.isInteger(cmSet), "Concurrent modification during iteration should propagate ConcurrentModificationException");
    }
}
