package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import org.ojalgo.function.constant.BigMath;
import org.ojalgo.structure.Access1D;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionsBasedModel_setKnownSolution_2_Test {

    private ExpressionsBasedModel model;

    //     @Mock
    private BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handlerMock;

    //     @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh model for each test
        model = new ExpressionsBasedModel();
        handlerMock = (m, s) -> {
            // no‑op
        };
    }

    private static Stream<Arguments> nullArgumentProvider() {
        // a dummy optimal result for the non‑null side of the pair
        ExpressionsBasedModel dummyModel = new ExpressionsBasedModel();
        Variable v = dummyModel.newVariable("v").lower(BigMath.ZERO).upper(BigMath.ONE);
        dummyModel.newExpression("obj").weight(BigMath.ONE).set(v, BigMath.ONE);
        Optimisation.Result optimalResult = dummyModel.minimise();
        return Stream.of(// knownSolution == null
        Arguments.of(null, (BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>>) (m, s) -> {
        }, "knownSolution"), // handler == null
        Arguments.of(optimalResult, null, "handler"));
    }

    /**
     * Verify that {@code Objects.requireNonNull} guards both parameters.
     */
    @ParameterizedTest(name = "{index} => knownSolution={0}, handler={1}, expectedMessage={2}")
    @MethodSource("nullArgumentProvider")
    void testSetKnownSolutionNullArguments(Optimisation.Result knownSolution, BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> handler, String expectedMessage) {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> model.setKnownSolution(knownSolution, handler));
        assertTrue(npe.getMessage().contains(expectedMessage), () -> "Exception message should contain '" + expectedMessage + "'");
    }

    /**
     * Happy‑path: an optimal and valid solution can be stored together with the handler.
     */
    @Test
    void testSetKnownSolutionSuccess() {
        // Build a trivial model:  x = 5  (optimal solution is x = 5)
        Variable x = model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.TEN).weight(BigMath.ONE);
        model.newExpression("c0").level(BigMath.FIVE).set(x, BigMath.ONE);
        // should be OPTIMAL and valid
        Optimisation.Result optimalResult = model.minimise();
        model.setKnownSolution(optimalResult, handlerMock);
        assertSame(optimalResult, model.getKnownSolution(), "The known solution should be stored unchanged");
        assertSame(handlerMock, model.getValidationFailureHandler(), "The handler should be stored unchanged");
    }

    /**
     * Passing a {@code null} knownSolution must raise {@link NullPointerException}.
     */
    @Test
    void testSetKnownSolutionNullResult() {
        assertThrows(NullPointerException.class, () -> model.setKnownSolution(null, handlerMock));
    }

    /**
     * Passing a {@code null} handler must raise {@link NullPointerException}.
     */
//     @Test
//     void testSetKnownSolutionNullHandler() {
//         Variable x = model.newVariable().lower(BigMath.ZERO).upper(BigMath.ONE);
//         model.newExpression().level(BigMath.ZERO).set(x, BigMath.ONE);
//         Optimisation.Result result = model.minimise();
//         assertThrows(NullPointerException.class, () -> model.setKnownSolution(result, null));
//     }

    /**
     * If the supplied result is not optimal, a {@link ProgrammingError} must be thrown.
     */
//     @Test
//     void testSetKnownSolutionNonOptimal() {
//         // Infeasible model: x must be 2 but bounds are [0,1]
//         Variable x = model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.ONE);
//         model.newExpression("c0").level(BigMath.TWO).set(x, BigMath.ONE);
//         // state will be INFEASIBLE
//         Optimisation.Result infeasibleResult = model.minimise();
//         assertFalse(infeasibleResult.getState().isOptimal(), "Result should not be optimal for this test");
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.setKnownSolution(infeasibleResult, handlerMock));
//         assertTrue(ex.getMessage().contains("Must be an optimal solution!"), "Exception message should indicate the optimality requirement");
//     }

    /**
     * If the supplied result is optimal but does not satisfy the model
     * (validation fails), a {@link ProgrammingError} must be thrown.
     */
//     @Test
//     void testSetKnownSolutionInvalidSolution() {
//         // Simple model with a single variable bounded to [0,10] and objective weight = 1
//         Variable x = model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.TEN);
//         model.newExpression("obj").weight(BigMath.ONE).set(x, BigMath.ONE);
//         // Create a handcrafted Access1D that pretends the solution is x = 20 (outside the bound)
//         Access1D<BigDecimal> bogusSolution = new Access1D<BigDecimal>() {
// 
//             @Override
//             public long size() {
//                 return 1L;
//             }
// 
//             @Override
//             public BigDecimal get(long index) {
//                 if (index == 0L) {
//                     return BigDecimal.valueOf(20);
//                 }
//                 throw new IndexOutOfBoundsException();
//             }
//         };
//         Optimisation.Result bogusResult = new Optimisation.Result(Optimisation.State.OPTIMAL, Double.NaN, bogusSolution);
//         // Verify that the model indeed considers this solution invalid
//         assertFalse(model.validate(bogusResult), "The fabricated solution should fail model validation");
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.setKnownSolution(bogusResult, handlerMock));
//         assertTrue(ex.getMessage().contains("Solution not valid!"), "Exception message should indicate validation failure");
//     }

    /**
     * Ensure that a no‑op handler is accepted and stored correctly.
     */
    @Test
    void testSetKnownSolutionWithNoOpHandler() {
        Variable x = model.newVariable("x").lower(BigMath.ZERO).upper(BigMath.ONE).weight(BigMath.ONE);
        model.newExpression("obj").weight(BigMath.ONE).set(x, BigMath.ONE);
        Optimisation.Result optimal = model.minimise();
        BiConsumer<ExpressionsBasedModel, Access1D<BigDecimal>> noOp = (m, s) -> {
            // no‑op
        };
        model.setKnownSolution(optimal, noOp);
        assertSame(optimal, model.getKnownSolution());
        assertSame(noOp, model.getValidationFailureHandler());
    }
}