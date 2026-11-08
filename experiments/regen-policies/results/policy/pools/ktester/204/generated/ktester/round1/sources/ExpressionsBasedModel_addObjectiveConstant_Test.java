package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.ojalgo.function.constant.BigMath;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionsBasedModel_addObjectiveConstant_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model with the default objective constant (ZERO).
        model = new ExpressionsBasedModel();
        // a fresh model for every test – the constant starts at BigMath.ZERO
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        model = null;
        model = null;
    }

    /**
     * Provides test data derived from the supplied JSON description.
     *
     * Each {@code Arguments} contains:
     *   - a {@code List<BigDecimal>} representing the sequence of calls to {@code addObjectiveConstant}
     *   - the expected constant after all calls have been performed
     *   - a short description used in the test name
     */
    static Stream<Arguments> addObjectiveConstantProvider() {
        return Stream.of(// 1️⃣ null argument – should be ignored, constant stays ZERO
        Arguments.of(Arrays.asList((BigDecimal) null), ZERO, "null argument is ignored"), // 2️⃣ zero argument – signum == 0, ignored
        Arguments.of(Arrays.asList(ZERO), ZERO, "zero argument is ignored"), // 3️⃣ positive addition
        Arguments.of(Arrays.asList(new BigDecimal("5.5")), new BigDecimal("5.5"), "positive non‑zero addition is applied"), // 4️⃣ negative addition
        Arguments.of(Arrays.asList(new BigDecimal("-3")), new BigDecimal("-3"), "negative non‑zero addition is applied"), // 5️⃣ multiple sequential calls: 2, -1, 0 → final constant 1
        Arguments.of(Arrays.asList(new BigDecimal("2"), new BigDecimal("-1"), // should be ignored
        ZERO), new BigDecimal("1"), "multiple sequential calls accumulate correctly"));
    }

    @Test
    void testAddObjectiveConstant_NullDoesNotChange() {
        BigDecimal before = model.getObjectiveConstant();
        assertEquals(BigMath.ZERO, before, "Initial constant should be ZERO");
        // package‑private method is visible because test is in same package
        model.addObjectiveConstant(null);
        BigDecimal after = model.getObjectiveConstant();
        assertEquals(before, after, "Constant must remain unchanged when null is added");
    }

    @Test
    void testAddObjectiveConstant_ZeroDoesNotChange() {
        BigDecimal before = model.getObjectiveConstant();
        assertEquals(BigMath.ZERO, before, "Initial constant should be ZERO");
        model.addObjectiveConstant(BigMath.ZERO);
        BigDecimal after = model.getObjectiveConstant();
        assertEquals(before, after, "Constant must remain unchanged when zero is added");
    }

    @Test
    void testAddObjectiveConstant_PositiveAdds() {
        BigDecimal addition = new BigDecimal("12.34");
        model.addObjectiveConstant(addition);
        BigDecimal expected = BigMath.ZERO.add(addition);
        assertEquals(expected, model.getObjectiveConstant(), "Positive addition should be added to the existing constant");
    }

    @Test
    void testAddObjectiveConstant_NegativeAdds() {
        // Start with a non‑zero constant to verify accumulation.
        model.addObjectiveConstant(new BigDecimal("5"));
        BigDecimal negative = new BigDecimal("-2.5");
        model.addObjectiveConstant(negative);
        // 2.5
        BigDecimal expected = new BigDecimal("5").add(negative);
        assertEquals(expected, model.getObjectiveConstant(), "Negative addition should be added (i.e., subtracted) from the existing constant");
    }

    @Test
    void testAddObjectiveConstant_MultipleCallsAccumulate() {
        model.addObjectiveConstant(new BigDecimal("1"));
        model.addObjectiveConstant(new BigDecimal("2"));
        model.addObjectiveConstant(new BigDecimal("-0.5"));
        // 1 + 2 - 0.5
        BigDecimal expected = new BigDecimal("2.5");
        assertEquals(expected, model.getObjectiveConstant(), "Multiple calls should accumulate correctly");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("addObjectiveConstantProvider")
    void testAddObjectiveConstant_Parameterized(List<BigDecimal> additions, BigDecimal expected, String description) {
        // Apply each addition in order, mimicking the real usage pattern.
        for (BigDecimal add : additions) {
            model.addObjectiveConstant(add);
        }
        // Verify the internal constant matches the expectation.
        assertEquals(expected, model.getObjectiveConstant(), () -> "After applying [" + additions + "], expected constant = " + expected);
    }
}
