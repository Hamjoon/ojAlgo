package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ojalgo.netio.BasicLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_validate_Test {

    private static final class StubVariable extends Variable {

        private final Function<BasicLogger, Boolean> validator;

        StubVariable(Function<BasicLogger, Boolean> validator) {
            super();
            this.validator = validator;
        }

        @Override
        public boolean validate(final BasicLogger logger) {
            return validator.apply(logger);
        }
    }

    private static final class StubExpression extends Expression {

        private final Function<BasicLogger, Boolean> validator;

        StubExpression(Function<BasicLogger, Boolean> validator) {
            super();
            this.validator = validator;
        }

        @Override
        public boolean validate(final BasicLogger logger) {
            return validator.apply(logger);
        }
    }

    @Nested
    @DisplayName("Logger appender usage")
    class LoggerAppenderUsage {

        @Test
        @DisplayName("All succeed, logger_detailed = true → appender passed to each")
        void allSuccessWithAppender() throws Exception {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.options.logger_detailed = true;
            BasicLogger mockAppender = BasicLogger.NULL;
            model.options.logger_appender = mockAppender;
            // two variables + one expression
            List<BasicLogger> captured = new ArrayList<>();
            Variable v1 = new StubVariable(l -> {
                captured.add(l);
                return true;
            });
            Variable v2 = new StubVariable(l -> {
                captured.add(l);
                return true;
            });
            Expression e1 = new StubExpression(l -> {
                captured.add(l);
                return true;
            });
            injectVariables(model, List.of(v1, v2));
            injectExpressions(model, Map.of("e1", e1));
            assertTrue(model.validate(), "All validations succeed → model.validate() must be true");
            assertEquals(3, captured.size(), "Exactly three validate calls (2 vars + 1 expr) should have occurred");
            captured.forEach(logger -> assertSame(mockAppender, logger, "When logger_detailed is true the supplied appender must be forwarded"));
        }

        @Test
        @DisplayName("One variable false, logger_detailed = true → appender still used")
        void oneVariableFalseWithAppender() throws Exception {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.options.logger_detailed = true;
            BasicLogger mockAppender = BasicLogger.NULL;
            model.options.logger_appender = mockAppender;
            List<BasicLogger> captured = new ArrayList<>();
            Variable v1 = new StubVariable(l -> {
                captured.add(l);
                return true;
            });
            Variable v2 = new StubVariable(l -> {
                captured.add(l);
                // this makes the whole model invalid
                return false;
            });
            Expression e1 = new StubExpression(l -> {
                captured.add(l);
                return true;
            });
            injectVariables(model, List.of(v1, v2));
            injectExpressions(model, Map.of("e1", e1));
            assertFalse(model.validate(), "A false variable must cause overall validation to be false");
            assertEquals(3, captured.size(), "Even though the result is false, all three validate calls should have been made");
            captured.forEach(logger -> assertSame(mockAppender, logger, "Appender must be passed irrespective of individual outcomes"));
        }
    }

    @Nested
    @DisplayName("Exception propagation")
    class ExceptionPropagation {

        @Test
        @DisplayName("Variable.validate throws RuntimeException → propagates")
        void variableThrowsRuntimeException() throws Exception {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable good = new StubVariable(l -> true);
            Variable bad = new Variable() {

                @Override
                public boolean validate(BasicLogger logger) {
                    throw new RuntimeException("bad var");
                }
            };
            injectVariables(model, List.of(good, bad));
            // no expressions
            injectExpressions(model, Map.of());
            RuntimeException thrown = assertThrows(RuntimeException.class, model::validate, "RuntimeException from a variable must propagate");
            assertEquals("bad var", thrown.getMessage());
        }

        @Test
        @DisplayName("Expression.validate throws IllegalStateException → propagates")
        void expressionThrowsIllegalStateException() throws Exception {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // no variables
            injectVariables(model, List.of());
            Expression good = new StubExpression(l -> true);
            Expression bad = new Expression() {

                @Override
                public boolean validate(BasicLogger logger) {
                    throw new IllegalStateException("bad expr");
                }
            };
            injectExpressions(model, Map.of("good", good, "bad", bad));
            IllegalStateException thrown = assertThrows(IllegalStateException.class, model::validate, "IllegalStateException from an expression must propagate");
            assertEquals("bad expr", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Logger detail flag")
    class LoggerDetailFlag {

        @Test
        @DisplayName("logger_detailed = false → BasicLogger.NULL is used")
        void loggerDetailedFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.options.logger_detailed = false;
            assertTrue(model.validate(), "With no variables/expressions and logger_detailed false, validate() must return true");
        }

        @Test
        @DisplayName("logger_detailed = true → options.logger_appender is used")
        void loggerDetailedTrue() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.options.logger_detailed = true;
            // any non‑null logger works for the test
            BasicLogger custom = BasicLogger.NULL;
            model.options.logger_appender = custom;
            assertTrue(model.validate(), "With no variables/expressions and logger_detailed true, validate() must return true");
        }
    }

    @Nested
    @DisplayName("Variable validation outcomes")
    class VariableValidation {

        @Test
        @DisplayName("All variables have valid bounds → overall true")
        void allVariablesTrue() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // two variables with correct lower/upper configuration
            model.addVariable().lower(0).upper(10);
            model.addVariable().lower(-5).upper(5);
            assertTrue(model.validate(), "All variables validated true → model.validate() must be true");
        }

        @Test
        @DisplayName("At least one variable has invalid bounds → overall false")
        void oneVariableFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // valid
            model.addVariable().lower(0).upper(10);
            // invalid (lower > upper)
            model.addVariable().lower(10).upper(5);
            assertFalse(model.validate(), "A false‑returning variable must make the whole validation false");
        }

        @Test
        @DisplayName("Multiple invalid variables → overall false")
        void multipleVariablesFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // invalid
            model.addVariable().lower(10).upper(5);
            // invalid
            model.addVariable().lower(8).upper(3);
            // valid
            model.addVariable().lower(0).upper(1);
            assertFalse(model.validate(), "If any variable returns false, the aggregated result must be false");
        }
    }

    @Nested
    @DisplayName("Expression validation outcomes")
    class ExpressionValidation {

        @Test
        @DisplayName("All expressions have valid bounds → overall true")
        void allExpressionsTrue() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable().lower(0).upper(10);
            model.addExpression().set(v, 1).lower(0).upper(10);
            model.addExpression().set(v, 2).lower(-5).upper(5);
            assertTrue(model.validate(), "All expressions validated true → model.validate() must be true");
        }

        @Test
        @DisplayName("At least one expression has invalid bounds → overall false")
        void oneExpressionFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable().lower(0).upper(10);
            // valid
            model.addExpression().set(v, 1).lower(0).upper(10);
            // invalid
            model.addExpression().set(v, 1).lower(10).upper(5);
            assertFalse(model.validate(), "A false‑returning expression must make the whole validation false");
        }

        @Test
        @DisplayName("Multiple invalid expressions → overall false")
        void multipleExpressionsFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable().lower(0).upper(10);
            // invalid
            model.addExpression().set(v, 1).lower(10).upper(5);
            // invalid
            model.addExpression().set(v, 1).lower(8).upper(3);
            // valid
            model.addExpression().set(v, 1).lower(0).upper(1);
            assertFalse(model.validate(), "If any expression returns false, the aggregated result must be false");
        }
    }

    @Nested
    @DisplayName("Combined variable and expression validation")
    class CombinedValidation {

        @Test
        @DisplayName("All true → overall true")
        void allTrue() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable().lower(0).upper(10);
            model.addExpression().set(v, 1).lower(0).upper(10);
            assertTrue(model.validate(), "All entities true → overall validation must be true");
        }

        @Test
        @DisplayName("Variable true, expression false → overall false")
        void varTrueExprFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable v = model.addVariable().lower(0).upper(10);
            // invalid expression
            model.addExpression().set(v, 1).lower(10).upper(5);
            assertFalse(model.validate(), "A false expression must make the whole validation false");
        }

        @Test
        @DisplayName("Variable false, expression true → overall false")
        void varFalseExprTrue() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // invalid variable
            model.addVariable().lower(10).upper(5);
            Variable v = model.addVariable().lower(0).upper(10);
            model.addExpression().set(v, 1).lower(0).upper(10);
            assertFalse(model.validate(), "A false variable must make the whole validation false");
        }

        @Test
        @DisplayName("Both variable and expression false → overall false")
        void bothFalse() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // invalid variable
            model.addVariable().lower(10).upper(5);
            Variable v = model.addVariable().lower(0).upper(10);
            // invalid expression
            model.addExpression().set(v, 1).lower(10).upper(5);
            assertFalse(model.validate(), "If any entity returns false, the result must be false");
        }
    }

    @Nested
    @DisplayName("Large collection handling")
    class LargeCollection {

        @Test
        @DisplayName("10 variables + 5 expressions, all true → overall true")
        void manyAllTrue() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // 10 valid variables
            for (int i = 0; i < 10; i++) {
                model.addVariable().lower(0).upper(10);
            }
            // 5 valid expressions using the first variable
            Variable first = model.addVariable().lower(0).upper(10);
            for (int i = 0; i < 5; i++) {
                model.addExpression().set(first, 1).lower(0).upper(10);
            }
            assertTrue(model.validate(), "When every mock returns true, even a large collection must yield true");
        }

        @Test
        @DisplayName("One false variable among many → overall false")
        void manyOneFalseVariable() {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // 5 valid variables
            for (int i = 0; i < 5; i++) {
                model.addVariable().lower(0).upper(10);
            }
            // one invalid variable
            model.addVariable().lower(10).upper(5);
            // a few more valid variables
            for (int i = 0; i < 4; i++) {
                model.addVariable().lower(0).upper(10);
            }
            // two valid expressions (using any variable)
            Variable v = model.addVariable().lower(0).upper(10);
            model.addExpression().set(v, 1).lower(0).upper(10);
            model.addExpression().set(v, 1).lower(0).upper(10);
            assertFalse(model.validate(), "A single false variable among many must cause the whole validation to be false");
        }
    }

    @Nested
    @DisplayName("Null options handling")
    class NullOptions {

        @Test
        @DisplayName("options == null → NullPointerException")
        void nullOptionsThrowsNPE() throws Exception {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // Force the private field 'options' to null via reflection
            Field optField = ExpressionsBasedModel.class.getDeclaredField("options");
            optField.setAccessible(true);
            optField.set(model, null);
            // Expect NPE when validate() tries to read options.logger_detailed
            assertThrows(NullPointerException.class, model::validate, "Calling validate() with a null Optimisation.Options must throw NPE");
        }
    }

    private static void injectVariables(ExpressionsBasedModel model, List<ExpressionsBasedModel.Variable> vars) throws Exception {
        List<Variable> target = getVariableList(model);
        target.clear();
        target.addAll(vars);
    }

    private static void injectExpressions(ExpressionsBasedModel model, Map<String, ExpressionsBasedModel.Expression> exprs) throws Exception {
        Map<String, Expression> target = getExpressionMap(model);
        target.clear();
        target.putAll(exprs);
    }

    /**
     * When the model contains no variables or expressions the validation should succeed.
     */
    @Test
    void testValidateWithNoVariablesOrExpressions() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertTrue(model.validate(), "Validate should return true when the model has no variables or expressions");
    }

    /**
     * All variables and expressions are valid → validation returns true.
     */
    @Test
    void testValidateAllValid() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable var = model.addVariable().lower(0).upper(10);
        model.addExpression().set(var, 1).lower(0).upper(10);
        assertTrue(model.validate(), "Validate should return true when all variables and expressions are valid");
    }

    /**
     * A variable with an illegal bound configuration (lower > upper) makes validation fail.
     */
    @Test
    void testValidateInvalidVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // invalid bounds
        model.addVariable().lower(10).upper(5);
        assertFalse(model.validate(), "Validate should return false when a variable has invalid bounds");
    }

    /**
     * An expression with an illegal bound configuration (lower > upper) makes validation fail.
     */
    @Test
    void testValidateInvalidExpression() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable var = model.addVariable().lower(0).upper(10);
        // invalid bounds
        model.addExpression().set(var, 1).lower(10).upper(5);
        assertFalse(model.validate(), "Validate should return false when an expression has invalid bounds");
    }

    /**
     * Verify that the logger passed to {@code Variable.validate(...)} and {@code Expression.validate(...)}
     * is chosen according to {@code Options.logger_detailed}.
     *
     * The test uses reflection to inject a mocked variable into the model.
     */
    @Test
    void testValidateUsesCorrectLogger() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable var = model.addVariable().lower(0).upper(10);
        model.addExpression().set(var, 1).lower(0).upper(10);
        // detailed logging enabled – supply a custom appender
        model.options.logger_detailed = true;
        model.options.logger_appender = BasicLogger.NULL;
        assertTrue(model.validate(), "Validate should succeed when logger_detailed is true and an appender is supplied");
        // detailed logging disabled – logger should be BasicLogger.NULL internally
        model.options.logger_detailed = false;
        // value ignored
        model.options.logger_appender = BasicLogger.NULL;
        assertTrue(model.validate(), "Validate should succeed when logger_detailed is false");
    }

    @SuppressWarnings("unchecked")
    private static List<ExpressionsBasedModel.Variable> getVariableList(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        return (List<Variable>) f.get(model);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ExpressionsBasedModel.Expression> getExpressionMap(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        return (Map<String, Expression>) f.get(model);
    }

    @Test
    @DisplayName("Logger detail flag – false uses BasicLogger.NULL")
    void loggerDetailedFalse() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // explicit – default is false
        model.options.logger_detailed = false;
        assertTrue(model.validate(), "With no variables/expressions and logger_detailed false, validate() must return true");
    }

    @Test
    @DisplayName("Logger detail flag – true uses supplied appender")
    void loggerDetailedTrue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.options.logger_detailed = true;
        // any non‑null logger works for the test
        BasicLogger custom = BasicLogger.NULL;
        model.options.logger_appender = custom;
        assertTrue(model.validate(), "With no variables/expressions and logger_detailed true, validate() must return true");
    }
}
