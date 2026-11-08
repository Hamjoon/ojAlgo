package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.ojalgo.netio.BasicLogger;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple description of the {@link Optimisation.Options} used in a test.
     */
    private static class OptionsSpec {

        // null → leave unchanged / set to null
        final Boolean loggerDetailed;

        // may be null
        final BasicLogger loggerAppender;

        OptionsSpec(Boolean loggerDetailed, BasicLogger loggerAppender) {
            this.loggerDetailed = loggerDetailed;
            this.loggerAppender = loggerAppender;
        }
    }

    /**
     * Specification of a variable that will be injected into the model.
     */
    private static class VariableSpec {

        final String name;

        // null → throws RuntimeException
        final Boolean validateResult;

        // logger that must be passed to validate()
        final BasicLogger expectedLogger;

        VariableSpec(String name, Boolean validateResult, BasicLogger expectedLogger) {
            this.name = name;
            this.validateResult = validateResult;
            this.expectedLogger = expectedLogger;
        }
    }

    /**
     * Specification of an expression that will be injected into the model.
     */
    private static class ExpressionSpec {

        final String name;

        // null → throws RuntimeException
        final Boolean validateResult;

        // logger that must be passed to validate()
        final BasicLogger expectedLogger;

        ExpressionSpec(String name, Boolean validateResult, BasicLogger expectedLogger) {
            this.name = name;
            this.validateResult = validateResult;
            this.expectedLogger = expectedLogger;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model instance.
        model = new ExpressionsBasedModel();
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model will be garbage‑collected.
        model = null;
        model = null;
    }

    /**
     * Sets a private field (including final) via reflection.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Supplies all test scenarios described in the JSON document.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>a textual description (used as test name)</li>
     *   <li>an {@link OptionsSpec} (may be {@code null})</li>
     *   <li>a list of {@link VariableSpec}</li>
     *   <li>a list of {@link ExpressionSpec}</li>
     *   <li>the expected outcome – either {@link Boolean} or {@link Class} of an exception</li>
     * </ul>
     */
    private static Stream<Arguments> provideValidateScenarios() {
        BasicLogger customLogger = mock(BasicLogger.class);
        // used for the “null appender” case
        BasicLogger nullLogger = null;
        return Stream.of(// -----------------------------------------------------------------
        // Empty model returns true
        // -----------------------------------------------------------------
        Arguments.of("Empty model returns true", null, Collections.emptyList(), Collections.emptyList(), true), // -----------------------------------------------------------------
        // All variables validate true, no expressions
        // -----------------------------------------------------------------
        Arguments.of("All variables validate true, no expressions", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), new VariableSpec("v2", true, BasicLogger.NULL)), Collections.emptyList(), true), // -----------------------------------------------------------------
        // At least one variable validates false, no expressions
        // -----------------------------------------------------------------
        Arguments.of("At least one variable validates false, no expressions", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), new VariableSpec("v2", false, BasicLogger.NULL)), Collections.emptyList(), false), // -----------------------------------------------------------------
        // All expressions validate true, no variables
        // -----------------------------------------------------------------
        Arguments.of("All expressions validate true, no variables", new OptionsSpec(false, BasicLogger.NULL), Collections.emptyList(), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), new ExpressionSpec("e2", true, BasicLogger.NULL)), true), // -----------------------------------------------------------------
        // At least one expression validates false, no variables
        // -----------------------------------------------------------------
        Arguments.of("At least one expression validates false, no variables", new OptionsSpec(false, BasicLogger.NULL), Collections.emptyList(), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), new ExpressionSpec("e2", false, BasicLogger.NULL)), false), // -----------------------------------------------------------------
        // Mixed variables and expressions – all true
        // -----------------------------------------------------------------
        Arguments.of("Mixed variables and expressions – all true", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), new VariableSpec("v2", true, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), new ExpressionSpec("e2", true, BasicLogger.NULL)), true), // -----------------------------------------------------------------
        // Mixed variables and expressions – variable false
        // -----------------------------------------------------------------
        Arguments.of("Mixed variables and expressions – variable false", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", false, BasicLogger.NULL), new VariableSpec("v2", true, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL)), false), // -----------------------------------------------------------------
        // Mixed variables and expressions – expression false
        // -----------------------------------------------------------------
        Arguments.of("Mixed variables and expressions – expression false", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", false, BasicLogger.NULL), new ExpressionSpec("e2", true, BasicLogger.NULL)), false), // -----------------------------------------------------------------
        // Logger selection – detailed logging enabled
        // -----------------------------------------------------------------
        Arguments.of("Logger selection – detailed logging enabled", new OptionsSpec(true, customLogger), Arrays.asList(new VariableSpec("v1", true, customLogger)), Collections.emptyList(), true), // -----------------------------------------------------------------
        // Logger selection – detailed logging disabled
        // -----------------------------------------------------------------
        Arguments.of("Logger selection – detailed logging disabled", // appender should be ignored
        new OptionsSpec(false, customLogger), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL)), Collections.emptyList(), true), // -----------------------------------------------------------------
        // Mixed variables and expressions – false variable appears later
        // -----------------------------------------------------------------
        Arguments.of("Mixed variables and expressions – false variable appears later", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), new VariableSpec("v2", true, BasicLogger.NULL), new VariableSpec("v3", false, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), new ExpressionSpec("e2", true, BasicLogger.NULL)), false), // -----------------------------------------------------------------
        // Mixed variables and expressions – false expression appears later
        // -----------------------------------------------------------------
        Arguments.of("Mixed variables and expressions – false expression appears later", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), new VariableSpec("v2", true, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), new ExpressionSpec("e2", false, BasicLogger.NULL), new ExpressionSpec("e3", true, BasicLogger.NULL)), false), // -----------------------------------------------------------------
        // Logger selection – detailed logging enabled for both variable and expression
        // -----------------------------------------------------------------
        Arguments.of("Logger selection – detailed logging enabled for both variable and expression", new OptionsSpec(true, customLogger), Arrays.asList(new VariableSpec("v1", true, customLogger)), Arrays.asList(new ExpressionSpec("e1", true, customLogger)), true), // -----------------------------------------------------------------
        // Logger selection – detailed logging disabled for both variable and expression
        // -----------------------------------------------------------------
        Arguments.of("Logger selection – detailed logging disabled for both variable and expression", new OptionsSpec(false, customLogger), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL)), true), // -----------------------------------------------------------------
        // Large model – all validations succeed
        // -----------------------------------------------------------------
        Arguments.of("Large model – all validations succeed", new OptionsSpec(false, BasicLogger.NULL), // 10 variables all true
        Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), new VariableSpec("v2", true, BasicLogger.NULL), new VariableSpec("v3", true, BasicLogger.NULL), new VariableSpec("v4", true, BasicLogger.NULL), new VariableSpec("v5", true, BasicLogger.NULL), new VariableSpec("v6", true, BasicLogger.NULL), new VariableSpec("v7", true, BasicLogger.NULL), new VariableSpec("v8", true, BasicLogger.NULL), new VariableSpec("v9", true, BasicLogger.NULL), new VariableSpec("v10", true, BasicLogger.NULL)), // 10 expressions all true
        Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), new ExpressionSpec("e2", true, BasicLogger.NULL), new ExpressionSpec("e3", true, BasicLogger.NULL), new ExpressionSpec("e4", true, BasicLogger.NULL), new ExpressionSpec("e5", true, BasicLogger.NULL), new ExpressionSpec("e6", true, BasicLogger.NULL), new ExpressionSpec("e7", true, BasicLogger.NULL), new ExpressionSpec("e8", true, BasicLogger.NULL), new ExpressionSpec("e9", true, BasicLogger.NULL), new ExpressionSpec("e10", true, BasicLogger.NULL)), true), // -----------------------------------------------------------------
        // Logger selection – detailed logging enabled with null appender
        // -----------------------------------------------------------------
        Arguments.of("Logger selection – detailed logging enabled with null appender", new OptionsSpec(true, nullLogger), Arrays.asList(new VariableSpec("v1", true, nullLogger)), Arrays.asList(new ExpressionSpec("e1", true, nullLogger)), true), // -----------------------------------------------------------------
        // Null options causes NullPointerException
        // -----------------------------------------------------------------
        Arguments.of("Null options causes NullPointerException", // will be overridden to null
        new OptionsSpec(null, null), Collections.emptyList(), Collections.emptyList(), NullPointerException.class), // -----------------------------------------------------------------
        // Variable.validate throws RuntimeException
        // -----------------------------------------------------------------
        Arguments.of("Variable.validate throws RuntimeException", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), // null → throws
        new VariableSpec("v2", null, BasicLogger.NULL)), Collections.emptyList(), RuntimeException.class), // -----------------------------------------------------------------
        // Expression.validate throws RuntimeException
        // -----------------------------------------------------------------
        Arguments.of("Expression.validate throws RuntimeException", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL)), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), // null → throws
        new ExpressionSpec("e2", null, BasicLogger.NULL)), RuntimeException.class), // -----------------------------------------------------------------
        // Null variable in model causes NullPointerException
        // -----------------------------------------------------------------
        Arguments.of("Null variable in model causes NullPointerException", new OptionsSpec(false, BasicLogger.NULL), Arrays.asList(new VariableSpec("v1", true, BasicLogger.NULL), // will be turned into a null entry
        null), Collections.emptyList(), NullPointerException.class), // -----------------------------------------------------------------
        // Null expression in model causes NullPointerException
        // -----------------------------------------------------------------
        Arguments.of("Null expression in model causes NullPointerException", new OptionsSpec(false, BasicLogger.NULL), Collections.emptyList(), Arrays.asList(new ExpressionSpec("e1", true, BasicLogger.NULL), // null entry
        null), NullPointerException.class));
    }

    /**
     * A model where every variable and expression is well‑formed – validation must succeed.
     */
    @Test
    void testValidateAllValid() {
        // Create two variables with consistent bounds.
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        Variable y = model.newVariable("y").lower(0).upper(5).weight(2);
        // Simple expression: 1·x + 1·y = 8 (both lower and upper are 8 → a valid equality constraint).
        Expression expr = model.newExpression("sum").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).level(BigDecimal.valueOf(8));
        // The model should be valid.
        assertTrue(model.validate(), "Model with correct variable/expression bounds should validate");
    }

    /**
     * A model containing a variable whose lower bound exceeds its upper bound – validation must fail.
     */
    @Test
    void testValidateVariableInvalid() {
        // Invalid variable: lower > upper.
        Variable bad = model.newVariable("bad").lower(10).upper(5).weight(1);
        // Add a trivial, valid expression so that the failure originates from the variable.
        Expression expr = model.newExpression("trivial").set(bad, BigDecimal.ONE).level(// any level is fine
        BigDecimal.ZERO);
        assertFalse(model.validate(), "Model with an invalid variable should not validate");
    }

    /**
     * A model containing an expression whose lower bound exceeds its upper bound – validation must fail.
     */
    @Test
    void testValidateExpressionInvalid() {
        // Valid variable.
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // Invalid expression: lower > upper.
        Expression badExpr = model.newExpression("badExpr").set(x, BigDecimal.ONE).lower(BigDecimal.valueOf(5)).upper(// lower > upper → invalid
        BigDecimal.valueOf(3));
        assertFalse(model.validate(), "Model with an invalid expression should not validate");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideValidateScenarios")
    void testValidate(String description, OptionsSpec optionsSpec, // Object to allow null entries
    List<Object> variableSpecs, // Object to allow null entries
    List<Object> expressionSpecs, Object expected) {
        // -----------------------------------------------------------------
        // 1️⃣  Prepare Options (may be set to null)
        // -----------------------------------------------------------------
        if (optionsSpec != null && optionsSpec.loggerDetailed != null) {
            // Ensure the model has a non‑null Options instance
            Optimisation.Options opts = new Optimisation.Options();
            setField(model, "options", opts);
            // Set the fields via reflection
            setField(opts, "logger_detailed", optionsSpec.loggerDetailed);
            setField(opts, "logger_appender", optionsSpec.loggerAppender);
        } else if (optionsSpec != null && optionsSpec.loggerDetailed == null) {
            // Explicitly set options to null to trigger NPE test case
            setField(model, "options", null);
        }
        // -----------------------------------------------------------------
        // 2️⃣  Build variable list (including possible null entries)
        // -----------------------------------------------------------------
        List<Variable> variableList = new ArrayList<>();
        for (Object specObj : variableSpecs) {
            if (specObj == null) {
                variableList.add(null);
                continue;
            }
            VariableSpec spec = (VariableSpec) specObj;
            Variable mockVar = mock(Variable.class);
            // Stubbing validate(appender)
            if (spec.validateResult == null) {
                when(mockVar.validate(any())).thenThrow(new RuntimeException("forced"));
            } else {
                when(mockVar.validate(spec.expectedLogger)).thenReturn(spec.validateResult);
            }
            variableList.add(mockVar);
        }
        setField(model, "myVariables", variableList);
        // -----------------------------------------------------------------
        // 3️⃣  Build expression map (including possible null entries)
        // -----------------------------------------------------------------
        Map<String, Expression> expressionMap = new HashMap<>();
        for (Object specObj : expressionSpecs) {
            if (specObj == null) {
                // Insert a null value under a dummy key
                expressionMap.put("nullEntry", null);
                continue;
            }
            ExpressionSpec spec = (ExpressionSpec) specObj;
            Expression mockExpr = mock(Expression.class);
            if (spec.validateResult == null) {
                when(mockExpr.validate(any())).thenThrow(new RuntimeException("forced"));
            } else {
                when(mockExpr.validate(spec.expectedLogger)).thenReturn(spec.validateResult);
            }
            expressionMap.put(spec.name, mockExpr);
        }
        setField(model, "myExpressions", expressionMap);
        // -----------------------------------------------------------------
        // 4️⃣  Execute and verify outcome
        // -----------------------------------------------------------------
        if (expected instanceof Boolean) {
            boolean result = model.validate();
            assertEquals(expected, result, "validate() returned unexpected boolean");
            // Verify that each non‑null mock received the expected logger
            for (Variable var : variableList) {
                if (var != null) {
                    verify(var).validate(any());
                }
            }
            for (Expression expr : expressionMap.values()) {
                if (expr != null) {
                    verify(expr).validate(any());
                }
            }
        } else if (expected instanceof Class) {
            Class<? extends Throwable> excClass = (Class<? extends Throwable>) expected;
            assertThrows(excClass, () -> model.validate(), "Expected exception " + excClass.getSimpleName() + " was not thrown");
        } else {
            fail("Invalid expected value in test data");
        }
    }
}
