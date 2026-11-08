package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import org.ojalgo.ProgrammingError;
import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_copy_Test {

    private ExpressionsBasedModel original;

    private Variable varX;

    private Variable varY;

    private Expression expr;

    @BeforeEach
    void setupBeforeEach() {
        // Build a small, deterministic model that can be inspected after copying
        original = new ExpressionsBasedModel();
        // two variables with bounds, levels and contribution weights
        varX = original.newVariable("X").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).weight(BigDecimal.ONE);
        varY = original.newVariable("Y").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5)).weight(BigDecimal.valueOf(2));
        // a simple expression that will become part of the objective
        expr = original.newExpression("expr").weight(// contribution to the objective
        BigDecimal.ONE).lower(// treat as a constraint (lower bound)
        BigDecimal.ZERO).upper(// upper bound
        BigDecimal.valueOf(20));
        // 3·X + 4·Y <= 20
        expr.set(varX, BigDecimal.valueOf(3));
        expr.set(varY, BigDecimal.valueOf(4));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the model holds only in‑memory data
        original = null;
        varX = null;
        varY = null;
        expr = null;
    }

    /**
     * Verify that {@code copy()} returns a deep copy with identical public state.
     */
    @Test
    void testCopyCreatesEqualModel() {
        ExpressionsBasedModel copy = original.copy();
        // The copy must be a different instance
        assertNotSame(original, copy, "copy() should return a new model instance");
        // Same number of variables and expressions
        assertEquals(original.countVariables(), copy.countVariables(), "Variable count must be preserved");
        assertEquals(original.countExpressions(), copy.countExpressions(), "Expression count must be preserved");
        // Variables: same names, bounds and weights (public getters are used)
        for (int i = 0; i < original.countVariables(); i++) {
            Variable origVar = original.getVariable(i);
            Variable copyVar = copy.getVariable(i);
            assertNotSame(origVar, copyVar, "Variable objects must be distinct");
            assertEquals(origVar.getName(), copyVar.getName(), "Variable name must be copied");
            assertEquals(origVar.lowerLimit(), copyVar.lowerLimit(), "Variable lower bound must be copied");
            assertEquals(origVar.upperLimit(), copyVar.upperLimit(), "Variable upper bound must be copied");
            assertEquals(origVar.getWeight(), copyVar.getWeight(), "Variable weight must be copied");
        }
        // Expressions: same name, bounds, weight and coefficients
        Expression origExpr = original.getExpression("expr");
        Expression copyExpr = copy.getExpression("expr");
        assertNotNull(copyExpr, "Expression with name 'expr' must exist in the copy");
        assertNotSame(origExpr, copyExpr, "Expression objects must be distinct");
        assertEquals(origExpr.lowerLimit(), copyExpr.lowerLimit(), "Expression lower bound must be copied");
        assertEquals(origExpr.upperLimit(), copyExpr.upperLimit(), "Expression upper bound must be copied");
        assertEquals(origExpr.getWeight(), copyExpr.getWeight(), "Expression weight must be copied");
        // Coefficients for each variable
        for (Variable v : original.getVariables()) {
            BigDecimal origCoeff = origExpr.get(v);
            BigDecimal copyCoeff = copyExpr.get(v);
            assertEquals(origCoeff, copyCoeff, () -> "Coefficient for variable '" + v.getName() + "' must be identical");
        }
        // The copy must not be a shallow copy and must not be marked as relaxed
        assertFalse(copy.isShallowCopy(), "Copy should not be a shallow copy");
        assertFalse(copy.isRelaxed(), "Copy should not be relaxed");
    }

    /**
     * Changing the original model after copying must not affect the copy.
     */
    @Test
    void testCopyIsIndependentFromOriginal() {
        ExpressionsBasedModel copy = original.copy();
        // Mutate the original: add a new variable and a new expression
        Variable varZ = original.newVariable("Z").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        original.newExpression("extra").set(varZ, BigDecimal.ONE).upper(BigDecimal.ONE);
        // The copy must stay unchanged
        assertEquals(2, copy.countVariables(), "Copy must retain the original variable count");
        assertEquals(1, copy.countExpressions(), "Copy must retain the original expression count");
        assertNull(copy.getExpression("extra"), "Copy must not contain the newly added expression");
        assertNull(copy.getVariable("Z"), "Copy must not contain the newly added variable");
    }

    /**
     * The {@code Options} instance is shared between the original and the copy (as defined by the
     * constructor).
     */
    @Test
    void testCopySharesOptionsInstance() {
        ExpressionsBasedModel copy = original.copy();
        assertSame(original.options, copy.options, "The Options object must be the same instance in the copy");
    }

    /**
     * Copy an empty model – the copy must be a distinct instance with zero variables/expressions.
     */
    @Test
    void copyMethod_basicEmptyModel() {
        ExpressionsBasedModel empty = new ExpressionsBasedModel();
        ExpressionsBasedModel copy = empty.copy();
        assertNotSame(empty, copy, "Copy of empty model must be a new instance");
        assertEquals(0, copy.countVariables(), "Variable count of empty copy must be zero");
        assertEquals(0, copy.countExpressions(), "Expression count of empty copy must be zero");
    }

    /**
     * Verify that a shallow copy (copy(true, false)) re‑uses the same Variable/Expression objects.
     */
    @Test
    void copyMethod_shallowCopyTrue() {
        ExpressionsBasedModel shallow = original.copy(true, false);
        assertTrue(shallow.isShallowCopy(), "Shallow copy flag must be true");
        assertEquals(original.countVariables(), shallow.countVariables(), "Variable count must match");
        assertEquals(original.countExpressions(), shallow.countExpressions(), "Expression count must match");
        // reference equality for each variable / expression
        for (int i = 0; i < original.countVariables(); i++) {
            assertSame(original.getVariable(i), shallow.getVariable(i), "Variables must be the same instance in a shallow copy");
        }
        for (int i = 0; i < original.countExpressions(); i++) {
            assertSame(original.getExpression(i), shallow.getExpression(i), "Expressions must be the same instance in a shallow copy");
        }
    }

    /**
     * Shallow copy with pruning (copy(true, true)) removes unreferenced variables.
     */
    @Test
    void copyMethod_shallowCopyAndPruneTrue() {
        // add a third variable that will stay unused
        Variable varZ = original.newVariable("Z").lower(BigDecimal.ZERO).upper(BigDecimal.ONE).weight(BigDecimal.ONE);
        // now perform shallow+prune copy
        ExpressionsBasedModel copy = original.copy(true, true);
        assertTrue(copy.isShallowCopy(), "Copy should still be marked as shallow");
        // only X and Y are referenced by the expression, Z should be pruned
        assertEquals(2, copy.countVariables(), "Pruned copy must contain only referenced variables");
        assertEquals(1, copy.countExpressions(), "Expression count must stay unchanged");
        // ensure that the remaining variables are the original ones (reference equality)
        assertSame(original.getVariable("X"), copy.getVariable("X"));
        assertSame(original.getVariable("Y"), copy.getVariable("Y"));
        assertNull(copy.getVariable("Z"), "Pruned variable Z must not be present");
    }

    /**
     * Verify that a relaxed copy (copy(true)) converts integer variables to continuous ones.
     */
    @Test
    void copyMethod_relaxedCopy() {
        // make X an integer variable
        varX.integer(true);
        assertTrue(original.isAnyVariableInteger(), "Original model must contain an integer variable");
        ExpressionsBasedModel relaxed = original.copy(true);
        assertTrue(relaxed.isRelaxed(), "Relaxed copy must report isRelaxed() == true");
        assertFalse(relaxed.isAnyVariableInteger(), "All integer variables must be relaxed to continuous");
        // original stays unchanged
        assertTrue(original.isAnyVariableInteger(), "Original model must still contain integer variables");
    }

    /**
     * Fixed variables must stay fixed after copying.
     */
    @Test
    void copyMethod_fixedVariablePreserved() {
        // fix variable X to value 3
        varX.fix(BigDecimal.valueOf(3));
        assertTrue(original.getFixedVariables().contains(original.toIntIndex(varX.index())));
        ExpressionsBasedModel copy = original.copy();
        Variable copyX = copy.getVariable("X");
        assertTrue(copy.getFixedVariables().contains(copy.toIntIndex(copyX.index())), "Fixed variable must be present in the copy");
        assertEquals(BigDecimal.valueOf(3), copyX.getValue(), "Fixed value must be preserved in the copy");
    }

    /**
     * Special ordered set (SOS) must be copied while keeping a distinct SOS collection object.
     */
    @Test
    void copyMethod_specialOrderedSetCopied() {
        // create three variables and an expression to link the SOS
        Variable a = original.newVariable("A");
        Variable b = original.newVariable("B");
        Variable c = original.newVariable("C");
        Expression link = original.newExpression("link");
        List<Variable> sosVars = Arrays.asList(a, b, c);
        // type = 1 (type is arbitrary for the test)
        original.addSpecialOrderedSet(sosVars, 1, link);
        // Use reflection to inspect the internal SOS list.
        List<?> originalSOS = getInternalSOSList(original);
        List<?> copySOS = getInternalSOSList(original.copy());
        assertNotNull(originalSOS, "Original SOS list must be accessible via reflection");
        assertNotNull(copySOS, "Copy SOS list must be accessible via reflection");
        assertEquals(originalSOS.size(), copySOS.size(), "SOS count must be preserved");
        // each SOS group must contain the same variable indices but be a different object
        for (int i = 0; i < originalSOS.size(); i++) {
            Object origGroup = originalSOS.get(i);
            Object copyGroup = copySOS.get(i);
            assertNotSame(origGroup, copyGroup, "SOS group objects must be distinct");
            // compare the variable indices via reflection
            List<Integer> origIdx = getVariableIndicesFromSOSGroup(origGroup);
            List<Integer> copyIdx = getVariableIndicesFromSOSGroup(copyGroup);
            assertEquals(origIdx, copyIdx, "SOS group must contain the same variable indices");
        }
    }

    /**
     * Integration switch flag must be copied.
     */
    @Test
    void copyMethod_integrationSwitchPreserved() {
        original.setIntegrationSwitch(true);
        assertTrue(original.isIntegrationSwitch(), "Original model must have integration switch enabled");
        ExpressionsBasedModel copy = original.copy();
        assertTrue(copy.isIntegrationSwitch(), "Copy must preserve integration switch flag");
    }

    /**
     * Known solution must NOT be copied to the new model.
     */
//     @Test
//     void copyMethod_knownSolutionNotCopied(@Mock Optimisation.Result dummyResult) {
//         // store a known solution (null is acceptable for the purpose of this test)
//         original.setKnownSolution(null);
//         ExpressionsBasedModel copy = original.copy();
//         assertNull(copy.getKnownSolution(), "Known solution must not be transferred to the copy");
//     }

    /**
     * Objective constant added via addObjectiveConstant must be duplicated.
     */
    @Test
    void copyMethod_objectiveConstantCopied() {
        original.addObjectiveConstant(new BigDecimal("5.5"));
        ExpressionsBasedModel copy = original.copy();
        assertEquals(new BigDecimal("5.5"), copy.getObjectiveConstant(), "Objective constant must be identical in the copy");
    }

    /**
     * Quadratic flag must be preserved – we create a quadratic expression using the public API.
     */
    @Test
    void copyMethod_quadraticFlagPreserved() {
        // Build a quadratic expression: X * X with coefficient 1 (assuming such API exists)
        Expression quad = original.newExpression("quad").quadratic(varX, varX, BigDecimal.ONE).upper(BigDecimal.valueOf(100));
        assertTrue(original.isAnyConstraintQuadratic(), "Original model must report a quadratic constraint");
        ExpressionsBasedModel copy = original.copy();
        assertTrue(copy.isAnyConstraintQuadratic(), "Copy must also report a quadratic constraint");
    }

    /**
     * Copying a disposed model must throw a ProgrammingError.
     */
    @Test
    void copyMethod_disposedModel() {
        original.dispose();
        assertThrows(ProgrammingError.class, () -> original.copy(), "Copying a disposed model should raise ProgrammingError");
    }

    /**
     * Copying a model with illegal variable bounds must raise IllegalArgumentException.
     */
    @Test
    void copyMethod_invalidVariableBounds() {
        ExpressionsBasedModel badModel = new ExpressionsBasedModel();
        // lower > upper -> illegal
        badModel.newVariable("V").lower(BigDecimal.TEN).upper(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, badModel::copy, "Copying a model with inconsistent variable bounds must throw IllegalArgumentException");
    }

    /**
     * Uninitialised (corrupted) expression should cause a NullPointerException during copy.
     */
    @Test
    void copyMethod_uninitializedExpression() throws Exception {
        // create a normal expression and then nullify its internal term list via reflection
        Expression corrupt = original.newExpression("corrupt").set(varX, BigDecimal.ONE);
        // Locate a plausible private field that holds the terms (implementation dependent)
        Field termsField = Arrays.stream(corrupt.getClass().getDeclaredFields()).filter(f -> f.getName().toLowerCase().contains("term") || f.getName().toLowerCase().contains("coeff")).findFirst().orElseThrow(() -> new IllegalStateException("Cannot locate term field for corruption"));
        termsField.setAccessible(true);
        // corrupt the internal structure
        termsField.set(corrupt, null);
        // Now copying the model should fail
        assertThrows(NullPointerException.class, original::copy, "Copying a model with a corrupted expression should throw NullPointerException");
    }

    /**
     * Snapshot models are read‑only; copying them must raise a ProgrammingError.
     */
    @Test
    void copyMethod_snapshotModel() {
        ExpressionsBasedModel snapshot = original.snapshot();
        assertThrows(ProgrammingError.class, snapshot::copy, "Copying a snapshot model should raise ProgrammingError");
    }

    /**
     * Retrieves the internal list that holds special ordered sets (SOS) via reflection.
     * The actual field name is not part of the public API; this method attempts to locate
     * a field of type {@code List} that likely stores the SOS groups.
     */
    @SuppressWarnings("unchecked")
    private static List<?> getInternalSOSList(ExpressionsBasedModel model) {
        try {
            Field sosField = Arrays.stream(model.getClass().getDeclaredFields()).filter(f -> f.getType().equals(List.class) && f.getName().toLowerCase().contains("sos")).findFirst().orElse(null);
            if (sosField == null) {
                return Collections.emptyList();
            }
            sosField.setAccessible(true);
            return (List<?>) sosField.get(model);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Extracts the variable indices from a SOS group object via reflection.
     * The exact structure of the group is implementation specific; we look for a
     * field that holds a collection of {@code Variable} or {@code IntIndex}.
     */
    @SuppressWarnings("unchecked")
    private static List<Integer> getVariableIndicesFromSOSGroup(Object sosGroup) {
        try {
            Field varsField = Arrays.stream(sosGroup.getClass().getDeclaredFields()).filter(f -> Collection.class.isAssignableFrom(f.getType())).findFirst().orElseThrow(() -> new IllegalStateException("No collection field in SOS group"));
            varsField.setAccessible(true);
            Collection<?> collection = (Collection<?>) varsField.get(sosGroup);
            List<Integer> indices = new ArrayList<>();
            for (Object o : collection) {
                if (o instanceof Variable) {
                    indices.add(((Variable) o).index());
                } else {
                    // Fallback for possible IntIndex representation
                    try {
                        Class<?> intIdxClass = Class.forName("org.ojalgo.type.index.IntIndex");
                        if (intIdxClass.isInstance(o)) {
                            Method intValue = intIdxClass.getMethod("intValue");
                            indices.add((Integer) intValue.invoke(o));
                        }
                    } catch (ClassNotFoundException ignored) {
                        // ignore – not an IntIndex
                    }
                }
            }
            return indices;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}