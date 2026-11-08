package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_snapshot_Test {

    private ExpressionsBasedModel original;

    @BeforeEach
    void setupBeforeEach() {
        // Build a deterministic model that can be snapshot in the tests.
        original = new ExpressionsBasedModel();
        // Three variables with various characteristics.
        Variable v0 = original.newVariable("v0").lower(0).upper(10).weight(1);
        Variable v1 = original.newVariable("v1").lower(1).upper(5).weight(2);
        Variable v2 = original.newVariable("v2").level(3).weight(3);
        // A simple equality constraint: v0 + 2·v1 + v2 = 10
        Expression expr = original.newExpression("c0").lower(BigDecimal.TEN).upper(BigDecimal.TEN);
        expr.set(v0, BigDecimal.ONE);
        expr.set(v1, new BigDecimal("2"));
        expr.set(v2, BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose the model to free any native resources the library may hold
        if (original != null) {
            original.dispose();
        }
    }

    @Test
    void testSnapshotCreatesRelaxedCopy() {
        // When
        ExpressionsBasedModel snapshot = original.snapshot();
        // Then
        assertNotSame(original, snapshot, "snapshot must be a different instance");
        assertEquals(original.countVariables(), snapshot.countVariables(), "snapshot must contain the same number of variables");
        assertEquals(original.countExpressions(), snapshot.countExpressions(), "snapshot must contain the same number of expressions");
        // The original model must stay non‑relaxed, the snapshot must be relaxed
        assertFalse(original.isRelaxed(), "original model should not be relaxed");
        assertTrue(snapshot.isRelaxed(), "snapshot model should be relaxed");
    }

    @Test
    void testSnapshotIndependenceOfVariables() {
        // Given a snapshot
        ExpressionsBasedModel snapshot = original.snapshot();
        // Grab a variable from the original model
        Variable originalVar0 = original.getVariable(0);
        // Corresponding variable in the snapshot (same position)
        Variable snapshotVar0 = snapshot.getVariable(0);
        // Verify they are different objects (deep copy of variables)
        assertNotSame(originalVar0, snapshotVar0, "Variables must be copied; they should not be the same instance");
        // Change a bound on the original variable
        originalVar0.lower(BigDecimal.valueOf(5));
        originalVar0.upper(BigDecimal.valueOf(15));
        // The snapshot's variable must retain its original bounds (0 and 10)
        assertEquals(BigDecimal.ZERO, snapshotVar0.lower(), "Snapshot variable lower bound must remain unchanged");
        assertEquals(BigDecimal.TEN, snapshotVar0.upper(), "Snapshot variable upper bound must remain unchanged");
    }

    @Test
    void testSnapshotIndependenceOfExpressions() {
        // Create a snapshot
        ExpressionsBasedModel snapshot = original.snapshot();
        // Retrieve the sole expression from both models
        List<Expression> originalExprs = original.expressions().toList();
        List<Expression> snapshotExprs = snapshot.expressions().toList();
        assertEquals(1, originalExprs.size());
        assertEquals(1, snapshotExprs.size());
        Expression originalExpr = originalExprs.get(0);
        Expression snapshotExpr = snapshotExprs.get(0);
        // They must be different instances
        assertNotSame(originalExpr, snapshotExpr, "Expressions must be copied; they should not be the same instance");
        // Modify the coefficient of a variable in the original expression
        Variable v0 = original.getVariable(0);
        originalExpr.set(v0, BigDecimal.valueOf(99));
        // The snapshot expression must keep its original coefficient (1)
        assertEquals(BigDecimal.ONE, snapshotExpr.get(v0, false), "Snapshot expression must retain its original coefficient");
    }

    @Test
    @DisplayName("Basic snapshot functionality – shallow copy of variables")
    void testBasicSnapshotShallowCopyVariables() {
        // Build a minimal model (one variable, one expression)
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(0).upper(5).weight(1);
        Expression e = model.newExpression("e").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        e.set(x, BigDecimal.valueOf(2));
        // Snapshot
        ExpressionsBasedModel snap = model.snapshot();
        // Verify shallow‑copy flag and relaxation state
        assertTrue(snap.isShallowCopy(), "snapshot should be a shallow copy");
        assertTrue(snap.isRelaxed(), "snapshot must be relaxed");
        assertFalse(model.isRelaxed(), "original must stay non‑relaxed");
        // Variables are shared (shallow copy) – modify in snapshot and see change in original
        Variable snapX = snap.getVariable(0);
        snapX.lower(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.valueOf(3), x.lower(), "Changing a variable in the snapshot must affect the original (shallow copy)");
    }

    @Test
    @DisplayName("Snapshot when original model is already relaxed")
    void testSnapshotWhenOriginalAlreadyRelaxed() {
        // Relax the original model first
        original.relax(true);
        assertTrue(original.isRelaxed(), "original should be relaxed before snapshot");
        // Take a snapshot
        ExpressionsBasedModel snap = original.snapshot();
        // Both must report relaxed, and the snapshot must still be a shallow copy
        assertTrue(original.isRelaxed(), "original remains relaxed after snapshot");
        assertTrue(snap.isRelaxed(), "snapshot is also relaxed");
        assertTrue(snap.isShallowCopy(), "snapshot must be a shallow copy");
    }

    @Test
    @DisplayName("Snapshot on an empty model")
    void testSnapshotOnEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel snap = empty.snapshot();
        assertNotSame(empty, snap, "snapshot must be a new instance");
        assertEquals(0, snap.countVariables(), "empty snapshot must have zero variables");
        assertEquals(0, snap.countExpressions(), "empty snapshot must have zero expressions");
        assertTrue(snap.isShallowCopy(), "snapshot of empty model must be shallow");
        assertTrue(snap.isRelaxed(), "snapshot must be relaxed");
    }

    @Test
    @DisplayName("Snapshot does not affect original model state")
    void testSnapshotDoesNotMutateOriginalState() {
        // Original is not relaxed initially
        assertFalse(original.isRelaxed(), "pre‑condition: original not relaxed");
        // Snapshot
        ExpressionsBasedModel snap = original.snapshot();
        // Original must stay unchanged
        assertFalse(original.isRelaxed(), "original must stay non‑relaxed");
        // Snapshot must be relaxed
        assertTrue(snap.isRelaxed(), "snapshot must be relaxed");
    }

    @Test
    @DisplayName("Snapshot preserves expression references (shared expressions)")
    void testSnapshotPreservesExpressionReferences() {
        // Model with a single variable and expression
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x");
        Expression expr = model.newExpression("e").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        expr.set(x, BigDecimal.valueOf(2));
        // Snapshot
        ExpressionsBasedModel snap = model.snapshot();
        // Retrieve the expression from both models (they should be the same instance)
        Expression originalExpr = model.expressions().findFirst().orElseThrow();
        Expression snapshotExpr = snap.expressions().findFirst().orElseThrow();
        assertSame(originalExpr, snapshotExpr, "Expressions must be shared between original and snapshot (shallow copy)");
        // Change a coefficient via the snapshot and verify the original sees the change
        snapshotExpr.set(x, BigDecimal.valueOf(5));
        assertEquals(BigDecimal.valueOf(5), originalExpr.get(x, false), "Modifying expression via snapshot must affect original");
    }

    @Test
    @DisplayName("Snapshot retains variable and expression counts")
    void testSnapshotRetainsCounts() {
        // Add a second expression to the model built in @BeforeEach
        original.newExpression("c1").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20)).set(original.getVariable(0), BigDecimal.ONE);
        ExpressionsBasedModel snap = original.snapshot();
        assertEquals(original.countVariables(), snap.countVariables(), "Variable count must be identical after snapshot");
        assertEquals(original.countExpressions(), snap.countExpressions(), "Expression count must be identical after snapshot");
    }

    @Test
    @DisplayName("Snapshot retains integer variable flag")
    void testSnapshotRetainsIntegerVariableFlag() {
        // Add an integer variable
        original.newVariable("intVar").integer(true).lower(0).upper(10);
        assertTrue(original.isAnyVariableInteger(), "original model must report having an integer variable");
        ExpressionsBasedModel snap = original.snapshot();
        assertTrue(snap.isAnyVariableInteger(), "snapshot must also report having an integer variable");
    }

    @Test
    @DisplayName("Snapshot retains fixed variable status")
    void testSnapshotRetainsFixedVariableStatus() {
        // Fix variable v0 by setting equal lower/upper bounds
        Variable v0 = original.getVariable(0);
        v0.lower(5).upper(5);
        assertTrue(original.getFixedVariables().contains(original.toIntIndex(0)), "original must recognise the variable as fixed");
        ExpressionsBasedModel snap = original.snapshot();
        assertEquals(original.getFixedVariables(), snap.getFixedVariables(), "snapshot must contain the same set of fixed variable indices");
    }

    @Test
    @DisplayName("Snapshot options independence")
    void testSnapshotOptionsIndependence() {
        // Configure an option on the original model
        original.options.maxIterations = 42;
        ExpressionsBasedModel snap = original.snapshot();
        // Change the same option on the snapshot
        snap.options.maxIterations = 99;
        // Verify original's option remains unchanged
        assertEquals(42, original.options.maxIterations, "Modifying options on the snapshot must NOT affect the original");
        assertEquals(99, snap.options.maxIterations, "Snapshot must reflect its own option change");
    }

    @Test
    @DisplayName("Snapshot after model disposal throws ProgrammingError")
    void testSnapshotAfterDisposalThrows() {
        original.dispose();
        // The library throws ProgrammingError (a subclass of RuntimeException) for illegal use.
        assertThrows(ProgrammingError.class, original::snapshot, "Calling snapshot() on a disposed model must throw ProgrammingError");
    }

    @Test
    @DisplayName("Snapshot with corrupted internal state throws exception")
    void testSnapshotWithCorruptedInternalStateThrows() throws Exception {
        // Attempt to corrupt the first List‑typed field we can find (usually the variable list)
        Optional<Field> listField = Optional.empty();
        for (Field f : ExpressionsBasedModel.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                listField = Optional.of(f);
                break;
            }
        }
        assertTrue(listField.isPresent(), "Unable to locate a List field to corrupt");
        Field fieldToCorrupt = listField.get();
        fieldToCorrupt.setAccessible(true);
        // corrupt internal state
        fieldToCorrupt.set(original, null);
        // Expect a NullPointerException or ProgrammingError when snapshot() attempts to copy
        assertThrows(RuntimeException.class, original::snapshot, "Snapshot on a model with corrupted internal state must fail");
    }
}
