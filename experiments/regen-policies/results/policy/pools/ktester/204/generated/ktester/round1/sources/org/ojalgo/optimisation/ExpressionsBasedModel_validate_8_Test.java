package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.ojalgo.array.Array1D;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.netio.BasicLogger;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_8_Test {

    private ExpressionsBasedModel model;

    private NumberContext context;

    private BasicLogger logger;

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for each test
        model = new ExpressionsBasedModel();
        // the context used in the JSON spec
        context = NumberContext.of(10, 10);
        // silent logger – no output needed
        logger = BasicLogger.NULL;
    }

    private static Stream<Arguments> dummyProvider() {
        // placeholder – no parameterised tests at the moment
        return Stream.empty();
    }

    /**
     * {@link ExpressionsBasedModel#validate(Access1D, NumberContext, BasicLogger)} must reject null arguments.
     */
    @Test
    void testValidateRejectsNullArguments() {
        // a real (but empty) solution – the value itself is irrelevant because the
        // method should fail before accessing it
        Access1D<BigDecimal> realSolution = new Array1D<>(new BigDecimal[] {});
        // 1️⃣ solution == null
        assertThrows(NullPointerException.class, () -> model.validate(null, context, logger), "Null solution should trigger NPE");
        // 2️⃣ context == null – any non‑null solution works because the method should fail before using it
        Access1D<BigDecimal> anySolution = new Array1D<>(new BigDecimal[] {});
        assertThrows(NullPointerException.class, () -> model.validate(anySolution, null, logger), "Null NumberContext should trigger NPE");
        // 3️⃣ logger == null
        assertThrows(NullPointerException.class, () -> model.validate(realSolution, context, null), "Null BasicLogger should trigger NPE");
    }

    /**
     * Validation fails when the supplied solution size does not match the number of variables.
     */
    @Test
    void testValidateFailsOnSizeMismatch() {
        // Model with two variables
        // index 0
        model.addVariable();
        // index 1
        model.addVariable();
        // Solution only contains one entry
        Access1D<BigDecimal> mismatched = new Array1D<>(new BigDecimal[] { BigDecimal.ZERO });
        boolean result = model.validate(mismatched, context, logger);
        assertFalse(result, "Validation should fail when solution size differs from variable count");
    }

    /**
     * Validation succeeds when all variables satisfy their bounds and all expressions are satisfied.
     */
    @Test
    void testValidateSucceedsWhenAllConstraintsMet() {
        // Variable 0 : bounds [0, 5]
        Variable v0 = model.addVariable().lower(BigDecimal.ZERO).upper(new BigDecimal("5"));
        // Variable 1 : bounds [4, 10]
        Variable v1 = model.addVariable().lower(new BigDecimal("4")).upper(new BigDecimal("10"));
        // Expression: v0 + v1 ≤ 10
        Expression expr = model.addExpression().upper(new BigDecimal("10"));
        expr.set(v0, BigDecimal.ONE);
        expr.set(v1, BigDecimal.ONE);
        // Feasible solution: v0 = 4, v1 = 6  (both within bounds, expression = 10)
        Access1D<BigDecimal> solution = new Array1D<>(new BigDecimal[] { new BigDecimal("4"), new BigDecimal("6") });
        assertTrue(model.validate(solution, context, logger), "Validation should succeed when all variable and expression constraints are satisfied");
    }

    /**
     * Validation fails when a variable violates its bounds.
     */
    @Test
    void testValidateFailsWhenVariableOutOfBounds() {
        // Variable with lower bound 0 (upper bound left unrestricted)
        model.addVariable().lower(BigDecimal.ZERO);
        // Solution violates the lower bound (value = -5)
        Access1D<BigDecimal> solution = new Array1D<>(new BigDecimal[] { new BigDecimal("-5") });
        boolean result = model.validate(solution, context, logger);
        assertFalse(result, "Validation must fail when a variable violates its bounds");
    }

    /**
     * Validation fails when an expression constraint is violated.
     */
    @Test
    void testValidateFailsWhenExpressionViolated() {
        // One unrestricted variable
        Variable v = model.addVariable();
        // Expression: 5 * v ≤ 10  (upper bound 10)
        Expression expr = model.addExpression().upper(BigDecimal.TEN);
        expr.set(v, new BigDecimal("5"));
        // Solution makes v = 3 → expression value = 15 > 10
        Access1D<BigDecimal> solution = new Array1D<>(new BigDecimal[] { new BigDecimal("3") });
        boolean result = model.validate(solution, context, logger);
        assertFalse(result, "Validation must fail when an expression constraint is violated");
    }

    @Test
    void testValidateSucceedsInRelaxedModeWhenVariableOutOfBounds() {
        // Integer variable with lower bound 0 and upper bound 5
        Variable v = model.addVariable().integer(// mark as integer
        true).lower(BigDecimal.ZERO).upper(new BigDecimal("5"));
        // Put the model into relaxed mode (integer constraints are ignored)
        model.relax(true);
        // Solution violates the lower bound (value = -3) – should be accepted because of relaxation
        Access1D<BigDecimal> solution = new Array1D<>(new BigDecimal[] { new BigDecimal("-3") });
        assertTrue(model.validate(solution, context, logger), "In relaxed mode, out‑of‑bounds integer variables must not cause validation to fail");
    }
}