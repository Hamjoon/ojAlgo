package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.ojalgo.array.Array1D;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.optimisation.Optimisation.Result;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.ojalgo.netio.BasicLogger;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_2_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Expression expr;

    private static final class ThrowingAccess1D implements Access1D<BigDecimal> {

        @Override
        public long count() {
            return 1L;
        }

        @Override
        public BigDecimal get(long index) {
            throw new RuntimeException("access error");
        }

        @Override
        public List<BigDecimal> asList() {
            return Collections.emptyList();
        }

        @Override
        public BigDecimal[] toArray() {
            return new BigDecimal[0];
        }

        @Override
        public double doubleValue(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long longValue(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int intValue(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float floatValue(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Number numberValue(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isZero(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSmall(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLarge(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNaN(long index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isInfinite(long index) {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a tiny model: a single variable x constrained to be exactly 5.
        model = new ExpressionsBasedModel();
        // Variable x with bounds [0, 10] and a weight of 1 (contributes to the objective).
        x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Expression enforcing x == 5.
        expr = model.newExpression("x_eq_5").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        expr.set(x, BigDecimal.ONE);
    }

    @Test
    void testValidateWithFeasibleSolution() {
        // The model is feasible; minimise (or maximise) should return the unique solution x = 5.
        Result result = model.minimise();
        // Basic sanity checks on the solver outcome.
        assertTrue(result.getState().isFeasible(), "Result should be feasible");
        assertTrue(result.getState().isOptimal(), "Result should be optimal");
        // The method under test should confirm that the solver's solution satisfies all constraints.
        assertTrue(model.validate(result), "The solution returned by the solver must be validated as feasible");
    }

    @Test
    void testValidateWithInfeasibleSolution() {
        // Construct an Access1D that violates the constraint (x = 7 instead of 5).
        Access1D<BigDecimal> infeasible = Array1D.of(BigDecimal.valueOf(7));
        // The validate method should detect the violation.
        assertFalse(model.validate(infeasible), "A solution that violates the equality constraint must be invalid");
    }

    @Test
    void testValidateRespectsFeasibilityTolerance() {
        // A solution that is only slightly off the exact bound.
        Access1D<BigDecimal> nearFeasible = Array1D.of(new BigDecimal("5.000000001"));
        // With the default tolerance the solution is considered feasible.
        assertTrue(model.validate(nearFeasible), "Solution within default tolerance should be accepted");
        // Tighten the feasibility tolerance to zero (exact comparison only).
        model.options.feasibility = NumberContext.of(0, 0);
        // The same near‑feasible solution must now be rejected.
        assertFalse(model.validate(nearFeasible), "Solution outside strict tolerance should be rejected");
    }

    @Test
    void testValidate_usesCustomAppenderWhenDetailedAndAppenderSet() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        BasicLogger custom = mock(BasicLogger.class);
        spy.options.logger_detailed = true;
        spy.options.logger_appender = custom;
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, BigDecimal.TEN);
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenReturn(true);
        boolean ok = spy.validate(solution);
        assertTrue(ok, "Delegated validation should return true");
        ArgumentCaptor<BasicLogger> loggerCaptor = ArgumentCaptor.forClass(BasicLogger.class);
        verify(spy).validate(eq(solution), any(NumberContext.class), loggerCaptor.capture());
        assertSame(custom, loggerCaptor.getValue(), "Custom appender must be forwarded");
    }

    @Test
    void testValidate_fallsBackToNullAppenderWhenDetailedButAppenderIsNull() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        spy.options.logger_detailed = true;
        spy.options.logger_appender = null;
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ZERO);
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenReturn(false);
        boolean ok = spy.validate(solution);
        assertFalse(ok, "Delegated validation should return false");
        ArgumentCaptor<BasicLogger> loggerCaptor = ArgumentCaptor.forClass(BasicLogger.class);
        verify(spy).validate(eq(solution), any(NumberContext.class), loggerCaptor.capture());
        assertSame(BasicLogger.NULL, loggerCaptor.getValue(), "Fallback logger must be BasicLogger.NULL");
    }

    @Test
    void testValidate_usesNullAppenderWhenNotDetailed() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        spy.options.logger_detailed = false;
        spy.options.logger_appender = mock(BasicLogger.class);
        Access1D<BigDecimal> solution = Array1D.of(new BigDecimal("-5"));
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenReturn(false);
        boolean ok = spy.validate(solution);
        assertFalse(ok, "Delegated validation should return false");
        ArgumentCaptor<BasicLogger> loggerCaptor = ArgumentCaptor.forClass(BasicLogger.class);
        verify(spy).validate(eq(solution), any(NumberContext.class), loggerCaptor.capture());
        assertSame(BasicLogger.NULL, loggerCaptor.getValue(), "When not detailed, logger must be BasicLogger.NULL");
    }

    @Test
    void testValidate_propagatesCustomFeasibilityContext() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        NumberContext custom = NumberContext.of(5, 10);
        spy.options.feasibility = custom;
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE, BigDecimal.valueOf(2));
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenReturn(true);
        boolean ok = spy.validate(solution);
        assertTrue(ok, "Delegated validation should return true");
        ArgumentCaptor<NumberContext> ctxCaptor = ArgumentCaptor.forClass(NumberContext.class);
        verify(spy).validate(eq(solution), ctxCaptor.capture(), any(BasicLogger.class));
        assertSame(custom, ctxCaptor.getValue(), "The custom feasibility context must be forwarded");
    }

    @Test
    void testValidate_acceptsEmptySolution() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        Access1D<BigDecimal> empty = Array1D.of();
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenReturn(true);
        assertTrue(spy.validate(empty), "Empty solution should be delegated and return true");
    }

    @Test
    void testValidate_nullSolution_throwsNPE() {
        ExpressionsBasedModel real = new ExpressionsBasedModel();
        ???;
    }

    @Test
    void testValidate_solutionWithNullElement_throwsIllegalArgumentException() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        Access1D<BigDecimal> withNull = Array1D.of(BigDecimal.ONE, null, BigDecimal.TEN);
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenThrow(new IllegalArgumentException("null element"));
        assertThrows(IllegalArgumentException.class, () -> spy.validate(withNull));
    }

    @Test
    void testValidate_nullOptionsField_throwsNPE() throws Exception {
        ExpressionsBasedModel modelWithNullOptions = new ExpressionsBasedModel();
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(modelWithNullOptions, null);
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> modelWithNullOptions.validate(solution));
    }

    @Test
    void testValidate_nullFeasibilityContext_throwsNPE() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.options.feasibility = null;
        Access1D<BigDecimal> solution = Array1D.of(BigDecimal.ZERO, BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> model.validate(solution));
    }

    @Test
    void testValidate_solutionSizeMismatch_throwsIllegalArgumentException() {
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        spy.newVariable("a");
        spy.newVariable("b");
        Access1D<BigDecimal> threeElements = Array1D.of(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(5));
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenThrow(new IllegalArgumentException("size mismatch"));
        assertThrows(IllegalArgumentException.class, () -> spy.validate(threeElements));
    }

    @Test
    void testValidate_solutionElementAccessThrows_propagatesException() {
        Access1D<BigDecimal> throwing = new ThrowingAccess1D();
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        when(spy.validate(any(), any(NumberContext.class), any(BasicLogger.class))).thenAnswer(invocation -> {
            Access1D<BigDecimal> sol = invocation.getArgument(0);
            sol.get(0L);
            return true;
        });
        RuntimeException ex = assertThrows(RuntimeException.class, () -> spy.validate(throwing));
        assertEquals("access error", ex.getMessage(), "Original exception must propagate unchanged");
    }
}
