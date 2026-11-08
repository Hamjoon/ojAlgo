package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.api.extension.ExtendWith;

class ExpressionsBasedModel_indexOfFreeVariable_3_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    private Variable varC;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for every test case.
        model = new ExpressionsBasedModel();
        // Three variables are added. The third one will be made “fixed”
        // (lower bound equals upper bound) so it must not appear in the
        // free‑variable list.
        varA = model.newVariable("A").lower(0).upper(10).weight(1);
        varB = model.newVariable("B").lower(0).upper(10).weight(1);
        // fixed variable
        varC = model.newVariable("C").lower(5).upper(5).weight(0);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose of the model to release any native resources
        // that a concrete solver might have allocated.
        model.dispose();
    }

    private static Stream<Arguments> basicScenariosProvider() {
        return Stream.of(// null variable → NullPointerException
        Arguments.of("null variable → NPE", (Function<ExpressionsBasedModel, Variable>) m -> null, null, NullPointerException.class), // newly added free variable → index 0
        Arguments.of("newly added free variable → index 0", (Function<ExpressionsBasedModel, Variable>) m -> m.newVariable("freeVar").lower(0).upper(10).weight(1), 0, null), // fixed variable (lower == upper) → -1
        Arguments.of("fixed variable → -1", (Function<ExpressionsBasedModel, Variable>) m -> m.newVariable("fixedVar").lower(5).upper(5).weight(0), -1, null), // orphan variable (never added) → -1
        Arguments.of("orphan variable → -1", (Function<ExpressionsBasedModel, Variable>) m -> {
            try {
                java.lang.reflect.Constructor<Variable> ctor = Variable.class.getDeclaredConstructor(String.class);
                ctor.setAccessible(true);
                return ctor.newInstance("orphanVar");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, -1, null), // third free variable when second is fixed → index 1
        Arguments.of("third variable with second fixed → index 1", (Function<ExpressionsBasedModel, Variable>) m -> {
            m.newVariable("firstFree").lower(0).upper(10).weight(1);
            m.newVariable("secondFixed").lower(5).upper(5).weight(0);
            return m.newVariable("thirdFree").lower(0).upper(10).weight(1);
        }, 1, null));
    }

    private static Stream<Arguments> integerVariableProvider() {
        return Stream.of(Arguments.of("integer variable (non‑fixed) → -1", (Function<ExpressionsBasedModel, Variable>) m -> m.newVariable("intVar").lower(0).upper(10).weight(1).integer(true), -1));
    }

    @Test
    void testIndexOfFreeVariable_mapsCorrectly() {
        // GIVEN – a model with two free variables (A, B) and one fixed variable (C)
        // The free‑variable list is ordered by the global index of the variables.
        // Hence we expect:
        //   A -> free index 0
        //   B -> free index 1
        //   C -> not free → index -1 (convention used by the implementation)
        int idxA = model.indexOfFreeVariable(varA);
        int idxB = model.indexOfFreeVariable(varB);
        int idxC = model.indexOfFreeVariable(varC);
        assertEquals(0, idxA, "Variable A should be the first free variable");
        assertEquals(1, idxB, "Variable B should be the second free variable");
        assertEquals(-1, idxC, "Fixed variable C must not have a free‑variable index");
    }

    @Test
    void testIndexOfFreeVariable_consistencyWithIndexOf() {
        // GIVEN – the same model as above
        // Verify that the overload taking a global index yields the same
        // result as the overload taking a {@link Variable}.
        int globalIdxA = model.indexOf(varA);
        int globalIdxB = model.indexOf(varB);
        int globalIdxC = model.indexOf(varC);
        int freeIdxViaVarA = model.indexOfFreeVariable(varA);
        int freeIdxViaVarB = model.indexOfFreeVariable(varB);
        int freeIdxViaVarC = model.indexOfFreeVariable(varC);
        int freeIdxViaGlobalA = model.indexOfFreeVariable(globalIdxA);
        int freeIdxViaGlobalB = model.indexOfFreeVariable(globalIdxB);
        int freeIdxViaGlobalC = model.indexOfFreeVariable(globalIdxC);
        assertEquals(freeIdxViaVarA, freeIdxViaGlobalA, "Free index for A should be identical via Variable and global index");
        assertEquals(freeIdxViaVarB, freeIdxViaGlobalB, "Free index for B should be identical via Variable and global index");
        assertEquals(freeIdxViaVarC, freeIdxViaGlobalC, "Free index for fixed C should be identical via Variable and global index");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{0}")
    @MethodSource("basicScenariosProvider")
    void testBasicScenarios(String description, Function<ExpressionsBasedModel, Variable> variableFactory, Integer expectedIndex, Class<? extends Throwable> expectedException) {
        // each invocation gets a brand‑new model to avoid cross‑test interference
        ExpressionsBasedModel localModel = new ExpressionsBasedModel();
        Variable variable = variableFactory.apply(localModel);
        if (expectedException != null) {
            assertThrows(expectedException, () -> localModel.indexOfFreeVariable(variable), description);
        } else {
            int actual = localModel.indexOfFreeVariable(variable);
            assertEquals(expectedIndex.intValue(), actual, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{0}")
    @MethodSource("integerVariableProvider")
    void testIntegerVariableHandling(String description, Function<ExpressionsBasedModel, Variable> variableFactory, int expectedIndex) {
        ExpressionsBasedModel localModel = new ExpressionsBasedModel();
        Variable var = variableFactory.apply(localModel);
        int actual = localModel.indexOfFreeVariable(var);
        assertEquals(expectedIndex, actual, description);
    }

    // -----------------------------------------------------------------
    @Test
    void testVariableFromDifferentModel() {
        // Create a second model and add a variable to it.
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        Variable otherVar = otherModel.newVariable("otherModelVar").lower(0).upper(10).weight(1);
        // The variable does not belong to the primary test model.
        assertEquals(-1, model.indexOfFreeVariable(otherVar), "Variable from a different model must return -1");
    }

    // -----------------------------------------------------------------
    @Test
    void testVariableBecomesFixedAfterBeingFree() {
        // Start with a free variable.
        Variable v = model.newVariable("freeThenFixed").lower(0).upper(10).weight(1);
        // Initially it is free → index 0 (because it is the first variable after the
        // three variables created in @BeforeEach).
        int idxBefore = model.indexOfFreeVariable(v);
        assertTrue(idxBefore >= 0, "Variable should be free before fixing");
        // Fix the variable by making lower == upper.
        v.lower(5).upper(5);
        // After fixing it must no longer be considered free.
        assertEquals(-1, model.indexOfFreeVariable(v), "Variable that became fixed must return -1");
    }

    // -----------------------------------------------------------------
    @Test
    void testMixedFreeFixedIntegerOrdering() {
        // Build the model exactly as described in the JSON scenario.
        // Order: freeVarA (free), fixedVar (fixed), intVar (integer), freeVarB (free)
        Variable freeVarA = model.newVariable("freeVarA").lower(0).upper(10).weight(1);
        Variable fixedVar = model.newVariable("fixedVar").lower(5).upper(5).weight(0);
        Variable intVar = model.newVariable("intVar").lower(0).upper(10).weight(1).integer(true);
        Variable freeVarB = model.newVariable("freeVarB").lower(0).upper(10).weight(1);
        // The free‑variable list (excluding fixed and integer variables) is:
        // [freeVarA, freeVarB] → freeVarB should have index 1.
        int idx = model.indexOfFreeVariable(freeVarB);
        assertEquals(1, idx, "Second free variable should have free index 1");
    }

    // -----------------------------------------------------------------
    @Test
    void testVariableWithInfeasibleBounds() {
        // lower > upper
        Variable // lower > upper
        infeasibleVar = // lower > upper
        model.newVariable("infeasibleVar").lower(10).upper(0).weight(1);
        // Such a variable cannot be free; the method should return -1.
        assertEquals(-1, model.indexOfFreeVariable(infeasibleVar), "Variable with infeasible bounds must return -1");
    }

    // -----------------------------------------------------------------
    @Test
    void testOperationAfterModelDisposal() {
        // Dispose the model first.
        model.dispose();
        // Any call to indexOfFreeVariable after disposal must throw ProgrammingError.
        assertThrows(ProgrammingError.class, () -> model.indexOfFreeVariable(varA), "Calling indexOfFreeVariable on a disposed model should throw ProgrammingError");
    }
}
