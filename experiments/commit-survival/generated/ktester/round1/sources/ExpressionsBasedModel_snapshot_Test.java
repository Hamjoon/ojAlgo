package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.ProgrammingError;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_snapshot_Test {

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// 1. model with two variables, one expression, non‑default optimisation sense
        Arguments.of("basicFunctionality1", (Runnable) () -> {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v1 = model.newVariable("v1").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
            Variable v2 = model.newVariable("v2").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
            model.newExpression("expr").set(v1, BigDecimal.ONE).set(v2, BigDecimal.ONE).weight(BigDecimal.ONE);
            model.setOptimisationSense(Optimisation.Sense.MAXIMISE);
            // store for assertions
            // dummy to avoid optimisation side effects
            model.setKnownSolution(null);
            Assertions.assertAll(() -> {
                ExpressionsBasedModel snapshot = model.snapshot();
                // basic contract
                assertNotSame(model, snapshot, "snapshot must be a different instance");
                assertTrue(snapshot.isRelaxed(), "snapshot must be relaxed");
                assertFalse(model.isRelaxed(), "original must stay un‑relaxed");
                assertTrue(snapshot.isShallowCopy(), "snapshot should be a shallow copy");
                // counts
                assertEquals(model.countVariables(), snapshot.countVariables(), "variable count must be identical");
                assertEquals(model.countExpressions(), snapshot.countExpressions(), "expression count must be identical");
                // optimisation sense is copied
                assertEquals(model.getOptimisationSense(), snapshot.getOptimisationSense(), "optimisation sense must be preserved");
            });
        }), // 2. empty model
        Arguments.of("basicFunctionality2", (Runnable) () -> {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            ExpressionsBasedModel snapshot = model.snapshot();
            assertNotSame(model, snapshot, "snapshot must be a different instance");
            assertTrue(snapshot.isRelaxed(), "snapshot must be relaxed");
            assertTrue(snapshot.isShallowCopy(), "snapshot must be a shallow copy");
            assertEquals(0, snapshot.countVariables(), "empty model must have 0 variables");
            assertEquals(0, snapshot.countExpressions(), "empty model must have 0 expressions");
            assertFalse(model.isRelaxed(), "original empty model must stay un‑relaxed");
        }), // 3. original already relaxed
        Arguments.of("basicFunctionality3", (Runnable) () -> {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
            // original is now relaxed
            model.relax(true);
            assertTrue(model.isRelaxed(), "pre‑condition: original must be relaxed");
            ExpressionsBasedModel snapshot = model.snapshot();
            assertNotSame(model, snapshot, "snapshot must be a distinct instance");
            assertTrue(snapshot.isRelaxed(), "snapshot must be relaxed");
            assertTrue(model.isRelaxed(), "original must remain relaxed");
            assertTrue(snapshot.isShallowCopy(), "snapshot should be a shallow copy");
        }), // 4. integration switch enabled
        Arguments.of("basicFunctionality4", (Runnable) () -> {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.setIntegrationSwitch(true);
            assertTrue(model.isIntegrationSwitch(), "pre‑condition: integration switch must be true");
            ExpressionsBasedModel snapshot = model.snapshot();
            assertNotSame(model, snapshot, "snapshot must be a distinct instance");
            assertTrue(snapshot.isIntegrationSwitch(), "snapshot must preserve the integration switch state");
        }));
    }

    /**
     * Verify that {@code snapshot()} returns a new, relaxed, shallow copy of the model.
     */
    @Test
    void testSnapshotCreatesRelaxedShallowCopy() {
        // -----------------------------------------------------------------
        // GIVEN – a simple model with one variable and one expression
        // -----------------------------------------------------------------
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // expression: x = 5
        original.newExpression("expr").set(x, BigDecimal.ONE).level(BigDecimal.valueOf(5));
        // -----------------------------------------------------------------
        // WHEN – we take a snapshot
        // -----------------------------------------------------------------
        ExpressionsBasedModel snapshot = original.snapshot();
        // -----------------------------------------------------------------
        // THEN – the snapshot must satisfy the contract
        // -----------------------------------------------------------------
        assertNotSame(original, snapshot, "snapshot must be a different instance");
        assertTrue(snapshot.isRelaxed(), "snapshot must be marked as relaxed");
        assertFalse(original.isRelaxed(), "original must stay un‑relaxed");
        assertTrue(snapshot.isShallowCopy(), "snapshot should be a shallow copy");
        // The variable objects are cloned – they must not be the same reference
        Variable clonedX = snapshot.getVariable(0);
        assertNotSame(x, clonedX, "variables must be cloned in the snapshot");
        assertEquals(x.getName(), clonedX.getName(), "cloned variable should keep the same name");
        // The number of expressions must be identical, but the expression objects are distinct
        assertEquals(original.countExpressions(), snapshot.countExpressions(), "snapshot must contain the same number of expressions");
        Expression originalExpr = original.getExpression("expr");
        Expression snapshotExpr = snapshot.getExpression("expr");
        assertNotSame(originalExpr, snapshotExpr, "expressions must be distinct objects in the snapshot");
        assertEquals(originalExpr.getContributionWeight(), snapshotExpr.getContributionWeight(), "expression contribution weight must be copied");
    }

    /**
     * Verify that after taking a snapshot, further changes to the original model
     * do not affect the snapshot.
     */
    @Test
    void testSnapshotIndependenceAfterModification() {
        // -----------------------------------------------------------------
        // GIVEN – a model with a single variable
        // -----------------------------------------------------------------
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable a = original.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // Take a snapshot
        ExpressionsBasedModel snapshot = original.snapshot();
        // -----------------------------------------------------------------
        // WHEN – we modify the original model (add another variable)
        // -----------------------------------------------------------------
        Variable b = original.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // -----------------------------------------------------------------
        // THEN – the snapshot must remain unchanged
        // -----------------------------------------------------------------
        assertEquals(1, snapshot.countVariables(), "snapshot should still contain only the original variable");
        assertEquals(2, original.countVariables(), "original model should now contain the newly added variable");
        // The newly added variable must not be present in the snapshot
        assertNull(snapshot.getVariable("b"), "snapshot must not contain a variable that was added after the snapshot");
        // The original variable 'a' should still be present in both models
        assertNotNull(snapshot.getVariable("a"));
        assertNotNull(original.getVariable("a"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicFunctionalityProvider")
    void testSnapshotBasicFunctionality(String caseId, Runnable scenario) {
        // Each scenario builds its own model and performs the assertions.
        scenario.run();
    }

    @Test
    void testSnapshotShallowCopyVariableIndependence() {
        // GIVEN – a model with one variable and one expression
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        original.newExpression("e").set(x, BigDecimal.ONE).weight(BigDecimal.ONE);
        // WHEN – a snapshot is taken and a new variable is added only to the snapshot
        ExpressionsBasedModel snapshot = original.snapshot();
        snapshot.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // THEN – the original model is unaffected
        assertEquals(1, original.countVariables(), "original variable count must stay unchanged");
        assertEquals(2, snapshot.countVariables(), "snapshot variable count must reflect the added variable");
        assertNull(original.getVariable("y"), "original must not contain the variable added to the snapshot");
        assertNotNull(snapshot.getVariable("y"), "snapshot must contain the newly added variable");
        assertTrue(snapshot.isShallowCopy(), "snapshot should still report being a shallow copy");
        assertFalse(original.isShallowCopy(), "original must not be a shallow copy");
    }

    @Test
    void testSnapshotShallowCopyExpressionSharing() {
        // GIVEN – a model with one variable and a named expression
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        Variable v = original.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        original.newExpression("e1").set(v, BigDecimal.ONE).weight(BigDecimal.ONE);
        // WHEN – we snapshot the model
        ExpressionsBasedModel snapshot = original.snapshot();
        // THEN – the expression objects are the same (shallow copy)
        Expression originalExpr = original.getExpression("e1");
        Expression snapshotExpr = snapshot.getExpression("e1");
        assertSame(originalExpr, snapshotExpr, "expressions must be shared between original and snapshot (shallow copy)");
        // MODIFY – change the contribution weight via the snapshot
        snapshotExpr.weight(BigDecimal.valueOf(5));
        // VERIFY – the change is visible in the original expression as well
        assertEquals(BigDecimal.valueOf(5), originalExpr.getContributionWeight(), "modifying the snapshot's expression must affect the original (shallow copy)");
    }

    @Test
    void testSnapshotThrowsWhenModelDisposed() {
        // GIVEN – a disposed model
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.dispose();
        // WHEN / THEN – snapshot must throw ProgrammingError
        assertThrows(ProgrammingError.class, model::snapshot, "snapshot() on a disposed model should throw ProgrammingError");
    }

    @Test
    void testSnapshotThrowsWhenInternalStateCorrupted() throws Exception {
        // GIVEN – a model whose internal list of variables is forcibly set to null
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Find the first private field of type List and null it via reflection.
        // This is a best‑effort approach; if the internal implementation changes,
        // the test will still fail with a NullPointerException as expected.
        Field targetField = null;
        for (Field f : ExpressionsBasedModel.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                targetField = f;
                break;
            }
        }
        assertNotNull(targetField, "Unable to locate a List field to corrupt via reflection");
        targetField.setAccessible(true);
        // corrupt internal state
        targetField.set(model, null);
        // WHEN / THEN – snapshot should now throw NPE due to the corrupted state
        assertThrows(NullPointerException.class, model::snapshot, "snapshot() should throw NullPointerException when internal state is null");
    }
}
