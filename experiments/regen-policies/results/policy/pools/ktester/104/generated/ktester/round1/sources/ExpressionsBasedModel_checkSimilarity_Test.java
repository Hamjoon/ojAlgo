package org.ojalgo.optimisation;

import static org.ojalgo.function.constant.BigMath.ONE;
import static org.ojalgo.function.constant.BigMath.TWO;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_checkSimilarity_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Variable y;

    /**
     * Simple descriptor used by the parameterised test.
     */
    private static class TestCase {

        final String group;

        final String description;

        final Runnable modelSetup;

        final Expression potential;

        // Boolean result or Class<? extends Throwable>
        final Object expected;

        TestCase(String group, String description, Runnable modelSetup, Expression potential, Object expected) {
            this.group = group;
            this.description = description;
            this.modelSetup = modelSetup;
            this.potential = potential;
            this.expected = expected;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        x = model.newVariable("x");
        y = model.newVariable("y");
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
        x = null;
        y = null;
    }

    /**
     * Creates a new expression with the given name and the supplied
     *  variable‑coefficient pairs.
     */
    private Expression createExpression(String name, Object... varCoeff) {
        Expression e = model.newExpression(name);
        for (int i = 0; i < varCoeff.length; i += 2) {
            Variable v = (Variable) varCoeff[i];
            BigDecimal coeff = (BigDecimal) varCoeff[i + 1];
            e.set(v, coeff);
        }
        return e;
    }

    /**
     * Returns a deep‑clone of the supplied expression (same terms,
     *  different instance).
     */
    private Expression cloneExpression(Expression original, String newName) {
        Expression clone = model.newExpression(newName);
        original.getLinearKeySet().forEach(idx -> clone.set(model.getVariable(idx), original.get(idx)));
        clone.setConstant(original.getConstant());
        return clone;
    }

    /**
     * Supplies all test cases.
     */
    static Stream<TestCase> data() {
        return Stream.of(// Empty model – no existing expressions
        new TestCase("Empty model – no existing expressions", "Any non‑null expression should be reported as not similar.", () -> {
        }, null, false), new TestCase("Empty model – no existing expressions", "Passing a null potential expression should result in NullPointerException.", () -> {
        }, null, NullPointerException.class), // Model with one expression – identical (similar) expression
        new TestCase("Model with one expression – identical (similar) expression", "Same instance should be considered similar.", () -> {
        }, null, true), // Model with one expression – different (non‑similar) expression
        new TestCase("Model with one expression – different (non‑similar) expression", "A distinct expression with different terms must return false.", () -> {
        }, null, false), // Model with multiple expressions – first match
        new TestCase("Model with multiple expressions – first match", "Potential matches the first stored expression.", () -> {
        }, null, true), // Model with multiple expressions – no match
        new TestCase("Model with multiple expressions – no match", "Potential does not match any stored expression.", () -> {
        }, null, false), // Model with duplicate similar expressions (different instances)
        new TestCase("Model with duplicate similar expressions (different instances)", "Two distinct but structurally identical expressions – similarity must be true.", () -> {
        }, null, true), // Model with expression having same name but different content
        new TestCase("Model with expression having same name but different content", "Same name does not imply similarity.", () -> {
        }, null, false), // Model with empty expression (no terms)
        new TestCase("Model with empty expression (no terms)", "Empty expression compared with itself must be similar.", () -> {
        }, null, true), // Model with many expressions – last match
        new TestCase("Model with many expressions – last match", "Potential matches the last added expression.", () -> {
        }, null, true), // Model after removing expression – no longer similar
        new TestCase("Model after removing expression – no longer similar", "Removed expression should no longer be considered similar.", () -> {
        }, null, false), // Potential expression with null name but identical content
        new TestCase("Potential expression with null name but identical content", "Unnamed but structurally identical expressions are similar.", () -> {
        }, null, true), // Disposed model – after dispose
        new TestCase("Disposed model – after dispose", "Calling checkSimilarity on a disposed model must throw IllegalStateException.", () -> {
        }, null, IllegalStateException.class), // Potential expression with null variable collection
        new TestCase("Potential expression with null variable collection", "Null internal variable collection should cause NullPointerException.", () -> {
        }, null, NullPointerException.class), // Potential expression with null coefficient map
        new TestCase("Potential expression with null coefficient map", "Null internal coefficient map should cause NullPointerException.", () -> {
        }, null, NullPointerException.class), // Potential expression referencing variable not in model
        new TestCase("Potential expression with external variable", "Expression referencing a variable from another model must throw IllegalArgumentException.", () -> {
        }, null, IllegalArgumentException.class), // Potential expression with NaN coefficient
        new TestCase("Potential expression with NaN coefficient", "Expression containing NaN coefficient must throw IllegalArgumentException.", () -> {
        }, null, IllegalArgumentException.class));
    }

    /**
     * When the potential expression has exactly the same linear terms as an expression
     * already stored in the model, {@code checkSimilarity} must return {@code true}.
     */
    @Test
    void testSimilarityReturnsTrueForIdenticalLinearExpression() {
        Expression existing = model.newExpression("existing");
        existing.set(x, ONE);
        existing.set(y, TWO);
        Expression potential = model.newExpression("potential");
        potential.set(x, ONE);
        potential.set(y, TWO);
        assertTrue(model.checkSimilarity(potential), "checkSimilarity should return true for an expression identical to an existing one");
    }

    /**
     * When the potential expression differs in at least one coefficient, the method
     * must return {@code false}.
     */
    @Test
    void testSimilarityReturnsFalseForDifferentLinearExpression() {
        Expression existing = model.newExpression("existing");
        existing.set(x, ONE);
        existing.set(y, TWO);
        Expression potential = model.newExpression("potential");
        potential.set(x, ONE);
        // different coefficient
        potential.set(y, ONE);
        assertFalse(model.checkSimilarity(potential), "checkSimilarity should return false when the expression differs from existing ones");
    }

    /**
     * If the model contains no expressions, any potential expression should be reported
     * as not similar.
     */
    @Test
    void testSimilarityWithEmptyModel() {
        Expression potential = model.newExpression("potential");
        potential.set(x, ONE);
        assertFalse(model.checkSimilarity(potential), "checkSimilarity should return false when the model has no stored expressions");
    }

    /**
     * Verify that similarity works for a single‑variable expression.
     */
    @Test
    void testSimilaritySingleVariable() {
        Expression existing = model.newExpression("existing");
        existing.set(x, TWO);
        Expression potential = model.newExpression("potential");
        potential.set(x, TWO);
        assertTrue(model.checkSimilarity(potential), "Single‑variable expressions with identical coefficients must be considered similar");
    }

    /**
     * Parameterised test that iterates over all cases defined in {@link #data()}.
     *
     * <p>The test builds the required model state, creates the potential expression,
     * configures the static mock of {@code Presolvers.checkSimilarity} to either return a
     * boolean or throw the expected exception, invokes {@code model.checkSimilarity}
     * and finally asserts the observed outcome.</p>
     */
    @ParameterizedTest(name = "[{index}] {0.group} – {0.description}")
    @MethodSource("data")
    void testCheckSimilarity(TestCase tc) throws Exception {
        // 1️⃣ Build the model state as described by the test case
        tc.modelSetup.run();
        // Helper variables
        Expression exprA = null, exprB = null, exprC = null, exprX = null, exprZ = null, exprClone = null, emptyExpr = null, potential = null;
        // 2️⃣ Create the potential expression (or null) according to the group
        switch(tc.group) {
            case "Empty model – no existing expressions":
                if (tc.expected == NullPointerException.class) {
                    potential = null;
                } else {
                    potential = model.newExpression("potential");
                    potential.set(x, ONE);
                }
                break;
            case "Model with one expression – identical (similar) expression":
                Expression expr1 = model.newExpression("expr1");
                expr1.set(x, ONE);
                expr1.set(y, TWO);
                // same instance
                potential = expr1;
                break;
            case "Model with one expression – different (non‑similar) expression":
                Expression expr2 = model.newExpression("expr1");
                expr2.set(x, ONE);
                expr2.set(y, TWO);
                potential = model.newExpression("expr2");
                potential.set(x, ONE);
                // different coefficient
                potential.set(y, ONE);
                break;
            case "Model with multiple expressions – first match":
                exprA = model.newExpression("A");
                exprA.set(x, ONE);
                exprB = model.newExpression("B");
                exprB.set(y, TWO);
                exprC = model.newExpression("C");
                exprC.set(x, TWO);
                potential = exprA;
                break;
            case "Model with multiple expressions – no match":
                exprA = model.newExpression("A");
                exprA.set(x, ONE);
                exprB = model.newExpression("B");
                exprB.set(y, TWO);
                exprC = model.newExpression("C");
                exprC.set(x, TWO);
                potential = model.newExpression("X");
                potential.set(x, TWO);
                potential.set(y, TWO);
                break;
            case "Model with duplicate similar expressions (different instances)":
                Expression original = model.newExpression("orig");
                original.set(x, ONE);
                original.set(y, TWO);
                exprClone = cloneExpression(original, "clone");
                potential = exprClone;
                break;
            case "Model with expression having same name but different content":
                Expression named = model.newExpression("e1");
                named.set(x, ONE);
                potential = model.newExpression("e1");
                potential.set(y, TWO);
                break;
            case "Model with empty expression (no terms)":
                emptyExpr = model.newExpression("empty");
                potential = emptyExpr;
                break;
            case "Model with many expressions – last match":
                for (char c = 'A'; c <= 'Y'; c++) {
                    Expression e = model.newExpression("expr" + c);
                    e.set(x, ONE);
                }
                exprZ = model.newExpression("Z");
                exprZ.set(y, TWO);
                potential = exprZ;
                break;
            case "Model after removing expression – no longer similar":
                Expression toRemove = model.newExpression("toRemove");
                toRemove.set(x, ONE);
                model.removeExpression("toRemove");
                potential = toRemove;
                break;
            case "Potential expression with null name but identical content":
                Expression unnamed = model.newExpression(null);
                unnamed.set(x, ONE);
                potential = model.newExpression(null);
                potential.set(x, ONE);
                break;
            case "Disposed model – after dispose":
                model.dispose();
                potential = model.newExpression("potential");
                potential.set(x, ONE);
                break;
            case "Potential expression with null variable collection":
                potential = model.newExpression("badVarColl");
                potential.set(x, ONE);
                Field varField = Expression.class.getDeclaredField("myVariables");
                varField.setAccessible(true);
                varField.set(potential, null);
                break;
            case "Potential expression with null coefficient map":
                potential = model.newExpression("badCoeff");
                Field coeffField = Expression.class.getDeclaredField("myCoefficients");
                coeffField.setAccessible(true);
                coeffField.set(potential, null);
                break;
            case "Potential expression with external variable":
                ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
                Variable external = otherModel.newVariable("external");
                potential = model.newExpression("externalRef");
                potential.set(external, ONE);
                break;
            case "Potential expression with NaN coefficient":
                potential = model.newExpression("nanCoeff");
                potential.set(x, new BigDecimal(Double.NaN));
                break;
            default:
                fail("Unhandled test group: " + tc.group);
        }
        // 3️⃣ Invoke the method under test and assert the outcome
        if (tc.expected instanceof Class) {
            // Expecting an exception
            assertThrows((Class<? extends Throwable>) tc.expected, () -> model.checkSimilarity(potential), "Expected exception not thrown for group: " + tc.group);
        } else {
            // Expecting a boolean result
            boolean result = model.checkSimilarity(potential);
            assertEquals(tc.expected, result, "Unexpected result for group: " + tc.group);
        }
    }
}
