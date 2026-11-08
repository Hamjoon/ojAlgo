package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_indexOfPositiveVariable_3_Test {

    private ExpressionsBasedModel model;

    //     @BeforeEach
    void setupBeforeEach() {
        // A fresh model for every test ensures independence.
        model = new ExpressionsBasedModel();
    }

    @Test
    void testIndexOfPositiveVariableWhenVariableIsPositive() {
        // A variable whose lower bound is 0 (non‑negative) is considered positive.
        Variable x = model.newVariable("x").lower(// explicit non‑negative lower bound
        BigDecimal.ZERO).upper(// any upper bound
        BigDecimal.TEN).weight(// weight is irrelevant for this test
        BigDecimal.ONE);
        int globalIndex = model.indexOf(x);
        int positiveIndex = model.indexOfPositiveVariable(x);
        // For a positive variable the two indices must be identical.
        assertEquals(globalIndex, positiveIndex, "Positive variable should have the same index in the positive‑variable list as its global index.");
    }

    @Test
    void testIndexOfPositiveVariableWhenVariableIsNotPositive() {
        // A variable with a negative lower bound is not considered positive.
        Variable y = model.newVariable("y").lower(// negative lower bound
        BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        int positiveIndex = model.indexOfPositiveVariable(y);
        // The method should indicate that the variable is not in the positive set.
        assertEquals(-1, positiveIndex, "Variable with a negative lower bound should not be regarded as positive and must return -1.");
    }

    @Test
    void testIndexOfPositiveVariableWhenVariableHasNoExplicitBounds() {
        // By default a variable has a lower bound of 0 (see Variable implementation),
        // therefore it is positive unless changed.
        // default variable, no explicit bounds
        Variable z = model.addVariable();
        int globalIndex = model.indexOf(z);
        int positiveIndex = model.indexOfPositiveVariable(z);
        assertEquals(globalIndex, positiveIndex, "A newly created variable without explicit bounds should be treated as positive.");
    }

//     @Test
//     void testIndexOfPositiveVariableWhenVariableIsNull() {
//         assertThrows(NullPointerException.class, () -> model.indexOfPositiveVariable(null), "Calling with null must throw NullPointerException.");
//     }

    @Test
    void testDelegationCorrectness() {
        Variable v = model.newVariable("v").lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        int direct = model.indexOfPositiveVariable(v);
        int twoStep = model.indexOfPositiveVariable(model.indexOf(v));
        assertEquals(twoStep, direct, "The one‑argument overload must delegate to the int overload without alteration.");
    }

    @Test
    void testPositiveVariableOrderingMultipleVariables() {
        Variable a = model.newVariable("a").lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        Variable b = model.newVariable("b").lower(BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(20));
        assertEquals(0, model.indexOfPositiveVariable(a), "First positive variable must have index 0.");
        assertEquals(1, model.indexOfPositiveVariable(b), "Second positive variable must have index 1.");
    }

    @Test
    void testPositiveVariableAfterNonPositive() {
        // Variable with lower bound = 0 (non‑positive according to the spec used here)
        Variable zeroBound = model.newVariable("zero").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Positive variable added afterwards
        Variable positive = model.newVariable("pos").lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        assertEquals(-1, model.indexOfPositiveVariable(zeroBound), "Variable with lower bound 0 must be reported as non‑positive.");
        assertEquals(0, model.indexOfPositiveVariable(positive), "First positive variable after a non‑positive one must have index 0.");
    }

    @Test
    void testPositiveIntegerVariable() {
        Variable intPos = model.newVariable("intPos").lower(BigDecimal.ONE).upper(BigDecimal.TEN).integer(// mark as integer
        true);
        assertEquals(0, model.indexOfPositiveVariable(intPos), "Integer variable with positive lower bound must be treated as positive.");
    }

    @Test
    void testFixedPositiveVariable() {
        Variable fixed = model.newVariable("fixed").lower(BigDecimal.valueOf(5)).upper(// fixed at 5 (>0)
        BigDecimal.valueOf(5));
        assertEquals(0, model.indexOfPositiveVariable(fixed), "Fixed variable with positive value must be in the positive list.");
    }

    @Test
    void testTinyPositiveLowerBound() {
        Variable tiny = model.newVariable("tiny").lower(new BigDecimal("1e-12")).upper(BigDecimal.ONE);
        assertEquals(0, model.indexOfPositiveVariable(tiny), "Variable with a tiny positive lower bound must be recognised as positive.");
    }

    @Test
    void testVariableNotPresentReturnsMinusOne() throws Exception {
        // Create a Variable instance via reflection that is never added to the model.
        Class<?> varClass = Class.forName("org.ojalgo.optimisation.Variable");
        Constructor<?> ctor = varClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        // raw Variable instance
        Object rawVar = ctor.newInstance();
        Variable unregistered = (Variable) rawVar;
        int result = model.indexOfPositiveVariable(unregistered);
        assertEquals(-1, result, "A variable never added to the model must yield -1.");
    }

    @Test
    void testDelegationWithInvalidGlobalIndex() throws Exception {
        // Re‑use the unregistered variable from the previous test.
        Class<?> varClass = Class.forName("org.ojalgo.optimisation.Variable");
        Constructor<?> ctor = varClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Variable unregistered = (Variable) ctor.newInstance();
        int direct = model.indexOfPositiveVariable(unregistered);
        int twoStep = model.indexOfPositiveVariable(model.indexOf(unregistered));
        assertEquals(-1, direct, "Direct call with an unknown variable must return -1.");
        assertEquals(-1, twoStep, "Two‑step delegation with an unknown variable must also return -1.");
    }

    @Test
    void testVariableFromDifferentModelThrowsException() {
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        Variable foreign = otherModel.newVariable("foreign").lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        // The variable was registered with 'otherModel', not with 'model'.
        assertThrows(IllegalArgumentException.class, () -> model.indexOfPositiveVariable(foreign), "Passing a Variable from a different model must throw IllegalArgumentException.");
    }
}