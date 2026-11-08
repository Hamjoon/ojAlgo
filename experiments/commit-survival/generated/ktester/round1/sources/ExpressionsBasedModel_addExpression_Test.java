package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.ojalgo.ProgrammingError;
import org.ojalgo.function.constant.BigMath;
import static org.ojalgo.function.constant.BigMath.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addExpression_Test {

    private ExpressionsBasedModel model;

    @Nested
    @DisplayName("Edge cases and state consistency")
    class EdgeCases {

        @Test
        @DisplayName("Expression naming is independent of existing variables")
        void testNamingUnaffectedByVariables() {
            model.addVariable("v1");
            model.addVariable("v2");
            Expression expr = model.addExpression();
            assertEquals("EXPR0", expr.getName(), "First expression name must be EXPR0 even when variables exist");
        }

        @Test
        @DisplayName("Naming restarts after clearing the internal map via reflection")
        void testNamingAfterMapClear() throws Exception {
            // Populate with two expressions
            model.addExpression();
            model.addExpression();
            // Clear the private map
            setMyExpressions(model, new java.util.HashMap<>());
            Expression expr = model.addExpression();
            assertEquals("EXPR0", expr.getName(), "After clearing the map, naming should restart from EXPR0");
        }

        @Test
        @DisplayName("Custom Options object does not interfere with default naming")
        void testNamingWithCustomOptions() {
            // Construct a model with a custom (but default) Options instance
            ExpressionsBasedModel custom = new ExpressionsBasedModel(new Optimisation.Options());
            Expression expr = custom.addExpression();
            assertEquals("EXPR0", expr.getName(), "Custom Options should not affect auto‑generated name");
        }

        @Test
        @DisplayName("Naming based on map size, not on numeric suffix of custom names")
        void testNamingAfterCustomNamedExpressions() {
            // Insert three custom‑named expressions
            model.addExpression("A");
            model.addExpression("B");
            model.addExpression("C");
            // Map size is now 3, next auto‑generated name must be EXPR3
            Expression expr = model.addExpression();
            assertEquals("EXPR3", expr.getName(), "Generated name must reflect map size, not existing numeric suffixes");
        }

        @Test
        @DisplayName("Shallow copy starts naming from zero")
        void testNamingAfterShallowCopy() {
            // Original model with two expressions
            model.addExpression();
            model.addExpression();
            // Shallow copy (first boolean = true)
            ExpressionsBasedModel copy = model.copy(true, false);
            Expression expr = copy.addExpression();
            assertEquals("EXPR0", expr.getName(), "Shallow copy should have its own expression map, naming restarts at zero");
        }

        @Test
        @DisplayName("Naming restarts after removing the only expression")
        void testNamingAfterRemoval() {
            // EXPR0
            Expression removed = model.addExpression();
            assertEquals("EXPR0", removed.getName());
            model.removeExpression("EXPR0");
            assertEquals(0, model.countExpressions(), "Expression map should be empty after removal");
            Expression expr = model.addExpression();
            assertEquals("EXPR0", expr.getName(), "After removal, naming should restart from EXPR0");
        }

        @Test
        @DisplayName("Unrelated static resets do not affect naming")
        void testStaticResetsNoSideEffect() {
            ExpressionsBasedModel.clearPresolvers();
            ExpressionsBasedModel.clearIntegrations();
            Expression expr = model.addExpression();
            assertEquals("EXPR0", expr.getName(), "Static resets must not influence expression naming");
        }
    }

    @Nested
    @DisplayName("Exception scenarios")
    class ExceptionScenarios {

        @Test
        @DisplayName("ProgrammingError when internal map is null")
        void testProgrammingErrorWhenMapNull() throws Exception {
            // Corrupt internal state
            setMyExpressions(model, null);
            ProgrammingError thrown = assertThrows(ProgrammingError.class, () -> model.addExpression(), "Calling addExpression() with a null internal map must throw ProgrammingError");
            assertNotNull(thrown.getMessage());
        }

        @Test
        @DisplayName("ProgrammingError on duplicate generated name")
        void testDuplicateGeneratedNameThrows() {
            // Manually insert an expression with the name that will be generated next
            // map size becomes 1, next auto name would be EXPR1
            model.addExpression("EXPR0");
            // Insert another expression with name EXPR1 to cause a clash when auto‑generating
            model.addExpression("EXPR1");
            // Now the map size is 2, next auto name would be EXPR2 – no clash.
            // To provoke a clash we need the map size to be 1 and the auto name to be EXPR1 which already exists.
            // Reset model to a clean state:
            ExpressionsBasedModel fresh = new ExpressionsBasedModel();
            // map size =1, next auto would be EXPR1 again (size=1)
            fresh.addExpression("EXPR1");
            ProgrammingError thrown = assertThrows(ProgrammingError.class, fresh::addExpression, "Duplicate generated name must cause ProgrammingError");
            assertNotNull(thrown.getMessage());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh model instance.
        model = new ExpressionsBasedModel();
    }

    /**
     * Sets the private {@code myExpressions} field of the given model to the supplied value.
     */
    private static void setMyExpressions(ExpressionsBasedModel target, Object value) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void testAddExpressionCreatesExpressionAndUpdatesCount() {
        // Initially the model has no expressions.
        assertEquals(0, model.countExpressions(), "Model should start with zero expressions");
        // Add the first expression.
        Expression expr0 = model.addExpression();
        assertNotNull(expr0, "addExpression() must return a non‑null Expression");
        // The automatically generated name should be "EXPR0".
        String expectedName0 = "EXPR0";
        assertEquals(expectedName0, expr0.getName(), "First generated name should be EXPR0");
        // The model should now contain exactly one expression.
        assertEquals(1, model.countExpressions(), "Expression count should be 1 after first addition");
        assertSame(expr0, model.getExpression(expectedName0), "Expression should be retrievable by its name");
        // Add a second expression.
        Expression expr1 = model.addExpression();
        assertNotNull(expr1, "Second call to addExpression() must also return a non‑null Expression");
        // The generated name should be "EXPR1".
        String expectedName1 = "EXPR1";
        assertEquals(expectedName1, expr1.getName(), "Second generated name should be EXPR1");
        // Verify the model now holds two expressions with distinct names.
        assertEquals(2, model.countExpressions(), "Expression count should be 2 after second addition");
        List<String> names = model.expressions().map(Expression::getName).collect(Collectors.toList());
        assertTrue(names.contains(expectedName0), "Model should contain the first expression name");
        assertTrue(names.contains(expectedName1), "Model should contain the second expression name");
        assertEquals(2, names.stream().distinct().count(), "Expression names must be unique");
    }

    @Test
    void testAddExpressionDoesNotAffectExistingExpressions() {
        // Add an initial expression and give it a coefficient for later verification.
        Expression first = model.addExpression();
        Variable var = model.addVariable("x");
        // simple coefficient 1
        first.set(var, BigMath.ONE);
        // Capture the state before adding a new expression.
        int countBefore = model.countExpressions();
        Expression retrievedBefore = model.getExpression(first.getName());
        // Add another expression.
        model.addExpression();
        // Ensure the original expression is unchanged.
        assertEquals(countBefore, model.countExpressions() - 1, "Only one new expression should have been added");
        assertSame(retrievedBefore, model.getExpression(first.getName()), "Original expression reference must stay the same");
        assertEquals(BigMath.ONE, retrievedBefore.get(var, false), "Coefficients of the original expression must remain unchanged");
    }

    /**
     * Retrieves the private {@code myExpressions} map for verification.
     */
    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Expression> getMyExpressions(ExpressionsBasedModel target) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
        f.setAccessible(true);
        return (java.util.Map<String, Expression>) f.get(target);
    }
}
