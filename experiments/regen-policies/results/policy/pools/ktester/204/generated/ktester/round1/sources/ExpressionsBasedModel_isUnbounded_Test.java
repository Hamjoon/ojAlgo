package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.type.ProgrammingError;

class ExpressionsBasedModel_isUnbounded_Test {

    /**
     * Simple data holder describing a variable to be added to the model.
     */
    private static final class VarSpec {

        // not used by the test, but kept for readability
        final String name;

        // null → no lower bound
        final BigDecimal lower;

        // null → no upper bound
        final BigDecimal upper;

        VarSpec(String name, BigDecimal lower, BigDecimal upper) {
            this.name = name;
            this.lower = lower;
            this.upper = upper;
        }
    }

    /**
     * Supplies the test cases defined in the JSON “isUnbounded” group.
     */
    private static Stream<Arguments> provideIsUnboundedCases() {
        return Stream.of(// Model has no variables → false
        Arguments.of(Collections.emptyList(), false, "Model has no variables; anyMatch on an empty stream should return false."), // All variables bounded → false
        Arguments.of(List.of(new VarSpec("x1", BigDecimal.valueOf(0), BigDecimal.valueOf(10)), new VarSpec("x2", BigDecimal.valueOf(-5), BigDecimal.valueOf(5))), false, "All added variables are bounded; anyMatch should not find an unbounded variable."), // One completely unbounded variable → true
        Arguments.of(List.of(new VarSpec("x1", BigDecimal.valueOf(0), BigDecimal.valueOf(10)), new VarSpec("xUnbounded", null, null)), true, "At least one variable is unbounded (both bounds null) → true."), // Missing lower bound only → true
        Arguments.of(List.of(new VarSpec("xUnboundedLower", null, BigDecimal.valueOf(100)), new VarSpec("xBounded", BigDecimal.valueOf(0), BigDecimal.valueOf(10))), true, "Variable with a missing lower bound is considered unbounded."), // Missing upper bound only → true
        Arguments.of(List.of(new VarSpec("xUnboundedUpper", BigDecimal.valueOf(-50), null)), true, "Variable with a missing upper bound is considered unbounded."));
    }

    /**
     * A model that contains only an unbounded variable must be reported as unbounded.
     */
    @Test
    void testIsUnboundedWhenVariableIsUnbounded() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // By default a newly created variable has no lower/upper limits → unbounded.
        // or model.newVariable()
        Variable v = model.addVariable();
        // No bounds are set on purpose.
        assertTrue(model.isUnbounded(), "Model should be unbounded when it contains at least one unbounded variable");
    }

    /**
     * A model that contains only bounded variables must not be reported as unbounded.
     */
    @Test
    void testIsUnboundedWhenAllVariablesAreBounded() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.addVariable().lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5));
        // fixed variable
        model.addVariable().lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(2));
        assertFalse(model.isUnbounded(), "Model should not be unbounded when all variables have finite bounds");
    }

    /**
     * Adding an unbounded variable after bounded ones should switch the model to unbounded.
     */
    @Test
    void testIsUnboundedAfterAddingUnboundedVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add a few bounded variables first.
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.addVariable().lower(BigDecimal.valueOf(-10)).upper(BigDecimal.valueOf(10));
        // Verify still bounded.
        assertFalse(model.isUnbounded(), "Model should be bounded before adding an unbounded variable");
        // Add an unbounded variable (no bounds set).
        // defaults to unbounded
        model.addVariable();
        assertTrue(model.isUnbounded(), "Model should become unbounded after an unbounded variable is added");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideIsUnboundedCases")
    void testIsUnboundedParameterized(List<VarSpec> specs, boolean expected, String description) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Add variables according to the specification.
        for (VarSpec spec : specs) {
            // name is irrelevant for the logic
            Variable v = model.addVariable();
            if (spec.lower != null) {
                v.lower(spec.lower);
            }
            if (spec.upper != null) {
                v.upper(spec.upper);
            }
        }
        assertEquals(expected, model.isUnbounded(), description);
    }

    /**
     * Inject a {@code null} entry into the internal variable list → NPE when streamed.
     */
    @Test
    void testIsUnbounded_nullVariableInList_throwsNullPointerException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Ensure the list exists by adding a normal variable first.
        model.addVariable();
        // Access the private field and inject a null element.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) field.get(model);
        // corrupt the list
        vars.add(null);
        assertThrows(NullPointerException.class, model::isUnbounded, "Streaming a list containing null should cause NullPointerException");
    }

    /**
     * Set the internal variable list itself to {@code null} → NPE when calling {@code stream()}.
     */
    @Test
    void testIsUnbounded_nullVariableList_throwsNullPointerException() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Replace the list with null via reflection.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        field.set(model, null);
        assertThrows(NullPointerException.class, model::isUnbounded, "Calling isUnbounded when myVariables is null should throw NullPointerException");
    }

    /**
     * A variable whose {@code isUnbounded()} throws {@link ProgrammingError} should propagate it.
     */
    @Test
    void testIsUnbounded_variableThrowsProgrammingError() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Create a mock Variable that throws ProgrammingError on isUnbounded().
        Variable badVar = mock(Variable.class);
        when(badVar.isUnbounded()).thenThrow(new ProgrammingError("mock error"));
        // Inject the mock into the internal list.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) field.get(model);
        vars.add(badVar);
        ProgrammingError thrown = assertThrows(ProgrammingError.class, model::isUnbounded, "The ProgrammingError thrown by a Variable must be propagated unchanged");
        assertEquals("mock error", thrown.getMessage());
    }
}
