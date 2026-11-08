package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getObjectiveConstant_Test {

    /**
     * Instance used in most tests.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for every test to avoid cross‑test interference.
        model = new ExpressionsBasedModel();
        model = new ExpressionsBasedModel();
    }

    /**
     * Supplies positive and negative single‑addition test data.
     */
    private static Stream<Arguments> singleAdditionProvider() {
        return Stream.of(Arguments.of(valueOf("5.75"), valueOf("5.75")), Arguments.of(valueOf("-3.20"), valueOf("-3.20")));
    }

    /**
     * The newly created model must start with a zero objective constant.
     */
    @Test
    void testDefaultObjectiveConstantIsZero() {
        assertEquals(ZERO, model.getObjectiveConstant(), "A fresh ExpressionsBasedModel should have an objective constant of ZERO.");
    }

    /**
     * Adding a constant should be reflected by {@code getObjectiveConstant()}.
     */
    @Test
    void testAddObjectiveConstantUpdatesValue() {
        // 5 as BigDecimal
        BigDecimal addition = valueOf(5);
        // package‑private method, accessible because of same package
        model.addObjectiveConstant(addition);
        assertEquals(addition, model.getObjectiveConstant(), "After adding 5 the objective constant should be 5.");
        // Adding another value should accumulate.
        BigDecimal secondAddition = valueOf(3);
        model.addObjectiveConstant(secondAddition);
        assertEquals(addition.add(secondAddition), model.getObjectiveConstant(), "Objective constant should accumulate successive additions.");
    }

    /**
     * The copy‑constructor must copy the objective constant from the source model.
     */
    @Test
    void testCopyConstructorCopiesObjectiveConstant() {
        // Prepare a source model with a non‑zero constant.
        model.addObjectiveConstant(valueOf(7));
        // Use the package‑private copy‑constructor (shallow = false, prune = false).
        ExpressionsBasedModel copy = new ExpressionsBasedModel(model, false, false);
        assertEquals(model.getObjectiveConstant(), copy.getObjectiveConstant(), "The copied model must contain the same objective constant as the original.");
    }

    /**
     * Adding variables or expressions must not affect the constant unless explicitly changed.
     */
    @Test
    void testVariableManipulationDoesNotChangeConstant() {
        // Record the initial constant (should be ZERO).
        BigDecimal initialConstant = model.getObjectiveConstant();
        // Add a variable and set a weight – this should not touch the constant.
        Variable v = model.newVariable("x").weight(valueOf(2));
        // contributes to objective but does not modify constant
        model.objective().set(v, valueOf(2));
        assertEquals(initialConstant, model.getObjectiveConstant(), "Manipulating variables or the objective expression must not alter the constant.");
    }

    /**
     * Parameterised test for single constant additions.
     *
     * @param addition the value passed to {@code addObjectiveConstant}
     * @param expected the expected result of {@code getObjectiveConstant}
     */
    @ParameterizedTest(name = "[{index}] add {0} → expect {1}")
    @MethodSource("singleAdditionProvider")
    void testSingleAdditions(BigDecimal addition, BigDecimal expected) {
        model.addObjectiveConstant(addition);
        assertEquals(expected, model.getObjectiveConstant(), () -> "After adding " + addition + " the objective constant should be " + expected);
    }

    /**
     * Adding {@code null} must throw a {@link NullPointerException}.
     */
    @Test
    void testAddObjectiveConstantNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.addObjectiveConstant(null), "addObjectiveConstant(null) should raise NullPointerException");
    }

    /**
     * Verifies that multiple calls to {@code addObjectiveConstant} accumulate
     * correctly. The test adds 7.10 and then 5.20 and expects 12.30.
     */
    @Test
    void testSequentialAdditionsAccumulate() {
        model.addObjectiveConstant(valueOf("7.10"));
        model.addObjectiveConstant(valueOf("5.20"));
        assertEquals(valueOf("12.30"), model.getObjectiveConstant(), "Sequential additions should be summed to 12.30");
    }
}
