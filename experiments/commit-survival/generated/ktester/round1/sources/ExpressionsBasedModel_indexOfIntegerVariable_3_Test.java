package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOfIntegerVariable_3_Test {

    private ExpressionsBasedModel model;

    // an integer variable
    private Variable intVar;

    // a continuous (non‑integer) variable
    private Variable contVar;

    @FunctionalInterface
    private interface VariableBuilder {

        Variable build(ExpressionsBasedModel model);
    }

    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        intVar = model.addVariable(Variable.make("IntVar").integer(true).lower(0).upper(10));
        contVar = model.addVariable(Variable.make("ContVar").lower(0).upper(5));
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
        intVar = null;
        contVar = null;
    }

    /* --------------------------------------------------------------
     *  Normal cases – simple, deterministic expectations
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(Arguments.of("firstInt", 0, (VariableBuilder) (m) -> m.addVariable(Variable.make("firstInt").integer(true).lower(0).upper(1))), Arguments.of("thirdInt", 2, (VariableBuilder) (m) -> {
            m.addVariable(Variable.make("int0").integer(true).lower(0).upper(1));
            m.addVariable(Variable.make("int1").integer(true).lower(0).upper(1));
            return m.addVariable(Variable.make("thirdInt").integer(true).lower(0).upper(1));
        }), Arguments.of("nonInt", -1, (VariableBuilder) (m) -> m.addVariable(Variable.make("nonInt").lower(0).upper(1))));
    }

    @Test
    void testIntegerVariableReturnsValidIndex() {
        int idx = model.indexOfIntegerVariable(intVar);
        assertTrue(idx >= 0, "Index of an integer variable should be non‑negative");
        assertEquals(0, idx);
    }

    @Test
    void testContinuousVariableReturnsMinusOne() {
        int idx = model.indexOfIntegerVariable(contVar);
        assertEquals(-1, idx, "Non‑integer variables should yield -1");
    }

    @Test
    void testNullVariableThrowsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable((Variable) null));
    }

    @Test
    void testDelegateToIntIndexVersion() {
        int globalIndex = intVar.getIndex().index;
        int idxFromVariable = model.indexOfIntegerVariable(intVar);
        int idxFromInt = model.indexOfIntegerVariable(globalIndex);
        assertEquals(idxFromVariable, idxFromInt, "Both overloads must return the same index");
    }

    @ParameterizedTest(name = "{index} ⇒ variable={0}, expected={1}")
    @MethodSource("normalCasesProvider")
    void testNormalCases(String name, int expected, VariableBuilder builder) {
        Variable var = builder.build(model);
        int idx = model.indexOfIntegerVariable(var);
        assertEquals(expected, idx, "Unexpected index for variable '" + name + "'");
    }

    /* --------------------------------------------------------------
     *  Edge cases – more involved situations
     * -------------------------------------------------------------- */
    @Test
    void testVariableFromOtherModelReturnsMinusOne() {
        ExpressionsBasedModel other = new ExpressionsBasedModel();
        Variable foreign = other.addVariable(Variable.make("ForeignInt").integer(true).lower(0).upper(1));
        assertEquals(-1, model.indexOfIntegerVariable(foreign), "Variable belonging to another model must not be found");
    }

    @Test
    void testIntegerFlagClearedAfterAdditionReturnsMinusOne() {
        Variable v = model.addVariable(Variable.make("TmpInt").integer(true).lower(0).upper(1));
        try {
            v.setInteger(false);
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            try {
                Field intFlag = Variable.class.getDeclaredField("integer");
                intFlag.setAccessible(true);
                intFlag.setBoolean(v, false);
            } catch (Exception ex) {
                fail("Unable to clear integer flag via reflection", ex);
            }
        }
        assertEquals(-1, model.indexOfIntegerVariable(v), "Variable that was integer but later cleared must not be reported as integer");
    }

    @Test
    void testForeignIntegerVarWithUnrelatedIndexReturnsMinusOne() throws Exception {
        Variable foreign = Variable.make("ForeignIdx");
        Field indexField = Variable.class.getDeclaredField("index");
        indexField.setAccessible(true);
        Class<?> intIdxCls = Class.forName("org.ojalgo.structure.Structure1D$IntIndex");
        Object intIdx = intIdxCls.getDeclaredConstructor(int.class).newInstance(100);
        indexField.set(foreign, intIdx);
        try {
            foreign.setInteger(true);
        } catch (Exception ignored) {
        }
        assertEquals(-1, model.indexOfIntegerVariable(foreign), "Variable with an index that was never added must yield -1");
    }

    @Test
    void testSixthIntegerVariableReturnsIndex5() {
        for (int i = 0; i < 5; i++) {
            model.addVariable(Variable.make("Int" + i).integer(true).lower(0).upper(1));
        }
        Variable sixth = model.addVariable(Variable.make("Int5").integer(true).lower(0).upper(1));
        int idx = model.indexOfIntegerVariable(sixth);
        assertEquals(5, idx, "Sixth integer variable should have integer‑variable index 5");
    }

    @Test
    void testIntegerVariableAfterContinuousVariablesReturnsZero() {
        for (int i = 0; i < 3; i++) {
            model.addVariable(Variable.make("Cont" + i).lower(0).upper(1));
        }
        Variable firstInt = model.addVariable(Variable.make("FirstInt").integer(true).lower(0).upper(1));
        int idx = model.indexOfIntegerVariable(firstInt);
        assertEquals(0, idx, "First integer variable should be index 0 regardless of continuous vars order");
    }

    @Test
    void testVariableNeverAddedToModelThrowsNPE() {
        Variable neverAdded = Variable.make("NeverAdded");
        assertThrows(NullPointerException.class, () -> model.indexOfIntegerVariable(neverAdded));
    }

    @Test
    void testVariableWithNegativeGlobalIndexThrowsIllegalArgumentException() throws Exception {
        Variable v = Variable.make("NegIdx");
        Field indexField = Variable.class.getDeclaredField("index");
        indexField.setAccessible(true);
        Class<?> intIdxCls = Class.forName("org.ojalgo.structure.Structure1D$IntIndex");
        Object negIdx = intIdxCls.getDeclaredConstructor(int.class).newInstance(-5);
        indexField.set(v, negIdx);
        try {
            v.setInteger(true);
        } catch (Exception ignored) {
        }
        assertThrows(IllegalArgumentException.class, () -> model.indexOfIntegerVariable(v), "Negative global index should result in IllegalArgumentException");
    }

    @Test
    void testVariableWithExcessivelyLargeGlobalIndexThrowsIndexOutOfBoundsException() throws Exception {
        Variable v = Variable.make("HugeIdx");
        Field indexField = Variable.class.getDeclaredField("index");
        indexField.setAccessible(true);
        Class<?> intIdxCls = Class.forName("org.ojalgo.structure.Structure1D$IntIndex");
        Object hugeIdx = intIdxCls.getDeclaredConstructor(int.class).newInstance(Integer.MAX_VALUE);
        indexField.set(v, hugeIdx);
        try {
            v.setInteger(true);
        } catch (Exception ignored) {
        }
        assertThrows(IndexOutOfBoundsException.class, () -> model.indexOfIntegerVariable(v), "Excessively large index should trigger IndexOutOfBoundsException");
    }

    @Test
    void testMethodAfterModelDisposedThrowsIllegalStateException() {
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.indexOfIntegerVariable(intVar), "Calling after dispose should raise IllegalStateException");
    }
}
