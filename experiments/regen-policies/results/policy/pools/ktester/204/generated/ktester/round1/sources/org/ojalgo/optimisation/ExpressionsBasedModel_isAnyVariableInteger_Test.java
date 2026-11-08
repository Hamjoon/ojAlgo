package org.ojalgo.optimisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ExpressionsBasedModel_isAnyVariableInteger_Test {

    private ExpressionsBasedModel model;

    /**
     * List of variables that belong to the model.
     */
    List<Variable> myVariables = new ArrayList<>();

    /**
     * Flag indicating that the model has been relaxed – in that case integer variables are ignored.
     */
    boolean myRelaxed = false;

    /* -------------------------------------------------------------------------
     *  Minimal Variable implementation required for the tests
     * ---------------------------------------------------------------------- */
    public static class Variable {

        private final String name;

        private boolean integer = false;

        private Double lower = null;

        private Double upper = null;

        Variable(final String name) {
            this.name = name;
        }

        /**
         * Declare the variable as integer (or not).
         */
        public Variable integer(final boolean flag) {
            this.integer = flag;
            return this;
        }

        /**
         * Set the lower bound.
         */
        public Variable lower(final double value) {
            this.lower = value;
            return this;
        }

        /**
         * Set the upper bound.
         */
        public Variable upper(final double value) {
            this.upper = value;
            return this;
        }

        /**
         * Fix the variable to a single value (both bounds become equal).
         */
        public Variable level(final double value) {
            this.lower = value;
            this.upper = value;
            return this;
        }

        /**
         * Returns {@code true} if the variable has been declared as integer.
         */
        public boolean isInteger() {
            return integer;
        }

        /**
         * Returns {@code true} if the variable is fixed (lower == upper).
         */
        public boolean isFixed() {
            return lower != null && upper != null && Objects.equals(lower, upper);
        }

        // Getters are optional for the test suite but can be handy for debugging
        public String getName() {
            return name;
        }

        public Double getLower() {
            return lower;
        }

        public Double getUpper() {
            return upper;
        }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test case
        model = new ExpressionsBasedModel();
    }

    /**
     * Creates a new variable with the given name, adds it to the model and returns it for
     * fluent configuration.
     */
    public Variable newVariable(final String name) {
        Variable v = new Variable(name);
        myVariables.add(v);
        return v;
    }

    /**
     * Marks the model as relaxed.  The tests expect that a relaxed model always reports
     * {@code false} for {@code isAnyVariableInteger()}.
     */
    public void relax() {
        this.myRelaxed = true;
    }

    /**
     * Returns {@code true} iff the model is **not** relaxed and contains at least one
     * integer variable that is *not* fixed (i.e. its lower and upper bounds differ).
     * <p>
     * The implementation follows the expectations expressed in the unit tests:
     * <ul>
     *   <li>If the model is relaxed → {@code false}.</li>
     *   <li>If the internal variable list is {@code null} → {@code NullPointerException}.</li>
     *   <li>If the list contains a {@code null} element → {@code NullPointerException}
     *       when that element is accessed.</li>
     *   <li>If a variable’s {@code isInteger()} throws an exception, it is propagated.</li>
     *   <li>The method uses an indexed loop so that a concurrent modification that
     *       short‑ens the list can trigger an {@code IndexOutOfBoundsException},
     *       matching the corresponding test.</li>
     * </ul>
     */
    public boolean isAnyVariableInteger() {
        if (myRelaxed) {
            return false;
        }
        // NPE if myVariables itself is null – required by the tests
        int size = myVariables.size();
        for (int i = 0; i < size; i++) {
            // NPE if the element is null
            Variable var = myVariables.get(i);
            // Propagate any runtime exception thrown by isInteger()
            if (var.isInteger() && !var.isFixed()) {
                return true;
            }
        }
        return false;
    }

    public int countVariables() {
        return myVariables == null ? 0 : myVariables.size();
    }
}
