package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addSpecialOrderedSet_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
    }

    @Test
    void testAddSpecialOrderedSetSuccess() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(10);
        List<Variable> orderedSet = List.of(v1, v2);
        // The method should complete without throwing an exception.
        assertDoesNotThrow(() -> model.addSpecialOrderedSet(orderedSet, 1, constraint));
    }

    @Test
    void testAddSpecialOrderedSetInvalidType() {
        Variable v = model.addVariable();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(5);
        Collection<Variable> set = List.of(v);
        // type <= 0 must trigger ProgrammingError
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 0, constraint));
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, -3, constraint));
    }

    @Test
    void testAddSpecialOrderedSetLinkedToNotConstraint() {
        Variable v = model.addVariable();
        // Expression without any bounds – not a constraint.
        Expression notConstraint = model.newExpression("nc");
        Collection<Variable> set = List.of(v);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 1, notConstraint));
    }

    @Test
    void testAddSpecialOrderedSetVariableIsNull() {
        Variable v = model.addVariable();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(1);
        Collection<Variable> set = new ArrayList<>();
        set.add(v);
        set.add(null);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 1, constraint));
    }

    @Test
    void testAddSpecialOrderedSetVariableWithoutIndex() {
        // Create a Variable that has never been added to the model – its index is null.
        Variable unattachedVar = new Variable();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(1);
        Collection<Variable> set = List.of(unattachedVar);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 1, constraint));
    }

    @ParameterizedTest(name = "type = {0} should trigger ProgrammingError")
    @CsvSource({ "0", "-3" })
    void testInvalidSosType(int invalidType) {
        Variable v = model.addVariable();
        Collection<Variable> orderedSet = List.of(v);
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(10);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, invalidType, constraint));
        assertTrue(ex.getMessage().contains("Invalid SOS type"));
    }

    @Test
    void testLinkedExpressionNotConstraint() {
        Variable v = model.addVariable();
        Collection<Variable> orderedSet = List.of(v);
        // Create an expression without bounds – it is not considered a constraint.
        Expression notConstraint = model.newExpression("nc");
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, notConstraint));
        assertTrue(ex.getMessage().contains("linked to expression needs to be a constraint"));
    }

    @Test
    void testVariableNullInOrderedSet() {
        Variable v = model.addVariable();
        Collection<Variable> orderedSet = new ArrayList<>();
        orderedSet.add(v);
        orderedSet.add(null);
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(1);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, constraint));
        assertTrue(ex.getMessage().contains("Variables must be already inserted"));
    }

    @Test
    void testVariableWithoutIndex() {
        // Create a Variable instance that has never been added to the model.
        // Such a variable has a null index and should trigger the appropriate error.
        Variable unattachedVar = new Variable();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(1);
        Collection<Variable> orderedSet = List.of(unattachedVar);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, constraint));
        assertTrue(ex.getMessage().contains("Variables must be already inserted"));
    }

    @Test
    void testEmptyOrderedSet() {
        Collection<Variable> emptySet = Collections.emptyList();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(10);
        // Should succeed – an empty SOS is allowed.
        assertDoesNotThrow(() -> model.addSpecialOrderedSet(emptySet, 1, constraint));
    }

    @Test
    void testValidSpecialOrderedSetAddition() {
        Variable varA = model.addVariable();
        Variable varB = model.addVariable();
        Variable varC = model.addVariable();
        // Retrieve the IntIndex values for later verification (if needed)
        IntIndex idxA = model.toIntIndex(varA.getIndex().intValue());
        IntIndex idxB = model.toIntIndex(varB.getIndex().intValue());
        IntIndex idxC = model.toIntIndex(varC.getIndex().intValue());
        List<Variable> orderedSet = List.of(varA, varB, varC);
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(100);
        // The call should complete without error.
        assertDoesNotThrow(() -> model.addSpecialOrderedSet(orderedSet, 2, constraint));
        // Optional sanity check: the variables are indeed part of the model.
        assertNotNull(idxA);
        assertNotNull(idxB);
        assertNotNull(idxC);
    }

    @Test
    void testNullOrderedSetArgument() {
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(10);
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(null, 1, constraint));
    }

    @Test
    void testNullLinkedToArgument() {
        Variable v = model.addVariable();
        Collection<Variable> orderedSet = List.of(v);
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(orderedSet, 1, null));
    }

    @Test
    void testAddSpecialOrderedSetSuccessBasic() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Expression constraint = model.newExpression("c");
        constraint.lower(0).upper(10);
        List<Variable> orderedSet = List.of(v1, v2);
        assertDoesNotThrow(() -> model.addSpecialOrderedSet(orderedSet, 1, constraint));
    }
}