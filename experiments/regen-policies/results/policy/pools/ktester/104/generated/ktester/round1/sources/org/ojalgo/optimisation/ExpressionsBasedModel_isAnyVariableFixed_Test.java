package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyVariableFixed_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for each test case
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any internal resources
        model.dispose();
    }

    @Test
    void testIsAnyVariableFixed_EmptyModel() {
        // No variables have been added – should be false
        assertFalse(model.isAnyVariableFixed(), "An empty model must report that no variable is fixed.");
    }

    @Test
    void testIsAnyVariableFixed_NoFixedVariables() {
        // Add a few free variables (only bounds, no fixing)
        model.addVariable().lower(0).upper(10);
        model.addVariable().lower(-5).upper(5);
        // completely free
        model.addVariable();
        assertFalse(model.isAnyVariableFixed(), "Model with only free variables must report that no variable is fixed.");
    }

    @Test
    void testIsAnyVariableFixed_SingleFixedVariable() {
        // Create a variable that is fixed by giving it the same lower and upper bound
        // fixed at 3
        model.addVariable().lower(3).upper(3);
        assertTrue(model.isAnyVariableFixed(), "Model with a single fixed variable must report that a variable is fixed.");
    }

    @Test
    void testIsAnyVariableFixed_MultipleVariablesOneFixed() {
        // Mix of free and fixed variables
        // free
        model.addVariable().lower(0).upper(10);
        // fixed
        model.addVariable().lower(5).upper(5);
        // free
        model.addVariable().lower(-2).upper(2);
        assertTrue(model.isAnyVariableFixed(), "Model with at least one fixed variable must report true.");
    }

    @Test
    void testIsAnyVariableFixed_AllFixedVariables() {
        // All variables are fixed
        model.addVariable().lower(1).upper(1);
        model.addVariable().lower(2).upper(2);
        model.addVariable().lower(3).upper(3);
        assertTrue(model.isAnyVariableFixed(), "Model where every variable is fixed must report true.");
    }

    @Test
    void testIsAnyVariableFixed_SingleVariableFixedViaFix() {
        // Variable is fixed by calling its fix(...) method
        model.addVariable().fix(BigDecimal.valueOf(3));
        assertTrue(model.isAnyVariableFixed(), "A model with a single fixed variable (via fix()) must report true.");
    }

    @Test
    void testIsAnyVariableFixed_VariableBecomesFixedAfterCreation() {
        // Add a free variable first
        Variable v = model.addVariable().lower(0).upper(10);
        // Later tighten bounds to the same value – now fixed
        v.lower(7).upper(7);
        assertTrue(model.isAnyVariableFixed(), "A variable that becomes fixed after creation must be detected.");
    }

    @Test
    void testIsAnyVariableFixed_NullMyVariablesField() throws Exception {
        // Force the private myVariables field to null via reflection
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, null);
        // The stream operation on a null list should throw NPE
        assertThrows(NullPointerException.class, () -> model.isAnyVariableFixed(), "Calling isAnyVariableFixed() when myVariables is null must throw NullPointerException.");
    }

    @Test
    void testIsAnyVariableFixed_NullElementInsideVariableList() throws Exception {
        // Obtain the internal list and insert a null entry
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) f.get(model);
        // inject null element
        vars.add(null);
        // The method reference Variable::isFixed will be applied to the null entry → NPE
        assertThrows(NullPointerException.class, () -> model.isAnyVariableFixed(), "A null element inside myVariables must cause NullPointerException.");
    }

    @Test
    void testIsAnyVariableFixed_VariableIsFixedThrowsException() throws Exception {
        // Create a custom Variable subclass that throws an exception from isFixed()
        Variable faulty = new Variable() {

            @Override
            public boolean isFixed() {
                throw new IllegalStateException("forced");
            }
        };
        // Inject the custom variable into the internal variable list
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> vars = (List<Variable>) f.get(model);
        vars.add(faulty);
        // The exception should propagate out of isAnyVariableFixed()
        assertThrows(IllegalStateException.class, () -> model.isAnyVariableFixed(), "Exception thrown from Variable.isFixed() must propagate.");
    }
}