package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_relax_2_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple {@link Variable} that counts how many times {@code relax()} is invoked
     * and can be configured to throw a runtime exception.
     */
    private static class CountingVariable extends Variable {

        private int relaxCalls = 0;

        private RuntimeException toThrow = null;

        CountingVariable() {
            super();
        }

        void setException(RuntimeException e) {
            this.toThrow = e;
        }

        int getRelaxCalls() {
            return relaxCalls;
        }

        @Override
        public void relax() {
            relaxCalls++;
            if (toThrow != null) {
                throw toThrow;
            }
        }
    }

    /**
     * {@link Variable} that, when relaxed, adds a new variable to the supplied model.
     * Used to provoke a {@link ConcurrentModificationException} during iteration.
     */
    private static class ModifyingVariable extends Variable {

        private final ExpressionsBasedModel model;

        ModifyingVariable(ExpressionsBasedModel model) {
            super();
            this.model = model;
        }

        @Override
        public void relax() {
            // side‑effect: modify the internal list while it is being iterated
            model.addVariable();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test ensures isolation.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model – good practice for resources that may hold native data.
        model.dispose();
    }

    /**
     * Inject one or more mock {@link Variable}s into the private {@code myVariables} field.
     *
     * @param mocks the mock variables to inject
     */
    private void injectMockVariable(Variable... mocks) throws Exception {
        // Create a fresh list containing the supplied mocks.
        List<Variable> list = new ArrayList<>(Arrays.asList(mocks));
        setPrivateField(model, "myVariables", list);
    }

    /**
     * Utility to set a private field via reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field
     * @param value     the new value
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * When {@code soft} is {@code true} the model should be marked as relaxed.
     */
    @Test
    void testRelaxSoftTrueSetsRelaxedFlag() {
        // Preconditions – the model is not relaxed initially.
        assertFalse(model.isRelaxed(), "Model should start non‑relaxed");
        // Exercise the method under test.
        model.relax(true);
        // Verify that the relaxed flag is now true.
        assertTrue(model.isRelaxed(), "Model should be marked as relaxed after relax(true)");
    }

    /**
     * When {@code soft} is {@code false} the model must NOT set the relaxed flag.
     * The method should also execute without throwing, even when variables are present.
     */
    @Test
    void testRelaxSoftFalseLeavesRelaxedFlagUnset() {
        // Add a couple of variables to make the “else” branch iterate over them.
        model.addVariable();
        model.addVariable();
        // Ensure the model is not relaxed before the call.
        assertFalse(model.isRelaxed(), "Model should start non‑relaxed");
        // The call should not throw any exception.
        assertDoesNotThrow(() -> model.relax(false), "relax(false) should not throw");
        // After the call the relaxed flag must still be false.
        assertFalse(model.isRelaxed(), "Model should remain non‑relaxed after relax(false)");
    }

    /**
     * Verify that calling the no‑argument {@code relax()} (which delegates to {@code relax(false)})
     * behaves the same as the explicit {@code relax(false)} call.
     */
    @Test
    void testRelaxNoArgDelegatesToRelaxFalse() {
        // Add a variable so the internal loop has something to process.
        model.addVariable();
        // Call the convenience method.
        assertDoesNotThrow(() -> model.relax(), "relax() should not throw");
        // The model must still be non‑relaxed.
        assertFalse(model.isRelaxed(), "Model should remain non‑relaxed after calling the no‑arg relax()");
    }

    /**
     * Calling {@code relax(true)} after a hard relax should keep the model relaxed
     * and must not invoke any {@code Variable.relax()} calls.
     */
    @Test
    void testSoftAfterHardRelaxKeepsFlagAndDoesNotTouchVariables() throws Exception {
        // Prepare a counting variable to observe relax() invocations.
        CountingVariable countingVar = new CountingVariable();
        // Replace the internal variable list with a list containing our counting variable.
        setPrivateField(model, "myVariables", new ArrayList<>(Collections.singletonList(countingVar)));
        // hard relax – should invoke countingVar.relax()
        model.relax(false);
        assertEquals(1, countingVar.getRelaxCalls(), "Variable.relax() should have been called once during hard relax");
        // soft relax – should only set the flag, no additional variable interaction.
        model.relax(true);
        assertTrue(model.isRelaxed(), "Model must be relaxed after soft call");
        assertEquals(1, countingVar.getRelaxCalls(), "No further Variable.relax() calls should happen during soft relax");
    }

    /**
     * Repeated hard relax calls must invoke {@code Variable.relax()} each time.
     */
    @Test
    void testRepeatedHardRelaxInvokesVariableRelaxEachTime() throws Exception {
        CountingVariable var1 = new CountingVariable();
        CountingVariable var2 = new CountingVariable();
        setPrivateField(model, "myVariables", new ArrayList<>(Arrays.asList(var1, var2)));
        // first hard relax
        model.relax(false);
        // second hard relax
        model.relax(false);
        // each mock should have been called twice
        assertEquals(2, var1.getRelaxCalls(), "First variable should have been relaxed twice");
        assertEquals(2, var2.getRelaxCalls(), "Second variable should have been relaxed twice");
        assertFalse(model.isRelaxed(), "Hard relax never sets the relaxed flag");
    }

    /**
     * If the internal {@code myVariables} list is {@code null} a {@link NullPointerException}
     * is expected when {@code relax(false)} tries to iterate over it.
     */
    @Test
    void testRelaxHardWithNullVariableListThrowsNPE() throws Exception {
        setPrivateField(model, "myVariables", null);
        assertThrows(NullPointerException.class, () -> model.relax(false), "Calling relax(false) with a null variable list should throw NPE");
    }

    /**
     * When a variable's {@code relax()} method throws a runtime exception,
     * the exception must propagate out of {@code relax(false)}.
     */
    @Test
    void testRelaxHardPropagatesVariableRuntimeException() throws Exception {
        // Create a normal variable and add it to the model.
        Variable normalVar = model.addVariable();
        // Replace the internal variable list with a single proxy variable that throws on relax().
        Variable throwingVar = new Variable("thrower", 0) {

            @Override
            public Variable relax() {
                throw new IllegalStateException("bad variable");
            }
        };
        setPrivateField(model, "myVariables", new ArrayList<>(Collections.singletonList(throwingVar)));
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.relax(false), "Exception from Variable.relax() should propagate");
        assertEquals("bad variable", thrown.getMessage());
    }

    /**
     * If a variable adds another variable to the model during its {@code relax()} call,
     * the underlying {@link ArrayList} iteration should detect the concurrent modification
     * and throw {@link ConcurrentModificationException}.
     */
    @Test
    void testRelaxHardConcurrentModificationThrowsCME() throws Exception {
        // Ensure there is at least one variable in the model.
        model.addVariable();
        // Retrieve the internal variable list.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) field.get(model);
        // Replace the list with a custom list that modifies itself during iteration.
        List<Variable> modifyingList = new ArrayList<>(vars) {

            @Override
            public java.util.Iterator<Variable> iterator() {
                final java.util.Iterator<Variable> it = super.iterator();
                return new java.util.Iterator<Variable>() {

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Variable next() {
                        Variable v = it.next();
                        // On first element, add a new variable to the model (causing concurrent modification).
                        if (size() == vars.size()) {
                            model.addVariable();
                        }
                        return v;
                    }
                };
            }
        };
        setPrivateField(model, "myVariables", modifyingList);
        assertThrows(ConcurrentModificationException.class, () -> model.relax(false), "Concurrent modification during iteration should raise CME");
    }
}
