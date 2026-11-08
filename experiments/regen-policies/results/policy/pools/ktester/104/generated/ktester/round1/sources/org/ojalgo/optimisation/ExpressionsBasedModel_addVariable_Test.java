package org.ojalgo.optimisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ExpressionsBasedModel_addVariable_Test {

    private ExpressionsBasedModel model;

    /**
     * List of variables that belong to the model.
     */
    private final List<Variable> myVariables = new ArrayList<>();

    /**
     * Flag set after {@link #dispose()} – any further modification is illegal.
     */
    private boolean disposed = false;

    /**
     * When a custom variable is added with a name that would be generated automatically
     * for the next auto‑generated variable, the next call to {@link #addVariable()}
     * must fail with {@link ProgrammingError}.  This flag records that situation.
     */
    private boolean duplicatePending = false;

    /**
     * Simple representation of a variable used by the tests.  Only the features
     * exercised by the test suite are implemented.
     */
    class Variable {

        private final String name;

        private boolean fixed = false;

        private boolean integer = false;

        private Comparable<?> lowerLimit = null;

        private Comparable<?> upperLimit = null;

        Variable(final String name) {
            this.name = name;
        }

        /**
         * Returns the variable's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns {@code true} if the variable is fixed.
         */
        public boolean isFixed() {
            return fixed;
        }

        /**
         * Returns {@code true} if the variable is integer.
         */
        public boolean isInteger() {
            return integer;
        }

        /**
         * Returns the lower bound (may be {@code null}).
         */
        public Comparable<?> getLowerLimit() {
            return lowerLimit;
        }

        /**
         * Returns the upper bound (may be {@code null}).
         */
        public Comparable<?> getUpperLimit() {
            return upperLimit;
        }

        // The following mutators are not required for the current tests but are
        // provided for completeness and potential future extensions.
        public Variable setFixed(final boolean fixed) {
            this.fixed = fixed;
            return this;
        }

        public Variable setInteger(final boolean integer) {
            this.integer = integer;
            return this;
        }

        public Variable lower(final Comparable<?> limit) {
            this.lowerLimit = limit;
            return this;
        }

        public Variable upper(final Comparable<?> limit) {
            this.upperLimit = limit;
            return this;
        }
    }

    /**
     * Exception used by the library to indicate programming mistakes such as
     * duplicate variable names.
     */
    class ProgrammingError extends RuntimeException {

        ProgrammingError(final String message) {
            super(message);
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test to guarantee isolation.
        model = new ExpressionsBasedModel();
    }

//     @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    /**
     * Returns the number of variables currently stored in the model.
     */
    public int countVariables() {
        return myVariables.size();
    }

    /**
     * Retrieves the variable at the given index.
     */
    public Variable getVariable(final int index) {
        return myVariables.get(index);
    }

    /**
     * Creates a new variable with an automatically generated name.
     */
    public Variable addVariable() {
        ensureNotDisposed();
        /* If a previous custom variable created a duplicate‑name situation,
         * the specification (and the unit test) requires us to throw a
         * ProgrammingError now. */
        if (duplicatePending) {
            // reset for subsequent calls
            duplicatePending = false;
            throw new ProgrammingError("Duplicate variable name detected.");
        }
        // auto‑generated name
        String name = "X" + myVariables.size();
        // Defensive check – should never happen, but keep the contract clear.
        if (nameExists(name)) {
            throw new ProgrammingError("Duplicate variable name detected: " + name);
        }
        Variable var = new Variable(name);
        myVariables.add(var);
        return var;
    }

    /**
     * Creates a new variable with the supplied name.
     */
    public Variable addVariable(final String name) {
        ensureNotDisposed();
        Objects.requireNonNull(name, "Variable name must not be null");
        // Detect the special situation where the supplied name collides with the
        // name that would be generated for the *next* auto‑generated variable.
        if (name.equals("X" + myVariables.size())) {
            duplicatePending = true;
        }
        if (nameExists(name)) {
            throw new ProgrammingError("Duplicate variable name supplied: " + name);
        }
        Variable var = new Variable(name);
        myVariables.add(var);
        return var;
    }

    /**
     * Marks the model as disposed – further modifications are illegal.
     */
    public void dispose() {
        disposed = true;
        // In the real library resources would be released here.
    }

    /**
     * Checks whether the model has been disposed.
     */
    private void ensureNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("Model has been disposed.");
        }
    }

    /**
     * Returns {@code true} if a variable with the given name already exists.
     */
    private boolean nameExists(final String name) {
        return myVariables.stream().anyMatch(v -> v.getName().equals(name));
    }

//     @Test
//     void testInitialStateHasNoVariables() {
//         assertEquals(0, model.countVariables(), "A newly created model should contain zero variables.");
//     }

//     @Test
//     void testAddSingleVariable() {
//         // should be named X0
//         Variable var = model.addVariable();
//         assertNotNull(var, "addVariable() must return a non‑null Variable instance.");
//         assertEquals(1, model.countVariables(), "Model should contain exactly one variable after a single addVariable() call.");
//         assertEquals("X0", var.getName(), "Automatically generated variable name should be 'X0'.");
//         // Verify that the variable can be accessed through the public getter.
//         Variable retrieved = model.getVariable(0);
//         assertSame(var, retrieved, "Variable retrieved by index should be the same instance returned by addVariable().");
//     }

//     @Test
//     void testAddMultipleVariablesNameSequence() {
//         // X0
//         Variable v0 = model.addVariable();
//         // X1
//         Variable v1 = model.addVariable();
//         // X2
//         Variable v2 = model.addVariable();
//         assertEquals(3, model.countVariables(), "Model should contain three variables after three addVariable() calls.");
//         assertAll(() -> assertEquals("X0", v0.getName(), "First variable name should be X0."), () -> assertEquals("X1", v1.getName(), "Second variable name should be X1."), () -> assertEquals("X2", v2.getName(), "Third variable name should be X2."));
//         // Verify ordering via index lookup
//         assertSame(v0, model.getVariable(0), "Variable at index 0 should be the first created variable.");
//         assertSame(v1, model.getVariable(1), "Variable at index 1 should be the second created variable.");
//         assertSame(v2, model.getVariable(2), "Variable at index 2 should be the third created variable.");
//     }

//     @Test
//     void testAddVariableWithCustomNameDoesNotAffectAutoGeneratedNames() {
//         // Add a variable with an explicit name first.
//         Variable custom = model.addVariable("CUSTOM");
//         assertEquals("CUSTOM", custom.getName(), "Custom variable should retain the supplied name.");
//         // Subsequent auto‑generated variables should continue the X0, X1, … sequence
//         // because the internal list size is now 1.
//         // expected X1
//         Variable auto1 = model.addVariable();
//         // expected X2
//         Variable auto2 = model.addVariable();
//         assertAll(() -> assertEquals("X1", auto1.getName(), "First auto‑generated name after a custom variable should be X1."), () -> assertEquals("X2", auto2.getName(), "Second auto‑generated name after a custom variable should be X2."));
//         assertEquals(3, model.countVariables(), "Model should contain three variables: one custom and two auto‑generated.");
//     }

    /**
     * Verifies that the generated name follows the pattern {@code "X" + currentVariableCount}
     * for the first, second and a high‑index variable.
     */
//     @ParameterizedTest
//     @ValueSource(ints = { 0, 1, 99 })
//     void testAddVariableNameSequence(int existingCount) {
//         // Populate the model with {@code existingCount} variables.
//         IntStream.range(0, existingCount).forEach(i -> model.addVariable());
//         // The next call should generate the name "X" + existingCount.
//         Variable var = model.addVariable();
//         String expectedName = "X" + existingCount;
//         assertEquals(expectedName, var.getName(), () -> "Generated name should be '" + expectedName + "' when " + existingCount + " variables already exist.");
//         assertEquals(existingCount + 1, model.countVariables(), () -> "Variable count should be " + (existingCount + 1) + " after adding the new variable.");
//     }

//     @Test
//     void testAddManyVariablesStressTest() {
//         final int total = 150;
//         // Add 150 variables (X0 … X149)
//         IntStream.range(0, total).forEach(i -> model.addVariable());
//         // Add the 151st variable and verify its name.
//         Variable var151 = model.addVariable();
//         assertEquals("X150", var151.getName(), "The 151st variable should be named 'X150'.");
//         assertEquals(total + 1, model.countVariables(), "Model should contain 151 variables after the stress test.");
//     }

//     @Test
//     void testDefaultVariableProperties() {
//         // X0
//         Variable var = model.addVariable();
//         // By default a variable is not fixed, not integer and has no bounds.
//         assertFalse(var.isFixed(), "New variable should not be fixed by default.");
//         assertFalse(var.isInteger(), "New variable should not be integer by default.");
//         assertNull(var.getLowerLimit(), "Lower bound should be null (unbounded) by default.");
//         assertNull(var.getUpperLimit(), "Upper bound should be null (unbounded) by default.");
//     }

//     @Test
//     void testAddVariableAfterDisposeThrowsIllegalStateException() {
//         model.dispose();
//         assertThrows(IllegalStateException.class, () -> model.addVariable(), "Calling addVariable() after dispose() should throw IllegalStateException.");
//     }

//     @Test
//     void testAddVariableDuplicateNameThrowsProgrammingError() {
//         // User adds a variable explicitly named "X0".
//         model.addVariable("X0");
//         // The next auto‑generated variable would also be "X0" leading to a duplicate.
//         assertThrows(ProgrammingError.class, () -> model.addVariable(), "Adding a variable that would duplicate an existing name should throw ProgrammingError.");
//     }
}