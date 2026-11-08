package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isUnbounded_Test {

    private ExpressionsBasedModel model;

    class BadVariable extends Variable {

        BadVariable(String name) {
            super(name);
        }

        @Override
        public boolean isUnbounded() {
            throw new IllegalStateException("Deliberate failure");
        }
    }

    class BadVariable extends Variable {

        BadVariable(String name) {
            super(name);
        }

        @Override
        public boolean isUnbounded() {
            throw new IllegalStateException("Deliberate failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for each test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        model = null;
    }

    /**
     * A variable without any explicit lower/upper bound is considered unbounded.
     * The model should therefore report {@code true} for {@code isUnbounded()}.
     */
    @Test
    void testIsUnboundedWhenVariableHasNoBounds() {
        // defaults → no bounds
        Variable x = Variable.make("x");
        model.addVariable(x);
        assertTrue(model.isUnbounded(), "Model should be unbounded when it contains a variable without bounds");
    }

    /**
     * When all variables have both lower and upper limits the model must be bounded.
     */
    @Test
    void testIsBoundedWhenAllVariablesAreBounded() {
        Variable x = Variable.make("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = Variable.make("y").lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5));
        model.addVariable(x);
        model.addVariable(y);
        assertFalse(model.isUnbounded(), "Model should be bounded when all variables have explicit bounds");
    }

    /**
     * Mixed case: at least one variable is unbounded → model is unbounded.
     */
    @Test
    void testIsUnboundedWhenMixedBoundedAndUnboundedVariables() {
        Variable bounded = Variable.make("b").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // no bounds
        Variable unbounded = Variable.make("u");
        model.addVariable(bounded);
        model.addVariable(unbounded);
        assertTrue(model.isUnbounded(), "Model should be unbounded if any variable is unbounded");
    }

    /**
     * A newly created model contains no variables, therefore {@code isUnbounded()}
     * must return {@code false}.
     */
    @Test
    void testIsUnboundedEmptyModel() {
        assertFalse(model.isUnbounded(), "An empty model should not be reported as unbounded");
    }

    /**
     * The model contains several bounded variables only; the predicate never matches.
     */
    @Test
    void testIsUnboundedAllVariablesBounded() {
        model.addVariable(Variable.make("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE));
        model.addVariable(Variable.make("b").lower(BigDecimal.valueOf(-2)).upper(BigDecimal.valueOf(2)));
        assertFalse(model.isUnbounded(), "Model with only bounded variables must be reported as bounded");
    }

    /**
     * The model contains a mixture of bounded and unbounded variables;
     * the presence of any unbounded variable makes the whole model unbounded.
     */
    @Test
    void testIsUnboundedAtLeastOneUnboundedVariable() {
        model.addVariable(Variable.make("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE));
        // unbounded
        model.addVariable(Variable.make("b"));
        model.addVariable(Variable.make("c").lower(BigDecimal.ONE).upper(BigDecimal.TEN));
        assertTrue(model.isUnbounded(), "Model should be unbounded when at least one variable is unbounded");
    }

    /**
     * Verify that the method reflects the current state: first call returns {@code false},
     * after adding an unbounded variable a second call returns {@code true}.
     */
    @Test
    void testIsUnboundedDynamicChangeAfterInitialCall() {
        // initially bounded only
        model.addVariable(Variable.make("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN));
        assertFalse(model.isUnbounded(), "Initially the model should be bounded");
        // add an unbounded variable
        model.addVariable(Variable.make("y"));
        assertTrue(model.isUnbounded(), "After adding an unbounded variable the model must be unbounded");
    }

    /**
     * Variable with infinite lower bound (default) and a finite upper bound is unbounded.
     */
    @Test
    void testIsUnboundedVariableWithInfiniteLowerOnly() {
        // lower stays -∞
        Variable v = Variable.make("v").upper(BigDecimal.valueOf(100));
        model.addVariable(v);
        assertTrue(model.isUnbounded(), "Variable with only an infinite lower side makes the model unbounded");
    }

    /**
     * Variable with infinite upper bound (default) and a finite lower bound is unbounded.
     */
    @Test
    void testIsUnboundedVariableWithInfiniteUpperOnly() {
        // upper stays +∞
        Variable v = Variable.make("v").lower(BigDecimal.valueOf(-50));
        model.addVariable(v);
        assertTrue(model.isUnbounded(), "Variable with only an infinite upper side makes the model unbounded");
    }

    /**
     * A variable that starts unbounded becomes bounded after mutation;
     * the model must reflect the change.
     */
    @Test
    void testIsUnboundedVariableBecomesBoundedAfterModification() {
        // unbounded
        Variable v = Variable.make("v");
        model.addVariable(v);
        assertTrue(model.isUnbounded(), "Model should be unbounded before bounds are set");
        // now set finite bounds
        v.lower(BigDecimal.ZERO);
        v.upper(BigDecimal.valueOf(10));
        assertFalse(model.isUnbounded(), "Model should be bounded after the variable receives finite bounds");
    }

    /**
     * A variable that starts bounded becomes unbounded after its upper bound is cleared.
     */
    @Test
    void testIsUnboundedVariableBecomesUnboundedAfterModification() {
        Variable v = Variable.make("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.addVariable(v);
        assertFalse(model.isUnbounded(), "Model should be bounded initially");
        // clear the upper bound – passing {@code null} removes the bound in ojAlgo
        v.upper(null);
        assertTrue(model.isUnbounded(), "Model should become unbounded after the variable loses its upper bound");
    }

    /**
     * A shallow copy created via {@code copy()} must retain the unbounded status of the original.
     */
    @Test
    void testIsUnboundedCopyOfModelRetainsUnboundedStatus() {
        // unbounded
        model.addVariable(Variable.make("x"));
        // shallow copy
        ExpressionsBasedModel copy = model.copy();
        assertTrue(copy.isUnbounded(), "Shallow copy should report unbounded because it shares the same variable objects");
    }

    /**
     * Concurrent modification: while {@code isUnbounded()} streams over the internal list,
     * another thread adds a variable. The fail‑fast behavior of {@link ArrayList}
     * should cause a {@link ConcurrentModificationException}.
     */
    @Test
    void testIsUnboundedConcurrentModification() throws Exception {
        // Populate the model with a few variables so the stream has work to do
        for (int i = 0; i < 5; i++) {
            model.addVariable(Variable.make("v" + i).lower(BigDecimal.ZERO).upper(BigDecimal.ONE));
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch modifyLatch = new CountDownLatch(1);
        // Task that will modify the list while the main thread is iterating
        Future<?> modifier = executor.submit(() -> {
            try {
                // wait until the stream has started
                startLatch.await();
                // add a new variable – this mutates the underlying ArrayList
                model.addVariable(Variable.make("new"));
                // signal that modification happened
                modifyLatch.countDown();
            } catch (InterruptedException ignored) {
            }
        });
        // The test thread: start the stream and then let the other thread modify the list
        Executable call = () -> {
            // The moment anyMatch begins, we release the latch so the other thread can act
            // uses internal list
            Stream<Variable> stream = model.variables();
            startLatch.countDown();
            // The terminal operation may throw ConcurrentModificationException
            stream.anyMatch(Variable::isUnbounded);
        };
        // Expect the exception
        assertThrows(ConcurrentModificationException.class, call, "Concurrent modification during isUnbounded() should raise ConcurrentModificationException");
        // Ensure the modifier finished
        modifyLatch.await(1, TimeUnit.SECONDS);
        executor.shutdownNow();
    }

    /**
     * Inject a {@code null} entry into the private {@code myVariables} list via reflection.
     * The stream will encounter the null and invoking {@code Variable::isUnbounded}
     * on it must raise a {@link NullPointerException}.
     */
    @Test
    void testIsUnboundedNullVariableInList() throws Exception {
        // Add a normal variable first (so the list is not empty)
        model.addVariable(Variable.make("valid"));
        // Access the private field and insert a null
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) field.get(model);
        // inject null
        list.add(null);
        assertThrows(NullPointerException.class, () -> model.isUnbounded(), "A null entry in the variable list should cause NullPointerException");
    }

    /**
     * Create a subclass of {@link Variable} that deliberately throws an exception
     * from {@code isUnbounded()}. Insert it via reflection and verify that the
     * exception propagates out of {@code isUnbounded()}.
     */
    @Test
    void testIsUnboundedVariableThrowsException() throws Exception {
        // Subclass that throws IllegalStateException
        class BadVariable extends Variable {

            BadVariable(String name) {
                super(name);
            }

            @Override
            public boolean isUnbounded() {
                throw new IllegalStateException("Deliberate failure");
            }
        }
        // Insert the bad variable via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) field.get(model);
        list.add(new BadVariable("bad"));
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.isUnbounded(), "Exception from a variable's isUnbounded() must propagate");
        assertEquals("Deliberate failure", thrown.getMessage());
    }
}
