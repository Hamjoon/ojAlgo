package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getObjectiveConstant_Test {

    private ExpressionsBasedModel model;

    /**
     * Create a fresh model before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // uses default Optimisation.Options
        model = new ExpressionsBasedModel();
    }

    /**
     * The default objective constant must be zero.
     */
    @Test
    void testDefaultObjectiveConstantIsZero() {
        BigDecimal constant = model.getObjectiveConstant();
        assertNotNull(constant, "Constant should never be null");
        assertEquals(ZERO, constant, "A newly created model must have an objective constant of 0");
    }

    /**
     * Adding a constant updates the value returned by {@code getObjectiveConstant()}.
     */
    @Test
    void testAddObjectiveConstantUpdatesValue() {
        // add 5.5
        model.addObjectiveConstant(valueOf("5.5"));
        assertEquals(valueOf("5.5"), model.getObjectiveConstant());
        // add another 2.25 – the method adds (i.e. accumulates) the value
        model.addObjectiveConstant(valueOf("2.25"));
        assertEquals(valueOf("7.75"), model.getObjectiveConstant());
    }

    /**
     * The copy‑constructor must copy the objective constant from the source model.
     */
    @Test
    void testCopyConstructorCopiesObjectiveConstant() {
        // arrange: set a non‑zero constant in the original model
        model.addObjectiveConstant(valueOf("12.34"));
        // act: create a shallow copy (shallow = false, prune = false – the typical public copy())
        ExpressionsBasedModel copy = model.copy();
        // assert: the constant in the copy equals the original's constant
        assertEquals(model.getObjectiveConstant(), copy.getObjectiveConstant(), "Copy constructor should preserve the objective constant");
    }

    /**
     * The {@code copy(boolean shallow, boolean prune)} overload also preserves the constant.
     */
    @Test
    void testCopyWithFlagsPreservesConstant() {
        model.addObjectiveConstant(valueOf("-3.21"));
        // use the protected constructor via the public API that forwards to it
        ExpressionsBasedModel copy = new ExpressionsBasedModel(model, true, true);
        assertEquals(valueOf("-3.21"), copy.getObjectiveConstant(), "Copy with shallow/prune flags must keep the original constant");
    }

    /**
     * Parameterised test covering:
     * <ul>
     *   <li>adding a single positive constant</li>
     *   <li>adding a single negative constant</li>
     *   <li>adding zero (should leave the constant unchanged)</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] add {0} → expect {1}")
    @CsvSource({ // input constant , expected constant after addition
    "'123.456',   '123.456'", "'-78.9',    '-78.9'", "'0',        '0'" })
    void testSimpleAdditions(String additionStr, String expectedStr) {
        BigDecimal addition = new BigDecimal(additionStr);
        BigDecimal expected = new BigDecimal(expectedStr);
        model.addObjectiveConstant(addition);
        assertEquals(expected, model.getObjectiveConstant(), () -> "After adding " + addition + " the constant should be " + expected);
    }

    /**
     * Test that multiple sequential additions are accumulated correctly.
     */
    @Test
    void testMultipleAdditionsAreAccumulated() {
        model.addObjectiveConstant(valueOf("5.0"));
        model.addObjectiveConstant(valueOf("5.5"));
        assertEquals(valueOf("10.5"), model.getObjectiveConstant(), "5.0 + 5.5 must result in 10.5");
    }

    /**
     * Adding {@code null} must throw a NullPointerException and leave the constant unchanged.
     */
    @Test
    void testAddObjectiveConstantNullThrowsNPE() {
        BigDecimal before = model.getObjectiveConstant();
        assertThrows(NullPointerException.class, () -> model.addObjectiveConstant(null), "addObjectiveConstant(null) should throw NPE");
        assertEquals(before, model.getObjectiveConstant(), "Constant must remain unchanged after NPE");
    }

    /**
     * Very large constants should be stored exactly.
     */
    @Test
    void testAddExtremelyLargeConstant() {
        BigDecimal huge = new BigDecimal("1E+100");
        model.addObjectiveConstant(huge);
        assertEquals(huge, model.getObjectiveConstant(), "Extremely large constant must be stored without loss of magnitude");
    }

    /**
     * High‑precision constants must preserve all digits.
     */
    @Test
    void testAddHighPrecisionConstant() {
        BigDecimal precise = new BigDecimal("0.123456789123456789");
        model.addObjectiveConstant(precise);
        assertEquals(precise, model.getObjectiveConstant(), "High‑precision constant must be preserved exactly");
    }

    /**
     * Adding a value and its negative counterpart must cancel out to zero.
     */
    @Test
    void testAddAndCancelConstant() {
        model.addObjectiveConstant(valueOf("42.5"));
        model.addObjectiveConstant(valueOf("-42.5"));
        assertEquals(ZERO, model.getObjectiveConstant(), "Adding 42.5 and -42.5 should result in ZERO");
    }

    /**
     * Copying a model must preserve the objective constant.
     * This also validates the {@code copy()} overload.
     */
    @Test
    void testCopyPreservesObjectiveConstant() {
        model.addObjectiveConstant(valueOf("999.99"));
        ExpressionsBasedModel copy = model.copy();
        assertEquals(model.getObjectiveConstant(), copy.getObjectiveConstant(), "Copy of model must retain the original objective constant");
    }

    /**
     * Relaxation must not modify the already stored objective constant.
     */
    @Test
    void testRelaxDoesNotChangeConstant() {
        model.addObjectiveConstant(valueOf("15.75"));
        // relaxation is a no‑op for the constant
        model.relax();
        assertEquals(valueOf("15.75"), model.getObjectiveConstant(), "Relaxation should not affect the objective constant");
    }

    /**
     * A freshly created model that is immediately relaxed must still have ZERO constant.
     */
    @Test
    void testRelaxOnFreshModelKeepsZero() {
        model.relax();
        assertEquals(ZERO, model.getObjectiveConstant(), "A fresh model relaxed without prior additions must still have ZERO constant");
    }
}
