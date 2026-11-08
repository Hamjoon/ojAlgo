package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_copy_Test {

    private ExpressionsBasedModel original;

    private ExpressionsBasedModel copy;

    @BeforeEach
    void setupBeforeEach() {
        // Build a tiny optimisation problem:
        // maximise  x + 2*y   subject to  x + y ≤ 4 , 0 ≤ x ≤ 3 , 0 ≤ y ≤ 3
        original = new ExpressionsBasedModel();
        Variable x = original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ONE);
        Variable y = original.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3)).weight(BigDecimal.valueOf(2));
        // Constraint: x + y ≤ 4
        original.newExpression("c1").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).upper(BigDecimal.valueOf(4));
        // Create the copy that will be used in most tests
        copy = original.copy();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly dispose models to free any native resources the library may hold.
        if (original != null) {
            original.dispose();
        }
        if (copy != null) {
            copy.dispose();
        }
    }

    @Test
    void testCopyIsIndependentModel() {
        // Solve the original model
        Optimisation.Result originalResult = original.maximise();
        assertTrue(original.validate(originalResult));
        assertTrue(originalResult.getState().isOptimal());
        // Solve the copy – it should give the same optimum values
        Optimisation.Result copyResult = copy.maximise();
        assertTrue(copy.validate(copyResult));
        assertTrue(copyResult.getState().isOptimal());
        // Compare variable values (they must be identical)
        for (int i = 0; i < originalResult.size(); i++) {
            assertEquals(originalResult.get(i), copyResult.get(i), "Variable " + i + " differs between original and copy");
        }
        // Now change a bound only on the copy and re‑solve it.
        // Tighten the upper bound of x in the copy to 1.
        // variables are kept in insertion order
        Variable copyX = copy.getVariable(0);
        copyX.upper(BigDecimal.ONE);
        Optimisation.Result tightenedCopyResult = copy.maximise();
        assertTrue(copy.validate(tightenedCopyResult));
        assertTrue(tightenedCopyResult.getState().isOptimal());
        // The original model must stay unchanged – its optimum should still be the same as before.
        Optimisation.Result originalResultAfterChange = original.maximise();
        assertTrue(original.validate(originalResultAfterChange));
        assertTrue(originalResultAfterChange.getState().isOptimal());
        // The tightened copy must have a lower objective value than the original.
        assertTrue(tightenedCopyResult.getValue().compareTo(originalResult.getValue()) < 0, "Tightened copy should have a lower objective value");
    }

    @Test
    void testCopyPreservesOptionsReference() {
        // The options field is public and final; the copy should reference the same instance.
        assertSame(original.options, copy.options, "Copy should reuse the same Optimisation.Options instance as the original");
    }

    @Test
    void testCopyIsNotShallowAndNotRelaxed() {
        // The public API exposes these predicates.
        assertFalse(copy.isShallowCopy(), "copy() must produce a deep copy");
        assertFalse(copy.isRelaxed(), "copy() must not mark the model as relaxed");
    }

    @Test
    void testCopyCreatesNewInstance() {
        // Verify that the copy is a distinct object (reference inequality)
        assertNotSame(original, copy, "copy() must return a new ExpressionsBasedModel instance");
    }

    @Test
    void testCopyPreservesStructure() {
        // The copy must contain the same number of variables and expressions.
        assertEquals(original.countVariables(), copy.countVariables(), "Variable count must be preserved after copy");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count must be preserved after copy");
    }

    @Test
    void testCopyDoesNotShareVariableInstances() {
        // Each variable in the copy must be a distinct object from the original.
        List<Variable> originalVars = original.getVariables();
        List<Variable> copyVars = copy.getVariables();
        assertEquals(originalVars.size(), copyVars.size(), "Both models should expose the same number of variables");
        for (int i = 0; i < originalVars.size(); i++) {
            assertNotSame(originalVars.get(i), copyVars.get(i), "Variable instance " + i + " must not be shared between original and copy");
        }
    }

    @Test
    void testCopyIsDeepForVariables_ModifyOriginalDoesNotAffectCopy() {
        // Change a bound in the original and verify the copy stays unchanged.
        Variable origX = original.getVariable(0);
        // original now has lower bound 5
        origX.lower(BigDecimal.valueOf(5));
        Variable copyX = copy.getVariable(0);
        assertEquals(BigDecimal.ZERO, copyX.lower(), "Copy's variable lower bound must remain unchanged after original is modified");
    }

    @Test
    void testCopyIsDeepForVariables_ModifyCopyDoesNotAffectOriginal() {
        // Change a bound in the copy and verify the original stays unchanged.
        Variable copyY = copy.getVariable(1);
        // tighten upper bound in copy
        copyY.upper(BigDecimal.ONE);
        Variable origY = original.getVariable(1);
        assertEquals(BigDecimal.valueOf(3), origY.upper(), "Original's variable upper bound must remain unchanged after copy is modified");
    }

    @Test
    void testCopyIsDeepForExpressions_ModifyOriginalDoesNotAffectCopy() {
        // Original expression: c1 : x + y ≤ 4
        Expression originalExpr = original.getExpression("c1");
        // Change coefficient of x from 1 to 5 in the original
        Variable x = original.getVariable("x");
        originalExpr.set(x, BigDecimal.valueOf(5));
        // The copy must still have coefficient 1 for x
        Expression copyExpr = copy.getExpression("c1");
        assertEquals(BigDecimal.ONE, copyExpr.get(x), "Copy's expression coefficient must remain unchanged after original is modified");
    }

    @Test
    void testCopyIsDeepForExpressions_ModifyCopyDoesNotAffectOriginal() {
        // Modify the copy's expression and ensure the original stays the same.
        Expression copyExpr = copy.getExpression("c1");
        Variable y = copy.getVariable("y");
        copyExpr.set(y, BigDecimal.valueOf(7));
        Expression originalExpr = original.getExpression("c1");
        assertEquals(BigDecimal.ONE, originalExpr.get(y), "Original's expression coefficient must remain unchanged after copy is modified");
    }

    @Test
    void testCopyPreservesObjectiveLimits() {
        // Set explicit objective limits on the original model.
        original.limitObjective(BigDecimal.valueOf(-10), BigDecimal.valueOf(20));
        ExpressionsBasedModel copyWithLimits = original.copy();
        Expression originalObj = original.objective();
        Expression copyObj = copyWithLimits.objective();
        assertEquals(originalObj.lowerLimit(), copyObj.lowerLimit(), "Objective lower limit must be preserved after copy");
        assertEquals(originalObj.upperLimit(), copyObj.upperLimit(), "Objective upper limit must be preserved after copy");
    }

    @Test
    void testCopyPreservesIntegrationSwitch() {
        original.setIntegrationSwitch(true);
        ExpressionsBasedModel copyWithSwitch = original.copy();
        assertTrue(copyWithSwitch.isIntegrationSwitch(), "Integration switch state must be copied");
    }

    @Test
    void testCopyEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel emptyCopy = empty.copy();
        assertEquals(0, empty.countVariables(), "Empty model must have zero variables");
        assertEquals(0, emptyCopy.countVariables(), "Copy of empty model must have zero variables");
        assertEquals(0, empty.countExpressions(), "Empty model must have zero expressions");
        assertEquals(0, emptyCopy.countExpressions(), "Copy of empty model must have zero expressions");
    }

    @Test
    void testCopyKnownSolutionPropagation() {
        // Solve the original model to obtain a result.
        Optimisation.Result result = original.maximise();
        assertTrue(result.getState().isOptimal());
        // Propagate the solution as a known solution.
        original.setKnownSolution(result);
        // Copy the model and verify that the known solution is present.
        ExpressionsBasedModel copyWithKnown = original.copy();
        Optimisation.Result known = copyWithKnown.getKnownSolution();
        assertNotNull(known, "Copy must contain a known solution when the original had one");
        assertEquals(result.getValue(), known.getValue(), "Known solution objective value must be identical in the copy");
        // Compare variable values one‑by‑one.
        for (int i = 0; i < result.size(); i++) {
            assertEquals(result.get(i), known.get(i), "Variable " + i + " value differs in known solution copy");
        }
    }

    @Test
    void testCopyAfterDisposeThrowsException() {
        // Dispose the original model and then attempt to copy it.
        original.dispose();
        // According to the API contract a ProgrammingError (runtime) should be thrown.
        assertThrows(ProgrammingError.class, () -> original.copy(), "Calling copy() on a disposed model must throw ProgrammingError");
    }
}
