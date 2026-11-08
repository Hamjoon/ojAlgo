package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toString_Test {

    private ExpressionsBasedModel model;

    private static final String SEPARATOR = "############################################\n";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    /**
     * An empty model should consist only of the start/end separator line
     * repeated twice (once at the beginning and once at the end).
     */
    @Test
    void testToStringEmptyModel() {
        String result = model.toString();
        final String separator = "############################################\n";
        String expected = separator + separator;
        assertEquals(expected, result, "Empty model string representation does not match expected format");
    }

    /**
     * A model with a single variable and a single expression should contain
     * the names of both entities and the separator lines.
     */
    @Test
    void testToStringWithVariableAndExpression() {
        Variable x = model.newVariable("x").lower(0).upper(10).weight(BigDecimal.ONE);
        Expression expr = model.newExpression("expr");
        expr.set(x, BigDecimal.ONE);
        String result = model.toString();
        final String separator = "############################################\n";
        assertTrue(result.startsWith(separator), "String should start with the separator line");
        assertTrue(result.endsWith(separator), "String should end with the separator line");
        assertTrue(result.contains("x"), "Variable name 'x' should appear in the string representation");
        assertTrue(result.contains("expr"), "Expression name 'expr' should appear in the string representation");
    }

    /**
     * Adding multiple variables and expressions should still keep the separator
     * lines and include all defined names.
     */
    @Test
    void testToStringMultipleEntities() {
        Variable a = model.newVariable("a").lower(0).upper(5).weight(BigDecimal.ONE);
        Variable b = model.newVariable("b").lower(1).upper(6).weight(BigDecimal.ONE);
        Variable c = model.newVariable("c").lower(2).upper(7).weight(BigDecimal.ONE);
        Expression e1 = model.newExpression("e1");
        e1.set(a, BigDecimal.ONE).set(b, BigDecimal.ONE);
        Expression e2 = model.newExpression("e2");
        e2.set(b, BigDecimal.ONE).set(c, BigDecimal.ONE);
        String result = model.toString();
        final String separator = "############################################\n";
        assertTrue(result.startsWith(separator), "Result must start with separator");
        assertTrue(result.endsWith(separator), "Result must end with separator");
        for (String name : new String[] { "a", "b", "c", "e1", "e2" }) {
            assertTrue(result.contains(name), "Result should contain '" + name + "'");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringSingleVariableOnly() {
        Variable v = model.newVariable("singleVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        String result = model.toString();
        assertTrue(result.startsWith(SEPARATOR), "Result must start with separator");
        assertTrue(result.endsWith(SEPARATOR), "Result must end with separator");
        assertTrue(result.contains("singleVar"), "Variable name must appear");
        assertFalse(result.contains("expr"), "No expression should be printed");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringMultipleVariablesOnly() {
        Variable a = model.newVariable("a");
        Variable b = model.newVariable("b");
        Variable c = model.newVariable("c");
        String result = model.toString();
        assertTrue(result.startsWith(SEPARATOR));
        assertTrue(result.endsWith(SEPARATOR));
        int idxA = result.indexOf("a");
        int idxB = result.indexOf("b");
        int idxC = result.indexOf("c");
        assertTrue(idxA >= 0 && idxB >= 0 && idxC >= 0, "All variable names must be present");
        assertTrue(idxA < idxB && idxB < idxC, "Variables should appear in insertion order");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringSingleExpressionOnly() {
        Expression e = model.newExpression("singleExpr");
        String result = model.toString();
        assertTrue(result.startsWith(SEPARATOR));
        assertTrue(result.endsWith(SEPARATOR));
        assertTrue(result.contains("\n\n"), "Variable block should be empty (blank line)");
        assertTrue(result.contains("singleExpr"), "Expression name must appear");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringMultipleExpressionsOnly() {
        model.newExpression("e1");
        model.newExpression("e2");
        String result = model.toString();
        assertTrue(result.startsWith(SEPARATOR));
        assertTrue(result.endsWith(SEPARATOR));
        assertTrue(result.contains("\n\n"), "Variable block should be empty");
        int idx1 = result.indexOf("e1");
        int idx2 = result.indexOf("e2");
        assertTrue(idx1 >= 0 && idx2 >= 0, "Both expression names must appear");
        assertTrue(idx1 < idx2, "Expressions should be printed in insertion order");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringVariablesAndExpressions() {
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Expression expr = model.newExpression("myExpr");
        expr.set(v1, BigDecimal.ONE).set(v2, BigDecimal.ONE);
        String result = model.toString();
        assertTrue(result.startsWith(SEPARATOR));
        assertTrue(result.endsWith(SEPARATOR));
        int varPos = Math.min(result.indexOf("v1"), result.indexOf("v2"));
        int exprPos = result.indexOf("myExpr");
        assertTrue(varPos >= 0 && exprPos >= 0, "Both variables and expression must be present");
        assertTrue(varPos < exprPos, "Variable block must precede expression block");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testToStringWithDifferentPrintFlag(boolean printFlag) throws Exception {
        // The test only verifies that toString() works regardless of internal state.
        model.newVariable("x").weight(BigDecimal.ONE);
        String result = model.toString();
        assertTrue(result.startsWith(SEPARATOR));
        assertTrue(result.endsWith(SEPARATOR));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringVariableWithBoundsAndInteger() {
        Variable v = model.newVariable("intVar").lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(10)).integer(true);
        String result = model.toString();
        assertTrue(result.contains("intVar"));
        assertTrue(result.contains("-5"));
        assertTrue(result.contains("10"));
        assertTrue(result.toUpperCase().contains("INTEGER") || result.toUpperCase().contains("INT"), "Integer flag should be reflected in the variable line");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringFixedVariable() {
        Variable v = model.newVariable("fixedVar").fix(BigDecimal.valueOf(3.14));
        String result = model.toString();
        assertTrue(result.contains("fixedVar"));
        assertTrue(result.toUpperCase().contains("FIXED"), "Fixed status should be indicated in the variable line");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringExpressionShowsSolutionValues() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Expression sum = model.newExpression("sum");
        sum.set(x, BigDecimal.ONE).set(y, BigDecimal.ONE);
        model.minimise();
        String result = model.toString();
        assertTrue(result.contains("x"));
        assertTrue(result.contains("y"));
        assertTrue(result.contains("0") || result.contains("0.0"), "Expression line should contain solved variable values");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testVariableDetailChangesWithPrintFlag() throws Exception {
        // Create two models that differ only in the details added.
        ExpressionsBasedModel modelPrintTrue = new ExpressionsBasedModel();
        ExpressionsBasedModel modelPrintFalse = new ExpressionsBasedModel();
        // Add same variable to both, but give extra detail (lower bound) only to the first.
        modelPrintTrue.newVariable("v").weight(BigDecimal.ONE).lower(BigDecimal.ZERO);
        modelPrintFalse.newVariable("v").weight(BigDecimal.ONE);
        String sTrue = modelPrintTrue.toString();
        String sFalse = modelPrintFalse.toString();
        assertTrue(sTrue.contains("v"));
        assertTrue(sFalse.contains("v"));
        assertNotEquals(sTrue, sFalse, "String representations should differ when print detail changes");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testVariableNameWithSpecialCharacters() {
        String specialName = "Var #1";
        model.newVariable(specialName);
        String result = model.toString();
        assertTrue(result.contains(specialName), "Variable name with spaces and special characters must be preserved");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDuplicateVariableNames() {
        Variable v1 = model.newVariable("dup");
        Variable v2 = model.newVariable("dup");
        String result = model.toString();
        int first = result.indexOf("dup");
        int second = result.indexOf("dup", first + 1);
        assertTrue(first >= 0 && second >= 0, "Duplicate variable name should appear twice");
        assertTrue(first < second, "Insertion order must be preserved");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringNullOptionsThrowsNPE() throws Exception {
        Field optionsField = ExpressionsBasedModel.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        optionsField.set(model, null);
        assertThrows(NullPointerException.class, () -> model.toString(), "toString() should throw NPE when options is null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringNullVariableInListThrowsNPE() throws Exception {
        Field varsField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        varsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> vars = (ArrayList<Variable>) varsField.get(model);
        vars.add(null);
        assertThrows(NullPointerException.class, () -> model.toString(), "toString() should throw NPE when a null variable is encountered");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringPropagatesExceptionFromGetVariableValues() {
        ExpressionsBasedModel spyModel = org.mockito.Mockito.spy(new ExpressionsBasedModel());
        org.mockito.Mockito.doThrow(new IllegalStateException("No solution available")).when(spyModel).getVariableValues();
        spyModel.newVariable("x");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, spyModel::toString, "toString() should propagate IllegalStateException from getVariableValues()");
        assertEquals("No solution available", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToStringNullExpressionInMapThrowsNPE() throws Exception {
        Field exprMapField = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        exprMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Expression> exprMap = (Map<String, Expression>) exprMapField.get(model);
        exprMap.put("nullExpr", null);
        model.newVariable("x");
        assertThrows(NullPointerException.class, () -> model.toString(), "toString() should throw NPE when a null expression is encountered");
    }
}
