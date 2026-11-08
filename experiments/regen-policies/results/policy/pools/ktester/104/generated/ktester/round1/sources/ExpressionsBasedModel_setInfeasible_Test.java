package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setInfeasible_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for every test – the default constructor creates a model with default options.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release resources; the model implements {@link Optimisation.Model#dispose()}.
        model.dispose();
    }

    @Test
    void testInitialStateIsNotInfeasible() {
        assertFalse(model.isInfeasible(), "A newly created model must not be marked as infeasible.");
    }

    @Test
    void testSetInfeasibleChangesFlag() {
        // package‑private, accessible because the test is in the same package
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "After calling setInfeasible() the model must report infeasible.");
    }

    @Test
    void testSetInfeasibleIsIdempotent() {
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "First call should set the flag.");
        // Call a second time – the flag must stay true and no exception should be thrown.
        model.setInfeasible();
        assertTrue(model.isInfeasible(), "Second call must leave the flag true.");
    }

    @Test
    void testInfeasibleModelReturnsInfeasibleResultOnMinimise() {
        // Arrange: a trivial feasible model (one variable, no constraints)
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // simple objective
        model.newExpression("obj").set(x, 1).weight(1);
        // Act: mark the model infeasible before solving
        model.setInfeasible();
        Optimisation.Result result = model.minimise();
        // Assert: the solver should immediately report infeasibility
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "Minimise on an infeasible model must return INFEASIBLE state.");
    }

    @Test
    void testInfeasibleModelReturnsInfeasibleResultOnMaximise() {
        // Arrange: a trivial feasible model (one variable, no constraints)
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // simple objective
        model.newExpression("obj").set(x, 1).weight(1);
        // Act: mark the model infeasible before solving
        model.setInfeasible();
        Optimisation.Result result = model.maximise();
        // Assert: the solver should immediately report infeasibility
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "Maximise on an infeasible model must return INFEASIBLE state.");
    }

    @Test
    void testSetInfeasibleDoesNotAlterVariableCount() {
        int before = model.countVariables();
        model.setInfeasible();
        int after = model.countVariables();
        assertEquals(before, after, "Marking a model infeasible must not change the number of variables.");
    }

    @Test
    void testOtherOperationsDoNotSetInfeasibility() {
        // Adding a variable should not change the infeasibility flag.
        model.newVariable("y");
        assertFalse(model.isInfeasible(), "Adding a variable must not mark the model as infeasible.");
        // Adding an expression should also leave the flag untouched.
        Variable x = model.newVariable("x");
        model.newExpression("c1").set(x, 1).lower(0);
        assertFalse(model.isInfeasible(), "Adding an expression must not mark the model as infeasible.");
    }

    @Test
    void testFlagPersistsAfterPresolve() {
        model.setInfeasible();
        // presolve is a no‑op for an infeasible model
        model.presolve();
        assertTrue(model.isInfeasible(), "Presolve must not clear the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterReduce() {
        model.setInfeasible();
        // reduce returns a new model
        ExpressionsBasedModel reduced = model.reduce();
        assertTrue(model.isInfeasible(), "Original model must keep its flag after reduce.");
        assertTrue(reduced.isInfeasible(), "Reduced model must inherit the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterSimplify() {
        model.setInfeasible();
        ExpressionsBasedModel simplified = model.simplify();
        assertTrue(model.isInfeasible(), "Original model must keep its flag after simplify.");
        assertTrue(simplified.isInfeasible(), "Simplified model must inherit the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterRelax() {
        model.setInfeasible();
        // relax modifies the model in‑place
        model.relax();
        assertTrue(model.isInfeasible(), "Relax must not clear the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterCopy() {
        model.setInfeasible();
        ExpressionsBasedModel copy = model.copy();
        assertTrue(model.isInfeasible(), "Original model must stay infeasible after copy.");
        assertTrue(copy.isInfeasible(), "Copy created by copy() must preserve the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterCopyRelaxVariant() {
        model.setInfeasible();
        ExpressionsBasedModel copyRelax = model.copy(true);
        assertTrue(model.isInfeasible(), "Original model must stay infeasible after copy(true).");
        assertTrue(copyRelax.isInfeasible(), "Copy created by copy(boolean relax) must preserve the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterCopyShallowPruneVariant() {
        model.setInfeasible();
        ExpressionsBasedModel copyShallow = model.copy(true, false);
        assertTrue(model.isInfeasible(), "Original model must stay infeasible after copy(true, false).");
        assertTrue(copyShallow.isInfeasible(), "Copy created by copy(boolean shallow, boolean prune) must preserve the infeasibility flag.");
    }

    @Test
    void testFlagPersistsAfterSnapshot() {
        model.setInfeasible();
        ExpressionsBasedModel snapshot = model.snapshot();
        assertTrue(model.isInfeasible(), "Original model must stay infeasible after snapshot().");
        assertTrue(snapshot.isInfeasible(), "Snapshot must preserve the infeasibility flag.");
    }

    @Test
    void testOtherStatusQueriesRemainFalseAfterInfeasibility() {
        model.setInfeasible();
        assertFalse(model.isUnbounded(), "setInfeasible() must not cause the model to be reported as unbounded.");
        assertFalse(model.isFixed(), "setInfeasible() must not cause the model to be reported as fixed.");
        assertFalse(model.isAnyVariableInteger(), "setInfeasible() must not affect the integer‑variable query.");
    }

    @Test
    void testAddingAfterInfeasibilityKeepsFlag() {
        model.setInfeasible();
        // Add a variable and an expression after the flag is set.
        Variable v = model.newVariable("z").lower(0).upper(5);
        model.newExpression("c2").set(v, 1).upper(3);
        assertTrue(model.isInfeasible(), "Adding variables or expressions after infeasibility must not clear the flag.");
    }
}
