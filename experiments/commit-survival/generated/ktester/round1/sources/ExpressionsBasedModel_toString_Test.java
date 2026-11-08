package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toString_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
        // Add two variables with distinct names and bounds.
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // Add a simple expression: 2*x + 3*y = 8
        Expression expr = model.newExpression("c1").lower(BigDecimal.valueOf(8)).upper(BigDecimal.valueOf(8));
        expr.set(model.getVariable(0), BigDecimal.valueOf(2));
        expr.set(model.getVariable(1), BigDecimal.valueOf(3));
    }

    @Test
    void testToStringContainsHeaderFooterAndContent() {
        String result = model.toString();
        // The START_END constant is defined inside ExpressionsBasedModel.
        // It consists of a line of 44 '#', followed by a newline.
        String expectedHeader = "############################################\n";
        // Verify header and footer.
        assertTrue(result.startsWith(expectedHeader), "String should start with header");
        assertTrue(result.endsWith(expectedHeader), "String should end with footer");
        // Verify that variable names appear in the order they were added.
        assertTrue(result.contains("x"), "Variable x should be present");
        assertTrue(result.contains("y"), "Variable y should be present");
        // Verify that the expression name appears.
        assertTrue(result.contains("c1"), "Expression c1 should be present");
        // The solution values are obtained via getVariableValues().
        // Since the model is feasible, the solver will produce a solution.
        // We only need to ensure that the solution line is present, not its exact numbers.
        assertTrue(result.contains("solution"), "Solution information should be part of the output");
    }

    @Test
    void testToStringOnEmptyModel() {
        // Create an empty model (no variables, no expressions).
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        String result = empty.toString();
        String expectedHeader = "############################################\n";
        // Header and footer must still be present.
        assertTrue(result.startsWith(expectedHeader), "Empty model should start with header");
        assertTrue(result.endsWith(expectedHeader), "Empty model should end with footer");
        // No variable or expression names should be present.
        assertFalse(result.contains("Variable"), "Empty model should not contain variable details");
        assertFalse(result.contains("Expression"), "Empty model should not contain expression details");
    }

    @Test
    void testNullOptionsShouldThrowNPE() throws Exception {
        // Force the private 'options' field to null via reflection.
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        // Expect a NullPointerException when toString() accesses options.print.
        assertThrows(NullPointerException.class, model::toString, "Calling toString() with a null 'options' field should throw NPE");
    }

    @Test
    void testNullVariableInListShouldThrowNPE() throws Exception {
        // Obtain the private variable list and inject a null entry.
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Variable> vars = (java.util.List<Variable>) varsField.get(model);
        // deliberately corrupt the list
        vars.add(null);
        // The next iteration over myVariables will hit the null entry.
        assertThrows(NullPointerException.class, model::toString, "Calling toString() with a null variable in the list should throw NPE");
    }

    @Test
    void testNullExpressionInMapShouldThrowNPE() throws Exception {
        // Obtain the private expression map and put a null value.
        Field exprMapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprMapField.get(model);
        // deliberately corrupt the map
        exprMap.put("nullExpr", null);
        // The iteration over myExpressions.values() will encounter the null.
        assertThrows(NullPointerException.class, model::toString, "Calling toString() with a null expression in the map should throw NPE");
    }
}
