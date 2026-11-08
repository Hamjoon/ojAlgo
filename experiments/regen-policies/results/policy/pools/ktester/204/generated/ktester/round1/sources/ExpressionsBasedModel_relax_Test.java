package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_relax_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test
        model = new ExpressionsBasedModel();
        // Two integer variables, each contributing weight 1 to the objective
        x = model.newVariable("x").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        y = model.newVariable("y").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Linear constraint: x + y ≤ 1.5
        model.newExpression("c").upper(BigDecimal.valueOf(1.5)).set(x, BigDecimal.ONE).set(y, BigDecimal.ONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        model = null;
        x = null;
        y = null;
    }

    @Test
    void testRelaxMakesModelRelaxedAndAllowsFractionalSolution() {
        // ----- 1. Solve the original integer model -----
        Optimisation.Result intResult = model.maximise();
        // Integer optimum for the given data is 1 (either x=1,y=0 or x=0,y=1)
        double intValue = intResult.getValue().doubleValue();
        assertEquals(1.0, intValue, 1e-12, "Integer optimum should be 1");
        // ----- 2. Relax the model -----
        // calls relax(false) internally
        model.relax();
        // The model should now report that it is relaxed
        assertTrue(model.isRelaxed(), "Model should be marked as relaxed after relax()");
        // After relaxation no variable should be considered integer
        assertFalse(model.isAnyVariableInteger(), "After relaxation the model must not contain any integer variables");
        // ----- 3. Solve the relaxed (continuous) model -----
        Optimisation.Result relaxedResult = model.maximise();
        // Continuous optimum for the same data is 1.5 (x=1.5, y=0 or any convex combination)
        double relaxedValue = relaxedResult.getValue().doubleValue();
        assertEquals(1.5, relaxedValue, 1e-12, "Relaxed optimum should be 1.5");
    }

    @Test
    void testIsRelaxedFlagBeforeAndAfterCallingRelax() {
        // Initially the model is not relaxed
        assertFalse(model.isRelaxed(), "New model must not be relaxed");
        // Call relax()
        model.relax();
        // Verify the flag is now true
        assertTrue(model.isRelaxed(), "Model should be relaxed after invoking relax()");
    }

    @Test
    void testRelaxDelegatesToHardRelax() {
        // pre‑condition: model contains integer variables
        assertTrue(model.isAnyVariableInteger());
        // should delegate to relax(false)
        model.relax();
        assertTrue(model.isRelaxed(), "Model must be marked relaxed");
        assertFalse(model.isAnyVariableInteger(), "All integer variables must be turned continuous after hard relax");
    }

    @Test
    void testRelaxIdempotenceWhenAlreadyRelaxed() {
        // first call – hard relax
        model.relax();
        assertTrue(model.isRelaxed());
        // second call must not change state nor throw
        assertDoesNotThrow(() -> model.relax(), "Calling relax() a second time should be safe");
        assertTrue(model.isRelaxed(), "Relaxed flag must stay true");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false })
    void testHardRelaxConvertsIntegers(boolean soft) {
        // model contains integer vars
        assertTrue(model.isAnyVariableInteger());
        model.relax(soft);
        assertTrue(model.isRelaxed(), "Model must be marked relaxed");
        assertFalse(model.isAnyVariableInteger(), "Hard relax must clear the integer flag of all variables");
    }

    @Test
    void testHardRelaxOnModelWithoutIntegers() {
        // make all variables continuous first
        model.relax(false);
        assertFalse(model.isAnyVariableInteger());
        // call relax again – should stay relaxed and not fail
        assertDoesNotThrow(() -> model.relax(false));
        assertTrue(model.isRelaxed());
        assertFalse(model.isAnyVariableInteger());
    }

    @Test
    void testHardRelaxPreservesBounds() {
        // custom bounds
        Variable a = model.newVariable("a").lower(BigDecimal.valueOf(-3.7)).upper(BigDecimal.valueOf(12.5)).integer(true);
        Variable b = model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).integer(true);
        model.relax(false);
        assertEquals(BigDecimal.valueOf(-3.7), a.lower(), "Lower bound must be preserved");
        assertEquals(BigDecimal.valueOf(12.5), a.upper(), "Upper bound must be preserved");
        assertEquals(BigDecimal.ZERO, b.lower(), "Lower bound must be preserved");
        assertEquals(BigDecimal.valueOf(5), b.upper(), "Upper bound must be preserved");
    }

    @Test
    void testHardRelaxKeepsObjectiveConstant() {
        BigDecimal constant = BigDecimal.valueOf(42);
        model.addObjectiveConstant(constant);
        assertEquals(constant, model.getObjectiveConstant());
        model.relax(false);
        assertEquals(constant, model.getObjectiveConstant(), "Objective constant must not be altered by hard relaxation");
    }

    @Test
    void testHardRelaxOnFixedIntegerVariable() {
        Variable fixed = model.newVariable("fixed").integer(true).lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        assertTrue(fixed.isFixed(), "Variable should be fixed before relaxation");
        model.relax(false);
        assertTrue(fixed.isFixed(), "Fixed status must survive relaxation");
        assertFalse(model.isAnyVariableInteger(), "Even fixed integer variables must lose the integer flag after hard relax");
    }

    @Test
    void testHardRelaxDoesNotTouchQuadraticFlag() {
        // create a quadratic expression (x*x)
        Expression quad = model.newExpression("quad").set(x, BigDecimal.ONE).quadratic(x, BigDecimal.ONE);
        assertTrue(model.isAnyExpressionQuadratic(), "Model must recognise a quadratic expression before relaxation");
        model.relax(false);
        assertTrue(model.isAnyExpressionQuadratic(), "Quadratic flag must stay true after hard relaxation");
    }

    @Test
    void testHardRelaxPreservesSpecialOrderedSet() {
        // SOS1 with integer variables x and y
        model.addSpecialOrderedSet(List.of(x, y), 1, 1);
        model.relax(false);
        // SOS is stored internally – we only verify that variables are still linked
        Collection<Variable> sosVars = model.getVariables().stream().filter(v -> v.getName().equals("x") || v.getName().equals("y")).toList();
        assertEquals(2, sosVars.size(), "SOS variables must still be present after hard relaxation");
        assertFalse(model.isAnyVariableInteger(), "Variables inside SOS must become continuous after hard relax");
    }

    @Test
    void testHardRelaxIdempotenceWhenAlreadyHardRelaxed() {
        model.relax(false);
        assertTrue(model.isRelaxed());
        // second hard relax – should be a no‑op
        assertDoesNotThrow(() -> model.relax(false));
        assertTrue(model.isRelaxed());
        assertFalse(model.isAnyVariableInteger());
    }

    @Test
    void testSoftRelaxKeepsIntegersAndSetsSwitch() {
        model.relax(true);
        assertTrue(model.isRelaxed(), "Model must be marked relaxed after soft relax");
        assertTrue(model.isAnyVariableInteger(), "Integer variables must stay integer after soft relax");
        assertTrue(model.isIntegrationSwitch(IntegrationProperty.SOFT_RELAX), "Soft‑relax integration flag must be true");
    }

    @Test
    void testSoftRelaxIdempotence() {
        model.relax(true);
        assertTrue(model.isIntegrationSwitch(IntegrationProperty.SOFT_RELAX));
        // second call – flag stays true, no exception
        assertDoesNotThrow(() -> model.relax(true));
        assertTrue(model.isIntegrationSwitch(IntegrationProperty.SOFT_RELAX));
    }

    @Test
    void testSoftRelaxAfterHardRelaxEnablesFlagOnly() {
        // hard relax first
        model.relax(false);
        assertTrue(model.isRelaxed());
        assertFalse(model.isAnyVariableInteger());
        // now enable soft flag
        model.relax(true);
        assertTrue(model.isRelaxed());
        assertTrue(model.isIntegrationSwitch(IntegrationProperty.SOFT_RELAX));
        // integer flag must remain cleared
        assertFalse(model.isAnyVariableInteger(), "Soft relax after hard relax must not resurrect integer status");
    }

    @Test
    void testRelaxOnEmptyModelDoesNotThrow() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        assertDoesNotThrow(() -> empty.relax(false));
        assertTrue(empty.isRelaxed(), "Empty model must still be marked relaxed");
    }

    @Test
    void testRelaxOnDisposedModelThrows() {
        model.dispose();
        // ProgrammingError extends RuntimeException
        assertThrows(RuntimeException.class, () -> model.relax(), "Calling relax() on a disposed model must throw");
    }

    @Test
    void testRelaxOnNativeModelThrowsUnsupportedOperation() {
        // Mock static method isNative to return true for our instance
        try (MockedStatic<ExpressionsBasedModel> mocked = mockStatic(ExpressionsBasedModel.class)) {
            mocked.when(() -> ExpressionsBasedModel.isNative(model)).thenReturn(true);
            assertThrows(UnsupportedOperationException.class, () -> model.relax(), "Relaxing a native model must raise UnsupportedOperationException");
        }
    }

    @Test
    void testRelaxWithNonFiniteBoundsThrows() {
        Variable bad = model.newVariable("bad").lower(new BigDecimal(Double.NaN)).upper(BigDecimal.ONE).integer(true);
        assertThrows(IllegalArgumentException.class, () -> model.relax(false), "Relaxation must reject variables with non‑finite bounds");
    }

    @Test
    void testSoftRelaxWithNonFiniteBoundsThrows() {
        Variable bad = model.newVariable("bad").lower(BigDecimal.ZERO).upper(new BigDecimal(Double.POSITIVE_INFINITY)).integer(true);
        assertThrows(IllegalArgumentException.class, () -> model.relax(true), "Soft relaxation must also reject non‑finite bounds");
    }

    @Test
    void testRelaxWhenIntegrationSwitchDisallowsRelaxation() {
        // Assume IntegrationProperty.NO_RELAX exists
        model.setIntegrationSwitch(IntegrationProperty.NO_RELAX, true);
        // ProgrammingError
        assertThrows(RuntimeException.class, () -> model.relax(false), "Relaxation should be blocked when NO_RELAX switch is set");
    }
}
