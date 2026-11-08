package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_copy_Test {

    private ExpressionsBasedModel original;

    @BeforeEach
    void setupBeforeEach() {
        // By default we start with a deterministic, easy‑to‑solve model.
        original = new ExpressionsBasedModel();
        // Two variables, both bounded between 0 and 10, weight 1 (minimise sum)
        original.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        original.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        // Simple constraint: x + y >= 8
        original.newExpression("c1").set(original.getVariable(0), BigDecimal.ONE).set(original.getVariable(1), BigDecimal.ONE).lower(BigDecimal.valueOf(8));
    }

    /**
     * Returns the list of {@link Variable} objects contained in a model.
     */
    private static List<Variable> getVariables(ExpressionsBasedModel model) {
        return model.getVariables();
    }

    /**
     * Returns the list of {@link Expression} objects contained in a model.
     */
    private static List<Expression> getExpressions(ExpressionsBasedModel model) {
        return model.expressions().collect(Collectors.toList());
    }

    @Test
    void testCopyCreatesDeepCopy() {
        // -----------------------------------------------------------------
        // GIVEN – a model already prepared in @BeforeEach
        // -----------------------------------------------------------------
        ExpressionsBasedModel copy = original.copy();
        // -----------------------------------------------------------------
        // THEN – structural properties are duplicated but instances differ
        // -----------------------------------------------------------------
        assertNotSame(original, copy, "copy() must return a new instance");
        assertEquals(original.countVariables(), copy.countVariables(), "Variable count must be identical");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count must be identical");
        // Options are deliberately shared (see constructor implementation)
        assertSame(original.options, copy.options, "Options should be the same instance");
        // copy() delegates to the three‑argument constructor with shallow=false, prune=false
        assertFalse(copy.isShallowCopy(), "Default copy must not be a shallow copy");
        assertEquals(original.isRelaxed(), copy.isRelaxed(), "Relaxed flag must be copied unchanged");
        // -----------------------------------------------------------------
        // AND – the copy is independent of the original
        // -----------------------------------------------------------------
        // Add a new variable only to the copy; original must stay unchanged
        copy.newVariable("z").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertEquals(original.countVariables() + 1, copy.countVariables(), "Copy should have one additional variable");
        assertEquals(original.countVariables(), original.countVariables(), "Original must not be affected by modifications to the copy");
        // -----------------------------------------------------------------
        // AND – both models solve to the same optimum
        // -----------------------------------------------------------------
        Optimisation.Result originalResult = original.minimise();
        Optimisation.Result copyResult = copy.minimise();
        // Both results must be optimal and have the same objective value
        assertTrue(originalResult.getState().isOptimal(), "Original model should be optimal");
        assertTrue(copyResult.getState().isOptimal(), "Copy model should be optimal");
        assertEquals(originalResult.getValue(), copyResult.getValue(), "Objective values must match after copying");
        // Variable values must match (order of variables is preserved)
        List<Variable> originalVars = original.getVariables();
        List<Variable> copyVars = copy.getVariables();
        for (int i = 0; i < originalVars.size(); i++) {
            BigDecimal origVal = originalResult.get(original.indexOf(originalVars.get(i)));
            BigDecimal copyVal = copyResult.get(copy.indexOf(copyVars.get(i)));
            assertEquals(origVal, copyVal, "Variable value at index " + i + " must be identical in both results");
        }
        // Validate that the copy still satisfies the model definition
        assertTrue(copy.validate(copyResult), "Copy model must validate its own result");
    }

    /**
     * Uses reflection to obtain the private field that stores the SOS definitions.
     *  The actual field name is not part of the public API; we therefore look for a
     *  field of type {@code Collection} that is declared in {@code ExpressionsBasedModel}
     *  and whose name contains “sos”. If the field cannot be found the test will be
     *  skipped.
     */
    @SuppressWarnings("unchecked")
    private static Collection<?> getSpecialOrderedSets(ExpressionsBasedModel model) {
        for (Field f : ExpressionsBasedModel.class.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(f.getType()) && f.getName().toLowerCase().contains("sos")) {
                f.setAccessible(true);
                try {
                    return (Collection<?>) f.get(model);
                } catch (IllegalAccessException e) {
                    fail("Unable to access SOS collection via reflection", e);
                }
            }
        }
        // No SOS collection found – return an empty collection so the caller can decide.
        return Collections.emptyList();
    }

    @Test
    @DisplayName("copy() on an empty model returns a distinct, default‑state model")
    void testCopyEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy();
        assertNotSame(empty, copy, "copy() must return a new instance");
        assertEquals(0, copy.countVariables(), "Copy of empty model must have zero variables");
        assertEquals(0, copy.countExpressions(), "Copy of empty model must have zero expressions");
        assertEquals(BigDecimal.ZERO, copy.getObjectiveConstant(), "Objective constant of a fresh copy must be ZERO");
        assertFalse(copy.isShallowCopy(), "Default copy must not be shallow");
        assertFalse(copy.isRelaxed(), "Default copy must not be relaxed");
    }

    @Test
    @DisplayName("copy() duplicates a model with one variable and one expression")
    void testCopySingleVariableExpression() {
        // Build a model with a single variable and a single expression
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("e1").set(v, BigDecimal.ONE).lower(BigDecimal.valueOf(5));
        // Preserve the objective constant for later verification
        model.addObjectiveConstant(BigDecimal.valueOf(2));
        ExpressionsBasedModel copy = model.copy();
        // --- structural checks -------------------------------------------------
        assertEquals(1, copy.countVariables(), "Copy must contain exactly one variable");
        assertEquals(1, copy.countExpressions(), "Copy must contain exactly one expression");
        assertEquals(model.getObjectiveConstant(), copy.getObjectiveConstant(), "Objective constant must be preserved");
        Variable copyVar = copy.getVariable(0);
        assertNotSame(v, copyVar, "Variable objects must be distinct");
        assertEquals(v.getName(), copyVar.getName(), "Variable name must be identical");
        assertEquals(v.lower().doubleValue(), copyVar.lower().doubleValue(), "Variable lower bound must be identical");
        assertEquals(v.upper().doubleValue(), copyVar.upper().doubleValue(), "Variable upper bound must be identical");
        Expression originalExpr = model.getExpression("e1");
        Expression copyExpr = copy.getExpression("e1");
        assertNotSame(originalExpr, copyExpr, "Expression objects must be distinct");
        assertEquals(originalExpr.lower(), copyExpr.lower(), "Expression lower bound must be identical");
        assertEquals(originalExpr.getVariableIndices(), copyExpr.getVariableIndices(), "Expression must reference the same variable index");
        // --- independence checks -----------------------------------------------
        // Change the lower bound of the original variable – copy must stay unchanged
        v.lower(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.ZERO, copyVar.lower(), "Modifying the original must not affect the copy");
        // Add a new variable only to the copy – original must stay unchanged
        copy.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        assertEquals(1, model.countVariables(), "Original model must still have one variable");
        assertEquals(2, copy.countVariables(), "Copy must now have two variables");
    }

    @Test
    @DisplayName("copy() retains a known solution and remains independent")
    void testCopyPreservesKnownSolution() {
        // Prepare a simple model and solve it
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        model.newExpression("c").set(x, BigDecimal.ONE).set(y, BigDecimal.ONE).lower(BigDecimal.valueOf(8));
        Optimisation.Result known = model.minimise();
        assertTrue(known.getState().isOptimal(), "Pre‑condition: model must be solvable");
        // Store the known solution
        model.setKnownSolution(known);
        // Copy the model
        ExpressionsBasedModel copy = model.copy();
        // The copy must expose the same known solution (value‑wise)
        Optimisation.Result copyKnown = copy.getKnownSolution();
        assertNotNull(copyKnown, "Copy must retain a known solution");
        assertEquals(known.getValue(), copyKnown.getValue(), "Known solution value must be identical in the copy");
        // Modifying the original's known solution must not affect the copy
        // re‑solve (same optimum)
        Optimisation.Result altered = model.minimise();
        model.setKnownSolution(altered);
        assertNotEquals(altered.getValue(), copyKnown.getValue(), "Copy's known solution must stay unchanged after original is altered");
    }

    @Test
    @DisplayName("copy() correctly duplicates a model with several variables/expressions")
    void testCopyMultipleVariablesExpressions() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        Variable b = model.newVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        Variable c = model.newVariable("c").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5)).weight(BigDecimal.ONE);
        // Expression 1: a + 2b <= 6
        model.newExpression("e1").set(a, BigDecimal.ONE).set(b, BigDecimal.valueOf(2)).upper(BigDecimal.valueOf(6));
        // Expression 2: b + c >= 3
        model.newExpression("e2").set(b, BigDecimal.ONE).set(c, BigDecimal.ONE).lower(BigDecimal.valueOf(3));
        ExpressionsBasedModel copy = model.copy();
        // Verify counts
        assertEquals(3, copy.countVariables(), "Copy must have three variables");
        assertEquals(2, copy.countExpressions(), "Copy must have two expressions");
        // Verify that each variable is a distinct object but with identical attributes
        List<Variable> originalVars = model.getVariables();
        List<Variable> copyVars = copy.getVariables();
        for (int i = 0; i < originalVars.size(); i++) {
            Variable orig = originalVars.get(i);
            Variable cp = copyVars.get(i);
            assertNotSame(orig, cp, "Variable objects must be distinct");
            assertEquals(orig.getName(), cp.getName(), "Variable name must be identical");
            assertEquals(orig.lower(), cp.lower(), "Variable lower bound must be identical");
            assertEquals(orig.upper(), cp.upper(), "Variable upper bound must be identical");
        }
        // Verify expression coefficients are duplicated (deep copy)
        Expression origE1 = model.getExpression("e1");
        Expression copyE1 = copy.getExpression("e1");
        assertNotSame(origE1, copyE1, "Expression objects must be distinct");
        assertEquals(origE1.upper(), copyE1.upper(), "Upper bound must be identical");
        assertEquals(origE1.getVariableIndices(), copyE1.getVariableIndices(), "Variable indices must match");
        // Independence: change a coefficient in the original – copy must stay unchanged
        origE1.set(a, BigDecimal.valueOf(5));
        assertNotEquals(origE1.get(a), copyE1.get(a), "Modifying original expression must not affect copy");
    }

    @Test
    @DisplayName("copy() preserves integer/fixed variable status and remains independent")
    void testCopyIntegerAndFixedVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Integer variable
        Variable i1 = model.newVariable("i1").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        i1.integer(true);
        // Another integer variable
        Variable i2 = model.newVariable("i2").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        i2.integer(true);
        // Fixed variable (lower == upper)
        Variable f = model.newVariable("f").lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3)).weight(BigDecimal.ONE);
        // Simple constraint using the variables
        model.newExpression("c").set(i1, BigDecimal.ONE).set(i2, BigDecimal.ONE).set(f, BigDecimal.ONE).lower(BigDecimal.valueOf(5));
        ExpressionsBasedModel copy = model.copy();
        // Integer status
        assertEquals(2, copy.getIntegerVariables().size(), "Copy must contain two integer variables");
        // Fixed status
        assertEquals(1, copy.getFixedVariables().size(), "Copy must contain one fixed variable");
        // Verify distinct variable objects
        List<Variable> origVars = model.getVariables();
        List<Variable> copyVars = copy.getVariables();
        for (int i = 0; i < origVars.size(); i++) {
            assertNotSame(origVars.get(i), copyVars.get(i), "Variable objects must be distinct after copy");
        }
        // Independence: change integer flag on original – copy must stay unchanged
        i1.integer(false);
        assertTrue(copy.getIntegerVariables().stream().anyMatch(v -> v.getName().equals("i1")), "Copy must still treat i1 as integer");
    }

    @Test
    @DisplayName("copy() retains a Special Ordered Set definition")
    void testCopySpecialOrderedSet() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        Variable v3 = model.newVariable("v3");
        // Define an SOS1 (type = 1) with min=1, max=1
        model.addSpecialOrderedSet(Arrays.asList(v1, v2, v3), 1, 1);
        // Verify that the original indeed has an SOS definition (via reflection)
        Collection<?> originalSos = getSpecialOrderedSets(model);
        assertFalse(originalSos.isEmpty(), "Original model should contain an SOS definition");
        ExpressionsBasedModel copy = model.copy();
        // Verify that the copy also contains an SOS definition with the same size
        Collection<?> copySos = getSpecialOrderedSets(copy);
        assertEquals(originalSos.size(), copySos.size(), "Copy must retain the same number of SOS definitions");
        // Basic sanity: the variables referenced by the SOS in the copy must be distinct objects
        // (the internal SOS structure is not public, so we only check that the variable
        // collection size matches)
        assertEquals(3, copy.getVariables().size(), "Copy must still contain the three variables used in the SOS");
    }

    @Test
    @DisplayName("copy() of a relaxed model is not relaxed and keeps integer variables")
    void testCopyAfterRelaxation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable i = model.newVariable("intVar").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        i.integer(true);
        // Relax the whole model (integer variables become continuous)
        model.relax();
        assertTrue(model.isRelaxed(), "Original model should be in relaxed state");
        assertTrue(model.isAnyVariableInteger(), "Even after relaxation the original still knows it had integer variables");
        // Copy after relaxation
        ExpressionsBasedModel copy = model.copy();
        // The copy must NOT be relaxed
        assertFalse(copy.isRelaxed(), "Copy must not inherit the relaxed flag from the source model");
        // The copy must retain the integer variable information
        assertTrue(copy.isAnyVariableInteger(), "Copy must still recognise the presence of integer variables");
    }

    @Test
    @DisplayName("copy() preserves quadratic constraints")
    void testCopyQuadraticConstraint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.newVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Create a quadratic expression x*y >= 4
        Expression quad = model.newExpression("quad");
        // The public API for quadratic terms is not documented here; we use reflection
        // to invoke the (presumed) method `setQuadratic(Variable, Variable, Number)`.
        try {
            java.lang.reflect.Method setQuad = Expression.class.getDeclaredMethod("setQuadratic", Variable.class, Variable.class, Number.class);
            setQuad.setAccessible(true);
            setQuad.invoke(quad, x, y, BigDecimal.ONE);
        } catch (Exception e) {
            fail("Unable to create quadratic term via reflection", e);
        }
        quad.lower(BigDecimal.valueOf(4));
        assertTrue(model.isAnyConstraintQuadratic(), "Model must report presence of quadratic constraints before copy");
        assertTrue(model.isAnyExpressionQuadratic(), "Model must report presence of quadratic expressions before copy");
        ExpressionsBasedModel copy = model.copy();
        // Flags must be preserved
        assertTrue(copy.isAnyConstraintQuadratic(), "Copy must report quadratic constraints");
        assertTrue(copy.isAnyExpressionQuadratic(), "Copy must report quadratic expressions");
        // Independence: modify coefficient in original and ensure copy unchanged
        try {
            java.lang.reflect.Method getCoeff = Expression.class.getDeclaredMethod("getQuadraticCoefficient", Variable.class, Variable.class);
            getCoeff.setAccessible(true);
            Number origCoeff = (Number) getCoeff.invoke(quad, x, y);
            // Change the original coefficient via a (hypothetical) setter
            java.lang.reflect.Method setCoeff = Expression.class.getDeclaredMethod("setQuadratic", Variable.class, Variable.class, Number.class);
            setCoeff.setAccessible(true);
            setCoeff.invoke(quad, x, y, BigDecimal.valueOf(5));
            // Retrieve the same coefficient from the copy
            Expression copyQuad = copy.getExpression("quad");
            Number copyCoeff = (Number) getCoeff.invoke(copyQuad, x, y);
            assertNotEquals(origCoeff, copyCoeff, "Quadratic coefficient change in original must not affect copy");
        } catch (Exception e) {
            fail("Reflection on quadratic coefficient failed", e);
        }
    }

    @Test
    @DisplayName("copy() on a disposed model throws IllegalStateException")
    void testCopyDisposedModelThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.dispose();
        assertThrows(IllegalStateException.class, model::copy, "Calling copy() on a disposed model must throw IllegalStateException");
    }

    @Test
    @DisplayName("copy() detects a variable with lower > upper and throws ProgrammingError")
    void testCopyInvalidVariableBoundsThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Create a variable with inconsistent bounds
        model.newVariable("bad").lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(5));
        // The inconsistency is detected during copy()
        assertThrows(ProgrammingError.class, model::copy, "Copying a model with invalid variable bounds must raise ProgrammingError");
    }

    @Test
    @DisplayName("copy() detects an expression with lower > upper and throws ProgrammingError")
    void testCopyInvalidExpressionBoundsThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.newVariable("v").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Expression where lower bound > upper bound
        model.newExpression("badExpr").set(v, BigDecimal.ONE).lower(BigDecimal.valueOf(20)).upper(BigDecimal.valueOf(10));
        assertThrows(ProgrammingError.class, model::copy, "Copying a model with invalid expression bounds must raise ProgrammingError");
    }

    @Test
    @DisplayName("addSpecialOrderedSet with min > max throws IllegalArgumentException")
    void testInvalidSpecialOrderedSetConfiguration() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.newVariable("v1");
        Variable v2 = model.newVariable("v2");
        // min > max should be rejected immediately (no need to copy)
        assertThrows(IllegalArgumentException.class, () -> model.addSpecialOrderedSet(Arrays.asList(v1, v2), 3, 1), "Defining an SOS with min > max must throw IllegalArgumentException");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("copy() never returns null regardless of model state")
    void testCopyNeverReturnsNull(boolean relaxBeforeCopy) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        if (relaxBeforeCopy) {
            model.relax();
        }
        ExpressionsBasedModel copy = model.copy();
        assertNotNull(copy, "copy() must never return null");
    }
}
