package org.ojalgo.optimisation;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfFreeVariable_3_Test {

    private ExpressionsBasedModel model;

    private Variable var0;

    private Variable var1;

    private Variable var2;

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // three variables – initially all free
        // index 0
        var0 = model.addVariable();
        // index 1
        var1 = model.addVariable();
        // index 2
        var2 = model.addVariable();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly necessary but keeps the pattern tidy.
        model = null;
        var0 = var1 = var2 = null;
    }

    private static Stream<Arguments> provideFreeVariableIndices() {
        ExpressionsBasedModel tmp = new ExpressionsBasedModel();
        Variable v0 = tmp.addVariable();
        Variable v1 = tmp.addVariable();
        return Stream.of(Arguments.of(v0, 0), Arguments.of(v1, 1));
    }

    @Test
    void testIndexOfFreeVariable_AllVariablesAreFree() {
        // All variables are free, therefore the free‑variable index must match the
        // order in which they were added (0, 1, 2).
        assertEquals(0, model.indexOfFreeVariable(var0), "First free variable should have index 0");
        assertEquals(1, model.indexOfFreeVariable(var1), "Second free variable should have index 1");
        assertEquals(2, model.indexOfFreeVariable(var2), "Third free variable should have index 2");
    }

    @Test
    void testIndexOfFreeVariable_WithFixedVariable() {
        // Fix the first variable by giving it identical lower and upper bounds.
        var0.lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);
        // After fixing, var0 is no longer a free variable.
        assertEquals(-1, model.indexOfFreeVariable(var0), "A fixed variable should return -1");
        // The remaining free variables should be re‑indexed consecutively.
        assertEquals(0, model.indexOfFreeVariable(var1), "First free variable after fixing should be index 0");
        assertEquals(1, model.indexOfFreeVariable(var2), "Second free variable after fixing should be index 1");
    }

    @Test
    void testIndexOfFreeVariable_VariableFromOtherModel() {
        // Create a second model and obtain a variable from it.
        ExpressionsBasedModel otherModel = new ExpressionsBasedModel();
        Variable foreignVar = otherModel.addVariable();
        // The variable does not belong to the original model → IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> model.indexOfFreeVariable(foreignVar), "Calling indexOfFreeVariable with a variable from another model should throw");
    }

    /**
     * Null handling – the method should fail fast.
     */
    @Test
    void testIndexOfFreeVariable_NullVariable() {
        assertThrows(NullPointerException.class, () -> model.indexOfFreeVariable(null), "Passing null should throw NullPointerException");
    }

    /**
     * Variable that has never been added to any model (created via reflection).
     */
    @Test
    void testIndexOfFreeVariable_UnattachedVariable() throws Exception {
        Constructor<Variable> ctor = Variable.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Variable unattached = ctor.newInstance();
        assertThrows(IllegalArgumentException.class, () -> model.indexOfFreeVariable(unattached), "Variable not belonging to the model should cause IllegalArgumentException");
    }

    /**
     * Parameterized free‑variable detection for the first two variables.
     */
    @ParameterizedTest(name = "Free variable {0} should have free‑index {1}")
    @MethodSource("provideFreeVariableIndices")
    void testIndexOfFreeVariable_FreeVariableDetection(Variable variable, int expectedIndex) {
        assertEquals(expectedIndex, model.indexOfFreeVariable(variable));
    }

    /**
     * Fixed variable must return -1; first free after a fixed variable must be 0.
     */
    @Test
    void testIndexOfFreeVariable_FixedVariableExclusion() {
        // convenience method to fix the variable
        var0.fix(BigDecimal.ONE);
        assertEquals(-1, model.indexOfFreeVariable(var0), "Fixed variable should return -1");
        assertEquals(0, model.indexOfFreeVariable(var1), "First free variable after a fixed one should be 0");
    }

    /**
     * Integer variable handling – still part of the free list when not fixed.
     */
    @Test
    void testIndexOfFreeVariable_IntegerVariableFree() {
        Variable intVar = model.addVariable().integer(true);
        // intVar is the 4th variable (global index 3) but should be the first free variable
        // because we have not fixed any variable yet.
        assertEquals(3, model.indexOf(intVar), "Global index of the new integer variable");
        assertEquals(3, model.indexOfFreeVariable(intVar), "Integer variable that is free should have its free‑index equal to its position among free vars");
    }

    /**
     * Fixed integer variable behaves like any other fixed variable.
     */
    @Test
    void testIndexOfFreeVariable_FixedIntegerVariable() {
        Variable intVar = model.addVariable().integer(true).fix(BigDecimal.TEN);
        assertEquals(-1, model.indexOfFreeVariable(intVar), "Fixed integer variable must return -1");
    }

    /**
     * Mixed ordering where a fixed variable sits between free ones.
     */
    @Test
    void testIndexOfFreeVariable_MixedVariableOrdering() {
        // var1 becomes fixed
        var1.fix(BigDecimal.ZERO);
        // var0 is free → index 0, var2 is free → index 1
        assertEquals(0, model.indexOfFreeVariable(var0));
        assertEquals(1, model.indexOfFreeVariable(var2));
    }

    /**
     * Multiple fixed variables before a free one.
     */
    @Test
    void testIndexOfFreeVariable_MultipleFixedBeforeFree() {
        var0.fix(BigDecimal.ONE);
        var1.fix(BigDecimal.ONE);
        // var2 is the first free variable now
        assertEquals(0, model.indexOfFreeVariable(var2));
    }

    /**
     * Mixed integer and continuous free variables – ordering across types.
     */
    @Test
    void testIndexOfFreeVariable_MixedIntegerAndContinuous() {
        // index 3 globally
        Variable intVar = model.addVariable().integer(true);
        // index 4 globally
        Variable contVar = model.addVariable();
        // Both are free, order should follow addition order
        assertEquals(3, model.indexOfFreeVariable(intVar));
        assertEquals(4, model.indexOfFreeVariable(contVar));
    }

    /**
     * Dynamic fixing – variable becomes non‑free after a call to fix().
     */
    @Test
    void testIndexOfFreeVariable_DynamicFixing() {
        // Initially free
        assertEquals(0, model.indexOfFreeVariable(var0));
        // Fix it
        var0.fix(BigDecimal.ONE);
        assertEquals(-1, model.indexOfFreeVariable(var0));
    }

    /**
     * Special Ordered Set (SOS) inclusion – variables remain free unless fixed.
     */
    @Test
    void testIndexOfFreeVariable_SpecialOrderedSet() {
        // Create a SOS with var0 and var1
        model.addSpecialOrderedSet(Arrays.asList(var0, var1), 1, 2);
        // Neither is fixed, they should still be reported as free
        assertEquals(0, model.indexOfFreeVariable(var0));
        assertEquals(1, model.indexOfFreeVariable(var1));
    }

    /**
     * Relaxed integer variable – after calling relax() the variable stays free.
     */
    @Test
    void testIndexOfFreeVariable_RelaxedIntegerVariable() {
        Variable intVar = model.addVariable().integer(true);
        // converts integer variables to continuous
        model.relax();
        // Still free
        assertEquals(3, model.indexOfFreeVariable(intVar));
    }

    /**
     * Simplify after fixing variables – fixed variables are pruned, free indices re‑computed.
     */
    @Test
    void testIndexOfFreeVariable_SimplifyAfterFixing() {
        var0.fix(BigDecimal.ZERO);
        // removes fixed variables
        model.simplify();
        // var1 becomes the first (and only) free variable in the reduced model
        assertEquals(0, model.indexOfFreeVariable(var1));
    }

    /**
     * Disposed model – any further lookup should fail.
     */
    @Test
    void testIndexOfFreeVariable_AfterDispose() {
        model.dispose();
        // ProgrammingError extends RuntimeException
        assertThrows(RuntimeException.class, () -> model.indexOfFreeVariable(var0), "Calling after dispose should throw an exception");
    }

    /**
     * Simplify removes a fixed variable – subsequent lookup throws IllegalArgumentException.
     */
    @Test
    void testIndexOfFreeVariable_SimplifyRemovesVariable() {
        var0.fix(BigDecimal.ZERO);
        // var0 is removed
        model.simplify();
        assertThrows(IllegalArgumentException.class, () -> model.indexOfFreeVariable(var0), "Variable removed by simplify should cause IllegalArgumentException");
    }

    /**
     * Corrupt internal index via reflection – should result in an exception.
     */
    @Test
    void testIndexOfFreeVariable_CorruptInternalIndex() throws Exception {
        // The internal index field in Variable is called "index". Setting it to null should cause a failure.
        Field indexField = Variable.class.getDeclaredField("index");
        indexField.setAccessible(true);
        indexField.set(var0, null);
        assertThrows(RuntimeException.class, () -> model.indexOfFreeVariable(var0), "Corrupted internal index should cause an exception (IllegalArgumentException or IndexOutOfBoundsException)");
    }
}