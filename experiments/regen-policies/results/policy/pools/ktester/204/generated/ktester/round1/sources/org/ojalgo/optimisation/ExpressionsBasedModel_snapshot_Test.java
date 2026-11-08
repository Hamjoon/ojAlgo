package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.ojalgo.ProgrammingError;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_snapshot_Test {

    /**
     * The snapshot must be a different instance.
     */
    @Test
    void testSnapshotIsDifferentInstance() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = original.snapshot();
        assertNotSame(original, copy, "snapshot should return a new instance");
    }

    /**
     * The snapshot must be marked as a shallow copy.
     */
    @Test
    void testSnapshotIsShallowCopy() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = original.snapshot();
        assertTrue(copy.isShallowCopy(), "snapshot should be flagged as a shallow copy");
        assertFalse(original.isShallowCopy(), "original model must not be flagged as a shallow copy");
    }

    /**
     * The snapshot must be relaxed, while the original stays un‑relaxed.
     */
    @Test
    void testSnapshotIsRelaxed() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = original.snapshot();
        assertTrue(copy.isRelaxed(), "snapshot should be relaxed");
        assertFalse(original.isRelaxed(), "original model must not be relaxed");
    }

    /**
     * Modifying the snapshot must not affect the original model.
     */
    @Test
    void testSnapshotIndependence() {
        // Build a simple model with one variable and one expression
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Expression expr = original.newExpression("expr").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // 1 * x = 5  =>  x = 5
        expr.add(x, BigDecimal.ONE);
        // Take a snapshot
        ExpressionsBasedModel snapshot = original.snapshot();
        // Solve both models – they should give the same result because the snapshot is a copy
        Optimisation.Result originalResult = original.minimise();
        Optimisation.Result snapshotResult = snapshot.minimise();
        assertTrue(original.validate(originalResult));
        assertTrue(snapshot.validate(snapshotResult));
        // Both results must be optimal and equal
        assertTrue(originalResult.getState().isOptimal());
        assertTrue(snapshotResult.getState().isOptimal());
        assertEquals(originalResult.getValue(), snapshotResult.getValue());
        // Now change the snapshot (add a new variable) and verify the original model is unchanged
        snapshot.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertEquals(1, original.countVariables(), "original model must still contain only the original variable");
        assertEquals(2, snapshot.countVariables(), "snapshot model must contain the additional variable");
    }

    /**
     * The snapshot must preserve the optimisation sense and objective constant.
     */
    @Test
    void testSnapshotPreservesSenseAndConstant() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // Change sense to maximise and set an objective constant
        original.options.sense = Optimisation.Sense.MAX;
        original.addObjectiveConstant(BigDecimal.valueOf(3));
        ExpressionsBasedModel copy = original.snapshot();
        assertEquals(Optimisation.Sense.MAX, copy.getOptimisationSense(), "snapshot must keep the optimisation sense");
        assertEquals(BigDecimal.valueOf(3), copy.getObjectiveConstant(), "snapshot must keep the objective constant");
    }

    /**
     * Snapshot of an empty model – basic flags and counts.
     */
    @Test
    void testEmptyModelSnapshotBasicBehaviour() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // pre‑conditions
        assertFalse(original.isRelaxed(), "original should start non‑relaxed");
        assertEquals(0, original.countVariables());
        assertEquals(0, original.countExpressions());
        ExpressionsBasedModel copy = original.snapshot();
        // expectations
        assertNotSame(original, copy);
        assertTrue(copy.isShallowCopy());
        assertTrue(copy.isRelaxed());
        assertEquals(0, copy.countVariables());
        assertEquals(0, copy.countExpressions());
        // original must stay unchanged
        assertFalse(original.isRelaxed(), "original must remain non‑relaxed after snapshot");
    }

    /**
     * Snapshot after adding variables/expressions and then modifying the original.
     */
    @Test
    void testSnapshotIndependenceAfterOriginalModification() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // two variables
        original.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        original.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // one expression linking them
        Expression e = original.newExpression("e").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        e.add(original.getVariable(0), BigDecimal.ONE);
        e.add(original.getVariable(1), BigDecimal.ONE);
        ExpressionsBasedModel snapshot = original.snapshot();
        // snapshot should reflect the state at the moment of copying
        assertEquals(2, snapshot.countVariables());
        assertEquals(1, snapshot.countExpressions());
        // add a third variable to the original – snapshot must stay at 2 variables
        original.newVariable("c").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertEquals(3, original.countVariables(), "original now has three variables");
        assertEquals(2, snapshot.countVariables(), "snapshot must still have only the two variables captured at snapshot time");
        // original relaxed flag must stay false (snapshot does not affect it)
        assertFalse(original.isRelaxed(), "original must stay non‑relaxed");
    }

    /**
     * Verify that snapshot does not relax the original model.
     */
    @Test
    void testOriginalRelaxationUnchanged() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        assertFalse(original.isRelaxed(), "original initially not relaxed");
        ExpressionsBasedModel copy = original.snapshot();
        assertTrue(copy.isRelaxed(), "snapshot must be relaxed");
        assertFalse(original.isRelaxed(), "original must still be non‑relaxed after snapshot");
    }

    /**
     * Snapshot should internally perform a shallow copy (no pruning).
     */
    @Test
    void testSnapshotUsesShallowCopyNoPrune() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // add a variable and an expression so that pruning would remove something if it were enabled
        original.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        original.newExpression("expr").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        ExpressionsBasedModel copy = original.snapshot();
        // The public API does not expose the 'prune' flag directly.
        // We infer the behaviour by checking that the copy still contains the expression.
        assertTrue(copy.isShallowCopy(), "snapshot must be flagged as a shallow copy");
        assertEquals(1, copy.countExpressions(), "expression must still be present – indicates no pruning occurred");
    }

    /**
     * Snapshot must retain objective constant while original constant stays unchanged.
     */
    @Test
    void testSnapshotWithObjectiveConstant() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.addObjectiveConstant(BigDecimal.valueOf(42));
        ExpressionsBasedModel copy = original.snapshot();
        assertNotSame(original, copy);
        assertTrue(copy.isShallowCopy());
        assertTrue(copy.isRelaxed());
        assertEquals(BigDecimal.valueOf(42), copy.getObjectiveConstant(), "snapshot must copy the objective constant");
        assertEquals(BigDecimal.valueOf(42), original.getObjectiveConstant(), "original constant must remain unchanged");
    }

    /**
     * Snapshot must correctly report integer and fixed variables.
     */
    @Test
    void testSnapshotWithIntegerAndFixedVariables() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // integer variable
        Variable intVar = original.newVariable("intVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        intVar.integer(true);
        // fixed variable (lower == upper)
        original.newVariable("fixedVar").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        ExpressionsBasedModel copy = original.snapshot();
        assertTrue(copy.isAnyVariableInteger(), "snapshot must recognise the integer variable");
        assertTrue(copy.isAnyVariableFixed(), "snapshot must recognise the fixed variable");
        // original flags must stay the same
        assertTrue(original.isAnyVariableInteger(), "original must still report an integer variable");
        assertTrue(original.isAnyVariableFixed(), "original must still report a fixed variable");
    }

    /**
     * Snapshot when the original model is already relaxed.
     */
    @Test
    void testSnapshotWhenOriginalAlreadyRelaxed() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // put original into relaxed mode
        original.relax();
        assertTrue(original.isRelaxed(), "original should be relaxed before snapshot");
        ExpressionsBasedModel copy = original.snapshot();
        assertTrue(copy.isRelaxed(), "snapshot must also be relaxed");
        assertTrue(original.isRelaxed(), "original must stay relaxed after snapshot");
        assertNotSame(original, copy);
        assertTrue(copy.isShallowCopy());
    }

    /**
     * Shallow‑copy semantics: changing original variable bounds reflects in snapshot.
     */
    @Test
    void testShallowCopyVariableBoundsModification() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable v = original.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        ExpressionsBasedModel snapshot = original.snapshot();
        // modify bounds on the original variable
        v.lower(BigDecimal.valueOf(5));
        v.upper(BigDecimal.valueOf(15));
        // because the copy is shallow, the same Variable instance is shared
        Variable vInSnapshot = snapshot.getVariable(0);
        assertEquals(BigDecimal.valueOf(5), vInSnapshot.lower(), "snapshot must see the modified lower bound (shallow copy)");
        assertEquals(BigDecimal.valueOf(15), vInSnapshot.upper(), "snapshot must see the modified upper bound (shallow copy)");
    }

    /**
     * Snapshot must not carry over a known solution.
     */
    @Test
    void testSnapshotDoesNotCopyKnownSolution() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // a trivial model: minimise x with 0 <= x <= 10
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // ensure x contributes to the objective
        original.objective().weight(BigDecimal.ONE);
        // solve and store the result as a known solution
        Optimisation.Result result = original.minimise();
        original.setKnownSolution(result);
        assertNotNull(original.getKnownSolution(), "original must have a known solution");
        ExpressionsBasedModel copy = original.snapshot();
        assertNull(copy.getKnownSolution(), "snapshot must not copy the known solution from the original");
        // original must still retain its known solution
        assertNotNull(original.getKnownSolution(), "original must still retain its known solution after snapshot");
    }

    /**
     * Snapshot on a disposed model must throw ProgrammingError.
     */
    @Test
    void testSnapshotOnDisposedModelThrows() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.dispose();
        ProgrammingError thrown = assertThrows(ProgrammingError.class, original::snapshot, "Calling snapshot on a disposed model should throw ProgrammingError");
        assertNotNull(thrown.getMessage());
    }
}