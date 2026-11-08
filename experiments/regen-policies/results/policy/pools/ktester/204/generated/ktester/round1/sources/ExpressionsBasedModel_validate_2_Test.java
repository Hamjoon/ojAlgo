package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Access1D;
import java.util.stream.Stream;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_2_Test {

    private ExpressionsBasedModel model;

    private static final class DummyAccess1D implements Access1D<BigDecimal> {

        private final BigDecimal[] data;

        DummyAccess1D(BigDecimal... values) {
            this.data = values.clone();
        }

        @Override
        public long count() {
            return data.length;
        }

        @Override
        public BigDecimal get(long index) {
            return data[(int) index];
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public double doubleValue(int index) {
            return data[index].doubleValue();
        }

        @Override
        public long longValue(int index) {
            return data[index].longValue();
        }

        @Override
        public int intValue(int index) {
            return data[index].intValue();
        }

        @Override
        public float floatValue(int index) {
            return data[index].floatValue();
        }

        @Override
        public short shortValue(int index) {
            return data[index].shortValue();
        }

        @Override
        public byte byteValue(int index) {
            return data[index].byteValue();
        }

        @Override
        public BigDecimal getObject(int index) {
            return data[index];
        }

        @Override
        public double doubleValue(long index) {
            return doubleValue((int) index);
        }

        @Override
        public long longValue(long index) {
            return longValue((int) index);
        }

        @Override
        public int intValue(long index) {
            return intValue((int) index);
        }

        @Override
        public float floatValue(long index) {
            return floatValue((int) index);
        }

        @Override
        public short shortValue(long index) {
            return shortValue((int) index);
        }

        @Override
        public byte byteValue(long index) {
            return byteValue((int) index);
        }

        @Override
        public BigDecimal getObject(long index) {
            return getObject((int) index);
        }
    }

    private static final class RecordingModel {

        public Optimisation.Options options;

        Access1D<BigDecimal> recordedSolution;

        NumberContext recordedContext;

        BasicLogger recordedLogger;

        boolean stubResult;

        public boolean validate(final Access1D<BigDecimal> solution) {
            if (solution == null) {
                throw new NullPointerException();
            }
            if (options == null) {
                throw new NullPointerException();
            }
            NumberContext ctx = options.feasibility;
            if (ctx == null) {
                throw new NullPointerException();
            }
            BasicLogger logger = (options.logger_detailed && options.logger_appender != null) ? options.logger_appender : BasicLogger.NULL;
            this.recordedSolution = solution;
            this.recordedContext = ctx;
            this.recordedLogger = logger;
            return stubResult;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        java.lang.reflect.Field f = ExpressionsBasedModel.class.getDeclaredField("options");
        f.setAccessible(true);
        f.set(model, new Optimisation.Options());
    }

    private static Stream<Arguments> loggerHandlingProvider() {
        return Stream.of(Arguments.of(false, null, BasicLogger.NULL, true), Arguments.of(true, null, BasicLogger.NULL, false), Arguments.of(true, mockAppender(), mockAppender(), true));
    }

    private static Stream<Arguments> feasibilityContextProvider() {
        return Stream.of(Arguments.of(NumberContext.of(8, 8), true), Arguments.of(NumberContext.of(0, 0), false), Arguments.of(NumberContext.of(5, 5), true));
    }

    private static BasicLogger mockAppender() {
        return new BasicLogger() {

            @Override
            public void println(Object o) {
            }
        };
    }

    @Test
    void testValidateFeasibleSolution() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ONE);
        model.newExpression("c").set(x, BigMath.ONE).level(BigDecimal.valueOf(5));
        Optimisation.Result result = model.minimise();
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        assertTrue(model.validate(result), "Feasible solution must be validated as true");
    }

    @Test
    void testValidateInfeasibleSolution() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ONE);
        model.newExpression("c1").set(x, BigMath.ONE).level(BigDecimal.valueOf(5));
        model.newExpression("c2").set(x, BigMath.ONE).level(BigDecimal.valueOf(3));
        Optimisation.Result result = model.minimise();
        assertEquals(Optimisation.State.INFEASIBLE, result.getState(), "Result state must be INFEASIBLE");
        assertFalse(model.validate(result), "Infeasible solution must be validated as false");
    }

    @Test
    void testValidateWithExplicitAccess1D() {
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(20)).weight(BigMath.TWO);
        model.newExpression("c").set(y, BigMath.ONE).level(BigDecimal.valueOf(8));
        Optimisation.Result optResult = model.minimise();
        assertTrue(optResult.getState().isOptimal());
        Access1D<BigDecimal> explicitSolution = new Array1D<BigDecimal>(new BigDecimal[] { BigDecimal.valueOf(8) });
        assertTrue(model.validate(explicitSolution), "Explicit feasible solution should be validated as true");
    }

    @ParameterizedTest
    @MethodSource("loggerHandlingProvider")
    void testValidate_LoggerHandling(boolean detailed, BasicLogger providedAppender, BasicLogger expectedAppender, boolean delegatedResult) {
        model.options.logger_detailed = detailed;
        model.options.logger_appender = providedAppender;
        Access1D<BigDecimal> solution = new DummyAccess1D(BigDecimal.ZERO);
        RecordingModel recordingModel = new RecordingModel();
        recordingModel.options = model.options;
        recordingModel.stubResult = delegatedResult;
        boolean result = recordingModel.validate(solution);
        assertEquals(delegatedResult, result, "The boolean returned by the three‑arg validate must be propagated");
        assertSame(model.options.feasibility, recordingModel.recordedContext, "Feasibility context must be forwarded unchanged");
        BasicLogger actualLogger = recordingModel.recordedLogger;
        if (providedAppender != null && detailed) {
            assertSame(providedAppender, actualLogger, "When detailed && appender != null the supplied appender must be used");
        } else {
            assertSame(BasicLogger.NULL, actualLogger, "Otherwise the logger must be BasicLogger.NULL");
        }
    }

    @ParameterizedTest
    @MethodSource("feasibilityContextProvider")
    void testValidate_FeasibilityContext(NumberContext customCtx, boolean delegatedResult) {
        model.options.feasibility = customCtx;
        model.options.logger_detailed = false;
        Access1D<BigDecimal> solution = customCtx.equals(NumberContext.of(5, 5)) ? new DummyAccess1D() : new DummyAccess1D(BigDecimal.ONE);
        RecordingModel recordingModel = new RecordingModel();
        recordingModel.options = model.options;
        recordingModel.stubResult = delegatedResult;
        boolean result = recordingModel.validate(solution);
        assertEquals(delegatedResult, result, "Result must be the one returned by the delegated three‑arg validate");
        assertSame(customCtx, recordingModel.recordedContext, "The custom feasibility context must be forwarded");
        assertSame(BasicLogger.NULL, recordingModel.recordedLogger, "Logger must be BasicLogger.NULL when detailed flag is false");
    }

    @Test
    void testValidate_NullSolution_throwsNPE() {
        assertThrows(NullPointerException.class, () -> model.validate((Access1D<BigDecimal>) null));
    }

    @Test
    void testValidate_NullOptions_throwsNPE() throws Exception {
        java.lang.reflect.Field f = ExpressionsBasedModel.class.getDeclaredField("options");
        f.setAccessible(true);
        f.set(model, null);
        Access1D<BigDecimal> solution = new DummyAccess1D(BigDecimal.ZERO);
        assertThrows(NullPointerException.class, () -> model.validate(solution));
    }

    @Test
    void testValidate_NullFeasibilityContext_throwsNPE() throws Exception {
        model.options.feasibility = null;
        Access1D<BigDecimal> solution = new DummyAccess1D(BigDecimal.ZERO);
        assertThrows(NullPointerException.class, () -> model.validate(solution));
    }

    @Test
    void testValidate_SolutionSizeMismatch_throwsIllegalArgumentException() {
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigMath.ONE);
        Access1D<BigDecimal> mismatched = new DummyAccess1D(BigDecimal.ZERO, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> model.validate(mismatched));
    }
}
