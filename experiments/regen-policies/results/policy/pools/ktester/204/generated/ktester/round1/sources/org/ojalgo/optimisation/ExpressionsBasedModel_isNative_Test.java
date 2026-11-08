package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link ExpressionsBasedModel#isNative(ExpressionsBasedModel)}.
 */
// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isNative_Test {

    /**
     * Simple stub that overrides the methods used by {@code isNative}.
     */
//     private static final class StubModel extends ExpressionsBasedModel {

        private final boolean anyVariableInteger;
        private final boolean anyExpressionQuadratic;
        private final int variableCount;
        private final int expressionCount;

        StubModel(boolean anyVariableInteger, boolean anyExpressionQuadratic, int variableCount, int expressionCount) {
            this.anyVariableInteger = anyVariableInteger;
            this.anyExpressionQuadratic = anyExpressionQuadratic;
            this.variableCount = variableCount;
            this.expressionCount = expressionCount;
        }

        @Override
        public boolean isAnyVariableInteger() {
            return anyVariableInteger;
        }

        @Override
        public boolean isAnyExpressionQuadratic() {
            return anyExpressionQuadratic;
        }

        @Override
        public int countVariables() {
            return variableCount;
        }

        @Override
        public int countExpressions() {
            return expressionCount;
        }
    }

    private static Stream<Arguments> mipTrueQpFalseVariableThresholdProvider() {
        return Stream.of(
                // exactly at threshold → false
                Arguments.of(true, false, 50, 10, false),
                // just above threshold → true
                Arguments.of(true, false, 51, 10, true));
    }

    private static Stream<Arguments> mipTrueQpFalseExpressionThresholdProvider() {
        return Stream.of(
                // exactly at threshold → false
                Arguments.of(true, false, 10, 40, false),
                // just above threshold → true
                Arguments.of(true, false, 10, 41, true));
    }

    private static Stream<Arguments> mipTrueQpTrueProvider() {
        return Stream.of(
                Arguments.of(true, true, 0, 0),
                Arguments.of(true, true, 1000, 1000));
    }

    private static Stream<Arguments> mipFalseQpTrueVariableThresholdProvider() {
        return Stream.of(
                // exactly at threshold → false
                Arguments.of(false, true, 200, 10, false),
                // just above threshold → true
                Arguments.of(false, true, 201, 10, true));
    }

    private static Stream<Arguments> mipFalseQpTrueExpressionThresholdProvider() {
        return Stream.of(
                // exactly at threshold → false
                Arguments.of(false, true, 10, 200, false),
                // just above threshold → true
                Arguments.of(false, true, 10, 201, true));
    }

    private static Stream<Arguments> mipFalseQpFalseVariableThresholdProvider() {
        return Stream.of(
                // exactly at threshold → false
                Arguments.of(false, false, 500, 10, false),
                // just above threshold → true
                Arguments.of(false, false, 501, 10, true));
    }

    private static Stream<Arguments> mipFalseQpFalseExpressionThresholdProvider() {
        return Stream.of(
                // exactly at threshold → false
                Arguments.of(false, false, 10, 400, false),
                // just above threshold → true
                Arguments.of(false, false, 10, 401, true));
    }

    /**
     * mip = true, qp = true → always false
     */
    @Test
    void testMipAndQpBothTrue() {
        ExpressionsBasedModel model = new StubModel(true, true, 100, 100);
        assertFalse(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = true, qp = false, variables > 50 → true
     */
    @Test
    void testMipTrueVariablesAboveThreshold() {
        ExpressionsBasedModel model = new StubModel(true, false, 51, 10);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = true, qp = false, expressions > 40 → true
     */
    @Test
    void testMipTrueExpressionsAboveThreshold() {
        ExpressionsBasedModel model = new StubModel(true, false, 10, 41);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = true, qp = false, both below thresholds → false
     */
    @Test
    void testMipTrueBothBelowThresholds() {
        ExpressionsBasedModel model = new StubModel(true, false, 30, 20);
        assertFalse(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = false, qp = true, variables > 200 → true
     */
    @Test
    void testQpTrueVariablesAboveThreshold() {
        ExpressionsBasedModel model = new StubModel(false, true, 201, 10);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = false, qp = true, expressions > 200 → true
     */
    @Test
    void testQpTrueExpressionsAboveThreshold() {
        ExpressionsBasedModel model = new StubModel(false, true, 10, 201);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = false, qp = true, both below thresholds → false
     */
    @Test
    void testQpTrueBothBelowThresholds() {
        ExpressionsBasedModel model = new StubModel(false, true, 150, 150);
        assertFalse(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = false, qp = false, variables > 500 → true
     */
    @Test
    void testNeitherTrueVariablesAboveThreshold() {
        ExpressionsBasedModel model = new StubModel(false, false, 501, 10);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = false, qp = false, expressions > 400 → true
     */
    @Test
    void testNeitherTrueExpressionsAboveThreshold() {
        ExpressionsBasedModel model = new StubModel(false, false, 10, 401);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * mip = false, qp = false, both below thresholds → false
     */
    @Test
    void testNeitherTrueBothBelowThresholds() {
        ExpressionsBasedModel model = new StubModel(false, false, 300, 300);
        assertFalse(ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP true, QP false – variable count threshold (exactly 50 should be false)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected={4}")
    @MethodSource("mipTrueQpFalseVariableThresholdProvider")
    void testMipTrueQpFalseVariableThreshold(boolean mip, boolean qp, int vars, int exprs, boolean expected) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertEquals(expected, ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP true, QP false – expression count threshold (exactly 40 should be false)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected={4}")
    @MethodSource("mipTrueQpFalseExpressionThresholdProvider")
    void testMipTrueQpFalseExpressionThreshold(boolean mip, boolean qp, int vars, int exprs, boolean expected) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertEquals(expected, ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP true, QP true – always false (any size)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected=false")
    @MethodSource("mipTrueQpTrueProvider")
    void testMipTrueQpTrueAlwaysFalse(boolean mip, boolean qp, int vars, int exprs) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertFalse(ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP false, QP true – variable count threshold (exactly 200 should be false)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected={4}")
    @MethodSource("mipFalseQpTrueVariableThresholdProvider")
    void testMipFalseQpTrueVariableThreshold(boolean mip, boolean qp, int vars, int exprs, boolean expected) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertEquals(expected, ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP false, QP true – expression count threshold (exactly 200 should be false)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected={4}")
    @MethodSource("mipFalseQpTrueExpressionThresholdProvider")
    void testMipFalseQpTrueExpressionThreshold(boolean mip, boolean qp, int vars, int exprs, boolean expected) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertEquals(expected, ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP false, QP false – variable count threshold (exactly 500 should be false)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected={4}")
    @MethodSource("mipFalseQpFalseVariableThresholdProvider")
    void testMipFalseQpFalseVariableThreshold(boolean mip, boolean qp, int vars, int exprs, boolean expected) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertEquals(expected, ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP false, QP false – expression count threshold (exactly 400 should be false)
     */
    @ParameterizedTest(name = "[{index}] mip={0}, qp={1}, vars={2}, exprs={3} → expected={4}")
    @MethodSource("mipFalseQpFalseExpressionThresholdProvider")
    void testMipFalseQpFalseExpressionThreshold(boolean mip, boolean qp, int vars, int exprs, boolean expected) {
        ExpressionsBasedModel model = new StubModel(mip, qp, vars, exprs);
        assertEquals(expected, ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP true, QP false – both thresholds exceeded → true
     */
    @Test
    void testMipTrueQpFalseBothExceeded() {
        ExpressionsBasedModel model = new StubModel(true, false, 100, 100);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP false, QP true – both thresholds exceeded → true
     */
    @Test
    void testMipFalseQpTrueBothExceeded() {
        ExpressionsBasedModel model = new StubModel(false, true, 300, 300);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * MIP false, QP false – both thresholds exceeded → true
     */
    @Test
    void testMipFalseQpFalseBothExceeded() {
        ExpressionsBasedModel model = new StubModel(false, false, 600, 500);
        assertTrue(ExpressionsBasedModel.isNative(model));
    }

    /**
     * Null model argument – should throw NullPointerException
     */
    @Test
    void testNullModelThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.isNative(null));
    }
}