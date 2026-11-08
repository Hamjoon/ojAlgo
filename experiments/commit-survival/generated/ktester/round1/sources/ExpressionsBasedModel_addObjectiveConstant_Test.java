package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_addObjectiveConstant_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        model = null;
    }

    /**
     * Provides the test data for {@link #testAddObjectiveConstant_SingleAddition}.
     * Each element is {addition, expectedResult}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> singleAdditionProvider() {
        return Stream.of(// null argument
        org.junit.jupiter.params.provider.Arguments.of(null, "0"), // explicit zero
        org.junit.jupiter.params.provider.Arguments.of("0", "0"), // positive number
        org.junit.jupiter.params.provider.Arguments.of("123.456", "123.456"), // negative number
        org.junit.jupiter.params.provider.Arguments.of("-78.90", "-78.90"), // zero with non‑default scale
        org.junit.jupiter.params.provider.Arguments.of("0.00", "0"), // zero expressed in scientific notation
        org.junit.jupiter.params.provider.Arguments.of("0E-10", "0"), // high‑precision positive number
        org.junit.jupiter.params.provider.Arguments.of("1.2345678901234567890123456789", "1.2345678901234567890123456789"), // small negative number
        org.junit.jupiter.params.provider.Arguments.of("-0.0001", "-0.0001"), // very large integer
        org.junit.jupiter.params.provider.Arguments.of("123456789012345678901234567890", "123456789012345678901234567890"), // scale handling: 1.0 + 2.00 => 3.00 (scale of the larger operand)
        // will be used in a multi‑call test
        org.junit.jupiter.params.provider.Arguments.of("1.0", "1.0"));
    }

    /**
     * Provides sequences of additions together with the expected final constant.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> multipleCallsProvider() {
        return Stream.of(// 2 + 3 - 1 = 4
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("2", "3", "-1"), "4"), // 10 then -4 = 6
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("10", "-4"), "6"), // 5 + 5 -10 = 0
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("5", "5", "-10"), "0"), // after a non‑zero addition, a null argument does not change the constant
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("100", null), "100"), // after a non‑zero addition, an explicit zero does not change the constant
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("100", "0"), "100"), // 1.0 + 2.00 = 3.00 (scale 2)
        org.junit.jupiter.params.provider.Arguments.of(Arrays.asList("1.0", "2.00"), "3.00"));
    }

    @Test
    void testAddObjectiveConstant_NullDoesNotChange() {
        BigDecimal before = model.getObjectiveConstant();
        assertEquals(BigDecimal.ZERO, before, "Initial objective constant should be zero");
        model.addObjectiveConstant(null);
        assertEquals(before, model.getObjectiveConstant(), "Calling with null must leave the objective constant unchanged");
    }

    @Test
    void testAddObjectiveConstant_ZeroDoesNotChange() {
        BigDecimal before = model.getObjectiveConstant();
        assertEquals(BigDecimal.ZERO, before, "Initial objective constant should be zero");
        model.addObjectiveConstant(BigDecimal.ZERO);
        assertEquals(before, model.getObjectiveConstant(), "Calling with zero must leave the objective constant unchanged");
    }

    @Test
    void testAddObjectiveConstant_PositiveAddition() {
        BigDecimal addition = new BigDecimal("5.25");
        model.addObjectiveConstant(addition);
        assertEquals(addition, model.getObjectiveConstant(), "Positive addition should be reflected in the objective constant");
    }

    @Test
    void testAddObjectiveConstant_NegativeAddition() {
        BigDecimal addition = new BigDecimal("-3.75");
        model.addObjectiveConstant(addition);
        assertEquals(addition, model.getObjectiveConstant(), "Negative addition should be reflected in the objective constant");
    }

    @Test
    void testAddObjectiveConstant_MultipleCallsAccumulate() {
        model.addObjectiveConstant(new BigDecimal("2"));
        model.addObjectiveConstant(new BigDecimal("3"));
        model.addObjectiveConstant(new BigDecimal("-1"));
        // 2 + 3 - 1
        BigDecimal expected = new BigDecimal("4");
        assertEquals(expected, model.getObjectiveConstant(), "Multiple calls must accumulate correctly");
    }

    @Test
    void testAddObjectiveConstant_IgnoresZeroSignumEvenWhenNonNull() {
        // A BigDecimal with scale but zero value, e.g. 0.00
        BigDecimal zeroWithScale = new BigDecimal("0.00");
        model.addObjectiveConstant(zeroWithScale);
        assertEquals(BigDecimal.ZERO, model.getObjectiveConstant(), "Zero value (signum == 0) must not affect the constant even if non‑null");
    }

    /**
     * Executes a single {@code addObjectiveConstant} call and checks the resulting
     * objective constant against the expected value.
     */
    @ParameterizedTest(name = "{index}: addition={0}, expected={1}")
    @MethodSource("singleAdditionProvider")
    void testAddObjectiveConstant_SingleAddition(String additionStr, String expectedStr) {
        // Arrange: convert the JSON strings to the appropriate objects.
        BigDecimal addition = additionStr == null ? null : new BigDecimal(additionStr);
        BigDecimal expected = new BigDecimal(expectedStr);
        // Act
        model.addObjectiveConstant(addition);
        // Assert
        assertEquals(expected, model.getObjectiveConstant(), () -> "After adding " + additionStr + " the constant should be " + expectedStr);
    }

    /**
     * Executes a sequence of {@code addObjectiveConstant} calls and verifies the final
     * objective constant.
     */
    @ParameterizedTest(name = "{index}: sequence={0}, expected={1}")
    @MethodSource("multipleCallsProvider")
    void testAddObjectiveConstant_MultipleCalls(List<String> additions, String expectedStr) {
        // Apply each addition in order (null strings represent a null argument)
        for (String s : additions) {
            BigDecimal addition = s == null ? null : new BigDecimal(s);
            model.addObjectiveConstant(addition);
        }
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(expected, model.getObjectiveConstant(), () -> "After sequence " + additions + " the constant should be " + expectedStr);
    }
}
