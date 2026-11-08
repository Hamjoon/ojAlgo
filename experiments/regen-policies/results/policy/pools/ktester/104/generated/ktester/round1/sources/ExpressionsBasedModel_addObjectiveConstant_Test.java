package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_addObjectiveConstant_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model instance.
        model = new ExpressionsBasedModel();
    }

    /**
     * Supplies all single‑addition scenarios extracted from the JSON test cases.
     */
    static Stream<Arguments> singleAdditionProvider() {
        return Stream.of(// null → unchanged (ZERO)
        Arguments.of(null, BigDecimal.ZERO), // zero (signum == 0) → unchanged
        Arguments.of(BigDecimal.ZERO, BigDecimal.ZERO), // positive integer
        Arguments.of(new BigDecimal("5"), new BigDecimal("5")), // negative integer
        Arguments.of(new BigDecimal("-3"), new BigDecimal("-3")), // positive fractional
        Arguments.of(new BigDecimal("2.5"), new BigDecimal("2.5")), // explicit zero with scale > 0
        Arguments.of(new BigDecimal("0.0"), BigDecimal.ZERO), // negative zero (signum == 0)
        Arguments.of(new BigDecimal("-0.0"), BigDecimal.ZERO), // very large positive number
        Arguments.of(new BigDecimal("123456789012345678901234567890"), new BigDecimal("123456789012345678901234567890")), // small positive number in scientific notation
        Arguments.of(new BigDecimal("1E-10"), new BigDecimal("0.0000000001")));
    }

    /**
     * Supplies cumulative‑addition scenarios extracted from the JSON test cases.
     */
    static Stream<Arguments> cumulativeAdditionProvider() {
        return Stream.of(// 2 then 3 → 5
        Arguments.of(Arrays.asList(new BigDecimal("2"), new BigDecimal("3")), new BigDecimal("5")), // -5 then 8 → 3
        Arguments.of(Arrays.asList(new BigDecimal("-5"), new BigDecimal("8")), new BigDecimal("3")), // 4 then null → 4 (null ignored)
        Arguments.of(Arrays.asList(new BigDecimal("4"), null), new BigDecimal("4")), // 10, -3.5, 0 (ignored), null (ignored) → 6.5
        Arguments.of(Arrays.asList(new BigDecimal("10"), new BigDecimal("-3.5"), BigDecimal.ZERO, null), new BigDecimal("6.5")));
    }

    @Test
    void testAddNullDoesNotChangeConstant() {
        BigDecimal before = model.getObjectiveConstant();
        // Invoke the method with a null argument.
        model.addObjectiveConstant(null);
        BigDecimal after = model.getObjectiveConstant();
        assertEquals(before, after, "Adding null should leave the objective constant unchanged");
    }

    @Test
    void testAddZeroDoesNotChangeConstant() {
        BigDecimal before = model.getObjectiveConstant();
        // Explicit zero (signum == 0) must be ignored.
        model.addObjectiveConstant(BigDecimal.ZERO);
        BigDecimal after = model.getObjectiveConstant();
        assertEquals(before, after, "Adding zero should leave the objective constant unchanged");
    }

    @Test
    void testAddPositiveValue() {
        BigDecimal addition = new BigDecimal("123.456");
        model.addObjectiveConstant(addition);
        assertEquals(addition, model.getObjectiveConstant(), "Positive addition should be reflected in the objective constant");
    }

    @Test
    void testAddNegativeValue() {
        BigDecimal addition = new BigDecimal("-78.9");
        model.addObjectiveConstant(addition);
        assertEquals(addition, model.getObjectiveConstant(), "Negative addition should be reflected in the objective constant");
    }

    @Test
    void testCumulativeAdditions() {
        BigDecimal first = new BigDecimal("10");
        BigDecimal second = new BigDecimal("-3.5");
        // should be ignored
        BigDecimal third = new BigDecimal("0");
        // should be ignored
        BigDecimal fourth = null;
        model.addObjectiveConstant(first);
        model.addObjectiveConstant(second);
        model.addObjectiveConstant(third);
        model.addObjectiveConstant(fourth);
        // 10 - 3.5 = 6.5
        BigDecimal expected = first.add(second);
        assertEquals(expected, model.getObjectiveConstant(), "Cumulative non‑zero, non‑null additions should be summed correctly");
    }

    /**
     * Parameterized test that verifies the effect of a single addition.
     *
     * @param addition the argument passed to {@code addObjectiveConstant}
     * @param expected the expected value of {@code getObjectiveConstant()} after the call
     */
    @ParameterizedTest(name = "{index}: addition={0}, expected={1}")
    @MethodSource("singleAdditionProvider")
    void testAddObjectiveConstantSingle(BigDecimal addition, BigDecimal expected) {
        // The model starts with a constant of ZERO.
        model.addObjectiveConstant(addition);
        assertEquals(expected, model.getObjectiveConstant(), () -> "After adding " + addition + " the constant should be " + expected);
    }

    /**
     * Parameterized test that performs a sequence of additions and checks the final constant.
     *
     * @param additions the list of arguments supplied to successive calls of {@code addObjectiveConstant}
     * @param expected  the expected final value of {@code getObjectiveConstant()}
     */
    @ParameterizedTest(name = "{index}: additions={0}, expected={1}")
    @MethodSource("cumulativeAdditionProvider")
    void testAddObjectiveConstantCumulative(List<BigDecimal> additions, BigDecimal expected) {
        // Apply each addition in order.
        for (BigDecimal add : additions) {
            model.addObjectiveConstant(add);
        }
        assertEquals(expected, model.getObjectiveConstant(), () -> "After cumulative additions " + additions + " the constant should be " + expected);
    }
}
