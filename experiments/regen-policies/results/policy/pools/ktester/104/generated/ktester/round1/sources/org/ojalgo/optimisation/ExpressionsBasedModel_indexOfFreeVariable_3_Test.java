package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_3_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test.
        model = new ExpressionsBasedModel();
        // Three variables – they will be configured differently in each test.
        varA = model.newVariable("A");
        varB = model.newVariable("B");
        varC = model.newVariable("C");
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any internal structures.
        model.dispose();
    }

    /**
     * Provides the basic scenario test data.
     *
     * <p>All variables are created fresh for each invocation of the test method,
     * therefore we configure them inside the stream.
     */
    static Stream<Arguments> basicScenarioProvider() {
        // Helper to create a model and a variable for the "unregisteredVariable" case.
        ExpressionsBasedModel dummyModel = new ExpressionsBasedModel();
        // never added to any model
        Variable unregistered = new Variable("unregistered", 999);
        // Variable that will be fixed later in the test data.
        ExpressionsBasedModel modelForFixed = new ExpressionsBasedModel();
        Variable fixedVar = modelForFixed.newVariable("fixed");
        // make it fixed
        fixedVar.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        // Integer variable (still free).
        ExpressionsBasedModel modelForInteger = new ExpressionsBasedModel();
        Variable integerVar = modelForInteger.newVariable("integer");
        // mark as integer, not fixed
        integerVar.integer(true);
        // Duplicate name – first instance is the one we will test.
        ExpressionsBasedModel modelForDuplicate = new ExpressionsBasedModel();
        Variable dup1 = modelForDuplicate.newVariable("dup");
        // second instance, ignored for this test
        modelForDuplicate.newVariable("dup");
        // Free variables for index 0 and 1.
        ExpressionsBasedModel modelForFree = new ExpressionsBasedModel();
        Variable freeVar0 = modelForFree.newVariable("free0");
        Variable freeVar1 = modelForFree.newVariable("free1");
        return Stream.of(// null reference → NPE (handled separately, see testNullVariable)
        Arguments.of("null variable should throw NPE", null, -999), Arguments.of("unregistered variable should return -1", unregistered, -1), Arguments.of("first free variable should have index 0", freeVar0, 0), Arguments.of("second free variable should have index 1", freeVar1, 1), Arguments.of("fixed variable should return -1", fixedVar, -1), Arguments.of("integer (non‑fixed) variable should be counted as free, index 0", integerVar, 0), Arguments.of("duplicate name – first instance should be free with index 0", dup1, 0));
    }

    @Test
    void testFreeVariableIndicesWhenAllVariablesAreFree() {
        // No variable is fixed – all are free.
        // Expected free‑variable indices are identical to the global indices.
        assertEquals(0, model.indexOfFreeVariable(varA), "First free variable should have free index 0");
        assertEquals(1, model.indexOfFreeVariable(varB), "Second free variable should have free index 1");
        assertEquals(2, model.indexOfFreeVariable(varC), "Third free variable should have free index 2");
    }

    @Test
    void testFreeVariableIndicesWithOneFixedVariable() {
        // Fix the first variable by giving it an equal lower and upper bound.
        // A variable with a single feasible value is considered *fixed*.
        varA.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        // After fixing varA, the free variables are varB and varC.
        // Their free indices should start at 0.
        assertEquals(0, model.indexOfFreeVariable(varB), "First free variable after fixing should have free index 0");
        assertEquals(1, model.indexOfFreeVariable(varC), "Second free variable after fixing should have free index 1");
        // The fixed variable itself should not appear in the free‑variable list.
        // The implementation returns -1 for a fixed variable (i.e. not found among free ones).
        assertEquals(-1, model.indexOfFreeVariable(varA), "Fixed variable should not have a free‑variable index");
    }

    @Test
    void testFreeVariableIndicesWithMultipleFixedVariables() {
        // Fix the first and third variables.
        varA.lower(BigDecimal.TEN).upper(BigDecimal.TEN);
        varC.lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        // Only varB remains free and should obtain free index 0.
        assertEquals(0, model.indexOfFreeVariable(varB), "Only free variable should have free index 0");
        // Fixed variables must return -1.
        assertEquals(-1, model.indexOfFreeVariable(varA), "First fixed variable should not have a free‑variable index");
        assertEquals(-1, model.indexOfFreeVariable(varC), "Second fixed variable should not have a free‑variable index");
    }

    @Test
    void testIndexOfFreeVariableForVariableNotInModel() {
        // Create a variable that has never been added to the model.
        Variable outsider = new Variable("outsider", 999);
        // The model does not know this variable, so the global index is -1
        // and consequently the free‑variable index is also -1.
        assertEquals(-1, model.indexOfFreeVariable(outsider), "Variable not belonging to the model should yield -1");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicScenarioProvider")
    void testBasicScenarios(String description, Variable variable, int expected) {
        assertEquals(expected, model.indexOfFreeVariable(variable), description);
    }

    /**
     * The null‑argument case cannot be expressed with the parameterised test above
     * because the test framework would try to call the method with a {@code null}
     * argument before the assertion. Therefore we test it separately.
     */
    @Test
    void testNullVariable() {
        assertThrows(NullPointerException.class, () -> model.indexOfFreeVariable(null), "Calling indexOfFreeVariable(null) must throw NullPointerException");
    }

    @Test
    void testInterleavedFixedAndFreeVariables() {
        // freeA – first variable, free
        Variable freeA = model.newVariable("freeA");
        // fixedB – second variable, fixed immediately
        Variable fixedB = model.newVariable("fixedB");
        // make it fixed
        fixedB.lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        // freeC – third variable, free
        Variable freeC = model.newVariable("freeC");
        // Expected free‑variable indices:
        // freeA -> 0, freeC -> 1, fixedB -> -1
        assertEquals(0, model.indexOfFreeVariable(freeA), "freeA should be free index 0");
        assertEquals(-1, model.indexOfFreeVariable(fixedB), "fixedB should not have a free index");
        assertEquals(1, model.indexOfFreeVariable(freeC), "freeC should be free index 1");
    }

    @Test
    void testIndexOfFreeVariableAfterDisposeThrowsIllegalStateException() {
        // Dispose the model first.
        model.dispose();
        // Any call after disposal should throw IllegalStateException.
        assertThrows(IllegalStateException.class, () -> model.indexOfFreeVariable(varA), "Calling indexOfFreeVariable on a disposed model must throw IllegalStateException");
    }

    @Test
    void testVariableFromAnotherModelIsNotRecognised() {
        // Create a second model and add a variable to it.
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        Variable otherVar = otherModel.newVariable("other");
        // The variable belongs to 'otherModel', not to 'model'.
        assertEquals(-1, model.indexOfFreeVariable(otherVar), "Variable from another model should yield -1");
    }

    @Test
    void testFixedVariableReturnsMinusOne() {
        // fix varA
        varA.lower(BigDecimal.TEN).upper(BigDecimal.TEN);
        assertEquals(-1, model.indexOfFreeVariable(varA), "A fixed variable must return -1 as free‑variable index");
    }
}