package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isReferenced_Test {

    private ExpressionsBasedModel model;

    private Variable varA;

    private Variable varB;

    /**
     * Helper class used only for the parameterised test above. It creates a fresh
     * {@link ExpressionsBasedModel}, adds a variable, optionally adds a reference,
     * and then returns the variable.
     */
    private static final class VariablePlaceholder extends Variable {

        private final Variable delegate;

        VariablePlaceholder(boolean addReference) {
            // Create a temporary model and variable.
            ExpressionsBasedModel tmpModel = new ExpressionsBasedModel();
            delegate = tmpModel.addVariable();
            if (addReference) {
                tmpModel.addReference(delegate.getIndex());
            }
        }

        @Override
        public IntIndex getIndex() {
            return delegate.getIndex();
        }

        @Override
        public String toString() {
            return "VariablePlaceholder(index=" + delegate.getIndex() + ")";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test contamination.
        model = new ExpressionsBasedModel();
        // Add two variables – they will automatically receive distinct indices.
        // index 0
        varA = model.addVariable();
        // index 1
        varB = model.addVariable();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit disposal,
        // but we nullify the fields to aid garbage collection and to make the intent clear.
        model = null;
        varA = null;
        varB = null;
    }

    /**
     * Supplies a stream of arguments where the {@link ExpressionsBasedModel} instance
     * (the field {@code model}) has already been populated with the appropriate
     * references. Each argument consists of the variable to test and the expected
     * boolean result.
     */
    static Stream<Arguments> provideReferenceScenarios() {
        // NOTE: The model field is not yet initialised when this static method runs.
        // Therefore we create a fresh model inside each argument and return both
        // the variable and the expected value. The test method will ignore the
        // temporary model – the real {@code model} field is set up by @BeforeEach.
        // The purpose of this parameterised test is only to demonstrate the
        // technique; the concrete scenarios are also covered by the explicit tests above.
        return Stream.of(// No reference added → false
        Arguments.of(new VariablePlaceholder(false), false), // Reference added → true
        Arguments.of(new VariablePlaceholder(true), true));
    }

    @Test
    void testIsReferenced_true() {
        // Arrange: add a reference to varA's index.
        IntIndex idxA = varA.getIndex();
        model.addReference(idxA);
        // Act & Assert
        assertTrue(model.isReferenced(varA), "Variable A should be reported as referenced");
        assertFalse(model.isReferenced(varB), "Variable B should NOT be reported as referenced");
    }

    @Test
    void testIsReferenced_falseWhenNoReferenceAdded() {
        // No references have been added at this point.
        assertFalse(model.isReferenced(varA), "Variable A should NOT be reported as referenced");
        assertFalse(model.isReferenced(varB), "Variable B should NOT be reported as referenced");
    }

    @Test
    void testIsReferenced_nullVariable() {
        // The method dereferences the argument, so a null argument must result in NPE.
        assertThrows(NullPointerException.class, () -> model.isReferenced(null));
    }

    @Test
    void testIsReferenced_sameIndexDifferentIntIndexObject() {
        // Create a variable and then create a *different* IntIndex instance that
        // represents the same logical index value.
        // gets index 0 (or next free)
        Variable varX = model.addVariable();
        // obtain the primitive index
        int idxValue = varX.getIndex().intValue();
        // distinct object, same value
        IntIndex anotherIdx = model.toIntIndex(idxValue);
        // Add the distinct IntIndex to the reference set.
        model.addReference(anotherIdx);
        // The variable should still be reported as referenced because Set.contains
        // relies on {@code IntIndex.equals}, which is based on the index value.
        assertTrue(model.isReferenced(varX), "Variable with matching index should be referenced");
    }

    @Test
    void testIsReferenced_multipleVariables() {
        // Add two variables and reference both of them.
        // index 2
        Variable varX = model.addVariable();
        // index 3
        Variable varY = model.addVariable();
        model.addReference(varX.getIndex());
        model.addReference(varY.getIndex());
        assertTrue(model.isReferenced(varX), "First variable should be referenced");
        assertTrue(model.isReferenced(varY), "Second variable should be referenced");
    }

    @Test
    void testIsReferenced_unrelatedIntIndex() {
        // Add a reference for an index that does not belong to any variable in the model.
        model.addReference(model.toIntIndex(99));
        // Create a fresh variable (its index will be 0 because the model started empty).
        Variable varZ = model.addVariable();
        // The variable's index (0) is not the unrelated reference (99), so it must be false.
        assertFalse(model.isReferenced(varZ), "Variable should not be referenced by an unrelated index");
    }

    @Test
    void testIsReferenced_variableFromDifferentModel() {
        // Model A – create a variable and add its index to A's reference set.
        ExpressionsBasedModel modelA = new ExpressionsBasedModel();
        // index 0 in modelA
        Variable varA = modelA.addVariable();
        modelA.addReference(varA.getIndex());
        // Model B – a completely separate instance.
        ExpressionsBasedModel modelB = new ExpressionsBasedModel();
        // also index 0, but belongs to modelB
        Variable varB = modelB.addVariable();
        // Even though the *numeric* index is the same, the reference sets are independent.
        assertFalse(modelB.isReferenced(varB), "Variable from a different model must not be reported as referenced");
    }

    @ParameterizedTest(name = "[{index}] variable {0} expected {1}")
    @MethodSource("provideReferenceScenarios")
    void testIsReferenced_parameterised(Variable variable, boolean expected) {
        // The reference set is prepared by the argument provider.
        assertEquals(expected, model.isReferenced(variable), () -> "Variable " + variable + " expected referenced = " + expected);
    }
}
