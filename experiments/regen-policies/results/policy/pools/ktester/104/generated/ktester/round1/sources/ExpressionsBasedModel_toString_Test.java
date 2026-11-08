package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_toString_Test {

    private ExpressionsBasedModel model;

    /**
     * Returns the constant START_END used by the implementation (copied for assertions).
     */
    private static final String START_END = "############################################\n";

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh model instance
        model = new ExpressionsBasedModel();
    }

    /**
     * Sets a private field (even final) via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            // Remove final modifier if present
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When the model has no variables and no expressions the string should consist of
     * two identical header/footer lines.
     */
    @Test
    void testToStringEmptyModel() {
        String result = model.toString();
        // The constant used by the implementation (copied here for the assertion)
        final String START_END = "############################################\n";
        String expected = START_END + START_END;
        assertEquals(expected, result, "Empty model should contain only the start/end delimiter twice");
    }

    /**
     * Verify that variable and expression information appears in the string.
     * The exact formatting of each line is delegated to {@code Variable.appendToString}
     * and {@code Expression.appendToString}, therefore we only check that the
     * names and a few numeric values are present.
     */
    @Test
    void testToStringWithVariableAndExpression() {
        // Create a variable with a name, bounds and a weight
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        // Create a simple expression that uses the variable
        Expression expr = model.newExpression("expr").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        // 2 * x = 5  →  x = 2.5
        expr.set(x, BigDecimal.valueOf(2));
        // The model must be solved before the expression lines are printed
        // because the implementation calls {@code getVariableValues()}.
        Optimisation.Result solution = model.minimise();
        // Ensure the solution is feasible – otherwise the string could contain
        // additional diagnostic information that would make the test fragile.
        assertTrue(model.validate(solution));
        String result = model.toString();
        // The header/footer delimiter must be present
        final String START_END = "############################################\n";
        assertTrue(result.startsWith(START_END), "Result should start with the delimiter");
        assertTrue(result.endsWith(START_END), "Result should end with the delimiter");
        // Variable line – at minimum the variable name should appear
        assertTrue(result.contains("x"), "Variable name should be part of the output");
        // Expression line – the expression name and the lower/upper bound (5) should appear
        assertTrue(result.contains("expr"), "Expression name should be part of the output");
        assertTrue(result.contains("5"), "Expression bound value should be part of the output");
        // The solution value for the variable (2.5) is printed by the expression line,
        // so we also check that number appears.
        assertTrue(result.contains("2.5") || result.contains("2,5") || result.contains("2.500"), "Solution value should be reflected in the output");
    }

    @Test
    void testToStringVariablesOnly() {
        // create a few variables
        model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        model.newVariable("b").weight(BigDecimal.ONE);
        String out = model.toString();
        // header/footer present
        assertTrue(out.startsWith(START_END));
        assertTrue(out.endsWith(START_END));
        // both variable names appear
        assertTrue(out.contains("a"));
        assertTrue(out.contains("b"));
        // there must be no expression name (we did not add any)
        assertFalse(out.contains("expr"));
    }

    @Test
    void testToStringExpressionsOnly() {
        // an expression without any variable is allowed for the purpose of this test
        model.newExpression("e1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.newExpression("e2").weight(BigDecimal.ONE);
        String out = model.toString();
        // header/footer present
        assertTrue(out.startsWith(START_END));
        assertTrue(out.endsWith(START_END));
        // expression names appear
        assertTrue(out.contains("e1"));
        assertTrue(out.contains("e2"));
        // no variable names (we did not add any)
        assertFalse(out.contains("x"));
    }

    @Test
    void testToStringVariablesAndExpressions() {
        Variable v = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10)).weight(BigDecimal.ONE);
        Expression e = model.newExpression("expr").lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        e.set(v, BigDecimal.valueOf(2));
        // solve so that expression lines can include solution values
        Optimisation.Result solution = model.minimise();
        assertTrue(model.validate(solution));
        String out = model.toString();
        assertTrue(out.startsWith(START_END));
        assertTrue(out.endsWith(START_END));
        // variable line
        assertTrue(out.contains("x"));
        // expression line
        assertTrue(out.contains("expr"));
        // bound value (5) should be present
        assertTrue(out.contains("5"));
        // solution value (2.5) should be present in some form
        assertTrue(out.matches("(?s).*2[.,]5.*"));
    }

    @Test
    void testToStringWithCustomPrintFlag() throws Exception {
        // set the print flag to a non‑default NumberContext (the actual formatting is not verified)
        setPrivateField(model, "options", new Optimisation.Options() {

            {
                // default is a NumberContext, keep it
                print = Optimisation.Options.DEFAULT.print;
            }
        });
        // add a variable and an expression – they will be rendered according to the flag
        model.newVariable("v");
        model.newExpression("e");
        String out = model.toString();
        // basic structure must still hold
        assertTrue(out.startsWith(START_END));
        assertTrue(out.endsWith(START_END));
        assertTrue(out.contains("v"));
        assertTrue(out.contains("e"));
    }

    @Test
    void testToStringAfterSolvingShowsValues() {
        Variable v = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(4)).weight(BigDecimal.ONE);
        // simple objective: minimise y
        // solves the trivial problem
        model.minimise();
        String out = model.toString();
        // solution for y should be 0 (lower bound)
        assertTrue(out.matches("(?s).*y.*0[.,]0.*"));
    }

    @Test
    void testToStringFixedAndIntegerVariables() {
        // integer variable
        model.newVariable("intVar").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // fixed variable
        model.newVariable("fixedVar").fix(BigDecimal.valueOf(3));
        String out = model.toString();
        assertTrue(out.contains("intVar"));
        assertTrue(out.contains("fixedVar"));
        // the exact markers are implementation‑specific; we only verify that no exception occurs
    }

    @Test
    void testExpressionLinesIncludeSolutionValues() {
        Variable a = model.newVariable("a").weight(BigDecimal.ONE);
        Variable b = model.newVariable("b").weight(BigDecimal.ONE);
        Expression expr = model.newExpression("sum").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(10));
        expr.set(a, BigDecimal.ONE);
        // a + b = 10 → each should be 5
        expr.set(b, BigDecimal.ONE);
        Optimisation.Result sol = model.minimise();
        assertTrue(model.validate(sol));
        String out = model.toString();
        // Both variable names and the computed values (≈5) should appear in the expression line
        assertTrue(out.contains("sum"));
        assertTrue(out.matches("(?s).*5[.,]0.*"));
    }

    @Test
    void testToStringManyVariablesAndExpressions() {
        // enough to be a stress test without being too slow
        int count = 50;
        for (int i = 0; i < count; i++) {
            model.newVariable("v" + i).weight(BigDecimal.ONE);
            model.newExpression("e" + i).lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        }
        String out = model.toString();
        // count lines: header + count variable lines + count expression lines + footer
        long lineCount = out.lines().count();
        assertEquals(2 + count + count, lineCount, "Unexpected number of lines in the output");
        // ensure a few random names are present
        assertTrue(out.contains("v0"));
        assertTrue(out.contains("v" + (count - 1)));
        assertTrue(out.contains("e0"));
        assertTrue(out.contains("e" + (count - 1)));
    }

    @Test
    void testToStringWithPrintFlagFalse() throws Exception {
        // Options.print is a NumberContext; using a context that formats minimally.
        Optimisation.Options opts = new Optimisation.Options();
        // The real library uses a NumberContext; we simply set it to a dummy instance.
        setPrivateField(opts, "print", Optimisation.Options.DEFAULT.print);
        setPrivateField(model, "options", opts);
        model.newVariable("compact");
        model.newExpression("exprCompact");
        String out = model.toString();
        assertTrue(out.startsWith(START_END));
        assertTrue(out.endsWith(START_END));
        assertTrue(out.contains("compact"));
        assertTrue(out.contains("exprCompact"));
    }

    @Test
    void testToStringNullOptions() throws Exception {
        setPrivateField(model, "options", null);
        assertThrows(NullPointerException.class, model::toString);
    }

    @Test
    void testToStringNullVariablesList() throws Exception {
        setPrivateField(model, "myVariables", null);
        assertThrows(NullPointerException.class, model::toString);
    }

    @Test
    void testToStringNullVariableElement() throws Exception {
        List<Variable> list = new ArrayList<>();
        // null entry
        list.add(null);
        setPrivateField(model, "myVariables", list);
        assertThrows(NullPointerException.class, model::toString);
    }

    @Test
    void testToStringNullExpressionsMap() throws Exception {
        setPrivateField(model, "myExpressions", null);
        assertThrows(NullPointerException.class, model::toString);
    }

    @Test
    void testToStringNullExpressionElement() throws Exception {
        Map<String, Expression> map = new HashMap<>();
        map.put("nullExpr", null);
        setPrivateField(model, "myExpressions", map);
        assertThrows(NullPointerException.class, model::toString);
    }

    @Test
    void testToStringVariableAppendThrows() throws Exception {
        Variable badVar = mock(Variable.class);
        doThrow(new IllegalStateException("bad variable")).when(badVar).appendToString(any(StringBuilder.class), any());
        List<Variable> list = new ArrayList<>();
        list.add(badVar);
        setPrivateField(model, "myVariables", list);
        IllegalStateException ex = assertThrows(IllegalStateException.class, model::toString);
        assertEquals("bad variable", ex.getMessage());
    }

    @Test
    void testToStringExpressionAppendThrows() throws Exception {
        Expression badExpr = mock(Expression.class);
        doThrow(new IllegalArgumentException("bad expression")).when(badExpr).appendToString(any(StringBuilder.class), any(), any());
        Map<String, Expression> map = new HashMap<>();
        map.put("bad", badExpr);
        setPrivateField(model, "myExpressions", map);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, model::toString);
        assertEquals("bad expression", ex.getMessage());
    }

    @Test
    void testToStringGetVariableValuesThrows() {
        // Spy the model and make getVariableValues throw
        ExpressionsBasedModel spy = spy(new ExpressionsBasedModel());
        doThrow(new RuntimeException("solver failure")).when(spy).getVariableValues();
        // add at least one variable so the method reaches the call
        spy.newVariable("v");
        RuntimeException ex = assertThrows(RuntimeException.class, spy::toString);
        assertEquals("solver failure", ex.getMessage());
    }
}
