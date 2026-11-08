package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addSpecialOrderedSet_Test {

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean presolver set before each test
        ExpressionsBasedModel.clearPresolvers();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up static presolvers so tests are independent
        ExpressionsBasedModel.clearPresolvers();
    }

    /**
     * A happy‑path test: a valid SOS is added and a presolver is registered.
     */
    @Test
    void testAddSpecialOrderedSetValid() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // three binary variables (lower 0, upper 1)
        Variable v0 = model.addVariable(Variable.make("v0")).lower(0).upper(1);
        Variable v1 = model.addVariable(Variable.make("v1")).lower(0).upper(1);
        Variable v2 = model.addVariable(Variable.make("v2")).lower(0).upper(1);
        // an expression that will act as the linking constraint (any bound makes it a constraint)
        Expression linking = model.newExpression("link");
        linking.upper(BigDecimal.valueOf(2));
        List<Variable> orderedSet = Arrays.asList(v0, v1, v2);
        model.addSpecialOrderedSet(orderedSet, 2, linking);
        // The static PRESOLVERS set should now contain exactly one presolver (the SOS)
        assertEquals(1, ExpressionsBasedModel.PRESOLVERS.size(), "A SpecialOrderedSet presolver should have been added");
    }

    /**
     * SOS type must be > 0, otherwise a ProgrammingError is thrown.
     */
    @Test
    void testAddSpecialOrderedSetInvalidType() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a real constraint expression – its isConstraint() result does not matter because
        // the type check is performed first
        Expression constraint = model.newExpression("c");
        constraint.upper(BigDecimal.ONE);
        List<Variable> emptySet = Collections.emptyList();
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(emptySet, 0, constraint), "type <= 0 should cause a ProgrammingError");
        assertEquals("Invalid SOS type!", ex.getMessage());
    }

    /**
     * The linked expression must be a constraint; otherwise a ProgrammingError is thrown.
     */
    @Test
    void testAddSpecialOrderedSetLinkedExpressionNotConstraint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable(Variable.make("v")).lower(0).upper(1);
        // expression that is NOT a constraint (no bounds set)
        Expression notConstraint = model.newExpression("nc");
        List<Variable> orderedSet = Collections.singletonList(v);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, notConstraint), "A non‑constraint expression should cause a ProgrammingError");
        assertEquals("The linked to expression needs to be a constraint!", ex.getMessage());
    }

    /**
     * Variables must already be inserted in the model; otherwise a ProgrammingError is thrown.
     */
    @Test
    void testAddSpecialOrderedSetVariableNotInserted() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable created but NOT added to the model
        Variable external = Variable.make("external").lower(0).upper(1);
        Expression linking = model.newExpression("link");
        linking.upper(BigDecimal.ONE);
        List<Variable> orderedSet = Collections.singletonList(external);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, linking), "A variable not inserted in the model should cause a ProgrammingError");
        assertEquals("Variables must be already inserted in the model!", ex.getMessage());
    }

    /**
     * All variables in the ordered set must be binary; otherwise a ProgrammingError is thrown.
     */
    @Test
    void testAddSpecialOrderedSetVariableNotBinary() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with bounds 0..2 – not binary
        Variable v = model.addVariable(Variable.make("v")).lower(0).upper(2);
        Expression linking = model.newExpression("link");
        linking.upper(BigDecimal.ONE);
        List<Variable> orderedSet = Collections.singletonList(v);
        // The public method we are testing checks binaryness and throws ProgrammingError
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, linking), "A non‑binary variable should cause a ProgrammingError");
    }

    /**
     * The orderedSet argument is null → NullPointerException when size() is accessed.
     */
    @Test
    void testAddSpecialOrderedSetNullOrderedSet() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression constraint = model.newExpression("c");
        constraint.upper(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(null, 1, constraint), "Passing null as orderedSet should result in NPE");
    }

    /**
     * The linkedTo argument is null → NullPointerException when isConstraint() is invoked.
     */
    @Test
    void testAddSpecialOrderedSetNullLinkedTo() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable(Variable.make("v")).lower(0).upper(1);
        List<Variable> orderedSet = Collections.singletonList(v);
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(orderedSet, 1, null), "Passing null as linkedTo should result in NPE");
    }

    /**
     * The orderedSet contains a null Variable → ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetVariableNullInCollection() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Expression constraint = model.newExpression("c");
        constraint.upper(BigDecimal.ONE);
        List<Variable> orderedSet = Arrays.asList((Variable) null);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, constraint), "A null variable in the collection should cause a ProgrammingError");
        assertEquals("Variables must be already inserted in the model!", ex.getMessage());
    }

    /**
     * The orderedSet contains a Variable that has not been inserted in the model
     * (its getIndex() returns null) → ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetVariableWithoutIndex() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable created but NOT added to the model
        Variable external = Variable.make("external").lower(0).upper(1);
        Expression constraint = model.newExpression("c");
        constraint.upper(BigDecimal.ONE);
        List<Variable> orderedSet = Collections.singletonList(external);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, constraint), "A variable without an index should cause a ProgrammingError");
        assertEquals("Variables must be already inserted in the model!", ex.getMessage());
    }
}